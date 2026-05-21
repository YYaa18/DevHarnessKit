package com.devharnesskit.dhk.cli;

import com.devharnesskit.dhk.command.HelpCommand;
import com.devharnesskit.dhk.command.DoctorCommand;
import com.devharnesskit.dhk.command.projectdb.DbCommand;
import com.devharnesskit.dhk.command.memory.MemoryCommand;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CommandRouter {
    private final Map<String, Command> commands;

    public CommandRouter() {
        this.commands = new LinkedHashMap<String, Command>();
        this.commands.put("help", new HelpCommand());
        this.commands.put("doctor", new DoctorCommand());
        this.commands.put("memory", new MemoryCommand());
        this.commands.put("db", new DbCommand());
    }

    public int run(String[] rawArgs, CommandContext context) {
        Args args = Args.parse(rawArgs);
        if (isHelpRequest(args)) {
            return commands.get("help").run(context, args);
        }

        Command command = commands.get(args.primaryCommand());
        if (command == null) {
            context.err().println("Unknown command: " + args.primaryCommand());
            context.err().println("Run `dhk help` for usage.");
            return ExitCodes.USAGE_ERROR;
        }

        return command.run(context, args);
    }

    private boolean isHelpRequest(Args args) {
        return args.positionals().isEmpty()
                || "help".equals(args.primaryCommand())
                || args.hasFlag("help");
    }
}
