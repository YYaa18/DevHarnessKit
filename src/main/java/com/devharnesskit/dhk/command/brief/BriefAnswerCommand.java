package com.devharnesskit.dhk.command.brief;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.brief.InteractionRequest;
import com.devharnesskit.dhk.service.brief.BriefLifecycleService;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;

public final class BriefAnswerCommand implements Command {
    private final BriefLifecycleService lifecycleService;

    public BriefAnswerCommand() {
        this(new BriefLifecycleService());
    }

    BriefAnswerCommand(BriefLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    public int run(CommandContext context, Args args) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        String requestId = args.option("request", "").trim();
        String choice = args.option("choice", args.option("answer", "")).trim();
        if (requestId.length() == 0 || choice.length() == 0) {
            context.err().println("Missing required parameters: --request and --choice");
            return ExitCodes.USAGE_ERROR;
        }
        try {
            InteractionRequest request = lifecycleService.answerInteraction(projectRoot, requestId, choice);
            context.out().println("request_id: " + request.requestId());
            context.out().println("status: " + request.status());
            context.out().println("answer: " + request.answer());
            context.out().println("agent_brief_path: " + PathUtil.agentBrief(projectRoot));
            return ExitCodes.SUCCESS;
        } catch (IllegalArgumentException ex) {
            context.err().println(ex.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR brief answer failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
