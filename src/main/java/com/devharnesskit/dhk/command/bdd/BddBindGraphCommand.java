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
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.bdd.BddFeatureRepository;
import com.devharnesskit.dhk.repository.bdd.BddScenarioRepository;
import com.devharnesskit.dhk.repository.bdd.BddStepRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.bdd.BddService;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;
import java.sql.Connection;

public final class BddBindGraphCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();
    private final BddService bddService = new BddService(new BddFeatureRepository(),
            new BddScenarioRepository(), new BddStepRepository());

    public int run(CommandContext context, Args args) {
        String scenarioKey = args.option("scenario").trim();
        String relation = defaultIfBlank(args.option("relation", "impacts"), "impacts");
        GraphTarget target = target(args);
        if (scenarioKey.length() == 0 || target == null) {
            context.err().println("Missing required parameters: --scenario and exactly one of --file, --symbol, --sql-table");
            return ExitCodes.USAGE_ERROR;
        }
        if (!BddService.isKeyAllowed(scenarioKey) || !BddService.isKeyAllowed(relation)) {
            context.err().println("Invalid BDD graph key. Scenario and relation use letters, numbers, dot, underscore, or dash.");
            return ExitCodes.VALIDATION_ERROR;
        }
        if (BddCommandSupport.rejectSensitive(context, sensitiveDataGuard, "bdd bind-graph",
                scenarioKey, target.bindingType, target.bindingKey, relation, args.option("metadata", ""))) {
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
            final String selectedScenarioKey = scenarioKey;
            final String selectedRelation = relation;
            final String selectedMetadata = args.option("metadata", "").trim();
            final GraphTarget selectedTarget = target;
            BddBinding binding = transactionTemplate.execute(connection,
                    new TransactionTemplate.Work<BddBinding>() {
                        public BddBinding execute() throws Exception {
                            return bddService.bindScenario(connection, project, selectedScenarioKey,
                                    selectedTarget.bindingType, selectedTarget.bindingKey,
                                    selectedRelation, selectedMetadata, context.clock().now().toString());
                        }
                    });
            printResult(context, args, binding);
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR bdd bind-graph failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private GraphTarget target(Args args) {
        String file = args.option("file", "").trim();
        String symbol = args.option("symbol", "").trim();
        String sqlTable = args.option("sql-table", "").trim();
        int count = (file.length() > 0 ? 1 : 0) + (symbol.length() > 0 ? 1 : 0) + (sqlTable.length() > 0 ? 1 : 0);
        if (count != 1) {
            return null;
        }
        if (file.length() > 0) {
            return new GraphTarget("file", file);
        }
        if (symbol.length() > 0) {
            return new GraphTarget("symbol", symbol);
        }
        return new GraphTarget("sql_table", sqlTable);
    }

    private void printResult(CommandContext context, Args args, BddBinding binding) {
        if (JsonOutput.enabled(args)) {
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", "bdd bind-graph"),
                    JsonOutput.stringField("scenario_key", binding.scenarioKey()),
                    JsonOutput.stringField("binding_type", binding.bindingType()),
                    JsonOutput.stringField("binding_key", binding.bindingKey()),
                    JsonOutput.stringField("relation", binding.relation())
            ));
            return;
        }
        context.out().println("bdd bind-graph complete");
        context.out().println("scenario_key: " + binding.scenarioKey());
        context.out().println("binding_type: " + binding.bindingType());
        context.out().println("binding_key: " + binding.bindingKey());
        context.out().println("relation: " + binding.relation());
    }

    private String defaultIfBlank(String value, String fallback) {
        String text = value == null ? "" : value.trim();
        return text.length() == 0 ? fallback : text;
    }

    private static final class GraphTarget {
        private final String bindingType;
        private final String bindingKey;

        private GraphTarget(String bindingType, String bindingKey) {
            this.bindingType = bindingType;
            this.bindingKey = bindingKey;
        }
    }
}
