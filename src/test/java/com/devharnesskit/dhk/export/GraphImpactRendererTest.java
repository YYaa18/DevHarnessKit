package com.devharnesskit.dhk.export;

import com.devharnesskit.dhk.model.graph.GraphImpactRequest;
import com.devharnesskit.dhk.model.graph.GraphImpactResult;
import com.devharnesskit.dhk.model.graph.GraphSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class GraphImpactRendererTest {
    @Test
    void digestMarksStaleSnapshotExplicitly() {
        GraphImpactResult result = new GraphImpactResult(
                new GraphImpactRequest("file", "src/main/java/Account.java", 2),
                new GraphSnapshot(1L, "project", "snap-1", "lite", "cfg",
                        "old-fp", "completed", 10, 20, 30, 0, 1000, 200,
                        "", "2026-06-01T00:00:00Z", "2026-06-01T00:00:01Z"),
                true,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("src/main/java/AccountService.java"),
                Collections.emptyList(),
                Collections.singletonList("src/test/java/AccountServiceTest.java"),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList("src/main/java/AccountController.java"),
                Collections.emptyList(),
                Paths.get("IMPACT_MAP.md"),
                2,
                2,
                false,
                "new-fp",
                true,
                false);

        String digest = new GraphImpactRenderer().renderDigest(result);

        assertTrue(digest.contains("## Graph Impact Digest"));
        assertTrue(digest.contains("required_read:"));
        assertTrue(digest.contains("AccountController.java"));
        assertTrue(digest.contains("optional_read:"));
        assertTrue(digest.contains("AccountService.java"));
        assertTrue(digest.contains("status=stale"));
        assertTrue(digest.contains("STALE_GRAPH_SNAPSHOT"));
        assertTrue(digest.contains("confidence=advisory_only"));
    }
}
