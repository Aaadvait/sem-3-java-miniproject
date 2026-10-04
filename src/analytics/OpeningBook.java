package analytics;

import move.Move;
import pieces.PieceColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recognises chess openings from a hard-coded ECO (Encyclopedia of Chess
 * Openings) table and returns the opening name for the move sequence played.
 *
 * The table contains the 60 most common openings. It is stored as a mapping
 * from a compact move-sequence string to an opening name; matching is done
 * by longest prefix.
 *
 * Move-sequence key format (example): "e2e4 e7e5 g1f3 b8c6"
 * (each half-move is "fromSquare toSquare", separated by spaces)
 */
public final class OpeningBook {

    /**
     * Returns the name of the opening for the moves played so far, or
     * {@code ""} if no recognised opening matches.
     *
     * @param moves all moves played, in order
     * @return opening name (e.g. "Ruy López Opening") or empty string
     */
    public static String recognise(List<Move> moves) {
        if (moves.isEmpty()) return "";

        String key = buildKey(moves);
        String bestMatch = "";
        String bestName  = "";

        for (Map.Entry<String, String> entry : BOOK.entrySet()) {
            String prefix = entry.getKey();
            if (key.startsWith(prefix) && prefix.length() > bestMatch.length()) {
                bestMatch = prefix;
                bestName  = entry.getValue();
            }
        }
        return bestName;
    }

    // --- --- --- --- --- HELPERS --- --- --- --- --- //

