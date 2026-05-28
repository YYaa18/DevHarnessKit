package com.devharnesskit.dhk.command.bdd;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.bdd.BddFeatureRepository;
import com.devharnesskit.dhk.repository.bdd.BddScenarioRepository;
import com.devharnesskit.dhk.repository.bdd.BddStepRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.bdd.BddService;
import com.devharnesskit.dhk.service.bdd.BddVerificationService;
import com.devharnesskit.dhk.service.bdd.BddVerificationService.BddVerificationResult;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;

public final class BddCoverageCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final BddVerifyCommand reportWriter = new BddVerifyCommand();
    private final BddVerificationService verificationService = new BddVerificationService();
    private final BddService bddService = new BddService(new BddFeatureRepository(),
            new BddScenarioRepository(), new BddStepRepository());

    public int run(CommandContext context, Args args) {
        String featureKey = args.option("feature", "").trim();
        String scenarioKey = args.option("scenario", "").trim();
        String goalKey = args.option("goal", "").trim();
        if (featureKey.length() > 0 && !BddService.isKeyAllowed(featureKey)
                || scenarioKey.length() > 0 && !BddService.isKeyAllowed(scenarioKey)
                || goalKey.length() > 0 && !BddService.isKeyAllowed(goalKey)) {
            return CommandErrorGuidance.invalidKey(context, args, "BDD_COVERAGE_INVALID_KEY",
                    "BDD/goal key", featureKey + "/" + scenarioKey + "/" + goalKey,
                    "dhk bdd coverage --scenario <scenario>",
                    "docs/GOAL_CONFIGURATION.md");
        }
        Path projectRoot = BddCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = BddCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            BddVerificationResult result = verificationService.evaluate(connection, project, bddService,
                    featureKey, scenarioKey, goalKey);
            if ((featureKey.length() > 0 || scenarioKey.length() > 0) && result.scenarioCount() == 0) {
                return CommandErrorGuidance.notFound(context, args, "BDD_SCENARIO_SELECTION_NOT_FOUND",
                        "BDD scenario selection",
                        scenarioKey.length() > 0 ? scenarioKey : featureKey,
                        "dhk bdd show --feature <feature>",
                        "docs/GOAL_CONFIGURATION.md");
            }
            reportWriter.writeReports(projectRoot, result, context.clock().now().toString());
            if (JsonOutput.enabled(args)) {
                context.out().print(reportWriter.resultJson("bdd coverage", result));
            } else {
                context.out().println("bdd coverage complete");
                context.out().println("scenario_count: " + result.scenarioCount());
                context.out().println("covered_count: " + result.coveredCount());
                context.out().println("missing_evidence: " + result.missingCount());
                context.out().println("pending_evidence: " + result.pendingCount());
                context.out().println("failed_evidence: " + result.failedCount());
                context.out().println("coverage_path: " + PathUtil.bddCoverage(projectRoot));
                context.out().println("evidence_path: " + PathUtil.bddEvidence(projectRoot));
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR bdd coverage failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
