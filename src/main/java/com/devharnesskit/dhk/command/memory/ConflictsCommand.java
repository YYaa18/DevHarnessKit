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
import java.util.Locale;
import java.util.Map;

public final class ConflictsCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final MemoryRepository memoryRepository;

    public ConflictsCommand() {
        this(new DbConnectionFactory(), new ProjectService(), new MemoryRepository());
    }

    ConflictsCommand(DbConnectionFactory connectionFactory, ProjectService projectService,
                     MemoryRepository memoryRepository) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.memoryRepository = memoryRepository;
    }

    public int run(CommandContext context, Args args) {
        String module = args.option("module", "").trim();
        int limit = MemoryCommandSupport.parseLimit(context, args, args.option("limit", "100"),
                "dhk memory conflicts --limit 100");
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
            List<ConflictGroup> groups = conflictGroups(items);
            if (JsonOutput.enabled(args)) {
                printJson(context, module, groups);
            } else {
                printText(context, module, groups);
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR memory conflicts failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private List<ConflictGroup> conflictGroups(List<MemoryItem> items) {
        Map<String, List<MemoryItem>> byKey = new LinkedHashMap<String, List<MemoryItem>>();
        for (MemoryItem item : items) {
            String key = item.canonicalKey().length() == 0
                    ? MemoryIdentity.canonicalKey(item.moduleName(), item.title()) : item.canonicalKey();
            List<MemoryItem> group = byKey.get(key);
            if (group == null) {
                group = new ArrayList<MemoryItem>();
                byKey.put(key, group);
            }
            group.add(item);
        }
        List<ConflictGroup> result = new ArrayList<ConflictGroup>();
        for (List<MemoryItem> group : byKey.values()) {
            if (group.size() <= 1) {
                continue;
            }
            if (hasOpposition(group)) {
                result.add(new ConflictGroup(group, "semantic_opposition", "high"));
            } else if (hasDifferentFingerprint(group)) {
                result.add(new ConflictGroup(group, "fact_variant", "low"));
            }
        }
        return result;
    }

    private boolean hasOpposition(List<MemoryItem> group) {
        for (int i = 0; i < group.size(); i++) {
            for (int j = i + 1; j < group.size(); j++) {
                if (opposes(group.get(i).content(), group.get(j).content())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasDifferentFingerprint(List<MemoryItem> group) {
        for (int i = 0; i < group.size(); i++) {
            for (int j = i + 1; j < group.size(); j++) {
                if (!fingerprint(group.get(i)).equals(fingerprint(group.get(j)))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String fingerprint(MemoryItem item) {
        return item.fingerprint().length() == 0
                ? MemoryIdentity.fingerprint(item.title(), item.content()) : item.fingerprint();
    }

    private boolean opposes(String left, String right) {
        String l = lower(left);
        String r = lower(right);
        return pair(l, r, "stable", "alpha")
                || pair(l, r, "stable", "experimental")
                || pair(l, r, "enabled", "disabled")
                || pair(l, r, "allow", "forbid")
                || pair(l, r, "allowed", "forbidden")
                || pair(l, r, "true", "false")
                || pair(l, r, "must", "must not")
                || pair(l, r, "required", "optional");
    }

    private boolean pair(String left, String right, String a, String b) {
        return (left.contains(a) && right.contains(b)) || (left.contains(b) && right.contains(a));
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void printText(CommandContext context, String module, List<ConflictGroup> groups) {
        context.out().println("memory conflicts");
        context.out().println("module: " + (module.length() == 0 ? "all" : module));
        context.out().println("groups: " + groups.size());
        for (ConflictGroup conflict : groups) {
            List<MemoryItem> group = conflict.items;
            context.out().println("- canonical_key: " + group.get(0).canonicalKey());
            context.out().println("  ids: " + ids(group));
            context.out().println("  title: " + group.get(0).title());
            context.out().println("  severity: " + conflict.severity);
            context.out().println("  reason: " + conflict.reason);
        }
    }

    private void printJson(CommandContext context, String module, List<ConflictGroup> groups) {
        List<String> rawGroups = new ArrayList<String>();
        for (ConflictGroup conflict : groups) {
            List<MemoryItem> group = conflict.items;
            rawGroups.add(JsonOutput.object(
                    JsonOutput.stringField("canonical_key", group.get(0).canonicalKey()),
                    JsonOutput.stringField("ids", ids(group)),
                    JsonOutput.stringField("title", group.get(0).title()),
                    JsonOutput.stringField("severity", conflict.severity),
                    JsonOutput.stringField("reason", conflict.reason)
            ).trim());
        }
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "memory conflicts"),
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

    private static final class ConflictGroup {
        private final List<MemoryItem> items;
        private final String reason;
        private final String severity;

        private ConflictGroup(List<MemoryItem> items, String reason, String severity) {
            this.items = items;
            this.reason = reason;
            this.severity = severity;
        }
    }
}
