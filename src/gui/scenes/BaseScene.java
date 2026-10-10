package gui.scenes;

import ai.AILevel;
import game.Game;
import game.GameMode;
import gui.CommonValues;
import gui.scenes.panes.*;
import gui.scenes.panes.GamePane;
import network.NetworkGame;
import persistence.GameSaver;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import move.Move;
import persistence.PGNHandler;

/**
 * Root scene of the application.
 *
 * Manages the main menu (IntroPane → SidePane → ModePane) and owns the
 * transition animations. Extended from the original to wire:
 *   – LOAD button (open file-load dialog, replay a saved PGN)
 *   – STATS button (navigate to StatsPane)
 *   – VS AI game mode
 *   – LAN multiplayer lobby
 *   – All new time controls (Bullet / Blitz / Rapid / Classical)
 *
 * All original animation code is preserved exactly.
 */
public class BaseScene {
    CommonValues cv = new CommonValues();

    // INTRO PANE
    public IntroPane introPane = new IntroPane();
    TranslateTransition moveIntroPane = new TranslateTransition(Duration.millis(600), introPane.root);

    // SIDE PANEL
    public SidePane sidePane = new SidePane();
    TranslateTransition moveSidePane = new TranslateTransition(Duration.millis(600), sidePane.root);

    // MODE PANE
    public ModePane modePane = new ModePane();

    // GAME SCREEN
    public GamePane gamePane = null;
    Stage stage = null;

    // BASE PANE
    StackPane basePane = new StackPane();

    // BACKGROUND
    Image backgroundImage    = new Image(getClass().getResourceAsStream("/gui/resources/bg.png"));
    ImageView background     = new ImageView(backgroundImage);
    StackPane backgroundPane = new StackPane();
    ScaleTransition    scaleBackground = new ScaleTransition(Duration.millis(600), backgroundPane);
    TranslateTransition moveBackground = new TranslateTransition(Duration.millis(600), backgroundPane);

    // EXIT FADE
    StackPane exitPane = new StackPane();
    FadeTransition exitFade = new FadeTransition(Duration.millis(1200), exitPane);

    public Scene scene = new Scene(basePane, cv.WIDTH, cv.HEIGHT, Color.BLACK);

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    public BaseScene() {
        // Pane interaction setup.
        introPane.root.setOnMouseClicked(event -> onClickIntroScene());
        exitPane.setStyle("-fx-background-color: black;");
        exitPane.setOpacity(0);
        introPane.root.setMouseTransparent(false);
        sidePane.root.setMouseTransparent(true);
        modePane.root.setMouseTransparent(true);

        // Menu button wiring.
        sidePane.playButton .setOnMouseClicked(event -> playButton());
        sidePane.loadButton .setOnMouseClicked(event -> loadButton());
        sidePane.statsButton.setOnMouseClicked(event -> statsButton());
        sidePane.exitButton .setOnMouseClicked(event -> exitButton());
        modePane.backButton .setOnMouseClicked(event -> bacKButton());

        // "Start game" button: triggered after mode + (optional) AI level are both selected.
        // We use single-click on time-control buttons — the wiring is done in ModePane itself,
        // but we hook into them via a dedicated confirm button (shown once a mode is picked).
        // For simplicity and backward-compatibility we wire directly to mode buttons here:

        // LOCAL game — time control buttons start the game immediately.
        wireLocalModeButton(modePane.bullet1Button,   GameMode.BULLET_1);
        wireLocalModeButton(modePane.bullet2Button,   GameMode.BULLET_2);
        wireLocalModeButton(modePane.blitz3Button,    GameMode.BLITZ_3);
        wireLocalModeButton(modePane.blitz5Button,    GameMode.BLITZ_5);
        wireLocalModeButton(modePane.rapid10Button,   GameMode.RAPID_10);
        wireLocalModeButton(modePane.rapid15Button,   GameMode.RAPID_15_10);
        wireLocalModeButton(modePane.rapid30Button,   GameMode.RAPID_30);
        wireLocalModeButton(modePane.classicalButton, GameMode.CLASSICAL);
        wireLocalModeButton(modePane.untimedButton,   GameMode.UNTIMED);

        // VS AI — time control selects mode, then AI level button starts the game.
        wireAILevelButton(modePane.beginnerButton, AILevel.BEGINNER);
        wireAILevelButton(modePane.easyButton,     AILevel.EASY);
        wireAILevelButton(modePane.mediumButton,   AILevel.MEDIUM);
        wireAILevelButton(modePane.hardButton,     AILevel.HARD);
        wireAILevelButton(modePane.expertButton,   AILevel.EXPERT);

        // LAN — opponent-type click transitions to NetworkPane.
        modePane.lanButton.setOnMouseClicked(e -> {
            modePane.selectOpponentType(false, true);
            openNetworkLobby();
        });
        modePane.onlineButton.setOnMouseClicked(e -> {
            modePane.selectOpponentType(false, false);
            openOnlineLobby();
        });

        // Background.
        background.setFitHeight(cv.HEIGHT);
        background.setFitWidth(cv.WIDTH);
        backgroundPane.getChildren().add(background);

        basePane.getChildren().addAll(backgroundPane, modePane.root, sidePane.root, introPane.root);
    }

