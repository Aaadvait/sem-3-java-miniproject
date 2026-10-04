package game;

import board.Board;
import board.State;
import move.Move;
import pieces.Piece;
import pieces.PieceColor;
import pieces.PieceType;

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
 * The clocks are real game state: only the side to move ticks, time switches
 * immediately after a legal move, and reaching 0:00 ends the game. Elapsed
 * time is measured against System.nanoTime (not animation frames), so the
 * clocks stay accurate regardless of frame timing.
 */
public class Game {

    // --- --- --- --- --- LISTENER --- --- --- --- --- //

    /** What the GUI needs to know about; implemented by GamePane. */
    public interface Listener {
        /** A move was played: the position, clocks and turn changed. */
        void onPositionChanged();

        /** The game just ended. */
        void onGameOver(GameStatus status, PieceColor winner);   // winner == null -> draw
    }

    // --- --- --- --- --- TIME --- --- --- --- --- //

    public static final long FIVE_MINUTES_MS  = 5 * 60 * 1000L;
    public static final long TEN_MINUTES_MS   = 10 * 60 * 1000L;
    public static final long UNTIMED          = 0L;   // DEFAULT mode: clocks count up, nobody loses on time

    private static final long NANOS_PER_MS = 1_000_000L;

    // --- --- --- --- --- STATE --- --- --- --- --- //

    private final Board board;                       // the authoritative position
    private final long timeControlNanos;             // per side; UNTIMED -> count up
    private long whiteNanos, blackNanos;             // remaining (or elapsed, when untimed)

    private long lastTickNanos;                      // when the active clock was last accrued
    private boolean clockRunning = false;

    private GameStatus status = GameStatus.PLAYING;
    private PieceColor winner = null;                // null while playing or on a draw

    public final List<Move> moveStack = new ArrayList<>();          // the move history
    private final Map<String, Integer> positionCounts = new HashMap<>();  // for threefold repetition

    private Listener listener;

    // --- --- --- --- --- CONSTRUCTION --- --- --- --- --- //

