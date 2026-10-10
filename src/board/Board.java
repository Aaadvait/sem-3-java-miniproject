package board;

import move.CastleSide;
import move.Move;
import pieces.Piece;
import pieces.PieceColor;
import pieces.PieceType;

/**
 * The authoritative chess position.
 *
 * Everything that reads the position (rules, GUI) reads it from here;
 * everything that changes the position goes through applyMove/undoMove so the
 * Piece[][] array, the Piece coordinates and the State can never disagree.
 *
 * Coordinates: board[y][x] with x = file 0..7 (a..h from White's side) and
 * y = rank 0..7 (rank 1 at index 0, so White starts at y = 0..1).
 */
public class Board {

    // --- Generating Board --- //
    public String[] pieceName = {
        "WP1", "WP2", "WP3", "WP4", "WP5", "WP6", "WP7", "WP8",
        "WK1", "WQ1", "WB1", "WB2", "WH1", "WH2", "WR1", "WR2",
        "BP1", "BP2", "BP3", "BP4", "BP5", "BP6", "BP7", "BP8",
        "BK1", "BQ1", "BB1", "BB2", "BH1", "BH2", "BR1", "BR2",
    };

    public Piece[][] board = new Piece[8][8];   // Board Array
    public State state = new State();           // Board State

    public Board(String savedGame) {            // Board Piece
        System.out.println("SYS:    Generating Board...");
        Piece P;
        int index;
        char type, color;
        for (int i = 0; i < 32; i++) {
            color = pieceName[i].charAt(0);
            type  = pieceName[i].charAt(1);
            index = pieceName[i].charAt(2) - '0';
            P     = new Piece(type, index, color, true);
            board[P.posY][P.posX] = P;
        }
        // --- Getting Board Data from file -- //
        {
            // savedGame is reserved for loading a saved position; the standard
            // starting position is used for now.
        }
    }

    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- //

    // --- Queries --- //

