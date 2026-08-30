package staniz.ui;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import staniz.task.Deadline;
import staniz.task.Task;
import staniz.task.TaskList;
import staniz.task.Todo;

/**
 * Tests console input and every user-visible UI response.
 */
class UiTest {
    private static final String SEPARATOR =
            "____________________________________________________________";

    private InputStream originalInput;
    private PrintStream originalOutput;
    private ByteArrayOutputStream capturedOutput;

    @BeforeEach
    void redirectStandardStreams() {
        originalInput = System.in;
        originalOutput = System.out;
        capturedOutput = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOutput, true, UTF_8));
    }

    @AfterEach
    void restoreStandardStreams() {
        System.setIn(originalInput);
        System.setOut(originalOutput);
    }

    @Test
    void hasNextCommandAndReadCommand_consumeCompleteInputLines() {
        try (Ui ui = createUi("todo first\nlist\n")) {
            assertTrue(ui.hasNextCommand());
            assertEquals("todo first", ui.readCommand());
            assertTrue(ui.hasNextCommand());
            assertEquals("list", ui.readCommand());
            assertFalse(ui.hasNextCommand());
        }
    }

    @Test
    void showResponse_enclosesTextWithSeparators() {
        try (Ui ui = createUi("")) {
            ui.showResponse("message");
        }

        String lineSeparator = System.lineSeparator();
        assertEquals(SEPARATOR + lineSeparator + "message" + lineSeparator
                + SEPARATOR + lineSeparator, output());
    }

    @Test
    void showWelcomeAndFarewell_displayExpectedMessages() {
        try (Ui ui = createUi("")) {
            ui.showWelcome();
            ui.showFarewell();
        }

        assertAll(
                () -> assertTrue(output().contains("____ _____  _    _   _ ___ _____")),
                () -> assertTrue(output().contains("Hello! I'm Staniz")),
                () -> assertTrue(output().contains("What can I do for you?")),
                () -> assertTrue(output().contains("Bye. Hope to see you again soon!")));
    }

    @Test
    void showTasks_numbersTasksFromOneInStoredOrder() {
        TaskList taskList = new TaskList(List.of(
                new Todo("first"),
                new Deadline("second", LocalDate.of(2026, 9, 2))));

        try (Ui ui = createUi("")) {
            ui.showTasks(taskList);
        }

        assertAll(
                () -> assertTrue(output().contains("Here are the tasks in your list:")),
                () -> assertTrue(output().contains("1.[T][ ] first")),
                () -> assertTrue(output().contains("2.[D][ ] second (by: Sep 02 2026)")));
    }

    @Test
    void taskConfirmationMethods_displayTaskStatusAndCounts() {
        Task task = new Todo("read book");
        try (Ui ui = createUi("")) {
            ui.showTaskAdded(task);
            task.markAsDone();
            ui.showTaskMarked(task);
            task.markAsNotDone();
            ui.showTaskUnmarked(task);
            ui.showTaskDeleted(task, 1);
            ui.showTaskDeleted(task, 2);
        }

        assertAll(
                () -> assertTrue(output().contains("added: [T][ ] read book")),
                () -> assertTrue(output().contains(
                        "Nice! I've marked this task as done:" + System.lineSeparator()
                                + "  [T][X] read book")),
                () -> assertTrue(output().contains(
                        "OK, I've marked this task as not done yet:" + System.lineSeparator()
                                + "  [T][ ] read book")),
                () -> assertTrue(output().contains("Now you have 1 task in the list.")),
                () -> assertTrue(output().contains("Now you have 2 tasks in the list.")));
    }

    private Ui createUi(String input) {
        System.setIn(new ByteArrayInputStream(input.getBytes(UTF_8)));
        return new Ui();
    }

    private String output() {
        return capturedOutput.toString(UTF_8);
    }
}
