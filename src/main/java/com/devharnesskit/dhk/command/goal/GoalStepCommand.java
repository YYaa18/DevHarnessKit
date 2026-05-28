package com.devharnesskit.dhk.command.goal;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.guidance.ActionableError;
import com.devharnesskit.dhk.guidance.ActionableErrorRenderer;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.model.goal.GoalEvidenceContract;
import com.devharnesskit.dhk.service.goal.GoalStepAutoEvidenceCollector;
import com.devharnesskit.dhk.service.goal.GoalOrchestrator;
import com.devharnesskit.dhk.service.goal.GoalStepEvidenceException;
import com.devharnesskit.dhk.service.brief.BlockingInteractionException;
import com.devharnesskit.dhk.service.brief.BriefLifecycleService;
import com.devharnesskit.dhk.service.policy.PolicyHookService;
import com.devharnesskit.dhk.service.policy.PolicyViolationException;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;
import java.util.List;

public final class GoalStepCommand implements Command {
    private static final String GOAL_SCRIPT_DIR = ".agents/skills/devharness-goal-development/scripts/";
    private final GoalOrchestrator orchestrator = new GoalOrchestrator();
    private final PolicyHookService policyHookService = new PolicyHookService();
    private final GoalStepAutoEvidenceCollector autoEvidenceCollector = new GoalStepAutoEvidenceCollector();
    private final BriefLifecycleService briefLifecycleService = new BriefLifecycleService();
    private final ActionableErrorRenderer actionableErrorRenderer = new ActionableErrorRenderer();

