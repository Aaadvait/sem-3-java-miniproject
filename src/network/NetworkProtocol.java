package network;

/**
 * Text-based protocol for LAN chess communication.
 *
 * Every message is a single UTF-8 line terminated with '\n'.
 * The format is: COMMAND [space] PAYLOAD
 *
 * Command reference:
 *
 *   C → S   HELLO <name>              Client announces its player name
 *   S → C   WELCOME <name>            Server confirms, sends server player name
 *   S → C   COLOR <W|B>               Assigns the client a color
 *   S → C   START                     Both players connected; game starts
 *
 *   C → S   MOVE <from> <to> [promo]  e.g. "MOVE e2 e4" or "MOVE e7 e8 Q"
 *   S → C   MOVE <from> <to> [promo]  Server relays the move to the other player
 *
 *   C → S   RESIGN                    Sender resigns
 *   S → C   RESIGN                    Relayed to the other player
 *
 *   C → S   DRAW_OFFER                Sender offers a draw
 *   S → C   DRAW_OFFER                Relayed
 *   C → S   DRAW_ACCEPT               Other player accepts the draw
 *   S → C   DRAW_ACCEPT               Relayed; game ends
 *   C → S   DRAW_DECLINE              Other player declines
 *   S → C   DRAW_DECLINE              Relayed
 *
 *   S → C   GAMEOVER <result>         "1-0", "0-1", "1/2-1/2"
 *
 *   C ↔ S   PING                      Keepalive
 *   C ↔ S   PONG                      Keepalive reply
 *
 *   C ↔ S   ERROR <message>           Something went wrong
 */
public final class NetworkProtocol {

    private NetworkProtocol() { }

    // --- Command tokens ---
    public static final String HELLO        = "HELLO";
    public static final String WELCOME      = "WELCOME";
    public static final String COLOR        = "COLOR";
    public static final String START        = "START";
    public static final String MOVE         = "MOVE";
    public static final String RESIGN       = "RESIGN";
    public static final String DRAW_OFFER   = "DRAW_OFFER";
    public static final String DRAW_ACCEPT  = "DRAW_ACCEPT";
    public static final String DRAW_DECLINE = "DRAW_DECLINE";
    public static final String GAMEOVER     = "GAMEOVER";
    public static final String PING         = "PING";
    public static final String PONG         = "PONG";
    public static final String ERROR        = "ERROR";
    public static final String SPECTATE     = "SPECTATE";

    // --- Default port ---
    public static final int DEFAULT_PORT = 55_765;

    // --- --- --- --- --- BUILDERS --- --- --- --- --- //

    public static String hello(String name)    { return HELLO   + " " + name; }
    public static String welcome(String name)  { return WELCOME + " " + name; }
    public static String color(char c)         { return COLOR   + " " + c; }
    public static String start()               { return START; }
    public static String ping()                { return PING; }
    public static String pong()                { return PONG; }
    public static String resign()              { return RESIGN; }
    public static String drawOffer()           { return DRAW_OFFER; }
    public static String drawAccept()          { return DRAW_ACCEPT; }
    public static String drawDecline()         { return DRAW_DECLINE; }
    public static String gameOver(String res)  { return GAMEOVER + " " + res; }
    public static String error(String msg)     { return ERROR + " " + msg; }

    /**
     * Builds a MOVE command string.
     *
     * @param fromX  source file 0..7
     * @param fromY  source rank 0..7
     * @param toX    dest file
     * @param toY    dest rank
     * @param promo  promotion piece letter (Q/R/B/N) or '\0' for none
     */
    public static String move(int fromX, int fromY, int toX, int toY, char promo) {
        String s = MOVE + " " + sq(fromX, fromY) + " " + sq(toX, toY);
        if (promo != 0 && promo != ' ') s += " " + promo;
        return s;
    }

    // --- --- --- --- --- PARSERS --- --- --- --- --- //

    public static String command(String line) {
        if (line == null) return "";
        int sp = line.indexOf(' ');
        return sp < 0 ? line.trim() : line.substring(0, sp).trim();
    }

    public static String payload(String line) {
        if (line == null) return "";
        int sp = line.indexOf(' ');
        return sp < 0 ? "" : line.substring(sp + 1).trim();
    }

    /**
     * Parses a MOVE payload ("e2 e4" or "e7 e8 Q") into [fromX, fromY, toX, toY, promoChar].
     * promoChar is 0 if there is no promotion.
     */
    public static int[] parseMove(String payload) {
        String[] parts = payload.trim().split("\\s+");
        if (parts.length < 2) return null;
        int[] result = new int[5];
        result[0] = parts[0].charAt(0) - 'a';
        result[1] = parts[0].charAt(1) - '1';
        result[2] = parts[1].charAt(0) - 'a';
        result[3] = parts[1].charAt(1) - '1';
        result[4] = (parts.length >= 3 && parts[2].length() > 0) ? parts[2].charAt(0) : 0;
        return result;
    }

    // --- --- --- --- --- HELPERS --- --- --- --- --- //

    private static String sq(int x, int y) {
        return "" + (char)('a' + x) + (y + 1);
    }
}
