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
        StringBuilder builder = new StringBuilder();
        GraphSnapshot snapshot = data.snapshot();
        builder.append("# GRAPH_CONTEXT\n\n");
        builder.append("<generated-at>").append(generatedAt.toString()).append("</generated-at>\n\n");
        builder.append("<boundary>\n");
        builder.append("- graph facts are snapshot-bound machine facts\n");
        builder.append("- graph facts are not confirmed long-term memory\n");
        builder.append("- regenerate graph exports after code changes\n");
        builder.append("</boundary>\n\n");
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
        appendFileHashes(builder, data);
        appendNodeKinds(builder, data);
        appendRiskNodes(builder, data);
        builder.append("<agent-instructions>\n");
        builder.append("- read IMPACT_MAP.md for task-specific impact before editing\n");
        builder.append("- do not treat graph facts as confirmed memory without human review\n");
        builder.append("- do not read memory.db directly; use graph commands and exports\n");
        builder.append("</agent-instructions>\n");
        return builder.toString();
    }

    public String renderSnapshotJson(GraphData data, Instant generatedAt) {
        GraphSnapshot snapshot = data.snapshot();
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
        builder.append("  \"file_hashes\": [\n");
        for (int i = 0; i < data.files().size(); i++) {
            GraphFileEntry file = data.files().get(i);
            builder.append("    {")
                    .append("\"path\": ").append(JsonOutput.quote(file.relativePath())).append(", ")
                    .append("\"language\": ").append(JsonOutput.quote(file.language())).append(", ")
                    .append("\"kind\": ").append(JsonOutput.quote(file.fileKind())).append(", ")
                    .append("\"indexed\": ").append(file.indexed()).append(", ")
                    .append("\"hash\": ").append(JsonOutput.quote(file.contentHash())).append(", ")
                    .append("\"skip_reason\": ").append(JsonOutput.quote(file.skipReason()))
                    .append("}");
            if (i + 1 < data.files().size()) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("  ]\n");
        builder.append("}\n");
        return builder.toString();
    }

    private void appendFileHashes(StringBuilder builder, GraphData data) {
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
            if (count >= 50) {
                builder.append("- truncated: file hash list limited to 50 entries\n");
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

    private void appendRiskNodes(StringBuilder builder, GraphData data) {
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
            if (count >= 50) {
                builder.append("- truncated: risk node list limited to 50 entries\n");
                break;
            }
        }
        builder.append("</risk-nodes>\n\n");
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
