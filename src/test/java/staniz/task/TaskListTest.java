package staniz.task;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests ordered task ownership and all list mutations.
 */
class TaskListTest {

    @Test
    void constructor_defensivelyCopiesCallerCollection() {
        List<Task> sourceTasks = new ArrayList<>();
        sourceTasks.add(new Todo("original"));

        TaskList taskList = new TaskList(sourceTasks);
        sourceTasks.add(new Todo("external change"));

        assertAll(
                () -> assertEquals(1, taskList.getTaskCount()),
                () -> assertEquals("[T][ ] original", taskList.get(0).toString()));
    }

    @Test
    void addGetAndTaskCount_preserveInsertionOrder() {
        Task first = new Todo("first");
        Task second = new Todo("second");
        TaskList taskList = new TaskList();

        taskList.add(first);
        taskList.add(second);

        assertAll(
                () -> assertEquals(2, taskList.getTaskCount()),
                () -> assertSame(first, taskList.get(0)),
                () -> assertSame(second, taskList.get(1)));
    }

    @Test
    void markAndUnmark_changeAndReturnTheSelectedTask() {
        Task task = new Todo("selected");
        TaskList taskList = new TaskList(List.of(task));

        Task markedTask = taskList.markAsDone(0);
        assertAll(
                () -> assertSame(task, markedTask),
                () -> assertEquals("X", task.getStatusIcon()));

        Task unmarkedTask = taskList.markAsNotDone(0);
        assertAll(
                () -> assertSame(task, unmarkedTask),
                () -> assertEquals(" ", task.getStatusIcon()));
    }

    @Test
    void delete_returnsSelectedTaskAndRetainsRemainingOrder() {
        Task first = new Todo("first");
        Task second = new Todo("second");
        Task third = new Todo("third");
        TaskList taskList = new TaskList(List.of(first, second, third));

        Task deletedTask = taskList.delete(1);

        assertAll(
                () -> assertSame(second, deletedTask),
                () -> assertEquals(List.of(first, third), taskList.getTasks()));
    }

    @Test
    void getTasks_returnsImmutableSnapshot() {
        Task first = new Todo("first");
        TaskList taskList = new TaskList(List.of(first));

        List<Task> taskSnapshot = taskList.getTasks();
        taskList.add(new Todo("later"));

        assertAll(
                () -> assertEquals(List.of(first), taskSnapshot),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> taskSnapshot.add(new Todo("not allowed"))),
                () -> assertEquals(2, taskList.getTaskCount()));
    }
}
