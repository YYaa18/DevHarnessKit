package com.devharnesskit.dhk.command.projectdb;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.export.SqlResultRenderer;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.service.MysqlConnectionService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.SqlExecutionService;
import com.devharnesskit.dhk.service.SqlSafetyGuard;
import com.devharnesskit.dhk.service.policy.PolicyHookService;
import com.devharnesskit.dhk.service.policy.PolicyViolationException;
import com.devharnesskit.dhk.sql.DbConnectionRequest;
import com.devharnesskit.dhk.sql.SqlExecutionRequest;
import com.devharnesskit.dhk.sql.SqlExecutionResult;
import com.devharnesskit.dhk.sql.SqlSafetyResult;
import com.devharnesskit.dhk.util.InputUtil;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;

public final class SqlCommand implements Command {
    private final MysqlConnectionService connectionService;
    private final SqlSafetyGuard safetyGuard;
    private final SqlExecutionService executionService;
    private final SqlResultRenderer renderer;
    private final SensitiveDataGuard sensitiveDataGuard;
    private final PolicyHookService policyHookService;

    public SqlCommand() {
        this(new MysqlConnectionService(), new SqlSafetyGuard(), new SqlExecutionService(),
                new SqlResultRenderer(), new SensitiveDataGuard(), new PolicyHookService());
    }

    SqlCommand(MysqlConnectionService connectionService, SqlSafetyGuard safetyGuard,
               SqlExecutionService executionService, SqlResultRenderer renderer,
               SensitiveDataGuard sensitiveDataGuard, PolicyHookService policyHookService) {
        this.connectionService = connectionService;
        this.safetyGuard = safetyGuard;
        this.executionService = executionService;
        this.renderer = renderer;
        this.sensitiveDataGuard = sensitiveDataGuard;
        this.policyHookService = policyHookService;
    }

    public int run(CommandContext context, Args args) {
        boolean json = JsonOutput.enabled(args);
        boolean jsonFlag = args.hasFlag("json");
        boolean explain = args.hasFlag("explain");
        boolean dryRun = args.hasFlag("dry-run");
        String sql;
        try {
            sql = InputUtil.readExclusiveText(context, args, "sql", "sql-file", "sql-stdin", false).trim();
        } catch (InputUtil.InputException ex) {
            if (json) {
                writeJsonError(context, ex.getMessage(), dryRun);
                return ExitCodes.USAGE_ERROR;
            }
            return CommandErrorGuidance.invalidUsage(context, args, "DB_SQL_INPUT_CONFLICT",
                    ex.getMessage(), "Provide SQL through only one input source.",
                    "dhk db sql --sql \"select 1\"", "docs/DB_READONLY_THREAT_MODEL.md");
        }
        if (sql.length() == 0) {
            String message = "Missing SQL text: use --sql, --sql-file, or --sql-stdin";
            if (json) {
                writeJsonError(context, message, dryRun);
                return ExitCodes.USAGE_ERROR;
            }
            return CommandErrorGuidance.missing(context, args, "DB_SQL_TEXT_MISSING",
                    new String[]{"--sql|--sql-file|--sql-stdin"},
                    "dhk db sql --sql \"select 1\"", "docs/DB_READONLY_THREAT_MODEL.md");
        }
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        SqlSafetyResult safety = safetyGuard.validate(sql, explain);
        if (!safety.allowed()) {
            if (json) {
                writeJsonRejection(context, dryRun, safety.reason());
            } else {
                context.err().println("SQL rejected: " + safety.reason());
            }
            return ExitCodes.VALIDATION_ERROR;
        }
        try {
            policyHookService.requireDbSqlAllowed(projectRoot, args, dryRun);
        } catch (PolicyViolationException ex) {
            if (json) {
                writeJsonRejection(context, dryRun, message(ex));
                return ExitCodes.VALIDATION_ERROR;
            }
            context.err().println(ex.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        }
        if (dryRun) {
            if (json) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "db sql"),
                        JsonOutput.stringField("status", "ok"),
                        JsonOutput.booleanField("dry_run", true),
                        JsonOutput.booleanField("explain", explain),
                        JsonOutput.stringField("sql", sensitiveDataGuard.redact(safety.executableSql()))
                ));
            } else {
                context.out().println("sql_safety: ok");
                context.out().println("sql: " + sensitiveDataGuard.redact(safety.executableSql()));
            }
            return ExitCodes.SUCCESS;
        }

        DbConnectionRequest connectionRequest = connectionService.fromArgs(args, false);
        if (!connectionRequest.valid()) {
            if (json) {
                writeJsonError(context, connectionRequest.error(), false);
                return ExitCodes.USAGE_ERROR;
            }
            context.err().println(connectionRequest.error());
            return ExitCodes.USAGE_ERROR;
        }
        DbRiskNotice.print(context, args);
        SqlExecutionRequest executionRequest = SqlExecutionRequest.fromArgs(safety.executableSql(), args);
        try {
            SqlExecutionResult result = executionService.execute(connectionService, connectionRequest, executionRequest);
            String format = jsonFlag && !args.hasOption("format") ? "json" : args.option("format", "table");
            String output = renderer.render(result, format, executionRequest.maxOutputBytes());
            output = sensitiveDataGuard.redact(output);
            if (sensitiveDataGuard.containsSensitiveData(output)) {
                if (json) {
                    writeJsonRejection(context, false, "Sensitive SQL result rejected; output was not written.");
                    return ExitCodes.VALIDATION_ERROR;
                }
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
            if (!"json".equals(format)) {
                context.out().println("rows: " + result.rows().size());
                context.out().println("truncated: " + result.truncated());
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            String hint = connectionService.compatibilityHint(ex);
            if (json) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "db sql"),
                        JsonOutput.stringField("status", "error"),
                        JsonOutput.booleanField("dry_run", false),
                        JsonOutput.stringField("error", message(ex)),
                        JsonOutput.stringField("compatibility_hint", hint),
                        JsonOutput.stringField("risk_warning", DbRiskNotice.text())
                ));
            } else {
                context.err().println("ERROR db sql failed: " + message(ex));
                if (hint.length() > 0) {
                    context.err().println("compatibility_hint: " + hint);
                }
            }
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void writeJsonError(CommandContext context, String error, boolean dryRun) {
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "db sql"),
                JsonOutput.stringField("status", "error"),
                JsonOutput.booleanField("dry_run", dryRun),
                JsonOutput.stringField("error", error),
                JsonOutput.stringField("compatibility_hint", ""),
                JsonOutput.stringField("risk_warning", DbRiskNotice.text())
        ));
    }

    private void writeJsonRejection(CommandContext context, boolean dryRun, String reason) {
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "db sql"),
                JsonOutput.stringField("status", "rejected"),
                JsonOutput.booleanField("dry_run", dryRun),
                JsonOutput.stringField("reason", reason),
                JsonOutput.stringField("risk_warning", DbRiskNotice.text())
        ));
    }

    private String message(Exception ex) {
        String message = ex.getMessage();
        if (message == null) {
            message = ex.getClass().getSimpleName();
        }
        return message.replaceAll("(?i)jdbc:mysql://\\S+", "[REDACTED_JDBC_URL]");
    }
}
