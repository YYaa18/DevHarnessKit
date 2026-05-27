package com.devharnesskit.dhk.service.bdd.adapter;

import com.devharnesskit.dhk.model.bdd.BddAdapterResult;
import com.devharnesskit.dhk.model.bdd.BddEvidence;

public final class JunitBddEvidenceAdapter implements BddEvidenceAdapter {
    public static final String COMMAND_PREFIX = "junit:";

    public String key() {
        return "junit";
    }

    public boolean supports(BddEvidence evidence) {
        return evidence != null
                && "test".equals(evidence.evidenceType())
                && evidence.command().startsWith(COMMAND_PREFIX);
    }

    public BddAdapterResult normalize(BddEvidence evidence) {
        String normalizedStatus = normalizedStatus(evidence.status());
        return new BddAdapterResult(key(), evidence.id(), evidence.evidenceType(), evidence.status(),
                normalizedStatus, "covered".equals(normalizedStatus), evidence.summary(),
                evidence.evidencePath(), evidence.command(), message(normalizedStatus));
    }

    private String normalizedStatus(String status) {
        if ("passed".equals(status) || "waived".equals(status) || "skipped".equals(status)) {
            return "covered";
        }
        if ("pending".equals(status)) {
            return "pending";
        }
        if ("failed".equals(status)) {
            return "failed";
        }
        return "missing";
    }

    private String message(String normalizedStatus) {
        if ("covered".equals(normalizedStatus)) {
            return "JUnit evidence accepted";
        }
        if ("pending".equals(normalizedStatus)) {
            return "JUnit evidence is pending";
        }
        if ("failed".equals(normalizedStatus)) {
            return "JUnit evidence failed";
        }
        return "JUnit evidence status is not accepted";
    }
}
