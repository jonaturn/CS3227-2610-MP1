package staniz;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import staniz.command.CommandResult;
import staniz.exception.StanizException;
import staniz.exception.StorageException;
import staniz.storage.Storage;
import staniz.task.TaskList;

/**
 * Tests the complete Staniz application loop in an isolated working directory.
 */
class StanizTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void executeCommand_withMutatingAndExitCommandsReturnsResponsesAndPersists() throws Exception {
        Path dataFile = temporaryDirectory.resolve("staniz.txt");
        Staniz staniz = new Staniz(new Storage(dataFile));

        CommandResult addedResult = staniz.executeCommand("todo test backend");
        CommandResult listResult = staniz.executeCommand("list");
        CommandResult exitResult = staniz.executeCommand("bye");

        assertAll(
                () -> assertEquals("Good. Another objective locked in:"
                                + System.lineSeparator() + "  [T][ ] test backend",
                        addedResult.getResponse()),
                () -> assertFalse(addedResult.shouldExit()),
                () -> assertTrue(listResult.getResponse().contains("1.[T][ ] test backend")),
                () -> assertFalse(listResult.shouldExit()),
                () -> assertEquals("Session complete. Stay disciplined.",
                        exitResult.getResponse()),
                () -> assertTrue(exitResult.shouldExit()),
                () -> assertEquals("T | 0 | test backend",
                        Files.readString(dataFile, UTF_8).strip()));
    }

    @Test
    void executeCommand_withEveryNonExitCommandDispatchesAndPersists() throws Exception {
        Path dataFile = temporaryDirectory.resolve("staniz.txt");
        Staniz staniz = new Staniz(new Storage(dataFile));

        staniz.executeCommand("todo borrow book");
        staniz.executeCommand("deadline submit report /by 2026-09-02");
        staniz.executeCommand("event conference /from 2026-09-03 /to 2026-09-05");
        CommandResult markedResult = staniz.executeCommand("mark 2");
        CommandResult unmarkedResult = staniz.executeCommand("unmark 2");
        CommandResult findResult = staniz.executeCommand("find report");
        CommandResult deletedResult = staniz.executeCommand("delete 1");
        CommandResult listResult = staniz.executeCommand("list");

        assertAll(
                () -> assertTrue(markedResult.getResponse()
                        .contains("[D][X] submit report (by: Sep 02 2026)")),
                () -> assertTrue(unmarkedResult.getResponse()
                        .contains("[D][ ] submit report (by: Sep 02 2026)")),
                () -> assertTrue(findResult.getResponse()
                        .contains("1.[D][ ] submit report (by: Sep 02 2026)")),
                () -> assertTrue(deletedResult.getResponse().contains("[T][ ] borrow book")),
                () -> assertFalse(listResult.getResponse().contains("borrow book")),
                () -> assertTrue(listResult.getResponse()
                        .contains("1.[D][ ] submit report (by: Sep 02 2026)")),
                () -> assertTrue(listResult.getResponse()
                        .contains("2.[E][ ] conference (from: Sep 03 2026 to: Sep 05 2026)")),
                () -> assertEquals(List.of(
                        "D | 0 | submit report | 2026-09-02",
                        "E | 0 | conference | 2026-09-03 | 2026-09-05"),
                        Files.readAllLines(dataFile, UTF_8)));
    }

    @Test
    void executeCommand_withInvalidCommandLeavesPersistedTasksUnchanged() throws Exception {
        Path dataFile = temporaryDirectory.resolve("staniz.txt");
        Staniz staniz = new Staniz(new Storage(dataFile));
        staniz.executeCommand("todo preserved task");
        String savedTasksBeforeError = Files.readString(dataFile, UTF_8);

        StanizException exception = assertThrows(StanizException.class,
                () -> staniz.executeCommand("unknown command"));

        assertAll(
                () -> assertTrue(exception.getMessage().startsWith("Form check:")),
                () -> assertEquals(savedTasksBeforeError, Files.readString(dataFile, UTF_8)),
                () -> assertTrue(staniz.executeCommand("list").getResponse()
                        .contains("1.[T][ ] preserved task")));
    }

    @Test
    void executeCommand_withFormatErrorsRecoversWithoutChangingTasks() throws Exception {
        Path dataFile = temporaryDirectory.resolve("staniz.txt");
        Staniz staniz = new Staniz(new Storage(dataFile));
        staniz.executeCommand("todo preserved task");
        String savedTasksBeforeErrors = Files.readString(dataFile, UTF_8);

        assertAll(
                () -> assertThrows(StanizException.class,
                        () -> staniz.executeCommand("list unexpected")),
                () -> assertThrows(StanizException.class,
                        () -> staniz.executeCommand(
                                "deadline duplicate /by 2026-09-01 /by 2026-09-02")),
                () -> assertThrows(StanizException.class,
                        () -> staniz.executeCommand(
                                "event reversed /to 2026-09-02 /from 2026-09-01")));

        assertAll(
                () -> assertEquals(savedTasksBeforeErrors, Files.readString(dataFile, UTF_8)),
                () -> assertTrue(staniz.executeCommand("list").getResponse()
                        .contains("1.[T][ ] preserved task")));
    }

    @Test
    void executeCommand_whenSavingFailsPropagatesStorageFailure() throws Exception {
        Path dataFile = temporaryDirectory.resolve("staniz.txt");
        Storage failingStorage = new Storage(dataFile) {
            @Override
            public void save(TaskList taskList) throws StorageException {
                throw new StorageException("Simulated save failure.");
            }
        };
        Staniz staniz = new Staniz(failingStorage);

        StorageException exception = assertThrows(StorageException.class,
                () -> staniz.executeCommand("todo cannot be saved"));

        assertAll(
                () -> assertEquals("Simulated save failure.", exception.getMessage()),
                () -> assertFalse(Files.exists(dataFile)));
    }

    /**
     * Runs the real entry point in a child JVM so its console and data file are isolated.
     */
    @Test
    void main_withCommandsProcessesInputPersistsTasksAndExits() throws Exception {
        MainRun run = runMain("todo test gradle\nlist\nbye\n");

        assertAll(
                () -> assertEquals(0, run.exitCode()),
                () -> assertTrue(run.output().contains(
                        "Staniz here. Let's get your tasks into fighting shape.")),
                () -> assertTrue(run.output().contains("Good. Another objective locked in:")),
                () -> assertTrue(run.output().contains("  [T][ ] test gradle")),
                () -> assertTrue(run.output().contains("1.[T][ ] test gradle")),
                () -> assertTrue(run.output().contains("Session complete. Stay disciplined.")),
                () -> assertEquals("T | 0 | test gradle",
                        Files.readString(temporaryDirectory.resolve("data/staniz.txt"), UTF_8).strip()));
    }

    @Test
    void main_withEndOfInputDisplaysFarewellAndExits() throws Exception {
        MainRun run = runMain("list\n");

        assertAll(
                () -> assertEquals(0, run.exitCode()),
                () -> assertTrue(run.output().contains("Current training plan:")),
                () -> assertTrue(run.output().contains("Session complete. Stay disciplined.")));
    }

    @Test
    void main_withUnreadableStorageReportsFailureAndStops() throws Exception {
        Files.createDirectories(temporaryDirectory.resolve("data/staniz.txt"));

        MainRun run = runMain("");

        assertAll(
                () -> assertEquals(0, run.exitCode()),
                () -> assertTrue(run.output().contains(
                        "System check: I couldn't read saved tasks from")),
                () -> assertFalse(run.output().contains("Session complete. Stay disciplined.")));
    }

    @Test
    void main_withCorruptedStorageReportsValidationFailureAndStops() throws Exception {
        Path dataFile = temporaryDirectory.resolve("data/staniz.txt");
        Files.createDirectories(dataFile.getParent());
        Files.writeString(dataFile, "T | 9 | invalid status", UTF_8);

        MainRun run = runMain("");

        assertAll(
                () -> assertEquals(0, run.exitCode()),
                () -> assertTrue(run.output().contains("System check: Saved task data is invalid "
                        + "on line 1: completion status must be 0 or 1.")),
                () -> assertFalse(run.output().contains("Session complete. Stay disciplined.")));
    }

    @Test
    void main_afterInvalidCommandsRecoversAndPersistsValidCommand() throws Exception {
        MainRun run = runMain("todo\nunknown command\ntodo recovered task\nlist\nbye\n");

        assertAll(
                () -> assertEquals(0, run.exitCode()),
                () -> assertTrue(run.output().contains(
                        "Form check: a todo needs a description.")),
                () -> assertTrue(run.output().contains(
                        "Form check: I don't recognize that command.")),
                () -> assertTrue(run.output().contains("1.[T][ ] recovered task")),
                () -> assertEquals("T | 0 | recovered task",
                        Files.readString(temporaryDirectory.resolve("data/staniz.txt"), UTF_8).strip()));
    }

    /**
     * Runs the real entry point in an isolated child JVM and captures its console output.
     *
     * @param commands complete console input supplied before end-of-file.
     * @return child-process exit code and merged console output.
     */
    private MainRun runMain(String commands) throws Exception {
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
        Path classesDirectory = Path.of(Staniz.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        Process process = new ProcessBuilder(javaExecutable.toString(), "-cp",
                classesDirectory.toString(), Staniz.class.getName())
                .directory(temporaryDirectory.toFile())
                .redirectErrorStream(true)
                .start();

        try {
            process.getOutputStream().write(commands.getBytes(UTF_8));
            process.getOutputStream().close();
            if (!process.waitFor(10, SECONDS)) {
                fail("Staniz did not exit within 10 seconds");
            }
            return new MainRun(process.exitValue(),
                    new String(process.getInputStream().readAllBytes(), UTF_8));
        } finally {
            process.destroyForcibly();
        }
    }

    /** Result captured from one isolated command-line application run. */
    private record MainRun(int exitCode, String output) {
    }
}
