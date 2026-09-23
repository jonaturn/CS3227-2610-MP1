package staniz.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import staniz.model.WorkoutData.State;
import staniz.model.WorkoutData.WorkoutExercise;
import staniz.model.WorkoutData.WorkoutSet;
import staniz.storage.StorageException;
import staniz.storage.WorkoutStore;

/**
 * Exercises persistence, identity, and rejection boundaries across the connected workout features.
 */
class WorkoutImprovementsTest {
    // Shared because one rule guards many operations: asserting the same constant everywhere makes
    // it visible that an active session is what blocks them, rather than six unrelated failures.
    private static final String ACTIVE_SESSION = "Finish or discard your active session first.";
    private static final String EXACT_REPS = "Enter an exact positive rep count before completing a set.";
    private static final String MISMATCHED_NAME = "The exercise name does not match its library ID.";

    @TempDir
    private Path directory;
    private Path file;
    private WorkoutService service;

    @BeforeEach
    void setup() throws Exception {
        file = directory.resolve("workouts.json");
        service = new WorkoutService(new WorkoutStore(file));
        service.addDay("Push");
        service.updateDay(0, "Push", List.of(new WorkoutExercise("Bench press",
                List.of(new WorkoutSet(60, 8, 12), new WorkoutSet(70, 8, 8)))));
        service.addDay("Pull");
        service.updateDay(1, "Pull", List.of(new WorkoutExercise("Barbell row", List.of(new WorkoutSet(40, 10, 10)))));
    }

    @Test
    void session_restoresDraftsRejectsInvalidCompletionAndRecordsOnlyCheckedSets() throws Exception {
        var plan = service.getState().days();
        service.startSession();
        service.updateSessionSet(0, 0, "62.", "8-12", false);
        service = new WorkoutService(new WorkoutStore(file));
        assertEquals("62.", service.getState().session().exercises().getFirst().sets().getFirst().weight());
        State draft = service.getState();
        assertEquals(EXACT_REPS, rejected(() -> service.updateSessionSet(0, 0, "62", "8-12", true)));
        assertEquals("Complete at least one set, or discard the session.", rejected(service::finishSession));
        assertEquals(ACTIVE_SESSION, rejected(service::skip));
        assertEquals(ACTIVE_SESSION, rejected(() -> service.removeDay(0)));
        assertEquals(ACTIVE_SESSION, rejected(service::startSession));
        assertEquals(draft, service.getState());
        service.updateSessionSet(0, 0, "62.5", "11", true);
        State checked = service.getState();
        String saved = Files.readString(file);
        assertEquals(ACTIVE_SESSION, rejected(service::completeAsPlanned));
        assertEquals(ACTIVE_SESSION, rejected(service::completeWithoutRecording));
        assertEquals(ACTIVE_SESSION, rejected(() -> service.completeWithChanges(List.of())));
        assertEquals(ACTIVE_SESSION, rejected(service::skip));
        assertEquals(checked, service.getState());
        assertEquals(saved, Files.readString(file));
        service.finishSession();
        assertNull(service.getState().session());
        assertEquals(1, service.getState().currentDay());
        assertEquals(List.of(new WorkoutSet(62.5, 11, 11)),
                service.getState().history().getFirst().exercises().getFirst().sets());
        assertEquals(plan, service.getState().days());
        assertEquals(1, service.getState().history().size());
        assertEquals(service.getState(), new WorkoutStore(file).load());
        service.startSession();
        service.discardSession();
        assertEquals(1, service.getState().currentDay());
        assertEquals(1, service.getState().history().size());
    }

    @Test
    void archiveAndRestore_preserveNamesSnapshotsAndPreviousPerformance() throws Exception {
        String id = benchId();
        service.completeWithChanges(List.of(new WorkoutExercise("Bench press", List.of(new WorkoutSet(65, 9, 9)))));
        var record = service.getState().history().getFirst();
        var plans = service.getState().days();
        var library = service.getState().library();
        assertEquals("Bench press", record.exercises().getFirst().name());
        assertEquals(record, service.previousPerformance(id).orElseThrow());
        service.archiveExercise(id, true);
        assertFalse(service.choices(List.of()).contains("Bench press"));
        assertTrue(service.choices(service.getState().days().getFirst().exercises()).contains("Bench press"));
        State before = service.getState();
        // Archiving hides the name from new choices but must not free it for reuse, and must not
        // allow the archived exercise into a workout that did not already contain it.
        assertEquals("That exercise is already in your library.",
                rejected(() -> service.addExercise("bench PRESS")));
        assertEquals("Choose an active exercise or restore it first.",
                rejected(() -> service.updateDay(1, "Pull",
                        List.of(new WorkoutExercise("Bench press", List.of(new WorkoutSet(1, 1, 1)))))));
        assertEquals(before, service.getState());
        service.correctRecord(record.id(), "Push corrected", record.exercises());
        assertEquals(id, service.filterHistory("corrected", id).getFirst().exercises().getFirst().exerciseId());
        service.archiveExercise(id, false);
        assertTrue(service.choices(List.of()).contains("Bench press"));
        assertEquals(library, service.getState().library());
        assertEquals(plans, service.getState().days());
        assertEquals(service.getState(), new WorkoutStore(file).load());
    }

