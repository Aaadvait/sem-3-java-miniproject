package gui.scenes.panes;

import game.Game;
import game.GameMode;
import gui.CommonValues;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import network.NetworkGame;
import network.NetworkProtocol;
import network.OnlineServer;

import java.io.IOException;
import java.util.Locale;
import java.util.function.Consumer;

/** Cross-network Online Play lobby. Requires a reachable relay server for internet play. */
public final class OnlinePane {
    private final CommonValues cv = new CommonValues();
    public final StackPane root = new StackPane();
    public final Scene scene;
    private NetworkGame currentGame;
    private final Consumer<NetworkGame> onGameReady;
    private final Consumer<String> onError;
    private final Label status = new Label("Enter a server address, or start a local test server.");
    private final Button hostButton = button("CREATE ROOM");
    private final Button joinButton = button("JOIN ROOM");
    private final TextField nameField = field("Your player name");
    private final TextField serverField = field("Public server hostname or IP");
    private final TextField portField = field("Server port");
    private final TextField roomField = field("Room code from your friend");

    public OnlinePane(Consumer<NetworkGame> onGameReady, Runnable onBack, Consumer<String> onError) {
        this.onGameReady = onGameReady;
        this.onError = onError;
        ImageView bg = new ImageView(new Image(getClass().getResourceAsStream("/gui/resources/bg.png")));
        bg.setPreserveRatio(false);
        bg.fitWidthProperty().bind(root.widthProperty());
        bg.fitHeightProperty().bind(root.heightProperty());

        Text title = new Text("ONLINE PLAY");
        title.setFont(cv.NEW_ROCKER_BIG);
        title.setFill(Color.WHITE);
        Label hint = new Label("Play with a friend on any network using a private room code.");
        hint.setTextFill(Color.web("#c6e7f7"));
        hint.setFont(cv.VARELA_BUTTON);
        hint.setWrapText(true);
        hint.setAlignment(Pos.CENTER);
        hint.setMaxWidth(650);

        nameField.setText(System.getProperty("user.name", "Player"));
        portField.setText(Integer.toString(OnlineServer.DEFAULT_PORT));
        nameField.setPromptText("Player name");
        serverField.setPromptText("Public server hostname / IP (not your name)");
        roomField.setPromptText("Six-character room code");
        roomField.setMaxWidth(420);

        VBox fields = new VBox(10,
                labeledField("PLAYER NAME", nameField),
                labeledField("SERVER HOST / PUBLIC IP", serverField),
                labeledField("SERVER PORT", portField));
        fields.setAlignment(Pos.CENTER);
        fields.setMaxWidth(500);

        hostButton.setOnAction(e -> connect(true));
        joinButton.setOnAction(e -> {
            String code = roomField.getText().trim().toUpperCase(Locale.ROOT);
            if (!code.matches("[A-HJ-NP-Z2-9]{6}")) {
                status.setText("Enter the six-character room code supplied by the host.");
                return;
            }
            connect(false);
        });

        Button localServerButton = button("START LOCAL TEST SERVER");
        localServerButton.setMinSize(280, 48);
        localServerButton.setPrefSize(300, 50);
        localServerButton.setOnAction(e -> {
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
                if (port < 1 || port > 65535) throw new NumberFormatException();
                OnlineServer.startInBackground(port);
                serverField.setText("127.0.0.1");
                status.setText("Local test server is running on this computer. Use CREATE ROOM here to test. " +
                        "For different networks, deploy OnlineServer publicly and enter its public host/IP.");
            } catch (NumberFormatException ex) {
                status.setText("Port must be a number between 1 and 65535.");
            } catch (IOException ex) {
                status.setText("Could not start local server: " + ex.getMessage() +
                        ". The port may already be in use.");
            }
        });

        HBox buttons = new HBox(16, hostButton, joinButton);
        buttons.setAlignment(Pos.CENTER);
        status.setFont(Font.font("Varela Round", 16));
        status.setTextFill(Color.web("#a9efff"));
        status.setWrapText(true);
        status.setAlignment(Pos.CENTER);
        status.setMaxWidth(720);
        status.setMinHeight(Region.USE_PREF_SIZE);
        status.setPadding(new Insets(8));

