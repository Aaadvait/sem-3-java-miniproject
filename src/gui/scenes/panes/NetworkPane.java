package gui.scenes.panes;

import gui.CommonValues;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;
import network.ChessServer;
import network.NetworkGame;
import network.NetworkProtocol;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Inet4Address;
import java.util.Enumeration;
import java.util.function.Consumer;

/**
 * LAN lobby screen.
 *
 * Lets the user either host a new game (acts as server + White player) or
 * join an existing game by entering the server IP (acts as client + Black player).
 *
 * Once both players are connected the lobby fires {@code onGameReady} with the
 * configured {@link NetworkGame} so that BaseScene can open the GamePane.
 */
public class NetworkPane {

    private final CommonValues cv = new CommonValues();

    public final StackPane root  = new StackPane();
    public final Scene     scene;

    public Button backButton = new Button();
    private NetworkGame currentNg = null;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    /**
     * @param onGameReady called (on the FX thread) with the live NetworkGame once both
     *                    players have connected
     * @param onBack      called when the user presses the back button
     * @param onError     called if the network setup fails
     */
    public NetworkPane(Consumer<NetworkGame> onGameReady, Runnable onBack,
                       Consumer<String> onError) {

        // Background.
        ImageView background = new ImageView(
                new Image(getClass().getResourceAsStream("/gui/resources/bg.png")));
        background.setPreserveRatio(false);
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());

        // Title.
        Text title = new Text("LAN MULTIPLAYER");
        title.setFont(cv.NEW_ROCKER_BIG);
        title.setFill(Color.WHITE);

        // Status label (shows IP / waiting message).
        Label statusLabel = new Label("Select HOST or JOIN");
        statusLabel.setFont(cv.VARELA_BUTTON);
        statusLabel.setTextFill(Color.web("#9fc6e0"));
        statusLabel.setWrapText(true);
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setMaxWidth(600);
        statusLabel.setMinHeight(Region.USE_PREF_SIZE);
        statusLabel.setPadding(new Insets(8, 16, 8, 16));

        // Host button.
        Button hostButton = createButton("HOST GAME");
        hostButton.setOnMouseClicked(e -> {
            hostButton.setDisable(true);
            statusLabel.setText("Starting server…");
            // Create a placeholder game (mode will be chosen in-game via mode screen too;
            // for LAN we always start untimed by default — the players agree via chat).
            game.Game g = game.Game.fromMode(game.GameMode.UNTIMED);
            currentNg = NetworkGame.host("White", g,
                    null,
                    err  -> { 
                        statusLabel.setText("Error: " + err); 
                        hostButton.setDisable(false);
                        if (onError != null) onError.accept(err); 
                    });

            NetworkGame establishedGame = currentNg;
            establishedGame.setOnStart(() -> {
                statusLabel.setText("Both players connected! Starting…");
                javafx.application.Platform.runLater(() -> {
                    if (currentNg == establishedGame) onGameReady.accept(establishedGame);
                });
            });

            // Do not publish the host address until the host's own client has connected.
            // The server assigns White to its first socket; this prevents a fast guest from
            // winning the accept race and being assigned the host's color.
            establishedGame.setOnConnected(() -> new Thread(() -> {
                try {
                    String ip = discoverLanIPv4();
                    javafx.application.Platform.runLater(() -> {
                        if (currentNg == establishedGame) {
                            statusLabel.setText("Server ready.\nYour IP: " + ip
                                    + "\nPort: " + NetworkProtocol.DEFAULT_PORT
                                    + "\nWaiting for opponent…");
                        }
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        if (currentNg == establishedGame)
                            statusLabel.setText("Host connected. Find your LAN IPv4 address in Windows Network Settings.");
                    });
                }
            }, "chess-host-ip").start());
        });

        // IP entry + join section.
        TextField ipField = new TextField();
        ipField.setPromptText("Enter server IP (e.g. 192.168.1.10)");
        ipField.setStyle(
                "-fx-background-color: #06182bdd;" +
                "-fx-text-fill: white;" +
                "-fx-border-color: #cceeee;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 5;" +
                "-fx-background-radius: 5;");
        ipField.setPrefWidth(340);
        ipField.setMinWidth(260);
        ipField.setFont(cv.VARELA_BUTTON);
        ipField.setAlignment(Pos.CENTER_LEFT);

