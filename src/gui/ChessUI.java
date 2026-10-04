package gui;

// JAVAFX IMPORTS
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

// COMMON VALUES

// TO BE REMOVED LATER
import gui.scenes.*;

public class ChessUI extends Application{

    public void runChessUI(String[] args){
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        CommonValues cv = new CommonValues();

        BaseScene ts = new BaseScene();

        //WindowDisplayInfo
        Image stageIcon = new Image("gui/resources/chess.png");
        stage.getIcons().add(stageIcon);
        stage.setTitle("Chess");

        //WindowSize
        stage.setWidth(cv.WIDTH);
        stage.setHeight(cv.HEIGHT);
        stage.setResizable(false);

        stage.setScene(ts.scene);
        stage.show();
    }
}
