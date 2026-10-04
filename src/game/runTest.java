package game;

import board.Board;
import move.CastleSide;
import move.Move;
import pieces.Piece;
import pieces.PieceColor;
import pieces.PieceType;

import java.util.ArrayList;
import java.util.List;

import static pieces.PieceColor.BLACK;
import static pieces.PieceColor.WHITE;

/**
 * Headless test suite for the chess engine (no JavaFX needed).
 *
 * Run with:  java game.runTest
 * Exits with code 1 if anything fails.
 *
 * Covers: starting position, basic/invalid movement, turns, check, checkmate,
 * castling, en passant, promotion, stalemate, draw rules, move history (undo)
 * and the chess clocks (5 min / 10 min / untimed, switching, timeout, stopping).
 */
public class runTest {

    private int passed = 0;
    private int failed = 0;

    public void test(String[] args) {
        System.out.println("=== CHESS ENGINE TESTS ===");

        testStartingPosition();
        testBasicMovement();
        testInvalidMovement();
        testTurnSystem();
        testCheck();
        testCheckmate();
        testCastling();
        testEnPassant();
        testPromotion();
        testStalemate();
        testDrawRules();
        testMoveHistoryUndo();
        testClocks();

        System.out.println();
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        new runTest().test(args);
    }

    // --- --- --- --- --- TEST SECTIONS --- --- --- --- --- //

    private void testStartingPosition() {
        section("Starting position");
        Board b = freshBoard();

        check("white king on e1", typeAt(b, 4, 0) == PieceType.KING && colorAt(b, 4, 0) == WHITE);
        check("white queen on d1", typeAt(b, 3, 0) == PieceType.QUEEN && colorAt(b, 3, 0) == WHITE);
        check("black king on e8", typeAt(b, 4, 7) == PieceType.KING && colorAt(b, 4, 7) == BLACK);
        check("black queen on d8", typeAt(b, 3, 7) == PieceType.QUEEN && colorAt(b, 3, 7) == BLACK);
        check("rooks on the corners",
                typeAt(b, 0, 0) == PieceType.ROOK && typeAt(b, 7, 0) == PieceType.ROOK
                && typeAt(b, 0, 7) == PieceType.ROOK && typeAt(b, 7, 7) == PieceType.ROOK);
        check("knights on b1/g1/b8/g8",
                typeAt(b, 1, 0) == PieceType.KNIGHT && typeAt(b, 6, 0) == PieceType.KNIGHT
                && typeAt(b, 1, 7) == PieceType.KNIGHT && typeAt(b, 6, 7) == PieceType.KNIGHT);
        check("bishops on c1/f1/c8/f8",
                typeAt(b, 2, 0) == PieceType.BISHOP && typeAt(b, 5, 0) == PieceType.BISHOP
                && typeAt(b, 2, 7) == PieceType.BISHOP && typeAt(b, 5, 7) == PieceType.BISHOP);

        boolean pawnsOk = true;
        for (int x = 0; x < 8; x++) {
            if (typeAt(b, x, 1) != PieceType.PAWN || colorAt(b, x, 1) != WHITE) pawnsOk = false;
            if (typeAt(b, x, 6) != PieceType.PAWN || colorAt(b, x, 6) != BLACK) pawnsOk = false;
        }
        check("all 16 pawns on rank 2 / rank 7", pawnsOk);
        check("32 pieces on the board", allPieces(b).size() == 32);
        check("white to move first", b.state.turn == WHITE);
        check("all castling rights present",
                b.state.castleWhiteKingside && b.state.castleWhiteQueenside
                && b.state.castleBlackKingside && b.state.castleBlackQueenside);
    }

