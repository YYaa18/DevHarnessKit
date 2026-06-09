package com.devharnesskit.dhk.context.compress;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildLogCompressorTest {
    @Test
    void preservesExitCodeRootCauseAndTailWhileOmittingNoise() {
        StringBuilder log = new StringBuilder();
        for (int i = 0; i < 180; i++) {
            log.append("download noise ").append(i).append(' ')
                    .append(repeat("classpath-resolving-noise ", 12)).append('\n');
        }
        log.append("[ERROR] COMPILATION ERROR\n");
        log.append("[ERROR] cannot find symbol\n");
        log.append("[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin\n");
        for (int i = 0; i < 40; i++) {
            log.append("tail noise ").append(i).append(' ')
                    .append(repeat("plugin-progress-noise ", 12)).append('\n');
        }

        CompressResult result = new BuildLogCompressor().compressBuild(
                "mvn -q -DskipTests package", 1, log.toString());

        assertTrue(result.tokenAfter() < result.tokenBefore());
        assertTrue(result.tokenAfter() * 2 < result.tokenBefore());
        assertFalse(result.degraded());
        assertTrue(result.omittedLines() > 0);
        assertTrue(result.compressedText().contains("exit_code: 1"));
        assertTrue(result.compressedText().contains("status: failed"));
        assertTrue(result.compressedText().contains("degraded: false"));
        assertTrue(result.compressedText().contains("cannot find symbol"));
        assertTrue(result.retainedSpansJson().contains("compile-symbol"));
    }

    @Test
    void marksFallbackAsDegradedWhenNoStructuredSignalExists() {
        StringBuilder log = new StringBuilder();
        for (int i = 0; i < 220; i++) {
            log.append("ordinary progress ").append(i).append('\n');
        }

        CompressResult result = new BuildLogCompressor().compressBuild("mvn -q test", 1, log.toString());

        assertTrue(result.degraded());
        assertTrue(result.compressedText().contains("degraded: true"));
        assertTrue(result.compressedText().contains("root_cause_candidates:\n    - none"));
        assertTrue(result.compressedText().contains("ordinary progress 219"));
    }

    private String repeat(String text, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(text);
        }
        return builder.toString();
    }
}
