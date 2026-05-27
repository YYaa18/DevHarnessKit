package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.repository.bdd.BddBindingRepository;
import com.devharnesskit.dhk.repository.bdd.BddFeatureRepository;
import com.devharnesskit.dhk.repository.bdd.BddScenarioRepository;
import com.devharnesskit.dhk.repository.bdd.BddStepRepository;
import com.devharnesskit.dhk.repository.goal.GoalCheckRepository;
import com.devharnesskit.dhk.repository.goal.GoalStepRepository;
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

import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public final class GoalCheckService {
    public static final String[] REQUIRED_CHECKS = GoalCheckPolicy.DEFAULT_REQUIRED_CHECKS;
    static final int MAX_COMMAND_LOG_BYTES = GoalCheckCommandExecutor.MAX_COMMAND_LOG_BYTES;

    private final GoalCheckPolicyService policyService;
    private final GoalProfileService profileService;
    private final GoalCheckRunnerRegistry runnerRegistry;

    public GoalCheckService() {
        this(new GoalCheckRepository(), new SpecTaskRepository(), new SpecAcceptanceRepository(),
                new WorkflowRunRepository(), new WorkflowGateRunRepository(), new SensitiveDataGuard(),
                new GoalCheckPolicyService(), new GoalProfileService(), new WorkspaceFingerprintService(),
                new GoalStepRepository(), new DevHarnessPolicyService(), new GraphArchitectureCheckService());
    }

    GoalCheckService(GoalCheckRepository checkRepository, SpecTaskRepository taskRepository,
                     SpecAcceptanceRepository acceptanceRepository,
                     WorkflowRunRepository workflowRunRepository,
                     WorkflowGateRunRepository gateRunRepository,
                     SensitiveDataGuard sensitiveDataGuard,
                     GoalCheckPolicyService policyService,
                     GoalProfileService profileService,
                     WorkspaceFingerprintService fingerprintService,
                     GoalStepRepository stepRepository,
                     DevHarnessPolicyService devHarnessPolicyService,
                     GraphArchitectureCheckService architectureCheckService) {
        this(checkRepository, taskRepository, acceptanceRepository, workflowRunRepository,
                gateRunRepository, sensitiveDataGuard, policyService, profileService, fingerprintService,
                stepRepository, devHarnessPolicyService, architectureCheckService,
                new BddBindingRepository(), new BddService(new BddFeatureRepository(),
                new BddScenarioRepository(), new BddStepRepository()), new BddVerificationService());
    }

    GoalCheckService(GoalCheckRepository checkRepository, SpecTaskRepository taskRepository,
                     SpecAcceptanceRepository acceptanceRepository,
                     WorkflowRunRepository workflowRunRepository,
                     WorkflowGateRunRepository gateRunRepository,
                     SensitiveDataGuard sensitiveDataGuard,
                     GoalCheckPolicyService policyService,
                     GoalProfileService profileService,
                     WorkspaceFingerprintService fingerprintService,
                     GoalStepRepository stepRepository,
                     DevHarnessPolicyService devHarnessPolicyService,
                     GraphArchitectureCheckService architectureCheckService,
                     BddBindingRepository bddBindingRepository,
                     BddService bddService,
                     BddVerificationService bddVerificationService) {
        this.policyService = policyService;
        this.profileService = profileService;
        GoalCheckRecorder recorder = new GoalCheckRecorder(checkRepository, fingerprintService);
        GoalCheckCommandExecutor commandExecutor = new GoalCheckCommandExecutor();
        SkillDisciplineGateService disciplineGateService = new SkillDisciplineGateService();
        this.runnerRegistry = GoalCheckRunnerRegistry.of(
                new MavenGoalCheckRunner("compile", recorder, commandExecutor),
                new MavenGoalCheckRunner("test", recorder, commandExecutor),
                new ManualVerificationGoalCheckRunner("manual-compile", recorder, stepRepository, "compile_scope"),
                new ManualVerificationGoalCheckRunner("manual-test", recorder, stepRepository, "test_scope"),
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

    public GoalCheck run(Connection connection, Path projectRoot, GoalRun goal,
                         String checkKey, String now) throws Exception {
        return run(connection, projectRoot, goal, checkKey, now, policyService.load(projectRoot));
    }

    public GoalCheck run(Connection connection, Path projectRoot, GoalRun goal,
                         String checkKey, String now, GoalCheckPolicy policy) throws Exception {
        GoalProfile profile = profileService.find(projectRoot, goal.profileKey());
        GoalCheckRunner runner = runnerRegistry.find(checkKey);
        if (runner == null) {
            throw new IllegalArgumentException("Unknown goal check: " + checkKey);
        }
        return runner.run(new GoalCheckContext(connection, projectRoot, goal, checkKey, now, policy, profile));
    }

    public List<GoalCheck> runAll(Connection connection, Path projectRoot, GoalRun goal,
                                  String now) throws Exception {
        GoalCheckPolicy policy = policyService.load(projectRoot);
        List<GoalCheck> results = new ArrayList<GoalCheck>();
        for (String check : policy.requiredChecks(profileService.find(projectRoot, goal.profileKey()))) {
            results.add(run(connection, projectRoot, goal, check, now, policy));
        }
        return results;
    }

    GoalCheckRunnerRegistry runnerRegistry() {
        return runnerRegistry;
    }
}
