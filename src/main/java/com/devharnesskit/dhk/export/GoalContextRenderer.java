package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalRun;

public final class GoalContextRenderer {
    private static final int MAX_CHARS = 16 * 1024;
    private static final String[] STRUCTURED_EVIDENCE_FIELDS = new String[]{
            "--read-files",
            "--changed-files",
            "--tests-run",
            "--compile-result",
            "--risks",
            "--pending"
    };

    public String render(GoalRun goal, GoalPlan plan, String generatedAt) {
        return render(goal, plan, new String[0], new String[0], generatedAt);
    }

    public String render(GoalRun goal, GoalPlan plan, String[] requiredChecks,
                         String[] completionBlockers, String generatedAt) {
        return render(goal, plan, requiredChecks, completionBlockers,
                new String[0], "fresh", generatedAt);
    }

    public String render(GoalRun goal, GoalPlan plan, String[] requiredChecks,
                         String[] completionBlockers, String[] staleChecks,
                         String freshnessStatus, String generatedAt) {
        StringBuilder builder = new StringBuilder();
        builder.append("# GOAL_CONTEXT\n\n");
        builder.append("<generated-at>").append(generatedAt).append("</generated-at>\n\n");
        builder.append("<goal>\n");
        builder.append("- goal_key: ").append(goal.goalKey()).append('\n');
        builder.append("- profile: ").append(goal.profileKey()).append('\n');
        builder.append("- task: ").append(goal.taskName()).append('\n');
        builder.append("- module: ").append(goal.moduleName()).append('\n');
        builder.append("- mode: ").append(goal.mode()).append('\n');
        builder.append("- status: ").append(goal.status()).append('\n');
        builder.append("- workflow_run: ").append(goal.workflowRunKey()).append('\n');
        if (goal.specChangeKey().length() > 0) {
            builder.append("- spec_change: ").append(goal.specChangeKey()).append('\n');
        }
        if (goal.conditionText().length() > 0) {
            builder.append("- condition: ").append(goal.conditionText()).append('\n');
        }
        builder.append("</goal>\n\n");

        builder.append("<current-action>\n").append(plan.currentAction()).append("\n</current-action>\n\n");
        builder.append("<next-instruction>\n").append(plan.instruction()).append("\n</next-instruction>\n\n");

        builder.append("<allowed-actions>\n");
        builder.append("- perform_current_action_only\n");
        builder.append("- record_goal_step_after_work\n");
        builder.append("- run_goal_next_before_continuing\n");
        builder.append("</allowed-actions>\n\n");

        builder.append("<allowed-commands>\n");
        builder.append("- dhk goal next --goal ").append(goal.goalKey()).append('\n');
        builder.append("- dhk goal step --goal ").append(goal.goalKey())
                .append(" --summary \"<summary>\" --evidence \"<evidence>\"\n");
        builder.append("- dhk goal verify --goal ").append(goal.goalKey()).append('\n');
        builder.append("- dhk goal complete --goal ").append(goal.goalKey())
                .append(" only when ready_to_complete\n");
        builder.append("</allowed-commands>\n\n");

        builder.append("<forbidden-actions>\n");
        for (String action : plan.forbiddenActions()) {
            builder.append("- ").append(action).append('\n');
        }
        builder.append("</forbidden-actions>\n\n");

        builder.append("<required-evidence>\n");
        for (String evidence : plan.requiredEvidence()) {
            builder.append("- ").append(evidence).append('\n');
        }
        if (plan.requiredEvidence().length == 0) {
            builder.append("- concise_summary\n");
        }
        builder.append("</required-evidence>\n\n");

        builder.append("<evidence-contract>\n");
        builder.append("- current_action: ").append(plan.currentAction()).append('\n');
        builder.append("- include every required-evidence key in goal step evidence\n");
        builder.append("- put modified paths in --changed-files when files changed\n");
        builder.append("- use structured fields for read_files, tests_run, compile_result, risks, and pending\n");
        builder.append("</evidence-contract>\n\n");

        builder.append("<structured-evidence-fields>\n");
        appendList(builder, STRUCTURED_EVIDENCE_FIELDS, "none");
        builder.append("</structured-evidence-fields>\n\n");

        builder.append("<required-checks>\n");
        appendList(builder, requiredChecks, "none");
        builder.append("</required-checks>\n\n");

        builder.append("<context-files>\n");
        builder.append("- .agents/memory/exports/CURRENT_CONTEXT.md\n");
        builder.append("- .agents/memory/exports/WORKFLOW_CONTEXT.md\n");
        if (goal.specChangeKey().length() > 0) {
            builder.append("- .agents/memory/exports/SPEC_CONTEXT.md\n");
        }
        builder.append("</context-files>\n\n");

        builder.append("<completion-blockers>\n");
        appendList(builder, completionBlockers, "none");
        builder.append("</completion-blockers>\n\n");

        builder.append("<freshness-status>\n");
        builder.append("- status: ").append(freshnessStatus == null || freshnessStatus.length() == 0
                ? "fresh" : freshnessStatus).append('\n');
        builder.append("- stale_checks:\n");
        appendList(builder, staleChecks, "none");
        builder.append("- rule: checks become stale after later goal steps or workspace fingerprint changes\n");
        builder.append("</freshness-status>\n\n");

        builder.append("<completion-condition>\n");
        builder.append("- goal evaluate must return ready_to_complete before final completion\n");
        builder.append("- no direct memory confirm, gate waive, spec archive, or DB SQL unless explicitly allowed\n");
        builder.append("- checkpoint must be created before stable completion\n");
        builder.append("</completion-condition>\n\n");

        builder.append("<next-command>\n").append(plan.nextCommand()).append("\n</next-command>\n");
        return limit(builder.toString());
    }

    private void appendList(StringBuilder builder, String[] values, String emptyValue) {
        if (values == null || values.length == 0) {
            builder.append("- ").append(emptyValue).append('\n');
            return;
        }
        for (String value : values) {
            builder.append("- ").append(value).append('\n');
        }
    }

    private String limit(String text) {
        if (text.length() <= MAX_CHARS) {
            return text;
        }
        return text.substring(0, MAX_CHARS - 80) + "\n\n<!-- truncated: goal context exceeded budget -->\n";
    }
}