    public int run(CommandContext context, Args args) {
        String goalKey = args.option("goal").trim();
        Path projectRoot = GoalCommandSupport.projectRoot(args, context);
        if (goalKey.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "GOAL_STEP_GOAL_MISSING",
                    new String[]{"--goal"}, "dhk goal next", "README.md#core-path");
        }
        if (args.hasFlag("template")) {
            return printTemplate(context, args, projectRoot, goalKey);
        }
        String summary = args.option("summary").trim();
        if (summary.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "GOAL_STEP_SUMMARY_MISSING",
                    new String[]{"--summary"},
                    "dhk goal evidence-template --goal " + goalKey, "README.md#core-path");
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
                String currentAction = orchestrator.find(context, projectRoot, goalKey).currentAction();
                briefLifecycleService.requireNoBlockingInteraction(projectRoot, goalKey, currentAction);
                GoalOrchestrator.GoalStepValidationResult result = orchestrator.validateStep(context,
                        projectRoot, goalKey, summary, changedFiles, combinedEvidence);
                context.out().println("goal step dry-run complete");
                context.out().println("status: passed");
                context.out().println("goal_key: " + result.goal().goalKey());
                context.out().println("current_action: " + result.plan().currentAction());
                context.out().println("would_record: true");
                context.out().println("step_count: " + result.goal().stepCount());
                context.out().println("next_command: " + GOAL_SCRIPT_DIR + "goal-step.sh --goal " + result.goal().goalKey()
                        + " --summary \"<summary>\" --evidence \"<evidence>\"");
                return ExitCodes.SUCCESS;
            }
            String currentAction = orchestrator.find(context, projectRoot, goalKey).currentAction();
            briefLifecycleService.requireNoBlockingInteraction(projectRoot, goalKey, currentAction);
            GoalOrchestrator.GoalStepResult result = orchestrator.step(context, projectRoot, goalKey,
                    summary, changedFiles, combinedEvidence);
            Path progressPath = briefLifecycleService.writeProgressBrief(projectRoot, result.goal(),
                    result.stepId(), summary);
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "goal step"),
                        JsonOutput.stringField("goal_key", result.goal().goalKey()),
                        JsonOutput.numberField("step_id", result.stepId()),
                        JsonOutput.numberField("goal_step_index", result.goal().stepCount()),
                        JsonOutput.stringField("status", result.goal().status()),
                        JsonOutput.stringField("current_action", result.goal().currentAction()),
                        JsonOutput.stringField("next_command", GOAL_SCRIPT_DIR + "goal-next.sh --goal " + result.goal().goalKey()),
                        JsonOutput.stringField("context_path", result.contextPath().toString()),
                        JsonOutput.stringField("progress_brief_path", progressPath.toString())
                ));
            } else {
                context.out().println("step: " + result.goal().stepCount());
                context.out().println("goal_step_index: " + result.goal().stepCount());
                context.out().println("step_number: " + result.goal().stepCount());
                context.out().println("internal_step_id: " + result.stepId());
                context.out().println("step_id: " + result.stepId());
                context.out().println("goal_key: " + result.goal().goalKey());
                context.out().println("status: " + result.goal().status());
                context.out().println("current_action: " + result.goal().currentAction());
                context.out().println("next_command: " + GOAL_SCRIPT_DIR + "goal-next.sh --goal " + result.goal().goalKey());
                context.out().println("context_path: " + result.contextPath());
                context.out().println("progress_brief_path: " + progressPath);
            }
            return ExitCodes.SUCCESS;
        } catch (GoalStepEvidenceException ex) {
            printMissingEvidence(context, args, goalKey, ex);
            return ExitCodes.VALIDATION_ERROR;
        } catch (BlockingInteractionException ex) {
            printBlockingInteraction(context, args, ex);
            return ExitCodes.VALIDATION_ERROR;
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
            GoalEvidenceContract contract = GoalEvidenceContract.from(goal, plan);
            context.out().println("goal step template");
            context.out().println("goal_key: " + goal.goalKey());
            context.out().println("current_action: " + contract.currentAction());
            context.out().println("summary: <summary>");
            context.out().println("required_evidence:");
            for (String required : contract.requiredEvidence()) {
                context.out().println("  --field " + required + "=<value>");
            }
            context.out().println("structured_fields:");
            for (String field : contract.structuredEvidenceFields()) {
                context.out().println("  " + field);
            }
            context.out().println("auto_fields:");
            context.out().println("  --auto collects changed_files, diff_stat, touched_modules, protected_file_hits, risk_flags");
            context.out().println("example_evidence: " + valueOrNone(contract.exampleEvidence()));
            context.out().println("example_command: " + contract.exampleCommand());
            context.out().println("dry_run_command: " + GOAL_SCRIPT_DIR + "goal-step.sh --goal " + goal.goalKey()
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

    private void printMissingEvidence(CommandContext context, Args args, String goalKey,
                                      GoalStepEvidenceException ex) {
        GoalEvidenceContract contract = GoalEvidenceContract.from(null, ex.plan());
        ActionableError error = ActionableError.builder("GOAL_STEP_EVIDENCE_MISSING",
                        "Goal step evidence missing required items")
                .reason("The current action requires every required_evidence key to appear in goal step evidence.")
                .missing(ex.missing())
                .validValues(contract.requiredEvidence())
                .nextCommand(GOAL_SCRIPT_DIR + "dhk.sh goal evidence-template --goal " + goalKey)
                .docs("docs/GOAL_CONFIGURATION.md#goal-evidence")
                .detail("current_action", contract.currentAction())
                .detail("example_evidence", contract.exampleEvidence())
                .detail("example_command", contract.exampleCommand().replace("<goal-key>", goalKey))
                .build();
        if (JsonOutput.enabled(args)) {
            context.out().print(actionableErrorRenderer.renderJson(error));
        } else {
            context.err().print(actionableErrorRenderer.renderText(error));
        }
    }

    private void printBlockingInteraction(CommandContext context, Args args, BlockingInteractionException ex) {
        ActionableError error = ActionableError.builder("BLOCKING_INTERACTION_REQUIRES_ANSWER",
                        "blocking interaction requires user answer")
                .reason(ex.question())
                .validValues(ex.choices())
                .nextCommand("dhk brief answer --request " + ex.requestId() + " --choice \"<choice>\"")
                .docs("docs/WORK_BRIEF.md#interaction-request")
                .detail("request_id", ex.requestId())
                .detail("details_command", "dhk brief show --request " + ex.requestId())
                .build();
        if (JsonOutput.enabled(args)) {
            context.out().print(actionableErrorRenderer.renderJson(error));
        } else {
            context.err().print(actionableErrorRenderer.renderText(error));
        }
    }

    private String valueOrNone(String value) {
        return value == null || value.length() == 0 ? "none" : value;
    }

}
