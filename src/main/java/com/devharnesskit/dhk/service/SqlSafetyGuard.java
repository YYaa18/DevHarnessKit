package com.devharnesskit.dhk.service;

import com.devharnesskit.dhk.sql.SqlSafetyResult;

import java.util.Locale;

public final class SqlSafetyGuard {
    public SqlSafetyResult validate(String sql, boolean explain) {
        String normalized = stripCommentsAndValidateSemicolons(sql);
        if (normalized == null) {
            return SqlSafetyResult.rejected("multiple statements are not allowed");
        }
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
        if ("explain".equals(first)) {
            String explained = lower.substring("explain".length()).trim();
            String explainedFirst = firstKeyword(explained);
            if (!"select".equals(explainedFirst) && !"show".equals(explainedFirst)
                    && !"desc".equals(explainedFirst) && !"describe".equals(explainedFirst)) {
                return SqlSafetyResult.rejected("EXPLAIN is only allowed for readonly statements");
            }
            return SqlSafetyResult.allowed(normalized);
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
        return SqlSafetyResult.allowed(normalized);
    }

    private boolean isReadonlyKeyword(String first) {
        return "select".equals(first) || "show".equals(first) || "desc".equals(first)
                || "describe".equals(first) || "explain".equals(first);
    }

    private boolean containsRiskyPattern(String lower) {
        return lower.contains(" into outfile")
                || lower.contains(" into dumpfile")
                || lower.contains("sleep(")
                || lower.contains("load_file(")
                || lower.matches(".*\\b(insert|update|delete|drop|alter|truncate|create|grant|revoke|call|set|replace|load)\\b.*");
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

    private String stripCommentsAndValidateSemicolons(String sql) {
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
                    state = 5;
                    i++;
                    out.append(' ');
                } else {
                    if (ch == ';') {
                        if (lastSemicolon >= 0) {
                            return null;
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
                    return null;
                }
            }
        }
        return out.toString();
    }
}
