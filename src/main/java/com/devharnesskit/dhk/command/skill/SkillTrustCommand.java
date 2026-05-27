package com.devharnesskit.dhk.command.skill;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.db.DbConnectionFactory;
import com.devharnesskit.dhk.db.MigrationRunner;
import com.devharnesskit.dhk.model.skill.SkillContract;
import com.devharnesskit.dhk.model.skill.SkillContractLintResult;
import com.devharnesskit.dhk.service.skill.SkillContractLintService;
import com.devharnesskit.dhk.service.skill.SkillContractService;
import com.devharnesskit.dhk.util.JsonOutput;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;

public final class SkillTrustCommand implements Command {
    private final SkillContractLintService lintService = new SkillContractLintService();
    private final SkillContractService contractService = new SkillContractService();
    private final DbConnectionFactory connectionFactory = new DbConnectionFactory();
    private final MigrationRunner migrationRunner = new MigrationRunner();

    public int run(CommandContext context, Args args) {
        try {
            Path projectRoot = SkillCommandSupport.projectRoot(args, context);
            Path skillDirectory = SkillCommandSupport.skillDirectory(args, context);
            SkillContractLintResult result = lintService.lint(skillDirectory);
            if (!result.passed()) {
                if (JsonOutput.enabled(args)) {
                    SkillCommandSupport.printJson(context, "skill trust", result);
                } else {
                    SkillCommandSupport.printText(context, "skill trust", result);
                    context.out().println("trusted: false");
                    context.out().println("persisted: false");
                }
                return ExitCodes.VALIDATION_ERROR;
            }

            SkillContract trusted;
            PathUtil.createMemoryDirectories(projectRoot);
            try (Connection connection = connectionFactory.open(projectRoot)) {
                migrationRunner.migrate(connection, context.clock());
                trusted = contractService.trustContract(connection, skillDirectory);
            }
            if (JsonOutput.enabled(args)) {
                SkillCommandSupport.printJson(context, "skill trust", result, trusted);
            } else {
                SkillCommandSupport.printText(context, "skill trust", result, trusted);
                context.out().println("persisted: true");
            }
            return ExitCodes.SUCCESS;
        } catch (IllegalArgumentException ex) {
            context.err().println("ERROR skill trust failed: " + ex.getMessage());
            return ExitCodes.USAGE_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR skill trust failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
