package pieces;

/**
 * A single chess piece.
 *
 * A Piece knows what it is (color/type) and where it currently stands
 * (posX/posY, 0-based board coordinates: x = file a..h, y = rank 1..8).
 * The Board array and these coordinates are always kept in sync by Board.applyMove.
 */
public class Piece {

    // --- Piece description --- //
    public final PieceColor color;
    public final PieceType type;
    public final int pieceIndex;        // identity inside its starting group (1..8), kept for naming
    public final String pieceName;      // e.g. "WP3", "BH2" (existing naming convention)

    // --- Piece attributes --- //
    public int posX, posY;
    public int moveNumber = 0;          // how many times this piece has moved (0 = never moved)
    public boolean exists;

    public Piece(char typeCode, int index, char colorCode, boolean placedOnBoard) {
        this(PieceType.fromCode(typeCode), index, PieceColor.fromCode(colorCode), placedOnBoard);
    }

    public Piece(PieceType type, int index, PieceColor color, boolean placedOnBoard) {
        this.color = color;
        this.type = type;
        this.pieceIndex = index;
        this.pieceName = "" + color.code + type.code + index;
        this.exists = placedOnBoard;
        initPos(type, index, color);
    }

    /** True if this piece belongs to the given color. */
    public boolean is(PieceColor other) {
        return color == other;
    }

    /** An identical copy at the same square, used for simulating moves without touching the real board. */
    public Piece duplicate() {
        Piece copy = new Piece(type, pieceIndex, color, exists);
        copy.posX = posX;
        copy.posY = posY;
        copy.moveNumber = moveNumber;
        return copy;
    }

    /** Starting square of a piece (0-based coordinates), from the standard chess setup. */
    private void initPos(PieceType type, int index, PieceColor color) {
        int file;   // 0-based file a..h
        int rank;   // 0-based rank 1..8
        int homeRank = (color == PieceColor.WHITE) ? 0 : 7;

        switch (type) {
            case PAWN:
                file = index - 1;
                rank = (color == PieceColor.WHITE) ? 1 : 6;
                break;
            case ROOK:
                file = (index == 1) ? 0 : 7;
                rank = homeRank;
                break;
            case KNIGHT:
                file = (index == 1) ? 1 : 6;
                rank = homeRank;
                break;
            case BISHOP:
                file = (index == 1) ? 2 : 5;
                rank = homeRank;
                break;
            case KING:
                file = 4;   // e-file, standard starting square
                rank = homeRank;
                break;
            case QUEEN:
                file = 3;   // d-file, standard starting square
                rank = homeRank;
                break;
            default:
                throw new IllegalArgumentException("Unknown piece type: " + type);
        }

        posX = file;
        posY = rank;
    }
}

//Piece is defined
//Piece is given a colour
//Piece is placed on the board
