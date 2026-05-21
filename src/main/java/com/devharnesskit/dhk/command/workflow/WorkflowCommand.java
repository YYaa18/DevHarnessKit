package com.devharnesskit.dhk.command.workflow;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WorkflowCommand implements Command {
    private final Map<String, Command> commands = new LinkedHashMap<String, Command>();

    public WorkflowCommand() {
        commands.put("template", new TemplateCommand());
        commands.put("start", new StartCommand());
        commands.put("status", new StatusCommand());
        commands.put("phase", new PhaseCommand());
        commands.put("gate", new GateCommand());
        commands.put("export", new WorkflowExportCommand());
    }

    public int run(CommandContext context, Args args) {
        Command command = commands.get(args.subCommand());
        if (command == null) {
            context.err().println("Unknown workflow subcommand: " + args.subCommand());
            context.err().println("Run `dhk help` for usage.");
            return ExitCodes.USAGE_ERROR;
        }
        return command.run(context, args);
    }
}
