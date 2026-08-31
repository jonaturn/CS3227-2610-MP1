package staniz.gui;

import javafx.application.Application;

/**
 * Starts the JavaFX application from a plain Java entry point.
 */
public final class Launcher {

    private Launcher() {
        // Application entry point; prevent instantiation.
    }

    /**
     * Launches the Staniz graphical application.
     *
     * @param args command-line arguments passed to JavaFX.
     */
    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}
