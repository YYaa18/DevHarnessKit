package com.devharnesskit.dhk.command.skill;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.util.List;

public final class SkillGateCommand implements Command {
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();

    public int run(CommandContext context, Args args) {
        String gate = args.positional(2).trim();
        String checkKey = checkKey(gate);
        if (checkKey.length() == 0) {
            context.err().println("Unknown skill gate: " + gate);
            context.err().println("Available gates: think-before-coding, goal-driven, simplicity, surgical-change");
            return ExitCodes.USAGE_ERROR;
        }
        Path projectRoot = PathUtil.resolveProjectRoot(args, context.workingDirectory());
        try {
            GoalRun goal = goal(context, args, projectRoot);
            List<GoalCheck> checks = orchestrator.runCheck(context, projectRoot, goal.goalKey(), checkKey, false);
            GoalCheck result = checks.isEmpty() ? null : checks.get(0);
            if (result == null) {
                context.err().println("Skill gate did not produce a result: " + checkKey);
                return ExitCodes.RUNTIME_ERROR;
            }
            context.out().println("skill gate complete");
            context.out().println("gate: " + checkKey);
            context.out().println("goal_key: " + goal.goalKey());
            context.out().println("status: " + result.status());
            context.out().println("result_summary: " + result.resultSummary());
            if (result.evidencePath().length() > 0) {
                context.out().println("evidence_path: " + result.evidencePath());
            }
            return "passed".equals(result.status()) ? ExitCodes.SUCCESS : ExitCodes.VALIDATION_ERROR;
        } catch (IllegalArgumentException ex) {
            context.err().println(ex.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR skill gate failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private GoalRun goal(CommandContext context, Args args, Path projectRoot) throws Exception {
        String goalKey = args.option("goal").trim();
        if (goalKey.length() == 0) {
            return orchestrator.latestOpen(context, projectRoot);
        }
        return orchestrator.find(context, projectRoot, goalKey);
    }

    private String checkKey(String gate) {
        if ("think-before-coding".equals(gate) || "think".equals(gate)) {
            return "think-before-coding";
        }
        if ("goal-driven".equals(gate) || "goal".equals(gate)) {
            return "goal-driven";
        }
        if ("simplicity".equals(gate) || "simple".equals(gate)) {
            return "simplicity";
        }
        if ("surgical-change".equals(gate) || "surgical".equals(gate)) {
            return "surgical-change";
        }
        return "";
    }
}
