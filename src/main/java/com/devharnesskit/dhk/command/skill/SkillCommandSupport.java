package com.devharnesskit.dhk.command.skill;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.model.skill.SkillContract;
import com.devharnesskit.dhk.model.skill.SkillContractLintIssue;
import com.devharnesskit.dhk.model.skill.SkillContractLintResult;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class SkillCommandSupport {
    private SkillCommandSupport() {
    }

    static Path projectRoot(Args args, CommandContext context) {
        return PathUtil.resolveProjectRoot(args, context.workingDirectory());
    }

    static Path skillDirectory(Args args, CommandContext context) {
        String configuredPath = args.option("path", "");
        if (configuredPath.length() > 0) {
            return PathUtil.resolvePath(configuredPath, context.workingDirectory());
        }
        String skillKey = args.option("skill", "");
        if (skillKey.length() == 0) {
            throw new IllegalArgumentException("Provide --skill <key> or --path <skill-dir>");
        }
        return PathUtil.skillDirectory(projectRoot(args, context), skillKey);
    }

    static void printText(CommandContext context, String command, SkillContractLintResult result) {
        printText(context, command, result, result.contract());
    }

    static void printText(CommandContext context, String command, SkillContractLintResult result,
                          SkillContract contract) {
        context.out().println(command + " complete");
        context.out().println("skill_key: " + result.skillKey());
        context.out().println("status: " + (result.passed() ? "passed" : "failed"));
        context.out().println("source_path: " + result.sourcePath());
        if (contract != null) {
            context.out().println("source_hash: " + contract.sourceHash());
            context.out().println("trusted_source_hash: " + contract.trustedSourceHash());
            context.out().println("trust_status: " + contract.trustStatus());
            context.out().println("trusted: " + contract.trusted());
        }
        context.out().println("missing: " + result.missingCount());
        context.out().println("invalid: " + result.invalidCount());
        context.out().println("forbidden: " + result.forbiddenCount());
        if (!result.issues().isEmpty()) {
            context.out().println("issues:");
            for (SkillContractLintIssue issue : result.issues()) {
                context.out().println("- " + issue.category() + " " + issue.field()
                        + ": " + issue.message());
                context.out().println("  suggestion: " + issue.suggestion());
            }
        }
    }

    static void printJson(CommandContext context, String command, SkillContractLintResult result) {
        printJson(context, command, result, result.contract());
    }

    static void printJson(CommandContext context, String command, SkillContractLintResult result,
                          SkillContract contract) {
        List<String> rawIssues = new ArrayList<String>();
        for (SkillContractLintIssue issue : result.issues()) {
            rawIssues.add(JsonOutput.object(
                    JsonOutput.stringField("category", issue.category()),
                    JsonOutput.stringField("field", issue.field()),
                    JsonOutput.stringField("message", issue.message()),
                    JsonOutput.stringField("suggestion", issue.suggestion())
            ));
        }
        context.out().print(JsonOutput.object(
                JsonOutput.stringField("command", command),
                JsonOutput.stringField("skill_key", result.skillKey()),
                JsonOutput.stringField("status", result.passed() ? "passed" : "failed"),
                JsonOutput.stringField("source_path", result.sourcePath()),
                JsonOutput.stringField("source_hash", contract == null ? "" : contract.sourceHash()),
                JsonOutput.stringField("trusted_source_hash", contract == null ? "" : contract.trustedSourceHash()),
                JsonOutput.stringField("trust_status", contract == null ? "" : contract.trustStatus()),
                JsonOutput.booleanField("trusted", contract != null && contract.trusted()),
                JsonOutput.numberField("missing", result.missingCount()),
                JsonOutput.numberField("invalid", result.invalidCount()),
                JsonOutput.numberField("forbidden", result.forbiddenCount()),
                JsonOutput.rawField("issues", JsonOutput.array(rawIssues))
        ));
    }
}
