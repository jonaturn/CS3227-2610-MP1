package staniz.task;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Tests event persistence and display formats.
 */
class EventTest {

    @Test
    void formats_includeIsoStorageRangeAndFriendlyDisplayRange() {
        Event event = new Event("conference", LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 9, 4));

        assertAll(
                () -> assertEquals("E | 0 | conference | 2026-09-02 | 2026-09-04",
                        event.toDataString()),
                () -> assertEquals(
                        "[E][ ] conference (from: Sep 02 2026 to: Sep 04 2026)",
                        event.toString()));
    }
}
