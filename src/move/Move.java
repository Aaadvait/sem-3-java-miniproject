package move;

import pieces.Piece;
import board.Board;

//Contains Move Rules
public class Move{
    public String[] moveStack = new String[1000];
    public int stackTop = -1;

// --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- //

    // --- BASIC MOVE --- //

    public void movePiece(int[] nextPos, Piece selPiece, Board board, Move move){
        if(board.board[nextPos[1]][nextPos[0]] == null){
            board.board[selPiece.posY][selPiece.posX] = null;
            move.moveStack[++move.stackTop] = ""
                    + selPiece.pieceName
                    + selPiece.posY + selPiece.posX
                    + nextPos[1] + nextPos[0]
                    + "m";
            selPiece.posY = nextPos[1];
            selPiece.posX = nextPos[0];
            selPiece.moveNumber++;
            board.board[nextPos[1]][nextPos[0]] = selPiece;
        } else {
            board.board[selPiece.posY][selPiece.posX] = null;
            move.moveStack[++move.stackTop] = ""
                    + selPiece.pieceName
                    + selPiece.posY + selPiece.posX
                    + nextPos[1]    + nextPos[0]
                    + "x"
                    + board.board[nextPos[1]][nextPos[0]].pieceName
                    + board.board[nextPos[1]][nextPos[0]].moveNumber;
            selPiece.posY = nextPos[1];
            selPiece.posX = nextPos[0];
            selPiece.moveNumber++;
            board.board[nextPos[1]][nextPos[0]] = selPiece;
        }
    }

    // --- READING MOVE STACK --- //

    public boolean captureMove(String moveData){
        return (moveData.charAt(7) == 'x');
    }

    public Piece readPiece(String moveData, boolean readCapture){
        Piece TEMP;
        char color, type;
        int index, posX, posY;
        int no_moves = 0;
        if(readCapture){
            color = moveData.charAt(8);
            type  = moveData.charAt(9);
            index = (moveData.charAt(10)-'0');
            posY  = (moveData.charAt(5)-'0');
            posX  = (moveData.charAt(6)-'0');
            no_moves = Integer.parseInt(moveData.substring(11));
        } else {
            color = moveData.charAt(0);
            type  = moveData.charAt(1);
            index = (moveData.charAt(2)-'0');
            posY  = (moveData.charAt(3)-'0');
            posX  = (moveData.charAt(4)-'0');
        }
        TEMP = new Piece(type, index, color, false);
        TEMP.moveNumber = no_moves;
        TEMP.posX = posX;
        TEMP.posY = posY;
        return TEMP;
    }

    // ---  --- //
}