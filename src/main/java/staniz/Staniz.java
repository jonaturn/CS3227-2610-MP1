package staniz;

import staniz.command.CommandResult;
import staniz.command.CommandType;
import staniz.exception.StanizException;
import staniz.exception.StorageException;
import staniz.parser.Parser;
import staniz.storage.Storage;
import staniz.task.Task;
import staniz.task.TaskList;
import staniz.ui.ResponseFormatter;
import staniz.ui.Ui;

/**
 * Runs the Staniz personal assistant chatbot.
 */
public class Staniz {
    private final Storage storage;
    private final TaskList tasks;

    /**
     * Creates a Staniz backend using the default persistent data file.
     *
     * @throws StorageException if existing tasks cannot be loaded.
     */
    public Staniz() throws StorageException {
        this(new Storage());
    }

    /**
     * Creates a Staniz backend using caller-provided storage.
     * This constructor keeps tests isolated from the user's real data file.
     *
     * @param storage persistence source and destination.
     * @throws StorageException if existing tasks cannot be loaded.
     */
    Staniz(Storage storage) throws StorageException {
        this.storage = storage;
        tasks = new TaskList(storage.load());
    }

    /**
     * Starts Staniz and stores user entries until the exit command is entered.
     *
     * @param args command-line arguments; not used.
     */
    public static void main(String[] args) {
        try (Ui ui = new Ui()) {
            ui.showWelcome();

            Staniz staniz;
            try {
                staniz = new Staniz();
            } catch (StorageException exception) {
                ui.showResponse("System check: " + exception.getMessage());
                return;
            }

            boolean exitCommandProcessed = false;
            while (ui.hasNextCommand()) {
                String input = ui.readCommand();
                try {
                    CommandResult result = staniz.executeCommand(input);
                    ui.showResponse(result.getResponse());
                    if (result.shouldExit()) {
                        exitCommandProcessed = true;
                        break;
                    }
                } catch (StanizException | StorageException exception) {
                    ui.showResponse(exception.getMessage());
                }
            }

            if (!exitCommandProcessed) {
                ui.showFarewell();
            }
        }
    }

    /**
     * Processes one command against the loaded task list.
     *
     * @param input command entered by the user.
     * @return user-facing response and whether the application should exit.
     * @throws StanizException if the command is invalid.
     * @throws StorageException if changed tasks cannot be saved.
     */
    public CommandResult executeCommand(String input) throws StanizException, StorageException {
        CommandType commandType = Parser.parseCommandType(input);
        if (commandType == CommandType.BYE) {
            return new CommandResult(ResponseFormatter.getFarewellMessage(), true);
        }

        String response = executeNonExitCommand(commandType, input);
        if (commandType.changesTasks()) {
            storage.save(tasks);
        }
        return new CommandResult(response, false);
    }

    /**
     * Delegates a non-exit command to the operation responsible for it.
     * Keeping this method at one abstraction level makes the supported command
     * paths visible without exposing each path's parsing and mutation details.
     *
     * @param commandType validated command to execute.
     * @param input complete user input.
     * @return user-facing response for the command.
     * @throws StanizException if the command arguments are invalid.
     */
    private String executeNonExitCommand(CommandType commandType, String input) throws StanizException {
        return switch (commandType) {
            case LIST -> ResponseFormatter.formatTasks(tasks);
            case FIND -> findTasks(input);
            case MARK -> markTask(input);
            case UNMARK -> unmarkTask(input);
            case DELETE -> deleteTask(input);
            case TODO -> addTask(Parser.parseTodo(input));
            case DEADLINE -> addTask(Parser.parseDeadline(input));
            case EVENT -> addTask(Parser.parseEvent(input));
            case BYE -> throw new AssertionError("Exit commands are handled before dispatch");
        };
    }

    /**
     * Finds tasks matching the command's keyword and formats the results.
     *
     * @param input complete find command.
     * @return matching-task response.
     * @throws StanizException if the search keyword is missing.
     */
    private String findTasks(String input) throws StanizException {
        return ResponseFormatter.formatMatchingTasks(
                tasks.find(Parser.parseFindKeyword(input)));
    }

    /**
     * Marks the task selected by the command as completed.
     *
     * @param input complete mark command.
     * @return completion confirmation.
     * @throws StanizException if the task number is invalid.
     */
    private String markTask(String input) throws StanizException {
        int taskIndex = Parser.parseTaskIndex(input, CommandType.MARK, tasks.getTaskCount());
        return ResponseFormatter.formatTaskMarked(tasks.markAsDone(taskIndex));
    }

    /**
     * Marks the task selected by the command as incomplete.
     *
     * @param input complete unmark command.
     * @return incomplete-status confirmation.
     * @throws StanizException if the task number is invalid.
     */
    private String unmarkTask(String input) throws StanizException {
        int taskIndex = Parser.parseTaskIndex(input, CommandType.UNMARK, tasks.getTaskCount());
        return ResponseFormatter.formatTaskUnmarked(tasks.markAsNotDone(taskIndex));
    }

    /**
     * Deletes the task selected by the command.
     *
     * @param input complete delete command.
     * @return deletion confirmation.
     * @throws StanizException if the task number is invalid.
     */
    private String deleteTask(String input) throws StanizException {
        int taskIndex = Parser.parseTaskIndex(input, CommandType.DELETE, tasks.getTaskCount());
        Task deletedTask = tasks.delete(taskIndex);
        return ResponseFormatter.formatTaskDeleted(deletedTask, tasks.getTaskCount());
    }

    /**
     * Stores a parsed task and confirms its formatted representation to the user.
     *
     * @param task task to store.
     * @return addition confirmation.
     */
    private String addTask(Task task) {
        tasks.add(task);
        return ResponseFormatter.formatTaskAdded(task);
    }
}
