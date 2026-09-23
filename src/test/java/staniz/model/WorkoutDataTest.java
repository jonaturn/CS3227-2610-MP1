package staniz.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import staniz.model.WorkoutData.Exercise;
import staniz.model.WorkoutData.Recording;
import staniz.model.WorkoutData.Session;
import staniz.model.WorkoutData.SessionExercise;
import staniz.model.WorkoutData.SessionSet;
import staniz.model.WorkoutData.State;
import staniz.model.WorkoutData.WorkoutDay;
import staniz.model.WorkoutData.WorkoutExercise;
import staniz.model.WorkoutData.WorkoutRecord;
import staniz.model.WorkoutData.WorkoutSet;

/**
 * Verifies value and state invariants directly at the model boundary.
 * State is the only layer that checks meaning rather than shape: JSON validation accepts a
 * well-formed file that refers to a deleted exercise, and these rules are what reject it.
 * Each case asserts the specific message so a rule cannot be satisfied by an unrelated failure.
 */
class WorkoutDataTest {
    private static final WorkoutSet SET = new WorkoutSet(60, 8, 12);
    private static final String BENCH = "Bench press";

    @Test
    void invalidNumbersAndState_areRejectedAtTheModelBoundary() {
        for (double kg : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertEquals("Weight must be a finite number of kg, zero or greater.",
                    rejected(() -> new WorkoutSet(kg, 8, 12)));
        }
        String reps = "Reps must be positive, with the maximum at least the minimum.";
        assertEquals(reps, rejected(() -> new WorkoutSet(0, 0, 1)));
        assertEquals(reps, rejected(() -> new WorkoutSet(1, 12, 8)));
        WorkoutDay day = new WorkoutDay("same", "Push", List.of());
        assertEquals("Workout day IDs must be unique.",
                rejected(() -> state(List.of(), List.of(day, day), 0, List.of(), null)));
        assertEquals("The current workout position is invalid.",
                rejected(() -> state(List.of(), List.of(day), -1, List.of(), null)));
    }

    @Test
    void duplicateIdentities_areRejectedAcrossLibraryAndHistory() {
        Exercise bench = exercise("bench", BENCH);
        assertEquals("Exercise names must be unique.",
                rejected(() -> state(List.of(bench, exercise("other", "bench PRESS")),
                        List.of(), 0, List.of(), null)));
        assertEquals("Exercise IDs must be unique.",
                rejected(() -> state(List.of(bench, exercise("bench", "Squat")), List.of(), 0, List.of(), null)));
        WorkoutRecord repeated = record("same", "bench", BENCH);
        assertEquals("History record IDs must be unique.",
                rejected(() -> state(List.of(bench), List.of(), 0, List.of(repeated, repeated), null)));
    }

    @Test
    void exercisesMissingFromTheLibrary_areRejectedWhereverTheyAppear() {
        Exercise bench = exercise("bench", BENCH);
        WorkoutDay push = new WorkoutDay("push", "Push", List.of(planned("bench", BENCH)));
        assertEquals("A planned exercise is missing from the library.",
                rejected(() -> state(List.of(bench),
                        List.of(new WorkoutDay("push", "Push", List.of(planned("ghost", "Ghost")))),
                        0, List.of(), null)));
        assertEquals("A history exercise is missing from the library.",
                rejected(() -> state(List.of(bench), List.of(), 0, List.of(record("old", "ghost", "Ghost")), null)));
        assertEquals("An active exercise is missing from the library.",
                rejected(() -> state(List.of(bench), List.of(push), 0, List.of(),
                        session("push", "ghost", "Ghost"))));
        // A blank identity is resolved by name instead, which is a separate rule with its own message.
        assertEquals("Exercise is missing from the library.",
                rejected(() -> state(List.of(bench),
                        List.of(new WorkoutDay("push", "Push", List.of(planned("", "Ghost")))),
                        0, List.of(), null)));
    }

    @Test
    void activeSession_mustBelongToTheCurrentDay() {
        Exercise bench = exercise("bench", BENCH);
        WorkoutDay push = new WorkoutDay("push", "Push", List.of(planned("bench", BENCH)));
        WorkoutDay pull = new WorkoutDay("pull", "Pull", List.of(planned("bench", BENCH)));
        String message = "An active session must belong to the current workout day.";
        assertEquals(message, rejected(() -> state(List.of(bench), List.of(push, pull), 0, List.of(),
                session("pull", "bench", BENCH))));
        assertEquals(message, rejected(() -> state(List.of(bench), List.of(), 0, List.of(),
                session("push", "bench", BENCH))));
        assertDoesNotThrow(() -> state(List.of(bench), List.of(push, pull), 1, List.of(),
                session("pull", "bench", BENCH)));
    }

    @Test
    void splitBounds_rejectOverflowAndAcceptTheirEdges() {
        List<WorkoutDay> sevenDays = new ArrayList<>();
        for (int index = 0; index < WorkoutData.MAX_WORKOUT_DAYS; index++) {
            sevenDays.add(new WorkoutDay("day" + index, "Day " + index, List.of()));
        }
        List<WorkoutDay> eightDays = new ArrayList<>(sevenDays);
        eightDays.add(new WorkoutDay("day7", "Day 7", List.of()));
        assertEquals("A split can contain at most " + WorkoutData.MAX_WORKOUT_DAYS + " workout days.",
                rejected(() -> state(List.of(), eightDays, 0, List.of(), null)));
        assertEquals("The current workout position is invalid.",
                rejected(() -> state(List.of(), sevenDays, sevenDays.size(), List.of(), null)));
        assertEquals("Unsupported workout file version.", assertThrows(IllegalArgumentException.class,
                () -> new State(WorkoutData.CURRENT_SCHEMA_VERSION + 1, List.of(), List.of(), 0, List.of(), null))
                .getMessage());
        // The accepted edges matter as much as the rejections: an empty split during setup,
        // and a full split sitting on its last day.
        assertDoesNotThrow(() -> state(List.of(), List.of(), 0, List.of(), null));
        assertDoesNotThrow(() -> state(List.of(), sevenDays, sevenDays.size() - 1, List.of(), null));
    }

    private static State state(List<Exercise> library, List<WorkoutDay> days, int currentDay,
                               List<WorkoutRecord> history, Session session) {
        return new State(WorkoutData.CURRENT_SCHEMA_VERSION, library, days, currentDay, history, session);
    }

    private static Exercise exercise(String id, String name) {
        return new Exercise(id, name, false);
    }

    private static WorkoutExercise planned(String exerciseId, String name) {
        return new WorkoutExercise(exerciseId, name, List.of(SET));
    }

    private static WorkoutRecord record(String id, String exerciseId, String name) {
        return new WorkoutRecord(id, "push", "2026-09-20T10:00:00Z", "Push", Recording.AS_PLANNED,
                List.of(planned(exerciseId, name)));
    }

    private static Session session(String dayId, String exerciseId, String name) {
        return new Session(dayId, "Push", "2026-09-22T10:00:00Z",
                List.of(new SessionExercise(exerciseId, name, List.of(new SessionSet(SET, "60", "10", false)))));
    }

    private static String rejected(Executable call) {
        return assertThrows(IllegalArgumentException.class, call).getMessage();
    }
}
