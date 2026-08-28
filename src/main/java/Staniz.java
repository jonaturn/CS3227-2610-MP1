import java.util.Scanner;

/**
 * Runs the Staniz personal assistant chatbot.
 */
public class Staniz {
    private static final String EXIT_COMMAND = "bye";
    private static final String BANNER = " ____ _____  _    _   _ ___ _____\n"
            + "/ ___|_   _|/ \\  | \\ | |_ _|__  /\n"
            + "\\___ \\ | | / _ \\ |  \\| || |  / /\n"
            + " ___) || |/ ___ \\| |\\  || | / /_\n"
            + "|____/ |_/_/   \\_\\_| \\_|___/____|";
    private static final String GREETING = "Hello! I'm Staniz\n"
            + "What can I do for you?";
    private static final String FAREWELL = "Bye. Hope to see you again soon!";

    /**
     * Starts Staniz and echoes user input until the exit command is entered.
     *
     * @param args command-line arguments; not used
     */
    public static void main(String[] args) {
        System.out.println(BANNER);
        System.out.println(GREETING);

        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();
                if (EXIT_COMMAND.equals(input)) {
                    break;
                }
                System.out.println(input);
            }
        }

        System.out.println(FAREWELL);
    }
}