    private void testBasicMovement() {
        section("Basic movement");

        // --- Pawn ---
        Board b = emptyBoard();
        place(b, WHITE, PieceType.PAWN, 1, 4, 1);               // white pawn e2
        check("pawn moves one square", canGo(b, WHITE, 4, 1, 4, 2));
        check("pawn moves two squares from start", canGo(b, WHITE, 4, 1, 4, 3));
        check("pawn cannot move three squares", !canGo(b, WHITE, 4, 1, 4, 4));

        b = emptyBoard();
        place(b, WHITE, PieceType.PAWN, 1, 4, 1);
        place(b, BLACK, PieceType.KNIGHT, 1, 5, 2);             // enemies beside e3
        place(b, BLACK, PieceType.KNIGHT, 2, 3, 2);
        check("pawn captures diagonally", canGo(b, WHITE, 4, 1, 5, 2) && canGo(b, WHITE, 4, 1, 3, 2));
        check("pawn still advances straight with enemies beside it", canGo(b, WHITE, 4, 1, 4, 2)
                && canGo(b, WHITE, 4, 1, 4, 3));

        b = emptyBoard();
        place(b, BLACK, PieceType.PAWN, 1, 4, 6);               // black pawn e7
        check("black pawn moves one square", canGo(b, BLACK, 4, 6, 4, 5));
        check("black pawn moves two squares from start", canGo(b, BLACK, 4, 6, 4, 4));

        // --- Knight ---
        b = emptyBoard();
        place(b, WHITE, PieceType.KNIGHT, 1, 1, 0);             // knight b1
        place(b, WHITE, PieceType.PAWN, 1, 0, 0);               // surround it with own pieces
        place(b, WHITE, PieceType.PAWN, 2, 2, 0);
        place(b, WHITE, PieceType.PAWN, 3, 0, 1);
        place(b, WHITE, PieceType.PAWN, 4, 1, 1);
        place(b, WHITE, PieceType.PAWN, 5, 2, 1);
        check("knight reaches a3/c3/d2", canGo(b, WHITE, 1, 0, 0, 2)
                && canGo(b, WHITE, 1, 0, 2, 2) && canGo(b, WHITE, 1, 0, 3, 1));
        check("knight ignores blocking pieces", moveCountFrom(b, WHITE, 1, 0) == 3);

        // --- Bishop ---
        b = emptyBoard();
        place(b, WHITE, PieceType.BISHOP, 1, 2, 0);             // bishop c1
        place(b, WHITE, PieceType.PAWN, 1, 3, 1);               // own pawn d2 blocks
        check("bishop moves on open diagonals", canGo(b, WHITE, 2, 0, 1, 1) && canGo(b, WHITE, 2, 0, 0, 2));
        check("bishop cannot jump own pawn", !canGo(b, WHITE, 2, 0, 3, 1) && !canGo(b, WHITE, 2, 0, 4, 2));

        b = emptyBoard();
        place(b, WHITE, PieceType.BISHOP, 1, 2, 0);
        place(b, BLACK, PieceType.KNIGHT, 1, 3, 1);             // enemy blocker d2
        check("bishop can capture blocker", canGo(b, WHITE, 2, 0, 3, 1));
        check("bishop cannot pass through capture", !canGo(b, WHITE, 2, 0, 4, 2));

        // --- Rook ---
        b = emptyBoard();
        place(b, WHITE, PieceType.ROOK, 1, 0, 0);               // rook a1
        place(b, WHITE, PieceType.PAWN, 1, 0, 2);               // own pawn a3 blocks the file
        check("rook moves along rank and file", canGo(b, WHITE, 0, 0, 1, 0) && canGo(b, WHITE, 0, 0, 4, 0));
        check("rook cannot pass own pawn", !canGo(b, WHITE, 0, 0, 0, 2) && !canGo(b, WHITE, 0, 0, 0, 5));

        b = emptyBoard();
        place(b, WHITE, PieceType.ROOK, 1, 0, 0);
        place(b, BLACK, PieceType.KNIGHT, 1, 0, 3);             // enemy on a4
        check("rook captures enemy on its line", canGo(b, WHITE, 0, 0, 0, 3));
        check("rook stops after a capture", !canGo(b, WHITE, 0, 0, 0, 4));

        // --- Queen ---
        b = emptyBoard();
        place(b, WHITE, PieceType.QUEEN, 1, 3, 0);              // queen d1
        check("queen moves like rook", canGo(b, WHITE, 3, 0, 3, 4) && canGo(b, WHITE, 3, 0, 7, 0));
        check("queen moves like bishop", canGo(b, WHITE, 3, 0, 4, 1) && canGo(b, WHITE, 3, 0, 0, 3));
        check("queen cannot move like knight", !canGo(b, WHITE, 3, 0, 5, 1));

        // --- King ---
        b = emptyBoard();
        place(b, WHITE, PieceType.KING, 1, 4, 4);               // king e5
        check("king moves one square in any direction",
                canGo(b, WHITE, 4, 4, 3, 3) && canGo(b, WHITE, 4, 4, 4, 3)
                && canGo(b, WHITE, 4, 4, 5, 3) && canGo(b, WHITE, 4, 4, 3, 4)
                && canGo(b, WHITE, 4, 4, 5, 4) && canGo(b, WHITE, 4, 4, 3, 5)
                && canGo(b, WHITE, 4, 4, 4, 5) && canGo(b, WHITE, 4, 4, 5, 5));
        check("king cannot move two squares", !canGo(b, WHITE, 4, 4, 4, 6));
        check("king finds all 8 neighbours", moveCountFrom(b, WHITE, 4, 4) == 8);
    }

