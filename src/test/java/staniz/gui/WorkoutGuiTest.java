package staniz.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.stage.Window;
import staniz.model.WorkoutData.Exercise;
import staniz.model.WorkoutData.Recording;
import staniz.model.WorkoutData.SessionSet;
import staniz.model.WorkoutData.State;
import staniz.model.WorkoutData.WorkoutDay;
import staniz.model.WorkoutData.WorkoutExercise;
import staniz.model.WorkoutData.WorkoutSet;
import staniz.service.WorkoutService;
import staniz.storage.StorageException;
import staniz.storage.WorkoutStore;

/**
 * Runs the interleaved positive and negative desktop scenarios in one fail-fast sequence.
 * Uses isolated saves and JavaFX controls, with screenshots and a transcript for review.
 */
@Tag("gui")
class WorkoutGuiTest {
    @TempDir
    private Path directory;
    private final List<String> transcript = new ArrayList<>();
    private String lastScenario = "<none>";
    private Stage stage;
    private MainWindow root;
    private WorkoutService service;

    @BeforeAll
    static void startToolkit() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            ready.countDown();
        });
        assertTrue(ready.await(20, TimeUnit.SECONDS), "JavaFX did not start.");
    }

    @AfterAll
    static void stopToolkit() {
        Platform.exit();
    }

    @Test
    void workoutJourney_interleavesValidInvalidAndRestartScenarios() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            try {
                runJourney();
            } catch (Throwable failure) {
                // The scenarios share accumulated state, so the run stops at the first failure.
                // Naming the last completed one saves counting entries in the transcript.
                throw new AssertionError("Failed after " + transcript.size() + " scenarios. Last completed: "
                        + lastScenario + ". Full record: build/gui-test/transcript.txt", failure);
            } finally {
                if (stage != null) {
                    stage.close();
                }
                Path artifacts = artifacts();
                Files.write(artifacts.resolve("transcript.txt"), transcript);
            }
            return null;
        });
        Platform.runLater(task);
        task.get(120, TimeUnit.SECONDS);
    }

    private void runJourney() throws Exception {
        Path file = directory.resolve("workouts.json");
        service = new WorkoutService(new WorkoutStore(file));
        showWindow();
        assertTrue(allText(root).contains("Create your first workout"));
        snapshot(root, "01-empty");
        record("Open fresh app", "Starter library available; no imposed split or history.");
        click(root, "build-split");
        assertEquals(1, ((TabPane) root.lookup("#navigation")).getSelectionModel().getSelectedIndex());
        record("Select Build my split", "Setup opens My Split directly.");

        tab(2);
        click(root, "add-library-exercise");
        assertTrue(status().contains("cannot be blank"));
        assertEquals(10, service.getState().exercises().size());
        record("Add blank exercise", status());
        field(root, "new-exercise-name").setText("Cable fly");
        click(root, "add-library-exercise");
        assertEquals(11, service.getState().exercises().size());
        record("Add Cable fly", status());
        field(root, "new-exercise-name").setText("cable FLY");
        click(root, "add-library-exercise");
        assertTrue(status().contains("already"));
        assertEquals(11, service.getState().exercises().size());
        record("Add duplicate cable FLY", status());
        field(root, "exercise-search").setText("CABLE");
        assertEquals(List.of("Cable fly"), ((ListView<?>) root.lookup("#exercise-list")).getItems());
        field(root, "exercise-search").setText("no-such-exercise");
        assertTrue(((ListView<?>) root.lookup("#exercise-list")).getItems().isEmpty());
        assertEquals(11, service.getState().exercises().size());
        field(root, "exercise-search").clear();
        assertEquals(11, ((ListView<?>) root.lookup("#exercise-list")).getItems().size());
        record("Search CABLE, then an unknown name, then clear", "Filtering works without changing the library.");
        snapshot(root, "02-library");

        tab(1);
        addDay("Push");
        field(root, "new-day-name").setText(" ");
        click(root, "add-day");
        assertTrue(status().contains("cannot be blank"));
        assertEquals(1, service.getState().days().size());
        record("Add blank day", status());
        addDay("Pull");
        addDay("Legs");
        selectDay(0);
        modal("edit-day", pane -> {
            click(pane, "add-workout-exercise");
            ComboBox<?> selector = (ComboBox<?>) pane.lookup(".exercise-selector");
            click(pane, "save-workout");
            assertTrue(((Label) pane.lookup("#editor-error")).getText().contains("Exercise 1"));
            assertTrue(selector.getStyleClass().contains("invalid"));
            assertTrue(service.getState().days().getFirst().exercises().isEmpty());
            record("Save a workout row with no exercise chosen",
                    "Error names the row and marks the selector; plan unchanged.");
            chooseExercise(pane, "Bench press");
            assertFalse(selector.getStyleClass().contains("invalid"));
            assertFalse(((Label) pane.lookup("#editor-error")).isVisible());
            record("Choose an exercise for the rejected row", "Mark and message clear; the row can be saved.");
            setFields(pane, 0, "-1", "8-12");
            click(pane, "save-workout");
            assertTrue(((Label) pane.lookup("#editor-error")).getText().contains("Weight"));
            assertTrue(service.getState().days().getFirst().exercises().isEmpty());
            record("Save set with -1 kg", "Weight rejected; dialog and original plan retained.");
            setFields(pane, 0, "60", "12-8");
            click(pane, "save-workout");
            assertTrue(((Label) pane.lookup("#editor-error")).getText().contains("maximum"));
            record("Save reversed rep range 12-8", "Range rejected; original plan retained.");
            setFields(pane, 0, "60", "8-12");
            assertFalse(((Label) pane.lookup("#editor-error")).isVisible());
            buttonWithText(pane, "Duplicate last set").fire();
            var draft = ((WorkoutEditor) descendants(pane).stream()
                    .filter(node -> node instanceof WorkoutEditor).findFirst().orElseThrow()).readExercises();
            assertEquals(draft.getFirst().sets().getFirst(), draft.getFirst().sets().getLast());
            record("Correct invalid input and duplicate the last set", "Error clears; kg and rep range are copied.");
            setFields(pane, 1, "70", "8");
            snapshotUnchecked(pane, "03-editor");
            click(pane, "save-workout");
        });
        assertEquals(2, service.getState().days().getFirst().exercises().getFirst().sets().size());
        record("Save Bench press: 60 kg × 8-12; 70 kg × 8", status());
        planDay(1, "Barbell row", "50", "10-15");
        var pullSet = service.getState().days().get(1).exercises().getFirst().sets().getFirst();
        assertEquals(10, pullSet.minReps());
        assertEquals(15, pullSet.maxReps());
        planDay(2, "Squat", "80", "5");
        var legSet = service.getState().days().get(2).exercises().getFirst().sets().getFirst();
        assertEquals(5, legSet.minReps());
        assertEquals(5, legSet.maxReps());
        String currentId = service.getState().days().getFirst().id();
        click(root, "move-day-up");
        click(root, "move-day-down");
        assertEquals(currentId, service.getState().days().get(service.getState().currentDay()).id());
        snapshot(root, "04-split");
        record("Move Legs up, then down", "Order restored; Push remains current.");

        tab(0);
        snapshot(root, "05-current");
        root.resize(804, 601);
        snapshot(root, "05-current-minimum");
        assertEquals(804, root.getWidth());
        var completeBounds = root.lookup("#complete-planned").localToScene(
                root.lookup("#complete-planned").getBoundsInLocal());
        var skipBounds = root.lookup("#skip-workout").localToScene(root.lookup("#skip-workout").getBoundsInLocal());
        assertTrue(completeBounds.getMinY() >= 0 && completeBounds.getMaxY() < 601);
        assertTrue(skipBounds.getMinY() >= 0 && skipBounds.getMaxY() < 601);
        record("Resize to minimum content size",
                "Completion and skip controls remain visible outside the scroll area.");
        root.resize(960, 780);
        click(root, "complete-planned");
        assertEquals(1, service.getState().history().size());
        assertEquals(1, service.getState().currentDay());
        assertEquals("Pull", ((Label) root.lookup("#current-day-name")).getText());
        record("Complete Push as planned", status());
        click(root, "skip-workout");
        assertEquals(2, service.getState().currentDay());
        assertEquals(1, service.getState().history().size());
        record("Skip Pull", status());
        click(root, "complete-unrecorded");
        assertEquals(0, service.getState().currentDay());
        assertEquals(1, service.getState().history().size());
        record("Complete Legs without recording", "Cycle returns to Push; history still has one record.");

        State beforeCancel = service.getState();
        modal("complete-changed", pane -> {
            setFields(pane, 0, "999", "1");
            ((Button) pane.lookupButton(ButtonType.CANCEL)).fire();
        });
        assertEquals(beforeCancel, service.getState());
        record("Change performance draft, then Cancel", "Plan, history, and current day unchanged.");
        modal("complete-changed", pane -> {
            click(pane, "save-workout");
            assertTrue(((Label) pane.lookup("#editor-error")).getText().contains("exact"));
            assertEquals(beforeCancel, service.getState());
            record("Record a range without entering actual reps", "Exact reps required; no advancement.");
            setFields(pane, 0, "62.5", "10");
            setFields(pane, 1, "70", "7");
            buttonWithText(pane, "+ Add set").fire();
            setFields(pane, 2, "50", "12");
            snapshotUnchecked(pane, "06-recording");
            click(pane, "save-workout");
        });
        assertEquals(2, service.getState().history().size());
        assertEquals(3, service.getState().history().getFirst().exercises().getFirst().sets().size());
        assertEquals(60, service.getState().days().getFirst().exercises().getFirst().sets().getFirst().kg());
        record("Record changed weights, exact reps, and an extra set", status());

        tab(4);
        @SuppressWarnings("unchecked")
        ComboBox<Exercise> progressExercise = (ComboBox<Exercise>) root.lookup("#progress-exercise");
        progressExercise.setValue(service.getState().library().stream()
                .filter(entry -> entry.name().equals("Bench press")).findFirst().orElseThrow());
        layout(root);
        // "1 exact recorded session" is a prefix of the plural form, so both checks are needed.
        assertTrue(allText(root).contains("1 exact recorded session"));
        assertFalse(allText(root).contains("1 exact recorded sessions"));
        assertNull(root.lookup("#weight-chart"));
        record("Open Progress after a single exact recording",
                "Count reads in the singular; charts stay hidden below two sessions.");
        tab(0);

        modal("complete-changed", pane -> {
            buttonWithText(pane, "Remove exercise").fire();
            click(pane, "add-workout-exercise");
            chooseExercise(pane, "Cable fly");
            setFields(pane, 0, "10", "12");
            buttonWithText(pane, "+ Add set").fire();
            setFields(pane, 1, "12.5", "8");
            buttonWithText(pane, "Remove set").fire();
            click(pane, "save-workout");
        });
        assertEquals("Cable fly", service.getState().history().getFirst().exercises().getFirst().name());
        assertEquals(1, service.getState().history().getFirst().exercises().getFirst().sets().size());
        assertEquals("Barbell row", service.getState().days().get(1).exercises().getFirst().name());
        record("Omit planned exercise; add Cable fly; remove one set",
                "Actual replacement saved; Pull plan unchanged.");
        tab(3);
        snapshot(root, "07-history");
        assertTrue(allText(root).contains("12.5 kg"));
        record("Open History", "Three sessions available; actual sets displayed separately from the plan.");
        testPlannedHistoryCorrection(file);

        State saved = service.getState();
        stage.close();
        service = new WorkoutService(new WorkoutStore(file));
        showWindow();
        assertEquals(saved, service.getState());
        assertEquals(2, service.getState().currentDay());
        assertEquals("Legs", ((Label) root.lookup("#current-day-name")).getText());
        record("Restart app", "Library, plan, current day, and all three history records restored.");
        tab(1);
        for (int index = 4; index <= 7; index++) {
            addDay("Day " + index);
        }
        assertTrue(((Button) root.lookup("#add-day")).isDisabled());
        assertTrue(((Button) root.lookup("#duplicate-day")).isDisabled());
        assertTrue(allText(root).contains("7 / 7 days"));
        assertTrue(allText(root).contains("Arrange up to 7 workout days"));
        field(root, "new-day-name").setText("Eighth");
        click(root, "add-day");
        assertEquals(7, service.getState().days().size());
        record("Attempt eighth day", "Limit shows 7; Add and Duplicate disabled at seven; split unchanged.");
        selectDay(2);
        modal("remove-day", pane -> ((Button) pane.lookupButton(ButtonType.CANCEL)).fire());
        assertEquals(7, service.getState().days().size());
        modal("remove-day", pane -> ((Button) pane.lookupButton(ButtonType.OK)).fire());
        assertEquals("Day 4", service.getState().days().get(service.getState().currentDay()).name());
        assertEquals(3, service.getState().history().size());
        record("Cancel removal, then remove current Legs day", "Day 4 becomes current; history preserved.");

        // Advance past the empty draft days to the planned Push workout for the save-failure scenario.
        for (int index = 0; index < 4; index++) {
            service.skip();
        }

        testSaveFailure(file);
        runEnhancedJourney();
        assertPlanCoverage();
        record("Suite complete", "All GUI scenarios passed.");
    }

    /**
     * Fails if a mapped scenario never ran, which only a completed journey can show. Whether the
     * plan and the mapping list the same cases is a documentation question, checked separately by
     * UiTestPlanCoverageTest so a stale Markdown table does not look like a GUI regression.
     */
    private void assertPlanCoverage() {
        for (Map.Entry<String, String> entry : UiTestPlan.COVERAGE.entrySet()) {
            assertTrue(transcript.stream().anyMatch(line -> line.contains(entry.getValue())),
                    "Plan case " + entry.getKey() + " has no recorded scenario matching: " + entry.getValue());
        }
    }

    /**
     * Verifies that history correction derives range support from an as-planned record.
     */
    private void testPlannedHistoryCorrection(Path file) throws Exception {
        State before = service.getState();
        int index = before.history().size() - 1;
        var original = before.history().get(index);
        assertEquals(Recording.AS_PLANNED, original.recording());
        ((ListView<?>) root.lookup("#history-list")).getSelectionModel().select(index);
        modal("edit-history", pane -> {
            setFields(pane, 0, "61", "12-8");
            click(pane, "save-workout");
            assertEquals(before, service.getState());
            assertTrue(((Label) pane.lookup("#editor-error")).getText().contains("maximum"));
            setFields(pane, 0, "61", "8-12");
            click(pane, "save-workout");
        });
        var corrected = service.getState().history().get(index);
        assertEquals(original.id(), corrected.id());
        assertEquals(original.completedAt(), corrected.completedAt());
        assertEquals(original.recording(), corrected.recording());
        assertEquals(new WorkoutSet(61, 8, 12), corrected.exercises().getFirst().sets().getFirst());
        assertEquals(before.days(), service.getState().days());
        assertEquals(before.library(), service.getState().library());
        assertEquals(before.currentDay(), service.getState().currentDay());
        assertEquals(before.history().subList(0, index), service.getState().history().subList(0, index));
        assertEquals(service.getState(), new WorkoutStore(file).load());
        record("Correct as-planned history: reject 12-8, then save 61 kg x 8-12",
                "Range retained; record identity/type/date, plan, library and cycle position preserved.");
    }

    /**
     * Checks the six improvements together, using another isolated save and real desktop controls.
     */
    private void runEnhancedJourney() throws Exception {
        stage.close();
        Path file = directory.resolve("improvements.json");
        service = new WorkoutService(new WorkoutStore(file));
        service.addDay("Push");
        service.updateDay(0, "Push", List.of(new WorkoutExercise("Bench press",
                List.of(new WorkoutSet(60, 8, 12), new WorkoutSet(70, 8, 8))),
                new WorkoutExercise("Biceps curl", List.of(new WorkoutSet(10, 12, 12)))));
        showWindow();
        click(root, "start-session");
        field(root, "session-reps-0-0").setText("8-12");
        ((CheckBox) root.lookup("#session-done-0-0")).fire();
        assertFalse(service.getState().session().exercises().getFirst().sets().getFirst().completed());
        assertTrue(((Label) root.lookup("#session-error")).getText().contains("exact"));
        record("Tick a set containing a rep range", "Exact reps required; set remains incomplete.");
        field(root, "session-weight-0-0").setText("62.5");
        field(root, "session-reps-0-0").setText("10");
        ((CheckBox) root.lookup("#session-done-0-0")).fire();
        assertTrue(allText(root).contains("2 left"));
        testSessionDraftSurvivesSaveFailure(file);
        stage.close();
        service = new WorkoutService(new WorkoutStore(file));
        showWindow();
        assertTrue(((CheckBox) root.lookup("#session-done-0-0")).isSelected());
        assertEquals("62.5", field(root, "session-weight-0-0").getText());
        snapshot(root, "08-active-session");
        record("Restart during workout", "Checked set and actual values restored; two sets remain.");
        modal("finish-session", pane -> ((Button) pane.lookupButton(ButtonType.CANCEL)).fire());
        assertTrue(service.getState().history().isEmpty());
        modal("finish-session", pane -> ((Button) pane.lookupButton(ButtonType.OK)).fire());
        assertEquals(1, service.getState().history().getFirst().exercises().size());
        assertTrue(allText(root).contains("62.5 kg × 10 reps"));
        record("Cancel finish, then finish partial session", "Only checked set saved; previous performance appears.");

        tab(1);
        modal("duplicate-day", pane -> {
            field(pane, "name-input").setText(" ");
            ((Button) pane.lookupButton(ButtonType.OK)).fire();
            assertEquals(1, service.getState().days().size());
            field(pane, "name-input").setText("Push B");
            ((Button) pane.lookupButton(ButtonType.OK)).fire();
        });
        assertEquals(2, service.getState().days().size());
        selectDay(1);
        modal("edit-day", pane -> {
            buttonWithText(pane, "Move down").fire();
            click(pane, "save-workout");
        });
        assertEquals("Biceps curl", service.getState().days().get(1).exercises().getFirst().name());
        List<WorkoutDay> daysBeforeCopy = service.getState().days();
        modal("copy-sets", pane -> {
            ((ComboBox<?>) pane.lookup("#copy-source-day")).getSelectionModel().select(0);
            ((ComboBox<?>) pane.lookup("#copy-source-exercise")).getSelectionModel().select(0);
            ((ComboBox<?>) pane.lookup("#copy-target-day")).getSelectionModel().select(1);
            ((ComboBox<?>) pane.lookup("#copy-target-exercise")).getSelectionModel().select(0);
            // Reproduces the state a day with no exercises produces: nothing selected to copy from.
            ((ComboBox<?>) pane.lookup("#copy-source-exercise")).getSelectionModel().clearSelection();
            ((Button) pane.lookupButton(ButtonType.OK)).fire();
            Label copyError = (Label) pane.lookup("#copy-error");
            assertTrue(copyError.getText().contains("Select source and target exercises."));
            assertTrue(copyError.isVisible());
            assertEquals(daysBeforeCopy, service.getState().days());
            record("Confirm a copy with no source exercise selected",
                    "Copy rejected with a visible message; the split is unchanged.");
            ((ComboBox<?>) pane.lookup("#copy-source-exercise")).getSelectionModel().select(0);
            assertTrue(copyError.getText().isEmpty());
            assertFalse(copyError.isVisible());
            record("Reselect a source exercise", "Stale message clears as soon as the selection changes.");
            ((Button) pane.lookupButton(ButtonType.OK)).fire();
        });
        assertEquals(2, service.getState().days().get(1).exercises().getFirst().sets().size());
        snapshot(root, "09-routine-tools");
        record("Duplicate day, reorder exercises, copy sets across days", "Copies saved; original order preserved.");

        tab(2);
        var libraryBeforeArchive = service.getState().library();
        var planBeforeArchive = service.getState().days();
        var historyBeforeArchive = service.getState().history();
        assertNull(root.lookup("#rename-exercise"));
        assertTrue(allText(root).contains("Exercise names are fixed"));
        ((ListView<?>) root.lookup("#exercise-list")).getSelectionModel().select(0);
        click(root, "archive-exercise");
        assertFalse(((ListView<?>) root.lookup("#exercise-list")).getItems().contains("Bench press"));
        assertEquals(planBeforeArchive, service.getState().days());
        assertEquals(historyBeforeArchive, service.getState().history());
        ((CheckBox) root.lookup("#show-archived")).fire();
        ((ListView<?>) root.lookup("#exercise-list")).getSelectionModel().select(0);
        assertTrue(allText(root).contains("Archived"));
        click(root, "archive-exercise");
        assertEquals(libraryBeforeArchive, service.getState().library());
        snapshot(root, "12-immutable-library");
        record("Check fixed names, archive, show archived, restore",
                "No Rename action; library identities and names, plans, and history retained.");

        tab(3);
        field(root, "history-day-filter").setText("missing");
        assertTrue(((ListView<?>) root.lookup("#history-list")).getItems().isEmpty());
        field(root, "history-day-filter").setText("push");
        ((ComboBox<?>) root.lookup("#history-exercise-filter")).getSelectionModel().select(1);
        assertEquals(1, ((ListView<?>) root.lookup("#history-list")).getItems().size());
        String timestamp = service.getState().history().getFirst().completedAt();
        modal("edit-history", pane -> {
            setFields(pane, 0, "65", "0");
            click(pane, "save-workout");
            assertEquals(62.5, service.getState().history().getFirst().exercises().getFirst().sets().getFirst().kg());
            State beforeRange = service.getState();
            setFields(pane, 0, "65", "8-12");
            click(pane, "save-workout");
            assertEquals(beforeRange, service.getState());
            assertTrue(((Label) pane.lookup("#editor-error")).getText().contains("exact"));
            setFields(pane, 0, "65", "11");
            click(pane, "save-workout");
        });
        assertEquals(timestamp, service.getState().history().getFirst().completedAt());
        assertEquals(0, service.getState().currentDay());
        snapshot(root, "10-history-correction");
        record("Filter history and correct an actual record after rejecting zero reps and a rep range",
                "Correction saved with original timestamp and exercise ID; split position unchanged.");

        tab(0);
        assertTrue(allText(root).contains("65.0 kg × 11 reps"));
        click(root, "start-session");
        field(root, "session-weight-0-0").setText("67.5");
        field(root, "session-reps-0-0").setText("9");
        ((CheckBox) root.lookup("#session-done-0-0")).fire();
        modal("finish-session", pane -> ((Button) pane.lookupButton(ButtonType.OK)).fire());
        tab(4);
        assertNotNull(root.lookup("#weight-chart"));
        assertNotNull(root.lookup("#reps-chart"));
        assertTrue(allText(root).contains("2 exact recorded sessions"));
        snapshot(root, "11-progress");
        record("Record second performance and open Progress", "Both charts appear; correction included in history.");

        State beforeReplacement = service.getState();
        var previousRecord = beforeReplacement.history().getFirst();
        String benchId = previousRecord.exercises().getFirst().exerciseId();
        String squatId = beforeReplacement.library().stream().filter(entry -> entry.name().equals("Squat"))
                .findFirst().orElseThrow().id();
        tab(3);
        modal("edit-history", pane -> {
            chooseExercise(pane, "Squat");
            setFields(pane, 0, "80", "0");
            click(pane, "save-workout");
            assertEquals(beforeReplacement, service.getState());
            setFields(pane, 0, "80", "8");
            click(pane, "save-workout");
        });
        var corrected = service.getState().history().getFirst();
        assertEquals(previousRecord.id(), corrected.id());
        assertEquals(previousRecord.completedAt(), corrected.completedAt());
        assertEquals(squatId, corrected.exercises().getFirst().exerciseId());
        assertEquals(beforeReplacement.library(), service.getState().library());
        assertEquals(beforeReplacement.days(), service.getState().days());
        assertEquals(beforeReplacement.currentDay(), service.getState().currentDay());
        assertEquals(1, service.performance(benchId).size());
        assertEquals(List.of(corrected), service.performance(squatId));
        snapshot(root, "13-history-replacement");
        record("Replace historical Bench press with Squat; reject zero reps, then save 80 kg x 8",
                "Record identity/date retained; performance reassigned; library, plan and position unchanged.");

        var historyAfterCorrection = service.getState().history();
        tab(1);
        selectDay(1);
        modal("edit-day", pane -> {
            chooseExercise(pane, "Squat");
            click(pane, "save-workout");
        });
        assertEquals(squatId, service.getState().days().get(1).exercises().getFirst().exerciseId());
        assertEquals(beforeReplacement.library(), service.getState().library());
        assertEquals(historyAfterCorrection, service.getState().history());
        assertEquals(beforeReplacement.currentDay(), service.getState().currentDay());
        assertEquals(service.getState(), new WorkoutStore(file).load());
        record("Replace a planned exercise with Squat",
                "Selected workout updated; library, history and cycle position retained after reload.");
    }

    /**
     * A failed session write must report the problem without erasing what the user is still typing,
     * and the retained entry must save once storage recovers, without the user retyping it.
     */
    private void testSessionDraftSurvivesSaveFailure(Path file) throws Exception {
        AtomicBoolean fail = new AtomicBoolean(true);
        WorkoutStore store = new WorkoutStore(file) {
            @Override
            public void save(State state) throws StorageException {
                if (fail.get()) {
                    throw new StorageException("Simulated disk failure. No change was applied.");
                }
                super.save(state);
            }
        };
        stage.close();
        service = new WorkoutService(store);
        showWindow();
        State before = service.getState();
        field(root, "session-weight-1-0").setText("12.5");
        assertEquals("12.5", field(root, "session-weight-1-0").getText());
        assertTrue(allText(root).contains("disk failure"));
        assertEquals(before, service.getState());
        assertEquals(before, new WorkoutStore(file).load());
        record("Type a session entry while the save fails", "Typed weight retained; nothing saved.");
        fail.set(false);
        // This set is prescribed 12 reps, so its field already holds "12"; typing a different count
        // is what fires the next save and carries the retained weight through with it.
        field(root, "session-reps-1-0").setText("11");
        SessionSet saved = service.getState().session().exercises().get(1).sets().getFirst();
        assertEquals("12.5", saved.weight());
        assertEquals("11", saved.reps());
        assertEquals(service.getState(), new WorkoutStore(file).load());
        record("Continue typing after storage recovers", "Retained weight saved without retyping it.");
        stage.close();
        service = new WorkoutService(new WorkoutStore(file));
        showWindow();
    }

    private void testSaveFailure(Path file) throws Exception {
        AtomicBoolean fail = new AtomicBoolean(true);
        WorkoutStore store = new WorkoutStore(file) {
            @Override
            public void save(State state) throws StorageException {
                if (fail.get()) {
                    throw new StorageException("Simulated disk failure. No change was applied.");
                }
                super.save(state);
            }
        };
        stage.close();
        service = new WorkoutService(store);
        showWindow();
        State before = service.getState();
        click(root, "complete-planned");
        assertTrue(status().contains("disk failure"));
        assertEquals(before, service.getState());
        assertEquals(before, new WorkoutStore(file).load());
        record("Complete workout while save fails", status());
        fail.set(false);
        click(root, "complete-planned");
        assertEquals(before.history().size() + 1, service.getState().history().size());
        assertEquals(service.getState(), new WorkoutStore(file).load());
        record("Retry after storage recovers", "Exactly one record saved and cycle advanced once.");
    }

    private void showWindow() {
        stage = new Stage();
        root = new MainWindow(service);
        Scene scene = new Scene(root, 960, 780);
        scene.getStylesheets().add(Main.class.getResource("/view/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
        layout(root);
    }

    private void addDay(String name) {
        field(root, "new-day-name").setText(name);
        click(root, "add-day");
        assertTrue(status().contains("added"));
        record("Add day " + name, status());
    }

    private void planDay(int index, String exercise, String weight, String reps) {
        selectDay(index);
        modal("edit-day", pane -> {
            click(pane, "add-workout-exercise");
            chooseExercise(pane, exercise);
            setFields(pane, 0, weight, reps);
            click(pane, "save-workout");
        });
        assertFalse(service.getState().days().get(index).exercises().isEmpty());
        record("Plan " + exercise + " at " + weight + " kg × " + reps, status());
    }

    private void selectDay(int index) {
        ((ListView<?>) root.lookup("#day-list")).getSelectionModel().select(index);
        layout(root);
    }

    private void tab(int index) {
        ((TabPane) root.lookup("#navigation")).getSelectionModel().select(index);
        layout(root);
    }

    private String status() {
        return ((Label) root.lookup("#status")).getText();
    }

    /**
     * Schedules dialog interaction inside JavaFX's nested event loop, propagating assertion failures.
     */
    private void modal(String opener, Consumer<DialogPane> interaction) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            DialogPane pane = null;
            try {
                pane = Window.getWindows().stream().filter(window -> window != stage && window.isShowing())
                        .map(window -> (DialogPane) window.getScene().lookup(".dialog-pane"))
                        .findFirst().orElseThrow();
                layout(pane);
                interaction.accept(pane);
            } catch (Throwable exception) {
                failure.set(exception);
                if (pane != null) {
                    ((Button) pane.lookupButton(ButtonType.CANCEL)).fire();
                }
            }
        });
        click(root, opener);
        if (failure.get() != null) {
            throw new AssertionError("Dialog interaction failed", failure.get());
        }
        layout(root);
    }

    private static void click(Parent parent, String id) {
        layout(parent);
        Button button = (Button) parent.lookup("#" + id);
        assertNotNull(button, id);
        button.fire();
        layout(parent);
    }

    private static TextField field(Parent parent, String id) {
        return (TextField) parent.lookup("#" + id);
    }

    @SuppressWarnings("unchecked")
    private static void chooseExercise(Parent parent, String name) {
        ComboBox<String> combo = (ComboBox<String>) parent.lookup(".exercise-selector");
        combo.setValue(name);
    }

    private static void setFields(Parent parent, int index, String weight, String reps) {
        layout(parent);
        List<Node> nodes = descendants(parent);
        List<TextField> weights = nodes.stream().filter(node -> node.getStyleClass().contains("set-weight"))
                .map(node -> (TextField) node).toList();
        List<TextField> counts = nodes.stream().filter(node -> node.getStyleClass().contains("set-reps"))
                .map(node -> (TextField) node).toList();
        weights.get(index).setText(weight);
        counts.get(index).setText(reps);
    }

    private static Button buttonWithText(Parent parent, String text) {
        return descendants(parent).stream()
                .filter(node -> node instanceof Button button && button.getText().equals(text))
                .map(node -> (Button) node).findFirst().orElseThrow();
    }

    private static List<Node> descendants(Parent parent) {
        List<Node> nodes = new ArrayList<>();
        for (Node child : parent.getChildrenUnmodifiable()) {
            nodes.add(child);
            if (child instanceof Parent nested) {
                nodes.addAll(descendants(nested));
            }
        }
        return nodes;
    }

    private static String allText(Parent parent) {
        return descendants(parent).stream().filter(node -> node instanceof Label)
                .map(node -> ((Label) node).getText()).reduce("", (left, right) -> left + "\n" + right);
    }

    private static void layout(Parent parent) {
        parent.applyCss();
        parent.layout();
    }

    private void record(String input, String output) {
        String entry = "INPUT: " + input + "\nOUTPUT: " + output + "\nPASS";
        transcript.add(entry);
        lastScenario = input;
        System.out.println(entry);
    }

    private static Path artifacts() throws Exception {
        return Files.createDirectories(Path.of(System.getProperty("staniz.gui.artifacts", "build/gui-test")));
    }

    private static void snapshotUnchecked(Parent parent, String name) {
        try {
            snapshot(parent, name);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static void snapshot(Parent parent, String name) throws Exception {
        layout(parent);
        if (parent instanceof DialogPane pane) {
            Button save = (Button) pane.lookupButton(ButtonType.OK);
            assertTrue(save.getWidth() >= 160, "Save button must display its full label.");
        }
        WritableImage image = parent.snapshot(null, null);
        BufferedImage pixels = new BufferedImage((int) image.getWidth(), (int) image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < pixels.getHeight(); y++) {
            for (int x = 0; x < pixels.getWidth(); x++) {
                pixels.setRGB(x, y, image.getPixelReader().getArgb(x, y));
            }
        }
        ImageIO.write(pixels, "png", artifacts().resolve(name + ".png").toFile());
    }
}
