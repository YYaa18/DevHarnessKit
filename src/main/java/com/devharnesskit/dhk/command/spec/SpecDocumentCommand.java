package com.devharnesskit.dhk.command.spec;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.db.TransactionTemplate;
import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.spec.SpecDocument;
import com.devharnesskit.dhk.repository.ProjectRepository;
import com.devharnesskit.dhk.repository.spec.SpecChangeRepository;
import com.devharnesskit.dhk.repository.spec.SpecDocumentRepository;
import com.devharnesskit.dhk.repository.spec.SpecEventRepository;
import com.devharnesskit.dhk.service.ProjectService;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.spec.SpecDocumentService;
import com.devharnesskit.dhk.util.InputUtil;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

public final class SpecDocumentCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();
    private final ProjectService projectService = new ProjectService();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final SpecChangeRepository changeRepository = new SpecChangeRepository();
    private final SensitiveDataGuard sensitiveDataGuard = new SensitiveDataGuard();
    private final TransactionTemplate transactionTemplate = new TransactionTemplate();
    private final SpecDocumentService documentService = new SpecDocumentService(
            new SpecDocumentRepository(), new SpecEventRepository());

    public int run(CommandContext context, Args args) {
        String action = args.positional(2);
        if (!"set".equals(action)) {
            context.err().println("Unknown spec document action: " + action);
            return ExitCodes.USAGE_ERROR;
        }
        String changeKey = args.option("change").trim();
        String type = args.option("type").trim();
        if (changeKey.length() == 0 || type.length() == 0) {
            context.err().println("Missing required parameters: --change, --type");
            return ExitCodes.USAGE_ERROR;
        }
        if (!SpecDocumentService.isDocumentTypeAllowed(type)) {
            context.err().println("Invalid document type: " + type);
            return ExitCodes.VALIDATION_ERROR;
        }
        String status = args.option("status", "draft").trim();
        if (!SpecDocumentService.isDocumentStatusAllowed(status)) {
            context.err().println("Invalid document status: " + status);
            return ExitCodes.VALIDATION_ERROR;
        }
        String title = args.option("title", type).trim();
        if (title.length() == 0) {
            title = type;
        }
        try {
            String content = content(context, args);
            if (SpecCommandSupport.rejectSensitive(context, sensitiveDataGuard, "spec document",
                    changeKey, type, title, content)) {
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
                final String selectedType = type;
                final String selectedTitle = title;
                final String selectedContent = content;
                final String selectedStatus = status;
                SpecDocument document = transactionTemplate.execute(connection, new TransactionTemplate.Work<SpecDocument>() {
                    public SpecDocument execute() throws Exception {
                        return documentService.setDocument(connection, selectedChange, selectedType,
                                selectedTitle, selectedContent, selectedStatus,
                                context.clock().now().toString());
                    }
                });
                context.out().println("document_type: " + document.documentType());
                context.out().println("version: " + document.version());
                context.out().println("status: " + document.status());
                return ExitCodes.SUCCESS;
            }
        } catch (Exception ex) {
            context.err().println("ERROR spec document failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private String content(CommandContext context, Args args) throws Exception {
        int count = 0;
        if (args.hasOption("content")) {
            count++;
        }
        if (args.hasOption("content-file")) {
            count++;
        }
        if (args.hasOption("file")) {
            count++;
        }
        if (args.hasFlag("content-stdin")) {
            count++;
        }
        if (count != 1) {
            throw new InputUtil.InputException("Provide exactly one of --content, --content-file, --file, or --content-stdin");
        }
        if (args.hasOption("file")) {
            Path file = PathUtil.resolvePath(args.option("file"), context.workingDirectory());
            try {
                return sensitiveDataGuard.redact(new String(Files.readAllBytes(file), "UTF-8"));
            } catch (Exception ex) {
                throw new InputUtil.InputException("Failed to read --file: " + ex.getMessage());
            }
        }
        return InputUtil.readExclusiveText(context, args, "content", "content-file", "content-stdin");
    }
}
