package com.devharnesskit.dhk.cli;

import com.devharnesskit.dhk.util.Clock;

import java.io.PrintStream;
import java.nio.file.Path;

public final class CommandContext {
    private final Path workingDirectory;
    private final PrintStream out;
    private final PrintStream err;
    private final Clock clock;

    public CommandContext(Path workingDirectory, PrintStream out, PrintStream err, Clock clock) {
        this.workingDirectory = workingDirectory;
        this.out = out;
        this.err = err;
        this.clock = clock;
    }

    public Path workingDirectory() {
        return workingDirectory;
    }

    public PrintStream out() {
        return out;
    }

    public PrintStream err() {
        return err;
    }

    public Clock clock() {
        return clock;
    }
}
