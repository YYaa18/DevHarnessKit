package com.devharnesskit.dhk.db.migration;

import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

final class BriefLifecycleLegacyStoreImporter {
    void importLegacyStores(Connection connection, Clock clock) throws SQLException {
        Path projectRoot = projectRoot(connection);
        if (projectRoot == null) {
            return;
        }
        String projectKey = projectKey(connection);
        String now = clock.now().toString();
        importCandidates(connection, projectRoot, projectKey, now);
        importInteractions(connection, projectRoot, projectKey, now);
        importGrowthLessons(connection, projectRoot, projectKey, now);
    }

    private void importCandidates(Connection connection, Path projectRoot, String projectKey, String now)
            throws SQLException {
        for (String[] row : readRows(PathUtil.knowledgeCandidatesStore(projectRoot))) {
            if (row.length < 11) {
                continue;
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO knowledge_candidate(candidate_id, project_key, goal_key, candidate_type, "
                            + "title, summary, evidence_refs, suggested_destination, confidence, "
                            + "requires_confirmation, sensitive_scan_status, status, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON CONFLICT(candidate_id) DO NOTHING")) {
                statement.setString(1, row[0]);
                statement.setString(2, projectKey);
                statement.setString(3, row[1]);
                statement.setString(4, row[2]);
                statement.setString(5, row[3]);
                statement.setString(6, row[4]);
                statement.setString(7, row[5]);
                statement.setString(8, row[6]);
                statement.setString(9, row[7]);
                statement.setInt(10, Boolean.parseBoolean(row[8]) ? 1 : 0);
                statement.setString(11, row[9]);
                statement.setString(12, row[10]);
                statement.setString(13, now);
                statement.setString(14, now);
                statement.executeUpdate();
            }
        }
    }

    private void importInteractions(Connection connection, Path projectRoot, String projectKey, String now)
            throws SQLException {
        for (String[] row : readRows(PathUtil.interactionRequests(projectRoot))) {
            if (row.length < 12) {
                continue;
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO interaction_request(request_id, project_key, goal_key, phase, "
                            + "interaction_type, priority, question, why, choices, default_choice, "
                            + "blocks_progress, status, answer, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON CONFLICT(request_id) DO NOTHING")) {
                statement.setString(1, row[0]);
                statement.setString(2, projectKey);
                statement.setString(3, row[1]);
                statement.setString(4, row[2]);
                statement.setString(5, row[3]);
                statement.setString(6, row[4]);
                statement.setString(7, row[5]);
                statement.setString(8, row[6]);
                statement.setString(9, row[7]);
                statement.setString(10, row[8]);
                statement.setInt(11, Boolean.parseBoolean(row[9]) ? 1 : 0);
                statement.setString(12, row[10]);
                statement.setString(13, row[11]);
                statement.setString(14, now);
                statement.setString(15, now);
                statement.executeUpdate();
            }
        }
    }

    private void importGrowthLessons(Connection connection, Path projectRoot, String projectKey, String now)
            throws SQLException {
        for (String[] row : readRows(PathUtil.growthLessonsStore(projectRoot))) {
            if (row.length < 6) {
                continue;
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO growth_lesson(lesson_id, project_key, source_candidate_id, title, "
                            + "summary, status, advisory_only, created_at, updated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                            + "ON CONFLICT(lesson_id) DO NOTHING")) {
                statement.setString(1, row[0]);
                statement.setString(2, projectKey);
                statement.setString(3, row[1]);
                statement.setString(4, row[2]);
                statement.setString(5, row[3]);
                statement.setString(6, row[4]);
                statement.setInt(7, Boolean.parseBoolean(row[5]) ? 1 : 0);
                statement.setString(8, now);
                statement.setString(9, now);
                statement.executeUpdate();
            }
        }
    }

    private Path projectRoot(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA database_list")) {
            while (resultSet.next()) {
                if ("main".equals(resultSet.getString("name"))) {
                    String file = resultSet.getString("file");
                    if (file == null || file.length() == 0) {
                        return null;
                    }
                    Path db = Paths.get(file).toAbsolutePath().normalize();
                    Path memory = db.getParent();
                    Path agents = memory == null ? null : memory.getParent();
                    return agents == null ? null : agents.getParent();
                }
            }
            return null;
        }
    }

    private String projectKey(Connection connection) {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT project_key FROM project LIMIT 1")) {
            return resultSet.next() ? value(resultSet.getString(1)) : "";
        } catch (SQLException ex) {
            return "";
        }
    }

    private List<String[]> readRows(Path path) {
        List<String[]> rows = new ArrayList<String[]>();
        if (!Files.isRegularFile(path)) {
            return rows;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                if (line.trim().length() == 0) {
                    continue;
                }
                String[] raw = line.split("\\t", -1);
                String[] unescaped = new String[raw.length];
                for (int i = 0; i < raw.length; i++) {
                    unescaped[i] = unescape(raw[i]);
                }
                rows.add(unescaped);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to import brief lifecycle store: " + ex.getMessage(), ex);
        }
        return rows;
    }

    private String unescape(String value) {
        StringBuilder builder = new StringBuilder();
        boolean slash = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (slash) {
                builder.append(ch == 't' ? '\t' : ch == 'n' ? '\n' : ch == 'r' ? '\r' : ch);
                slash = false;
            } else if (ch == '\\') {
                slash = true;
            } else {
                builder.append(ch);
            }
        }
        if (slash) {
            builder.append('\\');
        }
        return builder.toString();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
