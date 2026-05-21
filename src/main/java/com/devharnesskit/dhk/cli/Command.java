package com.devharnesskit.dhk.cli;

public interface Command {
    int run(CommandContext context, Args args);
}
