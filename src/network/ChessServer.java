package network;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * LAN chess server.
 *
 * Accepts exactly two player connections (and optionally unlimited spectators).
 * Once both players are connected the server starts the game and relays moves
 * between them until the game ends or a player disconnects.
 *
 * Architecture:
 *   - Main accept loop runs on a daemon thread.
 *   - Each player connection is handled by its own daemon thread.
 *   - All callbacks are posted to the JavaFX Application Thread.
 *   - The server does NOT validate chess rules; the clients do that locally.
 *     The server only relays moves, so both clients stay in sync.
 */
public class ChessServer {

    private final int port;
    private volatile boolean running = false;

    private ServerSocket serverSocket;
    private Connection white, black;

    /** Called when a player message arrives; args: (playerIndex 0=white/1=black, message). */
    private BiConsumer<Integer, String> onMessage;

    /** Called when the server is ready and waiting (server IP, port). */
    private BiConsumer<String, Integer> onReady;

    /** Called when both players are connected and the game can start. */
    private Runnable onBothConnected;

    /** Called on any fatal error. */
    private Consumer<String> onError;

    public ChessServer(int port) {
        this.port = port;
    }

    // --- --- --- --- --- CALLBACKS --- --- --- --- --- //

    public void setOnMessage(BiConsumer<Integer, String> cb)    { onMessage = cb; }
    public void setOnReady(BiConsumer<String, Integer> cb)      { onReady = cb; }
    public void setOnBothConnected(Runnable cb)                 { onBothConnected = cb; }
    public void setOnError(Consumer<String> cb)                 { onError = cb; }

    // --- --- --- --- --- START / STOP --- --- --- --- --- //

    /**
     * Starts listening for incoming connections on a daemon thread.
     * Returns immediately; callbacks fire on the JavaFX thread.
     */
    public void start() {
        running = true;
        Thread acceptThread = new Thread(this::acceptLoop, "chess-server-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    /** Shuts the server down and closes all connections. */
    public void stop() {
        running = false;
        closeQuietly(serverSocket);
        if (white != null) white.close();
        if (black != null) black.close();
    }

    // --- --- --- --- --- SENDING --- --- --- --- --- //

    /** Sends a message to the White player. */
    public void sendToWhite(String msg) { if (white != null) white.send(msg); }

    /** Sends a message to the Black player. */
    public void sendToBlack(String msg) { if (black != null) black.send(msg); }

    /** Sends a message to both players. */
    public void broadcast(String msg) {
        sendToWhite(msg);
        sendToBlack(msg);
    }

    // --- --- --- --- --- ACCEPT LOOP --- --- --- --- --- //

    private void acceptLoop() {
        try {
            serverSocket = new ServerSocket(port);
            String ip = InetAddress.getLocalHost().getHostAddress();
            post(() -> { if (onReady != null) onReady.accept(ip, port); });

            // Accept White player.
            Socket ws = serverSocket.accept();
            white = new Connection(ws);
            white.send(NetworkProtocol.color('W'));

            // Accept Black player.
            Socket bs = serverSocket.accept();
            black = new Connection(bs);
            black.send(NetworkProtocol.color('B'));

            // Tell both players the game is starting.
            broadcast(NetworkProtocol.start());
            post(() -> { if (onBothConnected != null) onBothConnected.run(); });

            // Start reader threads.
            startReader(white, 0);
            startReader(black, 1);

        } catch (IOException e) {
            if (running) post(() -> { if (onError != null) onError.accept(e.getMessage()); });
        }
    }

    private void startReader(Connection conn, int playerIndex) {
        Thread t = new Thread(() -> {
            try {
                String line;
                while ((line = conn.reader.readLine()) != null) {
                    String msg = line;
                    post(() -> { if (onMessage != null) onMessage.accept(playerIndex, msg); });
                }
            } catch (IOException e) {
                if (running) {
                    String err = "Player " + playerIndex + " disconnected";
                    post(() -> { if (onError != null) onError.accept(err); });
                }
            }
        }, "chess-server-reader-" + playerIndex);
        t.setDaemon(true);
        t.start();
    }

    // --- --- --- --- --- HELPERS --- --- --- --- --- //

    private static void post(Runnable r) {
        javafx.application.Platform.runLater(r);
    }

    private static void closeQuietly(Closeable c) {
        if (c != null) try { c.close(); } catch (IOException ignored) { }
    }

    // --- --- --- --- --- CONNECTION WRAPPER --- --- --- --- --- //

    private static class Connection {
        final Socket socket;
        final BufferedReader reader;
        final PrintWriter writer;

        Connection(Socket socket) throws IOException {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        }

        void send(String msg) { writer.println(msg); }

        void close() {
            closeQuietly(socket);
        }

        private static void closeQuietly(Closeable c) {
            if (c != null) try { c.close(); } catch (IOException ignored) { }
        }
    }
}
