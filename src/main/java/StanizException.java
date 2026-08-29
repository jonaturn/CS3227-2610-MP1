/**
 * Represents a user-facing error caused by an invalid Staniz command.
 */
public class StanizException extends Exception {

    /**
     * Creates an exception carrying guidance that can be shown to the user.
     *
     * @param message explanation of the error and, where useful, how to correct it
     */
    public StanizException(String message) {
        super(message);
    }
}
