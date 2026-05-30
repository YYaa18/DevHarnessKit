package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.bdd.BddBinding;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.repository.bdd.BddBindingRepository;
import com.devharnesskit.dhk.repository.goal.GoalStepRepository;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class ImpactGoalCheckRunner extends AbstractGoalCheckRunner {
    private final GoalCheckRecorder recorder;
    private final GoalStepRepository stepRepository;
    private final BddBindingRepository bddBindingRepository;

    ImpactGoalCheckRunner(GoalCheckRecorder recorder, GoalStepRepository stepRepository,
                          BddBindingRepository bddBindingRepository) {
        super("impact");
        this.recorder = recorder;
        this.stepRepository = stepRepository;
        this.bddBindingRepository = bddBindingRepository;
    }

    public GoalCheck run(GoalCheckContext context) throws Exception {
        Path log = recorder.logPath(context.projectRoot(), context.goal(), key());
        String command = GoalCheckSupport.impactRefreshCommand(context.projectRoot(), context.connection(),
                context.goal(), stepRepository);
        if (context.profile() == null || !context.profile().graphRequired()
                || !context.profile().graphRequireImpactMap()) {
            String summary = "impact map not required by goal profile";
            recorder.writeLog(log, summary + "\n");
            return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "impact",
                    command, "skipped", summary, log, context.now());
        }

        Path snapshot = PathUtil.graphSnapshotJson(context.projectRoot());
        Path impact = PathUtil.graphImpactMap(context.projectRoot());
        List<String> failures = new ArrayList<String>();
        List<GoalStep> steps = stepRepository.listByGoal(context.connection(), context.goal().goalKey());
        StringBuilder output = new StringBuilder();
        output.append("graph_snapshot: ").append(snapshot).append('\n');
        output.append("impact_map: ").append(impact).append('\n');
        output.append("next_command: ").append(command).append('\n');

        String snapshotText = "";
        String snapshotKey = "";
        if (!Files.isRegularFile(snapshot)) {
            failures.add("GRAPH_SNAPSHOT.json missing; next_command="
                    + GoalCheckSupport.graphRefreshCommand(context.projectRoot()));
        } else {
            snapshotText = new String(Files.readAllBytes(snapshot), "UTF-8");
            snapshotKey = GoalCheckSupport.jsonString(snapshotText, "snapshot_key");
            output.append("snapshot_key: ").append(snapshotKey).append('\n');
        }

        String impactText = "";
        if (!Files.isRegularFile(impact)) {
            failures.add("IMPACT_MAP.md missing; next_command=" + command);
        } else {
            impactText = new String(Files.readAllBytes(impact), "UTF-8");
            String impactSnapshotKey = GoalCheckSupport.lineValue(impactText, "- snapshot_key: ");
            String generatedAt = GoalCheckSupport.tagValue(impactText, "generated-at");
            output.append("impact_snapshot_key: ").append(impactSnapshotKey).append('\n');
            output.append("generated_at: ").append(generatedAt).append('\n');
            if (snapshotKey.length() > 0 && !snapshotKey.equals(impactSnapshotKey)) {
                failures.add("impact map stale: snapshot_key mismatch; next_command=" + command);
            }
            if (Files.isRegularFile(snapshot)
                    && Files.getLastModifiedTime(impact).compareTo(Files.getLastModifiedTime(snapshot)) < 0) {
                failures.add("impact map stale: generated before latest graph snapshot; next_command=" + command);
            }
            GoalCheckSupport.addAgeFailure("impact map", generatedAt,
                    context.profile().graphMaxStalenessMinutes(), context.now(), command, failures);
            List<String> uncovered = uncoveredChangedFiles(context, impactText, steps);
            if (!uncovered.isEmpty()) {
                failures.add("changed files not covered by impact map: " + uncovered
                        + "; next_command=" + GoalCheckSupport.commandForChangedFile(
                        context.projectRoot(), uncovered.get(0)));
            }
            List<String> missingTests = GoalCheckSupport.sectionValues(impactText, "missing-related-tests");
            output.append("missing_related_tests: ").append(missingTests).append('\n');
            output.append("missing_related_tests_count: ").append(missingTests.size()).append('\n');
            validateSafeRefactorReimpact(context.projectRoot(), context, steps, output, failures);
        }
        validateScenarioImpactMap(context, snapshot, snapshotKey, output, failures);

        recorder.writeLog(log, output.toString());
        String status = failures.isEmpty() ? "passed" : "failed";
        String summary = failures.isEmpty()
                ? "impact map fresh and covers changed files"
                + (impactText.length() > 0
                ? "; missing_related_tests="
                + GoalCheckSupport.sectionValues(impactText, "missing-related-tests").size()
                : "")
                : "impact freshness failed: " + failures;
        return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "impact",
                command, status, summary, log, context.now());
    }

    private List<String> uncoveredChangedFiles(GoalCheckContext context, String impactText,
                                               List<GoalStep> steps) throws Exception {
        List<String> changed = GoalCheckSupport.changedFilesForCoverage(stepRepository, context.connection(),
                context.projectRoot(), context.goal());
        List<String> uncovered = new ArrayList<String>();
        for (String file : changed) {
            if (GoalCheckSupport.requiresImpactCoverage(file) && impactText.indexOf(file) < 0
                    && !GoalCheckSupport.multiImpactEvidenceCovers(context.projectRoot(), steps, file)) {
                uncovered.add(file);
            }
        }
        return uncovered;
    }

    private void validateScenarioImpactMap(GoalCheckContext context, Path snapshot, String snapshotKey,
                                           StringBuilder output, List<String> failures) throws Exception {
        if (context.profile() == null || !context.profile().graphRequired() || !context.profile().bddRequired()) {
            return;
        }
        Path scenarioImpact = PathUtil.scenarioImpactMap(context.projectRoot());
        List<BddBinding> bindings = bddBindingRepository.listByBinding(context.connection(),
                "goal", context.goal().goalKey());
        String command = GoalCheckSupport.scenarioImpactCommand(context.projectRoot(), bindings);
        output.append("scenario_impact_required: true\n");
        output.append("scenario_impact_map: ").append(scenarioImpact).append('\n');
        output.append("scenario_impact_next_command: ").append(command).append('\n');
        output.append("scenario_bound_count: ").append(bindings.size()).append('\n');
        if (bindings.isEmpty()) {
            failures.add("scenario impact required but no BDD scenarios are bound to goal; next_command="
                    + command);
            return;
        }
        if (!Files.isRegularFile(scenarioImpact)) {
            failures.add("SCENARIO_IMPACT_MAP.md missing; next_command=" + command);
            return;
        }

        String text = new String(Files.readAllBytes(scenarioImpact), "UTF-8");
        String generatedAt = GoalCheckSupport.tagValue(text, "generated-at");
        output.append("scenario_impact_generated_at: ").append(generatedAt).append('\n');
        output.append("scenario_impact_snapshot_stale: ")
                .append(GoalCheckSupport.lineValue(text, "- snapshot_stale: ")).append('\n');
        String matchedScenario = GoalCheckSupport.matchedScenarioKey(bindings, text);
        output.append("scenario_impact_matched_scenario: ")
                .append(GoalCheckSupport.empty(matchedScenario, "none")).append('\n');
        if (matchedScenario.length() == 0) {
            failures.add("SCENARIO_IMPACT_MAP.md does not reference a goal-bound scenario; next_command="
                    + command);
        }
        if ("true".equalsIgnoreCase(GoalCheckSupport.lineValue(text, "- snapshot_stale: "))) {
            failures.add("SCENARIO_IMPACT_MAP.md is marked stale; next_command=" + command);
        }
        if (snapshot != null && Files.isRegularFile(snapshot)
                && Files.getLastModifiedTime(scenarioImpact).compareTo(Files.getLastModifiedTime(snapshot)) < 0) {
            failures.add("SCENARIO_IMPACT_MAP.md stale: generated before latest graph snapshot; next_command="
                    + command);
        }
        GoalCheckSupport.addAgeFailure("scenario impact map", generatedAt,
                context.profile().graphMaxStalenessMinutes(), context.now(), command, failures);
        if (snapshotKey != null && snapshotKey.length() > 0 && text.indexOf(snapshotKey) < 0) {
            output.append("scenario_impact_snapshot_key_warning: not_embedded\n");
        }
    }

    private void validateSafeRefactorReimpact(Path projectRoot, GoalCheckContext context, List<GoalStep> steps,
                                              StringBuilder output, List<String> failures) {
        if (context.profile() == null || !"safe-refactor-with-graph".equals(context.profile().profileKey())) {
            return;
        }
        String postChangeImpact = GoalCheckSupport.evidenceValue(steps, "post_change_impact_map");
        String impactDelta = GoalCheckSupport.evidenceValue(steps, "impact_delta");
        String changedFilesCovered = GoalCheckSupport.evidenceValue(steps, "changed_files_covered");
        String expansionRisk = GoalCheckSupport.firstNonEmpty(
                GoalCheckSupport.evidenceValue(steps, "impact_expansion_risk"),
                GoalCheckSupport.evidenceValue(steps, "risk_evidence"));
        output.append("safe_refactor_reimpact_required: true\n");
        output.append("post_change_impact_map: ").append(postChangeImpact).append('\n');
        output.append("impact_delta: ").append(impactDelta).append('\n');
        output.append("changed_files_covered: ").append(changedFilesCovered).append('\n');
        output.append("impact_expansion_risk: ").append(expansionRisk).append('\n');
        if (!GoalCheckSupport.impactArtifactExists(projectRoot, postChangeImpact)) {
            failures.add("safe-refactor reimpact missing or not exported: post_change_impact_map="
                    + GoalCheckSupport.empty(postChangeImpact, "none"));
        }
        if (!GoalCheckSupport.acceptedCoverageEvidence(changedFilesCovered)) {
            failures.add("safe-refactor changed files are not confirmed covered by reimpact: changed_files_covered="
                    + GoalCheckSupport.empty(changedFilesCovered, "none"));
        }
        if (GoalCheckSupport.impactExpanded(impactDelta) && expansionRisk.length() == 0) {
            failures.add("impact expansion risk evidence missing for safe-refactor: impact_delta="
                    + GoalCheckSupport.empty(impactDelta, "none"));
        }
    }
}
