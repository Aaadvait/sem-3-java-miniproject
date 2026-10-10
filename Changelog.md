## UI navigation and VS Code launch fix (2026-10-10)
- Reset and hide the mode-selection overlay when returning to the main menu, preventing AI difficulty buttons from remaining visible at the top edge.
- Use absolute transition destinations for the mode panel and back button so repeated menu visits do not accumulate translation offsets.
- Reset AI difficulty visibility, mode selection, and button highlights on return.
- Added VS Code launch/task/settings files so F5 builds with JavaFX and launches the game with the correct module path.
- Updated Windows build/run scripts to copy JavaFX resources into the runtime classpath.

# Changelog

## Online Play addition
- Added a separate Online Play option without removing the existing LAN mode.
- Added room creation and six-character room-code joining.
- Added `network.OnlineServer`, a Java TCP relay server that can run on a public VPS so players on different networks can connect.
- Added Online Play lobby fields for public server hostname, port, and room code.
- Added pending room-code handling to avoid a UI callback race.
- Added server start and project build scripts and deployment instructions.

## Existing project maintenance
- Retains the original JavaFX theme and bundled image/font resources.
- Generated build outputs are not part of the source package.


## Navigation callback fix
- Fixed the null Stage reference that made New Game appear unresponsive after switching away from the main-menu Scene.
- Updated game-over and confirmation buttons to use standard JavaFX action handlers.
- Network matches clearly mark New Game unavailable until a coordinated rematch flow is implemented.

## UI and online lobby corrections (2026-10-10)
- Removed clipped/ellipsized mode, time-control, and AI difficulty button labels by using concise time labels and properly sized controls.
- Widened Undo/Redo/Save controls in the game sidebar.
- Reworked Online Play lobby to show distinct player-name, server-host, port, and room-code labels; blank host is validated with a specific message.
- Added an embedded local relay test option for testing room creation/joining on one computer. Cross-network play still requires deploying OnlineServer to a public host.
- Verified Java compilation and tested online relay room creation/joining with two TCP clients.
