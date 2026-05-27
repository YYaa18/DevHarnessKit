package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.util.JsonUtil;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class GoalConfigDiagnosticsService {
    private static final Pattern KEY_PATTERN = Pattern.compile("[a-z0-9][a-z0-9_-]*");
    private static final Pattern ACTION_PATTERN = Pattern.compile("[a-z][a-z0-9_]*");
    private static final Set<String> PROFILE_FIELDS = set("profile_key", "workflow_key", "requires_spec",
            "default_mode", "actions", "required_checks", "completion_require_fresh_checks",
            "completion_allow_skipped_checks", "completion_require_checkpoint", "strict_workflow_phase_order",
            "spec_require_non_empty_tasks", "spec_require_non_empty_acceptance",
            "graph_required", "graph_provider", "graph_require_fresh_snapshot",
            "graph_require_impact_map", "graph_max_staleness_minutes", "graph_actions",
            "legacy_graph_profile", "rollback_plan_required", "manual_evidence_required",
            "protected_impact_requires_manual_evidence", "legacy_max_changed_files");
    private static final Set<String> POLICY_FIELDS = set("required_checks", "compile_command", "test_command",
            "fail_pending_hard_gates", "accepted_compile_statuses", "accepted_test_statuses",
            "accepted_sensitive_statuses", "accepted_spec_statuses", "accepted_workflow_statuses",
            "accepted_graph_statuses", "accepted_impact_statuses", "accepted_architecture_statuses");
    private static final Set<String> CHECK_KEYS = set("compile", "test", "manual-compile", "manual-test",
            "verification-risk", "sensitive", "spec", "workflow", "graph", "impact", "legacy", "architecture",
            "bdd", "think-before-coding", "goal-driven", "simplicity", "surgical-change");
    private static final Set<String> CHECK_STATUSES = set("passed", "skipped", "waived");
    private static final Set<String> MAPPING_FIELDS = set("workflow_phase", "required_gates",
            "spec_task", "spec_acceptance_update", "phase_pass_mode", "gate_pass_mode",
            "acceptance_source", "required_checks");
    private static final Set<String> ACCEPTANCE_FIELDS = set("description", "expected", "source",
            "required_checks", "evidence_key");
    private static final Set<String> SPEC_ACCEPTANCE_UPDATE_POLICIES = set("manual", "auto_pass", "disabled");
    private static final Set<String> MAPPING_PASS_MODES = set("none", "step", "check");
    private static final Set<String> ACCEPTANCE_SOURCES = set("none", "manual", "checks");
    private static final Set<String> BUSINESS_ACCEPTANCE_SOURCES = set("checks", "test", "evidence", "manual");
    private static final Set<String> GRAPH_PROVIDERS = set("lite", "cgc");
    private static final Set<String> BUILT_IN_WORKFLOW_KEYS = set("api-change", "mvc-change",
            "systematic-debugging", "safe-refactor", "sql-review", "code-review");
    private static final Map<String, Set<String>> BUILT_IN_WORKFLOW_PHASES = workflowPhases();
    private static final Map<String, Set<String>> BUILT_IN_WORKFLOW_GATES = workflowGates();

    public List<Diagnostic> diagnose(Path projectRoot, Connection connection) {
        List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
        Set<String> workflowKeys = workflowKeys(connection);
        diagnoseProfiles(projectRoot, workflowKeys, diagnostics);
        diagnoseCheckPolicy(projectRoot, diagnostics);
        return diagnostics;
    }

    private void diagnoseProfiles(Path projectRoot, Set<String> workflowKeys, List<Diagnostic> diagnostics) {
        Path directory = PathUtil.goalProfilesDirectory(projectRoot);
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.json")) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) {
                    diagnoseProfile(file, workflowKeys, diagnostics);
                }
            }
        } catch (IOException ex) {
            diagnostics.add(warning("goal-profiles", "cannot read goal profile directory: " + ex.getMessage()));
        }
    }

    private void diagnoseProfile(Path file, Set<String> workflowKeys, List<Diagnostic> diagnostics) {
        String profileKey = profileKey(file);
        if (!KEY_PATTERN.matcher(profileKey).matches()) {
            diagnostics.add(warning(file.toString(), "profile filename should match " + KEY_PATTERN.pattern()));
        }
        Map<String, String> raw;
        try {
            raw = JsonUtil.parseObject(new String(Files.readAllBytes(file), "UTF-8"));
        } catch (Exception ex) {
            diagnostics.add(warning(file.toString(), "invalid JSON; configured profile will be ignored: "
                    + ex.getMessage()));
            return;
        }

        warnUnknownProfileFields(file, raw, diagnostics);
        String declaredProfileKey = trim(raw.get("profile_key"));
        if (declaredProfileKey.length() > 0 && !profileKey.equals(declaredProfileKey)) {
            diagnostics.add(warning(file.toString(), "profile_key does not match filename: "
                    + declaredProfileKey + " != " + profileKey + "; configured profile will be ignored"));
        }
        String workflowKey = trim(raw.get("workflow_key"));
        if (workflowKey.length() == 0) {
            diagnostics.add(warning(file.toString(), "workflow_key is required; configured profile will be ignored"));
        } else {
            if (!KEY_PATTERN.matcher(workflowKey).matches()) {
                diagnostics.add(warning(file.toString(), "workflow_key has invalid format: " + workflowKey));
            }
            if (!workflowKeys.contains(workflowKey)) {
                diagnostics.add(warning(file.toString(), "workflow template not found: " + workflowKey
                        + "; run `dhk workflow template seed` or use a built-in workflow key"));
            }
        }

        if (raw.containsKey("requires_spec") && !validBoolean(raw.get("requires_spec"))) {
            diagnostics.add(warning(file.toString(), "requires_spec should be true/false, yes/no, or 1/0"));
        }
        if (raw.containsKey("default_mode")) {
            String defaultMode = trim(raw.get("default_mode"));
            if (defaultMode.length() == 0) {
                diagnostics.add(warning(file.toString(), "default_mode is empty; runtime fallback may use auto"));
            } else if (!KEY_PATTERN.matcher(defaultMode).matches()) {
                diagnostics.add(warning(file.toString(), "default_mode has invalid format: " + defaultMode));
            }
        }
        diagnoseList(file, "actions", raw.get("actions"), ACTION_PATTERN, null, true, diagnostics);
        Set<String> actions = splitSet(raw.get("actions"));
        if (raw.containsKey("required_checks")) {
            diagnoseList(file, "required_checks", raw.get("required_checks"), KEY_PATTERN, CHECK_KEYS,
                    false, diagnostics);
        }
        diagnoseCompletionBooleans(file, raw, diagnostics);
        diagnoseBoolean(file, raw, "strict_workflow_phase_order", diagnostics);
        diagnoseSpecBooleans(file, raw, diagnostics);
        diagnoseGraphFields(file, raw, actions, diagnostics);
        diagnoseLegacyFields(file, raw, diagnostics);
        diagnoseRequiredEvidence(file, raw, actions, diagnostics);
        diagnoseActionMappings(file, raw, workflowKey, actions, diagnostics);
        diagnoseAcceptanceMappings(file, raw, diagnostics);
    }

    private void diagnoseCheckPolicy(Path projectRoot, List<Diagnostic> diagnostics) {
        Path file = PathUtil.goalCheckPolicy(projectRoot);
        if (!Files.isRegularFile(file)) {
            return;
        }
        Map<String, String> raw;
        try {
            raw = JsonUtil.parseObject(new String(Files.readAllBytes(file), "UTF-8"));
        } catch (Exception ex) {
            diagnostics.add(warning(file.toString(), "invalid JSON; default check policy will be used: "
                    + ex.getMessage()));
            return;
        }

        warnUnknownFields(file, raw, POLICY_FIELDS, diagnostics);
        if (raw.containsKey("required_checks")) {
            diagnoseList(file, "required_checks", raw.get("required_checks"), KEY_PATTERN, CHECK_KEYS,
                    false, diagnostics);
        }
        if (raw.containsKey("compile_command") && trim(raw.get("compile_command")).length() == 0) {
            diagnostics.add(warning(file.toString(), "compile_command is empty; default compile command will be used"));
        }
        if (raw.containsKey("test_command") && trim(raw.get("test_command")).length() == 0) {
            diagnostics.add(warning(file.toString(), "test_command is empty; default test command will be used"));
        }
        if (raw.containsKey("fail_pending_hard_gates") && !validBoolean(raw.get("fail_pending_hard_gates"))) {
            diagnostics.add(warning(file.toString(),
                    "fail_pending_hard_gates should be true/false, yes/no, or 1/0"));
        }
        diagnoseAcceptedStatuses(file, raw, diagnostics);
    }

    private void diagnoseAcceptedStatuses(Path file, Map<String, String> raw, List<Diagnostic> diagnostics) {
        diagnoseList(file, "accepted_compile_statuses", raw.get("accepted_compile_statuses"),
                KEY_PATTERN, CHECK_STATUSES, false, diagnostics);
        diagnoseList(file, "accepted_test_statuses", raw.get("accepted_test_statuses"),
                KEY_PATTERN, CHECK_STATUSES, false, diagnostics);
        diagnoseList(file, "accepted_sensitive_statuses", raw.get("accepted_sensitive_statuses"),
                KEY_PATTERN, CHECK_STATUSES, false, diagnostics);
        diagnoseList(file, "accepted_spec_statuses", raw.get("accepted_spec_statuses"),
                KEY_PATTERN, CHECK_STATUSES, false, diagnostics);
        diagnoseList(file, "accepted_workflow_statuses", raw.get("accepted_workflow_statuses"),
                KEY_PATTERN, CHECK_STATUSES, false, diagnostics);
        diagnoseList(file, "accepted_graph_statuses", raw.get("accepted_graph_statuses"),
                KEY_PATTERN, CHECK_STATUSES, false, diagnostics);
        diagnoseList(file, "accepted_impact_statuses", raw.get("accepted_impact_statuses"),
                KEY_PATTERN, CHECK_STATUSES, false, diagnostics);
        diagnoseList(file, "accepted_architecture_statuses", raw.get("accepted_architecture_statuses"),
                KEY_PATTERN, CHECK_STATUSES, false, diagnostics);
    }

    private void diagnoseCompletionBooleans(Path file, Map<String, String> raw, List<Diagnostic> diagnostics) {
        diagnoseBoolean(file, raw, "completion_require_fresh_checks", diagnostics);
        diagnoseBoolean(file, raw, "completion_allow_skipped_checks", diagnostics);
        diagnoseBoolean(file, raw, "completion_require_checkpoint", diagnostics);
    }

    private void diagnoseSpecBooleans(Path file, Map<String, String> raw, List<Diagnostic> diagnostics) {
        diagnoseBoolean(file, raw, "spec_require_non_empty_tasks", diagnostics);
        diagnoseBoolean(file, raw, "spec_require_non_empty_acceptance", diagnostics);
    }

    private void diagnoseGraphFields(Path file, Map<String, String> raw, Set<String> actions,
                                     List<Diagnostic> diagnostics) {
        diagnoseBoolean(file, raw, "graph_required", diagnostics);
        diagnoseBoolean(file, raw, "graph_require_fresh_snapshot", diagnostics);
        diagnoseBoolean(file, raw, "graph_require_impact_map", diagnostics);
        if (raw.containsKey("graph_provider")) {
            String provider = trim(raw.get("graph_provider"));
            if (!GRAPH_PROVIDERS.contains(provider)) {
                diagnostics.add(warning(file.toString(), "graph_provider should be one of: "
                        + join(new ArrayList<String>(GRAPH_PROVIDERS))));
            }
        }
        if (raw.containsKey("graph_max_staleness_minutes")
                && !positiveInteger(trim(raw.get("graph_max_staleness_minutes")))) {
            diagnostics.add(warning(file.toString(), "graph_max_staleness_minutes should be a positive integer"));
        }
        if (raw.containsKey("graph_actions")) {
            diagnoseList(file, "graph_actions", raw.get("graph_actions"), ACTION_PATTERN, null,
                    false, diagnostics);
            for (String graphAction : splitSet(raw.get("graph_actions"))) {
                if (!actions.contains(graphAction)) {
                    diagnostics.add(warning(file.toString(),
                            "graph_actions references action not listed in actions: " + graphAction));
                }
            }
        }
    }

    private void diagnoseLegacyFields(Path file, Map<String, String> raw, List<Diagnostic> diagnostics) {
        diagnoseBoolean(file, raw, "legacy_graph_profile", diagnostics);
        diagnoseBoolean(file, raw, "rollback_plan_required", diagnostics);
        diagnoseBoolean(file, raw, "manual_evidence_required", diagnostics);
        diagnoseBoolean(file, raw, "protected_impact_requires_manual_evidence", diagnostics);
        if (raw.containsKey("legacy_max_changed_files")
                && !positiveInteger(trim(raw.get("legacy_max_changed_files")))) {
            diagnostics.add(warning(file.toString(), "legacy_max_changed_files should be a positive integer"));
        }
    }

    private void diagnoseBoolean(Path file, Map<String, String> raw, String field,
                                 List<Diagnostic> diagnostics) {
        if (raw.containsKey(field) && !validBoolean(raw.get(field))) {
            diagnostics.add(warning(file.toString(), field + " should be true/false, yes/no, or 1/0"));
        }
    }

    private void diagnoseRequiredEvidence(Path file, Map<String, String> raw, Set<String> actions,
                                          List<Diagnostic> diagnostics) {
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("required_evidence.")) {
                continue;
            }
            String action = key.substring("required_evidence.".length());
            if (!ACTION_PATTERN.matcher(action).matches()) {
                diagnostics.add(warning(file.toString(), key + " has invalid action key: " + action));
            } else if (!actions.contains(action)) {
                diagnostics.add(warning(file.toString(), key + " references action not listed in actions: " + action));
            }
            diagnoseList(file, key, entry.getValue(), ACTION_PATTERN, null, true, diagnostics);
        }
    }

    private void diagnoseActionMappings(Path file, Map<String, String> raw, String workflowKey,
                                        Set<String> actions, List<Diagnostic> diagnostics) {
        Set<String> workflowPhases = knownWorkflowPhases(workflowKey);
        Set<String> workflowGates = knownWorkflowGates(workflowKey);
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("mapping.")) {
                continue;
            }
            String[] parts = key.split("\\.", -1);
            if (parts.length != 3) {
                diagnostics.add(warning(file.toString(), key + " should use mapping.<action>.<field>"));
                continue;
            }
            String action = parts[1];
            String field = parts[2];
            if (!ACTION_PATTERN.matcher(action).matches()) {
                diagnostics.add(warning(file.toString(), key + " has invalid action key: " + action));
            } else if (!actions.contains(action)) {
                diagnostics.add(warning(file.toString(), key + " references action not listed in actions: " + action));
            }
            if (!MAPPING_FIELDS.contains(field)) {
                diagnostics.add(warning(file.toString(), key + " contains unsupported mapping field: " + field));
                continue;
            }
            diagnoseMappingField(file, key, field, entry.getValue(), workflowPhases, workflowGates, diagnostics);
        }
    }

    private void diagnoseAcceptanceMappings(Path file, Map<String, String> raw, List<Diagnostic> diagnostics) {
        Map<String, Set<String>> fieldsByAcceptance = new LinkedHashMap<String, Set<String>>();
        for (Map.Entry<String, String> entry : raw.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("acceptance.")) {
                continue;
            }
            String[] parts = key.split("\\.", -1);
            if (parts.length != 3) {
                diagnostics.add(warning(file.toString(), key + " should use acceptance.<acceptance>.<field>"));
                continue;
            }
            String acceptance = parts[1];
            String field = parts[2];
            if (!KEY_PATTERN.matcher(acceptance).matches()) {
                diagnostics.add(warning(file.toString(), key + " has invalid acceptance key: " + acceptance));
            }
            if (!ACCEPTANCE_FIELDS.contains(field)) {
                diagnostics.add(warning(file.toString(), key + " contains unsupported acceptance field: " + field));
                continue;
            }
            Set<String> fields = fieldsByAcceptance.get(acceptance);
            if (fields == null) {
                fields = new LinkedHashSet<String>();
                fieldsByAcceptance.put(acceptance, fields);
            }
            fields.add(field);
            diagnoseAcceptanceField(file, key, field, entry.getValue(), diagnostics);
        }
        for (Map.Entry<String, Set<String>> entry : fieldsByAcceptance.entrySet()) {
            String source = trim(raw.get("acceptance." + entry.getKey() + ".source"));
            if (source.length() == 0) {
                diagnostics.add(warning(file.toString(), "acceptance." + entry.getKey()
                        + ".source is required and should be one of: "
                        + join(new ArrayList<String>(BUSINESS_ACCEPTANCE_SOURCES))));
            } else if ("checks".equals(source) && !entry.getValue().contains("required_checks")) {
                diagnostics.add(warning(file.toString(), "acceptance." + entry.getKey()
                        + ".required_checks is required when source is checks"));
            } else if ("evidence".equals(source) && !entry.getValue().contains("evidence_key")) {
                diagnostics.add(warning(file.toString(), "acceptance." + entry.getKey()
                        + ".evidence_key is required when source is evidence"));
            }
        }
    }

    private void diagnoseAcceptanceField(Path file, String key, String field, String value,
                                         List<Diagnostic> diagnostics) {
        if ("description".equals(field) || "expected".equals(field)) {
            if (trim(value).length() == 0) {
                diagnostics.add(warning(file.toString(), key + " is empty"));
            }
        } else if ("source".equals(field)) {
            String source = trim(value);
            if (!BUSINESS_ACCEPTANCE_SOURCES.contains(source)) {
                diagnostics.add(warning(file.toString(), key + " should be one of: "
                        + join(new ArrayList<String>(BUSINESS_ACCEPTANCE_SOURCES))));
            }
        } else if ("required_checks".equals(field)) {
            diagnoseList(file, key, value, KEY_PATTERN, CHECK_KEYS, false, diagnostics);
        } else if ("evidence_key".equals(field)) {
            String evidenceKey = trim(value);
            if (evidenceKey.length() == 0) {
                diagnostics.add(warning(file.toString(), key + " is empty"));
            } else if (!ACTION_PATTERN.matcher(evidenceKey).matches()) {
                diagnostics.add(warning(file.toString(), key + " contains invalid evidence key: " + evidenceKey));
            }
        }
    }

    private void diagnoseMappingField(Path file, String key, String field, String value,
                                      Set<String> workflowPhases, Set<String> workflowGates,
                                      List<Diagnostic> diagnostics) {
        if ("workflow_phase".equals(field)) {
            String phase = trim(value);
            if (phase.length() == 0) {
                diagnostics.add(warning(file.toString(), key + " is empty"));
            } else if (!ACTION_PATTERN.matcher(phase).matches()) {
                diagnostics.add(warning(file.toString(), key + " contains invalid workflow phase: " + phase));
            } else if (!workflowPhases.isEmpty() && !workflowPhases.contains(phase)) {
                diagnostics.add(warning(file.toString(), key + " references unknown workflow phase: " + phase));
            }
        } else if ("required_gates".equals(field)) {
            diagnoseList(file, key, value, ACTION_PATTERN, workflowGates.isEmpty() ? null : workflowGates,
                    false, diagnostics);
        } else if ("spec_task".equals(field)) {
            String specTask = trim(value);
            if (specTask.length() == 0) {
                diagnostics.add(warning(file.toString(), key + " is empty"));
            } else if (!KEY_PATTERN.matcher(specTask.toLowerCase(Locale.ROOT)).matches()) {
                diagnostics.add(warning(file.toString(), key + " contains invalid spec task key: " + specTask));
            }
        } else if ("spec_acceptance_update".equals(field)) {
            String policy = trim(value);
            if (!SPEC_ACCEPTANCE_UPDATE_POLICIES.contains(policy)) {
                diagnostics.add(warning(file.toString(), key + " should be one of: "
                        + join(new ArrayList<String>(SPEC_ACCEPTANCE_UPDATE_POLICIES))));
            }
        } else if ("phase_pass_mode".equals(field) || "gate_pass_mode".equals(field)) {
            String mode = trim(value);
            if (!MAPPING_PASS_MODES.contains(mode)) {
                diagnostics.add(warning(file.toString(), key + " should be one of: "
                        + join(new ArrayList<String>(MAPPING_PASS_MODES))));
            }
        } else if ("acceptance_source".equals(field)) {
            String source = trim(value);
            if (!ACCEPTANCE_SOURCES.contains(source)) {
                diagnostics.add(warning(file.toString(), key + " should be one of: "
                        + join(new ArrayList<String>(ACCEPTANCE_SOURCES))));
            }
        } else if ("required_checks".equals(field)) {
            diagnoseList(file, key, value, KEY_PATTERN, CHECK_KEYS, false, diagnostics);
        }
    }

    private void diagnoseList(Path file, String field, String value, Pattern format,
                              Set<String> allowedValues, boolean required, List<Diagnostic> diagnostics) {
        if (value == null) {
            if (required) {
                diagnostics.add(warning(file.toString(), field + " is required; configured profile will be ignored"));
            }
            return;
        }
        String text = value.trim();
        if (text.length() == 0) {
            String fallback;
            if ("required_checks".equals(field)) {
                fallback = "; default checks will be used";
            } else if (field.startsWith("accepted_")) {
                fallback = "; default accepted statuses will be used";
            } else {
                fallback = "; configured profile will be ignored";
            }
            diagnostics.add(warning(file.toString(), field + " is empty" + fallback));
            return;
        }
        String[] parts = value.split(",", -1);
        Set<String> seen = new LinkedHashSet<String>();
        for (String part : parts) {
            String item = part.trim();
            if (item.length() == 0) {
                diagnostics.add(warning(file.toString(), field + " contains an empty item"));
                continue;
            }
            if (!format.matcher(item).matches()) {
                diagnostics.add(warning(file.toString(), field + " contains invalid item: " + item));
                continue;
            }
            if (allowedValues != null && !allowedValues.contains(item)) {
                diagnostics.add(warning(file.toString(), field + " contains unsupported item: " + item));
            }
            if (!seen.add(item)) {
                diagnostics.add(warning(file.toString(), field + " contains duplicate item: " + item));
            }
        }
    }

    private void warnUnknownProfileFields(Path file, Map<String, String> raw, List<Diagnostic> diagnostics) {
        List<String> unknown = new ArrayList<String>();
        for (String key : raw.keySet()) {
            if (!isAllowedProfileField(key)) {
                unknown.add(key);
            }
        }
        if (!unknown.isEmpty()) {
            diagnostics.add(warning(file.toString(), "unknown fields: " + join(unknown)));
        }
    }

    private boolean isAllowedProfileField(String key) {
        if (PROFILE_FIELDS.contains(key)) {
            return true;
        }
        if (key.startsWith("required_evidence.")) {
            return key.length() > "required_evidence.".length();
        }
        if (key.startsWith("acceptance.")) {
            String[] parts = key.split("\\.", -1);
            return parts.length == 3 && parts[1].length() > 0 && ACCEPTANCE_FIELDS.contains(parts[2]);
        }
        if (!key.startsWith("mapping.")) {
            return false;
        }
        String[] parts = key.split("\\.", -1);
        return parts.length == 3 && parts[1].length() > 0 && MAPPING_FIELDS.contains(parts[2]);
    }

    private void warnUnknownFields(Path file, Map<String, String> raw, Set<String> allowed,
                                   List<Diagnostic> diagnostics) {
        List<String> unknown = new ArrayList<String>();
        for (String key : raw.keySet()) {
            if (!allowed.contains(key)) {
                unknown.add(key);
            }
        }
        if (!unknown.isEmpty()) {
            diagnostics.add(warning(file.toString(), "unknown fields: " + join(unknown)));
        }
    }

    private Set<String> workflowKeys(Connection connection) {
        Set<String> keys = new LinkedHashSet<String>(BUILT_IN_WORKFLOW_KEYS);
        if (connection == null) {
            return keys;
        }
        try {
            if (!MigrationRunner.hasTable(connection, "workflow_template")) {
                return keys;
            }
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT workflow_key FROM workflow_template")) {
                while (resultSet.next()) {
                    keys.add(resultSet.getString(1));
                }
            }
        } catch (Exception ignored) {
            return keys;
        }
        return keys;
    }

    private Set<String> splitSet(String value) {
        if (value == null || value.trim().length() == 0) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<String>();
        String[] parts = value.split(",");
        for (String part : parts) {
            String item = part.trim();
            if (item.length() > 0) {
                result.add(item);
            }
        }
        return result;
    }

    private Set<String> knownWorkflowPhases(String workflowKey) {
        Set<String> phases = BUILT_IN_WORKFLOW_PHASES.get(workflowKey);
        return phases == null ? Collections.<String>emptySet() : phases;
    }

    private Set<String> knownWorkflowGates(String workflowKey) {
        Set<String> gates = BUILT_IN_WORKFLOW_GATES.get(workflowKey);
        return gates == null ? Collections.<String>emptySet() : gates;
    }

    private Diagnostic warning(String location, String message) {
        return new Diagnostic("warning", location, message);
    }

    private static Set<String> set(String... values) {
        Set<String> result = new LinkedHashSet<String>();
        for (String value : values) {
            result.add(value);
        }
        return result;
    }

    private String profileKey(Path file) {
        String name = file.getFileName().toString();
        return name.endsWith(".json") ? name.substring(0, name.length() - ".json".length()) : name;
    }

    private boolean validBoolean(String value) {
        String normalized = trim(value).toLowerCase(Locale.ROOT);
        return "true".equals(normalized) || "yes".equals(normalized) || "1".equals(normalized)
                || "false".equals(normalized) || "no".equals(normalized) || "0".equals(normalized);
    }

    private boolean positiveInteger(String value) {
        try {
            return Integer.parseInt(value) > 0;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private static Map<String, Set<String>> workflowPhases() {
        Map<String, Set<String>> values = new LinkedHashMap<String, Set<String>>();
        values.put("api-change", set("export_context", "inspect_existing_code", "create_change_plan",
                "user_approval", "implement_minimal_change", "verify_compile", "verify_tests",
                "create_checkpoint", "suggest_memory_updates"));
        values.put("mvc-change", set("export_context", "inspect_existing_code", "create_change_plan",
                "user_approval", "implement_minimal_change", "verify_compile", "verify_view_flow",
                "create_checkpoint", "suggest_memory_updates"));
        values.put("systematic-debugging", set("export_context", "collect_error",
                "identify_first_business_stack", "list_hypotheses", "verify_hypothesis",
                "minimal_fix_plan", "implement_fix", "verify_regression", "create_checkpoint"));
        values.put("safe-refactor", set("export_context", "identify_behavior_boundary",
                "create_refactor_plan", "user_approval", "apply_small_refactor", "verify_compile",
                "verify_tests", "create_checkpoint"));
        return values;
    }

    private static Map<String, Set<String>> workflowGates() {
        Map<String, Set<String>> values = new LinkedHashMap<String, Set<String>>();
        values.put("api-change", set("current_context_exists", "confirmed_memory_only",
                "impacted_files_listed", "verification_plan_ready", "user_approval_before_implementation",
                "tests_recorded", "checkpoint_created", "memory_suggestions_recorded"));
        values.put("mvc-change", set("mvc_confirmed", "view_name_checked", "model_fields_checked",
                "form_validation_checked", "checkpoint_created"));
        values.put("systematic-debugging", set("error_evidence_collected",
                "first_business_stack_identified", "hypothesis_has_evidence", "fix_has_verification"));
        values.put("safe-refactor", set("behavior_preservation_stated", "rollback_plan_ready",
                "single_boundary_change", "verification_recorded"));
        return values;
    }

    public static final class Diagnostic {
        private final String severity;
        private final String location;
        private final String message;

        private Diagnostic(String severity, String location, String message) {
            this.severity = severity;
            this.location = location;
            this.message = message;
        }

        public String severity() { return severity; }

        public String location() { return location; }

        public String message() { return message; }

        public String format() {
            return severity + " " + location + ": " + message;
        }
    }
}
