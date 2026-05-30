package com.devharnesskit.dhk.command.bdd;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.bdd.BddBinding;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.bdd.BddFeatureRepository;
import com.devharnesskit.dhk.repository.bdd.BddScenarioRepository;
import com.devharnesskit.dhk.repository.bdd.BddStepRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.bdd.BddService;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;
import java.sql.Connection;

public final class BddBindWorkflowCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final WorkflowRunRepository workflowRunRepository = new WorkflowRunRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();
    private final BddService bddService = new BddService(new BddFeatureRepository(),
            new BddScenarioRepository(), new BddStepRepository());

    public int run(CommandContext context, Args args) {
        String scenarioKey = args.option("scenario").trim();
        String runKey = value(args.option("run"), args.option("workflow-run"));
        String relation = defaultIfBlank(args.option("relation", "validates"), "validates");
        if (scenarioKey.length() == 0 || runKey.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "BDD_BIND_WORKFLOW_ARGUMENTS_MISSING",
                    new String[]{"--scenario", "--run"},
                    "dhk bdd bind-workflow --scenario <scenario> --run <workflow-run>",
                    "docs/GOAL_CONFIGURATION.md");
        }
        if (!BddService.isKeyAllowed(scenarioKey) || !BddService.isKeyAllowed(runKey)
                || !BddService.isKeyAllowed(relation)) {
            return CommandErrorGuidance.invalidKey(context, args, "BDD_BIND_WORKFLOW_INVALID_KEY",
                    "BDD/workflow key", scenarioKey + "/" + runKey + "/" + relation,
                    "dhk bdd bind-workflow --scenario <scenario> --run <workflow-run>",
                    "docs/GOAL_CONFIGURATION.md");
        }
        if (BddCommandSupport.rejectSensitive(context, sensitiveDataGuard, "bdd bind-workflow",
                scenarioKey, runKey, relation)) {
            return ExitCodes.VALIDATION_ERROR;
        }

        Path projectRoot = BddCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            final Project project = BddCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            if (bddService.findScenario(connection, project, scenarioKey) == null) {
                return CommandErrorGuidance.notFound(context, args, "BDD_SCENARIO_NOT_FOUND",
                        "BDD scenario", scenarioKey, "dhk bdd show --scenario <scenario>",
                        "docs/GOAL_CONFIGURATION.md");
            }
            WorkflowRun run = workflowRunRepository.findByKey(connection, runKey);
            if (run == null || !project.projectKey().equals(run.projectKey())) {
                return CommandErrorGuidance.notFound(context, args, "WORKFLOW_RUN_NOT_FOUND",
                        "workflow run", runKey, "dhk workflow status --run <run>",
                        "docs/GOAL_CONFIGURATION.md");
            }
            final String selectedScenarioKey = scenarioKey;
            final String selectedRunKey = runKey;
            final String selectedRelation = relation;
            BddBinding binding = transactionTemplate.execute(connection,
                    new TransactionTemplate.Work<BddBinding>() {
                        public BddBinding execute() throws Exception {
                            return bddService.bindScenario(connection, project, selectedScenarioKey,
                                    "workflow", selectedRunKey, selectedRelation,
                                    "run_key=" + selectedRunKey, context.clock().now().toString());
                        }
                    });
            printResult(context, args, binding, runKey);
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR bdd bind-workflow failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printResult(CommandContext context, Args args, BddBinding binding, String runKey) {
        if (JsonOutput.enabled(args)) {
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", "bdd bind-workflow"),
                    JsonOutput.stringField("scenario_key", binding.scenarioKey()),
                    JsonOutput.stringField("binding_type", binding.bindingType()),
                    JsonOutput.stringField("binding_key", binding.bindingKey()),
                    JsonOutput.stringField("relation", binding.relation()),
                    JsonOutput.stringField("run_key", runKey)
            ));
            return;
        }
        context.out().println("bdd bind-workflow complete");
        context.out().println("scenario_key: " + binding.scenarioKey());
        context.out().println("binding_type: " + binding.bindingType());
        context.out().println("binding_key: " + binding.bindingKey());
        context.out().println("relation: " + binding.relation());
        context.out().println("run_key: " + runKey);
    }

    private String value(String preferred, String fallback) {
        String text = preferred == null ? "" : preferred.trim();
        if (text.length() > 0) {
            return text;
        }
        return fallback == null ? "" : fallback.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        String text = value == null ? "" : value.trim();
        return text.length() == 0 ? fallback : text;
    }
}
