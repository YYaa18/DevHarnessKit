package com.devharnesskit.dhk.repository.workflow;

import com.devharnesskit.dhk.model.workflow.WorkflowGateTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class WorkflowGateTemplateRepository {
    public boolean upsert(Connection connection, WorkflowGateTemplate gate) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE workflow_gate_template SET gate_name = ?, gate_type = ?, severity = ?, rule_text = ?, "
                        + "check_command = ?, expected_result = ?, updated_at = ? "
                        + "WHERE workflow_key = ? AND phase_key = ? AND gate_key = ?")) {
            update.setString(1, gate.gateName());
            update.setString(2, gate.gateType());
            update.setString(3, gate.severity());
            update.setString(4, gate.ruleText());
            update.setString(5, gate.checkCommand());
            update.setString(6, gate.expectedResult());
            update.setString(7, gate.updatedAt());
            update.setString(8, gate.workflowKey());
            update.setString(9, gate.phaseKey());
            update.setString(10, gate.gateKey());
            if (update.executeUpdate() > 0) {
                return false;
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO workflow_gate_template(workflow_key, phase_key, gate_key, gate_name, gate_type, "
                        + "severity, rule_text, check_command, expected_result, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            insert.setString(1, gate.workflowKey());
            insert.setString(2, gate.phaseKey());
            insert.setString(3, gate.gateKey());
            insert.setString(4, gate.gateName());
            insert.setString(5, gate.gateType());
            insert.setString(6, gate.severity());
            insert.setString(7, gate.ruleText());
            insert.setString(8, gate.checkCommand());
            insert.setString(9, gate.expectedResult());
            insert.setString(10, gate.createdAt());
            insert.setString(11, gate.updatedAt());
            insert.executeUpdate();
            return true;
        }
    }

    public List<WorkflowGateTemplate> listByWorkflow(Connection connection, String workflowKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT g.* FROM workflow_gate_template g "
                        + "JOIN workflow_phase_template p ON p.workflow_key = g.workflow_key AND p.phase_key = g.phase_key "
                        + "WHERE g.workflow_key = ? ORDER BY p.phase_order, g.id")) {
            statement.setString(1, workflowKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<WorkflowGateTemplate> results = new ArrayList<WorkflowGateTemplate>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    private WorkflowGateTemplate map(ResultSet resultSet) throws SQLException {
        return new WorkflowGateTemplate(
                resultSet.getLong("id"),
                resultSet.getString("workflow_key"),
                resultSet.getString("phase_key"),
                resultSet.getString("gate_key"),
                resultSet.getString("gate_name"),
                resultSet.getString("gate_type"),
                resultSet.getString("severity"),
                resultSet.getString("rule_text"),
                resultSet.getString("check_command"),
                resultSet.getString("expected_result"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"));
    }
}
