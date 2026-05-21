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

public final class WorkflowGateService {
    private final WorkflowRunRepository runRepository;
    private final WorkflowGateRunRepository gateRunRepository;
    private final WorkflowPhaseRunRepository phaseRunRepository;
    private final WorkflowEventRepository eventRepository;

    public WorkflowGateService(WorkflowRunRepository runRepository,
                               WorkflowGateRunRepository gateRunRepository,
                               WorkflowPhaseRunRepository phaseRunRepository,
                               WorkflowEventRepository eventRepository) {
        this.runRepository = runRepository;
        this.gateRunRepository = gateRunRepository;
        this.phaseRunRepository = phaseRunRepository;
        this.eventRepository = eventRepository;
    }

    public GateUpdateResult update(Connection connection, WorkflowRun run, String phaseKey, String gateKey,
                                   String action, String summary, String reason, String evidence,
                                   String now) throws SQLException {
        WorkflowGateRun gate = findGate(connection, run, phaseKey, gateKey);
        if (gate == null) {
            return GateUpdateResult.notFound("Gate not found: " + gateKey);
        }
        if ("__ambiguous__".equals(gate.runKey())) {
            return GateUpdateResult.rejected("Ambiguous gate: " + gateKey + ". Specify --phase.");
        }
        String status = statusForAction(action);
        boolean resolvingHardGate = ("pass".equals(action) || "waive".equals(action))
                && "hard".equals(gate.severity());
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
            if (resolvingHardGate && "blocked".equals(run.status())
                    && gate.phaseKey().equals(run.currentPhaseKey())
                    && canResumePhase(connection, run.runKey(), gate.phaseKey())) {
                runStatus = "running";
                runRepository.updateCurrentPhase(connection, run.runKey(), runStatus, gate.phaseKey(), now);
            }
        }
        return GateUpdateResult.ok(status, runStatus);
    }

    private WorkflowGateRun findGate(Connection connection, WorkflowRun run, String phaseKey,
                                     String gateKey) throws SQLException {
        if (phaseKey != null && phaseKey.length() > 0) {
            return gateRunRepository.findByRunPhaseGate(connection, run.runKey(), phaseKey, gateKey);
        }
        List<WorkflowGateRun> matches = gateRunRepository.listByRunGate(connection, run.runKey(), gateKey);
        if (matches.size() > 1) {
            return new WorkflowGateRun(0L, "__ambiguous__", "", gateKey, "", "", "", "", "",
                    "", "", "", "", "", "");
        }
        return matches.isEmpty() ? null : matches.get(0);
    }

    private boolean canResumePhase(Connection connection, String runKey, String phaseKey) throws SQLException {
        if (!gateRunRepository.blockingHardForPhase(connection, runKey, phaseKey).isEmpty()) {
            return false;
        }
        WorkflowPhaseRun phase = phaseRunRepository.find(connection, runKey, phaseKey);
        return phase != null && !"failed".equals(phase.status()) && !"blocked".equals(phase.status());
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
        private final boolean rejected;
        private final String message;
        private final String gateStatus;
        private final String runStatus;

        private GateUpdateResult(boolean ok, boolean rejected, String message,
                                 String gateStatus, String runStatus) {
            this.ok = ok;
            this.rejected = rejected;
            this.message = message;
            this.gateStatus = gateStatus;
            this.runStatus = runStatus;
        }

        public static GateUpdateResult ok(String gateStatus, String runStatus) {
            return new GateUpdateResult(true, false, "", gateStatus, runStatus);
        }

        public static GateUpdateResult notFound(String message) {
            return new GateUpdateResult(false, false, message, "", "");
        }

        public static GateUpdateResult rejected(String message) {
            return new GateUpdateResult(false, true, message, "", "");
        }

        public boolean ok() { return ok; }
        public boolean rejected() { return rejected; }
        public String message() { return message; }
        public String gateStatus() { return gateStatus; }
        public String runStatus() { return runStatus; }
    }
}
