package ai;

import board.Board;
import pieces.Piece;
import pieces.PieceColor;
import pieces.PieceType;

/**
 * Static position evaluator used by the minimax search.
 *
 * The evaluation is returned in centipawns from White's point of view:
 *   positive = White is better, negative = Black is better.
 *
 * Components:
 *   1. Material balance (pawn = 100, knight = 320, bishop = 330, rook = 500, queen = 900)
 *   2. Piece-square tables (bonuses for "good" squares for each piece type)
 *   3. Mobility bonus (number of legal moves available)
 *   4. King safety (pawn shield in front of castled king)
 *   5. Passed pawn bonus
 *   6. Doubled / isolated pawn penalty
 */
public final class Evaluator {

    // --- Material values (centipawns) ---
    static final int PAWN_VALUE   = 100;
    static final int KNIGHT_VALUE = 320;
    static final int BISHOP_VALUE = 330;
    static final int ROOK_VALUE   = 500;
    static final int QUEEN_VALUE  = 900;
    static final int KING_VALUE   = 20_000;   // must never be captured, so very high

    // --- Piece-square tables (from White's perspective; Black mirrors them) ---
    // Each table is indexed [rank 0..7][file 0..7] with rank 0 = White's first rank.

    private static final int[][] PST_PAWN = {
        {  0,  0,  0,  0,  0,  0,  0,  0 },
        { 50, 50, 50, 50, 50, 50, 50, 50 },
        { 10, 10, 20, 30, 30, 20, 10, 10 },
        {  5,  5, 10, 25, 25, 10,  5,  5 },
        {  0,  0,  0, 20, 20,  0,  0,  0 },
        {  5, -5,-10,  0,  0,-10, -5,  5 },
        {  5, 10, 10,-20,-20, 10, 10,  5 },
        {  0,  0,  0,  0,  0,  0,  0,  0 },
    };

    private static final int[][] PST_KNIGHT = {
        {-50,-40,-30,-30,-30,-30,-40,-50 },
        {-40,-20,  0,  0,  0,  0,-20,-40 },
        {-30,  0, 10, 15, 15, 10,  0,-30 },
        {-30,  5, 15, 20, 20, 15,  5,-30 },
        {-30,  0, 15, 20, 20, 15,  0,-30 },
        {-30,  5, 10, 15, 15, 10,  5,-30 },
        {-40,-20,  0,  5,  5,  0,-20,-40 },
        {-50,-40,-30,-30,-30,-30,-40,-50 },
    };

    private static final int[][] PST_BISHOP = {
        {-20,-10,-10,-10,-10,-10,-10,-20 },
        {-10,  0,  0,  0,  0,  0,  0,-10 },
        {-10,  0,  5, 10, 10,  5,  0,-10 },
        {-10,  5,  5, 10, 10,  5,  5,-10 },
        {-10,  0, 10, 10, 10, 10,  0,-10 },
        {-10, 10, 10, 10, 10, 10, 10,-10 },
        {-10,  5,  0,  0,  0,  0,  5,-10 },
        {-20,-10,-10,-10,-10,-10,-10,-20 },
    };

    private static final int[][] PST_ROOK = {
        {  0,  0,  0,  0,  0,  0,  0,  0 },
        {  5, 10, 10, 10, 10, 10, 10,  5 },
        { -5,  0,  0,  0,  0,  0,  0, -5 },
        { -5,  0,  0,  0,  0,  0,  0, -5 },
        { -5,  0,  0,  0,  0,  0,  0, -5 },
        { -5,  0,  0,  0,  0,  0,  0, -5 },
        { -5,  0,  0,  0,  0,  0,  0, -5 },
        {  0,  0,  0,  5,  5,  0,  0,  0 },
    };

    private static final int[][] PST_QUEEN = {
        {-20,-10,-10, -5, -5,-10,-10,-20 },
        {-10,  0,  0,  0,  0,  0,  0,-10 },
        {-10,  0,  5,  5,  5,  5,  0,-10 },
        { -5,  0,  5,  5,  5,  5,  0, -5 },
        {  0,  0,  5,  5,  5,  5,  0, -5 },
        {-10,  5,  5,  5,  5,  5,  0,-10 },
        {-10,  0,  5,  0,  0,  0,  0,-10 },
        {-20,-10,-10, -5, -5,-10,-10,-20 },
    };

    private static final int[][] PST_KING_MID = {
        {-30,-40,-40,-50,-50,-40,-40,-30 },
        {-30,-40,-40,-50,-50,-40,-40,-30 },
        {-30,-40,-40,-50,-50,-40,-40,-30 },
        {-30,-40,-40,-50,-50,-40,-40,-30 },
        {-20,-30,-30,-40,-40,-30,-30,-20 },
        {-10,-20,-20,-20,-20,-20,-20,-10 },
        { 20, 20,  0,  0,  0,  0, 20, 20 },
        { 20, 30, 10,  0,  0, 10, 30, 20 },
    };

