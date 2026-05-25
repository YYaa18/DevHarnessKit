package com.devharnesskit.dhk.service.policy;

import com.devharnesskit.dhk.model.policy.DevHarnessPolicy;
import com.devharnesskit.dhk.util.JsonUtil;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class DevHarnessPolicyService {
    private static final Pattern COMMAND_PATTERN =
            Pattern.compile("[a-z][a-z0-9_-]*(\\s+[a-z][a-z0-9_-]*)*");
    private static final Pattern ENV_PATTERN = Pattern.compile("[a-z0-9][a-z0-9_-]*");
    private static final Set<String> FIELDS = set("mode", "allowed_dhk_commands",
            "forbidden_dhk_commands", "protected_files", "allowed_write_paths",
            "db_sql_requires_explicit_request", "db_require_readonly_credentials",
            "db_allowed_environments", "context_export_require_sensitive_scan",
            "context_export_block_on_sensitive", "context_export_allowed_files",
            "context_export_forbidden_files");
    private static final Set<String> MODES = set("strict", "guided", "expert");

    public DevHarnessPolicy load(Path projectRoot) {
        if (projectRoot == null) {
            return DevHarnessPolicy.defaults();
        }
        Path policyPath = PathUtil.devharnessPolicy(projectRoot);
        if (!Files.isRegularFile(policyPath)) {
            return DevHarnessPolicy.defaults();
        }
        try {
            Map<String, String> raw = JsonUtil.parseObject(new String(Files.readAllBytes(policyPath), "UTF-8"));
            return new DevHarnessPolicy(value(raw, "mode", "guided"),
                    splitList(raw.get("allowed_dhk_commands")),
                    splitList(raw.get("forbidden_dhk_commands")),
                    splitList(raw.get("protected_files")),
                    splitList(raw.get("allowed_write_paths")),
                    parseBoolean(raw.get("db_sql_requires_explicit_request"), true),
                    parseBoolean(raw.get("db_require_readonly_credentials"), true),
                    splitList(raw.get("db_allowed_environments")),
                    parseBoolean(raw.get("context_export_require_sensitive_scan"), true),
                    parseBoolean(raw.get("context_export_block_on_sensitive"), true),
                    splitList(raw.get("context_export_allowed_files")),
                    splitList(raw.get("context_export_forbidden_files")));
        } catch (Exception ex) {
            return DevHarnessPolicy.defaults();
        }
    }

    public boolean hasPolicy(Path projectRoot) {
        return projectRoot != null && Files.isRegularFile(PathUtil.devharnessPolicy(projectRoot));
    }

    public List<Diagnostic> diagnose(Path projectRoot) {
        List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
        if (projectRoot == null) {
            return diagnostics;
        }
        Path policyPath = PathUtil.devharnessPolicy(projectRoot);
        if (!Files.isRegularFile(policyPath)) {
            return diagnostics;
        }
        Map<String, String> raw;
        try {
            raw = JsonUtil.parseObject(new String(Files.readAllBytes(policyPath), "UTF-8"));
        } catch (Exception ex) {
            diagnostics.add(warning(policyPath.toString(), "invalid JSON; default policy will be used: "
                    + ex.getMessage()));
            return diagnostics;
        }
        warnUnknownFields(policyPath, raw, diagnostics);
        diagnoseMode(policyPath, raw, diagnostics);
        diagnoseCommandList(policyPath, "allowed_dhk_commands", raw.get("allowed_dhk_commands"), diagnostics);
        diagnoseCommandList(policyPath, "forbidden_dhk_commands", raw.get("forbidden_dhk_commands"), diagnostics);
        diagnosePathList(policyPath, "protected_files", raw.get("protected_files"), diagnostics);
        diagnosePathList(policyPath, "allowed_write_paths", raw.get("allowed_write_paths"), diagnostics);
        diagnosePathList(policyPath, "context_export_allowed_files", raw.get("context_export_allowed_files"), diagnostics);
        diagnosePathList(policyPath, "context_export_forbidden_files", raw.get("context_export_forbidden_files"), diagnostics);
        diagnoseBoolean(policyPath, raw, "db_sql_requires_explicit_request", diagnostics);
        diagnoseBoolean(policyPath, raw, "db_require_readonly_credentials", diagnostics);
        diagnoseBoolean(policyPath, raw, "context_export_require_sensitive_scan", diagnostics);
        diagnoseBoolean(policyPath, raw, "context_export_block_on_sensitive", diagnostics);
        diagnoseEnvironmentList(policyPath, raw.get("db_allowed_environments"), diagnostics);
        return diagnostics;
    }

    private void diagnoseMode(Path file, Map<String, String> raw, List<Diagnostic> diagnostics) {
        if (!raw.containsKey("mode")) {
            return;
        }
        String mode = value(raw, "mode", "");
        if (!MODES.contains(mode)) {
            diagnostics.add(warning(file.toString(), "mode should be one of: strict,guided,expert"));
        }
    }

    private void diagnoseCommandList(Path file, String field, String value, List<Diagnostic> diagnostics) {
        for (String item : diagnoseList(file, field, value, diagnostics)) {
            if (!COMMAND_PATTERN.matcher(item).matches()) {
                diagnostics.add(warning(file.toString(), field + " contains invalid command pattern: " + item));
            }
        }
    }

    private void diagnosePathList(Path file, String field, String value, List<Diagnostic> diagnostics) {
        for (String item : diagnoseList(file, field, value, diagnostics)) {
            if (item.startsWith("/") || item.startsWith("~") || item.indexOf("..") >= 0) {
                diagnostics.add(warning(file.toString(), field
                        + " should contain project-relative safe globs only: " + item));
            }
        }
    }

    private void diagnoseEnvironmentList(Path file, String value, List<Diagnostic> diagnostics) {
        for (String item : diagnoseList(file, "db_allowed_environments", value, diagnostics)) {
            if (!ENV_PATTERN.matcher(item).matches()) {
                diagnostics.add(warning(file.toString(),
                        "db_allowed_environments contains invalid environment key: " + item));
            }
        }
    }

    private void diagnoseBoolean(Path file, Map<String, String> raw, String field,
                                 List<Diagnostic> diagnostics) {
        if (raw.containsKey(field) && !validBoolean(raw.get(field))) {
            diagnostics.add(warning(file.toString(), field + " should be true/false, yes/no, or 1/0"));
        }
    }

    private List<String> diagnoseList(Path file, String field, String value, List<Diagnostic> diagnostics) {
        List<String> result = new ArrayList<String>();
        if (value == null) {
            return result;
        }
        if (value.trim().length() == 0) {
            diagnostics.add(warning(file.toString(), field + " is empty; default empty list will be used"));
            return result;
        }
        Set<String> seen = new LinkedHashSet<String>();
        String[] parts = value.split(",", -1);
        for (String part : parts) {
            String item = part.trim();
            if (item.length() == 0) {
                diagnostics.add(warning(file.toString(), field + " contains an empty item"));
                continue;
            }
            if (!seen.add(item)) {
                diagnostics.add(warning(file.toString(), field + " contains duplicate item: " + item));
            }
            result.add(item);
        }
        return result;
    }

    private void warnUnknownFields(Path file, Map<String, String> raw, List<Diagnostic> diagnostics) {
        List<String> unknown = new ArrayList<String>();
        for (String key : raw.keySet()) {
            if (!FIELDS.contains(key)) {
                unknown.add(key);
            }
        }
        if (!unknown.isEmpty()) {
            diagnostics.add(warning(file.toString(), "unknown fields: " + join(unknown)));
        }
    }

    private String[] splitList(String value) {
        if (value == null || value.trim().length() == 0) {
            return new String[0];
        }
        String[] parts = value.split(",");
        List<String> cleaned = new ArrayList<String>();
        for (String part : parts) {
            String text = part.trim();
            if (text.length() > 0) {
                cleaned.add(text);
            }
        }
        return cleaned.toArray(new String[cleaned.size()]);
    }

    private boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized) || "yes".equals(normalized) || "1".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "no".equals(normalized) || "0".equals(normalized)) {
            return false;
        }
        return defaultValue;
    }

    private boolean validBoolean(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return "true".equals(normalized) || "yes".equals(normalized) || "1".equals(normalized)
                || "false".equals(normalized) || "no".equals(normalized) || "0".equals(normalized);
    }

    private String value(Map<String, String> raw, String key, String defaultValue) {
        String value = raw.get(key);
        return value == null || value.trim().length() == 0 ? defaultValue : value.trim();
    }

    private Diagnostic warning(String location, String message) {
        return new Diagnostic("warning", location, message);
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

    private static Set<String> set(String... values) {
        Set<String> result = new LinkedHashSet<String>();
        for (String value : values) {
            result.add(value);
        }
        return result;
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
