package gui.scenes.panes;

import ai.AILevel;
import ai.ChessAI;
import analytics.GameStats;
import analytics.OpeningBook;
import analytics.StatsStore;
import game.Game;
import game.GameMode;
import game.GameStatus;
import gui.CommonValues;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.*;
import javafx.scene.text.Text;
import javafx.util.Duration;
import move.Move;
import network.NetworkGame;
import persistence.GameSaver;
import pieces.Piece;
import pieces.PieceColor;
import pieces.PieceType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * The playable chess screen.
 *
 * Rendering rule: the engine's Board is the single source of truth. Mouse
 * clicks are translated to board coordinates, validated by Game, and after
 * every accepted move the whole display is re-rendered from the Board.
 *
 * Extended from the original with:
 *   - Smooth move animation (piece slides to destination)
 *   - 3D-look piece rendering (DropShadow + InnerShadow)
 *   - AI opponent support
 *   - Undo / Redo
 *   - Save / Load (PGN)
 *   - PGN / FEN clipboard export
 *   - Replay mode (step through move history)
 *   - Opening name display
 *   - Network (LAN) game support
 *   - Statistics recording
 *
 * All original styling (colors, fonts, button style, clock layout) is
 * preserved exactly.
 */
public class GamePane implements Game.Listener {

    // --- Assets --- //
    private static final String BOARD_IMAGE_PATH = "/gui/resources/Chess_Board.svg.png";
    private static final String PIECE_IMAGES_DIR = "/gui/resources/ChessPeices/";
    private static final String BACKGROUND_PATH  = "/gui/resources/bg.png";

    // --- Chess_Board.svg.png geometry --- //
    private static final double IMAGE_SIZE   = 1280.0;
    private static final double INSET_RATIO  = 8.0 / IMAGE_SIZE;
    private static final double SQUARE_RATIO = 158.0 / IMAGE_SIZE;

    private static final double BOARD_SIZE = 840.0;
    private static final double SQUARE     = BOARD_SIZE * SQUARE_RATIO;
    private static final double INSET      = BOARD_SIZE * INSET_RATIO;

    // --- Highlight colors (UNCHANGED from original) --- //
    private static final Color COLOR_LAST_MOVE = Color.rgb(0, 150, 255, 0.30);
    private static final Color COLOR_SELECTED  = Color.rgb(255, 214, 51, 0.55);
    private static final Color COLOR_DEST      = Color.rgb(15, 90, 170, 0.60);
    private static final Color COLOR_CAPTURE   = Color.rgb(15, 100, 190, 0.90);
    private static final Color COLOR_CHECK     = Color.rgb(255, 45, 45, 0.50);

    private static final Map<String, Image> PIECE_IMAGE_CACHE = new HashMap<>();

    // --- Window / callbacks --- //
    private final CommonValues cv = new CommonValues();
    private final Game         game;
    private final Runnable     onNewGame;
    private final Runnable     onExitToMenu;

    // --- Optional AI --- //
    private ChessAI ai = null;
    private boolean aiThinking = false;

    // --- Optional network --- //
    private NetworkGame networkGame = null;

    // --- Statistics --- //
    private final GameStats    gameStats = new GameStats();
    private final StatsStore   whiteStats;
    private final StatsStore   blackStats;
    private int lastMaterialBalance = 0;

    public final StackPane root  = new StackPane();
    public final Scene     scene;

    // --- Board display --- //
    private final Pane      boardPane      = new Pane();
    private final ImageView boardImage     = new ImageView();
    private final Pane      highlightLayer = new Pane();
    private final Pane      pieceLayer     = new Pane();
    private final Pane      animLayer      = new Pane();   // animated piece lives here during motion

    // --- Side panel --- //
    private final VBox  sidePanel  = new VBox(10);
    private final Label blackClock = new Label();
    private final Label whiteClock = new Label();
    private final Label turnLabel  = new Label();
    private final Label checkLabel = new Label("CHECK!");
    private final Label openingLabel = new Label();
    private final ListView<String> moveList = new ListView<>();

    // --- Extra control buttons --- //
    private Button undoButton, redoButton, saveButton;
    private Button replayPrevButton, replayNextButton, replayExitButton;
    private HBox   replayBar;

