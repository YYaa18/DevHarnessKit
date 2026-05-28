package com.devharnesskit.dhk.service.spec;

import com.devharnesskit.dhk.model.spec.SpecAcceptance;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.spec.SpecEvent;
import com.devharnesskit.dhk.repository.spec.SpecAcceptanceRepository;
import com.devharnesskit.dhk.repository.spec.SpecEventRepository;

import java.sql.Connection;
import java.sql.SQLException;

public final class SpecAcceptanceService {
    public static final String[] ALLOWED_STATUSES = new String[]{"pending", "passed", "failed", "waived"};
    public static final String[] STATUS_ALIASES = new String[]{
            "done -> passed",
            "closed -> passed",
            "accepted -> passed",
            "resolved -> passed",
            "approved -> passed",
            "open -> pending",
            "in_progress -> pending",
            "rejected -> failed",
            "waive -> waived"
    };
    public static final String ALLOWED_STATUS_TEXT = "pending, passed, failed, waived";
    public static final String STATUS_ALIAS_TEXT = "done/closed/accepted/resolved/approved -> passed; "
            + "open/in_progress -> pending; rejected -> failed; waive -> waived";

    private final SpecAcceptanceRepository acceptanceRepository;
    private final SpecEventRepository eventRepository;

    public SpecAcceptanceService(SpecAcceptanceRepository acceptanceRepository,
                                 SpecEventRepository eventRepository) {
        this.acceptanceRepository = acceptanceRepository;
        this.eventRepository = eventRepository;
    }

    public SpecAcceptance addAcceptance(Connection connection, SpecChange change, String acceptanceKey,
                                        String description, String expectedResult,
                                        String now) throws SQLException {
        if (acceptanceRepository.findByKey(connection, change.changeKey(), acceptanceKey) != null) {
            throw new SQLException("Spec acceptance already exists: " + acceptanceKey);
        }
        SpecAcceptance acceptance = new SpecAcceptance(0L, change.changeKey(), acceptanceKey,
                acceptanceRepository.nextOrder(connection, change.changeKey()), description,
                expectedResult, "pending", "", now, now, "");
        long id = acceptanceRepository.insert(connection, acceptance);
        eventRepository.insert(connection, new SpecEvent(0L, change.projectKey(), change.changeKey(),
                "acceptance_added", "info", "Spec acceptance added: " + acceptanceKey,
                description, now));
        return new SpecAcceptance(id, acceptance.changeKey(), acceptance.acceptanceKey(),
                acceptance.acceptanceOrder(), acceptance.description(), acceptance.expectedResult(),
                acceptance.status(), acceptance.evidence(), acceptance.createdAt(),
                acceptance.updatedAt(), acceptance.verifiedAt());
    }

    public void updateAcceptance(Connection connection, SpecChange change, SpecAcceptance acceptance,
                                 String status, String evidence, String now) throws SQLException {
        String normalizedStatus = normalizeAcceptanceStatus(status);
        String verifiedAt = "passed".equals(normalizedStatus) || "waived".equals(normalizedStatus) ? now : "";
        acceptanceRepository.updateStatus(connection, change.changeKey(), acceptance.acceptanceKey(),
                normalizedStatus, evidence, verifiedAt, now);
        eventRepository.insert(connection, new SpecEvent(0L, change.projectKey(), change.changeKey(),
                "acceptance_updated", "info",
                "Spec acceptance updated: " + acceptance.acceptanceKey(), normalizedStatus, now));
    }

    public static boolean isAcceptanceStatusAllowed(String status) {
        return normalizeAcceptanceStatus(status).length() > 0;
    }

    public static String normalizeAcceptanceStatus(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase(java.util.Locale.ROOT);
        if ("pending".equals(normalized) || "passed".equals(normalized)
                || "failed".equals(normalized) || "waived".equals(normalized)) {
            return normalized;
        }
        if ("done".equals(normalized) || "closed".equals(normalized)
                || "accepted".equals(normalized) || "resolved".equals(normalized)
                || "approved".equals(normalized)) {
            return "passed";
        }
        if ("open".equals(normalized) || "in_progress".equals(normalized)
                || "in-progress".equals(normalized)) {
            return "pending";
        }
        if ("rejected".equals(normalized)) {
            return "failed";
        }
        if ("waive".equals(normalized)) {
            return "waived";
        }
        return "";
    }
}