    // =========================================================================
    // BUTTON WIRING HELPERS
    // =========================================================================

    /** For a LOCAL game: clicking a time-control button starts the game immediately. */
    private void wireLocalModeButton(javafx.scene.control.Button btn, GameMode mode) {
        btn.setOnMouseClicked(e -> {
            modePane.selectMode(mode);       // highlight it
            if (!modePane.isVsAI && !modePane.isLAN) {
                startGame(mode);
            }
            // If VS AI: wait for an AI level click (wired below).
        });
    }

    /** For a VS AI game: clicking an AI-level button starts the game using the already-selected mode. */
    private void wireAILevelButton(javafx.scene.control.Button btn, AILevel level) {
        btn.setOnMouseClicked(e -> {
            if (!modePane.isVsAI) return;
            modePane.selectAILevel(level);
            if (modePane.selectedMode == null) {
                // Default to Rapid 10 if no mode has been selected yet.
                modePane.selectedMode = GameMode.RAPID_10;
            }
            startAIGame(modePane.selectedMode, level);
        });
    }

    // =========================================================================
    // ORIGINAL MENU ANIMATIONS (UNCHANGED)
    // =========================================================================

    void onClickIntroScene() {
        moveIntroPane.setByX(500);
        moveIntroPane.setInterpolator(Interpolator.EASE_BOTH);
        moveSidePane.setByX(350);
        moveSidePane.setInterpolator(Interpolator.EASE_BOTH);
        scaleBackground.setToX(1.1);
        scaleBackground.setToY(1.1);
        scaleBackground.play();
        moveIntroPane.play();
        moveSidePane.play();
        introPane.root.setOnMouseClicked(null);
        introPane.root.setMouseTransparent(true);
        sidePane.root.setMouseTransparent(false);
    }

    void playButton() {
        // The mode overlay is hidden after navigating back. Re-show it from a
        // deterministic starting position instead of accumulating relative
        // TranslateTransition offsets across repeated visits.
        modePane.moveBackButton.stop();
        modePane.moveModePanel.stop();
        modePane.root.setVisible(true);
        modePane.root.setMouseTransparent(false);
        modePane.mainPanelSetForEntry();
        modePane.backButton.setTranslateY(-100);
        modePane.moveBackButton.setToY(0);
        modePane.moveBackButton.setInterpolator(Interpolator.EASE_OUT);
        modePane.moveModePanel.setToY(0);        // slide the complete mode panel into view
        modePane.moveModePanel.setInterpolator(Interpolator.EASE_OUT);

        moveIntroPane.setByX(500);
        moveIntroPane.setInterpolator(Interpolator.EASE_IN);
        moveSidePane.setByX(-350);
        moveIntroPane.setInterpolator(Interpolator.EASE_IN);

        moveBackground.setByY(50);
        moveBackground.setInterpolator(Interpolator.EASE_BOTH);

        modePane.moveModePanel.setOnFinished(null);
        modePane.moveBackButton.play();
        modePane.moveModePanel.play();
        moveIntroPane.play();
        moveSidePane.play();
        moveBackground.play();

        sidePane.root.setMouseTransparent(true);
        modePane.root.setMouseTransparent(false);
    }

