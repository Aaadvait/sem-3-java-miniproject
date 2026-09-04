package pieces;

public class Piece{
    
    //Peice Discription
    public char pieceType   = '\0';
    public int pieceIndex   =   0 ;
    public char pieceColor  = '\0';
    public String pieceName = "\0";

    //Peice Attributes
    public int posX, posY;
    public boolean isDead = false;
    public int moveNumber = 0;

    public Piece(){
        //Default Constructor
    }

    public Piece(char type, int index, char pc){	
        pieceColor = pc;
        intitPos(type, index, pc);
        pieceIndex = index;
        pieceType  = type;
        pieceName  = "" + pc + type + index;
    }

    void intitPos(char pieceType, int index, char pc){

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
        posX = posX - 1;
        posY = posY - 1;
    }
}

//Piece is defined
//Piece is given a colour
//Piece is placed on the board