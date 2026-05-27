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
import com.devharnesskit.dhk.model.spec.SpecAcceptance;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.bdd.BddFeatureRepository;
import com.devharnesskit.dhk.repository.bdd.BddScenarioRepository;
import com.devharnesskit.dhk.repository.bdd.BddStepRepository;
import com.devharnesskit.dhk.repository.spec.SpecAcceptanceRepository;
import com.devharnesskit.dhk.repository.spec.SpecChangeRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.bdd.BddService;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;
import java.sql.Connection;

public final class BddBindSpecCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final SpecChangeRepository changeRepository = new SpecChangeRepository();
    private final SpecAcceptanceRepository acceptanceRepository = new SpecAcceptanceRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();
    private final BddService bddService = new BddService(new BddFeatureRepository(),
            new BddScenarioRepository(), new BddStepRepository());

    public int run(CommandContext context, Args args) {
        String scenarioKey = args.option("scenario").trim();
        String changeKey = args.option("change").trim();
        String acceptanceKey = args.option("acceptance").trim();
        String relation = defaultIfBlank(args.option("relation", "verifies"), "verifies");
        if (scenarioKey.length() == 0 || changeKey.length() == 0 || acceptanceKey.length() == 0) {
            context.err().println("Missing required parameters: --scenario, --change, --acceptance");
            return ExitCodes.USAGE_ERROR;
        }
        if (!BddService.isKeyAllowed(scenarioKey) || !BddService.isKeyAllowed(changeKey)
                || !BddService.isKeyAllowed(acceptanceKey) || !BddService.isKeyAllowed(relation)) {
            context.err().println("Invalid BDD/spec key. Use letters, numbers, dot, underscore, or dash.");
            return ExitCodes.VALIDATION_ERROR;
        }
        if (BddCommandSupport.rejectSensitive(context, sensitiveDataGuard, "bdd bind-spec",
                scenarioKey, changeKey, acceptanceKey, relation)) {
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
            SpecChange change = changeRepository.findByKey(connection, changeKey);
            if (change == null || !project.projectKey().equals(change.projectKey())) {
                context.err().println("Spec change not found: " + changeKey);
                return ExitCodes.NOT_FOUND;
            }
            SpecAcceptance acceptance = acceptanceRepository.findByKey(connection, changeKey, acceptanceKey);
            if (acceptance == null) {
                context.err().println("Spec acceptance not found: " + acceptanceKey);
                return ExitCodes.NOT_FOUND;
            }
            final String selectedScenarioKey = scenarioKey;
            final String selectedBindingKey = changeKey + ":" + acceptanceKey;
            final String selectedRelation = relation;
            final String selectedMetadata = "change_key=" + changeKey + ";acceptance_key=" + acceptanceKey;
            BddBinding binding = transactionTemplate.execute(connection,
                    new TransactionTemplate.Work<BddBinding>() {
                        public BddBinding execute() throws Exception {
                            return bddService.bindScenario(connection, project, selectedScenarioKey,
                                    "spec_acceptance", selectedBindingKey, selectedRelation,
                                    selectedMetadata, context.clock().now().toString());
                        }
                    });
            printResult(context, args, binding, changeKey, acceptanceKey);
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR bdd bind-spec failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printResult(CommandContext context, Args args, BddBinding binding,
                             String changeKey, String acceptanceKey) {
        if (JsonOutput.enabled(args)) {
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", "bdd bind-spec"),
                    JsonOutput.stringField("scenario_key", binding.scenarioKey()),
                    JsonOutput.stringField("binding_type", binding.bindingType()),
                    JsonOutput.stringField("binding_key", binding.bindingKey()),
                    JsonOutput.stringField("relation", binding.relation()),
                    JsonOutput.stringField("change_key", changeKey),
                    JsonOutput.stringField("acceptance_key", acceptanceKey)
            ));
            return;
        }
        context.out().println("bdd bind-spec complete");
        context.out().println("scenario_key: " + binding.scenarioKey());
        context.out().println("binding_type: " + binding.bindingType());
        context.out().println("binding_key: " + binding.bindingKey());
        context.out().println("relation: " + binding.relation());
        context.out().println("change_key: " + changeKey);
        context.out().println("acceptance_key: " + acceptanceKey);
    }

    private String defaultIfBlank(String value, String fallback) {
        String text = value == null ? "" : value.trim();
        return text.length() == 0 ? fallback : text;
    }
}
