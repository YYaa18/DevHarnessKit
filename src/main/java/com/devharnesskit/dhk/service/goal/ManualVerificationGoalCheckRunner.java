package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.repository.goal.GoalStepRepository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class ManualVerificationGoalCheckRunner extends AbstractGoalCheckRunner {
    private final GoalCheckRecorder recorder;
    private final GoalStepRepository stepRepository;
    private final String scopeKey;

    ManualVerificationGoalCheckRunner(String key, GoalCheckRecorder recorder,
                                      GoalStepRepository stepRepository, String scopeKey) {
        super(key);
        this.recorder = recorder;
        this.stepRepository = stepRepository;
        this.scopeKey = scopeKey;
    }

    public GoalCheck run(GoalCheckContext context) throws Exception {
        Path log = recorder.logPath(context.projectRoot(), context.goal(), key());
        List<GoalStep> steps = stepRepository.listByGoal(context.connection(), context.goal().goalKey());
        List<String> failures = new ArrayList<String>();
        String manualEvidenceStatus = GoalCheckSupport.latestEvidenceValue(steps, "manual_evidence_status");
        String scope = GoalCheckSupport.latestEvidenceValue(steps, scopeKey);
        String manualEvidencePath = GoalCheckSupport.latestEvidenceValue(steps, "manual_evidence_path");
        String tester = GoalCheckSupport.latestEvidenceValue(steps, "tester");
        String riskIfNotRun = GoalCheckSupport.latestEvidenceValue(steps, "risk_if_not_run");

        StringBuilder output = new StringBuilder();
        output.append("check_key: ").append(key()).append('\n');
        output.append("manual_evidence_status: ")
                .append(GoalCheckSupport.empty(manualEvidenceStatus, "none")).append('\n');
        output.append(scopeKey).append(": ").append(GoalCheckSupport.empty(scope, "none")).append('\n');
        output.append("manual_evidence_path: ")
                .append(GoalCheckSupport.empty(manualEvidencePath, "none")).append('\n');
        output.append("tester: ").append(GoalCheckSupport.empty(tester, "none")).append('\n');
        output.append("risk_if_not_run: ").append(GoalCheckSupport.empty(riskIfNotRun, "none")).append('\n');

        if (!"passed".equalsIgnoreCase(manualEvidenceStatus)) {
            failures.add("manual evidence is not passed: manual_evidence_status="
                    + GoalCheckSupport.empty(manualEvidenceStatus, "none"));
        }
        if (scope.length() == 0) {
            failures.add(scopeKey + " is required");
        }
        if (!GoalCheckSupport.artifactExists(context.projectRoot(), manualEvidencePath)) {
            failures.add("manual evidence artifact missing: manual_evidence_path="
                    + GoalCheckSupport.empty(manualEvidencePath, "none"));
        }

        recorder.writeLog(log, output.toString());
        String status = failures.isEmpty() ? "passed" : "failed";
        String summary = failures.isEmpty()
                ? key() + " evidence passed; " + scopeKey + "=" + scope
                + " manual_evidence_path=" + manualEvidencePath
                : key() + " manual evidence required: " + failures;
        return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "manual",
                "", status, summary, log, context.now());
    }
}
