package gui.scenes;

// MY IMPORTS
import game.Game;
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
import javafx.stage.Stage;
import javafx.util.Duration;

public class BaseScene {
    CommonValues cv = new CommonValues();

    //INTRO PANE OBJECTS
    IntroPane introPane = new IntroPane();
    TranslateTransition moveIntroPane = new TranslateTransition(Duration.millis(600), introPane.root);

    //SIDE PANEL OBJECTS
    SidePane sidePane = new SidePane();
    TranslateTransition moveSidePane = new TranslateTransition(Duration.millis(600), sidePane.root);

    //MODE PANE OBJECTS
    ModePane modePane = new ModePane();

    //GAME SCREEN (created when a mode is chosen, disposed when we come back)
    GamePane gamePane = null;
    Stage stage = null;     // remembered here: while the game scene is shown,
                            // scene.getWindow() is null for the menu scene

    //BASE PANE OBJECTS
    StackPane basePane = new StackPane();

    //BACKGROUND PANE OBJECTS
    Image backgroundImage = new Image(getClass().getResourceAsStream("/gui/resources/bg.png"));
    ImageView background = new ImageView(backgroundImage);
    StackPane backgroundPane = new StackPane();
    ScaleTransition scaleBackground = new ScaleTransition(Duration.millis(600), backgroundPane);
    TranslateTransition moveBackground = new TranslateTransition(Duration.millis(600), backgroundPane);

    //EXIT FADE PANE
    StackPane exitPane = new StackPane();
    FadeTransition exitFade = new FadeTransition(Duration.millis(1200), exitPane);

    public Scene scene = new Scene(basePane, cv.WIDTH, cv.HEIGHT, Color.BLACK);

    public BaseScene(){
        //PANE LOGIC
        introPane.root.setOnMouseClicked(event -> {onClickIntroScene();});
        exitPane.setStyle("-fx-background-color: black;");
        exitPane.setOpacity(0);
        introPane.root.setMouseTransparent(false);
        sidePane.root.setMouseTransparent(true);
        modePane.root.setMouseTransparent(true);

        //Button Logic
        sidePane.playButton.setOnMouseClicked(event -> {playButton();});
        sidePane.loadButton.setOnMouseClicked(event -> {loadButton();});
        sidePane.exitButton.setOnMouseClicked(event -> {exitButton();});
        modePane.backButton.setOnMouseClicked(event -> {bacKButton();});

        //Mode buttons start a game of the chosen time control
        modePane.defaultModeButton.setOnMouseClicked(event -> {startGame(Game.UNTIMED);});
        modePane.fiveMinModeButton.setOnMouseClicked(event -> {startGame(Game.FIVE_MINUTES_MS);});
        modePane.tenMinModeButton.setOnMouseClicked(event -> {startGame(Game.TEN_MINUTES_MS);});

        //PLACING PLANES ON BASE PANE
        background.setFitHeight(cv.HEIGHT);
        background.setFitWidth(cv.WIDTH);
        backgroundPane.getChildren().add(background);

        basePane.getChildren().addAll(backgroundPane, modePane.root, sidePane.root, introPane.root);
    }

    // --- --- x --- ON CLICK FUNCTIONS --- x --- --- //

    void onClickIntroScene(){
        moveIntroPane.setByX(500);
        moveIntroPane.setInterpolator(Interpolator.EASE_BOTH);
        moveSidePane.setByX(350);
        moveSidePane.setInterpolator(Interpolator.EASE_BOTH);
        scaleBackground.setToX(1.1);
        scaleBackground.setToY(1.1);
        scaleBackground.play();
        moveIntroPane.play();
        moveSidePane.play();
        introPane.root.setOnMouseClicked(null);
        introPane.root.setMouseTransparent(true);
        sidePane.root.setMouseTransparent(false);
        //basePane.getChildren().remove(introPane.root);
    }

    void playButton(){
        //MODE PANE
        modePane.moveBackButton.setByY(100);
        modePane.moveBackButton.setInterpolator(Interpolator.EASE_OUT);
        modePane.moveModePanel.setByY(540);
        modePane.moveModePanel.setInterpolator(Interpolator.EASE_OUT);

        //Moving Controls to Side
        moveIntroPane.setByX(500);
        moveIntroPane.setInterpolator(Interpolator.EASE_IN);
        moveSidePane.setByX(-350);
        moveIntroPane.setInterpolator(Interpolator.EASE_IN);

        //Translating Background a little
        moveBackground.setByY(50);
        moveBackground.setInterpolator(Interpolator.EASE_BOTH);

        modePane.moveBackButton.play();
        modePane.moveModePanel.play();

        moveIntroPane.play();
        moveSidePane.play();
        moveBackground.play();

        sidePane.root.setMouseTransparent(true);
        modePane.root.setMouseTransparent(false);
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
        moveIntroPane.setInterpolator(Interpolator.EASE_OUT);
        moveSidePane.setByX(-350);
        moveSidePane.setInterpolator(Interpolator.EASE_IN);
        scaleBackground.setToX(1.0);
        scaleBackground.setToY(1.0);
        scaleBackground.setInterpolator(Interpolator.EASE_BOTH);
        exitFade.setFromValue(0.0);
        exitFade.setToValue(1.0);
        exitFade.setInterpolator(Interpolator.EASE_IN);
        moveIntroPane.play();
        moveSidePane.play();
        scaleBackground.play();
        exitFade.play();
        exitFade.setOnFinished(event -> {Platform.exit();});
    }

    void bacKButton(){
        modePane.moveBackButton.setByY(-100);
        modePane.moveBackButton.setInterpolator(Interpolator.EASE_IN);
        modePane.moveModePanel.setByY(-540);
        modePane.moveModePanel.setInterpolator(Interpolator.EASE_IN);

        //Moving Controls to Side
        moveIntroPane.setByX(-500);
        moveIntroPane.setInterpolator(Interpolator.EASE_OUT);
        moveSidePane.setByX(350);
        moveIntroPane.setInterpolator(Interpolator.EASE_OUT);
        //Translating Background a little
        moveBackground.setByY(-50);
        moveBackground.setInterpolator(Interpolator.EASE_BOTH);

        modePane.moveBackButton.play();
        modePane.moveModePanel.play();

        moveIntroPane.play();
        moveSidePane.play();
        moveBackground.play();

        sidePane.root.setMouseTransparent(false);
        modePane.root.setMouseTransparent(true);
    }

    // --- --- x --- GAME NAVIGATION --- x --- --- //

    /** Switches the stage to a fresh game screen for the chosen time control. */
    void startGame(long timeControlMs){
        stage = (Stage) scene.getWindow();
        if (stage == null) return;

        if (gamePane != null) gamePane.dispose();   // never two clocks running at once
        gamePane = new GamePane(timeControlMs,
                () -> startGame(timeControlMs),     // NEW GAME: same mode, fresh game
                () -> returnToMenu());

        stage.setScene(gamePane.scene);
    }

    /** Stops the game screen and returns to the side menu. */
    void returnToMenu(){
        if (gamePane != null){
            gamePane.dispose();                     // stops the ticker and the clock
            gamePane = null;
        }
        if (stage != null) stage.setScene(scene);

        // Close the mode-selection screen we launched the game from.
        bacKButton();
    }
}
