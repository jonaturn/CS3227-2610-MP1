import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Runs the Staniz personal assistant chatbot.
 */
public class Staniz {
    private static final String ADDED_MESSAGE_PREFIX = "added: ";
    private static final String EMPTY_INPUT_MESSAGE = "Please enter a command.";
    private static final String EXIT_COMMAND = "bye";
    private static final String LIST_COMMAND = "list";
    private static final String MARK_COMMAND_PREFIX = "mark ";
    private static final String MARKED_MESSAGE = "Nice! I've marked this task as done:";
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
                } else {
                    tasks.add(new Task(input));
                    printResponse(ADDED_MESSAGE_PREFIX + input);
                }
            }
        }

        printResponse(FAREWELL);
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
