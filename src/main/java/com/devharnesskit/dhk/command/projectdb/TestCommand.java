package com.devharnesskit.dhk.command.projectdb;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.service.DbCompatibilityProbeService;
import com.devharnesskit.dhk.service.MysqlConnectionService;
import com.devharnesskit.dhk.sql.DbConnectionRequest;

import java.sql.Connection;
import java.util.List;

public final class TestCommand implements Command {
    private final MysqlConnectionService connectionService;
    private final DbCompatibilityProbeService probeService;

    public TestCommand() {
        this(new MysqlConnectionService(), new DbCompatibilityProbeService());
    }

    TestCommand(MysqlConnectionService connectionService, DbCompatibilityProbeService probeService) {
        this.connectionService = connectionService;
        this.probeService = probeService;
    }

    public int run(CommandContext context, Args args) {
        DbConnectionRequest request = connectionService.fromArgs(args, false);
        if (!request.valid()) {
            context.err().println(request.error());
            return ExitCodes.USAGE_ERROR;
        }
        try (Connection connection = connectionService.open(request)) {
            connection.setReadOnly(true);
            List<String> probeLines = probeService.probe(connection, true);
            boolean selectProbeOk = containsLine(probeLines, "probe_select_1: ok");
            context.out().println("db_test: " + (selectProbeOk ? "ok" : "failed"));
            for (String line : probeLines) {
                context.out().println(line);
            }
            if (!selectProbeOk) {
                context.err().println("ERROR db test failed: SELECT 1 probe did not succeed");
                return ExitCodes.RUNTIME_ERROR;
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR db test failed: " + ex.getMessage());
            String hint = connectionService.compatibilityHint(ex);
            if (hint.length() > 0) {
                context.err().println("compatibility_hint: " + hint);
            }
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private boolean containsLine(List<String> lines, String expected) {
        for (String line : lines) {
            if (expected.equals(line)) {
                return true;
            }
        }
        return false;
    }
}
