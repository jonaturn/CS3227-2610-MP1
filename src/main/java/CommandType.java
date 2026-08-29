/**
 * Identifies the commands currently supported by Staniz.
 */
public enum CommandType {
    TODO("todo", true),
    DEADLINE("deadline", true),
    EVENT("event", true),
    LIST("list", false),
    MARK("mark", true),
    UNMARK("unmark", true),
    DELETE("delete", true),
    BYE("bye", false);

    private final String keyword;
    private final boolean acceptsArguments;

    /**
     * Creates a command type with its user-facing keyword and argument rule.
     *
     * @param keyword word used to invoke the command
     * @param acceptsArguments whether text may follow the command keyword
     */
    CommandType(String keyword, boolean acceptsArguments) {
        this.keyword = keyword;
        this.acceptsArguments = acceptsArguments;
    }

    /**
     * Checks whether the given input invokes this command.
     *
     * @param input complete user input
     * @return true if the input matches this command's accepted form
     */
    public boolean matches(String input) {
        return keyword.equals(input) || (acceptsArguments && input.startsWith(getArgumentPrefix()));
    }

    /**
     * Returns the command keyword followed by one space.
     *
     * @return prefix that separates the command from its argument
     */
    public String getArgumentPrefix() {
        return keyword + " ";
    }

    /**
     * Returns the keyword used to invoke this command.
     *
     * @return command keyword
     */
    public String getKeyword() {
        return keyword;
    }
}
