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
                        .contains("Hello! I'm Staniz")),
                () -> assertEquals("Hello! I'm Staniz\nWhat can I do for you?",
                        ResponseFormatter.getGreetingMessage()),
                () -> assertEquals("Bye. Hope to see you again soon!",
                        ResponseFormatter.getFarewellMessage()));
    }

    @Test
    void taskStatusResponses_includeChangedTask() {
        Todo task = new Todo("borrow book");

        assertAll(
                () -> assertEquals("added: [T][ ] borrow book",
                        ResponseFormatter.formatTaskAdded(task)),
                () -> assertTrue(ResponseFormatter.formatTaskMarked(task)
                        .contains("[T][ ] borrow book")),
                () -> assertTrue(ResponseFormatter.formatTaskUnmarked(task)
                        .contains("[T][ ] borrow book")),
                () -> assertTrue(ResponseFormatter.formatTaskDeleted(task, 1)
                        .contains("Now you have 1 task in the list.")),
                () -> assertTrue(ResponseFormatter.formatTaskDeleted(task, 2)
                        .contains("Now you have 2 tasks in the list.")));
    }

    @Test
    void taskLists_useCorrectHeadersAndOneBasedNumbering() {
        TaskList tasks = new TaskList(List.of(
                new Todo("first"),
                new Todo("second")));

        String storedTasks = ResponseFormatter.formatTasks(tasks);
        String matchingTasks = ResponseFormatter.formatMatchingTasks(tasks);

        assertAll(
                () -> assertTrue(storedTasks.startsWith("Here are the tasks in your list:")),
                () -> assertTrue(storedTasks.contains("1.[T][ ] first")),
                () -> assertTrue(storedTasks.contains("2.[T][ ] second")),
                () -> assertTrue(matchingTasks.startsWith(
                        "Here are the matching tasks in your list:")));
    }
}
