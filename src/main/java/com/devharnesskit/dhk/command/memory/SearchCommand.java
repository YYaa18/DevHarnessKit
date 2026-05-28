package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.guidance.EnumGuidance;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.SearchResult;
import com.devharnesskit.dhk.repository.FtsRepository;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.service.MemorySearchService;
import com.devharnesskit.dhk.service.MemoryStatus;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class SearchCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final MemorySearchService searchService;

    public SearchCommand() {
        this(new DbConnectionFactory(), new ProjectService(),
                new MemorySearchService(new MemoryRepository(), new FtsRepository()));
    }

    SearchCommand(DbConnectionFactory connectionFactory, ProjectService projectService, MemorySearchService searchService) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.searchService = searchService;
    }

    public int run(CommandContext context, Args args) {
        String query = args.option("q").trim();
        if (query.length() == 0) {
            context.err().println("Missing required parameter: --q");
            return ExitCodes.USAGE_ERROR;
        }
        String status = args.option("status", "").trim();
        if (status.length() > 0 && !MemoryStatus.isAllowed(status)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_MEMORY_STATUS", "memory status", status,
                    EnumGuidance.MEMORY_STATUSES, new String[0],
                    "dhk memory list --status confirmed", "README.md#core-path");
        }
        String module = args.option("module", "").trim();
        int limit = parseLimit(context, args.option("limit", "20"));
        if (limit <= 0) {
            return ExitCodes.VALIDATION_ERROR;
        }
        boolean explain = args.hasFlag("explain");
        boolean json = JsonOutput.enabled(args);

        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = MemoryCommandSupport.requireInitializedProject(context, projectRoot, projectService, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            List<SearchResult> results = searchService.search(connection, project.projectKey(), query, module, status, limit);
            if (json) {
                printJson(context, query, module, status, explain, results);
                return ExitCodes.SUCCESS;
            }
            if (results.isEmpty()) {
                context.out().println("No memory found.");
                return ExitCodes.SUCCESS;
            }
            for (SearchResult result : results) {
                context.out().println("[" + result.item().id() + "] " + result.item().title());
                context.out().println("  status: " + result.item().status()
                        + " confidence: " + result.item().confidence()
                        + " module: " + result.item().moduleName()
                        + " type: " + result.item().memoryType());
                context.out().println("  tags: " + result.item().tags());
                context.out().println("  content: " + result.item().summary(160));
                context.out().println("  match: " + result.match());
                if (explain) {
                    context.out().println("  score: " + result.score());
                    context.out().println("  query: " + query);
                    context.out().println("  explain: match lists weighted fields; tags=8 title=5 content=3 fts=1 module=10 confirmed=3 confidence=1..2");
                }
            }
            return ExitCodes.SUCCESS;
        } catch (SQLException ex) {
            context.err().println("ERROR memory search failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        } catch (RuntimeException ex) {
            context.err().println("ERROR memory search failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printJson(CommandContext context, String query, String module, String status,
                           boolean explain, List<SearchResult> results) {
        List<String> rawResults = new ArrayList<String>();
        for (SearchResult result : results) {
            rawResults.add(JsonOutput.object(
                    JsonOutput.numberField("id", result.item().id()),
                    JsonOutput.stringField("title", result.item().title()),
                    JsonOutput.stringField("status", result.item().status()),
                    JsonOutput.numberField("confidence", result.item().confidence()),
                    JsonOutput.stringField("module", result.item().moduleName()),
                    JsonOutput.stringField("type", result.item().memoryType()),
                    JsonOutput.stringField("tags", result.item().tags()),
                    JsonOutput.stringField("content", result.item().summary(160)),
                    JsonOutput.stringField("match", result.match()),
                    JsonOutput.numberField("score", result.score())
            ).trim());
        }
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "memory search"),
                JsonOutput.stringField("query", query),
                JsonOutput.stringField("module", module),
                JsonOutput.stringField("status", status),
                JsonOutput.booleanField("explain", explain),
                JsonOutput.numberField("count", results.size()),
                JsonOutput.rawField("results", JsonOutput.array(rawResults))
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
}
