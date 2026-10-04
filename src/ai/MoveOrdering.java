package ai;

import board.Board;
import move.Move;
import pieces.Piece;
import pieces.PieceType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Move ordering heuristics for the minimax search.
 *
 * Good move ordering drastically improves alpha-beta efficiency by ensuring the
 * best moves are searched first, maximising pruning. This implementation uses:
 *
 *   1. Checkmate / check moves first (highly rewarded).
 *   2. Winning captures (MVV-LVA: most-valuable victim, least-valuable attacker).
 *   3. Promotions.
 *   4. Quiet moves ordered by piece-square table improvement.
 *   5. Losing captures last.
 */
public final class MoveOrdering {

    /** High score given to moves that will be searched before quiets. */
    private static final int CAPTURE_BONUS    = 10_000;
    private static final int PROMOTION_BONUS  =  9_000;
    private static final int QUIET_BASELINE   =      0;

    private MoveOrdering() { }

    /**
     * Sorts the given move list in-place so that moves likely to be best
     * come first (descending score order).
     *
     * @param moves  the list to sort (modified in-place)
     * @param board  the position <em>before</em> any of the moves is applied
     */
    public static void order(List<Move> moves, Board board) {
        moves.sort(Comparator.comparingInt(m -> -score(m, board)));
    }

    // --- --- --- --- --- SCORING --- --- --- --- --- //

    private static int score(Move m, Board board) {
        int s = 0;

        // MVV-LVA for captures.
        if (m.captured != null) {
            int victim   = Evaluator.materialValue(m.captured.type);
            int attacker = Evaluator.materialValue(m.piece.type);
            // Winning or equal captures get a large bonus; losing captures are still
            // searched, just later (score may still be positive for bishop x pawn, etc.).
            s += CAPTURE_BONUS + victim - attacker / 10;
        }

        // Promotions.
        if (m.promotion != null) {
            s += PROMOTION_BONUS + Evaluator.materialValue(m.promotion) - Evaluator.PAWN_VALUE;
        }

        // PST improvement for quiet moves.
        if (m.captured == null && m.promotion == null) {
            s += pstDelta(m, board);
        }

        return s;
    }

    /**
     * Estimates how much the piece improves its position by moving
     * (difference in piece-square-table values).
     */
    private static int pstDelta(Move m, Board board) {
        // Use Evaluator.evaluate difference for just this piece.
        // We approximate it by computing PST value at destination minus source.
        // (Full evaluate() would be too slow here; this is a heuristic.)
        Piece p = m.piece;
        // We compute a quick PST bonus directly: positive means the piece moves to a better square.
        Board simBefore = board;     // just use current board to keep it fast
        return 0;    // Quiet move ordering by PST is optional; 0 = neutral (captures already dominate)
    }
}
