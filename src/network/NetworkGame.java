package network;

import game.Game;
import move.Move;
import pieces.PieceColor;
import pieces.PieceType;

import java.util.function.Consumer;

/**
 * Coordinates a LAN game between two human players.
 *
 * One player acts as the server (creates a game); the other connects as a client.
 * Both sides run the full chess engine locally; moves are validated before being
 * sent and applied on receipt without re-validation (the server is trusted).
 *
 * Spectator connections are handled by the server; this class is only concerned
 * with the two playing clients.
 *
 * Typical host flow:
 *   NetworkGame ng = NetworkGame.host("Alice", game, onMoveReceived, onError);
 *
 * Typical join flow:
 *   NetworkGame ng = NetworkGame.join("192.168.1.10", "Bob", game, onMoveReceived, onError);
 */
public class NetworkGame {

    private final Game          game;
    private final PieceColor    myColor;
    private final boolean       isHost;
    private ChessServer         server;   // non-null only when hosting
    private ChessClient         client;

    /** Fired (on FX thread) when the opponent plays a move. */
    private final Consumer<Move> onMoveReceived;

    /** Fired (on FX thread) on any network error. */
    private final Consumer<String> onError;

    /** Fired (on FX thread) when both players are connected and the game starts. */
    private Runnable onStart;

    /** Fired when the opponent resigns or offers/accepts a draw. */
    private Consumer<String> onGameEvent;

    // --- --- --- --- --- CONSTRUCTORS --- --- --- --- --- //

    private NetworkGame(Game game, PieceColor myColor, boolean isHost,
                        Consumer<Move> onMoveReceived, Consumer<String> onError) {
        this.game           = game;
        this.myColor        = myColor;
        this.isHost         = isHost;
        this.onMoveReceived = onMoveReceived;
        this.onError        = onError;
    }

    // --- --- --- --- --- FACTORY METHODS --- --- --- --- --- //

    /**
     * Creates a hosted LAN game. White plays as host.
     *
     * @param name           host player name
     * @param game           the Game object (fresh, untimed is fine)
     * @param onMoveReceived callback when opponent's move arrives
     * @param onError        callback on network failure
     * @return a NetworkGame configured as host/White
     */
    public static NetworkGame host(String name, Game game,
                                   Consumer<Move> onMoveReceived,
                                   Consumer<String> onError) {
        NetworkGame ng = new NetworkGame(game, PieceColor.WHITE, true, onMoveReceived, onError);
        ng.startServer(name);
        return ng;
    }

    /**
     * Joins a hosted LAN game. Black plays as client.
     *
     * @param host           server IP or hostname
     * @param name           client player name
     * @param game           a fresh Game object to play on
     * @param onMoveReceived callback when opponent's move arrives
     * @param onError        callback on network failure
     * @return a NetworkGame configured as client/Black
     */
    public static NetworkGame join(String host, String name, Game game,
                                   Consumer<Move> onMoveReceived,
                                   Consumer<String> onError) {
        NetworkGame ng = new NetworkGame(game, PieceColor.BLACK, false, onMoveReceived, onError);
        ng.connectToServer(host, name);
        return ng;
    }

    // --- --- --- --- --- SENDING --- --- --- --- --- //

    /**
     * Sends the local player's move to the opponent.
     * Should be called after the move has already been applied to the local board.
     */
    public void sendMove(int fromX, int fromY, int toX, int toY, char promoChar) {
        if (client != null) {
            client.sendMove(fromX, fromY, toX, toY, promoChar);
        }
    }

    public void sendResign()      { if (client != null) client.sendResign(); }
    public void sendDrawOffer()   { if (client != null) client.sendDrawOffer(); }
    public void sendDrawAccept()  { if (client != null) client.sendDrawAccept(); }
    public void sendDrawDecline() { if (client != null) client.sendDrawDecline(); }

    /** Shuts down all network connections. */
    public void shutdown() {
        if (client != null) client.disconnect();
        if (server != null) server.stop();
    }

    // --- --- --- --- --- CALLBACKS --- --- --- --- --- //

    public void setOnStart(Runnable cb)               { onStart = cb; }
    public void setOnGameEvent(Consumer<String> cb)   { onGameEvent = cb; }