    private void testInvalidMovement() {
        section("Invalid movement");

        // Pawn cannot move diagonally onto an empty square.
        Board b = emptyBoard();
        place(b, WHITE, PieceType.PAWN, 1, 4, 1);
        check("pawn cannot capture empty diagonal", !canGo(b, WHITE, 4, 1, 5, 2) && !canGo(b, WHITE, 4, 1, 3, 2));

        // Pawn cannot move forward into an occupied square.
        b = emptyBoard();
        place(b, WHITE, PieceType.PAWN, 1, 4, 1);
        place(b, BLACK, PieceType.PAWN, 1, 4, 2);
        check("pawn blocked by enemy in front", !canGo(b, WHITE, 4, 1, 4, 2) && !canGo(b, WHITE, 4, 1, 4, 3));

        b = emptyBoard();
        place(b, WHITE, PieceType.PAWN, 1, 4, 1);
        place(b, WHITE, PieceType.KNIGHT, 1, 4, 2);
        check("pawn blocked by own piece in front", !canGo(b, WHITE, 4, 1, 4, 2) && !canGo(b, WHITE, 4, 1, 4, 3));

        // Pieces cannot move through other pieces.
        b = emptyBoard();
        place(b, WHITE, PieceType.ROOK, 1, 0, 0);
        place(b, WHITE, PieceType.PAWN, 1, 0, 2);
        place(b, WHITE, PieceType.BISHOP, 1, 0, 4);
        check("rook cannot move through a piece", !canGo(b, WHITE, 0, 0, 0, 5) && !canGo(b, WHITE, 0, 0, 0, 7));

        // Opponent pieces are never selectable.
        b = emptyBoard();
        place(b, WHITE, PieceType.ROOK, 1, 0, 0);
        place(b, BLACK, PieceType.PAWN, 1, 4, 6);
        check("no moves generated for an opponent piece", moveCountFrom(b, WHITE, 4, 6) == 0);
        check("opponent piece cannot be selected", !canGo(b, WHITE, 4, 6, 4, 5));
    }

    private void testTurnSystem() {
        section("Turn system");
        Game g = new Game("", Game.UNTIMED);

        check("white moves first", g.turn() == WHITE);
        check("black piece has no moves on white's turn", g.legalMovesFrom(4, 6).isEmpty());

        check("white opening move accepted", g.tryMove(4, 1, 4, 3, null));   // e2-e4
        check("turn passes to black", g.turn() == BLACK);
        check("white cannot move a white piece on black's turn", !g.tryMove(5, 1, 5, 3, null));
        check("illegal move does not change the turn", g.turn() == BLACK);
        check("black reply accepted", g.tryMove(4, 6, 4, 4, null));          // e7-e5
        check("turn passes back to white", g.turn() == WHITE);
        check("pawn cannot jump from the 4th rank", !g.tryMove(4, 3, 4, 5, null));
        check("turn unchanged after rejected move", g.turn() == WHITE);
        check("pawn cannot move 3 squares forward", !g.tryMove(4, 3, 4, 6, null));
        check("move history has exactly 2 moves", g.moveStack.size() == 2);
    }

    private void testCheck() {
        section("Check");

        // A rook checks down the open e-file; the king must flee or block.
        Board b = emptyBoard();
        place(b, WHITE, PieceType.KING, 1, 4, 0);    // Ke1
        place(b, WHITE, PieceType.KNIGHT, 1, 6, 0);  // Ng1
        place(b, BLACK, PieceType.ROOK, 1, 4, 7);    // Re8
        place(b, BLACK, PieceType.KING, 1, 0, 7);    // Ka8
        b.state.turn = WHITE;

        check("white king is in check", Rules.isInCheck(b, WHITE));
        check("black king is not in check", !Rules.isInCheck(b, BLACK));
        check("king can escape sideways", canGo(b, WHITE, 4, 0, 3, 1));       // Kd2 leaves the e-file
        check("king cannot stay in check", !canGo(b, WHITE, 4, 0, 4, 1));     // Ke2 still attacked
        check("knight can block the check", canGo(b, WHITE, 6, 0, 4, 1));     // Ne2 interposes
        check("knight cannot ignore the check", !canGo(b, WHITE, 6, 0, 5, 2)
                && !canGo(b, WHITE, 6, 0, 7, 2));

        // King cannot move next to the enemy king.
        b = emptyBoard();
        place(b, WHITE, PieceType.KING, 1, 0, 0);
        place(b, BLACK, PieceType.KING, 1, 2, 2);
        b.state.turn = WHITE;
        check("king cannot walk next to enemy king", !canGo(b, WHITE, 0, 0, 1, 1));
        check("king can move away from enemy king", canGo(b, WHITE, 0, 0, 0, 1));

        // King cannot move into a square attacked by a rook.
        b = emptyBoard();
        place(b, WHITE, PieceType.KING, 1, 0, 0);
        place(b, BLACK, PieceType.ROOK, 1, 1, 7);   // Rb8 controls the b-file
        place(b, BLACK, PieceType.KING, 1, 7, 7);
        b.state.turn = WHITE;
        check("king cannot enter an attacked file", !canGo(b, WHITE, 0, 0, 1, 0) && !canGo(b, WHITE, 0, 0, 1, 1));
        check("king can move along a safe square", canGo(b, WHITE, 0, 0, 0, 1));

        // --- Game-level check flags ---
        Game g = new Game("", Game.UNTIMED);
        check("1. e4", g.tryMove(4, 1, 4, 3, null));
        check("1... d5", g.tryMove(3, 6, 3, 4, null));
        check("2. Bb5+", g.tryMove(5, 0, 1, 4, null));
        check("black is in check", g.isInCheck(BLACK));
        check("state records checkBlack only", g.getState().checkBlack && !g.getState().checkWhite);
        check("game continues while in check", g.status() == GameStatus.PLAYING);
        check("2... c6 blocks the check", g.tryMove(2, 6, 2, 5, null));
        check("check cleared by blocking", !g.isInCheck(BLACK) && !g.getState().checkBlack);
    }

