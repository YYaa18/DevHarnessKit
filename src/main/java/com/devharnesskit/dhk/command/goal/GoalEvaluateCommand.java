package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.goal.GoalEvaluation;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;

public final class GoalEvaluateCommand implements Command {
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();

    public int run(CommandContext context, Args args) {
        Path projectRoot = GoalCommandSupport.projectRoot(args, context);
        try {
            GoalRun goal = GoalCommandSupport.goal(orchestrator, context, args, projectRoot);
            GoalEvaluation evaluation = orchestrator.evaluate(context, projectRoot, goal.goalKey());
            print(context, args, evaluation);
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR goal evaluate failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    static void print(CommandContext context, Args args, GoalEvaluation evaluation) {
        if (JsonOutput.enabled(args)) {
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", "goal evaluate"),
                    JsonOutput.stringField("decision", evaluation.decision()),
                    JsonOutput.rawField("missing", JsonOutput.stringArray(evaluation.missing())),
                    JsonOutput.stringField("next_action", evaluation.nextAction()),
                    JsonOutput.stringField("next_command", evaluation.nextCommand())
            ));
            return;
        }
        context.out().println("decision: " + evaluation.decision());
        context.out().println("missing:");
        if (evaluation.missing().length == 0) {
            context.out().println("  - none");
        } else {
            for (String missing : evaluation.missing()) {
                context.out().println("  - " + missing);
            }
        }
        context.out().println("next_action: " + evaluation.nextAction());
        context.out().println("next_command: " + evaluation.nextCommand());
    }
}
