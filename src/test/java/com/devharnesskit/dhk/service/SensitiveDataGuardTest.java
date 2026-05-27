package com.devharnesskit.dhk.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.devharnesskit.dhk.util.PathUtil;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

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

    @Test
    void projectPolicyAliasesMatchCredentialPatternNames() throws Exception {
        Files.createDirectories(PathUtil.devharnessDirectory(tempDir));
        Files.write(PathUtil.sensitivePolicy(tempDir), ("{\n"
                + "  \"api_key\": \"allow\",\n"
                + "  \"private_key\": \"allow\"\n"
                + "}\n").getBytes("UTF-8"));
        SensitiveDataGuard.useProjectPolicy(tempDir);
        try {
            assertFalse(guard.containsSensitiveData("api_key=abc123"));
            assertFalse(guard.containsSensitiveData("private_key=abc123"));
            assertTrue(guard.containsSensitiveData("password=abc123"));
        } finally {
            SensitiveDataGuard.clearProjectPolicy();
        }
    }

    @Test
    void strictFixtureRejectsFinancialPii() throws Exception {
        useFixture("strict-reject.json");
        try {
            String pii = "contact test@example.com phone 13800138000 id 11010519491231002X";
            List<String> matches = guard.findMatches(pii);

            assertTrue(matches.contains("email"));
            assertTrue(matches.contains("phone"));
            assertTrue(matches.contains("identity number"));
        } finally {
            SensitiveDataGuard.clearProjectPolicy();
        }
    }

    @Test
    void financialRedactFixtureRedactsPiiButStillRejectsSecrets() throws Exception {
        useFixture("financial-redact-pii.json");
        try {
            String pii = "contact test@example.com phone 13800138000 id 11010519491231002X";
            assertFalse(guard.containsSensitiveData(pii));
            assertTrue(guard.redactedMatches(pii).contains("email"));
            assertEquals("contact [REDACTED_EMAIL] phone [REDACTED_PHONE] id [REDACTED_IDENTITY_NUMBER]",
                    guard.redact(pii));

            List<String> secretMatches = guard.findMatches(secretSample());
            assertTrue(secretMatches.contains("jwt"));
            assertTrue(secretMatches.contains("api_key="));
            assertTrue(secretMatches.contains("private_key="));
            assertTrue(secretMatches.contains("jdbc:mysql://"));
            assertTrue(secretMatches.contains("private key block"));
            assertTrue(secretMatches.contains("authorization:"));
            assertTrue(secretMatches.contains("github token"));
            assertTrue(secretMatches.contains("url credential"));
        } finally {
            SensitiveDataGuard.clearProjectPolicy();
        }
    }

    @Test
    void financialAllowFixtureAllowsPiiButStillRejectsSecrets() throws Exception {
        useFixture("financial-allow-pii.json");
        try {
            String pii = "contact test@example.com phone 13800138000 id 11010519491231002X";
            assertFalse(guard.containsSensitiveData(pii));
            assertTrue(guard.redactedMatches(pii).isEmpty());
            assertEquals(pii, guard.redact(pii));

            List<String> secretMatches = guard.findMatches(secretSample());
            assertTrue(secretMatches.contains("jwt"));
            assertTrue(secretMatches.contains("api_key="));
            assertTrue(secretMatches.contains("private_key="));
            assertTrue(secretMatches.contains("jdbc:mysql://"));
            assertTrue(secretMatches.contains("private key block"));
            assertTrue(secretMatches.contains("authorization:"));
            assertTrue(secretMatches.contains("github token"));
            assertTrue(secretMatches.contains("url credential"));
        } finally {
            SensitiveDataGuard.clearProjectPolicy();
        }
    }

    private void useFixture(String name) throws Exception {
        Files.createDirectories(PathUtil.devharnessDirectory(tempDir));
        InputStream input = SensitiveDataGuardTest.class.getResourceAsStream(
                "/fixtures/sensitive-policy/" + name);
        assertTrue(input != null);
        try {
            Files.copy(input, PathUtil.sensitivePolicy(tempDir), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            input.close();
        }
        SensitiveDataGuard.useProjectPolicy(tempDir);
    }

    private String secretSample() {
        return "jwt eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkw.signaturepart\n"
                + "api_key=abc123\n"
                + "private_key=abc123\n"
                + "jdbc:mysql://127.0.0.1/demo\n"
                + "Authorization: Bearer abcdefghijk\n"
                + "github token ghp_abcdefghijklmnopqrstuvwxyz\n"
                + "https://user:secret@example.com/path\n"
                + "-----BEGIN OPENSSH PRIVATE KEY-----";
    }
}
