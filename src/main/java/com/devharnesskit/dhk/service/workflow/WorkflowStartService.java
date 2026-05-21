package com.devharnesskit.dhk.service.workflow;

import com.devharnesskit.dhk.model.workflow.WorkflowEvent;
import com.devharnesskit.dhk.model.workflow.WorkflowGateRun;
import com.devharnesskit.dhk.model.workflow.WorkflowGateTemplate;
import com.devharnesskit.dhk.model.workflow.WorkflowPhaseRun;
import com.devharnesskit.dhk.model.workflow.WorkflowPhaseTemplate;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.model.workflow.WorkflowTemplate;
import com.devharnesskit.dhk.repository.workflow.WorkflowEventRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateTemplateRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseTemplateRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class WorkflowStartService {
    private static final DateTimeFormatter RUN_KEY_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final WorkflowPhaseTemplateRepository phaseTemplateRepository;
    private final WorkflowGateTemplateRepository gateTemplateRepository;
    private final WorkflowRunRepository runRepository;
    private final WorkflowPhaseRunRepository phaseRunRepository;
    private final WorkflowGateRunRepository gateRunRepository;
    private final WorkflowEventRepository eventRepository;

    public WorkflowStartService(WorkflowPhaseTemplateRepository phaseTemplateRepository,
                                WorkflowGateTemplateRepository gateTemplateRepository,
                                WorkflowRunRepository runRepository,
                                WorkflowPhaseRunRepository phaseRunRepository,
                                WorkflowGateRunRepository gateRunRepository,
                                WorkflowEventRepository eventRepository) {
        this.phaseTemplateRepository = phaseTemplateRepository;
        this.gateTemplateRepository = gateTemplateRepository;
        this.runRepository = runRepository;
        this.phaseRunRepository = phaseRunRepository;
        this.gateRunRepository = gateRunRepository;
        this.eventRepository = eventRepository;
    }

    public WorkflowRun start(Connection connection, String projectKey, WorkflowTemplate template,
                             String task, String taskSummary, String module, String mode,
                             Instant nowInstant) throws SQLException {
        String now = nowInstant.toString();
        List<WorkflowPhaseTemplate> phases = phaseTemplateRepository.listByWorkflow(connection, template.workflowKey());
        List<WorkflowGateTemplate> gates = gateTemplateRepository.listByWorkflow(connection, template.workflowKey());
        if (phases.isEmpty()) {
            throw new SQLException("Workflow template has no phases: " + template.workflowKey());
        }
        String runKey = uniqueRunKey(connection, template.workflowKey(), module, nowInstant);
        WorkflowRun run = new WorkflowRun(runKey, projectKey, template.workflowKey(), task, taskSummary,
                module, mode, "running", phases.get(0).phaseKey(), "", "", null, now, "", now, now);
        runRepository.insert(connection, run);
        for (WorkflowPhaseTemplate phase : phases) {
            phaseRunRepository.insert(connection, new WorkflowPhaseRun(0L, runKey, phase.phaseKey(),
                    phase.phaseName(), phase.phaseOrder(), "pending", "", "", "",
                    "", "", "", now, now));
        }
        for (WorkflowGateTemplate gate : gates) {
            gateRunRepository.insert(connection, new WorkflowGateRun(0L, runKey, gate.phaseKey(), gate.gateKey(),
                    gate.gateName(), gate.gateType(), gate.severity(), "pending", "", "",
                    "", "", "", now, now));
        }
        eventRepository.insert(connection, new WorkflowEvent(0L, projectKey, runKey, "run_created",
                "", "", "info", "Workflow run created", template.workflowKey(), now));
        return run;
    }

    private String uniqueRunKey(Connection connection, String workflowKey, String module, Instant now) throws SQLException {
        String base = RUN_KEY_TIME.format(now) + "-" + slug(workflowKey) + "-" + slug(module);
        String candidate = base;
        int suffix = 2;
        while (runRepository.findByKey(connection, candidate) != null) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String slug(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        String slug = lower.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.length() == 0 ? "global" : slug;
    }
}
