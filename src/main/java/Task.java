/**
 * Represents a task with a description and completion status.
 */
public class Task {
    private static final String DONE_ICON = "X";
    private static final String NOT_DONE_ICON = " ";

    private final String description;
    private boolean isDone;

    /**
     * Creates an incomplete task with the given description.
     *
     * @param description description of the task
     */
    public Task(String description) {
        this.description = description;
        this.isDone = false;
    }

    /**
     * Marks this task as completed.
     */
    public void markAsDone() {
        isDone = true;
    }

    /**
     * Returns the symbol representing the task's completion status.
     *
     * @return X if completed, or a blank space otherwise
     */
    public String getStatusIcon() {
        return isDone ? DONE_ICON : NOT_DONE_ICON;
    }

    /**
     * Returns the task formatted for display.
     *
     * @return completion status followed by the description
     */
    @Override
    public String toString() {
        return "[" + getStatusIcon() + "] " + description;
    }
}
