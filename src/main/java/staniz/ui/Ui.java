package staniz.ui;

import java.util.Scanner;

import staniz.task.Task;
import staniz.task.TaskList;

/**
 * Handles all console interactions between Staniz and the user.
 */
public class Ui implements AutoCloseable {
    private static final String ADDED_MESSAGE_PREFIX = "added: ";
    private static final String DELETED_MESSAGE = "Noted. I've removed this task:";
    private static final String MARKED_MESSAGE = "Nice! I've marked this task as done:";
    private static final String MATCHING_TASKS_HEADER = "Here are the matching tasks in your list:";
    private static final String SEPARATOR = "____________________________________________________________";
    private static final String TASK_LIST_HEADER = "Here are the tasks in your list:";
    private static final String UNMARKED_MESSAGE = "OK, I've marked this task as not done yet:";
    private static final String BANNER = " ____ _____  _    _   _ ___ _____\n"
            + "/ ___|_   _|/ \\  | \\ | |_ _|__  /\n"
            + "\\___ \\ | | / _ \\ |  \\| || |  / /\n"
            + " ___) || |/ ___ \\| |\\  || | / /_\n"
            + "|____/ |_/_/   \\_\\_| \\_|___/____|";
    private static final String GREETING = "Hello! I'm Staniz\n"
            + "What can I do for you?";
    private static final String FAREWELL = "Bye. Hope to see you again soon!";

    private final Scanner scanner;

    /**
     * Creates a UI that reads commands from standard input.
     */
    public Ui() {
        scanner = new Scanner(System.in);
    }

    /**
     * Reports whether another command is available from the user.
     *
     * @return true when another complete input line can be read.
     */
    public boolean hasNextCommand() {
        return scanner.hasNextLine();
    }

    /**
     * Reads the next command entered by the user.
     *
     * @return complete command line.
     */
    public String readCommand() {
        return scanner.nextLine();
    }

    /**
     * Displays the application banner and greeting.
     */
    public void showWelcome() {
        showResponse(BANNER + System.lineSeparator() + GREETING);
    }

    /**
     * Displays the farewell message.
     */
    public void showFarewell() {
        showResponse(FAREWELL);
    }

    /**
     * Confirms that a task was added.
     *
     * @param task task that was added.
     */
    public void showTaskAdded(Task task) {
        showResponse(ADDED_MESSAGE_PREFIX + task);
    }

    /**
     * Confirms that a task was marked as completed.
     *
     * @param task task whose status changed.
     */
    public void showTaskMarked(Task task) {
        showResponse(MARKED_MESSAGE + System.lineSeparator() + "  " + task);
    }

    /**
     * Confirms that a task was marked as incomplete.
     *
     * @param task task whose status changed.
     */
    public void showTaskUnmarked(Task task) {
        showResponse(UNMARKED_MESSAGE + System.lineSeparator() + "  " + task);
    }

    /**
     * Confirms that a task was deleted and reports the remaining count.
     *
     * @param task task that was deleted.
     * @param remainingTaskCount number of tasks remaining.
     */
    public void showTaskDeleted(Task task, int remainingTaskCount) {
        String taskLabel = remainingTaskCount == 1 ? "task" : "tasks";
        showResponse(DELETED_MESSAGE + System.lineSeparator()
                + "  " + task + System.lineSeparator()
                + "Now you have " + remainingTaskCount + " " + taskLabel + " in the list.");
    }

    /**
     * Displays a response enclosed by separator lines.
     *
     * @param response response to display.
     */
    public void showResponse(String response) {
        System.out.println(SEPARATOR);
        System.out.println(response);
        System.out.println(SEPARATOR);
    }

    /**
     * Displays all stored tasks using one-based numbering.
     *
     * @param tasks tasks to display.
     */
    public void showTasks(TaskList tasks) {
        showTasksWithHeader(TASK_LIST_HEADER, tasks);
    }

    /**
     * Displays matching tasks using numbering local to the search results.
     *
     * @param matchingTasks tasks whose descriptions matched the keyword.
     */
    public void showMatchingTasks(TaskList matchingTasks) {
        showTasksWithHeader(MATCHING_TASKS_HEADER, matchingTasks);
    }

    /**
     * Displays a task list below the supplied heading using one-based numbering.
     *
     * @param header heading that explains the displayed task list.
     * @param tasks tasks to display.
     */
    private void showTasksWithHeader(String header, TaskList tasks) {
        System.out.println(SEPARATOR);
        System.out.println(header);
        for (int index = 0; index < tasks.getTaskCount(); index++) {
            System.out.printf("%d.%s%n", index + 1, tasks.get(index));
        }
        System.out.println(SEPARATOR);
    }

    /**
     * Releases the console input reader when the application exits.
     */
    @Override
    public void close() {
        scanner.close();
    }
}
