package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.sql.SqlColumn;
import com.devharnesskit.dhk.sql.SqlExecutionResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class SqlResultRenderer {
    private static final int MAX_OUTPUT_BYTES = 64 * 1024;

    public String render(SqlExecutionResult result, String format) {
        return render(result, format, MAX_OUTPUT_BYTES);
    }

    public String render(SqlExecutionResult result, String format, int maxOutputBytes) {
        String output;
        if ("md".equals(format)) {
            output = markdown(result);
        } else {
            output = table(result);
        }
        return limitUtf8(output, maxOutputBytes);
    }

    private String markdown(SqlExecutionResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("# SQL_RESULT\n\n");
        builder.append("- rows: ").append(result.rows().size()).append('\n');
        builder.append("- truncated: ").append(result.truncated()).append("\n\n");
        if (result.columns().isEmpty()) {
            return builder.toString();
        }
        builder.append('|');
        for (SqlColumn column : result.columns()) {
            builder.append(' ').append(column.name()).append(" |");
        }
        builder.append('\n').append('|');
        for (int i = 0; i < result.columns().size(); i++) {
            builder.append(" --- |");
        }
        builder.append('\n');
        for (List<String> row : result.rows()) {
            builder.append('|');
            for (String cell : row) {
                builder.append(' ').append(escapeMarkdown(cell)).append(" |");
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private String table(SqlExecutionResult result) {
        StringBuilder builder = new StringBuilder();
        for (SqlColumn column : result.columns()) {
            builder.append(column.name()).append('\t');
        }
        builder.append('\n');
        for (List<String> row : result.rows()) {
            for (String cell : row) {
                builder.append(cell.replace('\n', ' ')).append('\t');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private String escapeMarkdown(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace("\n", " ");
    }

    private String limitUtf8(String output, int maxOutputBytes) {
        int limit = Math.max(1, maxOutputBytes);
        if (output.getBytes(StandardCharsets.UTF_8).length <= limit) {
            return output;
        }
        String suffix = "\n\n<!-- truncated: SQL_RESULT exceeded output byte limit -->\n";
        if (suffix.getBytes(StandardCharsets.UTF_8).length >= limit) {
            return truncateUtf8(suffix, limit);
        }
        int contentLimit = limit - suffix.getBytes(StandardCharsets.UTF_8).length;
        return truncateUtf8(output, contentLimit) + suffix;
    }

    private String truncateUtf8(String value, int maxBytes) {
        if (maxBytes <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int used = 0;
        for (int offset = 0; offset < value.length(); ) {
            int codePoint = value.codePointAt(offset);
            String next = new String(Character.toChars(codePoint));
            int nextBytes = next.getBytes(StandardCharsets.UTF_8).length;
            if (used + nextBytes > maxBytes) {
                break;
            }
            builder.append(next);
            used += nextBytes;
            offset += Character.charCount(codePoint);
        }
        return builder.toString();
    }
}
