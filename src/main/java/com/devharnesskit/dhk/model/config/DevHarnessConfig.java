package com.devharnesskit.dhk.model.config;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DevHarnessConfig {
    public static final String SCHEMA_VERSION = "devharness-config/v1-alpha";

    private final Map<String, String> values;

    public DevHarnessConfig(Map<String, String> values) {
        this.values = new LinkedHashMap<String, String>();
        if (values != null) {
            this.values.putAll(values);
        }
    }

    public Map<String, String> values() {
        return new LinkedHashMap<String, String>(values);
    }

    public String schemaVersion() {
        return value("schema_version", "");
    }

    public String preset() {
        return value("preset", "custom");
    }

    public String projectType() {
        return value("project.type", "unknown");
    }

    public String moduleStyle() {
        return value("project.module_style", "unknown");
    }

    public String runtime() {
        return value("project.runtime", "local");
    }

    public String compileMode() {
        return mode("verification.compile.mode");
    }

    public String compileCommand() {
        return value("verification.compile.command", "");
    }

    public String compileTrigger() {
        return value("verification.compile.manual_trigger", "");
    }

    public String testMode() {
        return mode("verification.test.mode");
    }

    public String testCommand() {
        return value("verification.test.command", "");
    }

    public String testTrigger() {
        return value("verification.test.manual_trigger", "");
    }

    public String testCost() {
        return value("verification.test.expected_duration", "");
    }

    public boolean graphRequired() {
        String mode = graphMode();
        if ("required".equals(mode)) {
            return true;
        }
        if ("advisory".equals(mode) || "off".equals(mode)) {
            return false;
        }
        return booleanValue("verification.graph.required", false);
    }

    public String graphMode() {
        String configured = value("verification.graph.mode", "");
        if (configured.length() > 0) {
            return configured.toLowerCase();
        }
        return booleanValue("verification.graph.required", false) ? "required" : "off";
    }

    public boolean graphFreshSnapshotRequired() {
        return booleanValue("verification.graph.fresh_snapshot_required", graphRequired());
    }

    public boolean impactMapRequired() {
        return booleanValue("verification.graph.impact_map_required", graphRequired());
    }

    public boolean allowStaleRequiresApproval() {
        return booleanValue("verification.graph.allow_stale_requires_approval", true);
    }

    public boolean preWorkConfirmationRequired() {
        return booleanValue("workflow.pre_work.confirmation.required", false);
    }

    public String preWorkConfirmationReason() {
        return value("workflow.pre_work.confirmation.reason", "项目配置要求实施前确认任务边界和推进顺序。");
    }

    public int contextBudgetTotalTokens(int defaultValue) {
        return positiveIntValue("context.budget.total_tokens", defaultValue);
    }

    public int contextBudget(String section, int defaultValue) {
        String normalized = section == null ? "" : section.trim().toLowerCase().replace('-', '_');
        if (normalized.length() == 0) {
            return defaultValue;
        }
        String primary = "context.budget." + normalized;
        String legacy = primary + "_tokens";
        String raw = value(primary, "");
        if (raw.length() == 0) {
            raw = value(legacy, "");
        }
        return positiveInt(raw, defaultValue);
    }

    public int contextOutputHeadroomTokens(int defaultValue) {
        return positiveIntValue("context.output.headroom_tokens", defaultValue);
    }

    public boolean contextCompressEnabled() {
        String evidence = value("context.compress.evidence", "");
        if (evidence.length() > 0) {
            return booleanText(evidence, true);
        }
        return booleanValue("context.compress.enabled", true);
    }

    public boolean demoMode() {
        return initialProjectMode();
    }

    public String demoWarning() {
        return initialProjectWarning();
    }

    public boolean initialProjectMode() {
        return booleanValue("verification.initial_project.enabled", false)
                || booleanValue("verification.demo.enabled", false)
                || "initial-new-project".equals(preset())
                || "demo-no-build".equals(preset());
    }

    public String initialProjectWarning() {
        String warning = value("verification.initial_project.warning", "");
        if (warning.length() > 0) {
            return warning;
        }
        return value("verification.demo.warning",
                "initial project mode does not prove production correctness until real compile/test evidence exists");
    }

    public String value(String key, String defaultValue) {
        String value = values.get(key);
        return value == null || value.trim().length() == 0 ? defaultValue : value.trim();
    }

    private String mode(String key) {
        return value(key, "auto").toLowerCase();
    }

    private boolean booleanValue(String key, boolean defaultValue) {
        String value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        return booleanText(value, defaultValue);
    }

    private boolean booleanText(String value, boolean defaultValue) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if ("true".equals(normalized) || "yes".equals(normalized) || "1".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized) || "no".equals(normalized) || "0".equals(normalized)) {
            return false;
        }
        return defaultValue;
    }

    private int positiveIntValue(String key, int defaultValue) {
        return positiveInt(value(key, ""), defaultValue);
    }

    private int positiveInt(String raw, int defaultValue) {
        try {
            int parsed = Integer.parseInt(raw);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
