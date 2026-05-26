package com.devharnesskit.dhk.command.graph;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.graph.GraphIndexResult;
import com.devharnesskit.dhk.model.graph.GraphSnapshot;
import com.devharnesskit.dhk.service.graph.GraphService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;

public final class GraphIndexCommand implements Command {
    private final GraphService graphService;

    public GraphIndexCommand() {
        this(new GraphService());
    }

    GraphIndexCommand(GraphService graphService) {
        this.graphService = graphService;
    }

    public int run(CommandContext context, Args args) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try {
            GraphIndexResult result = graphService.index(projectRoot, context.clock());
            GraphSnapshot snapshot = result.snapshot();
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "graph index"),
                        JsonOutput.stringField("snapshot_key", snapshot.snapshotKey()),
                        JsonOutput.stringField("status", snapshot.status()),
                        JsonOutput.stringField("provider", snapshot.provider()),
                        JsonOutput.stringField("workspace_fingerprint", snapshot.workspaceFingerprint()),
                        JsonOutput.numberField("files_considered", result.report().filesConsidered()),
                        JsonOutput.numberField("indexed_files", result.report().indexedFiles()),
                        JsonOutput.numberField("skipped_files", result.report().skippedFiles()),
                        JsonOutput.numberField("max_file_bytes", result.report().config().maxFileBytes()),
                        JsonOutput.numberField("max_indexed_files", result.report().config().maxIndexedFiles()),
                        JsonOutput.numberField("max_impact_depth", result.report().config().maxImpactDepth()),
                        JsonOutput.numberField("max_export_nodes", result.report().config().maxExportNodes()),
                        JsonOutput.numberField("graph_nodes", snapshot.nodeCount()),
                        JsonOutput.numberField("graph_edges", snapshot.edgeCount()),
                        JsonOutput.stringField("report_path", result.reportPath().toString())
                ));
            } else {
                context.out().println("graph index complete");
                context.out().println("snapshot_key: " + snapshot.snapshotKey());
                context.out().println("status: " + snapshot.status());
                context.out().println("provider: " + snapshot.provider());
                context.out().println("workspace_fingerprint: " + snapshot.workspaceFingerprint());
                context.out().println("files_considered: " + result.report().filesConsidered());
                context.out().println("indexed_files: " + result.report().indexedFiles());
                context.out().println("skipped_files: " + result.report().skippedFiles());
                context.out().println("max_file_bytes: " + result.report().config().maxFileBytes());
                context.out().println("max_indexed_files: " + result.report().config().maxIndexedFiles());
                context.out().println("max_impact_depth: " + result.report().config().maxImpactDepth());
                context.out().println("max_export_nodes: " + result.report().config().maxExportNodes());
                context.out().println("graph_nodes: " + snapshot.nodeCount());
                context.out().println("graph_edges: " + snapshot.edgeCount());
                context.out().println("report_path: " + result.reportPath());
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR graph index failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
