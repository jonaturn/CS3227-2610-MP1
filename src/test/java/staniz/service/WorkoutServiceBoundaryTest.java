package staniz.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import staniz.model.WorkoutData.State;
import staniz.model.WorkoutData.WorkoutExercise;
import staniz.model.WorkoutData.WorkoutRecord;
import staniz.model.WorkoutData.WorkoutSet;
import staniz.storage.StorageException;
import staniz.storage.WorkoutStore;

/**
 * Covers session index boundaries and performance ordering through real service operations and isolated saves.
 */
class WorkoutServiceBoundaryTest {
    @TempDir
    private Path directory;
    private Path file;
    private WorkoutService service;

    @BeforeEach
    void setUp() throws Exception {
        file = directory.resolve("workouts.json");
        service = new WorkoutService(new WorkoutStore(file));
        service.addDay("Full body");
        service.updateDay(0, "Full body", List.of(
                new WorkoutExercise("Bench press", List.of(new WorkoutSet(60, 8, 12), new WorkoutSet(70, 8, 8))),
                new WorkoutExercise("Squat", List.of(new WorkoutSet(80, 5, 5)))));
    }

    @Test
    void sessionIndexes_rejectOutsideBoundsAndAcceptFirstAndLastPositions() throws Exception {
        State noSession = service.getState();
        String initialBytes = Files.readString(file);
        assertEquals("Start a workout session first.",
                rejected(() -> service.updateSessionSet(0, 0, "60", "8", true)));
        assertEquals(noSession, service.getState());
        assertEquals(initialBytes, Files.readString(file));
        service.startSession();

        // Each rejection is followed by a valid boundary selection: invalid exercise/set, valid exercise/set.
        int[][] cases = {{-1, 0, 0, 0}, {2, 0, 1, 0}, {0, -1, 0, 0},
            {0, 2, 0, 1}, {1, -1, 1, 0}, {1, 1, 1, 0}};
        int weight = 60;
        for (int[] indexes : cases) {
            State before = service.getState();
            String saved = Files.readString(file);
            var error = assertThrows(IllegalArgumentException.class,
                    () -> service.updateSessionSet(indexes[0], indexes[1], "999", "99", true));
            assertEquals(indexes[0] < 0 || indexes[0] >= 2 ? "Select an exercise." : "Select a set.",
                    error.getMessage());
            assertEquals(before, service.getState());
            assertEquals(saved, Files.readString(file));

            String actualWeight = Integer.toString(weight++);
            service.updateSessionSet(indexes[2], indexes[3], actualWeight, "8", true);
            var updated = service.getState().session().exercises().get(indexes[2]).sets().get(indexes[3]);
            assertEquals(actualWeight, updated.weight());
            assertEquals("8", updated.reps());
            assertTrue(updated.completed());
            assertEquals(before.session().exercises().get(indexes[2]).sets().get(indexes[3]).planned(),
                    updated.planned());
            assertEquals(before.days(), service.getState().days());
            assertEquals(before.library(), service.getState().library());
            assertEquals(before.history(), service.getState().history());
            assertEquals(before.currentDay(), service.getState().currentDay());
            service = new WorkoutService(new WorkoutStore(file));
            assertEquals(updated, service.getState().session().exercises().get(indexes[2]).sets().get(indexes[3]));
        }
    }

    @Test
    void performance_handlesEmptySingleAndMixedHistoryWithoutChangingSavedOrder() throws Exception {
        String benchId = exerciseId("Bench press");
        State empty = service.getState();
        String emptyBytes = Files.readString(file);
        assertTrue(service.performance(benchId).isEmpty());
        assertTrue(service.previousPerformance(benchId).isEmpty());
        assertEquals(empty, service.getState());
        assertEquals(emptyBytes, Files.readString(file));

        var newest = recordAt("2026-09-20T10:00:00Z", "Bench press", 70);
        assertEquals(List.of(newest), service.performance(benchId));
        assertEquals(newest, service.previousPerformance(benchId).orElseThrow());
        var oldest = recordAt("2026-09-10T10:00:00Z", "Bench press", 60);
        var middle = recordAt("2026-09-15T10:00:00Z", "Bench press", 65);
        useClock("2026-09-25T10:00:00Z");
        service.completeAsPlanned();
        var prescription = service.getState().history().getFirst();
        var unrelated = recordAt("2026-09-26T10:00:00Z", "Squat", 80);
        useClock("2026-09-27T10:00:00Z");
        service.completeWithChanges(List.of());
        var omitted = service.getState().history().getFirst();
        State before = service.getState();
        String saved = Files.readString(file);

        assertEquals(List.of(oldest, middle, newest), service.performance(benchId));
        assertEquals(newest, service.previousPerformance(benchId).orElseThrow());
        assertEquals(List.of(unrelated), service.performance(exerciseId("Squat")));
        assertTrue(service.performance(exerciseId("Deadlift")).isEmpty());
        assertTrue(service.previousPerformance(exerciseId("Deadlift")).isEmpty());
        assertEquals(List.of(omitted, unrelated, prescription, middle, oldest, newest), service.getState().history());
        assertEquals(before, service.getState());
        assertEquals(saved, Files.readString(file));
        service = new WorkoutService(new WorkoutStore(file));
        assertEquals(List.of(oldest, middle, newest), service.performance(benchId));
        assertEquals(newest, service.previousPerformance(benchId).orElseThrow());
        assertEquals(before, service.getState());
    }

