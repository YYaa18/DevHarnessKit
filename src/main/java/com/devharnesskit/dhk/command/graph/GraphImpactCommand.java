package com.devharnesskit.dhk.command.graph;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.graph.GraphImpactRequest;
import com.devharnesskit.dhk.model.graph.GraphImpactResult;
import com.devharnesskit.dhk.model.graph.GraphNode;
import com.devharnesskit.dhk.service.graph.GraphImpactService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;

public final class GraphImpactCommand implements Command {
    private final GraphImpactService impactService;

    public GraphImpactCommand() {
        this(new GraphImpactService());
    }

    GraphImpactCommand(GraphImpactService impactService) {
        this.impactService = impactService;
    }

    public int run(CommandContext context, Args args) {
        GraphImpactRequest request = request(args);
        if (request == null) {
            context.err().println("Missing required parameter: one of --file, --symbol, or --sql-table");
            return ExitCodes.USAGE_ERROR;
        }
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try {
            GraphImpactResult result = impactService.impact(projectRoot, request, context.clock());
            if (JsonOutput.enabled(args)) {
                printJson(context, result);
            } else if ("md".equalsIgnoreCase(args.option("format", ""))) {
                context.out().print(new String(Files.readAllBytes(result.impactMapPath()), "UTF-8"));
            } else {
                printText(context, result);
            }
            return result.found() ? ExitCodes.SUCCESS : ExitCodes.NOT_FOUND;
        } catch (Exception ex) {
            context.err().println("ERROR graph impact failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private GraphImpactRequest request(Args args) {
        String query = args.option("file", "").trim();
        String type = "file";
        if (query.length() == 0) {
            query = args.option("symbol", "").trim();
            type = "symbol";
        }
        if (query.length() == 0) {
            query = args.option("sql-table", "").trim();
            type = "sql-table";
        }
        if (query.length() == 0) {
            return null;
        }
        return new GraphImpactRequest(type, query, depth(args), args.hasFlag("allow-stale"));
    }

    private int depth(Args args) {
        String value = args.option("depth", "4").trim();
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            return 4;
        }
    }

    private void printText(CommandContext context, GraphImpactResult result) {
        if (!result.found()) {
            context.err().println("Graph impact query found no matching node: "
                    + result.request().queryType() + "=" + result.request().query());
            if (!result.candidates().isEmpty()) {
                context.err().println("candidate_suggestions:");
                for (GraphNode node : result.candidates()) {
                    context.err().println("- " + node.nodeKind() + " " + displayName(node)
                            + " (" + node.relativePath() + ")");
                }
            }
            context.err().println("impact_map: " + result.impactMapPath());
            return;
        }
        context.out().println("graph impact");
        context.out().println("snapshot_key: " + result.snapshot().snapshotKey());
        context.out().println("snapshot_stale: " + result.snapshotStale());
        context.out().println("allow_stale: " + result.staleAllowed());
        if (result.snapshotStale()) {
            context.out().println("warning: STALE_GRAPH_SNAPSHOT");
        }
        context.out().println("query_type: " + result.request().queryType());
        context.out().println("query: " + result.request().query());
        context.out().println("depth: " + result.request().depth());
        context.out().println("requested_depth: " + result.requestedDepth());
        context.out().println("max_impact_depth: " + result.maxImpactDepth());
        context.out().println("depth_limited: " + result.depthLimited());
        context.out().println("related_files: " + result.relatedFiles().size());
        context.out().println("related_sql: " + result.relatedSql().size());
        context.out().println("related_tests: " + result.relatedTests().size());
        context.out().println("risk_nodes: " + result.riskNodes().size());
        context.out().println("impact_map: " + result.impactMapPath());
        context.out().println("recommended_read_files:");
        for (String file : result.recommendedReadFiles()) {
            context.out().println("- " + file);
        }
    }

    private void printJson(CommandContext context, GraphImpactResult result) {
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "graph impact"),
                JsonOutput.booleanField("found", result.found()),
                JsonOutput.stringField("query_type", result.request().queryType()),
                JsonOutput.stringField("query", result.request().query()),
                JsonOutput.numberField("depth", result.request().depth()),
                JsonOutput.numberField("requested_depth", result.requestedDepth()),
                JsonOutput.numberField("max_impact_depth", result.maxImpactDepth()),
                JsonOutput.booleanField("depth_limited", result.depthLimited()),
                JsonOutput.stringField("snapshot_key", result.snapshot() == null ? "" : result.snapshot().snapshotKey()),
                JsonOutput.booleanField("snapshot_stale", result.snapshotStale()),
                JsonOutput.booleanField("allow_stale", result.staleAllowed()),
                JsonOutput.stringField("current_workspace_fingerprint", result.currentWorkspaceFingerprint()),
                JsonOutput.stringField("snapshot_workspace_fingerprint",
                        result.snapshot() == null ? "" : result.snapshot().workspaceFingerprint()),
                JsonOutput.numberField("related_files", result.relatedFiles().size()),
                JsonOutput.numberField("related_sql", result.relatedSql().size()),
                JsonOutput.numberField("related_tests", result.relatedTests().size()),
                JsonOutput.numberField("risk_nodes", result.riskNodes().size()),
                JsonOutput.rawField("recommended_read_files",
                        JsonOutput.stringArray(result.recommendedReadFiles().toArray(new String[result.recommendedReadFiles().size()]))),
                JsonOutput.stringField("impact_map", result.impactMapPath().toString())
        ));
    }

    private String displayName(GraphNode node) {
        return node.qualifiedName().length() > 0 ? node.qualifiedName() : node.name();
    }
}