    @Test
    void routineCopies_haveIndependentDaysAndRetainDestinationIdentity() throws Exception {
        service.duplicateDay(0, "Push B");
        assertNotEquals(service.getState().days().get(0).id(), service.getState().days().get(2).id());
        assertEquals(0, service.getState().currentDay());
        var target = service.getState().days().get(1).exercises().getFirst();
        service.copySets(0, 0, 1, 0);
        assertEquals(target.exerciseId(), service.getState().days().get(1).exercises().getFirst().exerciseId());
        assertEquals(2, service.getState().days().get(1).exercises().getFirst().sets().size());
        service.updateDay(0, "Changed", List.of(new WorkoutExercise("Bench press", List.of(new WorkoutSet(1, 1, 1)))));
        assertEquals(60, service.getState().days().get(2).exercises().getFirst().sets().getFirst().kg());
        for (int index = 3; index < 7; index++) {
            service.duplicateDay(0, "Copy " + index);
        }
        State before = service.getState();
        assertEquals("A split can contain at most 7 workout days.",
                rejected(() -> service.duplicateDay(0, "Eighth")));
        assertEquals("Select source and target exercises.", rejected(() -> service.copySets(0, -1, 1, 0)));
        assertEquals(before, service.getState());
    }

    @Test
    void corrections_refreshPerformanceButNeverAdvanceOrAlterPlans() throws Exception {
        String id = benchId();
        service.completeAsPlanned();
        service.skip();
        assertTrue(service.performance(id).isEmpty());
        service.completeWithChanges(List.of(new WorkoutExercise("Bench press", List.of(new WorkoutSet(60, 8, 8)))));
        var record = service.getState().history().getFirst();
        State before = service.getState();
        assertEquals("Recorded reps must be exact counts.",
                rejected(() -> service.correctRecord(record.id(), "Push",
                        List.of(new WorkoutExercise("Bench press", List.of(new WorkoutSet(60, 8, 12)))))));
        assertEquals(before, service.getState());
        service.correctRecord(record.id(), "Push fixed",
                List.of(new WorkoutExercise("Bench press", List.of(new WorkoutSet(72.5, 10, 10)))));
        var corrected = service.previousPerformance(id).orElseThrow();
        assertEquals(record.id(), corrected.id());
        assertEquals(record.completedAt(), corrected.completedAt());
        assertEquals(72.5, corrected.exercises().getFirst().sets().getFirst().kg());
        assertEquals(before.days(), service.getState().days());
        assertEquals(before.currentDay(), service.getState().currentDay());
        assertEquals(1, service.filterHistory("FIXED", id).size());
        assertTrue(service.filterHistory("missing", id).isEmpty());
        assertEquals(2, service.filterHistory("", "").size());
    }

    @Test
    void failedSessionSave_keepsDraftAndDoesNotAdvanceOrDuplicateHistory() throws Exception {
        service.startSession();
        service.updateSessionSet(0, 0, "60", "10", true);
        AtomicBoolean fail = new AtomicBoolean(true);
        WorkoutService failing = new WorkoutService(new WorkoutStore(file) {
            @Override
            public void save(State state) throws StorageException {
                if (fail.get()) {
                    throw new StorageException("Disk failure");
                }
                super.save(state);
            }
        });
        State before = failing.getState();
        assertThrows(StorageException.class, failing::finishSession);
        assertThrows(StorageException.class, failing::discardSession);
        assertThrows(StorageException.class, () -> failing.updateSessionSet(0, 1, "70", "7", true));
        assertEquals(before, failing.getState());
        assertEquals(before, new WorkoutStore(file).load());
        fail.set(false);
        failing.finishSession();
        assertNull(failing.getState().session());
        assertEquals(1, failing.getState().currentDay());
        assertEquals(1, failing.getState().history().size());
        State completed = failing.getState();
        assertEquals("Start a workout session first.", rejected(failing::finishSession));
        assertEquals(completed, failing.getState());
        assertEquals(completed, new WorkoutStore(file).load());
    }

