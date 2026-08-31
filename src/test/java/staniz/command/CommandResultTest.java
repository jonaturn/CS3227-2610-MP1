package staniz.command;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests immutable command results shared by the console and graphical interfaces.
 */
class CommandResultTest {

    @Test
    void constructor_withNormalResultStoresResponseAndExitState() {
        CommandResult result = new CommandResult("saved", false);

        assertAll(
                () -> assertEquals("saved", result.getResponse()),
                () -> assertFalse(result.shouldExit()));
    }

    @Test
    void constructor_withExitResultStoresResponseAndExitState() {
        CommandResult result = new CommandResult("bye", true);

        assertAll(
                () -> assertEquals("bye", result.getResponse()),
                () -> assertTrue(result.shouldExit()));
    }

    @Test
    void constructor_withNullResponseFailsInternalAssertion() {
        assertThrows(AssertionError.class, () -> new CommandResult(null, false));
    }
}
