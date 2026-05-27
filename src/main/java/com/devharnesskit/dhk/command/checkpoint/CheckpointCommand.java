package com.devharnesskit.dhk.command.checkpoint;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

public final class CheckpointCommand implements Command {
    private final HumanCheckpointCommand humanCheckpointCommand = new HumanCheckpointCommand();

    public int run(CommandContext context, Args args) {
        String sub = args.subCommand();
        if ("request".equals(sub) || "approve".equals(sub) || "list".equals(sub)) {
            return humanCheckpointCommand.run(context, args);
        }
        context.err().println("Unknown checkpoint command: " + sub);
        context.err().println("Usage: dhk checkpoint request|approve|list --goal <goal-key>");
        return ExitCodes.USAGE_ERROR;
    }
}
