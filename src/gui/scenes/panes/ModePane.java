package gui.scenes.panes;

import gui.CommonValues;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import ai.AILevel;
import game.GameMode;

/**
 * The game-mode selection screen.
 *
 * Extended from the original to support:
 *   – All time controls (Bullet / Blitz / Rapid / Classical / Untimed)
 *   – AI difficulty selection (shown when "VS AI" is chosen)
 *
 * Visual style is identical to the original (same button styles, same fonts,
 * same hover-scale animation).
 */
public class ModePane {
    CommonValues cv = new CommonValues();

    public StackPane root = new StackPane();

    // ---x--- BACK BUTTON ---x--- //
    public Button backButton = new Button();
    Image backButtonImg      = new Image(getClass().getResourceAsStream("/gui/resources/back.png"));
    ImageView backButtonIcon = new ImageView(backButtonImg);
    ScaleTransition scaleBackButton       = new ScaleTransition(Duration.millis(20), backButton);
    public TranslateTransition moveBackButton = new TranslateTransition(Duration.millis(500), backButton);

    // ---x--- OPPONENT TYPE ROW (LOCAL / VS AI / LAN) ---x--- //
    private final HBox opponentRow  = new HBox(30);
    public  Button localButton      = createModeButton("LOCAL");
    public  Button vsAIButton       = createModeButton("VS AI");
    public  Button lanButton        = createModeButton("PLAY LAN");

    // ---x--- TIME CONTROL ROW ---x--- //
    private final VBox timeSection  = new VBox(12);
    private final Label timeTitle   = new Label("SELECT TIME CONTROL");
    private final HBox bulletRow    = new HBox(20);
    private final HBox blitzRow     = new HBox(20);
    private final HBox rapidRow     = new HBox(20);
    private final HBox classicalRow = new HBox(20);

    public Button bullet1Button     = createSmallButton("1+0 Bullet");
    public Button bullet2Button     = createSmallButton("2+1 Bullet");
    public Button blitz3Button      = createSmallButton("3+0 Blitz");
    public Button blitz5Button      = createSmallButton("5+0 Blitz");
    public Button rapid10Button     = createSmallButton("10+0 Rapid");
    public Button rapid15Button     = createSmallButton("15+10 Rapid");
    public Button rapid30Button     = createSmallButton("30+0 Rapid");
    public Button classicalButton   = createSmallButton("60+30 Classical");
    public Button untimedButton     = createSmallButton("Untimed");

    // ---x--- AI DIFFICULTY ROW (shown only when VS AI selected) ---x--- //
    private final VBox aiSection    = new VBox(12);
    private final Label aiTitle     = new Label("SELECT AI DIFFICULTY");
    private final HBox aiRow        = new HBox(20);
    public Button beginnerButton    = createSmallButton("Beginner");
    public Button easyButton        = createSmallButton("Easy");
    public Button mediumButton      = createSmallButton("Medium");
    public Button hardButton        = createSmallButton("Hard");
    public Button expertButton      = createSmallButton("Expert");

    // ---x--- MAIN PANEL (slides in from above, identical to original) ---x--- //
    private final VBox mainPanel    = new VBox(24);
    public TranslateTransition moveModePanel = new TranslateTransition(Duration.millis(500), mainPanel);

