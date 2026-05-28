package com.devharnesskit.dhk.command.spec;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.guidance.ActionableError;
import com.devharnesskit.dhk.guidance.ActionableErrorRenderer;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.spec.SpecAcceptance;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.spec.SpecAcceptanceRepository;
import com.devharnesskit.dhk.repository.spec.SpecChangeRepository;
import com.devharnesskit.dhk.repository.spec.SpecEventRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.spec.SpecAcceptanceService;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;
import java.sql.Connection;

public final class SpecAcceptanceCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final SpecChangeRepository changeRepository = new SpecChangeRepository();
    private final SpecAcceptanceRepository acceptanceRepository = new SpecAcceptanceRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();
    private final SpecAcceptanceService acceptanceService =
            new SpecAcceptanceService(acceptanceRepository, new SpecEventRepository());
    private final ActionableErrorRenderer actionableErrorRenderer = new ActionableErrorRenderer();

    public int run(CommandContext context, Args args) {
        String action = args.positional(2);
        if ("add".equals(action)) {
            return add(context, args);
        }
        if ("update".equals(action)) {
            return update(context, args);
        }
        if ("statuses".equals(action) || "status-values".equals(action)) {
            return statuses(context);
        }
        return com.devharnesskit.dhk.guidance.EnumGuidance.printInvalid(context, args,
                "INVALID_SPEC_ACCEPTANCE_ACTION", "spec acceptance action", action,
                new String[]{"add", "update", "statuses"}, new String[0],
                "dhk spec acceptance statuses", "docs/GOAL_CONFIGURATION.md");
    }

    private int add(CommandContext context, Args args) {
        String changeKey = args.option("change").trim();
        String acceptanceKey = args.option("acceptance").trim();
        String description = args.option("description").trim();
        if (changeKey.length() == 0 || acceptanceKey.length() == 0 || description.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "SPEC_ACCEPTANCE_ADD_ARGUMENTS_MISSING",
                    new String[]{"--change", "--acceptance", "--description"},
                    "dhk spec acceptance add --change <change> --acceptance <key> --description \"<description>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        if (!SpecCommandSupport.isKeyAllowed(acceptanceKey)) {
            return CommandErrorGuidance.invalidKey(context, args, "INVALID_SPEC_ACCEPTANCE_KEY",
                    "spec acceptance key", acceptanceKey,
                    "dhk spec acceptance add --change <change> --acceptance acceptance-1 --description \"<description>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        String expected = args.option("expected", args.option("expected-result", "")).trim();
        if (SpecCommandSupport.rejectSensitive(context, sensitiveDataGuard, "spec acceptance add",
                changeKey, acceptanceKey, description, expected)) {
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = SpecCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = SpecCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            SpecChange change = changeRepository.findByKey(connection, changeKey);
            if (!SpecCommandSupport.belongsToProject(change, project)) {
                return CommandErrorGuidance.notFound(context, args, "SPEC_CHANGE_NOT_FOUND",
                        "spec change", changeKey, "dhk spec status --change <change>",
                        "docs/GOAL_CONFIGURATION.md");
            }
            final SpecChange selectedChange = change;
            final String selectedAcceptance = acceptanceKey;
            final String selectedDescription = description;
            final String selectedExpected = expected;
            SpecAcceptance acceptance = transactionTemplate.execute(connection,
                    new TransactionTemplate.Work<SpecAcceptance>() {
                        public SpecAcceptance execute() throws Exception {
                            return acceptanceService.addAcceptance(connection, selectedChange,
                                    selectedAcceptance, selectedDescription, selectedExpected,
                                    context.clock().now().toString());
                        }
                    });
            context.out().println("acceptance_key: " + acceptance.acceptanceKey());
            context.out().println("status: " + acceptance.status());
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR spec acceptance add failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private int update(CommandContext context, Args args) {
        String changeKey = args.option("change").trim();
        String acceptanceKey = args.option("acceptance").trim();
        String status = args.option("status").trim();
        if (changeKey.length() == 0 || acceptanceKey.length() == 0 || status.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "SPEC_ACCEPTANCE_UPDATE_ARGUMENTS_MISSING",
                    new String[]{"--change", "--acceptance", "--status"},
                    "dhk spec acceptance update --change <change> --acceptance <key> --status passed --evidence \"<evidence>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        String normalizedStatus = SpecAcceptanceService.normalizeAcceptanceStatus(status);
        if (normalizedStatus.length() == 0) {
            ActionableError error = ActionableError.builder("INVALID_SPEC_ACCEPTANCE_STATUS",
                            "Invalid acceptance status: " + status)
                    .reason("Spec acceptance status must be one of the supported lifecycle values.")
                    .validValues(SpecAcceptanceService.ALLOWED_STATUSES)
                    .aliases(SpecAcceptanceService.STATUS_ALIASES)
                    .nextCommand("dhk spec acceptance statuses")
                    .docs("docs/GOAL_CONFIGURATION.md#spec-acceptance")
                    .detail("example", "dhk spec acceptance update --change " + changeKey
                            + " --acceptance " + acceptanceKey + " --status passed")
                    .build();
            if (JsonOutput.enabled(args)) {
                context.out().print(actionableErrorRenderer.renderJson(error));
            } else {
                context.err().print(actionableErrorRenderer.renderText(error));
            }
            return ExitCodes.VALIDATION_ERROR;
        }
        String evidence = args.option("evidence", args.option("reason", "")).trim();
        if ("waived".equals(normalizedStatus) && evidence.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "SPEC_ACCEPTANCE_WAIVE_EVIDENCE_MISSING",
                    new String[]{"--evidence or --reason"},
                    "dhk spec acceptance update --change " + changeKey + " --acceptance "
                            + acceptanceKey + " --status waived --reason \"<reason>\"",
                    "docs/GOAL_CONFIGURATION.md");
        }
        if (SpecCommandSupport.rejectSensitive(context, sensitiveDataGuard, "spec acceptance update",
                changeKey, acceptanceKey, status, evidence)) {
            return ExitCodes.VALIDATION_ERROR;
        }
        Path projectRoot = SpecCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            Project project = SpecCommandSupport.ensureProject(context, projectRoot, connection,
                    projectService, projectRepository, migrationRunner);
            SpecChange change = changeRepository.findByKey(connection, changeKey);
            if (!SpecCommandSupport.belongsToProject(change, project)) {
                return CommandErrorGuidance.notFound(context, args, "SPEC_CHANGE_NOT_FOUND",
                        "spec change", changeKey, "dhk spec status --change <change>",
                        "docs/GOAL_CONFIGURATION.md");
            }
            SpecAcceptance acceptance = acceptanceRepository.findByKey(connection, changeKey, acceptanceKey);
            if (acceptance == null) {
                return CommandErrorGuidance.notFound(context, args, "SPEC_ACCEPTANCE_NOT_FOUND",
                        "spec acceptance", acceptanceKey,
                        "dhk spec acceptance add --change " + changeKey
                                + " --acceptance <key> --description \"<description>\"",
                        "docs/GOAL_CONFIGURATION.md");
            }
            final SpecChange selectedChange = change;
            final SpecAcceptance selectedAcceptance = acceptance;
            final String selectedStatus = normalizedStatus;
            final String selectedEvidence = evidence.length() == 0 ? acceptance.evidence() : evidence;
            transactionTemplate.execute(connection, new TransactionTemplate.Work<Void>() {
                public Void execute() throws Exception {
                    acceptanceService.updateAcceptance(connection, selectedChange, selectedAcceptance,
                            selectedStatus, selectedEvidence, context.clock().now().toString());
                    return null;
                }
            });
            context.out().println("acceptance_key: " + acceptanceKey);
            context.out().println("status: " + normalizedStatus);
            if (!normalizedStatus.equals(status)) {
                context.out().println("normalized_from: " + status);
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR spec acceptance update failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private int statuses(CommandContext context) {
        context.out().println("allowed_acceptance_statuses: " + SpecAcceptanceService.ALLOWED_STATUS_TEXT);
        context.out().println("aliases: " + SpecAcceptanceService.STATUS_ALIAS_TEXT);
        context.out().println("closed_statuses: passed, waived");
        return ExitCodes.SUCCESS;
    }
}
