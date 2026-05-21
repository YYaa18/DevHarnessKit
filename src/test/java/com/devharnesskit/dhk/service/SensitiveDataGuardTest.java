package com.devharnesskit.dhk.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SensitiveDataGuardTest {
    private final SensitiveDataGuard guard = new SensitiveDataGuard();

    @Test
    void detectsMvpSensitivePatterns() {
        assertTrue(guard.containsSensitiveData("password=abc"));
        assertTrue(guard.containsSensitiveData("token=abc"));
        assertTrue(guard.containsSensitiveData("Authorization: Bearer eyJhbGciOiJIUzI1NiJ9"));
        assertTrue(guard.containsSensitiveData("jdbc:mysql://127.0.0.1/demo"));
        assertTrue(guard.containsSensitiveData("AKIA123456789"));
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
}
