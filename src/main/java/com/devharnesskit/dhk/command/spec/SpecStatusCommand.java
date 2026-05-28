package com.devharnesskit.dhk.command.spec;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.spec.SpecAcceptance;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.spec.SpecDocument;
import com.devharnesskit.dhk.model.spec.SpecTask;
import com.devharnesskit.dhk.model.spec.WorkflowSpecBinding;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.spec.SpecAcceptanceRepository;
import com.devharnesskit.dhk.repository.spec.SpecChangeRepository;
import com.devharnesskit.dhk.repository.spec.SpecDocumentRepository;
import com.devharnesskit.dhk.repository.spec.SpecTaskRepository;
import com.devharnesskit.dhk.repository.spec.WorkflowSpecBindingRepository;
import com.devharnesskit.dhk.service.ProjectService;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

public final class SpecStatusCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final SpecChangeRepository changeRepository = new SpecChangeRepository();
    private final SpecDocumentRepository documentRepository = new SpecDocumentRepository();
    private final SpecTaskRepository taskRepository = new SpecTaskRepository();
    private final SpecAcceptanceRepository acceptanceRepository = new SpecAcceptanceRepository();
    private final WorkflowSpecBindingRepository bindingRepository = new WorkflowSpecBindingRepository();

    public int run(CommandContext context, Args args) {
        String changeKey = args.option("change").trim();
        if (changeKey.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "SPEC_STATUS_CHANGE_MISSING",
                    new String[]{"--change"}, "dhk spec status --change <change>",
                    "docs/GOAL_CONFIGURATION.md");
        }
        Path projectRoot = SpecCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = SpecCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            SpecChange change = changeRepository.findByKey(connection, changeKey);
            if (!SpecCommandSupport.belongsToProject(change, project)) {
                return CommandErrorGuidance.notFound(context, args, "SPEC_CHANGE_NOT_FOUND",
                        "spec change", changeKey, "dhk spec create --change <key> --title \"<title>\"",
                        "docs/GOAL_CONFIGURATION.md");
            }
            context.out().println("change: " + change.changeKey());
            context.out().println("title: " + change.title());
            context.out().println("status: " + change.status());
            context.out().println("module: " + change.moduleName());
            context.out().println("mode: " + change.mode());
            context.out().println();
            context.out().println("documents:");
            List<SpecDocument> documents = documentRepository.listByChange(connection, change.changeKey());
            for (SpecDocument document : documents) {
                context.out().println("- " + document.documentType() + " " + document.status()
                        + " v" + document.version());
            }
            context.out().println();
            context.out().println("tasks:");
            List<SpecTask> tasks = taskRepository.listByChange(connection, change.changeKey());
            for (SpecTask task : tasks) {
                context.out().println("[" + task.status() + "] " + task.taskKey() + " " + task.title());
            }
            context.out().println();
            context.out().println("acceptance:");
            List<SpecAcceptance> acceptances = acceptanceRepository.listByChange(connection, change.changeKey());
            for (SpecAcceptance acceptance : acceptances) {
                context.out().println("[" + acceptance.status() + "] "
                        + acceptance.acceptanceKey() + " " + acceptance.description());
            }
            context.out().println();
            context.out().println("bound_workflows:");
            List<WorkflowSpecBinding> bindings = bindingRepository.listByChange(connection, change.changeKey());
            for (WorkflowSpecBinding binding : bindings) {
                context.out().println("- " + binding.runKey() + " " + binding.bindingType());
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR spec status failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
