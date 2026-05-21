package com.devharnesskit.dhk.sql;

import java.util.Collections;
import java.util.List;

public final class SqlExecutionResult {
    private final List<SqlColumn> columns;
    private final List<List<String>> rows;
    private final boolean truncated;

    public SqlExecutionResult(List<SqlColumn> columns, List<List<String>> rows, boolean truncated) {
        this.columns = Collections.unmodifiableList(columns);
        this.rows = Collections.unmodifiableList(rows);
        this.truncated = truncated;
    }

    public List<SqlColumn> columns() {
        return columns;
    }

    public List<List<String>> rows() {
        return rows;
    }

    public boolean truncated() {
        return truncated;
    }
}
