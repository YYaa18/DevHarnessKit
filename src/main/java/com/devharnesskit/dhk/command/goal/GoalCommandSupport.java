package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalEvidenceContract;
import com.devharnesskit.dhk.service.goal.GoalCheckPolicyService;
import com.devharnesskit.dhk.service.goal.GoalGraphStateService;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.service.goal.GoalProfileService;
import com.devharnesskit.dhk.model.goal.GoalGraphState;
import com.devharnesskit.dhk.util.PathUtil;
import com.devharnesskit.dhk.util.JsonOutput;

import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

final class GoalCommandSupport {
    private static final GoalCheckPolicyService CHECK_POLICY_SERVICE = new GoalCheckPolicyService();
    private static final GoalProfileService PROFILE_SERVICE = new GoalProfileService();
    private static final GoalGraphStateService GRAPH_STATE_SERVICE = new GoalGraphStateService();
    private static final String GOAL_SCRIPT_DIR = ".agents/skills/devharness-goal-development/scripts/";
    private static final String GRAPH_SCRIPT_DIR = ".agents/skills/devharness-goal-development/scripts/";
    private static final String[] ALLOWED_ACTIONS = new String[]{
            "perform_current_action_only",
            "record_goal_step_after_work",
            "run_goal_next_before_continuing"
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
        GoalEvidenceContract evidenceContract = GoalEvidenceContract.from(goal, plan);
        for (String evidence : evidenceContract.requiredEvidence()) {
            context.out().println("  - " + evidence);
        }
        context.out().println("structured_evidence_fields:");
        for (String field : evidenceContract.structuredEvidenceFields()) {
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
        printScenarioImpactState(context, projectRoot, profile);
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
        context.out().println("next_command: " + nextCommand(plan, graph, profile, projectRoot));
        context.out().println("evidence_template_command: " + GOAL_SCRIPT_DIR
                + "dhk.sh goal evidence-template --goal " + goal.goalKey());
        context.out().println("context_path: " + PathUtil.goalContext(projectRoot));
    }

    static void printPlanJson(CommandContext context, Path projectRoot, GoalRun goal, GoalPlan plan,
                              String[] completionBlockers) {
        GoalProfile profile = PROFILE_SERVICE.find(projectRoot, goal.profileKey());
        String[] requiredChecks = CHECK_POLICY_SERVICE.load(projectRoot).requiredChecks(profile);
        GoalGraphState graph = GRAPH_STATE_SERVICE.inspect(projectRoot, profile, plan);
        GoalEvidenceContract evidenceContract = GoalEvidenceContract.from(goal, plan);
        String[] contextFiles = contextFiles(goal);
        String nextCommand = nextCommand(plan, graph, profile, projectRoot);
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "goal next"),
                JsonOutput.stringField("goal_key", goal.goalKey()),
                JsonOutput.stringField("status", goal.status()),
                JsonOutput.stringField("current_action", plan.currentAction()),
                JsonOutput.stringField("instruction", plan.instruction()),
                JsonOutput.rawField("allowed_actions", JsonOutput.stringArray(ALLOWED_ACTIONS)),
                JsonOutput.rawField("required_evidence", JsonOutput.stringArray(evidenceContract.requiredEvidence())),
                JsonOutput.rawField("structured_evidence_fields", JsonOutput.stringArray(evidenceContract.structuredEvidenceFields())),
                JsonOutput.rawField("forbidden_actions", JsonOutput.stringArray(plan.forbiddenActions())),
                JsonOutput.rawField("required_checks", JsonOutput.stringArray(requiredChecks)),
                JsonOutput.rawField("context_files", JsonOutput.stringArray(contextFiles)),
                JsonOutput.rawField("completion_blockers", JsonOutput.stringArray(
                        completionBlockers == null ? new String[0] : completionBlockers)),
                JsonOutput.rawField("evidence_contract", evidenceContractJson(evidenceContract)),
                JsonOutput.rawField("graph", graphJson(graph)),
                JsonOutput.rawField("scenario_impact", scenarioImpactJson(projectRoot, profile)),
                JsonOutput.stringField("next_command", nextCommand),
                JsonOutput.stringField("evidence_template_command",
                        GOAL_SCRIPT_DIR + "dhk.sh goal evidence-template --goal " + goal.goalKey()),
                JsonOutput.stringField("context_path", PathUtil.goalContext(projectRoot).toString())
        ));
    }

    private static String[] contextFiles(GoalRun goal) {
        List<String> files = new ArrayList<String>();
        files.add(".agents/memory/exports/CURRENT_CONTEXT.md");
        files.add(".agents/memory/exports/WORKFLOW_CONTEXT.md");
        if (goal.specChangeKey().length() > 0) {
            files.add(".agents/memory/exports/SPEC_CONTEXT.md");
        }
        return files.toArray(new String[files.size()]);
    }

    private static String evidenceContractJson(GoalEvidenceContract contract) {
        return JsonOutput.object(
                JsonOutput.stringField("current_action", contract.currentAction()),
                JsonOutput.rawField("required_evidence", JsonOutput.stringArray(contract.requiredEvidence())),
                JsonOutput.rawField("structured_evidence_fields", JsonOutput.stringArray(contract.structuredEvidenceFields())),
                JsonOutput.stringField("example_evidence", contract.exampleEvidence()),
                JsonOutput.stringField("example_command", contract.exampleCommand()),
                JsonOutput.stringField("rule", "include every required_evidence key in goal step evidence")
        );
    }

    private static String graphJson(GoalGraphState graph) {
        if (graph == null || !graph.enabled()) {
            return JsonOutput.object(JsonOutput.booleanField("enabled", false));
        }
        return JsonOutput.object(
                JsonOutput.booleanField("enabled", graph.enabled()),
                JsonOutput.stringField("snapshot_path", graph.snapshotPath()),
                JsonOutput.booleanField("snapshot_exists", graph.snapshotExists()),
                JsonOutput.stringField("snapshot_key", graph.snapshotKey()),
                JsonOutput.stringField("snapshot_workspace_fingerprint", graph.snapshotWorkspaceFingerprint()),
                JsonOutput.stringField("current_workspace_fingerprint", graph.currentWorkspaceFingerprint()),
                JsonOutput.booleanField("graph_stale", graph.snapshotStale()),
                JsonOutput.stringField("freshness_status", graph.freshnessStatus()),
                JsonOutput.stringField("graph_context_path", graph.graphContextPath()),
                JsonOutput.booleanField("graph_context_exists", graph.graphContextExists()),
                JsonOutput.stringField("impact_map_path", graph.impactMapPath()),
                JsonOutput.booleanField("impact_map_exists", graph.impactMapExists()),
                JsonOutput.booleanField("integrated_into_main_flow", true),
                JsonOutput.stringField("required_graph_action", graph.requiredGraphAction()),
                JsonOutput.stringField("graph_next_command", graph.graphNextCommand())
        );
    }

    private static String scenarioImpactJson(Path projectRoot, GoalProfile profile) {
        boolean required = profile != null && profile.graphRequired() && profile.bddRequired();
        if (!required) {
            return JsonOutput.object(JsonOutput.booleanField("required", false));
        }
        Path scenarioImpact = PathUtil.scenarioImpactMap(projectRoot);
        return JsonOutput.object(
                JsonOutput.booleanField("required", true),
                JsonOutput.stringField("path", scenarioImpact.toString()),
                JsonOutput.booleanField("exists", Files.isRegularFile(scenarioImpact)),
                JsonOutput.stringField("next_command", scenarioImpactCommand(projectRoot))
        );
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
        context.out().println("graph_assist:");
        context.out().println("  integrated_into_main_flow: true");
        context.out().println("  recommended_internal_action: " + graph.requiredGraphAction());
        if (graph.graphNextCommand().length() > 0) {
            context.out().println("  internal_helper: " + localCommand(graph.graphNextCommand()));
        }
    }

    private static void printScenarioImpactState(CommandContext context, Path projectRoot, GoalProfile profile) {
        if (profile == null || !profile.graphRequired() || !profile.bddRequired()) {
            return;
        }
        Path scenarioImpact = PathUtil.scenarioImpactMap(projectRoot);
        context.out().println("scenario_impact:");
        context.out().println("  required: true");
        context.out().println("  path: " + scenarioImpact);
        context.out().println("  exists: " + Files.isRegularFile(scenarioImpact));
        context.out().println("  integrated_into_main_flow: true");
        context.out().println("  internal_helper: " + scenarioImpactCommand(projectRoot));
    }

    private static String nextCommand(GoalPlan plan, GoalGraphState graph, GoalProfile profile, Path projectRoot) {
        return localCommand(plan.nextCommand());
    }

    private static String scenarioImpactCommand(Path projectRoot) {
        return GRAPH_SCRIPT_DIR + "graph-impact.sh --project-root " + projectRoot.toAbsolutePath().normalize()
                + " --scenario <scenario-key>";
    }

    private static String localCommand(String command) {
        if (command == null) {
            return "";
        }
        if (command.startsWith("dhk goal step ")) {
            return GOAL_SCRIPT_DIR + "goal-step.sh " + command.substring("dhk goal step ".length());
        }
        if (command.startsWith("dhk goal next ")) {
            return GOAL_SCRIPT_DIR + "goal-next.sh " + command.substring("dhk goal next ".length());
        }
        if (command.startsWith("dhk goal resume ")) {
            return GOAL_SCRIPT_DIR + "goal-resume.sh " + command.substring("dhk goal resume ".length());
        }
        if (command.startsWith("dhk goal verify ")) {
            return GOAL_SCRIPT_DIR + "goal-verify.sh " + command.substring("dhk goal verify ".length());
        }
        if (command.startsWith("dhk goal complete ")) {
            return GOAL_SCRIPT_DIR + "goal-complete.sh " + command.substring("dhk goal complete ".length());
        }
        if (command.startsWith("dhk goal retrospective ")) {
            return GOAL_SCRIPT_DIR + "goal-retrospective.sh "
                    + command.substring("dhk goal retrospective ".length());
        }
        if (command.startsWith("dhk goal review-summary ")) {
            return GOAL_SCRIPT_DIR + "goal-review-summary.sh "
                    + command.substring("dhk goal review-summary ".length());
        }
        if (command.startsWith("dhk goal mr-summary ")) {
            return GOAL_SCRIPT_DIR + "goal-mr-summary.sh " + command.substring("dhk goal mr-summary ".length());
        }
        if (command.startsWith("dhk brief answer ")) {
            return GOAL_SCRIPT_DIR + "dhk.sh brief answer " + command.substring("dhk brief answer ".length());
        }
        if (command.startsWith("dhk graph impact ")) {
            return GRAPH_SCRIPT_DIR + "graph-impact.sh " + command.substring("dhk graph impact ".length());
        }
        if (command.startsWith("dhk graph index ")) {
            String args = command.substring("dhk graph index ".length());
            int andIndex = args.indexOf(" && ");
            if (andIndex >= 0) {
                args = args.substring(0, andIndex);
            }
            return GRAPH_SCRIPT_DIR + "graph-index-export.sh " + args;
        }
        return command;
    }
}
