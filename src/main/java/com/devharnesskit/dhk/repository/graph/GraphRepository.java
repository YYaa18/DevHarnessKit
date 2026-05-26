package com.devharnesskit.dhk.repository.graph;

import com.devharnesskit.dhk.model.graph.GraphConfig;
import com.devharnesskit.dhk.model.graph.GraphData;
import com.devharnesskit.dhk.model.graph.GraphEdge;
import com.devharnesskit.dhk.model.graph.GraphFileEntry;
import com.devharnesskit.dhk.model.graph.GraphNode;
import com.devharnesskit.dhk.model.graph.GraphPruneResult;
import com.devharnesskit.dhk.model.graph.GraphScanReport;
import com.devharnesskit.dhk.model.graph.GraphSnapshot;
import com.devharnesskit.dhk.util.JsonUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    public GraphPruneResult pruneCompletedSnapshots(Connection connection, String projectKey, int keep)
            throws SQLException {
        List<SnapshotRef> snapshots = completedSnapshotsBeyondKeep(connection, projectKey, keep);
        int deletedBindings = 0;
        int deletedCacheRows = 0;
        int deletedEdges = 0;
        int deletedNodes = 0;
        int deletedFiles = 0;
        int deletedSnapshots = 0;
        for (SnapshotRef snapshot : snapshots) {
            long id = snapshot.id;
            deletedBindings += deleteBySnapshotId(connection,
                    "DELETE FROM goal_graph_binding WHERE snapshot_id = ?", id);
            deletedCacheRows += deleteBySnapshotId(connection,
                    "DELETE FROM code_graph_query_cache WHERE snapshot_id = ?", id);
            deletedEdges += deleteBySnapshotId(connection,
                    "DELETE FROM code_graph_edge WHERE snapshot_id = ?", id);
            deletedNodes += deleteBySnapshotId(connection,
                    "DELETE FROM code_graph_node WHERE snapshot_id = ?", id);
            deletedFiles += deleteBySnapshotId(connection,
                    "DELETE FROM code_graph_file WHERE snapshot_id = ?", id);
            deletedSnapshots += deleteBySnapshotId(connection,
                    "DELETE FROM code_graph_snapshot WHERE id = ?", id);
        }
        return new GraphPruneResult(keep, deletedSnapshots, completedSnapshotCount(connection, projectKey),
                deletedFiles, deletedNodes, deletedEdges, deletedCacheRows, deletedBindings,
                false, snapshotKeys(snapshots));
    }

    public GraphPruneResult previewPruneCompletedSnapshots(Connection connection, String projectKey, int keep)
            throws SQLException {
        List<SnapshotRef> snapshots = completedSnapshotsBeyondKeep(connection, projectKey, keep);
        int files = 0;
        int nodes = 0;
        int edges = 0;
        int cacheRows = 0;
        int bindings = 0;
        for (SnapshotRef snapshot : snapshots) {
            long id = snapshot.id;
            bindings += countBySnapshotId(connection, "goal_graph_binding", id);
            cacheRows += countBySnapshotId(connection, "code_graph_query_cache", id);
            edges += countBySnapshotId(connection, "code_graph_edge", id);
            nodes += countBySnapshotId(connection, "code_graph_node", id);
            files += countBySnapshotId(connection, "code_graph_file", id);
        }
        int remaining = completedSnapshotCount(connection, projectKey) - snapshots.size();
        return new GraphPruneResult(keep, snapshots.size(), remaining, files, nodes, edges,
                cacheRows, bindings, true, snapshotKeys(snapshots));
    }

    public GraphData loadGraphData(Connection connection, GraphSnapshot snapshot) throws SQLException {
        return new GraphData(snapshot, listFiles(connection, snapshot.id()), listNodes(connection, snapshot.id()),
                listEdges(connection, snapshot.id()));
    }

    private List<SnapshotRef> completedSnapshotsBeyondKeep(Connection connection, String projectKey, int keep)
            throws SQLException {
        List<SnapshotRef> snapshots = new ArrayList<SnapshotRef>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, snapshot_key FROM code_graph_snapshot WHERE project_key = ? AND status = 'completed' "
                        + "ORDER BY id DESC")) {
            statement.setString(1, projectKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                int index = 0;
                while (resultSet.next()) {
                    long id = resultSet.getLong("id");
                    if (index >= keep) {
                        snapshots.add(new SnapshotRef(id, resultSet.getString("snapshot_key")));
                    }
                    index++;
                }
            }
        }
        return snapshots;
    }

    private int completedSnapshotCount(Connection connection, String projectKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM code_graph_snapshot WHERE project_key = ? AND status = 'completed'")) {
            statement.setString(1, projectKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private int deleteBySnapshotId(Connection connection, String sql, long snapshotId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, snapshotId);
            return statement.executeUpdate();
        }
    }

    private int countBySnapshotId(Connection connection, String table, long snapshotId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM " + table + " WHERE snapshot_id = ?")) {
            statement.setLong(1, snapshotId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private String[] snapshotKeys(List<SnapshotRef> snapshots) {
        List<String> keys = new ArrayList<String>();
        for (SnapshotRef snapshot : snapshots) {
            keys.add(snapshot.snapshotKey);
        }
        return keys.toArray(new String[keys.size()]);
    }

    private static final class SnapshotRef {
        private final long id;
        private final String snapshotKey;

        private SnapshotRef(long id, String snapshotKey) {
            this.id = id;
            this.snapshotKey = snapshotKey == null ? "" : snapshotKey;
        }
    }

    private List<GraphFileEntry> listFiles(Connection connection, long snapshotId) throws SQLException {
        List<GraphFileEntry> files = new ArrayList<GraphFileEntry>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT relative_path, file_kind, language, content_hash, size_bytes, indexed, skip_reason "
                        + "FROM code_graph_file WHERE snapshot_id = ? ORDER BY relative_path")) {
            statement.setLong(1, snapshotId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    files.add(new GraphFileEntry(
                            resultSet.getString("relative_path"),
                            resultSet.getString("language"),
                            resultSet.getString("file_kind"),
                            resultSet.getString("content_hash"),
                            resultSet.getLong("size_bytes"),
                            resultSet.getInt("indexed") != 0,
                            resultSet.getString("skip_reason")
                    ));
                }
            }
        }
        return files;
    }

    private List<GraphNode> listNodes(Connection connection, long snapshotId) throws SQLException {
        List<GraphNode> nodes = new ArrayList<GraphNode>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT n.node_key, n.node_kind, n.name, n.qualified_name, n.start_line, n.end_line, "
                        + "n.signature, n.metadata, f.relative_path, f.language "
                        + "FROM code_graph_node n JOIN code_graph_file f ON n.file_id = f.id "
                        + "WHERE n.snapshot_id = ? ORDER BY n.id")) {
            statement.setLong(1, snapshotId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Map<String, String> metadata = parseMetadata(resultSet.getString("metadata"));
                    nodes.add(new GraphNode(
                            resultSet.getString("node_key"),
                            resultSet.getString("node_kind"),
                            resultSet.getString("name"),
                            resultSet.getString("qualified_name"),
                            resultSet.getString("relative_path"),
                            resultSet.getInt("start_line"),
                            resultSet.getInt("end_line"),
                            value(metadata.get("language"), resultSet.getString("language")),
                            value(metadata.get("visibility"), ""),
                            resultSet.getString("signature"),
                            intValue(metadata.get("confidence"), 70),
                            value(metadata.get("source"), "lite"),
                            value(metadata.get("evidence"), "")
                    ));
                }
            }
        }
        return nodes;
    }

    private List<GraphEdge> listEdges(Connection connection, long snapshotId) throws SQLException {
        List<GraphEdge> edges = new ArrayList<GraphEdge>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT e.edge_kind, e.source_node_key, e.target_node_key, e.confidence, e.metadata, "
                        + "COALESCE(f.relative_path, '') AS relative_path "
                        + "FROM code_graph_edge e LEFT JOIN code_graph_file f ON e.file_id = f.id "
                        + "WHERE e.snapshot_id = ? ORDER BY e.id")) {
            statement.setLong(1, snapshotId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Map<String, String> metadata = parseMetadata(resultSet.getString("metadata"));
                    edges.add(new GraphEdge(
                            resultSet.getString("edge_kind"),
                            resultSet.getString("source_node_key"),
                            resultSet.getString("target_node_key"),
                            resultSet.getString("relative_path"),
                            resultSet.getInt("confidence"),
                            value(metadata.get("source"), "lite"),
                            value(metadata.get("evidence"), "")
                    ));
                }
            }
        }
        return edges;
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
        return "sensitive_filename".equals(entry.skipReason()) || "protected_file".equals(entry.skipReason());
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

    private Map<String, String> parseMetadata(String metadata) {
        try {
            return JsonUtil.parseObject(metadata == null ? "" : metadata);
        } catch (Exception ex) {
            return new LinkedHashMap<String, String>();
        }
    }

    private String value(String value, String fallback) {
        return value == null || value.length() == 0 ? fallback : value;
    }

    private int intValue(String value, int fallback) {
        try {
            return value == null || value.length() == 0 ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
