package com.devharnesskit.dhk.sql;

import com.devharnesskit.dhk.cli.Args;

public final class SqlExecutionRequest {
    private final String sql;
    private final int limit;
    private final int timeoutSeconds;
    private final int maxCellLength;

    public SqlExecutionRequest(String sql, int limit, int timeoutSeconds, int maxCellLength) {
        this.sql = sql;
        this.limit = limit;
        this.timeoutSeconds = timeoutSeconds;
        this.maxCellLength = maxCellLength;
    }

    public static SqlExecutionRequest fromArgs(String sql, Args args) {
        int maxLimit = parseInt(args.option("max-limit", "1000"), 1000);
        int limit = Math.min(parseInt(args.option("limit", "100"), 100), maxLimit);
        if (limit < 1) {
            limit = 100;
        }
        int timeout = parseInt(args.option("timeout-seconds", "30"), 30);
        int maxCellLength = parseInt(args.option("max-cell-length", "200"), 200);
        return new SqlExecutionRequest(sql, limit, timeout, maxCellLength);
    }

    public String sql() {
        return sql;
    }

    public int limit() {
        return limit;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    public int maxCellLength() {
        return maxCellLength;
    }

    private static int parseInt(String rawValue, int defaultValue) {
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
