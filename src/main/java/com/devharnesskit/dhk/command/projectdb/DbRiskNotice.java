package com.devharnesskit.dhk.command.projectdb;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.util.JsonOutput;

final class DbRiskNotice {
    private static final String MESSAGE = "WARNING db_readonly_risk: SQL guard and JDBC read-only mode are not "
            + "database permission boundaries; use a database account with read-only privileges.";

    private DbRiskNotice() {
    }

    static void print(CommandContext context, Args args) {
        if (JsonOutput.enabled(args) || args.hasFlag("i-understand-db-readonly-risk")) {
            return;
        }
        context.err().println(MESSAGE);
    }

    static String text() {
        return "SQL guard and JDBC read-only mode are not database permission boundaries; "
                + "use a database account with read-only privileges.";
    }
}
