package persistence;

import game.Game;
import game.GameMode;
import game.GameStatus;
import move.Move;
import pieces.PieceColor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves and loads games as PGN files in the user's home directory.
 *
 * Save location: {@code ~/ChessGames/} (created automatically).
 * File name format: {@code chess_YYYY-MM-DD_HH-mm-ss.pgn}
 *
 * A save file is a valid PGN with an extra custom tag {@code [FEN "..."]},
 * which stores the full position after the last move for quick resumption.
 * Any PGN-capable reader can open these files; the extra tag is simply ignored.
 */
public final class GameSaver {

    /** The directory where games are saved. Created on first save. */
    public static final Path SAVE_DIR = Paths.get(
            System.getProperty("user.home"), "ChessGames");

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private GameSaver() { }

    // --- --- --- --- --- SAVE --- --- --- --- --- //

    /**
     * Serialises the current game as a PGN file and writes it to disk.
     *
     * @param game   the game to save (may still be in progress)
     * @return the path of the file that was written
     * @throws IOException if the file cannot be written
     */
    public static Path saveGame(Game game) throws IOException {
        Files.createDirectories(SAVE_DIR);
        String stamp = LocalDateTime.now().format(STAMP);
        Path file = SAVE_DIR.resolve("chess_" + stamp + ".pgn");
        String pgn = buildPGN(game);
        Files.write(file, pgn.getBytes(StandardCharsets.UTF_8));
        System.out.println("SYS:    Game saved → " + file);
        return file;
    }

    /**
     * Returns a list of all previously saved PGN files, newest first.
     */
    public static List<Path> listSaves() {
        List<Path> saves = new ArrayList<>();
        if (!Files.exists(SAVE_DIR)) return saves;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(SAVE_DIR, "*.pgn")) {
            for (Path p : stream) saves.add(p);
        } catch (IOException ignored) { }
        saves.sort((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()));
        return saves;
    }

    // --- --- --- --- --- LOAD --- --- --- --- --- //

    /**
     * Loads a previously saved PGN file and returns the moves contained in it.
     * The caller should create a fresh {@link Game} and replay the moves.
     *
     * @param file the PGN file to load
     * @return ordered list of moves
     * @throws IOException              if the file cannot be read
     * @throws IllegalArgumentException if the PGN cannot be parsed
     */
    public static List<Move> loadGame(Path file) throws IOException {
        String pgn = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        return PGNHandler.importPGN(pgn);
    }

    // --- --- --- --- --- HELPERS --- --- --- --- --- //

    private static String buildPGN(Game game) {
        String result;
        if (game.isGameOver()) {
            if (game.winner() == PieceColor.WHITE) result = "1-0";
            else if (game.winner() == PieceColor.BLACK) result = "0-1";
            else result = "1/2-1/2";
        } else {
            result = "*";
        }

        String pgn = PGNHandler.export(
                game.getMoveHistory().getMoves(),
                game.getBoard(),
                result,
                game.getWhiteName(),
                game.getBlackName());

        // Append FEN of the current position as a custom tag at the very top.
        String fen = FENHandler.toFEN(game.getBoard());
        String fenTag = "[FEN \"" + fen + "\"]\n";

        // Insert the FEN tag right before the blank line that precedes the moves.
        int blankLine = pgn.indexOf("\n\n");
        if (blankLine >= 0) {
            return pgn.substring(0, blankLine) + "\n" + fenTag + pgn.substring(blankLine);
        }
        return fenTag + pgn;
    }
}
