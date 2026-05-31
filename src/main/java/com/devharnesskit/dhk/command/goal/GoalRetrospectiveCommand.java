package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.export.GoalRetrospectiveRenderer;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;

public final class GoalRetrospectiveCommand implements Command {
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();
    private final GoalRetrospectiveRenderer renderer = new GoalRetrospectiveRenderer();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();

    public int run(CommandContext context, Args args) {
        Path projectRoot = GoalCommandSupport.projectRoot(args, context);
        try {
            GoalRun goal = GoalCommandSupport.goal(orchestrator, context, args, projectRoot);
            GoalOrchestrator.GoalAuditResult result = orchestrator.audit(context, projectRoot, goal.goalKey());
            String generatedAt = context.clock().now().toString();
            boolean json = JsonOutput.enabled(args);
            String rendered = json
                    ? renderer.renderJson(result.goal(), result.steps(), result.checks(), result.artifacts(),
                    result.evaluation(), generatedAt)
                    : renderer.renderMarkdown(result.goal(), result.steps(), result.checks(), result.artifacts(),
                    result.evaluation(), generatedAt);
            rendered = sensitiveDataGuard.redact(rendered);
            if (sensitiveDataGuard.containsSensitiveData(rendered)) {
                context.err().println("ERROR goal retrospective failed: sensitive data rejected in output");
                return ExitCodes.VALIDATION_ERROR;
            }
            String writePath = args.option("write", "").trim();
            if (writePath.length() > 0) {
                Path out = PathUtil.resolvePath(writePath, context.workingDirectory());
                Files.createDirectories(out.getParent());
                Files.write(out, rendered.getBytes("UTF-8"));
                printWritten(context, json, result.goal(), out);
            } else {
                context.out().print(rendered);
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR goal retrospective failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printWritten(CommandContext context, boolean json, GoalRun goal, Path out) {
        if (json) {
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", "goal retrospective"),
                    JsonOutput.stringField("goal_key", goal.goalKey()),
                    JsonOutput.stringField("status", "written"),
                    JsonOutput.stringField("path", out.toString().replace('\\', '/'))
            ));
            return;
        }
        context.out().println("goal retrospective written");
        context.out().println("goal_key: " + goal.goalKey());
        context.out().println("path: " + out.toString().replace('\\', '/'));
    }
}
