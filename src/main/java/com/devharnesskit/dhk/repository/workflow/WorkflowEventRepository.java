package com.devharnesskit.dhk.repository.workflow;

import com.devharnesskit.dhk.model.workflow.WorkflowEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class WorkflowEventRepository {
    public void insert(Connection connection, WorkflowEvent event) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO workflow_event(project_key, run_key, event_type, phase_key, gate_key, level, "
                        + "message, data, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, event.projectKey());
            statement.setString(2, event.runKey());
            statement.setString(3, event.eventType());
            statement.setString(4, event.phaseKey());
            statement.setString(5, event.gateKey());
            statement.setString(6, event.level());
            statement.setString(7, event.message());
            statement.setString(8, event.data());
            statement.setString(9, event.createdAt());
            statement.executeUpdate();
        }
    }
}