    private static String buildKey(List<Move> moves) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < moves.size(); i++) {
            Move m = moves.get(i);
            if (i > 0) sb.append(' ');
            sb.append(sq(m.fromX, m.fromY)).append(sq(m.toX, m.toY));
        }
        return sb.toString();
    }

    private static String sq(int x, int y) {
        return "" + (char)('a' + x) + (y + 1);
    }

    // --- --- --- --- --- ECO TABLE --- --- --- --- --- //

    private static final Map<String, String> BOOK = new HashMap<>();

    static {
        // --- Open Games (1. e4 e5) ---
        put("e2e4 e7e5",                                         "Open Game");
        put("e2e4 e7e5 g1f3",                                    "King's Knight Opening");
        put("e2e4 e7e5 g1f3 b8c6 f1b5",                         "Ruy López Opening");
        put("e2e4 e7e5 g1f3 b8c6 f1b5 a7a6",                    "Ruy López: Morphy Defence");
        put("e2e4 e7e5 g1f3 b8c6 f1c4",                         "Italian Game");
        put("e2e4 e7e5 g1f3 b8c6 f1c4 f8c5",                    "Giuoco Piano");
        put("e2e4 e7e5 g1f3 b8c6 f1c4 g8f6",                    "Two Knights Defence");
        put("e2e4 e7e5 g1f3 b8c6 d2d4",                         "Scotch Game");
        put("e2e4 e7e5 g1f3 b8c6 d2d4 e5d4",                    "Scotch Game: Main Line");
        put("e2e4 e7e5 f2f4",                                    "King's Gambit");
        put("e2e4 e7e5 f2f4 e5f4",                               "King's Gambit Accepted");
        put("e2e4 e7e5 f2f4 f8c5",                               "King's Gambit Declined");
        put("e2e4 e7e5 g1f3 g8f6",                               "Petrov's Defence");
        put("e2e4 e7e5 g1f3 f7f5",                               "Latvian Gambit");
        put("e2e4 e7e5 g1f3 b8c6 b1c3",                         "Three Knights Game");
        put("e2e4 e7e5 g1f3 b8c6 b1c3 g8f6",                    "Four Knights Game");
        put("e2e4 e7e5 g1f3 b8c6 f1c4 d7d6",                    "Hungarian Defence");
        put("e2e4 e7e5 d2d4 e5d4",                               "Centre Game");
        put("e2e4 e7e5 b1c3",                                    "Vienna Game");

        // --- Semi-Open Games (1. e4, not e5) ---
        put("e2e4 c7c5",                                         "Sicilian Defence");
        put("e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 g7g6", "Sicilian: Dragon Variation");
        put("e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6", "Sicilian: Najdorf Variation");
        put("e2e4 c7c5 b1c3",                                   "Sicilian: Closed Variation");
        put("e2e4 c7c5 g1f3 e7e6",                              "Sicilian: Kan Variation");
        put("e2e4 c7c5 g1f3 b8c6",                              "Sicilian: Old Sicilian");
        put("e2e4 e7e6",                                         "French Defence");
        put("e2e4 e7e6 d2d4 d7d5 b1c3",                        "French: Classical Variation");
        put("e2e4 e7e6 d2d4 d7d5 e4d5",                        "French: Exchange Variation");
        put("e2e4 e7e6 d2d4 d7d5 e4e5",                        "French: Advance Variation");
        put("e2e4 c7c6",                                         "Caro-Kann Defence");
        put("e2e4 c7c6 d2d4 d7d5 b1c3",                        "Caro-Kann: Classical");
        put("e2e4 c7c6 d2d4 d7d5 e4d5",                        "Caro-Kann: Exchange");
        put("e2e4 c7c6 d2d4 d7d5 e4e5",                        "Caro-Kann: Advance");
        put("e2e4 d7d5",                                         "Scandinavian Defence");
        put("e2e4 d7d5 e4d5 d8d5",                              "Scandinavian: Main Line");
        put("e2e4 g7g6",                                         "Modern Defence");
        put("e2e4 d7d6",                                         "Pirc Defence");
        put("e2e4 d7d6 d2d4 g8f6 b1c3 g7g6",                  "Pirc: Austrian Attack");
        put("e2e4 b7b6",                                         "Owen's Defence");
        put("e2e4 a7a6",                                         "St. George Defence");

        // --- Closed Games (1. d4 d5) ---
        put("d2d4 d7d5",                                         "Queen's Pawn Game");
        put("d2d4 d7d5 c2c4",                                   "Queen's Gambit");
        put("d2d4 d7d5 c2c4 d5c4",                              "Queen's Gambit Accepted");
        put("d2d4 d7d5 c2c4 e7e6",                              "Queen's Gambit Declined");
        put("d2d4 d7d5 c2c4 c7c6",                              "Slav Defence");
        put("d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c1g5",             "Queen's Gambit Declined: Classical");
        put("d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 g1f3 f8e7",       "Queen's Gambit Declined: Orthodox");

        // --- Indian Defences ---
        put("d2d4 g8f6",                                         "Indian Defence");
        put("d2d4 g8f6 c2c4 g7g6",                              "King's Indian Defence");
        put("d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6",        "King's Indian: Main Line");
        put("d2d4 g8f6 c2c4 e7e6",                              "Nimzo/Queen's Indian");
        put("d2d4 g8f6 c2c4 e7e6 b1c3 f8b4",                   "Nimzo-Indian Defence");
        put("d2d4 g8f6 c2c4 e7e6 g1f3 b7b6",                   "Queen's Indian Defence");
        put("d2d4 g8f6 c2c4 c7c5",                              "Benoni Defence");
        put("d2d4 g8f6 c2c4 c7c5 d4d5 e7e6",                   "Modern Benoni");

        // --- Flank Openings ---
        put("g2g3",                                              "King's Fianchetto");
        put("c2c4",                                              "English Opening");
        put("c2c4 e7e5",                                         "English: King's English");
        put("c2c4 c7c5",                                         "English: Symmetrical");
        put("g1f3",                                              "Réti Opening");
        put("g1f3 d7d5 c2c4",                                   "Réti: Main Line");
        put("b2b3",                                              "Nimzowitsch-Larsen Attack");
        put("b2b4",                                              "Polish Opening (Orangutan)");
        put("f2f4",                                              "Bird's Opening");
        put("d2d4 g8f6 g1f3 g7g6 g2g3 f8g7 f1g2 d7d6",       "King's Indian Attack");
    }

    private static void put(String key, String name) {
        BOOK.put(key, name);
    }
}
