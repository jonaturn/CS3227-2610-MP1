package staniz;

import staniz.command.CommandType;
import staniz.exception.StanizException;
import staniz.exception.StorageException;
import staniz.parser.Parser;
import staniz.storage.Storage;
import staniz.task.Task;
import staniz.task.TaskList;
import staniz.ui.Ui;

/**
 * Runs the Staniz personal assistant chatbot.
 */
public class Staniz {

    /**
     * Starts Staniz and stores user entries until the exit command is entered.
     *
     * @param args command-line arguments; not used
     */
    public static void main(String[] args) {
        try (Ui ui = new Ui()) {
            ui.showWelcome();

            Storage storage = new Storage();
            TaskList tasks;
            try {
                tasks = new TaskList(storage.load());
            } catch (StorageException exception) {
                ui.showResponse("OOPS! " + exception.getMessage());
                return;
            }

            while (ui.hasNextCommand()) {
                String input = ui.readCommand();
                try {
                    if (processCommand(input, tasks, storage, ui)) {
                        break;
                    }
                } catch (StanizException | StorageException exception) {
                    ui.showResponse(exception.getMessage());
                }
            }

            ui.showFarewell();
        }
    }

    /**
     * Processes one command and reports whether the application should exit.
     *
     * @param input command entered by the user
     * @param tasks tasks that the command can read or update
     * @param storage persistence destination for task changes
     * @param ui user interface used to display command results
     * @return true only when the exit command is entered
     * @throws StanizException if the command is invalid
     * @throws StorageException if changed tasks cannot be saved
     */
    private static boolean processCommand(String input, TaskList tasks, Storage storage, Ui ui)
            throws StanizException, StorageException {
        CommandType commandType = Parser.parseCommandType(input);
        switch (commandType) {
        case BYE:
            return true;
        case LIST:
            ui.showTasks(tasks);
            break;
        case MARK:
            Task markedTask = tasks.markAsDone(
                    Parser.parseTaskIndex(input, CommandType.MARK, tasks.size()));
            ui.showTaskMarked(markedTask);
            break;
        case UNMARK:
            Task unmarkedTask = tasks.markAsNotDone(
                    Parser.parseTaskIndex(input, CommandType.UNMARK, tasks.size()));
            ui.showTaskUnmarked(unmarkedTask);
            break;
        case DELETE:
            Task deletedTask = tasks.delete(
                    Parser.parseTaskIndex(input, CommandType.DELETE, tasks.size()));
            ui.showTaskDeleted(deletedTask, tasks.size());
            break;
        case TODO:
            addTask(Parser.parseTodo(input), tasks, ui);
            break;
        case DEADLINE:
            addTask(Parser.parseDeadline(input), tasks, ui);
            break;
        case EVENT:
            addTask(Parser.parseEvent(input), tasks, ui);
            break;
        }
        if (commandType.changesTasks()) {
            storage.save(tasks);
        }
        return false;
    }

    /**
     * Stores a parsed task and confirms its formatted representation to the user.
     *
     * @param task task to store
     * @param tasks list that receives the task
     * @param ui user interface used to confirm the addition
     */
    private static void addTask(Task task, TaskList tasks, Ui ui) {
        tasks.add(task);
        ui.showTaskAdded(task);
    }
}
