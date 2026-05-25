package com.devharnesskit.dhk.command.projectdb;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.service.DbCompatibilityProbeService;
import com.devharnesskit.dhk.service.MysqlConnectionService;
import com.devharnesskit.dhk.sql.DbConnectionRequest;
import com.devharnesskit.dhk.util.JsonOutput;

import java.sql.Connection;
import java.util.ArrayList;
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
        boolean json = args.hasFlag("json");
        DbConnectionRequest request = connectionService.fromArgs(args, false);
        if (!request.valid()) {
            context.err().println(request.error());
            return ExitCodes.USAGE_ERROR;
        }
        DbRiskNotice.print(context, args);
        try (Connection connection = connectionService.open(request)) {
            connection.setReadOnly(true);
            List<String> probeLines = probeService.probe(connection, true);
            boolean selectProbeOk = containsLine(probeLines, "probe_select_1: ok");
            if (json) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "db test"),
                        JsonOutput.stringField("status", selectProbeOk ? "ok" : "failed"),
                        JsonOutput.booleanField("probe_select_1_ok", selectProbeOk),
                        JsonOutput.rawField("probes", stringArray(probeLines)),
                        JsonOutput.stringField("risk_warning", DbRiskNotice.text())
                ));
            } else {
                context.out().println("db_test: " + (selectProbeOk ? "ok" : "failed"));
                for (String line : probeLines) {
                    context.out().println(line);
                }
            }
            if (!selectProbeOk) {
                if (!json) {
                    context.err().println("ERROR db test failed: SELECT 1 probe did not succeed");
                }
                return ExitCodes.RUNTIME_ERROR;
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            String hint = connectionService.compatibilityHint(ex);
            if (json) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "db test"),
                        JsonOutput.stringField("status", "error"),
                        JsonOutput.stringField("error", message(ex)),
                        JsonOutput.stringField("compatibility_hint", hint),
                        JsonOutput.stringField("risk_warning", DbRiskNotice.text())
                ));
            } else {
                context.err().println("ERROR db test failed: " + message(ex));
                if (hint.length() > 0) {
                    context.err().println("compatibility_hint: " + hint);
                }
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

    private String stringArray(List<String> values) {
        List<String> quoted = new ArrayList<String>();
        for (String value : values) {
            quoted.add(JsonOutput.quote(value));
        }
        return JsonOutput.array(quoted);
    }

    private String message(Exception ex) {
        String message = ex.getMessage();
        if (message == null) {
            message = ex.getClass().getSimpleName();
        }
        return message.replaceAll("(?i)jdbc:mysql://\\S+", "[REDACTED_JDBC_URL]");
    }

}
