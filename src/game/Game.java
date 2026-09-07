package game;

import board.Board;
import pieces.*;

public class Game {

    public void gameRun(String savedGame){
        //Objects 
        Board B = new Board();
        //Ascess Peice object -> B.board[][]
        
        //Generating Peices and placing them on the board
        Piece[] P = new Piece[32];
        int index;
        char type, color;
        for(int i = 0; i < 32; i++){
            color = B.pieceName[i].charAt(0);
            type  = B.pieceName[i].charAt(1);
            index = (int)(B.pieceName[i].charAt(2)-'0');
            P[i]  = new Piece(type, index, color);
            B.board[P[i].posX][P[i].posY] = P[i];
        }
        //Getting Positions From files
        {
            //Open savedGame to get the Stringvalues
        }
    }
}