    private void testCheckmate() {
        section("Checkmate (fool's mate)");

        Game g = new Game("", Game.UNTIMED);
        check("1. f3", g.tryMove(5, 1, 5, 2, null));
        check("1... e5", g.tryMove(4, 6, 4, 4, null));
        check("2. g4", g.tryMove(6, 1, 6, 3, null));
        check("2... Qh4#", g.tryMove(3, 7, 7, 3, null));

        check("game status is CHECKMATE", g.status() == GameStatus.CHECKMATE);
        check("black wins", g.winner() == BLACK);
        check("white king flagged in check", g.getState().checkWhite);
        check("white flagged checkmated", g.getState().checkmateWhite);
        check("game is over", g.isGameOver());
        check("no moves after mate", g.legalMovesFrom(4, 0).isEmpty());
        check("tryMove rejected after game over", !g.tryMove(4, 0, 4, 1, null));
        check("move history has 4 moves", g.moveStack.size() == 4);
    }

    private void testCastling() {
        section("Castling");

        // --- Both sides available ---
        Board b = castlingBoard();
        check("white can castle kingside", canGo(b, WHITE, 4, 0, 6, 0));
        check("white can castle queenside", canGo(b, WHITE, 4, 0, 2, 0));
        check("black can castle kingside", canGo(b, BLACK, 4, 7, 6, 7));
        check("black can castle queenside", canGo(b, BLACK, 4, 7, 2, 7));

        // --- Blocked path ---
        b = castlingBoard();
        place(b, WHITE, PieceType.KNIGHT, 1, 6, 0);             // own piece on g1
        check("kingside blocked by own piece", !canGo(b, WHITE, 4, 0, 6, 0));
        check("queenside still clear", canGo(b, WHITE, 4, 0, 2, 0));

        b = castlingBoard();
        place(b, WHITE, PieceType.KNIGHT, 1, 1, 0);             // own piece on b1
        check("queenside blocked by own piece", !canGo(b, WHITE, 4, 0, 2, 0));
        check("kingside still clear", canGo(b, WHITE, 4, 0, 6, 0));

        // --- Transit square attacked (Rf8 attacks f1) ---
        b = castlingBoard();
        place(b, BLACK, PieceType.ROOK, 3, 5, 7);               // Rf8
        check("cannot castle through attacked square", !canGo(b, WHITE, 4, 0, 6, 0));
        check("queenside unaffected by the f-file", canGo(b, WHITE, 4, 0, 2, 0));

        // --- Destination square attacked (Rg8 attacks g1) ---
        b = castlingBoard();
        place(b, BLACK, PieceType.ROOK, 3, 6, 7);               // Rg8
        check("cannot castle onto attacked square", !canGo(b, WHITE, 4, 0, 6, 0));
        check("queenside unaffected by the g-file", canGo(b, WHITE, 4, 0, 2, 0));

        // --- Castling while in check (Re4 checks e1) ---
        b = castlingBoard();
        place(b, BLACK, PieceType.ROOK, 3, 4, 4);               // Re4
        check("cannot castle while in check", !canGo(b, WHITE, 4, 0, 6, 0)
                && !canGo(b, WHITE, 4, 0, 2, 0));

        // --- Rights tracking: king moves out and back ---
        Game g = new Game("", Game.UNTIMED);
        check("start with full white rights",
                g.getState().castleWhiteKingside && g.getState().castleWhiteQueenside);
        g.tryMove(4, 1, 4, 3, null);   // e2-e4
        g.tryMove(0, 6, 0, 5, null);   // a7-a6
        g.tryMove(4, 0, 4, 1, null);   // Ke1-e2
        g.tryMove(0, 5, 0, 4, null);   // a6-a5
        g.tryMove(4, 1, 4, 0, null);   // Ke2-e1
        check("king move clears both white rights",
                !g.getState().castleWhiteKingside && !g.getState().castleWhiteQueenside);
        check("black rights untouched", g.getState().castleBlackKingside && g.getState().castleBlackQueenside);
        check("no castling after king moved",
                noCastleMove(g.getBoard(), WHITE, 6, 0) && noCastleMove(g.getBoard(), WHITE, 2, 0));

        // --- Rights tracking: rook moves out and back ---
        g = new Game("", Game.UNTIMED);
        g.tryMove(7, 1, 7, 3, null);   // h2-h4
        g.tryMove(0, 6, 0, 5, null);   // a7-a6
        g.tryMove(7, 0, 7, 1, null);   // Rh1-h2
        g.tryMove(0, 5, 0, 4, null);   // a6-a5
        g.tryMove(7, 1, 7, 0, null);   // Rh2-h1
        check("rook move clears kingside right", !g.getState().castleWhiteKingside);
        check("queenside right survives", g.getState().castleWhiteQueenside);
        check("no kingside castling after rook moved", noCastleMove(g.getBoard(), WHITE, 6, 0));

        // --- Rook captured clears the right (and undo restores it) ---
        b = castlingBoard();
        Piece whiteRookH1 = b.pieceAt(7, 0);
        Piece blackRookH8 = b.pieceAt(7, 7);
        check("white kingside right set", b.state.castleWhiteKingside);
        Move capture = new Move(7, 7, 7, 0, blackRookH8, whiteRookH1, null, CastleSide.NONE, false, 1);
        b.applyMove(capture);
        check("capturing the rook clears white kingside right", !b.state.castleWhiteKingside);
        b.undoMove(capture);
        check("undo restores the right", b.state.castleWhiteKingside);
        check("undo restores the rook", b.pieceAt(7, 0) == whiteRookH1);
    }

