package com.devharnesskit.dhk.service;

import com.devharnesskit.dhk.sql.SqlSafetyResult;

import java.util.Locale;

public final class SqlSafetyGuard {
    public SqlSafetyResult validate(String sql, boolean explain) {
        SanitizedSql sanitized = stripCommentsAndValidateSemicolons(sql);
        if (!sanitized.allowed()) {
            return SqlSafetyResult.rejected(sanitized.reason());
        }
        String normalized = sanitized.sql();
        normalized = trimTrailingSemicolon(normalized.trim());
        if (normalized.length() == 0) {
            return SqlSafetyResult.rejected("empty SQL");
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        String first = firstKeyword(lower);
        if ("with".equals(first)) {
            return SqlSafetyResult.rejected("WITH is not allowed in MVP");
        }
        if (containsRiskyPattern(lower)) {
            return SqlSafetyResult.rejected("high risk SQL pattern is not allowed");
        }
        if (!isReadonlyKeyword(first)) {
            return SqlSafetyResult.rejected("only SELECT, SHOW, DESC, DESCRIBE, and EXPLAIN are allowed");
        }
        if (explain) {
            if (!"select".equals(first)) {
                return SqlSafetyResult.rejected("--explain only supports SELECT");
            }
            return SqlSafetyResult.allowed("EXPLAIN " + normalized);
        }
        if ("explain".equals(first)) {
            String explained = lower.substring("explain".length()).trim();
            String explainedFirst = firstKeyword(explained);
            if (!"select".equals(explainedFirst) && !"show".equals(explainedFirst)
                    && !"desc".equals(explainedFirst) && !"describe".equals(explainedFirst)) {
                return SqlSafetyResult.rejected("EXPLAIN is only allowed for readonly statements");
            }
            return SqlSafetyResult.allowed(normalized);
        }
        return SqlSafetyResult.allowed(normalized);
    }

    private boolean isReadonlyKeyword(String first) {
        return "select".equals(first) || "show".equals(first) || "desc".equals(first)
                || "describe".equals(first) || "explain".equals(first);
    }

    private boolean containsRiskyPattern(String lower) {
        String riskText = maskQuotedContent(lower).replaceAll("\\s+", " ");
        return riskText.matches(".*\\bwith\\b.*")
                || riskText.matches(".*\\binto\\b.*")
                || riskText.matches(".*\\bsleep\\s*\\(.*")
                || riskText.matches(".*\\bload_file\\s*\\(.*")
                || riskText.matches(".*\\b(get_lock|release_lock|benchmark)\\s*\\(.*")
                || riskText.matches(".*\\bfor\\s+update\\b.*")
                || riskText.matches(".*\\block\\s+in\\s+share\\s+mode\\b.*")
                || riskText.matches(".*\\b(insert|update|delete|drop|alter|truncate|create|grant|revoke|call|set|replace|load)\\b.*");
    }

    private String firstKeyword(String lower) {
        String trimmed = lower.trim();
        int end = 0;
        while (end < trimmed.length() && Character.isLetter(trimmed.charAt(end))) {
            end++;
        }
        return end == 0 ? "" : trimmed.substring(0, end);
    }

    private String trimTrailingSemicolon(String sql) {
        String trimmed = sql.trim();
        if (trimmed.endsWith(";")) {
            return trimmed.substring(0, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private SanitizedSql stripCommentsAndValidateSemicolons(String sql) {
        if (sql == null) {
            return SanitizedSql.allowed("");
        }
        StringBuilder out = new StringBuilder();
        int state = 0;
        int lastSemicolon = -1;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';
            if (state == 0) {
                if (ch == '\'') {
                    state = 1;
                    out.append(ch);
                } else if (ch == '"') {
                    state = 2;
                    out.append(ch);
                } else if (ch == '`') {
                    state = 3;
                    out.append(ch);
                } else if (ch == '-' && next == '-') {
                    state = 4;
                    i++;
                    out.append(' ');
                } else if (ch == '#') {
                    state = 4;
                    out.append(' ');
                } else if (ch == '/' && next == '*') {
                    if (i + 2 < sql.length() && sql.charAt(i + 2) == '!') {
                        return SanitizedSql.rejected("versioned comments are not allowed");
                    }
                    state = 5;
                    i++;
                    out.append(' ');
                } else {
                    if (ch == ';') {
                        if (lastSemicolon >= 0) {
                            return SanitizedSql.rejected("multiple statements are not allowed");
                        }
                        lastSemicolon = out.length();
                    }
                    out.append(ch);
                }
            } else if (state == 1) {
                out.append(ch);
                if (ch == '\\' && next != '\0') {
                    out.append(next);
                    i++;
                } else if (ch == '\'') {
                    state = 0;
                }
            } else if (state == 2) {
                out.append(ch);
                if (ch == '\\' && next != '\0') {
                    out.append(next);
                    i++;
                } else if (ch == '"') {
                    state = 0;
                }
            } else if (state == 3) {
                out.append(ch);
                if (ch == '`') {
                    state = 0;
                }
            } else if (state == 4) {
                if (ch == '\n' || ch == '\r') {
                    state = 0;
                    out.append(' ');
                }
            } else if (state == 5) {
                if (ch == '*' && next == '/') {
                    state = 0;
                    i++;
                    out.append(' ');
                }
            }
        }
        if (lastSemicolon >= 0) {
            for (int i = lastSemicolon + 1; i < out.length(); i++) {
                if (!Character.isWhitespace(out.charAt(i))) {
                    return SanitizedSql.rejected("multiple statements are not allowed");
                }
            }
        }
        return state == 0 ? SanitizedSql.allowed(out.toString())
                : SanitizedSql.rejected("unterminated string or comment");
    }

    private String maskQuotedContent(String sql) {
        StringBuilder out = new StringBuilder();
        int state = 0;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';
            if (state == 0) {
                if (ch == '\'') {
                    state = 1;
                    out.append(' ');
                } else if (ch == '"') {
                    state = 2;
                    out.append(' ');
                } else if (ch == '`') {
                    state = 3;
                    out.append(' ');
                } else {
                    out.append(ch);
                }
            } else if (state == 1) {
                out.append(' ');
                if (ch == '\\' && next != '\0') {
                    out.append(' ');
                    i++;
                } else if (ch == '\'') {
                    state = 0;
                }
            } else if (state == 2) {
                out.append(' ');
                if (ch == '\\' && next != '\0') {
                    out.append(' ');
                    i++;
                } else if (ch == '"') {
                    state = 0;
                }
            } else if (state == 3) {
                out.append(' ');
                if (ch == '`') {
                    state = 0;
                }
            }
        }
        return out.toString();
    }

    private static final class SanitizedSql {
        private final boolean allowed;
        private final String sql;
        private final String reason;

        private SanitizedSql(boolean allowed, String sql, String reason) {
            this.allowed = allowed;
            this.sql = sql == null ? "" : sql;
            this.reason = reason == null ? "" : reason;
        }

        private static SanitizedSql allowed(String sql) {
            return new SanitizedSql(true, sql, "");
        }

        private static SanitizedSql rejected(String reason) {
            return new SanitizedSql(false, "", reason);
        }

        private boolean allowed() {
            return allowed;
        }

        private String sql() {
            return sql;
        }

        private String reason() {
            return reason;
        }
    }
}
