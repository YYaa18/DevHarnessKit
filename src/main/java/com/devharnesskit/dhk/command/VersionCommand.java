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
        context.out().println("stability: beta developer preview; not stable or 1.0-ready");
        context.out().println("stable_candidate_surface: help,version,doctor,memory-core,goal-core,release-packaging");
        context.out().println("beta_surface: db-readonly");
        context.out().println("experimental_surface: graph,bdd,skill-contract,policy-governance,ecc-control-panel,routine");
        context.out().println("java: " + System.getProperty("java.version"));
        return ExitCodes.SUCCESS;
    }
}
