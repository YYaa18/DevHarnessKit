package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;

public final class GoalStartCommand implements Command {
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();

    public int run(CommandContext context, Args args) {
        String profile = args.option("profile").trim();
        String task = args.option("task").trim();
        if (profile.length() == 0 || task.length() == 0) {
            context.err().println("Missing required parameters: --profile, --task");
            return ExitCodes.USAGE_ERROR;
        }
        String module = args.option("module", "global").trim();
        if (module.length() == 0) {
            module = "global";
        }
        String mode = args.option("mode", "auto").trim();
        String condition = args.option("condition", "").trim();
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try {
            GoalOrchestrator.GoalStartResult result = orchestrator.start(context, projectRoot,
                    profile, task, module, mode, condition);
            SpecChange spec = result.specChange();
            context.out().println("goal_key: " + result.goal().goalKey());
            context.out().println("profile: " + result.goal().profileKey());
            context.out().println("workflow_run: " + result.workflowRun().runKey());
            context.out().println("spec_change: " + (spec == null ? "" : spec.changeKey()));
            context.out().println("status: " + result.goal().status());
            context.out().println("current_action: " + result.goal().currentAction());
            context.out().println("next_command: dhk goal next --goal " + result.goal().goalKey());
            context.out().println("context_path: " + result.contextPath());
            return ExitCodes.SUCCESS;
        } catch (IllegalArgumentException ex) {
            context.err().println(ex.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR goal start failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