    @Test
    void versionOneMigration_preservesOldBytesAndLinksHistoryToStableIds() throws Exception {
        String old = """
                {"version":1,"exercises":["Bench press"],"days":[
                  {"id":"push","name":"Push","exercises":[{"name":"Bench press",
                    "sets":[{"kg":60,"minReps":8,"maxReps":12}]}]}],"currentDay":0,
                 "history":[{"completedAt":"2026-09-20T10:00:00Z","dayName":"Old Push",
                    "recording":"WITH_CHANGES","exercises":[{"name":"Bench press",
                    "sets":[{"kg":62.5,"minReps":10,"maxReps":10}]}]}]}
                """;
        Files.writeString(file, old);
        service = new WorkoutService(new WorkoutStore(file));
        assertEquals(old, Files.readString(file));
        assertEquals(2, service.getState().version());
        String id = benchId();
        assertEquals(id, service.getState().history().getFirst().exercises().getFirst().exerciseId());
        assertEquals(service.getState(), new WorkoutStore(file).load());
        service.archiveExercise(id, true);
        assertEquals(old, Files.readString(file.resolveSibling("workouts.json.v1.bak")));
        assertEquals(service.getState(), new WorkoutStore(file).load());
        assertEquals("Bench press", service.previousPerformance(id).orElseThrow().exercises().getFirst().name());
    }

    @Test
    void plannedIdentity_rejectsMismatchedNamesWithoutChangingMemoryOrDisk() throws Exception {
        State before = service.getState();
        String saved = Files.readString(file);
        String rowId = before.days().get(1).exercises().getFirst().exerciseId();
        var sets = List.of(new WorkoutSet(60, 8, 8));
        assertEquals("The exercise name does not match its library ID.",
                rejected(() -> service.updateDay(0, "Push",
                        List.of(new WorkoutExercise(rowId, "Bench press", sets)))));
        assertEquals(before, service.getState());
        assertEquals(saved, Files.readString(file));

        service.updateDay(0, "Push", List.of(new WorkoutExercise(benchId(), "Bench press", sets)));
        assertEquals(sets, service.getState().days().getFirst().exercises().getFirst().sets());
        assertEquals(service.getState(), new WorkoutStore(file).load());
    }

    @Test
    void olderRenamedSnapshots_remainEditableAndResumableWithoutRenamingTheLibrary() throws Exception {
        String olderSave = """
                {"version":2,"library":[{"id":"bench-id","name":"Flat bench press","archived":false}],
                 "days":[{"id":"push","name":"Push","exercises":[{"exerciseId":"bench-id",
                   "name":"Flat bench press","sets":[{"kg":60,"minReps":8,"maxReps":12}]}]}],
                 "currentDay":0,"history":[{"id":"old-record","dayId":"push",
                   "completedAt":"2026-09-20T10:00:00Z","dayName":"Push","recording":"AS_PLANNED",
                   "exercises":[{"exerciseId":"bench-id","name":"Bench press",
                     "sets":[{"kg":60,"minReps":8,"maxReps":12}]}]}],
                 "session":{"dayId":"push","dayName":"Push","startedAt":"2026-09-22T10:00:00Z",
                   "exercises":[{"exerciseId":"bench-id","name":"Bench press","sets":[
                     {"planned":{"kg":60,"minReps":8,"maxReps":12},
                      "weight":"62.5","reps":"10","completed":true}]}]}}
                """;
        Files.writeString(file, olderSave);
        service = new WorkoutService(new WorkoutStore(file));
        assertEquals(olderSave, Files.readString(file));
        String id = benchId();
        var previous = service.getState().history().getFirst();
        var active = service.getState().session();
        // Reusing an old display name for a new library entry must not relink the old snapshot.
        service.addExercise("Bench press");
        var library = service.getState().library();
        service.correctRecord(previous.id(), previous.dayName(), previous.exercises());
        assertEquals("Flat bench press", service.getState().days().getFirst().exercises().getFirst().name());
        assertEquals(active, service.getState().session());
        assertEquals(previous, service.getState().history().getFirst());
        service = new WorkoutService(new WorkoutStore(file));
        assertEquals(active, service.getState().session());
        assertEquals(previous, service.getState().history().getFirst());

        service.finishSession();
        var completed = service.getState().history().getFirst().exercises().getFirst();
        assertEquals("Bench press", completed.name());
        assertEquals(id, completed.exerciseId());
        assertEquals(library, service.getState().library());
        assertEquals(service.getState(), new WorkoutStore(file).load());
    }

