package com.devharnesskit.dhk.command.graph;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.graph.GraphPruneResult;
import com.devharnesskit.dhk.service.graph.GraphService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;

public final class GraphPruneCommand implements Command {
    private final GraphService graphService;

    public GraphPruneCommand() {
        this(new GraphService());
    }

    GraphPruneCommand(GraphService graphService) {
        this.graphService = graphService;
    }

    public int run(CommandContext context, Args args) {
        int keep = keep(args);
        if (keep < 1) {
            context.err().println("Invalid --keep value: must be >= 1");
            return ExitCodes.USAGE_ERROR;
        }
        boolean dryRun = args.hasFlag("dry-run");
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try {
            GraphPruneResult result = graphService.prune(projectRoot, keep, context.clock(), dryRun);
            if (JsonOutput.enabled(args)) {
                printJson(context, result);
            } else {
                printText(context, result);
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR graph prune failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printJson(CommandContext context, GraphPruneResult result) {
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "graph prune"),
                JsonOutput.booleanField("dry_run", result.dryRun()),
                JsonOutput.numberField("keep", result.keep()),
                JsonOutput.numberField("deleted_snapshots", result.dryRun() ? 0 : result.deletedSnapshots()),
                JsonOutput.numberField("deleted_files", result.dryRun() ? 0 : result.deletedFiles()),
                JsonOutput.numberField("deleted_nodes", result.dryRun() ? 0 : result.deletedNodes()),
                JsonOutput.numberField("deleted_edges", result.dryRun() ? 0 : result.deletedEdges()),
                JsonOutput.numberField("deleted_query_cache_rows",
                        result.dryRun() ? 0 : result.deletedQueryCacheRows()),
                JsonOutput.numberField("deleted_goal_graph_bindings",
                        result.dryRun() ? 0 : result.deletedGoalGraphBindings()),
                JsonOutput.numberField("would_delete_snapshots", result.deletedSnapshots()),
                JsonOutput.numberField("would_delete_files", result.deletedFiles()),
                JsonOutput.numberField("would_delete_nodes", result.deletedNodes()),
                JsonOutput.numberField("would_delete_edges", result.deletedEdges()),
                JsonOutput.numberField("would_delete_query_cache_rows", result.deletedQueryCacheRows()),
                JsonOutput.numberField("would_delete_goal_graph_bindings", result.deletedGoalGraphBindings()),
                JsonOutput.numberField("remaining_snapshots", result.remainingSnapshots()),
                JsonOutput.rawField("snapshot_keys", JsonOutput.stringArray(result.snapshotKeys()))
        ));
    }

    private void printText(CommandContext context, GraphPruneResult result) {
        if (result.dryRun()) {
            context.out().println("graph prune dry-run");
            context.out().println("keep: " + result.keep());
            context.out().println("would_delete_snapshots: " + result.deletedSnapshots());
            context.out().println("remaining_snapshots_after_prune: " + result.remainingSnapshots());
            context.out().println("would_delete_files: " + result.deletedFiles());
            context.out().println("would_delete_nodes: " + result.deletedNodes());
            context.out().println("would_delete_edges: " + result.deletedEdges());
            context.out().println("would_delete_query_cache_rows: " + result.deletedQueryCacheRows());
            context.out().println("would_delete_goal_graph_bindings: " + result.deletedGoalGraphBindings());
            context.out().println("snapshot_keys:");
            for (String snapshotKey : result.snapshotKeys()) {
                context.out().println("- " + snapshotKey);
            }
            return;
        }
        context.out().println("graph prune complete");
        context.out().println("keep: " + result.keep());
        context.out().println("deleted_snapshots: " + result.deletedSnapshots());
        context.out().println("remaining_snapshots: " + result.remainingSnapshots());
        context.out().println("deleted_files: " + result.deletedFiles());
        context.out().println("deleted_nodes: " + result.deletedNodes());
        context.out().println("deleted_edges: " + result.deletedEdges());
        context.out().println("deleted_query_cache_rows: " + result.deletedQueryCacheRows());
        context.out().println("deleted_goal_graph_bindings: " + result.deletedGoalGraphBindings());
    }

    private int keep(Args args) {
        String value = args.option("keep", "10").trim();
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
