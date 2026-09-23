package staniz.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Tests the real loading boundary against silent defaults, coercion, and loss of saved data.
 */
class WorkoutJsonValidationTest {
    private static final String VALID = """
            {"version":2,"library":[{"id":"bench","name":"Bench press","archived":false}],
             "days":[{"id":"push","name":"Push","exercises":[
               {"exerciseId":"bench","name":"Bench press","sets":[{"kg":0,"minReps":8,"maxReps":12}]}]}],
             "currentDay":0,"history":[{"id":"record","dayId":"push","completedAt":"2026-09-20T10:00:00Z",
               "dayName":"Push","recording":"AS_PLANNED","exercises":[
                 {"exerciseId":"bench","name":"Bench press","sets":[{"kg":60.5,"minReps":8,"maxReps":12}]}]}],
             "session":{"dayId":"push","dayName":"Push","startedAt":"2026-09-22T10:00:00Z",
               "exercises":[{"exerciseId":"bench","name":"Bench press","sets":[
                 {"planned":{"kg":60.5,"minReps":8,"maxReps":12},"weight":"62.","reps":"8-","completed":false}]}]}}
            """;

    @TempDir
    private Path directory;

    @Test
    void missingAndNullFields_rejectDefaultsAcrossAllRecordTypes() throws Exception {
        List<String> fields = List.of("version", "currentDay", "library", "days", "history",
                "library.0.id", "library.0.name", "library.0.archived", "days.0.id", "days.0.name",
                "days.0.exercises", "days.0.exercises.0.exerciseId", "days.0.exercises.0.name",
                "days.0.exercises.0.sets", "days.0.exercises.0.sets.0.kg",
                "days.0.exercises.0.sets.0.minReps", "days.0.exercises.0.sets.0.maxReps",
                "history.0.id", "history.0.dayId", "history.0.completedAt", "history.0.dayName",
                "history.0.recording", "history.0.exercises", "history.0.exercises.0.exerciseId",
                "history.0.exercises.0.sets.0.kg", "session.dayId", "session.dayName", "session.startedAt",
                "session.exercises", "session.exercises.0.exerciseId", "session.exercises.0.name",
                "session.exercises.0.sets", "session.exercises.0.sets.0.planned",
                "session.exercises.0.sets.0.planned.kg", "session.exercises.0.sets.0.weight",
                "session.exercises.0.sets.0.reps", "session.exercises.0.sets.0.completed");
        for (String field : fields) {
            assertRejectedThenRecover(VALID, field, null, "Missing required field");
            assertRejectedThenRecover(VALID, field, JsonNull.INSTANCE, "Missing required field");
        }
    }

    @Test
    void fractionalAndOverflowingIntegers_areNeverTruncatedOrWrapped() throws Exception {
        for (String field : List.of("version", "currentDay", "days.0.exercises.0.sets.0.minReps",
                "days.0.exercises.0.sets.0.maxReps", "history.0.exercises.0.sets.0.minReps",
                "session.exercises.0.sets.0.planned.maxReps")) {
            for (String value : List.of("2.9", "8.5", "1.0000000000000000001", "2147483648",
                    "-2147483649", "4294967296", "1e100")) {
                assertRejectedThenRecover(VALID, field, JsonParser.parseString(value), "whole number");
            }
        }
    }

    @Test
    void incorrectJsonTypes_areRejectedInsteadOfCoerced() throws Exception {
        List<String[]> cases = List.of(
                new String[]{"version", "\"2\""},
                new String[]{"currentDay", "false"},
                new String[]{"days", "{}"},
                new String[]{"days.0", "[]"},
                new String[]{"library.0", "null"},
                new String[]{"library.0.name", "123"},
                new String[]{"library.0.archived", "\"false\""},
                new String[]{"days.0.exercises.0.sets.0.kg", "\"60.5\""},
                new String[]{"days.0.exercises.0.sets.0.minReps", "\"8\""},
                new String[]{"history.0.recording", "true"},
                new String[]{"session", "[]"},
                new String[]{"session.exercises.0.sets.0.planned", "false"},
                new String[]{"session.exercises.0.sets.0.weight", "62.5"},
                new String[]{"session.exercises.0.sets.0.reps", "8"},
                new String[]{"session.exercises.0.sets.0.completed", "\"true\""});
        for (String[] item : cases) {
            assertRejectedThenRecover(VALID, item[0], JsonParser.parseString(item[1]), "expected");
        }
    }

