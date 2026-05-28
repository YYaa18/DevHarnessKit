package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.goal.GoalEvidenceContract;
import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;

public final class GoalEvidenceTemplateCommand implements Command {
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();

    public int run(CommandContext context, Args args) {
        Path projectRoot = GoalCommandSupport.projectRoot(args, context);
        String goalKey = args.option("goal", "").trim();
        if (goalKey.length() == 0) {
            context.err().println("Missing required parameter: --goal");
            context.err().println("next_command: dhk goal evidence-template --goal <goal-key>");
            return ExitCodes.USAGE_ERROR;
        }
        try {
            GoalRun goal = orchestrator.find(context, projectRoot, goalKey);
            GoalPlan plan = orchestrator.plan(projectRoot, goal);
            GoalEvidenceContract contract = GoalEvidenceContract.from(goal, plan);
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "goal evidence-template"),
                        JsonOutput.stringField("goal_key", contract.goalKey()),
                        JsonOutput.stringField("current_action", contract.currentAction()),
                        JsonOutput.rawField("required_evidence", JsonOutput.stringArray(contract.requiredEvidence())),
                        JsonOutput.rawField("structured_evidence_fields",
                                JsonOutput.stringArray(contract.structuredEvidenceFields())),
                        JsonOutput.stringField("example_evidence", contract.exampleEvidence()),
                        JsonOutput.stringField("example_command", contract.exampleCommand())
                ));
            } else {
                context.out().println("goal evidence template");
                context.out().println("goal_key: " + contract.goalKey());
                context.out().println("current_action: " + contract.currentAction());
                context.out().println("required_evidence:");
                printArray(context, contract.requiredEvidence(), "none");
                context.out().println("structured_evidence_fields:");
                printArray(context, contract.structuredEvidenceFields(), "none");
                context.out().println("example_evidence: " + valueOrNone(contract.exampleEvidence()));
                context.out().println("example_command: " + contract.exampleCommand());
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR goal evidence-template failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printArray(CommandContext context, String[] values, String empty) {
        if (values.length == 0) {
            context.out().println("  - " + empty);
            return;
        }
        for (String value : values) {
            context.out().println("  - " + value);
        }
    }

    private String valueOrNone(String value) {
        return value == null || value.length() == 0 ? "none" : value;
    }
}
