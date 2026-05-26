package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.graph.GraphFileEntry;
import com.devharnesskit.dhk.model.graph.GraphEdge;
import com.devharnesskit.dhk.model.graph.GraphNode;
import com.devharnesskit.dhk.model.graph.GraphParseError;
import com.devharnesskit.dhk.model.graph.GraphScanReport;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class GraphIndexReportRenderer {
    public String render(GraphScanReport report, Instant generatedAt) {
        StringBuilder builder = new StringBuilder();
        builder.append("# GRAPH_INDEX_REPORT\n\n");
        builder.append("<generated-at>").append(generatedAt.toString()).append("</generated-at>\n\n");
        builder.append("<summary>\n");
        builder.append("- provider: ").append(report.config().provider()).append('\n');
        builder.append("- config_source: ").append(report.config().loadedFromFile() ? "file" : "default").append('\n');
        builder.append("- files_considered: ").append(report.filesConsidered()).append('\n');
        builder.append("- indexed_files: ").append(report.indexedFiles()).append('\n');
        builder.append("- skipped_files: ").append(report.skippedFiles()).append('\n');
        builder.append("- graph_nodes: ").append(report.parseResult().nodes().size()).append('\n');
        builder.append("- graph_edges: ").append(report.parseResult().edges().size()).append('\n');
        builder.append("- parse_errors: ").append(report.parseResult().errors().size()).append('\n');
        if (report.latestSnapshot() != null) {
            builder.append("- latest_snapshot_key: ").append(report.latestSnapshot().snapshotKey()).append('\n');
            builder.append("- latest_snapshot_status: ").append(report.latestSnapshot().status()).append('\n');
            builder.append("- latest_snapshot_nodes: ").append(report.latestSnapshot().nodeCount()).append('\n');
            builder.append("- latest_snapshot_edges: ").append(report.latestSnapshot().edgeCount()).append('\n');
        }
        builder.append("- max_file_bytes: ").append(report.config().maxFileBytes()).append('\n');
        builder.append("- max_indexed_files: ").append(report.config().maxIndexedFiles()).append('\n');
        builder.append("</summary>\n\n");

        builder.append("<languages>\n");
        for (Map.Entry<String, Integer> entry : report.languageCounts().entrySet()) {
            builder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }
        builder.append("</languages>\n\n");

        builder.append("<node-kinds>\n");
        for (Map.Entry<String, Integer> entry : report.parseResult().nodeKindCounts().entrySet()) {
            builder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }
        builder.append("</node-kinds>\n\n");

        builder.append("<indexed-files>\n");
        for (GraphFileEntry entry : report.entries()) {
            if (entry.indexed()) {
                builder.append("- ").append(entry.relativePath())
                        .append(" [").append(entry.language()).append(", ")
                        .append(entry.fileKind()).append(", ")
                        .append(entry.contentHash()).append("]\n");
            }
        }
        builder.append("</indexed-files>\n\n");

        builder.append("<skipped-files>\n");
        for (GraphFileEntry entry : report.entries()) {
            if (!entry.indexed()) {
                builder.append("- ").append(entry.relativePath())
                        .append(" [").append(entry.skipReason()).append("]\n");
            }
        }
        builder.append("</skipped-files>\n");
        appendNodes(builder, report.parseResult().nodes(), report.config().maxExportNodes());
        appendEdges(builder, report.parseResult().edges(), report.config().maxExportNodes());
        appendErrors(builder, report.parseResult().errors());
        return builder.toString();
    }

    private void appendNodes(StringBuilder builder, List<GraphNode> nodes, int limit) {
        builder.append("\n\n<graph-nodes>\n");
        int count = 0;
        for (GraphNode node : nodes) {
            if (count >= limit) {
                builder.append("- truncated: ").append(nodes.size() - count).append(" nodes not shown\n");
                break;
            }
            builder.append("- ").append(node.nodeKind()).append(" ").append(safe(displayName(node)))
                    .append(" [file=").append(safe(node.relativePath()))
                    .append(", line=").append(node.startLine())
                    .append(", confidence=").append(node.confidence())
                    .append(", source=").append(safe(node.source()))
                    .append(", evidence=").append(safe(node.evidence()))
                    .append("]\n");
            count++;
        }
        builder.append("</graph-nodes>");
    }

    private void appendEdges(StringBuilder builder, List<GraphEdge> edges, int limit) {
        builder.append("\n\n<graph-edges>\n");
        int count = 0;
        for (GraphEdge edge : edges) {
            if (count >= limit) {
                builder.append("- truncated: ").append(edges.size() - count).append(" edges not shown\n");
                break;
            }
            builder.append("- ").append(edge.edgeKind()).append(' ')
                    .append(safe(edge.sourceNodeKey())).append(" -> ")
                    .append(safe(edge.targetNodeKey()))
                    .append(" [file=").append(safe(edge.relativePath()))
                    .append(", confidence=").append(edge.confidence())
                    .append(", source=").append(safe(edge.source()))
                    .append(", evidence=").append(safe(edge.evidence()))
                    .append("]\n");
            count++;
        }
        builder.append("</graph-edges>");
    }

    private void appendErrors(StringBuilder builder, List<GraphParseError> errors) {
        builder.append("\n\n<parse-errors>\n");
        for (GraphParseError error : errors) {
            builder.append("- ").append(safe(error.relativePath()))
                    .append(" [parser=").append(safe(error.parser()))
                    .append(", message=").append(safe(error.message()))
                    .append("]\n");
        }
        builder.append("</parse-errors>\n");
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
