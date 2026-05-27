package com.devharnesskit.dhk.service.graph;

import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.export.ScenarioGraphImpactRenderer;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.bdd.BddBinding;
import com.devharnesskit.dhk.model.bdd.BddScenarioView;
import com.devharnesskit.dhk.model.graph.GraphImpactRequest;
import com.devharnesskit.dhk.model.graph.GraphImpactResult;
import com.devharnesskit.dhk.model.graph.ScenarioGraphImpactResult;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.bdd.BddBindingRepository;
import com.devharnesskit.dhk.repository.bdd.BddEvidenceRepository;
import com.devharnesskit.dhk.repository.bdd.BddFeatureRepository;
import com.devharnesskit.dhk.repository.bdd.BddScenarioRepository;
import com.devharnesskit.dhk.repository.bdd.BddStepRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.bdd.BddService;
import com.devharnesskit.dhk.util.Clock;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public final class ScenarioGraphImpactService {
    private final DbConnectionFactory connectionFactory;
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final MigrationRunner migrationRunner;
    private final BddService bddService;
    private final GraphImpactService graphImpactService;
    private final ScenarioGraphImpactRenderer renderer;

    public ScenarioGraphImpactService() {
        this(new DbConnectionFactory(), new ProjectService(), new ProjectRepository(), new MigrationRunner(),
                new BddService(new BddFeatureRepository(), new BddScenarioRepository(), new BddStepRepository(),
                        new BddBindingRepository(), new BddEvidenceRepository()),
                new GraphImpactService(), new ScenarioGraphImpactRenderer());
    }

    ScenarioGraphImpactService(DbConnectionFactory connectionFactory, ProjectService projectService,
                               ProjectRepository projectRepository, MigrationRunner migrationRunner,
                               BddService bddService, GraphImpactService graphImpactService,
                               ScenarioGraphImpactRenderer renderer) {
        this.connectionFactory = connectionFactory;
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.migrationRunner = migrationRunner;
        this.bddService = bddService;
        this.graphImpactService = graphImpactService;
        this.renderer = renderer;
    }

    public ScenarioGraphImpactResult impact(Path projectRoot, String scenarioKey, int depth,
                                            boolean allowStale, String allowStaleEvidence,
                                            Clock clock) throws Exception {
        PathUtil.createMemoryDirectories(projectRoot);
        PathUtil.createBddDirectories(projectRoot);
        PathUtil.createGraphDirectories(projectRoot);

        List<BddBinding> inputs;
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = projectService.ensureProject(projectRoot, clock);
            migrationRunner.migrate(connection, clock);
            projectRepository.upsert(connection, project);
            BddScenarioView scenario = bddService.findScenario(connection, project, scenarioKey);
            if (scenario == null) {
                throw new IllegalStateException("BDD scenario not found: " + scenarioKey);
            }
            inputs = graphInputBindings(scenario.bindings());
        }
        if (inputs.isEmpty()) {
            throw new IllegalStateException("No graph impact bindings found for scenario " + scenarioKey
                    + ". Run `dhk bdd bind-graph --scenario " + scenarioKey
                    + " --file <path>|--symbol <symbol>|--sql-table <table>` first.");
        }

        List<GraphImpactResult> results = new ArrayList<GraphImpactResult>();
        for (BddBinding binding : inputs) {
            GraphImpactRequest request = new GraphImpactRequest(queryType(binding), binding.bindingKey(),
                    depth, allowStale, allowStaleEvidence);
            results.add(graphImpactService.impact(projectRoot, request, clock));
        }
        ScenarioGraphImpactResult result = new ScenarioGraphImpactResult(scenarioKey, inputs, results,
                PathUtil.scenarioImpactMap(projectRoot));
        Files.write(result.scenarioImpactMapPath(), renderer.render(result, clock.now()).getBytes("UTF-8"));
        return result;
    }

    private List<BddBinding> graphInputBindings(List<BddBinding> bindings) {
        List<BddBinding> inputs = new ArrayList<BddBinding>();
        for (BddBinding binding : bindings) {
            if ("file".equals(binding.bindingType()) || "symbol".equals(binding.bindingType())
                    || "sql_table".equals(binding.bindingType())) {
                inputs.add(binding);
            }
        }
        return inputs;
    }

    private String queryType(BddBinding binding) {
        if ("sql_table".equals(binding.bindingType())) {
            return "sql-table";
        }
        return binding.bindingType();
    }
}
