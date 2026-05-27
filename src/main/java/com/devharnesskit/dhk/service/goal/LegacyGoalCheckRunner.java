package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.repository.goal.GoalStepRepository;
import com.devharnesskit.dhk.service.policy.DevHarnessPolicyService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class LegacyGoalCheckRunner extends AbstractGoalCheckRunner {
    private final GoalCheckRecorder recorder;
    private final GoalStepRepository stepRepository;
    private final DevHarnessPolicyService devHarnessPolicyService;

    LegacyGoalCheckRunner(GoalCheckRecorder recorder, GoalStepRepository stepRepository,
                          DevHarnessPolicyService devHarnessPolicyService) {
        super("legacy");
        this.recorder = recorder;
        this.stepRepository = stepRepository;
        this.devHarnessPolicyService = devHarnessPolicyService;
    }

    public GoalCheck run(GoalCheckContext context) throws Exception {
        Path log = recorder.logPath(context.projectRoot(), context.goal(), key());
        List<String> failures = new ArrayList<String>();
        StringBuilder output = new StringBuilder();
        if (context.profile() == null || !context.profile().legacyGraphProfile()) {
            String summary = "legacy evidence not required by goal profile";
            recorder.writeLog(log, summary + "\n");
            return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "legacy",
                    "", "skipped", summary, log, context.now());
        }

        List<GoalStep> steps = stepRepository.listByGoal(context.connection(), context.goal().goalKey());
        List<String> changed = GoalCheckSupport.changedFilesForCoverage(stepRepository, context.connection(),
                context.projectRoot(), context.goal());
        output.append("changed_files_count: ").append(changed.size()).append('\n');
        output.append("legacy_max_changed_files: ").append(context.profile().legacyMaxChangedFiles()).append('\n');
        if (changed.size() > context.profile().legacyMaxChangedFiles()) {
            failures.add("legacy changed file limit exceeded: changed_files=" + changed.size()
                    + " max=" + context.profile().legacyMaxChangedFiles());
        }
        if (GoalCheckSupport.containsEvidenceFlag(steps, "large_refactor")
                || GoalCheckSupport.containsEvidenceFlag(steps, "mass_refactor")
                || GoalCheckSupport.containsEvidenceFlag(steps, "format_only")) {
            failures.add("legacy profile forbids large refactor or format-only evidence flags");
        }

        String rollbackPlan = GoalCheckSupport.evidenceValue(steps, "rollback_plan");
        output.append("rollback_plan: ").append(rollbackPlan).append('\n');
        if (context.profile().rollbackPlanRequired()
                && !GoalCheckSupport.artifactExists(context.projectRoot(), rollbackPlan)) {
            failures.add("rollback plan artifact missing: rollback_plan="
                    + GoalCheckSupport.empty(rollbackPlan, "none"));
        }

        String manualEvidence = GoalCheckSupport.evidenceValue(steps, "manual_evidence");
        String manualEvidenceStatus = GoalCheckSupport.evidenceValue(steps, "manual_evidence_status");
        String manualEvidencePath = GoalCheckSupport.evidenceValue(steps, "manual_evidence_path");
        output.append("manual_evidence: ").append(manualEvidence).append('\n');
        output.append("manual_evidence_status: ").append(manualEvidenceStatus).append('\n');
        output.append("manual_evidence_path: ").append(manualEvidencePath).append('\n');
        if (context.profile().manualEvidenceRequired()) {
            if (!"passed".equalsIgnoreCase(manualEvidenceStatus)) {
                failures.add("manual evidence is not passed: manual_evidence_status="
                        + GoalCheckSupport.empty(manualEvidenceStatus, "none"));
            }
            if (manualEvidence.length() == 0) {
                failures.add("manual evidence summary missing: manual_evidence=none");
            }
            if (!GoalCheckSupport.artifactExists(context.projectRoot(), manualEvidencePath)) {
                failures.add("manual evidence artifact missing: manual_evidence_path="
                        + GoalCheckSupport.empty(manualEvidencePath, "none"));
            }
        }

        List<String> protectedFiles = GoalCheckSupport.protectedImpactFiles(
                context.projectRoot(), devHarnessPolicyService);
        output.append("protected_impact_files: ").append(protectedFiles).append('\n');
        if (!protectedFiles.isEmpty() && context.profile().protectedImpactRequiresManualEvidence()) {
            String confirmation = GoalCheckSupport.evidenceValue(steps, "protected_file_confirmation");
            output.append("protected_file_confirmation: ").append(confirmation).append('\n');
            if (!"approved".equalsIgnoreCase(confirmation) && !"confirmed".equalsIgnoreCase(confirmation)) {
                failures.add("protected impact files require manual confirmation: " + protectedFiles);
            }
        }

        recorder.writeLog(log, output.toString());
        String status = failures.isEmpty() ? "passed" : "failed";
        String summary = failures.isEmpty()
                ? "legacy evidence passed; rollback_plan=" + rollbackPlan
                + " manual_evidence_path=" + manualEvidencePath
                + " protected_impact_files=" + protectedFiles
                : "legacy evidence failed: " + failures;
        return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "legacy",
                "", status, summary, log, context.now());
    }
}
