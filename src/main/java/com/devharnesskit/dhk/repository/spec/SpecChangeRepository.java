package com.devharnesskit.dhk.repository.spec;

import com.devharnesskit.dhk.model.spec.SpecChange;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class SpecChangeRepository {
    public void insert(Connection connection, SpecChange change) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO spec_change(change_key, project_key, title, summary, module_name, mode, "
                        + "status, priority, source_kind, created_by, created_at, updated_at, archived_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            bind(statement, change);
            statement.executeUpdate();
        }
    }

    public SpecChange findByKey(Connection connection, String changeKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM spec_change WHERE change_key = ?")) {
            statement.setString(1, changeKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public void updateStatus(Connection connection, String changeKey, String status, String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE spec_change SET status = ?, updated_at = ? WHERE change_key = ?")) {
            statement.setString(1, status);
            statement.setString(2, now);
            statement.setString(3, changeKey);
            statement.executeUpdate();
        }
    }

    public void archive(Connection connection, String changeKey, String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE spec_change SET status = 'archived', archived_at = ?, updated_at = ? WHERE change_key = ?")) {
            statement.setString(1, now);
            statement.setString(2, now);
            statement.setString(3, changeKey);
            statement.executeUpdate();
        }
    }

    private void bind(PreparedStatement statement, SpecChange change) throws SQLException {
        statement.setString(1, change.changeKey());
        statement.setString(2, change.projectKey());
        statement.setString(3, change.title());
        statement.setString(4, change.summary());
        statement.setString(5, change.moduleName());
        statement.setString(6, change.mode());
        statement.setString(7, change.status());
        statement.setString(8, change.priority());
        statement.setString(9, change.sourceKind());
        statement.setString(10, change.createdBy());
        statement.setString(11, change.createdAt());
        statement.setString(12, change.updatedAt());
        statement.setString(13, change.archivedAt());
    }

    private SpecChange map(ResultSet resultSet) throws SQLException {
        return new SpecChange(
                resultSet.getString("change_key"),
                resultSet.getString("project_key"),
                resultSet.getString("title"),
                resultSet.getString("summary"),
                resultSet.getString("module_name"),
                resultSet.getString("mode"),
                resultSet.getString("status"),
                resultSet.getString("priority"),
                resultSet.getString("source_kind"),
                resultSet.getString("created_by"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"),
                resultSet.getString("archived_at"));
    }
}
