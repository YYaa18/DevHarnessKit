package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.workflow.WorkflowGateRun;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;

final class WorkflowGoalCheckRunner extends AbstractGoalCheckRunner {
    private final GoalCheckRecorder recorder;
    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowGateRunRepository gateRunRepository;

    WorkflowGoalCheckRunner(GoalCheckRecorder recorder, WorkflowRunRepository workflowRunRepository,
                            WorkflowGateRunRepository gateRunRepository) {
        super("workflow");
        this.recorder = recorder;
        this.workflowRunRepository = workflowRunRepository;
        this.gateRunRepository = gateRunRepository;
    }

    public GoalCheck run(GoalCheckContext context) throws Exception {
        if (context.goal().workflowRunKey().length() == 0) {
            return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "workflow",
                    "", "skipped", "goal has no workflow run", null, context.now());
        }
        WorkflowRun run = workflowRunRepository.findByKey(context.connection(), context.goal().workflowRunKey());
        if (run == null) {
            return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "workflow",
                    "", "failed", "workflow run not found", null, context.now());
        }
        if ("blocked".equals(run.status()) || "failed".equals(run.status()) || "abandoned".equals(run.status())) {
            return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "workflow",
                    "", "failed", "workflow run status is " + run.status(), null, context.now());
        }
        if (isPatchProfile(context)) {
            return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "workflow",
                    "", "passed", "patch workflow is advisory; strict workflow gates are not required", null,
                    context.now());
        }
        int failedHard = 0;
        int pendingHard = 0;
        int pendingCompletionHard = 0;
        for (WorkflowGateRun gate : gateRunRepository.listByRun(context.connection(), run.runKey())) {
            if ("hard".equals(gate.severity()) && "failed".equals(gate.status())) {
                failedHard++;
            }
            if ("hard".equals(gate.severity()) && "pending".equals(gate.status())) {
                if (context.profile() != null && context.profile().completionRequireCheckpoint()
                        && "checkpoint_created".equals(gate.gateKey())) {
                    pendingCompletionHard++;
                    continue;
                }
                pendingHard++;
            }
        }
        if (failedHard > 0) {
            return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "workflow",
                    "", "failed", "workflow has failed hard gates: " + failedHard, null, context.now());
        }
        if (context.policy().failPendingHardGates(context.profile()) && pendingHard > 0) {
            return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "workflow",
                    "", "failed", "workflow has pending hard gates: " + pendingHard, null, context.now());
        }
        return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "workflow",
                "", "passed", "workflow run is active; pending_hard_gates=" + pendingHard
                        + " pending_completion_gates=" + pendingCompletionHard, null, context.now());
    }

    private boolean isPatchProfile(GoalCheckContext context) {
        if (context.profile() == null) {
            return false;
        }
        return "java-api-patch".equals(context.profile().profileKey())
                || "safe-patch".equals(context.profile().profileKey());
    }
}
