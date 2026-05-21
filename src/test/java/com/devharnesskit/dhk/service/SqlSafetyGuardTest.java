package com.devharnesskit.dhk.service;

import com.devharnesskit.dhk.sql.SqlSafetyResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SqlSafetyGuardTest {
    private final SqlSafetyGuard guard = new SqlSafetyGuard();

    @Test
    void allowsReadonlyStatements() {
        assertAllowed("SELECT 1");
        assertAllowed("SHOW TABLES");
        assertAllowed("DESC t_order");
        assertAllowed("DESCRIBE t_order");
        assertAllowed("EXPLAIN SELECT * FROM t_order");
    }

    @Test
    void rejectsDangerousStatements() {
        assertRejected("UPDATE t SET a = 1");
        assertRejected("DELETE FROM t");
        assertRejected("DROP TABLE t");
        assertRejected("ALTER TABLE t ADD COLUMN x INT");
        assertRejected("SELECT * FROM t INTO OUTFILE '/tmp/a'");
        assertRejected("WITH x AS (SELECT 1) SELECT * FROM x");
        assertRejected("SELECT SLEEP(1)");
        assertRejected("SELECT LOAD_FILE('/etc/passwd')");
    }

    @Test
    void rejectsMultipleStatementsButAllowsSemicolonsInStringsAndComments() {
        assertRejected("SELECT 1; SELECT 2");
        assertAllowed("SELECT ';' AS semicolon");
        assertAllowed("SELECT 1 -- ; in comment\n");
        assertAllowed("SELECT 1 /* ; in comment */");
    }

    @Test
    void explainFlagOnlyWrapsSelect() {
        SqlSafetyResult select = guard.validate("SELECT * FROM t_order", true);
        assertTrue(select.allowed());
        assertTrue(select.executableSql().startsWith("EXPLAIN "));

        assertFalse(guard.validate("SHOW TABLES", true).allowed());
        assertFalse(guard.validate("EXPLAIN UPDATE t SET a = 1", false).allowed());
    }

    private void assertAllowed(String sql) {
        assertTrue(guard.validate(sql, false).allowed(), sql);
    }

    private void assertRejected(String sql) {
        assertFalse(guard.validate(sql, false).allowed(), sql);
    }
}
