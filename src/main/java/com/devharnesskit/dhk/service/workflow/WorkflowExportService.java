package com.devharnesskit.dhk.service.workflow;

import com.devharnesskit.dhk.export.WorkflowContextRenderer;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.workflow.WorkflowGateRun;
import com.devharnesskit.dhk.model.workflow.WorkflowPhaseRun;
import com.devharnesskit.dhk.model.workflow.WorkflowPhaseTemplate;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseTemplateRepository;
import com.devharnesskit.dhk.service.bdd.BddTraceService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class WorkflowExportService {
    private final WorkflowPhaseRunRepository phaseRunRepository;
    private final WorkflowGateRunRepository gateRunRepository;
    private final WorkflowPhaseTemplateRepository phaseTemplateRepository;
    private final WorkflowContextRenderer renderer;
    private final BddTraceService bddTraceService = new BddTraceService();

    public WorkflowExportService(WorkflowPhaseRunRepository phaseRunRepository,
                                 WorkflowGateRunRepository gateRunRepository,
                                 WorkflowPhaseTemplateRepository phaseTemplateRepository,
                                 WorkflowContextRenderer renderer) {
        this.phaseRunRepository = phaseRunRepository;
        this.gateRunRepository = gateRunRepository;
        this.phaseTemplateRepository = phaseTemplateRepository;
        this.renderer = renderer;
    }

    public String render(Connection connection, WorkflowRun run, String generatedAt) throws SQLException {
        List<WorkflowPhaseRun> phases = phaseRunRepository.listByRun(connection, run.runKey());
        List<WorkflowGateRun> gates = gateRunRepository.listByRun(connection, run.runKey());
        WorkflowPhaseTemplate currentTemplate = phaseTemplateRepository.find(connection,
                run.workflowKey(), run.currentPhaseKey());
        Project project = new Project(run.projectKey(), "", "", "", "", "", "", "", "");
        return renderer.render(run, phases, gates, currentTemplate,
                bddTraceService.workflowTrace(connection, project, run.runKey()), generatedAt);
    }

    public String renderInline(Connection connection, WorkflowRun run) throws SQLException {
        WorkflowPhaseRun currentPhase = phaseRunRepository.find(connection, run.runKey(), run.currentPhaseKey());
        WorkflowPhaseTemplate currentTemplate = phaseTemplateRepository.find(connection,
                run.workflowKey(), run.currentPhaseKey());
        return renderer.renderInline(run, currentPhase, currentTemplate,
                gateRunRepository.listByRun(connection, run.runKey()));
    }
}
