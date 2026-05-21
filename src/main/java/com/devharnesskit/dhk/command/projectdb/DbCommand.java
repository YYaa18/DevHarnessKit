package com.devharnesskit.dhk.command.projectdb;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

public final class DbCommand implements Command {
    private final TestCommand testCommand;
    private final SqlCommand sqlCommand;

    public DbCommand() {
        this(new TestCommand(), new SqlCommand());
    }

    DbCommand(TestCommand testCommand, SqlCommand sqlCommand) {
        this.testCommand = testCommand;
        this.sqlCommand = sqlCommand;
    }

    public int run(CommandContext context, Args args) {
        String subCommand = args.subCommand();
        if ("test".equals(subCommand)) {
            return testCommand.run(context, args);
        }
        if ("sql".equals(subCommand)) {
            return sqlCommand.run(context, args);
        }
        context.err().println("Unknown db command: " + subCommand);
        context.err().println("Run `dhk help` for usage.");
        return ExitCodes.USAGE_ERROR;
    }
}
