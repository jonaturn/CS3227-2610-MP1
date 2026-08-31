package staniz.gui;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import staniz.Staniz;
import staniz.command.CommandResult;
import staniz.exception.StanizException;
import staniz.exception.StorageException;
import staniz.ui.ResponseFormatter;

/**
 * Handles interactions in the main Staniz chat window.
 */
public class MainWindow {
    private static final Duration EXIT_DELAY = Duration.millis(900);

    @FXML
    private Button sendButton;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private TextField userInput;
    @FXML
    private VBox dialogContainer;

    private Staniz staniz;

    /**
     * Creates the controller instance that the FXML loader will initialize.
     */
    public MainWindow() {
        // Fields annotated with FXML are injected after construction.
    }

    /**
     * Connects the view to its command-processing backend and displays a greeting.
     *
     * @param staniz command-processing backend.
     */
    public void setStaniz(Staniz staniz) {
        this.staniz = staniz;
        dialogContainer.getChildren().add(
                DialogBox.getStanizDialog(ResponseFormatter.getGreetingMessage()));
        userInput.requestFocus();
    }

    /**
     * Keeps the most recent message visible as the conversation grows.
     */
    @FXML
    private void initialize() {
        dialogContainer.heightProperty().addListener(
                (observable, oldHeight, newHeight) -> scrollPane.setVvalue(1.0));
    }

    /**
     * Sends the text field contents to Staniz and displays both sides of the exchange.
     */
    @FXML
    private void handleUserInput() {
        String input = userInput.getText().strip();
        if (input.isEmpty()) {
            return;
        }

        dialogContainer.getChildren().add(DialogBox.getUserDialog(input));
        userInput.clear();

        try {
            CommandResult result = staniz.executeCommand(input);
            dialogContainer.getChildren().add(
                    DialogBox.getStanizDialog(result.getResponse()));
            if (result.shouldExit()) {
                prepareToExit();
            }
        } catch (StanizException | StorageException exception) {
            dialogContainer.getChildren().add(
                    DialogBox.getErrorDialog(exception.getMessage()));
        }
    }

    /**
     * Prevents further input and closes after the farewell remains visible briefly.
     */
    private void prepareToExit() {
        userInput.setDisable(true);
        sendButton.setDisable(true);
        PauseTransition exitPause = new PauseTransition(EXIT_DELAY);
        exitPause.setOnFinished(event -> ((Stage) sendButton.getScene().getWindow()).close());
        exitPause.play();
    }
}
