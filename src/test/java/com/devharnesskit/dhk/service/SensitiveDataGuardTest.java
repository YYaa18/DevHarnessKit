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
        assertTrue(guard.containsSensitiveData("Authorization: Bearer abc"));
        assertTrue(guard.containsSensitiveData("jdbc:mysql://127.0.0.1/demo"));
        assertTrue(guard.containsSensitiveData("AKIA123456789"));
    }

    @Test
    void allowsOrdinaryTechnicalText() {
        assertFalse(guard.containsSensitiveData("User ID is read from X-User-Id with gateway tags."));
    }
}