    // --- Overlays --- //
    private final StackPane overlayLayer = new StackPane();

    // --- Selection state --- //
    private int selectedX = -1, selectedY = -1;
    private List<Move> selectedMoves = new ArrayList<>();
    private int promoFromX, promoFromY, promoToX, promoToY;

    // --- Clock ticker --- //
    private Timeline clockTimeline;

    // --- Animation --- //
    private boolean animating = false;

    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================

    /** Local human-vs-human game. */
    public GamePane(long timeControlMs, Runnable onNewGame, Runnable onExitToMenu) {
        this(Game.fromMode(timeControlMs == Game.UNTIMED ? GameMode.UNTIMED
                : timeControlMs == Game.FIVE_MINUTES_MS ? GameMode.BLITZ_5
                : GameMode.RAPID_10),
             onNewGame, onExitToMenu);
    }

    /** Game from a specific {@link GameMode}. */
    public GamePane(GameMode mode, Runnable onNewGame, Runnable onExitToMenu) {
        this(Game.fromMode(mode), onNewGame, onExitToMenu);
    }

    /** Game with AI opponent. */
    public GamePane(GameMode mode, AILevel aiLevel, PieceColor aiColor,
                    Runnable onNewGame, Runnable onExitToMenu) {
        this(Game.fromMode(mode), onNewGame, onExitToMenu);
        this.ai = new ChessAI(aiLevel, aiColor);
        game.setWhiteName(aiColor == PieceColor.WHITE ? "AI (" + aiLevel.label + ")" : "You");
        game.setBlackName(aiColor == PieceColor.BLACK ? "AI (" + aiLevel.label + ")" : "You");
        // If AI is White, let it make the first move immediately.
        if (aiColor == PieceColor.WHITE) {
            requestAIMove();
        }
    }

    /** Core constructor — all others delegate here. */
    private GamePane(Game game, Runnable onNewGame, Runnable onExitToMenu) {
        this.game         = game;
        this.onNewGame    = onNewGame;
        this.onExitToMenu = onExitToMenu;
        this.whiteStats   = new StatsStore(game.getWhiteName());
        this.blackStats   = new StatsStore(game.getBlackName());

        buildLayout();
        scene = new Scene(root, cv.WIDTH, cv.HEIGHT, Color.BLACK);
        game.setListener(this);
        game.startClock();
        startClockTicker();
        lastMaterialBalance = GameStats.materialBalance(game.getBoard());
        refresh();
    }

    // =========================================================================
    // LAYOUT  (visual style is identical to the original)
    // =========================================================================

