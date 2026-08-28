import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Runs the Staniz personal assistant chatbot.
 */
public class Staniz {
    private static final String ADDED_MESSAGE_PREFIX = "added: ";
    private static final String EXIT_COMMAND = "bye";
    private static final String LIST_COMMAND = "list";
    private static final String BANNER = " ____ _____  _    _   _ ___ _____\n"
            + "/ ___|_   _|/ \\  | \\ | |_ _|__  /\n"
            + "\\___ \\ | | / _ \\ |  \\| || |  / /\n"
            + " ___) || |/ ___ \\| |\\  || | / /_\n"
            + "|____/ |_/_/   \\_\\_| \\_|___/____|";
    private static final String GREETING = "Hello! I'm Staniz\n"
            + "What can I do for you?";
    private static final String FAREWELL = "Bye. Hope to see you again soon!";

    /**
     * Starts Staniz and stores user entries until the exit command is entered.
     *
     * @param args command-line arguments; not used
     */
    public static void main(String[] args) {
        List<String> tasks = new ArrayList<>();

        System.out.println(BANNER);
        System.out.println(GREETING);

        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();
                if (EXIT_COMMAND.equals(input)) {
                    break;
                }

                if (LIST_COMMAND.equals(input)) {
                    printTasks(tasks);
                } else {
                    tasks.add(input);
                    System.out.println(ADDED_MESSAGE_PREFIX + input);
                }
            }
        }

        System.out.println(FAREWELL);
    }

    /**
     * Prints all stored tasks using one-based numbering.
     *
     * @param tasks tasks to display
     */
    private static void printTasks(List<String> tasks) {
        for (int index = 0; index < tasks.size(); index++) {
            System.out.printf("%d. %s%n", index + 1, tasks.get(index));
        }
    }
}
