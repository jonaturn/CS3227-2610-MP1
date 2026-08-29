import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Runs the Staniz personal assistant chatbot.
 */
public class Staniz {
    private static final String ADDED_MESSAGE_PREFIX = "added: ";
    private static final String DEADLINE_SEPARATOR = " /by";
    private static final String DELETED_MESSAGE = "Noted. I've removed this task:";
    private static final String EMPTY_INPUT_MESSAGE = "OOPS! Please enter a command.";
    private static final String EVENT_FROM_SEPARATOR = " /from";
    private static final String EVENT_TO_SEPARATOR = " /to";
    private static final String MARKED_MESSAGE = "Nice! I've marked this task as done:";
    private static final String UNKNOWN_COMMAND_MESSAGE = "OOPS! I don't recognize that command. "
            + "Try todo, deadline, event, list, mark, unmark, delete, or bye.";
    private static final String UNMARKED_MESSAGE = "OK, I've marked this task as not done yet:";
    private static final String SEPARATOR = "____________________________________________________________";
    private static final String TASK_LIST_HEADER = "Here are the tasks in your list:";
    private static final String BANNER = " ____ _____  _    _   _ ___ _____\n"
            + "/ ___|_   _|/ \\  | \\ | |_ _|__  /\n"
            + "\\___ \\ | | / _ \\ |  \\| || |  / /\n"
            + " ___) || |/ ___ \\| |\\  || | / /_\n"
            + "|____/ |_/_/   \\_\\_| \\_|___/____|";
    private static final String GREETING = "Hello! I'm Staniz\n"
            + "What can I do for you?";
    private static final String FAREWELL = "Bye. Hope to see you again soon!";

