package staniz.ui;

import staniz.task.Task;
import staniz.task.TaskList;

/**
 * Formats Staniz responses independently of a particular user interface.
 */
public final class ResponseFormatter {
    private static final String ADDED_MESSAGE = "Good. Another objective locked in:";
    private static final String BANNER = " ____ _____  _    _   _ ___ _____\n"
            + "/ ___|_   _|/ \\  | \\ | |_ _|__  /\n"
            + "\\___ \\ | | / _ \\ |  \\| || |  / /\n"
            + " ___) || |/ ___ \\| |\\  || | / /_\n"
            + "|____/ |_/_/   \\_\\_| \\_|___/____|";
    private static final String DELETED_MESSAGE = "Cutting dead weight. This task is gone:";
    private static final String FAREWELL = "Session complete. Stay disciplined.";
    private static final String GREETING = "Staniz here. Let's get your tasks into fighting shape.";
    private static final String MARKED_MESSAGE = "Strong work. One more task conquered:";
    private static final String MATCHING_TASKS_HEADER = "Matching objectives:";
    private static final String TASK_LIST_HEADER = "Current training plan:";
    private static final String UNMARKED_MESSAGE = "Reset accepted. This objective is active again:";

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
        return ADDED_MESSAGE + System.lineSeparator() + "  " + task;
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
        String objectiveLabel = remainingTaskCount == 1 ? "objective" : "objectives";
        return DELETED_MESSAGE + System.lineSeparator()
                + "  " + task + System.lineSeparator()
                + "You have " + remainingTaskCount + " " + objectiveLabel + " left in the program.";
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
