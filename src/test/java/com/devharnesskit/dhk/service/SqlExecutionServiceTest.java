package com.devharnesskit.dhk.service;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.sql.SqlExecutionRequest;
import com.devharnesskit.dhk.sql.SqlExecutionResult;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SqlExecutionServiceTest {
    @Test
    void detectsTruncationByReadingOneExtraRow() throws Exception {
        SqlExecutionService service = new SqlExecutionService();
        SqlExecutionRequest request = new SqlExecutionRequest(
                "SELECT 1 AS ok UNION ALL SELECT 2 AS ok", 1, 30, 200);

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SqlExecutionResult result = service.execute(connection, request);

            assertEquals(1, result.rows().size());
            assertEquals("1", result.rows().get(0).get(0));
            assertTrue(result.truncated());
        }
    }

    @Test
    void maxLimitIsCappedAtOneThousandRows() {
        SqlExecutionRequest request = SqlExecutionRequest.fromArgs("SELECT 1", Args.parse(new String[]{
                "db", "sql", "--limit", "5000", "--max-limit", "5000"
        }));

        assertEquals(1000, request.limit());
    }

    @Test
    void explainStatementsDoNotUseJdbcReadOnlyHint() {
        SqlExecutionRequest explain = SqlExecutionRequest.fromArgs("EXPLAIN SELECT 1", Args.parse(new String[]{
                "db", "sql", "--explain"
        }));
        SqlExecutionRequest select = SqlExecutionRequest.fromArgs("SELECT 1", Args.parse(new String[]{
                "db", "sql"
        }));

        assertEquals(false, explain.useJdbcReadOnlyHint());
        assertTrue(select.useJdbcReadOnlyHint());
    }
}
