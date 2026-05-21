package com.devharnesskit.dhk.command.workflow;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.model.workflow.WorkflowArtifact;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.workflow.WorkflowArtifactRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

public final class ArtifactCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final WorkflowRunRepository runRepository = new WorkflowRunRepository();
    private final WorkflowArtifactRepository artifactRepository = new WorkflowArtifactRepository();

    public int run(CommandContext context, Args args) {
        String action = args.positional(2);
        if (!"list".equals(action)) {
            context.err().println("Unknown workflow artifact action: " + action);
            return ExitCodes.USAGE_ERROR;
        }
        String runKey = args.option("run").trim();
        if (runKey.length() == 0) {
            context.err().println("Missing required parameter: --run");
            return ExitCodes.USAGE_ERROR;
        }
        Path projectRoot = WorkflowCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, context.clock());
            WorkflowRun run = runRepository.findByKey(connection, runKey);
            if (run == null) {
                context.err().println("Workflow run not found: " + runKey);
                return ExitCodes.NOT_FOUND;
            }
            List<WorkflowArtifact> artifacts = artifactRepository.listByRun(connection, runKey);
            for (WorkflowArtifact artifact : artifacts) {
                context.out().println("[" + artifact.artifactType() + "] " + artifact.title()
                        + "\t" + artifact.status() + "\t" + artifact.producedByPhase()
                        + "\t" + artifact.filePath());
            }
            context.out().println("artifact_count: " + artifacts.size());
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR workflow artifact list failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
