package staniz.task;

import java.time.LocalDate;

/**
 * Represents a task that must be completed by a specified date or time.
 */
public class Deadline extends Task {
    private final LocalDate by;

    /**
     * Creates an incomplete deadline with the given description and due time.
     *
     * @param description description of the deadline.
     * @param by date by which the task must be completed.
     */
    public Deadline(String description, LocalDate by) {
        super(description);
        this.by = by;
    }

    /**
     * Returns this deadline in the persistent storage format.
     *
     * @return serialized deadline data.
     */
    @Override
    public String toDataString() {
        return getDataPrefix("D") + DATA_SEPARATOR + by;
    }

    /**
     * Returns the deadline formatted with its task type and due time.
     *
     * @return type, completion status, description, and due time.
     */
    @Override
    public String toString() {
        return "[D]" + super.toString() + " (by: " + DateParser.format(by) + ")";
    }
}
