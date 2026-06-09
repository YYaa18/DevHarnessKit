package com.devharnesskit.dhk.context.compress;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TestLogCompressorTest {
    @Test
    void preservesPytestFailureSignalsAndReducesTokens() {
        StringBuilder log = new StringBuilder();
        for (int i = 0; i < 180; i++) {
            log.append("collecting test noise ").append(i).append('\n');
        }
        log.append("FAILED tests/test_checkout.py::test_address_validation - AssertionError: invalid zip accepted\n");
        log.append("E   AssertionError: invalid zip accepted\n");
        log.append("= short test summary info =\n");
        log.append("FAILED tests/test_checkout.py::test_address_validation\n");
        for (int i = 0; i < 80; i++) {
            log.append("cleanup noise ").append(i).append('\n');
        }

        CompressResult result = new TestLogCompressor().compressTest("pytest", 1, log.toString());

        assertFalse(result.degraded());
        assertTrue(result.tokenAfter() * 2 < result.tokenBefore());
        assertTrue(result.compressedText().contains("source_type: test-log"));
        assertTrue(result.compressedText().contains("exit_code: 1"));
        assertTrue(result.compressedText().contains("test_address_validation"));
        assertTrue(result.compressedText().contains("invalid zip accepted"));
        assertTrue(result.retainedSpansJson().contains("pytest-signal"));
    }

    @Test
    void preservesJUnitFailureSummary() {
        String log = "[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0\n"
                + "[ERROR] com.example.ProjectServiceTest.shouldCreateProject <<< FAILURE!\n"
                + "java.lang.AssertionError: expected project id\n"
                + "\tat com.example.ProjectServiceTest.shouldCreateProject(ProjectServiceTest.java:42)\n";

        CompressResult result = new TestLogCompressor().compressTest("mvn -q test", 1, log);

        assertFalse(result.degraded());
        assertTrue(result.compressedText().contains("Tests run: 1"));
        assertTrue(result.compressedText().contains("shouldCreateProject"));
        assertTrue(result.compressedText().contains("AssertionError"));
    }
}
