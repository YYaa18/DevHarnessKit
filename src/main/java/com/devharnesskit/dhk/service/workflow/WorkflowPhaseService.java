package com.devharnesskit.dhk.service.workflow;

import com.devharnesskit.dhk.model.workflow.WorkflowEvent;
import com.devharnesskit.dhk.model.workflow.WorkflowPhaseRun;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.workflow.WorkflowEventRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;

import java.sql.Connection;
import java.sql.SQLException;

public final class WorkflowPhaseService {
    private final WorkflowRunRepository runRepository;
    private final WorkflowPhaseRunRepository phaseRunRepository;
    private final WorkflowEventRepository eventRepository;

    public WorkflowPhaseService(WorkflowRunRepository runRepository,
                                WorkflowPhaseRunRepository phaseRunRepository,
                                WorkflowEventRepository eventRepository) {
        this.runRepository = runRepository;
        this.phaseRunRepository = phaseRunRepository;
        this.eventRepository = eventRepository;
    }

    public PhaseUpdateResult pass(Connection connection, WorkflowRun run, String phaseKey,
                                  String summary, String evidence, String now) throws SQLException {
        WorkflowPhaseRun phase = phaseRunRepository.find(connection, run.runKey(), phaseKey);
        if (phase == null) {
            return PhaseUpdateResult.notFound("Phase not found: " + phaseKey);
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
        WorkflowPhaseRun phase = phaseRunRepository.find(connection, run.runKey(), phaseKey);
        if (phase == null) {
            return PhaseUpdateResult.notFound("Phase not found: " + phaseKey);
        }
        phaseRunRepository.updateStatus(connection, run.runKey(), phaseKey, "failed",
                reason, "", now, now);
        runRepository.updateCurrentPhase(connection, run.runKey(), "blocked", phaseKey, now);
        eventRepository.insert(connection, new WorkflowEvent(0L, run.projectKey(), run.runKey(), "run_blocked",
                phaseKey, "", "error", "Phase failed: " + phaseKey, reason, now));
        return PhaseUpdateResult.ok(phaseKey, "blocked");
    }

    public static final class PhaseUpdateResult {
        private final boolean ok;
        private final String message;
        private final String currentPhase;
        private final String runStatus;

        private PhaseUpdateResult(boolean ok, String message, String currentPhase, String runStatus) {
            this.ok = ok;
            this.message = message;
            this.currentPhase = currentPhase;
            this.runStatus = runStatus;
        }

        public static PhaseUpdateResult ok(String currentPhase, String runStatus) {
            return new PhaseUpdateResult(true, "", currentPhase, runStatus);
        }

        public static PhaseUpdateResult notFound(String message) {
            return new PhaseUpdateResult(false, message, "", "");
        }

        public boolean ok() { return ok; }
        public String message() { return message; }
        public String currentPhase() { return currentPhase; }
        public String runStatus() { return runStatus; }
    }
}
