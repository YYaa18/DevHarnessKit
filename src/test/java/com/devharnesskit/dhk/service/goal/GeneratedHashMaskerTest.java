package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.service.SensitiveDataGuard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GeneratedHashMaskerTest {
    private final SensitiveDataGuard guard = new SensitiveDataGuard();

    @Test
    void masksPhoneLikeGeneratedFingerprintsBeforeSensitiveScan() {
        String fingerprint = "current_workspace_fingerprint: "
                + "git:f16753764299f196e99a06ab14fe95281bb27e7686390ee881e1afa7c24d38fe";
        String rawFingerprint = "check_fingerprint: "
                + "abcdef13800138000abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdef";

        assertTrue(guard.containsSensitiveData(fingerprint));
        assertFalse(guard.containsSensitiveData(GeneratedHashMasker.mask(fingerprint)));
        assertTrue(guard.containsSensitiveData(rawFingerprint));
        assertFalse(guard.containsSensitiveData(GeneratedHashMasker.mask(rawFingerprint)));
    }

    @Test
    void doesNotMaskRealPhoneNumbersOrSecrets() {
        String realPhone = "contact_phone: 13800138000";
        String secret = "password=abc123";

        assertTrue(guard.containsSensitiveData(GeneratedHashMasker.mask(realPhone)));
        assertTrue(guard.containsSensitiveData(GeneratedHashMasker.mask(secret)));
    }
}
