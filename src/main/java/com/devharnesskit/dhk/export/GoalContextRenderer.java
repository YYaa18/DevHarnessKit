package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalBddState;
import com.devharnesskit.dhk.model.goal.GoalEvidenceContract;
import com.devharnesskit.dhk.model.goal.GoalGraphState;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.config.DevHarnessConfig;
import com.devharnesskit.dhk.model.knowledge.KnowledgeSnippet;
import com.devharnesskit.dhk.model.knowledge.ProfessionalKnowledgeContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class GoalContextRenderer {
    private static final int MAX_CHARS = 16 * 1024;
    private static final String GOAL_SCRIPT_DIR = ".agents/skills/devharness-goal-development/scripts/";
    private static final String GRAPH_SCRIPT_DIR = ".agents/skills/devharness-goal-development/scripts/";

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
        return render(goal, plan, requiredChecks, completionBlockers, staleChecks, freshnessStatus,
                generatedAt, graphState, GoalBddState.disabled());
    }

    public String render(GoalRun goal, GoalPlan plan, String[] requiredChecks,
                         String[] completionBlockers, String[] staleChecks,
                         String freshnessStatus, String generatedAt, GoalGraphState graphState,
                         GoalBddState bddState) {
        return render(goal, plan, requiredChecks, completionBlockers, staleChecks, freshnessStatus,
                generatedAt, graphState, bddState, new String[0]);
    }

    public String render(GoalRun goal, GoalPlan plan, String[] requiredChecks,
                         String[] completionBlockers, String[] staleChecks,
                         String freshnessStatus, String generatedAt, GoalGraphState graphState,
                         GoalBddState bddState, String[] disciplineGateStatus) {
        return render(goal, plan, requiredChecks, completionBlockers, staleChecks, freshnessStatus,
                generatedAt, graphState, bddState, disciplineGateStatus, new String[0]);
    }

    public String render(GoalRun goal, GoalPlan plan, String[] requiredChecks,
                         String[] completionBlockers, String[] staleChecks,
                         String freshnessStatus, String generatedAt, GoalGraphState graphState,
                         GoalBddState bddState, String[] disciplineGateStatus,
                         String[] requiredCheckpointStatus) {
        return render(goal, plan, requiredChecks, completionBlockers, staleChecks, freshnessStatus,
                generatedAt, graphState, bddState, disciplineGateStatus, requiredCheckpointStatus, null);
    }

    public String render(GoalRun goal, GoalPlan plan, String[] requiredChecks,
                         String[] completionBlockers, String[] staleChecks,
                         String freshnessStatus, String generatedAt, GoalGraphState graphState,
                         GoalBddState bddState, String[] disciplineGateStatus,
                         String[] requiredCheckpointStatus, DevHarnessConfig verificationConfig) {
        return render(goal, plan, requiredChecks, completionBlockers, staleChecks, freshnessStatus,
                generatedAt, graphState, bddState, disciplineGateStatus, requiredCheckpointStatus,
                verificationConfig, ProfessionalKnowledgeContext.disabled());
    }

    public String render(GoalRun goal, GoalPlan plan, String[] requiredChecks,
                         String[] completionBlockers, String[] staleChecks,
                         String freshnessStatus, String generatedAt, GoalGraphState graphState,
                         GoalBddState bddState, String[] disciplineGateStatus,
                         String[] requiredCheckpointStatus, DevHarnessConfig verificationConfig,
                         ProfessionalKnowledgeContext knowledgeContext) {
        GoalGraphState graph = graphState == null ? GoalGraphState.disabled() : graphState;
        GoalBddState bdd = bddState == null ? GoalBddState.disabled() : bddState;
        DevHarnessConfig config = verificationConfig == null ? new DevHarnessConfig(null) : verificationConfig;
        ProfessionalKnowledgeContext knowledge = knowledgeContext == null
                ? ProfessionalKnowledgeContext.disabled() : knowledgeContext;
        GoalEvidenceContract evidenceContract = GoalEvidenceContract.from(goal, plan);
        StringBuilder builder = new StringBuilder();
        builder.append("# GOAL_CONTEXT\n\n");
        builder.append("<generated-at>").append(generatedAt).append("</generated-at>\n\n");
        builder.append("<goal>\n");
        builder.append("- goal_key: ").append(goal.goalKey()).append('\n');
        if (goal.externalRef().length() > 0) {
            builder.append("- external_ref: ").append(goal.externalRef()).append('\n');
        }
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
        builder.append("- ").append(GOAL_SCRIPT_DIR).append("goal-next.sh --goal ")
                .append(goal.goalKey()).append('\n');
        builder.append("- ").append(goalStepCommand(goal, evidenceContract)).append('\n');
        if (graph.enabled()) {
            builder.append("- internal graph helper: ").append(GRAPH_SCRIPT_DIR).append("graph-index-export.sh\n");
            builder.append("- ").append(GRAPH_SCRIPT_DIR)
                    .append("graph-impact.sh --file|--symbol|--sql-table <query> (internal helper)\n");
        }
        if (bdd.enabled()) {
            builder.append("- ").append(GOAL_SCRIPT_DIR).append("dhk.sh bdd bind-goal --scenario <scenario-key> --goal ")
                    .append(goal.goalKey()).append('\n');
            builder.append("- ").append(GOAL_SCRIPT_DIR).append("dhk.sh bdd evidence add --scenario <scenario-key> --goal ")
                    .append(goal.goalKey()).append(" --status passed --summary \"<evidence>\"\n");
            builder.append("- ").append(GOAL_SCRIPT_DIR).append("dhk.sh bdd verify --goal ")
                    .append(goal.goalKey()).append('\n');
            builder.append("- internal graph helper: ").append(GRAPH_SCRIPT_DIR)
                    .append("graph-impact.sh --scenario <scenario-key>\n");
        }
        builder.append("- ").append(GOAL_SCRIPT_DIR).append("goal-verify.sh --goal ")
                .append(goal.goalKey()).append('\n');
        builder.append("- ").append(GOAL_SCRIPT_DIR).append("goal-complete.sh --goal ").append(goal.goalKey())
                .append(" only when ready_to_complete\n");
        builder.append("</allowed-commands>\n\n");

        builder.append("<forbidden-actions>\n");
        for (String action : plan.forbiddenActions()) {
            builder.append("- ").append(action).append('\n');
        }
        builder.append("</forbidden-actions>\n\n");

        builder.append("<required-evidence>\n");
        for (String evidence : evidenceContract.requiredEvidence()) {
            builder.append("- ").append(evidence).append('\n');
        }
        if (evidenceContract.requiredEvidence().length == 0) {
            builder.append("- concise_summary\n");
        }
        builder.append("</required-evidence>\n\n");

        builder.append("<evidence-contract>\n");
        builder.append("- current_action: ").append(plan.currentAction()).append('\n');
        builder.append("- include every required-evidence key in goal step evidence\n");
        builder.append("- put modified paths in --changed-files when files changed\n");
        builder.append("- use --field key=value for required evidence that has no dedicated option\n");
        builder.append("- evidence_template_command: ").append(GOAL_SCRIPT_DIR)
                .append("dhk.sh goal evidence-template --goal ")
                .append(goal.goalKey()).append('\n');
        builder.append("- example_evidence: ").append(valueOrNone(evidenceContract.exampleEvidence())).append('\n');
        builder.append("</evidence-contract>\n\n");

        builder.append("<structured-evidence-fields>\n");
        appendList(builder, evidenceContract.structuredEvidenceFields(), "none");
        builder.append("</structured-evidence-fields>\n\n");

        builder.append("<required-checks>\n");
        appendList(builder, requiredChecks, "none");
        builder.append("</required-checks>\n\n");

        appendInitialProjectWarning(builder, config);
        appendVerificationPolicySection(builder, config);
        appendDisciplineGateSection(builder, disciplineGateStatus);
        appendRequiredCheckpointSection(builder, requiredCheckpointStatus);
        appendGraphSections(builder, graph);
        appendBddSections(builder, bdd);
        appendProfessionalKnowledgeSection(builder, knowledge);

        builder.append("<context-files>\n");
        builder.append("- .agents/memory/exports/CURRENT_CONTEXT.md\n");
        builder.append("- .agents/memory/exports/WORKFLOW_CONTEXT.md\n");
        if (goal.specChangeKey().length() > 0) {
            builder.append("- .agents/memory/exports/SPEC_CONTEXT.md\n");
        }
        if (bdd.enabled()) {
            builder.append("- .agents/bdd/exports/BDD_EVIDENCE.md\n");
            builder.append("- .agents/bdd/exports/BDD_COVERAGE.md\n");
            builder.append("- .agents/bdd/exports/SCENARIO_IMPACT_MAP.md\n");
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

        builder.append("<next-command>\n").append(nextCommand(plan, graph, bdd)).append("\n</next-command>\n");
        return limit(builder.toString());
    }

    private void appendDisciplineGateSection(StringBuilder builder, String[] disciplineGateStatus) {
        if (disciplineGateStatus == null || disciplineGateStatus.length == 0) {
            return;
        }
        builder.append("<discipline-gates>\n");
        appendList(builder, disciplineGateStatus, "none");
        builder.append("- rule: strict skills must not bypass discipline gates before goal complete\n");
        builder.append("</discipline-gates>\n\n");
    }

    private void appendRequiredCheckpointSection(StringBuilder builder, String[] checkpointStatus) {
        if (checkpointStatus == null || checkpointStatus.length == 0) {
            return;
        }
        builder.append("<required-checkpoints>\n");
        appendList(builder, checkpointStatus, "none");
        builder.append("- rule: strict skills must not complete high-risk goals without approved human checkpoints\n");
        builder.append("</required-checkpoints>\n\n");
    }

    private void appendVerificationPolicySection(StringBuilder builder, DevHarnessConfig config) {
        builder.append("<verification-policy>\n");
        builder.append("- config_schema: ")
                .append(config.schemaVersion().length() == 0 ? "default" : config.schemaVersion()).append('\n');
        builder.append("- preset: ").append(config.preset()).append('\n');
        builder.append("- compile_mode: ").append(config.compileMode()).append('\n');
        builder.append("- compile_command: ").append(valueOrNone(config.compileCommand())).append('\n');
        builder.append("- compile_trigger: ").append(valueOrNone(config.compileTrigger())).append('\n');
        builder.append("- test_mode: ").append(config.testMode()).append('\n');
        builder.append("- test_command: ").append(valueOrNone(config.testCommand())).append('\n');
        builder.append("- test_trigger: ").append(valueOrNone(config.testTrigger())).append('\n');
        builder.append("- test_cost: ").append(valueOrNone(config.testCost())).append('\n');
        boolean manual = "manual".equals(config.compileMode()) || "manual".equals(config.testMode());
        boolean autoMavenTest = "auto".equals(config.testMode());
        builder.append("- auto_maven_test: ").append(autoMavenTest ? "enabled" : "disabled").append('\n');
        builder.append("- manual_evidence_required: ").append(manual).append('\n');
        builder.append("- rollback_required_if_test_not_run: ")
                .append(config.value("verification.rollback.required_when_auto_tests_unavailable", "true"))
                .append('\n');
        if (!autoMavenTest) {
            builder.append("- instruction: do not run mvn test automatically; record manual or risk evidence\n");
        }
        builder.append("</verification-policy>\n\n");

        if (!manual) {
            return;
        }
        builder.append("<manual-verification-contract>\n");
        builder.append("- manual_evidence_status=passed\n");
        if ("manual".equals(config.compileMode())) {
            builder.append("- compile_scope=<module or changed classes>\n");
        }
        if ("manual".equals(config.testMode())) {
            builder.append("- test_scope=<class or method>\n");
        }
        builder.append("- manual_evidence_path=<path>\n");
        builder.append("- tester=<human or role>\n");
        builder.append("- risk_if_not_run=<risk summary>\n");
        builder.append("</manual-verification-contract>\n\n");
    }

    private void appendBddSections(StringBuilder builder, GoalBddState bdd) {
        if (!bdd.enabled()) {
            return;
        }
        builder.append("<bdd-status>\n");
        builder.append("- bdd_required: ").append(bdd.required()).append('\n');
        builder.append("- bound_scenario_count: ").append(bdd.scenarioCount()).append('\n');
        builder.append("- covered_count: ").append(bdd.coveredCount()).append('\n');
        builder.append("- missing_count: ").append(bdd.missingCount()).append('\n');
        builder.append("- pending_count: ").append(bdd.pendingCount()).append('\n');
        builder.append("- failed_count: ").append(bdd.failedCount()).append('\n');
        builder.append("- evidence_path: ").append(valueOrNone(bdd.evidencePath())).append('\n');
        builder.append("- coverage_path: ").append(valueOrNone(bdd.coveragePath())).append('\n');
        builder.append("- scenario_impact_map_path: ").append(valueOrNone(bdd.scenarioImpactMapPath())).append('\n');
        builder.append("- scenario_impact_map_exists: ").append(bdd.scenarioImpactMapExists()).append('\n');
        if (bdd.nextCommand().length() > 0) {
            builder.append("- next_command: ").append(localCommand(bdd.nextCommand())).append('\n');
        }
        if (!bdd.scenarioImpactMapExists()) {
            builder.append("- scenario_impact_next_command: ").append(GRAPH_SCRIPT_DIR)
                    .append("graph-impact.sh --scenario <scenario-key>\n");
        }
        builder.append("- rule: bdd_required profiles need goal-bound scenarios with latest passed evidence\n");
        builder.append("</bdd-status>\n\n");
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
        builder.append("- graph_precision: heuristic\n");
        builder.append("- graph_usage: advisory_preflight_not_completion_proof\n");
        builder.append("</graph-profile>\n\n");

        builder.append("<graph-snapshot>\n");
        builder.append("- path: ").append(graph.snapshotPath()).append('\n');
        builder.append("- exists: ").append(graph.snapshotExists()).append('\n');
        builder.append("- snapshot_key: ").append(graph.snapshotKey().length() == 0 ? "none" : graph.snapshotKey()).append('\n');
        builder.append("- snapshot_workspace_fingerprint: ")
                .append(valueOrNone(graph.snapshotWorkspaceFingerprint())).append('\n');
        builder.append("- current_workspace_fingerprint: ")
                .append(valueOrNone(graph.currentWorkspaceFingerprint())).append('\n');
        builder.append("- graph_stale: ").append(graph.snapshotStale()).append('\n');
        builder.append("- freshness_status: ")
                .append(graph.freshnessStatus().length() == 0 ? "unknown" : graph.freshnessStatus()).append('\n');
        builder.append("</graph-snapshot>\n\n");

        builder.append("<graph-confidence>\n");
        builder.append("- provider: ").append(graph.provider()).append('\n');
        builder.append("- precision: heuristic\n");
        builder.append("- confidence: advisory\n");
        builder.append("- must_verify_with_tests: true\n");
        builder.append("- do_not_treat_as_correctness_proof: true\n");
        if (graph.snapshotStale()) {
            builder.append("- warning: STALE_GRAPH_SNAPSHOT\n");
        }
        builder.append("</graph-confidence>\n\n");

        builder.append("<graph-context>\n");
        builder.append("- path: ").append(graph.graphContextPath()).append('\n');
        builder.append("- exists: ").append(graph.graphContextExists()).append('\n');
        builder.append("- impact_map_path: ").append(graph.impactMapPath()).append('\n');
        builder.append("- impact_map_exists: ").append(graph.impactMapExists()).append('\n');
        builder.append("</graph-context>\n\n");

        appendGraphContextDigest(builder, graph);

        builder.append("<graph-assist>\n");
        builder.append("- integrated_into_main_flow: true\n");
        builder.append("- recommended_internal_action: ").append(graph.requiredGraphAction()).append('\n');
        if (graph.graphNextCommand().length() > 0) {
            builder.append("- internal_helper: ").append(localCommand(graph.graphNextCommand())).append('\n');
        }
        builder.append("- rule: graph evidence is recorded inside the current goal action; do not create a separate graph step\n");
        if (graph.snapshotStale()) {
            builder.append("- stale_rule: fresh graph snapshot required before graph impact or completion\n");
        }
        builder.append("</graph-assist>\n\n");

        if (graph.protectedImpactFiles().length > 0) {
            builder.append("<protected-impact-risk>\n");
            for (String file : graph.protectedImpactFiles()) {
                builder.append("- ").append(file).append('\n');
            }
            builder.append("- rule: legacy graph profiles require manual confirmation before completion\n");
            builder.append("</protected-impact-risk>\n\n");
        }
    }

    private void appendGraphContextDigest(StringBuilder builder, GoalGraphState graph) {
        List<String> requiredRead = impactSection(graph.impactMapPath(), "recommended-read-files");
        List<String> optionalRead = impactSection(graph.impactMapPath(), "related-files");
        builder.append("<graph-context-digest>\n");
        builder.append("## Graph Impact Digest\n");
        builder.append("goal: ").append(valueOrNone(graph.impactMapPath())).append('\n');
        builder.append("required_read:\n");
        appendDigestList(builder, requiredRead);
        builder.append("optional_read:\n");
        appendDigestList(builder, optionalRead);
        builder.append("graph_snapshot: status=")
                .append(graph.freshnessStatus().length() == 0 ? "unknown" : graph.freshnessStatus())
                .append("; indexed_at=unknown; confidence=advisory_only\n");
        if (graph.snapshotStale()) {
            builder.append("warning: STALE_GRAPH_SNAPSHOT; regenerate with dhk graph index\n");
        }
        builder.append("</graph-context-digest>\n\n");
    }

    private List<String> impactSection(String rawPath, String section) {
        List<String> result = new ArrayList<String>();
        if (rawPath == null || rawPath.length() == 0) {
            return result;
        }
        try {
            Path path = Paths.get(rawPath);
            if (!Files.isRegularFile(path)) {
                return result;
            }
            String[] lines = new String(Files.readAllBytes(path), "UTF-8").split("\\r?\\n");
            boolean inside = false;
            String start = "<" + section + ">";
            String end = "</" + section + ">";
            for (String line : lines) {
                String trimmed = line.trim();
                if (start.equals(trimmed)) {
                    inside = true;
                    continue;
                }
                if (end.equals(trimmed)) {
                    break;
                }
                if (inside && trimmed.startsWith("- ")) {
                    result.add(trimmed.substring(2).trim());
                }
            }
        } catch (Exception ignored) {
            return result;
        }
        return result;
    }

    private void appendDigestList(StringBuilder builder, List<String> values) {
        if (values == null || values.isEmpty()) {
            builder.append("  - none\n");
            return;
        }
        int index = 1;
        for (String value : values) {
            builder.append("  ").append(index++).append(". ").append(value)
                    .append(" (reason: graph impact relation)\n");
        }
    }

    private void appendProfessionalKnowledgeSection(StringBuilder builder, ProfessionalKnowledgeContext knowledge) {
        if (!knowledge.enabled()) {
            return;
        }
        builder.append("<professional-knowledge>\n");
        builder.append("- status: enabled\n");
        builder.append("- advisory: true\n");
        builder.append("- rule: ").append(knowledge.advisory()).append('\n');
        builder.append("- pack_refs:\n");
        appendList(builder, knowledge.packRefs(), "none");
        for (KnowledgeSnippet snippet : knowledge.snippets()) {
            builder.append("\n<knowledge-snippet>\n");
            builder.append("- rule_id: ").append(snippet.ruleId()).append('\n');
            builder.append("- domain: ").append(snippet.domain()).append('\n');
            builder.append("- severity: ").append(snippet.severity()).append('\n');
            builder.append("- full_ref: ").append(snippet.fullRef()).append('\n');
            builder.append("- summary:\n");
            appendList(builder, snippet.summaryLines(), "none");
            builder.append("- evidence_checklist:\n");
            appendList(builder, snippet.evidenceChecklist(), "none");
            builder.append("</knowledge-snippet>\n");
        }
        builder.append("</professional-knowledge>\n\n");
    }

    private String valueOrNone(String value) {
        return value == null || value.length() == 0 ? "none" : value;
    }

    private String goalStepCommand(GoalRun goal, GoalEvidenceContract evidenceContract) {
        StringBuilder command = new StringBuilder();
        command.append(GOAL_SCRIPT_DIR).append("goal-step.sh --goal ")
                .append(goal.goalKey()).append(" --summary \"<summary>\"");
        boolean hasEvidence = false;
        for (String evidence : evidenceContract.requiredEvidence()) {
            String key = evidence == null ? "" : evidence.trim();
            if (key.length() == 0) {
                continue;
            }
            hasEvidence = true;
            command.append(" --field \"").append(key).append("=<value>\"");
        }
        if (!hasEvidence) {
            command.append(" --evidence \"<evidence>\"");
        }
        return command.toString();
    }

    private String nextCommand(GoalPlan plan, GoalGraphState graph, GoalBddState bdd) {
        return localCommand(plan.nextCommand());
    }

    private String localCommand(String command) {
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

    private void appendList(StringBuilder builder, String[] values, String emptyValue) {
        if (values == null || values.length == 0) {
            builder.append("- ").append(emptyValue).append('\n');
            return;
        }
        for (String value : values) {
            builder.append("- ").append(value).append('\n');
        }
    }

    private void appendInitialProjectWarning(StringBuilder builder, DevHarnessConfig config) {
        if (!config.initialProjectMode()) {
            return;
        }
        builder.append("<initial-project-warning>\n");
        builder.append("- verification_mode: initial-new-project\n");
        builder.append("- warning: ").append(config.initialProjectWarning()).append('\n');
        builder.append("- allowed_for: new empty projects, scaffolding, first setup\n");
        builder.append("- not_allowed_for: production correctness claims without real compile/test evidence\n");
        builder.append("</initial-project-warning>\n\n");
    }

    private String limit(String text) {
        if (text.length() <= MAX_CHARS) {
            return text;
        }
        return text.substring(0, MAX_CHARS - 80) + "\n\n<!-- truncated: goal context exceeded budget -->\n";
    }
}
