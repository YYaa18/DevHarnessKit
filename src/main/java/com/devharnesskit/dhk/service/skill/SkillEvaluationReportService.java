package com.devharnesskit.dhk.service.skill;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.model.policy.DevHarnessPolicy;
import com.devharnesskit.dhk.model.skill.SkillContract;
import com.devharnesskit.dhk.model.skill.SkillEvaluationReport;
import com.devharnesskit.dhk.model.skill.SkillQualityScore;
import com.devharnesskit.dhk.repository.skill.SkillContractRepository;
import com.devharnesskit.dhk.service.policy.DevHarnessPolicyService;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

public final class SkillEvaluationReportService {
    private final SkillQualityScoreService scoreService;
    private final DevHarnessPolicyService policyService;
    private final SkillContractRepository skillContractRepository;

    public SkillEvaluationReportService() {
        this(new SkillQualityScoreService(), new DevHarnessPolicyService(), new SkillContractRepository());
    }

    SkillEvaluationReportService(SkillQualityScoreService scoreService,
                                 DevHarnessPolicyService policyService,
                                 SkillContractRepository skillContractRepository) {
        this.scoreService = scoreService;
        this.policyService = policyService;
        this.skillContractRepository = skillContractRepository;
    }

    public SkillEvaluationReport report(Connection connection, Path projectRoot, GoalRun goal,
                                        List<GoalStep> steps, List<GoalCheck> checks,
                                        String group, int baselineScore) throws Exception {
        SkillQualityScore score = scoreService.score(goal.goalKey(), steps, checks);
        DevHarnessPolicy policy = policyService.load(projectRoot);
        String skillKey = policy.skillKey();
        SkillContract contract = skillKey.length() == 0 ? null : skillContractRepository.findByKey(connection, skillKey);
        return new SkillEvaluationReport(goal.goalKey(), group == null || group.length() == 0 ? "F" : group,
                baselineScore, score, contract != null, skillKey,
                contract == null ? "" : contract.trustStatus(),
                contract != null && contract.trusted());
    }
}
