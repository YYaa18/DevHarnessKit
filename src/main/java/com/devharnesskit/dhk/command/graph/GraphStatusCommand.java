package com.devharnesskit.dhk.command.graph;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.graph.GraphScanReport;
import com.devharnesskit.dhk.service.graph.GraphService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.IOException;
import java.nio.file.Path;

public final class GraphStatusCommand implements Command {
    private final GraphService graphService;

    public GraphStatusCommand() {
        this(new GraphService());
    }

    GraphStatusCommand(GraphService graphService) {
        this.graphService = graphService;
    }

    public int run(CommandContext context, Args args) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try {
            GraphScanReport report = graphService.status(projectRoot, context.clock());
            Path reportPath = PathUtil.graphIndexReport(projectRoot);
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "graph status"),
                        JsonOutput.stringField("config_source", report.config().loadedFromFile() ? "file" : "default"),
                        JsonOutput.stringField("report_path", reportPath.toString()),
                        JsonOutput.numberField("files_considered", report.filesConsidered()),
                        JsonOutput.numberField("indexed_files", report.indexedFiles()),
                        JsonOutput.numberField("skipped_files", report.skippedFiles())
                ));
            } else {
                context.out().println("graph status");
                context.out().println("config_source: " + (report.config().loadedFromFile() ? "file" : "default"));
                context.out().println("report_path: " + reportPath);
                context.out().println("files_considered: " + report.filesConsidered());
                context.out().println("indexed_files: " + report.indexedFiles());
                context.out().println("skipped_files: " + report.skippedFiles());
            }
            return ExitCodes.SUCCESS;
        } catch (IOException | RuntimeException ex) {
            context.err().println("ERROR graph status failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
