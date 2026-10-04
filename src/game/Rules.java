package game;

import board.Board;
import board.State;
import move.CastleSide;
import move.Move;
import pieces.Piece;
import pieces.PieceColor;
import pieces.PieceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Chess move generation and validation.
 *
 * Generation works the classic way:
 *
 *   pseudo-legal moves (geometry + path clear)
 *       -> simulate each move on a private copy of the board
 *       -> locate the moving side's king on that copy
 *       -> check whether that king is attacked
 *       -> reject the move if it is
 *
 * The real board is never modified while testing hypothetical moves.
 */
public final class Rules {

    private static final int[][] KNIGHT_STEPS = {
            { 1, 2}, { 2, 1}, { 2,-1}, { 1,-2},
            {-1,-2}, {-2,-1}, {-2, 1}, {-1, 2},
    };

    private static final int[][] ADJACENT = {
            {-1,-1}, {0,-1}, {1,-1},
            {-1, 0},         {1, 0},
            {-1, 1}, {0, 1}, {1, 1},
    };

    private static final int[][] BISHOP_DIRS = { {1,1}, {1,-1}, {-1,1}, {-1,-1} };
    private static final int[][] ROOK_DIRS   = { {1,0}, {-1,0}, {0,1}, {0,-1} };

    private Rules() { }

    // --- --- --- --- --- PUBLIC API --- --- --- --- --- //

    /** All legal moves the given color can make right now. */
    public static List<Move> legalMoves(Board board, PieceColor color) {
        List<Move> moves = new ArrayList<>();
        for (Move m : pseudoMoves(board, color)) {
            if (!leavesKingInCheck(board, m, color)) moves.add(m);
        }
        return moves;
    }

    /** All legal moves for the piece on (x, y); empty if the square holds no piece of that color. */
    public static List<Move> legalMovesFrom(Board board, int x, int y, PieceColor color) {
        List<Move> moves = new ArrayList<>();
        Piece p = board.pieceAt(x, y);
        if (p == null || p.color != color) return moves;
        for (Move m : pseudoMovesFrom(board, p)) {
            if (!leavesKingInCheck(board, m, color)) moves.add(m);
        }
        return moves;
    }

    /** True if the color has at least one legal move. */
    public static boolean hasLegalMoves(Board board, PieceColor color) {
        for (Move m : pseudoMoves(board, color)) {
            if (!leavesKingInCheck(board, m, color)) return true;
        }
        return false;
    }

    /** True if the given color's king is currently attacked by the opponent. */
    public static boolean isInCheck(Board board, PieceColor color) {
        Piece king = board.findKing(color);
        if (king == null) return false;
        return isSquareAttacked(board, king.posX, king.posY, color.other());
    }

    /**
     * True if a square is attacked by at least one piece of the given color.
     * Uses attack patterns (pawns attack diagonally forward, kings attack one
     * square, ...), not full move generation.
     */
    public static boolean isSquareAttacked(Board board, int x, int y, PieceColor by) {
        return isSquareAttacked(board.board, x, y, by);
    }

    /** Same, but for a simulated (copied) grid. */
    private static boolean isSquareAttacked(Piece[][] grid, int x, int y, PieceColor by) {
        // Pawns: a pawn of color `by` on (x-1, y-dir) / (x+1, y-dir) attacks (x, y).
        int dir = (by == PieceColor.WHITE) ? 1 : -1;
        for (int dx = -1; dx <= 1; dx += 2) {
            Piece p = pieceAt(grid, x + dx, y - dir);
            if (p != null && p.color == by && p.type == PieceType.PAWN) return true;
        }

        // Knights.
        for (int[] d : KNIGHT_STEPS) {
            Piece p = pieceAt(grid, x + d[0], y + d[1]);
            if (p != null && p.color == by && p.type == PieceType.KNIGHT) return true;
        }

        // King.
        for (int[] d : ADJACENT) {
            Piece p = pieceAt(grid, x + d[0], y + d[1]);
            if (p != null && p.color == by && p.type == PieceType.KING) return true;
        }

        // Sliding pieces: bishops/queens on diagonals, rooks/queens on files/ranks.
        if (rayAttacked(grid, x, y, by, BISHOP_DIRS, true)) return true;
        return rayAttacked(grid, x, y, by, ROOK_DIRS, false);
    }

    // --- --- --- --- --- PSEUDO-LEGAL GENERATION --- --- --- --- --- //

