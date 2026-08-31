package staniz.command;

/**
 * Contains the user-facing response and control-flow effect of one command.
 */
public final class CommandResult {
    private final String response;
    private final boolean shouldExit;

    /**
     * Creates the result of processing one command.
     *
     * @param response text that the user interface should display.
     * @param shouldExit whether the application should close after displaying the response.
     */
    public CommandResult(String response, boolean shouldExit) {
        assert response != null : "A command result must contain response text";
        this.response = response;
        this.shouldExit = shouldExit;
    }

    /**
     * Returns the text produced by the command.
     *
     * @return user-facing response text.
     */
    public String getResponse() {
        return response;
    }

    /**
     * Reports whether the command requested application shutdown.
     *
     * @return true only for the bye command.
     */
    public boolean shouldExit() {
        return shouldExit;
    }
}