    // --- State ---
    /** The mode the user has selected; null until confirmed. */
    public GameMode  selectedMode  = null;
    public AILevel   selectedLevel = null;
    public boolean   isVsAI       = false;
    public boolean   isLAN        = false;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    public ModePane() {

        // Back button.
        backButtonIcon.setFitWidth(40);
        backButtonIcon.setFitHeight(40);
        backButton.setTranslateY(-100);
        backButton.setGraphic(backButtonIcon);
        backButton.setStyle(cv.STYLE_BBMP);
        backButton.setOnMouseEntered(e -> { scaleBackButton.setToX(1.2); scaleBackButton.setToY(1.2); scaleBackButton.stop(); scaleBackButton.playFromStart(); });
        backButton.setOnMouseExited( e -> { scaleBackButton.setToX(1.0); scaleBackButton.setToY(1.0); scaleBackButton.stop(); scaleBackButton.playFromStart(); });

        // Section titles.
        styleTitle(timeTitle);
        styleTitle(aiTitle);

        // Opponent row.
        opponentRow.setAlignment(Pos.CENTER);
        opponentRow.getChildren().addAll(localButton, vsAIButton, lanButton);

        // Time control rows.
        bulletRow.setAlignment(Pos.CENTER);
        bulletRow.getChildren().addAll(bullet1Button, bullet2Button);

        blitzRow.setAlignment(Pos.CENTER);
        blitzRow.getChildren().addAll(blitz3Button, blitz5Button);

        rapidRow.setAlignment(Pos.CENTER);
        rapidRow.getChildren().addAll(rapid10Button, rapid15Button, rapid30Button);

        classicalRow.setAlignment(Pos.CENTER);
        classicalRow.getChildren().addAll(classicalButton, untimedButton);

        timeSection.setAlignment(Pos.CENTER);
        timeSection.getChildren().addAll(timeTitle, bulletRow, blitzRow, rapidRow, classicalRow);

        // AI difficulty row.
        aiRow.setAlignment(Pos.CENTER);
        aiRow.getChildren().addAll(beginnerButton, easyButton, mediumButton, hardButton, expertButton);
        aiSection.setAlignment(Pos.CENTER);
        aiSection.getChildren().addAll(aiTitle, aiRow);
        aiSection.setVisible(false);
        aiSection.setManaged(false);

        // Main panel.
        mainPanel.setAlignment(Pos.CENTER);
        mainPanel.setPadding(new Insets(20));
        mainPanel.setTranslateY(-700);   // starts above the window (same as original -540, enlarged for more rows)
        mainPanel.getChildren().addAll(opponentRow, timeSection, aiSection);

        // Wire opponent-type buttons.
        localButton.setOnMouseClicked(e -> selectOpponentType(false, false));
        vsAIButton .setOnMouseClicked(e -> selectOpponentType(true,  false));
        lanButton  .setOnMouseClicked(e -> selectOpponentType(false, true));

        // Wire time-control buttons.
        bullet1Button  .setOnMouseClicked(e -> selectMode(GameMode.BULLET_1));
        bullet2Button  .setOnMouseClicked(e -> selectMode(GameMode.BULLET_2));
        blitz3Button   .setOnMouseClicked(e -> selectMode(GameMode.BLITZ_3));
        blitz5Button   .setOnMouseClicked(e -> selectMode(GameMode.BLITZ_5));
        rapid10Button  .setOnMouseClicked(e -> selectMode(GameMode.RAPID_10));
        rapid15Button  .setOnMouseClicked(e -> selectMode(GameMode.RAPID_15_10));
        rapid30Button  .setOnMouseClicked(e -> selectMode(GameMode.RAPID_30));
        classicalButton.setOnMouseClicked(e -> selectMode(GameMode.CLASSICAL));
        untimedButton  .setOnMouseClicked(e -> selectMode(GameMode.UNTIMED));

        // Wire AI difficulty buttons.
        beginnerButton.setOnMouseClicked(e -> selectAILevel(AILevel.BEGINNER));
        easyButton    .setOnMouseClicked(e -> selectAILevel(AILevel.EASY));
        mediumButton  .setOnMouseClicked(e -> selectAILevel(AILevel.MEDIUM));
        hardButton    .setOnMouseClicked(e -> selectAILevel(AILevel.HARD));
        expertButton  .setOnMouseClicked(e -> selectAILevel(AILevel.EXPERT));

        // Assemble root.
        root.setMaxWidth(cv.WIDTH);
        root.setMaxHeight(cv.HEIGHT);
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(mainPanel, backButton);
        StackPane.setAlignment(backButton, Pos.TOP_LEFT);
        StackPane.setMargin(backButton, new Insets(20));
    }

    // =========================================================================
    // STATE LOGIC
    // =========================================================================

    public void selectOpponentType(boolean vsAI, boolean lan) {
        this.isVsAI = vsAI;
        this.isLAN  = lan;

        // Highlight selected button.
        resetHighlight(localButton);
        resetHighlight(vsAIButton);
        resetHighlight(lanButton);
        if (vsAI) highlightButton(vsAIButton);
        else if (lan) highlightButton(lanButton);
        else highlightButton(localButton);

        // Show / hide AI difficulty section.
        aiSection.setVisible(vsAI);
        aiSection.setManaged(vsAI);

        // Reset selections.
        selectedMode  = null;
        selectedLevel = null;
    }

