package gui.scenes.panes;

import gui.CommonValues;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * The main menu side panel.
 *
 * Identical visual appearance to the original (same style, same fonts,
 * same hover animation). Extended: the LOAD button and a new STATS button
 * are now wired (no longer dead) via callbacks set by {@link gui.scenes.BaseScene}.
 */
public class SidePane {
    CommonValues cv = new CommonValues();

    public StackPane root = new StackPane();

    VBox sidePanel = new VBox();

    // ---x--- SIDE BUTTONS ---x---
    public Button playButton  = new Button("PLAY");
    public Button loadButton  = new Button("LOAD");
    public Button statsButton = new Button("STATS");
    public Button exitButton  = new Button("EXIT");

    ScaleTransition scalePlayButton  = new ScaleTransition(Duration.millis(20), playButton);
    ScaleTransition scaleLoadButton  = new ScaleTransition(Duration.millis(20), loadButton);
    ScaleTransition scaleStatsButton = new ScaleTransition(Duration.millis(20), statsButton);
    ScaleTransition scaleExitButton  = new ScaleTransition(Duration.millis(20), exitButton);

    public SidePane() {
        styleButton(playButton,  scalePlayButton);
        styleButton(loadButton,  scaleLoadButton);
        styleButton(statsButton, scaleStatsButton);
        styleButton(exitButton,  scaleExitButton);

        sidePanel.setPrefWidth(300);
        sidePanel.setMaxWidth(300);
        sidePanel.setPrefHeight(800);
        sidePanel.setMaxHeight(800);
        sidePanel.setSpacing(40);
        sidePanel.getChildren().addAll(playButton, loadButton, statsButton, exitButton);
        sidePanel.setAlignment(Pos.CENTER_LEFT);

        root.setAlignment(Pos.CENTER_LEFT);
        root.setTranslateX(-300);
        root.getChildren().add(sidePanel);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void styleButton(Button button, ScaleTransition scale) {
        button.setMaxWidth(300);
        button.setPrefWidth(300);
        button.setFont(cv.VARELA_BUTTON);
        button.setTextFill(Color.WHITE);
        button.setStyle(cv.STYLE_BSP);
        button.setOnMouseEntered(event -> {
            scale.setToX(1.1); scale.setToY(1.1);
            scale.stop(); scale.playFromStart();
        });
        button.setOnMouseExited(event -> {
            scale.setToX(1.0); scale.setToY(1.0);
            scale.stop(); scale.playFromStart();
        });
    }
}