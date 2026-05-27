package com.devharnesskit.dhk.command.bdd;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.bdd.BddFeature;
import com.devharnesskit.dhk.model.bdd.BddScenario;
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

public final class BddListCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final BddService bddService = new BddService(new BddFeatureRepository(),
            new BddScenarioRepository(), new BddStepRepository());

    public int run(CommandContext context, Args args) {
        String featureKey = args.option("feature", "").trim();
        Path projectRoot = BddCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = BddCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            List<BddFeature> features = bddService.listFeatures(connection, project);
            List<BddScenario> scenarios = bddService.listScenarios(connection, project, featureKey);
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "bdd list"),
                        JsonOutput.numberField("feature_count", features.size()),
                        JsonOutput.numberField("scenario_count", scenarios.size()),
                        JsonOutput.rawField("features", featuresJson(features)),
                        JsonOutput.rawField("scenarios", scenariosJson(scenarios))
                ));
            } else {
                context.out().println("features: " + features.size());
                for (BddFeature feature : features) {
                    if (featureKey.length() > 0 && !featureKey.equals(feature.featureKey())) {
                        continue;
                    }
                    context.out().println("- " + feature.featureKey() + " [" + feature.status() + "] "
                            + feature.title() + " (" + feature.moduleName() + ")");
                    for (BddScenario scenario : scenarios) {
                        if (feature.featureKey().equals(scenario.featureKey())) {
                            context.out().println("  - " + scenario.scenarioKey() + " ["
                                    + scenario.status() + "] " + scenario.title());
                        }
                    }
                }
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR bdd list failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private String featuresJson(List<BddFeature> features) {
        List<String> values = new ArrayList<String>();
        for (BddFeature feature : features) {
            values.add(JsonOutput.object(
                    JsonOutput.stringField("feature_key", feature.featureKey()),
                    JsonOutput.stringField("title", feature.title()),
                    JsonOutput.stringField("module", feature.moduleName()),
                    JsonOutput.stringField("status", feature.status())
            ).trim());
        }
        return JsonOutput.array(values);
    }

    private String scenariosJson(List<BddScenario> scenarios) {
        List<String> values = new ArrayList<String>();
        for (BddScenario scenario : scenarios) {
            values.add(JsonOutput.object(
                    JsonOutput.stringField("scenario_key", scenario.scenarioKey()),
                    JsonOutput.stringField("feature_key", scenario.featureKey()),
                    JsonOutput.stringField("title", scenario.title()),
                    JsonOutput.stringField("status", scenario.status())
            ).trim());
        }
        return JsonOutput.array(values);
    }
}
