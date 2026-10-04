package gui.scenes.panes;

import gui.CommonValues;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * The game-mode selection screen: DEFAULT (no timer), 5 MIN and 10 MIN.
 * Same button style and hover animation as the side menu.
 */
public class ModePane {
    CommonValues cv = new CommonValues();

    public StackPane root = new StackPane();

    // ---x--- BACK BUTTON ---x--- //
    public Button backButton = new Button();
    Image backButtonImg = new Image(getClass().getResourceAsStream("/gui/resources/back.png"));
    ImageView backButtonIcon = new ImageView(backButtonImg);
    ScaleTransition scaleBackButton = new ScaleTransition(Duration.millis(20), backButton);
    public TranslateTransition moveBackButton = new TranslateTransition(Duration.millis(500), backButton);

    // ---x--- MODE BUTTONS ---x--- //
    HBox modePanel = new HBox();

    public Button defaultModeButton = createModeButton("DEFAULT");
    public Button fiveMinModeButton  = createModeButton("5 MIN");
    public Button tenMinModeButton   = createModeButton("10 MIN");

    public TranslateTransition moveModePanel = new TranslateTransition(Duration.millis(500), modePanel);

    public ModePane() {

        backButtonIcon.setFitWidth(40);
        backButtonIcon.setFitHeight(40);
        backButton.setTranslateY(-100);

        backButton.setGraphic(backButtonIcon);
        backButton.setStyle(cv.STYLE_BBMP);

        backButton.setOnMouseEntered(event -> {
            scaleBackButton.setToX(1.2);
            scaleBackButton.setToY(1.2);
            scaleBackButton.stop();
            scaleBackButton.playFromStart();
        });
        backButton.setOnMouseExited(event -> {
            scaleBackButton.setToX(1.0);
            scaleBackButton.setToY(1.0);
            scaleBackButton.stop();
            scaleBackButton.playFromStart();
        });

        modePanel.setSpacing(40);
        modePanel.setAlignment(Pos.CENTER);
        modePanel.setTranslateY(-540);   // fully above the window until PLAY is pressed
        modePanel.getChildren().addAll(defaultModeButton, fiveMinModeButton, tenMinModeButton);

        root.setMaxWidth(cv.WIDTH);
        root.setMaxHeight(cv.HEIGHT);
        root.setAlignment(Pos.CENTER);

        root.getChildren().add(modePanel);
        root.getChildren().add(backButton);
        StackPane.setAlignment(backButton, Pos.TOP_LEFT);
        StackPane.setMargin(backButton, new Insets(20));
    }

    /** Builds a mode button in the same style (and with the same hover animation) as the side menu buttons. */
    private Button createModeButton(String text) {
        Button button = new Button(text);

        button.setAlignment(Pos.CENTER);
        button.setPrefWidth(150);
        button.setMaxWidth(150);
        button.setMinWidth(150);
        button.setPrefHeight(150);
        button.setMaxHeight(150);
        button.setMinHeight(150);

        button.setFont(cv.VARELA_BUTTON);
        button.setTextFill(Color.WHITE);
        button.setStyle(cv.STYLE_BMP);

        ScaleTransition scale = new ScaleTransition(Duration.millis(20), button);
        button.setOnMouseEntered(event -> {
            scale.setToX(1.1);
            scale.setToY(1.1);
            scale.stop();
            scale.playFromStart();
        });
        button.setOnMouseExited(event -> {
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.stop();
            scale.playFromStart();
        });

        return button;
    }
}
