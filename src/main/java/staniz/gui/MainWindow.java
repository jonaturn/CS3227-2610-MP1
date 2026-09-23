package staniz.gui;

import static staniz.model.WorkoutData.MAX_WORKOUT_DAYS;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;

import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import staniz.model.WorkoutData.Exercise;
import staniz.model.WorkoutData.Recording;
import staniz.model.WorkoutData.State;
import staniz.model.WorkoutData.WorkoutDay;
import staniz.model.WorkoutData.WorkoutExercise;
import staniz.model.WorkoutData.WorkoutRecord;
import staniz.model.WorkoutData.WorkoutSet;
import staniz.service.WorkoutService;
import staniz.storage.StorageException;

/**
 * Presents the current workout, split editor, exercise library, workout history and progress.
 * All saved state changes go through the service.
 */
public class MainWindow extends BorderPane {
    private static final DateTimeFormatter HISTORY_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
            .withZone(ZoneId.systemDefault());
    private final WorkoutService service;
    private final Tab current = new Tab("Current Workout");
    private final Tab split = new Tab("My Split");
    private final Tab library = new Tab("Exercises");
    private final Tab history = new Tab("History");
    private final Tab progress = new Tab("Progress");
    private final TabPane navigation = new TabPane(current, split, library, history, progress);
    private final Label splitSummary = new Label();
    private final Label status = new Label("Your workouts, at your pace.");
    private int selectedDay;
    private String exerciseQuery = "";
    private boolean showArchived;

    /**
     * Connects five desktop views to the same validated workout state.
     */
    public MainWindow(WorkoutService service) {
        this.service = service;
        Label mark = label("S", "brand-mark");
        VBox brand = new VBox(2, label("STANIZ", "brand"), label("Your training, in rhythm.", "muted"));
        splitSummary.getStyleClass().add("header-summary");
        HBox header = new HBox(12, mark, brand, spacer(), splitSummary);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");
        setTop(header);
        navigation.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        navigation.setId("navigation");
        navigation.getSelectionModel().selectedItemProperty().addListener((observable, before, after) -> clearError());
        setCenter(navigation);
        status.setId("status");
        status.setWrapText(true);
        status.setMaxWidth(Double.MAX_VALUE);
        status.getStyleClass().add("status");
        setBottom(status);
        heightProperty().addListener((observable, before, after) ->
                pseudoClassStateChanged(PseudoClass.getPseudoClass("compact"), after.doubleValue() < 700));
        refresh();
    }

    private void refresh() {
        State state = service.getState();
        splitSummary.setText(state.days().isEmpty() ? "LET'S BUILD YOUR SPLIT"
                : quantity(state.days().size(), "workout day").toUpperCase(Locale.ROOT) + "  /  "
                        + quantity(state.history().size(), "saved session").toUpperCase(Locale.ROOT));
        current.setContent(currentView());
        split.setContent(splitView());
        library.setContent(libraryView());
        history.setContent(historyView());
        progress.setContent(scroll(new ProgressView(service)));
    }

    /**
     * Chooses between the three screens this tab can show: a session in progress, an invitation to
     * build a first split, or the current workout with its pinned completion actions.
     */
    private Node currentView() {
        State state = service.getState();
        if (state.session() != null) {
            return scroll(activeSession(state));
        }
        if (state.days().isEmpty()) {
            return scroll(emptySplit());
        }
        WorkoutDay day = state.days().get(state.currentDay());
        VBox content = page("Current workout", "Everything you need for your next session.");
        content.getChildren().addAll(workoutHero(state, day), cycleChips(state),
                workoutDetails(day.exercises(), true));
        if (day.exercises().isEmpty()) {
            Button plan = button("Plan this workout", "plan-current", () -> editPlan(state.currentDay()));
            plan.getStyleClass().add("primary");
            content.getChildren().add(plan);
        }
        BorderPane view = new BorderPane(scroll(content));
        view.setBottom(completionActions(day));
        return view;
    }

