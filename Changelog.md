# Chess Platform Transformation - Changelog

## Overview
This document outlines the architectural changes, feature additions, and file modifications made to transform the Java Chess Mini-Project into a professional-grade chess application. The core directive was strictly followed: **Add advanced functionality while completely preserving the original UI design, colors, and layout.**

---

## 1. Major Features Added

* **Realistic 3D Experience (No UI Redesign)**: Instead of replacing the original aesthetic, JavaFX `DropShadow` and `InnerShadow` effects were dynamically layered onto the existing 2D pieces to give them physical depth. Smooth `TranslateTransition` animations were added so pieces physically slide across the board.
* **Professional AI Engine**: Built a multi-threaded chess engine using the **Minimax algorithm with Alpha-Beta Pruning**. It features Move Ordering (MVV-LVA) and Piece-Square table evaluations. It includes 5 difficulty levels and an artificial 2-4 second "human thinking delay".
* **LAN Multiplayer**: A complete custom TCP Socket architecture allowing two computers on the same network to play together, featuring a Host/Join lobby system.
* **Advanced Chess Mechanics**: Full Undo/Redo capability, PGN (Portable Game Notation) exporting/importing, FEN clipboard exporting, and a Replay Mode to step through historical moves.
* **Clocks & Game Modes**: The original time controls were expanded into a full suite: Bullet (1+0, 2+1), Blitz (3+0, 5+0), Rapid (10+0, 15+10, 30+0), and Classical (60+30). Fischer increments are fully supported.
* **Analytics & Openings**: The game now tracks user Win/Loss/Draw statistics and calculates Move Accuracy based on material evaluations. It also features a 60+ ECO Opening Book that recognizes and displays the name of the chess opening being played in real-time.

---

## 2. Files Added (New Packages)

To maintain a clean MVC architecture, several new packages were introduced to the `src/` directory:

### AI System (`src/ai/`)
* `ChessAI.java` - Background thread manager for async engine calculations.
* `Minimax.java` - The core search tree algorithm.
* `Evaluator.java` - Position evaluation using material count and piece-square tables.
* `MoveOrdering.java` - Optimizer for Alpha-Beta pruning efficiency.
* `AILevel.java` - Enum defining search depths for Beginner to Expert.

### Network Multiplayer (`src/network/`)
* `NetworkGame.java` - Facade for managing the network game lifecycle.
* `ChessServer.java` & `ChessClient.java` - TCP socket handlers for the Host and Guest.
* `NetworkProtocol.java` - String builder/parser for securely transmitting chess moves.

### Persistence & Data (`src/persistence/`)
* `GameSaver.java` - Handles reading and writing saved games to the user's hard drive.
* `PGNHandler.java` & `FENHandler.java` - Generates Standard Algebraic Notation (SAN) for moves and decodes board states.

### Analytics (`src/analytics/`)
* `GameStats.java` & `StatsStore.java` - Calculates move accuracy and persists player statistics.
* `OpeningBook.java` - Longest-prefix matching dictionary for chess openings.

### New Game Logic (`src/game/` & `src/move/`)
* `GameMode.java` - Defines base times and increments for time controls.
* `MoveHistory.java` - A robust stack wrapping the core board for Undo/Redo tracking.

### New UI Screens (`src/gui/scenes/panes/`)
* `StatsPane.java` - Displays the Win/Loss records and accuracy charts.
* `NetworkPane.java` - The LAN lobby allowing users to Host or enter an IP to Join.

---

## 3. Files Modified (Refactored)

* **`src/game/Game.java`**: Completely rewritten to act as the true Model in the MVC. Replaced old dummy handlers with actual Fischer time increments, move history hooks, and game-over detection algorithms.
* **`src/gui/scenes/panes/GamePane.java`**: Added the rendering logic for shadows, slide animations, and clock updates. Wired the new AI engine callbacks so the JavaFX UI doesn't freeze while the computer thinks. Added Export/Undo buttons to the Side Panel.
* **`src/gui/scenes/panes/ModePane.java`**: Expanded the original 3 time controls to a 9-button grid plus an AI difficulty selector. **Preserved all original CSS styles and hover animations.**
* **`src/gui/scenes/BaseScene.java`**: Rewired the navigation graph to properly transition to the new Stats and LAN screens. Replaced the dummy `loadButton` action with real PGN save-loading.
* **`src/gui/scenes/panes/SidePane.java`**: Activated the Load button, added a Stats button, and removed invalid `java.awt` package imports.
* **`src/Main.java`**: Added the `public` modifier to the class declaration to ensure the VS Code Java Launcher detects the entry point properly.

---

## 4. Files Removed (Codebase Cleanup)

* `src/gui/test.java` - Removed (Unused/Dead code).
* Deleted all floating `.class` binaries that were polluting the `src/` directory. All compiled binaries are now strictly contained in the `bin/` directory.

---

## 5. IDE Setup & Environment

* Created `.vscode/settings.json` and `.vscode/launch.json` to natively support JavaFX compilation and execution via the VS Code "Run" button.
* Created Eclipse native `.classpath` and `.project` files to permanently fix VS Code Java compiler caching glitches and correctly map the JavaFX 27 SDK.
* Updated `src/To Run in CMD.txt` with native Windows Command Prompt instructions, removing the need for external `.bat` files.
