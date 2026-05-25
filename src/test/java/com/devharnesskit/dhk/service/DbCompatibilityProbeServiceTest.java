package com.devharnesskit.dhk.service;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DbCompatibilityProbeServiceTest {
    @Test
    void probeReportsMetadataAndSafeFailures() throws Exception {
        DbCompatibilityProbeService service = new DbCompatibilityProbeService();

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            List<String> lines = service.probe(connection, true);
            String output = join(lines);

            assertTrue(output.contains("database_product:"));
            assertTrue(output.contains("driver_name:"));
            assertTrue(output.contains("readonly_requested: true"));
            assertTrue(output.contains("probe_select_1: ok"));
            assertTrue(output.contains("probe_explain_select_1: ok"));
            assertTrue(output.contains("probe_show_tables: fail:"));
            assertTrue(output.contains("server_sql_mode: unknown:"));
        }
    }

    private String join(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line).append('\n');
        }
        return builder.toString();
    }
}