    private void testEnPassant() {
        section("En passant");

        // --- White captures ---
        Game g = new Game("", Game.UNTIMED);
        check("1. e4", g.tryMove(4, 1, 4, 3, null));
        check("ep target set after double push", g.getState().enPassantX == 4 && g.getState().enPassantY == 2);
        check("1... a6", g.tryMove(0, 6, 0, 5, null));
        check("ep target cleared by other move", g.getState().enPassantX == -1);
        check("2. e5", g.tryMove(4, 3, 4, 4, null));
        check("2... d5", g.tryMove(3, 6, 3, 4, null));
        check("ep target on d6", g.getState().enPassantX == 3 && g.getState().enPassantY == 5);
        check("white captures en passant", g.tryMove(4, 4, 3, 5, null));
        Board wb = g.getBoard();
        check("captured pawn removed", wb.pieceAt(3, 4) == null);
        check("capturing pawn landed on d6", typeAt(wb, 3, 5) == PieceType.PAWN && colorAt(wb, 3, 5) == WHITE);
        check("ep target cleared after capture", g.getState().enPassantX == -1);

        // --- Black captures ---
        g = new Game("", Game.UNTIMED);
        check("1. h4", g.tryMove(7, 1, 7, 3, null));
        check("1... g5", g.tryMove(6, 6, 6, 4, null));
        check("2. a4", g.tryMove(0, 1, 0, 3, null));
        check("2... g4", g.tryMove(6, 4, 6, 3, null));
        check("3. a5", g.tryMove(0, 3, 0, 4, null));
        check("3... h6", g.tryMove(7, 6, 7, 5, null));
        check("4. f4", g.tryMove(5, 1, 5, 3, null));
        check("ep target on f3", g.getState().enPassantX == 5 && g.getState().enPassantY == 2);
        check("black captures en passant", g.tryMove(6, 3, 5, 2, null));
        wb = g.getBoard();
        check("white pawn removed from f4", wb.pieceAt(5, 3) == null);
        check("black pawn landed on f3", typeAt(wb, 5, 2) == PieceType.PAWN && colorAt(wb, 5, 2) == BLACK);
        check("square g4 empty", wb.pieceAt(6, 3) == null);
    }

    private void testPromotion() {
        section("Promotion");

        // --- All four promotion choices for white ---
        for (PieceType promo : PieceType.PROMOTION_TYPES) {
            Game g = new Game("", Game.UNTIMED);
            Board b = g.getBoard();
            b.clear();
            place(b, WHITE, PieceType.PAWN, 2, 1, 6);           // pawn b7
            place(b, WHITE, PieceType.KING, 1, 0, 0);
            place(b, BLACK, PieceType.KING, 1, 4, 4);
            b.state.turn = WHITE;

            check("exactly 4 promotion moves (" + promo + ")", g.legalMovesFrom(1, 6).size() == 4);
            check("promotion needs an explicit choice (" + promo + ")", !g.tryMove(1, 6, 1, 7, null));
            check("promotion plays as " + promo, g.tryMove(1, 6, 1, 7, promo));
            check("pawn became " + promo, typeAt(b, 1, 7) == promo && colorAt(b, 1, 7) == WHITE);
        }

        // --- Black promotes too ---
        Game g = new Game("", Game.UNTIMED);
        Board b = g.getBoard();
        b.clear();
        place(b, WHITE, PieceType.KING, 1, 0, 0);
        place(b, BLACK, PieceType.KING, 1, 7, 7);
        place(b, BLACK, PieceType.PAWN, 2, 6, 1);               // black pawn g2
        b.state.turn = BLACK;
        check("black promotes to queen", g.tryMove(6, 1, 6, 0, PieceType.QUEEN));
        check("black queen on g1", typeAt(b, 6, 0) == PieceType.QUEEN && colorAt(b, 6, 0) == BLACK);
    }

