package com.devharnesskit.dhk.command.spec;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.guidance.EnumGuidance;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.spec.SpecChangeRepository;
import com.devharnesskit.dhk.repository.spec.SpecDocumentRepository;
import com.devharnesskit.dhk.repository.spec.SpecEventRepository;
import com.devharnesskit.dhk.repository.spec.WorkflowSpecBindingRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.spec.SpecService;

import java.nio.file.Path;
import java.sql.Connection;

public final class SpecBindWorkflowCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final SpecChangeRepository changeRepository = new SpecChangeRepository();
    private final WorkflowRunRepository runRepository = new WorkflowRunRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();
    private final SpecService specService = new SpecService(changeRepository, new SpecDocumentRepository(),
            new SpecEventRepository(), new WorkflowSpecBindingRepository());

    public int run(CommandContext context, Args args) {
        String changeKey = args.option("change").trim();
        String runKey = args.option("run").trim();
        String type = args.option("type", "implements").trim();
        if (changeKey.length() == 0 || runKey.length() == 0) {
            context.err().println("Missing required parameters: --change, --run");
            return ExitCodes.USAGE_ERROR;
        }
        if (!SpecService.isWorkflowBindingType(type)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_WORKFLOW_SPEC_BINDING_TYPE",
                    "workflow spec binding type", type, EnumGuidance.SPEC_WORKFLOW_BINDING_TYPES,
                    new String[0], "dhk spec bind-workflow --change <change> --run <run> --type implements",
                    "docs/GOAL_CONFIGURATION.md");
        }
        if (SpecCommandSupport.rejectSensitive(context, sensitiveDataGuard, "spec bind-workflow",
                changeKey, runKey, type)) {
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = SpecCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = SpecCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            SpecChange change = changeRepository.findByKey(connection, changeKey);
            if (!SpecCommandSupport.belongsToProject(change, project)) {
                context.err().println("Spec change not found: " + changeKey);
                return ExitCodes.NOT_FOUND;
            }
            WorkflowRun run = runRepository.findByKey(connection, runKey);
            if (run == null || !project.projectKey().equals(run.projectKey())) {
                context.err().println("Workflow run not found: " + runKey);
                return ExitCodes.NOT_FOUND;
            }
            final SpecChange selectedChange = change;
            final WorkflowRun selectedRun = run;
            final String selectedType = type;
            Long id = transactionTemplate.execute(connection, new TransactionTemplate.Work<Long>() {
                public Long execute() throws Exception {
                    return Long.valueOf(specService.bindWorkflow(connection, selectedChange, selectedRun,
                            selectedType, context.clock().now().toString()));
                }
            });
            context.out().println("workflow_spec_binding_id: " + id.longValue());
            context.out().println("change_key: " + change.changeKey());
            context.out().println("run_key: " + run.runKey());
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR spec bind-workflow failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
