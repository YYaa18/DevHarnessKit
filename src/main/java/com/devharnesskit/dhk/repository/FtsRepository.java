package com.devharnesskit.dhk.repository;

import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.util.TextUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class FtsRepository {
    public void sync(Connection connection, MemoryItem item) throws SQLException {
        if (item == null || !isAvailable(connection)) {
            return;
        }
        try (PreparedStatement delete = connection.prepareStatement("DELETE FROM memory_fts WHERE rowid = ?")) {
            delete.setLong(1, item.id());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO memory_fts(rowid, title, content, tags) VALUES (?, ?, ?, ?)")) {
            insert.setLong(1, item.id());
            insert.setString(2, item.title());
            insert.setString(3, item.content());
            insert.setString(4, item.tags());
            insert.executeUpdate();
        }
    }

    public Set<Long> searchIds(Connection connection, String query, int limit) {
        try {
            if (!isAvailable(connection)) {
                return Collections.emptySet();
            }
            String ftsQuery = buildFtsQuery(query);
            if (ftsQuery.length() == 0) {
                return Collections.emptySet();
            }
            Set<Long> ids = new LinkedHashSet<Long>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT rowid FROM memory_fts WHERE memory_fts MATCH ? LIMIT ?")) {
                statement.setString(1, ftsQuery);
                statement.setInt(2, limit);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        ids.add(Long.valueOf(resultSet.getLong(1)));
                    }
                }
            }
            return ids;
        } catch (SQLException ex) {
            return Collections.emptySet();
        }
    }

    private boolean isAvailable(Connection connection) throws SQLException {
        return MigrationRunner.hasTable(connection, "memory_fts");
    }

    private String buildFtsQuery(String query) {
        List<String> tokens = TextUtil.tokens(query);
        List<String> cleaned = new ArrayList<String>();
        for (String token : tokens) {
            String clean = token.replaceAll("[^a-z0-9_]", "");
            if (clean.length() > 0) {
                cleaned.add(clean);
            }
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < cleaned.size(); i++) {
            if (i > 0) {
                builder.append(" OR ");
            }
            builder.append('"').append(cleaned.get(i)).append('"');
        }
        return builder.toString();
    }
}
