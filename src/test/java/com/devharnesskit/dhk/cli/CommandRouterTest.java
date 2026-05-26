package com.devharnesskit.dhk.cli;

import com.devharnesskit.dhk.util.Clock;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CommandRouterTest {
    @Test
    void emptyArgumentsShowHelp() {
        Harness harness = new Harness();

        int exitCode = new CommandRouter().run(new String[]{}, harness.context());

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(harness.stdout().contains("DevHarness Kit CLI"));
        assertTrue(harness.stdout().contains("dhk memory init"));
        assertTrue(harness.stdout().contains("dhk goal verify"));
    }

    @Test
    void unknownCommandReturnsUsageError() {
        Harness harness = new Harness();

        int exitCode = new CommandRouter().run(new String[]{"scan"}, harness.context());

        assertEquals(ExitCodes.USAGE_ERROR, exitCode);
        assertTrue(harness.stderr().contains("Unknown command: scan"));
    }

    @Test
    void versionCommandReturnsBetaVersion() {
        Harness harness = new Harness();

        int exitCode = new CommandRouter().run(new String[]{"version"}, harness.context());

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(harness.stdout().contains("DevHarness Kit 0.4.4-beta.1"));
        assertTrue(harness.stdout().contains("release_channel: beta / developer preview"));
        assertTrue(harness.stdout().contains("schema_version: 9"));
    }

    @Test
    void versionFlagReturnsVersion() {
        Harness harness = new Harness();

        int exitCode = new CommandRouter().run(new String[]{"--version"}, harness.context());

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(harness.stdout().contains("DevHarness Kit 0.4.4-beta.1"));
    }

    private static final class Harness {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayOutputStream err = new ByteArrayOutputStream();

        CommandContext context() {
            return new CommandContext(
                    Paths.get(".").toAbsolutePath().normalize(),
                    new PrintStream(out),
                    new PrintStream(err),
                    new FixedClock()
            );
        }

        String stdout() {
            return out.toString();
        }

        String stderr() {
            return err.toString();
        }
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-05-21T00:00:00Z");
        }
    }
}