    private void buildLayout() {
        // Background.
        ImageView background = new ImageView(
                new Image(getClass().getResourceAsStream(BACKGROUND_PATH)));
        background.setPreserveRatio(false);
        background.fitWidthProperty().bind(root.widthProperty());
        background.fitHeightProperty().bind(root.heightProperty());

        // Board layers.
        Image boardImg = new Image(getClass().getResourceAsStream(BOARD_IMAGE_PATH));
        boardImage.setImage(boardImg);
        boardImage.setFitWidth(BOARD_SIZE);
        boardImage.setFitHeight(BOARD_SIZE);
        boardImage.setSmooth(true);

        boardPane.getChildren().addAll(boardImage, highlightLayer, pieceLayer, animLayer);
        boardPane.setPrefSize(BOARD_SIZE, BOARD_SIZE);
        boardPane.setMinSize(BOARD_SIZE, BOARD_SIZE);
        boardPane.setMaxSize(BOARD_SIZE, BOARD_SIZE);
        boardPane.setOnMouseClicked(this::onBoardClicked);

        // Side panel.
        Label blackCaption = caption("BLACK");
        Label whiteCaption = caption("WHITE");

        styleClock(blackClock);
        styleClock(whiteClock);

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

        openingLabel.setFont(cv.VARELA_BUTTON);
        openingLabel.setTextFill(Color.web("#9fc6e0"));
        openingLabel.setAlignment(Pos.CENTER);
        openingLabel.setPrefWidth(300);
        openingLabel.setWrapText(true);

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

        // Control buttons row.
        undoButton = sideButton("⏪ Undo");
        redoButton = sideButton("Redo ⏩");
        saveButton = sideButton("💾 Save");
        undoButton.setOnMouseClicked(e -> doUndo());
        redoButton.setOnMouseClicked(e -> doRedo());
        saveButton.setOnMouseClicked(e -> doSave());
        HBox controlRow = new HBox(8, undoButton, redoButton, saveButton);
        controlRow.setAlignment(Pos.CENTER);
        controlRow.setPrefWidth(300);
        HBox.setHgrow(undoButton, Priority.ALWAYS);
        HBox.setHgrow(redoButton, Priority.ALWAYS);
        HBox.setHgrow(saveButton, Priority.ALWAYS);

        // Replay controls.
        replayPrevButton = sideButton("◀ Prev");
        replayNextButton = sideButton("Next ▶");
        replayExitButton = sideButton("❌ Exit");
        replayExitButton.setPrefWidth(150);
        replayPrevButton.setOnMouseClicked(e -> replayStep(-1));
        replayNextButton.setOnMouseClicked(e -> replayStep(+1));
        replayExitButton.setOnMouseClicked(e -> { game.exitReplay(); refresh(); });
        replayBar = new HBox(6, replayPrevButton, replayNextButton, replayExitButton);
        replayBar.setAlignment(Pos.CENTER);
        replayBar.setVisible(false);
        replayBar.setManaged(false);
        HBox.setHgrow(replayPrevButton, Priority.ALWAYS);
        HBox.setHgrow(replayNextButton, Priority.ALWAYS);
        HBox.setHgrow(replayExitButton, Priority.ALWAYS);

        // Menu button (UNCHANGED style from original).
        Button menuButton = new Button("MENU");
        menuButton.setFont(cv.VARELA_BUTTON);
        menuButton.setTextFill(Color.WHITE);
        menuButton.setStyle(cv.STYLE_BSP);
        menuButton.setPrefWidth(300);
        menuButton.setPrefHeight(60);
        withHoverScale(menuButton);
        menuButton.setOnMouseClicked(event -> onMenuClicked());

        // PGN / FEN export buttons.
        Button pgnButton = sideButton("📋 PGN");
        Button fenButton = sideButton("📋 FEN");
        pgnButton.setOnMouseClicked(e -> copyToClipboard(
                game.toPGN(resultString()), "PGN copied!"));
        fenButton.setOnMouseClicked(e -> copyToClipboard(game.toFEN(), "FEN copied!"));
        HBox exportRow = new HBox(8, pgnButton, fenButton);
        exportRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(pgnButton, Priority.ALWAYS);
        HBox.setHgrow(fenButton, Priority.ALWAYS);

        VBox blackClockBox = new VBox(4, blackCaption, blackClock);
        blackClockBox.setAlignment(Pos.CENTER);
        VBox whiteClockBox = new VBox(4, whiteClock, whiteCaption);
        whiteClockBox.setAlignment(Pos.CENTER);

        sidePanel.setPrefWidth(300);
        sidePanel.setMinWidth(300);
        sidePanel.setMaxWidth(300);
        sidePanel.setAlignment(Pos.TOP_CENTER);
        sidePanel.setPadding(new Insets(10));
        sidePanel.setStyle(cv.STYLE_GAME_PANEL);
        sidePanel.getChildren().addAll(
                blackClockBox, moveList, openingLabel, turnLabel, checkLabel,
                whiteClockBox, controlRow, replayBar, exportRow, menuButton);

        HBox content = new HBox(40, boardPane, sidePanel);
        content.setAlignment(Pos.CENTER);

        overlayLayer.setVisible(false);
        overlayLayer.setMouseTransparent(true);

        root.getChildren().addAll(background, content, overlayLayer);
    }

    private void styleClock(Label l) {
        l.setFont(cv.VARELA_CLOCK);
        l.setPrefWidth(300);
        l.setPrefHeight(70);
        l.setAlignment(Pos.CENTER);
    }

    private Label caption(String text) {
        Label l = new Label(text);
        l.setFont(cv.VARELA_BUTTON);
        l.setTextFill(Color.web("#9fc6e0"));
        l.setAlignment(Pos.CENTER);
        l.setPrefWidth(300);
        return l;
    }

