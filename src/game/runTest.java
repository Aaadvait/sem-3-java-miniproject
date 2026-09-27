package game;

import game.Game;
import move.Move;
import pieces.Piece;
import gui.ChessUI;

public class runTest {
    public void test(String[] args){
        ChessUI chess = new ChessUI();
        chess.runChessUI(args);
    }
}
