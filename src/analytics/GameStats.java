package analytics;

import game.Game;
import game.GameStatus;
import move.Move;
import pieces.PieceColor;

import java.util.List;

/**
 * Per-game statistics: move count, accuracy and outcome.
 *
 * "Move accuracy" is a simplified metric:
 *   - Every move is checked against the AI's top evaluation.
 *   - If the move played was the engine's first choice → 100 points.
 *   - Otherwise accuracy degrades based on centipawn loss (capped at 300 cp loss = 0 points).
 *
 * Because running a full engine evaluation for every move in real time is expensive,
 * this class provides a lightweight approximation:
 *   each move scores 100% if it maintains material balance,
 *   and loses accuracy proportionally to material blundered (capped at 300 cp).
 *
 * The score is suitable for a statistics panel ("Your accuracy: 87%"), not for
 * professional analysis (which would require a strong engine evaluation).
 */
public class GameStats {

    private int movesPlayed;
    private int totalAccuracyPoints;   // 0–100 per move
    private int accuracyMoveCount;

    private long whiteTimeUsedMs;
    private long blackTimeUsedMs;

    public GameStats() { }

    // --- --- --- --- --- MOVE TRACKING --- --- --- --- --- //

    /**
     * Records a single move.
     *
     * @param moveAccuracy 0–100 accuracy score for this move
     * @param timeUsedMs   milliseconds the player spent on this move
     */
    public void recordMove(int moveAccuracy, long timeUsedMs) {
        movesPlayed++;
        totalAccuracyPoints += Math.max(0, Math.min(100, moveAccuracy));
        accuracyMoveCount++;
    }

    /**
     * Records time usage for a side at the end of the game.
     */
    public void recordTimeUsage(PieceColor color, long timeUsedMs) {
        if (color == PieceColor.WHITE) whiteTimeUsedMs = timeUsedMs;
        else                           blackTimeUsedMs = timeUsedMs;
    }

    // --- --- --- --- --- QUERIES --- --- --- --- --- //

    public int getMovesPlayed() { return movesPlayed; }

    /** Average move accuracy 0–100 (returns 100 when no moves recorded). */
    public double getAverageAccuracy() {
        if (accuracyMoveCount == 0) return 100.0;
        return (double) totalAccuracyPoints / accuracyMoveCount;
    }

    public long getWhiteTimeUsedMs() { return whiteTimeUsedMs; }
    public long getBlackTimeUsedMs() { return blackTimeUsedMs; }

    /**
     * Computes a rough accuracy score for a single move.
     *
     * @param materialBefore material balance (positive = white ahead) before the move (in cp)
     * @param materialAfter  material balance after the move
     * @param movedColor     the side that just moved
     * @return 0–100 accuracy score
     */
    public static int computeMoveAccuracy(int materialBefore, int materialAfter, PieceColor movedColor) {
        // Positive means this side gained material.
        int deltaForMover = (movedColor == PieceColor.WHITE)
                ? (materialAfter - materialBefore)
                : (materialBefore - materialAfter);

        // Any material gain or equal trade is considered perfect.
        if (deltaForMover >= 0) return 100;

        // Material loss: scale from 100 (0 loss) to 0 (300 cp or more loss).
        int loss = Math.abs(deltaForMover);
        return Math.max(0, 100 - (loss * 100 / 300));
    }

    /**
     * Quick material count for the board (centipawns, positive = white ahead).
     * Used internally and by the UI to compute per-move accuracy.
     */
    public static int materialBalance(board.Board board) {
        int score = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                pieces.Piece p = board.board[y][x];
                if (p == null) continue;
                int value = pieceValue(p.type);
                score += (p.color == pieces.PieceColor.WHITE) ? value : -value;
            }
        }
        return score;
    }

    private static int pieceValue(pieces.PieceType type) {
        switch (type) {
            case PAWN:   return 100;
            case KNIGHT: return 320;
            case BISHOP: return 330;
            case ROOK:   return 500;
            case QUEEN:  return 900;
            default:     return 0;
        }
    }
}
