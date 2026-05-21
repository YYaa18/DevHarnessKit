package com.devharnesskit.dhk.service.workflow;

import com.devharnesskit.dhk.model.workflow.WorkflowEvent;
import com.devharnesskit.dhk.model.workflow.WorkflowGateRun;
import com.devharnesskit.dhk.model.workflow.WorkflowPhaseRun;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.workflow.WorkflowEventRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class WorkflowPhaseService {
    private final WorkflowRunRepository runRepository;
    private final WorkflowPhaseRunRepository phaseRunRepository;
    private final WorkflowGateRunRepository gateRunRepository;
    private final WorkflowEventRepository eventRepository;

    public WorkflowPhaseService(WorkflowRunRepository runRepository,
                                WorkflowPhaseRunRepository phaseRunRepository,
                                WorkflowGateRunRepository gateRunRepository,
                                WorkflowEventRepository eventRepository) {
        this.runRepository = runRepository;
        this.phaseRunRepository = phaseRunRepository;
        this.gateRunRepository = gateRunRepository;
        this.eventRepository = eventRepository;
    }

    public PhaseUpdateResult pass(Connection connection, WorkflowRun run, String phaseKey,
                                  String summary, String evidence, String now) throws SQLException {
        if ("blocked".equals(run.status()) || "failed".equals(run.status())
                || "abandoned".equals(run.status()) || "completed".equals(run.status())) {
            return PhaseUpdateResult.rejected("Workflow run is not passable while status is: " + run.status());
        }
        WorkflowPhaseRun phase = phaseRunRepository.find(connection, run.runKey(), phaseKey);
        if (phase == null) {
            return PhaseUpdateResult.notFound("Phase not found: " + phaseKey);
        }
        if (!phaseKey.equals(run.currentPhaseKey())) {
            return PhaseUpdateResult.rejected("Phase is not current. current_phase=" + run.currentPhaseKey());
        }
        List<WorkflowGateRun> blockingHard = gateRunRepository.blockingHardForPhase(connection,
                run.runKey(), phaseKey);
        if (!blockingHard.isEmpty()) {
            return PhaseUpdateResult.rejected("Blocking hard gates exist for phase " + phaseKey
                    + ": " + gateKeys(blockingHard));
        }
        phaseRunRepository.updateStatus(connection, run.runKey(), phaseKey, "passed",
                summary, evidence, now, now);
        WorkflowPhaseRun next = phaseRunRepository.nextPendingAfter(connection, run.runKey(), phase.phaseOrder());
        if (next == null) {
            runRepository.updateStatus(connection, run.runKey(), "completed", now, now);
        } else {
            runRepository.updateCurrentPhase(connection, run.runKey(), "running", next.phaseKey(), now);
        }
        eventRepository.insert(connection, new WorkflowEvent(0L, run.projectKey(), run.runKey(), "phase_completed",
                phaseKey, "", "info", "Phase passed: " + phaseKey, summary, now));
        return PhaseUpdateResult.ok(next == null ? "" : next.phaseKey(), next == null ? "completed" : "running");
    }

    public PhaseUpdateResult fail(Connection connection, WorkflowRun run, String phaseKey,
                                  String reason, String now) throws SQLException {
        if ("blocked".equals(run.status()) || "failed".equals(run.status())
                || "abandoned".equals(run.status()) || "completed".equals(run.status())) {
            return PhaseUpdateResult.rejected("Workflow run is not fail-able while status is: " + run.status());
        }
        WorkflowPhaseRun phase = phaseRunRepository.find(connection, run.runKey(), phaseKey);
        if (phase == null) {
            return PhaseUpdateResult.notFound("Phase not found: " + phaseKey);
        }
        if (!phaseKey.equals(run.currentPhaseKey())) {
            return PhaseUpdateResult.rejected("Phase is not current. current_phase=" + run.currentPhaseKey());
        }
        phaseRunRepository.updateStatus(connection, run.runKey(), phaseKey, "failed",
                reason, "", now, now);
        runRepository.updateCurrentPhase(connection, run.runKey(), "blocked", phaseKey, now);
        eventRepository.insert(connection, new WorkflowEvent(0L, run.projectKey(), run.runKey(), "run_blocked",
                phaseKey, "", "error", "Phase failed: " + phaseKey, reason, now));
        return PhaseUpdateResult.ok(phaseKey, "blocked");
    }

    private String gateKeys(List<WorkflowGateRun> gates) {
        StringBuilder builder = new StringBuilder();
        for (WorkflowGateRun gate : gates) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(gate.gateKey()).append("=").append(gate.status());
        }
        return builder.toString();
    }

    public static final class PhaseUpdateResult {
        private final boolean ok;
        private final boolean rejected;
        private final String message;
        private final String currentPhase;
        private final String runStatus;

        private PhaseUpdateResult(boolean ok, boolean rejected, String message,
                                  String currentPhase, String runStatus) {
            this.ok = ok;
            this.rejected = rejected;
            this.message = message;
            this.currentPhase = currentPhase;
            this.runStatus = runStatus;
        }

        public static PhaseUpdateResult ok(String currentPhase, String runStatus) {
            return new PhaseUpdateResult(true, false, "", currentPhase, runStatus);
        }

        public static PhaseUpdateResult notFound(String message) {
            return new PhaseUpdateResult(false, false, message, "", "");
        }

        public static PhaseUpdateResult rejected(String message) {
            return new PhaseUpdateResult(false, true, message, "", "");
        }

        public boolean ok() { return ok; }
        public boolean rejected() { return rejected; }
        public String message() { return message; }
        public String currentPhase() { return currentPhase; }
        public String runStatus() { return runStatus; }
    }
}
