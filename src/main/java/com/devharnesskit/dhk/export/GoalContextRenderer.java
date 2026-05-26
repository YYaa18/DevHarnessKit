package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalGraphState;
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
        return render(goal, plan, requiredChecks, completionBlockers, staleChecks, freshnessStatus,
                generatedAt, GoalGraphState.disabled());
    }

    public String render(GoalRun goal, GoalPlan plan, String[] requiredChecks,
                         String[] completionBlockers, String[] staleChecks,
                         String freshnessStatus, String generatedAt, GoalGraphState graphState) {
        GoalGraphState graph = graphState == null ? GoalGraphState.disabled() : graphState;
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
        if (graph.enabled()) {
            builder.append("- dhk graph index --project-root <project-root>\n");
            builder.append("- dhk graph export --project-root <project-root>\n");
            builder.append("- dhk graph impact --project-root <project-root> --file|--symbol|--sql-table <query>\n");
        }
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

        appendGraphSections(builder, graph);

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

        builder.append("<next-command>\n").append(nextCommand(plan, graph)).append("\n</next-command>\n");
        return limit(builder.toString());
    }

    private void appendGraphSections(StringBuilder builder, GoalGraphState graph) {
        if (!graph.enabled()) {
            return;
        }
        builder.append("<graph-profile>\n");
        builder.append("- graph_required: true\n");
        builder.append("- graph_provider: ").append(graph.provider()).append('\n');
        builder.append("- require_fresh_snapshot: ").append(graph.requireFreshSnapshot()).append('\n');
        builder.append("- require_impact_map: ").append(graph.requireImpactMap()).append('\n');
        builder.append("- max_staleness_minutes: ").append(graph.maxStalenessMinutes()).append('\n');
        builder.append("</graph-profile>\n\n");

        builder.append("<graph-snapshot>\n");
        builder.append("- path: ").append(graph.snapshotPath()).append('\n');
        builder.append("- exists: ").append(graph.snapshotExists()).append('\n');
        builder.append("- snapshot_key: ").append(graph.snapshotKey().length() == 0 ? "none" : graph.snapshotKey()).append('\n');
        builder.append("</graph-snapshot>\n\n");

        builder.append("<graph-context>\n");
        builder.append("- path: ").append(graph.graphContextPath()).append('\n');
        builder.append("- exists: ").append(graph.graphContextExists()).append('\n');
        builder.append("- impact_map_path: ").append(graph.impactMapPath()).append('\n');
        builder.append("- impact_map_exists: ").append(graph.impactMapExists()).append('\n');
        builder.append("</graph-context>\n\n");

        builder.append("<required-graph-action>\n");
        builder.append("- action: ").append(graph.requiredGraphAction()).append('\n');
        if (graph.graphNextCommand().length() > 0) {
            builder.append("- next_command: ").append(graph.graphNextCommand()).append('\n');
        }
        builder.append("- rule: graph_required profiles must not skip required graph actions\n");
        builder.append("</required-graph-action>\n\n");
    }

    private String nextCommand(GoalPlan plan, GoalGraphState graph) {
        if (graph.enabled() && graph.graphNextCommand().length() > 0) {
            return graph.graphNextCommand();
        }
        return plan.nextCommand();
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
