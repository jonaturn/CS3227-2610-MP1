package staniz.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonParser;

import staniz.model.WorkoutData.Exercise;
import staniz.model.WorkoutData.State;
import staniz.model.WorkoutData.WorkoutDay;
import staniz.model.WorkoutData.WorkoutExercise;
import staniz.model.WorkoutData.WorkoutRecord;
import staniz.model.WorkoutData.WorkoutSet;
import staniz.service.WorkoutService;

/**
 * Exercises JSON validation and real file-system failures without touching user saves.
 */
class WorkoutStoreTest {
    @TempDir
    private Path directory;

    @Test
    void roundTrip_preservesUnicodeQuotesAndBackslashes() throws Exception {
        Path file = directory.resolve("nested/workouts.json");
        WorkoutService service = new WorkoutService(new WorkoutStore(file));
        service.addExercise("Développé \"incliné\" \\ cable");
        assertEquals(service.getState(), new WorkoutStore(file).load());
        assertTrue(Files.readString(file).contains("\"version\": 2"));
    }

    @Test
    void malformedOrUnsupportedJson_isRejectedAndNeverOverwritten() throws Exception {
        Path file = directory.resolve("workouts.json");
        // Every load failure must carry the recovery guidance, whatever layer rejected it. The
        // explanation is pinned only where this project produces the wording: Gson owns the text
        // for the parse failures, so asserting it would break on a library upgrade for no gain.
        Map<String, String> invalid = new LinkedHashMap<>();
        invalid.put("", null);
        invalid.put("null", null);
        invalid.put("[]", null);
        invalid.put("{broken", null);
        invalid.put("{}", null);
        invalid.put("{\"version\":2,\"exercises\":[],\"days\":[],\"currentDay\":0,\"history\":[]}",
                "Missing required field: $.library.");
        invalid.put("{\"version\":1,\"exercises\":[],\"days\":[],\"currentDay\":1,\"history\":[]}",
                "The current workout position is invalid.");
        invalid.put("{\"version\":1,\"exercises\":[\"Squat\",\"squat\"],\"days\":[],\"currentDay\":0,\"history\":[]}",
                "Exercise names must be unique.");
        invalid.put("{\"version\":1,\"exercises\":[],\"days\":[],\"currentDay\":0,\"history\":[]} trailing", null);
        for (Map.Entry<String, String> entry : invalid.entrySet()) {
            String contents = entry.getKey();
            Files.writeString(file, contents);
            StorageException error = assertThrows(StorageException.class,
                    () -> new WorkoutStore(file).load(), contents);
            assertTrue(error.getMessage().contains("Your saved file has been left unchanged"), contents);
            if (entry.getValue() != null) {
                assertTrue(error.getMessage().endsWith(entry.getValue()),
                        contents + " produced " + error.getMessage());
            }
            if (contents.equals("{broken")) {
                assertTrue(error.getMessage().endsWith(error.getCause().getMessage()));
            }
            assertEquals(contents, Files.readString(file));
        }
    }

    @Test
    void directoryInsteadOfFile_reportsReadAndWriteFailures() throws Exception {
        WorkoutStore store = new WorkoutStore(directory);
        StorageException error = assertThrows(StorageException.class, store::load);
        assertTrue(error.getMessage().endsWith(error.getCause().getMessage()));
        assertThrows(StorageException.class, () -> store.save(State.initial()));
        assertTrue(Files.isDirectory(directory));
    }

    @Test
    void mismatchedPlannedName_isRejectedWithoutOverwritingTheSavedFile() throws Exception {
        Path file = directory.resolve("workouts.json");
        String invalid = """
                {"version":2,"library":[
                  {"id":"bench-id","name":"Bench press","archived":false},
                  {"id":"squat-id","name":"Squat","archived":false}],
                 "days":[{"id":"push","name":"Push","exercises":[
                  {"exerciseId":"squat-id","name":"Bench press",
                   "sets":[{"kg":60,"minReps":8,"maxReps":12}]}]}],
                 "currentDay":0,"history":[]}
                """;
        Files.writeString(file, invalid);
        StorageException error = assertThrows(StorageException.class, () -> new WorkoutStore(file).load());
        assertTrue(error.getMessage().contains("Your saved file has been left unchanged"));
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        assertEquals("A planned exercise name does not match its library ID.", cause.getMessage());
        assertTrue(error.getMessage().endsWith(cause.getMessage()));
        assertFalse(error.getMessage().contains("Failed to invoke constructor"));
        assertEquals(invalid, Files.readString(file));
    }