    @Test
    void validNumbersDraftsAndOptionalSession_roundTripWithoutChangingValues() throws Exception {
        JsonObject document = JsonParser.parseString(VALID).getAsJsonObject();
        replace(document, "version", JsonParser.parseString("2.0"));
        replace(document, "currentDay", JsonParser.parseString("0e0"));
        replace(document, "days.0.exercises.0.sets.0.minReps", JsonParser.parseString("8.0"));
        replace(document, "days.0.exercises.0.sets.0.maxReps", JsonParser.parseString("2147483647"));
        Path file = directory.resolve("valid.json");
        Files.writeString(file, document.toString());
        WorkoutStore store = new WorkoutStore(file);
        var state = store.load();
        var set = state.days().getFirst().exercises().getFirst().sets().getFirst();
        assertEquals(0, set.kg());
        assertEquals(8, set.minReps());
        assertEquals(Integer.MAX_VALUE, set.maxReps());
        assertEquals("8-", state.session().exercises().getFirst().sets().getFirst().reps());
        store.save(state);
        assertEquals(state, store.load());
        document.remove("session");
        Files.writeString(file, document.toString());
        assertNull(store.load().session());
        document.add("session", JsonNull.INSTANCE);
        Files.writeString(file, document.toString());
        assertNull(store.load().session());
    }

    @Test
    void legacyFields_areValidatedBeforeMigrationAndOriginalBytesAreBackedUp() throws Exception {
        String legacy = """
                {"version":1,"exercises":["Bench press"],"days":[{"id":"push","name":"Push",
                 "exercises":[{"name":"Bench press","sets":[{"kg":60.5,"minReps":8,"maxReps":12}]}]}],
                 "currentDay":0,"history":[{"completedAt":"2026-09-20T10:00:00Z","dayName":"Push",
                 "recording":"AS_PLANNED","exercises":[{"name":"Bench press",
                 "sets":[{"kg":60.5,"minReps":8,"maxReps":12}]}]}]}
                """;
        assertRejectedThenRecover(legacy, "version", JsonParser.parseString("1.9"), "whole number");
        assertRejectedThenRecover(legacy, "exercises.0", JsonParser.parseString("123"), "a string");
        assertRejectedThenRecover(legacy, "days.0.exercises.0.name", JsonNull.INSTANCE, "Missing required");
        assertRejectedThenRecover(legacy, "days.0.exercises.0.sets.0.kg", null, "Missing required");
        assertRejectedThenRecover(legacy, "history.0.exercises.0.sets.0.minReps",
                JsonParser.parseString("8.5"), "whole number");
        Path file = directory.resolve("legacy.json");
        Files.writeString(file, legacy);
        WorkoutStore store = new WorkoutStore(file);
        var state = store.load();
        assertEquals(legacy, Files.readString(file));
        store.save(state);
        assertEquals(legacy, Files.readString(directory.resolve("legacy.json.v1.bak")));
        assertEquals(state, new WorkoutStore(file).load());
    }

    /**
     * Interleaves valid/invalid/valid loads through the same store and checks file preservation and diagnostics.
     */
    private void assertRejectedThenRecover(String valid, String field, JsonElement value, String message)
            throws Exception {
        Path file = directory.resolve("workouts.json");
        WorkoutStore store = new WorkoutStore(file);
        Files.writeString(file, valid);
        var expected = store.load();
        JsonObject invalid = JsonParser.parseString(valid).getAsJsonObject();
        replace(invalid, field, value);
        String contents = invalid.toString();
        Files.writeString(file, contents);
        StorageException error = assertThrows(StorageException.class, store::load, field);
        assertTrue(error.getMessage().contains(message), error.getMessage());
        String jsonPath = "$." + field.replaceAll("\\.(\\d+)", "[$1]");
        assertTrue(error.getMessage().contains(jsonPath), error.getMessage());
        assertEquals(contents, Files.readString(file));
        assertFalse(Files.exists(directory.resolve("workouts.json.v1.bak")));
        Files.writeString(file, valid);
        assertEquals(expected, store.load());
        assertEquals(valid, Files.readString(file));
    }

    /**
     * Uses dot-separated field names and array indexes to alter one fixture value; null removes an object field.
     */
    private static void replace(JsonObject document, String path, JsonElement value) {
        String[] parts = path.split("\\.");
        JsonElement parent = document;
        for (int index = 0; index < parts.length - 1; index++) {
            parent = parent.isJsonArray() ? parent.getAsJsonArray().get(Integer.parseInt(parts[index]))
                    : parent.getAsJsonObject().get(parts[index]);
        }
        String field = parts[parts.length - 1];
        if (parent.isJsonArray()) {
            parent.getAsJsonArray().set(Integer.parseInt(field), value);
        } else if (value == null) {
            parent.getAsJsonObject().remove(field);
        } else {
            parent.getAsJsonObject().add(field, value);
        }
    }
}
