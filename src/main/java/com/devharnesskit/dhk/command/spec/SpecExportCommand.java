package com.devharnesskit.dhk.command.spec;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.export.SpecContextRenderer;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.spec.SpecAcceptanceRepository;
import com.devharnesskit.dhk.repository.spec.SpecChangeRepository;
import com.devharnesskit.dhk.repository.spec.SpecDocumentRepository;
import com.devharnesskit.dhk.repository.spec.SpecTaskRepository;
import com.devharnesskit.dhk.repository.spec.WorkflowSpecBindingRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.spec.SpecExportService;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

public final class SpecExportCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final SpecChangeRepository changeRepository = new SpecChangeRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final SpecExportService exportService = new SpecExportService(
            new SpecDocumentRepository(), new SpecTaskRepository(), new SpecAcceptanceRepository(),
            new WorkflowSpecBindingRepository(), new SpecContextRenderer());

    public int run(CommandContext context, Args args) {
        String changeKey = args.option("change").trim();
        if (changeKey.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "SPEC_EXPORT_CHANGE_MISSING",
                    new String[]{"--change"}, "dhk spec export --change <change>",
                    "docs/GOAL_CONFIGURATION.md");
        }
        Path projectRoot = SpecCommandSupport.projectRoot(args, context);
        Path out = args.hasOption("out")
                ? PathUtil.resolvePath(args.option("out"), context.workingDirectory())
                : PathUtil.specContext(projectRoot);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = SpecCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            SpecChange change = changeRepository.findByKey(connection, changeKey);
            if (!SpecCommandSupport.belongsToProject(change, project)) {
                return CommandErrorGuidance.notFound(context, args, "SPEC_CHANGE_NOT_FOUND",
                        "spec change", changeKey, "dhk spec status --change <change>",
                        "docs/GOAL_CONFIGURATION.md");
            }
            String markdown = exportService.renderFull(connection, change, context.clock().now().toString());
            markdown = sensitiveDataGuard.redact(markdown);
            List<String> matches = sensitiveDataGuard.findMatches(markdown);
            if (!matches.isEmpty()) {
                context.err().println("Sensitive data rejected during spec export: " + matches);
                return ExitCodes.VALIDATION_ERROR;
            }
            Files.createDirectories(out.getParent());
            Files.write(out, markdown.getBytes("UTF-8"));
            context.out().println("export_path: " + out);
            context.out().println("change_key: " + change.changeKey());
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR spec export failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
