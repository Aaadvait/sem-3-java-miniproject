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
        
        //Ascess Peice object -> B.board[][]

        //TEST PIECE MOVE;
        System.out.println(" --- BEFORE MOVE ---");
        B.displayBoard();

        int[] nextplace = {3,3};
        B.movePiece(nextplace, B.board[1][3], M);
        System.out.println(" --- AFTER MOVE ---");
        B.displayBoard(); System.out.println("MOVE STACK: " + M.moveStack[M.stackTop]);

        nextplace[0] = 4; nextplace[1] = 4;
        B.movePiece(nextplace, B.board[6][4], M);
        System.out.println(" --- AFTER MOVE ---");
        B.displayBoard(); System.out.println("MOVE STACK: " + M.moveStack[M.stackTop]);

        B.movePiece(nextplace, B.board[3][3], M);
        System.out.println(" --- AFTER MOVE ---");
        B.displayBoard(); System.out.println("MOVE STACK: " + M.moveStack[M.stackTop]);
    }
}
