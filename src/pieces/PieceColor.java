package pieces;

/**
 * The two possible sides in chess.
 * The single-letter codes match the existing piece naming ("WP1", "BP8", ...)
 * and the piece image assets (WP.png, BK.png, ...).
 */
public enum PieceColor {
    WHITE('W'),
    BLACK('B');

    public final char code;

    PieceColor(char code) {
        this.code = code;
    }

    /** The opponent of this color. */
    public PieceColor other() {
        return this == WHITE ? BLACK : WHITE;
    }

    public static PieceColor fromCode(char code) {
        if (code == 'W') return WHITE;
        if (code == 'B') return BLACK;
        throw new IllegalArgumentException("Unknown piece color code: " + code);
    }
}