    /**
     * Starts Staniz and stores user entries until the exit command is entered.
     *
     * @param args command-line arguments; not used
     */
    public static void main(String[] args) {
        List<Task> tasks = new ArrayList<>();

        printResponse(BANNER + System.lineSeparator() + GREETING);

        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();
                try {
                    if (processCommand(input, tasks)) {
                        break;
                    }
                } catch (StanizException exception) {
                    printResponse(exception.getMessage());
                }
            }
        }

        printResponse(FAREWELL);
    }

    /**
     * Processes one command and reports whether the application should exit.
     *
     * @param input command entered by the user
     * @param tasks tasks that the command can read or update
     * @return true only when the exit command is entered
     * @throws StanizException if the command is invalid
     */
    private static boolean processCommand(String input, List<Task> tasks) throws StanizException {
        if (input.isBlank()) {
            throw new StanizException(EMPTY_INPUT_MESSAGE);
        }
        CommandType commandType = getCommandType(input);
        switch (commandType) {
        case BYE:
            return true;
        case LIST:
            printTasks(tasks);
            break;
        case MARK:
            markTaskAsDone(input, tasks);
            break;
        case UNMARK:
            markTaskAsNotDone(input, tasks);
            break;
        case DELETE:
            deleteTask(input, tasks);
            break;
        case TODO:
            addTodo(input, tasks);
            break;
        case DEADLINE:
            addDeadline(input, tasks);
            break;
        case EVENT:
            addEvent(input, tasks);
            break;
        }
        return false;
    }

    /**
     * Identifies the command invoked by the given input.
     *
     * @param input complete user input
     * @return matching command type
     * @throws StanizException if the input does not invoke a supported command
     */
    private static CommandType getCommandType(String input) throws StanizException {
        for (CommandType commandType : CommandType.values()) {
            if (commandType.matches(input)) {
                return commandType;
            }
        }
        throw new StanizException(UNKNOWN_COMMAND_MESSAGE);
    }

    /**
     * Creates and stores a to-do from its command.
     *
     * @param input command containing the task description
     * @param tasks list that receives the new task
     */
    private static void addTodo(String input, List<Task> tasks) throws StanizException {
        String description = getCommandArgument(input, CommandType.TODO);
        if (description.isBlank()) {
            throw new StanizException("OOPS! A todo needs a description. Try: todo borrow book");
        }
        addTask(new Todo(description), tasks);
    }

    /**
     * Creates and stores a deadline from its command.
     *
     * @param input command containing the description and deadline
     * @param tasks list that receives the new task
     */
    private static void addDeadline(String input, List<Task> tasks) throws StanizException {
        String arguments = getCommandArgument(input, CommandType.DEADLINE);
        int separatorIndex = arguments.indexOf(DEADLINE_SEPARATOR);
        if (separatorIndex < 0) {
            throw new StanizException("OOPS! A deadline needs '/by'. "
                    + "Try: deadline return book /by Sunday");
        }
        String description = arguments.substring(0, separatorIndex);
        String by = arguments.substring(separatorIndex + DEADLINE_SEPARATOR.length()).strip();
        if (description.isBlank()) {
            throw new StanizException("OOPS! A deadline needs a description before '/by'.");
        }
        if (by.isBlank()) {
            throw new StanizException("OOPS! A deadline needs a due time after '/by'.");
        }
        addTask(new Deadline(description, by), tasks);
    }

    /**
     * Creates and stores an event from its command.
     *
     * @param input command containing the description, start, and end
     * @param tasks list that receives the new task
     */
    private static void addEvent(String input, List<Task> tasks) throws StanizException {
        String arguments = getCommandArgument(input, CommandType.EVENT);
        int fromSeparatorIndex = arguments.indexOf(EVENT_FROM_SEPARATOR);
        if (fromSeparatorIndex < 0) {
            throw new StanizException("OOPS! An event needs '/from' and '/to'. "
                    + "Try: event meeting /from Mon 2pm /to 4pm");
        }
        int toSeparatorIndex = arguments.indexOf(EVENT_TO_SEPARATOR, fromSeparatorIndex
                + EVENT_FROM_SEPARATOR.length());
        if (toSeparatorIndex < 0) {
            throw new StanizException("OOPS! An event needs an end time after '/to'. "
                    + "Try: event meeting /from Mon 2pm /to 4pm");
        }
        String description = arguments.substring(0, fromSeparatorIndex);
        String from = arguments.substring(fromSeparatorIndex + EVENT_FROM_SEPARATOR.length(), toSeparatorIndex)
                .strip();
        String to = arguments.substring(toSeparatorIndex + EVENT_TO_SEPARATOR.length()).strip();
        if (description.isBlank()) {
            throw new StanizException("OOPS! An event needs a description before '/from'.");
        }
        if (from.isBlank()) {
            throw new StanizException("OOPS! An event needs a start time after '/from'.");
        }
        if (to.isBlank()) {
            throw new StanizException("OOPS! An event needs an end time after '/to'.");
        }
        addTask(new Event(description, from, to), tasks);
    }

    /**
     * Extracts the argument portion of a command, returning an empty string when absent.
     *
     * @param input complete user input
     * @param commandType type of command whose argument should be extracted
     * @return command argument or an empty string
     */
    private static String getCommandArgument(String input, CommandType commandType) {
        String commandPrefix = commandType.getArgumentPrefix();
        return input.length() < commandPrefix.length() ? "" : input.substring(commandPrefix.length());
    }

    /**
     * Stores a task and confirms its formatted representation to the user.
     *
     * @param task task to store
     * @param tasks list that receives the task
     */
    private static void addTask(Task task, List<Task> tasks) {
        tasks.add(task);
        printResponse(ADDED_MESSAGE_PREFIX + task);
    }

    /**
     * Marks the task identified by a one-based number as done.
     *
     * @param input command containing the task number
     * @param tasks tasks that can be updated
     */
    private static void markTaskAsDone(String input, List<Task> tasks) throws StanizException {
        int taskIndex = getTaskIndex(input, CommandType.MARK, tasks.size());
        Task task = tasks.get(taskIndex);
        task.markAsDone();

        printResponse(MARKED_MESSAGE + System.lineSeparator() + "  " + task);
    }

    /**
     * Marks the task identified by a one-based number as not done.
     *
     * @param input command containing the task number
     * @param tasks tasks that can be updated
     */
    private static void markTaskAsNotDone(String input, List<Task> tasks) throws StanizException {
        int taskIndex = getTaskIndex(input, CommandType.UNMARK, tasks.size());
        Task task = tasks.get(taskIndex);
        task.markAsNotDone();

        printResponse(UNMARKED_MESSAGE + System.lineSeparator() + "  " + task);
    }

    /**
     * Removes the task identified by a one-based number.
     *
     * @param input command containing the task number
     * @param tasks tasks from which an entry can be removed
     * @throws StanizException if the task number is absent, malformed, or out of range
     */
    private static void deleteTask(String input, List<Task> tasks) throws StanizException {
        int taskIndex = getTaskIndex(input, CommandType.DELETE, tasks.size());
        Task removedTask = tasks.remove(taskIndex);
        int remainingTaskCount = tasks.size();
        String taskLabel = remainingTaskCount == 1 ? "task" : "tasks";

        printResponse(DELETED_MESSAGE + System.lineSeparator()
                + "  " + removedTask + System.lineSeparator()
                + "Now you have " + remainingTaskCount + " " + taskLabel + " in the list.");
    }

    /**
     * Parses and validates the one-based task number supplied to a status command.
     *
     * @param input complete user input
     * @param commandType command type used to parse the number and provide error guidance
     * @param taskCount number of tasks currently stored
     * @return validated zero-based task index
     * @throws StanizException if the task number is absent, malformed, or out of range
     */
    private static int getTaskIndex(String input, CommandType commandType, int taskCount)
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
     * Prints a chatbot response enclosed by separator lines.
     *
     * @param response response to display
     */
    private static void printResponse(String response) {
        System.out.println(SEPARATOR);
        System.out.println(response);
        System.out.println(SEPARATOR);
    }

    /**
     * Prints all stored tasks using one-based numbering.
     *
     * @param tasks tasks to display
     */
    private static void printTasks(List<Task> tasks) {
        System.out.println(SEPARATOR);
        System.out.println(TASK_LIST_HEADER);
        for (int index = 0; index < tasks.size(); index++) {
            System.out.printf("%d.%s%n", index + 1, tasks.get(index));
        }
        System.out.println(SEPARATOR);
    }
}
