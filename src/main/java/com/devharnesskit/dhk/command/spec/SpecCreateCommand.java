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
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.spec.SpecChangeRepository;
import com.devharnesskit.dhk.repository.spec.SpecDocumentRepository;
import com.devharnesskit.dhk.repository.spec.SpecEventRepository;
import com.devharnesskit.dhk.repository.spec.WorkflowSpecBindingRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.spec.SpecService;

import java.nio.file.Path;
import java.sql.Connection;

public final class SpecCreateCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();
    private final SpecService specService = new SpecService(new SpecChangeRepository(),
            new SpecDocumentRepository(), new SpecEventRepository(), new WorkflowSpecBindingRepository());

    public int run(CommandContext context, Args args) {
        String changeKey = args.option("change").trim();
        String title = args.option("title").trim();
        if (changeKey.length() == 0 || title.length() == 0) {
            context.err().println("Missing required parameters: --change, --title");
            return ExitCodes.USAGE_ERROR;
        }
        if (!SpecCommandSupport.isKeyAllowed(changeKey)) {
            context.err().println("Invalid change key: " + changeKey);
            return ExitCodes.VALIDATION_ERROR;
        }
        String summary = args.option("summary", "").trim();
        String module = args.option("module", "global").trim();
        if (module.length() == 0) {
            module = "global";
        }
        String mode = args.option("mode", "auto").trim();
        String priority = args.option("priority", "normal").trim();
        if (!SpecService.isModeAllowed(mode)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_SPEC_MODE", "spec mode", mode,
                    EnumGuidance.SPEC_MODES, new String[0],
                    "dhk spec create --change <key> --title \"<title>\" --mode api",
                    "docs/GOAL_CONFIGURATION.md");
        }
        if (!SpecService.isPriorityAllowed(priority)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_SPEC_PRIORITY", "spec priority", priority,
                    EnumGuidance.SPEC_PRIORITIES, new String[0],
                    "dhk spec create --change <key> --title \"<title>\" --priority normal",
                    "docs/GOAL_CONFIGURATION.md");
        }
        if (SpecCommandSupport.rejectSensitive(context, sensitiveDataGuard, "spec create",
                changeKey, title, summary, module, mode, priority)) {
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = SpecCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            final Project project = SpecCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            final String selectedChange = changeKey;
            final String selectedTitle = title;
            final String selectedSummary = summary;
            final String selectedModule = module;
            final String selectedMode = mode;
            final String selectedPriority = priority;
            SpecChange created = transactionTemplate.execute(connection, new TransactionTemplate.Work<SpecChange>() {
                public SpecChange execute() throws Exception {
                    return specService.createChange(connection, project, selectedChange, selectedTitle,
                            selectedSummary, selectedModule, selectedMode, selectedPriority,
                            context.clock().now().toString());
                }
            });
            context.out().println("change_key: " + created.changeKey());
            context.out().println("status: " + created.status());
            context.out().println("proposal_document: proposal");
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR spec create failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
