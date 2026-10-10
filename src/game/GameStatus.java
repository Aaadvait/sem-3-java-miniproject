package game;

/** The overall status of a game. */
public enum GameStatus {
    PLAYING,                 // still running
    CHECKMATE,               // a king is in check with no legal move
    STALEMATE,               // no legal move, not in check
    TIMEOUT,                 // a player's clock reached 0:00
    INSUFFICIENT_MATERIAL,   // neither side can force mate
    THREEFOLD_REPETITION,    // the same position occurred three times
    FIFTY_MOVE_RULE,         // 50 moves without a pawn move or capture
    ABANDONED,               // the game was left before it finished
    RESIGNATION;             // a player resigned

    public boolean isGameOver() {
        return this != PLAYING;
    }
}
