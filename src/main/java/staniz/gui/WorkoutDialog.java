package staniz.gui;

import java.util.List;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import staniz.model.WorkoutData.Recording;
import staniz.model.WorkoutData.WorkoutDay;
import staniz.model.WorkoutData.WorkoutExercise;
import staniz.model.WorkoutData.WorkoutRecord;
import staniz.service.WorkoutService;
import staniz.storage.StorageException;

/**
 * Owns the modal workout form, save routing and validation feedback around a WorkoutEditor.
 * Invalid or unsaved drafts remain editable after a validation or storage failure.
 */
final class WorkoutDialog {
    private WorkoutDialog() {
    }

    /**
     * Edits the selected split day using planned rep counts or ranges.
     */
    static void editPlan(WorkoutService service, Window owner, int index, Runnable onSaved) {
        show(service, owner, false, index, null, onSaved);
    }

    /**
     * Records exact performed reps for the current workout and advances after saving.
     */
    static void recordWorkout(WorkoutService service, Window owner, Runnable onSaved) {
        show(service, owner, true, service.getState().currentDay(), null, onSaved);
    }

    /**
     * Corrects a history snapshot using the rep requirements of its original recording type.
     */
    static void correctHistory(WorkoutService service, Window owner, WorkoutRecord record, Runnable onSaved) {
        show(service, owner, record.recording() == Recording.WITH_CHANGES, -1, record, onSaved);
    }

    /**
     * Opens a plan, completion or history form and notifies the caller after saving successfully.
     * A non-null record selects history correction and ignores index; otherwise index selects a split day.
     * Actual mode requires exact performed reps. Cancel discards the draft without saving.
     */
    private static void show(WorkoutService service, Window owner, boolean actual, int index,
                             WorkoutRecord record, Runnable onSaved) {
        WorkoutDay day = record == null ? service.getState().days().get(index)
                : new WorkoutDay("history", record.dayName(), record.exercises());
        DialogText text = dialogText(actual, record != null);
        Dialog<Void> dialog = createDialog(owner, text);
        TextField name = new TextField(day.name());
        name.setId("edit-day-name");
        name.setDisable(actual && record == null);
        WorkoutEditor editor = new WorkoutEditor(service.choices(day.exercises()), day.exercises(), actual);
        Label error = createErrorLabel(name, editor);
        dialog.getDialogPane().setContent(createForm(name, editor, error));
        Button save = configureSaveButton(dialog, text.saveText());
        SaveOperation operation = saveOperation(service, actual, index, record);
        configureSaveAction(save, name, editor, error, operation, onSaved);
        dialog.showAndWait();
    }

    /**
     * Selects all mode-specific dialog text together so setup does not repeat mode checks.
     */
    private static DialogText dialogText(boolean actual, boolean history) {
        if (history) {
            return new DialogText("Correct history", "Correct this session · original date is preserved",
                    "Save correction");
        }
        if (actual) {
            return new DialogText("Record workout", "How did your session go?", "Save & complete");
        }
        return new DialogText("Edit workout", "Build your workout", "Save workout");
    }

    /**
     * Creates the owned, styled dialog with its standard confirmation and cancellation buttons.
     */
    private static Dialog<Void> createDialog(Window owner, DialogText text) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(text.title());
        dialog.setHeaderText(text.header());
        dialog.setResizable(true);
        dialog.getDialogPane().setId("workout-dialog");
        dialog.getDialogPane().getStylesheets()
                .add(WorkoutDialog.class.getResource("/view/styles.css").toExternalForm());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        return dialog;
    }

    /**
     * Shows errors only when present and clears stale feedback when either draft input changes.
     */
    private static Label createErrorLabel(TextField name, WorkoutEditor editor) {
        Label error = label("", "editor-error");
        error.setId("editor-error");
        error.setMaxWidth(Double.MAX_VALUE);
        error.visibleProperty().bind(error.textProperty().isNotEmpty());
        error.managedProperty().bind(error.visibleProperty());
        editor.setOnEdited(() -> error.setText(""));
        name.textProperty().addListener((observable, before, after) -> error.setText(""));
        return error;
    }

    /**
     * Keeps the editable content scrollable with validation feedback beneath it.
     */
    private static BorderPane createForm(TextField name, WorkoutEditor editor, Label error) {
        VBox content = new VBox(12, label("WORKOUT DAY", "eyebrow"), name, editor);
        content.getStyleClass().add("page");
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportWidth(680);
        scroll.setPrefViewportHeight(460);
        BorderPane form = new BorderPane(scroll);
        form.setBottom(error);
        return form;
    }

    /**
     * Configures the existing OK button so JavaFX retains its normal dialog-close behavior.
     */
    private static Button configureSaveButton(Dialog<Void> dialog, String text) {
        Button save = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        save.setText(text);
        ButtonBar.setButtonUniformSize(save, false);
        save.setPrefWidth(170);
        save.setMinWidth(160);
        save.setId("save-workout");
        save.getStyleClass().add("primary");
        return save;
    }

    /**
     * Selects the service operation once while leaving the common form independent of save routing.
     */
    private static SaveOperation saveOperation(WorkoutService service, boolean actual, int index,
                                              WorkoutRecord record) {
        if (record != null) {
            return (name, exercises) -> service.correctRecord(record.id(), name, exercises);
        }
        if (actual) {
            return (name, exercises) -> service.completeWithChanges(exercises);
        }
        return (name, exercises) -> service.updateDay(index, name, exercises);
    }

    /**
     * Validates and saves the draft, retaining the dialog and its inputs when an expected error occurs.
     * The callback runs outside the guarded region so a failure after a committed save is never
     * reported as a retryable form error, which would invite a second save of the same workout.
     */
    private static void configureSaveAction(Button save, TextField name, WorkoutEditor editor, Label error,
                                            SaveOperation operation, Runnable onSaved) {
        save.addEventFilter(ActionEvent.ACTION, event -> {
            boolean saved = false;
            try {
                List<WorkoutExercise> exercises = editor.readExercises();
                operation.save(name.getText(), exercises);
                saved = true;
            } catch (IllegalArgumentException | StorageException exception) {
                error.setText(exception.getMessage());
                event.consume();
            }
            if (saved) {
                onSaved.run();
            }
        });
    }

    /**
     * The title, guidance and button caption for one dialog mode.
     */
    private record DialogText(String title, String header, String saveText) {
    }

    /**
     * Saves the edited values while allowing storage failures to reach the common form handler.
     */
    @FunctionalInterface
    private interface SaveOperation {
        void save(String name, List<WorkoutExercise> exercises) throws StorageException;
    }

    private static Label label(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        label.setWrapText(true);
        label.setMinWidth(0);
        return label;
    }
}
