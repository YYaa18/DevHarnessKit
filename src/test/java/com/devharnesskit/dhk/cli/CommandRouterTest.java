package com.devharnesskit.dhk.cli;

import com.devharnesskit.dhk.util.Clock;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
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
        assertTrue(harness.stdout().contains("dhk quickstart"));
        assertTrue(harness.stdout().contains("dhk completion bash|zsh|fish"));
        assertTrue(harness.stdout().contains("dhk goal verify"));
        assertTrue(harness.stdout().contains("dhk bdd init"));
        assertTrue(harness.stdout().contains("dhk configure init"));
        assertTrue(harness.stdout().contains("Current build: beta developer preview; not stable or 1.0-ready."));
        assertTrue(harness.stdout().contains("Experimental surface: graph, BDD, skill contract/trust"));
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
        assertTrue(harness.stdout().contains("DevHarness Kit " + VersionInfo.version()));
        assertTrue(harness.stdout().contains("release_channel: beta / developer preview"));
        assertTrue(harness.stdout().contains("stability: beta developer preview; not stable or 1.0-ready"));
        assertTrue(harness.stdout().contains("stable_candidate_surface: help,version,doctor,memory-core,goal-core,release-packaging"));
        assertTrue(harness.stdout().contains("experimental_surface: graph,bdd,skill-contract,policy-governance,ecc-control-panel,routine"));
        assertTrue(harness.stdout().contains("schema_version: 15"));
    }

    @Test
    void versionFlagReturnsVersion() {
        Harness harness = new Harness();

        int exitCode = new CommandRouter().run(new String[]{"--version"}, harness.context());

        assertEquals(ExitCodes.SUCCESS, exitCode);
        assertTrue(harness.stdout().contains("DevHarness Kit " + VersionInfo.version()));
    }

    private static final class Harness {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayOutputStream err = new ByteArrayOutputStream();

        CommandContext context() {
            return new CommandContext(
                    Paths.get(".").toAbsolutePath().normalize(),
                    com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.printStream(out),
                    com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.printStream(err),
                    new FixedClock()
            );
        }

        String stdout() {
            return com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.text(out);
        }

        String stderr() {
            return com.devharnesskit.dhk.testsupport.Utf8HarnessSupport.text(err);
        }
    }

    private static final class FixedClock implements Clock {
        public Instant now() {
            return Instant.parse("2026-05-21T00:00:00Z");
        }
    }
}