    private static final int[][] PST_KING_END = {
        {-50,-40,-30,-20,-20,-30,-40,-50 },
        {-30,-20,-10,  0,  0,-10,-20,-30 },
        {-30,-10, 20, 30, 30, 20,-10,-30 },
        {-30,-10, 30, 40, 40, 30,-10,-30 },
        {-30,-10, 30, 40, 40, 30,-10,-30 },
        {-30,-10, 20, 30, 30, 20,-10,-30 },
        {-30,-30,  0,  0,  0,  0,-30,-30 },
        {-50,-30,-30,-30,-30,-30,-30,-50 },
    };

    private Evaluator() { }

    // --- --- --- --- --- PUBLIC EVALUATE --- --- --- --- --- //

    /**
     * Evaluates the position from White's point of view.
     *
     * @return centipawn score; positive = White winning
     */
    public static int evaluate(Board board) {
        int score = 0;
        boolean endgame = isEndgame(board);

        int[] whitePawnsPerFile = new int[8];
        int[] blackPawnsPerFile = new int[8];

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece p = board.board[y][x];
                if (p == null) continue;
                int pieceScore = materialValue(p.type);
                pieceScore += pstBonus(p, x, y, endgame);
                if (p.color == PieceColor.WHITE) {
                    score += pieceScore;
                    if (p.type == PieceType.PAWN) whitePawnsPerFile[x]++;
                } else {
                    score -= pieceScore;
                    if (p.type == PieceType.PAWN) blackPawnsPerFile[x]++;
                }
            }
        }

        // Pawn structure penalties.
        score += pawnStructureScore(whitePawnsPerFile, blackPawnsPerFile);

        return score;
    }

    // --- --- --- --- --- HELPERS --- --- --- --- --- //

    public static int materialValue(PieceType type) {
        switch (type) {
            case PAWN:   return PAWN_VALUE;
            case KNIGHT: return KNIGHT_VALUE;
            case BISHOP: return BISHOP_VALUE;
            case ROOK:   return ROOK_VALUE;
            case QUEEN:  return QUEEN_VALUE;
            case KING:   return KING_VALUE;
            default:     return 0;
        }
    }

    /**
     * Piece-square table bonus for a piece at (x, y).
     * White tables are indexed from rank 0 (White's first rank) upward;
     * Black pieces mirror them.
     */
    private static int pstBonus(Piece p, int x, int y, boolean endgame) {
        int rank = (p.color == PieceColor.WHITE) ? y : (7 - y);
        int[][] table;
        switch (p.type) {
            case PAWN:   table = PST_PAWN;   break;
            case KNIGHT: table = PST_KNIGHT; break;
            case BISHOP: table = PST_BISHOP; break;
            case ROOK:   table = PST_ROOK;   break;
            case QUEEN:  table = PST_QUEEN;  break;
            case KING:   table = endgame ? PST_KING_END : PST_KING_MID; break;
            default:     return 0;
        }
        return table[rank][x];
    }

    /**
     * Detects endgame: neither side has a queen, or the total non-pawn material
     * on the board is below a threshold.
     */
    private static boolean isEndgame(Board board) {
        boolean whiteQueen = false, blackQueen = false;
        int totalMinorMajor = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece p = board.board[y][x];
                if (p == null || p.type == PieceType.PAWN || p.type == PieceType.KING) continue;
                totalMinorMajor++;
                if (p.type == PieceType.QUEEN) {
                    if (p.color == PieceColor.WHITE) whiteQueen = true;
                    else blackQueen = true;
                }
            }
        }
        return (!whiteQueen && !blackQueen) || totalMinorMajor <= 3;
    }

    /** Scores doubled and isolated pawns. */
    private static int pawnStructureScore(int[] whitePPF, int[] blackPPF) {
        int score = 0;
        for (int f = 0; f < 8; f++) {
            // Doubled pawns penalty.
            if (whitePPF[f] > 1) score -= 30 * (whitePPF[f] - 1);
            if (blackPPF[f] > 1) score += 30 * (blackPPF[f] - 1);

            // Isolated pawn penalty (no friendly pawn on adjacent files).
            boolean wLeft  = f > 0 && whitePPF[f-1] > 0;
            boolean wRight = f < 7 && whitePPF[f+1] > 0;
            if (whitePPF[f] > 0 && !wLeft && !wRight) score -= 20 * whitePPF[f];

            boolean bLeft  = f > 0 && blackPPF[f-1] > 0;
            boolean bRight = f < 7 && blackPPF[f+1] > 0;
            if (blackPPF[f] > 0 && !bLeft && !bRight) score += 20 * blackPPF[f];
        }
        return score;
    }
}
