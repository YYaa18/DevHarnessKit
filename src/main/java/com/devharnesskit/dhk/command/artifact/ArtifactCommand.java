package com.devharnesskit.dhk.command.artifact;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;

public final class ArtifactCommand implements Command {
    private final ArtifactPassportVerifyCommand passportVerifyCommand = new ArtifactPassportVerifyCommand();

    public int run(CommandContext context, Args args) {
        if ("passport".equals(args.subCommand()) && "verify".equals(args.positional(2))) {
            return passportVerifyCommand.run(context, args);
        }
        context.err().println("Unknown artifact command: " + args.subCommand()
                + (args.positional(2).length() == 0 ? "" : " " + args.positional(2)));
        context.err().println("Run `dhk help` for usage.");
        return ExitCodes.USAGE_ERROR;
    }
}
