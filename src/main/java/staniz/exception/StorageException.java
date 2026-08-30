package staniz.exception;

/**
 * Represents a failure to read or write Staniz task data.
 */
public class StorageException extends Exception {

    /**
     * Creates a storage exception with a user-facing explanation.
     *
     * @param message explanation of the storage failure.
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * Creates a storage exception with its underlying cause.
     *
     * @param message explanation of the storage failure.
     * @param cause lower-level cause of the failure.
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
