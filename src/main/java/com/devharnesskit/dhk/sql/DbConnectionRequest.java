package com.devharnesskit.dhk.sql;

public final class DbConnectionRequest {
    private final boolean valid;
    private final String error;
    private final String jdbcUrl;
    private final String user;
    private final String password;
    private final int connectTimeoutMs;
    private final int socketTimeoutMs;

    public DbConnectionRequest(boolean valid, String error, String jdbcUrl, String user, String password,
                               int connectTimeoutMs, int socketTimeoutMs) {
        this.valid = valid;
        this.error = error == null ? "" : error;
        this.jdbcUrl = jdbcUrl == null ? "" : jdbcUrl;
        this.user = user == null ? "" : user;
        this.password = password == null ? "" : password;
        this.connectTimeoutMs = connectTimeoutMs;
        this.socketTimeoutMs = socketTimeoutMs;
    }

    public static DbConnectionRequest invalid(String error) {
        return new DbConnectionRequest(false, error, "", "", "", 0, 0);
    }

    public boolean valid() {
        return valid;
    }

    public String error() {
        return error;
    }

    public String jdbcUrl() {
        return jdbcUrl;
    }

    public String user() {
        return user;
    }

    public String password() {
        return password;
    }

    public int connectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int socketTimeoutMs() {
        return socketTimeoutMs;
    }
}
