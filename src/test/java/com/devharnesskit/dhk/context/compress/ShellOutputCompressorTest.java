package com.devharnesskit.dhk.context.compress;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ShellOutputCompressorTest {
    @Test
    void preservesTracebackAndPermissionSignals() {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            output.append("progress ").append(i).append(' ')
                    .append(repeat("streaming-background-output ", 12)).append('\n');
        }
        output.append("Traceback (most recent call last):\n");
        output.append("  File \"tool.py\", line 9, in <module>\n");
        output.append("Permission denied: /var/run/build-cache\n");
        for (int i = 0; i < 60; i++) {
            output.append("tail ").append(i).append(' ')
                    .append(repeat("cleanup-background-output ", 12)).append('\n');
        }

        CompressResult result = new ShellOutputCompressor().compressShell("python tool.py", 1, output.toString());

        assertFalse(result.degraded());
        assertTrue(result.tokenAfter() * 2 < result.tokenBefore());
        assertTrue(result.compressedText().contains("source_type: shell-output"));
        assertTrue(result.compressedText().contains("exit_code: 1"));
        assertTrue(result.compressedText().contains("Traceback"));
        assertTrue(result.compressedText().contains("Permission denied"));
        assertTrue(result.retainedSpansJson().contains("traceback"));
        assertTrue(result.retainedSpansJson().contains("permission"));
    }

    private String repeat(String text, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(text);
        }
        return builder.toString();
    }
}
