package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.FtsRepository;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

public final class ConfirmCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final MemoryRepository memoryRepository;
    private final FtsRepository ftsRepository;
    private final TransactionTemplate transactionTemplate;

    public ConfirmCommand() {
        this(new DbConnectionFactory(), new ProjectService(), new MemoryRepository(), new FtsRepository(),
                new TransactionTemplate());
    }

    ConfirmCommand(DbConnectionFactory connectionFactory, ProjectService projectService,
                   MemoryRepository memoryRepository, FtsRepository ftsRepository,
                   TransactionTemplate transactionTemplate) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.memoryRepository = memoryRepository;
        this.ftsRepository = ftsRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public int run(CommandContext context, Args args) {
        if (!args.hasOption("id")) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_CONFIRM_ID_MISSING",
                    new String[]{"--id"}, "dhk memory list --status draft", "README.md#core-path");
        }
        long id = parseId(context, args, args.option("id"));
        if (id <= 0) {
            return ExitCodes.VALIDATION_ERROR;
        }

        Integer requestedConfidence = null;
        if (args.hasOption("confidence")) {
            requestedConfidence = parseConfidence(context, args, args.option("confidence"));
            if (requestedConfidence == null) {
                return ExitCodes.VALIDATION_ERROR;
            }
        }

        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = MemoryCommandSupport.requireInitializedProject(context, projectRoot, projectService, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            MemoryItem item = memoryRepository.findById(connection, project.projectKey(), id);
            if (item == null) {
                return CommandErrorGuidance.notFound(context, args, "MEMORY_ITEM_NOT_FOUND",
                        "memory item", Long.toString(id), "dhk memory list", "README.md#core-path");
            }
            int confidence = requestedConfidence == null ? Math.max(item.confidence(), 70) : requestedConfidence.intValue();
            String confirmedBy = args.option("confirmed-by", "manual").trim();
            if (confirmedBy.length() == 0) {
                confirmedBy = "manual";
            }
            final String now = context.clock().now().toString();
            final Project currentProject = project;
            final long memoryId = id;
            final int confirmedConfidence = confidence;
            final String confirmer = confirmedBy;
            final int[] supersededCount = {0};
            final java.util.List<Long> supersededIds = new java.util.ArrayList<Long>();
            transactionTemplate.execute(connection, new TransactionTemplate.Work<Void>() {
                public Void execute() throws Exception {
                    memoryRepository.confirm(connection, currentProject.projectKey(), memoryId,
                            confirmedConfidence, confirmer, now);
                    MemoryItem updated = memoryRepository.findById(connection, currentProject.projectKey(), memoryId);
                    ftsRepository.sync(connection, updated);
                    // Auto-retire older confirmed knowledge for the SAME topic (canonical key) that
                    // says something different, so a newer confirmed version supersedes the stale one.
                    if (updated.canonicalKey().length() > 0) {
                        for (MemoryItem other : memoryRepository.findActiveByCanonicalKey(
                                connection, currentProject.projectKey(), updated.canonicalKey(), memoryId)) {
                            if (other.fingerprint().equals(updated.fingerprint())) {
                                continue; // identical knowledge, not a stale conflict
                            }
                            memoryRepository.supersede(connection, currentProject.projectKey(), other.id(), memoryId,
                                    "auto: superseded by newer confirmed knowledge for the same topic", now);
                            ftsRepository.sync(connection,
                                    memoryRepository.findById(connection, currentProject.projectKey(), other.id()));
                            supersededIds.add(Long.valueOf(other.id()));
                            supersededCount[0]++;
                        }
                    }
                    return null;
                }
            });
            context.out().println("memory_id: " + id);
            context.out().println("status: confirmed");
            context.out().println("confidence: " + confidence);
            context.out().println("confirmed_by: " + confirmedBy);
            if (supersededCount[0] > 0) {
                context.out().println("superseded_same_topic: " + supersededCount[0]);
                context.out().println("superseded_ids: " + join(supersededIds));
            }
            return ExitCodes.SUCCESS;
        } catch (SQLException ex) {
            context.err().println("ERROR memory confirm failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        } catch (RuntimeException ex) {
            context.err().println("ERROR memory confirm failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR memory confirm failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private String join(java.util.List<Long> ids) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) b.append(',');
            b.append(ids.get(i));
        }
        return b.toString();
    }

    private long parseId(CommandContext context, Args args, String rawValue) {
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException ex) {
            CommandErrorGuidance.invalidNumber(context, args, "MEMORY_CONFIRM_INVALID_ID",
                    "memory id", rawValue, "memory id must be a positive integer.",
                    "dhk memory list", "README.md#core-path");
            return -1L;
        }
    }

    private Integer parseConfidence(CommandContext context, Args args, String rawValue) {
        try {
            int value = Integer.parseInt(rawValue);
            if (value < 0 || value > 100) {
                CommandErrorGuidance.invalidNumber(context, args, "MEMORY_CONFIRM_INVALID_CONFIDENCE",
                        "memory confidence", rawValue, "confidence must be an integer from 0 to 100.",
                        "dhk memory confirm --id <id> --confidence 70", "README.md#core-path");
                return null;
            }
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            CommandErrorGuidance.invalidNumber(context, args, "MEMORY_CONFIRM_INVALID_CONFIDENCE",
                    "memory confidence", rawValue, "confidence must be an integer from 0 to 100.",
                    "dhk memory confirm --id <id> --confidence 70", "README.md#core-path");
            return null;
        }
    }
}
