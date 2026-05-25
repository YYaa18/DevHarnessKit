package com.devharnesskit.dhk.service;

import com.devharnesskit.dhk.sql.DbConnectionRequest;
import com.devharnesskit.dhk.sql.SqlColumn;
import com.devharnesskit.dhk.sql.SqlExecutionRequest;
import com.devharnesskit.dhk.sql.SqlExecutionResult;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SqlExecutionService {
    public SqlExecutionResult execute(MysqlConnectionService connectionService,
                                      DbConnectionRequest connectionRequest,
                                      SqlExecutionRequest request) throws Exception {
        try (Connection connection = connectionService.open(connectionRequest)) {
            if (request.useJdbcReadOnlyHint()) {
                connection.setReadOnly(true);
            }
            return execute(connection, request);
        }
    }

    public SqlExecutionResult execute(Connection connection, SqlExecutionRequest request) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(request.timeoutSeconds());
            statement.setMaxRows(request.limit() + 1);
            boolean hasResultSet = statement.execute(request.sql());
            if (!hasResultSet) {
                return new SqlExecutionResult(new ArrayList<SqlColumn>(), new ArrayList<List<String>>(), false);
            }
            try (ResultSet resultSet = statement.getResultSet()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                List<SqlColumn> columns = new ArrayList<SqlColumn>();
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    columns.add(new SqlColumn(metaData.getColumnLabel(i), metaData.getColumnTypeName(i)));
                }
                List<List<String>> rows = new ArrayList<List<String>>();
                boolean truncated = false;
                while (resultSet.next()) {
                    if (rows.size() >= request.limit()) {
                        truncated = true;
                        break;
                    }
                    List<String> row = new ArrayList<String>();
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        row.add(truncate(resultSet.getString(i), request.maxCellLength()));
                    }
                    rows.add(row);
                }
                return new SqlExecutionResult(columns, rows, truncated);
            }
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
