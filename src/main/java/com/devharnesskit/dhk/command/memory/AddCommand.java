package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.FtsRepository;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.service.MemoryType;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.util.PathUtil;
import com.devharnesskit.dhk.util.TagUtil;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class AddCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final MemoryRepository memoryRepository;
    private final FtsRepository ftsRepository;
    private final SensitiveDataGuard sensitiveDataGuard;
    private final TransactionTemplate transactionTemplate;

    public AddCommand() {
        this(new DbConnectionFactory(), new ProjectService(), new MemoryRepository(),
                new FtsRepository(), new SensitiveDataGuard(), new TransactionTemplate());
    }

    AddCommand(DbConnectionFactory connectionFactory, ProjectService projectService,
               MemoryRepository memoryRepository, FtsRepository ftsRepository,
               SensitiveDataGuard sensitiveDataGuard, TransactionTemplate transactionTemplate) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.memoryRepository = memoryRepository;
        this.ftsRepository = ftsRepository;
        this.sensitiveDataGuard = sensitiveDataGuard;
        this.transactionTemplate = transactionTemplate;
    }

    public int run(CommandContext context, Args args) {
        String type = args.option("type").trim();
        String title = args.option("title").trim();
        String content = args.option("content").trim();
        if (type.length() == 0 || title.length() == 0 || content.length() == 0) {
            context.err().println("Missing required parameters: --type, --title, --content");
            return ExitCodes.USAGE_ERROR;
        }
        if (!MemoryType.isAllowed(type)) {
            context.err().println("Invalid memory type: " + type);
            return ExitCodes.VALIDATION_ERROR;
        }
        if (args.hasOption("status") && !"draft".equals(args.option("status"))) {
            context.err().println("memory add only supports --status draft; use memory confirm to confirm facts");
            return ExitCodes.VALIDATION_ERROR;
        }

        int confidence = parseConfidence(context, args.option("confidence", "50"));
        if (confidence < 0) {
            return ExitCodes.VALIDATION_ERROR;
        }

        String module = args.option("module", "global").trim();
        if (module.length() == 0) {
            module = "global";
        }
        String tags = TagUtil.normalize(args.option("tags", ""));
        String source = args.option("source", "").trim();
        String evidence = args.option("evidence", "").trim();
        String sensitiveInput = title + "\n" + content + "\n" + tags + "\n" + source + "\n" + evidence;
        List<String> matches = sensitiveDataGuard.findMatches(sensitiveInput);
        if (!matches.isEmpty()) {
            context.err().println("Sensitive data rejected: " + matches);
            return ExitCodes.VALIDATION_ERROR;
        }

        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = MemoryCommandSupport.requireInitializedProject(context, projectRoot, projectService, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            final String now = context.clock().now().toString();
            final Project currentProject = project;
            final MemoryItem item = new MemoryItem(0L, currentProject.projectKey(), module, type, "project",
                    title, content, tags, "draft", confidence, "manual", "",
                    "", source, evidence, "", "", now, now, "", 0);
            Long insertedId = transactionTemplate.execute(connection, new TransactionTemplate.Work<Long>() {
                public Long execute() throws Exception {
                    long id = memoryRepository.insert(connection, item);
                    MemoryItem inserted = memoryRepository.findById(connection, currentProject.projectKey(), id);
                    ftsRepository.sync(connection, inserted);
                    return Long.valueOf(id);
                }
            });
            long id = insertedId.longValue();
            context.out().println("memory_id: " + id);
            context.out().println("status: draft");
            return ExitCodes.SUCCESS;
        } catch (SQLException ex) {
            context.err().println("ERROR memory add failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        } catch (RuntimeException ex) {
            context.err().println("ERROR memory add failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR memory add failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private int parseConfidence(CommandContext context, String rawValue) {
        try {
            int value = Integer.parseInt(rawValue);
            if (value < 0 || value > 100) {
                context.err().println("Invalid confidence, expected 0..100: " + rawValue);
                return -1;
            }
            return value;
        } catch (NumberFormatException ex) {
            context.err().println("Invalid confidence, expected integer 0..100: " + rawValue);
            return -1;
        }
    }
}
