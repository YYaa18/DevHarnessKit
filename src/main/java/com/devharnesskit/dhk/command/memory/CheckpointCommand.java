package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.model.Checkpoint;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.CheckpointRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.util.InputUtil;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class CheckpointCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final CheckpointRepository checkpointRepository;
    private final SensitiveDataGuard sensitiveDataGuard;

    public CheckpointCommand() {
        this(new DbConnectionFactory(), new ProjectService(), new CheckpointRepository(), new SensitiveDataGuard());
    }

    CheckpointCommand(DbConnectionFactory connectionFactory, ProjectService projectService,
                      CheckpointRepository checkpointRepository, SensitiveDataGuard sensitiveDataGuard) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.checkpointRepository = checkpointRepository;
        this.sensitiveDataGuard = sensitiveDataGuard;
    }

    public int run(CommandContext context, Args args) {
        String task = args.option("task").trim();
        String summary;
        try {
            summary = InputUtil.readExclusiveText(context, args, "summary", "summary-file", "summary-stdin").trim();
        } catch (InputUtil.InputException ex) {
            return CommandErrorGuidance.invalidUsage(context, args, "MEMORY_CHECKPOINT_INPUT_CONFLICT",
                    ex.getMessage(), "Provide checkpoint summary through only one input source.",
                    "dhk memory checkpoint --task <task> \"<summary>\"", "README.md#core-path");
        }
        if (task.length() == 0 || summary.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_CHECKPOINT_ARGUMENTS_MISSING",
                    new String[]{"--task", "summary"},
                    "dhk memory checkpoint --task <task> \"<summary>\"",
                    "README.md#core-path");
        }
        String module = args.option("module", "global").trim();
        if (module.length() == 0) {
            module = "global";
        }
        String changed = args.option("changed", "").trim();
        String pending = args.option("pending", "").trim();
        String verify = args.option("verify", "").trim();
        String nextRead = args.option("next-read", "").trim();
        List<String> matches = sensitiveDataGuard.findMatches(task + "\n" + summary + "\n" + changed
                + "\n" + pending + "\n" + verify + "\n" + nextRead);
        if (!matches.isEmpty()) {
            context.err().println("Sensitive data rejected: " + matches);
            return ExitCodes.VALIDATION_ERROR;
        }

        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = MemoryCommandSupport.requireInitializedProject(context, projectRoot, projectService, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            Checkpoint checkpoint = new Checkpoint(0L, project.projectKey(), task, module, summary,
                    changed, pending, verify, nextRead, context.clock().now().toString());
            long id = checkpointRepository.insert(connection, checkpoint);
            context.out().println("checkpoint_id: " + id);
            return ExitCodes.SUCCESS;
        } catch (SQLException ex) {
            context.err().println("ERROR memory checkpoint failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
