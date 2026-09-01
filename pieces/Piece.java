public class Piece{
    String color;			//Gives the Color of the Piece
    boolean isDead = false;		//Tells if the piece is alive
    int[] position = new int[2];	//Tells the position of the board
    Piece(String pc, int[] pos){	
        color = pc;			//Gets the color from inititalizing the piece using super()
        position = pos;			//Gets the position from initializing the piece using super()
    }
}

//Piece is defined
//Piece is given a colour
//Piece is placed on the board