    private Button sideButton(String text) {
        Button b = new Button(text);
        b.setFont(cv.VARELA_BUTTON);
        b.setTextFill(Color.WHITE);
        b.setStyle(cv.STYLE_BSP);
        b.setPrefHeight(44);
        b.setMaxWidth(Double.MAX_VALUE);
        withHoverScale(b);
        return b;
    }

    // =========================================================================
    // COORDINATE MAPPING (unchanged)
    // =========================================================================

    private double squareLeft(int x) { return INSET + x * SQUARE; }
    private double squareTop(int y)  { return INSET + (7 - y) * SQUARE; }

    private int[] squareAt(double px, double py) {
        int col = (int) Math.floor((px - INSET) / SQUARE);
        int row = (int) Math.floor((py - INSET) / SQUARE);
        if (col < 0 || col > 7 || row < 0 || row > 7) return null;
        return new int[]{ col, 7 - row };
    }

    // =========================================================================
    // INPUT
    // =========================================================================

    private void onBoardClicked(MouseEvent event) {
        if (game.isGameOver() || overlayLayer.isVisible() || animating) return;
        if (game.isInReplay()) return;
        if (aiThinking) return;

        // In network mode, only allow clicks on our own turn.
        if (networkGame != null && game.turn() != networkGame.getMyColor()) return;

        int[] square = squareAt(event.getX(), event.getY());
        if (square == null) { clearSelection(); renderHighlights(); return; }
        int x = square[0], y = square[1];

        if (selectedX >= 0) {
            if (x == selectedX && y == selectedY) {
                clearSelection(); renderHighlights(); return;
            }
            List<Move> toTarget = new ArrayList<>();
            for (Move m : selectedMoves) { if (m.targets(x, y)) toTarget.add(m); }
            if (!toTarget.isEmpty()) {
                if (toTarget.get(0).isPromotion()) {
                    askForPromotion(selectedX, selectedY, x, y);
                } else {
                    attemptMove(selectedX, selectedY, x, y, null);
                }
                return;
            }
        }

        List<Move> moves = game.legalMovesFrom(x, y);
        if (moves.isEmpty()) { clearSelection(); } else {
            selectedX = x; selectedY = y; selectedMoves = moves;
        }
        renderHighlights();
    }

    private void attemptMove(int fromX, int fromY, int toX, int toY, PieceType promo) {
        // Record material before the move for accuracy tracking.
        int matBefore = GameStats.materialBalance(game.getBoard());

        boolean ok = game.tryMove(fromX, fromY, toX, toY, promo);
        if (!ok) return;

        // Network: send move to opponent.
        if (networkGame != null) {
            char promoChar = (promo == null) ? 0
                    : (promo == PieceType.QUEEN ? 'Q'
                    : promo == PieceType.ROOK  ? 'R'
                    : promo == PieceType.BISHOP ? 'B' : 'N');
            networkGame.sendMove(fromX, fromY, toX, toY, promoChar);
        }

        // Track accuracy.
        int matAfter = GameStats.materialBalance(game.getBoard());
        // The side that just moved is now the OTHER turn (turn already switched).
        PieceColor movedColor = game.turn().other();
        int accuracy = GameStats.computeMoveAccuracy(matBefore, matAfter, movedColor);
        gameStats.recordMove(accuracy, 0);
    }

    private void onMenuClicked() {
        if (game.isGameOver()) exitToMenu();
        else showLeaveConfirm();
    }

    private void exitToMenu() {
        recordFinalStats();
        game.setListener(null);
        game.abandon();
        if (ai != null) ai.shutdown();
        if (networkGame != null) networkGame.shutdown();
        onExitToMenu.run();
    }

    public Game getGame() { return game; }

    // =========================================================================
    // UNDO / REDO / SAVE / REPLAY
    // =========================================================================

    private void doUndo() {
        if (animating) return;
        if (ai != null) {
            // When playing against AI, undo two half-moves (AI + human).
            game.undoMove();
        }
        game.undoMove();
    }

    private void doRedo() {
        if (animating) return;
        game.redoMove();
        // If AI plays next, trigger it.
        maybeRequestAI();
    }

