package com.devharnesskit.dhk.command.graph;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.graph.GraphExportResult;
import com.devharnesskit.dhk.service.graph.GraphExportService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;

public final class GraphExportCommand implements Command {
    private final GraphExportService exportService;

    public GraphExportCommand() {
        this(new GraphExportService());
    }

    GraphExportCommand(GraphExportService exportService) {
        this.exportService = exportService;
    }

    public int run(CommandContext context, Args args) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try {
            GraphExportResult result = exportService.export(projectRoot, context.clock());
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "graph export"),
                        JsonOutput.stringField("snapshot_key", result.data().snapshot().snapshotKey()),
                        JsonOutput.numberField("files", result.data().files().size()),
                        JsonOutput.numberField("nodes", result.data().nodes().size()),
                        JsonOutput.numberField("edges", result.data().edges().size()),
                        JsonOutput.stringField("context_path", result.contextPath().toString()),
                        JsonOutput.stringField("snapshot_path", result.snapshotPath().toString())
                ));
            } else {
                context.out().println("graph export complete");
                context.out().println("snapshot_key: " + result.data().snapshot().snapshotKey());
                context.out().println("files: " + result.data().files().size());
                context.out().println("nodes: " + result.data().nodes().size());
                context.out().println("edges: " + result.data().edges().size());
                context.out().println("context_path: " + result.contextPath());
                context.out().println("snapshot_path: " + result.snapshotPath());
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR graph export failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
