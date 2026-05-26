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
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try {
            GraphPruneResult result = graphService.prune(projectRoot, keep, context.clock());
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "graph prune"),
                        JsonOutput.numberField("keep", result.keep()),
                        JsonOutput.numberField("deleted_snapshots", result.deletedSnapshots()),
                        JsonOutput.numberField("remaining_snapshots", result.remainingSnapshots()),
                        JsonOutput.numberField("deleted_files", result.deletedFiles()),
                        JsonOutput.numberField("deleted_nodes", result.deletedNodes()),
                        JsonOutput.numberField("deleted_edges", result.deletedEdges()),
                        JsonOutput.numberField("deleted_query_cache_rows", result.deletedQueryCacheRows()),
                        JsonOutput.numberField("deleted_goal_graph_bindings", result.deletedGoalGraphBindings())
                ));
            } else {
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
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR graph prune failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
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
