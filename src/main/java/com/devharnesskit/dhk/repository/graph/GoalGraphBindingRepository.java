package com.devharnesskit.dhk.repository.graph;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class GoalGraphBindingRepository {
    public void upsert(Connection connection, String goalKey, long snapshotId, String bindingType,
                       String artifactPath, String impactHash, String createdAt) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE goal_graph_binding SET artifact_path = ?, impact_hash = ?, created_at = ? "
                        + "WHERE goal_key = ? AND snapshot_id = ? AND binding_type = ?")) {
            update.setString(1, value(artifactPath));
            update.setString(2, value(impactHash));
            update.setString(3, value(createdAt));
            update.setString(4, value(goalKey));
            update.setLong(5, snapshotId);
            update.setString(6, value(bindingType));
            if (update.executeUpdate() > 0) {
                return;
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO goal_graph_binding(goal_key, snapshot_id, binding_type, artifact_path, "
                        + "impact_hash, created_at) VALUES (?, ?, ?, ?, ?, ?)")) {
            insert.setString(1, value(goalKey));
            insert.setLong(2, snapshotId);
            insert.setString(3, value(bindingType));
            insert.setString(4, value(artifactPath));
            insert.setString(5, value(impactHash));
            insert.setString(6, value(createdAt));
            insert.executeUpdate();
        }
    }

    private String value(String raw) {
        return raw == null ? "" : raw;
    }
}
