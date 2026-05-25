package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.sql.SqlColumn;
import com.devharnesskit.dhk.sql.SqlExecutionResult;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SqlResultRendererTest {
    @Test
    void rendersJsonResult() {
        List<SqlColumn> columns = Arrays.asList(new SqlColumn("ok", "INTEGER"));
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(Arrays.asList("1"));
        SqlExecutionResult result = new SqlExecutionResult(columns, rows, false);

        String rendered = new SqlResultRenderer().render(result, "json", 4096);

        assertTrue(rendered.contains("\"command\": \"db sql\""));
        assertTrue(rendered.contains("\"status\": \"ok\""));
        assertTrue(rendered.contains("\"rows\": 1"));
        assertTrue(rendered.contains("\"output_truncated\": false"));
        assertTrue(rendered.contains("\"columns\": [{"));
        assertTrue(rendered.contains("\"name\": \"ok\""));
        assertTrue(rendered.contains("\"data\": [[\"1\"]]"));
    }

    @Test
    void jsonResultStaysValidWhenOutputWouldBeTooLarge() {
        List<SqlColumn> columns = Arrays.asList(new SqlColumn("name", "VARCHAR"));
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(Arrays.asList(repeat("中文", 500)));
        SqlExecutionResult result = new SqlExecutionResult(columns, rows, false);

        String rendered = new SqlResultRenderer().render(result, "json", 1024);

        assertTrue(rendered.contains("\"command\": \"db sql\""));
        assertTrue(rendered.contains("\"truncated\": true"));
        assertTrue(rendered.contains("\"output_truncated\": true"));
        assertTrue(rendered.contains("\"data\": []"));
        assertFalse(rendered.contains("<!-- truncated:"));
    }

    @Test
    void truncatesByUtf8BytesWithoutSplittingCharacters() {
        List<SqlColumn> columns = Arrays.asList(new SqlColumn("name", "VARCHAR"));
        List<List<String>> rows = new ArrayList<List<String>>();
        rows.add(Arrays.asList(repeat("中文", 500)));
        SqlExecutionResult result = new SqlExecutionResult(columns, rows, false);

        String rendered = new SqlResultRenderer().render(result, "md", 1024);

        assertTrue(rendered.getBytes(StandardCharsets.UTF_8).length <= 1024);
        assertTrue(rendered.contains("truncated: SQL_RESULT exceeded output byte limit"));
        assertFalse(rendered.contains("\uFFFD"));
    }

    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
