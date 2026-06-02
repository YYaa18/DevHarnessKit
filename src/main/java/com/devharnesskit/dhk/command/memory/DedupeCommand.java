package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.service.MemoryIdentity;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DedupeCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final MemoryRepository memoryRepository;

    public DedupeCommand() {
        this(new DbConnectionFactory(), new ProjectService(), new MemoryRepository());
    }

    DedupeCommand(DbConnectionFactory connectionFactory, ProjectService projectService,
                  MemoryRepository memoryRepository) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.memoryRepository = memoryRepository;
    }

    public int run(CommandContext context, Args args) {
        String module = args.option("module", "").trim();
        int limit = MemoryCommandSupport.parseLimit(context, args, args.option("limit", "100"),
                "dhk memory dedupe --limit 100");
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
            List<List<MemoryItem>> groups = duplicateGroups(items);
            if (JsonOutput.enabled(args)) {
                printJson(context, module, groups);
            } else {
                printText(context, module, groups);
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR memory dedupe failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private List<List<MemoryItem>> duplicateGroups(List<MemoryItem> items) {
        Map<String, List<MemoryItem>> byFingerprint = new LinkedHashMap<String, List<MemoryItem>>();
        for (MemoryItem item : items) {
            String fingerprint = item.fingerprint().length() == 0
                    ? MemoryIdentity.fingerprint(item.title(), item.content()) : item.fingerprint();
            List<MemoryItem> group = byFingerprint.get(fingerprint);
            if (group == null) {
                group = new ArrayList<MemoryItem>();
                byFingerprint.put(fingerprint, group);
            }
            group.add(item);
        }
        List<List<MemoryItem>> result = new ArrayList<List<MemoryItem>>();
        for (List<MemoryItem> group : byFingerprint.values()) {
            if (group.size() > 1) {
                result.add(group);
            }
        }
        return result;
    }

    private void printText(CommandContext context, String module, List<List<MemoryItem>> groups) {
        context.out().println("memory dedupe");
        context.out().println("module: " + (module.length() == 0 ? "all" : module));
        context.out().println("groups: " + groups.size());
        for (List<MemoryItem> group : groups) {
            context.out().println("- fingerprint: " + group.get(0).fingerprint());
            context.out().println("  duplicate_count: " + group.size());
            context.out().println("  ids: " + ids(group));
            context.out().println("  title: " + group.get(0).title());
        }
    }

    private void printJson(CommandContext context, String module, List<List<MemoryItem>> groups) {
        List<String> rawGroups = new ArrayList<String>();
        for (List<MemoryItem> group : groups) {
            rawGroups.add(JsonOutput.object(
                    JsonOutput.stringField("fingerprint", group.get(0).fingerprint()),
                    JsonOutput.numberField("duplicate_count", group.size()),
                    JsonOutput.stringField("ids", ids(group)),
                    JsonOutput.stringField("title", group.get(0).title())
            ).trim());
        }
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "memory dedupe"),
                JsonOutput.stringField("module", module),
                JsonOutput.numberField("group_count", groups.size()),
                JsonOutput.rawField("groups", JsonOutput.array(rawGroups))
        ));
    }

    private String ids(List<MemoryItem> group) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < group.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(group.get(i).id());
        }
        return builder.toString();
    }
}
