package com.devharnesskit.dhk.repository.workflow;

import com.devharnesskit.dhk.model.workflow.WorkflowCheckpointBinding;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class WorkflowCheckpointBindingRepository {
    public long insert(Connection connection, WorkflowCheckpointBinding binding) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO workflow_checkpoint_binding(run_key, checkpoint_id, binding_type, created_at) "
                        + "VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, binding.runKey());
            statement.setLong(2, binding.checkpointId());
            statement.setString(3, binding.bindingType());
            statement.setString(4, binding.createdAt());
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for workflow checkpoint binding insert");
                }
                return resultSet.getLong(1);
            }
        }
    }

    public int countByRun(Connection connection, String runKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM workflow_checkpoint_binding WHERE run_key = ?")) {
            statement.setString(1, runKey);
            return singleInt(statement);
        }
    }

    private int singleInt(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }
}
