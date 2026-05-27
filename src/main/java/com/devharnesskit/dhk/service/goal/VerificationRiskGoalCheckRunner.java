package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.repository.goal.GoalStepRepository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class VerificationRiskGoalCheckRunner extends AbstractGoalCheckRunner {
    private final GoalCheckRecorder recorder;
    private final GoalStepRepository stepRepository;

    VerificationRiskGoalCheckRunner(GoalCheckRecorder recorder, GoalStepRepository stepRepository) {
        super("verification-risk");
        this.recorder = recorder;
        this.stepRepository = stepRepository;
    }

    public GoalCheck run(GoalCheckContext context) throws Exception {
        Path log = recorder.logPath(context.projectRoot(), context.goal(), key());
        List<GoalStep> steps = stepRepository.listByGoal(context.connection(), context.goal().goalKey());
        List<String> failures = new ArrayList<String>();
        String waiveReason = GoalCheckSupport.latestEvidenceValue(steps, "waive_reason");
        String approver = GoalCheckSupport.latestEvidenceValue(steps, "approver");
        String rollbackPlan = GoalCheckSupport.latestEvidenceValue(steps, "rollback_plan");
        String riskScope = GoalCheckSupport.firstNonEmpty(
                GoalCheckSupport.latestEvidenceValue(steps, "risk_scope"),
                GoalCheckSupport.latestEvidenceValue(steps, "risk_if_not_run"));

        StringBuilder output = new StringBuilder();
        output.append("check_key: verification-risk\n");
        output.append("waive_reason: ").append(GoalCheckSupport.empty(waiveReason, "none")).append('\n');
        output.append("approver: ").append(GoalCheckSupport.empty(approver, "none")).append('\n');
        output.append("rollback_plan: ").append(GoalCheckSupport.empty(rollbackPlan, "none")).append('\n');
        output.append("risk_scope: ").append(GoalCheckSupport.empty(riskScope, "none")).append('\n');

        if (waiveReason.length() == 0) {
            failures.add("waive_reason is required when verification is disabled");
        }
        if (approver.length() == 0) {
            failures.add("approver is required when verification is disabled");
        }
        if (riskScope.length() == 0) {
            failures.add("risk_scope or risk_if_not_run is required when verification is disabled");
        }
        if (!GoalCheckSupport.artifactExists(context.projectRoot(), rollbackPlan)) {
            failures.add("rollback plan artifact missing: rollback_plan="
                    + GoalCheckSupport.empty(rollbackPlan, "none"));
        }

        recorder.writeLog(log, output.toString());
        String status = failures.isEmpty() ? "passed" : "failed";
        String summary = failures.isEmpty()
                ? "verification risk accepted; approver=" + approver + " rollback_plan=" + rollbackPlan
                : "verification risk evidence required: " + failures;
        return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "risk",
                "", status, summary, log, context.now());
    }
}
