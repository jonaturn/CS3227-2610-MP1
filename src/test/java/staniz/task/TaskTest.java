package staniz.task;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests completion behavior shared by all task types.
 */
class TaskTest {

    @Test
    void markAndUnmark_updateStatusIconDisplayAndPersistentState() {
        Task task = new Todo("read book");

        assertAll(
                () -> assertEquals(" ", task.getStatusIcon()),
                () -> assertEquals("[T][ ] read book", task.toString()),
                () -> assertEquals("T | 0 | read book", task.toDataString()));

        task.markAsDone();
        assertAll(
                () -> assertEquals("X", task.getStatusIcon()),
                () -> assertEquals("[T][X] read book", task.toString()),
                () -> assertEquals("T | 1 | read book", task.toDataString()));

        task.markAsNotDone();
        assertAll(
                () -> assertEquals(" ", task.getStatusIcon()),
                () -> assertEquals("[T][ ] read book", task.toString()),
                () -> assertEquals("T | 0 | read book", task.toDataString()));
    }

    @Test
    void hasDescriptionContaining_matchesCaseSensitiveSubstringsOnlyInDescription() {
        Task task = new Todo("Read library book");

        assertAll(
                () -> assertTrue(task.hasDescriptionContaining("library")),
                () -> assertTrue(task.hasDescriptionContaining("book")),
                () -> assertFalse(task.hasDescriptionContaining("Library")));
    }
}
