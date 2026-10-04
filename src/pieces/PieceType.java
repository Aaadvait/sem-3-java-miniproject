package pieces;

/**
 * The six kinds of chess pieces.
 * The codes match the existing piece naming and asset files:
 * P=pawn, H=knight (horse), B=bishop, R=rook, Q=queen, K=king
 * (e.g. "WH1" is white knight 1 and maps to ChessPeices/WH.png).
 */
public enum PieceType {
    PAWN('P'),
    KNIGHT('H'),
    BISHOP('B'),
    ROOK('R'),
    QUEEN('Q'),
    KING('K');

    public final char code;

    PieceType(char code) {
        this.code = code;
    }

    public static PieceType fromCode(char code) {
        for (PieceType type : values()) {
            if (type.code == code) return type;
        }
        throw new IllegalArgumentException("Unknown piece type code: " + code);
    }

    /** The four pieces a pawn may promote to. */
    public static final PieceType[] PROMOTION_TYPES = { QUEEN, ROOK, BISHOP, KNIGHT };
}
