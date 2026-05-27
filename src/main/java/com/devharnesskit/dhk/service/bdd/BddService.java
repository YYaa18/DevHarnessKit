package com.devharnesskit.dhk.service.bdd;

import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.bdd.BddBinding;
import com.devharnesskit.dhk.model.bdd.BddEvidence;
import com.devharnesskit.dhk.model.bdd.BddFeature;
import com.devharnesskit.dhk.model.bdd.BddScenario;
import com.devharnesskit.dhk.model.bdd.BddScenarioView;
import com.devharnesskit.dhk.model.bdd.BddStep;
import com.devharnesskit.dhk.repository.bdd.BddBindingRepository;
import com.devharnesskit.dhk.repository.bdd.BddEvidenceRepository;
import com.devharnesskit.dhk.repository.bdd.BddFeatureRepository;
import com.devharnesskit.dhk.repository.bdd.BddScenarioRepository;
import com.devharnesskit.dhk.repository.bdd.BddStepRepository;
import com.devharnesskit.dhk.util.PathUtil;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class BddService {
    private final BddFeatureRepository featureRepository;
    private final BddScenarioRepository scenarioRepository;
    private final BddStepRepository stepRepository;
    private final BddBindingRepository bindingRepository;
    private final BddEvidenceRepository evidenceRepository;

    public BddService(BddFeatureRepository featureRepository,
                      BddScenarioRepository scenarioRepository,
                      BddStepRepository stepRepository) {
        this(featureRepository, scenarioRepository, stepRepository, new BddBindingRepository());
    }

    public BddService(BddFeatureRepository featureRepository,
                      BddScenarioRepository scenarioRepository,
                      BddStepRepository stepRepository,
                      BddBindingRepository bindingRepository) {
        this(featureRepository, scenarioRepository, stepRepository, bindingRepository,
                new BddEvidenceRepository());
    }

    public BddService(BddFeatureRepository featureRepository,
                      BddScenarioRepository scenarioRepository,
                      BddStepRepository stepRepository,
                      BddBindingRepository bindingRepository,
                      BddEvidenceRepository evidenceRepository) {
        this.featureRepository = featureRepository;
        this.scenarioRepository = scenarioRepository;
        this.stepRepository = stepRepository;
        this.bindingRepository = bindingRepository;
        this.evidenceRepository = evidenceRepository;
    }

    public BddInitResult init(Path projectRoot) {
        PathUtil.createBddDirectories(projectRoot);
        return new BddInitResult(PathUtil.bddDirectory(projectRoot),
                PathUtil.bddFeaturesDirectory(projectRoot),
                PathUtil.bddEvidenceDirectory(projectRoot),
                PathUtil.bddExportsDirectory(projectRoot));
    }

    public BddScenarioView addScenario(Connection connection, Project project, BddAddRequest request,
                                       String now) throws SQLException {
        BddFeature feature = featureRepository.findByKey(connection, request.featureKey());
        if (feature == null) {
            feature = new BddFeature(request.featureKey(), project.projectKey(), request.moduleName(),
                    request.featureTitle(), request.featureDescription(), request.tags(),
                    "active", "manual", now, now);
            featureRepository.insert(connection, feature);
        } else if (!project.projectKey().equals(feature.projectKey())) {
            throw new SQLException("BDD feature belongs to another project: " + request.featureKey());
        }
        BddScenario existing = scenarioRepository.findByKey(connection, request.scenarioKey());
        if (existing != null) {
            throw new SQLException("BDD scenario already exists: " + request.scenarioKey());
        }
        String status = request.status();
        if (status.length() == 0) {
            status = request.hasCoreSteps() ? "active" : "draft";
        }
        BddScenario scenario = new BddScenario(request.scenarioKey(), feature.featureKey(), project.projectKey(),
                request.scenarioTitle(), request.scenarioDescription(), request.scenarioType(),
                request.priority(), status, request.tags(), now, now);
        scenarioRepository.insert(connection, scenario);
        List<BddStep> steps = new ArrayList<BddStep>();
        addStep(connection, steps, scenario.scenarioKey(), "given", request.given(), now);
        addStep(connection, steps, scenario.scenarioKey(), "when", request.when(), now);
        addStep(connection, steps, scenario.scenarioKey(), "then", request.then(), now);
        addStep(connection, steps, scenario.scenarioKey(), "and", request.andStep(), now);
        return new BddScenarioView(feature, scenario, steps);
    }

    public List<BddFeature> listFeatures(Connection connection, Project project) throws SQLException {
        return featureRepository.listByProject(connection, project.projectKey());
    }

    public List<BddScenario> listScenarios(Connection connection, Project project, String featureKey)
            throws SQLException {
        if (featureKey != null && featureKey.length() > 0) {
            BddFeature feature = featureRepository.findByKey(connection, featureKey);
            if (feature == null || !project.projectKey().equals(feature.projectKey())) {
                return new ArrayList<BddScenario>();
            }
            return scenarioRepository.listByFeature(connection, featureKey);
        }
        return scenarioRepository.listByProject(connection, project.projectKey());
    }

    public BddFeature findFeature(Connection connection, Project project, String featureKey) throws SQLException {
        BddFeature feature = featureRepository.findByKey(connection, featureKey);
        return feature != null && project.projectKey().equals(feature.projectKey()) ? feature : null;
    }

    public BddScenarioView findScenario(Connection connection, Project project, String scenarioKey)
            throws SQLException {
        BddScenario scenario = scenarioRepository.findByKey(connection, scenarioKey);
        if (scenario == null || !project.projectKey().equals(scenario.projectKey())) {
            return null;
        }
        BddFeature feature = featureRepository.findByKey(connection, scenario.featureKey());
        List<BddStep> steps = stepRepository.listByScenario(connection, scenario.scenarioKey());
        List<BddBinding> bindings = bindingRepository.listByScenario(connection, scenario.scenarioKey());
        List<BddEvidence> evidence = evidenceRepository.listByScenario(connection, scenario.scenarioKey());
        return new BddScenarioView(feature, scenario, steps, bindings, evidence);
    }

    public BddBinding bindScenario(Connection connection, Project project, String scenarioKey,
                                   String bindingType, String bindingKey, String relation,
                                   String metadata, String now) throws SQLException {
        BddScenario scenario = scenarioRepository.findByKey(connection, scenarioKey);
        if (scenario == null || !project.projectKey().equals(scenario.projectKey())) {
            throw new SQLException("BDD scenario not found: " + scenarioKey);
        }
        if (!isBindingTypeAllowed(bindingType)) {
            throw new SQLException("Invalid BDD binding type: " + bindingType);
        }
        if (bindingKey == null || bindingKey.trim().length() == 0) {
            throw new SQLException("Missing BDD binding key");
        }
        String selectedRelation = relation == null || relation.trim().length() == 0
                ? "relates_to" : relation.trim();
        if (!isKeyAllowed(selectedRelation)) {
            throw new SQLException("Invalid BDD binding relation: " + selectedRelation);
        }
        return bindingRepository.insertOrFind(connection, new BddBinding(0L, scenario.scenarioKey(),
                bindingType, bindingKey.trim(), selectedRelation, metadata == null ? "" : metadata, now));
    }

    public BddEvidence addEvidence(Connection connection, Project project, String scenarioKey,
                                   String goalKey, String evidenceType, String status,
                                   String evidencePath, String summary, String command,
                                   String now) throws SQLException {
        BddScenario scenario = scenarioRepository.findByKey(connection, scenarioKey);
        if (scenario == null || !project.projectKey().equals(scenario.projectKey())) {
            throw new SQLException("BDD scenario not found: " + scenarioKey);
        }
        if (!isEvidenceTypeAllowed(evidenceType)) {
            throw new SQLException("Invalid BDD evidence type: " + evidenceType);
        }
        if (!isEvidenceStatusAllowed(status)) {
            throw new SQLException("Invalid BDD evidence status: " + status);
        }
        return evidenceRepository.insert(connection, new BddEvidence(0L, scenario.scenarioKey(),
                goalKey == null ? "" : goalKey, evidenceType, status,
                evidencePath == null ? "" : evidencePath, summary == null ? "" : summary,
                command == null ? "" : command, now, now));
    }

    public List<BddEvidence> listEvidence(Connection connection, String scenarioKey, String goalKey)
            throws SQLException {
        return evidenceRepository.listByScenarioAndGoal(connection, scenarioKey, goalKey);
    }

    public static boolean isKeyAllowed(String key) {
        return key != null && key.matches("[A-Za-z0-9._-]+");
    }

    public static boolean isScenarioTypeAllowed(String type) {
        return "acceptance".equals(type) || "edge_case".equals(type) || "regression".equals(type)
                || "manual".equals(type) || "exploratory".equals(type);
    }

    public static boolean isPriorityAllowed(String priority) {
        return "low".equals(priority) || "normal".equals(priority)
                || "high".equals(priority) || "critical".equals(priority);
    }

    public static boolean isScenarioStatusAllowed(String status) {
        return "draft".equals(status) || "active".equals(status) || "implemented".equals(status)
                || "verified".equals(status) || "blocked".equals(status)
                || "deprecated".equals(status) || "archived".equals(status);
    }

    public static boolean isBindingTypeAllowed(String type) {
        return "spec_acceptance".equals(type) || "spec_task".equals(type) || "goal".equals(type)
                || "workflow".equals(type) || "graph".equals(type) || "file".equals(type)
                || "symbol".equals(type) || "sql_table".equals(type) || "test".equals(type);
    }

    public static boolean isEvidenceTypeAllowed(String type) {
        return "manual".equals(type) || "test".equals(type) || "review".equals(type)
                || "screenshot".equals(type) || "command".equals(type) || "artifact".equals(type);
    }

    public static boolean isEvidenceStatusAllowed(String status) {
        return "pending".equals(status) || "passed".equals(status) || "failed".equals(status)
                || "skipped".equals(status) || "waived".equals(status);
    }

    private void addStep(Connection connection, List<BddStep> steps, String scenarioKey, String type,
                         String text, String now) throws SQLException {
        if (text == null || text.trim().length() == 0) {
            return;
        }
        BddStep step = new BddStep(0L, scenarioKey, steps.size() + 1, type,
                text.trim(), normalize(text), now);
        long id = stepRepository.insert(connection, step);
        steps.add(new BddStep(id, scenarioKey, step.stepOrder(), type,
                step.stepText(), step.normalizedText(), now));
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    public static final class BddInitResult {
        private final Path bddDirectory;
        private final Path featuresDirectory;
        private final Path evidenceDirectory;
        private final Path exportsDirectory;

        public BddInitResult(Path bddDirectory, Path featuresDirectory,
                             Path evidenceDirectory, Path exportsDirectory) {
            this.bddDirectory = bddDirectory;
            this.featuresDirectory = featuresDirectory;
            this.evidenceDirectory = evidenceDirectory;
            this.exportsDirectory = exportsDirectory;
        }

        public Path bddDirectory() {
            return bddDirectory;
        }

        public Path featuresDirectory() {
            return featuresDirectory;
        }

        public Path evidenceDirectory() {
            return evidenceDirectory;
        }

        public Path exportsDirectory() {
            return exportsDirectory;
        }
    }

    public static final class BddAddRequest {
        private final String featureKey;
        private final String featureTitle;
        private final String featureDescription;
        private final String scenarioKey;
        private final String scenarioTitle;
        private final String scenarioDescription;
        private final String moduleName;
        private final String scenarioType;
        private final String priority;
        private final String status;
        private final String tags;
        private final String given;
        private final String when;
        private final String then;
        private final String andStep;

        public BddAddRequest(String featureKey, String featureTitle, String featureDescription,
                             String scenarioKey, String scenarioTitle, String scenarioDescription,
                             String moduleName, String scenarioType, String priority, String status,
                             String tags, String given, String when, String then, String andStep) {
            this.featureKey = featureKey;
            this.featureTitle = featureTitle;
            this.featureDescription = featureDescription;
            this.scenarioKey = scenarioKey;
            this.scenarioTitle = scenarioTitle;
            this.scenarioDescription = scenarioDescription;
            this.moduleName = moduleName;
            this.scenarioType = scenarioType;
            this.priority = priority;
            this.status = status;
            this.tags = tags;
            this.given = given;
            this.when = when;
            this.then = then;
            this.andStep = andStep;
        }

        public String featureKey() { return featureKey; }
        public String featureTitle() { return featureTitle; }
        public String featureDescription() { return featureDescription; }
        public String scenarioKey() { return scenarioKey; }
        public String scenarioTitle() { return scenarioTitle; }
        public String scenarioDescription() { return scenarioDescription; }
        public String moduleName() { return moduleName; }
        public String scenarioType() { return scenarioType; }
        public String priority() { return priority; }
        public String status() { return status; }
        public String tags() { return tags; }
        public String given() { return given; }
        public String when() { return when; }
        public String then() { return then; }
        public String andStep() { return andStep; }

        public boolean hasCoreSteps() {
            return given.trim().length() > 0 && when.trim().length() > 0 && then.trim().length() > 0;
        }
    }
}
