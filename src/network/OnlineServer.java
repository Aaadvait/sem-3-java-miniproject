package network;

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;

/**
 * Public online relay server for cross-network chess.
 * Run this on a publicly reachable VPS/server. Clients connect outbound to this
 * server, so neither player needs to share Wi-Fi or configure router port forwarding.
 *
 * Protocol: client sends HELLO, then ROOM_CREATE or ROOM_JOIN CODE. The server
 * assigns colors and relays chess commands between the two members of a room.
 * This server relays moves; the desktop clients validate moves using the chess engine.
 */
public final class OnlineServer {
    public static final int DEFAULT_PORT = 55766;
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ConcurrentMap<String, Room> ROOMS = new ConcurrentHashMap<>();
    private static volatile ServerSocket activeServerSocket;

    private OnlineServer() { }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        ServerSocket server = bind(port);
        activeServerSocket = server;
        acceptLoop(server);
    }

    /** Starts an embedded relay for local testing. It is reachable only on this computer
     * unless the host network explicitly exposes the port; use a public deployment for
     * play between unrelated networks. */
    public static synchronized void startInBackground(int port) throws IOException {
        if (activeServerSocket != null && !activeServerSocket.isClosed()) return;
        ServerSocket server = bind(port);
        activeServerSocket = server;
        Thread acceptThread = new Thread(() -> acceptLoop(server), "chess-online-local-relay");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private static ServerSocket bind(int port) throws IOException {
        ServerSocket server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress("0.0.0.0", port));
        System.out.println("Chess Online relay listening on TCP port " + port);
        System.out.println("Keep this process running while players use Online Play.");
        return server;
    }

    private static void acceptLoop(ServerSocket server) {
        while (!server.isClosed()) {
            try {
                Socket socket = server.accept();
                socket.setKeepAlive(true);
                Thread thread = new Thread(() -> handle(socket), "online-client-" + socket.getRemoteSocketAddress());
                thread.setDaemon(true);
                thread.start();
            } catch (SocketException closed) {
                if (!server.isClosed()) closed.printStackTrace();
                return;
            } catch (IOException e) {
                if (!server.isClosed()) e.printStackTrace();
            }
        }
    }

    private static void handle(Socket socket) {
        Peer peer = null;
        try {
            peer = new Peer(socket);
            String hello = peer.in.readLine();
            if (hello == null || !hello.startsWith("HELLO ")) {
                peer.send("ERROR Expected HELLO");
                return;
            }
            String request = peer.in.readLine();
            if (request == null) return;
            if (request.equals("ROOM_CREATE")) {
                String code = newRoomCode();
                Room room = new Room(code);
                room.white = peer;
                peer.room = room;
                peer.color = 'W';
                ROOMS.put(code, room);
                peer.send("ROOM_CODE " + code);
                peer.send("COLOR W");
                System.out.println("Room " + code + " created");
            } else if (request.startsWith("ROOM_JOIN ")) {
                String code = request.substring("ROOM_JOIN ".length()).trim().toUpperCase(Locale.ROOT);
                Room room = ROOMS.get(code);
                if (room == null) {
                    peer.send("ERROR Room not found. Check the code or ask the host to create a new room.");
                    return;
                }
                synchronized (room) {
                    if (room.black != null || room.closed) {
                        peer.send("ERROR Room is already full or closed.");
                        return;
                    }
                    room.black = peer;
                    peer.room = room;
                    peer.color = 'B';
                    peer.send("COLOR B");
                    room.white.send("START");
                    peer.send("START");
                    System.out.println("Room " + code + " started");
                }
            } else {
                peer.send("ERROR Expected ROOM_CREATE or ROOM_JOIN CODE");
                return;
            }

            String line;
            while ((line = peer.in.readLine()) != null) {
                if (line.equals("PING")) { peer.send("PONG"); continue; }
                Room room = peer.room;
                if (room == null) continue;
                String command = line.contains(" ") ? line.substring(0, line.indexOf(' ')) : line;
                if (Arrays.asList("MOVE", "RESIGN", "DRAW_OFFER", "DRAW_ACCEPT", "DRAW_DECLINE").contains(command)) {
                    synchronized (room) {
                        Peer opponent = peer.color == 'W' ? room.black : room.white;
                        if (opponent != null) opponent.send(line);
                    }
                }
            }
        } catch (IOException ignored) {
            // Normal disconnect; notified below if the peer had joined a room.
        } finally {
            if (peer != null) removePeer(peer);
            try { socket.close(); } catch (IOException ignored) { }
        }
    }

    private static void removePeer(Peer peer) {
        Room room = peer.room;
        if (room == null) return;
        synchronized (room) {
            if (peer.color == 'W' && room.white == peer) room.white = null;
            if (peer.color == 'B' && room.black == peer) room.black = null;
            Peer other = peer.color == 'W' ? room.black : room.white;
            if (other != null) other.send("ERROR Opponent disconnected.");
            if (room.white == null || room.black == null) {
                room.closed = true;
                ROOMS.remove(room.code, room);
            }
        }
    }

    private static String newRoomCode() {
        String code;
        do {
            StringBuilder b = new StringBuilder(6);
            for (int i = 0; i < 6; i++) b.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            code = b.toString();
        } while (ROOMS.containsKey(code));
        return code;
    }

    private static final class Room {
        final String code;
        volatile Peer white;
        volatile Peer black;
        volatile boolean closed;
        Room(String code) { this.code = code; }
    }

    private static final class Peer {
        final Socket socket;
        final BufferedReader in;
        final PrintWriter out;
        volatile Room room;
        volatile char color;
        Peer(Socket socket) throws IOException {
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true);
        }
        synchronized void send(String message) {
            out.println(message);
            if (out.checkError()) {
                try { socket.close(); } catch (IOException ignored) { }
            }
        }
    }
}
