package com.devharnesskit.dhk.command.context;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.context.ContextBudget;
import com.devharnesskit.dhk.context.ContextBudgetPolicy;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.goal.GoalRunRepository;
import com.devharnesskit.dhk.repository.spec.SpecChangeRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;
import com.devharnesskit.dhk.service.config.DevHarnessConfigService;
import com.devharnesskit.dhk.service.goal.GoalContextService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;

public final class ContextRenderCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final GoalRunRepository goalRunRepository = new GoalRunRepository();
    private final WorkflowRunRepository workflowRunRepository = new WorkflowRunRepository();
    private final SpecChangeRepository specChangeRepository = new SpecChangeRepository();
    private final GoalContextService goalContextService = new GoalContextService();
    private final DevHarnessConfigService configService = new DevHarnessConfigService();

    public int run(CommandContext context, Args args) {
        String goalKey = args.option("goal", "").trim();
        if (goalKey.length() == 0) {
            context.err().println("Missing --goal.");
            context.err().println("Example: dhk context render --goal <goal-key>");
            return ExitCodes.USAGE_ERROR;
        }
        String compressor = args.option("compressor", "default").trim();
        boolean headroom = "headroom".equalsIgnoreCase(compressor);
        Path projectRoot = ContextCommandSupport.projectRoot(args, context);
        if (!ContextCommandSupport.requireProjectJson(context, projectRoot)) {
            return ExitCodes.NOT_FOUND;
        }
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = ContextCommandSupport.requireProject(context, projectRoot, connection);
            if (project == null) {
                return ExitCodes.NOT_FOUND;
            }
            GoalRun goal = goalRunRepository.findByKey(connection, goalKey);
            if (goal == null || !project.projectKey().equals(goal.projectKey())) {
                context.err().println("Goal not found: " + goalKey);
                return ExitCodes.NOT_FOUND;
            }
            WorkflowRun workflowRun = goal.workflowRunKey().length() == 0
                    ? null : workflowRunRepository.findByKey(connection, goal.workflowRunKey());
            SpecChange specChange = goal.specChangeKey().length() == 0
                    ? null : specChangeRepository.findByKey(connection, goal.specChangeKey());
            ContextBudget renderBudget = headroom
                    ? headroomBudget(projectRoot) : ContextBudgetPolicy.fromConfig(configService.load(projectRoot));
            Path goalContext = goalContextService.export(connection, projectRoot, project, goal,
                    workflowRun, specChange, context.clock().now().toString(), renderBudget);
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "context render"),
                        JsonOutput.stringField("goal_key", goal.goalKey()),
                        JsonOutput.stringField("compressor", headroom ? "headroom" : "default"),
                        JsonOutput.booleanField("experimental", headroom),
                        JsonOutput.numberField("total_tokens_budget", renderBudget.totalTokens()),
                        JsonOutput.stringField("current_context_path", PathUtil.currentContext(projectRoot).toString()),
                        JsonOutput.stringField("goal_context_path", goalContext.toString())
                ));
            } else {
                context.out().println("rendered: true");
                context.out().println("goal_key: " + goal.goalKey());
                context.out().println("compressor: " + (headroom ? "headroom (experimental)" : "default"));
                context.out().println("current_context_path: " + PathUtil.currentContext(projectRoot));
                context.out().println("goal_context_path: " + goalContext);
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR context render failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private ContextBudget headroomBudget(Path projectRoot) throws Exception {
        ContextBudget base = ContextBudgetPolicy.fromConfig(configService.load(projectRoot));
        int total = Math.max(1000, base.totalTokens() - base.outputHeadroomTokens());
        return new ContextBudget(total, base.goalTokens(), base.currentStepTokens(), base.memoryTokens(),
                base.graphTokens(), base.bddTokens(), base.workflowTokens(), base.specTokens(),
                base.evidenceTokens(), base.risksTokens(), base.outputHeadroomTokens());
    }
}
