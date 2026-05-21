package com.devharnesskit.dhk.command.workflow;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TemplateCommand implements Command {
    private final Map<String, Command> commands = new LinkedHashMap<String, Command>();

    public TemplateCommand() {
        commands.put("seed", new TemplateSeedCommand());
        commands.put("list", new TemplateListCommand());
        commands.put("show", new TemplateShowCommand());
    }

    public int run(CommandContext context, Args args) {
        String action = args.positional(2);
        Command command = commands.get(action);
        if (command == null) {
            context.err().println("Unknown workflow template action: " + action);
            return ExitCodes.USAGE_ERROR;
        }
        return command.run(context, args);
    }
}
