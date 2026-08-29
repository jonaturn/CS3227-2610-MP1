import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Runs the Staniz personal assistant chatbot.
 */
public class Staniz {
    private static final String ADDED_MESSAGE_PREFIX = "added: ";
    private static final String DEADLINE_COMMAND_PREFIX = "deadline ";
    private static final String DEADLINE_SEPARATOR = " /by ";
    private static final String EMPTY_INPUT_MESSAGE = "Please enter a command.";
    private static final String EVENT_COMMAND_PREFIX = "event ";
    private static final String EVENT_FROM_SEPARATOR = " /from ";
    private static final String EVENT_TO_SEPARATOR = " /to ";
    private static final String EXIT_COMMAND = "bye";
    private static final String LIST_COMMAND = "list";
    private static final String MARK_COMMAND_PREFIX = "mark ";
    private static final String MARKED_MESSAGE = "Nice! I've marked this task as done:";
    private static final String TODO_COMMAND_PREFIX = "todo ";
    private static final String UNMARK_COMMAND_PREFIX = "unmark ";
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
                if (input.isBlank()) {
                    printResponse(EMPTY_INPUT_MESSAGE);
                    continue;
                }

                if (EXIT_COMMAND.equals(input)) {
                    break;
                }

                if (LIST_COMMAND.equals(input)) {
                    printTasks(tasks);
                } else if (input.startsWith(MARK_COMMAND_PREFIX)) {
                    markTaskAsDone(input, tasks);
                } else if (input.startsWith(UNMARK_COMMAND_PREFIX)) {
                    markTaskAsNotDone(input, tasks);
                } else if (input.startsWith(TODO_COMMAND_PREFIX)) {
                    addTodo(input, tasks);
                } else if (input.startsWith(DEADLINE_COMMAND_PREFIX)) {
                    addDeadline(input, tasks);
                } else if (input.startsWith(EVENT_COMMAND_PREFIX)) {
                    addEvent(input, tasks);
                }
            }
        }

        printResponse(FAREWELL);
    }

    /**
     * Creates and stores a to-do from its command.
     *
     * @param input command containing the task description
     * @param tasks list that receives the new task
     */
    private static void addTodo(String input, List<Task> tasks) {
        String description = input.substring(TODO_COMMAND_PREFIX.length());
        addTask(new Todo(description), tasks);
    }

    /**
     * Creates and stores a deadline from its command.
     *
     * @param input command containing the description and deadline
     * @param tasks list that receives the new task
     */
    private static void addDeadline(String input, List<Task> tasks) {
        int separatorIndex = input.indexOf(DEADLINE_SEPARATOR);
        String description = input.substring(DEADLINE_COMMAND_PREFIX.length(), separatorIndex);
        String by = input.substring(separatorIndex + DEADLINE_SEPARATOR.length());
        addTask(new Deadline(description, by), tasks);
    }

    /**
     * Creates and stores an event from its command.
     *
     * @param input command containing the description, start, and end
     * @param tasks list that receives the new task
     */
    private static void addEvent(String input, List<Task> tasks) {
        int fromSeparatorIndex = input.indexOf(EVENT_FROM_SEPARATOR);
        int toSeparatorIndex = input.indexOf(EVENT_TO_SEPARATOR, fromSeparatorIndex
                + EVENT_FROM_SEPARATOR.length());
        String description = input.substring(EVENT_COMMAND_PREFIX.length(), fromSeparatorIndex);
        String from = input.substring(fromSeparatorIndex + EVENT_FROM_SEPARATOR.length(), toSeparatorIndex);
        String to = input.substring(toSeparatorIndex + EVENT_TO_SEPARATOR.length());
        addTask(new Event(description, from, to), tasks);
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
    private static void markTaskAsDone(String input, List<Task> tasks) {
        int taskIndex = Integer.parseInt(input.substring(MARK_COMMAND_PREFIX.length())) - 1;
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
    private static void markTaskAsNotDone(String input, List<Task> tasks) {
        int taskIndex = Integer.parseInt(input.substring(UNMARK_COMMAND_PREFIX.length())) - 1;
        Task task = tasks.get(taskIndex);
        task.markAsNotDone();

        printResponse(UNMARKED_MESSAGE + System.lineSeparator() + "  " + task);
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
