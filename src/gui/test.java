package gui;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class test extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Create UI components
        Label messageLabel = new Label("Welcome to JavaFX!");
        Button actionButton = new Button("Click Me");

        // Add event handling
        actionButton.setOnAction(event -> messageLabel.setText("Button was clicked!"));

        // Arrange components in a layout container
        VBox root = new VBox(15); // 15px spacing between elements
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(messageLabel, actionButton);

        // Configure the scene and stage
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("JavaFX Sample Application");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void startapp(String args[]) {
        launch(args);
    }
}