    void loadButton() {
        // Find the newest saved PGN and load it.
        List<Path> saves = GameSaver.listSaves();
        if (saves.isEmpty()) {
            showMenuToast("No saved games found.");
            return;
        }

        // Slide out the menu (same animation as PLAY).
        moveIntroPane.setByX(500);
        moveIntroPane.setInterpolator(Interpolator.EASE_IN);
        moveSidePane.setByX(-350);
        moveBackground.setByY(50);
        moveBackground.setInterpolator(Interpolator.EASE_BOTH);
        moveIntroPane.play();
        moveSidePane.play();
        moveBackground.play();

        // Try to load and replay the most recent save.
        try {
            List<Move> moves = GameSaver.loadGame(saves.get(0));
            startLoadedGame(moves);
        } catch (IOException | IllegalArgumentException ex) {
            showMenuToast("Load failed: " + ex.getMessage());
            // Slide back.
            moveIntroPane.setByX(-500);
            moveSidePane.setByX(350);
            moveIntroPane.play();
            moveSidePane.play();
        }
    }

    void statsButton() {
        stage = resolveStage();
        if (stage == null) return;
        StatsPane sp = new StatsPane(() -> {
            if (stage != null) stage.setScene(scene);
        });
        stage.setScene(sp.scene);
    }

    void exitButton() {
        basePane.getChildren().add(exitPane);
        moveIntroPane.setByX(-500);
        moveIntroPane.setInterpolator(Interpolator.EASE_OUT);
        moveSidePane.setByX(-350);
        moveSidePane.setInterpolator(Interpolator.EASE_IN);
        scaleBackground.setToX(1.0);
        scaleBackground.setToY(1.0);
        scaleBackground.setInterpolator(Interpolator.EASE_BOTH);
        exitFade.setFromValue(0.0);
        exitFade.setToValue(1.0);
        exitFade.setInterpolator(Interpolator.EASE_IN);
        moveIntroPane.play();
        moveSidePane.play();
        scaleBackground.play();
        exitFade.play();
        exitFade.setOnFinished(event -> Platform.exit());
    }

    void bacKButton() {
        // Reset AI controls before the reverse animation. Otherwise the AI
        // difficulty row can remain visible at the top edge of the main menu.
        modePane.resetForMenu();
        modePane.moveBackButton.stop();
        modePane.moveModePanel.stop();
        modePane.root.setVisible(true);
        modePane.moveBackButton.setToY(-100);
        modePane.moveBackButton.setInterpolator(Interpolator.EASE_IN);
        modePane.moveModePanel.setToY(-700);
        modePane.moveModePanel.setInterpolator(Interpolator.EASE_IN);
        modePane.moveModePanel.setOnFinished(event -> {
            // Do not leave off-screen controls rendered above the main menu.
            modePane.root.setVisible(false);
            modePane.mainPanelSetForEntry();
            modePane.backButton.setTranslateY(-100);
        });

        moveIntroPane.setByX(-500);
        moveIntroPane.setInterpolator(Interpolator.EASE_OUT);
        moveSidePane.setByX(350);
        moveIntroPane.setInterpolator(Interpolator.EASE_OUT);
        moveBackground.setByY(-50);
        moveBackground.setInterpolator(Interpolator.EASE_BOTH);

        modePane.moveBackButton.play();
        modePane.moveModePanel.play();
        moveIntroPane.play();
        moveSidePane.play();
        moveBackground.play();

        sidePane.root.setMouseTransparent(false);
        modePane.root.setMouseTransparent(true);
    }

    // =========================================================================
    // GAME NAVIGATION
    // =========================================================================

    /** Starts a fresh local (human vs human) game. */
    void startGame(GameMode mode) {
        stage = resolveStage();
        if (stage == null) return;
        if (gamePane != null) gamePane.dispose();
        gamePane = new GamePane(mode,
                () -> startGame(mode),
                () -> returnToMenu());
        stage.setScene(gamePane.scene);
    }

