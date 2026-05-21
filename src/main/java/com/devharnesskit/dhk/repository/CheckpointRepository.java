package com.devharnesskit.dhk.repository;

import com.devharnesskit.dhk.model.Checkpoint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class CheckpointRepository {
    public long insert(Connection connection, Checkpoint checkpoint) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO checkpoint(project_key, task_name, module_name, summary, changed_files, "
                        + "pending_items, verify_status, next_read_files, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, checkpoint.projectKey());
            statement.setString(2, checkpoint.taskName());
            statement.setString(3, checkpoint.moduleName());
            statement.setString(4, checkpoint.summary());
            statement.setString(5, checkpoint.changedFiles());
            statement.setString(6, checkpoint.pendingItems());
            statement.setString(7, checkpoint.verifyStatus());
            statement.setString(8, checkpoint.nextReadFiles());
            statement.setString(9, checkpoint.createdAt());
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for checkpoint insert");
                }
                return resultSet.getLong(1);
            }
        }
    }

    public Checkpoint latest(Connection connection, String projectKey, String module) throws SQLException {
        String sql = "SELECT * FROM checkpoint WHERE project_key = ?";
        if (module != null && module.length() > 0) {
            sql += " AND module_name = ?";
        }
        sql += " ORDER BY created_at DESC, id DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, projectKey);
            if (module != null && module.length() > 0) {
                statement.setString(2, module);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return map(resultSet);
            }
        }
    }

    public int countAll(Connection connection, String projectKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM checkpoint WHERE project_key = ?")) {
            statement.setString(1, projectKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return 0;
                }
                return resultSet.getInt(1);
            }
        }
    }

    private Checkpoint map(ResultSet resultSet) throws SQLException {
        return new Checkpoint(
                resultSet.getLong("id"),
                resultSet.getString("project_key"),
                resultSet.getString("task_name"),
                resultSet.getString("module_name"),
                resultSet.getString("summary"),
                resultSet.getString("changed_files"),
                resultSet.getString("pending_items"),
                resultSet.getString("verify_status"),
                resultSet.getString("next_read_files"),
                resultSet.getString("created_at")
        );
    }
}
