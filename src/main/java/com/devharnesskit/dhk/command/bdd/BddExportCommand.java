package com.devharnesskit.dhk.command.bdd;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.export.BddContextRenderer;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.bdd.BddFeature;
import com.devharnesskit.dhk.model.bdd.BddScenario;
import com.devharnesskit.dhk.model.bdd.BddScenarioView;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.bdd.BddFeatureRepository;
import com.devharnesskit.dhk.repository.bdd.BddScenarioRepository;
import com.devharnesskit.dhk.repository.bdd.BddStepRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.bdd.BddService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public final class BddExportCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final BddContextRenderer renderer = new BddContextRenderer();
    private final BddService bddService = new BddService(new BddFeatureRepository(),
            new BddScenarioRepository(), new BddStepRepository());

    public int run(CommandContext context, Args args) {
        String featureFilter = args.option("feature", "").trim();
        Path projectRoot = BddCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = BddCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            List<BddFeature> features = selectedFeatures(connection, project, featureFilter);
            if (featureFilter.length() > 0 && features.isEmpty()) {
                return CommandErrorGuidance.notFound(context, args, "BDD_FEATURE_NOT_FOUND",
                        "BDD feature", featureFilter,
                        "dhk bdd show --feature <feature>",
                        "docs/GOAL_CONFIGURATION.md");
            }
            List<BddScenarioView> views = scenarioViews(connection, project, features);
            String contextMarkdown = guard(renderer.renderContext(features, views, context.clock().now().toString()));
            Files.createDirectories(PathUtil.bddExportsDirectory(projectRoot));
            Files.createDirectories(PathUtil.bddFeaturesDirectory(projectRoot));
            Files.write(PathUtil.bddContext(projectRoot), contextMarkdown.getBytes("UTF-8"));
            int featureFiles = 0;
            for (BddFeature feature : features) {
                String featureText = guard(renderer.renderFeature(feature, views));
                Files.write(featurePath(projectRoot, feature), featureText.getBytes("UTF-8"));
                featureFiles++;
            }
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "bdd export"),
                        JsonOutput.stringField("context_path", PathUtil.bddContext(projectRoot).toString()),
                        JsonOutput.numberField("features", features.size()),
                        JsonOutput.numberField("scenarios", views.size()),
                        JsonOutput.numberField("feature_files", featureFiles)
                ));
            } else {
                context.out().println("bdd export complete");
                context.out().println("context_path: " + PathUtil.bddContext(projectRoot));
                context.out().println("features: " + features.size());
                context.out().println("scenarios: " + views.size());
                context.out().println("feature_files: " + featureFiles);
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR bdd export failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private List<BddFeature> selectedFeatures(Connection connection, Project project, String featureFilter)
            throws Exception {
        if (featureFilter.length() > 0) {
            List<BddFeature> result = new ArrayList<BddFeature>();
            BddFeature feature = bddService.findFeature(connection, project, featureFilter);
            if (feature != null) {
                result.add(feature);
            }
            return result;
        }
        return bddService.listFeatures(connection, project);
    }

    private List<BddScenarioView> scenarioViews(Connection connection, Project project,
                                                List<BddFeature> features) throws Exception {
        List<BddScenarioView> views = new ArrayList<BddScenarioView>();
        for (BddFeature feature : features) {
            List<BddScenario> scenarios = bddService.listScenarios(connection, project, feature.featureKey());
            for (BddScenario scenario : scenarios) {
                BddScenarioView view = bddService.findScenario(connection, project, scenario.scenarioKey());
                if (view != null) {
                    views.add(view);
                }
            }
        }
        return views;
    }

    private Path featurePath(Path projectRoot, BddFeature feature) {
        return PathUtil.bddFeaturesDirectory(projectRoot).resolve(feature.featureKey() + ".feature");
    }

    private String guard(String text) {
        String redacted = sensitiveDataGuard.redact(text);
        List<String> matches = sensitiveDataGuard.findMatches(redacted);
        if (!matches.isEmpty()) {
            throw new IllegalStateException("BDD export contains sensitive data: " + matches);
        }
        return redacted;
    }
}
