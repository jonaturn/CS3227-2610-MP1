package staniz.ui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import staniz.task.TaskList;
import staniz.task.Todo;

/**
 * Tests interface-independent response formatting.
 */
class ResponseFormatterTest {

    @Test
    void greetingAndFarewell_returnConversationalMessages() {
        assertAll(
                () -> assertTrue(ResponseFormatter.getWelcomeMessage()
                        .contains("Staniz here. Let's get your tasks into fighting shape.")),
                () -> assertEquals("Staniz here. Let's get your tasks into fighting shape.",
                        ResponseFormatter.getGreetingMessage()),
                () -> assertEquals("Session complete. Stay disciplined.",
                        ResponseFormatter.getFarewellMessage()));
    }

    @Test
    void taskStatusResponses_includeChangedTask() {
        Todo task = new Todo("borrow book");

        assertAll(
                () -> assertEquals("Good. Another objective locked in:"
                                + System.lineSeparator() + "  [T][ ] borrow book",
                        ResponseFormatter.formatTaskAdded(task)),
                () -> assertTrue(ResponseFormatter.formatTaskMarked(task)
                        .contains("[T][ ] borrow book")),
                () -> assertTrue(ResponseFormatter.formatTaskUnmarked(task)
                        .contains("[T][ ] borrow book")),
                () -> assertTrue(ResponseFormatter.formatTaskDeleted(task, 1)
                        .contains("You have 1 objective left in the program.")),
                () -> assertTrue(ResponseFormatter.formatTaskDeleted(task, 2)
                        .contains("You have 2 objectives left in the program.")));
    }

    @Test
    void taskLists_useCorrectHeadersAndOneBasedNumbering() {
        TaskList tasks = new TaskList(List.of(
                new Todo("first"),
                new Todo("second")));

        String storedTasks = ResponseFormatter.formatTasks(tasks);
        String matchingTasks = ResponseFormatter.formatMatchingTasks(tasks);

        assertAll(
                () -> assertTrue(storedTasks.startsWith("Current training plan:")),
                () -> assertTrue(storedTasks.contains("1.[T][ ] first")),
                () -> assertTrue(storedTasks.contains("2.[T][ ] second")),
                () -> assertTrue(matchingTasks.startsWith("Matching objectives:")));
    }

    @Test
    void emptyListsAndDeletionCounts_useBoundaryAppropriateWording() {
        TaskList emptyTasks = new TaskList();
        Todo deletedTask = new Todo("finished");

        assertAll(
                () -> assertEquals("Current training plan:",
                        ResponseFormatter.formatTasks(emptyTasks)),
                () -> assertEquals("Matching objectives:",
                        ResponseFormatter.formatMatchingTasks(emptyTasks)),
                () -> assertTrue(ResponseFormatter.formatTaskDeleted(deletedTask, 0)
                        .endsWith("You have 0 objectives left in the program.")),
                () -> assertTrue(ResponseFormatter.formatTaskDeleted(deletedTask, 1)
                        .endsWith("You have 1 objective left in the program.")));
    }
}
