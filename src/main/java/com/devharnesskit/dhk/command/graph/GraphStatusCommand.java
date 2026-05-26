package com.devharnesskit.dhk.command.graph;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.graph.GraphScanReport;
import com.devharnesskit.dhk.model.graph.GraphSnapshot;
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
            GraphSnapshot latest = report.latestSnapshot();
            Path reportPath = PathUtil.graphIndexReport(projectRoot);
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "graph status"),
                        JsonOutput.stringField("config_source", report.config().loadedFromFile() ? "file" : "default"),
                        JsonOutput.stringField("report_path", reportPath.toString()),
                        JsonOutput.numberField("files_considered", report.filesConsidered()),
                        JsonOutput.numberField("indexed_files", report.indexedFiles()),
                        JsonOutput.numberField("skipped_files", report.skippedFiles()),
                        JsonOutput.numberField("max_file_bytes", report.config().maxFileBytes()),
                        JsonOutput.numberField("max_indexed_files", report.config().maxIndexedFiles()),
                        JsonOutput.numberField("max_impact_depth", report.config().maxImpactDepth()),
                        JsonOutput.numberField("max_export_nodes", report.config().maxExportNodes()),
                        JsonOutput.numberField("graph_nodes", report.parseResult().nodes().size()),
                        JsonOutput.numberField("graph_edges", report.parseResult().edges().size()),
                        JsonOutput.numberField("parse_errors", report.parseResult().errors().size()),
                        JsonOutput.stringField("latest_snapshot_key", latest == null ? "" : latest.snapshotKey()),
                        JsonOutput.stringField("latest_snapshot_status", latest == null ? "" : latest.status()),
                        JsonOutput.numberField("latest_snapshot_nodes", latest == null ? 0 : latest.nodeCount()),
                        JsonOutput.numberField("latest_snapshot_edges", latest == null ? 0 : latest.edgeCount())
                ));
            } else {
                context.out().println("graph status");
                context.out().println("config_source: " + (report.config().loadedFromFile() ? "file" : "default"));
                context.out().println("report_path: " + reportPath);
                context.out().println("files_considered: " + report.filesConsidered());
                context.out().println("indexed_files: " + report.indexedFiles());
                context.out().println("skipped_files: " + report.skippedFiles());
                context.out().println("max_file_bytes: " + report.config().maxFileBytes());
                context.out().println("max_indexed_files: " + report.config().maxIndexedFiles());
                context.out().println("max_impact_depth: " + report.config().maxImpactDepth());
                context.out().println("max_export_nodes: " + report.config().maxExportNodes());
                context.out().println("graph_nodes: " + report.parseResult().nodes().size());
                context.out().println("graph_edges: " + report.parseResult().edges().size());
                context.out().println("parse_errors: " + report.parseResult().errors().size());
                if (latest == null) {
                    context.out().println("latest_snapshot_key: none");
                } else {
                    context.out().println("latest_snapshot_key: " + latest.snapshotKey());
                    context.out().println("latest_snapshot_status: " + latest.status());
                    context.out().println("latest_snapshot_nodes: " + latest.nodeCount());
                    context.out().println("latest_snapshot_edges: " + latest.edgeCount());
                }
            }
            return ExitCodes.SUCCESS;
        } catch (IOException | RuntimeException ex) {
            context.err().println("ERROR graph status failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
