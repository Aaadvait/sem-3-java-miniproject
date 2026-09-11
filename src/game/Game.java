package game;

import board.Board;
import pieces.*;
import move.Move;

public class Game {

    public void gameRun(String savedGame){
        System.out.println("SYS:    Running Game...");

        Board B = new Board(savedGame); //Creating Board
        Move M = new Move();            //Defining Move
        //Piece P = new Piece();

        //TEST PIECE MOVE;
        System.out.println(" --- BEFORE MOVE ---");
        B.displayBoard();

        int[] nextplace = {3,3};
        M.movePiece(nextplace, B.board[1][3], B, M);
        System.out.println(" --- AFTER MOVE ---");
        B.displayBoard(); System.out.println("MOVE STACK: " + M.moveStack[M.stackTop]);

        nextplace[0] = 4; nextplace[1] = 4;
        M.movePiece(nextplace, B.board[6][4], B, M);
        System.out.println(" --- AFTER MOVE ---");
        B.displayBoard(); System.out.println("MOVE STACK: " + M.moveStack[M.stackTop]);

        M.movePiece(nextplace, B.board[3][3], B, M);
        System.out.println(" --- AFTER MOVE ---");
        B.displayBoard(); System.out.println("MOVE STACK: " + M.moveStack[M.stackTop]);
    }
}
