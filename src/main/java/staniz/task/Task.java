package staniz.task;

/**
 * Represents a task with a description and completion status.
 */
public abstract class Task {
    private static final String DONE_ICON = "X";
    private static final String NOT_DONE_ICON = " ";

    /** Separator used between fields in the persistent task format. */
    protected static final String DATA_SEPARATOR = " | ";

    private final String description;
    private boolean isDone;

    /**
     * Creates an incomplete task with the given description.
     *
     * @param description description of the task.
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
     * Marks this task as not completed.
     */
    public void markAsNotDone() {
        isDone = false;
    }

    /**
     * Returns the symbol representing the task's completion status.
     *
     * @return X if completed, or a blank space otherwise.
     */
    public String getStatusIcon() {
        return isDone ? DONE_ICON : NOT_DONE_ICON;
    }

    /**
     * Checks whether the task description contains the given keyword.
     * Matching is case-sensitive and accepts partial words.
     *
     * @param keyword text to find in the description.
     * @return true if the description contains the keyword.
     */
    boolean hasDescriptionContaining(String keyword) {
        return description.contains(keyword);
    }

    /**
     * Returns the common type, status, and description fields for persistence.
     *
     * @param taskType single-character task type identifier.
     * @return serialized common task fields.
     */
    protected String getDataPrefix(String taskType) {
        String completionValue = isDone ? "1" : "0";
        return taskType + DATA_SEPARATOR + completionValue + DATA_SEPARATOR + encodeDataField(description);
    }

    /**
     * Escapes field separator and escape characters in a persisted text value.
     *
     * @param value task value to encode.
     * @return value safe for the persistent task format.
     */
    protected String encodeDataField(String value) {
        return value.replace("\\", "\\\\").replace("|", "\\|");
    }

    /**
     * Returns this task in the persistent storage format.
     *
     * @return serialized task data.
     */
    public abstract String toDataString();

    /**
     * Returns the task formatted for display.
     *
     * @return completion status followed by the description.
     */
    @Override
    public String toString() {
        return "[" + getStatusIcon() + "] " + description;
    }
}