    @Test
    void actualIdentity_rejectsMismatchedNamesAndArchivedAdditionsWithoutSaving() throws Exception {
        String rowId = service.getState().days().get(1).exercises().getFirst().exerciseId();
        var sets = List.of(new WorkoutSet(60, 8, 8));
        State before = service.getState();
        String saved = Files.readString(file);
        assertEquals(MISMATCHED_NAME, rejected(() -> service.completeWithChanges(
                List.of(new WorkoutExercise(rowId, "Bench press", sets)))));
        assertEquals(before, service.getState());
        assertEquals(saved, Files.readString(file));
        service.completeWithChanges(List.of(new WorkoutExercise(benchId(), "Bench press", sets)));
        var record = service.getState().history().getFirst();
        State recorded = service.getState();
        String recordedBytes = Files.readString(file);
        assertEquals(MISMATCHED_NAME, rejected(() -> service.correctRecord(record.id(), record.dayName(),
                List.of(new WorkoutExercise(rowId, "Bench press", sets)))));
        assertEquals(recorded, service.getState());
        assertEquals(recordedBytes, Files.readString(file));
        service.archiveExercise(rowId, true);
        State archived = service.getState();
        String archivedBytes = Files.readString(file);
        // The name now matches, so this rejection must come from the archive rule rather than the
        // identity rule above. Only the message distinguishes them.
        assertEquals("Choose an active exercise or restore it first.",
                rejected(() -> service.correctRecord(record.id(), record.dayName(),
                        List.of(new WorkoutExercise(rowId, "Barbell row", sets)))));
        assertEquals(archived, service.getState());
        assertEquals(archivedBytes, Files.readString(file));
        service.archiveExercise(rowId, false);
        service.correctRecord(record.id(), record.dayName(), List.of(new WorkoutExercise(rowId, "Barbell row", sets)));
        assertEquals(rowId, service.getState().history().getFirst().exercises().getFirst().exerciseId());
        assertEquals(service.getState(), new WorkoutStore(file).load());
    }

    @Test
    void replacingExercises_updatesOnlyTheSelectedPlanOrHistoryAndDerivedPerformance() throws Exception {
        String id = benchId();
        String rowId = service.getState().days().get(1).exercises().getFirst().exerciseId();
        var actual = List.of(new WorkoutExercise(id, "Bench press", List.of(new WorkoutSet(60, 8, 8))));
        service.completeWithChanges(actual);
        service.skip();
        service.completeWithChanges(actual);
        var record = service.getState().history().getFirst();
        var otherRecord = service.getState().history().get(1);
        service.startSession();
        State before = service.getState();
        var replacement = List.of(new WorkoutExercise("Barbell row", List.of(new WorkoutSet(40, 10, 10))));
        service.correctRecord(record.id(), record.dayName(), replacement);
        var corrected = service.getState().history().getFirst();
        assertEquals(rowId, corrected.exercises().getFirst().exerciseId());
        assertEquals(record.id(), corrected.id());
        assertEquals(record.dayId(), corrected.dayId());
        assertEquals(record.completedAt(), corrected.completedAt());
        assertEquals(record.recording(), corrected.recording());
        assertEquals(List.of(corrected), service.performance(rowId));
        assertEquals(List.of(otherRecord), service.performance(id));
        assertEquals(corrected, service.previousPerformance(rowId).orElseThrow());
        assertEquals(otherRecord, service.previousPerformance(id).orElseThrow());
        assertEquals(before.library(), service.getState().library());
        assertEquals(before.days(), service.getState().days());
        assertEquals(before.session(), service.getState().session());
        assertEquals(before.currentDay(), service.getState().currentDay());
        service.discardSession();
        var history = service.getState().history();
        service.updateDay(0, "Push", replacement);
        assertEquals(rowId, service.getState().days().getFirst().exercises().getFirst().exerciseId());
        assertEquals(before.library(), service.getState().library());
        assertEquals(history, service.getState().history());
        assertEquals(before.currentDay(), service.getState().currentDay());
        assertEquals(service.getState(), new WorkoutStore(file).load());
    }

    /**
     * Returns the message of an expected rejection, so each check names the rule it exercises
     * instead of accepting any IllegalArgumentException the call happens to raise.
     */
    private static String rejected(Executable call) {
        return assertThrows(IllegalArgumentException.class, call).getMessage();
    }

    private String benchId() {
        return service.getState().days().getFirst().exercises().getFirst().exerciseId();
    }
}
