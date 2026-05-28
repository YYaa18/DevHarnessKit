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
                        + "requires_confirmation, sensitive_scan_status, status, source_rule_id, "
                        + "source_pack_key, domain, severity, applicable_when, not_applicable_when, "
                        + "destination_reason, metadata, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
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
                        + "source_rule_id = excluded.source_rule_id, "
                        + "source_pack_key = excluded.source_pack_key, "
                        + "domain = excluded.domain, "
                        + "severity = excluded.severity, "
                        + "applicable_when = excluded.applicable_when, "
                        + "not_applicable_when = excluded.not_applicable_when, "
                        + "destination_reason = excluded.destination_reason, "
                        + "metadata = excluded.metadata, "
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
            statement.setString(13, candidate.sourceRuleId());
            statement.setString(14, candidate.sourcePackKey());
            statement.setString(15, candidate.domain());
            statement.setString(16, candidate.severity());
            statement.setString(17, candidate.applicableWhen());
            statement.setString(18, candidate.notApplicableWhen());
            statement.setString(19, candidate.destinationReason());
            statement.setString(20, candidate.metadata());
            statement.setString(21, now);
            statement.setString(22, now);
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
                resultSet.getString("status"),
                get(resultSet, "source_rule_id"),
                get(resultSet, "source_pack_key"),
                get(resultSet, "domain"),
                get(resultSet, "severity"),
                get(resultSet, "applicable_when"),
                get(resultSet, "not_applicable_when"),
                get(resultSet, "destination_reason"),
                get(resultSet, "metadata"));
    }

    private String get(ResultSet resultSet, String column) throws SQLException {
        try {
            String value = resultSet.getString(column);
            return value == null ? "" : value;
        } catch (SQLException ex) {
            return "";
        }
    }
}
