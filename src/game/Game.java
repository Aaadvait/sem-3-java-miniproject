package game;

import board.Board;
import pieces.*;
import move.Move;

public class Game {

    public void gameRun(String savedGame){
        System.out.println("SYS:    Running Game...");
        //Objects 
        Board B = new Board(savedGame);
        Piece P = new Piece();
        Move M = new Move();

        //MoveStack
        String[] moveStack = new String[1000];
        
        //Ascess Peice object -> B.board[][]

        //TEST PIECE MOVE;
        System.out.println(" --- BEFORE MOVE ---");
        B.displayBoard();

        int[] nextplace = {3,3};
        B.movePiece(nextplace, B.board[1][3], moveStack);

        System.out.println(" --- AFTER MOVE ---");
        B.displayBoard();
    }
}
