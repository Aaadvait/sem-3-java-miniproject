package game;

import board.Board;
import board.State;
import move.Move;
import move.MoveHistory;
import pieces.Piece;
import pieces.PieceColor;
import pieces.PieceType;
import persistence.FENHandler;
import persistence.PGNHandler;
import analytics.OpeningBook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Coordinates one game of chess: the board, the rules, the turn, the clocks
 * and the result. The GUI talks only to this class (never to the pieces
 * directly) and is notified through a {@link Listener}.
 *
 * Extended from the original to support:
 *   - Undo / redo (via MoveHistory)
 *   - Increment-based time controls (Fischer)
 *   - FEN and PGN import/export
 *   - Named players (for PGN export and statistics)
 *   - Opening recognition
 *   - Replay mode (freeze the board at a historical move)
 */
public class Game {

    // --- --- --- --- --- LISTENER --- --- --- --- --- //

    /** What the GUI needs to know about; implemented by GamePane. */
    public interface Listener {
        /** A move was played (or undone/redone): position, clocks and turn changed. */
        void onPositionChanged();

        /** The game just ended. */
        void onGameOver(GameStatus status, PieceColor winner);   // winner == null -> draw
    }

    // --- --- --- --- --- TIME --- --- --- --- --- //

    public static final long FIVE_MINUTES_MS  = 5 * 60 * 1000L;
    public static final long TEN_MINUTES_MS   = 10 * 60 * 1000L;
    public static final long UNTIMED          = 0L;

    private static final long NANOS_PER_MS = 1_000_000L;

    // --- --- --- --- --- STATE --- --- --- --- --- //

    private final Board       board;
    private final MoveHistory history;
    private final long        timeControlNanos;
    private final long        incrementNanos;

    private long whiteNanos, blackNanos;
    private long lastTickNanos;
    private boolean clockRunning = false;

    private GameStatus status = GameStatus.PLAYING;
    private PieceColor winner = null;

    private final Map<String, Integer> positionCounts = new HashMap<>();

    private Listener listener;

    // --- Player names (for PGN / stats) ---
    private String whiteName = "White";
    private String blackName = "Black";

    // --- Replay mode ---
    private int replayIndex = -1;       // -1 = not in replay mode; ≥ 0 = viewing this ply

    // --- --- --- --- --- CONSTRUCTION --- --- --- --- --- //

    /**
     * @param savedGame      reserved for loading a saved position (pass null for a fresh game)
     * @param timeControlMs  starting time per player in milliseconds, or UNTIMED
     */
    public Game(String savedGame, long timeControlMs) {
        this(savedGame, timeControlMs, 0L);
    }

    /**
     * @param savedGame      reserved for loading a saved position (pass null for a fresh game)
     * @param timeControlMs  starting time per player in milliseconds, or UNTIMED
     * @param incrementMs    increment added after each move (Fischer increment), or 0
     */
    public Game(String savedGame, long timeControlMs, long incrementMs) {
        this.board            = new Board(savedGame);
        this.history          = new MoveHistory(board);
        this.timeControlNanos = timeControlMs * NANOS_PER_MS;
        this.incrementNanos   = incrementMs   * NANOS_PER_MS;
        this.whiteNanos       = this.timeControlNanos;
        this.blackNanos       = this.timeControlNanos;
        this.lastTickNanos    = now();
        this.positionCounts.put(board.positionKey(), 1);
    }

