package move;

import board.State;
import pieces.Piece;
import pieces.PieceType;

/**
 * One move on the board.
 *
 * A Move carries everything needed to describe, display and undo a move:
 * source/destination squares, the moving piece, the captured piece,
 * promotion/castling/en passant information, the move number, and a snapshot
 * of the State the board was in before the move was applied (saved by
 * Board.applyMove so Board.undoMove can restore it exactly).
 */
public class Move {

    // --- Geometry --- //
    public final int fromX, fromY;
    public final int toX, toY;

    // --- Participants --- //
    public final Piece piece;               // the piece that moves
    public final Piece captured;            // the piece that is captured, or null
    public final int capturedX, capturedY;  // where the captured piece stood (differs from toX/toY for en passant)

    // --- Special move info --- //
    public final PieceType promotion;       // the piece promoted to, or null if this is not a promotion
    public final CastleSide castle;         // which castling side, or CastleSide.NONE
    public final boolean enPassant;         // true if this move captures en passant

    // --- Book-keeping --- //
    public final int moveNumber;            // fullmove number when this move was made

    /** The board state before this move was applied; filled in by Board.applyMove. */
    public State previousState;

    public Move(int fromX, int fromY, int toX, int toY,
                Piece piece, Piece captured, PieceType promotion,
                CastleSide castle, boolean enPassant, int moveNumber) {
        this.fromX      = fromX;
        this.fromY      = fromY;
        this.toX        = toX;
        this.toY        = toY;
        this.piece      = piece;
        this.captured   = captured;
        this.promotion  = promotion;
        this.castle     = castle == null ? CastleSide.NONE : castle;
        this.enPassant  = enPassant;
        this.moveNumber = moveNumber;

        if (enPassant) {
            // the captured pawn stands next to the destination, on the mover's own rank
            this.capturedX = toX;
            this.capturedY = fromY;
        } else {
            this.capturedX = toX;
            this.capturedY = toY;
        }
    }

    /** Convenience constructor for an ordinary (non-special) move. */
    public Move(int fromX, int fromY, int toX, int toY, Piece piece, int moveNumber) {
        this(fromX, fromY, toX, toY, piece, null, null, CastleSide.NONE, false, moveNumber);
    }

    public boolean isCapture() {
        return captured != null;
    }

    public boolean isPromotion() {
        return promotion != null;
    }

    public boolean targets(int x, int y) {
        return toX == x && toY == y;
    }

    /** Simple notation used by the move list, e.g. "Pe2-e4", "Pe5xd6", "O-O", "Pb7-b8=Q". */
    public String notation() {
        if (castle == CastleSide.KING_SIDE) return "O-O";
        if (castle == CastleSide.QUEEN_SIDE) return "O-O-O";

        StringBuilder sb = new StringBuilder();
        sb.append(piece.type.code);
        sb.append(squareName(fromX, fromY));
        sb.append(captured != null ? 'x' : '-');
        sb.append(squareName(toX, toY));
        if (promotion != null) sb.append('=').append(promotion.code);
        if (enPassant) sb.append(" e.p.");
        return sb.toString();
    }

    public static String squareName(int x, int y) {
        return "" + (char) ('a' + x) + (y + 1);
    }

    @Override
    public String toString() {
        return notation();
    }
}
