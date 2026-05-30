package com.devharnesskit.dhk.service.bdd;

import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.bdd.BddAdapterResult;
import com.devharnesskit.dhk.model.bdd.BddBinding;
import com.devharnesskit.dhk.model.bdd.BddEvidence;
import com.devharnesskit.dhk.model.bdd.BddScenario;
import com.devharnesskit.dhk.model.bdd.BddScenarioView;
import com.devharnesskit.dhk.repository.bdd.BddBindingRepository;
import com.devharnesskit.dhk.service.bdd.adapter.BddEvidenceAdapter;
import com.devharnesskit.dhk.service.bdd.adapter.BddEvidenceAdapterRegistry;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BddVerificationService {
    private final BddEvidenceAdapterRegistry adapterRegistry;
    private final BddBindingRepository bindingRepository;

    public BddVerificationService() {
        this(new BddEvidenceAdapterRegistry(), new BddBindingRepository());
    }

    public BddVerificationService(BddEvidenceAdapterRegistry adapterRegistry) {
        this(adapterRegistry, new BddBindingRepository());
    }

    public BddVerificationService(BddEvidenceAdapterRegistry adapterRegistry,
                                  BddBindingRepository bindingRepository) {
        this.adapterRegistry = adapterRegistry;
        this.bindingRepository = bindingRepository;
    }

    public BddVerificationResult evaluate(Connection connection, Project project, BddService bddService,
                                          String featureKey, String scenarioKey, String goalKey)
            throws SQLException {
        List<BddScenarioView> views = selectedScenarios(connection, project, bddService,
                featureKey, scenarioKey, goalKey);
        List<BddScenarioEvidenceResult> scenarioResults = new ArrayList<BddScenarioEvidenceResult>();
        int coveredCount = 0;
        int missingCount = 0;
        int pendingCount = 0;
        int failedCount = 0;
        for (BddScenarioView view : views) {
            List<BddEvidence> evidence = bddService.listEvidence(connection,
                    view.scenario().scenarioKey(), goalKey);
            BddScenarioEvidenceResult scenarioResult = evaluateScenario(view, evidence);
            scenarioResults.add(scenarioResult);
            if (scenarioResult.covered()) {
                coveredCount++;
            }
            if ("missing".equals(scenarioResult.status())) {
                missingCount++;
            } else if ("pending".equals(scenarioResult.status())) {
                pendingCount++;
            } else if ("failed".equals(scenarioResult.status())) {
                failedCount++;
            }
        }
        boolean passed = !views.isEmpty() && missingCount == 0 && pendingCount == 0 && failedCount == 0;
        return new BddVerificationResult(passed, views.size(), coveredCount,
                missingCount, pendingCount, failedCount, goalKey, scenarioResults);
    }

    private List<BddScenarioView> selectedScenarios(Connection connection, Project project, BddService bddService,
                                                    String featureKey, String scenarioKey, String goalKey)
            throws SQLException {
        List<BddScenarioView> views = new ArrayList<BddScenarioView>();
        if (scenarioKey != null && scenarioKey.length() > 0) {
            BddScenarioView view = bddService.findScenario(connection, project, scenarioKey);
            if (view != null) {
                views.add(view);
            }
            return views;
        }
        if ((featureKey == null || featureKey.length() == 0)
                && goalKey != null && goalKey.length() > 0) {
            List<BddBinding> bindings = bindingRepository.listByBinding(connection, "goal", goalKey);
            for (BddBinding binding : bindings) {
                BddScenarioView view = bddService.findScenario(connection, project, binding.scenarioKey());
                if (view != null && !"archived".equals(view.scenario().status())
                        && !"deprecated".equals(view.scenario().status())) {
                    views.add(view);
                }
            }
            return views;
        }
        List<BddScenario> scenarios = bddService.listScenarios(connection, project, featureKey);
        for (BddScenario scenario : scenarios) {
            if ("archived".equals(scenario.status()) || "deprecated".equals(scenario.status())) {
                continue;
            }
            BddScenarioView view = bddService.findScenario(connection, project, scenario.scenarioKey());
            if (view != null) {
                views.add(view);
            }
        }
        return views;
    }

    public BddScenarioEvidenceResult evaluateScenario(BddScenarioView view, List<BddEvidence> evidence) {
        if (evidence.isEmpty()) {
            return new BddScenarioEvidenceResult(view, evidence, null, "missing", false);
        }
        BddEvidence latest = evidence.get(0);
        BddEvidenceAdapter adapter = adapterRegistry.findAdapter(latest);
        if (adapter != null) {
            BddAdapterResult adapterResult = adapter.normalize(latest);
            return new BddScenarioEvidenceResult(view, evidence, adapterResult,
                    adapterResult.normalizedStatus(), adapterResult.covered());
        }
        if ("failed".equals(latest.status())) {
            return new BddScenarioEvidenceResult(view, evidence, null, "failed", false);
        }
        if ("pending".equals(latest.status())) {
            return new BddScenarioEvidenceResult(view, evidence, null, "pending", false);
        }
        if ("passed".equals(latest.status()) || "waived".equals(latest.status())
                || "skipped".equals(latest.status())) {
            return new BddScenarioEvidenceResult(view, evidence, null, "covered", true);
        }
        return new BddScenarioEvidenceResult(view, evidence, null, "missing", false);
    }

    public static final class BddVerificationResult {
        private final boolean passed;
        private final int scenarioCount;
        private final int coveredCount;
        private final int missingCount;
        private final int pendingCount;
        private final int failedCount;
        private final String goalKey;
        private final List<BddScenarioEvidenceResult> scenarios;

        public BddVerificationResult(boolean passed, int scenarioCount, int coveredCount,
                                     int missingCount, int pendingCount, int failedCount,
                                     String goalKey, List<BddScenarioEvidenceResult> scenarios) {
            this.passed = passed;
            this.scenarioCount = scenarioCount;
            this.coveredCount = coveredCount;
            this.missingCount = missingCount;
            this.pendingCount = pendingCount;
            this.failedCount = failedCount;
            this.goalKey = goalKey == null ? "" : goalKey;
            this.scenarios = Collections.unmodifiableList(scenarios);
        }

        public boolean passed() { return passed; }
        public int scenarioCount() { return scenarioCount; }
        public int coveredCount() { return coveredCount; }
        public int missingCount() { return missingCount; }
        public int pendingCount() { return pendingCount; }
        public int failedCount() { return failedCount; }
        public String goalKey() { return goalKey; }
        public List<BddScenarioEvidenceResult> scenarios() { return scenarios; }
    }

    public static final class BddScenarioEvidenceResult {
        private final BddScenarioView view;
        private final List<BddEvidence> evidence;
        private final BddAdapterResult adapterResult;
        private final String status;
        private final boolean covered;

        public BddScenarioEvidenceResult(BddScenarioView view, List<BddEvidence> evidence,
                                         String status, boolean covered) {
            this(view, evidence, null, status, covered);
        }

        public BddScenarioEvidenceResult(BddScenarioView view, List<BddEvidence> evidence,
                                         BddAdapterResult adapterResult, String status, boolean covered) {
            this.view = view;
            this.evidence = Collections.unmodifiableList(evidence);
            this.adapterResult = adapterResult;
            this.status = status;
            this.covered = covered;
        }

        public BddScenarioView view() { return view; }
        public List<BddEvidence> evidence() { return evidence; }
        public BddAdapterResult adapterResult() { return adapterResult; }
        public String status() { return status; }
        public boolean covered() { return covered; }
    }
}
