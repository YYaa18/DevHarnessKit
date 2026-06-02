package com.devharnesskit.dhk.repository;

import com.devharnesskit.dhk.model.MemoryCandidate;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.service.MemoryIdentity;

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
                        + "effective_from, effective_to, created_at, updated_at, last_used_at, use_count, "
                        + "fingerprint, canonical_key, superseded_by, stale_reason, last_verified_at, source_ref) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
            String fingerprint = item.fingerprint().length() == 0
                    ? MemoryIdentity.fingerprint(item.title(), item.content()) : item.fingerprint();
            String canonicalKey = item.canonicalKey().length() == 0
                    ? MemoryIdentity.canonicalKey(item.moduleName(), item.title()) : item.canonicalKey();
            statement.setString(21, fingerprint);
            statement.setString(22, canonicalKey);
            statement.setLong(23, item.supersededBy());
            statement.setString(24, item.staleReason());
            statement.setString(25, item.lastVerifiedAt());
            statement.setString(26, item.sourceRef().length() == 0 ? item.sourceFiles() : item.sourceRef());
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for memory insert");
                }
                return resultSet.getLong(1);
            }
        }
    }

    public long insertCandidate(Connection connection, MemoryCandidate candidate) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO memory_candidate(project_key, candidate_status, module_name, memory_type, title, "
                        + "content, tags, confidence, source_kind, source_ref, reason, evidence, fingerprint, "
                        + "canonical_key, created_at, updated_at, decided_at, decision_reason, accepted_memory_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, candidate.projectKey());
            statement.setString(2, candidate.candidateStatus());
            statement.setString(3, candidate.moduleName());
            statement.setString(4, candidate.memoryType());
            statement.setString(5, candidate.title());
            statement.setString(6, candidate.content());
            statement.setString(7, candidate.tags());
            statement.setInt(8, candidate.confidence());
            statement.setString(9, candidate.sourceKind());
            statement.setString(10, candidate.sourceRef());
            statement.setString(11, candidate.reason());
            statement.setString(12, candidate.evidence());
            statement.setString(13, candidate.fingerprint());
            statement.setString(14, candidate.canonicalKey());
            statement.setString(15, candidate.createdAt());
            statement.setString(16, candidate.updatedAt());
            statement.setString(17, candidate.decidedAt());
            statement.setString(18, candidate.decisionReason());
            statement.setLong(19, candidate.acceptedMemoryId());
            statement.executeUpdate();
            try (ResultSet resultSet = statement.getGeneratedKeys()) {
                if (!resultSet.next()) {
                    throw new SQLException("No generated key returned for memory candidate insert");
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

    public MemoryCandidate findCandidateById(Connection connection, String projectKey, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM memory_candidate WHERE project_key = ? AND id = ?")) {
            statement.setString(1, projectKey);
            statement.setLong(2, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapCandidate(resultSet);
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

    public List<MemoryCandidate> listMemoryCandidates(Connection connection, String projectKey,
                                                      String status, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM memory_candidate WHERE project_key = ?");
        if (status != null && status.length() > 0) {
            sql.append(" AND candidate_status = ?");
        }
        sql.append(" ORDER BY updated_at DESC, id DESC LIMIT ?");
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setString(index++, projectKey);
            if (status != null && status.length() > 0) {
                statement.setString(index++, status);
            }
            statement.setInt(index, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MemoryCandidate> results = new ArrayList<MemoryCandidate>();
                while (resultSet.next()) {
                    results.add(mapCandidate(resultSet));
                }
                return results;
            }
        }
    }

    public List<MemoryItem> listMemory(Connection connection, String projectKey, String module,
                                       String status, String tag, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM memory_item WHERE project_key = ?");
        if (module != null && module.length() > 0) {
            sql.append(" AND module_name = ?");
        }
        if (status != null && status.length() > 0) {
            sql.append(" AND status = ?");
        }
        if (tag != null && tag.length() > 0) {
            sql.append(" AND lower(tags) LIKE ? ESCAPE '\\'");
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
            if (tag != null && tag.length() > 0) {
                statement.setString(index++, "%" + escapeLike(tag.toLowerCase()) + "%");
            }
            statement.setInt(index, limit);
            return list(statement);
        }
    }

    public List<MemoryItem> listAllMemory(Connection connection, String projectKey, String module,
                                          int limit) throws SQLException {
        return listMemory(connection, projectKey, module, "", "", limit);
    }

    public List<MemoryItem> listPackableMemory(Connection connection, String projectKey, String module,
                                               int limit) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM memory_item WHERE project_key = ? AND status = 'confirmed' "
                + "AND superseded_by = 0 AND stale_reason = ''");
        if (module != null && module.length() > 0) {
            sql.append(" AND (module_name = 'global' OR module_name = ?)");
        }
        sql.append(" ORDER BY confidence DESC, updated_at DESC, id DESC LIMIT ?");
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setString(index++, projectKey);
            if (module != null && module.length() > 0) {
                statement.setString(index++, module);
            }
            statement.setInt(index, limit);
            return list(statement);
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

    public void markCandidateAccepted(Connection connection, String projectKey, long candidateId,
                                      long memoryId, String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE memory_candidate SET candidate_status = 'accepted', accepted_memory_id = ?, "
                        + "decided_at = ?, updated_at = ? WHERE project_key = ? AND id = ?")) {
            statement.setLong(1, memoryId);
            statement.setString(2, now);
            statement.setString(3, now);
            statement.setString(4, projectKey);
            statement.setLong(5, candidateId);
            statement.executeUpdate();
        }
    }

    public void markCandidateRejected(Connection connection, String projectKey, long candidateId,
                                      String reason, String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE memory_candidate SET candidate_status = 'rejected', decision_reason = ?, "
                        + "decided_at = ?, updated_at = ? WHERE project_key = ? AND id = ?")) {
            statement.setString(1, reason);
            statement.setString(2, now);
            statement.setString(3, now);
            statement.setString(4, projectKey);
            statement.setLong(5, candidateId);
            statement.executeUpdate();
        }
    }

    public void supersede(Connection connection, String projectKey, long oldId, long newId,
                          String reason, String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE memory_item SET status = 'deprecated', superseded_by = ?, stale_reason = ?, "
                        + "effective_to = ?, updated_at = ? WHERE project_key = ? AND id = ?")) {
            statement.setLong(1, newId);
            statement.setString(2, reason);
            statement.setString(3, now);
            statement.setString(4, now);
            statement.setString(5, projectKey);
            statement.setLong(6, oldId);
            statement.executeUpdate();
        }
    }

    public void expire(Connection connection, String projectKey, long id, String reason,
                       String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE memory_item SET status = 'deprecated', stale_reason = ?, effective_to = ?, "
                        + "updated_at = ? WHERE project_key = ? AND id = ?")) {
            statement.setString(1, reason);
            statement.setString(2, now);
            statement.setString(3, now);
            statement.setString(4, projectKey);
            statement.setLong(5, id);
            statement.executeUpdate();
        }
    }

    public void refresh(Connection connection, String projectKey, long id, String evidence,
                        String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE memory_item SET status = CASE "
                        + "WHEN status = 'deprecated' AND superseded_by = 0 AND confirmed_at <> '' THEN 'confirmed' "
                        + "WHEN status = 'deprecated' AND superseded_by = 0 THEN 'draft' "
                        + "ELSE status END, "
                        + "stale_reason = '', last_verified_at = ?, evidence = ?, "
                        + "effective_to = '', updated_at = ? WHERE project_key = ? AND id = ?")) {
            statement.setString(1, now);
            statement.setString(2, evidence);
            statement.setString(3, now);
            statement.setString(4, projectKey);
            statement.setLong(5, id);
            statement.executeUpdate();
        }
    }

    public MemoryItem findByFingerprint(Connection connection, String projectKey,
                                        String fingerprint) throws SQLException {
        if (fingerprint == null || fingerprint.length() == 0) {
            return null;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM memory_item WHERE project_key = ? AND fingerprint = ? "
                        + "ORDER BY id DESC LIMIT 1")) {
            statement.setString(1, projectKey);
            statement.setString(2, fingerprint);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return map(resultSet);
            }
        }
    }

    public MemoryItem findByTitleAndContent(Connection connection, String projectKey, String title,
                                            String content) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM memory_item WHERE project_key = ? "
                        + "AND lower(title) = lower(?) AND lower(content) = lower(?) "
                        + "ORDER BY id DESC LIMIT 1")) {
            statement.setString(1, projectKey);
            statement.setString(2, title == null ? "" : title);
            statement.setString(3, content == null ? "" : content);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return map(resultSet);
            }
        }
    }

    public List<MemoryItem> listConfirmedForExport(Connection connection, String projectKey, String module,
                                                   int limit) throws SQLException {
        String sql = "SELECT * FROM memory_item WHERE project_key = ? AND status = 'confirmed' "
                + "AND confidence >= 70 AND (module_name = 'global' OR module_name = ?) "
                + "AND superseded_by = 0 AND stale_reason = '' "
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
                + "AND (module_name = 'global' OR module_name = ?) "
                + "AND superseded_by = 0 AND stale_reason = ''";
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
                resultSet.getInt("use_count"),
                resultSet.getString("fingerprint"),
                resultSet.getString("canonical_key"),
                resultSet.getLong("superseded_by"),
                resultSet.getString("stale_reason"),
                resultSet.getString("last_verified_at"),
                resultSet.getString("source_ref")
        );
    }

    private MemoryCandidate mapCandidate(ResultSet resultSet) throws SQLException {
        return new MemoryCandidate(
                resultSet.getLong("id"),
                resultSet.getString("project_key"),
                resultSet.getString("candidate_status"),
                resultSet.getString("module_name"),
                resultSet.getString("memory_type"),
                resultSet.getString("title"),
                resultSet.getString("content"),
                resultSet.getString("tags"),
                resultSet.getInt("confidence"),
                resultSet.getString("source_kind"),
                resultSet.getString("source_ref"),
                resultSet.getString("reason"),
                resultSet.getString("evidence"),
                resultSet.getString("fingerprint"),
                resultSet.getString("canonical_key"),
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"),
                resultSet.getString("decided_at"),
                resultSet.getString("decision_reason"),
                resultSet.getLong("accepted_memory_id")
        );
    }
}
