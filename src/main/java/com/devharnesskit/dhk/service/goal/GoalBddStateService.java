package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.bdd.BddBinding;
import com.devharnesskit.dhk.model.bdd.BddEvidence;
import com.devharnesskit.dhk.model.bdd.BddScenarioView;
import com.devharnesskit.dhk.model.goal.GoalBddState;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.repository.bdd.BddBindingRepository;
import com.devharnesskit.dhk.repository.bdd.BddFeatureRepository;
import com.devharnesskit.dhk.repository.bdd.BddScenarioRepository;
import com.devharnesskit.dhk.repository.bdd.BddStepRepository;
import com.devharnesskit.dhk.service.bdd.BddService;
import com.devharnesskit.dhk.service.bdd.BddVerificationService;
import com.devharnesskit.dhk.service.bdd.BddVerificationService.BddScenarioEvidenceResult;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

public final class GoalBddStateService {
    private final BddBindingRepository bindingRepository;
    private final BddService bddService;
    private final BddVerificationService verificationService;

    public GoalBddStateService() {
        this(new BddBindingRepository(), new BddService(new BddFeatureRepository(),
                new BddScenarioRepository(), new BddStepRepository()), new BddVerificationService());
    }

    GoalBddStateService(BddBindingRepository bindingRepository, BddService bddService,
                        BddVerificationService verificationService) {
        this.bindingRepository = bindingRepository;
        this.bddService = bddService;
        this.verificationService = verificationService;
    }

    public GoalBddState inspect(Connection connection, Path projectRoot, Project project,
                                GoalRun goal, GoalProfile profile) throws Exception {
        if (profile == null || !profile.bddRequired()) {
            return GoalBddState.disabled();
        }
        int scenarioCount = 0;
        int coveredCount = 0;
        int missingCount = 0;
        int pendingCount = 0;
        int failedCount = 0;
        List<BddBinding> bindings = bindingRepository.listByBinding(connection, "goal", goal.goalKey());
        for (BddBinding binding : bindings) {
            BddScenarioView view = bddService.findScenario(connection, project, binding.scenarioKey());
            if (view == null) {
                continue;
            }
            List<BddEvidence> evidence = bddService.listEvidence(connection,
                    view.scenario().scenarioKey(), goal.goalKey());
            BddScenarioEvidenceResult scenario = verificationService.evaluateScenario(view, evidence);
            scenarioCount++;
            if (scenario.covered()) {
                coveredCount++;
            } else if ("missing".equals(scenario.status())) {
                missingCount++;
            } else if ("pending".equals(scenario.status())) {
                pendingCount++;
            } else if ("failed".equals(scenario.status())) {
                failedCount++;
            }
        }
        if (scenarioCount == 0) {
            missingCount = 1;
        }
        Path scenarioImpactMap = PathUtil.scenarioImpactMap(projectRoot);
        return new GoalBddState(true, true, scenarioCount, coveredCount, missingCount,
                pendingCount, failedCount, PathUtil.bddEvidence(projectRoot).toString(),
                PathUtil.bddCoverage(projectRoot).toString(), scenarioImpactMap.toString(),
                Files.isRegularFile(scenarioImpactMap), nextCommand(projectRoot, goal, bindings));
    }

    private String nextCommand(Path projectRoot, GoalRun goal, List<BddBinding> bindings) {
        String root = projectRoot.toAbsolutePath().normalize().toString();
        if (bindings.isEmpty()) {
            return "dhk bdd bind-goal --project-root " + root
                    + " --scenario <scenario-key> --goal " + goal.goalKey();
        }
        return "dhk bdd evidence add --project-root " + root
                + " --scenario <scenario-key> --goal " + goal.goalKey()
                + " --status passed --summary \"<evidence>\"";
    }
}
