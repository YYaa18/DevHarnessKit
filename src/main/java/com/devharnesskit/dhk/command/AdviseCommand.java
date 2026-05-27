package com.devharnesskit.dhk.command;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.brief.BriefRequest;
import com.devharnesskit.dhk.model.brief.BriefResult;
import com.devharnesskit.dhk.model.brief.WorkBrief;
import com.devharnesskit.dhk.service.brief.BriefService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;

public final class AdviseCommand implements Command {
    private final BriefService briefService;

    public AdviseCommand() {
        this(new BriefService());
    }

    AdviseCommand(BriefService briefService) {
        this.briefService = briefService;
    }

    public int run(CommandContext context, Args args) {
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        String task = args.option("task", "").trim();
        if (task.length() == 0) {
            context.err().println("Missing required parameter: --task");
            return ExitCodes.USAGE_ERROR;
        }
        BriefRequest request = new BriefRequest(projectRoot, task,
                args.option("module", "global"), args.option("target", "all"),
                args.option("mode", "recommend"), args.option("profile", "java-api-change"),
                args.option("preset", "springboot-manual-ide-test"), args.option("graph", ""));
        try {
            BriefResult result = briefService.prepare(request, true);
            if (JsonOutput.enabled(args)) {
                printJson(context, result);
            } else {
                printText(context, result);
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR advise failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printText(CommandContext context, BriefResult result) {
        WorkBrief brief = result.workBrief();
        context.out().println("brief: ready");
        context.out().println("recommendation: " + brief.recommendation());
        context.out().println("confidence: " + brief.confidence());
        context.out().println("safe_to_start: " + brief.safeToStart());
        context.out().println("confirmation_required: " + brief.confirmationRequired());
        context.out().println("confirmation_reason: " + brief.confirmationReason());
        context.out().println("requires_user_confirmation_reason: " + brief.confirmationReason());
        context.out().println("work_brief_path: " + result.workBriefPath());
        context.out().println("agent_brief_path: " + result.agentBriefPath());
        context.out().println("user_message: 已生成面向用户的工作说明；Agent 将读取执行说明，不需要用户理解底层命令。");
    }

    private void printJson(CommandContext context, BriefResult result) {
        WorkBrief brief = result.workBrief();
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "advise"),
                JsonOutput.stringField("brief", "ready"),
                JsonOutput.stringField("recommendation", brief.recommendation()),
                JsonOutput.stringField("confidence", brief.confidence()),
                JsonOutput.booleanField("safe_to_start", brief.safeToStart()),
                JsonOutput.booleanField("confirmation_required", brief.confirmationRequired()),
                JsonOutput.stringField("confirmation_reason", brief.confirmationReason()),
                JsonOutput.stringField("requires_user_confirmation_reason", brief.confirmationReason()),
                JsonOutput.stringField("work_brief_path", result.workBriefPath().toString()),
                JsonOutput.stringField("agent_brief_path", result.agentBriefPath().toString())
        ));
    }
}
