package com.devharnesskit.dhk.command.spec;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SpecCommand implements Command {
    private final Map<String, Command> commands = new LinkedHashMap<String, Command>();

    public SpecCommand() {
        commands.put("create", new SpecCreateCommand());
        commands.put("document", new SpecDocumentCommand());
        commands.put("task", new SpecTaskCommand());
        commands.put("acceptance", new SpecAcceptanceCommand());
        commands.put("status", new SpecStatusCommand());
        commands.put("export", new SpecExportCommand());
        commands.put("archive", new SpecArchiveCommand());
        commands.put("bind-workflow", new SpecBindWorkflowCommand());
    }

    public int run(CommandContext context, Args args) {
        Command command = commands.get(args.subCommand());
        if (command == null) {
            context.err().println("Unknown spec subcommand: " + args.subCommand());
            context.err().println("Run `dhk help` for usage.");
            return ExitCodes.USAGE_ERROR;
        }
        return command.run(context, args);
    }
}
