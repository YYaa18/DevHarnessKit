package com.devharnesskit.dhk.service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class DbCompatibilityProbeService {
    public List<String> probe(Connection connection, boolean readOnlyRequested) {
        List<String> lines = new ArrayList<String>();
        addMetadata(lines, connection);
        lines.add("readonly_requested: " + readOnlyRequested);
        lines.add("readonly_effective: " + readOnlyState(connection));
        lines.add("probe_select_1: " + probeStatement(connection, "SELECT 1"));
        lines.add("probe_explain_select_1: " + probeExplainStatement(connection));
        lines.add("probe_show_tables: " + probeStatement(connection, "SHOW TABLES"));
        lines.add("server_version_query: " + querySingleValue(connection, "SELECT VERSION()", 1));
        lines.add("server_time_zone: " + querySingleValue(connection, "SHOW VARIABLES LIKE 'time_zone'", 2));
        lines.add("server_sql_mode: " + querySingleValue(connection, "SHOW VARIABLES LIKE 'sql_mode'", 2));
        lines.add("server_version_comment: "
                + querySingleValue(connection, "SHOW VARIABLES LIKE 'version_comment'", 2));
        return lines;
    }

    private void addMetadata(List<String> lines, Connection connection) {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            lines.add("database_product: " + value(metaData.getDatabaseProductName()));
            lines.add("database_version: " + value(metaData.getDatabaseProductVersion()));
            lines.add("driver_name: " + value(metaData.getDriverName()));
            lines.add("driver_version: " + value(metaData.getDriverVersion()));
        } catch (Exception ex) {
            lines.add("database_metadata: fail: " + summarize(ex));
        }
    }

    private String readOnlyState(Connection connection) {
        try {
            return String.valueOf(connection.isReadOnly());
        } catch (Exception ex) {
            return "unknown: " + summarize(ex);
        }
    }

    private String probeStatement(Connection connection, String sql) {
        try (Statement statement = connection.createStatement()) {
            setQueryTimeout(statement);
            boolean hasResultSet = statement.execute(sql);
            if (hasResultSet) {
                try (ResultSet resultSet = statement.getResultSet()) {
                    int rows = 0;
                    while (resultSet.next() && rows < 1) {
                        rows++;
                    }
                    return "ok";
                }
            }
            return "ok";
        } catch (Exception ex) {
            return "fail: " + summarize(ex);
        }
    }

    private String probeExplainStatement(Connection connection) {
        Boolean originalReadOnly = originalReadOnly(connection);
        if (Boolean.TRUE.equals(originalReadOnly)) {
            try {
                connection.setReadOnly(false);
            } catch (Exception ex) {
                return "fail: " + summarize(ex);
            }
        }
        try {
            return probeStatement(connection, "EXPLAIN SELECT 1");
        } finally {
            if (Boolean.TRUE.equals(originalReadOnly)) {
                try {
                    connection.setReadOnly(true);
                } catch (Exception ignored) {
                    // Compatibility probe output should not fail just because a driver cannot restore the hint.
                }
            }
        }
    }

    private Boolean originalReadOnly(Connection connection) {
        try {
            return Boolean.valueOf(connection.isReadOnly());
        } catch (Exception ex) {
            return null;
        }
    }

    private String querySingleValue(Connection connection, String sql, int columnIndex) {
        try (Statement statement = connection.createStatement()) {
            setQueryTimeout(statement);
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                if (!resultSet.next()) {
                    return "";
                }
                return value(resultSet.getString(columnIndex));
            }
        } catch (Exception ex) {
            return "unknown: " + summarize(ex);
        }
    }

    private void setQueryTimeout(Statement statement) {
        try {
            statement.setQueryTimeout(5);
        } catch (Exception ignored) {
            // Some JDBC drivers ignore or reject query timeout; compatibility probing should continue.
        }
    }

    private String value(String value) {
        if (value == null) {
            return "";
        }
        return sanitize(value);
    }

    private String summarize(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.trim().length() == 0) {
            message = ex.getClass().getSimpleName();
        }
        return sanitize(message);
    }

    private String sanitize(String value) {
        String sanitized = value.replace('\n', ' ').replace('\r', ' ').trim();
        if (sanitized.length() <= 180) {
            return sanitized;
        }
        return sanitized.substring(0, 177) + "...";
    }
}
