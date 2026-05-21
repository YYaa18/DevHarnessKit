package com.devharnesskit.dhk.repository.spec;

import com.devharnesskit.dhk.model.spec.SpecDocument;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SpecDocumentRepository {
    public long insert(Connection connection, SpecDocument document) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO spec_document(change_key, document_type, title, content, content_hash, "
                        + "status, version, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, document);
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for spec document insert");
                }
                return resultSet.getLong(1);
            }
        }
    }

    public void update(Connection connection, SpecDocument document) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE spec_document SET title = ?, content = ?, content_hash = ?, status = ?, "
                        + "version = ?, updated_at = ? WHERE id = ?")) {
            statement.setString(1, document.title());
            statement.setString(2, document.content());
            statement.setString(3, document.contentHash());
            statement.setString(4, document.status());
            statement.setInt(5, document.version());
            statement.setString(6, document.updatedAt());
            statement.setLong(7, document.id());
            statement.executeUpdate();
        }
    }

    public SpecDocument findLatestByType(Connection connection, String changeKey, String documentType) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM spec_document WHERE change_key = ? AND document_type = ? "
                        + "ORDER BY version DESC, id DESC LIMIT 1")) {
            statement.setString(1, changeKey);
            statement.setString(2, documentType);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        }
    }

    public List<SpecDocument> listByChange(Connection connection, String changeKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM spec_document WHERE change_key = ? ORDER BY document_type, version DESC, id DESC")) {
            statement.setString(1, changeKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<SpecDocument> results = new ArrayList<SpecDocument>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    private void bind(PreparedStatement statement, SpecDocument document) throws SQLException {
        statement.setString(1, document.changeKey());
        statement.setString(2, document.documentType());
        statement.setString(3, document.title());
        statement.setString(4, document.content());
        statement.setString(5, document.contentHash());
        statement.setString(6, document.status());
        statement.setInt(7, document.version());
        statement.setString(8, document.createdAt());
        statement.setString(9, document.updatedAt());
    }

    private SpecDocument map(ResultSet resultSet) throws SQLException {
        return new SpecDocument(
                resultSet.getLong("id"),
                resultSet.getString("change_key"),
                resultSet.getString("document_type"),
                resultSet.getString("title"),
                resultSet.getString("content"),
                resultSet.getString("content_hash"),
                resultSet.getString("status"),
                resultSet.getInt("version"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"));
    }
}
