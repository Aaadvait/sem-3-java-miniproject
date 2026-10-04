package analytics;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Persists per-player chess statistics to a Java {@link Properties} file.
 *
 * Storage location: {@code ~/ChessGames/stats.properties}
 *
 * Key scheme:
 *   {@code <playerKey>.gamesPlayed}
 *   {@code <playerKey>.wins}
 *   {@code <playerKey>.losses}
 *   {@code <playerKey>.draws}
 *   {@code <playerKey>.totalAccuracy}  (sum of per-game accuracy percentages × 100)
 *   {@code <playerKey>.accuracyGames}  (number of games with accuracy data)
 *
 * The {@code playerKey} is a lowercase, whitespace-free version of the player name.
 */
public class StatsStore {

    private static final Path STATS_FILE = Paths.get(
            System.getProperty("user.home"), "ChessGames", "stats.properties");

    private final Properties props;
    private final String playerKey;

    /**
     * Opens (or creates) the stats store for the given player name.
     *
     * @param playerName human-readable name, e.g. "White" or "Alice"
     */
    public StatsStore(String playerName) {
        this.playerKey = toKey(playerName);
        this.props = load();
    }

    // --- --- --- --- --- READ --- --- --- --- --- //

    public int gamesPlayed()  { return getInt("gamesPlayed"); }
    public int wins()         { return getInt("wins");  }
    public int losses()       { return getInt("losses"); }
    public int draws()        { return getInt("draws");  }

    /**
     * Average move accuracy across all games, 0–100.
     * Returns 0 if no accuracy data has been recorded yet.
     */
    public double averageAccuracy() {
        int games = getInt("accuracyGames");
        if (games == 0) return 0.0;
        return getInt("totalAccuracy") / (double) games;
    }

    /** Win rate 0–100, or 0 when no games have been played. */
    public double winRate() {
        int played = gamesPlayed();
        if (played == 0) return 0.0;
        return wins() * 100.0 / played;
    }

    // --- --- --- --- --- WRITE --- --- --- --- --- //

    /** Increments the games-played counter and the outcome counter. */
    public void recordResult(GameResult result) {
        increment("gamesPlayed");
        switch (result) {
            case WIN:  increment("wins");   break;
            case LOSS: increment("losses"); break;
            case DRAW: increment("draws");  break;
        }
        save();
    }

    /**
     * Records the move accuracy for a single game (0–100).
     * Multiple calls accumulate; the average is computed on read.
     */
    public void recordAccuracy(double accuracyPercent) {
        int current = getInt("totalAccuracy");
        setInt("totalAccuracy", current + (int) Math.round(accuracyPercent * 100));
        increment("accuracyGames");
        save();
    }

    /** Resets all statistics for this player to zero. */
    public void reset() {
        setInt("gamesPlayed", 0);
        setInt("wins",   0);
        setInt("losses", 0);
        setInt("draws",  0);
        setInt("totalAccuracy", 0);
        setInt("accuracyGames", 0);
        save();
    }

    // --- --- --- --- --- RESULT ENUM --- --- --- --- --- //

    public enum GameResult { WIN, LOSS, DRAW }

    // --- --- --- --- --- HELPERS --- --- --- --- --- //

    private int getInt(String field) {
        try {
            return Integer.parseInt(props.getProperty(playerKey + "." + field, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void setInt(String field, int value) {
        props.setProperty(playerKey + "." + field, Integer.toString(value));
    }

    private void increment(String field) {
        setInt(field, getInt(field) + 1);
    }

    private Properties load() {
        Properties p = new Properties();
        if (Files.exists(STATS_FILE)) {
            try (InputStream in = Files.newInputStream(STATS_FILE)) {
                p.load(in);
            } catch (IOException ignored) { }
        }
        return p;
    }

    private void save() {
        try {
            Files.createDirectories(STATS_FILE.getParent());
            try (OutputStream out = Files.newOutputStream(STATS_FILE)) {
                props.store(out, "Chess statistics");
            }
        } catch (IOException e) {
            System.err.println("Could not save stats: " + e.getMessage());
        }
    }

    private static String toKey(String name) {
        return name == null ? "player" : name.toLowerCase().replaceAll("\\s+", "_");
    }
}