    @Test
    void invalidSetValues_showValidationExplanationAndAllowRecovery() throws Exception {
        Path file = directory.resolve("workouts.json");
        WorkoutStore store = new WorkoutStore(file);
        WorkoutService service = new WorkoutService(store);
        service.addDay("Push");
        service.updateDay(0, "Push", List.of(new WorkoutExercise("Bench press", List.of(new WorkoutSet(60, 8, 12)))));
        String valid = Files.readString(file);
        for (String field : List.of("minReps", "kg")) {
            var document = JsonParser.parseString(valid).getAsJsonObject();
            document.getAsJsonArray("days").get(0).getAsJsonObject().getAsJsonArray("exercises")
                    .get(0).getAsJsonObject().getAsJsonArray("sets").get(0).getAsJsonObject().addProperty(field, -1);
            String invalid = document.toString();
            Files.writeString(file, invalid);
            StorageException error = assertThrows(StorageException.class, store::load);
            String explanation = field.equals("minReps")
                    ? "Reps must be positive, with the maximum at least the minimum."
                    : "Weight must be a finite number of kg, zero or greater.";
            assertEquals("Cannot load " + file + ". Your saved file has been left unchanged. "
                    + "Check its contents or restore a backup. " + explanation, error.getMessage());
            assertTrue(error.getCause().getMessage().contains("Failed to invoke constructor"));
            assertEquals(invalid, Files.readString(file));
            Files.writeString(file, valid);
            assertEquals(service.getState(), store.load());
        }
    }

    @Test
    void invalidCompletedSession_keepsFriendlyExplanationAndUnderlyingNumberFailure() throws Exception {
        Path file = directory.resolve("workouts.json");
        WorkoutStore store = new WorkoutStore(file);
        WorkoutService service = new WorkoutService(store);
        service.addDay("Push");
        service.updateDay(0, "Push", List.of(new WorkoutExercise("Bench press", List.of(new WorkoutSet(60, 8, 12)))));
        service.startSession();
        String valid = Files.readString(file);
        var document = JsonParser.parseString(valid).getAsJsonObject();
        var set = document.getAsJsonObject("session").getAsJsonArray("exercises").get(0).getAsJsonObject()
                .getAsJsonArray("sets").get(0).getAsJsonObject();
        set.addProperty("completed", true);
        set.addProperty("weight", "not a number");
        set.addProperty("reps", "8");
        String invalid = document.toString();
        Files.writeString(file, invalid);
        StorageException error = assertThrows(StorageException.class, store::load);
        assertTrue(error.getMessage().endsWith("Enter a valid kg weight and exact positive reps."));
        assertFalse(error.getMessage().contains("Failed to invoke constructor"));
        Throwable root = error;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        assertInstanceOf(NumberFormatException.class, root);
        assertTrue(error.getCause().getMessage().contains("Failed to invoke constructor"));
        assertEquals(invalid, Files.readString(file));
        Files.writeString(file, valid);
        assertEquals(service.getState(), store.load());
    }

