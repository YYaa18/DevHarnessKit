package com.devharnesskit.dhk.command.context;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.context.artifact.ContextArtifact;
import com.devharnesskit.dhk.context.artifact.ContextArtifactRepository;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public final class ContextArtifactsCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final ContextArtifactRepository repository = new ContextArtifactRepository();

    public int run(CommandContext context, Args args) {
        Path projectRoot = ContextCommandSupport.projectRoot(args, context);
        String goalKey = args.option("goal", "").trim();
        String sourceType = args.option("type", "").trim();
        int limit = ContextCommandSupport.parseLimit(context, args.option("limit", "50"),
                "dhk context artifacts --limit 50");
        int offset = ContextCommandSupport.parseNonNegativeInt(context, args.option("offset", "0"),
                "dhk context artifacts --offset 0");
        if (limit <= 0) {
            return ExitCodes.VALIDATION_ERROR;
        }
        if (offset < 0) {
            return ExitCodes.VALIDATION_ERROR;
        }
        if (!ContextCommandSupport.requireProjectJson(context, projectRoot)) {
            return ExitCodes.NOT_FOUND;
        }
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = ContextCommandSupport.requireProject(context, projectRoot, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            List<ContextArtifact> artifacts = repository.list(connection, project.projectKey(), goalKey,
                    sourceType, limit, offset);
            if (JsonOutput.enabled(args)) {
                printJson(context, project, goalKey, sourceType, limit, offset, artifacts);
                return ExitCodes.SUCCESS;
            }
            if (artifacts.isEmpty()) {
                context.out().println("No context artifacts found.");
                return ExitCodes.SUCCESS;
            }
            for (ContextArtifact artifact : artifacts) {
                context.out().println(artifact.artifactKey());
                context.out().println("  source_type: " + artifact.sourceType());
                context.out().println("  goal_key: " + (artifact.goalKey().length() == 0 ? "global" : artifact.goalKey()));
                context.out().println("  tokens: " + artifact.tokenBefore() + " -> " + artifact.tokenAfter());
                context.out().println("  omitted_lines: " + artifact.omittedLines());
                context.out().println("  created_at: " + artifact.createdAt());
                if (artifact.sourcePath().length() > 0) {
                    context.out().println("  source_path: " + artifact.sourcePath());
                }
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR context artifacts failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printJson(CommandContext context, Project project, String goalKey, String sourceType,
                           int limit, int offset, List<ContextArtifact> artifacts) {
        List<String> raw = new ArrayList<String>();
        for (ContextArtifact artifact : artifacts) {
            raw.add(JsonOutput.object(
                    JsonOutput.stringField("artifact_key", artifact.artifactKey()),
                    JsonOutput.stringField("source_type", artifact.sourceType()),
                    JsonOutput.stringField("goal_key", artifact.goalKey()),
                    JsonOutput.stringField("source_path", artifact.sourcePath()),
                    JsonOutput.stringField("original_sha256", artifact.originalSha256()),
                    JsonOutput.numberField("omitted_lines", artifact.omittedLines()),
                    JsonOutput.numberField("token_before", artifact.tokenBefore()),
                    JsonOutput.numberField("token_after", artifact.tokenAfter()),
                    JsonOutput.stringField("created_at", artifact.createdAt())
            ).trim());
        }
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "context artifacts"),
                JsonOutput.stringField("project_key", project.projectKey()),
                JsonOutput.stringField("goal_key", goalKey),
                JsonOutput.stringField("source_type", sourceType),
                JsonOutput.numberField("limit", limit),
                JsonOutput.numberField("offset", offset),
                JsonOutput.numberField("count", artifacts.size()),
                JsonOutput.rawField("artifacts", JsonOutput.array(raw))
        ));
    }
}
