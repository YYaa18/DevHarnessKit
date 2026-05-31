package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.model.goal.GoalRun;
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
            return CommandErrorGuidance.missing(context, args, "GOAL_START_ARGUMENTS_MISSING",
                    new String[]{"--profile", "--task"},
                    "dhk goal start --profile java-api-change --task \"<task>\"",
                    "README.md#core-path");
        }
        String module = args.option("module", "global").trim();
        if (module.length() == 0) {
            module = "global";
        }
        String mode = args.option("mode", "auto").trim();
        String condition = args.option("condition", "").trim();
        String externalRef = args.option("external-ref", args.option("card-ref", "")).trim();
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try {
            if (!args.hasFlag("force-new")) {
                GoalRun existing = orchestrator.findOpenByIdentity(context, projectRoot, profile, task, module);
                if (existing != null) {
                    context.out().println("goal_key: " + existing.goalKey());
                    context.out().println("profile: " + existing.profileKey());
                    context.out().println("external_ref: " + existing.externalRef());
                    context.out().println("workflow_run: " + existing.workflowRunKey());
                    context.out().println("spec_change: " + existing.specChangeKey());
                    context.out().println("status: " + existing.status());
                    context.out().println("start_result: existing_goal");
                    context.out().println("current_action: " + existing.currentAction());
                    context.out().println("next_command: dhk goal next --goal " + existing.goalKey());
                    context.out().println("context_path: " + PathUtil.goalContext(projectRoot));
                    context.out().println("note: matching open goal reused; pass --force-new to create another goal");
                    return ExitCodes.SUCCESS;
                }
            }
            GoalOrchestrator.GoalStartResult result = orchestrator.start(context, projectRoot,
                    profile, task, module, mode, condition, externalRef);
            SpecChange spec = result.specChange();
            context.out().println("goal_key: " + result.goal().goalKey());
            context.out().println("profile: " + result.goal().profileKey());
            context.out().println("external_ref: " + result.goal().externalRef());
            context.out().println("workflow_run: " + result.workflowRun().runKey());
            context.out().println("spec_change: " + (spec == null ? "" : spec.changeKey()));
            context.out().println("status: " + result.goal().status());
            context.out().println("current_action: " + result.goal().currentAction());
            context.out().println("next_command: dhk goal next --goal " + result.goal().goalKey());
            context.out().println("context_path: " + result.contextPath());
            return ExitCodes.SUCCESS;
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("Sensitive data rejected")) {
                context.err().println(ex.getMessage());
                return ExitCodes.VALIDATION_ERROR;
            }
            return CommandErrorGuidance.invalidUsage(context, args, "GOAL_START_REJECTED",
                    ex.getMessage(), "Goal start rejected the requested profile, mode, or task input.",
                    "dhk advise --task \"" + task + "\"", "docs/GOAL_CONFIGURATION.md");
        } catch (Exception ex) {
            context.err().println("ERROR goal start failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
