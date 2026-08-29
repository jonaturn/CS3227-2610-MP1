package staniz.task;

import java.time.LocalDate;

/**
 * Represents a task that takes place between specified start and end times.
 */
public class Event extends Task {
    private final LocalDate from;
    private final LocalDate to;

    /**
     * Creates an incomplete event with the given description and times.
     *
     * @param description description of the event
     * @param from event start date
     * @param to event end date
     */
    public Event(String description, LocalDate from, LocalDate to) {
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
        return getDataPrefix("E") + DATA_SEPARATOR + from + DATA_SEPARATOR + to;
    }

    /**
     * Returns the event formatted with its task type and time range.
     *
     * @return type, completion status, description, and time range
     */
    @Override
    public String toString() {
        return "[E]" + super.toString() + " (from: " + DateParser.format(from)
                + " to: " + DateParser.format(to) + ")";
    }
}