    public void selectMode(GameMode mode) {
        this.selectedMode = mode;
        resetAllModeHighlights();
        highlightModeButton(mode);
        // If VS AI: wait for AI level. Otherwise: game can start.
    }

    public void selectAILevel(AILevel level) {
        this.selectedLevel = level;
        resetAIHighlights();
        highlightAIButton(level);
    }

    // =========================================================================
    // HIGHLIGHT HELPERS
    // =========================================================================

    private void resetAllModeHighlights() {
        for (Button b : new Button[]{ bullet1Button, bullet2Button, blitz3Button,
                blitz5Button, rapid10Button, rapid15Button, rapid30Button,
                classicalButton, untimedButton }) {
            b.setStyle(cv.STYLE_BMP);
        }
    }

    private void resetAIHighlights() {
        for (Button b : new Button[]{ beginnerButton, easyButton, mediumButton, hardButton, expertButton }) {
            b.setStyle(cv.STYLE_BMP);
        }
    }

    private void highlightModeButton(GameMode mode) {
        Button b = modeToButton(mode);
        if (b != null) highlightButton(b);
    }

    private void highlightAIButton(AILevel level) {
        Button b = levelToButton(level);
        if (b != null) highlightButton(b);
    }

    private void highlightButton(Button b) {
        b.setStyle(
            "-fx-background-color: #00ccff;" +
            "-fx-border-color: #ffffff;" +
            "-fx-border-width: 3;" +
            "-fx-border-radius: 5;" +
            "-fx-background-radius: 5;");
    }

    private void resetHighlight(Button b) {
        b.setStyle(cv.STYLE_BMP);
    }

    private Button modeToButton(GameMode mode) {
        switch (mode) {
            case BULLET_1:    return bullet1Button;
            case BULLET_2:    return bullet2Button;
            case BLITZ_3:     return blitz3Button;
            case BLITZ_5:     return blitz5Button;
            case RAPID_10:    return rapid10Button;
            case RAPID_15_10: return rapid15Button;
            case RAPID_30:    return rapid30Button;
            case CLASSICAL:   return classicalButton;
            case UNTIMED:     return untimedButton;
            default:          return null;
        }
    }

    private Button levelToButton(AILevel level) {
        switch (level) {
            case BEGINNER: return beginnerButton;
            case EASY:     return easyButton;
            case MEDIUM:   return mediumButton;
            case HARD:     return hardButton;
            case EXPERT:   return expertButton;
            default:       return null;
        }
    }

    // =========================================================================
    // BUTTON FACTORIES (identical animation to original)
    // =========================================================================

    /** Large opponent-type button (same look as original mode buttons). */
    private Button createModeButton(String text) {
        Button button = new Button(text);
        button.setAlignment(Pos.CENTER);
        button.setPrefSize(160, 150);
        button.setMinSize(160, 150);
        button.setMaxSize(160, 150);
        button.setFont(cv.VARELA_BUTTON);
        button.setTextFill(Color.WHITE);
        button.setStyle(cv.STYLE_BMP);
        button.setWrapText(true);
        ScaleTransition scale = new ScaleTransition(Duration.millis(20), button);
        button.setOnMouseEntered(e -> { scale.setToX(1.1); scale.setToY(1.1); scale.stop(); scale.playFromStart(); });
        button.setOnMouseExited( e -> { scale.setToX(1.0); scale.setToY(1.0); scale.stop(); scale.playFromStart(); });
        return button;
    }

    /** Compact time-control / AI-level button. */
    private Button createSmallButton(String text) {
        Button button = new Button(text);
        button.setAlignment(Pos.CENTER);
        button.setPrefSize(180, 60);
        button.setMinSize(180, 60);
        button.setMaxSize(180, 60);
        button.setFont(cv.VARELA_BUTTON);
        button.setTextFill(Color.WHITE);
        button.setStyle(cv.STYLE_BMP);
        button.setWrapText(true);
        ScaleTransition scale = new ScaleTransition(Duration.millis(20), button);
        button.setOnMouseEntered(e -> { scale.setToX(1.08); scale.setToY(1.08); scale.stop(); scale.playFromStart(); });
        button.setOnMouseExited( e -> { scale.setToX(1.00); scale.setToY(1.00); scale.stop(); scale.playFromStart(); });
        return button;
    }

    private void styleTitle(Label l) {
        l.setFont(cv.VARELA_BUTTON);
        l.setTextFill(Color.web("#9fc6e0"));
    }
}
