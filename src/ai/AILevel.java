package ai;

/**
 * Named AI difficulty levels.
 *
 * Each level maps to a search depth and a set of evaluation weights used by
 * the {@link Minimax} engine. Lower levels deliberately play weaker by
 * reducing search depth and introducing random noise into the evaluation.
 */
public enum AILevel {

    /** Random legal move. */
    BEGINNER ("Beginner", 1, 0.85, true),

    /** 1-ply; picks the best immediate move but ignores opponent replies. */
    EASY     ("Easy",     1, 0.95, false),

    /** 2-ply; looks one full move ahead. */
    MEDIUM   ("Medium",   2, 1.00, false),

    /** 3-ply; sees two full moves ahead. */
    HARD     ("Hard",     3, 1.00, false),

    /** 4-ply; sees three full moves ahead plus quiescence search. */
    EXPERT   ("Expert",   4, 1.00, false);

    // --- --- --- --- --- --- --- --- --- --- --- //

    /** Human-readable label for the UI. */
    public final String label;

    /** Minimax search depth (half-moves / plies). */
    public final int depth;

    /**
     * Evaluation scale factor (< 1 makes weaker levels less decisive).
     * The engine multiplies its raw evaluation by this value, so a rating of
     * 0.85 means "sort of good move" and "sort of bad move" look more alike,
     * causing the AI to pick worse moves occasionally.
     */
    public final double evalScale;

    /**
     * When true, 30 % of the time the AI picks a random legal move instead of
     * the engine's first choice (used for Beginner).
     */
    public final boolean randomise;

    AILevel(String label, int depth, double evalScale, boolean randomise) {
        this.label     = label;
        this.depth     = depth;
        this.evalScale = evalScale;
        this.randomise = randomise;
    }

    @Override
    public String toString() { return label; }
}
