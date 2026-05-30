package com.devharnesskit.dhk.service.policy;

import com.devharnesskit.dhk.model.policy.DevHarnessPolicy;
import com.devharnesskit.dhk.util.PathUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DevHarnessPolicyServiceTest {
    @TempDir
    Path tempDir;

    private final DevHarnessPolicyService service = new DevHarnessPolicyService();

    @Test
    void missingPolicyUsesDefaultsWithoutDiagnostics() {
        Path projectRoot = tempDir.resolve("missing-policy");

        DevHarnessPolicy policy = service.load(projectRoot);

        assertEquals("guided", policy.mode());
        assertTrue(policy.graphAllowStaleRequiresApproval());
        assertTrue(service.diagnose(projectRoot).isEmpty());
    }

    @Test
    void invalidPolicyJsonWarnsAndLoadsDefaults() throws Exception {
        Path projectRoot = tempDir.resolve("invalid-policy");
        Files.createDirectories(PathUtil.devharnessDirectory(projectRoot));
        Files.write(PathUtil.devharnessPolicy(projectRoot), "{not-json".getBytes("UTF-8"));

        DevHarnessPolicy policy = service.load(projectRoot);
        List<DevHarnessPolicyService.Diagnostic> diagnostics = service.diagnose(projectRoot);

        assertEquals("guided", policy.mode());
        assertEquals(1, diagnostics.size());
        assertTrue(diagnostics.get(0).format().contains("invalid JSON; default policy will be used"));
    }

    @Test
    void conflictingPolicyWarnsAboutCommandAndExportPathConflicts() throws Exception {
        Path projectRoot = tempDir.resolve("conflicting-policy");
        Files.createDirectories(PathUtil.devharnessDirectory(projectRoot));
        Files.write(PathUtil.devharnessPolicy(projectRoot), ("{\n"
                + "  \"schema_version\": \"" + DevHarnessPolicyService.SCHEMA_VERSION + "\",\n"
                + "  \"allowed_dhk_commands\": \"goal,db sql\",\n"
                + "  \"forbidden_dhk_commands\": \"goal step,db sql\",\n"
                + "  \"context_export_allowed_files\": \".agents/memory/exports/*.md,.env\",\n"
                + "  \"context_export_forbidden_files\": \".env\"\n"
                + "}\n").getBytes("UTF-8"));

        List<DevHarnessPolicyService.Diagnostic> diagnostics = service.diagnose(projectRoot);
        String text = join(diagnostics);

        assertTrue(text.contains("allowed_dhk_commands conflicts with forbidden_dhk_commands"), text);
        assertTrue(text.contains("goal <> goal step"), text);
        assertTrue(text.contains("db sql <> db sql"), text);
        assertTrue(text.contains("context_export_allowed_files conflicts with context_export_forbidden_files: .env"),
                text);
    }

    @Test
    void missingSchemaVersionWarnsButStillLoadsPolicy() throws Exception {
        Path projectRoot = tempDir.resolve("legacy-policy");
        Files.createDirectories(PathUtil.devharnessDirectory(projectRoot));
        Files.write(PathUtil.devharnessPolicy(projectRoot), ("{\n"
                + "  \"mode\": \"strict\"\n"
                + "}\n").getBytes("UTF-8"));

        DevHarnessPolicy policy = service.load(projectRoot);
        String text = join(service.diagnose(projectRoot));

        assertEquals("strict", policy.mode());
        assertTrue(text.contains("schema_version is missing; use " + DevHarnessPolicyService.SCHEMA_VERSION),
                text);
    }

    private String join(List<DevHarnessPolicyService.Diagnostic> diagnostics) {
        StringBuilder builder = new StringBuilder();
        for (DevHarnessPolicyService.Diagnostic diagnostic : diagnostics) {
            builder.append(diagnostic.format()).append('\n');
        }
        return builder.toString();
    }
}
