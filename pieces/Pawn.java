public class Pawn extends Piece{
    boolean has_moved = false;
    Pawn(char pieceType, int index, char pc){
        super(pieceType, index, pc);
    }
}

//Piece is conveted into a Pawn.