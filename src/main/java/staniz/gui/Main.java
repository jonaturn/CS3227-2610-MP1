package staniz.gui;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import staniz.Staniz;
import staniz.exception.StorageException;

/**
 * Configures and displays the Staniz JavaFX window.
 */
public class Main extends Application {
    private static final String MAIN_WINDOW_FXML = "/view/MainWindow.fxml";

    /**
     * Creates the JavaFX application instance.
     */
    public Main() {
        // JavaFX constructs this class before invoking start.
    }

    /**
     * Passes command-line arguments to the JavaFX runtime.
     *
     * @param args command-line arguments passed to JavaFX.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Loads the FXML view and connects it to the Staniz backend.
     *
     * @param stage primary application window.
     */
    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource(MAIN_WINDOW_FXML));
            AnchorPane root = fxmlLoader.load();
            MainWindow mainWindow = fxmlLoader.getController();
            mainWindow.setStaniz(new Staniz());

            Scene scene = new Scene(root);
            stage.setMinWidth(480);
            stage.setMinHeight(560);
            stage.setScene(scene);
            stage.setTitle("Staniz");
            stage.show();
        } catch (IOException | StorageException exception) {
            showStartupError(exception.getMessage());
        }
    }

    /**
     * Explains startup failures before closing the application.
     *
     * @param message reason the application could not start.
     */
    private void showStartupError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Staniz could not start");
        alert.setHeaderText("Unable to open Staniz");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
