package com.devharnesskit.dhk.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SensitiveDataGuardTest {
    private final SensitiveDataGuard guard = new SensitiveDataGuard();

    @TempDir
    Path tempDir;

    @Test
    void detectsMvpSensitivePatterns() {
        assertTrue(guard.containsSensitiveData("password=abc"));
        assertTrue(guard.containsSensitiveData("token=abc"));
        assertTrue(guard.containsSensitiveData("Authorization: Bearer eyJhbGciOiJIUzI1NiJ9"));
        assertTrue(guard.containsSensitiveData("jdbc:mysql://127.0.0.1/demo"));
        assertTrue(guard.containsSensitiveData("AKIA123456789"));
        assertTrue(guard.containsSensitiveData("api_key=abc123"));
        assertTrue(guard.containsSensitiveData("-----BEGIN OPENSSH PRIVATE KEY-----"));
        assertTrue(guard.containsSensitiveData("github token ghp_abcdefghijklmnopqrstuvwxyz"));
        assertTrue(guard.containsSensitiveData("jwt eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkw.signaturepart"));
        assertTrue(guard.containsSensitiveData("https://user:secret@example.com/path"));
        assertTrue(guard.containsSensitiveData("slack xoxb-1234567890-abcdef"));
        assertTrue(guard.containsSensitiveData("13800138000"));
        assertTrue(guard.containsSensitiveData("test@example.com"));
        assertTrue(guard.containsSensitiveData("11010519491231002X"));
    }

    @Test
    void allowsOrdinaryTechnicalText() {
        assertFalse(guard.containsSensitiveData("User ID is read from X-User-Id with gateway tags."));
        assertFalse(guard.containsSensitiveData("业务服务不解析 Bearer token。"));
        assertFalse(guard.containsSensitiveData("token validation is handled upstream."));
    }

    @Test
    void projectPolicyCanRedactOrAllowSpecificPatterns() throws Exception {
        Files.createDirectories(PathUtil.devharnessDirectory(tempDir));
        Files.write(PathUtil.sensitivePolicy(tempDir), ("{\n"
                + "  \"email\": \"allow\",\n"
                + "  \"phone\": \"redact\",\n"
                + "  \"identity_number\": \"redact\"\n"
                + "}\n").getBytes("UTF-8"));
        SensitiveDataGuard.useProjectPolicy(tempDir);
        try {
            assertFalse(guard.containsSensitiveData("contact test@example.com"));
            assertFalse(guard.containsSensitiveData("phone 13800138000"));
            assertEquals("phone [REDACTED_PHONE]", guard.redact("phone 13800138000"));
            assertEquals("id [REDACTED_IDENTITY_NUMBER]", guard.redact("id 11010519491231002X"));
        } finally {
            SensitiveDataGuard.clearProjectPolicy();
        }
    }
}
