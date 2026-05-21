package com.devharnesskit.dhk.service.workflow;

import com.devharnesskit.dhk.model.workflow.WorkflowEvent;
import com.devharnesskit.dhk.model.workflow.WorkflowGateRun;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.workflow.WorkflowEventRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;

import java.sql.Connection;
import java.sql.SQLException;

public final class WorkflowGateService {
    private final WorkflowRunRepository runRepository;
    private final WorkflowGateRunRepository gateRunRepository;
    private final WorkflowEventRepository eventRepository;

    public WorkflowGateService(WorkflowRunRepository runRepository,
                               WorkflowGateRunRepository gateRunRepository,
                               WorkflowEventRepository eventRepository) {
        this.runRepository = runRepository;
        this.gateRunRepository = gateRunRepository;
        this.eventRepository = eventRepository;
    }

    public GateUpdateResult update(Connection connection, WorkflowRun run, String gateKey, String action,
                                   String summary, String reason, String evidence, String now) throws SQLException {
        WorkflowGateRun gate = gateRunRepository.findByRunGate(connection, run.runKey(), gateKey);
        if (gate == null) {
            return GateUpdateResult.notFound("Gate not found: " + gateKey);
        }
        String status = statusForAction(action);
        gateRunRepository.updateStatus(connection, gate.id(), status, summary, reason, evidence, now, now);
        String runStatus = run.status();
        if ("fail".equals(action) && "hard".equals(gate.severity())) {
            runStatus = "blocked";
            runRepository.updateCurrentPhase(connection, run.runKey(), runStatus, gate.phaseKey(), now);
            eventRepository.insert(connection, new WorkflowEvent(0L, run.projectKey(), run.runKey(), "run_blocked",
                    gate.phaseKey(), gate.gateKey(), "error", "Hard gate failed: " + gate.gateKey(), reason, now));
        } else {
            String eventType = "waive".equals(action) ? "gate_waived" : "gate_checked";
            String level = "fail".equals(action) ? "warn" : "info";
            eventRepository.insert(connection, new WorkflowEvent(0L, run.projectKey(), run.runKey(), eventType,
                    gate.phaseKey(), gate.gateKey(), level, "Gate " + status + ": " + gate.gateKey(),
                    "fail".equals(action) ? reason : summary, now));
        }
        return GateUpdateResult.ok(status, runStatus);
    }

    private String statusForAction(String action) {
        if ("pass".equals(action)) {
            return "passed";
        }
        if ("fail".equals(action)) {
            return "failed";
        }
        return "waived";
    }

    public static final class GateUpdateResult {
        private final boolean ok;
        private final String message;
        private final String gateStatus;
        private final String runStatus;

        private GateUpdateResult(boolean ok, String message, String gateStatus, String runStatus) {
            this.ok = ok;
            this.message = message;
            this.gateStatus = gateStatus;
            this.runStatus = runStatus;
        }

        public static GateUpdateResult ok(String gateStatus, String runStatus) {
            return new GateUpdateResult(true, "", gateStatus, runStatus);
        }

        public static GateUpdateResult notFound(String message) {
            return new GateUpdateResult(false, message, "", "");
        }

        public boolean ok() { return ok; }
        public String message() { return message; }
        public String gateStatus() { return gateStatus; }
        public String runStatus() { return runStatus; }
    }
}
