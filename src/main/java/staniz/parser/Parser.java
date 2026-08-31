package staniz.parser;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import staniz.command.CommandType;
import staniz.exception.StanizException;
import staniz.task.DateParser;
import staniz.task.Deadline;
import staniz.task.Event;
import staniz.task.Todo;

/**
 * Parses and validates commands entered by the user.
 */
public final class Parser {
    private static final String DEADLINE_PARAMETER = "/by";
    private static final String EMPTY_INPUT_MESSAGE = "Form check: enter a command.";
    private static final String EXAMPLE_DATE = "2019-12-02";
    private static final String EVENT_FROM_PARAMETER = "/from";
    private static final String EVENT_TO_PARAMETER = "/to";
    private static final Pattern DEADLINE_PARAMETER_PATTERN = createParameterPattern(DEADLINE_PARAMETER);
    private static final Pattern EVENT_FROM_PARAMETER_PATTERN = createParameterPattern(EVENT_FROM_PARAMETER);
    private static final Pattern EVENT_TO_PARAMETER_PATTERN = createParameterPattern(EVENT_TO_PARAMETER);
    private static final String UNKNOWN_COMMAND_MESSAGE = "Form check: I don't recognize that command. "
            + "Try todo, deadline, event, list, find, mark, unmark, delete, or bye.";

    private Parser() {
        // Utility class; prevent instantiation.
    }

    /**
     * Identifies the command invoked by the given input.
     *
     * @param input complete user input.
     * @return matching command type.
     * @throws StanizException if the input is blank or does not invoke a supported command.
     */
    public static CommandType parseCommandType(String input) throws StanizException {
        String normalizedInput = input.strip();
        if (normalizedInput.isBlank()) {
            throw new StanizException(EMPTY_INPUT_MESSAGE);
        }
        for (CommandType commandType : CommandType.values()) {
            if (commandType.matches(normalizedInput)) {
                return commandType;
            }
            if (!commandType.acceptsArguments()
                    && startsWithCommandKeyword(normalizedInput, commandType)) {
                String keyword = commandType.getKeyword();
                throw new StanizException("Form check: '" + keyword
                        + "' does not take arguments. Try: " + keyword);
            }
        }
        throw new StanizException(UNKNOWN_COMMAND_MESSAGE);
    }

    /**
     * Parses a to-do command into a task.
     *
     * @param input complete to-do command.
     * @return parsed to-do.
     * @throws StanizException if the description is missing.
     */
    public static Todo parseTodo(String input) throws StanizException {
        String description = getCommandArgument(input, CommandType.TODO);
        if (description.isBlank()) {
            throw new StanizException("Form check: a todo needs a description. Try: todo borrow book");
        }
        return new Todo(description);
    }

    /**
     * Extracts and validates the keyword supplied to a find command.
     *
     * @param input complete find command.
     * @return non-blank keyword to search for.
     * @throws StanizException if the keyword is missing.
     */
    public static String parseFindKeyword(String input) throws StanizException {
        String keyword = getCommandArgument(input, CommandType.FIND).strip();
        if (keyword.isBlank()) {
            throw new StanizException("Form check: 'find' needs a keyword. Try: find book");
        }
        return keyword;
    }

    /**
     * Parses a deadline command into a task.
     *
     * @param input complete deadline command.
     * @return parsed deadline.
     * @throws StanizException if a required field or valid date is missing.
     */
    public static Deadline parseDeadline(String input) throws StanizException {
        String arguments = getCommandArgument(input, CommandType.DEADLINE);
        ParameterLocation byParameter = findParameter(arguments, DEADLINE_PARAMETER_PATTERN);
        if (byParameter.count() == 0) {
            throw new StanizException("Form check: a deadline needs '/by'. "
                    + "Try: deadline return book /by " + EXAMPLE_DATE);
        }
        if (byParameter.count() > 1) {
            throw new StanizException("Form check: '/by' must be specified exactly once. "
                    + "Try: deadline return book /by " + EXAMPLE_DATE);
        }
        String description = arguments.substring(0, byParameter.start()).strip();
        String byText = arguments.substring(byParameter.end()).strip();
        if (description.isBlank()) {
            throw new StanizException("Form check: a deadline needs a description before '/by'.");
        }
        if (byText.isBlank()) {
            throw new StanizException("Form check: a deadline needs a due time after '/by'.");
        }
        LocalDate by = parseDate(byText, "deadline date");
        return new Deadline(description, by);
    }

