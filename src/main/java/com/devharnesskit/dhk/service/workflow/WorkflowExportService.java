package com.devharnesskit.dhk.service.workflow;

import com.devharnesskit.dhk.export.WorkflowContextRenderer;
import com.devharnesskit.dhk.model.workflow.WorkflowGateRun;
import com.devharnesskit.dhk.model.workflow.WorkflowPhaseRun;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseRunRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class WorkflowExportService {
    private final WorkflowPhaseRunRepository phaseRunRepository;
    private final WorkflowGateRunRepository gateRunRepository;
    private final WorkflowContextRenderer renderer;

    public WorkflowExportService(WorkflowPhaseRunRepository phaseRunRepository,
                                 WorkflowGateRunRepository gateRunRepository,
                                 WorkflowContextRenderer renderer) {
        this.phaseRunRepository = phaseRunRepository;
        this.gateRunRepository = gateRunRepository;
        this.renderer = renderer;
    }

    public String render(Connection connection, WorkflowRun run, String generatedAt) throws SQLException {
        List<WorkflowPhaseRun> phases = phaseRunRepository.listByRun(connection, run.runKey());
        List<WorkflowGateRun> gates = gateRunRepository.listByRun(connection, run.runKey());
        return renderer.render(run, phases, gates, generatedAt);
    }

    public String renderInline(Connection connection, WorkflowRun run) throws SQLException {
        return renderer.renderInline(run, gateRunRepository.listByRun(connection, run.runKey()));
    }
}
