package gui.scenes.panes;

import game.Game;
import game.GameStatus;
import gui.CommonValues;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import move.Move;
import pieces.Piece;
import pieces.PieceColor;
import pieces.PieceType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The playable chess screen.
 *
 * Rendering rule: the engine's Board is the single source of truth. Mouse
 * clicks are translated to board coordinates, validated by Game, and after
 * every accepted move the whole display is re-rendered from the Board.
 *
 * Screen layout:
 *
 *   +--------------------------------------------------------------+
 *   |  [ 8x8 board ]   |  black clock                             |
 *   |  pieces          |  move list                               |
 *   |  highlights      |  turn indicator                          |
 *   |                  |  white clock                             |
 *   |                  |  [ MENU ]                                |
 *   +--------------------------------------------------------------+
 *   overlays: promotion chooser / game result / leave-game confirm
 */
public class GamePane implements Game.Listener {

    // --- Assets --- //
    private static final String BOARD_IMAGE_PATH  = "/gui/resources/Chess_Board.svg.png";
    private static final String PIECE_IMAGES_DIR  = "/gui/resources/ChessPeices/";
    private static final String BACKGROUND_PATH   = "/gui/resources/bg.png";

    // --- Chess_Board.svg.png geometry --- //
    // The image is 1280x1280: an 8px dark frame around an 8x8 grid of 158px squares.
    private static final double IMAGE_SIZE = 1280.0;
    private static final double INSET_RATIO = 8.0 / IMAGE_SIZE;
    private static final double SQUARE_RATIO = 158.0 / IMAGE_SIZE;

    private static final double BOARD_SIZE = 840.0;
    private static final double SQUARE = BOARD_SIZE * SQUARE_RATIO;
    private static final double INSET = BOARD_SIZE * INSET_RATIO;

    // --- Highlight colors --- //
    private static final Color COLOR_LAST_MOVE = Color.rgb(0, 150, 255, 0.30);
    private static final Color COLOR_SELECTED  = Color.rgb(255, 214, 51, 0.55);
    private static final Color COLOR_DEST      = Color.rgb(15, 90, 170, 0.60);
    private static final Color COLOR_CAPTURE   = Color.rgb(15, 100, 190, 0.90);
    private static final Color COLOR_CHECK     = Color.rgb(255, 45, 45, 0.50);

    private static final Map<String, Image> PIECE_IMAGE_CACHE = new HashMap<>();

    // --- Window / callbacks --- //
    private final CommonValues cv = new CommonValues();
    private final Game game;
    private final Runnable onNewGame;
    private final Runnable onExitToMenu;

    public final StackPane root = new StackPane();
    public final Scene scene;

    // --- Board display --- //
    private final Pane boardPane = new Pane();
    private final ImageView boardImage = new ImageView();
    private final Pane highlightLayer = new Pane();
    private final Pane pieceLayer = new Pane();

    // --- Side panel --- //
    private final VBox sidePanel = new VBox(14);
    private final Label blackClock = new Label();
    private final Label whiteClock = new Label();
    private final Label turnLabel = new Label();
    private final Label checkLabel = new Label("CHECK!");
    private final ListView<String> moveList = new ListView<>();

    // --- Overlays --- //
    private final StackPane overlayLayer = new StackPane();

    // --- Selection state --- //
    private int selectedX = -1, selectedY = -1;
    private List<Move> selectedMoves = new ArrayList<>();
    private int promoFromX, promoFromY, promoToX, promoToY;   // pending promotion move

    // --- Clock ticker --- //
    private Timeline clockTimeline;

    public GamePane(long timeControlMs, Runnable onNewGame, Runnable onExitToMenu) {
        this.game = new Game("", timeControlMs);
        this.onNewGame = onNewGame;
        this.onExitToMenu = onExitToMenu;

        buildLayout();

        scene = new Scene(root, cv.WIDTH, cv.HEIGHT, Color.BLACK);

        game.setListener(this);
        game.startClock();
        startClockTicker();
        refresh();
    }

    // --- --- --- --- --- LAYOUT --- --- --- --- --- //

    private void buildLayout() {
        // Background, stretched like the menu background.
        ImageView background = new ImageView(new Image(getClass().getResourceAsStream(BACKGROUND_PATH)));
        background.setPreserveRatio(false);
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());

        // Board: image at the bottom, then highlights, then pieces.
        Image boardImg = new Image(getClass().getResourceAsStream(BOARD_IMAGE_PATH));
        boardImage.setImage(boardImg);
        boardImage.setFitWidth(BOARD_SIZE);
        boardImage.setFitHeight(BOARD_SIZE);
        boardImage.setX(0);
        boardImage.setY(0);
        boardImage.setSmooth(true);

