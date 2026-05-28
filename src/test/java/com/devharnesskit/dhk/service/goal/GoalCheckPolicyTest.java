package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalProfile;
import org.junit.jupiter.api.Test;

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
}
