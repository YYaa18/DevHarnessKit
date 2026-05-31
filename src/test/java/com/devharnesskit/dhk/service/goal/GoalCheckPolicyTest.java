package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalProfile;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GoalCheckPolicyTest {
    @Test
    void patchProfileAcceptsSkippedCompileAndTestButStrictProfileDoesNot() {
        GoalCheckPolicy policy = GoalCheckPolicy.defaults();
        GoalProfile patch = new GoalProfileService().find("java-api-patch");
        GoalProfile strict = new GoalProfileService().find("java-api-change");

        assertTrue(policy.accepts("compile", "skipped", patch));
        assertTrue(policy.accepts("test", "skipped", patch));
        assertFalse(policy.accepts("sensitive", "skipped", patch));

        assertFalse(policy.accepts("compile", "skipped", strict));
        assertFalse(policy.accepts("test", "skipped", strict));
    }

    @Test
    void strictProfileAcceptsApprovedManualVerificationWaiver() {
        GoalCheckPolicy policy = GoalCheckPolicy.defaults();
        GoalProfile strict = new GoalProfileService().find("java-api-change");

        assertTrue(policy.accepts("manual-compile", "passed", strict));
        assertTrue(policy.accepts("manual-compile", "waived", strict));
        assertTrue(policy.accepts("manual-test", "passed", strict));
        assertTrue(policy.accepts("manual-test", "waived", strict));

        assertFalse(policy.accepts("compile", "waived", strict));
        assertFalse(policy.accepts("test", "waived", strict));
    }

    @Test
    void requiredChecksAlwaysIncludePreWorkFileWriteGuard() {
        GoalCheckPolicy policy = GoalCheckPolicy.defaults();
        GoalProfile patch = new GoalProfileService().find("java-api-patch");

        assertTrue(Arrays.asList(policy.requiredChecks(patch))
                .contains(GoalCheckPolicy.PRE_WORK_FILE_WRITE_CHECK));
        assertTrue(policy.accepts(GoalCheckPolicy.PRE_WORK_FILE_WRITE_CHECK, "passed", patch));
        assertFalse(policy.accepts(GoalCheckPolicy.PRE_WORK_FILE_WRITE_CHECK, "failed", patch));
    }
}
