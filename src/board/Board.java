package board;

import pieces.Piece;

public class Board{

    public String[] pieceName = {
    "WP1", "WP2", "WP3", "WP4", "WP5", "WP6", "WP7", "WP8",
    "WK1", "WQ1", "WB1", "WB2", "WH1", "WH2", "WR1", "WR2",
    "BP1", "BP2", "BP3", "BP4", "BP5", "BP6", "BP7", "BP8",
    "BK1", "BQ1", "BB1", "BB2", "BH1", "BH2", "BR1", "BR2",
    };

    
    
    public Piece[][] board = new Piece[8][8];
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
