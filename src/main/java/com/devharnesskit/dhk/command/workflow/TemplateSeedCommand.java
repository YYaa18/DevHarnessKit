package com.devharnesskit.dhk.command.workflow;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateTemplateRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseTemplateRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowTemplateRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.workflow.WorkflowSeedService;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;

public final class TemplateSeedCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final WorkflowSeedService seedService = new WorkflowSeedService(
            new WorkflowTemplateRepository(), new WorkflowPhaseTemplateRepository(), new WorkflowGateTemplateRepository());
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();

    public int run(CommandContext context, Args args) {
        Path projectRoot = WorkflowCommandSupport.projectRoot(args, context);
        PathUtil.createMemoryDirectories(projectRoot);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = WorkflowCommandSupport.ensureProject(context, projectRoot, connection, projectService,
                    projectRepository, migrationRunner);
            WorkflowSeedService.SeedResult result = transactionTemplate.execute(connection,
                    new TransactionTemplate.Work<WorkflowSeedService.SeedResult>() {
                        public WorkflowSeedService.SeedResult execute() throws Exception {
                            return seedService.seed(connection, context.clock().now().toString());
                        }
                    });
            context.out().println("project_key: " + project.projectKey());
            context.out().println("workflow_templates_seeded: " + result.templatesSeeded());
            context.out().println("phase_templates_seeded: " + result.phasesSeeded());
            context.out().println("gate_templates_seeded: " + result.gatesSeeded());
            context.out().println("workflow_templates_inserted: " + result.templatesInserted());
            context.out().println("phase_templates_inserted: " + result.phasesInserted());
            context.out().println("gate_templates_inserted: " + result.gatesInserted());
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR workflow template seed failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
