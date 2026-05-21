package com.devharnesskit.dhk.sql;

public final class SqlSafetyResult {
    private final boolean allowed;
    private final String reason;
    private final String executableSql;

    private SqlSafetyResult(boolean allowed, String reason, String executableSql) {
        this.allowed = allowed;
        this.reason = reason == null ? "" : reason;
        this.executableSql = executableSql == null ? "" : executableSql;
    }

    public static SqlSafetyResult allowed(String executableSql) {
        return new SqlSafetyResult(true, "", executableSql);
    }

    public static SqlSafetyResult rejected(String reason) {
        return new SqlSafetyResult(false, reason, "");
    }

    public boolean allowed() {
        return allowed;
    }

    public String reason() {
        return reason;
    }

    public String executableSql() {
        return executableSql;
    }
}
