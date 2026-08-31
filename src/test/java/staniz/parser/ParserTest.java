package staniz.parser;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import staniz.command.CommandType;
import staniz.exception.StanizException;

/**
 * Tests conversion and validation of every supported command form.
 */
class ParserTest {

    @Test
    void parseCommandType_supportedInputReturnsItsCommand() throws StanizException {
        assertAll(
                () -> assertEquals(CommandType.TODO, Parser.parseCommandType("todo read")),
                () -> assertEquals(CommandType.DEADLINE,
                        Parser.parseCommandType("deadline submit /by 2026-09-01")),
                () -> assertEquals(CommandType.EVENT,
                        Parser.parseCommandType("event lesson /from 2026-09-01 /to 2026-09-02")),
                () -> assertEquals(CommandType.LIST, Parser.parseCommandType("list")),
                () -> assertEquals(CommandType.FIND, Parser.parseCommandType("find lesson")),
                () -> assertEquals(CommandType.MARK, Parser.parseCommandType("mark 1")),
                () -> assertEquals(CommandType.UNMARK, Parser.parseCommandType("unmark 1")),
                () -> assertEquals(CommandType.DELETE, Parser.parseCommandType("delete 1")),
                () -> assertEquals(CommandType.BYE, Parser.parseCommandType("bye")),
                () -> assertEquals(CommandType.TODO,
                        Parser.parseCommandType("  todo\tborrow book  ")),
                () -> assertEquals(CommandType.LIST, Parser.parseCommandType("  list\t")));
    }

    @Test
    void parseCommandType_blankAndUnknownInputExplainsTheProblem() {
        assertAll(
                () -> assertParsingError(() -> Parser.parseCommandType("   "),
                        "Form check: enter a command."),
                () -> assertParsingError(() -> Parser.parseCommandType("dance"),
                        "Form check: I don't recognize that command. "
                                + "Try todo, deadline, event, list, find, mark, unmark, delete, or bye."));
    }

    @Test
    void parseCommandType_noArgumentCommandsRejectUnexpectedArguments() {
        assertAll(
                () -> assertParsingError(() -> Parser.parseCommandType("list extra"),
                        "Form check: 'list' does not take arguments. Try: list"),
                () -> assertParsingError(() -> Parser.parseCommandType("  bye\tnow  "),
                        "Form check: 'bye' does not take arguments. Try: bye"));
    }

    @Test
    void parseTodo_validInputBuildsTodo() throws StanizException {
        assertAll(
                () -> assertEquals("T | 0 | borrow book",
                        Parser.parseTodo("todo borrow book").toDataString()),
                () -> assertEquals("T | 0 | borrow   book",
                        Parser.parseTodo("  todo\t  borrow   book  ").toDataString()));
    }

    @Test
    void parseTodo_missingDescriptionIsRejected() {
        assertParsingError(() -> Parser.parseTodo("todo"),
                "Form check: a todo needs a description. Try: todo borrow book");
    }

    @Test
    void parseFindKeyword_validInputReturnsTrimmedKeyword() throws StanizException {
        assertEquals("return book", Parser.parseFindKeyword("find   return book  "));
    }

    @Test
    void parseFindKeyword_missingKeywordIsRejected() {
        assertAll(
                () -> assertParsingError(() -> Parser.parseFindKeyword("find"),
                        "Form check: 'find' needs a keyword. Try: find book"),
                () -> assertParsingError(() -> Parser.parseFindKeyword("find   "),
                        "Form check: 'find' needs a keyword. Try: find book"));
    }

    @Test
    void parseDeadline_validInputBuildsDeadline() throws StanizException {
        assertEquals("D | 0 | return book | 2026-09-01",
                Parser.parseDeadline("deadline return book /by 2026-09-01").toDataString());
    }

    @Test
    void parseDeadline_missingFieldsAndInvalidDatesAreRejected() {
        assertAll(
                () -> assertParsingError(() -> Parser.parseDeadline("deadline return book"),
                        "Form check: a deadline needs '/by'. "
                                + "Try: deadline return book /by 2019-12-02"),
                () -> assertParsingError(() -> Parser.parseDeadline("deadline  /by 2026-09-01"),
                        "Form check: a deadline needs a description before '/by'."),
                () -> assertParsingError(() -> Parser.parseDeadline("deadline return book /by"),
                        "Form check: a deadline needs a due time after '/by'."),
                () -> assertParsingError(
                        () -> Parser.parseDeadline(
                                "deadline return book /by 2026-09-01 /by 2026-09-02"),
                        "Form check: '/by' must be specified exactly once. "
                                + "Try: deadline return book /by 2019-12-02"),
                () -> assertParsingError(() -> Parser.parseDeadline("deadline return /by 2026-02-30"),
                        "Form check: the deadline date must use yyyy-MM-dd, e.g. 2019-12-02."));
    }

    @Test
    void parseEvent_validAndSingleDayInputBuildsEvents() throws StanizException {
        assertAll(
                () -> assertEquals("E | 0 | conference | 2026-09-01 | 2026-09-03",
                        Parser.parseEvent("event conference /from 2026-09-01 /to 2026-09-03")
                                .toDataString()),
                () -> assertEquals("E | 0 | consultation | 2026-09-01 | 2026-09-01",
                        Parser.parseEvent("event consultation /from 2026-09-01 /to 2026-09-01")
                                .toDataString()),
                () -> assertEquals("E | 0 | spaced   event | 2026-09-01 | 2026-09-02",
                        Parser.parseEvent(
                                "  event\tspaced   event\t/from\t2026-09-01   /to\t2026-09-02  ")
                                .toDataString()));
    }

