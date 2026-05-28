package com.devharnesskit.dhk.command.brief;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.guidance.ActionableError;
import com.devharnesskit.dhk.guidance.ActionableErrorRenderer;
import com.devharnesskit.dhk.model.brief.InteractionRequest;
import com.devharnesskit.dhk.service.brief.BriefLifecycleService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;

public final class BriefAnswerCommand implements Command {
    private final BriefLifecycleService lifecycleService;
    private final ActionableErrorRenderer errorRenderer = new ActionableErrorRenderer();

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
            ActionableError error = ActionableError.builder("BRIEF_ANSWER_ARGUMENTS_MISSING",
                            "Missing required parameters: --request and --choice")
                    .reason("Answering a blocking interaction requires the request id and one selected choice.")
                    .missing(requestId.length() == 0 && choice.length() == 0
                            ? new String[]{"--request", "--choice"}
                            : (requestId.length() == 0 ? new String[]{"--request"} : new String[]{"--choice"}))
                    .nextCommand("dhk brief answer --request <request-id> --choice <choice>")
                    .nextAction("Run `dhk brief show --request <request-id>` if you need to inspect the request.")
                    .docs("docs/WORK_BRIEF.md#interaction-request")
                    .build();
            if (JsonOutput.enabled(args)) {
                context.out().print(errorRenderer.renderJson(error));
            } else {
                context.err().print(errorRenderer.renderText(error));
            }
            return ExitCodes.USAGE_ERROR;
        }
        try {
            InteractionRequest request = lifecycleService.answerInteraction(projectRoot, requestId, choice,
                    args.option("evidence-path", args.option("manual-evidence-path", "")).trim(),
                    args.option("scope", "").trim(),
                    args.option("tester", "").trim(),
                    args.option("reason", args.option("waive-reason", "")).trim(),
                    args.option("approver", "").trim(),
                    args.option("risk-scope", args.option("risk", "")).trim(),
                    args.option("rollback-plan", "").trim());
            context.out().println("request_id: " + request.requestId());
            context.out().println("status: " + request.status());
            context.out().println("answer: " + request.answer());
            context.out().println("agent_brief_path: " + PathUtil.agentBrief(projectRoot));
            if ("manual_verification".equals(request.type())) {
                context.out().println("next_action: rerun goal verify");
            }
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
