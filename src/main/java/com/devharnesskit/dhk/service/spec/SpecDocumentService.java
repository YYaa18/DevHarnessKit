package com.devharnesskit.dhk.service.spec;

import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.spec.SpecDocument;
import com.devharnesskit.dhk.model.spec.SpecEvent;
import com.devharnesskit.dhk.repository.spec.SpecDocumentRepository;
import com.devharnesskit.dhk.repository.spec.SpecEventRepository;

import java.sql.Connection;
import java.sql.SQLException;

public final class SpecDocumentService {
    private final SpecDocumentRepository documentRepository;
    private final SpecEventRepository eventRepository;

    public SpecDocumentService(SpecDocumentRepository documentRepository,
                               SpecEventRepository eventRepository) {
        this.documentRepository = documentRepository;
        this.eventRepository = eventRepository;
    }

    public SpecDocument setDocument(Connection connection, SpecChange change, String type,
                                    String title, String content, String status,
                                    String now) throws SQLException {
        SpecDocument existing = documentRepository.findLatestByType(connection, change.changeKey(), type);
        SpecDocument document;
        if (existing == null) {
            document = new SpecDocument(0L, change.changeKey(), type, title, content,
                    SpecService.sha256(content), status, 1, now, now);
            long id = documentRepository.insert(connection, document);
            document = new SpecDocument(id, change.changeKey(), type, title, content,
                    document.contentHash(), status, 1, now, now);
        } else {
            document = new SpecDocument(existing.id(), change.changeKey(), type, title, content,
                    SpecService.sha256(content), status, existing.version() + 1,
                    existing.createdAt(), now);
            documentRepository.update(connection, document);
        }
        eventRepository.insert(connection, new SpecEvent(0L, change.projectKey(), change.changeKey(),
                "document_upserted", "info", "Spec document upserted: " + type,
                "version=" + document.version(), now));
        return document;
    }

    public static boolean isDocumentTypeAllowed(String type) {
        return "proposal".equals(type) || "design".equals(type) || "requirements".equals(type)
                || "tasks".equals(type) || "acceptance".equals(type)
                || "verification".equals(type) || "notes".equals(type);
    }

    public static boolean isDocumentStatusAllowed(String status) {
        return "draft".equals(status) || "confirmed".equals(status)
                || "deprecated".equals(status) || "archived".equals(status);
    }
}
