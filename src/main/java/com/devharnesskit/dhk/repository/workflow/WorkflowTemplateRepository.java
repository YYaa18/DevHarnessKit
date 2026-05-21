package com.devharnesskit.dhk.repository.workflow;

import com.devharnesskit.dhk.model.workflow.WorkflowTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class WorkflowTemplateRepository {
    public boolean upsert(Connection connection, WorkflowTemplate template) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE workflow_template SET name = ?, description = ?, category = ?, target_mode = ?, "
                        + "status = ?, version = ?, source_kind = ?, source_ref = ?, updated_at = ? "
                        + "WHERE workflow_key = ?")) {
            update.setString(1, template.name());
            update.setString(2, template.description());
            update.setString(3, template.category());
            update.setString(4, template.targetMode());
            update.setString(5, template.status());
            update.setInt(6, template.version());
            update.setString(7, template.sourceKind());
            update.setString(8, template.sourceRef());
            update.setString(9, template.updatedAt());
            update.setString(10, template.workflowKey());
            if (update.executeUpdate() > 0) {
                return false;
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO workflow_template(workflow_key, name, description, category, target_mode, status, "
                        + "version, source_kind, source_ref, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            insert.setString(1, template.workflowKey());
            insert.setString(2, template.name());
            insert.setString(3, template.description());
            insert.setString(4, template.category());
            insert.setString(5, template.targetMode());
            insert.setString(6, template.status());
            insert.setInt(7, template.version());
            insert.setString(8, template.sourceKind());
            insert.setString(9, template.sourceRef());
            insert.setString(10, template.createdAt());
            insert.setString(11, template.updatedAt());
            insert.executeUpdate();
            return true;
        }
    }

    public WorkflowTemplate findByKey(Connection connection, String workflowKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM workflow_template WHERE workflow_key = ?")) {
            statement.setString(1, workflowKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public List<WorkflowTemplate> listActive(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM workflow_template WHERE status = 'active' ORDER BY workflow_key")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                List<WorkflowTemplate> results = new ArrayList<WorkflowTemplate>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    private WorkflowTemplate map(ResultSet resultSet) throws SQLException {
        return new WorkflowTemplate(
                resultSet.getString("workflow_key"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getString("category"),
                resultSet.getString("target_mode"),
                resultSet.getString("status"),
                resultSet.getInt("version"),
                resultSet.getString("source_kind"),
                resultSet.getString("source_ref"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"));
    }
}
