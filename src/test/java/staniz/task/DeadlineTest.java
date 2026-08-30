package staniz.task;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Tests deadline persistence and display formats.
 */
class DeadlineTest {

    @Test
    void formats_includeIsoStorageDateAndFriendlyDisplayDate() {
        Deadline deadline = new Deadline("submit report", LocalDate.of(2026, 9, 2));
        deadline.markAsDone();

        assertAll(
                () -> assertEquals("D | 1 | submit report | 2026-09-02",
                        deadline.toDataString()),
                () -> assertEquals("[D][X] submit report (by: Sep 02 2026)",
                        deadline.toString()));
    }
}
