package com.devharnesskit.dhk.command.graph;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

import java.util.LinkedHashMap;
import java.util.Map;

public final class GraphCommand implements Command {
    private final Map<String, Command> commands = new LinkedHashMap<String, Command>();

    public GraphCommand() {
        commands.put("init", new GraphInitCommand());
        commands.put("status", new GraphStatusCommand());
        commands.put("index", new GraphIndexCommand());
    }

    public int run(CommandContext context, Args args) {
        Command command = commands.get(args.subCommand());
        if (command == null) {
            context.err().println("Unknown graph subcommand: " + args.subCommand());
            context.err().println("Run `dhk help` for usage.");
            return ExitCodes.USAGE_ERROR;
        }
        return command.run(context, args);
    }
}