        boardPane.getChildren().addAll(boardImage, highlightLayer, pieceLayer);
        boardPane.setPrefSize(BOARD_SIZE, BOARD_SIZE);
        boardPane.setMinSize(BOARD_SIZE, BOARD_SIZE);
        boardPane.setMaxSize(BOARD_SIZE, BOARD_SIZE);
        boardPane.setOnMouseClicked(this::onBoardClicked);

        // Side panel: clocks, move list, turn indicator, menu button.
        Label blackCaption = caption("BLACK");
        Label whiteCaption = caption("WHITE");

        blackClock.setFont(cv.VARELA_CLOCK);
        blackClock.setPrefWidth(300);
        blackClock.setPrefHeight(70);
        blackClock.setAlignment(Pos.CENTER);
        whiteClock.setFont(cv.VARELA_CLOCK);
        whiteClock.setPrefWidth(300);
        whiteClock.setPrefHeight(70);
        whiteClock.setAlignment(Pos.CENTER);

        turnLabel.setFont(cv.VARELA_BUTTON);
        turnLabel.setTextFill(Color.WHITE);
        turnLabel.setWrapText(true);
        turnLabel.setAlignment(Pos.CENTER);
        turnLabel.setPrefWidth(300);

        checkLabel.setFont(cv.VARELA_BUTTON);
        checkLabel.setTextFill(Color.web("#ff5555"));
        checkLabel.setAlignment(Pos.CENTER);
        checkLabel.setPrefWidth(300);
        checkLabel.setVisible(false);