        Button joinButton = createButton("JOIN GAME");
        joinButton.setOnMouseClicked(e -> {
            String ip = ipField.getText().trim();
            if (ip.isEmpty()) { statusLabel.setText("Please enter the server IP address."); return; }
            joinButton.setDisable(true);
            statusLabel.setText("Connecting to " + ip + "…");

            game.Game g = game.Game.fromMode(game.GameMode.UNTIMED);
            currentNg = NetworkGame.join(ip, "Black", g,
                    null,
                    err  -> { 
                        statusLabel.setText("Error: " + err); 
                        joinButton.setDisable(false);
                        if (onError != null) onError.accept(err); 
                    });

            NetworkGame establishedGame = currentNg;
            establishedGame.setOnConnected(() -> {
                if (currentNg == establishedGame) statusLabel.setText("Connected to host. Waiting for game to start…");
            });
            establishedGame.setOnStart(() -> {
                statusLabel.setText("Connected! Starting game…");
                javafx.application.Platform.runLater(() -> {
                    if (currentNg == establishedGame) onGameReady.accept(establishedGame);
                });
            });
        });

        HBox joinRow = new HBox(16, ipField, joinButton);
        joinRow.setAlignment(Pos.CENTER);
        joinRow.setMaxWidth(620);
        HBox.setHgrow(ipField, Priority.ALWAYS);

        // Back button.
        ImageView backIcon = new ImageView(
                new Image(getClass().getResourceAsStream("/gui/resources/back.png")));
        backIcon.setFitWidth(40);
        backIcon.setFitHeight(40);
        backButton.setGraphic(backIcon);
        backButton.setStyle(cv.STYLE_BBMP);
        backButton.setOnMouseClicked(ev -> {
            if (currentNg != null) {
                currentNg.shutdown();
                currentNg = null;
            }
            onBack.run();
        });
        ScaleTransition scaleBack = new ScaleTransition(Duration.millis(20), backButton);
        backButton.setOnMouseEntered(ev -> { scaleBack.setToX(1.2); scaleBack.setToY(1.2); scaleBack.stop(); scaleBack.playFromStart(); });
        backButton.setOnMouseExited( ev -> { scaleBack.setToX(1.0); scaleBack.setToY(1.0); scaleBack.stop(); scaleBack.playFromStart(); });

        // Layout.
        VBox content = new VBox(28, title, hostButton, new Separator(), joinRow, statusLabel);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(28));
        content.setMaxWidth(760);

        root.getChildren().addAll(background, content, backButton);
        StackPane.setAlignment(backButton, Pos.TOP_LEFT);
        StackPane.setMargin(backButton, new Insets(20));

        scene = new Scene(root, cv.WIDTH, cv.HEIGHT, Color.BLACK);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Button createButton(String text) {
        Button b = new Button(text);
        b.setFont(cv.VARELA_BUTTON);
        b.setTextFill(Color.WHITE);
        b.setStyle(cv.STYLE_BSP);
        b.setMinSize(240, 60);
        b.setPrefSize(260, 64);
        b.setMaxWidth(300);
        b.setWrapText(true);
        b.setAlignment(Pos.CENTER);
        b.setPadding(new Insets(10, 16, 10, 16));
        ScaleTransition scale = new ScaleTransition(Duration.millis(20), b);
        b.setOnMouseEntered(e -> { scale.setToX(1.1); scale.setToY(1.1); scale.stop(); scale.playFromStart(); });
        b.setOnMouseExited( e -> { scale.setToX(1.0); scale.setToY(1.0); scale.stop(); scale.playFromStart(); });
        return b;
    }

    /** Prefer a usable LAN IPv4 address over localhost or an IPv6 address. */
    private static String discoverLanIPv4() throws Exception {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces != null && interfaces.hasMoreElements()) {
            NetworkInterface network = interfaces.nextElement();
            if (!network.isUp() || network.isLoopback() || network.isVirtual()) continue;
            Enumeration<InetAddress> addresses = network.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (address instanceof Inet4Address && !address.isLoopbackAddress()
                        && !address.isLinkLocalAddress()) return address.getHostAddress();
            }
        }
        return InetAddress.getLocalHost().getHostAddress();
    }

    /** Simple horizontal separator. */
    private static class Separator extends javafx.scene.control.Separator {
        Separator() {
            setStyle("-fx-background-color: #35597a;");
            setPrefWidth(600);
        }
    }
}
