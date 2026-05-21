package com.devharnesskit.dhk.command.workflow;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.model.workflow.WorkflowTemplate;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowEventRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateTemplateRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseTemplateRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowTemplateRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.workflow.WorkflowStartService;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;

public final class StartCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final WorkflowTemplateRepository templateRepository = new WorkflowTemplateRepository();
    private final WorkflowStartService startService = new WorkflowStartService(
            new WorkflowPhaseTemplateRepository(), new WorkflowGateTemplateRepository(),
            new WorkflowRunRepository(), new WorkflowPhaseRunRepository(),
            new WorkflowGateRunRepository(), new WorkflowEventRepository());
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();

    public int run(CommandContext context, Args args) {
        String workflowKey = args.option("workflow").trim();
        String task = args.option("task").trim();
        if (workflowKey.length() == 0 || task.length() == 0) {
            context.err().println("Missing required parameters: --workflow, --task");
            return ExitCodes.USAGE_ERROR;
        }
        String module = args.option("module", "global").trim();
        if (module.length() == 0) {
            module = "global";
        }
        String mode = args.option("mode", "auto").trim();
        if (!WorkflowCommandSupport.isModeAllowed(mode)) {
            context.err().println("Invalid workflow mode: " + mode);
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = WorkflowCommandSupport.projectRoot(args, context);
        PathUtil.createMemoryDirectories(projectRoot);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = WorkflowCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            WorkflowTemplate template = templateRepository.findByKey(connection, workflowKey);
            if (template == null) {
                context.err().println("Workflow template not found: " + workflowKey);
                context.err().println("Run `dhk workflow template seed --project-root <path>` first.");
                return ExitCodes.NOT_FOUND;
            }
            final Project currentProject = project;
            final WorkflowTemplate selectedTemplate = template;
            final String selectedTask = task;
            final String selectedSummary = args.option("summary", "");
            final String selectedModule = module;
            final String selectedMode = mode;
            WorkflowRun run = transactionTemplate.execute(connection, new TransactionTemplate.Work<WorkflowRun>() {
                public WorkflowRun execute() throws Exception {
                    return startService.start(connection, currentProject.projectKey(), selectedTemplate,
                            selectedTask, selectedSummary, selectedModule, selectedMode, context.clock().now());
                }
            });
            context.out().println("run_key: " + run.runKey());
            context.out().println("workflow: " + run.workflowKey());
            context.out().println("status: " + run.status());
            context.out().println("current_phase: " + run.currentPhaseKey());
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR workflow start failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
