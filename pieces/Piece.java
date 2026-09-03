public class Piece{
    int pieceIndex = 0;
    char pieceColor;			//Gives the Color of the Piece
    boolean isDead = false;		//Tells if the piece is alive
    int[] piecePos = new int[2];//Tells the position of the board
    Piece(char pieceType, int index, char pc){	
        pieceColor = pc;	                        //Gets the color from inititalizing the piece using super()
        piecePos = intitPos(pieceType, index, pc);  //Gets the initial position of the Piece
        pieceIndex = index;                         //Get the PieceIndex, Useful for differentiating similar Pieces
    }

    int[] intitPos(char pieceType, int index, char pc){
        int posX = -1, posY = -1;

        switch (pc) {
            case 'W':
                switch (pieceType) {
                    case 'P':
                        posX = index;
                        posY = 2;
                        break;
                    case 'R':
                        if(index == 1) posX = 1;
                        else posX = 8;
                        posY = 1;
                        break;
                    case 'H':
                        if(index == 1) posX = 2;
                        else posX = 7;
                        posY = 1;
                        break;
                    case 'B':
                        if(index == 1) posX = 3;
                        else posX = 6;
                        posY = 1;
                        break;
                    case 'K':
                        posX = 4;
                        posY = 1;
                        break;
                    case 'Q':
                        posX = 5;
                        posY = 1;
                        break;
                    default:
                        break;
                }
                break;

            case 'B':
                switch (pieceType) {
                    case 'P':
                        posX = index;
                        posY = 7;
                        break;
                    case 'R':
                        if(index == 1) posX = 1;
                        else posX = 8;
                        posY = 8;
                        break;
                    case 'H':
                        if(index == 1) posX = 2;
                        else posX = 7;
                        posY = 8;
                        break;
                    case 'B':
                        if(index == 1) posX = 3;
                        else posX = 6;
                        posY = 8;
                        break;
                    case 'K':
                        posX = 4;
                        posY = 8;
                        break;
                    case 'Q':
                        posX = 5;
                        posY = 8;
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }

        int[] returnval = {posX, posY};

        return returnval;
    }
}

//Piece is defined
//Piece is given a colour
//Piece is placed on the board

