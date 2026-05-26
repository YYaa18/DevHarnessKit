package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.graph.GraphEdge;
import com.devharnesskit.dhk.model.graph.GraphImpactResult;
import com.devharnesskit.dhk.model.graph.GraphNode;

import java.time.Instant;

public final class GraphImpactRenderer {
    public String render(GraphImpactResult result, Instant generatedAt) {
        StringBuilder builder = new StringBuilder();
        builder.append("# IMPACT_MAP\n\n");
        builder.append("<generated-at>").append(generatedAt.toString()).append("</generated-at>\n\n");
        builder.append("<summary>\n");
        builder.append("- query_type: ").append(result.request().queryType()).append('\n');
        builder.append("- query: ").append(safe(result.request().query())).append('\n');
        builder.append("- effective_depth: ").append(result.request().depth()).append('\n');
        builder.append("- requested_depth: ").append(result.requestedDepth()).append('\n');
        builder.append("- max_impact_depth: ").append(result.maxImpactDepth()).append('\n');
        builder.append("- depth_limited: ").append(result.depthLimited()).append('\n');
        builder.append("- found: ").append(result.found()).append('\n');
        if (result.snapshot() != null) {
            builder.append("- snapshot_key: ").append(result.snapshot().snapshotKey()).append('\n');
            builder.append("- snapshot_status: ").append(result.snapshot().status()).append('\n');
            builder.append("- snapshot_workspace_fingerprint: ")
                    .append(result.snapshot().workspaceFingerprint()).append('\n');
        }
        if (result.currentWorkspaceFingerprint().length() > 0) {
            builder.append("- current_workspace_fingerprint: ")
                    .append(result.currentWorkspaceFingerprint()).append('\n');
        }
        builder.append("- snapshot_stale: ").append(result.snapshotStale()).append('\n');
        builder.append("- allow_stale: ").append(result.staleAllowed()).append('\n');
        builder.append("- start_nodes: ").append(result.startNodes().size()).append('\n');
        builder.append("- impacted_nodes: ").append(result.impactedNodes().size()).append('\n');
        builder.append("- related_files: ").append(result.relatedFiles().size()).append('\n');
        builder.append("- related_sql: ").append(result.relatedSql().size()).append('\n');
        builder.append("- related_tests: ").append(result.relatedTests().size()).append('\n');
        builder.append("- missing_related_tests: ").append(result.missingRelatedTests().size()).append('\n');
        builder.append("- risk_nodes: ").append(result.riskNodes().size()).append('\n');
        builder.append("</summary>\n\n");

        appendSnapshotFreshness(builder, result);
        appendGraphConfidence(builder, result);
        appendNodes(builder, "start-nodes", result.startNodes());
        appendEdges(builder, "direct-callers", result.directCallers());
        appendEdges(builder, "direct-callees", result.directCallees());
        appendStrings(builder, "related-files", result.relatedFiles());
        appendNodes(builder, "related-sql", result.relatedSql());
        appendStrings(builder, "related-tests", result.relatedTests());
        appendStrings(builder, "missing-related-tests", result.missingRelatedTests());
        appendNodes(builder, "risk-nodes", result.riskNodes());
        appendStrings(builder, "recommended-read-files", result.recommendedReadFiles());
        appendNodes(builder, "candidate-suggestions", result.candidates());

        builder.append("<scoring-data>\n");
        builder.append("- impact_file_count: ").append(result.relatedFiles().size()).append('\n');
        builder.append("- related_test_count: ").append(result.relatedTests().size()).append('\n');
        builder.append("- missing_related_test_count: ").append(result.missingRelatedTests().size()).append('\n');
        builder.append("- sql_node_count: ").append(result.relatedSql().size()).append('\n');
        builder.append("- risk_node_count: ").append(result.riskNodes().size()).append('\n');
        builder.append("- ground_truth_ready: false\n");
        builder.append("- scoring_note: compare related-files against task ground truth outside the CLI\n");
        builder.append("</scoring-data>\n");
        return builder.toString();
    }

    private void appendSnapshotFreshness(StringBuilder builder, GraphImpactResult result) {
        builder.append("<snapshot-freshness>\n");
        builder.append("- status: ").append(freshnessStatus(result)).append('\n');
        if (result.snapshot() != null) {
            builder.append("- snapshot_key: ").append(result.snapshot().snapshotKey()).append('\n');
            builder.append("- snapshot_workspace_fingerprint: ")
                    .append(result.snapshot().workspaceFingerprint()).append('\n');
        }
        builder.append("- current_workspace_fingerprint: ")
                .append(result.currentWorkspaceFingerprint()).append('\n');
        builder.append("- allow_stale: ").append(result.staleAllowed()).append('\n');
        if (result.snapshotStale()) {
            builder.append("- warning: STALE_GRAPH_SNAPSHOT; impact map is based on an older graph snapshot\n");
            builder.append("- next_command: dhk graph index\n");
        }
        builder.append("</snapshot-freshness>\n\n");
    }

    private void appendGraphConfidence(StringBuilder builder, GraphImpactResult result) {
        builder.append("<graph-confidence>\n");
        builder.append("- provider: ").append(result.snapshot() == null ? "unknown" : result.snapshot().provider()).append('\n');
        builder.append("- precision: heuristic\n");
        builder.append("- confidence: advisory\n");
        builder.append("- snapshot_freshness: ").append(freshnessStatus(result)).append('\n');
        builder.append("- must_verify_with_tests: true\n");
        builder.append("- do_not_skip_manual_review: true\n");
        builder.append("- do_not_treat_as_correctness_proof: true\n");
        builder.append("</graph-confidence>\n\n");
    }

    private String freshnessStatus(GraphImpactResult result) {
        if (!result.snapshotStale()) {
            return "fresh";
        }
        return result.staleAllowed() ? "stale_allowed" : "stale";
    }

    private void appendNodes(StringBuilder builder, String section, Iterable<GraphNode> nodes) {
        builder.append('<').append(section).append(">\n");
        for (GraphNode node : nodes) {
            builder.append("- ").append(node.nodeKind()).append(" ").append(safe(displayName(node)))
                    .append(" [file=").append(safe(node.relativePath()))
                    .append(", line=").append(node.startLine())
                    .append(", confidence=").append(node.confidence())
                    .append(", source=").append(safe(node.source()))
                    .append(", evidence=").append(safe(node.evidence()))
                    .append("]\n");
        }
        builder.append("</").append(section).append(">\n\n");
    }

    private void appendEdges(StringBuilder builder, String section, Iterable<GraphEdge> edges) {
        builder.append('<').append(section).append(">\n");
        for (GraphEdge edge : edges) {
            builder.append("- ").append(edge.edgeKind()).append(' ')
                    .append(safe(edge.sourceNodeKey())).append(" -> ")
                    .append(safe(edge.targetNodeKey()))
                    .append(" [file=").append(safe(edge.relativePath()))
                    .append(", confidence=").append(edge.confidence())
                    .append(", source=").append(safe(edge.source()))
                    .append(", evidence=").append(safe(edge.evidence()))
                    .append("]\n");
        }
        builder.append("</").append(section).append(">\n\n");
    }

    private void appendStrings(StringBuilder builder, String section, Iterable<String> values) {
        builder.append('<').append(section).append(">\n");
        for (String value : values) {
            builder.append("- ").append(safe(value)).append('\n');
        }
        builder.append("</").append(section).append(">\n\n");
    }

    private String displayName(GraphNode node) {
        return node.qualifiedName().length() > 0 ? node.qualifiedName() : node.name();
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ').replace('\r', ' ').replace('[', '(').replace(']', ')');
    }
}
