package com.devharnesskit.dhk.repository.graph;

import com.devharnesskit.dhk.model.graph.GraphConfig;
import com.devharnesskit.dhk.model.graph.GraphEdge;
import com.devharnesskit.dhk.model.graph.GraphFileEntry;
import com.devharnesskit.dhk.model.graph.GraphNode;
import com.devharnesskit.dhk.model.graph.GraphScanReport;
import com.devharnesskit.dhk.model.graph.GraphSnapshot;
import com.devharnesskit.dhk.util.JsonUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GraphRepository {
    public GraphSnapshot saveCompletedSnapshot(Connection connection, String projectKey, String snapshotKey,
                                               String configHash, String workspaceFingerprint,
                                               String summary, String createdAt, String completedAt,
                                               GraphScanReport report) throws SQLException {
        long snapshotId = insertSnapshot(connection, projectKey, snapshotKey, configHash, workspaceFingerprint,
                summary, createdAt, completedAt, report);
        Map<String, Long> fileIds = insertFiles(connection, snapshotId, report, createdAt);
        insertNodes(connection, snapshotId, fileIds, report, createdAt);
        insertEdges(connection, snapshotId, fileIds, report, createdAt);
        return findById(connection, snapshotId);
    }

    public GraphSnapshot latestCompletedSnapshot(Connection connection, String projectKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, project_key, snapshot_key, provider, config_hash, workspace_fingerprint, status, "
                        + "file_count, node_count, edge_count, skipped_file_count, max_file_bytes, "
                        + "max_indexed_files, summary, created_at, completed_at "
                        + "FROM code_graph_snapshot WHERE project_key = ? AND status = 'completed' "
                        + "ORDER BY id DESC LIMIT 1")) {
            statement.setString(1, projectKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapSnapshot(resultSet) : null;
            }
        }
    }

    public int snapshotCount(Connection connection, String projectKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM code_graph_snapshot WHERE project_key = ?")) {
            statement.setString(1, projectKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private long insertSnapshot(Connection connection, String projectKey, String snapshotKey,
                                String configHash, String workspaceFingerprint, String summary,
                                String createdAt, String completedAt, GraphScanReport report) throws SQLException {
        GraphConfig config = report.config();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO code_graph_snapshot(project_key, snapshot_key, provider, config_hash, "
                        + "workspace_fingerprint, status, file_count, node_count, edge_count, skipped_file_count, "
                        + "max_file_bytes, max_indexed_files, summary, created_at, completed_at) "
                        + "VALUES (?, ?, ?, ?, ?, 'completed', ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, projectKey);
            statement.setString(2, snapshotKey);
            statement.setString(3, config.provider());
            statement.setString(4, configHash);
            statement.setString(5, workspaceFingerprint);
            statement.setInt(6, report.filesConsidered());
            statement.setInt(7, report.parseResult().nodes().size());
            statement.setInt(8, report.parseResult().edges().size());
            statement.setInt(9, report.skippedFiles());
            statement.setInt(10, config.maxFileBytes());
            statement.setInt(11, config.maxIndexedFiles());
            statement.setString(12, summary);
            statement.setString(13, createdAt);
            statement.setString(14, completedAt);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to create graph snapshot");
    }

    private Map<String, Long> insertFiles(Connection connection, long snapshotId, GraphScanReport report,
                                          String createdAt) throws SQLException {
        Map<String, Long> ids = new LinkedHashMap<String, Long>();
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO code_graph_file(snapshot_id, relative_path, file_kind, language, content_hash, "
                        + "size_bytes, indexed, skip_reason, protected_file, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            for (GraphFileEntry entry : report.entries()) {
                statement.setLong(1, snapshotId);
                statement.setString(2, entry.relativePath());
                statement.setString(3, entry.fileKind());
                statement.setString(4, entry.language());
                statement.setString(5, entry.contentHash());
                statement.setLong(6, entry.sizeBytes());
                statement.setInt(7, entry.indexed() ? 1 : 0);
                statement.setString(8, entry.skipReason());
                statement.setInt(9, isProtected(entry) ? 1 : 0);
                statement.setString(10, createdAt);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        ids.put(entry.relativePath(), keys.getLong(1));
                    }
                }
            }
        }
        return ids;
    }

    private void insertNodes(Connection connection, long snapshotId, Map<String, Long> fileIds,
                             GraphScanReport report, String createdAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR IGNORE INTO code_graph_node(snapshot_id, file_id, node_key, node_kind, name, "
                        + "qualified_name, start_line, end_line, signature, metadata) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (GraphNode node : report.parseResult().nodes()) {
                Long fileId = fileIds.get(node.relativePath());
                if (fileId == null) {
                    continue;
                }
                statement.setLong(1, snapshotId);
                statement.setLong(2, fileId.longValue());
                statement.setString(3, node.nodeKey());
                statement.setString(4, node.nodeKind());
                statement.setString(5, node.name());
                statement.setString(6, node.qualifiedName());
                statement.setInt(7, node.startLine());
                statement.setInt(8, node.endLine());
                statement.setString(9, node.signature());
                statement.setString(10, nodeMetadata(node, createdAt));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertEdges(Connection connection, long snapshotId, Map<String, Long> fileIds,
                             GraphScanReport report, String createdAt) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR IGNORE INTO code_graph_edge(snapshot_id, source_node_key, target_node_key, "
                        + "edge_kind, file_id, confidence, metadata) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            for (GraphEdge edge : report.parseResult().edges()) {
                Long fileId = fileIds.get(edge.relativePath());
                statement.setLong(1, snapshotId);
                statement.setString(2, edge.sourceNodeKey());
                statement.setString(3, edge.targetNodeKey());
                statement.setString(4, edge.edgeKind());
                if (fileId == null) {
                    statement.setNull(5, java.sql.Types.INTEGER);
                } else {
                    statement.setLong(5, fileId.longValue());
                }
                statement.setInt(6, edge.confidence());
                statement.setString(7, edgeMetadata(edge, createdAt));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private GraphSnapshot findById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, project_key, snapshot_key, provider, config_hash, workspace_fingerprint, status, "
                        + "file_count, node_count, edge_count, skipped_file_count, max_file_bytes, "
                        + "max_indexed_files, summary, created_at, completed_at "
                        + "FROM code_graph_snapshot WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapSnapshot(resultSet);
                }
            }
        }
        throw new SQLException("Graph snapshot not found after insert: " + id);
    }

    private GraphSnapshot mapSnapshot(ResultSet resultSet) throws SQLException {
        return new GraphSnapshot(
                resultSet.getLong("id"),
                resultSet.getString("project_key"),
                resultSet.getString("snapshot_key"),
                resultSet.getString("provider"),
                resultSet.getString("config_hash"),
                resultSet.getString("workspace_fingerprint"),
                resultSet.getString("status"),
                resultSet.getInt("file_count"),
                resultSet.getInt("node_count"),
                resultSet.getInt("edge_count"),
                resultSet.getInt("skipped_file_count"),
                resultSet.getInt("max_file_bytes"),
                resultSet.getInt("max_indexed_files"),
                resultSet.getString("summary"),
                resultSet.getString("created_at"),
                resultSet.getString("completed_at")
        );
    }

    private boolean isProtected(GraphFileEntry entry) {
        return "sensitive_filename".equals(entry.skipReason());
    }

    private String nodeMetadata(GraphNode node, String createdAt) {
        Map<String, String> metadata = new LinkedHashMap<String, String>();
        metadata.put("language", node.language());
        metadata.put("visibility", node.visibility());
        metadata.put("confidence", String.valueOf(node.confidence()));
        metadata.put("source", node.source());
        metadata.put("evidence", node.evidence());
        metadata.put("created_at", createdAt);
        return JsonUtil.toObject(metadata);
    }

    private String edgeMetadata(GraphEdge edge, String createdAt) {
        Map<String, String> metadata = new LinkedHashMap<String, String>();
        metadata.put("source", edge.source());
        metadata.put("evidence", edge.evidence());
        metadata.put("created_at", createdAt);
        return JsonUtil.toObject(metadata);
    }
}
