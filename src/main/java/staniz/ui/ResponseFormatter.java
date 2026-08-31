package staniz.ui;

import staniz.task.Task;
import staniz.task.TaskList;

/**
 * Formats Staniz responses independently of a particular user interface.
 */
public final class ResponseFormatter {
    private static final String ADDED_MESSAGE_PREFIX = "added: ";
    private static final String BANNER = " ____ _____  _    _   _ ___ _____\n"
            + "/ ___|_   _|/ \\  | \\ | |_ _|__  /\n"
            + "\\___ \\ | | / _ \\ |  \\| || |  / /\n"
            + " ___) || |/ ___ \\| |\\  || | / /_\n"
            + "|____/ |_/_/   \\_\\_| \\_|___/____|";
    private static final String DELETED_MESSAGE = "Noted. I've removed this task:";
    private static final String FAREWELL = "Bye. Hope to see you again soon!";
    private static final String GREETING = "Hello! I'm Staniz\n"
            + "What can I do for you?";
    private static final String MARKED_MESSAGE = "Nice! I've marked this task as done:";
    private static final String MATCHING_TASKS_HEADER = "Here are the matching tasks in your list:";
    private static final String TASK_LIST_HEADER = "Here are the tasks in your list:";
    private static final String UNMARKED_MESSAGE = "OK, I've marked this task as not done yet:";

    private ResponseFormatter() {
        // Utility class; prevent instantiation.
    }

    /**
     * Formats the banner and greeting used by the command-line interface.
     *
     * @return welcome message.
     */
    public static String getWelcomeMessage() {
        return BANNER + System.lineSeparator() + GREETING;
    }

    /**
     * Returns the conversational greeting used by graphical interfaces.
     *
     * @return greeting without the command-line banner.
     */
    public static String getGreetingMessage() {
        return GREETING;
    }

    /**
     * Returns the message shown when Staniz exits.
     *
     * @return farewell message.
     */
    public static String getFarewellMessage() {
        return FAREWELL;
    }

    /**
     * Formats confirmation that a task was added.
     *
     * @param task task that was added.
     * @return addition confirmation.
     */
    public static String formatTaskAdded(Task task) {
        return ADDED_MESSAGE_PREFIX + task;
    }

    /**
     * Formats confirmation that a task was marked as completed.
     *
     * @param task task whose status changed.
     * @return completion confirmation.
     */
    public static String formatTaskMarked(Task task) {
        return MARKED_MESSAGE + System.lineSeparator() + "  " + task;
    }

    /**
     * Formats confirmation that a task was marked as incomplete.
     *
     * @param task task whose status changed.
     * @return incomplete-status confirmation.
     */
    public static String formatTaskUnmarked(Task task) {
        return UNMARKED_MESSAGE + System.lineSeparator() + "  " + task;
    }

    /**
     * Formats confirmation that a task was deleted.
     *
     * @param task task that was deleted.
     * @param remainingTaskCount number of tasks remaining.
     * @return deletion confirmation and remaining count.
     */
    public static String formatTaskDeleted(Task task, int remainingTaskCount) {
        String taskLabel = remainingTaskCount == 1 ? "task" : "tasks";
        return DELETED_MESSAGE + System.lineSeparator()
                + "  " + task + System.lineSeparator()
                + "Now you have " + remainingTaskCount + " " + taskLabel + " in the list.";
    }

    /**
     * Formats all stored tasks using one-based numbering.
     *
     * @param tasks tasks to display.
     * @return numbered task list.
     */
    public static String formatTasks(TaskList tasks) {
        return formatTasksWithHeader(TASK_LIST_HEADER, tasks);
    }

    /**
     * Formats matching tasks using numbering local to the search results.
     *
     * @param matchingTasks tasks whose descriptions matched the keyword.
     * @return numbered matching-task list.
     */
    public static String formatMatchingTasks(TaskList matchingTasks) {
        return formatTasksWithHeader(MATCHING_TASKS_HEADER, matchingTasks);
    }

    /**
     * Formats a task list beneath a supplied heading.
     *
     * @param header heading that explains the displayed task list.
     * @param tasks tasks to display.
     * @return heading followed by zero or more numbered tasks.
     */
    private static String formatTasksWithHeader(String header, TaskList tasks) {
        StringBuilder response = new StringBuilder(header);
        for (int index = 0; index < tasks.getTaskCount(); index++) {
            response.append(System.lineSeparator())
                    .append(index + 1)
                    .append('.')
                    .append(tasks.get(index));
        }
        return response.toString();
    }
}
