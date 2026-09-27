package gui;

import javafx.scene.text.Font;

public class CommonValues {

    // ---x--- CHANGE THESE IF YOU WANT, TOUFH PREFREBLY DONT ---x---

    // Window Size
    public final int HEIGHT = 900;
    public final int WIDTH = 1600;

    // FONTS
    public final
    Font NEW_ROCKER_BUTTON = Font.loadFont(
            getClass().getResourceAsStream("/gui/resources/fonts/NewRocker-Regular.ttf"), 24
    );

    public final
    Font NEW_ROCKER_BIG = Font.loadFont(
            getClass().getResourceAsStream("/gui/resources/fonts/NewRocker-Regular.ttf"), 72
    );

    // Colour Buttons

    public final String B_HOVER_FALSE = "#080D2B";
}
