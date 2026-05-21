package com.devharnesskit.dhk.repository.workflow;

import com.devharnesskit.dhk.model.workflow.WorkflowGateRun;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class WorkflowGateRunRepository {
    public void insert(Connection connection, WorkflowGateRun gate) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO workflow_gate_run(run_key, phase_key, gate_key, gate_name, gate_type, severity, "
                        + "status, checked_at, result_summary, failure_reason, evidence, command_output_path, "
                        + "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, gate.runKey());
            statement.setString(2, gate.phaseKey());
            statement.setString(3, gate.gateKey());
            statement.setString(4, gate.gateName());
            statement.setString(5, gate.gateType());
            statement.setString(6, gate.severity());
            statement.setString(7, gate.status());
            nullable(statement, 8, gate.checkedAt());
            statement.setString(9, gate.resultSummary());
            statement.setString(10, gate.failureReason());
            statement.setString(11, gate.evidence());
            statement.setString(12, gate.commandOutputPath());
            statement.setString(13, gate.createdAt());
            statement.setString(14, gate.updatedAt());
            statement.executeUpdate();
        }
    }

    public WorkflowGateRun findByRunGate(Connection connection, String runKey, String gateKey) throws SQLException {
        List<WorkflowGateRun> gates = listByRunGate(connection, runKey, gateKey);
        return gates.isEmpty() ? null : gates.get(0);
    }

    public List<WorkflowGateRun> listByRunGate(Connection connection, String runKey,
                                               String gateKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM workflow_gate_run WHERE run_key = ? AND gate_key = ? ORDER BY id")) {
            statement.setString(1, runKey);
            statement.setString(2, gateKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<WorkflowGateRun> results = new ArrayList<WorkflowGateRun>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    public WorkflowGateRun findByRunPhaseGate(Connection connection, String runKey, String phaseKey,
                                              String gateKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM workflow_gate_run WHERE run_key = ? AND phase_key = ? AND gate_key = ?")) {
            statement.setString(1, runKey);
            statement.setString(2, phaseKey);
            statement.setString(3, gateKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public List<WorkflowGateRun> listByRun(Connection connection, String runKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT g.* FROM workflow_gate_run g "
                        + "JOIN workflow_phase_run p ON p.run_key = g.run_key AND p.phase_key = g.phase_key "
                        + "WHERE g.run_key = ? ORDER BY p.phase_order, g.id")) {
            statement.setString(1, runKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<WorkflowGateRun> results = new ArrayList<WorkflowGateRun>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    public List<WorkflowGateRun> pendingHardForPhase(Connection connection, String runKey,
                                                     String phaseKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM workflow_gate_run WHERE run_key = ? AND phase_key = ? "
                        + "AND severity = 'hard' AND status = 'pending' ORDER BY id")) {
            statement.setString(1, runKey);
            statement.setString(2, phaseKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<WorkflowGateRun> results = new ArrayList<WorkflowGateRun>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    public List<WorkflowGateRun> blockingHardForPhase(Connection connection, String runKey,
                                                      String phaseKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM workflow_gate_run WHERE run_key = ? AND phase_key = ? "
                        + "AND severity = 'hard' AND status IN ('pending', 'failed') ORDER BY id")) {
            statement.setString(1, runKey);
            statement.setString(2, phaseKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<WorkflowGateRun> results = new ArrayList<WorkflowGateRun>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    public void updateStatus(Connection connection, long id, String status, String resultSummary,
                             String failureReason, String evidence, String checkedAt, String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE workflow_gate_run SET status = ?, result_summary = ?, failure_reason = ?, evidence = ?, "
                        + "checked_at = ?, updated_at = ? WHERE id = ?")) {
            statement.setString(1, status);
            statement.setString(2, resultSummary);
            statement.setString(3, failureReason);
            statement.setString(4, evidence);
            nullable(statement, 5, checkedAt);
            statement.setString(6, now);
            statement.setLong(7, id);
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

    private WorkflowGateRun map(ResultSet resultSet) throws SQLException {
        return new WorkflowGateRun(
                resultSet.getLong("id"),
                resultSet.getString("run_key"),
                resultSet.getString("phase_key"),
                resultSet.getString("gate_key"),
                resultSet.getString("gate_name"),
                resultSet.getString("gate_type"),
                resultSet.getString("severity"),
                resultSet.getString("status"),
                resultSet.getString("checked_at"),
                resultSet.getString("result_summary"),
                resultSet.getString("failure_reason"),
                resultSet.getString("evidence"),
                resultSet.getString("command_output_path"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"));
    }
}