    public boolean inside(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    /** The piece standing on (x, y), or null if the square is empty (or off the board). */
    public Piece pieceAt(int x, int y) {
        if (!inside(x, y)) return null;
        return board[y][x];
    }

    /** The king of the given color, or null if it is not on the board. */
    public Piece findKing(PieceColor color) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece p = board[y][x];
                if (p != null && p.color == color && p.type == PieceType.KING) return p;
            }
        }
        return null;
    }

    /** A string that identifies the current position (for repetition detection). */
    public String positionKey() {
        StringBuilder sb = new StringBuilder(80);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece p = board[y][x];
                if (p == null) {
                    sb.append('.');
                } else {
                    char c = p.type.code;
                    sb.append(p.color == PieceColor.WHITE ? c : Character.toLowerCase(c));
                }
            }
            sb.append('/');
        }
        sb.append(' ').append(state.turn.code);
        sb.append(' ');
        sb.append(state.castleWhiteKingside  ? 'K' : '-');
        sb.append(state.castleWhiteQueenside ? 'Q' : '-');
        sb.append(state.castleBlackKingside  ? 'k' : '-');
        sb.append(state.castleBlackQueenside ? 'q' : '-');
        sb.append(' ');
        sb.append(state.enPassantX < 0 ? "-" : Move.squareName(state.enPassantX, state.enPassantY));
        return sb.toString();
    }

    // --- Moving --- //

    /**
     * Applies a move to this board and updates the State (turn, castling
     * rights, en passant square, counters). Saves the previous State in the
     * Move so undoMove can restore it.
     */
    public void applyMove(Move m) {
        m.previousState = state.copy();

        Piece moving = board[m.fromY][m.fromX];
        if (moving == null) {
            throw new IllegalStateException("Move has no piece at " + Move.squareName(m.fromX, m.fromY));
        }

        // Remove the captured piece first (for en passant it is NOT on the destination square).
        if (m.captured != null) {
            board[m.capturedY][m.capturedX] = null;
        }

        // Place the moving piece (or its promotion replacement).
        board[m.fromY][m.fromX] = null;
        Piece placed = moving;
        if (m.promotion != null) {
            placed = new Piece(m.promotion, moving.pieceIndex, moving.color, true);
            placed.moveNumber = moving.moveNumber + 1;
        }
        board[m.toY][m.toX] = placed;
        moving.posX = m.toX;
        moving.posY = m.toY;
        moving.moveNumber++;

        // Castling also shifts the rook.
        if (m.castle != CastleSide.NONE) {
            int rank = m.fromY;
            if (m.castle == CastleSide.KING_SIDE) {
                Piece rook = board[rank][7];
                board[rank][7] = null;
                board[rank][5] = rook;
                rook.posX = 5;
                rook.moveNumber++;
            } else {
                Piece rook = board[rank][0];
                board[rank][0] = null;
                board[rank][3] = rook;
                rook.posX = 3;
                rook.moveNumber++;
            }
        }

        updateStateAfter(m, moving);
    }

    /** Restores the board to exactly the position before this move was applied. */
    public void undoMove(Move m) {
        if (m.previousState == null) {
            throw new IllegalStateException("Cannot undo a move that was never applied");
        }

        // Remove whatever now stands on the destination and restore the mover
        // (for a promotion that is the original pawn, not the promoted piece).
        board[m.toY][m.toX] = null;
        Piece moving = m.piece;
        board[m.fromY][m.fromX] = moving;
        moving.posX = m.fromX;
        moving.posY = m.fromY;
        moving.moveNumber--;

        if (m.captured != null) {
            board[m.capturedY][m.capturedX] = m.captured;
        }

        if (m.castle != CastleSide.NONE) {
            int rank = m.fromY;
            if (m.castle == CastleSide.KING_SIDE) {
                Piece rook = board[rank][5];
                board[rank][5] = null;
                board[rank][7] = rook;
                rook.posX = 7;
                rook.moveNumber--;
            } else {
                Piece rook = board[rank][3];
                board[rank][3] = null;
                board[rank][0] = rook;
                rook.posX = 0;
                rook.moveNumber--;
            }
        }

        state.restoreFrom(m.previousState);
        m.previousState = null;
    }

    /** Derives the new State values from the move that was just applied. */
    private void updateStateAfter(Move m, Piece moving) {
        boolean wasPawnMove = moving.type == PieceType.PAWN;
        boolean wasCapture  = m.captured != null;

        // Castling rights follow actual piece movement.
        if (moving.type == PieceType.KING) {
            state.clearCastling(moving.color, CastleSide.KING_SIDE);
            state.clearCastling(moving.color, CastleSide.QUEEN_SIDE);
        }
        if (moving.type == PieceType.ROOK) {
            state.clearCastlingForSquare(m.fromX, m.fromY);
        }
        if (wasCapture) {
            state.clearCastlingForSquare(m.capturedX, m.capturedY);
        }

        // En passant target: only right after a two-square initial pawn push,
        // and only on the square the pawn skipped.
        if (wasPawnMove && Math.abs(m.toY - m.fromY) == 2) {
            state.enPassantX = m.toX;
            state.enPassantY = (m.fromY + m.toY) / 2;
        } else {
            state.clearEnPassant();
        }

        // Counters.
        state.halfmoveClock = (wasPawnMove || wasCapture) ? 0 : state.halfmoveClock + 1;
        if (moving.color == PieceColor.BLACK) {
            state.fullmoveNumber++;
        }

        // Turn passes to the opponent.
        state.turn = moving.color.other();
    }

    // --- TEST FUNCTIONS --- //

    /** Empties the board and resets the state (used by tests to build positions). */
    public void clear() {
        board = new Piece[8][8];
        state = new State();
    }

    public void displayBoard() {
        for (int row = 7; row >= 0; row--) {
            System.out.print((row + 1) + "   ");
            for (int col = 0; col < 8; col++) {
                if (board[row][col] == null) System.out.print("  -  ");
                else System.out.print(" " + board[row][col].pieceName + " ");
            }
            System.out.println("\n");
        }
        System.out.println("      a    b    c    d    e    f    g    h");
    }
}

/*
        1   2   3   4   5   6   7   8

    1   wr1 wh1 wb1 wq1 wk1 wb2 wh2 wr2

    2   wp1 wp2 wp3 wp4 wp5 wp6 wp7 wp8

    3

    4

    5

    6

    7   bp1 bp2 bp3 bp4 bp5 bp6 bp7 bp8

    8   br1 bh1 bb1 bq1 bk1 bb2 bh2 br2
*/
