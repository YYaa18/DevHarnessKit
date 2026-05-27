package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.service.goal.GoalStepAutoEvidenceCollector;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.service.policy.PolicyHookService;
import com.devharnesskit.dhk.service.policy.PolicyViolationException;

import java.nio.file.Path;
import java.util.List;

public final class GoalStepCommand implements Command {
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();
    private final PolicyHookService policyHookService = new PolicyHookService();
    private final GoalStepAutoEvidenceCollector autoEvidenceCollector = new GoalStepAutoEvidenceCollector();

    public int run(CommandContext context, Args args) {
        String goalKey = args.option("goal").trim();
        Path projectRoot = GoalCommandSupport.projectRoot(args, context);
        if (goalKey.length() == 0) {
            context.err().println("Missing required parameter: --goal");
            return ExitCodes.USAGE_ERROR;
        }
        if (args.hasFlag("template")) {
            return printTemplate(context, args, projectRoot, goalKey);
        }
        String summary = args.option("summary").trim();
        if (summary.length() == 0) {
            context.err().println("Missing required parameter: --summary");
            return ExitCodes.USAGE_ERROR;
        }
        String changedFiles = changedFiles(args);
        if (args.hasFlag("auto") && changedFiles.length() == 0) {
            changedFiles = autoEvidenceCollector.changedFiles(projectRoot);
        }
        String evidence = args.option("evidence", "").trim();
        String structuredEvidence = structuredEvidence(args, changedFiles);
        try {
            String fieldEvidence = fieldEvidence(args);
            String autoEvidence = args.hasFlag("auto") ? autoEvidenceCollector.evidence(projectRoot, changedFiles) : "";
            String combinedEvidence = combineEvidence(evidence, structuredEvidence, fieldEvidence, autoEvidence);
            policyHookService.requireGoalStepAllowed(projectRoot, changedFiles);
            if (args.hasFlag("dry-run")) {
                GoalOrchestrator.GoalStepValidationResult result = orchestrator.validateStep(context,
                        projectRoot, goalKey, summary, changedFiles, combinedEvidence);
                context.out().println("goal step dry-run complete");
                context.out().println("status: passed");
                context.out().println("goal_key: " + result.goal().goalKey());
                context.out().println("current_action: " + result.plan().currentAction());
                context.out().println("would_record: true");
                context.out().println("step_count: " + result.goal().stepCount());
                context.out().println("next_command: dhk goal step --goal " + result.goal().goalKey()
                        + " --summary \"<summary>\" --evidence \"<evidence>\"");
                return ExitCodes.SUCCESS;
            }
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

    private int printTemplate(CommandContext context, Args args, Path projectRoot, String goalKey) {
        try {
            com.devharnesskit.dhk.model.goal.GoalRun goal = orchestrator.find(context, projectRoot, goalKey);
            com.devharnesskit.dhk.model.goal.GoalPlan plan = orchestrator.plan(projectRoot, goal);
            context.out().println("goal step template");
            context.out().println("goal_key: " + goal.goalKey());
            context.out().println("current_action: " + plan.currentAction());
            context.out().println("summary: <summary>");
            context.out().println("required_evidence:");
            for (String required : plan.requiredEvidence()) {
                context.out().println("  --field " + required + "=<value>");
            }
            context.out().println("structured_fields:");
            context.out().println("  --read-files <files>");
            context.out().println("  --changed-files <files>");
            context.out().println("  --tests-run <command/result>");
            context.out().println("  --compile-result <result>");
            context.out().println("  --risks <risks>");
            context.out().println("  --pending <pending-or-none>");
            context.out().println("auto_fields:");
            context.out().println("  --auto collects changed_files, diff_stat, touched_modules, protected_file_hits, risk_flags");
            context.out().println("dry_run_command: dhk goal step --goal " + goal.goalKey()
                    + " --summary \"<summary>\" --field <key=value> --dry-run");
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR goal step template failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private String changedFiles(Args args) {
        String changedFiles = args.option("changed-files", "").trim();
        if (changedFiles.length() > 0) {
            return changedFiles;
        }
        for (String field : args.optionValues("field")) {
            int equals = field.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String key = field.substring(0, equals).trim();
            if ("changed_files".equals(key) || "changed-files".equals(key)) {
                return field.substring(equals + 1).trim();
            }
        }
        return "";
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

    private String fieldEvidence(Args args) {
        StringBuilder builder = new StringBuilder();
        List<String> fields = args.optionValues("field");
        for (String field : fields) {
            String value = field == null ? "" : field.trim();
            if (value.length() == 0) {
                continue;
            }
            int equals = value.indexOf('=');
            if (equals <= 0) {
                throw new IllegalArgumentException("--field must use key=value: " + value);
            }
            String key = value.substring(0, equals).trim();
            String text = value.substring(equals + 1).trim();
            if (key.length() == 0 || text.length() == 0) {
                throw new IllegalArgumentException("--field must use non-empty key=value: " + value);
            }
            appendField(builder, key.replace('-', '_'), text);
        }
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

    private String combineEvidence(String... parts) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            String text = part == null ? "" : part.trim();
            if (text.length() == 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(text);
        }
        return builder.toString();
    }

}
