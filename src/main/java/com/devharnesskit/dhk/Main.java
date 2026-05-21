package com.devharnesskit.dhk;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.CommandRouter;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.util.SystemClock;

import java.nio.file.Paths;

public final class Main {
    private Main() {
    }

    public static void main(String[] rawArgs) {
        int exitCode = ExitCodes.RUNTIME_ERROR;
        try {
            CommandContext context = new CommandContext(
                    Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize(),
                    System.out,
                    System.err,
                    new SystemClock()
            );
            exitCode = new CommandRouter().run(rawArgs, context);
        } catch (RuntimeException ex) {
            System.err.println("Error: " + ex.getMessage());
        }
        System.exit(exitCode);
    }
}
