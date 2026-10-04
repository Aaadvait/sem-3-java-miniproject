package gui.scenes.panes;

import gui.CommonValues;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.awt.*;

public class SidePane {
    CommonValues cv = new CommonValues();

    public StackPane root = new StackPane();

    VBox sidePanel = new VBox();

    // ---x--- SIDE BUTTONS ---x---
    public Button playButton = new Button("PLAY");
    public Button loadButton = new Button("LOAD");
    public Button exitButton = new Button("EXIT");

    ScaleTransition scalePlayButton = new ScaleTransition(Duration.millis(20), playButton);
    ScaleTransition scaleLoadButton = new ScaleTransition(Duration.millis(20), loadButton);
    ScaleTransition scaleExitButton = new ScaleTransition(Duration.millis(20), exitButton);

    public SidePane(){

        playButton.setMaxWidth(300);
        playButton.setPrefWidth(300);
        loadButton.setMaxWidth(300);
        loadButton.setPrefWidth(300);
        exitButton.setMaxWidth(300);
        exitButton.setPrefWidth(300);

        // NEED to do the TEXT separately as the FONTS are not on System.
        playButton.setFont(cv.VARELA_BUTTON);
        playButton.setTextFill(Color.WHITE);
        loadButton.setFont(cv.VARELA_BUTTON);
        loadButton.setTextFill(Color.WHITE);
        exitButton.setFont(cv.VARELA_BUTTON);
        exitButton.setTextFill(Color.WHITE);

        playButton.setStyle(cv.STYLE_BSP);
        loadButton.setStyle(cv.STYLE_BSP);
        exitButton.setStyle(cv.STYLE_BSP);

        playButton.setOnMouseEntered(event -> {
            scalePlayButton.setToX(1.1);
            scalePlayButton.setToY(1.1);
            scalePlayButton.stop();
            scalePlayButton.playFromStart();
        });
        playButton.setOnMouseExited(event -> {
            scalePlayButton.setToX(1.0);
            scalePlayButton.setToY(1.0);
            scalePlayButton.stop();
            scalePlayButton.playFromStart();
        });

        loadButton.setOnMouseEntered(event -> {
            scaleLoadButton.setToX(1.1);
            scaleLoadButton.setToY(1.1);
            scaleLoadButton.stop();
            scaleLoadButton.playFromStart();
        });
        loadButton.setOnMouseExited(event -> {
            scaleLoadButton.setToX(1.0);
            scaleLoadButton.setToY(1.0);
            scaleLoadButton.stop();
            scaleLoadButton.playFromStart();
        });

        exitButton.setOnMouseEntered(event -> {
            scaleExitButton.setToX(1.1);
            scaleExitButton.setToY(1.1);
            scaleExitButton.stop();
            scaleExitButton.playFromStart();
        });
        exitButton.setOnMouseExited(event -> {
            scaleExitButton.setToX(1.0);
            scaleExitButton.setToY(1.0);
            scaleExitButton.stop();
            scaleExitButton.playFromStart();
        });

        sidePanel.setPrefWidth(300);
        sidePanel.setMaxWidth(300);
        sidePanel.setPrefHeight(800);
        sidePanel.setMaxHeight(800);
        sidePanel.setSpacing(50);
        sidePanel.getChildren().addAll(playButton, exitButton);
        sidePanel.setAlignment(Pos.CENTER_LEFT);

        root.setAlignment(Pos.CENTER_LEFT);
        root.setTranslateX(-300);
        root.getChildren().add(sidePanel);
    }
}