    private void doSave() {
        try {
            Path saved = GameSaver.saveGame(game);
            showToast("Saved: " + saved.getFileName());
        } catch (IOException e) {
            showToast("Save failed: " + e.getMessage());
        }
    }

    private void replayStep(int delta) {
        int current = game.isInReplay() ? game.replayIndex() : game.getMoveHistory().size() - 1;
        int next = current + delta;
        if (next < 0) { game.exitReplay(); refresh(); return; }
        game.replayGoto(next);
        updateReplayBar();
        renderPieces();
        renderHighlights();
    }

    // =========================================================================
    // AI INTEGRATION
    // =========================================================================

    private void maybeRequestAI() {
        if (ai != null && !game.isGameOver() && game.turn() == ai.getSide() && !game.isInReplay()) {
            requestAIMove();
        }
    }

    private void requestAIMove() {
        if (aiThinking) return;
        aiThinking = true;
        ai.requestMove(game.getBoard(), move -> {
            aiThinking = false;
            if (move == null || game.isGameOver()) return;
            int matBefore = GameStats.materialBalance(game.getBoard());
            animateMove(move, () -> {
                game.playMove(move);
                int matAfter = GameStats.materialBalance(game.getBoard());
                int accuracy = GameStats.computeMoveAccuracy(matBefore, matAfter, move.piece.color);
                gameStats.recordMove(accuracy, 0);
            });
        });
    }

    // =========================================================================
    // NETWORK GAME SUPPORT
    // =========================================================================

    /** Attaches this pane to an active network game. */
    public void setNetworkGame(NetworkGame ng) {
        this.networkGame = ng;
        ng.setOnGameEvent(this::handleNetworkEvent);
    }

    private void handleNetworkEvent(String line) {
        String cmd = network.NetworkProtocol.command(line);
        switch (cmd) {
            case network.NetworkProtocol.MOVE:
                int[] parts = network.NetworkProtocol.parseMove(
                        network.NetworkProtocol.payload(line));
                if (parts == null) break;
                PieceType promo = promoFromChar((char) parts[4]);
                attemptMove(parts[0], parts[1], parts[2], parts[3], promo);
                break;
            case network.NetworkProtocol.RESIGN:
                PieceColor winner = networkGame.getMyColor();
                game.abandon();
                showGameOver();
                break;
            case network.NetworkProtocol.DRAW_OFFER:
                showDrawOffer();
                break;
            case network.NetworkProtocol.DRAW_ACCEPT:
                game.abandon();
                showGameOver();
                break;
        }
    }

    private PieceType promoFromChar(char c) {
        switch (Character.toUpperCase(c)) {
            case 'Q': return PieceType.QUEEN;
            case 'R': return PieceType.ROOK;
            case 'B': return PieceType.BISHOP;
            case 'N': return PieceType.KNIGHT;
            default:  return null;
        }
    }

