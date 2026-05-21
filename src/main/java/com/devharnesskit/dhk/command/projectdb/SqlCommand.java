package com.devharnesskit.dhk.command.projectdb;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.export.SqlResultRenderer;
import com.devharnesskit.dhk.service.MysqlConnectionService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.SqlExecutionService;
import com.devharnesskit.dhk.service.SqlSafetyGuard;
import com.devharnesskit.dhk.sql.DbConnectionRequest;
import com.devharnesskit.dhk.sql.SqlExecutionRequest;
import com.devharnesskit.dhk.sql.SqlExecutionResult;
import com.devharnesskit.dhk.sql.SqlSafetyResult;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;

public final class SqlCommand implements Command {
    private final MysqlConnectionService connectionService;
    private final SqlSafetyGuard safetyGuard;
    private final SqlExecutionService executionService;
    private final SqlResultRenderer renderer;
    private final SensitiveDataGuard sensitiveDataGuard;

    public SqlCommand() {
        this(new MysqlConnectionService(), new SqlSafetyGuard(), new SqlExecutionService(),
                new SqlResultRenderer(), new SensitiveDataGuard());
    }

    SqlCommand(MysqlConnectionService connectionService, SqlSafetyGuard safetyGuard,
               SqlExecutionService executionService, SqlResultRenderer renderer,
               SensitiveDataGuard sensitiveDataGuard) {
        this.connectionService = connectionService;
        this.safetyGuard = safetyGuard;
        this.executionService = executionService;
        this.renderer = renderer;
        this.sensitiveDataGuard = sensitiveDataGuard;
    }

    public int run(CommandContext context, Args args) {
        String sql = args.option("sql").trim();
        if (sql.length() == 0) {
            context.err().println("Missing required parameter: --sql");
            return ExitCodes.USAGE_ERROR;
        }
        boolean explain = args.hasFlag("explain");
        boolean dryRun = args.hasFlag("dry-run");
        SqlSafetyResult safety = safetyGuard.validate(sql, explain);
        if (!safety.allowed()) {
            context.err().println("SQL rejected: " + safety.reason());
            return ExitCodes.VALIDATION_ERROR;
        }
        if (dryRun) {
            context.out().println("sql_safety: ok");
            context.out().println("sql: " + safety.executableSql());
            return ExitCodes.SUCCESS;
        }

        DbConnectionRequest connectionRequest = connectionService.fromArgs(args, false);
        if (!connectionRequest.valid()) {
            context.err().println(connectionRequest.error());
            return ExitCodes.USAGE_ERROR;
        }
        SqlExecutionRequest executionRequest = SqlExecutionRequest.fromArgs(safety.executableSql(), args);
        try {
            SqlExecutionResult result = executionService.execute(connectionService, connectionRequest, executionRequest);
            String format = args.option("format", "table");
            String output = renderer.render(result, format, executionRequest.maxOutputBytes());
            if (sensitiveDataGuard.containsSensitiveData(output)) {
                context.err().println("Sensitive SQL result rejected; output was not written.");
                return ExitCodes.VALIDATION_ERROR;
            }
            if (args.hasOption("out")) {
                Path out = PathUtil.resolvePath(args.option("out"), context.workingDirectory());
                Files.createDirectories(out.getParent());
                Files.write(out, output.getBytes("UTF-8"));
                context.out().println("sql_result_path: " + out);
            } else {
                context.out().print(output);
            }
            context.out().println("rows: " + result.rows().size());
            context.out().println("truncated: " + result.truncated());
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR db sql failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
