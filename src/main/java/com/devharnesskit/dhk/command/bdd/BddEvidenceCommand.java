package com.devharnesskit.dhk.command.bdd;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.bdd.BddEvidence;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.bdd.BddFeatureRepository;
import com.devharnesskit.dhk.repository.bdd.BddScenarioRepository;
import com.devharnesskit.dhk.repository.bdd.BddStepRepository;
import com.devharnesskit.dhk.repository.goal.GoalRunRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.bdd.BddService;
import com.devharnesskit.dhk.service.bdd.JsonReportBddEvidenceService;
import com.devharnesskit.dhk.service.bdd.JsonReportBddEvidenceService.ReportEvidenceImportResult;
import com.devharnesskit.dhk.service.bdd.JunitBddEvidenceService;
import com.devharnesskit.dhk.service.bdd.JunitBddEvidenceService.JunitEvidenceImportResult;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public final class BddEvidenceCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final GoalRunRepository goalRunRepository = new GoalRunRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();
    private final BddService bddService = new BddService(new BddFeatureRepository(),
            new BddScenarioRepository(), new BddStepRepository());
    private final JunitBddEvidenceService junitEvidenceService = new JunitBddEvidenceService();
    private final JsonReportBddEvidenceService jsonReportEvidenceService = new JsonReportBddEvidenceService();

    public int run(CommandContext context, Args args) {
        String action = args.positional(2);
        if ("add".equals(action)) {
            return add(context, args);
        }
        if ("junit".equals(action)) {
            return junit(context, args);
        }
        if ("report".equals(action)) {
            return report(context, args);
        }
        context.err().println("Unknown bdd evidence action: " + action);
        return ExitCodes.USAGE_ERROR;
    }

    private int add(CommandContext context, Args args) {
        String scenarioKey = args.option("scenario").trim();
        String goalKey = args.option("goal", "").trim();
        String type = defaultIfBlank(args.option("type", "manual"), "manual");
        String status = defaultIfBlank(args.option("status", "passed"), "passed");
        String path = args.option("path", "").trim();
        String summary = args.option("summary", "").trim();
        String command = args.option("command", "").trim();
        if (scenarioKey.length() == 0 || summary.length() == 0) {
            context.err().println("Missing required parameters: --scenario, --summary");
            return ExitCodes.USAGE_ERROR;
        }
        if (!BddService.isKeyAllowed(scenarioKey) || (goalKey.length() > 0 && !BddService.isKeyAllowed(goalKey))) {
            context.err().println("Invalid BDD/goal key. Use letters, numbers, dot, underscore, or dash.");
            return ExitCodes.VALIDATION_ERROR;
        }
        if (!BddService.isEvidenceTypeAllowed(type)) {
            context.err().println("Invalid BDD evidence type: " + type);
            return ExitCodes.VALIDATION_ERROR;
        }
        if (!BddService.isEvidenceStatusAllowed(status)) {
            context.err().println("Invalid BDD evidence status: " + status);
            return ExitCodes.VALIDATION_ERROR;
        }
        if (BddCommandSupport.rejectSensitive(context, sensitiveDataGuard, "bdd evidence add",
                scenarioKey, goalKey, type, status, path, summary, command)) {
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
            if (goalKey.length() > 0) {
                GoalRun goal = goalRunRepository.findByKey(connection, goalKey);
                if (goal == null || !project.projectKey().equals(goal.projectKey())) {
                    context.err().println("Goal run not found: " + goalKey);
                    return ExitCodes.NOT_FOUND;
                }
            }
            final String selectedScenarioKey = scenarioKey;
            final String selectedGoalKey = goalKey;
            final String selectedType = type;
            final String selectedStatus = status;
            final String selectedPath = path;
            final String selectedSummary = summary;
            final String selectedCommand = command;
            BddEvidence evidence = transactionTemplate.execute(connection,
                    new TransactionTemplate.Work<BddEvidence>() {
                        public BddEvidence execute() throws Exception {
                            return bddService.addEvidence(connection, project, selectedScenarioKey,
                                    selectedGoalKey, selectedType, selectedStatus, selectedPath,
                                    selectedSummary, selectedCommand, context.clock().now().toString());
                        }
                    });
            printEvidence(context, args, evidence);
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR bdd evidence add failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printEvidence(CommandContext context, Args args, BddEvidence evidence) {
        if (JsonOutput.enabled(args)) {
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", "bdd evidence add"),
                    JsonOutput.numberField("evidence_id", evidence.id()),
                    JsonOutput.stringField("scenario_key", evidence.scenarioKey()),
                    JsonOutput.stringField("goal_key", evidence.goalKey()),
                    JsonOutput.stringField("evidence_type", evidence.evidenceType()),
                    JsonOutput.stringField("status", evidence.status()),
                    JsonOutput.stringField("evidence_path", evidence.evidencePath()),
                    JsonOutput.stringField("summary", evidence.summary())
            ));
            return;
        }
        context.out().println("bdd evidence add complete");
        context.out().println("evidence_id: " + evidence.id());
        context.out().println("scenario_key: " + evidence.scenarioKey());
        context.out().println("goal_key: " + evidence.goalKey());
        context.out().println("evidence_type: " + evidence.evidenceType());
        context.out().println("status: " + evidence.status());
        if (evidence.evidencePath().length() > 0) {
            context.out().println("evidence_path: " + evidence.evidencePath());
        }
    }

    private int junit(CommandContext context, Args args) {
        String scenarioKey = args.option("scenario", "").trim();
        String goalKey = args.option("goal", "").trim();
        if (scenarioKey.length() > 0 && !BddService.isKeyAllowed(scenarioKey)
                || goalKey.length() > 0 && !BddService.isKeyAllowed(goalKey)) {
            context.err().println("Invalid BDD/goal key. Use letters, numbers, dot, underscore, or dash.");
            return ExitCodes.VALIDATION_ERROR;
        }
        if (BddCommandSupport.rejectSensitive(context, sensitiveDataGuard, "bdd evidence junit",
                scenarioKey, goalKey, args.option("reports", ""))) {
            return ExitCodes.VALIDATION_ERROR;
        }

        Path projectRoot = BddCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            final Project project = BddCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            if (goalKey.length() > 0) {
                GoalRun goal = goalRunRepository.findByKey(connection, goalKey);
                if (goal == null || !project.projectKey().equals(goal.projectKey())) {
                    context.err().println("Goal run not found: " + goalKey);
                    return ExitCodes.NOT_FOUND;
                }
            }
            final String selectedScenarioKey = scenarioKey;
            final String selectedGoalKey = goalKey;
            final List<Path> selectedReports = reportRoots(args);
            JunitEvidenceImportResult result = transactionTemplate.execute(connection,
                    new TransactionTemplate.Work<JunitEvidenceImportResult>() {
                        public JunitEvidenceImportResult execute() throws Exception {
                            return junitEvidenceService.importReports(connection, project, bddService,
                                    projectRoot, selectedReports, selectedScenarioKey, selectedGoalKey,
                                    context.clock().now().toString());
                        }
                    });
            if (result.scenarioCount() == 0) {
                context.err().println("BDD scenario selection not found");
                return ExitCodes.NOT_FOUND;
            }
            printJunitResult(context, args, result);
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR bdd evidence junit failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private int report(CommandContext context, Args args) {
        String scenarioKey = args.option("scenario", "").trim();
        String goalKey = args.option("goal", "").trim();
        String adapterKey = args.option("adapter", "").trim();
        String report = args.option("report", "").trim();
        if (scenarioKey.length() == 0 || adapterKey.length() == 0 || report.length() == 0) {
            context.err().println("Missing required parameters: --scenario, --adapter, --report");
            return ExitCodes.USAGE_ERROR;
        }
        if (!BddService.isKeyAllowed(scenarioKey) || (goalKey.length() > 0 && !BddService.isKeyAllowed(goalKey))
                || !BddService.isKeyAllowed(adapterKey)) {
            context.err().println("Invalid BDD/goal/adapter key. Use letters, numbers, dot, underscore, or dash.");
            return ExitCodes.VALIDATION_ERROR;
        }
        if (!jsonReportEvidenceService.isAdapterAllowed(adapterKey)) {
            context.err().println("Unsupported BDD report adapter: " + adapterKey);
            return ExitCodes.VALIDATION_ERROR;
        }
        if (BddCommandSupport.rejectSensitive(context, sensitiveDataGuard, "bdd evidence report",
                scenarioKey, goalKey, adapterKey, report)) {
            return ExitCodes.VALIDATION_ERROR;
        }

        Path projectRoot = BddCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            final Project project = BddCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            if (goalKey.length() > 0) {
                GoalRun goal = goalRunRepository.findByKey(connection, goalKey);
                if (goal == null || !project.projectKey().equals(goal.projectKey())) {
                    context.err().println("Goal run not found: " + goalKey);
                    return ExitCodes.NOT_FOUND;
                }
            }
            final String selectedScenarioKey = scenarioKey;
            final String selectedGoalKey = goalKey;
            final String selectedAdapterKey = adapterKey;
            final Path selectedReport = Paths.get(report);
            ReportEvidenceImportResult result = transactionTemplate.execute(connection,
                    new TransactionTemplate.Work<ReportEvidenceImportResult>() {
                        public ReportEvidenceImportResult execute() throws Exception {
                            return jsonReportEvidenceService.importReport(connection, project, bddService,
                                    projectRoot, selectedAdapterKey, selectedReport,
                                    selectedScenarioKey, selectedGoalKey, context.clock().now().toString());
                        }
                    });
            if (result.scenarioCount() == 0) {
                context.err().println("BDD scenario not found: " + scenarioKey);
                return ExitCodes.NOT_FOUND;
            }
            printReportResult(context, args, result);
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR bdd evidence report failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private List<Path> reportRoots(Args args) {
        List<Path> roots = new ArrayList<Path>();
        String reports = args.option("reports", "").trim();
        if (reports.length() == 0) {
            roots.add(Paths.get("target/surefire-reports"));
            roots.add(Paths.get("target/failsafe-reports"));
            return roots;
        }
        String[] parts = reports.split(",");
        for (String part : parts) {
            String path = part.trim();
            if (path.length() > 0) {
                roots.add(Paths.get(path));
            }
        }
        return roots;
    }

    private void printJunitResult(CommandContext context, Args args, JunitEvidenceImportResult result) {
        if (JsonOutput.enabled(args)) {
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", "bdd evidence junit"),
                    JsonOutput.numberField("scenario_count", result.scenarioCount()),
                    JsonOutput.numberField("binding_count", result.bindingCount()),
                    JsonOutput.numberField("report_result_count", result.reportResultCount()),
                    JsonOutput.numberField("evidence_count", result.evidenceCount()),
                    JsonOutput.numberField("passed_count", result.passedCount()),
                    JsonOutput.numberField("failed_count", result.failedCount()),
                    JsonOutput.numberField("skipped_count", result.skippedCount()),
                    JsonOutput.numberField("pending_count", result.pendingCount())
            ));
            return;
        }
        context.out().println("bdd evidence junit complete");
        context.out().println("scenario_count: " + result.scenarioCount());
        context.out().println("binding_count: " + result.bindingCount());
        context.out().println("report_result_count: " + result.reportResultCount());
        context.out().println("evidence_count: " + result.evidenceCount());
        context.out().println("passed_count: " + result.passedCount());
        context.out().println("failed_count: " + result.failedCount());
        context.out().println("skipped_count: " + result.skippedCount());
        context.out().println("pending_count: " + result.pendingCount());
    }

    private void printReportResult(CommandContext context, Args args, ReportEvidenceImportResult result) {
        if (JsonOutput.enabled(args)) {
            context.out().print(JsonOutput.object(
                    JsonOutput.stringField("command", "bdd evidence report"),
                    JsonOutput.stringField("adapter_key", result.adapterKey()),
                    JsonOutput.numberField("scenario_count", result.scenarioCount()),
                    JsonOutput.numberField("evidence_count", result.evidenceCount()),
                    JsonOutput.stringField("status", result.status()),
                    JsonOutput.stringField("report_path", result.reportPath())
            ));
            return;
        }
        context.out().println("bdd evidence report complete");
        context.out().println("adapter_key: " + result.adapterKey());
        context.out().println("scenario_count: " + result.scenarioCount());
        context.out().println("evidence_count: " + result.evidenceCount());
        context.out().println("status: " + result.status());
        if (result.reportPath().length() > 0) {
            context.out().println("report_path: " + result.reportPath());
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        String text = value == null ? "" : value.trim();
        return text.length() == 0 ? fallback : text;
    }
}