    private void testStalemate() {
        section("Stalemate");

        Game g = new Game("", Game.UNTIMED);
        Board b = g.getBoard();
        b.clear();
        place(b, WHITE, PieceType.KING, 1, 2, 5);    // Kc6
        place(b, WHITE, PieceType.QUEEN, 1, 2, 4);   // Qc5
        place(b, BLACK, PieceType.KING, 1, 0, 7);    // Ka8
        b.state.turn = WHITE;

        check("white plays Qb6", g.tryMove(2, 4, 1, 5, null));
        check("black king is not in check", !g.isInCheck(BLACK));
        check("status is STALEMATE", g.status() == GameStatus.STALEMATE);
        check("game is a draw (no winner)", g.winner() == null);
        check("stalemate flag set", g.getState().stalemate);
        check("game is over", g.isGameOver());
    }

    private void testDrawRules() {
        section("Draw rules");

        // --- K vs K is insufficient material ---
        Game g = new Game("", Game.UNTIMED);
        Board b = g.getBoard();
        b.clear();
        place(b, WHITE, PieceType.KING, 1, 3, 3);
        place(b, BLACK, PieceType.KING, 1, 5, 5);
        check("white king shuffle", g.tryMove(3, 3, 3, 4, null));
        check("K vs K is a draw", g.status() == GameStatus.INSUFFICIENT_MATERIAL);
        check("draw has no winner", g.winner() == null);

        // --- Same-colored bishops are insufficient ---
        g = new Game("", Game.UNTIMED);
        b = g.getBoard();
        b.clear();
        place(b, WHITE, PieceType.KING, 1, 0, 0);
        place(b, WHITE, PieceType.BISHOP, 1, 2, 3);   // c4 (light square)
        place(b, BLACK, PieceType.KING, 1, 7, 7);
        place(b, BLACK, PieceType.BISHOP, 1, 4, 7);   // e8 (light square)
        check("white king shuffle (same colors)", g.tryMove(0, 0, 1, 0, null));
        check("K+B vs K+B same color is a draw", g.status() == GameStatus.INSUFFICIENT_MATERIAL);

        // --- Opposite-colored bishops keep playing ---
        g = new Game("", Game.UNTIMED);
        b = g.getBoard();
        b.clear();
        place(b, WHITE, PieceType.KING, 1, 0, 0);
        place(b, WHITE, PieceType.BISHOP, 1, 2, 3);   // c4 (light square)
        place(b, BLACK, PieceType.KING, 1, 5, 5);
        place(b, BLACK, PieceType.BISHOP, 1, 6, 6);   // g6 (dark square)
        check("white king shuffle (opposite colors)", g.tryMove(0, 0, 1, 0, null));
        check("K+B vs K+B opposite colors keeps playing", g.status() == GameStatus.PLAYING);

        // --- Threefold repetition ---
        g = new Game("", Game.UNTIMED);
        check("1. Nf3", g.tryMove(6, 0, 5, 2, null));
        check("1... Nf6", g.tryMove(6, 7, 5, 5, null));
        check("2. Ng1", g.tryMove(5, 2, 6, 0, null));
        check("2... Ng8", g.tryMove(5, 5, 6, 7, null));
        check("still playing after 1st return", g.status() == GameStatus.PLAYING);
        g.tryMove(6, 0, 5, 2, null);
        g.tryMove(6, 7, 5, 5, null);
        g.tryMove(5, 2, 6, 0, null);
        check("still playing after 2nd occurrence", g.status() == GameStatus.PLAYING);
        g.tryMove(5, 5, 6, 7, null);
        check("threefold repetition declared", g.status() == GameStatus.THREEFOLD_REPETITION);
        check("repetition is a draw", g.winner() == null);

        // --- Fifty-move rule ---
        g = new Game("", Game.UNTIMED);
        b = g.getBoard();
        b.state.halfmoveClock = 99;
        check("knight move played", g.tryMove(6, 0, 5, 2, null));
        check("fifty-move rule declared", g.status() == GameStatus.FIFTY_MOVE_RULE);
        check("fifty-move is a draw", g.winner() == null);
    }

