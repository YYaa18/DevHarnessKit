package com.devharnesskit.dhk.command.bdd;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.bdd.BddBinding;
import com.devharnesskit.dhk.model.bdd.BddFeature;
import com.devharnesskit.dhk.model.bdd.BddScenario;
import com.devharnesskit.dhk.model.bdd.BddScenarioView;
import com.devharnesskit.dhk.model.bdd.BddStep;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.bdd.BddFeatureRepository;
import com.devharnesskit.dhk.repository.bdd.BddScenarioRepository;
import com.devharnesskit.dhk.repository.bdd.BddStepRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.bdd.BddService;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public final class BddShowCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final BddService bddService = new BddService(new BddFeatureRepository(),
            new BddScenarioRepository(), new BddStepRepository());

    public int run(CommandContext context, Args args) {
        String scenarioKey = args.option("scenario", "").trim();
        String featureKey = args.option("feature", "").trim();
        if (scenarioKey.length() == 0 && featureKey.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "BDD_SHOW_TARGET_MISSING",
                    new String[]{"--scenario|--feature"},
                    "dhk bdd show --scenario <scenario>",
                    "docs/GOAL_CONFIGURATION.md");
        }
        Path projectRoot = BddCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = BddCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            if (scenarioKey.length() > 0) {
                return showScenario(context, args, connection, project, scenarioKey);
            }
            return showFeature(context, args, connection, project, featureKey);
        } catch (Exception ex) {
            context.err().println("ERROR bdd show failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private int showScenario(CommandContext context, Args args, Connection connection,
                             Project project, String scenarioKey) throws Exception {
        BddScenarioView view = bddService.findScenario(connection, project, scenarioKey);
        if (view == null) {
            return CommandErrorGuidance.notFound(context, args, "BDD_SCENARIO_NOT_FOUND",
                    "BDD scenario", scenarioKey, "dhk bdd show --feature <feature>",
                    "docs/GOAL_CONFIGURATION.md");
        }
        if (JsonOutput.enabled(args)) {
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", "bdd show"),
                    JsonOutput.stringField("feature_key", view.feature().featureKey()),
                    JsonOutput.stringField("scenario_key", view.scenario().scenarioKey()),
                    JsonOutput.stringField("title", view.scenario().title()),
                    JsonOutput.stringField("status", view.scenario().status()),
                    JsonOutput.rawField("steps", stepsJson(view.steps())),
                    JsonOutput.rawField("bindings", bindingsJson(view.bindings()))
            ));
        } else {
            context.out().println("feature_key: " + view.feature().featureKey());
            context.out().println("feature_title: " + view.feature().title());
            context.out().println("scenario_key: " + view.scenario().scenarioKey());
            context.out().println("scenario_title: " + view.scenario().title());
            context.out().println("status: " + view.scenario().status());
            context.out().println("type: " + view.scenario().scenarioType());
            context.out().println("priority: " + view.scenario().priority());
            context.out().println("tags: " + view.scenario().tags());
            context.out().println("steps:");
            for (BddStep step : view.steps()) {
                context.out().println("- " + step.stepType() + ": " + step.stepText());
            }
            if (!view.bindings().isEmpty()) {
                context.out().println("bindings:");
                for (BddBinding binding : view.bindings()) {
                    context.out().println("- " + binding.bindingType() + " "
                            + binding.bindingKey() + " (" + binding.relation() + ")");
                }
            }
        }
        return ExitCodes.SUCCESS;
    }

    private int showFeature(CommandContext context, Args args, Connection connection,
                            Project project, String featureKey) throws Exception {
        BddFeature feature = bddService.findFeature(connection, project, featureKey);
        if (feature == null) {
            return CommandErrorGuidance.notFound(context, args, "BDD_FEATURE_NOT_FOUND",
                    "BDD feature", featureKey, "dhk bdd add --feature <feature> --title \"<title>\" --scenario <scenario> --scenario-title \"<title>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        List<BddScenario> scenarios = bddService.listScenarios(connection, project, featureKey);
        if (JsonOutput.enabled(args)) {
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", "bdd show"),
                    JsonOutput.stringField("feature_key", feature.featureKey()),
                    JsonOutput.stringField("title", feature.title()),
                    JsonOutput.stringField("status", feature.status()),
                    JsonOutput.numberField("scenario_count", scenarios.size()),
                    JsonOutput.rawField("scenarios", scenariosJson(scenarios))
            ));
        } else {
            context.out().println("feature_key: " + feature.featureKey());
            context.out().println("title: " + feature.title());
            context.out().println("status: " + feature.status());
            context.out().println("module: " + feature.moduleName());
            context.out().println("scenarios:");
            for (BddScenario scenario : scenarios) {
                context.out().println("- " + scenario.scenarioKey() + " ["
                        + scenario.status() + "] " + scenario.title());
            }
        }
        return ExitCodes.SUCCESS;
    }

    private String stepsJson(List<BddStep> steps) {
        List<String> values = new ArrayList<String>();
        for (BddStep step : steps) {
            values.add(JsonOutput.object(
                    JsonOutput.stringField("type", step.stepType()),
                    JsonOutput.stringField("text", step.stepText())
            ).trim());
        }
        return JsonOutput.array(values);
    }

    private String bindingsJson(List<BddBinding> bindings) {
        List<String> values = new ArrayList<String>();
        for (BddBinding binding : bindings) {
            values.add(JsonOutput.object(
                    JsonOutput.stringField("binding_type", binding.bindingType()),
                    JsonOutput.stringField("binding_key", binding.bindingKey()),
                    JsonOutput.stringField("relation", binding.relation())
            ).trim());
        }
        return JsonOutput.array(values);
    }

    private String scenariosJson(List<BddScenario> scenarios) {
        List<String> values = new ArrayList<String>();
        for (BddScenario scenario : scenarios) {
            values.add(JsonOutput.object(
                    JsonOutput.stringField("scenario_key", scenario.scenarioKey()),
                    JsonOutput.stringField("title", scenario.title()),
                    JsonOutput.stringField("status", scenario.status())
            ).trim());
        }
        return JsonOutput.array(values);
    }
}
