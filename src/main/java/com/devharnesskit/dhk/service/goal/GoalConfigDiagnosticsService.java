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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class GoalConfigDiagnosticsService {
    private static final Pattern KEY_PATTERN = Pattern.compile("[a-z0-9][a-z0-9_-]*");
    private static final Pattern ACTION_PATTERN = Pattern.compile("[a-z][a-z0-9_]*");
    private static final Set<String> PROFILE_FIELDS = set("workflow_key", "requires_spec", "default_mode", "actions");
    private static final Set<String> POLICY_FIELDS = set("required_checks", "compile_command", "test_command",
            "fail_pending_hard_gates");
    private static final Set<String> CHECK_KEYS = set("compile", "test", "sensitive", "spec", "workflow");
    private static final Set<String> BUILT_IN_WORKFLOW_KEYS = set("api-change", "mvc-change",
            "systematic-debugging", "safe-refactor", "sql-review", "code-review");

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

        warnUnknownFields(file, raw, PROFILE_FIELDS, diagnostics);
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
            String fallback = "required_checks".equals(field)
                    ? "; default checks will be used"
                    : "; configured profile will be ignored";
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