    private void testMoveHistoryUndo() {
        section("Move history / undo");

        // Quiet move, apply and undo.
        Board b = freshBoard();
        String before = b.positionKey();
        Piece pawn = b.pieceAt(4, 1);
        Move m = new Move(4, 1, 4, 3, pawn, null, null, CastleSide.NONE, false, 1);

        b.applyMove(m);
        check("board changed after apply", b.pieceAt(4, 3) == pawn && b.pieceAt(4, 1) == null);
        check("position key changed", !before.equals(b.positionKey()));
        check("en passant target recorded", b.state.enPassantX == 4 && b.state.enPassantY == 2);
        check("turn passed to black", b.state.turn == BLACK);
        check("move number incremented", pawn.moveNumber == 1);

        b.undoMove(m);
        check("position restored after undo", before.equals(b.positionKey()));
        check("pawn back on e2", b.pieceAt(4, 1) == pawn && b.pieceAt(4, 3) == null);
        check("turn restored", b.state.turn == WHITE);
        check("move number restored", pawn.moveNumber == 0);
        check("notation of a quiet move", m.notation().equals("Pe2-e4"));

        // Capture, apply and undo.
        Board c = emptyBoard();
        Piece whitePawn = new Piece(PieceType.PAWN, 2, WHITE, true);
        whitePawn.posX = 1;
        whitePawn.posY = 1;
        c.board[1][1] = whitePawn;
        Piece blackPawn = new Piece(PieceType.PAWN, 3, BLACK, true);
        blackPawn.posX = 0;
        blackPawn.posY = 2;
        c.board[2][0] = blackPawn;

        Move cap = new Move(0, 2, 1, 1, blackPawn, whitePawn, null, CastleSide.NONE, false, 1);
        c.applyMove(cap);
        check("capturing pawn stands on b2", c.pieceAt(1, 1) == blackPawn && blackPawn.posX == 1);
        check("captured pawn off the board", c.pieceAt(0, 2) == null && c.board[1][1] != whitePawn);

        c.undoMove(cap);
        check("undo restores the captured pawn", c.pieceAt(1, 1) == whitePawn);
        check("undo removes the capturing pawn", c.pieceAt(0, 2) == blackPawn && blackPawn.posY == 2);
        check("capture notation", cap.notation().equals("Pa3xb2"));
    }

    private void testClocks() {
        section("Clocks");

        // --- Starting times ---
        FakeGame g5 = new FakeGame(Game.FIVE_MINUTES_MS);
        check("5 min mode: white starts 5:00", g5.whiteMillis() == 5 * 60 * 1000L);
        check("5 min mode: black starts 5:00", g5.blackMillis() == 5 * 60 * 1000L);

        FakeGame g10 = new FakeGame(Game.TEN_MINUTES_MS);
        check("10 min mode: white starts 10:00", g10.whiteMillis() == 10 * 60 * 1000L);
        check("10 min mode: black starts 10:00", g10.blackMillis() == 10 * 60 * 1000L);

        // --- Only the active clock runs ---
        g5 = new FakeGame(Game.FIVE_MINUTES_MS);
        RecordingListener listener = new RecordingListener();
        g5.setListener(listener);
        g5.startClock();
        g5.advanceMillis(3000);
        g5.tickClock();
        check("white clock ticks down", g5.whiteMillis() == 5 * 60 * 1000L - 3000);
        check("black clock is paused", g5.blackMillis() == 5 * 60 * 1000L);

        // --- Clock switches immediately after a legal move ---
        check("white plays e4", g5.tryMove(4, 1, 4, 3, null));
        check("white accrued exactly up to the move", g5.whiteMillis() == 5 * 60 * 1000L - 3000);
        g5.advanceMillis(2000);
        g5.tickClock();
        check("black clock now runs", g5.blackMillis() == 5 * 60 * 1000L - 2000);
        check("white clock frozen after the move", g5.whiteMillis() == 5 * 60 * 1000L - 3000);
        check("clock still running", g5.isClockRunning());
        check("position listener fired once", listener.positionChanges == 1);

        // --- Illegal move does not touch the clocks ---
        long whiteBefore = g5.whiteMillis();
        long blackBefore = g5.blackMillis();
        check("illegal move rejected", !g5.tryMove(4, 3, 4, 5, null));
        g5.advanceMillis(5000);
        g5.tickClock();
        check("rejected move switched nothing", g5.whiteMillis() == whiteBefore);
        check("black kept its clock", blackBefore - g5.blackMillis() == 5000);

        // --- Timeout ---
        RecordingListener timeoutListener = new RecordingListener();
        g5.setListener(timeoutListener);
        g5.advanceMillis(g5.blackMillis());       // let black run all the way down
        g5.tickClock();
        check("black clock stops at 0:00", g5.blackMillis() == 0);
        check("timeout status", g5.status() == GameStatus.TIMEOUT);
        check("white wins on time", g5.winner() == WHITE);
        check("game over after timeout", g5.isGameOver());
        check("game-over listener fired once", timeoutListener.gameOvers == 1);
        check("clock stopped after game over", !g5.isClockRunning());
        g5.advanceMillis(60_000);
        g5.tickClock();
        check("no negative time, no second ending", g5.blackMillis() == 0
                && timeoutListener.gameOvers == 1 && g5.status() == GameStatus.TIMEOUT);

        // --- Leaving the game pauses the clock ---
        g5 = new FakeGame(Game.FIVE_MINUTES_MS);
        g5.startClock();
        g5.advanceMillis(1000);
        g5.pauseClock();                          // what GamePane.dispose() calls
        long paused = g5.whiteMillis();
        check("time accrued up to pause", paused == 5 * 60 * 1000L - 1000);
        g5.advanceMillis(10_000);
        g5.tickClock();
        check("clock does not run after leaving", g5.whiteMillis() == paused && !g5.isClockRunning());

        // --- Untimed DEFAULT mode counts up ---
        FakeGame gu = new FakeGame(Game.UNTIMED);
        check("untimed mode starts at 0:00", gu.whiteMillis() == 0 && gu.isUntimed());
        gu.startClock();
        gu.advanceMillis(5000);
        gu.tickClock();
        check("untimed white counts up", gu.whiteMillis() == 5000);
        gu.tryMove(4, 1, 4, 3, null);
        gu.advanceMillis(3000);
        gu.tickClock();
        check("untimed black counts up after switch", gu.blackMillis() == 3000);
        check("untimed white keeps its total", gu.whiteMillis() == 5000);
        check("untimed never times out", gu.status() == GameStatus.PLAYING);
    }