    public PieceColor getMyColor() { return myColor; }

    // --- --- --- --- --- SERVER SIDE --- --- --- --- --- //

    private void startServer(String name) {
        server = new ChessServer(NetworkProtocol.DEFAULT_PORT);
        // The host also connects as a client on localhost so all message handling
        // is symmetric — one client object per player regardless of host/guest role.
        server.setOnReady((ip, port) -> {
            System.out.println("SYS: Chess server listening on " + ip + ":" + port);
            // Host connects to its own server.
            client = new ChessClient("localhost", port);
            setupClientCallbacks();
            client.connect(name);
        });
        server.setOnBothConnected(() -> {
            if (onStart != null) onStart.run();
        });
        server.setOnMessage((playerIdx, msg) -> {
            // Relay all messages from one player to the other.
            String cmd = NetworkProtocol.command(msg);
            if (cmd.equals(NetworkProtocol.MOVE) ||
                cmd.equals(NetworkProtocol.RESIGN) ||
                cmd.equals(NetworkProtocol.DRAW_OFFER) ||
                cmd.equals(NetworkProtocol.DRAW_ACCEPT) ||
                cmd.equals(NetworkProtocol.DRAW_DECLINE)) {
                // Relay to the other player.
                if (playerIdx == 0) server.sendToBlack(msg);
                else                server.sendToWhite(msg);
            }
        });
        server.setOnError(msg -> { if (onError != null) onError.accept(msg); });
        server.start();
    }

    // --- --- --- --- --- CLIENT SIDE --- --- --- --- --- //

    private void connectToServer(String host, String name) {
        client = new ChessClient(host, NetworkProtocol.DEFAULT_PORT);
        setupClientCallbacks();
        client.connect(name);
    }

    private void setupClientCallbacks() {
        client.setOnConnected(() -> System.out.println("SYS: Connected to chess server"));
        client.setOnMessage(this::handleIncoming);
        client.setOnError(msg -> { if (onError != null) onError.accept(msg); });
    }

    // --- --- --- --- --- MESSAGE DISPATCH --- --- --- --- --- //

    private void handleIncoming(String line) {
        String cmd     = NetworkProtocol.command(line);
        String payload = NetworkProtocol.payload(line);

        switch (cmd) {
            case NetworkProtocol.START:
                if (onStart != null) onStart.run();
                break;

            case NetworkProtocol.MOVE:
                int[] parts = NetworkProtocol.parseMove(payload);
                if (parts == null || onMoveReceived == null) break;
                // Translate the move into a Move object by searching legal moves.
                PieceColor opponentColor = myColor.other();
                char promoChar = (char) parts[4];
                PieceType promo = promoFromChar(promoChar);
                // Find the matching legal move on the local board.
                Move m = findLegalMove(parts[0], parts[1], parts[2], parts[3], promo, opponentColor);
                if (m != null) onMoveReceived.accept(m);
                break;

            case NetworkProtocol.RESIGN:
            case NetworkProtocol.DRAW_OFFER:
            case NetworkProtocol.DRAW_ACCEPT:
            case NetworkProtocol.DRAW_DECLINE:
            case NetworkProtocol.GAMEOVER:
                if (onGameEvent != null) onGameEvent.accept(line);
                break;

            case NetworkProtocol.PING:
                client.sendPing();   // send PONG
                break;
        }
    }

    // --- --- --- --- --- HELPERS --- --- --- --- --- //

    private Move findLegalMove(int fromX, int fromY, int toX, int toY,
                               PieceType promo, PieceColor color) {
        java.util.List<Move> moves = game.legalMovesFrom(fromX, fromY);
        for (Move m : moves) {
            if (m.toX == toX && m.toY == toY) {
                if (promo == null && !m.isPromotion()) return m;
                if (promo != null && m.promotion == promo) return m;
            }
        }
        return null;
    }

    private static PieceType promoFromChar(char c) {
        switch (Character.toUpperCase(c)) {
            case 'Q': return PieceType.QUEEN;
            case 'R': return PieceType.ROOK;
            case 'B': return PieceType.BISHOP;
            case 'N': return PieceType.KNIGHT;
            default:  return null;
        }
    }
}
