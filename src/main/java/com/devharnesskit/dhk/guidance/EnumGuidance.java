package com.devharnesskit.dhk.guidance;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.CommandContext;

public final class EnumGuidance {
    public static final String[] MEMORY_STATUSES = values("draft", "confirmed", "deprecated", "archived");
    public static final String[] MEMORY_TYPES = values("project_fact", "api_convention", "mvc_convention",
            "gateway_convention", "database_convention", "code_pattern", "module_pattern",
            "exception_convention", "logging_convention", "security_convention", "testing_convention",
            "decision", "risk", "todo");
    public static final String[] SPEC_MODES = values("auto", "api", "mvc", "mixed", "sql", "debug", "review");
    public static final String[] SPEC_PRIORITIES = values("low", "normal", "high", "critical");
    public static final String[] SPEC_DOCUMENT_TYPES = values("proposal", "design", "requirements", "tasks",
            "acceptance", "verification", "notes");
    public static final String[] SPEC_DOCUMENT_STATUSES = MEMORY_STATUSES;
    public static final String[] SPEC_WORKFLOW_BINDING_TYPES = values("implements", "reviews", "verifies",
            "archives");
    public static final String[] SPEC_TASK_STATUSES = values("pending", "in_progress", "done", "blocked",
            "skipped");
    public static final String[] SPEC_TASK_ALIASES = values("complete -> done", "completed -> done",
            "closed -> done", "working -> in_progress", "skip -> skipped");
    public static final String[] WORKFLOW_MODES = SPEC_MODES;
    public static final String[] WORKFLOW_GATE_ACTIONS = values("pass", "fail", "waive");
    public static final String[] WORKFLOW_PHASE_ACTIONS = values("pass", "fail");
    public static final String[] WORKFLOW_MEMORY_BINDING_TYPES = values("read", "exported", "suggested",
            "created_draft", "confirmed_after_run", "deprecated_after_run");
    public static final String[] WORKFLOW_CHECKPOINT_BINDING_TYPES = values("created", "recovered_from", "updated");
    public static final String[] VERIFICATION_MODES = values("auto", "manual", "disabled");
    public static final String[] RECOMMENDATION_MODES = values("recommend", "patch", "standard", "strict",
            "ask", "analyze-only", "analyze_only", "full", "api", "mvc", "mixed", "sql", "debug", "review");
    public static final String[] GOAL_VERIFY_LEVELS = values("fast", "standard", "release");
    public static final String[] GRAPH_MODES = values("off", "advisory", "required", "optional", "disabled");
    public static final String[] CONFIGURE_PRESETS = values("springboot-manual-ide-test",
            "springboot-auto-test", "springboot-ci-only-test", "legacy-java-small-fix",
            "legacy-jsp-servlet", "mybatis-monolith-manual-test", "graph-advisory",
            "safe-refactor-graph", "initial-new-project", "manual-ide-test");
    public static final String[] BDD_SCENARIO_TYPES = values("acceptance", "edge_case", "regression",
            "manual", "exploratory");
    public static final String[] BDD_SCENARIO_STATUSES = values("draft", "active", "implemented",
            "verified", "blocked", "deprecated", "archived");
    public static final String[] BDD_EVIDENCE_TYPES = values("manual", "test", "review", "screenshot",
            "command", "artifact");
    public static final String[] BDD_EVIDENCE_STATUSES = values("pending", "passed", "failed", "skipped",
            "waived");

    private static final ActionableErrorRenderer RENDERER = new ActionableErrorRenderer();

    private EnumGuidance() {
    }

    public static int printInvalid(CommandContext context, Args args, String errorCode, String label,
                                   String value, String[] validValues, String[] aliases,
                                   String nextCommand, String docs) {
        ActionableError error = ActionableError.builder(errorCode, "Invalid " + label + ": " + safe(value))
                .reason(label + " must be one of the documented values.")
                .validValues(validValues)
                .aliases(aliases)
                .nextCommand(nextCommand)
                .docs(docs)
                .build();
        if (com.devharnesskit.dhk.util.JsonOutput.enabled(args)) {
            context.err().print(RENDERER.renderJson(error));
        } else {
            context.err().print(RENDERER.renderText(error));
        }
        return com.devharnesskit.dhk.cli.ExitCodes.VALIDATION_ERROR;
    }

    public static String normalizeSpecTaskStatus(String status) {
        String normalized = normalize(status);
        if ("complete".equals(normalized) || "completed".equals(normalized) || "closed".equals(normalized)) {
            return "done";
        }
        if ("working".equals(normalized) || "started".equals(normalized)) {
            return "in_progress";
        }
        if ("skip".equals(normalized)) {
            return "skipped";
        }
        return normalized;
    }

    public static String normalizeRecommendationMode(String mode) {
        String normalized = normalize(mode).replace('_', '-');
        if (normalized.length() == 0 || "auto".equals(normalized)) {
            return "recommend";
        }
        if ("analyze-only".equals(normalized)) {
            return "analyze-only";
        }
        return normalized;
    }

    public static boolean isRecommendationModeAllowed(String mode) {
        String normalized = normalizeRecommendationMode(mode);
        return "recommend".equals(normalized) || "patch".equals(normalized) || "standard".equals(normalized)
                || "strict".equals(normalized) || "ask".equals(normalized)
                || "analyze-only".equals(normalized) || "full".equals(normalized)
                || "api".equals(normalized) || "mvc".equals(normalized) || "mixed".equals(normalized)
                || "sql".equals(normalized) || "debug".equals(normalized) || "review".equals(normalized);
    }

    public static boolean isVerificationModeAllowed(String mode) {
        String normalized = normalize(mode);
        return normalized.length() == 0 || "auto".equals(normalized) || "manual".equals(normalized)
                || "disabled".equals(normalized);
    }

    public static boolean isGraphModeAllowed(String mode) {
        String normalized = normalize(mode);
        return normalized.length() == 0 || "off".equals(normalized) || "advisory".equals(normalized)
                || "required".equals(normalized) || "optional".equals(normalized)
                || "disabled".equals(normalized);
    }

    public static boolean isConfigurePresetAllowed(String preset) {
        String normalized = normalize(preset);
        if (normalized.length() == 0 || "auto".equals(normalized)) {
            return true;
        }
        if ("demo-no-build".equals(normalized)) {
            return true;
        }
        for (String value : CONFIGURE_PRESETS) {
            if (value.equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String[] values(String... values) {
        return values;
    }
}
