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

    @Test
    void deterministicReadonlyFuzzAcceptsCaseWhitespaceCommentsAndTrailingSemicolon() {
        String[] bases = new String[]{
                "SELECT id, name FROM t_order WHERE status = 'PAID'",
                "select ';' as semicolon_text",
                "SHOW TABLES",
                "DESC t_order",
                "DESCRIBE t_order",
                "EXPLAIN SELECT id FROM t_order"
        };
        String[] prefixes = new String[]{
                "",
                "  \n\t",
                "-- leading comment with update keyword\n",
                "/* leading block comment with drop keyword */ "
        };
        String[] suffixes = new String[]{
                "",
                ";",
                "   ;   ",
                " -- trailing comment with delete keyword\n"
        };

        for (int i = 0; i < bases.length; i++) {
            for (int j = 0; j < prefixes.length; j++) {
                for (int k = 0; k < suffixes.length; k++) {
                    String sql = prefixes[j] + deterministicCase(bases[i], i + j + k) + suffixes[k];
                    assertAllowed(sql);
                }
            }
        }
    }

    @Test
    void deterministicMutationFuzzRejectsDangerousKeywords() {
        String[] statements = new String[]{
                "insert into t_order(id) values (1)",
                "update t_order set status = 'PAID'",
                "delete from t_order",
                "drop table t_order",
                "alter table t_order add column x int",
                "truncate table t_order",
                "create table t_order_copy(id int)",
                "grant select on demo.* to u",
                "revoke select on demo.* from u",
                "call refresh_order()",
                "set @x = 1",
                "replace into t_order(id) values (1)",
                "load data infile '/tmp/a' into table t_order"
        };
        String[] wrappers = new String[]{
                "%s",
                "  %s  ",
                "-- readonly-looking comment\n%s",
                "/* block comment */ %s ; "
        };

        for (int i = 0; i < statements.length; i++) {
            for (int j = 0; j < wrappers.length; j++) {
                String sql = String.format(wrappers[j], deterministicCase(statements[i], i + j));
                assertRejected(sql);
            }
        }
    }

    @Test
    void deterministicMultipleStatementFuzzRejectsHiddenSecondStatement() {
        String[] separators = new String[]{
                ";",
                ";   ",
                ";\n",
                "; -- comment before second statement\n",
                "; /* comment before second statement */ "
        };
        String[] seconds = new String[]{
                "SELECT 2",
                "UPDATE t_order SET status = 'PAID'",
                "DROP TABLE t_order"
        };

        for (String separator : separators) {
            for (String second : seconds) {
                assertRejected("SELECT 1" + separator + second);
            }
        }
    }

    @Test
    void deterministicSemicolonFuzzAllowsSemicolonsInsideQuotedContentAndComments() {
        assertAllowed("SELECT ';' AS semicolon_text");
        assertAllowed("SELECT \";\" AS semicolon_text");
        assertAllowed("SELECT `semi;colon` FROM t_order");
        assertAllowed("SELECT 1 -- ; hidden in line comment\n");
        assertAllowed("SELECT 1 /* ; hidden in block comment */");
    }

    @Test
    void deterministicRiskPatternFuzzRejectsWhitespaceVariants() {
        assertRejected("SELECT * FROM t_order INTO   OUTFILE '/tmp/orders.txt'");
        assertRejected("SELECT * FROM t_order INTO\nDUMPFILE '/tmp/orders.bin'");
        assertRejected("SELECT SLEEP (1)");
        assertRejected("SELECT LOAD_FILE ('/etc/passwd')");
        assertRejected("EXPLAIN SELECT SLEEP (1)");
    }

    @Test
    void quotedDangerousWordsAreNotTreatedAsSqlOperations() {
        assertAllowed("SELECT 'drop update delete' AS text_value");
        assertAllowed("SELECT \"insert replace load\" AS text_value");
        assertAllowed("SELECT `drop` FROM t_order");
    }

    @Test
    void ambiguousQuotesOrCommentsFailClosed() {
        assertRejected("SELECT 'unterminated");
        assertRejected("SELECT \"unterminated");
        assertRejected("SELECT `unterminated");
        assertRejected("SELECT 1 /* unterminated");
    }

    @Test
    void explainFlagFuzzOnlyWrapsPlainSelect() {
        assertTrue(guard.validate("  SELECT id FROM t_order ; ", true).allowed());
        assertFalse(guard.validate("SHOW TABLES", true).allowed());
        assertFalse(guard.validate("DESC t_order", true).allowed());
        assertFalse(guard.validate("EXPLAIN SELECT id FROM t_order", true).allowed());
        assertFalse(guard.validate("UPDATE t_order SET status = 'PAID'", true).allowed());
    }

    private void assertAllowed(String sql) {
        assertTrue(guard.validate(sql, false).allowed(), sql);
    }

    private void assertRejected(String sql) {
        assertFalse(guard.validate(sql, false).allowed(), sql);
    }

    private String deterministicCase(String sql, int seed) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (!Character.isLetter(ch)) {
                builder.append(ch);
            } else if (((i + seed) % 2) == 0) {
                builder.append(Character.toUpperCase(ch));
            } else {
                builder.append(Character.toLowerCase(ch));
            }
        }
        return builder.toString();
    }
}