    /** Backward-compatible variant used by SceneFlowSmoke. */
    void startGame(long timeControlMs) {
        GameMode mode = timeControlMs == Game.UNTIMED ? GameMode.UNTIMED
                : timeControlMs == Game.FIVE_MINUTES_MS ? GameMode.BLITZ_5
                : GameMode.RAPID_10;
        startGame(mode);
    }

    /** Starts a game vs the AI. */
    void startAIGame(GameMode mode, AILevel level) {
        stage = resolveStage();
        if (stage == null) return;
        if (gamePane != null) gamePane.dispose();
        // AI always plays as Black.
        gamePane = new GamePane(mode, level, pieces.PieceColor.BLACK,
                () -> startAIGame(mode, level),
                () -> returnToMenu());
        stage.setScene(gamePane.scene);
    }

    /** Opens the LAN lobby. */
    void openNetworkLobby() {
        stage = resolveStage();
        if (stage == null) return;
        NetworkPane np = new NetworkPane(
                ng -> startNetworkGame(ng),
                () -> { if (stage != null) stage.setScene(scene); bacKButton(); },
                err -> System.err.println("Network error: " + err));
        stage.setScene(np.scene);
    }

    /** Opens the cross-network online lobby. A public relay server must be configured/deployed. */
    void openOnlineLobby() {
        stage = resolveStage();
        if (stage == null) return;
        OnlinePane pane = new OnlinePane(
                ng -> startNetworkGame(ng),
                () -> { if (stage != null) stage.setScene(scene); bacKButton(); },
                err -> System.err.println("Online network error: " + err));
        stage.setScene(pane.scene);
    }

    /** Called when a NetworkGame is fully established (both players connected). */
    void startNetworkGame(NetworkGame ng) {
        if (stage == null) return;
        if (gamePane != null) gamePane.dispose();
        // IMPORTANT: use the same Game instance that NetworkGame validates against.
        // Creating a second Game here makes remote moves get validated against a stale board.
        gamePane = new GamePane(ng.getGame(),
                () -> { /* no new game in LAN */ },
                () -> returnToMenu());
        gamePane.setNetworkGame(ng);
        stage.setScene(gamePane.scene);
    }

    /** Loads a saved game from a move list and opens it in the GamePane in replay mode. */
    void startLoadedGame(List<Move> moves) {
        stage = resolveStage();
        if (stage == null) return;
        if (gamePane != null) gamePane.dispose();
        gamePane = new GamePane(GameMode.UNTIMED,
                () -> startLoadedGame(moves),
                () -> returnToMenu());
        // Replay all moves onto the game.
        for (Move m : moves) {
            gamePane.getGame().tryMove(m.fromX, m.fromY, m.toX, m.toY, m.promotion);
        }
        // Enter replay mode at ply 0.
        gamePane.getGame().replayGoto(0);
        stage.setScene(gamePane.scene);
    }

    /**
     * Resolve the window from the currently displayed scene. When a game is
     * active, the main-menu Scene is detached from the Stage and getWindow()
     * returns null; always check the current GamePane before giving up.
     */
    private Stage resolveStage() {
        if (scene.getWindow() instanceof Stage) {
            stage = (Stage) scene.getWindow();
        } else if (gamePane != null && gamePane.scene.getWindow() instanceof Stage) {
            stage = (Stage) gamePane.scene.getWindow();
        }
        return stage;
    }

    /** Returns to the main menu from any game screen. */
    public void returnToMenu() {
        resolveStage();
        if (gamePane != null) {
            gamePane.dispose();
            gamePane = null;
        }
        if (stage != null) stage.setScene(scene);

        // Fully reset the mode overlay before the reverse transition. This prevents
        // stale AI controls (Beginner/Easy/Hard/Expert) remaining over the main menu.
        modePane.selectOpponentType(false, false);
        modePane.root.setMouseTransparent(true);
        modePane.root.setVisible(true);
        modePane.moveModePanel.stop();
        modePane.moveBackButton.stop();
        modePane.moveModePanel.setByY(-700);
        modePane.moveBackButton.setByY(-100);
        modePane.root.setTranslateX(0);

        bacKButton();
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /** Brief status label shown in the intro pane area when no stage-level alert is available. */
    private void showMenuToast(String msg) {
        System.out.println("[Menu] " + msg);
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Message");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
