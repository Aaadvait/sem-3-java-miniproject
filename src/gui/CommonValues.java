package gui;

import javafx.scene.text.Font;

public class CommonValues {

    // ---x--- CHANGE THESE IF YOU WANT, TOUFH PREFREBLY DONT ---x---

    // Window Size
    public final int HEIGHT = 900;
    public final int WIDTH = 1600;

    // FONTS

    public final
    Font VARELA_BUTTON = Font.loadFont(
            getClass().getResourceAsStream("/gui/resources/fonts/VarelaRound-Regular.ttf"),
            24
    );

    public final
    Font NEW_ROCKER_BUTTON = Font.loadFont(
            getClass().getResourceAsStream("/gui/resources/fonts/NewRocker-Regular.ttf"),
            24
    );

    public final
    Font NEW_ROCKER_BIG = Font.loadFont(
            getClass().getResourceAsStream("/gui/resources/fonts/NewRocker-Regular.ttf"),
            72
    );

    public final
    Font NEW_ROCKER_MEDIUM = Font.loadFont(
            getClass().getResourceAsStream("/gui/resources/fonts/NewRocker-Regular.ttf"),
            48
    );

    public final
    Font VARELA_CLOCK = Font.loadFont(
            getClass().getResourceAsStream("/gui/resources/fonts/VarelaRound-Regular.ttf"),
            42
    );

    // Button Styles - Side Pane

    public final String STYLE_BSP =
                    "-fx-background-color: #0096ff;" +
                    "-fx-border-color: #cceeee;" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 5;" +
                    "-fx-background-radius: 5;";

    public final String STYLE_BMP =
                    "-fx-background-color: #0096ff;" +
                    "-fx-border-color: #cceeee;" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 5;" +
                    "-fx-background-radius: 5;";

    public final String STYLE_BBMP =
                    "-fx-background-color: transparent;" +
                    "-fx-border-color: transparent;";

    // Game screen styles

    public final String STYLE_GAME_PANEL =
                    "-fx-background-color: #06182bdd;" +
                    "-fx-border-color: #cceeee;" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 10;" +
                    "-fx-background-radius: 10;";

    public final String STYLE_CLOCK_ACTIVE =
                    "-fx-background-color: #0096ff66;" +
                    "-fx-border-color: #cceeee;" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 8;" +
                    "-fx-background-radius: 8;" +
                    "-fx-alignment: center;";

    public final String STYLE_CLOCK_IDLE =
                    "-fx-background-color: #0a274099;" +
                    "-fx-border-color: #35597a;" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 8;" +
                    "-fx-background-radius: 8;" +
                    "-fx-alignment: center;";

    public final String STYLE_OVERLAY_PANEL =
                    "-fx-background-color: #06182bf5;" +
                    "-fx-border-color: #cceeee;" +
                    "-fx-border-width: 2;" +
                    "-fx-border-radius: 14;" +
                    "-fx-background-radius: 14;";

    public final String STYLE_MOVE_LIST =
                    "-fx-background-color: #06182bcc;" +
                    "-fx-border-color: #35597a;" +
                    "-fx-border-width: 1;" +
                    "-fx-border-radius: 6;" +
                    "-fx-background-radius: 6;";
}
