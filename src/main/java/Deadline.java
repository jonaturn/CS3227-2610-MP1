/**
 * Represents a task that must be completed by a specified date or time.
 */
public class Deadline extends Task {
    private final String by;

    /**
     * Creates an incomplete deadline with the given description and due time.
     *
     * @param description description of the deadline
     * @param by date or time by which the task must be completed
     */
    public Deadline(String description, String by) {
        super(description);
        this.by = by;
    }

    /**
     * Returns the deadline formatted with its task type and due time.
     *
     * @return type, completion status, description, and due time
     */
    @Override
    public String toString() {
        return "[D]" + super.toString() + " (by: " + by + ")";
    }
}
