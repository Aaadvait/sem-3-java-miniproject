package move;

/** Which side of the king a castling move goes to. */
public enum CastleSide {
    KING_SIDE,      // O-O   (king two squares toward the h-file rook)
    QUEEN_SIDE,     // O-O-O (king two squares toward the a-file rook)
    NONE;           // not a castling move
}
