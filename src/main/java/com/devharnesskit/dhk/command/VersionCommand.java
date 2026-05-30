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
        context.out().println("stability: stable; Graph Lite is stable-advisory and full governance surfaces remain outside the stable contract");
        context.out().println("stable_surface: help,version,doctor,memory-core,goal-core,bdd,configure,status,readiness,advise,quickstart,release-packaging");
        context.out().println("stable_candidate_surface: skill-contract,policy-hooks,routine-report");
        context.out().println("beta_surface: db-readonly");
        context.out().println("experimental_surface: graph-aware-goal,skill-governance-full,policy-governance-full,ecc-control-panel,routine");
        context.out().println("java: " + System.getProperty("java.version"));
        return ExitCodes.SUCCESS;
    }
}
