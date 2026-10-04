package gui.scenes;

import game.Game;
import game.GameStatus;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.stage.Stage;
import javafx.util.Duration;
import pieces.PieceColor;

import javax.imageio.ImageIO;
import java.io.File;

/**
 * Optional integration smoke test for the whole application journey:
 *
 *   intro -> PLAY -> 5 MIN -> game screen -> scripted fool's mate
 *         -> checkmate + clocks verified -> BACK TO MENU
 *
 * It clicks the real buttons (by dispatching mouse events), so it also
 * verifies the navigation wiring between the menu and the game screen.
 *
 * Run with:
 *   java --module-path "C:\JavaFX\javafx-sdk-27\lib" --add-modules javafx.controls,javafx.fxml gui.scenes.SceneFlowSmoke
 *
 * Exits with code 0 when every step passes, 1 otherwise.
 */
public class SceneFlowSmoke extends Application {

    // Fool's mate: 1. f3 e5 2. g4 Qh4#
    private static final int[][] MOVES = {
            { 5, 1, 5, 2 },
            { 4, 6, 4, 4 },
            { 6, 1, 6, 3 },
            { 3, 7, 7, 3 },
    };

    private int passed = 0;
    private int failed = 0;

    private BaseScene base;
    private Game game;
    private int moveIndex = 0;

    @Override
    public void start(Stage stage) {
        base = new BaseScene();
        stage.setScene(base.scene);
        stage.setTitle("SceneFlowSmoke");
        stage.show();

        // Drive the menu flow with real clicks.
        Timeline prologue = new Timeline(
                new KeyFrame(Duration.millis(600), e -> click(base.introPane.root)),
                new KeyFrame(Duration.millis(1400), e -> snap(base.scene, "smoke_menu.png")),
                new KeyFrame(Duration.millis(1600), e -> click(base.sidePane.playButton)),
                new KeyFrame(Duration.millis(2500), e -> snap(base.scene, "smoke_modes.png")),
                new KeyFrame(Duration.millis(2600), e -> enterGame(stage))
        );
        prologue.play();
    }

    private void enterGame(Stage stage) {
        click(base.modePane.blitz5Button);

        check("game pane created", base.gamePane != null);
        if (base.gamePane == null) {
            printSummary();
            Platform.exit();
            return;
        }
        check("stage shows the game scene", stage.getScene() == base.gamePane.scene);
        game = base.gamePane.getGame();
        check("5 minute clocks", game.whiteMillis() == Game.FIVE_MINUTES_MS
                && game.blackMillis() == Game.FIVE_MINUTES_MS);
        check("clock started", game.isClockRunning());

        playGame();   // (started here, not from a timeline 'finished' handler,
                      //  which can fire reentrantly while the scene switches)

        Timeline shots = new Timeline(new KeyFrame(Duration.millis(1700), e ->
                snap(base.gamePane.scene, "smoke_midgame.png")));
        shots.play();
    }

    private void playGame() {
        if (game == null) {
            failed++;
            System.out.println("  FAIL  no game to play");
            printSummary();
            Platform.exit();
            return;
        }
        Timeline player = new Timeline(new KeyFrame(Duration.millis(900), e -> playNextMove()));
        player.setCycleCount(MOVES.length);
        player.setOnFinished(e -> finish());
        player.play();
    }

    private void playNextMove() {
        int[] m = MOVES[moveIndex++];
        boolean ok = game.tryMove(m[0], m[1], m[2], m[3], null);
        check("move " + moveIndex + " played", ok);
    }

    private void finish() {
        check("checkmate reached", game.status() == GameStatus.CHECKMATE);
        check("black wins", game.winner() == PieceColor.BLACK);
        check("white clock ran down", game.whiteMillis() > 0 && game.whiteMillis() < Game.FIVE_MINUTES_MS);
        check("black clock ran down", game.blackMillis() > 0 && game.blackMillis() < Game.FIVE_MINUTES_MS);
        check("clock stopped at game over", !game.isClockRunning());

        snap(base.gamePane.scene, "smoke_gameover.png");

        // Give the result overlay a moment on screen, then go back to the menu.
        Timeline exit = new Timeline(new KeyFrame(Duration.millis(2500), e -> returnToMenu()));
        exit.play();
    }

    private void returnToMenu() {
        base.returnToMenu();

        check("back on the menu scene", base.scene.getWindow() != null
                && ((Stage) base.scene.getWindow()).getScene() == base.scene);
        check("game pane disposed", base.gamePane == null);
        check("clock stopped after leaving", !game.isClockRunning());

        // Wait for the menu transition, snapshot the restored menu, then quit.
        Timeline quit = new Timeline(
                new KeyFrame(Duration.millis(900), e -> snap(base.scene, "smoke_menu_return.png")),
                new KeyFrame(Duration.millis(1200), e -> {
                    printSummary();
                    Platform.exit();
                }));
        quit.play();
    }

    /** Saves a snapshot of a scene (works even when the window is not focused). */
    private void snap(Scene scene, String file) {
        try {
            WritableImage image = scene.snapshot(null);
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", new File(file));
            System.out.println("  SNAP  " + file);
        } catch (Exception ex) {
            failed++;
            System.out.println("  FAIL  snapshot " + file + ": " + ex);
        }
    }

    @Override
    public void stop() {
        System.exit(failed > 0 ? 1 : 0);
    }

    // --- --- --- --- --- HELPERS --- --- --- --- --- //

    private void click(Node target) {
        target.fireEvent(new MouseEvent(
                MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false,
                false, false, false, false, false, false,
                new PickResult(target, 0, 0)));
    }

    private void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    private void printSummary() {
        System.out.println();
        System.out.println("FLOW SMOKE RESULT: " + (failed == 0 ? "PASS" : "FAIL")
                + " (" + passed + " passed, " + failed + " failed)");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
