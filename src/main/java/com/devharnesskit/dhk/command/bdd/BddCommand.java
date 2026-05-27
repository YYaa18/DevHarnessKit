package com.devharnesskit.dhk.command.bdd;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class BddCommand implements Command {
    private final Map<String, Supplier<Command>> commands = new LinkedHashMap<String, Supplier<Command>>();

    public BddCommand() {
        commands.put("init", new Supplier<Command>() {
            public Command get() { return new BddInitCommand(); }
        });
        commands.put("add", new Supplier<Command>() {
            public Command get() { return new BddAddCommand(); }
        });
        commands.put("list", new Supplier<Command>() {
            public Command get() { return new BddListCommand(); }
        });
        commands.put("show", new Supplier<Command>() {
            public Command get() { return new BddShowCommand(); }
        });
        commands.put("export", new Supplier<Command>() {
            public Command get() { return new BddExportCommand(); }
        });
        commands.put("lint", new Supplier<Command>() {
            public Command get() { return new BddLintCommand(); }
        });
        commands.put("evidence", new Supplier<Command>() {
            public Command get() { return new BddEvidenceCommand(); }
        });
        commands.put("verify", new Supplier<Command>() {
            public Command get() { return new BddVerifyCommand(); }
        });
        commands.put("coverage", new Supplier<Command>() {
            public Command get() { return new BddCoverageCommand(); }
        });
        commands.put("bind-spec", new Supplier<Command>() {
            public Command get() { return new BddBindSpecCommand(); }
        });
        commands.put("bind-goal", new Supplier<Command>() {
            public Command get() { return new BddBindGoalCommand(); }
        });
        commands.put("bind-graph", new Supplier<Command>() {
            public Command get() { return new BddBindGraphCommand(); }
        });
        commands.put("bind-test", new Supplier<Command>() {
            public Command get() { return new BddBindTestCommand(); }
        });
    }

    public int run(CommandContext context, Args args) {
        Supplier<Command> command = commands.get(args.subCommand());
        if (command == null) {
            context.err().println("Unknown bdd subcommand: " + args.subCommand());
            context.err().println("Run `dhk help` for usage.");
            return ExitCodes.USAGE_ERROR;
        }
        return command.get().run(context, args);
    }
}
