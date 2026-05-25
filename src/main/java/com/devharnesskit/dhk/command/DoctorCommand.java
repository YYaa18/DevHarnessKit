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
import com.devharnesskit.dhk.util.PathUtil;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
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
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        Path memoryDir = PathUtil.memoryDirectory(projectRoot);
        Path dbPath = PathUtil.memoryDb(projectRoot);
        Path projectJson = PathUtil.projectJson(projectRoot);
        Path exportsDir = PathUtil.exportsDirectory(projectRoot);
        Path sensitivePolicy = PathUtil.sensitivePolicy(projectRoot);

        context.out().println("DevHarness Kit doctor");
        context.out().println("java.version: " + System.getProperty("java.version"));
        context.out().println("project_root: " + projectRoot);

        boolean missing = false;
        missing = printCheck(context, "memory_dir", Files.isDirectory(memoryDir), memoryDir.toString()) || missing;
        missing = printCheck(context, "memory_db", Files.isRegularFile(dbPath), dbPath.toString()) || missing;
        missing = printCheck(context, "project_json", Files.isRegularFile(projectJson), projectJson.toString()) || missing;
        missing = printCheck(context, "exports_dir", Files.isDirectory(exportsDir) && Files.isWritable(exportsDir), exportsDir.toString()) || missing;
        context.out().println("sensitive_policy: "
                + (Files.isRegularFile(sensitivePolicy) ? "configured" : "default")
                + " (" + sensitivePolicy + ")");

        boolean mysqlDriverLoaded = false;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            mysqlDriverLoaded = true;
        } catch (ClassNotFoundException ex) {
            context.err().println("ERROR mysql_driver: com.mysql.jdbc.Driver not loadable");
        }
        context.out().println("mysql_driver: " + (mysqlDriverLoaded ? "ok" : "missing"));

        if (!Files.isRegularFile(dbPath)) {
            scanExports(context, exportsDir);
            return ExitCodes.NOT_FOUND;
        }

        try (Connection connection = connectionFactory.open(projectRoot)) {
            int schemaVersion = MigrationRunner.currentSchemaVersion(connection);
            boolean schemaOk = schemaVersion >= MigrationRunner.V1;
            missing = printCheck(context, "schema_version", schemaOk, String.valueOf(schemaVersion)) || missing;

            boolean ftsAvailable = MigrationRunner.hasTable(connection, "memory_fts");
            context.out().println("fts: " + (ftsAvailable ? "available" : "fallback"));

            Project project = null;
            if (Files.isRegularFile(projectJson)) {
                project = projectService.readProject(projectJson);
            }
            boolean projectRecordOk = project != null && projectRepository.findByKey(connection, project.projectKey()) != null;
            missing = printCheck(context, "project_record", projectRecordOk,
                    project == null ? "missing" : project.projectKey()) || missing;

            if (projectRecordOk) {
                String projectKey = project.projectKey();
                context.out().println("memory_total: " + memoryRepository.countAll(connection, projectKey));
                context.out().println("memory_draft: " + memoryRepository.countByStatus(connection, projectKey, "draft"));
                context.out().println("memory_confirmed: " + memoryRepository.countByStatus(connection, projectKey, "confirmed"));
                context.out().println("checkpoint_total: " + checkpointRepository.countAll(connection, projectKey));
            }
        } catch (SQLException ex) {
            context.err().println("ERROR sqlite: " + ex.getMessage());
            scanExports(context, exportsDir);
            return ExitCodes.RUNTIME_ERROR;
        } catch (RuntimeException ex) {
            context.err().println("ERROR project: " + ex.getMessage());
            scanExports(context, exportsDir);
            return ExitCodes.RUNTIME_ERROR;
        }

        scanExports(context, exportsDir);
        if (!mysqlDriverLoaded) {
            return ExitCodes.RUNTIME_ERROR;
        }
        return missing ? ExitCodes.NOT_FOUND : ExitCodes.SUCCESS;
    }

    private boolean printCheck(CommandContext context, String name, boolean ok, String detail) {
        context.out().println(name + ": " + (ok ? "ok" : "missing") + " (" + detail + ")");
        return !ok;
    }

    private void scanExports(CommandContext context, Path exportsDir) {
        if (!Files.isDirectory(exportsDir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(exportsDir)) {
            for (Path file : stream) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                String content = sensitiveDataGuard.redact(new String(Files.readAllBytes(file), "UTF-8"));
                List<String> matches = sensitiveDataGuard.findMatches(content);
                if (!matches.isEmpty()) {
                    context.err().println("WARNING sensitive export content: " + file + " matches " + matches);
                }
            }
        } catch (IOException ex) {
            context.err().println("WARNING export scan failed: " + ex.getMessage());
        }
    }
}
