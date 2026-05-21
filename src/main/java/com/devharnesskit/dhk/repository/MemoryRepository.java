package com.devharnesskit.dhk.repository;

import com.devharnesskit.dhk.model.MemoryItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MemoryRepository {
    public long insert(Connection connection, MemoryItem item) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO memory_item(project_key, module_name, memory_type, scope, title, content, tags, "
                        + "status, confidence, source_kind, confirmed_at, confirmed_by, source_files, evidence, "
                        + "effective_from, effective_to, created_at, updated_at, last_used_at, use_count) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, item.projectKey());
            statement.setString(2, item.moduleName());
            statement.setString(3, item.memoryType());
            statement.setString(4, item.scope());
            statement.setString(5, item.title());
            statement.setString(6, item.content());
            statement.setString(7, item.tags());
            statement.setString(8, item.status());
            statement.setInt(9, item.confidence());
            statement.setString(10, item.sourceKind());
            statement.setString(11, item.confirmedAt());
            statement.setString(12, item.confirmedBy());
            statement.setString(13, item.sourceFiles());
            statement.setString(14, item.evidence());
            statement.setString(15, item.effectiveFrom());
            statement.setString(16, item.effectiveTo());
            statement.setString(17, item.createdAt());
            statement.setString(18, item.updatedAt());
            if (item.lastUsedAt().length() == 0) {
                statement.setNull(19, java.sql.Types.VARCHAR);
            } else {
                statement.setString(19, item.lastUsedAt());
            }
            statement.setInt(20, item.useCount());
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for memory insert");
                }
                return resultSet.getLong(1);
            }
        }
    }

    public MemoryItem findById(Connection connection, String projectKey, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM memory_item WHERE project_key = ? AND id = ?")) {
            statement.setString(1, projectKey);
            statement.setLong(2, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return map(resultSet);
            }
        }
    }

    public List<MemoryItem> listCandidates(Connection connection, String projectKey, String module,
                                           String status, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM memory_item WHERE project_key = ?");
        if (module != null && module.length() > 0) {
            sql.append(" AND module_name = ?");
        }
        if (status != null && status.length() > 0) {
            sql.append(" AND status = ?");
        }
        sql.append(" ORDER BY updated_at DESC, id DESC LIMIT ?");

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setString(index++, projectKey);
            if (module != null && module.length() > 0) {
                statement.setString(index++, module);
            }
            if (status != null && status.length() > 0) {
                statement.setString(index++, status);
            }
            statement.setInt(index, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MemoryItem> results = new ArrayList<MemoryItem>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    public List<MemoryItem> searchLike(Connection connection, String projectKey, List<String> tokens,
                                       String module, String status, int limit) throws SQLException {
        if (tokens == null || tokens.isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT * FROM memory_item WHERE project_key = ?");
        if (module != null && module.length() > 0) {
            sql.append(" AND module_name = ?");
        }
        if (status != null && status.length() > 0) {
            sql.append(" AND status = ?");
        }
        appendLikeClause(sql, tokens.size());
        sql.append(" ORDER BY updated_at DESC, id DESC LIMIT ?");

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setString(index++, projectKey);
            if (module != null && module.length() > 0) {
                statement.setString(index++, module);
            }
            if (status != null && status.length() > 0) {
                statement.setString(index++, status);
            }
            index = bindLikeTokens(statement, index, tokens);
            statement.setInt(index, limit);
            return list(statement);
        }
    }

    public List<MemoryItem> findByIds(Connection connection, String projectKey, List<Long> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<MemoryItem> items = new ArrayList<MemoryItem>();
        for (Long id : ids) {
            MemoryItem item = findById(connection, projectKey, id.longValue());
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    public void confirm(Connection connection, String projectKey, long id, int confidence,
                        String confirmedBy, String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE memory_item SET status = 'confirmed', confidence = ?, confirmed_at = ?, "
                        + "confirmed_by = ?, updated_at = ? WHERE project_key = ? AND id = ?")) {
            statement.setInt(1, confidence);
            statement.setString(2, now);
            statement.setString(3, confirmedBy);
            statement.setString(4, now);
            statement.setString(5, projectKey);
            statement.setLong(6, id);
            statement.executeUpdate();
        }
    }

    public List<MemoryItem> listConfirmedForExport(Connection connection, String projectKey, String module,
                                                   int limit) throws SQLException {
        String sql = "SELECT * FROM memory_item WHERE project_key = ? AND status = 'confirmed' "
                + "AND confidence >= 70 AND (module_name = 'global' OR module_name = ?) "
                + "ORDER BY confidence DESC, updated_at DESC, id DESC LIMIT ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, projectKey);
            statement.setString(2, module == null || module.length() == 0 ? "global" : module);
            statement.setInt(3, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MemoryItem> results = new ArrayList<MemoryItem>();
                while (resultSet.next()) {
                    results.add(map(resultSet));
                }
                return results;
            }
        }
    }

    public List<MemoryItem> searchConfirmedForExport(Connection connection, String projectKey, String module,
                                                     List<String> tokens, int limit) throws SQLException {
        if (tokens == null || tokens.isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }
        String sql = "SELECT DISTINCT * FROM memory_item WHERE project_key = ? "
                + "AND status = 'confirmed' AND confidence >= 70 "
                + "AND (module_name = 'global' OR module_name = ?)";
        StringBuilder builder = new StringBuilder(sql);
        appendLikeClause(builder, tokens.size());
        builder.append(" ORDER BY confidence DESC, updated_at DESC, id DESC LIMIT ?");
        try (PreparedStatement statement = connection.prepareStatement(builder.toString())) {
            statement.setString(1, projectKey);
            statement.setString(2, module == null || module.length() == 0 ? "global" : module);
            int index = bindLikeTokens(statement, 3, tokens);
            statement.setInt(index, limit);
            return list(statement);
        }
    }

    public void markUsed(Connection connection, String projectKey, List<Long> ids, String now) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE memory_item SET last_used_at = ?, use_count = use_count + 1 WHERE project_key = ? AND id = ?")) {
            for (Long id : ids) {
                statement.setString(1, now);
                statement.setString(2, projectKey);
                statement.setLong(3, id.longValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public int countAll(Connection connection, String projectKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM memory_item WHERE project_key = ?")) {
            statement.setString(1, projectKey);
            return singleInt(statement);
        }
    }

    public int countByStatus(Connection connection, String projectKey, String status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM memory_item WHERE project_key = ? AND status = ?")) {
            statement.setString(1, projectKey);
            statement.setString(2, status);
            return singleInt(statement);
        }
    }

    private int singleInt(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return 0;
            }
            return resultSet.getInt(1);
        }
    }

    private void appendLikeClause(StringBuilder sql, int tokenCount) {
        sql.append(" AND (");
        for (int i = 0; i < tokenCount; i++) {
            if (i > 0) {
                sql.append(" OR ");
            }
            sql.append("(lower(title) LIKE ? ESCAPE '\\' ")
                    .append("OR lower(content) LIKE ? ESCAPE '\\' ")
                    .append("OR lower(tags) LIKE ? ESCAPE '\\')");
        }
        sql.append(')');
    }

    private int bindLikeTokens(PreparedStatement statement, int startIndex, List<String> tokens) throws SQLException {
        int index = startIndex;
        for (String token : tokens) {
            String pattern = "%" + escapeLike(token.toLowerCase()) + "%";
            statement.setString(index++, pattern);
            statement.setString(index++, pattern);
            statement.setString(index++, pattern);
        }
        return index;
    }

    private String escapeLike(String token) {
        return token.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private List<MemoryItem> list(PreparedStatement statement) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery()) {
            List<MemoryItem> results = new ArrayList<MemoryItem>();
            while (resultSet.next()) {
                results.add(map(resultSet));
            }
            return results;
        }
    }

    private MemoryItem map(ResultSet resultSet) throws SQLException {
        return new MemoryItem(
                resultSet.getLong("id"),
                resultSet.getString("project_key"),
                resultSet.getString("module_name"),
                resultSet.getString("memory_type"),
                resultSet.getString("scope"),
                resultSet.getString("title"),
                resultSet.getString("content"),
                resultSet.getString("tags"),
                resultSet.getString("status"),
                resultSet.getInt("confidence"),
                resultSet.getString("source_kind"),
                resultSet.getString("confirmed_at"),
                resultSet.getString("confirmed_by"),
                resultSet.getString("source_files"),
                resultSet.getString("evidence"),
                resultSet.getString("effective_from"),
                resultSet.getString("effective_to"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"),
                resultSet.getString("last_used_at"),
                resultSet.getInt("use_count")
        );
    }
}
