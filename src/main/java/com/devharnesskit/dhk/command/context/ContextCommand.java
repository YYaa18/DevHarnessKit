package com.devharnesskit.dhk.command.context;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class ContextCommand implements Command {
    private final Map<String, Supplier<Command>> commands = new LinkedHashMap<String, Supplier<Command>>();

    public ContextCommand() {
        commands.put("stats", new Supplier<Command>() {
            public Command get() { return new ContextStatsCommand(); }
        });
        commands.put("artifacts", new Supplier<Command>() {
            public Command get() { return new ContextArtifactsCommand(); }
        });
        commands.put("retrieve", new Supplier<Command>() {
            public Command get() { return new ContextRetrieveCommand(); }
        });
        commands.put("render", new Supplier<Command>() {
            public Command get() { return new ContextRenderCommand(); }
        });
        commands.put("doctor", new Supplier<Command>() {
            public Command get() { return new ContextDoctorCommand(); }
        });
    }

    public int run(CommandContext context, Args args) {
        Supplier<Command> command = commands.get(args.subCommand());
        if (command == null) {
            context.err().println("Unknown context subcommand: " + args.subCommand());
            context.err().println("Run `dhk help` for usage.");
            return ExitCodes.USAGE_ERROR;
        }
        return command.get().run(context, args);
    }
}