    /**
     * Shows the session in progress, reporting afterwards whether it was recorded or discarded.
     * The comparison uses the history size captured before the session ended.
     */
    private SessionView activeSession(State state) {
        return new SessionView(service, () -> {
            refresh();
            success(service.getState().history().size() > state.history().size()
                    ? "Session saved. Your next workout is ready."
                    : "Session discarded. Your current workout is unchanged.");
        });
    }

    /**
     * Invites a first-time user to build a split rather than presenting an empty workout.
     */
    private VBox emptySplit() {
        Button create = button("Build my split", "build-split", () -> {
            navigation.getSelectionModel().select(split);
            split.getContent().lookup("#new-day-name").requestFocus();
        });
        create.getStyleClass().add("primary");
        VBox empty = new VBox(16, label("YOUR FIRST SESSION STARTS HERE", "eyebrow"),
                label("Make room for your routine.", "workout-title"),
                label("Create your first workout day, choose your exercises, and set your targets.", "muted"),
                create);
        empty.getStyleClass().addAll("card", "onboarding");
        VBox content = page("Current workout", "Everything you need for your next session.");
        content.getChildren().add(empty);
        return content;
    }

    /**
     * Names the current day and summarizes its size and the day that follows it.
     */
    private VBox workoutHero(State state, WorkoutDay day) {
        Label title = label(day.name(), "workout-title");
        // Identified so checks can name the current day exactly; the workout-title style class is
        // shared with the onboarding card and history details.
        title.setId("current-day-name");
        VBox hero = new VBox(12, label("DAY " + (state.currentDay() + 1) + " OF " + state.days().size(), "eyebrow"),
                title);
        hero.getStyleClass().add("workout-hero");
        String next = state.days().get((state.currentDay() + 1) % state.days().size()).name();
        HBox metrics = new HBox(32, metric(Integer.toString(day.exercises().size()),
                day.exercises().size() == 1 ? "EXERCISE" : "EXERCISES"),
                metric(Integer.toString(setCount(day.exercises())), "TOTAL SETS"), metric(next, "UP NEXT"));
        metrics.setAlignment(Pos.CENTER_LEFT);
        hero.getChildren().add(metrics);
        return hero;
    }

    /**
     * Shows the whole cycle in order so the current day is visible in context.
     */
    private FlowPane cycleChips(State state) {
        FlowPane cycle = new FlowPane(8, 8);
        for (int index = 0; index < state.days().size(); index++) {
            Label chip = label((index + 1) + "  " + state.days().get(index).name(), "cycle-chip");
            if (index == state.currentDay()) {
                chip.getStyleClass().add("current-chip");
            }
            cycle.getChildren().add(chip);
        }
        return cycle;
    }

    /**
     * Builds the pinned completion actions. Every action that records or starts work needs at least
     * one exercise, so that single rule is applied in one place; skipping stays available because it
     * records nothing.
     */
    private VBox completionActions(WorkoutDay day) {
        Button start = button("Start workout", "start-session", () -> {
            service.startSession();
            refresh();
            success("Session started. Your set entries save automatically.");
        });
        start.getStyleClass().add("primary");
        Button planned = button("Complete as planned", "complete-planned", () -> {
            service.completeAsPlanned();
            refresh();
            success("Workout recorded as planned. Your next workout is ready.");
        });
        Button changed = button("Record with changes", "complete-changed", this::recordWorkout);
        changed.getStyleClass().add("secondary");
        Button unrecorded = button("Complete without recording", "complete-unrecorded", () -> {
            service.completeWithoutRecording();
            refresh();
            success("Workout completed without a performance record. Your next workout is ready.");
        });
        unrecorded.getStyleClass().add("quiet");
        for (Button action : List.of(start, planned, changed, unrecorded)) {
            action.setDisable(day.exercises().isEmpty());
        }
        Button skip = button("Skip workout", "skip-workout", () -> {
            service.skip();
            refresh();
            success("Workout skipped. No completion was recorded.");
        });
        skip.getStyleClass().add("quiet");
        VBox actions = new VBox(8, new FlowPane(10, 8, start, planned, changed),
                new FlowPane(10, 8, unrecorded, skip));
        actions.getStyleClass().add("workout-actions");
        actions.setId("workout-actions");
        return actions;
    }

