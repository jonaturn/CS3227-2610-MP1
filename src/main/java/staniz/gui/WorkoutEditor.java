package staniz.gui;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import staniz.model.WorkoutData.WorkoutExercise;
import staniz.model.WorkoutData.WorkoutSet;

/**
 * Edits a detached workout draft and validates every set before the service saves anything.
 */
public class WorkoutEditor extends VBox {
    private final List<String> library;
    private final boolean actual;
    private final VBox exercises = new VBox(12);
    private final List<ExerciseFields> fields = new ArrayList<>();
    private Runnable onEdited = () -> { };

    /**
     * Creates a cancellable editor for planned sets or exact performed sets.
     */
    public WorkoutEditor(List<String> library, List<WorkoutExercise> initial, boolean actual) {
        super(12);
        this.library = library;
        this.actual = actual;
        Label help = new Label(actual
                ? "Enter exact reps performed. Remove skipped sets or exercises; add any extra work."
                : "Each set accepts any positive whole-number rep count or range, for example 5, 15, or 10-15.");
        help.setWrapText(true);
        help.getStyleClass().add("muted");
        Button add = new Button("+ Add exercise");
        add.setId("add-workout-exercise");
        add.getStyleClass().add("secondary");
        add.setOnAction(event -> addExercise(null));
        getChildren().addAll(help, exercises, add);
        initial.forEach(this::addExercise);
    }

    /**
     * Reads rows in order, returning immutable values for a plan or history snapshot.
     * Each row reports its own first problem, so the caller shows one actionable message.
     */
    public List<WorkoutExercise> readExercises() {
        return fields.stream().map(ExerciseFields::read).toList();
    }

    public void setOnEdited(Runnable onEdited) {
        this.onEdited = onEdited;
    }

    private void addExercise(WorkoutExercise initial) {
        ExerciseFields row = new ExerciseFields(initial);
        fields.add(row);
        exercises.getChildren().add(row);
        onEdited.run();
    }

    /**
     * Owns one exercise selector and its ordered set editors.
     */
    private class ExerciseFields extends VBox {
        private final WorkoutExercise initial;
        private final ComboBox<String> name = new ComboBox<>();
        private final VBox setRows = new VBox(8);
        private final List<SetFields> sets = new ArrayList<>();

        ExerciseFields(WorkoutExercise initial) {
            super(10);
            this.initial = initial;
            getStyleClass().add("card");
            name.getItems().setAll(library);
            name.setPromptText("Choose an exercise");
            name.setMaxWidth(Double.MAX_VALUE);
            name.getStyleClass().add("exercise-selector");
            name.setAccessibleText("Exercise name");
            name.valueProperty().addListener((observable, before, after) -> {
                name.getStyleClass().remove("invalid");
                onEdited.run();
            });
            HBox.setHgrow(name, Priority.ALWAYS);
            Button remove = new Button("Remove exercise");
            remove.getStyleClass().add("danger-quiet");
            remove.setOnAction(event -> {
                fields.remove(this);
                exercises.getChildren().remove(this);
                onEdited.run();
            });
            Button addSet = new Button("+ Add set");
            addSet.getStyleClass().add("quiet");
            addSet.setOnAction(event -> addSet(null));
            Button duplicate = new Button("Duplicate last set");
            duplicate.getStyleClass().add("quiet");
            duplicate.setTooltip(new Tooltip("Copy the last set's kg and reps into a new row."));
            duplicate.setOnAction(event -> {
                SetFields previous = sets.getLast();
                addSet(null);
                sets.getLast().weight.setText(previous.weight.getText());
                sets.getLast().reps.setText(previous.reps.getText());
            });
            Button up = new Button("Move up");
            up.getStyleClass().add("quiet");
            up.setOnAction(event -> move(-1));
            Button down = new Button("Move down");
            down.getStyleClass().add("quiet");
            down.setOnAction(event -> move(1));
            Label setHeading = new Label("SET");
            setHeading.setMinWidth(42);
            Label weightHeading = new Label("WEIGHT · KG");
            weightHeading.setPrefWidth(100);
            Label repsHeading = new Label(actual ? "REPS PERFORMED" : "REPS / RANGE");
            HBox headings = new HBox(12, setHeading, weightHeading, repsHeading);
            headings.getStyleClass().add("table-heading");
            getChildren().addAll(new HBox(10, name, remove), new HBox(8, up, down),
                    headings, setRows, new HBox(8, addSet, duplicate));
            if (initial == null) {
                addSet(null);
            } else {
                name.setValue(initial.name());
                initial.sets().forEach(this::addSet);
            }
        }

        /**
         * Reads this row, checking its exercise before its sets so the reported problem,
         * the marked control and the focused control always refer to the same row.
         */
        private WorkoutExercise read() {
            String chosen = name.getValue();
            if (chosen == null || chosen.isBlank()) {
                throw invalidName();
            }
            List<WorkoutSet> values = sets.stream().map(SetFields::read).toList();
            String id = initial != null && initial.name().equals(chosen) ? initial.exerciseId() : "";
            return new WorkoutExercise(id, chosen, values);
        }

