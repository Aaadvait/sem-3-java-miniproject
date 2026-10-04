package persistence;

import board.Board;
import board.State;
import game.Game;
import move.CastleSide;
import move.Move;
import move.MoveHistory;
import pieces.Piece;
import pieces.PieceColor;
import pieces.PieceType;
import game.Rules;

import java.util.ArrayList;
import java.util.List;

/**
 * PGN (Portable Game Notation) import and export.
 *
 * Export produces a standard seven-tag roster followed by the move text
 * in SAN (Standard Algebraic Notation). Import parses SAN moves and replays
 * them on a board so the full game history is reconstructed.
 *
 * Supported tag roster on export:
 *   Event, Site, Date, Round, White, Black, Result
 *
 * Parser limitations (sufficient for all legal games):
 *   - Accepts SAN with check/mate symbols (+ #) and annotation symbols (! ? !!)
 *   - Handles castling (O-O / O-O-O), promotions (=Q), en passant
 *   - Does NOT parse variations ({ ... }) or NAG symbols ($n) in import
 */
public final class PGNHandler {

    private PGNHandler() { }

    // --- --- --- --- --- EXPORT --- --- --- --- --- //

    /**
     * Exports the game to a complete PGN string.
     *
     * @param history  all moves played, in order
     * @param board    the current position (used only for its state counters)
     * @param result   e.g. "1-0", "0-1", "1/2-1/2", "*"
     * @param white    White player's name
     * @param black    Black player's name
     */
    public static String export(List<Move> history, Board board, String result,
                                String white, String black) {
        StringBuilder sb = new StringBuilder();

        // --- Seven-tag roster ---
        java.time.LocalDate today = java.time.LocalDate.now();
        sb.append("[Event \"Chess Game\"]\n");
        sb.append("[Site \"Local\"]\n");
        sb.append(String.format("[Date \"%d.%02d.%02d\"]\n",
                today.getYear(), today.getMonthValue(), today.getDayOfMonth()));
        sb.append("[Round \"?\"]\n");
        sb.append("[White \"").append(white).append("\"]\n");
        sb.append("[Black \"").append(black).append("\"]\n");
        sb.append("[Result \"").append(result).append("\"]\n");
        sb.append("\n");

        // --- Move text ---
        // We need to replay from the start to compute SAN for each move.
        Board replay = new Board(null);

        int lineLen = 0;
        for (int i = 0; i < history.size(); i++) {
            Move m = history.get(i);
            if (i % 2 == 0) {
                String prefix = (i / 2 + 1) + ". ";
                sb.append(prefix);
                lineLen += prefix.length();
            }
            String san = toSAN(m, replay);
            replay.applyMove(m);

            // Check / checkmate suffix.
            State s = replay.state;
            PieceColor justMoved = m.piece.color;
            PieceColor opponent  = justMoved.other();
            boolean inCheck = Rules.isInCheck(replay, opponent);
            boolean hasMoves = Rules.hasLegalMoves(replay, opponent);
            if (inCheck) {
                san += hasMoves ? "+" : "#";
            }

            sb.append(san).append(' ');
            lineLen += san.length() + 1;
            if (lineLen > 75) { sb.append('\n'); lineLen = 0; }
        }

        sb.append(result).append('\n');
        return sb.toString();
    }

    // --- --- --- --- --- IMPORT --- --- --- --- --- //

