package persistence;

import board.Board;
import board.State;
import move.CastleSide;
import move.Move;
import pieces.Piece;
import pieces.PieceColor;
import pieces.PieceType;

/**
 * Converts a board position to and from Forsyth–Edwards Notation (FEN).
 *
 * Example start position FEN:
 *   rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
 *
 * Coordinate convention (same as the rest of the project):
 *   x = file 0..7 (a..h), y = rank 0..7 (rank 1 at y=0, White's side).
 *   FEN rank 8 (y=7, Black's back rank) appears first in the string.
 */
public final class FENHandler {

    private FENHandler() { }

    // --- --- --- --- --- EXPORT --- --- --- --- --- //

    /**
     * Generates a FEN string for the current board position.
     *
     * @param board the position to export
     * @return valid FEN string
     */
    public static String toFEN(Board board) {
        StringBuilder sb = new StringBuilder(80);

        // 1. Piece placement (rank 8 first, rank 1 last).
        for (int y = 7; y >= 0; y--) {
            int empty = 0;
            for (int x = 0; x < 8; x++) {
                Piece p = board.board[y][x];
                if (p == null) {
                    empty++;
                } else {
                    if (empty > 0) { sb.append(empty); empty = 0; }
                    sb.append(fenChar(p));
                }
            }
            if (empty > 0) sb.append(empty);
            if (y > 0) sb.append('/');
        }

        State s = board.state;

        // 2. Active color.
        sb.append(' ').append(s.turn == PieceColor.WHITE ? 'w' : 'b');

        // 3. Castling availability.
        sb.append(' ');
        StringBuilder castle = new StringBuilder();
        if (s.castleWhiteKingside)  castle.append('K');
        if (s.castleWhiteQueenside) castle.append('Q');
        if (s.castleBlackKingside)  castle.append('k');
        if (s.castleBlackQueenside) castle.append('q');
        sb.append(castle.length() == 0 ? "-" : castle);

        // 4. En passant target square.
        sb.append(' ');
        if (s.enPassantX >= 0) {
            sb.append((char)('a' + s.enPassantX)).append(s.enPassantY + 1);
        } else {
            sb.append('-');
        }

        // 5. Halfmove clock.
        sb.append(' ').append(s.halfmoveClock);

        // 6. Fullmove number.
        sb.append(' ').append(s.fullmoveNumber);

        return sb.toString();
    }

    // --- --- --- --- --- IMPORT --- --- --- --- --- //

    /**
     * Loads a FEN string into a board, overwriting all current pieces and state.
     *
     * Only the standard 6-field FEN is supported. The method is lenient about
     * extra whitespace but strict about illegal piece characters.
     *
     * @param board the board to update (all previous pieces are erased)
     * @param fen   a valid FEN string
     * @throws IllegalArgumentException if the FEN is malformed
     */
    public static void fromFEN(Board board, String fen) {
        String[] fields = fen.trim().split("\\s+");
        if (fields.length < 4) {
            throw new IllegalArgumentException("FEN must have at least 4 fields: " + fen);
        }

        // Erase everything.
        board.clear();

        // --- Field 1: piece placement ---
        String[] ranks = fields[0].split("/");
        if (ranks.length != 8) {
            throw new IllegalArgumentException("FEN piece placement must have 8 ranks: " + fields[0]);
        }
        for (int rankIdx = 0; rankIdx < 8; rankIdx++) {
            int y = 7 - rankIdx;   // FEN rank 8 (index 0) = engine y=7
            int x = 0;
            for (char c : ranks[rankIdx].toCharArray()) {
                if (Character.isDigit(c)) {
                    x += c - '0';
                } else {
                    PieceColor color = Character.isUpperCase(c) ? PieceColor.WHITE : PieceColor.BLACK;
                    PieceType  type  = typeFromFenChar(Character.toUpperCase(c));
                    // Use a synthetic pieceIndex=1; won't collide in practice for positions loaded from FEN.
                    Piece p = new Piece(type, nextIndex(board, color, type), color, true);
                    p.posX = x;
                    p.posY = y;
                    p.moveNumber = 1;          // mark as "already moved" so castling rights come from FEN
                    board.board[y][x] = p;
                    x++;
                }
            }
        }

        // --- Field 2: active color ---
        State s = board.state;
        s.turn = fields[1].equals("b") ? PieceColor.BLACK : PieceColor.WHITE;

        // --- Field 3: castling ---
        s.castleWhiteKingside  = fields[2].contains("K");
        s.castleWhiteQueenside = fields[2].contains("Q");
        s.castleBlackKingside  = fields[2].contains("k");
        s.castleBlackQueenside = fields[2].contains("q");

        // Mark the kings as un-moved only when castling rights are still available.
        // (Board.applyMove clears rights when a king/rook moves, so we need to
        //  reset moveNumber for pieces that are still on their home squares.)
        resetMoveNumbers(board, s);

        // --- Field 4: en passant ---
        if (!fields[3].equals("-") && fields[3].length() >= 2) {
            s.enPassantX = fields[3].charAt(0) - 'a';
            s.enPassantY = fields[3].charAt(1) - '1';
        }

        // --- Field 5: halfmove clock (optional) ---
        if (fields.length >= 5) {
            try { s.halfmoveClock = Integer.parseInt(fields[4]); }
            catch (NumberFormatException ignored) { s.halfmoveClock = 0; }
        }

        // --- Field 6: fullmove number (optional) ---
        if (fields.length >= 6) {
            try { s.fullmoveNumber = Integer.parseInt(fields[5]); }
            catch (NumberFormatException ignored) { s.fullmoveNumber = 1; }
        }
    }