        VBox content = new VBox(16, title, hint, fields, new Separator(), buttons,
                labeledField("ROOM CODE (JOIN ONLY)", roomField), localServerButton, status);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24));
        content.setMaxWidth(780);

        Button back = new Button("‹ BACK");
        back.setStyle(cv.STYLE_BBMP);
        back.setTextFill(Color.WHITE);
        back.setFont(cv.VARELA_BUTTON);
        back.setOnAction(e -> {
            if (currentGame != null) currentGame.shutdown();
            onBack.run();
        });
        root.getChildren().addAll(bg, content, back);
        StackPane.setAlignment(back, Pos.TOP_LEFT);
        StackPane.setMargin(back, new Insets(20));
        scene = new Scene(root, cv.WIDTH, cv.HEIGHT, Color.BLACK);
    }

    private VBox labeledField(String labelText, TextField field) {
        Label label = new Label(labelText);
        label.setFont(Font.font("Varela Round", 13));
        label.setTextFill(Color.web("#d5f3ff"));
        VBox box = new VBox(4, label, field);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void connect(boolean create) {
        String host = serverField.getText().trim();
        if (host.isEmpty() || host.contains(" ")) {
            status.setText("Enter the SERVER HOST / PUBLIC IP field. A player name is not a server address. " +
                    "Use START LOCAL TEST SERVER to test on this computer.");
            serverField.requestFocus();
            return;
        }
        String playerName = nameField.getText().trim();
        if (playerName.isEmpty()) playerName = "Player";
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
            if (port < 1 || port > 65535) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            status.setText("Port must be a number between 1 and 65535.");
            return;
        }

        hostButton.setDisable(true);
        joinButton.setDisable(true);
        status.setText(create ? "Connecting to server and creating room…" : "Connecting and joining room…");
        Game game = Game.fromMode(GameMode.UNTIMED);
        String roomCode = create ? "" : roomField.getText().trim().toUpperCase(Locale.ROOT);
        currentGame = NetworkGame.online(host, port, roomCode, create, playerName, game, null, message -> {
            status.setText("Online connection failed: " + message +
                    "\nCheck that OnlineServer is running and the host/port are reachable.");
            hostButton.setDisable(false);
            joinButton.setDisable(false);
            if (this.onError != null) this.onError.accept(message);
        });
        NetworkGame ng = currentGame;
        ng.setOnRoomCode(code -> status.setText("ROOM CREATED: " + code +
                "\nShare this code with your friend. Keep this window open while they join."));
        ng.setOnConnected(() -> status.setText("Connected to " + host + ":" + port + ". Setting up room…"));
        ng.setOnStart(() -> {
            status.setText("Opponent connected. Starting game…");
            javafx.application.Platform.runLater(() -> {
                if (currentGame == ng) onGameReady.accept(ng);
            });
        });
    }

    private static TextField field(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-background-color: #06182bdd; -fx-text-fill: white; -fx-prompt-text-fill: #9fc6e0; -fx-border-color: #8dddf4; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
        f.setFont(Font.font("Varela Round", 16));
        f.setMaxWidth(Double.MAX_VALUE);
        f.setPrefHeight(44);
        return f;
    }

    private static Button button(String text) {
        Button b = new Button(text);
        b.setFont(Font.font("Varela Round", 16));
        b.setTextFill(Color.WHITE);
        b.setStyle("-fx-background-color: #087fc0; -fx-border-color: #bdefff; -fx-border-width: 2; -fx-background-radius: 7; -fx-border-radius: 7;");
        b.setMinSize(190, 56);
        b.setPrefSize(210, 58);
        b.setMaxWidth(320);
        b.setWrapText(false);
        b.setEllipsisString("");
        b.setAlignment(Pos.CENTER);
        b.setPadding(new Insets(8, 10, 8, 10));
        ScaleTransition scale = new ScaleTransition(Duration.millis(100), b);
        b.setOnMouseEntered(e -> { if (!b.isDisabled()) { scale.setToX(1.04); scale.setToY(1.04); scale.playFromStart(); } });
        b.setOnMouseExited(e -> { scale.setToX(1); scale.setToY(1); scale.playFromStart(); });
        return b;
    }
}