        /**
         * Reports an unchosen exercise the way set errors report themselves, naming the row
         * so the message identifies one card among several.
         */
        private IllegalArgumentException invalidName() {
            if (!name.getStyleClass().contains("invalid")) {
                name.getStyleClass().add("invalid");
            }
            name.requestFocus();
            return new IllegalArgumentException("Exercise " + (fields.indexOf(this) + 1)
                    + ": choose an exercise for this row.");
        }

        private void move(int direction) {
            int from = fields.indexOf(this);
            int to = from + direction;
            if (to >= 0 && to < fields.size()) {
                fields.remove(from);
                fields.add(to, this);
                exercises.getChildren().setAll(fields);
                onEdited.run();
            }
        }

        private void addSet(WorkoutSet initial) {
            SetFields set = new SetFields(initial);
            Button remove = new Button("Remove set");
            remove.getStyleClass().add("danger-quiet");
            remove.setOnAction(event -> {
                sets.remove(set);
                setRows.getChildren().remove(set);
                numberSets();
                onEdited.run();
            });
            set.getChildren().add(remove);
            sets.add(set);
            setRows.getChildren().add(set);
            numberSets();
            onEdited.run();
        }

        private void numberSets() {
            for (int index = 0; index < sets.size(); index++) {
                sets.get(index).number.setText("Set " + (index + 1));
                sets.get(index).weight.setAccessibleText("Set " + (index + 1) + " weight in kilograms");
                sets.get(index).reps.setAccessibleText("Set " + (index + 1) + " reps");
                Button remove = (Button) sets.get(index).getChildren().getLast();
                remove.setDisable(sets.size() == 1);
                remove.setTooltip(new Tooltip(sets.size() == 1
                        ? "Remove the exercise to omit all its sets." : "Remove this set."));
            }
        }
    }

    /**
     * Stores unfinished typing without mutating the workout model.
     */
    private class SetFields extends HBox {
        private final Label number = new Label();
        private final TextField weight = new TextField();
        private final TextField reps = new TextField();

        SetFields(WorkoutSet initial) {
            super(12);
            getStyleClass().add("set-row");
            number.setMinWidth(42);
            weight.setPromptText("kg");
            weight.setPrefWidth(100);
            weight.setMaxWidth(100);
            weight.getStyleClass().add("set-weight");
            reps.setPromptText(actual ? "Exact reps" : "Count or range");
            reps.setPrefWidth(135);
            reps.setMaxWidth(135);
            reps.getStyleClass().add("set-reps");
            if (initial != null) {
                weight.setText(Double.toString(initial.kg()));
                // A prescribed range is not evidence of exact performed reps.
                if (!actual || initial.minReps() == initial.maxReps()) {
                    reps.setText(initial.reps());
                }
            }
            weight.textProperty().addListener((observable, before, after) -> edited(weight));
            reps.textProperty().addListener((observable, before, after) -> edited(reps));
            getChildren().addAll(number, weight, reps);
        }

        private WorkoutSet read() {
            String count = reps.getText().strip().replace('–', '-');
            if (!count.matches(actual ? "[0-9]+" : "[0-9]+(\\s*-\\s*[0-9]+)?")) {
                throw invalid(reps, actual ? "Enter an exact positive rep count for every performed set."
                        : "Enter a positive whole-number rep count or range, for example 5, 15, or 10-15.");
            }
            int minimum;
            int maximum;
            try {
                String[] bounds = count.split("\\s*-\\s*");
                minimum = Integer.parseInt(bounds[0]);
                maximum = bounds.length == 1 ? minimum : Integer.parseInt(bounds[1]);
            } catch (NumberFormatException exception) {
                throw invalid(reps, "The rep count is too large. Enter a smaller whole number.");
            }
            if (minimum < 1 || maximum < minimum) {
                throw invalid(reps, "Reps must be positive, with the maximum at least the minimum.");
            }
            double kg;
            try {
                kg = Double.parseDouble(weight.getText().strip());
            } catch (NumberFormatException exception) {
                throw invalid(weight, "Enter a weight in kg, such as 0, 20, or 62.5.");
            }
            if (!Double.isFinite(kg) || kg < 0) {
                throw invalid(weight, "Weight must be a finite number of kg, zero or greater.");
            }
            return new WorkoutSet(kg, minimum, maximum);
        }

        private void edited(TextField field) {
            field.getStyleClass().remove("invalid");
            onEdited.run();
        }

        private IllegalArgumentException invalid(TextField field, String message) {
            if (!field.getStyleClass().contains("invalid")) {
                field.getStyleClass().add("invalid");
            }
            field.requestFocus();
            return new IllegalArgumentException(number.getText() + ": " + message);
        }
    }
}