    // --- --- --- --- --- HELPERS --- --- --- --- --- //

    private Board freshBoard() {
        return new Board("");
    }

    private Board emptyBoard() {
        Board b = new Board("");
        b.clear();
        return b;
    }

    /** A board with both kings and rooks home, for castling tests. */
    private Board castlingBoard() {
        Board b = emptyBoard();
        place(b, WHITE, PieceType.KING, 1, 4, 0);
        place(b, WHITE, PieceType.ROOK, 1, 0, 0);
        place(b, WHITE, PieceType.ROOK, 2, 7, 0);
        place(b, BLACK, PieceType.KING, 1, 4, 7);
        place(b, BLACK, PieceType.ROOK, 1, 0, 7);
        place(b, BLACK, PieceType.ROOK, 2, 7, 7);
        b.state.turn = WHITE;
        return b;
    }

    private void place(Board b, PieceColor color, PieceType type, int index, int x, int y) {
        Piece p = new Piece(type, index, color, true);
        p.posX = x;
        p.posY = y;
        b.board[y][x] = p;
    }

    private boolean canGo(Board b, PieceColor color, int fx, int fy, int tx, int ty) {
        for (Move m : Rules.legalMovesFrom(b, fx, fy, color)) {
            if (m.targets(tx, ty)) return true;
        }
        return false;
    }

    private int moveCountFrom(Board b, PieceColor color, int x, int y) {
        return Rules.legalMovesFrom(b, x, y, color).size();
    }

    private boolean noCastleMove(Board b, PieceColor color, int tx, int ty) {
        int rank = (color == WHITE) ? 0 : 7;
        for (Move m : Rules.legalMovesFrom(b, 4, rank, color)) {
            if (m.castle != CastleSide.NONE && m.targets(tx, ty)) return false;
        }
        return true;
    }

    private PieceType typeAt(Board b, int x, int y) {
        Piece p = b.pieceAt(x, y);
        return p == null ? null : p.type;
    }

    private PieceColor colorAt(Board b, int x, int y) {
        Piece p = b.pieceAt(x, y);
        return p == null ? null : p.color;
    }

    private List<Piece> allPieces(Board b) {
        List<Piece> list = new ArrayList<>();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (b.board[y][x] != null) list.add(b.board[y][x]);
            }
        }
        return list;
    }

    private void section(String name) {
        System.out.println();
        System.out.println("--- " + name + " ---");
    }

    private void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    /** A Game whose clock can be driven manually instead of waiting for real time. */
    private static class FakeGame extends Game {
        private long nanos = 0;

        FakeGame(long timeControlMs) {
            super("", timeControlMs);
        }

        void advanceMillis(long ms) {
            nanos += ms * 1_000_000L;
        }

        @Override
        protected long now() {
            return nanos;
        }
    }

    private static class RecordingListener implements Game.Listener {
        int positionChanges = 0;
        int gameOvers = 0;

        @Override
        public void onPositionChanged() {
            positionChanges++;
        }

        @Override
        public void onGameOver(GameStatus status, PieceColor winner) {
            gameOvers++;
        }
    }
}
