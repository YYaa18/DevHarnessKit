package com.devharnesskit.dhk.command.bdd;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.export.BddEvidenceRenderer;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.model.bdd.BddAdapterResult;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.bdd.BddFeatureRepository;
import com.devharnesskit.dhk.repository.bdd.BddScenarioRepository;
import com.devharnesskit.dhk.repository.bdd.BddStepRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.bdd.BddService;
import com.devharnesskit.dhk.service.bdd.BddVerificationService;
import com.devharnesskit.dhk.service.bdd.BddVerificationService.BddScenarioEvidenceResult;
import com.devharnesskit.dhk.service.bdd.BddVerificationService.BddVerificationResult;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public final class BddVerifyCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final BddEvidenceRenderer renderer = new BddEvidenceRenderer();
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
            return CommandErrorGuidance.invalidKey(context, args, "BDD_VERIFY_INVALID_KEY",
                    "BDD/goal key", featureKey + "/" + scenarioKey + "/" + goalKey,
                    "dhk bdd verify --scenario <scenario>",
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
            writeReports(projectRoot, result, context.clock().now().toString());
            if (JsonOutput.enabled(args)) {
                context.out().print(resultJson("bdd verify", result));
            } else {
                printHuman(context, result);
            }
            return result.passed() ? ExitCodes.SUCCESS : ExitCodes.VALIDATION_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR bdd verify failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printHuman(CommandContext context, BddVerificationResult result) {
        context.out().println(result.passed() ? "bdd verify passed" : "bdd verify failed");
        context.out().println("scenario_count: " + result.scenarioCount());
        context.out().println("covered_count: " + result.coveredCount());
        context.out().println("missing_evidence: " + result.missingCount());
        context.out().println("pending_evidence: " + result.pendingCount());
        context.out().println("failed_evidence: " + result.failedCount());
        for (BddScenarioEvidenceResult scenario : result.scenarios()) {
            context.out().println("- " + scenario.view().scenario().scenarioKey()
                    + " [" + scenario.status() + "] "
                    + scenario.view().scenario().title());
            if (scenario.adapterResult() != null) {
                context.out().println("  adapter: " + scenario.adapterResult().adapterKey()
                        + " normalized_status=" + scenario.adapterResult().normalizedStatus());
            }
        }
        context.out().println("evidence_path: " + PathUtil.BDD_EVIDENCE);
        context.out().println("coverage_path: " + PathUtil.BDD_COVERAGE);
    }

    String resultJson(String command, BddVerificationResult result) {
        return JsonOutput.object(
                JsonOutput.stringField("command", command),
                JsonOutput.booleanField("passed", result.passed()),
                JsonOutput.numberField("scenario_count", result.scenarioCount()),
                JsonOutput.numberField("covered_count", result.coveredCount()),
                JsonOutput.numberField("missing_evidence", result.missingCount()),
                JsonOutput.numberField("pending_evidence", result.pendingCount()),
                JsonOutput.numberField("failed_evidence", result.failedCount()),
                JsonOutput.rawField("scenarios", scenariosJson(result.scenarios()))
        );
    }

    private String scenariosJson(List<BddScenarioEvidenceResult> scenarios) {
        List<String> values = new ArrayList<String>();
        for (BddScenarioEvidenceResult scenario : scenarios) {
            BddAdapterResult adapterResult = scenario.adapterResult();
            values.add(JsonOutput.object(
                    JsonOutput.stringField("scenario_key", scenario.view().scenario().scenarioKey()),
                    JsonOutput.stringField("title", scenario.view().scenario().title()),
                    JsonOutput.stringField("status", scenario.status()),
                    JsonOutput.booleanField("covered", scenario.covered()),
                    JsonOutput.numberField("evidence_count", scenario.evidence().size()),
                    JsonOutput.stringField("adapter_key", adapterResult == null ? "" : adapterResult.adapterKey()),
                    JsonOutput.stringField("adapter_normalized_status",
                            adapterResult == null ? "" : adapterResult.normalizedStatus())
            ).trim());
        }
        return JsonOutput.array(values);
    }

    void writeReports(Path projectRoot, BddVerificationResult result, String generatedAt) throws Exception {
        Files.createDirectories(PathUtil.bddExportsDirectory(projectRoot));
        String evidence = guard(renderer.renderEvidence(result, generatedAt));
        String coverage = guard(renderer.renderCoverage(result, generatedAt));
        Files.write(PathUtil.bddEvidence(projectRoot), evidence.getBytes("UTF-8"));
        Files.write(PathUtil.bddCoverage(projectRoot), coverage.getBytes("UTF-8"));
    }

    private String guard(String text) {
        List<String> matches = sensitiveDataGuard.findMatches(text);
        if (!matches.isEmpty()) {
            throw new IllegalStateException("BDD verify export contains sensitive data: " + matches);
        }
        return text;
    }
}
