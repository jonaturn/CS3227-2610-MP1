package staniz.storage;

import java.util.function.BiConsumer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Checks persisted field presence and JSON types before Gson can default or coerce record values.
 * Record constructors remain responsible for workout rules and relationships between records.
 */
final class WorkoutJsonValidation {
    private WorkoutJsonValidation() {
    }

    /**
     * Reads the schema without truncating a fractional version or accepting a numeric string.
     */
    static int readVersion(JsonObject document) {
        return readInteger(document, "version", "$");
    }

    /**
     * Validates the saved representation before migration or record construction.
     * Legacy saves contain names instead of library entries and omit identities added by migration.
     * Unknown fields are ignored; an absent or null active session means no session is running.
     */
    static void validate(JsonObject document, boolean legacy) {
        readInteger(document, "currentDay", "$");
        if (legacy) {
            JsonArray names = requireArray(document, "exercises", "$");
            for (int index = 0; index < names.size(); index++) {
                validateString(names.get(index), "$.exercises[" + index + "]");
            }
        } else {
            validateObjectArray(document, "library", "$", (entry, path) -> {
                validateStringFields(entry, path, "id", "name");
                validateBooleanField(entry, "archived", path);
            });
        }
        validateObjectArray(document, "days", "$", (day, path) -> {
            validateStringFields(day, path, "id", "name");
            validateExercises(day, path, legacy);
        });
        validateObjectArray(document, "history", "$", (record, path) -> {
            validateStringFields(record, path, "completedAt", "dayName", "recording");
            if (!legacy) {
                validateStringFields(record, path, "id", "dayId");
            }
            validateExercises(record, path, legacy);
        });
        JsonElement session = document.get("session");
        if (session != null && !session.isJsonNull()) {
            validateSession(requireObject(session, "$.session"), "$.session");
        }
    }

    private static void validateExercises(JsonObject parent, String path, boolean legacy) {
        validateObjectArray(parent, "exercises", path, (exercise, exercisePath) -> {
            validateStringFields(exercise, exercisePath, "name");
            if (!legacy) {
                validateStringFields(exercise, exercisePath, "exerciseId");
            }
            validateObjectArray(exercise, "sets", exercisePath, WorkoutJsonValidation::validateSet);
        });
    }

    /**
     * Draft weight and reps intentionally remain strings, including unfinished or invalid input.
     * The model validates actual values only when the completed flag is true.
     */
    private static void validateSession(JsonObject session, String path) {
        validateStringFields(session, path, "dayId", "dayName", "startedAt");
        validateObjectArray(session, "exercises", path, (exercise, exercisePath) -> {
            validateStringFields(exercise, exercisePath, "exerciseId", "name");
            validateObjectArray(exercise, "sets", exercisePath, (draft, draftPath) -> {
                validateStringFields(draft, draftPath, "weight", "reps");
                validateBooleanField(draft, "completed", draftPath);
                validateSet(requireObject(requireField(draft, "planned", draftPath), draftPath + ".planned"),
                        draftPath + ".planned");
            });
        });
    }

    private static void validateSet(JsonObject set, String path) {
        requireNumber(requireField(set, "kg", path), path + ".kg");
        readInteger(set, "minReps", path);
        readInteger(set, "maxReps", path);
    }

    /**
     * Checks each array element and keeps its index in any validation error.
     */
    private static void validateObjectArray(JsonObject parent, String field, String path,
                                            BiConsumer<JsonObject, String> validateEntry) {
        JsonArray entries = requireArray(parent, field, path);
        for (int index = 0; index < entries.size(); index++) {
            String entryPath = path + "." + field + "[" + index + "]";
            validateEntry.accept(requireObject(entries.get(index), entryPath), entryPath);
        }
    }

    private static JsonArray requireArray(JsonObject parent, String field, String path) {
        JsonElement value = requireField(parent, field, path);
        if (!value.isJsonArray()) {
            throw invalidFieldError(path + "." + field, "an array");
        }
        return value.getAsJsonArray();
    }

    private static JsonObject requireObject(JsonElement value, String path) {
        if (!value.isJsonObject()) {
            throw invalidFieldError(path, "an object");
        }
        return value.getAsJsonObject();
    }

    private static void validateStringFields(JsonObject parent, String path, String... fields) {
        for (String field : fields) {
            validateString(requireField(parent, field, path), path + "." + field);
        }
    }

    private static void validateString(JsonElement value, String path) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw invalidFieldError(path, "a string");
        }
    }

    private static void validateBooleanField(JsonObject parent, String field, String path) {
        JsonElement value = requireField(parent, field, path);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw invalidFieldError(path + "." + field, "a boolean");
        }
    }

    private static JsonPrimitive requireNumber(JsonElement value, String path) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw invalidFieldError(path, "a number");
        }
        return value.getAsJsonPrimitive();
    }

    /**
     * Exact decimal conversion rejects fractions and overflow before Gson reads a Java int.
     * Numerically whole JSON values such as 8.0 or 8e0 remain valid.
     */
    private static int readInteger(JsonObject parent, String field, String path) {
        String fieldPath = path + "." + field;
        JsonPrimitive value = requireNumber(requireField(parent, field, path), fieldPath);
        try {
            return value.getAsBigDecimal().intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw invalidFieldError(fieldPath, "a whole number within the Java integer range");
        }
    }

    private static JsonElement requireField(JsonObject parent, String field, String path) {
        JsonElement value = parent.get(field);
        if (value == null || value.isJsonNull()) {
            throw new IllegalArgumentException("Missing required field: " + path + "." + field + ".");
        }
        return value;
    }

    private static IllegalArgumentException invalidFieldError(String path, String type) {
        return new IllegalArgumentException("Invalid field " + path + ": expected " + type + ".");
    }
}
