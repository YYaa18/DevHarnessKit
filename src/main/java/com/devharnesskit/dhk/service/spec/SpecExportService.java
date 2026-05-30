package com.devharnesskit.dhk.service.spec;

import com.devharnesskit.dhk.export.SpecContextRenderer;
import com.devharnesskit.dhk.model.spec.SpecAcceptance;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.spec.SpecDocument;
import com.devharnesskit.dhk.model.spec.SpecTask;
import com.devharnesskit.dhk.model.spec.WorkflowSpecBinding;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.spec.SpecAcceptanceRepository;
import com.devharnesskit.dhk.repository.spec.SpecDocumentRepository;
import com.devharnesskit.dhk.repository.spec.SpecTaskRepository;
import com.devharnesskit.dhk.repository.spec.WorkflowSpecBindingRepository;
import com.devharnesskit.dhk.service.bdd.BddTraceService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class SpecExportService {
    private final SpecDocumentRepository documentRepository;
    private final SpecTaskRepository taskRepository;
    private final SpecAcceptanceRepository acceptanceRepository;
    private final WorkflowSpecBindingRepository bindingRepository;
    private final SpecContextRenderer renderer;
    private final BddTraceService bddTraceService = new BddTraceService();

    public SpecExportService(SpecDocumentRepository documentRepository,
                             SpecTaskRepository taskRepository,
                             SpecAcceptanceRepository acceptanceRepository,
                             WorkflowSpecBindingRepository bindingRepository,
                             SpecContextRenderer renderer) {
        this.documentRepository = documentRepository;
        this.taskRepository = taskRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.bindingRepository = bindingRepository;
        this.renderer = renderer;
    }

    public String renderFull(Connection connection, SpecChange change, String generatedAt) throws SQLException {
        List<SpecDocument> documents = documentRepository.listByChange(connection, change.changeKey());
        List<SpecTask> tasks = taskRepository.listByChange(connection, change.changeKey());
        List<SpecAcceptance> acceptances = acceptanceRepository.listByChange(connection, change.changeKey());
        List<WorkflowSpecBinding> bindings = bindingRepository.listByChange(connection, change.changeKey());
        Project project = new Project(change.projectKey(), "", "", "", "", "", "", "", "");
        return renderer.renderFull(change, documents, tasks, acceptances, bindings,
                bddTraceService.specTrace(connection, project, change.changeKey(), acceptances),
                generatedAt);
    }

    public String renderInline(Connection connection, SpecChange change) throws SQLException {
        List<SpecTask> tasks = taskRepository.listByChange(connection, change.changeKey());
        List<SpecAcceptance> acceptances = acceptanceRepository.listByChange(connection, change.changeKey());
        return renderer.renderInline(change, tasks, acceptances);
    }
}
