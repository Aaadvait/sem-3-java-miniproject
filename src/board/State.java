package board;

import move.CastleSide;
import pieces.PieceColor;

/**
 * The mutable state of a game that is not the piece position itself:
 * whose turn it is, castling rights, the en passant square and the
 * check / end-of-game flags.
 *
 * Castling rights are tracked from actual piece movement (they are cleared
 * when a king or rook moves, or when a rook is captured) rather than being
 * derived from coordinates alone.
 */
public class State {

    // --- Whose turn it is --- //
    public PieceColor turn = PieceColor.WHITE;

    // --- Castling rights --- //
    public boolean castleWhiteKingside  = true;
    public boolean castleWhiteQueenside = true;
    public boolean castleBlackKingside  = true;
    public boolean castleBlackQueenside = true;

    // --- En passant --- //
    // The square a pawn of the side to move may capture on right now.
    // Set only immediately after an opponent pawn's two-square initial push; -1 when none.
    public int enPassantX = -1;
    public int enPassantY = -1;

    // --- Check / end flags --- //
    public boolean checkWhite     = false;
    public boolean checkBlack     = false;
    public boolean checkmateWhite = false;
    public boolean checkmateBlack = false;
    public boolean stalemate      = false;

    // --- Move counters --- //
    public int halfmoveClock  = 0;  // plies since the last pawn move or capture (fifty-move rule)
    public int fullmoveNumber = 1;  // starts at 1, increments after Black's move

    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- //

    public boolean inCheck(PieceColor color) {
        return color == PieceColor.WHITE ? checkWhite : checkBlack;
    }

    public boolean isCheckmated(PieceColor color) {
        return color == PieceColor.WHITE ? checkmateWhite : checkmateBlack;
    }

    public boolean castlingRight(PieceColor color, CastleSide side) {
        if (color == PieceColor.WHITE) {
            return side == CastleSide.KING_SIDE ? castleWhiteKingside : castleWhiteQueenside;
        }
        return side == CastleSide.KING_SIDE ? castleBlackKingside : castleBlackQueenside;
    }

    public void clearCastling(PieceColor color, CastleSide side) {
        if (color == PieceColor.WHITE) {
            if (side == CastleSide.KING_SIDE) castleWhiteKingside = false;
            else castleWhiteQueenside = false;
        } else {
            if (side == CastleSide.KING_SIDE) castleBlackKingside = false;
            else castleBlackQueenside = false;
        }
    }

    /** Clears the castling right belonging to the corner square (x, y), if any. */
    public void clearCastlingForSquare(int x, int y) {
        if (y == 0 && x == 0) castleWhiteQueenside = false;
        else if (y == 0 && x == 7) castleWhiteKingside = false;
        else if (y == 7 && x == 0) castleBlackQueenside = false;
        else if (y == 7 && x == 7) castleBlackKingside = false;
    }

    public void clearEnPassant() {
        enPassantX = -1;
        enPassantY = -1;
    }

    /** Full copy of this state (used to save/restore state around a move). */
    public State copy() {
        State s = new State();
        s.turn  = turn;
        s.castleWhiteKingside  = castleWhiteKingside;
        s.castleWhiteQueenside = castleWhiteQueenside;
        s.castleBlackKingside  = castleBlackKingside;
        s.castleBlackQueenside = castleBlackQueenside;
        s.enPassantX = enPassantX;
        s.enPassantY = enPassantY;
        s.checkWhite = checkWhite;
        s.checkBlack = checkBlack;
        s.checkmateWhite = checkmateWhite;
        s.checkmateBlack = checkmateBlack;
        s.stalemate      = stalemate;
        s.halfmoveClock  = halfmoveClock;
        s.fullmoveNumber = fullmoveNumber;
        return s;
    }
}