    @Test
    void migration_archivesHistoricalOnlyExerciseOnceAndPreservesRecordIdentities() throws Exception {
        String record = """
                {"completedAt":"2026-09-20T10:00:00Z","dayName":"Old Push","recording":"WITH_CHANGES",
                 "exercises":[{"name":"Cable fly","sets":[{"kg":12.5,"minReps":10,"maxReps":10}]},
                 {"name":"Bench press","sets":[{"kg":60,"minReps":8,"maxReps":8}]}]}
                """;
        String legacy = """
                {"version":1,"exercises":["Bench press","Squat"],"days":[
                 {"id":"push","name":"Push","exercises":[{"name":"Bench press",
                  "sets":[{"kg":60,"minReps":8,"maxReps":12}]}]},
                 {"id":"legs","name":"Legs","exercises":[]}],"currentDay":1,"history":[
                """ + record + "," + record + "]}";
        Path file = directory.resolve("workouts.json");
        Path backup = directory.resolve("workouts.json.v1.bak");
        Files.writeString(file, legacy);
        WorkoutStore store = new WorkoutStore(file);
        State state = store.load();
        assertEquals(List.of("Bench press", "Squat", "Cable fly"), state.exercises());
        assertEquals(List.of(false, false, true), state.library().stream().map(Exercise::archived).toList());
        assertEquals(List.of("push", "legs"), state.days().stream().map(WorkoutDay::id).toList());
        assertEquals(1, state.currentDay());
        // Fixed identities protect compatibility with saves migrated before helper extraction.
        assertEquals(List.of("38b8227a-2f20-31e1-a188-4c8ce2d44dea", "0f2cde89-0441-3a18-81c2-5a3219a6ee7e"),
                state.history().stream().map(WorkoutRecord::id).toList());
        String archivedId = state.library().get(2).id();
        String benchId = state.library().getFirst().id();
        for (WorkoutRecord migrated : state.history()) {
            assertEquals("", migrated.dayId());
            assertEquals("2026-09-20T10:00:00Z", migrated.completedAt());
            assertEquals("Old Push", migrated.dayName());
            assertEquals(List.of(new WorkoutExercise(archivedId, "Cable fly", List.of(new WorkoutSet(12.5, 10, 10))),
                    new WorkoutExercise(benchId, "Bench press", List.of(new WorkoutSet(60, 8, 8)))),
                    migrated.exercises());
        }
        assertEquals(benchId, state.days().getFirst().exercises().getFirst().exerciseId());
        assertEquals(state, new WorkoutStore(file).load());
        assertEquals(legacy, Files.readString(file));
        assertFalse(Files.exists(backup));
        store.save(state);
        assertEquals(legacy, Files.readString(backup));
        assertEquals(state, new WorkoutStore(file).load());
        store.save(state);
        assertEquals(legacy, Files.readString(backup));
    }

    @Test
    void unwritableParent_doesNotChangeExistingFile() throws Exception {
        Path parent = directory.resolve("not-a-folder");
        Files.writeString(parent, "keep this");
        WorkoutStore store = new WorkoutStore(parent.resolve("workouts.json"));
        assertThrows(StorageException.class, () -> store.save(State.initial()));
        assertEquals("keep this", Files.readString(parent));
    }

    /**
     * A save that fails after its temporary file exists must not leave that file behind, or repeated
     * failures would litter the user's data folder. The move is blocked by pointing the store at a
     * non-empty directory, which cannot be replaced by a file on any supported platform.
     */
    @Test
    void failedSave_removesItsTemporaryFileAndLeavesTheTargetAlone() throws Exception {
        Path target = directory.resolve("workouts.json");
        Files.createDirectory(target);
        Files.writeString(target.resolve("blocker.txt"), "not a save file");
        WorkoutStore store = new WorkoutStore(target);

        StorageException error = assertThrows(StorageException.class, () -> store.save(State.initial()));
        assertTrue(error.getMessage().contains("No change was applied"));
        try (var entries = Files.list(directory)) {
            assertEquals(List.of(), entries.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".tmp")).sorted().toList(),
                    "A failed save left a temporary file behind.");
        }
        assertEquals("not a save file", Files.readString(target.resolve("blocker.txt")));
    }

    @Test
    void legacyTaskFile_isLeftUntouched() throws Exception {
        Path legacy = directory.resolve("staniz.txt");
        Files.writeString(legacy, "T | 0 | old task");
        WorkoutService service = new WorkoutService(new WorkoutStore(directory.resolve("workouts.json")));
        service.addDay("Push");
        assertEquals("T | 0 | old task", Files.readString(legacy));
    }

}
