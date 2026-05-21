package com.devharnesskit.dhk.repository.spec;

import com.devharnesskit.dhk.model.spec.SpecAcceptance;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SpecAcceptanceRepository {
    public long insert(Connection connection, SpecAcceptance acceptance) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO spec_acceptance(change_key, acceptance_key, acceptance_order, description, "
                        + "expected_result, status, evidence, created_at, updated_at, verified_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, acceptance);
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for spec acceptance insert");
                }
                return resultSet.getLong(1);
            }
        }
    }

    public SpecAcceptance findByKey(Connection connection, String changeKey, String acceptanceKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM spec_acceptance WHERE change_key = ? AND acceptance_key = ?")) {
            statement.setString(1, changeKey);
            statement.setString(2, acceptanceKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public List<SpecAcceptance> listByChange(Connection connection, String changeKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM spec_acceptance WHERE change_key = ? ORDER BY acceptance_order, id")) {
            statement.setString(1, changeKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SpecAcceptance> results = new ArrayList<SpecAcceptance>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    public int nextOrder(Connection connection, String changeKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(MAX(acceptance_order), 0) + 1 FROM spec_acceptance WHERE change_key = ?")) {
            statement.setString(1, changeKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 1;
            }
        }
    }

    public void updateStatus(Connection connection, String changeKey, String acceptanceKey, String status,
                             String evidence, String verifiedAt, String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE spec_acceptance SET status = ?, evidence = ?, verified_at = ?, updated_at = ? "
                        + "WHERE change_key = ? AND acceptance_key = ?")) {
            statement.setString(1, status);
            statement.setString(2, evidence);
            statement.setString(3, verifiedAt);
            statement.setString(4, now);
            statement.setString(5, changeKey);
            statement.setString(6, acceptanceKey);
            statement.executeUpdate();
        }
    }

    private void bind(PreparedStatement statement, SpecAcceptance acceptance) throws SQLException {
        statement.setString(1, acceptance.changeKey());
        statement.setString(2, acceptance.acceptanceKey());
        statement.setInt(3, acceptance.acceptanceOrder());
        statement.setString(4, acceptance.description());
        statement.setString(5, acceptance.expectedResult());
        statement.setString(6, acceptance.status());
        statement.setString(7, acceptance.evidence());
        statement.setString(8, acceptance.createdAt());
        statement.setString(9, acceptance.updatedAt());
        statement.setString(10, acceptance.verifiedAt());
    }

    private SpecAcceptance map(ResultSet resultSet) throws SQLException {
        return new SpecAcceptance(
                resultSet.getLong("id"),
                resultSet.getString("change_key"),
                resultSet.getString("acceptance_key"),
                resultSet.getInt("acceptance_order"),
                resultSet.getString("description"),
                resultSet.getString("expected_result"),
                resultSet.getString("status"),
                resultSet.getString("evidence"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"),
                resultSet.getString("verified_at"));
    }
}