    // --- --- --- --- --- HELPERS --- --- --- --- --- //

    private static char fenChar(Piece p) {
        char c;
        switch (p.type) {
            case PAWN:   c = 'P'; break;
            case KNIGHT: c = 'N'; break;
            case BISHOP: c = 'B'; break;
            case ROOK:   c = 'R'; break;
            case QUEEN:  c = 'Q'; break;
            case KING:   c = 'K'; break;
            default: throw new IllegalStateException("Unknown piece type: " + p.type);
        }
        return p.color == PieceColor.WHITE ? c : Character.toLowerCase(c);
    }

    private static PieceType typeFromFenChar(char upper) {
        switch (upper) {
            case 'P': return PieceType.PAWN;
            case 'N': return PieceType.KNIGHT;
            case 'B': return PieceType.BISHOP;
            case 'R': return PieceType.ROOK;
            case 'Q': return PieceType.QUEEN;
            case 'K': return PieceType.KING;
            default:  throw new IllegalArgumentException("Unknown FEN piece char: " + upper);
        }
    }

    /** Assigns a unique 1-based index to each piece as we scan the board. */
    private static int nextIndex(Board board, PieceColor color, PieceType type) {
        int count = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece p = board.board[y][x];
                if (p != null && p.color == color && p.type == type) count++;
            }
        }
        return count + 1;
    }

    /**
     * Resets moveNumber=0 for kings and rooks that are still on their starting
     * squares AND still have castling rights, so castling logic keeps working.
     */
    private static void resetMoveNumbers(Board board, State s) {
        // White king and rooks.
        Piece wk = board.board[0][4];
        if (wk != null && wk.color == PieceColor.WHITE && wk.type == PieceType.KING) wk.moveNumber = 0;

        if (s.castleWhiteKingside) {
            Piece r = board.board[0][7];
            if (r != null && r.color == PieceColor.WHITE && r.type == PieceType.ROOK) r.moveNumber = 0;
        }
        if (s.castleWhiteQueenside) {
            Piece r = board.board[0][0];
            if (r != null && r.color == PieceColor.WHITE && r.type == PieceType.ROOK) r.moveNumber = 0;
        }

        // Black king and rooks.
        Piece bk = board.board[7][4];
        if (bk != null && bk.color == PieceColor.BLACK && bk.type == PieceType.KING) bk.moveNumber = 0;

        if (s.castleBlackKingside) {
            Piece r = board.board[7][7];
            if (r != null && r.color == PieceColor.BLACK && r.type == PieceType.ROOK) r.moveNumber = 0;
        }
        if (s.castleBlackQueenside) {
            Piece r = board.board[7][0];
            if (r != null && r.color == PieceColor.BLACK && r.type == PieceType.ROOK) r.moveNumber = 0;
        }
    }
}
