package com.devharnesskit.dhk.command.brief;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.brief.KnowledgeCandidate;
import com.devharnesskit.dhk.service.brief.BriefLifecycleService;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.util.List;

public final class BriefKnowledgeCommand implements Command {
    private final BriefLifecycleService lifecycleService;

    public BriefKnowledgeCommand() {
        this(new BriefLifecycleService());
    }

    BriefKnowledgeCommand(BriefLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    public int run(CommandContext context, Args args) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        String action = args.positional(2);
        try {
            if ("review".equals(action) || action.length() == 0) {
                return review(context, projectRoot);
            }
            if ("confirm".equals(action)) {
                return confirm(context, args, projectRoot);
            }
            if ("reject".equals(action)) {
                return reject(context, args, projectRoot);
            }
            context.err().println("Unknown brief knowledge command: " + action);
            return ExitCodes.USAGE_ERROR;
        } catch (IllegalArgumentException ex) {
            context.err().println(ex.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR brief knowledge failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private int review(CommandContext context, Path projectRoot) throws Exception {
        lifecycleService.writeKnowledgeBrief(projectRoot);
        List<KnowledgeCandidate> candidates = lifecycleService.loadCandidates(projectRoot);
        context.out().println("knowledge_candidates: " + candidates.size());
        for (KnowledgeCandidate candidate : candidates) {
            if ("rejected".equals(candidate.status())) {
                continue;
            }
            context.out().println("- " + candidate.candidateId() + " [" + candidate.status()
                    + "] -> " + candidate.suggestedDestination() + ": " + candidate.title());
        }
        context.out().println("knowledge_brief_path: " + PathUtil.knowledgeCandidatesBrief(projectRoot));
        return ExitCodes.SUCCESS;
    }

    private int confirm(CommandContext context, Args args, Path projectRoot) throws Exception {
        String candidateId = candidateId(args);
        String destination = args.option("destination", "").trim();
        if (candidateId.length() == 0) {
            context.err().println("Missing required parameter: --candidate");
            return ExitCodes.USAGE_ERROR;
        }
        KnowledgeCandidate candidate = lifecycleService.confirmCandidate(context, projectRoot, candidateId, destination);
        context.out().println("candidate_id: " + candidate.candidateId());
        context.out().println("status: " + candidate.status());
        context.out().println("destination: " + candidate.suggestedDestination());
        context.out().println("note: confirmed candidate created draft destination only");
        return ExitCodes.SUCCESS;
    }

    private int reject(CommandContext context, Args args, Path projectRoot) throws Exception {
        String candidateId = candidateId(args);
        if (candidateId.length() == 0) {
            context.err().println("Missing required parameter: --candidate");
            return ExitCodes.USAGE_ERROR;
        }
        KnowledgeCandidate candidate = lifecycleService.rejectCandidate(projectRoot, candidateId);
        context.out().println("candidate_id: " + candidate.candidateId());
        context.out().println("status: " + candidate.status());
        return ExitCodes.SUCCESS;
    }

    private String candidateId(Args args) {
        return args.option("candidate", args.option("candidate-id", "")).trim();
    }
}
