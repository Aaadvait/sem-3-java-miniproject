package move;

import board.Board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Full undo/redo history for a game.
 *
 * Every move played is pushed onto the <em>done</em> stack. Undoing pops it,
 * restores the board and pushes onto <em>redone</em>. Redoing pops from
 * <em>redone</em>, re-applies the move and pushes back onto <em>done</em>.
 *
 * The redo stack is cleared whenever a new move is played so a newly played
 * move always branches the history forward (just like in a text editor).
 */
public class MoveHistory {

    private final Board board;
    private final List<Move> done   = new ArrayList<>();
    private final List<Move> redone = new ArrayList<>();

    public MoveHistory(Board board) {
        this.board = board;
    }

    // --- --- --- --- --- OPERATIONS --- --- --- --- --- //

    /**
     * Records and applies a move that was already validated.
     * Clears the redo stack.
     */
    public void push(Move m) {
        board.applyMove(m);
        done.add(m);
        redone.clear();
    }

    /**
     * Undoes the last move.
     *
     * @return the move that was undone, or null if there is nothing to undo
     */
    public Move undo() {
        if (done.isEmpty()) return null;
        Move m = done.remove(done.size() - 1);
        board.undoMove(m);
        redone.add(m);
        return m;
    }

    /**
     * Re-applies the most-recently undone move.
     *
     * @return the move that was redone, or null if there is nothing to redo
     */
    public Move redo() {
        if (redone.isEmpty()) return null;
        Move m = redone.remove(redone.size() - 1);
        board.applyMove(m);
        done.add(m);
        return m;
    }

    // --- --- --- --- --- QUERIES --- --- --- --- --- //

    /** All moves played so far, in order (oldest first). Unmodifiable. */
    public List<Move> getMoves() {
        return Collections.unmodifiableList(done);
    }

    /** The most recently played move, or null if none. */
    public Move lastMove() {
        return done.isEmpty() ? null : done.get(done.size() - 1);
    }

    /** True when undo is possible. */
    public boolean canUndo() { return !done.isEmpty(); }

    /** True when redo is possible. */
    public boolean canRedo() { return !redone.isEmpty(); }

    /** Total number of half-moves (plies) played. */
    public int size() { return done.size(); }

    /** Removes all history (used when starting a fresh game). */
    public void clear() {
        done.clear();
        redone.clear();
    }
}
