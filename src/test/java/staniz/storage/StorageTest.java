package staniz.storage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import staniz.exception.StorageException;
import staniz.task.Deadline;
import staniz.task.Event;
import staniz.task.Task;
import staniz.task.TaskList;
import staniz.task.Todo;

/**
 * Tests persistence round trips and safeguards against malformed saved data.
 */
class StorageTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void load_missingFileReturnsEmptyList() throws StorageException {
        Storage storage = new Storage(temporaryDirectory.resolve("missing/staniz.txt"));

        assertTrue(storage.load().isEmpty());
    }

    @Test
    void saveAndLoad_allTaskTypesPreservesOrderStatusDatesAndEscapedText() throws Exception {
        Todo todo = new Todo("read C:\\docs | notes");
        todo.markAsDone();
        TaskList originalTasks = new TaskList(List.of(
                todo,
                new Deadline("submit report", LocalDate.of(2026, 9, 2)),
                new Event("conference", LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 5))));
        Path dataFile = temporaryDirectory.resolve("nested/data/staniz.txt");
        Storage storage = new Storage(dataFile);

        storage.save(originalTasks);
        List<Task> loadedTasks = storage.load();

        assertAll(
                () -> assertEquals(List.of(
                        "T | 1 | read C:\\\\docs \\| notes",
                        "D | 0 | submit report | 2026-09-02",
                        "E | 0 | conference | 2026-09-03 | 2026-09-05"),
                        Files.readAllLines(dataFile, UTF_8)),
                () -> assertEquals(
                        originalTasks.getTasks().stream().map(Task::toDataString).toList(),
                        loadedTasks.stream().map(Task::toDataString).toList()));
    }

    @Test
    void save_existingFileReplacesOldContents() throws Exception {
        Path dataFile = temporaryDirectory.resolve("staniz.txt");
        Storage storage = new Storage(dataFile);
        storage.save(new TaskList(List.of(new Todo("old one"), new Todo("old two"))));

        storage.save(new TaskList(List.of(new Todo("replacement"))));

        assertAll(
                () -> assertEquals(List.of("T | 0 | replacement"),
                        Files.readAllLines(dataFile, UTF_8)),
                () -> assertFalse(hasTemporaryDataFile(temporaryDirectory)));
    }

    @Test
    void load_malformedFieldsEscapesStatusesDatesAndRangesAreRejected() {
        assertAll(
                () -> assertInvalidData("T", "missing task type or completion status"),
                () -> assertInvalidData("X | 0 | unknown", "unknown task type 'X'"),
                () -> assertInvalidData("T | 2 | invalid status", "completion status must be 0 or 1"),
                () -> assertInvalidData("T | 0 | unfinished\\", "incomplete escape sequence"),
                () -> assertInvalidData("T | 0 | bad\\q", "unsupported escape sequence '\\q'"),
                () -> assertInvalidData("T | 0", "expected 3 fields but found 2"),
                () -> assertInvalidData("T | 0 |   ", "description cannot be empty"),
                () -> assertInvalidData("D | 0 | report", "expected 4 fields but found 3"),
                () -> assertInvalidData("D | 0 | report | 2026-02-30",
                        "deadline date must use yyyy-MM-dd"),
                () -> assertInvalidData("E | 0 | trip | 2026-09-03",
                        "expected 5 fields but found 4"),
                () -> assertInvalidData("E | 0 | trip | 2026-09-03 | 2026-09-02",
                        "event start date cannot be after the end date"));
    }

    @Test
    void load_invalidLaterLineReportsItsOneBasedLineNumber() throws Exception {
        Path dataFile = temporaryDirectory.resolve("line-number.txt");
        Files.write(dataFile, List.of("T | 0 | valid", "T | 9 | invalid"), UTF_8);

        StorageException exception = assertThrows(StorageException.class,
                () -> new Storage(dataFile).load());

        assertEquals("Saved task data is invalid on line 2: completion status must be 0 or 1.",
                exception.getMessage());
    }

    @Test
    void load_directoryInsteadOfFileReportsReadFailureWithCause() throws Exception {
        Path directory = temporaryDirectory.resolve("tasks-directory");
        Files.createDirectory(directory);

        StorageException exception = assertThrows(StorageException.class,
                () -> new Storage(directory).load());

        assertAll(
                () -> assertEquals("I couldn't read saved tasks from " + directory + ".",
                        exception.getMessage()),
                () -> assertTrue(exception.getCause() instanceof IOException));
    }

    @Test
    void save_directoryInsteadOfFileReportsWriteFailureWithCause() throws Exception {
        Path directory = temporaryDirectory.resolve("tasks-directory");
        Files.createDirectory(directory);

        StorageException exception = assertThrows(StorageException.class,
                () -> new Storage(directory).save(new TaskList(new Todo("task"))));

        assertAll(
                () -> assertEquals("I couldn't save tasks to " + directory + ".",
                        exception.getMessage()),
                () -> assertTrue(exception.getCause() instanceof IOException),
                () -> assertFalse(hasTemporaryDataFile(temporaryDirectory)));
    }

    @Test
    void save_fileSystemRootReportsWriteFailureBeforeCreatingTemporaryFile() {
        Path fileSystemRoot = temporaryDirectory.getRoot();

        StorageException exception = assertThrows(StorageException.class,
                () -> new Storage(fileSystemRoot).save(new TaskList(new Todo("task"))));

        assertAll(
                () -> assertEquals("I couldn't save tasks to " + fileSystemRoot + ".",
                        exception.getMessage()),
                () -> assertTrue(exception.getCause() instanceof IOException));
    }

    private void assertInvalidData(String line, String expectedProblem) throws Exception {
        Path dataFile = temporaryDirectory.resolve("invalid.txt");
        Files.writeString(dataFile, line, UTF_8);
        StorageException exception = assertThrows(StorageException.class,
                () -> new Storage(dataFile).load());
        assertEquals("Saved task data is invalid on line 1: " + expectedProblem + ".",
                exception.getMessage());
    }

    /**
     * Checks whether an interrupted atomic save left a sibling temporary file behind.
     *
     * @param directory directory containing the configured data path.
     * @return true if a Staniz temporary data file remains.
     * @throws IOException if the directory cannot be inspected.
     */
    private boolean hasTemporaryDataFile(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            return files.anyMatch(path -> path.getFileName().toString().startsWith(".staniz-"));
        }
    }
}
