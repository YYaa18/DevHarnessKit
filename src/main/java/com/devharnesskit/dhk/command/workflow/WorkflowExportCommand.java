package com.devharnesskit.dhk.command.workflow;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.export.WorkflowContextRenderer;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowArtifactRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowCheckpointBindingRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowEventRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowMemoryBindingRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseTemplateRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.workflow.WorkflowArtifactService;
import com.devharnesskit.dhk.service.workflow.WorkflowExportService;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

public final class WorkflowExportCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final WorkflowRunRepository runRepository = new WorkflowRunRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final WorkflowExportService exportService = new WorkflowExportService(
            new WorkflowPhaseRunRepository(), new WorkflowGateRunRepository(),
            new WorkflowPhaseTemplateRepository(), new WorkflowContextRenderer());
    private final WorkflowArtifactService artifactService = new WorkflowArtifactService(
            new WorkflowArtifactRepository(), new WorkflowMemoryBindingRepository(),
            new WorkflowCheckpointBindingRepository(), new WorkflowEventRepository());

    public int run(CommandContext context, Args args) {
        String runKey = args.option("run").trim();
        if (runKey.length() == 0) {
            context.err().println("Missing required parameter: --run");
            return ExitCodes.USAGE_ERROR;
        }
        Path projectRoot = WorkflowCommandSupport.projectRoot(args, context);
        Path out = args.hasOption("out")
                ? PathUtil.resolvePath(args.option("out"), context.workingDirectory())
                : PathUtil.workflowContext(projectRoot);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            migrationRunner.migrate(connection, context.clock());
            WorkflowRun run = runRepository.findByKey(connection, runKey);
            if (run == null) {
                context.err().println("Workflow run not found: " + runKey);
                return ExitCodes.NOT_FOUND;
            }
            String markdown = exportService.render(connection, run, context.clock().now().toString());
            markdown = sensitiveDataGuard.redact(markdown);
            if (WorkflowCommandSupport.rejectSensitive(context, sensitiveDataGuard,
                    "workflow export", markdown)) {
                return ExitCodes.VALIDATION_ERROR;
            }
            Files.createDirectories(out.getParent());
            Files.write(out, markdown.getBytes("UTF-8"));
            runRepository.updateContextExportPath(connection, run.runKey(), out.toString(), context.clock().now().toString());
            artifactService.recordArtifact(connection, run, "workflow_context", "WORKFLOW_CONTEXT.md",
                    out.toString(), markdown, "Workflow context exported", context.clock().now().toString());
            context.out().println("workflow_context_path: " + out);
            context.out().println("run_key: " + run.runKey());
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR workflow export failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