    private Node splitView() {
        VBox content = page("My split",
                "Arrange up to " + MAX_WORKOUT_DAYS + " workout days in the order you want to train.");
        TextField name = new TextField();
        name.setPromptText("Day name, e.g. Push");
        name.setId("new-day-name");
        name.textProperty().addListener((observable, before, after) -> clearError());
        HBox.setHgrow(name, Priority.ALWAYS);
        Button add = button("Add day", "add-day", () -> {
            service.addDay(name.getText());
            selectedDay = service.getState().days().size() - 1;
            refresh();
            success("Workout day added. Select Edit workout to plan its exercises.");
        });
        add.setDisable(service.getState().days().size() >= MAX_WORKOUT_DAYS);
        add.getStyleClass().add("primary");
        name.setOnAction(event -> add.fire());
        Label count = label(service.getState().days().size() + " / " + MAX_WORKOUT_DAYS + " days", "badge");
        content.getChildren().add(new HBox(10, name, add, count));
        ListView<String> days = new ListView<>();
        days.setId("day-list");
        days.setPrefHeight(360);
        days.setPlaceholder(new Label("No workout days yet."));
        State state = service.getState();
        for (int index = 0; index < state.days().size(); index++) {
            days.getItems().add((index + 1) + ". " + state.days().get(index).name()
                    + (index == state.currentDay() ? "  •  Current" : ""));
        }
        renderRows(days, index -> {
            WorkoutDay day = state.days().get(index);
            VBox text = new VBox(5, label(day.name(), "row-title"),
                    label(quantity(day.exercises().size(), "exercise") + " · "
                            + quantity(setCount(day.exercises()), "set"), "muted"));
            if (index == state.currentDay()) {
                text.getChildren().add(label("CURRENT WORKOUT", "eyebrow"));
            }
            HBox row = new HBox(12, label(String.format(Locale.ROOT, "%02d", index + 1), "row-number"), text);
            row.setAlignment(Pos.CENTER_LEFT);
            return row;
        });
        VBox detail = new VBox(12);
        days.getSelectionModel().selectedIndexProperty().addListener((observable, before, after) -> {
            selectedDay = after.intValue();
            showDay(detail, selectedDay);
        });
        days.setPrefWidth(240);
        days.setMinWidth(210);
        detail.setMinWidth(0);
        HBox.setHgrow(detail, Priority.ALWAYS);
        HBox columns = new HBox(20, days, detail);
        content.getChildren().add(columns);
        if (!state.days().isEmpty()) {
            days.getSelectionModel().select(Math.max(0, Math.min(selectedDay, state.days().size() - 1)));
        }
        return scroll(content);
    }

