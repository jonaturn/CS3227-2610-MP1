package staniz.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests command matching and persistence behavior metadata.
 */
class CommandTypeTest {

    @Test
    void matches_argumentAndExactCommandsAcceptOnlyTheirValidForms() {
        assertTrue(CommandType.TODO.matches("todo"));
        assertTrue(CommandType.TODO.matches("todo borrow book"));
        assertFalse(CommandType.TODO.matches("todoist"));
        assertTrue(CommandType.LIST.matches("list"));
        assertFalse(CommandType.LIST.matches("list extra"));
        assertTrue(CommandType.BYE.matches("bye"));
        assertFalse(CommandType.BYE.matches("bye now"));
    }

    @Test
    void changesTasks_mutatingAndReadOnlyCommandsReportCorrectly() {
        assertTrue(CommandType.TODO.changesTasks());
        assertTrue(CommandType.DEADLINE.changesTasks());
        assertTrue(CommandType.EVENT.changesTasks());
        assertTrue(CommandType.MARK.changesTasks());
        assertTrue(CommandType.UNMARK.changesTasks());
        assertTrue(CommandType.DELETE.changesTasks());
        assertFalse(CommandType.LIST.changesTasks());
        assertFalse(CommandType.BYE.changesTasks());
    }
}
