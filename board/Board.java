public class Board{

    String[] pieceNameArray = {
    "WP1", "WP2", "WP3", "WP4", "WP5", "WP6", "WP7", "WP8",
    "WK1", "WQ1", "WB1", "WB2", "WH1", "WH2", "WR1", "WR2",
    "BP1", "BP2", "BP3", "BP4", "BP5", "BP6", "BP7", "BP8",
    "BK1", "BQ1", "BB1", "BB2", "BH1", "BH2", "BR1", "BR2",
    };
    // The format of the pieces is "color_name_index". '_' is delimiter for seprating values
    // The values will be seprated in a loop and then fed into the proper piece Classes

    int[][] boardPositions = new int[8][8];
    char pieceName, pieceColor, pieceIndex;
    void generateBoard(){
        for(int i = 0; i < 32; i++){
	    pieceColor = pieceNameArray[i].charAt(0);
	    pieceName  = pieceNameArray[i].charAt(1);
	    pieceIndex = pieceNameArray[i].charAt(2);
	
	    System.out.println("Piece: " + pieceName +  " - Color: " + pieceColor + " - Index: " + pieceIndex);
	}
    }

    public static void main(String[] args){

        Board board = new Board();

       board.generateBoard();

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