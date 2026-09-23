package staniz.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import staniz.model.WorkoutData.Recording;
import staniz.model.WorkoutData.State;
import staniz.model.WorkoutData.WorkoutExercise;
import staniz.model.WorkoutData.WorkoutSet;
import staniz.storage.StorageException;
import staniz.storage.WorkoutStore;

/**
 * Verifies complete workout workflows and rejected operations against isolated real files.
 */
class WorkoutServiceTest {
    // The service's rejection messages are shown to the user verbatim by the GUI, so they are
    // asserted exactly. Sharing the repeated ones keeps a wording change to a single edit here.
    private static final String NO_DAY = "Select a workout day first.";
    private static final String NOT_IN_LIBRARY = "Choose an active exercise or restore it first.";

    @TempDir
    private Path directory;
    private Path file;
    private WorkoutService service;

    @BeforeEach
    void setUp() throws Exception {
        file = directory.resolve("data/workouts.json");
        service = new WorkoutService(new WorkoutStore(file),
                Clock.fixed(Instant.parse("2026-09-20T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void newUser_hasStarterLibraryAndNoPrescribedRoutine() {
        assertEquals(2, service.getState().version());
        assertEquals(10, service.getState().library().size());
        assertTrue(service.getState().library().stream().noneMatch(exercise -> exercise.archived()));
        assertTrue(service.getState().exercises().contains("Bench press"));
        assertTrue(service.getState().days().isEmpty());
        assertTrue(service.getState().history().isEmpty());
        assertTrue(Files.notExists(file));
        assertEquals(NO_DAY, rejected(() -> service.skip()));
        assertEquals(NO_DAY, rejected(() -> service.completeAsPlanned()));
    }

    @Test
    void library_rejectsBlankAndDuplicateNamesWithoutChangingSavedState() throws Exception {
        service.addExercise("  Cable fly  ");
        State before = service.getState();
        String disk = Files.readString(file);
        assertEquals("That exercise is already in your library.", rejected(() -> service.addExercise("cable FLY")));
        assertEquals("Exercise name cannot be blank.", rejected(() -> service.addExercise("  ")));
        assertEquals(before, service.getState());
        assertEquals(disk, Files.readString(file));
        assertEquals(before, new WorkoutStore(file).load());
    }

    @Test
    void split_supportsSevenDaysAndRejectsAnEighthWithoutMutation() throws Exception {
        for (int index = 1; index <= 7; index++) {
            service.addDay("Workout " + index);
        }
        State before = service.getState();
        assertEquals("A split can contain at most 7 workout days.", rejected(() -> service.addDay("Eighth")));
        assertEquals(before, service.getState());
        assertEquals(before, new WorkoutStore(file).load());
        service.removeDay(6);
        service.addDay("Replacement");
        assertEquals(7, service.getState().days().size());
    }

    @Test
    void plannedCompletion_skipAndUnrecordedCompletionCycleAndPersist() throws Exception {
        addRoutine();
        service.completeAsPlanned();
        assertEquals(1, service.getState().currentDay());
        assertEquals("Push", service.getState().history().getFirst().dayName());
        assertEquals(Recording.AS_PLANNED, service.getState().history().getFirst().recording());
        assertEquals("2026-09-20T10:00:00Z", service.getState().history().getFirst().completedAt());
        assertEquals(12, service.getState().history().getFirst().exercises().getFirst().sets().getFirst().maxReps());
        service.skip();
        assertEquals(2, service.getState().currentDay());
        service.completeWithoutRecording();
        assertEquals(0, service.getState().currentDay());
        assertEquals(1, service.getState().history().size());
        assertEquals(service.getState(), new WorkoutStore(file).load());
    }

    @Test
    void actualWorkout_recordsChangedExtraAndOmittedWorkWithoutChangingPlan() throws Exception {
        addRoutine();
        var plan = service.getState().days();
        List<WorkoutExercise> actual = List.of(new WorkoutExercise("Bench press",
                List.of(new WorkoutSet(65, 9, 9), new WorkoutSet(70, 6, 6), new WorkoutSet(50, 12, 12))),
                new WorkoutExercise("Biceps curl", List.of(new WorkoutSet(10, 10, 10))));
        service.completeWithChanges(actual);
        assertEquals(Recording.WITH_CHANGES, service.getState().history().getFirst().recording());
        assertEquals(actual.stream().map(WorkoutExercise::sets).toList(),
                service.getState().history().getFirst().exercises().stream().map(WorkoutExercise::sets).toList());
        assertEquals(actual.stream().map(WorkoutExercise::name).toList(),
                service.getState().history().getFirst().exercises().stream().map(WorkoutExercise::name).toList());
        assertEquals(plan, service.getState().days());
        assertEquals(service.getState(), new WorkoutStore(file).load());
    }

    @Test
    void actualWorkout_rejectsRangesAndUnknownExercisesBeforeAdvancing() throws Exception {
        addRoutine();
        State before = service.getState();
        assertEquals("Recorded reps must be exact counts.",
                rejected(() -> service.completeWithChanges(planned())));
        assertEquals(NOT_IN_LIBRARY, rejected(() -> service.completeWithChanges(
                List.of(new WorkoutExercise("Unknown", List.of(new WorkoutSet(1, 1, 1)))))));
        assertEquals(before, service.getState());
        assertEquals(before, new WorkoutStore(file).load());
        service.completeWithChanges(List.of());
        assertTrue(service.getState().history().getFirst().exercises().isEmpty());
        assertEquals(1, service.getState().currentDay());
    }

    @Test
    void editingAndDeletingPlan_preservesHistorySnapshot() throws Exception {
        addRoutine();
        service.completeAsPlanned();
        var record = service.getState().history().getFirst();
        service.updateDay(0, "New push", List.of(new WorkoutExercise("Bench press",
                List.of(new WorkoutSet(100, 1, 1)))));
        service.removeDay(0);
        assertEquals(record, service.getState().history().getFirst());
        assertEquals("Push", record.dayName());
        assertEquals(60, record.exercises().getFirst().sets().getFirst().kg());
    }

    @Test
    void reorderingDays_keepsCurrentDayAndDeletingItSelectsItsSuccessor() throws Exception {
        addRoutine();
        service.skip();
        String currentId = service.getState().days().get(1).id();
        service.moveDay(1, 1);
        assertEquals(2, service.getState().currentDay());
        assertEquals(currentId, service.getState().days().get(2).id());
        service.removeDay(0);
        assertEquals(1, service.getState().currentDay());
        service.removeDay(1);
        assertEquals(0, service.getState().currentDay());
        assertEquals("Legs", service.getState().days().getFirst().name());
        service.removeDay(0);
        assertTrue(service.getState().days().isEmpty());
        service.addDay("Fresh start");
        assertEquals(0, service.getState().currentDay());
    }

    @Test
    void removingCurrentMiddleDay_selectsTheFollowingDay() throws Exception {
        addRoutine();
        service.skip();
        service.removeDay(1);
        assertEquals("Legs", service.getState().days().get(service.getState().currentDay()).name());
    }

    @Test
    void invalidDayEditsAndMoves_preserveCurrentState() throws Exception {
        addRoutine();
        State before = service.getState();
        assertEquals("Workout day name cannot be blank.", rejected(() -> service.updateDay(0, " ", planned())));
        assertEquals(NOT_IN_LIBRARY, rejected(() -> service.updateDay(0, "Push",
                List.of(new WorkoutExercise("Not in library", List.of(new WorkoutSet(1, 1, 1)))))));
        assertEquals(NO_DAY, rejected(() -> service.removeDay(-1)));
        // Moving up from the first day is a valid direction onto an invalid position, so it is
        // rejected by the position rule; only a direction other than one step hits the other rule.
        assertEquals(NO_DAY, rejected(() -> service.moveDay(0, -1)));
        assertEquals("Move a day up or down one position.", rejected(() -> service.moveDay(0, 2)));
        assertEquals(before, service.getState());
        assertEquals(before, new WorkoutStore(file).load());
    }

    @Test
    void emptyWorkout_cannotCompleteButCanBeSkipped() throws Exception {
        service.addDay("Draft");
        // All three completion routes share one guard, which asserting the message makes visible.
        String empty = "Add exercises to this workout before completing it.";
        assertEquals(empty, rejected(() -> service.completeAsPlanned()));
        assertEquals(empty, rejected(() -> service.completeWithoutRecording()));
        assertEquals(empty, rejected(() -> service.completeWithChanges(List.of())));
        service.skip();
        assertEquals(0, service.getState().currentDay());
        assertTrue(service.getState().history().isEmpty());
    }

    @Test
    void oneDaySplit_returnsToItselfAndKeepsEveryRecordedSession() throws Exception {
        service.addDay("Full body");
        service.updateDay(0, "Full body", planned());
        service.completeAsPlanned();
        service.completeAsPlanned();
        assertEquals(0, service.getState().currentDay());
        assertEquals(2, service.getState().history().size());
    }

    @Test
    void saveFailure_keepsMemoryDiskAndCycleUnchanged() throws Exception {
        addRoutine();
        WorkoutStore failing = new WorkoutStore(file) {
            @Override
            public void save(State state) throws StorageException {
                throw new StorageException("Simulated disk failure");
            }
        };
        WorkoutService failingService = new WorkoutService(failing);
        State before = failingService.getState();
        // Asserting the message confirms the service propagates the store's explanation rather
        // than replacing it, which is what the GUI shows the user.
        assertEquals("Simulated disk failure",
                assertThrows(StorageException.class, () -> failingService.completeAsPlanned()).getMessage());
        assertEquals("Simulated disk failure",
                assertThrows(StorageException.class, () -> failingService.skip()).getMessage());
        assertEquals("Simulated disk failure",
                assertThrows(StorageException.class, () -> failingService.addExercise("Cable fly")).getMessage());
        assertEquals(before, failingService.getState());
        assertEquals(before, new WorkoutStore(file).load());
    }

    @Test
    void callerCollections_cannotChangePlanOrHistory() throws Exception {
        service.addDay("Push");
        List<WorkoutSet> sets = new ArrayList<>(List.of(new WorkoutSet(0, 8, 8)));
        List<WorkoutExercise> exercises = new ArrayList<>(List.of(new WorkoutExercise("Pull-up", sets)));
        service.updateDay(0, "Push", exercises);
        sets.clear();
        exercises.clear();
        service.completeAsPlanned();
        assertFalse(service.getState().days().getFirst().exercises().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> service.getState().days().clear());
        assertEquals(1, service.getState().history().getFirst().exercises().getFirst().sets().size());
    }

    private void addRoutine() throws StorageException {
        for (String name : List.of("Push", "Pull", "Legs")) {
            service.addDay(name);
            service.updateDay(service.getState().days().size() - 1, name, planned());
        }
    }

    private List<WorkoutExercise> planned() {
        return List.of(new WorkoutExercise("Bench press",
                List.of(new WorkoutSet(60, 8, 12), new WorkoutSet(70, 8, 8))));
    }

    /**
     * Returns the message of an expected rejection, so each check names the rule it exercises
     * instead of accepting any IllegalArgumentException the call happens to raise.
     */
    private static String rejected(Executable call) {
        return assertThrows(IllegalArgumentException.class, call).getMessage();
    }
}
