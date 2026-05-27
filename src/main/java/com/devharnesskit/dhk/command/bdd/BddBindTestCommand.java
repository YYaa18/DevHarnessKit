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

public final class BddBindTestCommand implements Command {
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
        String bindingKey = bindingKey(args);
        String relation = args.option("relation", "verifies").trim();
        if (scenarioKey.length() == 0 || bindingKey.length() == 0) {
            context.err().println("Missing required parameters: --scenario and --test or --class");
            return ExitCodes.USAGE_ERROR;
        }
        if (!BddService.isKeyAllowed(scenarioKey) || !BddService.isKeyAllowed(relation)) {
            context.err().println("Invalid BDD key or relation. Use letters, numbers, dot, underscore, or dash.");
            return ExitCodes.VALIDATION_ERROR;
        }
        if (BddCommandSupport.rejectSensitive(context, sensitiveDataGuard, "bdd bind-test",
                scenarioKey, bindingKey, relation)) {
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
            final String selectedBindingKey = bindingKey;
            final String selectedRelation = relation;
            final String metadata = metadata(args);
            BddBinding binding = transactionTemplate.execute(connection,
                    new TransactionTemplate.Work<BddBinding>() {
                        public BddBinding execute() throws Exception {
                            return bddService.bindScenario(connection, project, selectedScenarioKey,
                                    "test", selectedBindingKey, selectedRelation, metadata,
                                    context.clock().now().toString());
                        }
                    });
            printResult(context, args, binding);
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR bdd bind-test failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private String bindingKey(Args args) {
        String test = args.option("test", "").trim();
        if (test.length() > 0) {
            return test;
        }
        String className = args.option("class", "").trim();
        String method = args.option("method", "").trim();
        if (className.length() == 0) {
            return "";
        }
        return method.length() == 0 ? className : className + "#" + method;
    }

    private String metadata(Args args) {
        String metadata = args.option("metadata", "").trim();
        if (metadata.length() > 0) {
            return metadata;
        }
        String className = args.option("class", "").trim();
        String method = args.option("method", "").trim();
        if (className.length() == 0) {
            return "";
        }
        return method.length() == 0 ? "class=" + className : "class=" + className + ";method=" + method;
    }

    private void printResult(CommandContext context, Args args, BddBinding binding) {
        if (JsonOutput.enabled(args)) {
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", "bdd bind-test"),
                    JsonOutput.stringField("scenario_key", binding.scenarioKey()),
                    JsonOutput.stringField("binding_type", binding.bindingType()),
                    JsonOutput.stringField("binding_key", binding.bindingKey()),
                    JsonOutput.stringField("relation", binding.relation())
            ));
            return;
        }
        context.out().println("bdd bind-test complete");
        context.out().println("scenario_key: " + binding.scenarioKey());
        context.out().println("binding_type: " + binding.bindingType());
        context.out().println("binding_key: " + binding.bindingKey());
        context.out().println("relation: " + binding.relation());
    }
}
