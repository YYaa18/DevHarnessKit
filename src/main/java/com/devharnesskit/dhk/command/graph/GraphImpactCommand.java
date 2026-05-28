package com.devharnesskit.dhk.command.graph;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.model.graph.GraphImpactRequest;
import com.devharnesskit.dhk.model.graph.GraphImpactResult;
import com.devharnesskit.dhk.model.graph.GraphNode;
import com.devharnesskit.dhk.model.graph.ScenarioGraphImpactResult;
import com.devharnesskit.dhk.service.graph.GraphImpactService;
import com.devharnesskit.dhk.service.graph.ScenarioGraphImpactService;
import com.devharnesskit.dhk.service.policy.PolicyHookService;
import com.devharnesskit.dhk.service.policy.PolicyViolationException;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;

public final class GraphImpactCommand implements Command {
    private final GraphImpactService impactService;
    private final ScenarioGraphImpactService scenarioImpactService;
    private final PolicyHookService policyHookService;

    public GraphImpactCommand() {
        this(new GraphImpactService(), new ScenarioGraphImpactService(), new PolicyHookService());
    }

    GraphImpactCommand(GraphImpactService impactService) {
        this(impactService, new ScenarioGraphImpactService(), new PolicyHookService());
    }

    GraphImpactCommand(GraphImpactService impactService, ScenarioGraphImpactService scenarioImpactService,
                       PolicyHookService policyHookService) {
        this.impactService = impactService;
        this.scenarioImpactService = scenarioImpactService;
        this.policyHookService = policyHookService;
    }

    public int run(CommandContext context, Args args) {
        ImpactTarget target = target(args);
        if (target == null) {
            return CommandErrorGuidance.missing(context, args, "GRAPH_IMPACT_TARGET_MISSING",
                    new String[]{"exactly one of --file|--symbol|--sql-table|--scenario"},
                    "dhk graph impact --file <path>",
                    "docs/GRAPH.md");
        }
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try {
            policyHookService.requireGraphImpactAllowed(projectRoot, args);
            if (target.scenarioKey.length() > 0) {
                ScenarioGraphImpactResult result = scenarioImpactService.impact(projectRoot, target.scenarioKey,
                        depth(args), args.hasFlag("allow-stale"), args.option("allow-stale-evidence", ""),
                        context.clock());
                if (JsonOutput.enabled(args)) {
                    printScenarioJson(context, result);
                } else if ("md".equalsIgnoreCase(args.option("format", ""))) {
                    context.out().print(new String(Files.readAllBytes(result.scenarioImpactMapPath()), "UTF-8"));
                } else {
                    printScenarioText(context, result);
                }
                return result.found() ? ExitCodes.SUCCESS : ExitCodes.NOT_FOUND;
            }
            GraphImpactRequest request = target.request;
            GraphImpactResult result = impactService.impact(projectRoot, request, context.clock());
            if (JsonOutput.enabled(args)) {
                printJson(context, result);
            } else if ("md".equalsIgnoreCase(args.option("format", ""))) {
                context.out().print(new String(Files.readAllBytes(result.impactMapPath()), "UTF-8"));
            } else {
                printText(context, result);
            }
            return result.found() ? ExitCodes.SUCCESS : ExitCodes.NOT_FOUND;
        } catch (PolicyViolationException ex) {
            context.err().println(ex.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR graph impact failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private ImpactTarget target(Args args) {
        String file = args.option("file", "").trim();
        String symbol = args.option("symbol", "").trim();
        String sqlTable = args.option("sql-table", "").trim();
        String scenario = args.option("scenario", "").trim();
        int count = (file.length() > 0 ? 1 : 0) + (symbol.length() > 0 ? 1 : 0)
                + (sqlTable.length() > 0 ? 1 : 0);
        count += scenario.length() > 0 ? 1 : 0;
        if (count != 1) {
            return null;
        }
        if (scenario.length() > 0) {
            return new ImpactTarget(scenario);
        }
        if (file.length() > 0) {
            return new ImpactTarget(new GraphImpactRequest("file", file, depth(args), args.hasFlag("allow-stale"),
                    args.option("allow-stale-evidence", "")));
        }
        if (symbol.length() > 0) {
            return new ImpactTarget(new GraphImpactRequest("symbol", symbol, depth(args), args.hasFlag("allow-stale"),
                    args.option("allow-stale-evidence", "")));
        }
        return new ImpactTarget(new GraphImpactRequest("sql-table", sqlTable, depth(args), args.hasFlag("allow-stale"),
                args.option("allow-stale-evidence", "")));
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
        if (result.staleAllowed()) {
            context.out().println("allow_stale_evidence: "
                    + (result.request().allowStaleEvidence().length() > 0 ? "provided" : "policy"));
        }
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

    private void printScenarioText(CommandContext context, ScenarioGraphImpactResult result) {
        if (!result.found()) {
            context.err().println("Graph scenario impact found no matching graph nodes: scenario="
                    + result.scenarioKey());
            context.err().println("scenario_impact_map: " + result.scenarioImpactMapPath());
            return;
        }
        context.out().println("graph scenario impact");
        context.out().println("scenario_key: " + result.scenarioKey());
        context.out().println("input_bindings: " + result.inputBindings().size());
        context.out().println("impact_results: " + result.impactResults().size());
        context.out().println("found_results: " + result.foundCount());
        context.out().println("snapshot_stale: " + result.snapshotStale());
        context.out().println("allow_stale: " + result.staleAllowed());
        context.out().println("related_files: " + result.relatedFiles().size());
        context.out().println("related_tests: " + result.relatedTests().size());
        context.out().println("related_sql: " + result.relatedSql().size());
        context.out().println("risk_nodes: " + result.riskNodes().size());
        context.out().println("scenario_impact_map: " + result.scenarioImpactMapPath());
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
                JsonOutput.stringField("allow_stale_evidence",
                        result.request().allowStaleEvidence().length() > 0 ? "provided" : ""),
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

    private void printScenarioJson(CommandContext context, ScenarioGraphImpactResult result) {
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "graph impact"),
                JsonOutput.stringField("scenario_key", result.scenarioKey()),
                JsonOutput.booleanField("found", result.found()),
                JsonOutput.numberField("input_bindings", result.inputBindings().size()),
                JsonOutput.numberField("impact_results", result.impactResults().size()),
                JsonOutput.numberField("found_results", result.foundCount()),
                JsonOutput.booleanField("snapshot_stale", result.snapshotStale()),
                JsonOutput.booleanField("allow_stale", result.staleAllowed()),
                JsonOutput.numberField("related_files", result.relatedFiles().size()),
                JsonOutput.numberField("related_tests", result.relatedTests().size()),
                JsonOutput.numberField("related_sql", result.relatedSql().size()),
                JsonOutput.numberField("risk_nodes", result.riskNodes().size()),
                JsonOutput.rawField("recommended_read_files",
                        JsonOutput.stringArray(result.recommendedReadFiles().toArray(
                                new String[result.recommendedReadFiles().size()]))),
                JsonOutput.stringField("scenario_impact_map", result.scenarioImpactMapPath().toString())
        ));
    }

    private String displayName(GraphNode node) {
        return node.qualifiedName().length() > 0 ? node.qualifiedName() : node.name();
    }

    private static final class ImpactTarget {
        private final GraphImpactRequest request;
        private final String scenarioKey;

        private ImpactTarget(GraphImpactRequest request) {
            this.request = request;
            this.scenarioKey = "";
        }

        private ImpactTarget(String scenarioKey) {
            this.request = null;
            this.scenarioKey = scenarioKey;
        }
    }
}
