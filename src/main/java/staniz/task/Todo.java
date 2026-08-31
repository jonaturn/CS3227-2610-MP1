package staniz.task;

/**
 * Represents a task without an associated date or time.
 */
public class Todo extends Task {

    /**
     * Creates an incomplete to-do with the given description.
     *
     * @param description description of the to-do.
     */
    public Todo(String description) {
        super(description);
    }

    /**
     * Returns this to-do in the persistent storage format.
     *
     * @return serialized to-do data.
     */
    @Override
    public String toDataString() {
        return getDataPrefix("T");
    }

    /**
     * Returns the to-do formatted with its task type.
     *
     * @return type, completion status, and description.
     */
    @Override
    public String toString() {
        return "[T]" + super.toString();
    }
}
