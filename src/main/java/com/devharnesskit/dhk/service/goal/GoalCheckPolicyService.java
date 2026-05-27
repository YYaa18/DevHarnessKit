package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.config.DevHarnessConfig;
import com.devharnesskit.dhk.service.config.DevHarnessConfigService;
import com.devharnesskit.dhk.util.JsonUtil;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GoalCheckPolicyService {
    private final DevHarnessConfigService configService = new DevHarnessConfigService();

    public GoalCheckPolicy load(Path projectRoot) {
        if (projectRoot == null) {
            return GoalCheckPolicy.defaults();
        }
        DevHarnessConfig config = loadConfig(projectRoot);
        Path policyPath = PathUtil.goalCheckPolicy(projectRoot);
        if (!Files.isRegularFile(policyPath)) {
            return withConfig(GoalCheckPolicy.defaults(), config, new String[0], new String[0], new String[0],
                    false, false, new String[0], new String[0], new String[0], new String[0],
                    new String[0], new String[0], new String[0], new String[0], new String[0],
                    100, 0, false, false);
        }
        try {
            Map<String, String> raw = JsonUtil.parseObject(new String(Files.readAllBytes(policyPath), "UTF-8"));
            String[] requiredChecks = splitList(raw.get("required_checks"));
            String[] compileCommand = firstNonEmpty(splitCommand(raw.get("compile_command")),
                    splitCommand(config.compileCommand()));
            String[] testCommand = firstNonEmpty(splitCommand(raw.get("test_command")),
                    splitCommand(config.testCommand()));
            boolean failPendingHardGates = parseBoolean(raw.get("fail_pending_hard_gates"), false);
            return new GoalCheckPolicy(requiredChecks, raw.containsKey("required_checks"),
                    compileCommand, testCommand, failPendingHardGates, raw.containsKey("fail_pending_hard_gates"),
                    splitList(raw.get("accepted_compile_statuses")),
                    splitList(raw.get("accepted_test_statuses")),
                    splitList(raw.get("accepted_sensitive_statuses")),
                    splitList(raw.get("accepted_spec_statuses")),
                    splitList(raw.get("accepted_workflow_statuses")),
                    splitList(raw.get("accepted_graph_statuses")),
                    splitList(raw.get("accepted_impact_statuses")),
                    splitList(raw.get("accepted_architecture_statuses")),
                    splitList(raw.get("accepted_bdd_statuses")),
                    parsePercent(raw.get("bdd_min_coverage_percent"), 100),
                    parsePercent(raw.get("bdd_min_quality_score"), 0),
                    parseBoolean(raw.get("bdd_fail_on_quality_errors"), false),
                    parseBoolean(raw.get("bdd_fail_on_quality_warnings"), false),
                    config.compileMode(), config.testMode());
        } catch (Exception ex) {
            return withConfig(GoalCheckPolicy.defaults(), config, new String[0], new String[0], new String[0],
                    false, false, new String[0], new String[0], new String[0], new String[0],
                    new String[0], new String[0], new String[0], new String[0], new String[0],
                    100, 0, false, false);
        }
    }

    private GoalCheckPolicy withConfig(GoalCheckPolicy fallback, DevHarnessConfig config,
                                       String[] requiredChecks, String[] compileCommand, String[] testCommand,
                                       boolean requiredChecksConfigured, boolean failPendingHardGates,
                                       String[] acceptedCompileStatuses, String[] acceptedTestStatuses,
                                       String[] acceptedSensitiveStatuses, String[] acceptedSpecStatuses,
                                       String[] acceptedWorkflowStatuses, String[] acceptedGraphStatuses,
                                       String[] acceptedImpactStatuses, String[] acceptedArchitectureStatuses,
                                       String[] acceptedBddStatuses, int bddMinCoveragePercent,
                                       int bddMinQualityScore, boolean bddFailOnQualityErrors,
                                       boolean bddFailOnQualityWarnings) {
        return new GoalCheckPolicy(requiredChecks.length == 0 ? fallback.requiredChecks() : requiredChecks,
                requiredChecksConfigured,
                firstNonEmpty(compileCommand, splitCommand(config.compileCommand())),
                firstNonEmpty(testCommand, splitCommand(config.testCommand())),
                failPendingHardGates, false,
                acceptedCompileStatuses, acceptedTestStatuses, acceptedSensitiveStatuses,
                acceptedSpecStatuses, acceptedWorkflowStatuses, acceptedGraphStatuses,
                acceptedImpactStatuses, acceptedArchitectureStatuses, acceptedBddStatuses,
                bddMinCoveragePercent, bddMinQualityScore, bddFailOnQualityErrors, bddFailOnQualityWarnings,
                config.compileMode(), config.testMode());
    }

    private DevHarnessConfig loadConfig(Path projectRoot) {
        try {
            return configService.load(projectRoot);
        } catch (Exception ex) {
            return new DevHarnessConfig(new java.util.LinkedHashMap<String, String>());
        }
    }

    private String[] firstNonEmpty(String[] first, String[] second) {
        return first != null && first.length > 0 ? first : (second == null ? new String[0] : second);
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

    private String[] splitCommand(String value) {
        if (value == null || value.trim().length() == 0) {
            return new String[0];
        }
        String[] parts = value.trim().split("\\s+");
        List<String> cleaned = new ArrayList<String>();
        for (String part : parts) {
            if (part.length() > 0) {
                cleaned.add(part);
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

    private int parsePercent(String value, int defaultValue) {
        if (value == null || value.trim().length() == 0) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 0 || parsed > 100) {
                return defaultValue;
            }
            return parsed;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