    /**
     * Parses an event command into a task.
     *
     * @param input complete event command.
     * @return parsed event.
     * @throws StanizException if a required field, valid date, or valid date range is missing.
     */
    public static Event parseEvent(String input) throws StanizException {
        String arguments = getCommandArgument(input, CommandType.EVENT);
        ParameterLocation fromParameter = findParameter(arguments, EVENT_FROM_PARAMETER_PATTERN);
        ParameterLocation toParameter = findParameter(arguments, EVENT_TO_PARAMETER_PATTERN);
        if (fromParameter.count() > 1) {
            throw new StanizException("Form check: '/from' must be specified exactly once. "
                    + "Try: event meeting /from " + EXAMPLE_DATE + " /to 2019-12-03");
        }
        if (toParameter.count() > 1) {
            throw new StanizException("Form check: '/to' must be specified exactly once. "
                    + "Try: event meeting /from " + EXAMPLE_DATE + " /to 2019-12-03");
        }
        if (fromParameter.count() == 0) {
            throw new StanizException("Form check: an event needs '/from' and '/to'. "
                    + "Try: event meeting /from " + EXAMPLE_DATE + " /to 2019-12-03");
        }
        if (toParameter.count() == 0) {
            throw new StanizException("Form check: an event needs an end time after '/to'. "
                    + "Try: event meeting /from " + EXAMPLE_DATE + " /to 2019-12-03");
        }
        if (fromParameter.start() > toParameter.start()) {
            throw new StanizException("Form check: '/from' must appear before '/to'. "
                    + "Try: event meeting /from " + EXAMPLE_DATE + " /to 2019-12-03");
        }
        String description = arguments.substring(0, fromParameter.start()).strip();
        String fromText = arguments.substring(fromParameter.end(), toParameter.start()).strip();
        String toText = arguments.substring(toParameter.end()).strip();
        if (description.isBlank()) {
            throw new StanizException("Form check: an event needs a description before '/from'.");
        }
        if (fromText.isBlank()) {
            throw new StanizException("Form check: an event needs a start time after '/from'.");
        }
        if (toText.isBlank()) {
            throw new StanizException("Form check: an event needs an end time after '/to'.");
        }
        LocalDate from = parseDate(fromText, "event start date");
        LocalDate to = parseDate(toText, "event end date");
        if (from.isAfter(to)) {
            throw new StanizException("Form check: the event start date cannot be after the end date.");
        }
        return new Event(description, from, to);
    }

    /**
     * Parses and validates the one-based task number supplied to a task command.
     *
     * @param input complete user input.
     * @param commandType command type used to parse the number and provide error guidance.
     * @param taskCount number of tasks currently stored.
     * @return validated zero-based task index.
     * @throws StanizException if the task number is absent, malformed, or out of range.
     */
    public static int parseTaskIndex(String input, CommandType commandType, int taskCount)
            throws StanizException {
        assert commandType == CommandType.MARK
                || commandType == CommandType.UNMARK
                || commandType == CommandType.DELETE
                : "Task indexes are parsed only for mark, unmark, and delete commands";
        assert taskCount >= 0 : "Task count cannot be negative";

        String command = commandType.getKeyword();
        String taskNumberText = getCommandArgument(input, commandType);
        if (taskNumberText.isBlank()) {
            throw new StanizException("Form check: '" + command
                    + "' needs a task number. Try: " + command + " 1");
        }

        int taskNumber;
        try {
            taskNumber = Integer.parseInt(taskNumberText);
        } catch (NumberFormatException exception) {
            throw new StanizException("Form check: the task number must be a whole number.");
        }

        if (taskNumber < 1 || taskNumber > taskCount) {
            throw new StanizException("Form check: there is no task numbered " + taskNumber
                    + ". Your training plan currently has " + taskCount + " task(s).");
        }
        return taskNumber - 1;
    }

    /**
     * Parses a command date and translates parser failures into user-facing guidance.
     *
     * @param dateText date text supplied by the user.
     * @param fieldName field name used in the error message.
     * @return parsed calendar date.
     * @throws StanizException if the date is not a valid ISO calendar date.
     */
    private static LocalDate parseDate(String dateText, String fieldName) throws StanizException {
        try {
            return DateParser.parse(dateText);
        } catch (DateTimeParseException exception) {
            throw new StanizException("Form check: the " + fieldName
                    + " must use yyyy-MM-dd, e.g. " + EXAMPLE_DATE + ".");
        }
    }

    /**
     * Extracts the argument portion of a command, returning an empty string when absent.
     *
     * @param input complete user input.
     * @param commandType type of command whose argument should be extracted.
     * @return command argument or an empty string.
     */
    private static String getCommandArgument(String input, CommandType commandType) {
        String normalizedInput = input.strip();
        int keywordLength = commandType.getKeyword().length();
        assert normalizedInput.startsWith(commandType.getKeyword())
                : "Arguments must be extracted from the expected command type";
        return normalizedInput.substring(keywordLength).strip();
    }

    /**
     * Checks whether input starts with a complete command keyword followed by arguments.
     *
     * @param input stripped user input.
     * @param commandType command whose keyword is expected.
     * @return true if the keyword is followed by whitespace and more input.
     */
    private static boolean startsWithCommandKeyword(String input, CommandType commandType) {
        String keyword = commandType.getKeyword();
        return input.startsWith(keyword)
                && input.length() > keyword.length()
                && Character.isWhitespace(input.charAt(keyword.length()));
    }

    /**
     * Creates a pattern for a slash parameter surrounded by whitespace or input boundaries.
     *
     * @param parameter parameter token such as {@code /by}.
     * @return compiled token pattern.
     */
    private static Pattern createParameterPattern(String parameter) {
        return Pattern.compile("(?<!\\S)" + Pattern.quote(parameter) + "(?!\\S)");
    }

    /**
     * Finds the first location and total occurrence count of a parameter token.
     *
     * @param arguments command arguments to inspect.
     * @param parameterPattern token pattern to find.
     * @return first token location and number of occurrences.
     */
    private static ParameterLocation findParameter(String arguments, Pattern parameterPattern) {
        Matcher matcher = parameterPattern.matcher(arguments);
        if (!matcher.find()) {
            return new ParameterLocation(-1, -1, 0);
        }

        int start = matcher.start();
        int end = matcher.end();
        int count = 1;
        while (matcher.find()) {
            count++;
        }
        return new ParameterLocation(start, end, count);
    }

    /**
     * Describes the first occurrence and total count of a command parameter.
     *
     * @param start zero-based start of the first occurrence.
     * @param end zero-based exclusive end of the first occurrence.
     * @param count number of occurrences.
     */
    private record ParameterLocation(int start, int end, int count) {
    }
}
