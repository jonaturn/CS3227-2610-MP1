import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and saves Staniz tasks using a local text file.
 */
public class Storage {
    private static final Path DEFAULT_FILE_PATH = Path.of("data", "staniz.txt");

    private final Path filePath;

    /**
     * Creates storage at the default OS-independent relative path.
     */
    public Storage() {
        this(DEFAULT_FILE_PATH);
    }

    /**
     * Creates storage at a caller-provided path.
     *
     * @param filePath file used to persist tasks
     */
    public Storage(Path filePath) {
        this.filePath = filePath;
    }

    /**
     * Loads tasks from disk in their saved order.
     *
     * @return loaded tasks, or an empty list when the file does not exist
     * @throws StorageException if the file cannot be read or contains invalid data
     */
    public List<Task> load() throws StorageException {
        if (Files.notExists(filePath)) {
            return new ArrayList<>();
        }

        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            List<Task> tasks = new ArrayList<>();
            for (int index = 0; index < lines.size(); index++) {
                tasks.add(parseTask(lines.get(index), index + 1));
            }
            return tasks;
        } catch (IOException exception) {
            throw new StorageException("I couldn't read saved tasks from " + filePath + ".", exception);
        }
    }

    /**
     * Replaces the data file with the current ordered task list.
     *
     * @param tasks tasks to persist
     * @throws StorageException if directories or the data file cannot be written
     */
    public void save(List<Task> tasks) throws StorageException {
        try {
            Path parentDirectory = filePath.getParent();
            if (parentDirectory != null) {
                Files.createDirectories(parentDirectory);
            }

            List<String> lines = tasks.stream()
                    .map(Task::toDataString)
                    .toList();
            Files.write(filePath, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new StorageException("I couldn't save tasks to " + filePath + ".", exception);
        }
    }

    /**
     * Reconstructs one task from a validated data line.
     *
     * @param line serialized task
     * @param lineNumber one-based line number used in error messages
     * @return reconstructed task
     * @throws StorageException if the line is malformed
     */
    private Task parseTask(String line, int lineNumber) throws StorageException {
        List<String> fields = splitDataFields(line, lineNumber);
        if (fields.size() < 2) {
            throw invalidData(lineNumber, "missing task type or completion status");
        }

        String taskType = fields.get(0);
        boolean isDone = parseCompletionStatus(fields.get(1), lineNumber);
        Task task;
        switch (taskType) {
        case "T":
            requireFieldCount(fields, 3, lineNumber);
            task = new Todo(requireValue(fields.get(2), "description", lineNumber));
            break;
        case "D":
            requireFieldCount(fields, 4, lineNumber);
            task = new Deadline(
                    requireValue(fields.get(2), "description", lineNumber),
                    requireValue(fields.get(3), "deadline", lineNumber));
            break;
        case "E":
            requireFieldCount(fields, 5, lineNumber);
            task = new Event(
                    requireValue(fields.get(2), "description", lineNumber),
                    requireValue(fields.get(3), "start time", lineNumber),
                    requireValue(fields.get(4), "end time", lineNumber));
            break;
        default:
            throw invalidData(lineNumber, "unknown task type '" + taskType + "'");
        }

        if (isDone) {
            task.markAsDone();
        }
        return task;
    }

    /**
     * Splits an escaped persistent line into decoded fields.
     *
     * @param line serialized task line
     * @param lineNumber one-based line number used in error messages
     * @return decoded task fields
     * @throws StorageException if an escape sequence is incomplete
     */
    private List<String> splitDataFields(String line, int lineNumber) throws StorageException {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean isEscaped = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (isEscaped) {
                if (character != '\\' && character != '|') {
                    throw invalidData(lineNumber, "unsupported escape sequence '\\" + character + "'");
                }
                currentField.append(character);
                isEscaped = false;
            } else if (character == '\\') {
                isEscaped = true;
            } else if (character == '|') {
                fields.add(currentField.toString().strip());
                currentField = new StringBuilder();
            } else {
                currentField.append(character);
            }
        }

        if (isEscaped) {
            throw invalidData(lineNumber, "incomplete escape sequence");
        }
        fields.add(currentField.toString().strip());
        return fields;
    }

    /**
     * Parses the persisted completion marker.
     *
     * @param value stored completion marker
     * @param lineNumber one-based line number used in error messages
     * @return true for 1 and false for 0
     * @throws StorageException if the marker is neither 0 nor 1
     */
    private boolean parseCompletionStatus(String value, int lineNumber) throws StorageException {
        if ("1".equals(value)) {
            return true;
        }
        if ("0".equals(value)) {
            return false;
        }
        throw invalidData(lineNumber, "completion status must be 0 or 1");
    }

    /**
     * Checks that a task type has exactly the expected number of fields.
     *
     * @param fields parsed fields
     * @param expectedCount required field count
     * @param lineNumber one-based line number used in error messages
     * @throws StorageException if the count differs
     */
    private void requireFieldCount(List<String> fields, int expectedCount, int lineNumber)
            throws StorageException {
        if (fields.size() != expectedCount) {
            throw invalidData(lineNumber, "expected " + expectedCount
                    + " fields but found " + fields.size());
        }
    }

    /**
     * Checks that a required persisted field contains text.
     *
     * @param value field value
     * @param fieldName field name used in error messages
     * @param lineNumber one-based line number used in error messages
     * @return the validated value
     * @throws StorageException if the field is blank
     */
    private String requireValue(String value, String fieldName, int lineNumber) throws StorageException {
        if (value.isBlank()) {
            throw invalidData(lineNumber, fieldName + " cannot be empty");
        }
        return value;
    }

    /**
     * Creates a consistently formatted corrupted-data exception.
     *
     * @param lineNumber one-based invalid line number
     * @param problem explanation of the invalid content
     * @return storage exception containing line-specific guidance
     */
    private StorageException invalidData(int lineNumber, String problem) {
        return new StorageException("Saved task data is invalid on line " + lineNumber + ": " + problem + ".");
    }
}
