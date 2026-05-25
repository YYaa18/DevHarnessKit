package com.devharnesskit.dhk.service.goal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoalCheckServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void executeTruncatesLargeOutputAndRecordsMetadata() throws Exception {
        GoalCheckService service = new GoalCheckService();

        GoalCheckService.CommandResult result = service.execute(tempDir,
                fixtureCommand("output"), 10);

        assertEquals(0, result.exitCode());
        assertTrue(result.durationMs() >= 0);
        assertTrue(result.truncated());
        assertTrue(result.output().contains("command: "));
        assertTrue(result.output().contains("working_directory: "
                + tempDir.toAbsolutePath().normalize()));
        assertTrue(result.output().contains("duration_ms:"));
        assertTrue(result.output().contains("timeout: false"));
        assertTrue(result.output().contains("output_truncated: true"));
        assertTrue(result.output().contains("COMMAND OUTPUT TRUNCATED after "
                + GoalCheckService.MAX_COMMAND_LOG_BYTES + " bytes"));
        assertTrue(result.output().length() < 280 * 1024);
    }

    @Test
    void executeTerminatesTimedOutProcessAndRecordsTimeout() throws Exception {
        GoalCheckService service = new GoalCheckService();

        GoalCheckService.CommandResult result = service.execute(tempDir,
                fixtureCommand("timeout"), 1);

        assertEquals(124, result.exitCode());
        assertTrue(result.timedOut());
        assertTrue(result.output().contains("timeout: true"));
        assertTrue(result.output().contains("CHECK TIMEOUT"));
    }

    private static String[] fixtureCommand(String mode) {
        return new String[]{
                javaExecutable(),
                "-cp",
                System.getProperty("java.class.path"),
                OutputFixture.class.getName(),
                mode
        };
    }

    private static String javaExecutable() {
        String binary = isWindows() ? "java.exe" : "java";
        return System.getProperty("java.home") + File.separator + "bin" + File.separator + binary;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public static final class OutputFixture {
        public static void main(String[] args) throws Exception {
            if (args.length > 0 && "timeout".equals(args[0])) {
                while (true) {
                    Thread.sleep(1000L);
                }
            }
            for (int i = 0; i < 50000; i++) {
                System.out.println("line-" + i + "-abcdefghijklmnopqrstuvwxyz0123456789");
            }
        }
    }
}
