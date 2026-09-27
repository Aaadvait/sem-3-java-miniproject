package gui.scenes;

// MY IMPORTS
import gui.CommonValues;
import gui.scenes.panes.*;

// JAVAFX IMPORTS
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class BaseScene {
    CommonValues cv = new CommonValues();

    //INTRO PANE OBJECTS
    IntroPane introPane = new IntroPane();
    TranslateTransition moveIntroPane = new TranslateTransition(Duration.millis(300), introPane.root);

    //SIDE PANEL OBJECTS
    SidePane sidePane = new SidePane();
    TranslateTransition moveSidePane = new TranslateTransition(Duration.millis(250), sidePane.root);

    //BASE PANE OBJECTS
    StackPane basePane = new StackPane();

    //BACKGROUND PANE OBJECTS
    Image backgroundImage = new Image(getClass().getResourceAsStream("/gui/resources/bg.png"));
    ImageView background = new ImageView(backgroundImage);
    StackPane backgroundPane = new StackPane();
    ScaleTransition scaleBackground = new ScaleTransition(Duration.millis(200), backgroundPane);
    TranslateTransition moveBackground = new TranslateTransition(Duration.millis(300), backgroundPane);

    //EXIT FADE PANE
    StackPane exitPane = new StackPane();
    FadeTransition exitFade = new FadeTransition(Duration.millis(1000), exitPane);

    public Scene scene = new Scene(basePane, cv.WIDTH, cv.HEIGHT, Color.BLACK);

    public BaseScene(){
        //PANE LOGIC
        introPane.root.setOnMouseClicked(event -> {onClickIntroScene();});
        exitPane.setStyle("-fx-background-color: black;");
        exitPane.setOpacity(0);

        //Button Logic
        sidePane.playButton.setOnMouseClicked(event -> {playButton();});
        sidePane.loadButton.setOnMouseClicked(event -> {loadButton();});
        sidePane.exitButton.setOnMouseClicked(event -> {exitButton();});

        //PLACING PLANES ON BASE PANE
        background.setFitHeight(cv.HEIGHT);
        background.setFitWidth(cv.WIDTH);
        backgroundPane.getChildren().add(background);

        basePane.getChildren().addAll(backgroundPane, sidePane.root, introPane.root);
    }

    // --- --- x --- ON CLICK FUNCTIONS --- x --- --- //

    void onClickIntroScene(){
        moveIntroPane.setByX(500);
        moveIntroPane.setInterpolator(Interpolator.EASE_BOTH);
        moveSidePane.setByX(350);
        moveSidePane.setInterpolator(Interpolator.EASE_OUT);
        scaleBackground.setToX(1.1);
        scaleBackground.setToY(1.1);
        scaleBackground.play();
        moveIntroPane.play();
        moveSidePane.play();
        introPane.root.setOnMouseClicked(null);
        //basePane.getChildren().remove(introPane.root);
    }

    void playButton(){
        //Moving Controls to Side
        moveIntroPane.setByX(500);
        moveIntroPane.setInterpolator(Interpolator.EASE_IN);
        moveSidePane.setByX(-350);
        moveIntroPane.setInterpolator(Interpolator.EASE_IN);
        //Translating Background a little
        moveBackground.setByY(50);
        moveBackground.setInterpolator(Interpolator.EASE_BOTH);

        moveIntroPane.play();
        moveSidePane.play();
        moveBackground.play();
    }

    void loadButton(){
        //Moving Controls to Side
        moveIntroPane.setByX(500);
        moveIntroPane.setInterpolator(Interpolator.EASE_IN);
        moveSidePane.setByX(-350);
        moveIntroPane.setInterpolator(Interpolator.EASE_IN);
        //Translating Background a little
        moveBackground.setByY(50);
        moveBackground.setInterpolator(Interpolator.EASE_BOTH);

        moveIntroPane.play();
        moveSidePane.play();
        moveBackground.play();
    }

    void exitButton(){
        basePane.getChildren().add(exitPane);
        moveIntroPane.setByX(-500);
        moveIntroPane.setInterpolator(Interpolator.EASE_BOTH);
        moveSidePane.setByX(-350);
        moveSidePane.setInterpolator(Interpolator.EASE_IN);
        scaleBackground.setToX(1.0);
        scaleBackground.setToY(1.0);
        exitFade.setFromValue(0.0);
        exitFade.setToValue(1.0);
        moveIntroPane.play();
        moveSidePane.play();
        scaleBackground.play();
        exitFade.play();
        exitFade.setOnFinished(event -> {Platform.exit();});
    }
}
