package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.model.skill.SkillDisciplineGateResult;
import com.devharnesskit.dhk.repository.goal.GoalStepRepository;
import com.devharnesskit.dhk.service.policy.DevHarnessPolicyService;
import com.devharnesskit.dhk.service.skill.SkillDisciplineGateService;

import java.nio.file.Path;
import java.util.List;

final class DisciplineGateGoalCheckRunner extends AbstractGoalCheckRunner {
    private final GoalCheckRecorder recorder;
    private final GoalStepRepository stepRepository;
    private final DevHarnessPolicyService devHarnessPolicyService;
    private final SkillDisciplineGateService disciplineGateService;

    DisciplineGateGoalCheckRunner(String key, GoalCheckRecorder recorder, GoalStepRepository stepRepository,
                                  DevHarnessPolicyService devHarnessPolicyService,
                                  SkillDisciplineGateService disciplineGateService) {
        super(key);
        this.recorder = recorder;
        this.stepRepository = stepRepository;
        this.devHarnessPolicyService = devHarnessPolicyService;
        this.disciplineGateService = disciplineGateService;
    }

    public GoalCheck run(GoalCheckContext context) throws Exception {
        Path log = recorder.logPath(context.projectRoot(), context.goal(), key());
        List<GoalStep> steps = stepRepository.listByGoal(context.connection(), context.goal().goalKey());
        SkillDisciplineGateResult result;
        if ("think-before-coding".equals(key())) {
            result = disciplineGateService.thinkBeforeCoding(context.goal(), context.profile(), steps);
        } else if ("goal-driven".equals(key())) {
            result = disciplineGateService.goalDriven(context.goal(), context.profile(), steps);
        } else if ("simplicity".equals(key())) {
            result = disciplineGateService.simplicity(context.goal(), context.profile(), steps);
        } else {
            result = disciplineGateService.surgicalChange(context.goal(), context.profile(), steps,
                    devHarnessPolicyService.load(context.projectRoot()).protectedFiles());
        }
        recorder.writeLog(log, result.output());
        return recorder.save(context.connection(), context.projectRoot(), context.goal(), key(), "discipline",
                "", result.status(), result.summary(), log, context.now());
    }
}
