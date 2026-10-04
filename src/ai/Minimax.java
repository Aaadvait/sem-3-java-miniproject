package ai;

import board.Board;
import game.Rules;
import move.Move;
import pieces.PieceColor;

import java.util.List;

/**
 * Minimax search with alpha-beta pruning.
 *
 * The search is always done on a copy of the board (via Board.applyMove /
 * Board.undoMove); the real game board is never modified.
 *
 * Quiescence search is performed at leaf nodes (EXPERT level only) to avoid
 * the horizon effect in tactical positions.
 */
public final class Minimax {

    /** Score assigned to a checkmate in N plies (large, beats all material scores). */
    private static final int MATE_SCORE  = 100_000;
    private static final int INF         = Integer.MAX_VALUE / 2;

    /** Maximum number of captures/checks examined in quiescence search. */
    private static final int QUIESCE_DEPTH = 4;

    private Minimax() { }

    // --- --- --- --- --- PUBLIC API --- --- --- --- --- //

    /**
     * Finds the best move for {@code side} at the given search depth.
     *
     * @param board starting position
     * @param depth search depth (plies)
     * @param side  the side to move
     * @param quiescence true = enable quiescence search at leaf nodes
     * @return the best Move found, or null if there are no legal moves
     */
    public static Move bestMove(Board board, int depth, PieceColor side, boolean quiescence) {
        List<Move> moves = Rules.legalMoves(board, side);
        if (moves.isEmpty()) return null;

        MoveOrdering.order(moves, board);

        Move best = null;
        int bestScore = -INF;
        int alpha = -INF, beta = INF;

        for (Move m : moves) {
            board.applyMove(m);
            int score = -negamax(board, depth - 1, -beta, -alpha,
                                 side.other(), quiescence);
            board.undoMove(m);

            if (score > bestScore) {
                bestScore = score;
                best = m;
            }
            alpha = Math.max(alpha, score);
        }
        return best;
    }

    // --- --- --- --- --- NEGAMAX CORE --- --- --- --- --- //

    /**
     * Negamax with alpha-beta pruning.
     * Score is always from the perspective of {@code side} (positive = good for side).
     */
    private static int negamax(Board board, int depth, int alpha, int beta,
                               PieceColor side, boolean quiescence) {
        List<Move> moves = Rules.legalMoves(board, side);

        if (moves.isEmpty()) {
            // No legal moves: either checkmate or stalemate.
            if (Rules.isInCheck(board, side)) {
                return -(MATE_SCORE - depth);  // prefer faster mates
            }
            return 0;    // stalemate
        }

        if (depth <= 0) {
            if (quiescence) {
                return quiesce(board, QUIESCE_DEPTH, alpha, beta, side);
            }
            return fromSide(Evaluator.evaluate(board), side);
        }

        MoveOrdering.order(moves, board);

        for (Move m : moves) {
            board.applyMove(m);
            int score = -negamax(board, depth - 1, -beta, -alpha, side.other(), quiescence);
            board.undoMove(m);

            if (score >= beta) return beta;   // beta cut-off
            if (score > alpha)  alpha = score;
        }
        return alpha;
    }

    // --- --- --- --- --- QUIESCENCE SEARCH --- --- --- --- --- //

    /**
     * Extends the search until the position is "quiet" (no captures available).
     * This avoids the horizon effect where the engine stops just before a piece
     * is captured or recaptured.
     */
    private static int quiesce(Board board, int depth, int alpha, int beta, PieceColor side) {
        int standPat = fromSide(Evaluator.evaluate(board), side);
        if (standPat >= beta) return beta;
        if (standPat > alpha) alpha = standPat;
        if (depth <= 0) return alpha;

        // Generate only captures.
        List<Move> captures = Rules.legalMoves(board, side);
        captures.removeIf(m -> !m.isCapture() && m.promotion == null);
        MoveOrdering.order(captures, board);

        for (Move m : captures) {
            board.applyMove(m);
            int score = -quiesce(board, depth - 1, -beta, -alpha, side.other());
            board.undoMove(m);

            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }
        return alpha;
    }

    // --- --- --- --- --- HELPERS --- --- --- --- --- //

    /** Converts a White-centric score to a side-to-move-centric score. */
    private static int fromSide(int whiteScore, PieceColor side) {
        return side == PieceColor.WHITE ? whiteScore : -whiteScore;
    }
}
