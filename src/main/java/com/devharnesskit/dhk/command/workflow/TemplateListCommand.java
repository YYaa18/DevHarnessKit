package com.devharnesskit.dhk.command.workflow;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.model.workflow.WorkflowTemplate;
import com.devharnesskit.dhk.repository.workflow.WorkflowTemplateRepository;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

public final class TemplateListCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final WorkflowTemplateRepository templateRepository = new WorkflowTemplateRepository();

    public int run(CommandContext context, Args args) {
        Path projectRoot = WorkflowCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            if (MigrationRunner.currentSchemaVersion(connection) < MigrationRunner.V2) {
                context.err().println("Workflow schema is not initialized; run workflow template seed.");
                return ExitCodes.NOT_FOUND;
            }
            List<WorkflowTemplate> templates = templateRepository.listActive(connection);
            for (WorkflowTemplate template : templates) {
                context.out().println(template.workflowKey() + "\t" + template.status()
                        + "\t" + template.targetMode() + "\t" + template.category());
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR workflow template list failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
