package staniz.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Checks that `test/ui-test-plan.md` and the scenario mapping describe the same set of cases.
 * This is a documentation consistency check rather than a behavior check, so it runs with the
 * backend suite and reports under its own name. A failure here means the plan and the desktop
 * suite disagree, not that the application regressed.
 */
class UiTestPlanCoverageTest {
    @Test
    void everyPlanCase_hasAScenarioMappingAndViceVersa() throws Exception {
        assertTrue(Files.exists(UiTestPlan.file()),
                "UI test plan not found at " + UiTestPlan.file().toAbsolutePath());
        Set<String> planned = UiTestPlan.caseIds();
        Set<String> mapped = UiTestPlan.COVERAGE.keySet();
        // Reported as two directed differences rather than one set comparison, because COVERAGE is
        // an unordered map and comparing the sets whole prints both sides scrambled.
        assertEquals(List.of(), planned.stream().filter(id -> !mapped.contains(id)).toList(),
                "Cases listed in test/ui-test-plan.md with no entry in UiTestPlan.COVERAGE.");
        assertEquals(List.of(), mapped.stream().filter(id -> !planned.contains(id)).sorted().toList(),
                "Entries in UiTestPlan.COVERAGE for cases no longer listed in test/ui-test-plan.md.");
    }
}
