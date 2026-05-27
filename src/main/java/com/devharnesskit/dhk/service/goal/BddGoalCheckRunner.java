package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.bdd.BddBinding;
import com.devharnesskit.dhk.model.bdd.BddEvidence;
import com.devharnesskit.dhk.model.bdd.BddQualityIssue;
import com.devharnesskit.dhk.model.bdd.BddScenarioView;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.repository.bdd.BddBindingRepository;
import com.devharnesskit.dhk.repository.bdd.BddQualityIssueRepository;
import com.devharnesskit.dhk.service.bdd.BddLintService;
import com.devharnesskit.dhk.service.bdd.BddService;
import com.devharnesskit.dhk.service.bdd.BddVerificationService;
import com.devharnesskit.dhk.service.bdd.BddVerificationService.BddScenarioEvidenceResult;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class BddGoalCheckRunner extends AbstractGoalCheckRunner {
    private final GoalCheckRecorder recorder;
    private final BddBindingRepository bddBindingRepository;
    private final BddService bddService;
    private final BddVerificationService bddVerificationService;

    BddGoalCheckRunner(GoalCheckRecorder recorder, BddBindingRepository bddBindingRepository,
                       BddService bddService, BddVerificationService bddVerificationService) {
        super("bdd");
        this.recorder = recorder;
        this.bddBindingRepository = bddBindingRepository;
        this.bddService = bddService;
        this.bddVerificationService = bddVerificationService;
    }

    public GoalCheck run(GoalCheckContext context) throws Exception {
        Path log = recorder.logPath(context.projectRoot(), context.goal(), key());
        if (context.profile() == null || !context.profile().bddRequired()) {
            String summary = "bdd not required by goal profile";
            recorder.writeLog(log, summary + "\n");
            return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "bdd",
                    "", "skipped", summary, log, context.now());
        }
        List<BddBinding> bindings = bddBindingRepository.listByBinding(context.connection(),
                "goal", context.goal().goalKey());
        List<String> failures = new ArrayList<String>();
        StringBuilder output = new StringBuilder();
        output.append("bdd_required: true\n");
        output.append("goal_key: ").append(context.goal().goalKey()).append('\n');
        output.append("bound_scenario_count: ").append(bindings.size()).append('\n');
        output.append("evidence_path: ").append(PathUtil.bddEvidence(context.projectRoot())).append('\n');
        output.append("coverage_path: ").append(PathUtil.bddCoverage(context.projectRoot())).append('\n');
        if (bindings.isEmpty()) {
            failures.add("bdd required but no scenarios are bound to goal; next_command=dhk bdd bind-goal --scenario <scenario-key> --goal "
                    + context.goal().goalKey());
        }
        Set<String> boundScenarioKeys = new LinkedHashSet<String>();
        int coveredCount = 0;
        Project project = new Project(context.goal().projectKey(), "", "", "", "", "", "", "", "");
        for (BddBinding binding : bindings) {
            boundScenarioKeys.add(binding.scenarioKey());
            BddScenarioView view = bddService.findScenario(context.connection(), project, binding.scenarioKey());
            if (view == null) {
                failures.add("bound BDD scenario not found: " + binding.scenarioKey());
                continue;
            }
            List<BddEvidence> evidence = bddService.listEvidence(context.connection(),
                    view.scenario().scenarioKey(), context.goal().goalKey());
            BddScenarioEvidenceResult scenario = bddVerificationService.evaluateScenario(view, evidence);
            output.append("- ").append(view.scenario().scenarioKey()).append(" [")
                    .append(scenario.status()).append("] evidence_count=")
                    .append(evidence.size()).append('\n');
            if (scenario.covered()) {
                coveredCount++;
            }
            if (!scenario.covered()) {
                failures.add("scenario " + view.scenario().scenarioKey()
                        + " evidence is " + scenario.status()
                        + "; next_command=dhk bdd evidence add --scenario "
                        + view.scenario().scenarioKey()
                        + " --goal " + context.goal().goalKey()
                        + " --status passed --summary \"<evidence>\"");
            }
        }
        int coveragePercent = bindings.isEmpty() ? 0 : coveredCount * 100 / bindings.size();
        output.append("bdd_coverage_percent: ").append(coveragePercent).append('\n');
        output.append("bdd_min_coverage_percent: ").append(context.policy().bddMinCoveragePercent()).append('\n');
        if (!bindings.isEmpty() && coveragePercent < context.policy().bddMinCoveragePercent()) {
            failures.add("bdd coverage " + coveragePercent + "% is below threshold "
                    + context.policy().bddMinCoveragePercent() + "%");
        }
        List<BddQualityIssue> qualityIssues = new BddLintService(bddService,
                new BddQualityIssueRepository()).lint(context.connection(), project, "", context.now());
        int qualityErrors = 0;
        int qualityWarnings = 0;
        for (BddQualityIssue issue : qualityIssues) {
            if (!boundScenarioKeys.contains(issue.scenarioKey())) {
                continue;
            }
            if ("error".equals(issue.severity())) {
                qualityErrors++;
            } else if ("warning".equals(issue.severity())) {
                qualityWarnings++;
            }
        }
        int qualityScore = Math.max(0, 100 - qualityErrors * 25 - qualityWarnings * 10);
        output.append("bdd_quality_score: ").append(qualityScore).append('\n');
        output.append("bdd_quality_errors: ").append(qualityErrors).append('\n');
        output.append("bdd_quality_warnings: ").append(qualityWarnings).append('\n');
        output.append("bdd_min_quality_score: ").append(context.policy().bddMinQualityScore()).append('\n');
        if (qualityScore < context.policy().bddMinQualityScore()) {
            failures.add("bdd quality score " + qualityScore + " is below threshold "
                    + context.policy().bddMinQualityScore());
        }
        if (context.policy().bddFailOnQualityErrors() && qualityErrors > 0) {
            failures.add("bdd quality errors present: " + qualityErrors);
        }
        if (context.policy().bddFailOnQualityWarnings() && qualityWarnings > 0) {
            failures.add("bdd quality warnings present: " + qualityWarnings);
        }
        recorder.writeLog(log, output.toString());
        String status = failures.isEmpty() ? "passed" : "failed";
        String summary = failures.isEmpty()
                ? "bdd scenarios covered; bound_scenarios=" + bindings.size()
                + " coverage=" + coveragePercent + "% quality_score=" + qualityScore
                : "bdd incomplete: " + failures;
        return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "bdd",
                "", status, summary, log, context.now());
    }
}
