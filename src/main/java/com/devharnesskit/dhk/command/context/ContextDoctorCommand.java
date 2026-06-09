package com.devharnesskit.dhk.command.context;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.context.ContextBudget;
import com.devharnesskit.dhk.context.ContextBudgetPolicy;
import com.devharnesskit.dhk.context.artifact.ContextArtifact;
import com.devharnesskit.dhk.context.artifact.ContextArtifactRepository;
import com.devharnesskit.dhk.context.token.CharsOverFourTokenEstimator;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.service.config.DevHarnessConfigService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ContextDoctorCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final ContextArtifactRepository repository = new ContextArtifactRepository();
    private final DevHarnessConfigService configService = new DevHarnessConfigService();
    private final CharsOverFourTokenEstimator estimator = new CharsOverFourTokenEstimator();

    public int run(CommandContext context, Args args) {
        Path projectRoot = ContextCommandSupport.projectRoot(args, context);
        String goalKey = args.option("goal", "").trim();
        if (!ContextCommandSupport.requireProjectJson(context, projectRoot)) {
            return ExitCodes.NOT_FOUND;
        }
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = ContextCommandSupport.requireProject(context, projectRoot, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            List<DoctorCheck> checks = inspect(projectRoot, connection, project, goalKey);
            boolean failed = hasFailed(checks);
            if (JsonOutput.enabled(args)) {
                printJson(context, project, goalKey, checks, failed);
            } else {
                for (DoctorCheck check : checks) {
                    context.out().println(check.key + ": " + check.status + " - " + check.message);
                }
                context.out().println("decision: " + (failed ? "failed" : "passed"));
            }
            return failed ? ExitCodes.VALIDATION_ERROR : ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR context doctor failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private List<DoctorCheck> inspect(Path projectRoot, Connection connection, Project project, String goalKey)
            throws Exception {
        List<DoctorCheck> checks = new ArrayList<DoctorCheck>();
        ContextBudget budget = ContextBudgetPolicy.fromConfig(configService.load(projectRoot));
        inspectCurrentContext(projectRoot, budget, checks);
        inspectGoalContext(projectRoot, checks);
        inspectArtifacts(connection, project, goalKey, checks);
        return checks;
    }

    private void inspectCurrentContext(Path projectRoot, ContextBudget budget, List<DoctorCheck> checks)
            throws Exception {
        Path current = PathUtil.currentContext(projectRoot);
        if (!Files.isRegularFile(current)) {
            checks.add(new DoctorCheck("current-context-present", "failed",
                    "CURRENT_CONTEXT.md is missing; run context render or memory export"));
            return;
        }
        String text = new String(Files.readAllBytes(current), "UTF-8");
        String lower = text.toLowerCase(Locale.ROOT);
        int tokens = estimator.estimate(text);
        checks.add(new DoctorCheck("total-budget", tokens <= budget.totalTokens() ? "passed" : "failed",
                "estimated_tokens=" + tokens + " budget=" + budget.totalTokens()));
        checks.add(new DoctorCheck("context-budget-report",
                text.contains("<context-budget-report>") ? "passed" : "failed",
                "CURRENT_CONTEXT.md should include <context-budget-report>"));
        checks.add(new DoctorCheck("truncation-report",
                text.contains("<truncation-report>") ? "passed" : "failed",
                "CURRENT_CONTEXT.md should keep <truncation-report> anchor"));
        checks.add(new DoctorCheck("unconfirmed-memory-default",
                containsUnconfirmedMemory(lower) ? "failed" : "passed",
                "default context must not include unconfirmed draft memory"));
        checks.add(new DoctorCheck("required-evidence-visible",
                lower.indexOf("required_evidence") >= 0
                        && (lower.indexOf("required evidence omitted") >= 0
                        || lower.indexOf("bdd_evidence omitted") >= 0) ? "failed" : "passed",
                "required evidence must not be marked omitted in context"));
    }

    private void inspectGoalContext(Path projectRoot, List<DoctorCheck> checks) throws Exception {
        Path goalContext = PathUtil.goalContext(projectRoot);
        if (!Files.isRegularFile(goalContext)) {
            checks.add(new DoctorCheck("goal-context-present", "failed",
                    "GOAL_CONTEXT.md is missing; run context render for goal-specific checks"));
            return;
        }
        String text = new String(Files.readAllBytes(goalContext), "UTF-8");
        String lower = text.toLowerCase(Locale.ROOT);
        boolean staleGraph = lower.indexOf("snapshot_stale: true") >= 0
                || lower.indexOf("graph_snapshot: status=stale") >= 0;
        boolean explicitMarker = lower.indexOf("stale_graph_snapshot") >= 0
                || lower.indexOf("graph_snapshot: status=stale") >= 0;
        checks.add(new DoctorCheck("stale-graph-visible",
                staleGraph && !explicitMarker ? "failed" : "passed",
                "stale graph context must be explicitly marked when present"));
    }

    private void inspectArtifacts(Connection connection, Project project, String goalKey, List<DoctorCheck> checks)
            throws Exception {
        List<ContextArtifact> artifacts = repository.list(connection, project.projectKey(), goalKey, 200);
        int missing = 0;
        int invertedTokens = 0;
        int highPriorityCompressed = 0;
        int requiredEvidenceOmitted = 0;
        for (ContextArtifact artifact : artifacts) {
            if (artifact.originalText().length() == 0 && !isReadableArtifactPath(artifact.sourcePath())) {
                missing++;
            }
            if (artifact.tokenBefore() > 0 && artifact.tokenAfter() > artifact.tokenBefore()) {
                invertedTokens++;
            }
            if (isHighPrioritySource(artifact.sourceType())
                    && (artifact.omittedLines() > 0 || artifact.tokenAfter() < artifact.tokenBefore())) {
                highPriorityCompressed++;
            }
            if (isRequiredEvidenceSource(artifact.sourceType())
                    && (artifact.omittedLines() > 0
                    || artifact.compressedText().toLowerCase(Locale.ROOT).indexOf("degraded: true") >= 0)) {
                requiredEvidenceOmitted++;
            }
        }
        checks.add(new DoctorCheck("artifact-original-ref", missing == 0 ? "passed" : "failed",
                "artifacts_missing_original_ref=" + missing));
        checks.add(new DoctorCheck("artifact-token-accounting", invertedTokens == 0 ? "passed" : "failed",
                "artifacts_with_token_after_gt_before=" + invertedTokens));
        checks.add(new DoctorCheck("high-priority-not-compressed",
                highPriorityCompressed == 0 ? "passed" : "failed",
                "high_priority_artifacts_compressed=" + highPriorityCompressed));
        checks.add(new DoctorCheck("required-evidence-not-omitted",
                requiredEvidenceOmitted == 0 ? "passed" : "failed",
                "required_evidence_artifacts_omitted_or_degraded=" + requiredEvidenceOmitted));
    }

    private boolean containsUnconfirmedMemory(String lower) {
        return lower.indexOf("status=draft") >= 0
                || lower.indexOf("status: draft") >= 0
                || lower.indexOf("memory_candidate") >= 0
                || lower.indexOf("type=\"memory_candidate\"") >= 0
                || lower.indexOf("candidate memory") >= 0;
    }

    private boolean isHighPrioritySource(String sourceType) {
        String type = normalizeSourceType(sourceType);
        return "goal".equals(type)
                || "current-step".equals(type)
                || "bdd-evidence".equals(type)
                || "risk".equals(type)
                || "recovery-state".equals(type);
    }

    private boolean isRequiredEvidenceSource(String sourceType) {
        String type = normalizeSourceType(sourceType);
        return "bdd-evidence".equals(type) || "required-evidence".equals(type);
    }

    private String normalizeSourceType(String sourceType) {
        return sourceType == null ? "" : sourceType.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private boolean isReadableArtifactPath(String rawPath) {
        if (rawPath == null || rawPath.length() == 0) {
            return false;
        }
        try {
            return Files.isRegularFile(java.nio.file.Paths.get(rawPath));
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean hasFailed(List<DoctorCheck> checks) {
        for (DoctorCheck check : checks) {
            if ("failed".equals(check.status)) {
                return true;
            }
        }
        return false;
    }

    private void printJson(CommandContext context, Project project, String goalKey,
                           List<DoctorCheck> checks, boolean failed) {
        List<String> raw = new ArrayList<String>();
        for (DoctorCheck check : checks) {
            raw.add(JsonOutput.object(
                    JsonOutput.stringField("key", check.key),
                    JsonOutput.stringField("status", check.status),
                    JsonOutput.stringField("message", check.message)
            ).trim());
        }
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "context doctor"),
                JsonOutput.stringField("project_key", project.projectKey()),
                JsonOutput.stringField("goal_key", goalKey),
                JsonOutput.stringField("decision", failed ? "failed" : "passed"),
                JsonOutput.rawField("checks", JsonOutput.array(raw))
        ));
    }

    private static final class DoctorCheck {
        private final String key;
        private final String status;
        private final String message;

        private DoctorCheck(String key, String status, String message) {
            this.key = key;
            this.status = status;
            this.message = message;
        }
    }
}
