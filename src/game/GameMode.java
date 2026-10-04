package game;

/**
 * Named time-control presets mirroring the modes found on modern chess platforms.
 *
 * Each mode stores its time limit per side (in milliseconds). UNTIMED means
 * clocks count up and nobody ever loses on time – it is the legacy DEFAULT mode.
 * CUSTOM lets callers supply any value not covered by the named modes.
 */
public enum GameMode {

    // --- Bullet -----------------------------------------------------------
    BULLET_1    ("Bullet  1+0",  1 * 60_000L, 0),
    BULLET_2    ("Bullet  2+1",  2 * 60_000L, 1_000L),

    // --- Blitz ------------------------------------------------------------
    BLITZ_3     ("Blitz   3+0",  3 * 60_000L, 0),
    BLITZ_3_2   ("Blitz   3+2",  3 * 60_000L, 2_000L),
    BLITZ_5     ("Blitz   5+0",  5 * 60_000L, 0),

    // --- Rapid ------------------------------------------------------------
    RAPID_10    ("Rapid  10+0", 10 * 60_000L, 0),
    RAPID_15_10 ("Rapid 15+10", 15 * 60_000L, 10_000L),
    RAPID_30    ("Rapid  30+0", 30 * 60_000L, 0),

    // --- Classical --------------------------------------------------------
    CLASSICAL   ("Classical",   60 * 60_000L, 30_000L),

    // --- No clock ---------------------------------------------------------
    UNTIMED     ("Untimed",               0L, 0),

    // --- Custom (caller sets timeMs/incrementMs themselves) ---------------
    CUSTOM      ("Custom",                0L, 0);

    // --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- //

    /** Human-readable label shown in the mode picker. */
    public final String label;

    /** Starting time per side in milliseconds (0 = untimed / custom). */
    public final long timeMs;

    /**
     * Increment added to the active player's clock after each move, in ms.
     * 0 means no increment (simple sudden-death).
     */
    public final long incrementMs;

    GameMode(String label, long timeMs, long incrementMs) {
        this.label       = label;
        this.timeMs      = timeMs;
        this.incrementMs = incrementMs;
    }

    /** True when this mode uses running clocks. */
    public boolean isTimed() {
        return this != UNTIMED && timeMs > 0;
    }

    @Override
    public String toString() {
        return label;
    }
}