    private void showDay(VBox detail, int index) {
        detail.getChildren().clear();
        if (index < 0) {
            return;
        }
        WorkoutDay day = service.getState().days().get(index);
        Button edit = button("Edit workout", "edit-day", () -> editPlan(index));
        edit.getStyleClass().add("primary");
        Button up = button("Move up", "move-day-up", () -> moveDay(index, -1));
        Button down = button("Move down", "move-day-down", () -> moveDay(index, 1));
        up.setDisable(index == 0);
        down.setDisable(index == service.getState().days().size() - 1);
        Button remove = button("Remove day", "remove-day", () -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Remove “" + day.name() + "” from your split? Saved history will stay available.",
                    ButtonType.OK, ButtonType.CANCEL);
            alert.initOwner(getScene().getWindow());
            alert.setHeaderText("Remove workout day");
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                service.removeDay(index);
                refresh();
                success("Workout day removed.");
            }
        });
        remove.getStyleClass().add("danger-quiet");
        up.getStyleClass().add("quiet");
        down.getStyleClass().add("quiet");
        Button duplicate = button("Duplicate day", "duplicate-day", () ->
                editText("Duplicate workout day", day.name() + " copy", value -> service.duplicateDay(index, value)));
        duplicate.setDisable(service.getState().days().size() >= MAX_WORKOUT_DAYS);
        Button copy = button("Copy sets", "copy-sets", () -> {
            RoutineDialogs.copySets(service, index, getScene().getWindow(), this::refresh);
        });
        FlowPane actions = new FlowPane(8, 8, edit, duplicate, copy, up, down, remove);
        detail.getChildren().addAll(label(day.name(), "section-title"), actions, workoutDetails(day.exercises()));
    }

    private void moveDay(int index, int direction) throws StorageException {
        service.moveDay(index, direction);
        selectedDay = index + direction;
        refresh();
        success("Workout order saved. Your current workout is unchanged.");
    }

    private Node libraryView() {
        VBox content = page("Exercise library",
                "Exercise names are fixed. Add exercises or archive ones you no longer use.");
        TextField name = new TextField();
        name.setId("new-exercise-name");
        name.setPromptText("Exercise name");
        name.textProperty().addListener((observable, before, after) -> clearError());
        HBox.setHgrow(name, Priority.ALWAYS);
        Button add = button("Add exercise", "add-library-exercise", () -> {
            service.addExercise(name.getText());
            refresh();
            success("Exercise added to your library.");
        });
        add.getStyleClass().add("primary");
        name.setOnAction(event -> add.fire());
        ListView<String> exercises = new ListView<>();
        exercises.setId("exercise-list");
        TextField search = new TextField(exerciseQuery);
        search.setId("exercise-search");
        search.setPromptText("Search your exercises…");
        search.setAccessibleText("Search exercise library");
        Label matches = label("", "eyebrow");
        CheckBox archived = new CheckBox("Show archived");
        archived.setId("show-archived");
        archived.setSelected(showArchived);
        Runnable filter = () -> {
            exerciseQuery = search.getText();
            String query = exerciseQuery.strip().toLowerCase(Locale.ROOT);
            showArchived = archived.isSelected();
            exercises.getItems().setAll(service.getState().library().stream()
                    .filter(exercise -> showArchived || !exercise.archived()).map(Exercise::name)
                    .filter(exercise -> exercise.toLowerCase(Locale.ROOT).contains(query)).toList());
            matches.setText(exercises.getItems().size() + " OF " + service.getState().exercises().size()
                    + " EXERCISES");
        };
        search.textProperty().addListener((observable, before, after) -> filter.run());
        archived.setOnAction(event -> filter.run());
        filter.run();
        exercises.setPlaceholder(label("No matching exercises. Try another name.", "empty"));
        renderRows(exercises, index -> {
            Exercise entry = libraryEntry(exercises.getItems().get(index));
            return new HBox(12, label("•", "exercise-symbol"), label(entry.name(), "row-title"),
                    label(entry.archived() ? "Archived" : "", "muted"));
        });
        exercises.setPrefHeight(320);
        VBox create = new VBox(10, label("ADD TO YOUR LIBRARY", "eyebrow"), new HBox(10, name, add));
        create.getStyleClass().add("card");
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox searchBar = new HBox(16, search, matches);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        Button archive = button("Archive / restore", "archive-exercise", () -> {
            Exercise entry = libraryEntry(exercises.getSelectionModel().getSelectedItem());
            service.archiveExercise(entry.id(), !entry.archived());
            refresh();
            success(entry.archived() ? "Exercise restored." : "Exercise archived. Existing plans and history remain.");
        });
        archive.disableProperty().bind(exercises.getSelectionModel().selectedItemProperty().isNull());
        content.getChildren().addAll(create, searchBar, archived, exercises, new HBox(10, archive));
        return scroll(content);
    }

    private Exercise libraryEntry(String name) {
        return service.getState().library().stream().filter(entry -> entry.name().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Select an exercise first."));
    }

    /**
     * Keeps invalid names editable instead of discarding the user's draft.
     */
    private void editText(String title, String initial, TextOperation operation) {
        TextInputDialog dialog = new TextInputDialog(initial);
        dialog.initOwner(getScene().getWindow());
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.getEditor().setId("name-input");
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            try {
                operation.run(dialog.getEditor().getText());
                refresh();
                success("Changes saved.");
            } catch (IllegalArgumentException | StorageException exception) {
                dialog.setHeaderText(exception.getMessage());
                event.consume();
            }
        });
        dialog.showAndWait();
    }

    private Node historyView() {
        VBox content = page("Workout history", "Your saved sessions, preserved as they were recorded.");
        List<WorkoutRecord> records = new ArrayList<>(service.getState().history());
        ListView<String> list = new ListView<>();
        list.setId("history-list");
        list.setPrefHeight(Math.min(250, Math.max(110, records.size() * 76 + 4)));
        list.setPlaceholder(label("Your recorded sessions will appear here after your first workout.", "empty"));
        TextField dayFilter = new TextField();
        dayFilter.setId("history-day-filter");
        dayFilter.setPromptText("Filter by workout day");
        ComboBox<String> exerciseFilter = new ComboBox<>();
        exerciseFilter.setId("history-exercise-filter");
        exerciseFilter.getItems().add("All exercises");
        exerciseFilter.getItems().addAll(service.getState().exercises());
        exerciseFilter.getSelectionModel().selectFirst();
        renderRows(list, index -> {
            WorkoutRecord record = records.get(index);
            VBox summary = new VBox(5, label(record.dayName(), "row-title"),
                    label(HISTORY_TIME.format(Instant.parse(record.completedAt())), "muted"));
            Label type = label(record.recording() == Recording.AS_PLANNED ? "As planned" : "With changes", "badge");
            HBox row = new HBox(12, summary, spacer(), type);
            row.setAlignment(Pos.CENTER_LEFT);
            return row;
        });
        VBox detail = new VBox(12);
        list.getSelectionModel().selectedIndexProperty().addListener((observable, before, after) -> {
            detail.getChildren().clear();
            if (after.intValue() >= 0 && after.intValue() < records.size()) {
                WorkoutRecord record = records.get(after.intValue());
                detail.getChildren().addAll(label(record.dayName(), "workout-title"),
                        label(record.recording() == Recording.AS_PLANNED
                                ? "Prescribed sets saved as planned; exact performed reps were not recorded."
                                : "Actual performed sets. Omitted sets and exercises were not recorded.", "muted"),
                        button("Correct record", "edit-history", () -> correctHistory(record)),
                        workoutDetails(record.exercises()));
            }
        });
        Label count = label("", "eyebrow");
        Runnable filter = () -> {
            list.getSelectionModel().clearSelection();
            list.getItems().clear();
            records.clear();
            String id = exerciseFilter.getSelectionModel().getSelectedIndex() <= 0 ? ""
                    : libraryEntry(exerciseFilter.getValue()).id();
            records.addAll(service.filterHistory(dayFilter.getText(), id));
            list.getItems().addAll(records.stream().map(record -> record.dayName() + " · "
                    + HISTORY_TIME.format(Instant.parse(record.completedAt()))).toList());
            count.setText(quantity(records.size(), "matching session").toUpperCase(Locale.ROOT));
            list.getSelectionModel().selectFirst();
        };
        dayFilter.textProperty().addListener((observable, before, after) -> filter.run());
        exerciseFilter.setOnAction(event -> filter.run());
        HBox.setHgrow(dayFilter, Priority.ALWAYS);
        content.getChildren().addAll(new HBox(12, dayFilter, exerciseFilter), count, list, detail);
        filter.run();
        return scroll(content);
    }

    /**
     * Opens the selected day's plan and refreshes after a successful save.
     */
    private void editPlan(int index) {
        WorkoutDialog.editPlan(service, getScene().getWindow(), index,
                () -> workoutSaved("Workout plan saved."));
    }

    /**
     * Opens actual recording for the current workout.
     */
    private void recordWorkout() {
        WorkoutDialog.recordWorkout(service, getScene().getWindow(),
                () -> workoutSaved("Actual workout saved. Your next workout is ready."));
    }

    /**
     * Opens a history correction without exposing recording-mode decisions to the window.
     */
    private void correctHistory(WorkoutRecord record) {
        WorkoutDialog.correctHistory(service, getScene().getWindow(), record,
                () -> workoutSaved("History corrected. Your workout position is unchanged."));
    }

    /**
     * Refreshes the views and reports the result after a dialog saves successfully.
     */
    private void workoutSaved(String message) {
        refresh();
        success(message);
    }

    private VBox workoutDetails(List<WorkoutExercise> exercises) {
        return workoutDetails(exercises, false);
    }

    private VBox workoutDetails(List<WorkoutExercise> exercises, boolean previous) {
        VBox result = new VBox(12);
        if (exercises.isEmpty()) {
            result.getChildren().add(label("No exercises in this workout.", "empty"));
        }
        for (WorkoutExercise exercise : exercises) {
            int exerciseNumber = result.getChildren().size() + 1;
            HBox heading = new HBox(12, label(String.format(Locale.ROOT, "%02d", exerciseNumber), "exercise-number"),
                    label(exercise.name(), "exercise-title"), spacer(),
                    label(quantity(exercise.sets().size(), "set"), "badge"));
            heading.setAlignment(Pos.CENTER_LEFT);
            VBox card = new VBox(14, heading);
            if (previous) {
                card.getChildren().add(label(ProgressView.previous(service, exercise.exerciseId()), "muted"));
            }
            card.getStyleClass().add("card");
            GridPane table = new GridPane();
            table.setVgap(8);
            table.setHgap(12);
            for (int percentage : new int[]{20, 40, 40}) {
                ColumnConstraints column = new ColumnConstraints();
                column.setPercentWidth(percentage);
                table.getColumnConstraints().add(column);
            }
            table.addRow(0, label("SET", "table-heading"), label("REPS", "table-heading"),
                    label("WEIGHT", "table-heading"));
            for (int index = 0; index < exercise.sets().size(); index++) {
                WorkoutSet set = exercise.sets().get(index);
                table.addRow(index + 1, label(Integer.toString(index + 1), "set-index"),
                        label(set.reps() + " reps", "set-value"),
                        label(BigDecimal.valueOf(set.kg()).stripTrailingZeros().toPlainString() + " kg", "set-value"));
            }
            card.getChildren().add(table);
            result.getChildren().add(card);
        }
        return result;
    }

    private static VBox page(String title, String subtitle) {
        VBox heading = new VBox(5, label(title, "page-title"), label(subtitle, "muted"));
        VBox page = new VBox(20, heading);
        page.getStyleClass().add("page");
        return page;
    }

    private static Label label(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        label.setWrapText(true);
        label.setMinWidth(0);
        return label;
    }

    private static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private static VBox metric(String value, String caption) {
        VBox metric = new VBox(5, label(value, "metric-value"), label(caption, "metric-caption"));
        metric.setMinWidth(0);
        HBox.setHgrow(metric, Priority.ALWAYS);
        return metric;
    }

    private static int setCount(List<WorkoutExercise> exercises) {
        return exercises.stream().mapToInt(exercise -> exercise.sets().size()).sum();
    }

    /**
     * Formats a count with its noun, pluralized only when the count is not one.
     * Package-private so every view counting something shares one rule instead of repeating it.
     */
    static String quantity(int count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }

    /**
     * Uses compact, descriptive rows while leaving selection and keyboard navigation to JavaFX.
     */
    private static void renderRows(ListView<String> list, IntFunction<Node> render) {
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || getIndex() < 0 || getIndex() >= list.getItems().size()
                        ? null : render.apply(getIndex()));
            }
        });
    }

    private static ScrollPane scroll(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private Button button(String text, String id, Operation operation) {
        Button button = new Button(text);
        button.setId(id);
        button.setMinWidth(Region.USE_PREF_SIZE);
        button.setOnAction(event -> {
            try {
                operation.run();
            } catch (IllegalArgumentException | StorageException exception) {
                status.setText(exception.getMessage());
                status.getStyleClass().setAll("status", "error");
            }
        });
        return button;
    }

    private void success(String message) {
        status.setText(message);
        status.getStyleClass().setAll("status");
    }

    private void clearError() {
        if (status.getStyleClass().contains("error")) {
            success("Changes save automatically as you go.");
        }
    }

    /**
     * Allows UI actions to report storage errors at one boundary.
     */
    @FunctionalInterface
    private interface Operation {
        void run() throws StorageException;
    }

    /**
     * Accepts an editable name while allowing persistence failures to be shown in the dialog.
     */
    @FunctionalInterface
    private interface TextOperation {
        void run(String value) throws StorageException;
    }
}
