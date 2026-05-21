package com.devharnesskit.dhk.service;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.sql.DbConnectionRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class MysqlConnectionService {
    public DbConnectionRequest fromArgs(Args args, boolean allowMissingCredentials) {
        String jdbcUrl = args.option("jdbc-url").trim();
        if (jdbcUrl.length() == 0) {
            String host = args.option("host").trim();
            String database = args.option("database").trim();
            if (host.length() == 0 || database.length() == 0) {
                return DbConnectionRequest.invalid("Missing connection parameters: use --jdbc-url or --host --database");
            }
            String port = args.option("port", "3306").trim();
            jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useUnicode=true&characterEncoding=utf8&useSSL=false";
        }
        String user = args.option("user").trim();
        String password = "";
        if (args.hasOption("password-env")) {
            String envName = args.option("password-env").trim();
            password = System.getenv(envName);
            if (password == null) {
                return DbConnectionRequest.invalid("Password environment variable is not set: " + envName);
            }
        } else if (args.hasFlag("password-stdin")) {
            try {
                password = new BufferedReader(new InputStreamReader(System.in, "UTF-8")).readLine();
                if (password == null) {
                    password = "";
                }
            } catch (Exception ex) {
                return DbConnectionRequest.invalid("Failed to read password from stdin");
            }
        } else if (!allowMissingCredentials) {
            return DbConnectionRequest.invalid("Missing password source: use --password-env or --password-stdin");
        }
        if (user.length() == 0 && !allowMissingCredentials) {
            return DbConnectionRequest.invalid("Missing required parameter: --user");
        }
        int connectTimeoutMs = parseInt(args.option("connect-timeout-ms", "5000"), 5000);
        int socketTimeoutMs = parseInt(args.option("socket-timeout-ms", "30000"), 30000);
        return new DbConnectionRequest(true, "", jdbcUrl, user, password, connectTimeoutMs, socketTimeoutMs);
    }

    public Connection open(DbConnectionRequest request) throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.jdbc.Driver");
        Properties properties = new Properties();
        properties.setProperty("user", request.user());
        properties.setProperty("password", request.password());
        properties.setProperty("connectTimeout", String.valueOf(request.connectTimeoutMs()));
        properties.setProperty("socketTimeout", String.valueOf(request.socketTimeoutMs()));
        return DriverManager.getConnection(request.jdbcUrl(), properties);
    }

    private int parseInt(String rawValue, int defaultValue) {
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
