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
