package com.devharnesskit.dhk.command.context;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.context.artifact.ContextArtifact;
import com.devharnesskit.dhk.context.artifact.ContextArtifactRepository;
import com.devharnesskit.dhk.context.artifact.ContextArtifactService;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;
import java.sql.Connection;

public final class ContextRetrieveCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final ContextArtifactRepository repository = new ContextArtifactRepository();
    private final ContextArtifactService artifactService = new ContextArtifactService();

    public int run(CommandContext context, Args args) {
        String artifactKey = args.positional(2).trim();
        if (artifactKey.length() == 0) {
            context.err().println("Missing artifact key.");
            context.err().println("Example: dhk context retrieve <artifact-key>");
            return ExitCodes.USAGE_ERROR;
        }
        LineRange range = parseRange(context, args.option("lines", ""));
        if (range == null) {
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = ContextCommandSupport.projectRoot(args, context);
        if (!ContextCommandSupport.requireProjectJson(context, projectRoot)) {
            return ExitCodes.NOT_FOUND;
        }
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = ContextCommandSupport.requireProject(context, projectRoot, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            ContextArtifact artifact = repository.findByKey(connection, project.projectKey(), artifactKey);
            if (artifact == null) {
                context.err().println("Context artifact not found: " + artifactKey);
                return ExitCodes.NOT_FOUND;
            }
            String content = artifactService.range(artifactService.originalContent(artifact),
                    range.startLine, range.endLine);
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "context retrieve"),
                        JsonOutput.stringField("artifact_key", artifact.artifactKey()),
                        JsonOutput.stringField("source_type", artifact.sourceType()),
                        JsonOutput.stringField("source_path", artifact.sourcePath()),
                        JsonOutput.stringField("original_sha256", artifact.originalSha256()),
                        JsonOutput.numberField("token_before", artifact.tokenBefore()),
                        JsonOutput.numberField("token_after", artifact.tokenAfter()),
                        JsonOutput.stringField("content", content)
                ));
                return ExitCodes.SUCCESS;
            }
            context.out().print(content);
            if (!content.endsWith("\n")) {
                context.out().println();
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR context retrieve failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private LineRange parseRange(CommandContext context, String raw) {
        if (raw == null || raw.trim().length() == 0) {
            return new LineRange(0, 0);
        }
        String[] parts = raw.trim().split("-", -1);
        if (parts.length != 2) {
            context.err().println("--lines must use start-end, for example --lines 10-40");
            return null;
        }
        try {
            int start = parts[0].length() == 0 ? 1 : Integer.parseInt(parts[0]);
            int end = parts[1].length() == 0 ? 0 : Integer.parseInt(parts[1]);
            if (start < 1 || (end != 0 && end < start)) {
                context.err().println("--lines must use a positive start and an end greater than start.");
                return null;
            }
            return new LineRange(start, end);
        } catch (NumberFormatException ex) {
            context.err().println("--lines must use numeric start-end, for example --lines 10-40");
            return null;
        }
    }

    private static final class LineRange {
        private final int startLine;
        private final int endLine;

        private LineRange(int startLine, int endLine) {
            this.startLine = startLine;
            this.endLine = endLine;
        }
    }
}
