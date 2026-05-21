package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.sql.SqlColumn;
import com.devharnesskit.dhk.sql.SqlExecutionResult;

import java.util.List;

public final class SqlResultRenderer {
    private static final int MAX_OUTPUT_BYTES = 64 * 1024;

    public String render(SqlExecutionResult result, String format) {
        String output;
        if ("md".equals(format)) {
            output = markdown(result);
        } else {
            output = table(result);
        }
        if (output.getBytes().length <= MAX_OUTPUT_BYTES) {
            return output;
        }
        return output.substring(0, Math.min(output.length(), MAX_OUTPUT_BYTES - 80))
                + "\n\n<!-- truncated: SQL_RESULT exceeded 64KB -->\n";
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
}
