package com.devharnesskit.dhk.service;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.sql.DbConnectionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MysqlConnectionServiceTest {
    @Test
    void buildsConservativeMysql51CompatibleUrlByDefault() {
        DbConnectionRequest request = new MysqlConnectionService().fromArgs(Args.parse(new String[]{
                "db", "test",
                "--host", "127.0.0.1",
                "--database", "demo"
        }), true);

        assertTrue(request.valid());
        assertEquals("jdbc:mysql://127.0.0.1:3306/demo"
                + "?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull",
                request.jdbcUrl());
    }

    @Test
    void appendsMysql8LocalCompatibilityParametersWhenRequested() {
        DbConnectionRequest request = new MysqlConnectionService().fromArgs(Args.parse(new String[]{
                "db", "test",
                "--host", "localhost",
                "--port", "3307",
                "--database", "demo",
                "--server-timezone", "Asia/Shanghai",
                "--use-ssl", "false",
                "--allow-public-key-retrieval",
                "--jdbc-params", "tinyInt1isBit=false"
        }), true);

        assertTrue(request.valid());
        assertEquals("jdbc:mysql://localhost:3307/demo"
                + "?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false"
                + "&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&tinyInt1isBit=false",
                request.jdbcUrl());
    }

    @Test
    void explicitJdbcUrlIsPreserved() {
        DbConnectionRequest request = new MysqlConnectionService().fromArgs(Args.parse(new String[]{
                "db", "test",
                "--jdbc-url", "jdbc:mysql://db:3306/demo?useSSL=false",
                "--server-timezone", "Asia/Shanghai"
        }), true);

        assertTrue(request.valid());
        assertEquals("jdbc:mysql://db:3306/demo?useSSL=false", request.jdbcUrl());
    }
}
