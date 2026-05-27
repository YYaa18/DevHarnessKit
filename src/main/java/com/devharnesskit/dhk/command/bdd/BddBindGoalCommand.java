package com.devharnesskit.dhk.command.bdd;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.bdd.BddBinding;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.bdd.BddFeatureRepository;
import com.devharnesskit.dhk.repository.bdd.BddScenarioRepository;
import com.devharnesskit.dhk.repository.bdd.BddStepRepository;
import com.devharnesskit.dhk.repository.goal.GoalRunRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.bdd.BddService;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;
import java.sql.Connection;

public final class BddBindGoalCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final GoalRunRepository goalRunRepository = new GoalRunRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();
    private final BddService bddService = new BddService(new BddFeatureRepository(),
            new BddScenarioRepository(), new BddStepRepository());

    public int run(CommandContext context, Args args) {
        String scenarioKey = args.option("scenario").trim();
        String goalKey = args.option("goal").trim();
        String relation = defaultIfBlank(args.option("relation", "supports"), "supports");
        if (scenarioKey.length() == 0 || goalKey.length() == 0) {
            context.err().println("Missing required parameters: --scenario, --goal");
            return ExitCodes.USAGE_ERROR;
        }
        if (!BddService.isKeyAllowed(scenarioKey) || !BddService.isKeyAllowed(goalKey)
                || !BddService.isKeyAllowed(relation)) {
            context.err().println("Invalid BDD/goal key. Use letters, numbers, dot, underscore, or dash.");
            return ExitCodes.VALIDATION_ERROR;
        }
        if (BddCommandSupport.rejectSensitive(context, sensitiveDataGuard, "bdd bind-goal",
                scenarioKey, goalKey, relation)) {
            return ExitCodes.VALIDATION_ERROR;
        }

        Path projectRoot = BddCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            final Project project = BddCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            if (bddService.findScenario(connection, project, scenarioKey) == null) {
                context.err().println("BDD scenario not found: " + scenarioKey);
                return ExitCodes.NOT_FOUND;
            }
            GoalRun goal = goalRunRepository.findByKey(connection, goalKey);
            if (goal == null || !project.projectKey().equals(goal.projectKey())) {
                context.err().println("Goal run not found: " + goalKey);
                return ExitCodes.NOT_FOUND;
            }
            final String selectedScenarioKey = scenarioKey;
            final String selectedGoalKey = goalKey;
            final String selectedRelation = relation;
            BddBinding binding = transactionTemplate.execute(connection,
                    new TransactionTemplate.Work<BddBinding>() {
                        public BddBinding execute() throws Exception {
                            return bddService.bindScenario(connection, project, selectedScenarioKey,
                                    "goal", selectedGoalKey, selectedRelation,
                                    "goal_key=" + selectedGoalKey, context.clock().now().toString());
                        }
                    });
            printResult(context, args, binding, goalKey);
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR bdd bind-goal failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printResult(CommandContext context, Args args, BddBinding binding, String goalKey) {
        if (JsonOutput.enabled(args)) {
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", "bdd bind-goal"),
                    JsonOutput.stringField("scenario_key", binding.scenarioKey()),
                    JsonOutput.stringField("binding_type", binding.bindingType()),
                    JsonOutput.stringField("binding_key", binding.bindingKey()),
                    JsonOutput.stringField("relation", binding.relation()),
                    JsonOutput.stringField("goal_key", goalKey)
            ));
            return;
        }
        context.out().println("bdd bind-goal complete");
        context.out().println("scenario_key: " + binding.scenarioKey());
        context.out().println("binding_type: " + binding.bindingType());
        context.out().println("binding_key: " + binding.bindingKey());
        context.out().println("relation: " + binding.relation());
        context.out().println("goal_key: " + goalKey);
    }

    private String defaultIfBlank(String value, String fallback) {
        String text = value == null ? "" : value.trim();
        return text.length() == 0 ? fallback : text;
    }
}
