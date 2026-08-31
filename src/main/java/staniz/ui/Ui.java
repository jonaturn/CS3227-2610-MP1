package staniz.ui;

import java.util.Scanner;

import staniz.task.Task;
import staniz.task.TaskList;

/**
 * Handles all console interactions between Staniz and the user.
 */
public class Ui implements AutoCloseable {
    private static final String SEPARATOR = "____________________________________________________________";

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
        showResponse(ResponseFormatter.getWelcomeMessage());
    }

    /**
     * Displays the farewell message.
     */
    public void showFarewell() {
        showResponse(ResponseFormatter.getFarewellMessage());
    }

    /**
     * Confirms that a task was added.
     *
     * @param task task that was added.
     */
    public void showTaskAdded(Task task) {
        showResponse(ResponseFormatter.formatTaskAdded(task));
    }

    /**
     * Confirms that a task was marked as completed.
     *
     * @param task task whose status changed.
     */
    public void showTaskMarked(Task task) {
        showResponse(ResponseFormatter.formatTaskMarked(task));
    }

    /**
     * Confirms that a task was marked as incomplete.
     *
     * @param task task whose status changed.
     */
    public void showTaskUnmarked(Task task) {
        showResponse(ResponseFormatter.formatTaskUnmarked(task));
    }

    /**
     * Confirms that a task was deleted and reports the remaining count.
     *
     * @param task task that was deleted.
     * @param remainingTaskCount number of tasks remaining.
     */
    public void showTaskDeleted(Task task, int remainingTaskCount) {
        showResponse(ResponseFormatter.formatTaskDeleted(task, remainingTaskCount));
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
        showResponse(ResponseFormatter.formatTasks(tasks));
    }

    /**
     * Displays matching tasks using numbering local to the search results.
     *
     * @param matchingTasks tasks whose descriptions matched the keyword.
     */
    public void showMatchingTasks(TaskList matchingTasks) {
        showResponse(ResponseFormatter.formatMatchingTasks(matchingTasks));
    }

    /**
     * Releases the console input reader when the application exits.
     */
    @Override
    public void close() {
        scanner.close();
    }
}
