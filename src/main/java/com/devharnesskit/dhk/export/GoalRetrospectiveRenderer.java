package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.goal.GoalArtifact;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalEvaluation;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.util.EvidenceValueParser;
import com.devharnesskit.dhk.util.JsonOutput;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class GoalRetrospectiveRenderer {
    public String renderMarkdown(GoalRun goal, List<GoalStep> steps, List<GoalCheck> checks,
                                 List<GoalArtifact> artifacts, GoalEvaluation evaluation,
                                 String generatedAt) {
        StringBuilder builder = new StringBuilder();
        builder.append("# GOAL_RETROSPECTIVE\n\n");
        builder.append("<generated-at>").append(generatedAt).append("</generated-at>\n\n");

        builder.append("<goal>\n");
        builder.append("- goal_key: ").append(goal.goalKey()).append('\n');
        builder.append("- external_ref: ").append(valueOrNone(goal.externalRef())).append('\n');
        builder.append("- task: ").append(goal.taskName()).append('\n');
        builder.append("- module: ").append(goal.moduleName()).append('\n');
        builder.append("- profile: ").append(goal.profileKey()).append('\n');
        builder.append("- mode: ").append(goal.mode()).append('\n');
        builder.append("- status: ").append(goal.status()).append('\n');
        builder.append("- created_at: ").append(valueOrNone(goal.createdAt())).append('\n');
        builder.append("- completed_at: ").append(valueOrNone(goal.completedAt())).append('\n');
        builder.append("- duration_ms: ").append(durationMs(goal)).append('\n');
        builder.append("</goal>\n\n");

        builder.append("<review-summary>\n");
        builder.append("- decision: ").append(evaluation.decision()).append('\n');
        builder.append("- ready_to_complete: ").append(evaluation.readyToComplete()).append('\n');
        builder.append("- step_count: ").append(size(steps)).append('\n');
        builder.append("- check_count: ").append(size(checks)).append('\n');
        builder.append("- artifact_count: ").append(size(artifacts)).append('\n');
        builder.append("- changed_files: ").append(join(changedFiles(steps))).append('\n');
        builder.append("</review-summary>\n\n");

        appendSteps(builder, steps);
        appendChecks(builder, checks);
        appendArtifacts(builder, artifacts);
        appendFollowUp(builder, evaluation);
        return builder.toString();
    }

    public String renderJson(GoalRun goal, List<GoalStep> steps, List<GoalCheck> checks,
                             List<GoalArtifact> artifacts, GoalEvaluation evaluation,
                             String generatedAt) {
        return JsonOutput.object(
                JsonOutput.stringField("schema_version", "goal-retrospective/v1-alpha"),
                JsonOutput.stringField("command", "goal retrospective"),
                JsonOutput.stringField("generated_at", generatedAt),
                JsonOutput.stringField("goal_key", goal.goalKey()),
                JsonOutput.stringField("external_ref", goal.externalRef()),
                JsonOutput.stringField("task", goal.taskName()),
                JsonOutput.stringField("module", goal.moduleName()),
                JsonOutput.stringField("profile", goal.profileKey()),
                JsonOutput.stringField("mode", goal.mode()),
                JsonOutput.stringField("status", goal.status()),
                JsonOutput.stringField("created_at", goal.createdAt()),
                JsonOutput.stringField("completed_at", goal.completedAt()),
                JsonOutput.numberField("duration_ms", durationMs(goal)),
                JsonOutput.numberField("step_count", size(steps)),
                JsonOutput.numberField("check_count", size(checks)),
                JsonOutput.numberField("artifact_count", size(artifacts)),
                JsonOutput.stringField("decision", evaluation.decision()),
                JsonOutput.booleanField("ready_to_complete", evaluation.readyToComplete()),
                JsonOutput.rawField("changed_files", JsonOutput.stringArray(changedFiles(steps))),
                JsonOutput.rawField("missing", JsonOutput.stringArray(evaluation.missing())),
                JsonOutput.rawField("stale_checks", JsonOutput.stringArray(evaluation.staleChecks())),
                JsonOutput.rawField("steps", stepsJson(steps)),
                JsonOutput.rawField("checks", checksJson(checks)),
                JsonOutput.rawField("artifacts", artifactsJson(artifacts)),
                JsonOutput.stringField("next_command", evaluation.nextCommand())
        );
    }

    private void appendSteps(StringBuilder builder, List<GoalStep> steps) {
        builder.append("<steps>\n");
        if (steps == null || steps.isEmpty()) {
            builder.append("- none\n");
        } else {
            for (GoalStep step : steps) {
                builder.append("- #").append(step.stepIndex()).append(' ')
                        .append(step.actionKey()).append(": ").append(step.summary()).append('\n');
                if (step.changedFiles().length() > 0) {
                    builder.append("  changed_files: ").append(normalizePathText(step.changedFiles())).append('\n');
                }
            }
        }
        builder.append("</steps>\n\n");
    }

    private void appendChecks(StringBuilder builder, List<GoalCheck> checks) {
        builder.append("<checks>\n");
        if (checks == null || checks.isEmpty()) {
            builder.append("- none\n");
        } else {
            for (GoalCheck check : checks) {
                builder.append("- [").append(check.status()).append("] ")
                        .append(check.checkKey()).append(": ").append(check.resultSummary()).append('\n');
                if (check.evidencePath().length() > 0) {
                    builder.append("  evidence_path: ").append(normalizePathText(check.evidencePath())).append('\n');
                }
            }
        }
        builder.append("</checks>\n\n");
    }

    private void appendArtifacts(StringBuilder builder, List<GoalArtifact> artifacts) {
        builder.append("<artifacts>\n");
        if (artifacts == null || artifacts.isEmpty()) {
            builder.append("- none\n");
        } else {
            for (GoalArtifact artifact : artifacts) {
                builder.append("- ").append(artifact.artifactType()).append(": ")
                        .append(artifact.title()).append('\n');
                if (artifact.filePath().length() > 0) {
                    builder.append("  path: ").append(normalizePathText(artifact.filePath())).append('\n');
                }
            }
        }
        builder.append("</artifacts>\n\n");
    }

    private void appendFollowUp(StringBuilder builder, GoalEvaluation evaluation) {
        builder.append("<follow-up>\n");
        builder.append("- missing: ").append(join(evaluation.missing())).append('\n');
        builder.append("- stale_checks: ").append(join(evaluation.staleChecks())).append('\n');
        builder.append("- next_command: ").append(valueOrNone(evaluation.nextCommand())).append('\n');
        builder.append("</follow-up>\n");
    }

    private String stepsJson(List<GoalStep> steps) {
        List<String> raw = new ArrayList<String>();
        for (GoalStep step : steps == null ? new ArrayList<GoalStep>() : steps) {
            raw.add(JsonOutput.object(
                    JsonOutput.numberField("step_index", step.stepIndex()),
                    JsonOutput.stringField("action", step.actionKey()),
                    JsonOutput.stringField("summary", step.summary()),
                    JsonOutput.stringField("changed_files", normalizePathText(step.changedFiles())),
                    JsonOutput.stringField("created_at", step.createdAt())
            ).trim());
        }
        return JsonOutput.array(raw);
    }

    private String checksJson(List<GoalCheck> checks) {
        List<String> raw = new ArrayList<String>();
        for (GoalCheck check : checks == null ? new ArrayList<GoalCheck>() : checks) {
            raw.add(JsonOutput.object(
                    JsonOutput.stringField("check_key", check.checkKey()),
                    JsonOutput.stringField("status", check.status()),
                    JsonOutput.stringField("summary", check.resultSummary()),
                    JsonOutput.stringField("evidence_path", normalizePathText(check.evidencePath()))
            ).trim());
        }
        return JsonOutput.array(raw);
    }

    private String artifactsJson(List<GoalArtifact> artifacts) {
        List<String> raw = new ArrayList<String>();
        for (GoalArtifact artifact : artifacts == null ? new ArrayList<GoalArtifact>() : artifacts) {
            raw.add(JsonOutput.object(
                    JsonOutput.stringField("type", artifact.artifactType()),
                    JsonOutput.stringField("title", artifact.title()),
                    JsonOutput.stringField("path", normalizePathText(artifact.filePath()))
            ).trim());
        }
        return JsonOutput.array(raw);
    }

    private String[] changedFiles(List<GoalStep> steps) {
        Set<String> files = new LinkedHashSet<String>();
        for (GoalStep step : steps == null ? new ArrayList<GoalStep>() : steps) {
            addFiles(files, step.changedFiles());
            addFiles(files, evidenceValue(step.evidence(), "changed_files"));
        }
        return files.toArray(new String[files.size()]);
    }

    private void addFiles(Set<String> files, String raw) {
        if (raw == null || raw.trim().length() == 0 || "none".equalsIgnoreCase(raw.trim())) {
            return;
        }
        String[] parts = raw.replace('\n', ',').replace(';', ',').split(",");
        for (String part : parts) {
            String file = normalizePathText(part.trim());
            if (file.length() > 0) {
                files.add(file);
            }
        }
    }

    private String evidenceValue(String evidence, String key) {
        return EvidenceValueParser.value(evidence, key);
    }

    private long durationMs(GoalRun goal) {
        String end = goal.completedAt().length() > 0 ? goal.completedAt() : goal.updatedAt();
        if (goal.createdAt().length() == 0 || end.length() == 0) {
            return 0L;
        }
        try {
            return Duration.between(Instant.parse(goal.createdAt()), Instant.parse(end)).toMillis();
        } catch (Exception ex) {
            return 0L;
        }
    }

    private int size(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private String join(String[] values) {
        if (values == null || values.length == 0) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private String valueOrNone(String value) {
        return value == null || value.length() == 0 ? "none" : value;
    }

    private String normalizePathText(String text) {
        return text == null ? "" : text.replace('\\', '/');
    }
}
