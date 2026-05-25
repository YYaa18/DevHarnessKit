package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.util.JsonUtil;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class GoalProfileService {
    public GoalProfile find(Path projectRoot, String profileKey) {
        GoalProfile configured = loadConfiguredProfile(projectRoot, profileKey);
        return configured == null ? find(profileKey) : configured;
    }

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

    private GoalProfile loadConfiguredProfile(Path projectRoot, String profileKey) {
        if (projectRoot == null || profileKey == null || profileKey.length() == 0) {
            return null;
        }
        Path profilePath = PathUtil.goalProfile(projectRoot, profileKey);
        if (!Files.isRegularFile(profilePath)) {
            return null;
        }
        try {
            Map<String, String> raw = JsonUtil.parseObject(new String(Files.readAllBytes(profilePath), "UTF-8"));
            String workflowKey = value(raw, "workflow_key", "");
            String defaultMode = value(raw, "default_mode", "auto");
            String actionsText = value(raw, "actions", "");
            boolean specRequired = parseBoolean(value(raw, "requires_spec", "false"));
            if (workflowKey.length() == 0 || actionsText.length() == 0) {
                return null;
            }
            return new GoalProfile(profileKey, workflowKey, specRequired, defaultMode, split(actionsText));
        } catch (Exception ex) {
            return null;
        }
    }

    private String value(Map<String, String> raw, String key, String defaultValue) {
        String value = raw.get(key);
        return value == null ? defaultValue : value.trim();
    }

    private boolean parseBoolean(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
        return "true".equals(normalized) || "yes".equals(normalized) || "1".equals(normalized);
    }

    private String[] split(String value) {
        String[] parts = value.split(",");
        java.util.List<String> cleaned = new java.util.ArrayList<String>();
        for (String part : parts) {
            String text = part.trim();
            if (text.length() > 0) {
                cleaned.add(text);
            }
        }
        return cleaned.toArray(new String[cleaned.size()]);
    }
}
