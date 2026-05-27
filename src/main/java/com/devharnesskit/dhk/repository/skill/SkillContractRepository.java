package com.devharnesskit.dhk.repository.skill;

import com.devharnesskit.dhk.model.skill.SkillContract;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class SkillContractRepository {
    public void upsert(Connection connection, SkillContract contract) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO skill_contract(skill_key, version, task_type, risk_level, mode, "
                        + "data_access_level, allowed_commands, forbidden_commands, contract_json, "
                        + "source_path, source_hash, trusted_source_hash, trust_status, trusted, "
                        + "created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                        + "ON CONFLICT(skill_key) DO UPDATE SET "
                        + "version = excluded.version, "
                        + "task_type = excluded.task_type, "
                        + "risk_level = excluded.risk_level, "
                        + "mode = excluded.mode, "
                        + "data_access_level = excluded.data_access_level, "
                        + "allowed_commands = excluded.allowed_commands, "
                        + "forbidden_commands = excluded.forbidden_commands, "
                        + "contract_json = excluded.contract_json, "
                        + "source_path = excluded.source_path, "
                        + "source_hash = excluded.source_hash, "
                        + "trusted_source_hash = excluded.trusted_source_hash, "
                        + "trust_status = excluded.trust_status, "
                        + "trusted = excluded.trusted, "
                        + "updated_at = excluded.updated_at")) {
            statement.setString(1, contract.skillKey());
            statement.setString(2, contract.version());
            statement.setString(3, contract.taskType());
            statement.setString(4, contract.riskLevel());
            statement.setString(5, contract.mode());
            statement.setString(6, contract.dataAccessLevel());
            statement.setString(7, contract.allowedCommandsText());
            statement.setString(8, contract.forbiddenCommandsText());
            statement.setString(9, contract.contractJson());
            statement.setString(10, contract.sourcePath());
            statement.setString(11, contract.sourceHash());
            statement.setString(12, contract.trustedSourceHash());
            statement.setString(13, contract.trustStatus());
            statement.setInt(14, contract.trusted() ? 1 : 0);
            statement.setString(15, contract.createdAt());
            statement.setString(16, contract.updatedAt());
            statement.executeUpdate();
        }
    }

    public SkillContract findByKey(Connection connection, String skillKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM skill_contract WHERE skill_key = ?")) {
            statement.setString(1, skillKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return map(resultSet);
            }
        }
    }

    private SkillContract map(ResultSet resultSet) throws SQLException {
        return new SkillContract(
                resultSet.getString("skill_key"),
                resultSet.getString("version"),
                resultSet.getString("task_type"),
                resultSet.getString("risk_level"),
                resultSet.getString("mode"),
                resultSet.getString("data_access_level"),
                splitLines(resultSet.getString("allowed_commands")),
                splitLines(resultSet.getString("forbidden_commands")),
                resultSet.getString("contract_json"),
                resultSet.getString("source_path"),
                safeString(resultSet, "source_hash"),
                safeString(resultSet, "trusted_source_hash"),
                safeString(resultSet, "trust_status"),
                resultSet.getInt("trusted") == 1,
                resultSet.getString("created_at"),
                resultSet.getString("updated_at"));
    }

    private String safeString(ResultSet resultSet, String columnName) throws SQLException {
        try {
            String value = resultSet.getString(columnName);
            return value == null ? "" : value;
        } catch (SQLException ex) {
            return "";
        }
    }

    private java.util.List<String> splitLines(String value) {
        java.util.List<String> result = new java.util.ArrayList<String>();
        if (value == null || value.length() == 0) {
            return result;
        }
        String[] parts = value.split("\\r?\\n");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.length() > 0) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
