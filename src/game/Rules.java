package game;

import board.Board;
import move.CastleSide;
import move.Move;
import pieces.Piece;
import pieces.PieceColor;
import pieces.PieceType;

import java.util.ArrayList;
import java.util.List;

/** Complete legal-move generator for standard chess. Coordinates use a1=(0,0). */
public final class Rules {
    private Rules() { }

    public static List<Move> legalMovesFrom(Board board, int x, int y, PieceColor turn) {
        List<Move> legal = new ArrayList<>();
        if (!inside(x, y)) return legal;
        Piece piece = board.pieceAt(x, y);
        if (piece == null || piece.color != turn) return legal;

        List<int[]> targets = new ArrayList<>();
        switch (piece.type) {
            case PAWN:   pawnTargets(board, piece, targets); break;
            case KNIGHT: knightTargets(x, y, targets); break;
            case BISHOP: slidingTargets(board, x, y, targets, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}}); break;
            case ROOK:   slidingTargets(board, x, y, targets, new int[][]{{1,0},{-1,0},{0,1},{0,-1}}); break;
            case QUEEN:  slidingTargets(board, x, y, targets, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1},{1,0},{-1,0},{0,1},{0,-1}}); break;
            case KING:   kingTargets(board, piece, targets); break;
        }

        for (int[] target : targets) {
            int tx = target[0], ty = target[1];
            Piece captured = board.pieceAt(tx, ty);
            if (captured != null && (captured.color == piece.color || captured.type == PieceType.KING)) continue;
            boolean ep = piece.type == PieceType.PAWN && tx != x && captured == null
                    && board.state.enPassantX == tx && board.state.enPassantY == ty;
            if (ep) {
                captured = board.pieceAt(tx, y);
                if (captured == null || captured.type != PieceType.PAWN || captured.color == piece.color) continue;
            }
            boolean castle = piece.type == PieceType.KING && Math.abs(tx - x) == 2;
            if (castle && !castleAllowed(board, piece, tx)) continue;

            if (piece.type == PieceType.PAWN && ty == (piece.color == PieceColor.WHITE ? 7 : 0)) {
                for (PieceType promotion : PieceType.PROMOTION_TYPES) {
                    Move move = new Move(x, y, tx, ty, piece, captured, promotion, CastleSide.NONE, ep, board.state.fullmoveNumber);
                    if (!willMoveIntoCheck(board, move, piece.color)) legal.add(move);
                }
            } else {
                CastleSide side = castle ? (tx > x ? CastleSide.KING_SIDE : CastleSide.QUEEN_SIDE) : CastleSide.NONE;
                Move move = new Move(x, y, tx, ty, piece, captured, null, side, ep, board.state.fullmoveNumber);
                if (!willMoveIntoCheck(board, move, piece.color)) legal.add(move);
            }
        }
        return legal;
    }

    public static List<Move> legalMoves(Board board, PieceColor side) {
        List<Move> all = new ArrayList<>();
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) all.addAll(legalMovesFrom(board, x, y, side));
        return all;
    }

    public static boolean isInCheck(Board board, PieceColor color) {
        Piece king = board.findKing(color);
        return king != null && isSquareAttacked(board, king.posX, king.posY, color.other());
    }

    public static boolean hasLegalMoves(Board board, PieceColor side) {
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++)
            if (!legalMovesFrom(board, x, y, side).isEmpty()) return true;
        return false;
    }

    private static void pawnTargets(Board board, Piece p, List<int[]> out) {
        int dir = p.color == PieceColor.WHITE ? 1 : -1;
        int x = p.posX, y = p.posY, nextY = y + dir;
        if (inside(x, nextY) && board.pieceAt(x, nextY) == null) {
            out.add(new int[]{x, nextY});
            int startY = p.color == PieceColor.WHITE ? 1 : 6;
            int doubleY = y + 2 * dir;
            if (y == startY && inside(x, doubleY) && board.pieceAt(x, doubleY) == null)
                out.add(new int[]{x, doubleY});
        }
        for (int dx : new int[]{-1, 1}) {
            int tx = x + dx, ty = y + dir;
            if (!inside(tx, ty)) continue;
            Piece target = board.pieceAt(tx, ty);
            if ((target != null && target.color != p.color && target.type != PieceType.KING)
                    || (board.state.enPassantX == tx && board.state.enPassantY == ty)) out.add(new int[]{tx, ty});
        }
    }

    private static void knightTargets(int x, int y, List<int[]> out) {
        int[][] deltas = {{1,2},{2,1},{2,-1},{1,-2},{-1,-2},{-2,-1},{-2,1},{-1,2}};
        for (int[] d : deltas) if (inside(x+d[0], y+d[1])) out.add(new int[]{x+d[0], y+d[1]});
    }

    private static void kingTargets(Board board, Piece king, List<int[]> out) {
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) {
            if (dx == 0 && dy == 0) continue;
            int tx = king.posX + dx, ty = king.posY + dy;
            if (inside(tx, ty)) out.add(new int[]{tx, ty});
        }
        int rank = king.color == PieceColor.WHITE ? 0 : 7;
        if (king.posX != 4 || king.posY != rank || king.moveNumber != 0 || isInCheck(board, king.color)) return;
        if (board.state.castlingRight(king.color, CastleSide.KING_SIDE)
                && clearPath(board, 4, rank, 7) && rookUnmoved(board, 7, rank, king.color)
                && !isSquareAttacked(board, 5, rank, king.color.other())
                && !isSquareAttacked(board, 6, rank, king.color.other())) out.add(new int[]{6, rank});
        if (board.state.castlingRight(king.color, CastleSide.QUEEN_SIDE)
                && clearPath(board, 4, rank, 0) && rookUnmoved(board, 0, rank, king.color)
                && !isSquareAttacked(board, 3, rank, king.color.other())
                && !isSquareAttacked(board, 2, rank, king.color.other())) out.add(new int[]{2, rank});
    }

    private static void slidingTargets(Board board, int x, int y, List<int[]> out, int[][] directions) {
        for (int[] d : directions) {
            int tx = x + d[0], ty = y + d[1];
            while (inside(tx, ty)) {
                out.add(new int[]{tx, ty});
                if (board.pieceAt(tx, ty) != null) break;
                tx += d[0]; ty += d[1];
            }
        }
    }

    private static boolean castleAllowed(Board board, Piece king, int targetX) {
        int rank = king.color == PieceColor.WHITE ? 0 : 7;
        CastleSide side = targetX > king.posX ? CastleSide.KING_SIDE : CastleSide.QUEEN_SIDE;
        int rookX = side == CastleSide.KING_SIDE ? 7 : 0;
        int transitX = side == CastleSide.KING_SIDE ? 5 : 3;
        return king.posX == 4 && king.posY == rank && king.moveNumber == 0
                && board.state.castlingRight(king.color, side)
                && rookUnmoved(board, rookX, rank, king.color)
                && clearPath(board, 4, rank, rookX)
                && !isInCheck(board, king.color)
                && !isSquareAttacked(board, transitX, rank, king.color.other())
                && !isSquareAttacked(board, targetX, rank, king.color.other());
    }

    private static boolean rookUnmoved(Board board, int x, int y, PieceColor color) {
        Piece rook = board.pieceAt(x, y);
        return rook != null && rook.type == PieceType.ROOK && rook.color == color && rook.moveNumber == 0;
    }

    private static boolean clearPath(Board board, int fromX, int rank, int rookX) {
        int step = rookX > fromX ? 1 : -1;
        for (int x = fromX + step; x != rookX; x += step) if (board.pieceAt(x, rank) != null) return false;
        return true;
    }

    private static boolean willMoveIntoCheck(Board board, Move move, PieceColor color) {
        try {
            board.applyMove(move);
            return isInCheck(board, color);
        } finally {
            if (move.previousState != null) board.undoMove(move);
        }
    }

    /** Attack detection is pseudo-legal and intentionally does not call legalMovesFrom (avoids recursion). */
    private static boolean isSquareAttacked(Board board, int x, int y, PieceColor byColor) {
        for (int py = 0; py < 8; py++) for (int px = 0; px < 8; px++) {
            Piece p = board.pieceAt(px, py);
            if (p == null || p.color != byColor) continue;
            int dx = x - px, dy = y - py;
            switch (p.type) {
                case PAWN:
                    int dir = p.color == PieceColor.WHITE ? 1 : -1;
                    if (dy == dir && Math.abs(dx) == 1) return true;
                    break;
                case KNIGHT:
                    if (Math.abs(dx) * Math.abs(dy) == 2) return true;
                    break;
                case KING:
                    if (Math.max(Math.abs(dx), Math.abs(dy)) == 1) return true;
                    break;
                case BISHOP:
                    if (Math.abs(dx) == Math.abs(dy) && dx != 0 && rayClear(board, px, py, x, y)) return true;
                    break;
                case ROOK:
                    if ((dx == 0) != (dy == 0) && rayClear(board, px, py, x, y)) return true;
                    break;
                case QUEEN:
                    if ((Math.abs(dx) == Math.abs(dy) && dx != 0 || (dx == 0) != (dy == 0))
                            && rayClear(board, px, py, x, y)) return true;
                    break;
            }
        }
        return false;
    }

    private static boolean rayClear(Board board, int x1, int y1, int x2, int y2) {
        int sx = Integer.compare(x2, x1), sy = Integer.compare(y2, y1);
        int x = x1 + sx, y = y1 + sy;
        while (x != x2 || y != y2) {
            if (board.pieceAt(x, y) != null) return false;
            x += sx; y += sy;
        }
        return true;
    }

    private static boolean inside(int x, int y) { return x >= 0 && x < 8 && y >= 0 && y < 8; }
}
