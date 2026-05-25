package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalActionMapping;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.util.JsonUtil;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class GoalProfileService {
    public GoalProfile find(Path projectRoot, String profileKey) {
        GoalProfile configured = loadConfiguredProfile(projectRoot, profileKey);
        return configured == null ? find(profileKey) : configured;
    }

    public GoalProfile find(String profileKey) {
        if ("java-api-change".equals(profileKey)) {
            return javaProfile(profileKey, "api-change", "api");
        }
        if ("java-mvc-change".equals(profileKey)) {
            return javaProfile(profileKey, "mvc-change", "mvc");
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
            String configuredProfileKey = value(raw, "profile_key", profileKey);
            if (!profileKey.equals(configuredProfileKey)) {
                return null;
            }
            String workflowKey = value(raw, "workflow_key", "");
            String defaultMode = value(raw, "default_mode", "auto");
            String actionsText = value(raw, "actions", "");
            boolean specRequired = parseBoolean(value(raw, "requires_spec", "false"));
            if (workflowKey.length() == 0 || actionsText.length() == 0) {
                return null;
            }
            String[] actions = split(actionsText);
            return new GoalProfile(profileKey, workflowKey, specRequired, defaultMode, actions,
                    requiredEvidence(raw), split(raw.get("required_checks")),
                    parseBoolean(value(raw, "completion_require_fresh_checks", "true")),
                    parseBoolean(value(raw, "completion_allow_skipped_checks", "true")),
                    parseBoolean(value(raw, "completion_require_checkpoint", "true")),
                    actionMappings(raw, actions));
        } catch (Exception ex) {
            return null;
        }
    }

    private GoalProfile javaProfile(String profileKey, String workflowKey, String defaultMode) {
        String[] actions = new String[]{"inspect_existing_code", "create_change_plan",
                "implement_minimal_change", "verify"};
        Map<String, String[]> evidence = new LinkedHashMap<String, String[]>();
        evidence.put("inspect_existing_code",
                new String[]{"existing_controller", "existing_service", "existing_mapper", "existing_tests"});
        evidence.put("create_change_plan",
                new String[]{"impacted_files", "risk_points", "verification_plan"});
        evidence.put("implement_minimal_change",
                new String[]{"changed_files", "implementation_summary"});
        evidence.put("verify",
                new String[]{"compile_result", "test_result", "sensitive_result"});

        Map<String, GoalActionMapping> mappings = new LinkedHashMap<String, GoalActionMapping>();
        mappings.put("inspect_existing_code", new GoalActionMapping("inspect_existing_code",
                "inspect_existing_code", new String[0], "", "manual"));
        mappings.put("create_change_plan", new GoalActionMapping("create_change_plan",
                "create_change_plan", new String[]{"impacted_files_listed", "verification_plan_ready"},
                "", "manual"));
        mappings.put("implement_minimal_change", new GoalActionMapping("implement_minimal_change",
                "implement_minimal_change", new String[0], "", "manual"));
        mappings.put("verify", new GoalActionMapping("verify",
                "verify_tests", new String[]{"tests_recorded"}, "", "manual"));

        return new GoalProfile(profileKey, workflowKey, true, defaultMode, actions, evidence,
                GoalCheckPolicy.DEFAULT_REQUIRED_CHECKS, true, false, true, mappings);
    }

    private Map<String, String[]> requiredEvidence(Map<String, String> raw) {
        Map<String, String[]> result = new LinkedHashMap<String, String[]>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("required_evidence.")) {
                String action = key.substring("required_evidence.".length()).trim();
                if (action.length() > 0) {
                    result.put(action, split(entry.getValue()));
                }
            }
        }
        return result;
    }

    private Map<String, GoalActionMapping> actionMappings(Map<String, String> raw, String[] actions) {
        Map<String, MappingBuilder> builders = new LinkedHashMap<String, MappingBuilder>();
        for (String action : actions) {
            builders.put(action, new MappingBuilder(action));
        }
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("mapping.")) {
                continue;
            }
            String[] parts = key.split("\\.");
            if (parts.length != 3) {
                continue;
            }
            MappingBuilder builder = builders.get(parts[1]);
            if (builder == null) {
                continue;
            }
            builder.set(parts[2], entry.getValue());
        }
        Map<String, GoalActionMapping> mappings = new LinkedHashMap<String, GoalActionMapping>();
        for (MappingBuilder builder : builders.values()) {
            GoalActionMapping mapping = builder.build();
            if (mapping.workflowPhase().length() > 0 || mapping.requiredGates().length > 0
                    || mapping.specTask().length() > 0 || mapping.specAcceptanceUpdate().length() > 0) {
                mappings.put(mapping.actionKey(), mapping);
            }
        }
        return mappings;
    }

    private String value(Map<String, String> raw, String key, String defaultValue) {
        String value = raw.get(key);
        return value == null ? defaultValue : value.trim();
    }

    private boolean parseBoolean(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return "true".equals(normalized) || "yes".equals(normalized) || "1".equals(normalized);
    }

    private String[] split(String value) {
        if (value == null || value.trim().length() == 0) {
            return new String[0];
        }
        String[] parts = value.split(",");
        java.util.List<String> cleaned = new ArrayList<String>();
        for (String part : parts) {
            String text = part.trim();
            if (text.length() > 0) {
                cleaned.add(text);
            }
        }
        return cleaned.toArray(new String[cleaned.size()]);
    }

    private static final class MappingBuilder {
        private final String action;
        private String workflowPhase = "";
        private String[] requiredGates = new String[0];
        private String specTask = "";
        private String specAcceptanceUpdate = "";

        private MappingBuilder(String action) {
            this.action = action;
        }

        private void set(String field, String value) {
            if ("workflow_phase".equals(field)) {
                workflowPhase = value == null ? "" : value.trim();
            } else if ("required_gates".equals(field)) {
                requiredGates = splitStatic(value);
            } else if ("spec_task".equals(field)) {
                specTask = value == null ? "" : value.trim();
            } else if ("spec_acceptance_update".equals(field)) {
                specAcceptanceUpdate = value == null ? "" : value.trim();
            }
        }

        private GoalActionMapping build() {
            return new GoalActionMapping(action, workflowPhase, requiredGates, specTask, specAcceptanceUpdate);
        }

        private static String[] splitStatic(String value) {
            if (value == null || value.trim().length() == 0) {
                return new String[0];
            }
            String[] parts = value.split(",");
            ArrayList<String> cleaned = new ArrayList<String>();
            for (String part : parts) {
                String text = part.trim();
                if (text.length() > 0) {
                    cleaned.add(text);
                }
            }
            return cleaned.toArray(new String[cleaned.size()]);
        }
    }
}
