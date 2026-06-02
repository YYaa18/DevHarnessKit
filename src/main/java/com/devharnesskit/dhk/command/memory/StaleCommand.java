package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class StaleCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final MemoryRepository memoryRepository;

    public StaleCommand() {
        this(new DbConnectionFactory(), new ProjectService(), new MemoryRepository());
    }

    StaleCommand(DbConnectionFactory connectionFactory, ProjectService projectService,
                 MemoryRepository memoryRepository) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.memoryRepository = memoryRepository;
    }

    public int run(CommandContext context, Args args) {
        String action = args.positional(2);
        if (action.length() > 0 && !"scan".equals(action)) {
            context.err().println("Unknown memory stale command: " + action);
            context.err().println("Run `dhk help` for usage.");
            return ExitCodes.USAGE_ERROR;
        }
        String module = args.option("module", "").trim();
        int limit = MemoryCommandSupport.parseLimit(context, args, args.option("limit", "100"),
                "dhk memory stale scan --limit 100");
        if (limit <= 0) {
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = MemoryCommandSupport.requireInitializedProject(context, projectRoot, projectService, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            List<MemoryItem> items = memoryRepository.listAllMemory(connection, project.projectKey(), module, limit);
            List<StaleItem> stale = staleItems(items, context.clock().now());
            if (JsonOutput.enabled(args)) {
                printJson(context, module, stale);
            } else {
                printText(context, module, stale);
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR memory stale scan failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private List<StaleItem> staleItems(List<MemoryItem> items, Instant now) {
        List<StaleItem> result = new ArrayList<StaleItem>();
        for (MemoryItem item : items) {
            String reason = staleReason(item, now);
            if (reason.length() > 0) {
                result.add(new StaleItem(item, reason));
            }
        }
        return result;
    }

    private String staleReason(MemoryItem item, Instant now) {
        if (item.supersededBy() > 0) {
            return "superseded_by=" + item.supersededBy();
        }
        if (item.staleReason().length() > 0) {
            return item.staleReason();
        }
        if ("deprecated".equals(item.status())) {
            return "deprecated";
        }
        if (item.effectiveTo().length() > 0) {
            try {
                if (Instant.parse(item.effectiveTo()).isBefore(now)) {
                    return "effective_to_elapsed";
                }
            } catch (RuntimeException ex) {
                return "invalid_effective_to";
            }
        }
        return "";
    }

    private void printText(CommandContext context, String module, List<StaleItem> stale) {
        context.out().println("memory stale scan");
        context.out().println("module: " + (module.length() == 0 ? "all" : module));
        context.out().println("stale_count: " + stale.size());
        for (StaleItem item : stale) {
            context.out().println("- memory_id: " + item.item.id());
            context.out().println("  title: " + item.item.title());
            context.out().println("  status: " + item.item.status());
            context.out().println("  reason: " + item.reason);
        }
    }

    private void printJson(CommandContext context, String module, List<StaleItem> stale) {
        List<String> rawItems = new ArrayList<String>();
        for (StaleItem item : stale) {
            rawItems.add(JsonOutput.object(
                    JsonOutput.numberField("memory_id", item.item.id()),
                    JsonOutput.stringField("title", item.item.title()),
                    JsonOutput.stringField("status", item.item.status()),
                    JsonOutput.stringField("reason", item.reason)
            ).trim());
        }
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "memory stale scan"),
                JsonOutput.stringField("module", module),
                JsonOutput.numberField("stale_count", stale.size()),
                JsonOutput.rawField("items", JsonOutput.array(rawItems))
        ));
    }

    private static final class StaleItem {
        private final MemoryItem item;
        private final String reason;

        private StaleItem(MemoryItem item, String reason) {
            this.item = item;
            this.reason = reason;
        }
    }
}
