package board;

import pieces.Piece;

public class Board{

    // --- Generating Board --- //
    public String[] pieceName = {
    "WP1", "WP2", "WP3", "WP4", "WP5", "WP6", "WP7", "WP8",
    "WK1", "WQ1", "WB1", "WB2", "WH1", "WH2", "WR1", "WR2",
    "BP1", "BP2", "BP3", "BP4", "BP5", "BP6", "BP7", "BP8",
    "BK1", "BQ1", "BB1", "BB2", "BH1", "BH2", "BR1", "BR2",
    };
    public Piece[][] board = new Piece[8][8];               //Board Array
    public State state = new State();                       //Board State
    public Board(String savedGame){                         //Board Piece
        System.out.println("SYS:    Generating Board...");
        Piece P;
        int index;
        char type, color;
        for(int i = 0; i < 32; i++){
            color = pieceName[i].charAt(0);
            type  = pieceName[i].charAt(1);
            index = pieceName[i].charAt(2)-'0';
            P     = new Piece(type, index, color, true);
            board[P.posY][P.posX] = P;
            System.out.println("SYS:    Generating Piece - " + P.pieceName);
        }
        // --- Getting Board Data from file -- //
        {

        }
    }

    // --- TEST FUNCTIONS --- //

    public void displayBoard(){
        for(int row = 0; row < 8; row++){
            for(int col = 0; col < 8; col++){
                if(board[row][col] == null) System.out.print("  -  ");
                else System.out.print(" " + board[row][col].pieceName + " ");
            }
            System.out.println("\n");
        }
    }
    /*
        1   2   3   4   5   6   7   8

    1   wr1 wh1 wb1 wk1 wq1 wb2 wh2 wr2

    2   wp1 wp2 wp3 wp4 wp5 wp6 wp7 wp8

    3

    4

    5

    6

    7   bp1 bp2 bp3 bp4 bp5 bp6 bp7 bp8

    8   br1 bh1 bb1 bk1 bq1 bb2 bh2 br2
    */
}


