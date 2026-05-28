package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.repository.bdd.BddBindingRepository;
import com.devharnesskit.dhk.repository.bdd.BddFeatureRepository;
import com.devharnesskit.dhk.repository.bdd.BddScenarioRepository;
import com.devharnesskit.dhk.repository.bdd.BddStepRepository;
import com.devharnesskit.dhk.repository.goal.GoalCheckRepository;
import com.devharnesskit.dhk.repository.goal.GoalStepRepository;
import com.devharnesskit.dhk.repository.brief.InteractionRequestRepository;
import com.devharnesskit.dhk.repository.spec.SpecAcceptanceRepository;
import com.devharnesskit.dhk.repository.spec.SpecTaskRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;
import com.devharnesskit.dhk.service.SensitiveDataGuard;
import com.devharnesskit.dhk.service.bdd.BddService;
import com.devharnesskit.dhk.service.bdd.BddVerificationService;
import com.devharnesskit.dhk.service.graph.GraphArchitectureCheckService;
import com.devharnesskit.dhk.service.policy.DevHarnessPolicyService;
import com.devharnesskit.dhk.service.skill.SkillDisciplineGateService;

final class DefaultGoalCheckRunnerFactory {
    GoalCheckRunnerRegistry create(GoalCheckRepository checkRepository,
                                   SpecTaskRepository taskRepository,
                                   SpecAcceptanceRepository acceptanceRepository,
                                   WorkflowRunRepository workflowRunRepository,
                                   WorkflowGateRunRepository gateRunRepository,
                                   SensitiveDataGuard sensitiveDataGuard,
                                   WorkspaceFingerprintService fingerprintService,
                                   GoalStepRepository stepRepository,
                                   DevHarnessPolicyService devHarnessPolicyService,
                                   GraphArchitectureCheckService architectureCheckService) {
        return create(checkRepository, taskRepository, acceptanceRepository, workflowRunRepository,
                gateRunRepository, sensitiveDataGuard, fingerprintService, stepRepository,
                devHarnessPolicyService, architectureCheckService,
                new BddBindingRepository(), new BddService(new BddFeatureRepository(),
                        new BddScenarioRepository(), new BddStepRepository()), new BddVerificationService());
    }

    GoalCheckRunnerRegistry create(GoalCheckRepository checkRepository,
                                   SpecTaskRepository taskRepository,
                                   SpecAcceptanceRepository acceptanceRepository,
                                   WorkflowRunRepository workflowRunRepository,
                                   WorkflowGateRunRepository gateRunRepository,
                                   SensitiveDataGuard sensitiveDataGuard,
                                   WorkspaceFingerprintService fingerprintService,
                                   GoalStepRepository stepRepository,
                                   DevHarnessPolicyService devHarnessPolicyService,
                                   GraphArchitectureCheckService architectureCheckService,
                                   BddBindingRepository bddBindingRepository,
                                   BddService bddService,
                                   BddVerificationService bddVerificationService) {
        GoalCheckRecorder recorder = new GoalCheckRecorder(checkRepository, fingerprintService);
        GoalCheckCommandExecutor commandExecutor = new GoalCheckCommandExecutor();
        SkillDisciplineGateService disciplineGateService = new SkillDisciplineGateService();
        return GoalCheckRunnerRegistry.of(
                new MavenGoalCheckRunner("compile", recorder, commandExecutor),
                new MavenGoalCheckRunner("test", recorder, commandExecutor),
                new ManualVerificationGoalCheckRunner("manual-compile", recorder, stepRepository,
                        new InteractionRequestRepository(), commandExecutor, "compile_scope"),
                new ManualVerificationGoalCheckRunner("manual-test", recorder, stepRepository,
                        new InteractionRequestRepository(), commandExecutor, "test_scope"),
                new VerificationRiskGoalCheckRunner(recorder, stepRepository),
                new SensitiveGoalCheckRunner(recorder, sensitiveDataGuard),
                new SpecGoalCheckRunner(recorder, taskRepository, acceptanceRepository),
                new WorkflowGoalCheckRunner(recorder, workflowRunRepository, gateRunRepository),
                new GraphGoalCheckRunner(recorder, fingerprintService),
                new ImpactGoalCheckRunner(recorder, stepRepository, bddBindingRepository),
                new LegacyGoalCheckRunner(recorder, stepRepository, devHarnessPolicyService),
                new ArchitectureGoalCheckRunner(recorder, architectureCheckService),
                new BddGoalCheckRunner(recorder, bddBindingRepository, bddService, bddVerificationService),
                new DisciplineGateGoalCheckRunner("think-before-coding", recorder, stepRepository,
                        devHarnessPolicyService, disciplineGateService),
                new DisciplineGateGoalCheckRunner("goal-driven", recorder, stepRepository,
                        devHarnessPolicyService, disciplineGateService),
                new DisciplineGateGoalCheckRunner("simplicity", recorder, stepRepository,
                        devHarnessPolicyService, disciplineGateService),
                new DisciplineGateGoalCheckRunner("surgical-change", recorder, stepRepository,
                        devHarnessPolicyService, disciplineGateService)
        );
    }
}
