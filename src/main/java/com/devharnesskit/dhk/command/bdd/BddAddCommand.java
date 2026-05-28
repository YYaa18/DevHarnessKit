package com.devharnesskit.dhk.command.bdd;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.guidance.EnumGuidance;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.bdd.BddScenarioView;
import com.devharnesskit.dhk.model.bdd.BddStep;
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
import java.util.ArrayList;
import java.util.List;

public final class BddAddCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();
    private final BddService bddService = new BddService(new BddFeatureRepository(),
            new BddScenarioRepository(), new BddStepRepository());

    public int run(CommandContext context, Args args) {
        String feature = args.option("feature").trim();
        String title = args.option("title").trim();
        String scenario = args.option("scenario").trim();
        String scenarioTitle = args.option("scenario-title").trim();
        if (feature.length() == 0 || title.length() == 0
                || scenario.length() == 0 || scenarioTitle.length() == 0) {
            context.err().println("Missing required parameters: --feature, --title, --scenario, --scenario-title");
            return ExitCodes.USAGE_ERROR;
        }
        if (!BddService.isKeyAllowed(feature) || !BddService.isKeyAllowed(scenario)) {
            context.err().println("Invalid BDD key. Use letters, numbers, dot, underscore, or dash.");
            return ExitCodes.VALIDATION_ERROR;
        }
        String module = defaultIfBlank(args.option("module", "global"), "global");
        String description = args.option("description", "").trim();
        String scenarioDescription = args.option("scenario-description", "").trim();
        String type = defaultIfBlank(args.option("type", "acceptance"), "acceptance");
        String priority = defaultIfBlank(args.option("priority", "normal"), "normal");
        String status = args.option("status", "").trim();
        String tags = args.option("tags", "").trim();
        String given = args.option("given", "").trim();
        String when = args.option("when", "").trim();
        String then = args.option("then", "").trim();
        String andStep = args.option("and", "").trim();
        if (!BddService.isScenarioTypeAllowed(type)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_BDD_SCENARIO_TYPE",
                    "BDD scenario type", type, EnumGuidance.BDD_SCENARIO_TYPES, new String[0],
                    "dhk bdd add --feature <feature> --title \"<title>\" --scenario <scenario> --scenario-title \"<title>\" --type acceptance",
                    "docs/GOAL_CONFIGURATION.md");
        }
        if (!BddService.isPriorityAllowed(priority)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_BDD_PRIORITY", "BDD priority", priority,
                    EnumGuidance.SPEC_PRIORITIES, new String[0],
                    "dhk bdd add --feature <feature> --title \"<title>\" --scenario <scenario> --scenario-title \"<title>\" --priority normal",
                    "docs/GOAL_CONFIGURATION.md");
        }
        if (status.length() > 0 && !BddService.isScenarioStatusAllowed(status)) {
            return EnumGuidance.printInvalid(context, args, "INVALID_BDD_SCENARIO_STATUS",
                    "BDD scenario status", status, EnumGuidance.BDD_SCENARIO_STATUSES, new String[0],
                    "dhk bdd add --feature <feature> --title \"<title>\" --scenario <scenario> --scenario-title \"<title>\" --status active",
                    "docs/GOAL_CONFIGURATION.md");
        }
        if (BddCommandSupport.rejectSensitive(context, sensitiveDataGuard, "bdd add",
                feature, title, scenario, scenarioTitle, module, description,
                scenarioDescription, type, priority, status, tags, given, when, then, andStep)) {
            return ExitCodes.VALIDATION_ERROR;
        }

        final BddService.BddAddRequest request = new BddService.BddAddRequest(feature, title, description,
                scenario, scenarioTitle, scenarioDescription, module, type, priority, status,
                tags, given, when, then, andStep);
        Path projectRoot = BddCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            final Project project = BddCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            BddScenarioView created = transactionTemplate.execute(connection,
                    new TransactionTemplate.Work<BddScenarioView>() {
                        public BddScenarioView execute() throws Exception {
                            return bddService.addScenario(connection, project, request,
                                    context.clock().now().toString());
                        }
                    });
            if (JsonOutput.enabled(args)) {
                context.out().print(JsonOutput.object(
                        JsonOutput.stringField("command", "bdd add"),
                        JsonOutput.stringField("feature_key", created.feature().featureKey()),
                        JsonOutput.stringField("scenario_key", created.scenario().scenarioKey()),
                        JsonOutput.stringField("status", created.scenario().status()),
                        JsonOutput.numberField("step_count", created.steps().size()),
                        JsonOutput.rawField("steps", stepsJson(created.steps()))
                ));
            } else {
                context.out().println("feature_key: " + created.feature().featureKey());
                context.out().println("scenario_key: " + created.scenario().scenarioKey());
                context.out().println("status: " + created.scenario().status());
                context.out().println("steps: " + created.steps().size());
                if (!request.hasCoreSteps()) {
                    context.out().println("lint_warning: missing Given/When/Then; scenario kept draft");
                }
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR bdd add failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
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

    private String defaultIfBlank(String value, String fallback) {
        String text = value == null ? "" : value.trim();
        return text.length() == 0 ? fallback : text;
    }
}
