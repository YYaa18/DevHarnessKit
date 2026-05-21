package com.devharnesskit.dhk.service.spec;

import com.devharnesskit.dhk.model.spec.SpecAcceptance;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.spec.SpecEvent;
import com.devharnesskit.dhk.repository.spec.SpecAcceptanceRepository;
import com.devharnesskit.dhk.repository.spec.SpecEventRepository;

import java.sql.Connection;
import java.sql.SQLException;

public final class SpecAcceptanceService {
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
        String verifiedAt = "passed".equals(status) || "waived".equals(status) ? now : "";
        acceptanceRepository.updateStatus(connection, change.changeKey(), acceptance.acceptanceKey(),
                status, evidence, verifiedAt, now);
        eventRepository.insert(connection, new SpecEvent(0L, change.projectKey(), change.changeKey(),
                "acceptance_updated", "info",
                "Spec acceptance updated: " + acceptance.acceptanceKey(), status, now));
    }

    public static boolean isAcceptanceStatusAllowed(String status) {
        return "pending".equals(status) || "passed".equals(status)
                || "failed".equals(status) || "waived".equals(status);
    }
}
