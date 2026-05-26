package com.devharnesskit.dhk.command.graph;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class GraphCommand implements Command {
    private final Map<String, Supplier<Command>> commands = new LinkedHashMap<String, Supplier<Command>>();

    public GraphCommand() {
        commands.put("init", new Supplier<Command>() {
            public Command get() { return new GraphInitCommand(); }
        });
        commands.put("doctor", new Supplier<Command>() {
            public Command get() { return new GraphDoctorCommand(); }
        });
        commands.put("status", new Supplier<Command>() {
            public Command get() { return new GraphStatusCommand(); }
        });
        commands.put("index", new Supplier<Command>() {
            public Command get() { return new GraphIndexCommand(); }
        });
        commands.put("impact", new Supplier<Command>() {
            public Command get() { return new GraphImpactCommand(); }
        });
        commands.put("export", new Supplier<Command>() {
            public Command get() { return new GraphExportCommand(); }
        });
        commands.put("prune", new Supplier<Command>() {
            public Command get() { return new GraphPruneCommand(); }
        });
    }

    public int run(CommandContext context, Args args) {
        Supplier<Command> command = commands.get(args.subCommand());
        if (command == null) {
            context.err().println("Unknown graph subcommand: " + args.subCommand());
            context.err().println("Run `dhk help` for usage.");
            return ExitCodes.USAGE_ERROR;
        }
        return command.get().run(context, args);
    }
}
