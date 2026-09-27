package game;

import board.Board;
import pieces.*;
import move.Move;

public class Game {

    // Gamemodes :
    // 0 - 1v1
    public void gameRun(String savedGame, String gamemode){
        System.out.println("SYS:    Running Game...");

        Board B = new Board(savedGame); //Creating Board
        Move M = new Move();            //Defining Move
        //Piece P = new Piece();

        if (gamemode.equals("default")){
            default_game(B, M);
        }
    }

    public void default_game(Board B, Move M){
        while (true){
            if (B.state.checkmateBlack || B.state.checkmateWhite){
                break;
            }
        }
    }
}
