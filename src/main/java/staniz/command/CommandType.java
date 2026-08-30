package staniz.command;

/**
 * Identifies the commands currently supported by Staniz.
 */
public enum CommandType {
    /** Adds a task without a date. */
    TODO("todo", true, true),
    /** Adds a task with a due date. */
    DEADLINE("deadline", true, true),
    /** Adds a task with a start and end date. */
    EVENT("event", true, true),
    /** Displays all stored tasks. */
    LIST("list", false, false),
    /** Finds stored tasks by a description keyword. */
    FIND("find", true, false),
    /** Marks a stored task as completed. */
    MARK("mark", true, true),
    /** Marks a stored task as incomplete. */
    UNMARK("unmark", true, true),
    /** Removes a stored task. */
    DELETE("delete", true, true),
    /** Exits the application. */
    BYE("bye", false, false);

    private final String keyword;
    private final boolean acceptsArguments;
    private final boolean changesTasks;

    /**
     * Creates a command type with its user-facing keyword and argument rule.
     *
     * @param keyword word used to invoke the command.
     * @param acceptsArguments whether text may follow the command keyword.
     * @param changesTasks whether the command changes persistent task state.
     */
    CommandType(String keyword, boolean acceptsArguments, boolean changesTasks) {
        this.keyword = keyword;
        this.acceptsArguments = acceptsArguments;
        this.changesTasks = changesTasks;
    }

    /**
     * Checks whether the given input invokes this command.
     *
     * @param input complete user input.
     * @return true if the input matches this command's accepted form.
     */
    public boolean matches(String input) {
        return keyword.equals(input) || (acceptsArguments && input.startsWith(getArgumentPrefix()));
    }

    /**
     * Returns the command keyword followed by one space.
     *
     * @return prefix that separates the command from its argument.
     */
    public String getArgumentPrefix() {
        return keyword + " ";
    }

    /**
     * Returns the keyword used to invoke this command.
     *
     * @return command keyword.
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Reports whether successful execution changes persistent task state.
     *
     * @return true if tasks must be saved after this command.
     */
    public boolean changesTasks() {
        return changesTasks;
    }
}
