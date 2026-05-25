package com.devharnesskit.dhk.command;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.CheckpointRepository;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class DoctorCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final MemoryRepository memoryRepository;
    private final CheckpointRepository checkpointRepository;
    private final SensitiveDataGuard sensitiveDataGuard;

    public DoctorCommand() {
        this(new DbConnectionFactory(), new ProjectService(), new ProjectRepository(),
                new MemoryRepository(), new CheckpointRepository(), new SensitiveDataGuard());
    }

    DoctorCommand(DbConnectionFactory connectionFactory, ProjectService projectService,
                  ProjectRepository projectRepository, MemoryRepository memoryRepository,
                  CheckpointRepository checkpointRepository, SensitiveDataGuard sensitiveDataGuard) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.memoryRepository = memoryRepository;
        this.checkpointRepository = checkpointRepository;
        this.sensitiveDataGuard = sensitiveDataGuard;
    }

    public int run(CommandContext context, Args args) {
        boolean json = JsonOutput.enabled(args);
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        Path memoryDir = PathUtil.memoryDirectory(projectRoot);
        Path dbPath = PathUtil.memoryDb(projectRoot);
        Path projectJson = PathUtil.projectJson(projectRoot);
        Path exportsDir = PathUtil.exportsDirectory(projectRoot);
        Path sensitivePolicy = PathUtil.sensitivePolicy(projectRoot);

        if (!json) {
            context.out().println("DevHarness Kit doctor");
            context.out().println("java.version: " + System.getProperty("java.version"));
            context.out().println("project_root: " + projectRoot);
        }

        boolean missing = false;
        boolean memoryDirOk = Files.isDirectory(memoryDir);
        boolean memoryDbOk = Files.isRegularFile(dbPath);
        boolean projectJsonOk = Files.isRegularFile(projectJson);
        boolean exportsDirOk = Files.isDirectory(exportsDir) && Files.isWritable(exportsDir);
        missing = printCheck(context, json, "memory_dir", memoryDirOk, memoryDir.toString()) || missing;
        missing = printCheck(context, json, "memory_db", memoryDbOk, dbPath.toString()) || missing;
        missing = printCheck(context, json, "project_json", projectJsonOk, projectJson.toString()) || missing;
        missing = printCheck(context, json, "exports_dir", exportsDirOk, exportsDir.toString()) || missing;
        String sensitivePolicyStatus = Files.isRegularFile(sensitivePolicy) ? "configured" : "default";
        if (!json) {
            context.out().println("sensitive_policy: " + sensitivePolicyStatus + " (" + sensitivePolicy + ")");
        }

        boolean mysqlDriverLoaded = false;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            mysqlDriverLoaded = true;
        } catch (ClassNotFoundException ex) {
            if (!json) {
                context.err().println("ERROR mysql_driver: com.mysql.jdbc.Driver not loadable");
            }
        }
        if (!json) {
            context.out().println("mysql_driver: " + (mysqlDriverLoaded ? "ok" : "missing"));
        }

        int schemaVersion = 0;
        String fts = "unknown";
        String projectRecord = "missing";
        long memoryTotal = -1L;
        long memoryDraft = -1L;
        long memoryConfirmed = -1L;
        long checkpointTotal = -1L;
        List<String> exportWarnings;
        if (!Files.isRegularFile(dbPath)) {
            exportWarnings = scanExports(context, exportsDir, json);
            if (json) {
                printJson(context, projectRoot, memoryDirOk, memoryDbOk, projectJsonOk, exportsDirOk,
                        sensitivePolicyStatus, mysqlDriverLoaded, schemaVersion, fts, projectRecord,
                        memoryTotal, memoryDraft, memoryConfirmed, checkpointTotal, exportWarnings);
            }
            return ExitCodes.NOT_FOUND;
        }

        try (Connection connection = connectionFactory.open(projectRoot)) {
            schemaVersion = MigrationRunner.currentSchemaVersion(connection);
            boolean schemaOk = schemaVersion >= MigrationRunner.V1;
            missing = printCheck(context, json, "schema_version", schemaOk, String.valueOf(schemaVersion)) || missing;

            boolean ftsAvailable = MigrationRunner.hasTable(connection, "memory_fts");
            fts = ftsAvailable ? "available" : "fallback";
            if (!json) {
                context.out().println("fts: " + fts);
            }

            Project project = null;
            if (Files.isRegularFile(projectJson)) {
                project = projectService.readProject(projectJson);
            }
            boolean projectRecordOk = project != null && projectRepository.findByKey(connection, project.projectKey()) != null;
            projectRecord = project == null ? "missing" : project.projectKey();
            missing = printCheck(context, json, "project_record", projectRecordOk,
                    project == null ? "missing" : project.projectKey()) || missing;

            if (projectRecordOk) {
                String projectKey = project.projectKey();
                memoryTotal = memoryRepository.countAll(connection, projectKey);
                memoryDraft = memoryRepository.countByStatus(connection, projectKey, "draft");
                memoryConfirmed = memoryRepository.countByStatus(connection, projectKey, "confirmed");
                checkpointTotal = checkpointRepository.countAll(connection, projectKey);
                if (!json) {
                    context.out().println("memory_total: " + memoryTotal);
                    context.out().println("memory_draft: " + memoryDraft);
                    context.out().println("memory_confirmed: " + memoryConfirmed);
                    context.out().println("checkpoint_total: " + checkpointTotal);
                }
            }
        } catch (SQLException ex) {
            if (!json) {
                context.err().println("ERROR sqlite: " + ex.getMessage());
            }
            exportWarnings = scanExports(context, exportsDir, json);
            if (json) {
                printJson(context, projectRoot, memoryDirOk, memoryDbOk, projectJsonOk, exportsDirOk,
                        sensitivePolicyStatus, mysqlDriverLoaded, schemaVersion, fts, projectRecord,
                        memoryTotal, memoryDraft, memoryConfirmed, checkpointTotal, exportWarnings);
            }
            return ExitCodes.RUNTIME_ERROR;
        } catch (RuntimeException ex) {
            if (!json) {
                context.err().println("ERROR project: " + ex.getMessage());
            }
            exportWarnings = scanExports(context, exportsDir, json);
            if (json) {
                printJson(context, projectRoot, memoryDirOk, memoryDbOk, projectJsonOk, exportsDirOk,
                        sensitivePolicyStatus, mysqlDriverLoaded, schemaVersion, fts, projectRecord,
                        memoryTotal, memoryDraft, memoryConfirmed, checkpointTotal, exportWarnings);
            }
            return ExitCodes.RUNTIME_ERROR;
        }

        exportWarnings = scanExports(context, exportsDir, json);
        if (json) {
            printJson(context, projectRoot, memoryDirOk, memoryDbOk, projectJsonOk, exportsDirOk,
                    sensitivePolicyStatus, mysqlDriverLoaded, schemaVersion, fts, projectRecord,
                    memoryTotal, memoryDraft, memoryConfirmed, checkpointTotal, exportWarnings);
        }
        if (!mysqlDriverLoaded) {
            return ExitCodes.RUNTIME_ERROR;
        }
        return missing ? ExitCodes.NOT_FOUND : ExitCodes.SUCCESS;
    }

    private boolean printCheck(CommandContext context, boolean json, String name, boolean ok, String detail) {
        if (!json) {
            context.out().println(name + ": " + (ok ? "ok" : "missing") + " (" + detail + ")");
        }
        return !ok;
    }

    private List<String> scanExports(CommandContext context, Path exportsDir, boolean json) {
        List<String> warnings = new ArrayList<String>();
        if (!Files.isDirectory(exportsDir)) {
            return warnings;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(exportsDir)) {
            for (Path file : stream) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                String content = sensitiveDataGuard.redact(new String(Files.readAllBytes(file), "UTF-8"));
                List<String> matches = sensitiveDataGuard.findMatches(content);
                if (!matches.isEmpty()) {
                    String warning = file + " matches " + matches;
                    warnings.add(warning);
                    if (!json) {
                        context.err().println("WARNING sensitive export content: " + warning);
                    }
                }
            }
        } catch (IOException ex) {
            warnings.add("export scan failed: " + ex.getMessage());
            if (!json) {
                context.err().println("WARNING export scan failed: " + ex.getMessage());
            }
        }
        return warnings;
    }

    private void printJson(CommandContext context, Path projectRoot, boolean memoryDirOk, boolean memoryDbOk,
                           boolean projectJsonOk, boolean exportsDirOk, String sensitivePolicyStatus,
                           boolean mysqlDriverLoaded, int schemaVersion, String fts, String projectRecord,
                           long memoryTotal, long memoryDraft, long memoryConfirmed, long checkpointTotal,
                           List<String> exportWarnings) {
        List<String> warningJson = new ArrayList<String>();
        for (String warning : exportWarnings) {
            warningJson.add(JsonOutput.quote(warning));
        }
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "doctor"),
                JsonOutput.stringField("java_version", System.getProperty("java.version")),
                JsonOutput.stringField("project_root", projectRoot.toString()),
                JsonOutput.booleanField("memory_dir_ok", memoryDirOk),
                JsonOutput.booleanField("memory_db_ok", memoryDbOk),
                JsonOutput.booleanField("project_json_ok", projectJsonOk),
                JsonOutput.booleanField("exports_dir_ok", exportsDirOk),
                JsonOutput.stringField("sensitive_policy", sensitivePolicyStatus),
                JsonOutput.booleanField("mysql_driver_loaded", mysqlDriverLoaded),
                JsonOutput.numberField("schema_version", schemaVersion),
                JsonOutput.stringField("fts", fts),
                JsonOutput.stringField("project_record", projectRecord),
                JsonOutput.numberField("memory_total", memoryTotal),
                JsonOutput.numberField("memory_draft", memoryDraft),
                JsonOutput.numberField("memory_confirmed", memoryConfirmed),
                JsonOutput.numberField("checkpoint_total", checkpointTotal),
                JsonOutput.rawField("export_warnings", JsonOutput.array(warningJson))
        ));
    }
}
