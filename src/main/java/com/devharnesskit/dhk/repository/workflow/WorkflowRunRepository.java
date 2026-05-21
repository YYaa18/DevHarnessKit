package com.devharnesskit.dhk.repository.workflow;

import com.devharnesskit.dhk.model.workflow.WorkflowRun;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class WorkflowRunRepository {
    public void insert(Connection connection, WorkflowRun run) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO workflow_run(run_key, project_key, workflow_key, task_name, task_summary, "
                        + "module_name, mode, status, current_phase_key, context_export_path, recovery_export_path, "
                        + "checkpoint_id, started_at, completed_at, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            bind(statement, run);
            statement.executeUpdate();
        }
    }

    public WorkflowRun findByKey(Connection connection, String runKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM workflow_run WHERE run_key = ?")) {
            statement.setString(1, runKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public void updateCurrentPhase(Connection connection, String runKey, String status,
                                   String currentPhaseKey, String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE workflow_run SET status = ?, current_phase_key = ?, updated_at = ? WHERE run_key = ?")) {
            statement.setString(1, status);
            statement.setString(2, currentPhaseKey);
            statement.setString(3, now);
            statement.setString(4, runKey);
            statement.executeUpdate();
        }
    }

    public void updateStatus(Connection connection, String runKey, String status, String completedAt,
                             String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE workflow_run SET status = ?, completed_at = ?, updated_at = ? WHERE run_key = ?")) {
            statement.setString(1, status);
            if (completedAt == null || completedAt.length() == 0) {
                statement.setNull(2, java.sql.Types.VARCHAR);
            } else {
                statement.setString(2, completedAt);
            }
            statement.setString(3, now);
            statement.setString(4, runKey);
            statement.executeUpdate();
        }
    }

    public void updateContextExportPath(Connection connection, String runKey, String path, String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE workflow_run SET context_export_path = ?, updated_at = ? WHERE run_key = ?")) {
            statement.setString(1, path);
            statement.setString(2, now);
            statement.setString(3, runKey);
            statement.executeUpdate();
        }
    }

    private void bind(PreparedStatement statement, WorkflowRun run) throws SQLException {
        statement.setString(1, run.runKey());
        statement.setString(2, run.projectKey());
        statement.setString(3, run.workflowKey());
        statement.setString(4, run.taskName());
        statement.setString(5, run.taskSummary());
        statement.setString(6, run.moduleName());
        statement.setString(7, run.mode());
        statement.setString(8, run.status());
        statement.setString(9, run.currentPhaseKey());
        statement.setString(10, run.contextExportPath());
        statement.setString(11, run.recoveryExportPath());
        if (run.checkpointId() == null) {
            statement.setNull(12, java.sql.Types.INTEGER);
        } else {
            statement.setLong(12, run.checkpointId().longValue());
        }
        nullable(statement, 13, run.startedAt());
        nullable(statement, 14, run.completedAt());
        statement.setString(15, run.createdAt());
        statement.setString(16, run.updatedAt());
    }

    private void nullable(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.length() == 0) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private WorkflowRun map(ResultSet resultSet) throws SQLException {
        long checkpointId = resultSet.getLong("checkpoint_id");
        Long checkpoint = resultSet.wasNull() ? null : Long.valueOf(checkpointId);
        return new WorkflowRun(
                resultSet.getString("run_key"),
                resultSet.getString("project_key"),
                resultSet.getString("workflow_key"),
                resultSet.getString("task_name"),
                resultSet.getString("task_summary"),
                resultSet.getString("module_name"),
                resultSet.getString("mode"),
                resultSet.getString("status"),
                resultSet.getString("current_phase_key"),
                resultSet.getString("context_export_path"),
                resultSet.getString("recovery_export_path"),
                checkpoint,
                resultSet.getString("started_at"),
                resultSet.getString("completed_at"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"));
    }
}
