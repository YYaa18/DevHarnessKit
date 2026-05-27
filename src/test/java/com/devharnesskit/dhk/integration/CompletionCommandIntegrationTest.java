package com.devharnesskit.dhk.integration;

import com.devharnesskit.dhk.cli.CommandContext;
import com.devharnesskit.dhk.cli.CommandRouter;
import com.devharnesskit.dhk.cli.ExitCodes;
import com.devharnesskit.dhk.util.Clock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CompletionCommandIntegrationTest {
    @TempDir
    Path tempDir;

    @Test
    void completionOutputsBashZshAndFishStaticScripts() {
        Harness bash = new Harness(tempDir);
        int bashExit = new CommandRouter().run(new String[]{"completion", "bash"}, bash.context());
        assertEquals(ExitCodes.SUCCESS, bashExit);
        assertTrue(bash.stdout().contains("_dhk_completion()"));
        assertTrue(bash.stdout().contains("complete -F _dhk_completion dhk"));
        assertTrue(bash.stdout().contains("quickstart"));
        assertTrue(bash.stdout().contains("--project-root"));
        assertTrue(bash.stdout().contains("--goal"));

        Harness zsh = new Harness(tempDir);
        int zshExit = new CommandRouter().run(new String[]{"completion", "zsh"}, zsh.context());
        assertEquals(ExitCodes.SUCCESS, zshExit);
        assertTrue(zsh.stdout().contains("#compdef dhk"));
        assertTrue(zsh.stdout().contains("compdef _dhk dhk"));
        assertTrue(zsh.stdout().contains("'goal'"));

        Harness fish = new Harness(tempDir);
        int fishExit = new CommandRouter().run(new String[]{"completion", "fish"}, fish.context());
        assertEquals(ExitCodes.SUCCESS, fishExit);
        assertTrue(fish.stdout().contains("complete -c dhk -f"));
        assertTrue(fish.stdout().contains("__fish_use_subcommand"));
        assertTrue(fish.stdout().contains("__fish_seen_subcommand_from goal"));
    }

    @Test
    void completionRejectsUnknownShell() {
        Harness harness = new Harness(tempDir);
        int exit = new CommandRouter().run(new String[]{"completion", "powershell"}, harness.context());

        assertEquals(ExitCodes.USAGE_ERROR, exit);
        assertTrue(harness.stderr().contains("Usage: dhk completion bash|zsh|fish"));
    }

    private static final class Harness {
        private final Path workingDirectory;
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayOutputStream err = new ByteArrayOutputStream();

        private Harness(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        CommandContext context() {
            return new CommandContext(workingDirectory, new PrintStream(out), new PrintStream(err), new FixedClock());
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
            return Instant.parse("2026-05-27T00:00:00Z");
        }
    }
}
