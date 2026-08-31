package staniz;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import staniz.command.CommandResult;
import staniz.storage.Storage;

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

    /**
     * Runs the real entry point in a child JVM so its console and data file are isolated.
     */
    @Test
    void main_withCommandsProcessesInputPersistsTasksAndExits() throws Exception {
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
            process.getOutputStream().write("todo test gradle\nlist\nbye\n".getBytes(UTF_8));
            process.getOutputStream().close();
            if (!process.waitFor(10, SECONDS)) {
                fail("Staniz did not exit within 10 seconds");
            }

            String output = new String(process.getInputStream().readAllBytes(), UTF_8);
            assertAll(
                    () -> assertEquals(0, process.exitValue()),
                    () -> assertTrue(output.contains(
                            "Staniz here. Let's get your tasks into fighting shape.")),
                    () -> assertTrue(output.contains("Good. Another objective locked in:")),
                    () -> assertTrue(output.contains("  [T][ ] test gradle")),
                    () -> assertTrue(output.contains("1.[T][ ] test gradle")),
                    () -> assertTrue(output.contains("Session complete. Stay disciplined.")),
                    () -> assertEquals("T | 0 | test gradle",
                            java.nio.file.Files.readString(
                                    temporaryDirectory.resolve("data/staniz.txt"), UTF_8).strip()));
        } finally {
            process.destroyForcibly();
        }
    }
}
