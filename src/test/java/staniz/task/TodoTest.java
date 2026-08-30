package staniz.task;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests to-do display and serialization formats.
 */
class TodoTest {

    @Test
    void toDataString_escapesBackslashesAndFieldSeparators() {
        Todo todo = new Todo("read C:\\docs | notes");

        assertEquals("T | 0 | read C:\\\\docs \\| notes", todo.toDataString());
    }

    @Test
    void toString_includesTypeStatusAndDescription() {
        assertEquals("[T][ ] borrow book", new Todo("borrow book").toString());
    }
}
