package com.devharnesskit.dhk.command.skill;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.skill.SkillAuditIssue;
import com.devharnesskit.dhk.model.skill.SkillAuditResult;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.skill.SkillAuditService;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SkillAuditCommand implements Command {
    private final SkillAuditService auditService = new SkillAuditService();

    public int run(CommandContext context, Args args) {
        Path projectRoot = SkillCommandSupport.projectRoot(args, context);
        try {
            Path skillDirectory = SkillCommandSupport.skillDirectory(args, context);
            SensitiveDataGuard.useProjectPolicy(projectRoot);
            SkillAuditResult result;
            try {
                result = auditService.audit(projectRoot, skillDirectory);
            } finally {
                SensitiveDataGuard.clearProjectPolicy();
            }
            if (JsonOutput.enabled(args)) {
                printJson(context, result);
            } else {
                printText(context, result);
            }
            return result.passed() ? ExitCodes.SUCCESS : ExitCodes.VALIDATION_ERROR;
        } catch (IllegalArgumentException ex) {
            SensitiveDataGuard.clearProjectPolicy();
            context.err().println("ERROR skill audit failed: " + ex.getMessage());
            return ExitCodes.USAGE_ERROR;
        } catch (Exception ex) {
            SensitiveDataGuard.clearProjectPolicy();
            context.err().println("ERROR skill audit failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }

    private void printText(CommandContext context, SkillAuditResult result) {
        context.out().println("skill audit complete");
        context.out().println("skill_key: " + result.skillKey());
        context.out().println("decision: " + result.decision());
        context.out().println("source_path: " + result.sourcePath());
        context.out().println("source_hash: " + result.sourceHash());
        context.out().println("issue_count: " + result.issueCount());
        context.out().println("critical: " + result.countBySeverity("critical"));
        context.out().println("high: " + result.countBySeverity("high"));
        context.out().println("medium: " + result.countBySeverity("medium"));
        if (!result.issues().isEmpty()) {
            context.out().println("issues:");
            for (SkillAuditIssue issue : result.issues()) {
                context.out().println("- " + issue.severity() + " " + issue.category()
                        + " " + issue.path() + ": " + issue.message());
                context.out().println("  suggestion: " + issue.suggestion());
            }
        }
    }

    private void printJson(CommandContext context, SkillAuditResult result) {
        List<String> rawIssues = new ArrayList<String>();
        for (SkillAuditIssue issue : result.issues()) {
            rawIssues.add(JsonOutput.object(
                    JsonOutput.stringField("severity", issue.severity()),
                    JsonOutput.stringField("category", issue.category()),
                    JsonOutput.stringField("path", issue.path()),
                    JsonOutput.stringField("message", issue.message()),
                    JsonOutput.stringField("suggestion", issue.suggestion())
            ));
        }
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", "skill audit"),
                JsonOutput.stringField("skill_key", result.skillKey()),
                JsonOutput.stringField("decision", result.decision()),
                JsonOutput.stringField("source_path", result.sourcePath()),
                JsonOutput.stringField("source_hash", result.sourceHash()),
                JsonOutput.numberField("issue_count", result.issueCount()),
                JsonOutput.numberField("critical", result.countBySeverity("critical")),
                JsonOutput.numberField("high", result.countBySeverity("high")),
                JsonOutput.numberField("medium", result.countBySeverity("medium")),
                JsonOutput.rawField("issues", JsonOutput.array(rawIssues))
        ));
    }
}
