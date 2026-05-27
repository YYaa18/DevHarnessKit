package com.devharnesskit.dhk.service.checkpoint;

import com.devharnesskit.dhk.model.checkpoint.HumanCheckpoint;
import com.devharnesskit.dhk.model.goal.GoalEvaluation;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.policy.DevHarnessPolicy;
import com.devharnesskit.dhk.model.skill.SkillContract;
import com.devharnesskit.dhk.repository.checkpoint.HumanCheckpointRepository;
import com.devharnesskit.dhk.repository.skill.SkillContractRepository;
import com.devharnesskit.dhk.service.policy.PolicyHookService;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public final class HumanCheckpointService {
    private final HumanCheckpointRepository repository;
    private final SkillContractRepository skillContractRepository;

    public HumanCheckpointService() {
        this(new HumanCheckpointRepository(), new SkillContractRepository());
    }

    public HumanCheckpointService(HumanCheckpointRepository repository) {
        this(repository, new SkillContractRepository());
    }

    public HumanCheckpointService(HumanCheckpointRepository repository,
                                  SkillContractRepository skillContractRepository) {
        this.repository = repository;
        this.skillContractRepository = skillContractRepository;
    }

    public HumanCheckpoint request(Connection connection, GoalRun goal, String checkpointType,
                                   String reason, String requestedBy, String now) throws Exception {
        HumanCheckpoint checkpoint = new HumanCheckpoint(0L, goal.goalKey(),
                key(checkpointType, "before_complete"), reason, "pending",
                value(requestedBy, "manual"), now, "", "", "", now, now);
        long id = repository.insert(connection, checkpoint);
        return repository.find(connection, id);
    }

    public HumanCheckpoint approve(Connection connection, long id, String approver,
                                   String decisionReason, String now) throws Exception {
        HumanCheckpoint checkpoint = repository.find(connection, id);
        if (checkpoint == null) {
            throw new IllegalArgumentException("Human checkpoint not found: " + id);
        }
        if (!"pending".equals(checkpoint.status())) {
            throw new IllegalArgumentException("Human checkpoint is not pending: " + id);
        }
        repository.approve(connection, id, value(approver, "manual"), now, decisionReason);
        return repository.find(connection, id);
    }

    public List<HumanCheckpoint> list(Connection connection, String goalKey) throws Exception {
        return repository.listByGoal(connection, goalKey);
    }

    public GoalEvaluation applyCompletionGate(Connection connection, GoalRun goal,
                                              GoalEvaluation evaluation,
                                              DevHarnessPolicy policy) throws Exception {
        if (evaluation == null || !evaluation.readyToComplete()) {
            return evaluation;
        }
        if (policy == null || !policy.humanCheckpointRequired()) {
            return applySkillTrustGate(connection, goal, evaluation, policy);
        }
        String type = key(policy.humanCheckpointType(), "before_complete");
        boolean approved = repository.hasApproved(connection, goal.goalKey(), type);
        String blocker = PolicyHookService.humanCheckpointBlocker(policy, approved, type);
        if (blocker.length() == 0) {
            return applySkillTrustGate(connection, goal, evaluation, policy);
        }
        List<String> missing = new ArrayList<String>();
        missing.add(blocker);
        return new GoalEvaluation("not_ready", missing.toArray(new String[missing.size()]), new String[0],
                "request_human_checkpoint",
                "dhk checkpoint request --goal " + goal.goalKey() + " --type " + type
                        + " --reason \"<reason>\"");
    }

    private GoalEvaluation applySkillTrustGate(Connection connection, GoalRun goal,
                                               GoalEvaluation evaluation,
                                               DevHarnessPolicy policy) throws Exception {
        if (evaluation == null || !evaluation.readyToComplete()) {
            return evaluation;
        }
        if (policy == null || !policy.skillTrustRequiredForHighRisk()) {
            return evaluation;
        }
        String skillKey = policy.skillKey();
        if (skillKey.length() == 0) {
            return blocked(goal, policy, "skill_trust_required: skill=missing status=missing_skill_key");
        }
        SkillContract contract = skillContractRepository.findByKey(connection, skillKey);
        if (contract == null) {
            return blocked(goal, policy, "skill_trust_required: skill=" + skillKey
                    + " status=missing_contract");
        }
        if (!"high".equals(contract.riskLevel())) {
            return evaluation;
        }
        if (isTrusted(contract)) {
            return evaluation;
        }
        String type = key(policy.skillTrustOverrideCheckpointType(), "skill_trust_override");
        boolean approved = repository.hasApproved(connection, goal.goalKey(), type);
        if (approved) {
            return evaluation;
        }
        return blocked(goal, policy, "skill_trust_required: skill=" + skillKey
                + " risk_level=" + contract.riskLevel()
                + " trust_status=" + contract.trustStatus()
                + " trusted=" + contract.trusted()
                + " status=missing_approved");
    }

    private GoalEvaluation blocked(GoalRun goal, DevHarnessPolicy policy, String blocker) {
        List<String> missing = new ArrayList<String>();
        missing.add(blocker);
        String type = key(policy == null ? "" : policy.skillTrustOverrideCheckpointType(), "skill_trust_override");
        return new GoalEvaluation("not_ready", missing.toArray(new String[missing.size()]), new String[0],
                "request_skill_trust_checkpoint",
                "dhk checkpoint request --goal " + goal.goalKey() + " --type " + type
                        + " --reason \"<reason>\"");
    }

    private boolean isTrusted(SkillContract contract) {
        return contract.trusted()
                && "trusted".equals(contract.trustStatus())
                && contract.sourceHash().length() > 0
                && contract.sourceHash().equals(contract.trustedSourceHash());
    }

    private String value(String raw, String fallback) {
        if (raw == null || raw.trim().length() == 0) {
            return fallback;
        }
        return raw.trim();
    }

    private String key(String raw, String fallback) {
        String value = value(raw, fallback).toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
        return value.length() == 0 ? fallback : value;
    }
}
