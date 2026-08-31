package staniz.gui;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

/**
 * Displays one message bubble in the Staniz conversation.
 */
public class DialogBox extends HBox {
    private static final String DIALOG_BOX_FXML = "/view/DialogBox.fxml";
    private static final String USER_AVATAR = "/images/user-avatar.png";
    private static final String STANIZ_AVATAR = "/images/staniz-avatar.png";
    private static final double MAX_DIALOG_WIDTH_RATIO = 0.85;
    private static final double AVATAR_SIZE = 40.0;

    @FXML
    private Label dialog;

    @FXML
    private ImageView displayPicture;

    /**
     * Loads the reusable dialog-box layout and associates it with this control.
     */
    public DialogBox() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(DialogBox.class.getResource(DIALOG_BOX_FXML));
            fxmlLoader.setController(this);
            fxmlLoader.setRoot(this);
            fxmlLoader.load();
            dialog.maxWidthProperty().bind(widthProperty().multiply(MAX_DIALOG_WIDTH_RATIO));
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
        dialogBox.setAvatar(USER_AVATAR);
        dialogBox.getChildren().setAll(dialogBox.dialog, dialogBox.displayPicture);
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
        dialogBox.setAvatar(STANIZ_AVATAR);
        dialogBox.setAlignment(Pos.TOP_LEFT);
        return dialogBox;
    }

    /**
     * Loads an avatar and clips it to a circle without modifying the source image.
     *
     * @param resourcePath classpath location of the avatar image.
     */
    private void setAvatar(String resourcePath) {
        var resource = DialogBox.class.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Unable to load avatar: " + resourcePath);
        }

        displayPicture.setImage(new Image(resource.toExternalForm()));
        double radius = AVATAR_SIZE / 2;
        displayPicture.setClip(new Circle(radius, radius, radius));
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