    private static List<Move> pseudoMoves(Board board, PieceColor color) {
        List<Move> moves = new ArrayList<>();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece p = board.board[y][x];
                if (p != null && p.color == color) {
                    pseudoMovesFrom(board, p, moves);
                }
            }
        }
        addCastlingMoves(board, color, moves);
        return moves;
    }

    private static List<Move> pseudoMovesFrom(Board board, Piece p) {
        List<Move> moves = new ArrayList<>();
        pseudoMovesFrom(board, p, moves);
        if (p.type == PieceType.KING) {
            // Castling is a king move, so only the king needs it.
            addCastlingMoves(board, p.color, moves);
        }
        return moves;
    }

    private static void pseudoMovesFrom(Board board, Piece p, List<Move> moves) {
        switch (p.type) {
            case PAWN:  addPawnMoves(board, p, moves); break;
            case KNIGHT: addStepMoves(board, p, KNIGHT_STEPS, moves); break;
            case KING:  addStepMoves(board, p, ADJACENT, moves); break;
            case BISHOP: addSlideMoves(board, p, BISHOP_DIRS, moves); break;
            case ROOK:  addSlideMoves(board, p, ROOK_DIRS, moves); break;
            case QUEEN:
                addSlideMoves(board, p, BISHOP_DIRS, moves);
                addSlideMoves(board, p, ROOK_DIRS, moves);
                break;
        }
    }

    private static void addPawnMoves(Board board, Piece p, List<Move> moves) {
        int x = p.posX, y = p.posY;
        int dir = (p.color == PieceColor.WHITE) ? 1 : -1;
        int startRank = (p.color == PieceColor.WHITE) ? 1 : 6;
        int promotionRank = (p.color == PieceColor.WHITE) ? 7 : 0;
        int moveNumber = board.state.fullmoveNumber;

        // One square forward, if empty.
        int one = y + dir;
        if (board.inside(x, one) && board.pieceAt(x, one) == null) {
            addPawnDestination(board, p, x, one, null, promotionRank, moves);
            // Two squares forward from the starting rank, if both squares are empty.
            if (y == startRank && p.moveNumber == 0) {
                int two = y + 2 * dir;
                if (board.pieceAt(x, two) == null) {
                    moves.add(new Move(x, y, x, two, p, null, null, CastleSide.NONE, false, moveNumber));
                }
            }
        }

        // Diagonal captures (and en passant).
        for (int dx = -1; dx <= 1; dx += 2) {
            int tx = x + dx;
            if (!board.inside(tx, one)) continue;

            Piece target = board.pieceAt(tx, one);
            if (target != null && target.color != p.color) {
                addPawnDestination(board, p, tx, one, target, promotionRank, moves);
            } else if (target == null
                    && board.state.enPassantX == tx && board.state.enPassantY == one) {
                Piece victim = board.pieceAt(tx, y);
                if (victim != null && victim.type == PieceType.PAWN && victim.color != p.color) {
                    moves.add(new Move(x, y, tx, one, p, victim, null, CastleSide.NONE, true, moveNumber));
                }
            }
        }
    }

    /** Adds a forward/capturing pawn move, expanded into promotion moves on the last rank. */
    private static void addPawnDestination(Board board, Piece p, int tx, int ty,
                                           Piece target, int promotionRank, List<Move> moves) {
        int moveNumber = board.state.fullmoveNumber;
        if (ty == promotionRank) {
            for (PieceType promo : PieceType.PROMOTION_TYPES) {
                moves.add(new Move(p.posX, p.posY, tx, ty, p, target, promo, CastleSide.NONE, false, moveNumber));
            }
        } else {
            moves.add(new Move(p.posX, p.posY, tx, ty, p, target, null, CastleSide.NONE, false, moveNumber));
        }
    }

    private static void addStepMoves(Board board, Piece p, int[][] steps, List<Move> moves) {
        int moveNumber = board.state.fullmoveNumber;
        for (int[] d : steps) {
            int tx = p.posX + d[0], ty = p.posY + d[1];
            if (!board.inside(tx, ty)) continue;
            Piece target = board.pieceAt(tx, ty);
            if (target == null || target.color != p.color) {
                moves.add(new Move(p.posX, p.posY, tx, ty, p, target, null, CastleSide.NONE, false, moveNumber));
            }
        }
    }

    private static void addSlideMoves(Board board, Piece p, int[][] dirs, List<Move> moves) {
        int moveNumber = board.state.fullmoveNumber;
        for (int[] d : dirs) {
            int tx = p.posX + d[0], ty = p.posY + d[1];
            while (board.inside(tx, ty)) {
                Piece target = board.pieceAt(tx, ty);
                if (target == null) {
                    moves.add(new Move(p.posX, p.posY, tx, ty, p, null, null, CastleSide.NONE, false, moveNumber));
                } else {
                    if (target.color != p.color) {
                        moves.add(new Move(p.posX, p.posY, tx, ty, p, target, null, CastleSide.NONE, false, moveNumber));
                    }
                    break;
                }
                tx += d[0];
                ty += d[1];
            }
        }
    }

    /**
     * Castling, subject to all standard restrictions: rights intact, king and
     * rook unmoved, squares between empty, king not in check, and the king
     * not passing through or landing on an attacked square.
     */
    private static void addCastlingMoves(Board board, PieceColor color, List<Move> moves) {
        State s = board.state;
        int rank = (color == PieceColor.WHITE) ? 0 : 7;
        int moveNumber = s.fullmoveNumber;

        if (isInCheck(board, color)) return;                       // not while in check

        Piece king = board.pieceAt(4, rank);
        if (king == null || king.type != PieceType.KING || king.color != color) return;
        if (king.moveNumber != 0) return;                          // king has moved before
        if (!s.castlingRight(color, CastleSide.KING_SIDE)
                && !s.castlingRight(color, CastleSide.QUEEN_SIDE)) return;

        PieceColor enemy = color.other();

        // Kingside: king e1 -> g1, rook h1 -> f1 (squares f and g must be free and safe).
        if (s.castlingRight(color, CastleSide.KING_SIDE)) {
            Piece rook = board.pieceAt(7, rank);
            if (rook != null && rook.type == PieceType.ROOK && rook.color == color && rook.moveNumber == 0
                    && board.pieceAt(5, rank) == null
                    && board.pieceAt(6, rank) == null
                    && !isSquareAttacked(board, 5, rank, enemy)
                    && !isSquareAttacked(board, 6, rank, enemy)) {
                moves.add(new Move(4, rank, 6, rank, king, null, null, CastleSide.KING_SIDE, false, moveNumber));
            }
        }

        // Queenside: king e1 -> c1, rook a1 -> d1 (squares b, c, d must be free; c and d safe).
        if (s.castlingRight(color, CastleSide.QUEEN_SIDE)) {
            Piece rook = board.pieceAt(0, rank);
            if (rook != null && rook.type == PieceType.ROOK && rook.color == color && rook.moveNumber == 0
                    && board.pieceAt(1, rank) == null
                    && board.pieceAt(2, rank) == null
                    && board.pieceAt(3, rank) == null
                    && !isSquareAttacked(board, 2, rank, enemy)
                    && !isSquareAttacked(board, 3, rank, enemy)) {
                moves.add(new Move(4, rank, 2, rank, king, null, null, CastleSide.QUEEN_SIDE, false, moveNumber));
            }
        }
    }

    // --- --- --- --- --- SIMULATION --- --- --- --- --- //

    /** True if playing this move would leave the mover's own king in check. */
    private static boolean leavesKingInCheck(Board board, Move m, PieceColor color) {
        Piece[][] sim = simulate(board, m);
        Piece king = findKing(sim, color);
        if (king == null) return false;    // no king on the board (test position) -> nothing to protect
        return isSquareAttacked(sim, king.posX, king.posY, color.other());
    }

    /**
     * Plays the move on a deep copy of the piece array and returns the copy.
     * The real board and its Pieces are never touched.
     */
    private static Piece[][] simulate(Board board, Move m) {
        Piece[][] sim = new Piece[8][8];
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece p = board.board[y][x];
                sim[y][x] = (p == null) ? null : p.duplicate();
            }
        }

        Piece moving = sim[m.fromY][m.fromX];
        sim[m.fromY][m.fromX] = null;
        if (m.enPassant) {
            sim[m.capturedY][m.capturedX] = null;
        }

        if (m.promotion != null) {
            Piece promoted = new Piece(m.promotion, moving.pieceIndex, moving.color, true);
            promoted.posX = m.toX;
            promoted.posY = m.toY;
            promoted.moveNumber = moving.moveNumber + 1;
            sim[m.toY][m.toX] = promoted;
        } else {
            moving.posX = m.toX;
            moving.posY = m.toY;
            sim[m.toY][m.toX] = moving;
        }

        if (m.castle != CastleSide.NONE) {
            int rank = m.fromY;
            if (m.castle == CastleSide.KING_SIDE) {
                Piece rook = sim[rank][7];
                sim[rank][7] = null;
                sim[rank][5] = rook;
                rook.posX = 5;
            } else {
                Piece rook = sim[rank][0];
                sim[rank][0] = null;
                sim[rank][3] = rook;
                rook.posX = 3;
            }
        }

        return sim;
    }

    // --- --- --- --- --- HELPERS --- --- --- --- --- //

    private static Piece findKing(Piece[][] grid, PieceColor color) {
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece p = grid[y][x];
                if (p != null && p.color == color && p.type == PieceType.KING) return p;
            }
        }
        return null;
    }

    private static Piece pieceAt(Piece[][] grid, int x, int y) {
        if (x < 0 || x > 7 || y < 0 || y > 7) return null;
        return grid[y][x];
    }

    /** True if a rook- or bishop-style ray from (x, y) hits a matching attacker. */
    private static boolean rayAttacked(Piece[][] grid, int x, int y, PieceColor by,
                                       int[][] dirs, boolean diagonal) {
        for (int[] d : dirs) {
            int tx = x + d[0], ty = y + d[1];
            while (tx >= 0 && tx < 8 && ty >= 0 && ty < 8) {
                Piece p = grid[ty][tx];
                if (p != null) {
                    if (p.color == by) {
                        if (diagonal) {
                            if (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN) return true;
                        } else {
                            if (p.type == PieceType.ROOK || p.type == PieceType.QUEEN) return true;
                        }
                    }
                    break;  // the first piece on the ray blocks the rest
                }
                tx += d[0];
                ty += d[1];
            }
        }
        return false;
    }
}
