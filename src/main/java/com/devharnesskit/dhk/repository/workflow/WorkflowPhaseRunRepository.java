package com.devharnesskit.dhk.repository.workflow;

import com.devharnesskit.dhk.model.workflow.WorkflowPhaseRun;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class WorkflowPhaseRunRepository {
    public void insert(Connection connection, WorkflowPhaseRun phase) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO workflow_phase_run(run_key, phase_key, phase_name, phase_order, status, "
                        + "started_at, completed_at, input_summary, output_summary, evidence, notes, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, phase.runKey());
            statement.setString(2, phase.phaseKey());
            statement.setString(3, phase.phaseName());
            statement.setInt(4, phase.phaseOrder());
            statement.setString(5, phase.status());
            nullable(statement, 6, phase.startedAt());
            nullable(statement, 7, phase.completedAt());
            statement.setString(8, phase.inputSummary());
            statement.setString(9, phase.outputSummary());
            statement.setString(10, phase.evidence());
            statement.setString(11, phase.notes());
            statement.setString(12, phase.createdAt());
            statement.setString(13, phase.updatedAt());
            statement.executeUpdate();
        }
    }

    public WorkflowPhaseRun find(Connection connection, String runKey, String phaseKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM workflow_phase_run WHERE run_key = ? AND phase_key = ?")) {
            statement.setString(1, runKey);
            statement.setString(2, phaseKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public List<WorkflowPhaseRun> listByRun(Connection connection, String runKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM workflow_phase_run WHERE run_key = ? ORDER BY phase_order, id")) {
            statement.setString(1, runKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<WorkflowPhaseRun> results = new ArrayList<WorkflowPhaseRun>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    public WorkflowPhaseRun nextPendingAfter(Connection connection, String runKey, int phaseOrder) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM workflow_phase_run WHERE run_key = ? AND phase_order > ? "
                        + "AND status = 'pending' ORDER BY phase_order, id LIMIT 1")) {
            statement.setString(1, runKey);
            statement.setInt(2, phaseOrder);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public void updateStatus(Connection connection, String runKey, String phaseKey, String status,
                             String outputSummary, String evidence, String completedAt, String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE workflow_phase_run SET status = ?, output_summary = ?, evidence = ?, "
                        + "completed_at = ?, updated_at = ? WHERE run_key = ? AND phase_key = ?")) {
            statement.setString(1, status);
            statement.setString(2, outputSummary);
            statement.setString(3, evidence);
            nullable(statement, 4, completedAt);
            statement.setString(5, now);
            statement.setString(6, runKey);
            statement.setString(7, phaseKey);
            statement.executeUpdate();
        }
    }

    private void nullable(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.length() == 0) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private WorkflowPhaseRun map(ResultSet resultSet) throws SQLException {
        return new WorkflowPhaseRun(
                resultSet.getLong("id"),
                resultSet.getString("run_key"),
                resultSet.getString("phase_key"),
                resultSet.getString("phase_name"),
                resultSet.getInt("phase_order"),
                resultSet.getString("status"),
                resultSet.getString("started_at"),
                resultSet.getString("completed_at"),
                resultSet.getString("input_summary"),
                resultSet.getString("output_summary"),
                resultSet.getString("evidence"),
                resultSet.getString("notes"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"));
    }
}
