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
                ui.showResponse("OOPS! " + exception.getMessage());
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
        String response;
        switch (commandType) {
            case BYE:
                return new CommandResult(ResponseFormatter.getFarewellMessage(), true);
            case LIST:
                response = ResponseFormatter.formatTasks(tasks);
                break;
            case FIND:
                response = ResponseFormatter.formatMatchingTasks(
                        tasks.find(Parser.parseFindKeyword(input)));
                break;
            case MARK:
                Task markedTask = tasks.markAsDone(
                        Parser.parseTaskIndex(input, CommandType.MARK, tasks.getTaskCount()));
                response = ResponseFormatter.formatTaskMarked(markedTask);
                break;
            case UNMARK:
                Task unmarkedTask = tasks.markAsNotDone(
                        Parser.parseTaskIndex(input, CommandType.UNMARK, tasks.getTaskCount()));
                response = ResponseFormatter.formatTaskUnmarked(unmarkedTask);
                break;
            case DELETE:
                Task deletedTask = tasks.delete(
                        Parser.parseTaskIndex(input, CommandType.DELETE, tasks.getTaskCount()));
                response = ResponseFormatter.formatTaskDeleted(deletedTask, tasks.getTaskCount());
                break;
            case TODO:
                response = addTask(Parser.parseTodo(input));
                break;
            case DEADLINE:
                response = addTask(Parser.parseDeadline(input));
                break;
            case EVENT:
                response = addTask(Parser.parseEvent(input));
                break;
            default:
                throw new AssertionError("Unexpected command type: " + commandType);
        }
        if (commandType.changesTasks()) {
            storage.save(tasks);
        }
        return new CommandResult(response, false);
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
