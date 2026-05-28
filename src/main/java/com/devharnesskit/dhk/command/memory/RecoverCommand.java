package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.export.RecoveryContextRenderer;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.model.Checkpoint;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.CheckpointRepository;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class RecoverCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final CheckpointRepository checkpointRepository;
    private final MemoryRepository memoryRepository;
    private final RecoveryContextRenderer renderer;
    private final SensitiveDataGuard sensitiveDataGuard;

    public RecoverCommand() {
        this(new DbConnectionFactory(), new ProjectService(), new CheckpointRepository(),
                new MemoryRepository(), new RecoveryContextRenderer(), new SensitiveDataGuard());
    }

    RecoverCommand(DbConnectionFactory connectionFactory, ProjectService projectService,
                   CheckpointRepository checkpointRepository, MemoryRepository memoryRepository,
                   RecoveryContextRenderer renderer, SensitiveDataGuard sensitiveDataGuard) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.checkpointRepository = checkpointRepository;
        this.memoryRepository = memoryRepository;
        this.renderer = renderer;
        this.sensitiveDataGuard = sensitiveDataGuard;
    }

    public int run(CommandContext context, Args args) {
        if (!args.hasFlag("latest")) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_RECOVER_LATEST_MISSING",
                    new String[]{"--latest"}, "dhk memory recover --latest",
                    "README.md#core-path");
        }
        String module = args.option("module", "").trim();
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        Path out = args.hasOption("out")
                ? PathUtil.resolvePath(args.option("out"), context.workingDirectory())
                : PathUtil.recoveryContext(projectRoot);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = MemoryCommandSupport.requireInitializedProject(context, projectRoot, projectService, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            Checkpoint checkpoint = checkpointRepository.latest(connection, project.projectKey(), module);
            if (checkpoint == null) {
                return CommandErrorGuidance.notFound(context, args, "MEMORY_CHECKPOINT_NOT_FOUND",
                        "checkpoint", module.length() == 0 ? "latest" : module,
                        "dhk memory checkpoint --task <task> \"<summary>\"",
                        "README.md#core-path");
            }
            List<MemoryItem> memory = memoryRepository.listConfirmedForExport(connection, project.projectKey(),
                    checkpoint.moduleName(), 20);
            String markdown = renderer.render(context.clock().now().toString(), checkpoint, memory);
            markdown = sensitiveDataGuard.redact(markdown);
            List<String> matches = sensitiveDataGuard.findMatches(markdown);
            if (!matches.isEmpty()) {
                context.err().println("Sensitive data rejected during recover: " + matches);
                return ExitCodes.VALIDATION_ERROR;
            }
            Files.createDirectories(out.getParent());
            Files.write(out, markdown.getBytes("UTF-8"));
            context.out().println("recovery_path: " + out);
            context.out().println("checkpoint_id: " + checkpoint.id());
            return ExitCodes.SUCCESS;
        } catch (SQLException ex) {
            context.err().println("ERROR memory recover failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR memory recover failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
