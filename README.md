# Chess — JavaFX desktop project with LAN + Online Play

This is the complete project folder: Java sources, JavaFX resources, piece images, fonts, run/build scripts, and the optional online relay server. Generated `.class` files and bundled JavaFX SDK files are intentionally excluded.

## Requirements
- JDK 21 or compatible JDK
- JavaFX SDK (JavaFX 21+ recommended), installed separately

## Run directly from VS Code (configured for this project)
1. Open the **Chess_Project_Online_Complete** folder in VS Code (the folder containing `src`, `.vscode`, and `scripts`).
2. Ensure the Java Extension Pack is installed.
3. This project includes `.vscode/launch.json`, `.vscode/tasks.json`, and `.vscode/settings.json`. The launch configuration runs `scripts/build.bat` first and then starts the game with JavaFX enabled.
4. Press **F5** and select **Run Chess Game (JavaFX)**. Do not use the Code Runner extension's **Run Code** command, because it does not automatically apply the JavaFX module path.
5. The build script is configured for `C:\javafx-sdk-27\lib`. If your SDK is elsewhere, edit `JAVA_FX` in `scripts/build.bat` and the path in `.vscode/launch.json` and `.vscode/settings.json`.

## Run on Windows
1. Install a JDK and JavaFX SDK.
2. Edit `JAVA_FX` in `scripts/run.bat` to your JavaFX SDK `lib` folder.
3. Run `scripts/run.bat`.

## LAN multiplayer
- Both players must be on the same reachable local network.
- Choose **PLAY LAN → HOST GAME** on one PC and share its LAN IPv4 address.
- On the second PC choose **PLAY LAN**, enter the host IP, and join.
- Allow inbound TCP port `55765` in the host computer firewall.

## Cross-network Online Play
Online Play works across different Wi-Fi networks by connecting both desktop clients to a publicly reachable Java relay server. The relay server is included at `src/network/OnlineServer.java`; it is not a cloud service by itself and must be deployed somewhere with a public hostname/IP.

The lobby now has separate, clearly labelled **Player Name**, **Server Host / Public IP**, **Server Port**, and **Room Code** fields. Do not enter your name in the server-host field. The **START LOCAL TEST SERVER** button starts a relay inside the running application and sets the host to `127.0.0.1`; use this only to test locally (run a second app instance to join the room). It does not make the server publicly reachable. To play across different networks, deploy the server as described below and enter that public hostname/IP on both computers.

### Deploy the relay server
1. Use a public Linux VPS or server with Java 21 installed. A cloud VM is the most straightforward option. A computer behind home NAT is not enough unless you configure public port forwarding and have a reachable public IP.
2. Copy the entire project folder to the server.
3. Compile the server-only class: `javac -d out src/network/OnlineServer.java`
4. Start it: `java -cp out network.OnlineServer 55766`
5. Allow inbound TCP port `55766` in the server/cloud firewall. Keep the process running. On Linux, use a service manager such as systemd or a terminal multiplexer so it stays alive.
6. In the desktop app choose **ONLINE PLAY**, enter the server's public hostname/IP and port `55766`.
7. Player 1 selects **CREATE ROOM** and shares the displayed six-character code. Player 2 enters the same server host/port and room code, then selects **JOIN ROOM**.

### Network/security note
The included relay is a functional starter server using plain TCP. Do not expose it to untrusted public use as a production service without adding TLS, authentication/rate limiting, server-side legal-move validation, logging/monitoring, and resource limits. A publicly reachable host and open firewall port are required; the Java client cannot bypass NAT by itself.

## Project structure
- `src/ai`, `src/board`, `src/game`, `src/move`, `src/pieces`: chess rules, engine, AI and moves
- `src/gui`: JavaFX screens and visual resources
- `src/network`: LAN networking and online relay client/server
- `src/persistence`: save/load, FEN and PGN support
- `src/analytics`: game statistics and opening data
- `scripts`: Windows launch/build helpers

## Notes
- Keep resource paths under `src/gui/resources` intact.
- `bin/` and `out/` are generated build output and should not be committed as source.
- Online room codes are temporary; a room is removed when a player disconnects.
- Mode-selection buttons use complete labels and wider dimensions; time controls show concise time values instead of clipped text.
- Game side-panel controls have wider equal widths to prevent UNDO/REDO/SAVE text clipping.


## Navigation callback fix (2026-10-10)
- Fixed the Stage lookup used by New Game and other navigation actions. The menu Scene is detached while a game Scene is displayed, so calling `menuScene.getWindow()` during a game can return `null`; the app now resolves the active game Scene's Stage and retains it.
- Switched game-over and confirmation overlay buttons to JavaFX `setOnAction` handlers.
- The network game-over overlay now disables/relabels New Game because restarting a shared match requires a coordinated rematch/room handshake; Back to Menu remains available.
