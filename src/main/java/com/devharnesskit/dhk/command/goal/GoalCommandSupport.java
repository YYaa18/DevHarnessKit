package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.service.goal.GoalCheckPolicyService;
import com.devharnesskit.dhk.service.goal.GoalGraphStateService;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.service.goal.GoalProfileService;
import com.devharnesskit.dhk.model.goal.GoalGraphState;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;

final class GoalCommandSupport {
    private static final GoalCheckPolicyService CHECK_POLICY_SERVICE = new GoalCheckPolicyService();
    private static final GoalProfileService PROFILE_SERVICE = new GoalProfileService();
    private static final GoalGraphStateService GRAPH_STATE_SERVICE = new GoalGraphStateService();
    private static final String[] ALLOWED_ACTIONS = new String[]{
            "perform_current_action_only",
            "record_goal_step_after_work",
            "run_goal_next_before_continuing"
    };
    private static final String[] STRUCTURED_EVIDENCE_FIELDS = new String[]{
            "--read-files",
            "--changed-files",
            "--tests-run",
            "--compile-result",
            "--risks",
            "--pending"
    };

    private GoalCommandSupport() {
    }

    static Path projectRoot(Args args, CommandContext context) {
        return PathUtil.resolveProjectRoot(args, context.workingDirectory());
    }

    static GoalRun goal(GoalOrchestrator orchestrator, CommandContext context,
                        Args args, Path projectRoot) throws Exception {
        String goalKey = args.option("goal").trim();
        if (goalKey.length() == 0) {
            return orchestrator.latestOpen(context, projectRoot);
        }
        return orchestrator.find(context, projectRoot, goalKey);
    }

    static void printPlan(CommandContext context, Path projectRoot, GoalRun goal, GoalPlan plan) {
        printPlan(context, projectRoot, goal, plan, new String[0]);
    }

    static void printPlan(CommandContext context, Path projectRoot, GoalRun goal, GoalPlan plan,
                          String[] completionBlockers) {
        context.out().println("goal_key: " + goal.goalKey());
        context.out().println("status: " + goal.status());
        context.out().println("current_action: " + plan.currentAction());
        context.out().println("instruction: " + plan.instruction());
        context.out().println("allowed_actions:");
        for (String action : ALLOWED_ACTIONS) {
            context.out().println("  - " + action);
        }
        context.out().println("required_evidence:");
        for (String evidence : plan.requiredEvidence()) {
            context.out().println("  - " + evidence);
        }
        context.out().println("structured_evidence_fields:");
        for (String field : STRUCTURED_EVIDENCE_FIELDS) {
            context.out().println("  - " + field);
        }
        context.out().println("forbidden_actions:");
        for (String action : plan.forbiddenActions()) {
            context.out().println("  - " + action);
        }
        context.out().println("required_checks:");
        GoalProfile profile = PROFILE_SERVICE.find(projectRoot, goal.profileKey());
        for (String check : CHECK_POLICY_SERVICE.load(projectRoot).requiredChecks(profile)) {
            context.out().println("  - " + check);
        }
        GoalGraphState graph = GRAPH_STATE_SERVICE.inspect(projectRoot, profile, plan);
        printGraphState(context, graph);
        context.out().println("context_files:");
        context.out().println("  - .agents/memory/exports/CURRENT_CONTEXT.md");
        context.out().println("  - .agents/memory/exports/WORKFLOW_CONTEXT.md");
        if (goal.specChangeKey().length() > 0) {
            context.out().println("  - .agents/memory/exports/SPEC_CONTEXT.md");
        }
        context.out().println("completion_blockers:");
        if (completionBlockers == null || completionBlockers.length == 0) {
            context.out().println("  - none");
        } else {
            for (String blocker : completionBlockers) {
                context.out().println("  - " + blocker);
            }
        }
        context.out().println("next_command: " + nextCommand(plan, graph));
        context.out().println("context_path: " + PathUtil.goalContext(projectRoot));
    }

    private static void printGraphState(CommandContext context, GoalGraphState graph) {
        if (graph == null || !graph.enabled()) {
            return;
        }
        context.out().println("graph_snapshot:");
        context.out().println("  path: " + graph.snapshotPath());
        context.out().println("  exists: " + graph.snapshotExists());
        context.out().println("  snapshot_key: " + (graph.snapshotKey().length() == 0 ? "none" : graph.snapshotKey()));
        context.out().println("  snapshot_workspace_fingerprint: "
                + (graph.snapshotWorkspaceFingerprint().length() == 0 ? "none" : graph.snapshotWorkspaceFingerprint()));
        context.out().println("  current_workspace_fingerprint: "
                + (graph.currentWorkspaceFingerprint().length() == 0 ? "none" : graph.currentWorkspaceFingerprint()));
        context.out().println("  graph_stale: " + graph.snapshotStale());
        context.out().println("  freshness_status: "
                + (graph.freshnessStatus().length() == 0 ? "unknown" : graph.freshnessStatus()));
        context.out().println("graph_context:");
        context.out().println("  path: " + graph.graphContextPath());
        context.out().println("  exists: " + graph.graphContextExists());
        context.out().println("  impact_map_path: " + graph.impactMapPath());
        context.out().println("  impact_map_exists: " + graph.impactMapExists());
        context.out().println("required_graph_action: " + graph.requiredGraphAction());
        if (graph.graphNextCommand().length() > 0) {
            context.out().println("graph_next_command: " + graph.graphNextCommand());
        }
    }

    private static String nextCommand(GoalPlan plan, GoalGraphState graph) {
        if (graph != null && graph.enabled() && graph.graphNextCommand().length() > 0) {
            return graph.graphNextCommand();
        }
        return plan.nextCommand();
    }
}
