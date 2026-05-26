package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.graph.GraphData;
import com.devharnesskit.dhk.model.graph.GraphFileEntry;
import com.devharnesskit.dhk.model.graph.GraphNode;
import com.devharnesskit.dhk.model.graph.GraphSnapshot;
import com.devharnesskit.dhk.util.JsonOutput;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GraphExportRenderer {
    public String renderContext(GraphData data, Instant generatedAt) {
        return renderContext(data, generatedAt, 500);
    }

    public String renderContext(GraphData data, Instant generatedAt, int exportLimit) {
        StringBuilder builder = new StringBuilder();
        GraphSnapshot snapshot = data.snapshot();
        int limit = Math.max(1, exportLimit);
        builder.append("# GRAPH_CONTEXT\n\n");
        builder.append("<generated-at>").append(generatedAt.toString()).append("</generated-at>\n\n");
        builder.append("<boundary>\n");
        builder.append("- graph facts are snapshot-bound machine facts\n");
        builder.append("- graph facts are not confirmed long-term memory\n");
        builder.append("- regenerate graph exports after code changes\n");
        builder.append("</boundary>\n\n");
        builder.append("<graph-confidence>\n");
        builder.append("- provider: ").append(snapshot.provider()).append('\n');
        builder.append("- precision: heuristic\n");
        builder.append("- confidence: advisory\n");
        builder.append("- snapshot_bound: true\n");
        builder.append("- must_verify_with_tests: true\n");
        builder.append("- do_not_skip_manual_review: true\n");
        builder.append("- do_not_treat_as_correctness_proof: true\n");
        builder.append("</graph-confidence>\n\n");
        builder.append("<snapshot>\n");
        builder.append("- snapshot_id: ").append(snapshot.id()).append('\n');
        builder.append("- snapshot_key: ").append(snapshot.snapshotKey()).append('\n');
        builder.append("- provider: ").append(snapshot.provider()).append('\n');
        builder.append("- status: ").append(snapshot.status()).append('\n');
        builder.append("- workspace_fingerprint: ").append(snapshot.workspaceFingerprint()).append('\n');
        builder.append("- file_count: ").append(snapshot.fileCount()).append('\n');
        builder.append("- node_count: ").append(snapshot.nodeCount()).append('\n');
        builder.append("- edge_count: ").append(snapshot.edgeCount()).append('\n');
        builder.append("- summary: ").append(safe(snapshot.summary())).append('\n');
        builder.append("</snapshot>\n\n");
        appendLimits(builder, snapshot, limit);
        appendFileHashes(builder, data, limit);
        appendNodeKinds(builder, data);
        appendRiskNodes(builder, data, limit);
        appendTruncationReport(builder, data, limit);
        builder.append("<agent-instructions>\n");
        builder.append("- read IMPACT_MAP.md for task-specific impact before editing\n");
        builder.append("- do not treat graph facts as confirmed memory without human review\n");
        builder.append("- do not read memory.db directly; use graph commands and exports\n");
        builder.append("</agent-instructions>\n");
        return builder.toString();
    }

    public String renderSnapshotJson(GraphData data, Instant generatedAt) {
        return renderSnapshotJson(data, generatedAt, 500);
    }

    public String renderSnapshotJson(GraphData data, Instant generatedAt, int exportLimit) {
        GraphSnapshot snapshot = data.snapshot();
        int limit = Math.max(1, exportLimit);
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        appendJsonField(builder, "schema_version", "devharness-graph-snapshot/v1", true);
        appendJsonField(builder, "generated_at", generatedAt.toString(), true);
        appendJsonNumber(builder, "snapshot_id", snapshot.id(), true);
        appendJsonField(builder, "snapshot_key", snapshot.snapshotKey(), true);
        appendJsonField(builder, "provider", snapshot.provider(), true);
        appendJsonField(builder, "status", snapshot.status(), true);
        appendJsonField(builder, "workspace_fingerprint", snapshot.workspaceFingerprint(), true);
        appendJsonField(builder, "config_hash", snapshot.configHash(), true);
        appendJsonField(builder, "git_commit", summaryValue(snapshot.summary(), "git_commit"), true);
        appendJsonField(builder, "git_dirty", summaryValue(snapshot.summary(), "git_dirty"), true);
        appendJsonNumber(builder, "file_count", snapshot.fileCount(), true);
        appendJsonNumber(builder, "node_count", snapshot.nodeCount(), true);
        appendJsonNumber(builder, "edge_count", snapshot.edgeCount(), true);
        appendJsonNumber(builder, "max_file_bytes", snapshot.maxFileBytes(), true);
        appendJsonNumber(builder, "max_indexed_files", snapshot.maxIndexedFiles(), true);
        appendJsonNumber(builder, "max_export_nodes", limit, true);
        appendJsonBoolean(builder, "file_hashes_truncated", indexedFileCount(data) > limit, true);
        appendJsonBoolean(builder, "risk_nodes_truncated", riskNodeCount(data) > limit, true);
        builder.append("  \"file_hashes\": [\n");
        int exported = 0;
        boolean first = true;
        for (int i = 0; i < data.files().size(); i++) {
            GraphFileEntry file = data.files().get(i);
            if (file.indexed() && exported >= limit) {
                continue;
            }
            if (!first) {
                builder.append(",\n");
            }
            builder.append("    {")
                    .append("\"path\": ").append(JsonOutput.quote(file.relativePath())).append(", ")
                    .append("\"language\": ").append(JsonOutput.quote(file.language())).append(", ")
                    .append("\"kind\": ").append(JsonOutput.quote(file.fileKind())).append(", ")
                    .append("\"indexed\": ").append(file.indexed()).append(", ")
                    .append("\"hash\": ").append(JsonOutput.quote(file.contentHash())).append(", ")
                    .append("\"skip_reason\": ").append(JsonOutput.quote(file.skipReason()))
                    .append("}");
            first = false;
            if (file.indexed()) {
                exported++;
            }
        }
        if (!first) {
            builder.append('\n');
        }
        builder.append("  ]\n");
        builder.append("}\n");
        return builder.toString();
    }

    private void appendLimits(StringBuilder builder, GraphSnapshot snapshot, int limit) {
        builder.append("<limits>\n");
        builder.append("- max_file_bytes: ").append(snapshot.maxFileBytes()).append('\n');
        builder.append("- max_indexed_files: ").append(snapshot.maxIndexedFiles()).append('\n');
        builder.append("- max_export_nodes: ").append(limit).append('\n');
        builder.append("- skipped_files: ").append(snapshot.skippedFileCount()).append('\n');
        builder.append("</limits>\n\n");
    }

    private void appendFileHashes(StringBuilder builder, GraphData data, int limit) {
        builder.append("<file-hashes>\n");
        int count = 0;
        for (GraphFileEntry file : data.files()) {
            if (!file.indexed()) {
                continue;
            }
            builder.append("- ").append(file.relativePath())
                    .append(" [").append(file.language())
                    .append(", ").append(file.fileKind())
                    .append(", ").append(file.contentHash())
                    .append("]\n");
            count++;
            if (count >= limit) {
                int remaining = indexedFileCount(data) - count;
                if (remaining > 0) {
                    builder.append("- truncated: ").append(remaining)
                            .append(" indexed file hashes not shown due to max_export_nodes\n");
                }
                break;
            }
        }
        builder.append("</file-hashes>\n\n");
    }

    private void appendNodeKinds(StringBuilder builder, GraphData data) {
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        for (GraphNode node : data.nodes()) {
            Integer current = counts.get(node.nodeKind());
            counts.put(node.nodeKind(), current == null ? 1 : current + 1);
        }
        builder.append("<node-kinds>\n");
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            builder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }
        builder.append("</node-kinds>\n\n");
    }

    private void appendRiskNodes(StringBuilder builder, GraphData data, int limit) {
        builder.append("<risk-nodes>\n");
        int count = 0;
        for (GraphNode node : data.nodes()) {
            if (!isRiskNode(node)) {
                continue;
            }
            builder.append("- ").append(node.nodeKind()).append(' ')
                    .append(safe(displayName(node)))
                    .append(" [file=").append(safe(node.relativePath()))
                    .append(", confidence=").append(node.confidence())
                    .append(", source=").append(safe(node.source()))
                    .append(", evidence=").append(safe(node.evidence()))
                    .append("]\n");
            count++;
            if (count >= limit) {
                int remaining = riskNodeCount(data) - count;
                if (remaining > 0) {
                    builder.append("- truncated: ").append(remaining)
                            .append(" risk nodes not shown due to max_export_nodes\n");
                }
                break;
            }
        }
        builder.append("</risk-nodes>\n\n");
    }

    private void appendTruncationReport(StringBuilder builder, GraphData data, int limit) {
        int indexedFiles = indexedFileCount(data);
        int riskNodes = riskNodeCount(data);
        builder.append("<truncation-report>\n");
        builder.append("- indexed_files_total: ").append(indexedFiles).append('\n');
        builder.append("- indexed_file_hashes_exported: ").append(Math.min(indexedFiles, limit)).append('\n');
        builder.append("- indexed_file_hashes_truncated: ").append(indexedFiles > limit).append('\n');
        builder.append("- risk_nodes_total: ").append(riskNodes).append('\n');
        builder.append("- risk_nodes_exported: ").append(Math.min(riskNodes, limit)).append('\n');
        builder.append("- risk_nodes_truncated: ").append(riskNodes > limit).append('\n');
        builder.append("</truncation-report>\n\n");
    }

    private int indexedFileCount(GraphData data) {
        int count = 0;
        for (GraphFileEntry file : data.files()) {
            if (file.indexed()) {
                count++;
            }
        }
        return count;
    }

    private int riskNodeCount(GraphData data) {
        int count = 0;
        for (GraphNode node : data.nodes()) {
            if (isRiskNode(node)) {
                count++;
            }
        }
        return count;
    }

    private boolean isRiskNode(GraphNode node) {
        return "route".equals(node.nodeKind()) || "sql_statement".equals(node.nodeKind())
                || "db_table".equals(node.nodeKind()) || "db_column".equals(node.nodeKind())
                || "xml_mapper".equals(node.nodeKind());
    }

    private void appendJsonField(StringBuilder builder, String name, String value, boolean comma) {
        builder.append("  ").append(JsonOutput.quote(name)).append(": ")
                .append(JsonOutput.quote(value));
        if (comma) {
            builder.append(',');
        }
        builder.append('\n');
    }

    private void appendJsonNumber(StringBuilder builder, String name, long value, boolean comma) {
        builder.append("  ").append(JsonOutput.quote(name)).append(": ").append(value);
        if (comma) {
            builder.append(',');
        }
        builder.append('\n');
    }

    private void appendJsonBoolean(StringBuilder builder, String name, boolean value, boolean comma) {
        builder.append("  ").append(JsonOutput.quote(name)).append(": ").append(value);
        if (comma) {
            builder.append(',');
        }
        builder.append('\n');
    }

    private String summaryValue(String summary, String key) {
        if (summary == null) {
            return "";
        }
        String prefix = key + "=";
        for (String part : summary.split(";")) {
            if (part.startsWith(prefix)) {
                return part.substring(prefix.length());
            }
        }
        return "";
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
