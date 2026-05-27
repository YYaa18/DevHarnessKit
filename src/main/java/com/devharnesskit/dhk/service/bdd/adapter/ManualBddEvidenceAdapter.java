package com.devharnesskit.dhk.service.bdd.adapter;

import com.devharnesskit.dhk.model.bdd.BddAdapterResult;
import com.devharnesskit.dhk.model.bdd.BddEvidence;

public final class ManualBddEvidenceAdapter implements BddEvidenceAdapter {
    public String key() {
        return "manual";
    }

    public boolean supports(BddEvidence evidence) {
        return evidence != null && "manual".equals(evidence.evidenceType());
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
            return "manual evidence accepted";
        }
        if ("pending".equals(normalizedStatus)) {
            return "manual evidence is pending";
        }
        if ("failed".equals(normalizedStatus)) {
            return "manual evidence failed";
        }
        return "manual evidence status is not accepted";
    }
}