    /** Constructs a game using a specific {@link GameMode}. */
    public static Game fromMode(GameMode mode) {
        return new Game(null, mode.timeMs, mode.incrementMs);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    // --- --- --- --- --- PLAYER NAMES --- --- --- --- --- //

    public void setWhiteName(String name) { this.whiteName = name == null ? "White" : name; }
    public void setBlackName(String name) { this.blackName = name == null ? "Black" : name; }
    public String getWhiteName() { return whiteName; }
    public String getBlackName() { return blackName; }

    // --- --- --- --- --- QUERIES --- --- --- --- --- //

    public Board getBoard()             { return board; }
    public State getState()             { return board.state; }
    public MoveHistory getMoveHistory() { return history; }
    /** @deprecated use {@link #getMoveHistory()} instead */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    public List<Move> moveStack        = Collections.emptyList(); // kept for binary compatibility

    public PieceColor turn()            { return board.state.turn; }
    public GameStatus status()          { return status; }
    public PieceColor winner()          { return winner; }
    public boolean isGameOver()         { return status.isGameOver(); }

    public boolean isInCheck(PieceColor color) {
        return board.state.inCheck(color);
    }

    public boolean isUntimed()          { return timeControlNanos == UNTIMED; }

    public long whiteMillis()           { return whiteNanos / NANOS_PER_MS; }
    public long blackMillis()           { return blackNanos / NANOS_PER_MS; }

    /** All legal moves for the piece on (x, y); empty if it is not that side's turn. */
    public List<Move> legalMovesFrom(int x, int y) {
        if (isGameOver()) return Collections.emptyList();
        PieceColor turn = board.state.turn;
        return Rules.legalMovesFrom(board, x, y, turn);
    }

    /** Current opening name based on moves played so far (empty string if none recognised). */
    public String openingName() {
        return OpeningBook.recognise(history.getMoves());
    }

    // --- --- --- --- --- PLAYING --- --- --- --- --- //

    /**
     * Attempts to move the piece on (fromX, fromY) to (toX, toY).
     * Only a legal move of the side to move changes anything.
     *
     * @param promotionType the piece to promote to (required for promotions)
     * @return true if the move was legal and was played
     */
    public boolean tryMove(int fromX, int fromY, int toX, int toY, PieceType promotionType) {
        if (isGameOver()) return false;
        if (replayIndex >= 0) return false;   // cannot play in replay mode

        Move chosen = null;
        for (Move m : legalMovesFrom(fromX, fromY)) {
            if (!m.targets(toX, toY)) continue;
            if (m.isPromotion()) {
                if (m.promotion == promotionType) chosen = m;
            } else if (promotionType == null) {
                chosen = m;
            }
        }
        if (chosen == null) return false;

        play(chosen);
        return true;
    }

    /**
     * Directly plays a validated Move (used by the AI and network layer).
     * The caller is responsible for ensuring the move is legal.
     */
    public void playMove(Move m) {
        if (isGameOver() || replayIndex >= 0) return;
        play(m);
    }

    /** Plays a move that was already validated against the rules. */
    private void play(Move m) {
        accrueClock();
        history.push(m);         // push() calls board.applyMove internally
        // After push, update the legacy public list reference.
        moveStack = history.getMoves();

        // Apply Fischer increment to the player who just moved.
        if (!isUntimed() && incrementNanos > 0) {
            if (m.piece.color == PieceColor.WHITE) {
                whiteNanos = Math.min(timeControlNanos > 0 ? Long.MAX_VALUE : Long.MAX_VALUE,
                        whiteNanos + incrementNanos);
            } else {
                blackNanos = Math.min(Long.MAX_VALUE, blackNanos + incrementNanos);
            }
        }

        recordPosition();
        updateEndOfGameFlags();

        if (listener != null) {
            listener.onPositionChanged();
            if (isGameOver()) listener.onGameOver(status, winner);
        }
    }

    // --- --- --- --- --- UNDO / REDO --- --- --- --- --- //

    /**
     * Undoes the last move. Does nothing if there is nothing to undo or if the
     * game is over (the caller should explicitly resume the game if desired).
     *
     * @return true if a move was undone
     */
    public boolean undoMove() {
        if (!history.canUndo()) return false;
        if (isGameOver()) return false;
        accrueClock();
        history.undo();         // undo() calls board.undoMove internally
        moveStack = history.getMoves();
        status = GameStatus.PLAYING;
        winner = null;
        replayIndex = -1;
        rebuildPositionCounts();
        if (listener != null) listener.onPositionChanged();
        return true;
    }

    /**
     * Redoes the last undone move.
     *
     * @return true if a move was redone
     */
    public boolean redoMove() {
        if (!history.canRedo()) return false;
        history.redo();
        moveStack = history.getMoves();
        rebuildPositionCounts();
        updateEndOfGameFlags();
        if (listener != null) {
            listener.onPositionChanged();
            if (isGameOver()) listener.onGameOver(status, winner);
        }
        return true;
    }

    // --- --- --- --- --- REPLAY MODE --- --- --- --- --- //

    /**
     * Enters replay mode: the board is rewound to show the position after the
     * given ply (0-based). Does not affect the actual game history.
     *
     * @param plyIndex 0 = position after ply 1; -1 = exit replay mode
     */
    public void replayGoto(int plyIndex) {
        List<Move> moves = history.getMoves();
        if (plyIndex < 0 || plyIndex >= moves.size()) {
            exitReplay();
            return;
        }
        // Rebuild the board from the start.
        Board temp = new Board(null);
        for (int i = 0; i <= plyIndex && i < moves.size(); i++) {
            temp.applyMove(moves.get(i));
        }
        // Mirror the temp board back onto the real board (display only – history is intact).
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                board.board[y][x] = temp.board[y][x];
            }
        }
        board.state = temp.state;
        replayIndex = plyIndex;
        if (listener != null) listener.onPositionChanged();
    }

    /** Exits replay mode and restores the live position. */
    public void exitReplay() {
        if (replayIndex < 0) return;
        // Restore live position by replaying all moves from scratch.
        Board temp = new Board(null);
        for (Move m : history.getMoves()) temp.applyMove(m);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                board.board[y][x] = temp.board[y][x];
            }
        }
        board.state = temp.state;
        replayIndex = -1;
        if (listener != null) listener.onPositionChanged();
    }

    public boolean isInReplay()  { return replayIndex >= 0; }
    public int    replayIndex()  { return replayIndex; }

    // --- --- --- --- --- PGN / FEN --- --- --- --- --- //

    /** Returns a FEN string representing the current board position. */
    public String toFEN() {
        return FENHandler.toFEN(board);
    }

    /**
     * Returns the PGN for this game so far.
     *
     * @param result the game result string ("1-0", "0-1", "1/2-1/2", or "*")
     */
    public String toPGN(String result) {
        return PGNHandler.export(history.getMoves(), board, result, whiteName, blackName);
    }

    // --- --- --- --- --- END OF GAME --- --- --- --- --- //

    private void updateEndOfGameFlags() {
        State s = board.state;
        PieceColor sideToMove = s.turn;
        PieceColor mover = sideToMove.other();

        s.checkWhite = Rules.isInCheck(board, PieceColor.WHITE);
        s.checkBlack = Rules.isInCheck(board, PieceColor.BLACK);

        boolean hasMoves = Rules.hasLegalMoves(board, sideToMove);
        if (!hasMoves) {
            if (s.inCheck(sideToMove)) {
                if (sideToMove == PieceColor.WHITE) s.checkmateWhite = true;
                else s.checkmateBlack = true;
                endGame(GameStatus.CHECKMATE, mover);
            } else {
                s.stalemate = true;
                endGame(GameStatus.STALEMATE, null);
            }
            return;
        }

        if (s.halfmoveClock >= 100) {
            endGame(GameStatus.FIFTY_MOVE_RULE, null);
            return;
        }
        if (hasInsufficientMaterial()) {
            endGame(GameStatus.INSUFFICIENT_MATERIAL, null);
            return;
        }
        if (positionCounts.getOrDefault(board.positionKey(), 0) >= 3) {
            endGame(GameStatus.THREEFOLD_REPETITION, null);
        }
    }

    private void endGame(GameStatus newStatus, PieceColor winner) {
        this.status = newStatus;
        this.winner = winner;
        this.clockRunning = false;
    }

    /** The game was left before finishing. */
    public void abandon() {
        if (!isGameOver()) {
            pauseClock();
            endGame(GameStatus.ABANDONED, null);
            if (listener != null) listener.onGameOver(status, winner);
        }
    }

    /** Neither side has enough material to force checkmate. */
    private boolean hasInsufficientMaterial() {
        int minors = 0;
        List<Piece> bishops = new ArrayList<>();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece p = board.board[y][x];
                if (p == null || p.type == PieceType.KING) continue;
                if (p.type == PieceType.PAWN || p.type == PieceType.ROOK || p.type == PieceType.QUEEN)
                    return false;
                minors++;
                if (p.type == PieceType.BISHOP) bishops.add(p);
            }
        }
        if (minors <= 1) return true;
        if (minors == 2 && bishops.size() == 2) {
            Piece a = bishops.get(0), b = bishops.get(1);
            return ((a.posX + a.posY) % 2) == ((b.posX + b.posY) % 2);
        }
        return false;
    }

    private void recordPosition() {
        String key = board.positionKey();
        positionCounts.merge(key, 1, Integer::sum);
    }

    /** Rebuilds the position count map from scratch (needed after undo). */
    private void rebuildPositionCounts() {
        positionCounts.clear();
        Board temp = new Board(null);
        positionCounts.put(temp.positionKey(), 1);
        for (Move m : history.getMoves()) {
            temp.applyMove(m);
            positionCounts.merge(temp.positionKey(), 1, Integer::sum);
        }
    }

    // --- --- --- --- --- CLOCK --- --- --- --- --- //

    /** Starts the active clock (called when the game screen opens). */
    public void startClock() {
        if (isGameOver()) return;
        lastTickNanos = now();
        clockRunning = true;
    }

    /** Accrues the elapsed time and stops ticking. */
    public void pauseClock() {
        if (clockRunning) accrueClock();
        clockRunning = false;
    }

    /** Accrues time for the side to move. Called by the GUI's Timeline every ~100 ms. */
    public void tickClock() {
        if (!clockRunning || isGameOver()) return;
        accrueClock();
    }

    private void accrueClock() {
        long t = now();
        long elapsed = t - lastTickNanos;
        lastTickNanos = t;
        if (!clockRunning || elapsed <= 0) return;

        if (board.state.turn == PieceColor.WHITE) {
            if (isUntimed()) {
                whiteNanos += elapsed;
            } else {
                whiteNanos = Math.max(0, whiteNanos - elapsed);
                if (whiteNanos == 0) {
                    clockRunning = false;
                    endGame(GameStatus.TIMEOUT, PieceColor.BLACK);
                    if (listener != null) listener.onGameOver(status, winner);
                }
            }
        } else {
            if (isUntimed()) {
                blackNanos += elapsed;
            } else {
                blackNanos = Math.max(0, blackNanos - elapsed);
                if (blackNanos == 0) {
                    clockRunning = false;
                    endGame(GameStatus.TIMEOUT, PieceColor.WHITE);
                    if (listener != null) listener.onGameOver(status, winner);
                }
            }
        }
    }

    public boolean isClockRunning() { return clockRunning; }

    /** Current wall-clock time in nanoseconds; overridable so tests can drive the clock. */
    protected long now() {
        return System.nanoTime();
    }
}
