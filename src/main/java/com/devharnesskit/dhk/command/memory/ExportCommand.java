package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.export.CurrentContextRenderer;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.CheckpointRepository;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class ExportCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final MemoryRepository memoryRepository;
    private final CheckpointRepository checkpointRepository;
    private final CurrentContextRenderer renderer;
    private final SensitiveDataGuard sensitiveDataGuard;

    public ExportCommand() {
        this(new DbConnectionFactory(), new ProjectService(), new MemoryRepository(),
                new CheckpointRepository(), new CurrentContextRenderer(), new SensitiveDataGuard());
    }

    ExportCommand(DbConnectionFactory connectionFactory, ProjectService projectService,
                  MemoryRepository memoryRepository, CheckpointRepository checkpointRepository,
                  CurrentContextRenderer renderer, SensitiveDataGuard sensitiveDataGuard) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.memoryRepository = memoryRepository;
        this.checkpointRepository = checkpointRepository;
        this.renderer = renderer;
        this.sensitiveDataGuard = sensitiveDataGuard;
    }

    public int run(CommandContext context, Args args) {
        String task = args.option("task").trim();
        if (task.length() == 0) {
            context.err().println("Missing required parameter: --task");
            return ExitCodes.USAGE_ERROR;
        }
        String module = args.option("module", "global").trim();
        if (module.length() == 0) {
            module = "global";
        }
        int limit = parseLimit(context, args.option("limit", "30"));
        if (limit <= 0) {
            return ExitCodes.VALIDATION_ERROR;
        }

        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        Path out = args.hasOption("out")
                ? PathUtil.resolvePath(args.option("out"), context.workingDirectory())
                : PathUtil.currentContext(projectRoot);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = MemoryCommandSupport.requireInitializedProject(context, projectRoot, projectService, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            List<MemoryItem> candidates = memoryRepository.listConfirmedForExport(connection, project.projectKey(), module, limit * 4);
            List<MemoryItem> exportItems = filterSensitive(candidates, limit);
            String markdown = renderer.render(project, task, module, args.option("mode", "auto"),
                    args.option("keywords", ""), context.clock().now().toString(),
                    exportItems, checkpointRepository.latest(connection, project.projectKey(), ""));
            List<String> matches = sensitiveDataGuard.findMatches(markdown);
            if (!matches.isEmpty()) {
                context.err().println("Sensitive data rejected during export: " + matches);
                return ExitCodes.VALIDATION_ERROR;
            }
            Files.createDirectories(out.getParent());
            Files.write(out, markdown.getBytes("UTF-8"));
            memoryRepository.markUsed(connection, project.projectKey(), ids(exportItems), context.clock().now().toString());
            context.out().println("export_path: " + out);
            context.out().println("memory_exported: " + exportItems.size());
            return ExitCodes.SUCCESS;
        } catch (SQLException ex) {
            context.err().println("ERROR memory export failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR memory export failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private List<MemoryItem> filterSensitive(List<MemoryItem> candidates, int limit) {
        List<MemoryItem> filtered = new ArrayList<MemoryItem>();
        for (MemoryItem item : candidates) {
            if (sensitiveDataGuard.containsSensitiveData(item.title() + "\n" + item.content() + "\n" + item.tags())) {
                continue;
            }
            filtered.add(item);
            if (filtered.size() >= limit) {
                break;
            }
        }
        return filtered;
    }

    private List<Long> ids(List<MemoryItem> items) {
        List<Long> ids = new ArrayList<Long>();
        for (MemoryItem item : items) {
            ids.add(Long.valueOf(item.id()));
        }
        return ids;
    }

    private int parseLimit(CommandContext context, String rawValue) {
        try {
            int value = Integer.parseInt(rawValue);
            if (value < 1) {
                context.err().println("Invalid limit, expected positive integer: " + rawValue);
                return -1;
            }
            return Math.min(value, 100);
        } catch (NumberFormatException ex) {
            context.err().println("Invalid limit, expected positive integer: " + rawValue);
            return -1;
        }
    }
}
