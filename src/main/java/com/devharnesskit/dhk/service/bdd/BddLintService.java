package com.devharnesskit.dhk.service.bdd;

import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.bdd.BddFeature;
import com.devharnesskit.dhk.model.bdd.BddBinding;
import com.devharnesskit.dhk.model.bdd.BddQualityIssue;
import com.devharnesskit.dhk.model.bdd.BddScenario;
import com.devharnesskit.dhk.model.bdd.BddScenarioView;
import com.devharnesskit.dhk.model.bdd.BddStep;
import com.devharnesskit.dhk.repository.bdd.BddQualityIssueRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class BddLintService {
    private final BddService bddService;
    private final BddQualityIssueRepository issueRepository;

    public BddLintService(BddService bddService, BddQualityIssueRepository issueRepository) {
        this.bddService = bddService;
        this.issueRepository = issueRepository;
    }

    public List<BddQualityIssue> lint(Connection connection, Project project,
                                      String featureFilter, String now) throws SQLException {
        issueRepository.deleteOpenByProject(connection, project.projectKey(), featureFilter);
        List<BddQualityIssue> issues = new ArrayList<BddQualityIssue>();
        List<BddFeature> features = selectedFeatures(connection, project, featureFilter);
        for (BddFeature feature : features) {
            lintFeature(connection, project, feature, now, issues);
        }
        for (BddQualityIssue issue : issues) {
            issueRepository.insert(connection, issue);
        }
        return issueRepository.listOpenByProject(connection, project.projectKey(), featureFilter);
    }

    private List<BddFeature> selectedFeatures(Connection connection, Project project, String featureFilter)
            throws SQLException {
        if (featureFilter != null && featureFilter.length() > 0) {
            List<BddFeature> result = new ArrayList<BddFeature>();
            BddFeature feature = bddService.findFeature(connection, project, featureFilter);
            if (feature != null) {
                result.add(feature);
            }
            return result;
        }
        return bddService.listFeatures(connection, project);
    }

    private void lintFeature(Connection connection, Project project, BddFeature feature, String now,
                             List<BddQualityIssue> issues) throws SQLException {
        List<BddScenario> scenarios = bddService.listScenarios(connection, project, feature.featureKey());
        Map<String, String> titleOwners = new HashMap<String, String>();
        for (BddScenario scenario : scenarios) {
            String titleKey = normalize(scenario.title());
            String existing = titleOwners.get(titleKey);
            if (existing != null) {
                issues.add(issue(project, feature.featureKey(), scenario.scenarioKey(),
                        "duplicate_scenario_title", "warning",
                        "Scenario title duplicates " + existing + ": " + scenario.title(), now));
            } else {
                titleOwners.put(titleKey, scenario.scenarioKey());
            }
            BddScenarioView view = bddService.findScenario(connection, project, scenario.scenarioKey());
            if (view != null) {
                lintScenario(project, feature, view, now, issues);
            }
        }
    }

    private void lintScenario(Project project, BddFeature feature, BddScenarioView view,
                              String now, List<BddQualityIssue> issues) {
        if (isVagueScenario(view.scenario().title(), view.scenario().description())) {
            issues.add(issue(project, feature.featureKey(), view.scenario().scenarioKey(),
                    "vague_scenario", "warning",
                    "Scenario title or description is too vague: " + view.scenario().title(), now));
        }
        Set<String> stepTypes = new HashSet<String>();
        for (BddStep step : view.steps()) {
            stepTypes.add(step.stepType());
            if (isVagueStep(step.stepText())) {
                issues.add(issue(project, feature.featureKey(), view.scenario().scenarioKey(),
                        "vague_step", "warning",
                        "Step is too vague: " + step.stepType() + " " + step.stepText(), now));
            }
        }
        addMissingStepIssue(project, feature, view, stepTypes, "given", now, issues);
        addMissingStepIssue(project, feature, view, stepTypes, "when", now, issues);
        addMissingStepIssue(project, feature, view, stepTypes, "then", now, issues);
        if (!hasAcceptanceBinding(view)) {
            issues.add(issue(project, feature.featureKey(), view.scenario().scenarioKey(),
                    "missing_acceptance_mapping", "warning",
                    "Scenario is not bound to a spec acceptance", now));
        }
    }

    private void addMissingStepIssue(Project project, BddFeature feature, BddScenarioView view,
                                     Set<String> stepTypes, String type, String now,
                                     List<BddQualityIssue> issues) {
        if (!stepTypes.contains(type)) {
            issues.add(issue(project, feature.featureKey(), view.scenario().scenarioKey(),
                    "missing_" + type, "error",
                    "Scenario is missing " + type + " step", now));
        }
    }

    private boolean isVagueStep(String text) {
        String normalized = normalize(text);
        return normalized.length() < 6
                || "系统正常".equals(normalized)
                || "用户操作".equals(normalized)
                || "准备数据".equals(normalized)
                || "正常".equals(normalized)
                || "正确".equals(normalized)
                || "处理成功".equals(normalized);
    }

    private boolean isVagueScenario(String title, String description) {
        String normalizedTitle = normalize(title);
        String normalizedDescription = normalize(description);
        return normalizedTitle.length() < 4
                || "测试场景".equals(normalizedTitle)
                || "正常场景".equals(normalizedTitle)
                || "功能正常".equals(normalizedTitle)
                || "验证通过".equals(normalizedTitle)
                || "场景".equals(normalizedTitle)
                || normalizedDescription.equals(normalizedTitle);
    }

    private boolean hasAcceptanceBinding(BddScenarioView view) {
        for (BddBinding binding : view.bindings()) {
            if ("spec_acceptance".equals(binding.bindingType())) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private BddQualityIssue issue(Project project, String featureKey, String scenarioKey,
                                  String issueType, String severity, String message, String now) {
        return new BddQualityIssue(0L, project.projectKey(), featureKey, scenarioKey,
                issueType, severity, "open", message, now);
    }
}
