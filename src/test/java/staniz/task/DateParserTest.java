package staniz.task;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

/**
 * Tests strict ISO date parsing and stable user-facing formatting.
 */
class DateParserTest {

    @Test
    void parse_validLeapDayReturnsCalendarDate() {
        assertEquals(LocalDate.of(2024, 2, 29), DateParser.parse("2024-02-29"));
    }

    @Test
    void parse_invalidFormatAndImpossibleDateAreRejected() {
        assertAll(
                () -> assertThrows(DateTimeParseException.class,
                        () -> DateParser.parse("29-02-2024")),
                () -> assertThrows(DateTimeParseException.class,
                        () -> DateParser.parse("2023-02-29")));
    }

    @Test
    void format_usesEnglishAbbreviatedMonthAndPaddedDay() {
        assertEquals("Sep 02 2026", DateParser.format(LocalDate.of(2026, 9, 2)));
    }
}
