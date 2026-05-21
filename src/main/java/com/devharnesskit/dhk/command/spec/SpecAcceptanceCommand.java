package com.devharnesskit.dhk.command.spec;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
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

    public int run(CommandContext context, Args args) {
        String action = args.positional(2);
        if ("add".equals(action)) {
            return add(context, args);
        }
        if ("update".equals(action)) {
            return update(context, args);
        }
        context.err().println("Unknown spec acceptance action: " + action);
        return ExitCodes.USAGE_ERROR;
    }

    private int add(CommandContext context, Args args) {
        String changeKey = args.option("change").trim();
        String acceptanceKey = args.option("acceptance").trim();
        String description = args.option("description").trim();
        if (changeKey.length() == 0 || acceptanceKey.length() == 0 || description.length() == 0) {
            context.err().println("Missing required parameters: --change, --acceptance, --description");
            return ExitCodes.USAGE_ERROR;
        }
        if (!SpecCommandSupport.isKeyAllowed(acceptanceKey)) {
            context.err().println("Invalid acceptance key: " + acceptanceKey);
            return ExitCodes.VALIDATION_ERROR;
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
                context.err().println("Spec change not found: " + changeKey);
                return ExitCodes.NOT_FOUND;
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
            context.err().println("Missing required parameters: --change, --acceptance, --status");
            return ExitCodes.USAGE_ERROR;
        }
        if (!SpecAcceptanceService.isAcceptanceStatusAllowed(status)) {
            context.err().println("Invalid acceptance status: " + status);
            return ExitCodes.VALIDATION_ERROR;
        }
        String evidence = args.option("evidence", args.option("reason", "")).trim();
        if ("waived".equals(status) && evidence.length() == 0) {
            context.err().println("Waived acceptance requires --evidence or --reason");
            return ExitCodes.USAGE_ERROR;
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
                context.err().println("Spec change not found: " + changeKey);
                return ExitCodes.NOT_FOUND;
            }
            SpecAcceptance acceptance = acceptanceRepository.findByKey(connection, changeKey, acceptanceKey);
            if (acceptance == null) {
                context.err().println("Spec acceptance not found: " + acceptanceKey);
                return ExitCodes.NOT_FOUND;
            }
            final SpecChange selectedChange = change;
            final SpecAcceptance selectedAcceptance = acceptance;
            final String selectedStatus = status;
            final String selectedEvidence = evidence.length() == 0 ? acceptance.evidence() : evidence;
            transactionTemplate.execute(connection, new TransactionTemplate.Work<Void>() {
                public Void execute() throws Exception {
                    acceptanceService.updateAcceptance(connection, selectedChange, selectedAcceptance,
                            selectedStatus, selectedEvidence, context.clock().now().toString());
                    return null;
                }
            });
            context.out().println("acceptance_key: " + acceptanceKey);
            context.out().println("status: " + status);
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR spec acceptance update failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
