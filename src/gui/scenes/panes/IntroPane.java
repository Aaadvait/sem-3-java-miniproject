package gui.scenes.panes;

import gui.CommonValues;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class IntroPane {
    CommonValues cv = new CommonValues();

    public StackPane root = new StackPane();

    HBox intro = new HBox();

    Text welcomeText = new Text("CHESS");
    Image chessImage = new Image(getClass().getResourceAsStream("/gui/resources/chessIcon.png"));
    ImageView chessIcon = new ImageView(chessImage);


    public IntroPane(){
        welcomeText.setX(50);
        welcomeText.setY(50);
        welcomeText.setFont(cv.NEW_ROCKER_BIG);
        welcomeText.setFill(Color.WHITE);

        chessIcon.setFitHeight(76);
        chessIcon.setPreserveRatio(true);

        intro.setSpacing(50);
        intro.setAlignment(Pos.CENTER);
        intro.getChildren().addAll(welcomeText, chessIcon);

        root.getChildren().add(intro);
    }
}
