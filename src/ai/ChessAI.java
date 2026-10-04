package ai;

import board.Board;
import game.Rules;
import move.Move;
import pieces.PieceColor;

import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Public interface to the chess AI.
 *
 * The AI runs on a dedicated background thread so it never blocks the JavaFX
 * Application Thread. When the search finishes it calls the {@code callback}
 * on the JavaFX thread via {@link javafx.application.Platform#runLater}.
 *
 * Usage:
 * <pre>
 *   ChessAI ai = new ChessAI(AILevel.HARD, PieceColor.BLACK);
 *   ai.requestMove(game.getBoard(), move -> game.tryMove(...));
 *   // later, when the game ends or the user switches mode:
 *   ai.shutdown();
 * </pre>
 */
public class ChessAI {

    private final AILevel      level;
    private final PieceColor   side;
    private final Random       rng = new Random();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chess-ai");
        t.setDaemon(true);
        return t;
    });

    /** Move currently being computed (cancelled if a new request arrives). */
    private Future<?> current;

    /**
     * Creates an AI player.
     *
     * @param level the difficulty level
     * @param side  the color this AI plays (WHITE or BLACK)
     */
    public ChessAI(AILevel level, PieceColor side) {
        this.level = level;
        this.side  = side;
    }

    // --- --- --- --- --- PUBLIC API --- --- --- --- --- //

    /**
     * Asynchronously computes the best move for the AI's side.
     *
     * The callback is invoked on the <b>JavaFX Application Thread</b> once the
     * search finishes. The move passed to the callback has already been validated
     * against the given board; the caller should play it via {@code Game.tryMove}.
     *
     * If a previous search is still running, it is cancelled first.
     *
     * @param board    position to search (a deep copy is taken immediately)
     * @param callback receives the chosen Move, or null if no legal moves exist
     */
    public void requestMove(Board board, Consumer<Move> callback) {
        // Cancel any ongoing search.
        if (current != null && !current.isDone()) {
            current.cancel(true);
        }

        // Take a snapshot of the board to avoid data races.
        Board snapshot = deepCopy(board);

        current = executor.submit(() -> {
            long startTime = System.currentTimeMillis();
            Move chosen = compute(snapshot);
            long elapsed = System.currentTimeMillis() - startTime;
            
            // Artificial delay to simulate human thinking (between 2 and 4 seconds)
            long targetDelay = 2000 + rng.nextInt(2000);
            if (elapsed < targetDelay) {
                try {
                    Thread.sleep(targetDelay - elapsed);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            javafx.application.Platform.runLater(() -> callback.accept(chosen));
        });
    }

    /**
     * Computes the best move for a given evaluation request without spawning a thread.
     * (Used internally and in tests.)
     *
     * @param board position to search
     * @return the chosen move, or null if no legal moves exist
     */
    public Move computeSync(Board board) {
        return compute(board);
    }

    /** Stops the background thread. Must be called when the AI is no longer needed. */
    public void shutdown() {
        executor.shutdownNow();
    }

    // --- --- --- --- --- INTERNALS --- --- --- --- --- //

    private Move compute(Board board) {
        List<Move> legal = Rules.legalMoves(board, side);
        if (legal.isEmpty()) return null;

        // Beginner: random move 30 % of the time.
        if (level.randomise && rng.nextDouble() < 0.30) {
            return legal.get(rng.nextInt(legal.size()));
        }

        boolean quiescence = (level == AILevel.EXPERT);
        Move best = Minimax.bestMove(board, level.depth, side, quiescence);

        // Apply the evaluation scale: if scale < 1, occasionally pick a sub-optimal move.
        if (level.evalScale < 1.0 && rng.nextDouble() > level.evalScale) {
            // Pick randomly from the top-3 moves instead of the very best.
            MoveOrdering.order(legal, board);
            int cap = Math.min(3, legal.size());
            best = legal.get(rng.nextInt(cap));
        }

        return best != null ? best : legal.get(0);
    }

    /** Deep-copies the board piece array and state for thread safety. */
    private static Board deepCopy(Board original) {
        Board copy = new Board(null);   // creates a fresh standard starting position
        // Overwrite the fresh board with the actual position.
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                pieces.Piece p = original.board[y][x];
                copy.board[y][x] = (p == null) ? null : p.duplicate();
            }
        }
        copy.state = original.state.copy();
        return copy;
    }

    // --- --- --- --- --- ACCESSORS --- --- --- --- --- //

    public AILevel getLevel() { return level; }
    public PieceColor getSide() { return side; }
}