        moveList.setPrefWidth(300);
        moveList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setTextFill(Color.WHITE);
                setStyle("-fx-background-color: transparent;");
            }
        });
        moveList.setStyle(cv.STYLE_MOVE_LIST);
        VBox.setVgrow(moveList, Priority.ALWAYS);

        Button menuButton = new Button("MENU");
        menuButton.setFont(cv.VARELA_BUTTON);
        menuButton.setTextFill(Color.WHITE);
        menuButton.setStyle(cv.STYLE_BSP);
        menuButton.setPrefWidth(300);
        menuButton.setPrefHeight(60);
        withHoverScale(menuButton);
        menuButton.setOnMouseClicked(event -> onMenuClicked());

        VBox blackClockBox = new VBox(4, blackCaption, blackClock);
        blackClockBox.setAlignment(Pos.CENTER);
        VBox whiteClockBox = new VBox(4, whiteClock, whiteCaption);
        whiteClockBox.setAlignment(Pos.CENTER);

        sidePanel.setPrefWidth(300);
        sidePanel.setMinWidth(300);
        sidePanel.setMaxWidth(300);
        sidePanel.setAlignment(Pos.TOP_CENTER);
        sidePanel.setPadding(new javafx.geometry.Insets(10));
        sidePanel.setStyle(cv.STYLE_GAME_PANEL);
        sidePanel.getChildren().addAll(blackClockBox, moveList, turnLabel, checkLabel, whiteClockBox, menuButton);

        HBox content = new HBox(40, boardPane, sidePanel);
        content.setAlignment(Pos.CENTER);

        overlayLayer.setVisible(false);
        overlayLayer.setMouseTransparent(true);

        root.getChildren().addAll(background, content, overlayLayer);
    }

    private Label caption(String text) {
        Label l = new Label(text);
        l.setFont(cv.VARELA_BUTTON);
        l.setTextFill(Color.web("#9fc6e0"));
        l.setAlignment(Pos.CENTER);
        l.setPrefWidth(300);
        return l;
    }

    // --- --- --- --- --- COORDINATE MAPPING --- --- --- --- --- //

    /** Engine x (file 0..7) -> pixel left edge inside the board pane. */
    private double squareLeft(int x) {
        return INSET + x * SQUARE;
    }

    /** Engine y (rank 0..7, 0 = white's back rank) -> pixel top edge (rank 8 shown at the top). */
    private double squareTop(int y) {
        return INSET + (7 - y) * SQUARE;
    }

    /** Pixel position -> engine square, or null when the click was off the grid. */
    private int[] squareAt(double px, double py) {
        int col = (int) Math.floor((px - INSET) / SQUARE);
        int row = (int) Math.floor((py - INSET) / SQUARE);   // row counted from the top
        if (col < 0 || col > 7 || row < 0 || row > 7) return null;
        return new int[] { col, 7 - row };
    }

    // --- --- --- --- --- INPUT --- --- --- --- --- //

    private void onBoardClicked(MouseEvent event) {
        if (game.isGameOver() || overlayLayer.isVisible()) return;

        int[] square = squareAt(event.getX(), event.getY());
        if (square == null) {
            clearSelection();
            renderHighlights();
            return;
        }
        int x = square[0], y = square[1];

        // Clicking a legal destination moves the selected piece.
        if (selectedX >= 0) {
            if (x == selectedX && y == selectedY) {          // clicking the piece again deselects
                clearSelection();
                renderHighlights();
                return;
            }
            List<Move> toTarget = new ArrayList<>();
            for (Move m : selectedMoves) {
                if (m.targets(x, y)) toTarget.add(m);
            }
            if (!toTarget.isEmpty()) {
                if (toTarget.get(0).isPromotion()) {
                    askForPromotion(selectedX, selectedY, x, y);
                } else {
                    game.tryMove(selectedX, selectedY, x, y, null);
                }
                return;
            }
        }

        // Otherwise: select one of the side to move's pieces (empty list for
        // opponent pieces or illegal selections - the state never changes).
        List<Move> moves = game.legalMovesFrom(x, y);
        if (moves.isEmpty()) {
            clearSelection();
        } else {
            selectedX = x;
            selectedY = y;
            selectedMoves = moves;
        }
        renderHighlights();
    }

    private void onMenuClicked() {
        if (game.isGameOver()) {
            exitToMenu();
        } else {
            showLeaveConfirm();
        }
    }

    private void exitToMenu() {
        game.setListener(null);      // no result-overlay flash while leaving
        game.abandon();              // records the result and stops the clock
        onExitToMenu.run();          // BaseScene disposes us and switches scenes
    }

    /** The game this screen is playing (used by tools/tests that drive the screen). */
    public Game getGame() {
        return game;
    }

    // --- --- --- --- --- RENDERING --- --- --- --- --- //

    /** Re-renders everything from the Board (the single source of truth). */
    private void refresh() {
        renderPieces();
        renderHighlights();
        renderSidePanel();
    }

    private void renderPieces() {
        pieceLayer.getChildren().clear();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Piece p = game.getBoard().pieceAt(x, y);
                if (p == null) continue;
                ImageView view = new ImageView(pieceImage(p));
                view.setFitWidth(SQUARE);
                view.setFitHeight(SQUARE);
                view.setSmooth(true);
                view.setX(squareLeft(x));
                view.setY(squareTop(y));
                pieceLayer.getChildren().add(view);
            }
        }
    }

    private void renderHighlights() {
        highlightLayer.getChildren().clear();

        // Last move (from -> to).
        if (!game.moveStack.isEmpty()) {
            Move last = game.moveStack.get(game.moveStack.size() - 1);
            addSquareHighlight(last.fromX, last.fromY, COLOR_LAST_MOVE);
            addSquareHighlight(last.toX, last.toY, COLOR_LAST_MOVE);
        }

        // Check: highlight the king of the side to move.
        if (!game.isGameOver() && game.isInCheck(game.turn())) {
            Piece king = game.getBoard().findKing(game.turn());
            if (king != null) addSquareHighlight(king.posX, king.posY, COLOR_CHECK);
        }

        // Selection and its legal destinations.
        if (selectedX >= 0) {
            addSquareHighlight(selectedX, selectedY, COLOR_SELECTED);
            for (Move m : selectedMoves) {
                if (m.isCapture()) {
                    // Ring around squares where a capture (or en passant) would happen.
                    Circle ring = new Circle(squareLeft(m.toX) + SQUARE / 2,
                            squareTop(m.toY) + SQUARE / 2, SQUARE * 0.42);
                    ring.setFill(Color.TRANSPARENT);
                    ring.setStroke(COLOR_CAPTURE);
                    ring.setStrokeWidth(SQUARE * 0.08);
                    highlightLayer.getChildren().add(ring);
                } else {
                    Circle dot = new Circle(squareLeft(m.toX) + SQUARE / 2,
                            squareTop(m.toY) + SQUARE / 2, SQUARE * 0.13);
                    dot.setFill(COLOR_DEST);
                    highlightLayer.getChildren().add(dot);
                }
            }
        }
    }

    private void addSquareHighlight(int x, int y, Color color) {
        Rectangle rect = new Rectangle(squareLeft(x), squareTop(y), SQUARE, SQUARE);
        rect.setFill(color);
        highlightLayer.getChildren().add(rect);
    }

    private void renderSidePanel() {
        updateClockLabels();
        updateTurnIndicator();
        rebuildMoveList();
    }

    private void updateClockLabels() {
        blackClock.setText(formatClock(game.blackMillis()));
        whiteClock.setText(formatClock(game.whiteMillis()));

        PieceColor active = game.isGameOver() ? null : game.turn();
        boolean blackActive = active == PieceColor.BLACK;
        boolean whiteActive = active == PieceColor.WHITE;
        blackClock.setStyle(blackActive ? cv.STYLE_CLOCK_ACTIVE : cv.STYLE_CLOCK_IDLE);
        whiteClock.setStyle(whiteActive ? cv.STYLE_CLOCK_ACTIVE : cv.STYLE_CLOCK_IDLE);
        blackClock.setTextFill(blackActive || !game.isGameOver() ? Color.WHITE : Color.web("#cfe4f2"));
        whiteClock.setTextFill(whiteActive || !game.isGameOver() ? Color.WHITE : Color.web("#cfe4f2"));
    }

    private void updateTurnIndicator() {
        if (game.isGameOver()) {
            turnLabel.setText("GAME OVER");
            checkLabel.setVisible(false);
            return;
        }
        turnLabel.setText(game.turn() == PieceColor.WHITE ? "WHITE TO MOVE" : "BLACK TO MOVE");
        checkLabel.setVisible(game.isInCheck(game.turn()));
    }

    private void rebuildMoveList() {
        List<String> items = new ArrayList<>();
        List<Move> moves = game.moveStack;
        for (int i = 0; i < moves.size(); i++) {
            Move m = moves.get(i);
            int moveNo = i / 2 + 1;
            String prefix = (i % 2 == 0) ? (moveNo + ".") : (moveNo + " ...");
            String suffix = "";
            if (i == moves.size() - 1) {
                if (game.status() == GameStatus.CHECKMATE) suffix = " #";
                else if (game.isInCheck(m.piece.color.other())) suffix = " +";
            }
            items.add(prefix + " " + m.notation() + suffix);
        }
        moveList.setItems(FXCollections.observableArrayList(items));
        if (!items.isEmpty()) moveList.scrollTo(items.size() - 1);
    }

    private String formatClock(long millis) {
        long totalSeconds = Math.max(0, millis) / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private Image pieceImage(Piece p) {
        return pieceImageByName("" + p.color.code + p.type.code);   // e.g. "WP", "BH"
    }

    private Image pieceImageByName(String assetName) {
        return PIECE_IMAGE_CACHE.computeIfAbsent(assetName,
                name -> new Image(getClass().getResourceAsStream(PIECE_IMAGES_DIR + name + ".png")));
    }

    // --- --- --- --- --- OVERLAYS --- --- --- --- --- //

    private void showOverlay(StackPane panel) {
        Rectangle backdrop = new Rectangle();
        backdrop.setFill(Color.rgb(0, 0, 0, 0.55));
        backdrop.widthProperty().bind(root.widthProperty());
        backdrop.heightProperty().bind(root.heightProperty());
        overlayLayer.getChildren().setAll(backdrop, panel);
        overlayLayer.setVisible(true);
        overlayLayer.setMouseTransparent(false);
    }

    private void hideOverlay() {
        overlayLayer.getChildren().clear();
        overlayLayer.setMouseTransparent(true);
        overlayLayer.setVisible(false);
    }

    private StackPane overlayPanel(javafx.scene.Node content) {
        StackPane panel = new StackPane(content);
        panel.setStyle(cv.STYLE_OVERLAY_PANEL);
        panel.setAlignment(Pos.CENTER);
        panel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return panel;
    }

    /** Promotion chooser: four pieces of the promoting color. */
    private void askForPromotion(int fromX, int fromY, int toX, int toY) {
        promoFromX = fromX;
        promoFromY = fromY;
        promoToX = toX;
        promoToY = toY;

        Text prompt = new Text("CHOOSE PROMOTION");
        prompt.setFont(cv.NEW_ROCKER_MEDIUM);
        prompt.setFill(Color.WHITE);

        HBox choices = new HBox(20);
        choices.setAlignment(Pos.CENTER);
        for (PieceType type : PieceType.PROMOTION_TYPES) {
            String assetName = "" + game.turn().code + type.code;
            ImageView icon = new ImageView(pieceImageByName(assetName));
            icon.setFitWidth(90);
            icon.setFitHeight(90);
            icon.setSmooth(true);

            Button button = new Button();
            button.setGraphic(icon);
            button.setStyle(cv.STYLE_BSP);
            button.setPrefSize(110, 110);
            withHoverScale(button);
            button.setOnMouseClicked(event -> {
                hideOverlay();
                game.tryMove(promoFromX, promoFromY, promoToX, promoToY, type);
            });
            choices.getChildren().add(button);
        }

        VBox panelContent = new VBox(30, prompt, choices);
        panelContent.setAlignment(Pos.CENTER);
        panelContent.setPadding(new javafx.geometry.Insets(30, 50, 30, 50));
        showOverlay(overlayPanel(panelContent));
    }

    /** Result screen after the game ends. */
    private void showGameOver() {
        Text title = new Text(resultTitle());
        title.setFont(cv.NEW_ROCKER_MEDIUM);
        title.setFill(Color.WHITE);

        Text reason = new Text(resultReason());
        reason.setFont(cv.VARELA_BUTTON);
        reason.setFill(Color.web("#9fc6e0"));

        Button newGameButton = overlayButton("NEW GAME");
        newGameButton.setOnMouseClicked(event -> onNewGame.run());
        Button backButton = overlayButton("BACK TO MENU");
        backButton.setOnMouseClicked(event -> exitToMenu());

        HBox buttons = new HBox(30, newGameButton, backButton);
        buttons.setAlignment(Pos.CENTER);

        VBox content = new VBox(24, title, reason, buttons);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new javafx.geometry.Insets(40, 60, 40, 60));
        showOverlay(overlayPanel(content));
    }

    /** Warning before abandoning a game that is still running. */
    private void showLeaveConfirm() {
        Text question = new Text("LEAVE THE GAME?");
        question.setFont(cv.NEW_ROCKER_MEDIUM);
        question.setFill(Color.WHITE);

        Text warning = new Text("The current game will be lost.");
        warning.setFont(cv.VARELA_BUTTON);
        warning.setFill(Color.web("#9fc6e0"));

        Button yesButton = overlayButton("YES, LEAVE");
        yesButton.setOnMouseClicked(event -> exitToMenu());
        Button noButton = overlayButton("KEEP PLAYING");
        noButton.setOnMouseClicked(event -> hideOverlay());

        HBox buttons = new HBox(30, yesButton, noButton);
        buttons.setAlignment(Pos.CENTER);

        VBox content = new VBox(24, question, warning, buttons);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new javafx.geometry.Insets(40, 60, 40, 60));
        showOverlay(overlayPanel(content));
    }

    private Button overlayButton(String text) {
        Button b = new Button(text);
        b.setFont(cv.VARELA_BUTTON);
        b.setTextFill(Color.WHITE);
        b.setStyle(cv.STYLE_BSP);
        b.setPrefSize(240, 64);
        withHoverScale(b);
        return b;
    }

    private String resultTitle() {
        if (game.winner() == null) return "DRAW";
        return (game.winner() == PieceColor.WHITE ? "WHITE" : "BLACK") + " WINS";
    }

    private String resultReason() {
        switch (game.status()) {
            case CHECKMATE:             return "by Checkmate";
            case TIMEOUT:               return "by Time";
            case STALEMATE:             return "Stalemate";
            case INSUFFICIENT_MATERIAL: return "Insufficient Material";
            case THREEFOLD_REPETITION:  return "Threefold Repetition";
            case FIFTY_MOVE_RULE:       return "Fifty-Move Rule";
            case ABANDONED:             return "Game Abandoned";
            default:                    return "";
        }
    }

    // --- --- --- --- --- CLOCK TICKER --- --- --- --- --- //

    private void startClockTicker() {
        clockTimeline = new Timeline(new KeyFrame(Duration.millis(100), event -> {
            game.tickClock();
            updateClockLabels();
        }));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    // --- --- --- --- --- GAME LISTENER --- --- --- --- --- //

    @Override
    public void onPositionChanged() {
        clearSelection();
        refresh();
    }

    @Override
    public void onGameOver(GameStatus status, PieceColor winner) {
        clearSelection();
        renderHighlights();
        updateClockLabels();
        updateTurnIndicator();
        if (clockTimeline != null) clockTimeline.stop();
        showGameOver();
    }

    // --- --- --- --- --- CLEANUP --- --- --- --- --- //

    /** Stops the ticker and the clock; safe to call more than once. */
    public void dispose() {
        if (clockTimeline != null) {
            clockTimeline.stop();
            clockTimeline = null;
        }
        game.setListener(null);
        game.pauseClock();
    }

    // --- --- --- --- --- HELPERS --- --- --- --- --- //

    private void clearSelection() {
        selectedX = -1;
        selectedY = -1;
        selectedMoves = new ArrayList<>();
    }

    private void withHoverScale(Button button) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(20), button);
        button.setOnMouseEntered(event -> {
            scale.setToX(1.1);
            scale.setToY(1.1);
            scale.stop();
            scale.playFromStart();
        });
        button.setOnMouseExited(event -> {
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.stop();
            scale.playFromStart();
        });
    }
}
