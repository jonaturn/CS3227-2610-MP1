package staniz.gui;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import staniz.model.WorkoutData.SessionExercise;
import staniz.model.WorkoutData.SessionSet;
import staniz.service.WorkoutService;
import staniz.storage.StorageException;

/**
 * Saves per-set session drafts immediately and allows checked sets to be recorded as actual performance.
 */
public class SessionView extends VBox {
    private final WorkoutService service;
    private final Runnable refresh;
    private final Label remaining = new Label();
    private final Label error = new Label();
    private final ProgressBar progress = new ProgressBar();

    /**
     * Restores a session's text and checkboxes from persistent state.
     */
    public SessionView(WorkoutService service, Runnable refresh) {
        super(16);
        this.service = service;
        this.refresh = refresh;
        getStyleClass().addAll("page", "session-view");
        Label title = new Label(service.getState().session().dayName() + " · In progress");
        title.getStyleClass().add("page-title");
        remaining.setId("session-remaining");
        progress.setMaxWidth(Double.MAX_VALUE);
        error.setId("session-error");
        error.getStyleClass().add("editor-error");
        error.setWrapText(true);
        error.managedProperty().bind(error.textProperty().isNotEmpty());
        error.visibleProperty().bind(error.managedProperty());
        Button finish = new Button("Finish session");
        finish.setId("finish-session");
        finish.getStyleClass().add("primary");
        finish.setOnAction(event -> finish());
        Button discard = new Button("Discard session");
        discard.setId("discard-session");
        discard.getStyleClass().add("danger-quiet");
        discard.setOnAction(event -> {
            if (confirm("Discard this session?", "Your entries will be discarded. The split stays on this day.")) {
                try {
                    service.discardSession();
                    refresh.run();
                } catch (StorageException exception) {
                    error.setText(exception.getMessage());
                }
            }
        });
        getChildren().addAll(title, new Label("Entries save as you type. Tick each set when it is complete."),
                remaining, progress, new HBox(10, finish, discard), error);
        for (int exerciseIndex = 0; exerciseIndex < service.getState().session().exercises().size(); exerciseIndex++) {
            SessionExercise exercise = service.getState().session().exercises().get(exerciseIndex);
            Label previous = new Label(ProgressView.previous(service, exercise.exerciseId()));
            previous.setWrapText(true);
            previous.getStyleClass().add("muted");
            Label name = new Label(exercise.name());
            name.getStyleClass().add("exercise-title");
            VBox card = new VBox(10, name, previous, headings());
            card.getStyleClass().add("card");
            for (int setIndex = 0; setIndex < exercise.sets().size(); setIndex++) {
                card.getChildren().add(new SetRow(exerciseIndex, setIndex, exercise.name(),
                        exercise.sets().get(setIndex)));
            }
            getChildren().add(card);
        }
        updateCount();
    }

    private void updateCount() {
        var sets = service.getState().session().exercises().stream().flatMap(item -> item.sets().stream()).toList();
        long completed = sets.stream().filter(SessionSet::completed).count();
        remaining.setText(completed + " / " + sets.size() + " sets completed · " + (sets.size() - completed) + " left");
        progress.setProgress((double) completed / sets.size());
    }

    private HBox headings() {
        Label plan = new Label("PLANNED SET");
        plan.setPrefWidth(190);
        Label weight = new Label("ACTUAL KG");
        weight.setPrefWidth(100);
        Label reps = new Label("ACTUAL REPS");
        reps.setPrefWidth(110);
        HBox row = new HBox(12, plan, weight, reps);
        row.getStyleClass().add("table-heading");
        return row;
    }

    private void finish() {
        boolean incomplete = service.getState().session().exercises().stream().flatMap(item -> item.sets().stream())
                .anyMatch(set -> !set.completed());
        if (incomplete && !confirm("Finish with incomplete sets?", "Only checked sets will be recorded.")) {
            return;
        }
        try {
            service.finishSession();
            refresh.run();
        } catch (IllegalArgumentException | StorageException exception) {
            error.setText(exception.getMessage());
        }
    }

    private boolean confirm(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.OK, ButtonType.CANCEL);
        alert.initOwner(getScene().getWindow());
        alert.setHeaderText(title);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    /**
     * Retains what the user typed after a failed save and resynchronizes only the completion box,
     * matching how the workout dialog keeps an unsaved draft editable so it can be retried.
     */
    private class SetRow extends HBox {
        private final int exerciseIndex;
        private final int setIndex;
        private final TextField weight;
        private final TextField reps;
        private final CheckBox done;

        SetRow(int exerciseIndex, int setIndex, String exerciseName, SessionSet set) {
            super(12);
            this.exerciseIndex = exerciseIndex;
            this.setIndex = setIndex;
            // The plan label identifies the row visually; assistive technology needs the same
            // position spoken, since every row otherwise presents identical fields and a "Done" box.
            String position = exerciseName + ", set " + (setIndex + 1);
            Label plan = new Label((setIndex + 1) + ".  " + set.planned().kg() + " kg × " + set.planned().reps());
            plan.setPrefWidth(190);
            weight = new TextField(set.weight());
            weight.setPrefWidth(100);
            weight.setMaxWidth(100);
            weight.setId("session-weight-" + exerciseIndex + "-" + setIndex);
            weight.setAccessibleText(position + " weight in kilograms");
            reps = new TextField(set.reps());
            reps.setPrefWidth(110);
            reps.setMaxWidth(110);
            reps.setPromptText("Exact reps");
            reps.setId("session-reps-" + exerciseIndex + "-" + setIndex);
            reps.setAccessibleText(position + " reps performed");
            done = new CheckBox("Done");
            done.setSelected(set.completed());
            done.setId("session-done-" + exerciseIndex + "-" + setIndex);
            done.setAccessibleText("Mark " + position + " complete");
            weight.setDisable(set.completed());
            reps.setDisable(set.completed());
            weight.textProperty().addListener((observable, before, after) -> save());
            reps.textProperty().addListener((observable, before, after) -> save());
            done.setOnAction(event -> save());
            getChildren().addAll(plan, weight, reps, done);
        }

        private void save() {
            try {
                service.updateSessionSet(exerciseIndex, setIndex, weight.getText(), reps.getText(), done.isSelected());
                error.setText("");
                updateCount();
            } catch (IllegalArgumentException | StorageException exception) {
                error.setText(exception.getMessage());
                // Only completion is resynchronized. Restoring the text too would discard characters
                // the user just typed whenever the write failed, and would be a no-op otherwise, since
                // values are validated only on completion and so are already saved by then.
                SessionSet saved = service.getState().session().exercises().get(exerciseIndex).sets().get(setIndex);
                done.setSelected(saved.completed());
            }
            weight.setDisable(done.isSelected());
            reps.setDisable(done.isSelected());
        }
    }
}
