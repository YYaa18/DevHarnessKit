package com.devharnesskit.dhk.command.memory;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.model.MemoryCandidate;
import com.devharnesskit.dhk.model.MemoryItem;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.FtsRepository;
import com.devharnesskit.dhk.repository.MemoryRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public final class CandidatesCommand implements Command {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final MemoryRepository memoryRepository;
    private final FtsRepository ftsRepository;
    private final TransactionTemplate transactionTemplate;

    public CandidatesCommand() {
        this(new DbConnectionFactory(), new ProjectService(), new MemoryRepository(),
                new FtsRepository(), new TransactionTemplate());
    }

    CandidatesCommand(DbConnectionFactory connectionFactory, ProjectService projectService,
                      MemoryRepository memoryRepository, FtsRepository ftsRepository,
                      TransactionTemplate transactionTemplate) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.memoryRepository = memoryRepository;
        this.ftsRepository = ftsRepository;
        this.transactionTemplate = transactionTemplate;
    }

    public int run(CommandContext context, Args args) {
        String action = args.positional(2);
        if ("list".equals(action)) {
            return list(context, args);
        }
        if ("accept".equals(action)) {
            return accept(context, args);
        }
        if ("reject".equals(action)) {
            return reject(context, args);
        }
        context.err().println("Unknown memory candidates command: " + action);
        context.err().println("Run `dhk help` for usage.");
        return ExitCodes.USAGE_ERROR;
    }

    private int list(CommandContext context, Args args) {
        String status = args.option("status", "pending").trim();
        if ("all".equals(status)) {
            status = "";
        }
        if (status.length() > 0 && !isCandidateStatus(status)) {
            context.err().println("Invalid candidate status: " + status);
            context.err().println("valid_values: pending, accepted, rejected, all");
            return ExitCodes.VALIDATION_ERROR;
        }
        int limit = MemoryCommandSupport.parseLimit(context, args, args.option("limit", "20"),
                "dhk memory candidates list --limit 20");
        if (limit <= 0) {
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = MemoryCommandSupport.requireInitializedProject(context, projectRoot, projectService, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            List<MemoryCandidate> items = memoryRepository.listMemoryCandidates(connection, project.projectKey(), status, limit);
            if (JsonOutput.enabled(args)) {
                printJson(context, status, limit, items);
            } else {
                printText(context, status, limit, items);
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR memory candidates list failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private int accept(CommandContext context, Args args) {
        if (!args.hasOption("id")) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_CANDIDATE_ID_MISSING",
                    new String[]{"--id"}, "dhk memory candidates list", "README.md#core-path");
        }
        long id = MemoryCommandSupport.parseId(context, args, "candidate id", args.option("id"),
                "MEMORY_CANDIDATE_INVALID_ID", "dhk memory candidates accept --id <id>");
        if (id <= 0) {
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = MemoryCommandSupport.requireInitializedProject(context, projectRoot, projectService, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            MemoryCandidate candidate = memoryRepository.findCandidateById(connection, project.projectKey(), id);
            if (candidate == null) {
                return CommandErrorGuidance.notFound(context, args, "MEMORY_CANDIDATE_NOT_FOUND",
                        "memory candidate", Long.toString(id), "dhk memory candidates list", "README.md#core-path");
            }
            if ("accepted".equals(candidate.candidateStatus())) {
                return printAccept(context, args, id, candidate.acceptedMemoryId(), "accepted");
            }
            if (!"pending".equals(candidate.candidateStatus())) {
                context.err().println("memory candidate cannot be accepted from status: " + candidate.candidateStatus());
                return ExitCodes.VALIDATION_ERROR;
            }
            String sensitiveInput = candidate.title() + "\n" + candidate.content() + "\n" + candidate.tags()
                    + "\n" + candidate.sourceRef() + "\n" + candidate.reason() + "\n" + candidate.evidence();
            if (MemoryCommandSupport.rejectSensitive(context, sensitiveInput)) {
                return ExitCodes.VALIDATION_ERROR;
            }
            String now = context.clock().now().toString();
            Long memoryId = transactionTemplate.execute(connection, new TransactionTemplate.Work<Long>() {
                public Long execute() throws Exception {
                    MemoryItem duplicate = memoryRepository.findByFingerprint(connection, project.projectKey(),
                            candidate.fingerprint());
                    if (duplicate == null) {
                        duplicate = memoryRepository.findByTitleAndContent(connection, project.projectKey(),
                                candidate.title(), candidate.content());
                    }
                    if (duplicate != null) {
                        memoryRepository.markCandidateAccepted(connection, project.projectKey(), candidate.id(),
                                duplicate.id(), now);
                        return Long.valueOf(duplicate.id());
                    }
                    MemoryItem item = new MemoryItem(0L, project.projectKey(), candidate.moduleName(),
                            candidate.memoryType(), "project", candidate.title(), candidate.content(),
                            candidate.tags(), "draft", candidate.confidence(), "candidate",
                            "", "", candidate.sourceRef(), candidate.evidence(),
                            "", "", now, now, "", 0, candidate.fingerprint(), candidate.canonicalKey(),
                            0L, "", "", candidate.sourceRef());
                    long inserted = memoryRepository.insert(connection, item);
                    MemoryItem insertedItem = memoryRepository.findById(connection, project.projectKey(), inserted);
                    ftsRepository.sync(connection, insertedItem);
                    memoryRepository.markCandidateAccepted(connection, project.projectKey(), candidate.id(),
                            inserted, now);
                    return Long.valueOf(inserted);
                }
            });
            return printAccept(context, args, id, memoryId.longValue(), "accepted");
        } catch (Exception ex) {
            context.err().println("ERROR memory candidates accept failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private int reject(CommandContext context, Args args) {
        if (!args.hasOption("id")) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_CANDIDATE_ID_MISSING",
                    new String[]{"--id"}, "dhk memory candidates list", "README.md#core-path");
        }
        String reason = args.option("reason", "").trim();
        if (reason.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "MEMORY_CANDIDATE_REJECT_REASON_MISSING",
                    new String[]{"--reason"}, "dhk memory candidates reject --id <id> --reason \"<reason>\"",
                    "README.md#core-path");
        }
        if (MemoryCommandSupport.rejectSensitive(context, reason)) {
            return ExitCodes.VALIDATION_ERROR;
        }
        long id = MemoryCommandSupport.parseId(context, args, "candidate id", args.option("id"),
                "MEMORY_CANDIDATE_INVALID_ID", "dhk memory candidates reject --id <id> --reason \"<reason>\"");
        if (id <= 0) {
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = MemoryCommandSupport.requireInitializedProject(context, projectRoot, projectService, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            MemoryCandidate candidate = memoryRepository.findCandidateById(connection, project.projectKey(), id);
            if (candidate == null) {
                return CommandErrorGuidance.notFound(context, args, "MEMORY_CANDIDATE_NOT_FOUND",
                        "memory candidate", Long.toString(id), "dhk memory candidates list", "README.md#core-path");
            }
            if ("accepted".equals(candidate.candidateStatus())) {
                context.err().println("accepted memory candidate cannot be rejected");
                return ExitCodes.VALIDATION_ERROR;
            }
            memoryRepository.markCandidateRejected(connection, project.projectKey(), id, reason,
                    context.clock().now().toString());
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "memory candidates reject"),
                        JsonOutput.numberField("candidate_id", id),
                        JsonOutput.stringField("status", "rejected")
                ));
            } else {
                context.out().println("candidate_id: " + id);
                context.out().println("status: rejected");
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR memory candidates reject failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private int printAccept(CommandContext context, Args args, long candidateId, long memoryId, String status) {
        if (JsonOutput.enabled(args)) {
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", "memory candidates accept"),
                    JsonOutput.numberField("candidate_id", candidateId),
                    JsonOutput.numberField("memory_id", memoryId),
                    JsonOutput.stringField("status", status),
                    JsonOutput.stringField("memory_status", "draft")
            ));
        } else {
            context.out().println("candidate_id: " + candidateId);
            context.out().println("memory_id: " + memoryId);
            context.out().println("status: " + status);
            context.out().println("memory_status: draft");
        }
        return ExitCodes.SUCCESS;
    }

    private void printText(CommandContext context, String status, int limit, List<MemoryCandidate> items) {
        context.out().println("memory candidates");
        context.out().println("status: " + (status.length() == 0 ? "all" : status));
        context.out().println("limit: " + limit);
        context.out().println("total: " + items.size());
        if (items.isEmpty()) {
            context.out().println("items: none");
            return;
        }
        context.out().println("items:");
        for (MemoryCandidate item : items) {
            context.out().println("- candidate_id: " + item.id());
            context.out().println("  title: " + item.title());
            context.out().println("  status: " + item.candidateStatus());
            context.out().println("  module: " + item.moduleName());
            context.out().println("  type: " + item.memoryType());
            context.out().println("  confidence: " + item.confidence());
            context.out().println("  content: " + item.summary(160));
            context.out().println("  accepted_memory_id: " + item.acceptedMemoryId());
        }
    }

    private void printJson(CommandContext context, String status, int limit, List<MemoryCandidate> items) {
        List<String> rawItems = new ArrayList<String>();
        for (MemoryCandidate item : items) {
            rawItems.add(JsonOutput.object(
                    JsonOutput.numberField("candidate_id", item.id()),
                    JsonOutput.stringField("title", item.title()),
                    JsonOutput.stringField("status", item.candidateStatus()),
                    JsonOutput.stringField("module", item.moduleName()),
                    JsonOutput.stringField("type", item.memoryType()),
                    JsonOutput.numberField("confidence", item.confidence()),
                    JsonOutput.stringField("content", item.summary(160)),
                    JsonOutput.numberField("accepted_memory_id", item.acceptedMemoryId())
            ).trim());
        }
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "memory candidates list"),
                JsonOutput.stringField("status", status),
                JsonOutput.numberField("limit", limit),
                JsonOutput.numberField("count", items.size()),
                JsonOutput.rawField("items", JsonOutput.array(rawItems))
        ));
    }

    private boolean isCandidateStatus(String status) {
        return "pending".equals(status) || "accepted".equals(status) || "rejected".equals(status);
    }
}
