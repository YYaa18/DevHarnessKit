package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.service.policy.PolicyHookService;
import com.devharnesskit.dhk.service.policy.PolicyViolationException;

import java.nio.file.Path;

public final class GoalStepCommand implements Command {
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();
    private final PolicyHookService policyHookService = new PolicyHookService();

    public int run(CommandContext context, Args args) {
        String goalKey = args.option("goal").trim();
        String summary = args.option("summary").trim();
        if (goalKey.length() == 0 || summary.length() == 0) {
            context.err().println("Missing required parameters: --goal, --summary");
            return ExitCodes.USAGE_ERROR;
        }
        String changedFiles = args.option("changed-files", "").trim();
        String evidence = args.option("evidence", "").trim();
        String structuredEvidence = structuredEvidence(args, changedFiles);
        String combinedEvidence = combineEvidence(evidence, structuredEvidence);
        Path projectRoot = GoalCommandSupport.projectRoot(args, context);
        try {
            policyHookService.requireGoalStepAllowed(projectRoot, changedFiles);
            GoalOrchestrator.GoalStepResult result = orchestrator.step(context, projectRoot, goalKey,
                    summary, changedFiles, combinedEvidence);
            context.out().println("step_id: " + result.stepId());
            context.out().println("goal_key: " + result.goal().goalKey());
            context.out().println("status: " + result.goal().status());
            context.out().println("current_action: " + result.goal().currentAction());
            context.out().println("next_command: dhk goal next --goal " + result.goal().goalKey());
            context.out().println("context_path: " + result.contextPath());
            return ExitCodes.SUCCESS;
        } catch (IllegalArgumentException ex) {
            context.err().println(ex.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (PolicyViolationException ex) {
            context.err().println(ex.getMessage());
            return ExitCodes.VALIDATION_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR goal step failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private String structuredEvidence(Args args, String changedFiles) {
        StringBuilder builder = new StringBuilder();
        appendField(builder, "read_files", args.option("read-files", ""));
        if (changedFiles.length() > 0) {
            appendField(builder, "changed_files", changedFiles);
        }
        String testsRun = args.option("tests-run", "");
        appendField(builder, "tests_run", testsRun);
        appendField(builder, "test_result", testsRun);
        appendField(builder, "compile_result", args.option("compile-result", ""));
        String risks = args.option("risks", "");
        appendField(builder, "risks", risks);
        appendField(builder, "risk_points", risks);
        appendField(builder, "pending", args.option("pending", ""));
        return builder.toString();
    }

    private void appendField(StringBuilder builder, String key, String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() == 0) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('\n');
        }
        builder.append(key).append('=').append(text);
    }

    private String combineEvidence(String evidence, String structuredEvidence) {
        if (evidence.length() == 0) {
            return structuredEvidence;
        }
        if (structuredEvidence.length() == 0) {
            return evidence;
        }
        return evidence + "\n" + structuredEvidence;
    }
}
