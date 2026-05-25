package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalProfile;

public final class GoalProfileService {
    public GoalProfile find(String profileKey) {
        if ("java-api-change".equals(profileKey)) {
            return new GoalProfile(profileKey, "api-change", true, "api",
                    new String[]{"inspect_existing_code", "create_change_plan",
                            "implement_minimal_change", "verify"});
        }
        if ("java-mvc-change".equals(profileKey)) {
            return new GoalProfile(profileKey, "mvc-change", true, "mvc",
                    new String[]{"inspect_existing_code", "create_change_plan",
                            "implement_minimal_change", "verify"});
        }
        if ("bugfix".equals(profileKey)) {
            return new GoalProfile(profileKey, "systematic-debugging", false, "debug",
                    new String[]{"collect_error", "list_hypotheses", "implement_fix",
                            "verify_regression"});
        }
        if ("sql-review".equals(profileKey)) {
            return new GoalProfile(profileKey, "sql-review", false, "sql",
                    new String[]{"collect_sql", "review_safety",
                            "review_index_and_pagination", "summarize_findings"});
        }
        if ("code-review".equals(profileKey)) {
            return new GoalProfile(profileKey, "code-review", false, "review",
                    new String[]{"collect_diff", "review_correctness", "review_safety",
                            "review_tests", "summarize_findings"});
        }
        if ("refactor".equals(profileKey)) {
            return new GoalProfile(profileKey, "safe-refactor", true, "auto",
                    new String[]{"identify_behavior_boundary", "create_refactor_plan",
                            "apply_small_refactor", "verify"});
        }
        return null;
    }
}
