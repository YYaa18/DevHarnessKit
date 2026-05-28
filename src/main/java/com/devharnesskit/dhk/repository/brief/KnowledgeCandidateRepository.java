package com.devharnesskit.dhk.repository.brief;

import com.devharnesskit.dhk.model.brief.KnowledgeCandidate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class KnowledgeCandidateRepository {
    public void upsert(Connection connection, String projectKey, KnowledgeCandidate candidate,
                       String now) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO knowledge_candidate(candidate_id, project_key, goal_key, candidate_type, "
                        + "title, summary, evidence_refs, suggested_destination, confidence, "
                        + "requires_confirmation, sensitive_scan_status, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                        + "ON CONFLICT(candidate_id) DO UPDATE SET "
                        + "project_key = excluded.project_key, "
                        + "goal_key = excluded.goal_key, "
                        + "candidate_type = excluded.candidate_type, "
                        + "title = excluded.title, "
                        + "summary = excluded.summary, "
                        + "evidence_refs = excluded.evidence_refs, "
                        + "suggested_destination = excluded.suggested_destination, "
                        + "confidence = excluded.confidence, "
                        + "requires_confirmation = excluded.requires_confirmation, "
                        + "sensitive_scan_status = excluded.sensitive_scan_status, "
                        + "status = excluded.status, "
                        + "updated_at = excluded.updated_at")) {
            statement.setString(1, candidate.candidateId());
            statement.setString(2, projectKey);
            statement.setString(3, candidate.goalKey());
            statement.setString(4, candidate.type());
            statement.setString(5, candidate.title());
            statement.setString(6, candidate.summary());
            statement.setString(7, candidate.evidenceRefs());
            statement.setString(8, candidate.suggestedDestination());
            statement.setString(9, candidate.confidence());
            statement.setInt(10, candidate.requiresConfirmation() ? 1 : 0);
            statement.setString(11, candidate.sensitiveScanStatus());
            statement.setString(12, candidate.status());
            statement.setString(13, now);
            statement.setString(14, now);
            statement.executeUpdate();
        }
    }

    public List<KnowledgeCandidate> list(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM knowledge_candidate ORDER BY updated_at DESC, candidate_id ASC");
             ResultSet resultSet = statement.executeQuery()) {
            List<KnowledgeCandidate> result = new ArrayList<KnowledgeCandidate>();
            while (resultSet.next()) {
                result.add(map(resultSet));
            }
            return result;
        }
    }

    private KnowledgeCandidate map(ResultSet resultSet) throws SQLException {
        return new KnowledgeCandidate(
                resultSet.getString("candidate_id"),
                resultSet.getString("goal_key"),
                resultSet.getString("candidate_type"),
                resultSet.getString("title"),
                resultSet.getString("summary"),
                resultSet.getString("evidence_refs"),
                resultSet.getString("suggested_destination"),
                resultSet.getString("confidence"),
                resultSet.getInt("requires_confirmation") == 1,
                resultSet.getString("sensitive_scan_status"),
                resultSet.getString("status"));
    }
}
