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
    public Button playButton = new Button("NEW");
    public Button loadButton = new Button("LOAD");
    public Button exitButton = new Button("EXIT");

    ScaleTransition scalePlayButton = new ScaleTransition(Duration.millis(20), playButton);
    ScaleTransition scaleLoadButton = new ScaleTransition(Duration.millis(20), loadButton);
    ScaleTransition scaleExitButton = new ScaleTransition(Duration.millis(20), exitButton);

    public SidePane(){

        playButton.setMaxWidth(300);
        playButton.setPrefWidth(300);
        playButton.setFont(cv.NEW_ROCKER_BUTTON);
        playButton.setTextFill(Color.WHITE);
        loadButton.setMaxWidth(300);
        loadButton.setPrefWidth(300);
        loadButton.setFont(cv.NEW_ROCKER_BUTTON);
        loadButton.setTextFill(Color.WHITE);
        exitButton.setMaxWidth(300);
        exitButton.setPrefWidth(300);
        exitButton.setFont(cv.NEW_ROCKER_BUTTON);
        exitButton.setTextFill(Color.WHITE);

        playButton.setStyle(
                "-fx-background-color: #213b15;" +
                        "-fx-border-color: #182b10;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-radius: 5;"
        );
        loadButton.setStyle(
                "-fx-background-color: #213b15;" +
                        "-fx-border-color: #182b10;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-radius: 5;"
        );
        exitButton.setStyle(
                        "-fx-background-color: #213b15;" +
                        "-fx-border-color: #182b10;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 5;" +
                        "-fx-background-radius: 5;"
        );

        playButton.setOnMouseEntered(event -> {
            scalePlayButton.setToX(1.1);
            scalePlayButton.setToY(1.1);
            scalePlayButton.play();
        });
        playButton.setOnMouseExited(event -> {
            scalePlayButton.setToX(1.0);
            scalePlayButton.setToY(1.0);
            scalePlayButton.play();
        });
        loadButton.setOnMouseEntered(event -> {
            scaleLoadButton.setToX(1.1);
            scaleLoadButton.setToY(1.1);
            scaleLoadButton.play();
        });
        loadButton.setOnMouseExited(event -> {
            scaleLoadButton.setToX(1.0);
            scaleLoadButton.setToY(1.0);
            scaleLoadButton.play();
        });
        exitButton.setOnMouseEntered(event -> {
            scaleExitButton.setToX(1.1);
            scaleExitButton.setToY(1.1);
            scaleExitButton.play();
        });
        exitButton.setOnMouseExited(event -> {
            scaleExitButton.setToX(1.0);
            scaleExitButton.setToY(1.0);
            scaleExitButton.play();
        });

        sidePanel.setPrefWidth(300);
        sidePanel.setMaxWidth(300);
        sidePanel.setPrefHeight(800);
        sidePanel.setMaxHeight(800);
        sidePanel.setSpacing(50);
        sidePanel.getChildren().addAll(playButton, loadButton, exitButton);
        sidePanel.setAlignment(Pos.CENTER_LEFT);

        root.setAlignment(Pos.CENTER_LEFT);
        root.setTranslateX(-300);
        root.getChildren().add(sidePanel);
    }
}