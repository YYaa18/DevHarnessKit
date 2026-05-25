package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;

import java.nio.file.Path;
import java.util.List;

public final class GoalCheckCommand implements Command {
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();

    public int run(CommandContext context, Args args) {
        boolean all = args.hasFlag("all");
        String check = args.option("check").trim();
        if (!all && check.length() == 0) {
            context.err().println("Missing required parameter: --check <check-key> or --all");
            return ExitCodes.USAGE_ERROR;
        }
        Path projectRoot = GoalCommandSupport.projectRoot(args, context);
        try {
            GoalRun goal = GoalCommandSupport.goal(orchestrator, context, args, projectRoot);
            List<GoalCheck> checks = orchestrator.runCheck(context, projectRoot, goal.goalKey(), check, all);
            for (GoalCheck result : checks) {
                context.out().println("check_key: " + result.checkKey());
                context.out().println("status: " + result.status());
                context.out().println("result_summary: " + result.resultSummary());
                if (result.evidencePath().length() > 0) {
                    context.out().println("evidence_path: " + result.evidencePath());
                }
            }
            return ExitCodes.SUCCESS;
        } catch (IllegalArgumentException ex) {
            context.err().println(ex.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR goal check failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
