import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Parses task dates from commands and formats them for display.
 */
public final class DateParser {
    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.ENGLISH);

    private DateParser() {
        // Utility class; prevent instantiation.
    }

    /**
     * Parses an ISO calendar date such as {@code 2019-12-02}.
     *
     * @param dateText date in yyyy-MM-dd format
     * @return parsed calendar date
     */
    public static LocalDate parse(String dateText) {
        return LocalDate.parse(dateText, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * Formats a date for user-facing task descriptions.
     *
     * @param date date to format
     * @return date such as {@code Dec 02 2019}
     */
    public static String format(LocalDate date) {
        return date.format(DISPLAY_FORMATTER);
    }
}