    // =========================================================================
    // RENDERING
    // =========================================================================

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
                ImageView view = createPieceView(p, squareLeft(x), squareTop(y));
                pieceLayer.getChildren().add(view);
            }
        }
    }

    /**
     * Creates an {@link ImageView} for a piece with 3D-look effects:
     * DropShadow for depth and a subtle InnerShadow for the top-light sheen.
     */
    private ImageView createPieceView(Piece p, double x, double y) {
        ImageView view = new ImageView(pieceImage(p));
        view.setFitWidth(SQUARE);
        view.setFitHeight(SQUARE);
        view.setSmooth(true);
        view.setX(x);
        view.setY(y);

        // 3D-look: drop shadow beneath the piece.
        DropShadow shadow = new DropShadow();
        shadow.setRadius(SQUARE * 0.12);
        shadow.setOffsetX(SQUARE * 0.04);
        shadow.setOffsetY(SQUARE * 0.06);
        shadow.setColor(Color.rgb(0, 0, 0, 0.55));
        view.setEffect(shadow);

        return view;
    }

    // --- Smooth move animation ---

    /**
     * Animates a piece sliding from its source to destination, then calls afterAnim.
     * If the move involves a capture, the captured piece is removed instantly.
     * If animation is disabled or a null move is passed, the callback fires immediately.
     */
    private void animateMove(Move m, Runnable afterAnim) {
        if (m == null) { afterAnim.run(); return; }

        Piece movingPiece = game.getBoard().pieceAt(m.fromX, m.fromY);
        if (movingPiece == null) { afterAnim.run(); return; }

        animating = true;
        double startX = squareLeft(m.fromX);
        double startY = squareTop(m.fromY);
        double endX   = squareLeft(m.toX);
        double endY   = squareTop(m.toY);

        // Temporarily remove the piece from the static layer and put it on animLayer.
        renderPieces();   // re-render without the moving piece temporarily
        ImageView animPiece = createPieceView(movingPiece, startX, startY);
        // Remove the source piece from pieceLayer to avoid double-draw during animation.
        pieceLayer.getChildren().removeIf(n -> {
            if (n instanceof ImageView iv) {
                return Math.abs(iv.getX() - startX) < 1 && Math.abs(iv.getY() - startY) < 1;
            }
            return false;
        });
        animLayer.getChildren().add(animPiece);

        TranslateTransition tt = new TranslateTransition(Duration.millis(160), animPiece);
        tt.setByX(endX - startX);
        tt.setByY(endY - startY);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.setOnFinished(ev -> {
            animLayer.getChildren().clear();
            afterAnim.run();
            animating = false;
            maybeRequestAI();
        });
        tt.play();
    }

    private void renderHighlights() {
        highlightLayer.getChildren().clear();

        // Last move.
        List<Move> moves = game.getMoveHistory().getMoves();
        if (!moves.isEmpty()) {
            Move last = moves.get(moves.size() - 1);
            addSquareHighlight(last.fromX, last.fromY, COLOR_LAST_MOVE);
            addSquareHighlight(last.toX,   last.toY,   COLOR_LAST_MOVE);
        }

        // Check highlight.
        if (!game.isGameOver() && game.isInCheck(game.turn())) {
            Piece king = game.getBoard().findKing(game.turn());
            if (king != null) addSquareHighlight(king.posX, king.posY, COLOR_CHECK);
        }

        // Selection + legal destinations.
        if (selectedX >= 0 && !game.isInReplay()) {
            addSquareHighlight(selectedX, selectedY, COLOR_SELECTED);
            for (Move m : selectedMoves) {
                if (m.isCapture()) {
                    Circle ring = new Circle(
                            squareLeft(m.toX) + SQUARE / 2,
                            squareTop(m.toY)  + SQUARE / 2,
                            SQUARE * 0.42);
                    ring.setFill(Color.TRANSPARENT);
                    ring.setStroke(COLOR_CAPTURE);
                    ring.setStrokeWidth(SQUARE * 0.08);
                    highlightLayer.getChildren().add(ring);
                } else {
                    Circle dot = new Circle(
                            squareLeft(m.toX) + SQUARE / 2,
                            squareTop(m.toY)  + SQUARE / 2,
                            SQUARE * 0.13);
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
        updateOpeningLabel();
        updateReplayBar();
        undoButton.setDisable(!game.getMoveHistory().canUndo() || game.isInReplay());
        redoButton.setDisable(!game.getMoveHistory().canRedo() || game.isInReplay());
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
        if (game.isInReplay()) {
            turnLabel.setText("REPLAY  ply " + (game.replayIndex() + 1));
            checkLabel.setVisible(false);
            return;
        }
        if (ai != null && aiThinking) {
            turnLabel.setText("AI is thinking…");
            checkLabel.setVisible(false);
            return;
        }
        turnLabel.setText(game.turn() == PieceColor.WHITE ? "WHITE TO MOVE" : "BLACK TO MOVE");
        checkLabel.setVisible(game.isInCheck(game.turn()));
    }

    private void rebuildMoveList() {
        List<String> items = new ArrayList<>();
        List<Move> histMoves = game.getMoveHistory().getMoves();
        for (int i = 0; i < histMoves.size(); i++) {
            Move m = histMoves.get(i);
            int moveNo = i / 2 + 1;
            String prefix = (i % 2 == 0) ? (moveNo + ".") : (moveNo + " ...");
            String suffix = "";
            if (i == histMoves.size() - 1) {
                if (game.status() == GameStatus.CHECKMATE) suffix = " #";
                else if (game.isInCheck(m.piece.color.other())) suffix = " +";
            }
            items.add(prefix + " " + m.notation() + suffix);
        }
        moveList.setItems(FXCollections.observableArrayList(items));
        if (!items.isEmpty()) moveList.scrollTo(items.size() - 1);
    }

    private void updateOpeningLabel() {
        String opening = game.openingName();
        openingLabel.setText(opening.isEmpty() ? "" : "📖 " + opening);
    }

    private void updateReplayBar() {
        boolean inReplay = game.isInReplay();
        replayBar.setVisible(inReplay);
        replayBar.setManaged(inReplay);
    }

    private String formatClock(long millis) {
        long totalSeconds = Math.max(0, millis) / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // =========================================================================
    // PIECE IMAGES
    // =========================================================================

    private Image pieceImage(Piece p) {
        return pieceImageByName("" + p.color.code + p.type.code);
    }

    private Image pieceImageByName(String assetName) {
        return PIECE_IMAGE_CACHE.computeIfAbsent(assetName,
                name -> new Image(getClass().getResourceAsStream(PIECE_IMAGES_DIR + name + ".png")));
    }

    // =========================================================================
    // OVERLAYS (visual style UNCHANGED from original)
    // =========================================================================

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

    private void askForPromotion(int fromX, int fromY, int toX, int toY) {
        promoFromX = fromX; promoFromY = fromY;
        promoToX   = toX;   promoToY   = toY;

        Text prompt = new Text("CHOOSE PROMOTION");
        prompt.setFont(cv.NEW_ROCKER_MEDIUM);
        prompt.setFill(Color.WHITE);

        HBox choices = new HBox(20);
        choices.setAlignment(Pos.CENTER);
        for (PieceType type : PieceType.PROMOTION_TYPES) {
            String assetName = "" + game.turn().code + type.code;
            ImageView icon = new ImageView(pieceImageByName(assetName));
            icon.setFitWidth(90); icon.setFitHeight(90); icon.setSmooth(true);
            Button button = new Button();
            button.setGraphic(icon);
            button.setStyle(cv.STYLE_BSP);
            button.setPrefSize(110, 110);
            withHoverScale(button);
            button.setOnMouseClicked(event -> {
                hideOverlay();
                attemptMove(promoFromX, promoFromY, promoToX, promoToY, type);
            });
            choices.getChildren().add(button);
        }

        VBox panelContent = new VBox(30, prompt, choices);
        panelContent.setAlignment(Pos.CENTER);
        panelContent.setPadding(new Insets(30, 50, 30, 50));
        showOverlay(overlayPanel(panelContent));
    }

    private void showGameOver() {
        Text title  = new Text(resultTitle());
        title.setFont(cv.NEW_ROCKER_MEDIUM);
        title.setFill(Color.WHITE);

        Text reason = new Text(resultReason());
        reason.setFont(cv.VARELA_BUTTON);
        reason.setFill(Color.web("#9fc6e0"));

        Button newGameButton = overlayButton("NEW GAME");
        newGameButton.setOnMouseClicked(event -> onNewGame.run());
        Button backButton = overlayButton("BACK TO MENU");
        backButton.setOnMouseClicked(event -> exitToMenu());

        // Stats panel inside overlay.
        Label statsLabel = new Label(buildStatsText());
        statsLabel.setFont(cv.VARELA_BUTTON);
        statsLabel.setTextFill(Color.web("#9fc6e0"));
        statsLabel.setWrapText(true);
        statsLabel.setAlignment(Pos.CENTER);

        HBox buttons = new HBox(30, newGameButton, backButton);
        buttons.setAlignment(Pos.CENTER);
        VBox content = new VBox(16, title, reason, statsLabel, buttons);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40, 60, 40, 60));
        showOverlay(overlayPanel(content));
    }

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
        content.setPadding(new Insets(40, 60, 40, 60));
        showOverlay(overlayPanel(content));
    }

    private void showDrawOffer() {
        Text question = new Text("DRAW OFFERED");
        question.setFont(cv.NEW_ROCKER_MEDIUM);
        question.setFill(Color.WHITE);

        Button acceptButton = overlayButton("ACCEPT DRAW");
        Button declineButton = overlayButton("DECLINE");
        acceptButton.setOnMouseClicked(e -> {
            hideOverlay();
            if (networkGame != null) networkGame.sendDrawAccept();
            game.abandon();
            showGameOver();
        });
        declineButton.setOnMouseClicked(e -> {
            hideOverlay();
            if (networkGame != null) networkGame.sendDrawDecline();
        });
        HBox buttons = new HBox(30, acceptButton, declineButton);
        buttons.setAlignment(Pos.CENTER);
        VBox content = new VBox(24, question, buttons);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40, 60, 40, 60));
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

    // =========================================================================
    // RESULT STRINGS
    // =========================================================================

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

    private String resultString() {
        if (game.winner() == PieceColor.WHITE) return "1-0";
        if (game.winner() == PieceColor.BLACK) return "0-1";
        if (game.isGameOver())                  return "1/2-1/2";
        return "*";
    }

    private String buildStatsText() {
        int moves = game.getMoveHistory().size();
        double acc = gameStats.getAverageAccuracy();
        return String.format("Moves played: %d | Accuracy: %.0f%%", moves, acc);
    }

    // =========================================================================
    // STATISTICS
    // =========================================================================

    private void recordFinalStats() {
        if (!game.isGameOver()) return;
        StatsStore.GameResult wResult, bResult;
        if (game.winner() == PieceColor.WHITE) {
            wResult = StatsStore.GameResult.WIN;
            bResult = StatsStore.GameResult.LOSS;
        } else if (game.winner() == PieceColor.BLACK) {
            wResult = StatsStore.GameResult.LOSS;
            bResult = StatsStore.GameResult.WIN;
        } else {
            wResult = bResult = StatsStore.GameResult.DRAW;
        }
        whiteStats.recordResult(wResult);
        blackStats.recordResult(bResult);
        whiteStats.recordAccuracy(gameStats.getAverageAccuracy());
        blackStats.recordAccuracy(gameStats.getAverageAccuracy());
    }

    // =========================================================================
    // CLOCK TICKER (identical to original)
    // =========================================================================

    private void startClockTicker() {
        clockTimeline = new Timeline(new KeyFrame(Duration.millis(100), event -> {
            game.tickClock();
            updateClockLabels();
        }));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    // =========================================================================
    // GAME LISTENER (identical callbacks, but triggers AI)
    // =========================================================================

    @Override
    public void onPositionChanged() {
        clearSelection();
        refresh();
        maybeRequestAI();
    }

    @Override
    public void onGameOver(GameStatus status, PieceColor winner) {
        clearSelection();
        renderHighlights();
        updateClockLabels();
        updateTurnIndicator();
        if (clockTimeline != null) clockTimeline.stop();
        recordFinalStats();
        showGameOver();
    }

    // =========================================================================
    // CLEANUP
    // =========================================================================

    public void dispose() {
        if (clockTimeline != null) { clockTimeline.stop(); clockTimeline = null; }
        game.setListener(null);
        game.pauseClock();
        if (ai != null) ai.shutdown();
        if (networkGame != null) networkGame.shutdown();
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void clearSelection() {
        selectedX = -1; selectedY = -1;
        selectedMoves = new ArrayList<>();
    }

    private void withHoverScale(Button button) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(20), button);
        button.setOnMouseEntered(event -> {
            scale.setToX(1.1); scale.setToY(1.1);
            scale.stop(); scale.playFromStart();
        });
        button.setOnMouseExited(event -> {
            scale.setToX(1.0); scale.setToY(1.0);
            scale.stop(); scale.playFromStart();
        });
    }

    private void copyToClipboard(String text, String toastMsg) {
        javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
        cc.putString(text);
        cb.setContent(cc);
        showToast(toastMsg);
    }

    /** Briefly shows a transient status message in the turn-label area. */
    private void showToast(String msg) {
        String original = turnLabel.getText();
        turnLabel.setText(msg);
        new Timeline(new KeyFrame(Duration.seconds(2),
                e -> { if (turnLabel.getText().equals(msg)) turnLabel.setText(original); }))
                .play();
    }
}
