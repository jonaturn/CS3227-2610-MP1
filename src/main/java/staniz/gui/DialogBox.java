package staniz.gui;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * Displays one message bubble in the Staniz conversation.
 */
public class DialogBox extends HBox {
    private static final String DIALOG_BOX_FXML = "/view/DialogBox.fxml";

    @FXML
    private Label dialog;

    /**
     * Loads the reusable dialog-box layout and associates it with this control.
     */
    public DialogBox() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(DialogBox.class.getResource(DIALOG_BOX_FXML));
            fxmlLoader.setController(this);
            fxmlLoader.setRoot(this);
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load the dialog-box layout.", exception);
        }
    }

    /**
     * Creates a right-aligned bubble for user input.
     *
     * @param text command entered by the user.
     * @return configured user dialog.
     */
    public static DialogBox getUserDialog(String text) {
        DialogBox dialogBox = new DialogBox();
        dialogBox.dialog.setText(text);
        dialogBox.dialog.getStyleClass().add("user-dialog");
        dialogBox.setAlignment(Pos.TOP_RIGHT);
        return dialogBox;
    }

    /**
     * Creates a left-aligned bubble for a successful Staniz response.
     *
     * @param text response produced by Staniz.
     * @return configured Staniz dialog.
     */
    public static DialogBox getStanizDialog(String text) {
        return getStanizDialog(text, "staniz-dialog");
    }

    /**
     * Creates a left-aligned response bubble with the requested visual style.
     *
     * @param text response produced by Staniz.
     * @param styleClass CSS style applied to the message label.
     * @return configured response dialog.
     */
    private static DialogBox getStanizDialog(String text, String styleClass) {
        DialogBox dialogBox = new DialogBox();
        dialogBox.dialog.setText(text);
        dialogBox.dialog.getStyleClass().add(styleClass);
        dialogBox.setAlignment(Pos.TOP_LEFT);
        return dialogBox;
    }

    /**
     * Creates a left-aligned bubble for a rejected command or storage failure.
     *
     * @param text error response produced by Staniz.
     * @return configured error dialog.
     */
    public static DialogBox getErrorDialog(String text) {
        return getStanizDialog(text, "error-dialog");
    }
}
