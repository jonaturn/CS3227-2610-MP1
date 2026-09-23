package staniz.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Links the numbered cases in `test/ui-test-plan.md` to the desktop scenarios that exercise them.
 * Two different checks use this. UiTestPlanCoverageTest compares the plan against COVERAGE, which
 * needs no desktop and runs with the backend suite. WorkoutGuiTest checks that each mapped scenario
 * actually ran, which only a completed journey can show.
 */
final class UiTestPlan {
    /**
     * Cases 24 and 25 share one scenario, which filters history and then corrects a record, so they
     * are told apart by different substrings of that scenario's text.
     */
    static final Map<String, String> COVERAGE = Map.ofEntries(
            Map.entry("1", "Open fresh app"),
            Map.entry("2", "Add blank exercise"),
            Map.entry("3", "Add duplicate cable FLY"),
            Map.entry("4", "Add blank day"),
            Map.entry("4a", "Save a workout row with no exercise chosen"),
            Map.entry("5", "Save reversed rep range 12-8"),
            Map.entry("6", "Correct invalid input and duplicate the last set"),
            Map.entry("7", "Move Legs up, then down"),
            Map.entry("8", "Complete Push as planned"),
            Map.entry("9", "Skip Pull"),
            Map.entry("10", "Complete Legs without recording"),
            Map.entry("11", "Change performance draft, then Cancel"),
            Map.entry("12", "Record a range without entering actual reps"),
            Map.entry("13", "Record changed weights, exact reps, and an extra set"),
            Map.entry("13a", "Open Progress after a single exact recording"),
            Map.entry("14", "Omit planned exercise; add Cable fly; remove one set"),
            Map.entry("14a", "Correct as-planned history"),
            Map.entry("15", "Restart app"),
            Map.entry("16", "Attempt eighth day"),
            Map.entry("17", "Cancel removal, then remove current Legs day"),
            Map.entry("18", "Complete workout while save fails"),
            Map.entry("19", "Tick a set containing a rep range"),
            Map.entry("20", "Restart during workout"),
            Map.entry("20a", "Type a session entry while the save fails"),
            Map.entry("21", "Cancel finish, then finish partial session"),
            Map.entry("22", "Duplicate day, reorder exercises, copy sets across days"),
            Map.entry("22a", "Confirm a copy with no source exercise selected"),
            Map.entry("23", "Check fixed names, archive, show archived, restore"),
            Map.entry("24", "Filter history and correct an actual record"),
            Map.entry("25", "after rejecting zero reps and a rep range"),
            Map.entry("26", "Record second performance and open Progress"),
            Map.entry("27", "Replace historical Bench press with Squat"),
            Map.entry("28", "Replace a planned exercise with Squat"));

    private static final Pattern CASE_ID = Pattern.compile("^\\|\\s*(\\d+[a-z]?)\\s*\\|");

    private UiTestPlan() {
    }

    /**
     * Returns the plan file, taking the absolute path the build supplies so the check does not
     * depend on the directory the tests happen to be launched from.
     */
    static Path file() {
        return Path.of(System.getProperty("staniz.ui.plan", "test/ui-test-plan.md"));
    }

    /**
     * Reads the case identifiers from the plan's table rows, in the order they appear.
     */
    static Set<String> caseIds() throws IOException {
        Set<String> cases = new LinkedHashSet<>();
        for (String line : Files.readAllLines(file())) {
            Matcher matcher = CASE_ID.matcher(line);
            if (matcher.find()) {
                cases.add(matcher.group(1));
            }
        }
        return cases;
    }
}
