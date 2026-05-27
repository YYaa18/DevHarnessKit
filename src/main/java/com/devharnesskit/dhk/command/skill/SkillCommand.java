package com.devharnesskit.dhk.command.skill;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class SkillCommand implements Command {
    private final Map<String, Supplier<Command>> commands = new LinkedHashMap<String, Supplier<Command>>();

    public SkillCommand() {
        commands.put("lint", new Supplier<Command>() {
            public Command get() { return new SkillLintCommand(); }
        });
        commands.put("verify", new Supplier<Command>() {
            public Command get() { return new SkillVerifyCommand(); }
        });
        commands.put("trust", new Supplier<Command>() {
            public Command get() { return new SkillTrustCommand(); }
        });
        commands.put("audit", new Supplier<Command>() {
            public Command get() { return new SkillAuditCommand(); }
        });
        commands.put("score", new Supplier<Command>() {
            public Command get() { return new SkillScoreCommand(); }
        });
        commands.put("report", new Supplier<Command>() {
            public Command get() { return new SkillReportCommand(); }
        });
        commands.put("gate", new Supplier<Command>() {
            public Command get() { return new SkillGateCommand(); }
        });
    }

    public int run(CommandContext context, Args args) {
        Supplier<Command> command = commands.get(args.subCommand());
        if (command == null) {
            context.err().println("Unknown skill subcommand: " + args.subCommand());
            context.err().println("Run `dhk help` for usage.");
            return ExitCodes.USAGE_ERROR;
        }
        return command.get().run(context, args);
    }
}
