/**
 * Represents a task that takes place between specified start and end times.
 */
public class Event extends Task {
    private final String from;
    private final String to;

    /**
     * Creates an incomplete event with the given description and times.
     *
     * @param description description of the event
     * @param from event start date or time
     * @param to event end date or time
     */
    public Event(String description, String from, String to) {
        super(description);
        this.from = from;
        this.to = to;
    }

    /**
     * Returns this event in the persistent storage format.
     *
     * @return serialized event data
     */
    @Override
    public String toDataString() {
        return getDataPrefix("E") + DATA_SEPARATOR + encodeDataField(from)
                + DATA_SEPARATOR + encodeDataField(to);
    }

    /**
     * Returns the event formatted with its task type and time range.
     *
     * @return type, completion status, description, and time range
     */
    @Override
    public String toString() {
        return "[E]" + super.toString() + " (from: " + from + " to: " + to + ")";
    }
}