    @Test
    void parseEvent_missingFieldsInvalidDatesAndReversedRangeAreRejected() {
        assertAll(
                () -> assertParsingError(() -> Parser.parseEvent("event meeting"),
                        "Form check: an event needs '/from' and '/to'. "
                                + "Try: event meeting /from 2019-12-02 /to 2019-12-03"),
                () -> assertParsingError(() -> Parser.parseEvent("event meeting /from 2026-09-01"),
                        "Form check: an event needs an end time after '/to'. "
                                + "Try: event meeting /from 2019-12-02 /to 2019-12-03"),
                () -> assertParsingError(() -> Parser.parseEvent("event  /from 2026-09-01 /to 2026-09-02"),
                        "Form check: an event needs a description before '/from'."),
                () -> assertParsingError(() -> Parser.parseEvent("event meeting /from /to 2026-09-02"),
                        "Form check: an event needs a start time after '/from'."),
                () -> assertParsingError(() -> Parser.parseEvent("event meeting /from 2026-09-01 /to"),
                        "Form check: an event needs an end time after '/to'."),
                () -> assertParsingError(
                        () -> Parser.parseEvent("event meeting /to 2026-09-02 /from 2026-09-01"),
                        "Form check: '/from' must appear before '/to'. "
                                + "Try: event meeting /from 2019-12-02 /to 2019-12-03"),
                () -> assertParsingError(
                        () -> Parser.parseEvent(
                                "event meeting /from 2026-09-01 /from 2026-09-02 /to 2026-09-03"),
                        "Form check: '/from' must be specified exactly once. "
                                + "Try: event meeting /from 2019-12-02 /to 2019-12-03"),
                () -> assertParsingError(
                        () -> Parser.parseEvent(
                                "event meeting /from 2026-09-01 /from 2026-09-02"),
                        "Form check: '/from' must be specified exactly once. "
                                + "Try: event meeting /from 2019-12-02 /to 2019-12-03"),
                () -> assertParsingError(
                        () -> Parser.parseEvent(
                                "event meeting /from 2026-09-01 /to 2026-09-02 /to 2026-09-03"),
                        "Form check: '/to' must be specified exactly once. "
                                + "Try: event meeting /from 2019-12-02 /to 2019-12-03"),
                () -> assertParsingError(
                        () -> Parser.parseEvent("event meeting /from 2026-02-30 /to 2026-09-02"),
                        "Form check: the event start date must use yyyy-MM-dd, e.g. 2019-12-02."),
                () -> assertParsingError(
                        () -> Parser.parseEvent("event meeting /from 2026-09-01 /to next week"),
                        "Form check: the event end date must use yyyy-MM-dd, e.g. 2019-12-02."),
                () -> assertParsingError(
                        () -> Parser.parseEvent("event meeting /from 2026-09-03 /to 2026-09-02"),
                        "Form check: the event start date cannot be after the end date."));
    }

    @Test
    void parseTaskIndex_validFirstAndLastNumbersReturnZeroBasedIndices() throws StanizException {
        assertAll(
                () -> assertEquals(0, Parser.parseTaskIndex("mark 1", CommandType.MARK, 3)),
                () -> assertEquals(2, Parser.parseTaskIndex("delete 3", CommandType.DELETE, 3)),
                () -> assertEquals(0,
                        Parser.parseTaskIndex("  mark\t  1  ", CommandType.MARK, 3)));
    }

    @Test
    void parseTaskIndex_missingMalformedAndOutOfRangeNumbersAreRejected() {
        assertAll(
                () -> assertParsingError(() -> Parser.parseTaskIndex("mark", CommandType.MARK, 2),
                        "Form check: 'mark' needs a task number. Try: mark 1"),
                () -> assertParsingError(() -> Parser.parseTaskIndex("delete one", CommandType.DELETE, 2),
                        "Form check: the task number must be a whole number."),
                () -> assertParsingError(() -> Parser.parseTaskIndex("unmark 0", CommandType.UNMARK, 2),
                        "Form check: there is no task numbered 0. "
                                + "Your training plan currently has 2 task(s)."),
                () -> assertParsingError(() -> Parser.parseTaskIndex("mark 3", CommandType.MARK, 2),
                        "Form check: there is no task numbered 3. "
                                + "Your training plan currently has 2 task(s)."));
    }

    @Test
    void parseTaskIndex_withInvalidInternalArgumentsFailsAssertions() {
        assertAll(
                () -> assertThrows(AssertionError.class,
                        () -> Parser.parseTaskIndex("list", CommandType.LIST, 2)),
                () -> assertThrows(AssertionError.class,
                        () -> Parser.parseTaskIndex("mark 1", CommandType.MARK, -1)));
    }

    private static void assertParsingError(Executable action, String expectedMessage) {
        StanizException exception = assertThrows(StanizException.class, action);
        assertEquals(expectedMessage, exception.getMessage());
    }
}