    /**
     * Parses a PGN string and returns the moves in order.
     * The caller is responsible for replaying them on a fresh board.
     *
     * @param pgn the PGN text to parse
     * @return ordered list of moves, ready to be applied to a new Board
     * @throws IllegalArgumentException if a move cannot be parsed or is illegal
     */
    public static List<Move> importPGN(String pgn) {
        Board board = new Board(null);
        List<Move> result = new ArrayList<>();

        // Strip tags and comments.
        String moveText = stripTagsAndComments(pgn);

        // Tokenise: split on whitespace, remove move numbers (digits followed by '.' or "...").
        String[] tokens = moveText.trim().split("\\s+");
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            // Skip move numbers (e.g. "1.", "1...", "12.")
            if (token.matches("\\d+\\.+")) continue;
            // Skip result tokens.
            if (token.equals("1-0") || token.equals("0-1") ||
                token.equals("1/2-1/2") || token.equals("*")) continue;

            // Strip annotation symbols: !, ?, +, #
            String san = token.replaceAll("[+#!?]+$", "");
            if (san.isEmpty()) continue;

            Move m = parseSANMove(san, board);
            if (m == null) {
                throw new IllegalArgumentException("Cannot parse SAN move: '" + token
                        + "' at move " + (result.size() + 1));
            }
            board.applyMove(m);
            result.add(m);
        }
        return result;
    }

    // --- --- --- --- --- SAN GENERATION --- --- --- --- --- //

    /**
     * Returns the SAN for a move given the board position BEFORE the move is applied.
     */
    private static String toSAN(Move m, Board board) {
        if (m.castle == CastleSide.KING_SIDE)  return "O-O";
        if (m.castle == CastleSide.QUEEN_SIDE) return "O-O-O";

        StringBuilder sb = new StringBuilder();
        PieceType type = m.piece.type;
        PieceColor color = m.piece.color;

        // Piece letter (omit for pawns).
        if (type != PieceType.PAWN) {
            sb.append(sanPieceLetter(type));
        }

        // Disambiguation if needed.
        String disambig = disambiguate(m, board, color, type);
        sb.append(disambig);

        // Capture indicator.
        if (m.captured != null) {
            if (type == PieceType.PAWN) sb.append((char)('a' + m.fromX));
            sb.append('x');
        }

        // Destination square.
        sb.append((char)('a' + m.toX)).append(m.toY + 1);

        // Promotion.
        if (m.promotion != null) {
            sb.append('=').append(sanPieceLetter(m.promotion));
        }

        return sb.toString();
    }

    private static char sanPieceLetter(PieceType type) {
        switch (type) {
            case KNIGHT: return 'N';
            case BISHOP: return 'B';
            case ROOK:   return 'R';
            case QUEEN:  return 'Q';
            case KING:   return 'K';
            default:     return 'P';
        }
    }

    /** Returns file, rank, or full square disambiguation string (may be empty). */
    private static String disambiguate(Move m, Board board, PieceColor color, PieceType type) {
        if (type == PieceType.PAWN || type == PieceType.KING) return "";

        List<Move> all = Rules.legalMoves(board, color);
        List<Move> same = new ArrayList<>();
        for (Move candidate : all) {
            if (candidate.piece.type == type
                    && candidate.toX == m.toX && candidate.toY == m.toY
                    && candidate.fromX != m.fromX | candidate.fromY != m.fromY) {
                same.add(candidate);
            }
        }
        if (same.isEmpty()) return "";

        // Try file disambiguation first.
        boolean fileUnique = true;
        boolean rankUnique = true;
        for (Move candidate : same) {
            if (candidate.fromX == m.fromX) fileUnique = false;
            if (candidate.fromY == m.fromY) rankUnique = false;
        }
        if (fileUnique) return "" + (char)('a' + m.fromX);
        if (rankUnique) return "" + (m.fromY + 1);
        return "" + (char)('a' + m.fromX) + (m.fromY + 1);
    }

    // --- --- --- --- --- SAN PARSING --- --- --- --- --- //

    /**
     * Parses one SAN token and returns the corresponding legal Move on this board,
     * or null if no legal move matches.
     */
    private static Move parseSANMove(String san, Board board) {
        PieceColor color = board.state.turn;

        // --- Castling ---
        if (san.equals("O-O-O") || san.equals("0-0-0")) {
            return findCastle(board, color, CastleSide.QUEEN_SIDE);
        }
        if (san.equals("O-O") || san.equals("0-0")) {
            return findCastle(board, color, CastleSide.KING_SIDE);
        }

        // --- Normal moves ---
        // SAN format: [piece][fromFile][fromRank][x][toFile][toRank][=promotion]
        char first = san.charAt(0);
        PieceType movingType;
        String rest;
        if (Character.isUpperCase(first) && first != 'O') {
            movingType = typeFromSanLetter(first);
            rest = san.substring(1);
        } else {
            movingType = PieceType.PAWN;
            rest = san;
        }

        // Promotion.
        PieceType promotion = null;
        int eqIdx = rest.indexOf('=');
        if (eqIdx >= 0) {
            promotion = typeFromSanLetter(rest.charAt(eqIdx + 1));
            rest = rest.substring(0, eqIdx);
        }

        // Strip trailing annotation.
        rest = rest.replaceAll("[+#!?]+$", "");

        // Remove 'x' capture indicator.
        rest = rest.replace("x", "");

        // Destination square: last two characters.
        if (rest.length() < 2) return null;
        char destFile = rest.charAt(rest.length() - 2);
        char destRank = rest.charAt(rest.length() - 1);
        if (destFile < 'a' || destFile > 'h') return null;
        if (destRank < '1' || destRank > '8') return null;
        int toX = destFile - 'a';
        int toY  = destRank - '1';
        String disambiguation = rest.substring(0, rest.length() - 2);

        // Decode disambiguation.
        int fromFileHint = -1, fromRankHint = -1;
        for (char c : disambiguation.toCharArray()) {
            if (c >= 'a' && c <= 'h') fromFileHint = c - 'a';
            else if (c >= '1' && c <= '8') fromRankHint = c - '1';
        }

        // Find the matching legal move.
        List<Move> candidates = Rules.legalMoves(board, color);
        for (Move m : candidates) {
            if (m.piece.type != movingType) continue;
            if (m.toX != toX || m.toY != toY) continue;
            if (fromFileHint >= 0 && m.fromX != fromFileHint) continue;
            if (fromRankHint >= 0 && m.fromY != fromRankHint) continue;
            if (promotion != null && m.promotion != promotion) continue;
            if (promotion == null && m.isPromotion()) continue;
            return m;
        }
        return null;
    }

    private static Move findCastle(Board board, PieceColor color, CastleSide side) {
        for (Move m : Rules.legalMoves(board, color)) {
            if (m.castle == side) return m;
        }
        return null;
    }

    private static PieceType typeFromSanLetter(char c) {
        switch (Character.toUpperCase(c)) {
            case 'N': return PieceType.KNIGHT;
            case 'B': return PieceType.BISHOP;
            case 'R': return PieceType.ROOK;
            case 'Q': return PieceType.QUEEN;
            case 'K': return PieceType.KING;
            default:  return PieceType.PAWN;
        }
    }

    // --- --- --- --- --- STRING UTILITIES --- --- --- --- --- //

    /** Removes PGN tag lines ([...]) and comments ({ ... }). */
    private static String stripTagsAndComments(String pgn) {
        // Remove comment blocks.
        String s = pgn.replaceAll("\\{[^}]*}", " ");
        // Remove tag lines.
        s = s.replaceAll("\\[[^\\]]*]", " ");
        // Remove line comments (;).
        s = s.replaceAll(";[^\n]*", " ");
        return s;
    }
}
