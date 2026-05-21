package com.devharnesskit.dhk.repository.spec;

import com.devharnesskit.dhk.model.spec.WorkflowSpecBinding;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class WorkflowSpecBindingRepository {
    public long insert(Connection connection, WorkflowSpecBinding binding) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO workflow_spec_binding(run_key, change_key, binding_type, created_at) "
                        + "VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, binding.runKey());
            statement.setString(2, binding.changeKey());
            statement.setString(3, binding.bindingType());
            statement.setString(4, binding.createdAt());
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for workflow spec binding insert");
                }
                return resultSet.getLong(1);
            }
        }
    }

    public List<WorkflowSpecBinding> listByChange(Connection connection, String changeKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM workflow_spec_binding WHERE change_key = ? ORDER BY id")) {
            statement.setString(1, changeKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<WorkflowSpecBinding> results = new ArrayList<WorkflowSpecBinding>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    public int countByRun(Connection connection, String runKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM workflow_spec_binding WHERE run_key = ?")) {
            statement.setString(1, runKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private WorkflowSpecBinding map(ResultSet resultSet) throws SQLException {
        return new WorkflowSpecBinding(
                resultSet.getLong("id"),
                resultSet.getString("run_key"),
                resultSet.getString("change_key"),
                resultSet.getString("binding_type"),
                resultSet.getString("created_at"));
    }
}
