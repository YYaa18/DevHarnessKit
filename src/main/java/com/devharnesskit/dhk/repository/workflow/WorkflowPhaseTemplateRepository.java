package com.devharnesskit.dhk.repository.workflow;

import com.devharnesskit.dhk.model.workflow.WorkflowPhaseTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class WorkflowPhaseTemplateRepository {
    public boolean upsert(Connection connection, WorkflowPhaseTemplate phase) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE workflow_phase_template SET phase_name = ?, phase_order = ?, phase_type = ?, required = ?, "
                        + "instruction = ?, expected_output = ?, default_gate_policy = ?, updated_at = ? "
                        + "WHERE workflow_key = ? AND phase_key = ?")) {
            update.setString(1, phase.phaseName());
            update.setInt(2, phase.phaseOrder());
            update.setString(3, phase.phaseType());
            update.setInt(4, phase.required());
            update.setString(5, phase.instruction());
            update.setString(6, phase.expectedOutput());
            update.setString(7, phase.defaultGatePolicy());
            update.setString(8, phase.updatedAt());
            update.setString(9, phase.workflowKey());
            update.setString(10, phase.phaseKey());
            if (update.executeUpdate() > 0) {
                return false;
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO workflow_phase_template(workflow_key, phase_key, phase_name, phase_order, "
                        + "phase_type, required, instruction, expected_output, default_gate_policy, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            insert.setString(1, phase.workflowKey());
            insert.setString(2, phase.phaseKey());
            insert.setString(3, phase.phaseName());
            insert.setInt(4, phase.phaseOrder());
            insert.setString(5, phase.phaseType());
            insert.setInt(6, phase.required());
            insert.setString(7, phase.instruction());
            insert.setString(8, phase.expectedOutput());
            insert.setString(9, phase.defaultGatePolicy());
            insert.setString(10, phase.createdAt());
            insert.setString(11, phase.updatedAt());
            insert.executeUpdate();
            return true;
        }
    }

    public List<WorkflowPhaseTemplate> listByWorkflow(Connection connection, String workflowKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM workflow_phase_template WHERE workflow_key = ? ORDER BY phase_order, id")) {
            statement.setString(1, workflowKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<WorkflowPhaseTemplate> results = new ArrayList<WorkflowPhaseTemplate>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    private WorkflowPhaseTemplate map(ResultSet resultSet) throws SQLException {
        return new WorkflowPhaseTemplate(
                resultSet.getLong("id"),
                resultSet.getString("workflow_key"),
                resultSet.getString("phase_key"),
                resultSet.getString("phase_name"),
                resultSet.getInt("phase_order"),
                resultSet.getString("phase_type"),
                resultSet.getInt("required"),
                resultSet.getString("instruction"),
                resultSet.getString("expected_output"),
                resultSet.getString("default_gate_policy"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"));
    }
}
