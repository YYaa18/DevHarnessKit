package com.devharnesskit.dhk.repository.bdd;

import com.devharnesskit.dhk.model.bdd.BddEvidence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class BddEvidenceRepository {
    public BddEvidence insert(Connection connection, BddEvidence evidence) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO bdd_evidence(scenario_key, goal_key, evidence_type, status, "
                        + "evidence_path, summary, command, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, evidence.scenarioKey());
            statement.setString(2, evidence.goalKey());
            statement.setString(3, evidence.evidenceType());
            statement.setString(4, evidence.status());
            statement.setString(5, evidence.evidencePath());
            statement.setString(6, evidence.summary());
            statement.setString(7, evidence.command());
            statement.setString(8, evidence.createdAt());
            statement.setString(9, evidence.updatedAt());
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for bdd evidence insert");
                }
                return findById(connection, resultSet.getLong(1));
            }
        }
    }

    public BddEvidence findById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM bdd_evidence WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public List<BddEvidence> listByScenario(Connection connection, String scenarioKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM bdd_evidence WHERE scenario_key = ? ORDER BY updated_at DESC, id DESC")) {
            statement.setString(1, scenarioKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        }
    }

    public List<BddEvidence> listByScenarioAndGoal(Connection connection, String scenarioKey,
                                                   String goalKey) throws SQLException {
        if (goalKey == null || goalKey.length() == 0) {
            return listByScenario(connection, scenarioKey);
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM bdd_evidence WHERE scenario_key = ? AND goal_key = ? "
                        + "ORDER BY updated_at DESC, id DESC")) {
            statement.setString(1, scenarioKey);
            statement.setString(2, goalKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        }
    }

    private List<BddEvidence> mapAll(ResultSet resultSet) throws SQLException {
        List<BddEvidence> results = new ArrayList<BddEvidence>();
        while (resultSet.next()) {
            results.add(map(resultSet));
        }
        return results;
    }

    private BddEvidence map(ResultSet resultSet) throws SQLException {
        return new BddEvidence(
                resultSet.getLong("id"),
                resultSet.getString("scenario_key"),
                resultSet.getString("goal_key"),
                resultSet.getString("evidence_type"),
                resultSet.getString("status"),
                resultSet.getString("evidence_path"),
                resultSet.getString("summary"),
                resultSet.getString("command"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"));
    }
}
