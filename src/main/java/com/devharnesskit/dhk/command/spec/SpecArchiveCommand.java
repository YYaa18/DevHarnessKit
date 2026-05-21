package com.devharnesskit.dhk.command.spec;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.spec.SpecAcceptance;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.spec.SpecTask;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.spec.SpecAcceptanceRepository;
import com.devharnesskit.dhk.repository.spec.SpecChangeRepository;
import com.devharnesskit.dhk.repository.spec.SpecDocumentRepository;
import com.devharnesskit.dhk.repository.spec.SpecEventRepository;
import com.devharnesskit.dhk.repository.spec.SpecTaskRepository;
import com.devharnesskit.dhk.repository.spec.WorkflowSpecBindingRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.spec.SpecService;
import com.devharnesskit.dhk.service.spec.SpecStatusService;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

public final class SpecArchiveCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final SpecChangeRepository changeRepository = new SpecChangeRepository();
    private final SpecTaskRepository taskRepository = new SpecTaskRepository();
    private final SpecAcceptanceRepository acceptanceRepository = new SpecAcceptanceRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();
    private final SpecStatusService statusService = new SpecStatusService();
    private final SpecService specService = new SpecService(changeRepository, new SpecDocumentRepository(),
            new SpecEventRepository(), new WorkflowSpecBindingRepository());

    public int run(CommandContext context, Args args) {
        String changeKey = args.option("change").trim();
        String reason = args.option("reason").trim();
        if (changeKey.length() == 0 || reason.length() == 0) {
            context.err().println("Missing required parameters: --change, --reason");
            return ExitCodes.USAGE_ERROR;
        }
        if (SpecCommandSupport.rejectSensitive(context, sensitiveDataGuard, "spec archive",
                changeKey, reason)) {
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
            List<SpecTask> tasks = taskRepository.listByChange(connection, change.changeKey());
            List<SpecAcceptance> acceptances = acceptanceRepository.listByChange(connection, change.changeKey());
            int openTasks = statusService.openTaskCount(tasks);
            int openAcceptances = statusService.openAcceptanceCount(acceptances);
            if (openTasks > 0 || openAcceptances > 0) {
                context.err().println("Spec cannot be archived with open tasks or acceptance.");
                context.err().println("open_task_count: " + openTasks);
                context.err().println("open_acceptance_count: " + openAcceptances);
                return ExitCodes.VALIDATION_ERROR;
            }
            final SpecChange selectedChange = change;
            final String selectedReason = reason;
            transactionTemplate.execute(connection, new TransactionTemplate.Work<Void>() {
                public Void execute() throws Exception {
                    specService.archive(connection, selectedChange, selectedReason,
                            context.clock().now().toString());
                    return null;
                }
            });
            context.out().println("change_key: " + change.changeKey());
            context.out().println("status: archived");
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR spec archive failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
