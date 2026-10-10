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

        StatsStore whiteStats = new StatsStore("White");
        StatsStore blackStats = new StatsStore("Black");

        // Load WebView with HTML/JS
        javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
        webView.setPrefSize(800, 600);
        webView.setMaxSize(800, 600);
        
        try {
            java.net.URL url = getClass().getResource("/gui/resources/stats.html");
            if (url != null) {
                String htmlUrl = url.toExternalForm() + 
                    String.format("?wg=%d&ww=%d&wl=%d&wd=%d&wwr=%.1f&wacc=%.1f&bg=%d&bw=%d&bl=%d&bd=%d&bwr=%.1f&bacc=%.1f",
                        whiteStats.gamesPlayed(), whiteStats.wins(), whiteStats.losses(), whiteStats.draws(), whiteStats.winRate(), whiteStats.averageAccuracy(),
                        blackStats.gamesPlayed(), blackStats.wins(), blackStats.losses(), blackStats.draws(), blackStats.winRate(), blackStats.averageAccuracy());
                webView.getEngine().load(htmlUrl);
            }
        } catch(Exception e) { e.printStackTrace(); }

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
        VBox content = new VBox(20, webView);
        content.setAlignment(Pos.CENTER);

        root.getChildren().addAll(background, content, backButton);
        StackPane.setAlignment(backButton, Pos.TOP_LEFT);
        StackPane.setMargin(backButton, new Insets(20));

        scene = new Scene(root, cv.WIDTH, cv.HEIGHT, Color.BLACK);
    }
}
