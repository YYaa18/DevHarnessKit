package com.devharnesskit.dhk.repository.workflow;

import com.devharnesskit.dhk.model.workflow.WorkflowMemoryBinding;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class WorkflowMemoryBindingRepository {
    public long insert(Connection connection, WorkflowMemoryBinding binding) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO workflow_memory_binding(run_key, memory_id, binding_type, phase_key, reason, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, binding.runKey());
            statement.setLong(2, binding.memoryId());
            statement.setString(3, binding.bindingType());
            statement.setString(4, binding.phaseKey());
            statement.setString(5, binding.reason());
            statement.setString(6, binding.createdAt());
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for workflow memory binding insert");
                }
                return resultSet.getLong(1);
            }
        }
    }

    public int countByRun(Connection connection, String runKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM workflow_memory_binding WHERE run_key = ?")) {
            statement.setString(1, runKey);
            return singleInt(statement);
        }
    }

    public int countByRunAndType(Connection connection, String runKey, String bindingType) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM workflow_memory_binding WHERE run_key = ? AND binding_type = ?")) {
            statement.setString(1, runKey);
            statement.setString(2, bindingType);
            return singleInt(statement);
        }
    }

    private int singleInt(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }
}