    @Test
    void equalTimestamps_preserveSavedTieOrderAndPreferFirstMatchingRecord() throws Exception {
        String benchId = exerciseId("Bench press");
        var first = recordAt("2026-09-20T10:00:00Z", "Bench press", 60);
        var second = recordAt("2026-09-20T10:00:00Z", "Bench press", 70);
        var older = recordAt("2026-09-10T10:00:00Z", "Bench press", 50);
        State before = service.getState();
        String saved = Files.readString(file);
        assertEquals(List.of(older, second, first), service.performance(benchId));
        assertEquals(second, service.previousPerformance(benchId).orElseThrow());
        assertEquals(before, service.getState());
        assertEquals(saved, Files.readString(file));
        service = new WorkoutService(new WorkoutStore(file));
        assertEquals(List.of(older, second, first), service.performance(benchId));
        assertEquals(second, service.previousPerformance(benchId).orElseThrow());
        assertEquals(before, service.getState());
    }

    /**
     * The two identity guards reject unknown IDs without touching saved data, and the same
     * operations succeed once given real identities, so the guards reject only what they should.
     */
    @Test
    void unknownIdentities_areRejectedWithoutChangingHistoryOrLibrary() throws Exception {
        service.completeWithChanges(List.of(new WorkoutExercise("Bench press", List.of(new WorkoutSet(65, 9, 9)))));
        WorkoutRecord record = service.getState().history().getFirst();
        State before = service.getState();
        String saved = Files.readString(file);

        assertEquals("Select a history record.",
                rejected(() -> service.correctRecord("no-such-record", "Full body", record.exercises())));
        assertEquals("Select a library exercise.", rejected(() -> service.archiveExercise("no-such-id", true)));
        // A non-empty identity is looked up directly instead of resolved by name, so an unknown one
        // inside a workout reaches the same library guard.
        assertEquals("Select a library exercise.", rejected(() -> service.updateDay(0, "Full body",
                List.of(new WorkoutExercise("no-such-id", "Bench press", List.of(new WorkoutSet(60, 8, 8)))))));
        assertEquals(before, service.getState());
        assertEquals(saved, Files.readString(file));

        service.correctRecord(record.id(), "Full body fixed", record.exercises());
        assertEquals("Full body fixed", service.getState().history().getFirst().dayName());
        String benchId = exerciseId("Bench press");
        service.archiveExercise(benchId, true);
        assertTrue(service.getState().library().stream()
                .anyMatch(entry -> entry.id().equals(benchId) && entry.archived()));
        assertEquals(service.getState(), new WorkoutStore(file).load());
    }

    /**
     * Returns the message of an expected rejection, so each check names the rule it exercises
     * instead of accepting any IllegalArgumentException the call happens to raise.
     */
    private static String rejected(Executable call) {
        return assertThrows(IllegalArgumentException.class, call).getMessage();
    }

    /**
     * Reloads the same saved state with a deterministic completion time, independent of test execution speed.
     */
    private void useClock(String timestamp) throws StorageException {
        service = new WorkoutService(new WorkoutStore(file), Clock.fixed(Instant.parse(timestamp), ZoneOffset.UTC));
    }

    private WorkoutRecord recordAt(String timestamp, String name, double kg) throws StorageException {
        useClock(timestamp);
        service.completeWithChanges(List.of(new WorkoutExercise(name, List.of(new WorkoutSet(kg, 8, 8)))));
        return service.getState().history().getFirst();
    }

    private String exerciseId(String name) {
        return service.getState().library().stream().filter(entry -> entry.name().equals(name)).findFirst()
                .orElseThrow().id();
    }
}
