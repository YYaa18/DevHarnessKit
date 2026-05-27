package com.devharnesskit.dhk.command.skill;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.model.skill.SkillContractLintResult;
import com.devharnesskit.dhk.service.skill.SkillContractLintService;
import com.devharnesskit.dhk.util.JsonOutput;

import java.nio.file.Path;

public final class SkillLintCommand implements Command {
    private final SkillContractLintService lintService = new SkillContractLintService();

    public int run(CommandContext context, Args args) {
        try {
            Path skillDirectory = SkillCommandSupport.skillDirectory(args, context);
            SkillContractLintResult result = lintService.lint(skillDirectory);
            if (JsonOutput.enabled(args)) {
                SkillCommandSupport.printJson(context, "skill lint", result);
            } else {
                SkillCommandSupport.printText(context, "skill lint", result);
            }
            return result.passed() ? ExitCodes.SUCCESS : ExitCodes.VALIDATION_ERROR;
        } catch (IllegalArgumentException ex) {
            context.err().println("ERROR skill lint failed: " + ex.getMessage());
            return ExitCodes.USAGE_ERROR;
        } catch (Exception ex) {
            context.err().println("ERROR skill lint failed: " + ex.getMessage());
            return ExitCodes.RUNTIME_ERROR;
        }
    }
}
