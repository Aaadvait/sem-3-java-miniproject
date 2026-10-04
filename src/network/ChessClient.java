package network;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

/**
 * LAN chess client.
 *
 * Connects to a {@link ChessServer} and exchanges messages with it.
 * Incoming messages are delivered to a callback on the JavaFX Application Thread.
 *
 * Typical flow:
 *   1. Client connects → server sends COLOR and START.
 *   2. Client plays a move → calls {@link #sendMove}.
 *   3. Server relays the opponent's move → callback fires with a MOVE message.
 */
public class ChessClient {

    private final String host;
    private final int    port;

    private Socket       socket;
    private BufferedReader reader;
    private PrintWriter  writer;
    private volatile boolean connected = false;

    /** Fired (on the FX thread) for every message received from the server. */
    private Consumer<String> onMessage;

    /** Fired (on the FX thread) when the connection succeeds. */
    private Runnable onConnected;

    /** Fired (on the FX thread) on any error or disconnection. */
    private Consumer<String> onError;

    public ChessClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // --- --- --- --- --- CALLBACKS --- --- --- --- --- //

    public void setOnMessage(Consumer<String> cb)    { onMessage = cb; }
    public void setOnConnected(Runnable cb)           { onConnected = cb; }
    public void setOnError(Consumer<String> cb)       { onError = cb; }

    // --- --- --- --- --- CONNECT / DISCONNECT --- --- --- --- --- //

    /**
     * Attempts to connect to the server on a daemon thread.
     * Returns immediately; fires {@code onConnected} when the connection is established.
     */
    public void connect(String playerName) {
        Thread t = new Thread(() -> {
            try {
                socket = new Socket(host, port);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                connected = true;
                // Send our name.
                writer.println(NetworkProtocol.hello(playerName));
                post(() -> { if (onConnected != null) onConnected.run(); });
                readLoop();
            } catch (IOException e) {
                post(() -> { if (onError != null) onError.accept(e.getMessage()); });
            }
        }, "chess-client");
        t.setDaemon(true);
        t.start();
    }

    /** Closes the connection. */
    public void disconnect() {
        connected = false;
        closeQuietly(socket);
    }

    // --- --- --- --- --- SENDING --- --- --- --- --- //

    public void sendMove(int fromX, int fromY, int toX, int toY, char promo) {
        send(NetworkProtocol.move(fromX, fromY, toX, toY, promo));
    }

    public void sendResign()         { send(NetworkProtocol.resign()); }
    public void sendDrawOffer()      { send(NetworkProtocol.drawOffer()); }
    public void sendDrawAccept()     { send(NetworkProtocol.drawAccept()); }
    public void sendDrawDecline()    { send(NetworkProtocol.drawDecline()); }
    public void sendPing()           { send(NetworkProtocol.ping()); }

    public boolean isConnected() { return connected; }

    // --- --- --- --- --- READ LOOP --- --- --- --- --- //

    private void readLoop() throws IOException {
        String line;
        while (connected && (line = reader.readLine()) != null) {
            String msg = line;
            post(() -> { if (onMessage != null) onMessage.accept(msg); });
        }
    }

    // --- --- --- --- --- HELPERS --- --- --- --- --- //

    private void send(String msg) {
        if (writer != null) writer.println(msg);
    }

    private static void post(Runnable r) {
        javafx.application.Platform.runLater(r);
    }

    private static void closeQuietly(Closeable c) {
        if (c != null) try { c.close(); } catch (IOException ignored) { }
    }
}
