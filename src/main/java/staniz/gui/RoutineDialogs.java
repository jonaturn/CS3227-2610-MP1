package staniz.gui;

import java.util.List;

import javafx.event.ActionEvent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import staniz.model.WorkoutData.WorkoutDay;
import staniz.service.WorkoutService;
import staniz.storage.StorageException;

/**
 * Small routine operations that retain their draft when validation or saving fails.
 */
final class RoutineDialogs {
    private RoutineDialogs() {
    }

    /**
     * Copies a prescription across exercises or days, replacing the destination's sets.
     */
    static void copySets(WorkoutService service, int initialDay, Window owner, Runnable refresh) {
        Dialog<Void> dialog = createDialog(owner);
        Selectors selectors = createSelectors(service, initialDay);
        Label error = createErrorLabel(selectors);
        dialog.getDialogPane().setContent(createForm(selectors, error));
        configureCopyAction(dialog, service, selectors, error, refresh);
        dialog.showAndWait();
    }

    /**
     * Creates the owned, styled dialog with its standard confirmation and cancellation buttons.
     */
    private static Dialog<Void> createDialog(Window owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Copy sets");
        dialog.setHeaderText("Replace the destination's sets with a copy of the source sets");
        dialog.getDialogPane().getStylesheets()
                .add(RoutineDialogs.class.getResource("/view/styles.css").toExternalForm());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        return dialog;
    }

    /**
     * Builds the four selectors, keeping each day's exercise list in step with the day chosen beside it.
     * Both sides start on the day the user opened the dialog from.
     */
    private static Selectors createSelectors(WorkoutService service, int initialDay) {
        Selectors selectors = new Selectors(new ComboBox<>(), new ComboBox<>(), new ComboBox<>(), new ComboBox<>());
        selectors.sourceDay().setId("copy-source-day");
        selectors.targetDay().setId("copy-target-day");
        selectors.sourceExercise().setId("copy-source-exercise");
        selectors.targetExercise().setId("copy-target-exercise");
        for (WorkoutDay day : service.getState().days()) {
            selectors.sourceDay().getItems().add(day.name());
            selectors.targetDay().getItems().add(day.name());
        }
        selectors.sourceDay().setOnAction(event ->
                populate(service, selectors.sourceDay(), selectors.sourceExercise()));
        selectors.targetDay().setOnAction(event ->
                populate(service, selectors.targetDay(), selectors.targetExercise()));
        selectors.sourceDay().getSelectionModel().select(initialDay);
        selectors.targetDay().getSelectionModel().select(initialDay);
        populate(service, selectors.sourceDay(), selectors.sourceExercise());
        populate(service, selectors.targetDay(), selectors.targetExercise());
        return selectors;
    }

    /**
     * Shows errors only when present and clears stale feedback when any selection changes,
     * which is this dialog's equivalent of the user editing a field.
     */
    private static Label createErrorLabel(Selectors selectors) {
        Label error = new Label();
        error.setWrapText(true);
        error.setId("copy-error");
        error.getStyleClass().add("editor-error");
        error.setMaxWidth(Double.MAX_VALUE);
        error.visibleProperty().bind(error.textProperty().isNotEmpty());
        error.managedProperty().bind(error.visibleProperty());
        for (ComboBox<String> box : selectors.all()) {
            box.valueProperty().addListener((observable, before, after) -> error.setText(""));
        }
        return error;
    }

    /**
     * Groups each side under its own heading with validation feedback beneath both.
     */
    private static VBox createForm(Selectors selectors, Label error) {
        VBox content = new VBox(10, new Label("Copy from"), selectors.sourceDay(), selectors.sourceExercise(),
                new Label("Copy to"), selectors.targetDay(), selectors.targetExercise(), error);
        content.setPrefWidth(440);
        return content;
    }

    /**
     * Copies the selection, retaining the dialog and its choices when an expected error occurs.
     */
    private static void configureCopyAction(Dialog<Void> dialog, WorkoutService service, Selectors selectors,
                                            Label error, Runnable refresh) {
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            boolean copied = false;
            try {
                service.copySets(selectors.sourceDay().getSelectionModel().getSelectedIndex(),
                        selectors.sourceExercise().getSelectionModel().getSelectedIndex(),
                        selectors.targetDay().getSelectionModel().getSelectedIndex(),
                        selectors.targetExercise().getSelectionModel().getSelectedIndex());
                copied = true;
            } catch (IllegalArgumentException | StorageException exception) {
                error.setText(exception.getMessage());
                event.consume();
            }
            // The refresh runs outside the guarded region. Inside it, a failure after a committed
            // copy would consume the event, so every further OK would re-copy and refuse to close.
            if (copied) {
                refresh.run();
            }
        });
    }

    /**
     * The four selectors of one copy, paired so setup and save routing pass them as a unit.
     */
    private record Selectors(ComboBox<String> sourceDay, ComboBox<String> sourceExercise,
                             ComboBox<String> targetDay, ComboBox<String> targetExercise) {
        private List<ComboBox<String>> all() {
            return List.of(sourceDay, sourceExercise, targetDay, targetExercise);
        }
    }

    private static void populate(WorkoutService service, ComboBox<String> days, ComboBox<String> exercises) {
        int index = days.getSelectionModel().getSelectedIndex();
        exercises.getItems().clear();
        if (index >= 0) {
            service.getState().days().get(index).exercises().forEach(exercise ->
                    exercises.getItems().add(exercise.name()));
            exercises.getSelectionModel().selectFirst();
        }
    }
}
