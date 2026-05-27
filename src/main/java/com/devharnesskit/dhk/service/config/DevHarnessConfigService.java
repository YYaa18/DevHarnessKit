package com.devharnesskit.dhk.service.config;

import com.devharnesskit.dhk.model.config.ConfigureInitResult;
import com.devharnesskit.dhk.model.config.DevHarnessConfig;
import com.devharnesskit.dhk.util.JsonUtil;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DevHarnessConfigService {
    private static final Set<String> MODES = set("auto", "manual", "disabled");
    private static final Set<String> KNOWN_FIELDS = set(
            "schema_version", "preset", "project.type", "project.module_style", "project.runtime",
            "verification.compile.mode", "verification.compile.command",
            "verification.compile.reason", "verification.compile.manual_trigger",
            "verification.compile.required_evidence",
            "verification.test.mode", "verification.test.command",
            "verification.test.reason", "verification.test.manual_trigger",
            "verification.test.expected_duration", "verification.test.required_evidence",
            "verification.graph.mode", "verification.graph.required", "verification.graph.fresh_snapshot_required",
            "verification.graph.impact_map_required", "verification.graph.allow_stale_requires_approval",
            "verification.architecture.mode", "verification.rollback.required_when_auto_tests_unavailable",
            "adapter.target");

    public ConfigureInitResult init(Path projectRoot, String preset, boolean force,
                                    String compileOverride, String testOverride,
                                    String graphOverride, String adapterTarget) throws IOException {
        return init(projectRoot, preset, force, compileOverride, testOverride, graphOverride, adapterTarget, false);
    }

    public ConfigureInitResult init(Path projectRoot, String preset, boolean force,
                                    String compileOverride, String testOverride,
                                    String graphOverride, String adapterTarget,
                                    boolean dryRun) throws IOException {
        String normalizedPreset = normalizePreset(preset);
        Map<String, String> values = presetValues(normalizedPreset);
        applyModeOverride(values, "verification.compile.mode", compileOverride);
        applyModeOverride(values, "verification.test.mode", testOverride);
        applyGraphOverride(values, graphOverride);
        if (adapterTarget != null && adapterTarget.trim().length() > 0) {
            values.put("adapter.target", adapterTarget.trim());
        }

        Path configPath = PathUtil.devharnessConfig(projectRoot);
        boolean exists = Files.exists(configPath);
        if (dryRun) {
            return new ConfigureInitResult(configPath, normalizedPreset, false, new DevHarnessConfig(values),
                    true, !exists || force, exists && !force);
        }
        if (Files.exists(configPath) && !force) {
            throw new IllegalStateException(configPath + " already exists. Use --force to overwrite.");
        }
        Files.createDirectories(configPath.getParent());
        Files.write(configPath, JsonUtil.toObject(values).getBytes("UTF-8"));
        return new ConfigureInitResult(configPath, normalizedPreset, true, new DevHarnessConfig(values));
    }

    public boolean hasConfig(Path projectRoot) {
        return Files.isRegularFile(PathUtil.devharnessConfig(projectRoot));
    }

    public DevHarnessConfig load(Path projectRoot) throws IOException {
        Path configPath = PathUtil.devharnessConfig(projectRoot);
        if (!Files.isRegularFile(configPath)) {
            return new DevHarnessConfig(defaultValues("default"));
        }
        return new DevHarnessConfig(JsonUtil.parseObject(new String(Files.readAllBytes(configPath), "UTF-8")));
    }

    public List<String> diagnose(Path projectRoot) {
        List<String> warnings = new ArrayList<String>();
        Path configPath = PathUtil.devharnessConfig(projectRoot);
        if (!Files.isRegularFile(configPath)) {
            warnings.add("config missing: " + configPath);
            return warnings;
        }

        Map<String, String> values;
        try {
            values = JsonUtil.parseObject(read(configPath));
        } catch (Exception ex) {
            warnings.add("invalid JSON; configure defaults cannot be trusted: " + ex.getMessage());
            return warnings;
        }

        String schema = value(values, "schema_version", "");
        if (!DevHarnessConfig.SCHEMA_VERSION.equals(schema)) {
            warnings.add("schema_version should be " + DevHarnessConfig.SCHEMA_VERSION);
        }
        warnUnknown(values, warnings);
        diagnoseMode(values, "verification.compile.mode", warnings);
        diagnoseMode(values, "verification.test.mode", warnings);
        diagnoseGraphMode(values, warnings);
        diagnoseBoolean(values, "verification.graph.required", warnings);
        diagnoseBoolean(values, "verification.graph.fresh_snapshot_required", warnings);
        diagnoseBoolean(values, "verification.graph.impact_map_required", warnings);
        diagnoseBoolean(values, "verification.graph.allow_stale_requires_approval", warnings);
        diagnoseManualEvidence(values, "verification.compile", warnings);
        diagnoseManualEvidence(values, "verification.test", warnings);
        return warnings;
    }

    public String explain(String key) {
        String normalized = key == null ? "" : key.trim();
        if (normalized.length() == 0) {
            return "Available keys: verification.compile.mode, verification.test.mode, "
                    + "verification.graph.required, verification.graph.allow_stale_requires_approval, "
                    + "manual_evidence";
        }
        if ("verification.compile.mode".equals(normalized)) {
            return "verification.compile.mode controls compile verification: auto runs a CLI command, "
                    + "manual requires human/IDE/CI evidence, disabled requires alternate risk controls.";
        }
        if ("verification.test.mode".equals(normalized)) {
            return "verification.test.mode controls test verification: auto runs tests, manual requires "
                    + "manual_evidence_status=passed plus scope and evidence path, disabled must be paired "
                    + "with rollback/risk evidence.";
        }
        if ("manual_evidence".equals(normalized)) {
            return "Manual verification evidence should include manual_evidence_status=passed, "
                    + "compile_scope or test_scope, manual_evidence_path, and tester when available.";
        }
        if ("verification.graph.required".equals(normalized)) {
            return "verification.graph.required tells goal flows to use graph-aware preflight and impact evidence.";
        }
        if ("verification.graph.mode".equals(normalized)) {
            return "verification.graph.mode is off, advisory, or required. Advisory lets tools surface graph context "
                    + "without making graph checks completion gates.";
        }
        if ("verification.graph.allow_stale_requires_approval".equals(normalized)) {
            return "verification.graph.allow_stale_requires_approval defaults to true; weak-model skills must not "
                    + "use --allow-stale without explicit policy or human approval evidence.";
        }
        return "Unknown configure key: " + normalized;
    }

    public Map<String, String> presetValues(String preset) {
        String normalized = normalizePreset(preset);
        if ("springboot-auto-test".equals(normalized)) {
            Map<String, String> values = defaultValues(normalized);
            values.put("project.type", "springboot-api");
            values.put("project.runtime", "local-cli");
            values.put("verification.compile.mode", "auto");
            values.put("verification.compile.command", "mvn -q -DskipTests compile");
            values.put("verification.test.mode", "auto");
            values.put("verification.test.command", "mvn -q test");
            values.put("verification.test.expected_duration", "fast");
            values.put("verification.graph.mode", "off");
            values.put("verification.graph.required", "false");
            values.put("verification.graph.fresh_snapshot_required", "false");
            values.put("verification.graph.impact_map_required", "false");
            return values;
        }
        if ("springboot-manual-ide-test".equals(normalized)) {
            Map<String, String> values = defaultValues(normalized);
            values.put("project.type", "springboot-enterprise-large");
            values.put("project.runtime", "company-environment");
            values.put("verification.compile.mode", "manual");
            values.put("verification.compile.reason", "mvn compile requires company-specific environment");
            values.put("verification.compile.manual_trigger", "IDE build action");
            values.put("verification.compile.required_evidence",
                    "manual_evidence_status=passed,compile_scope,manual_evidence_path");
            values.put("verification.test.mode", "manual");
            values.put("verification.test.reason", "mvn test is not the trusted local verification entry");
            values.put("verification.test.manual_trigger", "IDE test button");
            values.put("verification.test.expected_duration", "10m+ per test");
            values.put("verification.test.required_evidence",
                    "manual_evidence_status=passed,test_scope,manual_evidence_path");
            values.put("verification.graph.mode", "required");
            values.put("verification.graph.required", "true");
            values.put("verification.graph.fresh_snapshot_required", "true");
            values.put("verification.graph.impact_map_required", "true");
            return values;
        }
        if ("springboot-ci-only-test".equals(normalized)) {
            Map<String, String> values = presetValues("springboot-manual-ide-test");
            values.put("preset", normalized);
            values.put("verification.test.manual_trigger", "CI pipeline");
            values.put("verification.test.reason", "tests are only trusted in CI");
            values.put("verification.test.expected_duration", "slow");
            return values;
        }
        if ("legacy-java-small-fix".equals(normalized)
                || "legacy-jsp-servlet".equals(normalized)
                || "mybatis-monolith-manual-test".equals(normalized)) {
            Map<String, String> values = defaultValues(normalized);
            values.put("project.type", normalized);
            values.put("project.runtime", "legacy-company-environment");
            values.put("verification.compile.mode", "manual");
            values.put("verification.compile.manual_trigger", "IDE or company build");
            values.put("verification.compile.required_evidence",
                    "manual_evidence_status=passed,compile_scope,manual_evidence_path");
            values.put("verification.test.mode", "manual");
            values.put("verification.test.manual_trigger", "manual regression or CI evidence");
            values.put("verification.test.required_evidence",
                    "manual_evidence_status=passed,test_scope,manual_evidence_path");
            values.put("verification.graph.mode", "required");
            values.put("verification.graph.required", "true");
            values.put("verification.graph.fresh_snapshot_required", "true");
            values.put("verification.graph.impact_map_required", "true");
            values.put("verification.rollback.required_when_auto_tests_unavailable", "true");
            return values;
        }
        if ("graph-advisory".equals(normalized)) {
            Map<String, String> values = presetValues("springboot-auto-test");
            values.put("preset", normalized);
            values.put("verification.graph.mode", "advisory");
            values.put("verification.graph.required", "false");
            values.put("verification.graph.fresh_snapshot_required", "false");
            values.put("verification.graph.impact_map_required", "false");
            return values;
        }
        if ("safe-refactor-graph".equals(normalized)) {
            Map<String, String> values = presetValues("springboot-auto-test");
            values.put("preset", normalized);
            values.put("verification.graph.mode", "required");
            values.put("verification.graph.required", "true");
            values.put("verification.graph.fresh_snapshot_required", "true");
            values.put("verification.graph.impact_map_required", "true");
            return values;
        }
        throw new IllegalArgumentException("unknown preset: " + preset);
    }

    private Map<String, String> defaultValues(String preset) {
        Map<String, String> values = new LinkedHashMap<String, String>();
        values.put("schema_version", DevHarnessConfig.SCHEMA_VERSION);
        values.put("preset", preset);
        values.put("project.type", "unknown");
        values.put("project.module_style", "api");
        values.put("project.runtime", "local");
        values.put("verification.compile.mode", "auto");
        values.put("verification.compile.command", "mvn -q -DskipTests compile");
        values.put("verification.test.mode", "auto");
        values.put("verification.test.command", "mvn -q test");
        values.put("verification.graph.mode", "off");
        values.put("verification.graph.required", "false");
        values.put("verification.graph.fresh_snapshot_required", "false");
        values.put("verification.graph.impact_map_required", "false");
        values.put("verification.graph.allow_stale_requires_approval", "true");
        values.put("verification.architecture.mode", "warn");
        values.put("verification.rollback.required_when_auto_tests_unavailable", "true");
        return values;
    }

    private String normalizePreset(String preset) {
        if (preset == null || preset.trim().length() == 0 || "auto".equalsIgnoreCase(preset.trim())) {
            return "springboot-manual-ide-test";
        }
        String normalized = preset.trim().toLowerCase(Locale.ROOT);
        if ("manual-ide-test".equals(normalized)) {
            return "springboot-manual-ide-test";
        }
        return normalized;
    }

    private void applyModeOverride(Map<String, String> values, String key, String mode) {
        if (mode == null || mode.trim().length() == 0) {
            return;
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        if (!MODES.contains(normalized)) {
            throw new IllegalArgumentException(key + " must be auto, manual, or disabled");
        }
        values.put(key, normalized);
    }

    private void applyGraphOverride(Map<String, String> values, String graph) {
        if (graph == null || graph.trim().length() == 0) {
            return;
        }
        String normalized = graph.trim().toLowerCase(Locale.ROOT);
        if ("required".equals(normalized)) {
            values.put("verification.graph.mode", "required");
            values.put("verification.graph.required", "true");
            values.put("verification.graph.fresh_snapshot_required", "true");
            values.put("verification.graph.impact_map_required", "true");
        } else if ("advisory".equals(normalized) || "optional".equals(normalized)) {
            values.put("verification.graph.mode", "advisory");
            values.put("verification.graph.required", "false");
            values.put("verification.graph.fresh_snapshot_required", "false");
            values.put("verification.graph.impact_map_required", "false");
        } else if ("off".equals(normalized) || "disabled".equals(normalized)) {
            values.put("verification.graph.mode", "off");
            values.put("verification.graph.required", "false");
            values.put("verification.graph.fresh_snapshot_required", "false");
            values.put("verification.graph.impact_map_required", "false");
        } else {
            throw new IllegalArgumentException("--graph must be required, advisory, optional, or off");
        }
    }

    private void diagnoseMode(Map<String, String> values, String key, List<String> warnings) {
        String mode = value(values, key, "");
        if (mode.length() == 0) {
            warnings.add(key + " is required");
        } else if (!MODES.contains(mode.toLowerCase(Locale.ROOT))) {
            warnings.add(key + " should be one of: auto,manual,disabled");
        }
    }

    private void diagnoseManualEvidence(Map<String, String> values, String prefix, List<String> warnings) {
        String mode = value(values, prefix + ".mode", "");
        if (!"manual".equalsIgnoreCase(mode)) {
            return;
        }
        if (value(values, prefix + ".manual_trigger", "").length() == 0) {
            warnings.add(prefix + ".manual_trigger is required when mode is manual");
        }
        String evidence = value(values, prefix + ".required_evidence", "");
        if (evidence.indexOf("manual_evidence_status=passed") < 0
                || evidence.indexOf("manual_evidence_path") < 0) {
            warnings.add(prefix + ".required_evidence should include manual_evidence_status=passed and manual_evidence_path");
        }
    }

    private void diagnoseBoolean(Map<String, String> values, String key, List<String> warnings) {
        if (!values.containsKey(key)) {
            return;
        }
        String value = value(values, key, "");
        if (!validBoolean(value)) {
            warnings.add(key + " should be true/false, yes/no, or 1/0");
        }
    }

    private boolean validBoolean(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return "true".equals(normalized) || "yes".equals(normalized) || "1".equals(normalized)
                || "false".equals(normalized) || "no".equals(normalized) || "0".equals(normalized);
    }

    private void diagnoseGraphMode(Map<String, String> values, List<String> warnings) {
        if (!values.containsKey("verification.graph.mode")) {
            return;
        }
        String mode = value(values, "verification.graph.mode", "").toLowerCase(Locale.ROOT);
        if (!"off".equals(mode) && !"advisory".equals(mode) && !"required".equals(mode)) {
            warnings.add("verification.graph.mode should be one of: off,advisory,required");
        }
    }

    private void warnUnknown(Map<String, String> values, List<String> warnings) {
        List<String> unknown = new ArrayList<String>();
        for (String key : values.keySet()) {
            if (!KNOWN_FIELDS.contains(key)) {
                unknown.add(key);
            }
        }
        if (!unknown.isEmpty()) {
            warnings.add("unknown fields: " + join(unknown));
        }
    }

    private String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), "UTF-8");
    }

    private String value(Map<String, String> values, String key, String defaultValue) {
        String value = values.get(key);
        return value == null || value.trim().length() == 0 ? defaultValue : value.trim();
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
}
