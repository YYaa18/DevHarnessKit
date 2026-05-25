package com.devharnesskit.dhk.service;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.sql.DbConnectionRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
            jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?"
                    + connectionParameters(args);
        }
        if (!jdbcUrl.toLowerCase(Locale.ROOT).startsWith("jdbc:mysql://")) {
            return DbConnectionRequest.invalid("Only jdbc:mysql:// URLs are allowed");
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

    private String connectionParameters(Args args) {
        List<String> params = new ArrayList<String>();
        params.add("useUnicode=true");
        params.add("characterEncoding=" + option(args, "character-encoding", "utf8"));
        params.add("useSSL=" + option(args, "use-ssl", "false"));
        params.add("zeroDateTimeBehavior=" + option(args, "zero-date-time-behavior", "convertToNull"));
        if (args.hasOption("server-timezone")) {
            params.add("serverTimezone=" + args.option("server-timezone").trim());
        }
        if (args.hasFlag("allow-public-key-retrieval")) {
            params.add("allowPublicKeyRetrieval=true");
        }
        if (args.hasOption("jdbc-params")) {
            appendRawParams(params, args.option("jdbc-params"));
        }
        return join(params, "&");
    }

    private String option(Args args, String key, String defaultValue) {
        String value = args.option(key, defaultValue).trim();
        return value.length() == 0 ? defaultValue : value;
    }

    private void appendRawParams(List<String> params, String rawParams) {
        String normalized = rawParams == null ? "" : rawParams.trim();
        while (normalized.startsWith("?") || normalized.startsWith("&")) {
            normalized = normalized.substring(1);
        }
        if (normalized.length() > 0) {
            params.add(normalized);
        }
    }

    private String join(List<String> values, String separator) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(separator);
            }
            builder.append(values.get(i));
        }
        return builder.toString();
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

    public String compatibilityHint(Exception ex) {
        String message = ex.getMessage();
        if (message == null) {
            return "";
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("caching_sha2_password")) {
            return "Connector/J 5.1.49 cannot authenticate MySQL 8 accounts using caching_sha2_password. "
                    + "Create the DevHarness Kit readonly user with mysql_native_password.";
        }
        if (lower.contains("server timezone") || lower.contains("time zone") || lower.contains("timezone")) {
            return "Add an explicit serverTimezone parameter to --jdbc-url, for example serverTimezone=Asia/Shanghai.";
        }
        if (lower.contains("public key retrieval")) {
            return "For local MySQL 8 testing, either use mysql_native_password or add allowPublicKeyRetrieval=true "
                    + "only to a local/test JDBC URL.";
        }
        if (lower.contains("access denied")) {
            return "Check that the account exists for this host and has read-only privileges on the target schema.";
        }
        return "";
    }

    private int parseInt(String rawValue, int defaultValue) {
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
