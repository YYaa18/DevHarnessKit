package com.devharnesskit.dhk.service.bdd.adapter;

import com.devharnesskit.dhk.model.bdd.BddAdapterResult;
import com.devharnesskit.dhk.model.bdd.BddEvidence;

public final class ReportBddEvidenceAdapter implements BddEvidenceAdapter {
    public String key() {
        return "report";
    }

    public boolean supports(BddEvidence evidence) {
        if (evidence == null || !"test".equals(evidence.evidenceType())) {
            return false;
        }
        return adapterKey(evidence).length() > 0;
    }

    public BddAdapterResult normalize(BddEvidence evidence) {
        String normalizedStatus = normalizedStatus(evidence.status());
        String adapterKey = adapterKey(evidence);
        return new BddAdapterResult(adapterKey, evidence.id(), evidence.evidenceType(), evidence.status(),
                normalizedStatus, "covered".equals(normalizedStatus), evidence.summary(),
                evidence.evidencePath(), evidence.command(), message(adapterKey, normalizedStatus));
    }

    private String adapterKey(BddEvidence evidence) {
        String command = evidence.command();
        if (command.startsWith("cucumber:")) {
            return "cucumber";
        }
        if (command.startsWith("postman:")) {
            return "postman";
        }
        if (command.startsWith("playwright:")) {
            return "playwright";
        }
        return "";
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

    private String message(String adapterKey, String normalizedStatus) {
        if ("covered".equals(normalizedStatus)) {
            return adapterKey + " report evidence accepted";
        }
        if ("pending".equals(normalizedStatus)) {
            return adapterKey + " report evidence is pending";
        }
        if ("failed".equals(normalizedStatus)) {
            return adapterKey + " report evidence failed";
        }
        return adapterKey + " report evidence status is not accepted";
    }
}
