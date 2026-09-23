package staniz.gui;

import java.nio.file.Path;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import staniz.service.WorkoutService;
import staniz.storage.StorageException;
import staniz.storage.WorkoutStore;

/**
 * Opens the workout application and reports saved-data failures without overwriting user data.
 */
public class Main extends Application {
    @Override
    public void start(Stage stage) {
        try {
            WorkoutStore store = new WorkoutStore(Path.of("data", "workouts.json"));
            MainWindow root = new MainWindow(new WorkoutService(store));
            Scene scene = new Scene(root, 960, 780);
            scene.getStylesheets().add(getClass().getResource("/view/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Staniz · Workout Planner");
            stage.setMinWidth(820);
            stage.setMinHeight(640);
            stage.show();
        } catch (StorageException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Staniz could not start");
            alert.setHeaderText("Unable to load your workouts");
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
        }
    }
}
