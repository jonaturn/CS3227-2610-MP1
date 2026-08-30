package staniz.parser;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

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
    private static final String DEADLINE_SEPARATOR = " /by";
    private static final String EMPTY_INPUT_MESSAGE = "OOPS! Please enter a command.";
    private static final String EXAMPLE_DATE = "2019-12-02";
    private static final String EVENT_FROM_SEPARATOR = " /from";
    private static final String EVENT_TO_SEPARATOR = " /to";
    private static final String UNKNOWN_COMMAND_MESSAGE = "OOPS! I don't recognize that command. "
            + "Try todo, deadline, event, list, mark, unmark, delete, or bye.";

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
        if (input.isBlank()) {
            throw new StanizException(EMPTY_INPUT_MESSAGE);
        }
        for (CommandType commandType : CommandType.values()) {
            if (commandType.matches(input)) {
                return commandType;
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
            throw new StanizException("OOPS! A todo needs a description. Try: todo borrow book");
        }
        return new Todo(description);
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
        int separatorIndex = arguments.indexOf(DEADLINE_SEPARATOR);
        if (separatorIndex < 0) {
            throw new StanizException("OOPS! A deadline needs '/by'. "
                    + "Try: deadline return book /by " + EXAMPLE_DATE);
        }
        String description = arguments.substring(0, separatorIndex);
        String byText = arguments.substring(separatorIndex + DEADLINE_SEPARATOR.length()).strip();
        if (description.isBlank()) {
            throw new StanizException("OOPS! A deadline needs a description before '/by'.");
        }
        if (byText.isBlank()) {
            throw new StanizException("OOPS! A deadline needs a due time after '/by'.");
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
        int fromSeparatorIndex = arguments.indexOf(EVENT_FROM_SEPARATOR);
        if (fromSeparatorIndex < 0) {
            throw new StanizException("OOPS! An event needs '/from' and '/to'. "
                    + "Try: event meeting /from " + EXAMPLE_DATE + " /to 2019-12-03");
        }
        int toSeparatorIndex = arguments.indexOf(EVENT_TO_SEPARATOR, fromSeparatorIndex
                + EVENT_FROM_SEPARATOR.length());
        if (toSeparatorIndex < 0) {
            throw new StanizException("OOPS! An event needs an end time after '/to'. "
                    + "Try: event meeting /from " + EXAMPLE_DATE + " /to 2019-12-03");
        }
        String description = arguments.substring(0, fromSeparatorIndex);
        String fromText = arguments.substring(fromSeparatorIndex + EVENT_FROM_SEPARATOR.length(), toSeparatorIndex)
                .strip();
        String toText = arguments.substring(toSeparatorIndex + EVENT_TO_SEPARATOR.length()).strip();
        if (description.isBlank()) {
            throw new StanizException("OOPS! An event needs a description before '/from'.");
        }
        if (fromText.isBlank()) {
            throw new StanizException("OOPS! An event needs a start time after '/from'.");
        }
        if (toText.isBlank()) {
            throw new StanizException("OOPS! An event needs an end time after '/to'.");
        }
        LocalDate from = parseDate(fromText, "event start date");
        LocalDate to = parseDate(toText, "event end date");
        if (from.isAfter(to)) {
            throw new StanizException("OOPS! The event start date cannot be after the end date.");
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
        String command = commandType.getKeyword();
        String taskNumberText = getCommandArgument(input, commandType);
        if (taskNumberText.isBlank()) {
            throw new StanizException("OOPS! '" + command + "' needs a task number. Try: " + command + " 1");
        }

        int taskNumber;
        try {
            taskNumber = Integer.parseInt(taskNumberText);
        } catch (NumberFormatException exception) {
            throw new StanizException("OOPS! The task number must be a whole number.");
        }

        if (taskNumber < 1 || taskNumber > taskCount) {
            throw new StanizException("OOPS! There is no task numbered " + taskNumber
                    + ". Your list currently has " + taskCount + " task(s).");
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
            throw new StanizException("OOPS! The " + fieldName
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
        String commandPrefix = commandType.getArgumentPrefix();
        return input.length() < commandPrefix.length() ? "" : input.substring(commandPrefix.length());
    }
}
