package staniz.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import staniz.exception.StorageException;
import staniz.task.DateParser;
import staniz.task.Deadline;
import staniz.task.Event;
import staniz.task.Task;
import staniz.task.TaskList;
import staniz.task.Todo;

/**
 * Loads and saves Staniz tasks using a local text file.
 */
public class Storage {
    private static final Path DEFAULT_FILE_PATH = Path.of("data", "staniz.txt");
    private static final String TEMPORARY_FILE_PREFIX = ".staniz-";
    private static final String TEMPORARY_FILE_SUFFIX = ".tmp";

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
     * @param filePath file used to persist tasks.
     */
    public Storage(Path filePath) {
        this.filePath = filePath;
    }

    /**
     * Loads tasks from disk in their saved order.
     *
     * @return loaded tasks, or an empty list when the file does not exist.
     * @throws StorageException if the file cannot be read or contains invalid data.
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
     * @param taskList tasks to persist.
     * @throws StorageException if directories or the data file cannot be written.
     */
    public void save(TaskList taskList) throws StorageException {
        Path temporaryFile = null;
        try {
            Path absoluteFilePath = filePath.toAbsolutePath().normalize();
            Path parentDirectory = absoluteFilePath.getParent();
            if (parentDirectory == null) {
                throw new IOException("The data file has no parent directory.");
            }
            Files.createDirectories(parentDirectory);

            List<String> lines = taskList.getTasks().stream()
                    .map(Task::toDataString)
                    .toList();
            temporaryFile = Files.createTempFile(
                    parentDirectory, TEMPORARY_FILE_PREFIX, TEMPORARY_FILE_SUFFIX);
            Files.write(temporaryFile, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            replaceDataFile(temporaryFile, absoluteFilePath);
        } catch (IOException exception) {
            deleteTemporaryFile(temporaryFile, exception);
            throw new StorageException("I couldn't save tasks to " + filePath + ".", exception);
        }
    }

    /**
     * Replaces the data file atomically when the file system supports it.
     * A normal replacement is used as a compatibility fallback while still ensuring
     * that the complete temporary file is written before the existing file is touched.
     *
     * @param temporaryFile fully written temporary data file.
     * @param absoluteFilePath destination data file.
     * @throws IOException if neither replacement strategy succeeds.
     */
    private void replaceDataFile(Path temporaryFile, Path absoluteFilePath) throws IOException {
        try {
            Files.move(temporaryFile, absoluteFilePath,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, absoluteFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Removes an incomplete temporary file without hiding the original save failure.
     *
     * @param temporaryFile temporary file, or {@code null} if creation failed.
     * @param saveFailure original file-system failure.
     */
    private void deleteTemporaryFile(Path temporaryFile, IOException saveFailure) {
        if (temporaryFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException cleanupFailure) {
            saveFailure.addSuppressed(cleanupFailure);
        }
    }

    /**
     * Reconstructs one task from a validated data line.
     *
     * @param line serialized task.
     * @param lineNumber one-based line number used in error messages.
     * @return reconstructed task.
     * @throws StorageException if the line is malformed.
     */
    private Task parseTask(String line, int lineNumber) throws StorageException {
        List<String> fields = splitDataFields(line, lineNumber);
        if (fields.size() < 2) {
            throw createInvalidDataException(
                    lineNumber, "missing task type or completion status");
        }

        String taskType = fields.get(0);
        boolean isDone = isCompleted(fields.get(1), lineNumber);
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
                        parseDate(fields.get(3), "deadline date", lineNumber));
                break;
            case "E":
                requireFieldCount(fields, 5, lineNumber);
                LocalDate from = parseDate(fields.get(3), "event start date", lineNumber);
                LocalDate to = parseDate(fields.get(4), "event end date", lineNumber);
                if (from.isAfter(to)) {
                    throw createInvalidDataException(
                            lineNumber, "event start date cannot be after the end date");
                }
                task = new Event(
                        requireValue(fields.get(2), "description", lineNumber),
                        from,
                        to);
                break;
            default:
                throw createInvalidDataException(lineNumber, "unknown task type '" + taskType + "'");
        }

        if (isDone) {
            task.markAsDone();
        }
        return task;
    }

    /**
     * Splits an escaped persistent line into decoded fields.
     *
     * @param line serialized task line.
     * @param lineNumber one-based line number used in error messages.
     * @return decoded task fields.
     * @throws StorageException if an escape sequence is incomplete.
     */
    private List<String> splitDataFields(String line, int lineNumber) throws StorageException {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean isEscaped = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (isEscaped) {
                if (character != '\\' && character != '|') {
                    throw createInvalidDataException(
                            lineNumber, "unsupported escape sequence '\\" + character + "'");
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
            throw createInvalidDataException(lineNumber, "incomplete escape sequence");
        }
        fields.add(currentField.toString().strip());
        return fields;
    }

    /**
     * Parses the persisted completion marker.
     *
     * @param value stored completion marker.
     * @param lineNumber one-based line number used in error messages.
     * @return true for 1 and false for 0.
     * @throws StorageException if the marker is neither 0 nor 1.
     */
    private boolean isCompleted(String value, int lineNumber) throws StorageException {
        if ("1".equals(value)) {
            return true;
        }
        if ("0".equals(value)) {
            return false;
        }
        throw createInvalidDataException(lineNumber, "completion status must be 0 or 1");
    }

    /**
     * Parses a required ISO date from persistent data.
     *
     * @param value persisted date text.
     * @param fieldName field name used in errors.
     * @param lineNumber one-based line number used in errors.
     * @return parsed calendar date.
     * @throws StorageException if the value is blank or is not a valid ISO date.
     */
    private LocalDate parseDate(String value, String fieldName, int lineNumber) throws StorageException {
        String requiredValue = requireValue(value, fieldName, lineNumber);
        try {
            return DateParser.parse(requiredValue);
        } catch (DateTimeParseException exception) {
            throw createInvalidDataException(lineNumber, fieldName + " must use yyyy-MM-dd");
        }
    }

    /**
     * Checks that a task type has exactly the expected number of fields.
     *
     * @param fields parsed fields.
     * @param expectedCount required field count.
     * @param lineNumber one-based line number used in error messages.
     * @throws StorageException if the count differs.
     */
    private void requireFieldCount(List<String> fields, int expectedCount, int lineNumber)
            throws StorageException {
        if (fields.size() != expectedCount) {
            throw createInvalidDataException(lineNumber, "expected " + expectedCount
                    + " fields but found " + fields.size());
        }
    }

    /**
     * Checks that a required persisted field contains text.
     *
     * @param value field value.
     * @param fieldName field name used in error messages.
     * @param lineNumber one-based line number used in error messages.
     * @return the validated value.
     * @throws StorageException if the field is blank.
     */
    private String requireValue(String value, String fieldName, int lineNumber) throws StorageException {
        if (value.isBlank()) {
            throw createInvalidDataException(lineNumber, fieldName + " cannot be empty");
        }
        return value;
    }

    /**
     * Creates a consistently formatted corrupted-data exception.
     *
     * @param lineNumber one-based invalid line number.
     * @param problem explanation of the invalid content.
     * @return storage exception containing line-specific guidance.
     */
    private StorageException createInvalidDataException(int lineNumber, String problem) {
        return new StorageException("Saved task data is invalid on line " + lineNumber + ": " + problem + ".");
    }
}
