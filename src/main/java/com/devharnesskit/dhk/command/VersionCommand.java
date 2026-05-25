package com.devharnesskit.dhk.command;

import com.devharnesskit.dhk.cli.Args;
import com.devharnesskit.dhk.cli.Command;
import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.cli.VersionInfo;

public final class VersionCommand implements Command {
    public int run(CommandContext context, Args args) {
        context.out().println("DevHarness Kit " + VersionInfo.version());
        context.out().println("release_channel: " + VersionInfo.RELEASE_CHANNEL);
        context.out().println("schema_version: " + VersionInfo.CURRENT_SCHEMA_VERSION);
        context.out().println("java: " + System.getProperty("java.version"));
        return ExitCodes.SUCCESS;
    }
}
