package com.devharnesskit.dhk.service.spec;

import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.spec.SpecDocument;
import com.devharnesskit.dhk.model.spec.SpecEvent;
import com.devharnesskit.dhk.model.spec.WorkflowSpecBinding;
import com.devharnesskit.dhk.model.workflow.WorkflowRun;
import com.devharnesskit.dhk.repository.spec.SpecChangeRepository;
import com.devharnesskit.dhk.repository.spec.SpecDocumentRepository;
import com.devharnesskit.dhk.repository.spec.SpecEventRepository;
import com.devharnesskit.dhk.repository.spec.WorkflowSpecBindingRepository;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.SQLException;

public final class SpecService {
    private final SpecChangeRepository changeRepository;
    private final SpecDocumentRepository documentRepository;
    private final SpecEventRepository eventRepository;
    private final WorkflowSpecBindingRepository bindingRepository;

    public SpecService(SpecChangeRepository changeRepository,
                       SpecDocumentRepository documentRepository,
                       SpecEventRepository eventRepository,
                       WorkflowSpecBindingRepository bindingRepository) {
        this.changeRepository = changeRepository;
        this.documentRepository = documentRepository;
        this.eventRepository = eventRepository;
        this.bindingRepository = bindingRepository;
    }

    public SpecChange createChange(Connection connection, Project project, String changeKey,
                                   String title, String summary, String module, String mode,
                                   String priority, String now) throws SQLException {
        SpecChange existing = changeRepository.findByKey(connection, changeKey);
        if (existing != null) {
            throw new SQLException("Spec change already exists: " + changeKey);
        }
        SpecChange change = new SpecChange(changeKey, project.projectKey(), title, summary,
                module, mode, "draft", priority, "manual", "manual", now, now, "");
        changeRepository.insert(connection, change);
        String proposal = defaultProposal(title, summary);
        documentRepository.insert(connection, new SpecDocument(0L, changeKey, "proposal",
                title + " Proposal", proposal, sha256(proposal), "draft", 1, now, now));
        eventRepository.insert(connection, new SpecEvent(0L, project.projectKey(), changeKey,
                "change_created", "info", "Spec change created", title, now));
        return change;
    }

    public long bindWorkflow(Connection connection, SpecChange change, WorkflowRun run,
                             String bindingType, String now) throws SQLException {
        long id = bindingRepository.insert(connection, new WorkflowSpecBinding(0L, run.runKey(),
                change.changeKey(), bindingType, now));
        eventRepository.insert(connection, new SpecEvent(0L, change.projectKey(), change.changeKey(),
                "workflow_bound", "info", "Workflow bound: " + run.runKey(), bindingType, now));
        return id;
    }

    public void archive(Connection connection, SpecChange change, String reason, String now) throws SQLException {
        changeRepository.archive(connection, change.changeKey(), now);
        eventRepository.insert(connection, new SpecEvent(0L, change.projectKey(), change.changeKey(),
                "change_archived", "info", "Spec change archived", reason, now));
    }

    public static boolean isModeAllowed(String mode) {
        return "auto".equals(mode) || "api".equals(mode) || "mvc".equals(mode) || "mixed".equals(mode)
                || "sql".equals(mode) || "debug".equals(mode) || "review".equals(mode);
    }

    public static boolean isPriorityAllowed(String priority) {
        return "low".equals(priority) || "normal".equals(priority)
                || "high".equals(priority) || "critical".equals(priority);
    }

    public static boolean isWorkflowBindingType(String type) {
        return "implements".equals(type) || "reviews".equals(type)
                || "verifies".equals(type) || "archives".equals(type);
    }

    static String sha256(String text) {
        if (text == null || text.length() == 0) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes("UTF-8"));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toString(b & 0xff, 16);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    private String defaultProposal(String title, String summary) {
        return "# Proposal\n\n"
                + "## Why\n\n"
                + summary + "\n\n"
                + "## What Changes\n\n"
                + "- " + title + "\n\n"
                + "## Non-goals\n\n"
                + "## Risks\n\n"
                + "## Acceptance Summary\n";
    }
}
