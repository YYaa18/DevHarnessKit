package com.devharnesskit.dhk.command.workflow;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.guidance.CommandErrorGuidance;
import com.devharnesskit.dhk.model.workflow.WorkflowGateTemplate;
import com.devharnesskit.dhk.model.workflow.WorkflowPhaseTemplate;
import com.devharnesskit.dhk.model.workflow.WorkflowTemplate;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateTemplateRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseTemplateRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowTemplateRepository;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

public final class TemplateShowCommand implements Command {
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final WorkflowTemplateRepository templateRepository = new WorkflowTemplateRepository();
    private final WorkflowPhaseTemplateRepository phaseRepository = new WorkflowPhaseTemplateRepository();
    private final WorkflowGateTemplateRepository gateRepository = new WorkflowGateTemplateRepository();

    public int run(CommandContext context, Args args) {
        String key = args.option("key").trim();
        if (key.length() == 0) {
            return CommandErrorGuidance.missing(context, args, "WORKFLOW_TEMPLATE_KEY_MISSING",
                    new String[]{"--key"}, "dhk workflow template show --key <template>",
                    "docs/GOAL_CONFIGURATION.md");
        }
        Path projectRoot = WorkflowCommandSupport.projectRoot(args, context);
        try (Connection connection = connectionFactory.open(projectRoot)) {
            WorkflowTemplate template = templateRepository.findByKey(connection, key);
            if (template == null) {
                return CommandErrorGuidance.notFound(context, args, "WORKFLOW_TEMPLATE_NOT_FOUND",
                        "workflow template", key, "dhk workflow template list",
                        "docs/GOAL_CONFIGURATION.md");
            }
            context.out().println("workflow: " + template.workflowKey());
            context.out().println("mode: " + template.targetMode());
            context.out().println("category: " + template.category());
            context.out().println("description: " + template.description());
            context.out().println("phases:");
            List<WorkflowPhaseTemplate> phases = phaseRepository.listByWorkflow(connection, key);
            for (WorkflowPhaseTemplate phase : phases) {
                context.out().println("  " + phase.phaseOrder() + ". " + phase.phaseKey());
            }
            context.out().println("gates:");
            List<WorkflowGateTemplate> gates = gateRepository.listByWorkflow(connection, key);
            for (WorkflowGateTemplate gate : gates) {
                context.out().println("  " + gate.phaseKey() + "/" + gate.gateKey() + " " + gate.severity());
            }
            return ExitCodes.SUCCESS;
        } catch (Exception ex) {
            context.err().println("ERROR workflow template show failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
