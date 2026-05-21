package com.devharnesskit.dhk.command.projectdb;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.service.MysqlConnectionService;
import com.devharnesskit.dhk.sql.DbConnectionRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public final class TestCommand implements Command {
    private final MysqlConnectionService connectionService;

    public TestCommand() {
        this(new MysqlConnectionService());
    }

    TestCommand(MysqlConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    public int run(CommandContext context, Args args) {
        DbConnectionRequest request = connectionService.fromArgs(args, false);
        if (!request.valid()) {
            context.err().println(request.error());
            return ExitCodes.USAGE_ERROR;
        }
        try (Connection connection = connectionService.open(request)) {
            connection.setReadOnly(true);
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT 1")) {
                resultSet.next();
            }
            context.out().println("db_test: ok");
            context.out().println("database_product: " + connection.getMetaData().getDatabaseProductName());
            context.out().println("database_version: " + connection.getMetaData().getDatabaseProductVersion());
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR db test failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
