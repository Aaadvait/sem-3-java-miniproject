package gui.scenes.panes;

import analytics.StatsStore;
import gui.CommonValues;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

/**
 * Statistics screen: displays per-player statistics loaded from disk.
 *
 * Shows wins, losses, draws, win rate and average move accuracy for both
 * White and Black player keys ("white" / "black" by default).
 *
 * Visual style is consistent with the rest of the application (same panel
 * background, same fonts, same button hover animation).
 */
public class StatsPane {

    private final CommonValues cv = new CommonValues();

    public final StackPane root = new StackPane();
    public final Scene     scene;

    // --- Back button ---
    public Button backButton = new Button();

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    public StatsPane(Runnable onBack) {

        // Background.
        ImageView background = new ImageView(
                new Image(getClass().getResourceAsStream("/gui/resources/bg.png")));
        background.setPreserveRatio(false);
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());

        // Title.
        Text title = new Text("STATISTICS");
        title.setFont(cv.NEW_ROCKER_BIG);
        title.setFill(Color.WHITE);

        // Cards for White and Black.
        VBox whiteCard = buildPlayerCard("White", new StatsStore("White"));
        VBox blackCard = buildPlayerCard("Black", new StatsStore("Black"));

        HBox cardsRow = new HBox(60, whiteCard, blackCard);
        cardsRow.setAlignment(Pos.CENTER);

        // Back button.
        ImageView backIcon = new ImageView(
                new Image(getClass().getResourceAsStream("/gui/resources/back.png")));
        backIcon.setFitWidth(40);
        backIcon.setFitHeight(40);
        backButton.setGraphic(backIcon);
        backButton.setStyle(cv.STYLE_BBMP);
        backButton.setOnMouseClicked(e -> onBack.run());
        ScaleTransition scaleBack = new ScaleTransition(Duration.millis(20), backButton);
        backButton.setOnMouseEntered(e -> { scaleBack.setToX(1.2); scaleBack.setToY(1.2); scaleBack.stop(); scaleBack.playFromStart(); });
        backButton.setOnMouseExited( e -> { scaleBack.setToX(1.0); scaleBack.setToY(1.0); scaleBack.stop(); scaleBack.playFromStart(); });

        // Layout.
        VBox content = new VBox(40, title, cardsRow);
        content.setAlignment(Pos.CENTER);

        root.getChildren().addAll(background, content, backButton);
        StackPane.setAlignment(backButton, Pos.TOP_LEFT);
        StackPane.setMargin(backButton, new Insets(20));

        scene = new Scene(root, cv.WIDTH, cv.HEIGHT, Color.BLACK);
    }

    // =========================================================================
    // CARD BUILDER
    // =========================================================================

    private VBox buildPlayerCard(String playerName, StatsStore stats) {
        Label nameLabel = new Label(playerName.toUpperCase());
        nameLabel.setFont(cv.NEW_ROCKER_MEDIUM);
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setAlignment(Pos.CENTER);

        Label gamesLabel    = statLine("Games Played", stats.gamesPlayed());
        Label winsLabel     = statLine("Wins",         stats.wins());
        Label lossesLabel   = statLine("Losses",       stats.losses());
        Label drawsLabel    = statLine("Draws",        stats.draws());
        Label winRateLabel  = statLineD("Win Rate",    stats.winRate(), "%");
        Label accuracyLabel = statLineD("Avg Accuracy", stats.averageAccuracy(), "%");

        VBox card = new VBox(12, nameLabel, gamesLabel, winsLabel,
                lossesLabel, drawsLabel, winRateLabel, accuracyLabel);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(30));
        card.setStyle(
                "-fx-background-color: #06182bdd;" +
                "-fx-border-color: #cceeee;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;");
        card.setMinWidth(320);
        return card;
    }

    private Label statLine(String name, int value) {
        Label l = new Label(name + ":  " + value);
        l.setFont(cv.VARELA_BUTTON);
        l.setTextFill(Color.WHITE);
        return l;
    }

    private Label statLineD(String name, double value, String unit) {
        Label l = new Label(String.format("%s:  %.1f%s", name, value, unit));
        l.setFont(cv.VARELA_BUTTON);
        l.setTextFill(Color.web("#9fc6e0"));
        return l;
    }
}