    /**
     * @param savedGame        reserved for loading a saved position (unused for now)
     * @param timeControlMs    starting time per player in milliseconds (5min / 10min),
     *                         or UNTIMED for the DEFAULT mode
     */
    public Game(String savedGame, long timeControlMs) {
        this.board = new Board(savedGame);
        this.timeControlNanos = timeControlMs * NANOS_PER_MS;
        this.whiteNanos = this.timeControlNanos;
        this.blackNanos = this.timeControlNanos;
        this.lastTickNanos = now();
        this.positionCounts.put(board.positionKey(), 1);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    // --- --- --- --- --- QUERIES --- --- --- --- --- //

    public Board getBoard() {
        return board;
    }

    public State getState() {
        return board.state;
    }

    public PieceColor turn() {
        return board.state.turn;
    }

    public GameStatus status() {
        return status;
    }

    public PieceColor winner() {
        return winner;
    }

    public boolean isGameOver() {
        return status.isGameOver();
    }

    public boolean isInCheck(PieceColor color) {
        return board.state.inCheck(color);
    }

    public boolean isUntimed() {
        return timeControlNanos == UNTIMED;
    }

    public long whiteMillis() {
        return whiteNanos / NANOS_PER_MS;
    }

    public long blackMillis() {
        return blackNanos / NANOS_PER_MS;
    }

    /** All legal moves for the piece on (x, y); empty if it is not that side's turn. */
    public List<Move> legalMovesFrom(int x, int y) {
        if (isGameOver()) return Collections.emptyList();
        PieceColor turn = board.state.turn;
        return Rules.legalMovesFrom(board, x, y, turn);
    }

    // --- --- --- --- --- PLAYING --- --- --- --- --- //

    /**
     * Attempts to move the piece on (fromX, fromY) to (toX, toY).
     * Only a legal move of the side to move changes anything.
     *
     * @param promotionType the piece to promote to; required for promotion
     *                      moves, ignored otherwise (use a move from
     *                      {@link #legalMovesFrom} to discover promotions)
     * @return true if the move was legal and was played
     */
    public boolean tryMove(int fromX, int fromY, int toX, int toY, PieceType promotionType) {
        if (isGameOver()) return false;

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

    /** Plays a move that was already validated against the rules. */
    private void play(Move m) {
        accrueClock();          // the mover keeps every millisecond they used
        board.applyMove(m);     // board + state (turn, castling, ep, counters) change here
        moveStack.add(m);

        recordPosition();
        updateEndOfGameFlags();

        if (listener != null) {
            listener.onPositionChanged();
            if (isGameOver()) listener.onGameOver(status, winner);
        }
    }

    // --- --- --- --- --- END OF GAME --- --- --- --- --- //

    private void updateEndOfGameFlags() {
        State s = board.state;
        PieceColor sideToMove = s.turn;
        PieceColor mover = sideToMove.other();

        // Check is tracked independently for both kings.
        s.checkWhite = Rules.isInCheck(board, PieceColor.WHITE);
        s.checkBlack = Rules.isInCheck(board, PieceColor.BLACK);

        boolean hasMoves = Rules.hasLegalMoves(board, sideToMove);
        if (!hasMoves) {
            if (s.inCheck(sideToMove)) {
                // Checkmate: the side to move is in check and nothing can help.
                if (sideToMove == PieceColor.WHITE) s.checkmateWhite = true;
                else s.checkmateBlack = true;
                endGame(GameStatus.CHECKMATE, mover);
            } else {
                // Stalemate: not in check, but no legal move.
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

    /** The game was left before finishing: stop the clocks and record it. */
    public void abandon() {
        if (!isGameOver()) {
            pauseClock();
            endGame(GameStatus.ABANDONED, null);
            if (listener != null) listener.onGameOver(status, winner);
        }
    }

    /** Neither side has enough material to force checkmate (or the bare-minimum cases). */
    private boolean hasInsufficientMaterial() {
        int minors = 0;
        List<Piece> bishops = new ArrayList<>();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece p = board.board[y][x];
                if (p == null || p.type == PieceType.KING) continue;
                if (p.type == PieceType.PAWN || p.type == PieceType.ROOK || p.type == PieceType.QUEEN) {
                    return false;   // mating material exists
                }
                minors++;
                if (p.type == PieceType.BISHOP) bishops.add(p);
            }
        }

        if (minors <= 1) return true;                       // K vs K, K+B vs K, K+N vs K
        if (minors == 2 && bishops.size() == 2) {           // K+B vs K+B on the same color squares
            Piece a = bishops.get(0), b = bishops.get(1);
            return ((a.posX + a.posY) % 2) == ((b.posX + b.posY) % 2);
        }
        return false;
    }

    private void recordPosition() {
        String key = board.positionKey();
        positionCounts.merge(key, 1, Integer::sum);
    }

    // --- --- --- --- --- CLOCK --- --- --- --- --- //

    /** Starts the active clock (called when the game screen opens). */
    public void startClock() {
        if (isGameOver()) return;
        lastTickNanos = now();
        clockRunning = true;
    }

    /** Accrues the elapsed time and stops ticking (used when leaving the game). */
    public void pauseClock() {
        if (clockRunning) accrueClock();
        clockRunning = false;
    }

    /** Accrues time for the side to move. Called by the GUI's Timeline every ~100 ms. */
    public void tickClock() {
        if (!clockRunning || isGameOver()) return;
        accrueClock();
    }

    /**
     * Moves the elapsed time of the side to move onto their clock.
     * Time is measured with nanoTime, so repeated ticks never drift.
     */
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

    public boolean isClockRunning() {
        return clockRunning;
    }

    /** Current wall-clock time in nanoseconds; overridable so tests can drive the clock. */
    protected long now() {
        return System.nanoTime();
    }
}
