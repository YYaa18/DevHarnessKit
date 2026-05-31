package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.brief.InteractionRequest;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.repository.brief.InteractionRequestRepository;
import com.devharnesskit.dhk.service.brief.PreWorkGuardService;

import java.nio.file.Path;
import java.util.List;

final class PreWorkFileWriteGoalCheckRunner extends AbstractGoalCheckRunner {
    private final GoalCheckRecorder recorder;
    private final InteractionRequestRepository interactionRepository;
    private final PreWorkGuardService preWorkGuardService;

    PreWorkFileWriteGoalCheckRunner(GoalCheckRecorder recorder,
                                    InteractionRequestRepository interactionRepository,
                                    PreWorkGuardService preWorkGuardService) {
        super("pre-work-file-write");
        this.recorder = recorder;
        this.interactionRepository = interactionRepository;
        this.preWorkGuardService = preWorkGuardService;
    }

    public GoalCheck run(GoalCheckContext context) throws Exception {
        Path log = recorder.logPath(context.projectRoot(), context.goal(), key());
        List<InteractionRequest> requests = interactionRepository.list(context.connection());
        List<PreWorkGuardService.Finding> findings = preWorkGuardService.violations(
                context.projectRoot(), requests, context.goal().goalKey());

        StringBuilder output = new StringBuilder();
        output.append("check_key: ").append(key()).append('\n');
        output.append("goal_key: ").append(context.goal().goalKey()).append('\n');
        output.append("pre_work_file_write_violation: ").append(!findings.isEmpty()).append('\n');
        output.append("violation_count: ").append(findings.size()).append('\n');
        for (PreWorkGuardService.Finding finding : findings) {
            output.append("- request_id: ").append(finding.requestId()).append('\n');
            output.append("  status: ").append(finding.status()).append('\n');
            output.append("  baseline_fingerprint: ").append(finding.baselineFingerprint()).append('\n');
            output.append("  observed_fingerprint: ").append(finding.observedFingerprint()).append('\n');
        }

        recorder.writeLog(log, output.toString());
        String status = findings.isEmpty() ? "passed" : "failed";
        String summary = findings.isEmpty()
                ? "pre-work file write guard passed"
                : "pre_work_file_write_violation: business files changed before required user confirmation";
        return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(),
                "pre-work-guard", "", status, summary, log, context.now());
    }
}
