package com.devharnesskit.dhk.context;

import com.devharnesskit.dhk.context.compress.BuildLogCompressor;
import com.devharnesskit.dhk.context.compress.CompressResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class ContextWindowAcceptanceTest {
    @Test
    void preservesRequiredSignalsAcross64k128kAnd200kWindows() {
        CompressResult buildLog = new BuildLogCompressor().compressBuild(
                "mvn -q test", 1, failingMavenLog());
        assertTrue(buildLog.tokenAfter() * 2 < buildLog.tokenBefore());

        int[] windows = new int[]{64000, 128000, 200000};
        for (int window : windows) {
            ContextGovernor governor = new ContextGovernor(
                    new ContextBudget(window, 8000, 4000, 4000, 4000, 8000, 4000), null);
            ContextRenderResult result = governor.render(items(buildLog, window));

            assertTrue(result.tokenAfter() <= window || containsRisk(result, "required_item_exceeds_budget"));
            assertTrue(result.text().contains("manual_evidence_status=passed"));
            assertTrue(result.text().contains("exit_code: 1"));
            assertTrue(result.text().contains("CheckoutServiceTest.shouldRejectInvalidZip"));
            assertTrue(result.text().contains("invalid zip accepted"));
            assertTrue(result.text().contains("Given: customer has an empty cart"));
            assertTrue(result.text().contains("When: customer submits invalid shipping zip"));
            assertTrue(result.text().contains("Then: checkout is rejected with validation error"));
            assertTrue(result.text().contains("STALE_GRAPH_SNAPSHOT"));
            assertTrue(result.text().contains("artifact_ref: ctx-build-log-demo"));
        }
    }

    private List<ContextItem> items(CompressResult buildLog, int window) {
        List<ContextItem> items = new ArrayList<ContextItem>();
        items.add(new ContextItem(ContextItemType.GOAL, ContextPriority.REQUIRED,
                "current_action=verify\nrequired_evidence=compile_result,test_result,sensitive_result",
                "goal", false));
        items.add(new ContextItem(ContextItemType.BDD_EVIDENCE, ContextPriority.REQUIRED,
                "manual_evidence_status=passed\n"
                        + "Given: customer has an empty cart\n"
                        + "When: customer submits invalid shipping zip\n"
                        + "Then: checkout is rejected with validation error",
                "bdd-required", false));
        items.add(new ContextItem(ContextItemType.GRAPH_IMPACT, ContextPriority.HIGH,
                "## Graph Impact Digest\n"
                        + "required_read:\n"
                        + "  1. src/main/java/CheckoutService.java (reason: direct symbol match)\n"
                        + "graph_snapshot: status=stale; confidence=advisory_only\n"
                        + "warning: STALE_GRAPH_SNAPSHOT; regenerate with dhk graph index",
                "graph-digest", false));
        items.add(new ContextItem(ContextItemType.BUILD_LOG, ContextPriority.NORMAL,
                buildLog.compressedText() + "\nartifact_ref: ctx-build-log-demo",
                "ctx-build-log-demo", true));
        int memoryCount = window == 64000 ? 120 : 80;
        for (int i = 0; i < memoryCount; i++) {
            items.add(new ContextItem(ContextItemType.MEMORY_FACT, ContextPriority.LOW,
                    "confirmed memory " + i + " " + repeat("historical context ", 180),
                    "memory-" + i, true));
        }
        return items;
    }

    private boolean containsRisk(ContextRenderResult result, String expected) {
        for (String risk : result.risks()) {
            if (risk.contains(expected)) {
                return true;
            }
        }
        return false;
    }

    private String failingMavenLog() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 3000; i++) {
            builder.append("[INFO] downloading dependency ").append(i).append(' ')
                    .append(repeat("classpath-noise ", 10)).append('\n');
        }
        builder.append("[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0\n");
        builder.append("[ERROR] com.example.CheckoutServiceTest.shouldRejectInvalidZip <<< FAILURE!\n");
        builder.append("java.lang.AssertionError: invalid zip accepted\n");
        builder.append("Caused by: expected validation error for invalid shipping zip\n");
        for (int i = 0; i < 200; i++) {
            builder.append("[INFO] cleanup ").append(i).append('\n');
        }
        return builder.toString();
    }

    private String repeat(String text, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(text);
        }
        return builder.toString();
    }
}
