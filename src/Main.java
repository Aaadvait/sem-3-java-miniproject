import game.runTest;
import gui.ChessUI;

public class Main {
    public static void main(String[] args){
        boolean test = false;
        if (!test) {
            System.out.println("Runing MAIN");
            // --- STARTING UP APP --- //
            ChessUI chessUI = new ChessUI();
            chessUI.runChessUI(args);
        } else {
          runTest T = new runTest();
          T.test(args);
        }
    }
}