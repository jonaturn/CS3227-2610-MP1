package staniz.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;

import staniz.model.WorkoutData;
import staniz.model.WorkoutData.State;

/**
 * Persists one versioned JSON document, keeping workout advancement and history together.
 * Existing task-manager data is never read, converted, or overwritten.
 */
public class WorkoutStore {
    /**
     * Fixed input schema understood by the migration implemented below.
     */
    private static final int MIGRATION_SOURCE_VERSION = 1;

    /**
     * Fixed output schema; a future current schema needs a further migration step.
     */
    private static final int MIGRATION_TARGET_VERSION = 2;

    private final Path file;
    private boolean migrated;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().setStrictness(Strictness.STRICT)
            .disableJdkUnsafe().create();

    public WorkoutStore(Path file) {
        this.file = file.toAbsolutePath().normalize();
    }

    /**
     * Loads validated records, or a starter library if no workout file exists.
     * Invalid files remain untouched so the user can recover them.
     */
    public State load() throws StorageException {
        if (Files.notExists(file)) {
            return State.initial();
        }
        try {
            JsonObject document = gson.fromJson(Files.readString(file, StandardCharsets.UTF_8), JsonObject.class);
            if (document == null) {
                throw new IllegalArgumentException("The document is empty.");
            }
            int version = WorkoutJsonValidation.readVersion(document);
            if (version != MIGRATION_SOURCE_VERSION && version != WorkoutData.CURRENT_SCHEMA_VERSION) {
                throw new IllegalArgumentException("Unsupported workout file version.");
            }
            WorkoutJsonValidation.validate(document, version == MIGRATION_SOURCE_VERSION);
            if (version == MIGRATION_SOURCE_VERSION) {
                migrate(document);
                migrated = true;
            }
            return gson.fromJson(document, State.class);
        } catch (IOException | RuntimeException exception) {
            throw new StorageException("Cannot load " + file + ". Your saved file has been left unchanged. "
                    + "Check its contents or restore a backup. " + loadFailureMessage(exception), exception);
        }
    }

    /**
     * Finds a validation explanation inside Gson's constructor wrappers without discarding the cause chain.
     * The first argument-validation message may explain a deeper parsing failure more clearly than its root cause.
     * Other failures retain their original diagnostic message.
     */
    private static String loadFailureMessage(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (cause instanceof IllegalArgumentException && message != null && !message.isBlank()) {
                return message;
            }
        }
        return exception.getMessage();
    }

    /**
     * Writes a complete sibling temporary file before replacing the saved state.
     */
    public void save(State state) throws StorageException {
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            if (migrated && Files.exists(file)) {
                Path backup = file.resolveSibling(file.getFileName() + ".v1.bak");
                if (Files.notExists(backup)) {
                    Files.copy(file, backup);
                }
            }
            temporary = Files.createTempFile(file.getParent(), ".workouts-", ".tmp");
            Files.writeString(temporary, gson.toJson(state) + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            migrated = false;
        } catch (IOException | RuntimeException exception) {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException cleanupFailure) {
                    exception.addSuppressed(cleanupFailure);
                }
            }
            throw new StorageException("Cannot save " + file + ". No change was applied. "
                    + "Check that the folder is writable and has free space.", exception);
        }
    }

    /**
     * Upgrades the original name-based JSON in memory; the old file is backed up before the first save.
     */
    private void migrate(JsonObject document) {
        JsonArray library = createLegacyLibrary(document.getAsJsonArray("exercises"));
        migratePlannedDays(document.getAsJsonArray("days"));
        migrateHistory(document.getAsJsonArray("history"), library);
        document.remove("exercises");
        document.add("library", library);
        document.addProperty("version", MIGRATION_TARGET_VERSION);
    }

    /**
     * Converts the old name list into active library entries, retaining its order and deterministic IDs.
     */
    private JsonArray createLegacyLibrary(JsonArray names) {
        JsonArray library = new JsonArray();
        for (JsonElement name : names) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", WorkoutData.legacyId(name.getAsString()));
            entry.addProperty("name", name.getAsString());
            entry.addProperty("archived", false);
            library.add(entry);
        }
        return library;
    }

    /**
     * Adds exercise identities to the existing planned days without changing their order or prescriptions.
     */
    private void migratePlannedDays(JsonArray days) {
        for (JsonElement value : days) {
            linkLegacy(value.getAsJsonObject().getAsJsonArray("exercises"));
        }
    }

    /**
     * Upgrades history in order and retains exercises no longer in the original library.
     * Each record ID must use its index and original JSON before any fields are added to that record.
     */
    private void migrateHistory(JsonArray history, JsonArray library) {
        int index = 0;
        for (JsonElement value : history) {
            JsonObject record = value.getAsJsonObject();
            record.addProperty("id", UUID.nameUUIDFromBytes((index++ + record.toString())
                    .getBytes(StandardCharsets.UTF_8)).toString());
            record.addProperty("dayId", "");
            linkLegacy(record.getAsJsonArray("exercises"));
            archiveHistoricalExercises(record.getAsJsonArray("exercises"), library);
        }
    }

    /**
     * Appends each historical-only exercise once, archived, in first-appearance order.
     * Existing library entries retain their names and active status.
     */
    private void archiveHistoricalExercises(JsonArray exercises, JsonArray library) {
        for (JsonElement item : exercises) {
            JsonObject exercise = item.getAsJsonObject();
            String id = exercise.get("exerciseId").getAsString();
            boolean known = false;
            for (JsonElement entry : library) {
                known |= entry.getAsJsonObject().get("id").getAsString().equals(id);
            }
            if (!known) {
                JsonObject entry = new JsonObject();
                entry.addProperty("id", id);
                entry.add("name", exercise.get("name"));
                entry.addProperty("archived", true);
                library.add(entry);
            }
        }
    }

    /**
     * Links name-based exercise occurrences to the same deterministic identities used by the library.
     */
    private void linkLegacy(JsonArray exercises) {
        for (JsonElement value : exercises) {
            JsonObject exercise = value.getAsJsonObject();
            exercise.addProperty("exerciseId", WorkoutData.legacyId(exercise.get("name").getAsString()));
        }
    }
}
