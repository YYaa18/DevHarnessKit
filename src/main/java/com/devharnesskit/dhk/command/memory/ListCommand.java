package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.guidance.EnumGuidance;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.service.MemoryStatus;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public final class ListCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final MemoryRepository memoryRepository;

    public ListCommand() {
        this(new DbConnectionFactory(), new ProjectService(), new MemoryRepository());
    }

    ListCommand(DbConnectionFactory connectionFactory, ProjectService projectService,
                MemoryRepository memoryRepository) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.memoryRepository = memoryRepository;
    }

    public int run(CommandContext context, Args args) {
        String status = args.option("status", "").trim();
        if (status.length() > 0 && !MemoryStatus.isAllowed(status)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_MEMORY_STATUS", "memory status", status,
                    EnumGuidance.MEMORY_STATUSES, new String[0],
                    "dhk memory list --status confirmed", "README.md#core-path");
        }
        String module = args.option("module", "").trim();
        String tag = args.option("tag", "").trim();
        int limit = parseLimit(context, args.option("limit", "20"));
        if (limit <= 0) {
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = MemoryCommandSupport.requireInitializedProject(context, projectRoot,
                    projectService, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            List<MemoryItem> items = memoryRepository.listMemory(connection, project.projectKey(),
                    module, status, tag, limit);
            if (JsonOutput.enabled(args)) {
                printJson(context, status, module, tag, limit, items);
            } else {
                printText(context, status, module, tag, limit, items);
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR memory list failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printText(CommandContext context, String status, String module, String tag,
                           int limit, List<MemoryItem> items) {
        context.out().println("memory list");
        context.out().println("status: " + valueOrAll(status));
        context.out().println("module: " + valueOrAll(module));
        context.out().println("tag: " + valueOrAll(tag));
        context.out().println("limit: " + limit);
        context.out().println("total: " + items.size());
        if (items.isEmpty()) {
            context.out().println("items: none");
            return;
        }
        context.out().println("items:");
        for (MemoryItem item : items) {
            context.out().println("- memory_id: " + item.id());
            context.out().println("  title: " + item.title());
            context.out().println("  status: " + item.status());
            context.out().println("  confidence: " + item.confidence());
            context.out().println("  module: " + item.moduleName());
            context.out().println("  type: " + item.memoryType());
            context.out().println("  tags: " + item.tags());
            context.out().println("  content: " + item.summary(160));
        }
    }

    private void printJson(CommandContext context, String status, String module, String tag,
                           int limit, List<MemoryItem> items) {
        List<String> rawItems = new ArrayList<String>();
        for (MemoryItem item : items) {
            rawItems.add(JsonOutput.object(
                    JsonOutput.numberField("memory_id", item.id()),
                    JsonOutput.stringField("title", item.title()),
                    JsonOutput.stringField("status", item.status()),
                    JsonOutput.numberField("confidence", item.confidence()),
                    JsonOutput.stringField("module", item.moduleName()),
                    JsonOutput.stringField("type", item.memoryType()),
                    JsonOutput.stringField("tags", item.tags()),
                    JsonOutput.stringField("content", item.summary(160))
            ).trim());
        }
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "memory list"),
                JsonOutput.stringField("status", status),
                JsonOutput.stringField("module", module),
                JsonOutput.stringField("tag", tag),
                JsonOutput.numberField("limit", limit),
                JsonOutput.numberField("count", items.size()),
                JsonOutput.rawField("items", JsonOutput.array(rawItems))
        ));
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

    private String valueOrAll(String value) {
        return value == null || value.length() == 0 ? "all" : value;
    }
}
