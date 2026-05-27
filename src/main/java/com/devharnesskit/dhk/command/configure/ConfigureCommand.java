package com.devharnesskit.dhk.command.configure;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class ConfigureCommand implements Command {
    private final Map<String, Supplier<Command>> commands = new LinkedHashMap<String, Supplier<Command>>();

    public ConfigureCommand() {
        commands.put("init", new Supplier<Command>() {
            public Command get() { return new ConfigureInitCommand(); }
        });
        commands.put("show", new Supplier<Command>() {
            public Command get() { return new ConfigureShowCommand(); }
        });
        commands.put("doctor", new Supplier<Command>() {
            public Command get() { return new ConfigureDoctorCommand(); }
        });
        commands.put("explain", new Supplier<Command>() {
            public Command get() { return new ConfigureExplainCommand(); }
        });
    }

    public int run(CommandContext context, Args args) {
        Supplier<Command> command = commands.get(args.subCommand());
        if (command == null) {
            context.err().println("Unknown configure subcommand: " + args.subCommand());
            context.err().println("Run `dhk help` for usage.");
            return ExitCodes.USAGE_ERROR;
        }
        return command.get().run(context, args);
    }
}
