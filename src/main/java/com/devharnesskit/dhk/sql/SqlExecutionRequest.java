package com.devharnesskit.dhk.sql;

import com.devharnesskit.dhk.cli.Args;

import java.util.Locale;

public final class SqlExecutionRequest {
    private final String sql;
    private final int limit;
    private final int timeoutSeconds;
    private final int maxCellLength;
    private final int maxOutputBytes;

    public SqlExecutionRequest(String sql, int limit, int timeoutSeconds, int maxCellLength) {
        this(sql, limit, timeoutSeconds, maxCellLength, 64 * 1024);
    }

    public SqlExecutionRequest(String sql, int limit, int timeoutSeconds, int maxCellLength, int maxOutputBytes) {
        this.sql = sql;
        this.limit = limit;
        this.timeoutSeconds = timeoutSeconds;
        this.maxCellLength = maxCellLength;
        this.maxOutputBytes = maxOutputBytes;
    }

    public static SqlExecutionRequest fromArgs(String sql, Args args) {
        int maxLimit = Math.min(parseInt(args.option("max-limit", "1000"), 1000), 1000);
        if (maxLimit < 1) {
            maxLimit = 1000;
        }
        int limit = Math.min(parseInt(args.option("limit", "100"), 100), maxLimit);
        if (limit < 1) {
            limit = 100;
        }
        int timeout = parseInt(args.option("timeout-seconds", "30"), 30);
        int maxCellLength = parseInt(args.option("max-cell-length", "200"), 200);
        int maxOutputBytes = parseInt(args.option("max-output-bytes", String.valueOf(64 * 1024)), 64 * 1024);
        if (maxOutputBytes < 1024) {
            maxOutputBytes = 1024;
        }
        return new SqlExecutionRequest(sql, limit, timeout, maxCellLength, maxOutputBytes);
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

    public int maxOutputBytes() {
        return maxOutputBytes;
    }

    public boolean useJdbcReadOnlyHint() {
        return !isExplainStatement();
    }

    public boolean isExplainStatement() {
        return firstKeyword(sql).equals("explain");
    }

    private static int parseInt(String rawValue, int defaultValue) {
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static String firstKeyword(String value) {
        String trimmed = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        int end = 0;
        while (end < trimmed.length() && Character.isLetter(trimmed.charAt(end))) {
            end++;
        }
        return end == 0 ? "" : trimmed.substring(0, end);
    }
}
