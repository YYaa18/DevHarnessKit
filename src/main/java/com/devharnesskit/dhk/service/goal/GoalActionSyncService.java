package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.Project;
import com.devharnesskit.dhk.model.goal.GoalAcceptanceMapping;
import com.devharnesskit.dhk.model.goal.GoalActionMapping;
import com.devharnesskit.dhk.model.goal.GoalCheck;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;
import com.devharnesskit.dhk.model.goal.GoalStep;
import com.devharnesskit.dhk.model.spec.SpecAcceptance;
import com.devharnesskit.dhk.model.spec.SpecChange;
import com.devharnesskit.dhk.model.spec.SpecEvent;
import com.devharnesskit.dhk.model.spec.SpecTask;
import com.devharnesskit.dhk.repository.goal.GoalCheckRepository;
import com.devharnesskit.dhk.repository.goal.GoalStepRepository;
import com.devharnesskit.dhk.repository.spec.SpecAcceptanceRepository;
import com.devharnesskit.dhk.repository.spec.SpecChangeRepository;
import com.devharnesskit.dhk.repository.spec.SpecEventRepository;
import com.devharnesskit.dhk.repository.spec.SpecTaskRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowEventRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseRunRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowRunRepository;
import com.devharnesskit.dhk.service.spec.SpecAcceptanceService;
import com.devharnesskit.dhk.service.spec.SpecTaskService;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GoalActionSyncService {
    private static final String GOAL_ACCEPTANCE = "goal_checks_pass";

    private final GoalStepRepository stepRepository;
    private final GoalCheckRepository checkRepository;
    private final WorkflowRunRepository workflowRunRepository;
    private final WorkflowPhaseRunRepository phaseRunRepository;
    private final WorkflowGateRunRepository gateRunRepository;
    private final WorkflowEventRepository workflowEventRepository;
    private final SpecChangeRepository specChangeRepository;
    private final SpecTaskRepository taskRepository;
    private final SpecAcceptanceRepository acceptanceRepository;
    private final SpecEventRepository specEventRepository;
    private final SpecTaskService taskService;
    private final SpecAcceptanceService acceptanceService;
    private final GoalWorkflowSyncSupport workflowSyncSupport;
    private final GoalSpecSyncSupport specSyncSupport;

    public GoalActionSyncService() {
        this(new GoalStepRepository(), new GoalCheckRepository(), new WorkflowRunRepository(),
                new WorkflowPhaseRunRepository(), new WorkflowGateRunRepository(), new WorkflowEventRepository(),
                new SpecChangeRepository(), new SpecTaskRepository(), new SpecAcceptanceRepository(),
                new SpecEventRepository());
    }

    GoalActionSyncService(GoalStepRepository stepRepository, GoalCheckRepository checkRepository,
                          WorkflowRunRepository workflowRunRepository,
                          WorkflowPhaseRunRepository phaseRunRepository,
                          WorkflowGateRunRepository gateRunRepository,
                          WorkflowEventRepository workflowEventRepository,
                          SpecChangeRepository specChangeRepository,
                          SpecTaskRepository taskRepository,
                          SpecAcceptanceRepository acceptanceRepository,
                          SpecEventRepository specEventRepository) {
        this.stepRepository = stepRepository;
        this.checkRepository = checkRepository;
        this.workflowRunRepository = workflowRunRepository;
        this.phaseRunRepository = phaseRunRepository;
        this.gateRunRepository = gateRunRepository;
        this.workflowEventRepository = workflowEventRepository;
        this.specChangeRepository = specChangeRepository;
        this.taskRepository = taskRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.specEventRepository = specEventRepository;
        this.taskService = new SpecTaskService(taskRepository, specEventRepository);
        this.acceptanceService = new SpecAcceptanceService(acceptanceRepository, specEventRepository);
        this.workflowSyncSupport = new GoalWorkflowSyncSupport(workflowRunRepository, phaseRunRepository,
                gateRunRepository, workflowEventRepository);
        this.specSyncSupport = new GoalSpecSyncSupport(specChangeRepository, taskRepository, acceptanceRepository,
                specEventRepository, taskService, acceptanceService);
    }

    public void syncAfterContextExport(Connection connection, Path projectRoot, Project project,
                                       GoalRun goal, GoalProfile profile, String now) throws Exception {
        if (goal.workflowRunKey().length() == 0) {
            return;
        }
        workflowSyncSupport.passContextExport(connection, projectRoot, project, goal, now);
        specSyncSupport.ensureScaffold(connection, project, goal, profile, now);
        workflowSyncSupport.refreshProgress(connection, goal.workflowRunKey(), now);
    }

    public void syncAfterStep(Connection connection, Path projectRoot, Project project, GoalRun goal,
                              GoalProfile profile, GoalStep step, String now) throws Exception {
        syncRecordedSteps(connection, projectRoot, project, goal, profile, now);
        specSyncSupport.syncTaskForStep(connection, project, goal, profile, step, now);
    }

    public void syncBeforeCheck(Connection connection, Path projectRoot, Project project,
                                GoalRun goal, GoalProfile profile, GoalCheckPolicy policy,
                                String checkKey, List<GoalCheck> currentRunChecks,
                                String now) throws Exception {
        syncRecordedSteps(connection, projectRoot, project, goal, profile, now);
        if ("workflow".equals(checkKey)) {
            syncWorkflowVerification(connection, project, goal, profile, policy, currentRunChecks, now);
        }
        if ("spec".equals(checkKey)) {
            syncSpecAcceptance(connection, project, goal, profile, policy, currentRunChecks, now);
        }
    }

    public void syncBeforeEvaluate(Connection connection, Path projectRoot, Project project,
                                   GoalRun goal, GoalProfile profile, GoalCheckPolicy policy,
                                   String now) throws Exception {
        List<GoalCheck> checks = checkRepository.listByGoal(connection, goal.goalKey());
        syncRecordedSteps(connection, projectRoot, project, goal, profile, now);
        syncWorkflowVerification(connection, project, goal, profile, policy, checks, now);
        syncSpecAcceptance(connection, project, goal, profile, policy, checks, now);
    }

    public void syncOnComplete(Connection connection, Project project, GoalRun goal, GoalProfile profile,
                               GoalCheckPolicy policy, List<GoalCheck> checks, long checkpointId,
                               String now) throws Exception {
        workflowSyncSupport.passCheckpoint(connection, project, goal, profile, checkpointId, now);
        workflowSyncSupport.passOptionalFinalWorkflowPhases(connection, project, goal, profile, now);
        if (!workflowSyncSupport.completeUnmappedWorkflow(connection, project, goal, profile, checkpointId, now)) {
            workflowSyncSupport.refreshProgress(connection, goal.workflowRunKey(), now);
        }
        specSyncSupport.verifyChange(connection, goal, profile, now);
    }

    private void syncRecordedSteps(Connection connection, Path projectRoot, Project project,
                                   GoalRun goal, GoalProfile profile, String now) throws Exception {
        if (goal.workflowRunKey().length() > 0) {
            workflowSyncSupport.passContextExport(connection, projectRoot, project, goal, now);
        }
        specSyncSupport.ensureScaffold(connection, project, goal, profile, now);
        List<GoalStep> steps = stepRepository.listByGoal(connection, goal.goalKey());
        for (GoalStep step : steps) {
            syncWorkflowForStep(connection, project, goal, profile, step, now);
            specSyncSupport.syncTaskForStep(connection, project, goal, profile, step, now);
        }
        if (hasImplementationStep(steps)) {
            workflowSyncSupport.passUserApproval(connection, project, goal, profile, now);
        }
        workflowSyncSupport.refreshProgress(connection, goal.workflowRunKey(), now);
    }

    private void syncWorkflowForStep(Connection connection, Project project, GoalRun goal,
                                     GoalProfile profile, GoalStep step, String now) throws Exception {
        if (goal.workflowRunKey().length() == 0 || profile == null) {
            return;
        }
        GoalActionMapping mapping = profile.actionMapping(step.actionKey());
        if (mapping == null) {
            return;
        }
        if (isImplementationAction(step, mapping)) {
            workflowSyncSupport.passUserApproval(connection, project, goal, profile, now);
        }
        if (GoalActionMapping.MODE_STEP.equals(mapping.gatePassMode())) {
            for (String gateKey : mapping.requiredGates()) {
                workflowSyncSupport.passGate(connection, project, goal, gateKey, "Passed by goal action evidence",
                        "goal_step=" + step.id() + " action=" + step.actionKey(), now);
            }
        }
        if (mapping.workflowPhase().length() > 0
                && GoalActionMapping.MODE_STEP.equals(mapping.phasePassMode())) {
            workflowSyncSupport.passPhase(connection, project, goal, mapping.workflowPhase(), "Completed by goal action",
                    "goal_step=" + step.id() + " action=" + step.actionKey(), now, profile);
        }
    }

    private void syncWorkflowVerification(Connection connection, Project project, GoalRun goal,
                                          GoalProfile profile, GoalCheckPolicy policy,
                                          List<GoalCheck> currentRunChecks, String now) throws Exception {
        if (goal.workflowRunKey().length() == 0) {
            return;
        }
        Map<String, GoalCheck> checks = checksByKey(connection, goal.goalKey(), currentRunChecks);
        GoalCheck compile = acceptedCheck("compile", checks, goal, policy, profile);
        if (compile != null) {
            workflowSyncSupport.passPhase(connection, project, goal, "verify_compile", "Compile check passed",
                    checkEvidence(compile), now, profile);
        }
        if (profile != null) {
            for (GoalActionMapping mapping : profile.actionMappings().values()) {
                if (!mappingChecksAccepted(mapping, checks, goal, policy, profile)) {
                    continue;
                }
                if (GoalActionMapping.MODE_CHECK.equals(mapping.gatePassMode())) {
                    for (String gateKey : mapping.requiredGates()) {
                        workflowSyncSupport.passGate(connection, project, goal, gateKey, "Goal checks accepted",
                                checksEvidence(mapping, checks, goal, policy, profile), now);
                    }
                }
                if (mapping.workflowPhase().length() > 0
                        && GoalActionMapping.MODE_CHECK.equals(mapping.phasePassMode())) {
                    workflowSyncSupport.passPhase(connection, project, goal, mapping.workflowPhase(), "Goal checks accepted",
                            checksEvidence(mapping, checks, goal, policy, profile), now, profile);
                }
            }
        }
        workflowSyncSupport.refreshProgress(connection, goal.workflowRunKey(), now);
    }

    private void syncSpecAcceptance(Connection connection, Project project, GoalRun goal,
                                    GoalProfile profile, GoalCheckPolicy policy,
                                    List<GoalCheck> currentRunChecks, String now) throws Exception {
        SpecChange change = specSyncSupport.specChange(connection, goal);
        if (change == null || profile == null || !specSyncSupport.hasManagedAcceptance(profile)) {
            return;
        }
        Map<String, GoalCheck> checks = checksByKey(connection, goal.goalKey(), currentRunChecks);
        List<GoalStep> steps = stepRepository.listByGoal(connection, goal.goalKey());
        if (!profile.acceptanceMappings().isEmpty()) {
            for (GoalAcceptanceMapping mapping : profile.acceptanceMappings().values()) {
                syncMappedAcceptance(connection, change, mapping, checks, steps, goal, policy, profile, now);
            }
            if (profile.acceptanceMapping(GOAL_ACCEPTANCE) != null) {
                return;
            }
        }
        if (specSyncSupport.hasAutoAcceptance(profile)) {
            syncLegacyAutoAcceptance(connection, change, goal, profile, policy, checks, now);
        }
    }

    private void syncMappedAcceptance(Connection connection, SpecChange change, GoalAcceptanceMapping mapping,
                                      Map<String, GoalCheck> checks, List<GoalStep> steps, GoalRun goal,
                                      GoalCheckPolicy policy, GoalProfile profile, String now) throws Exception {
        SpecAcceptance acceptance = acceptanceRepository.findByKey(connection, change.changeKey(),
                mapping.acceptanceKey());
        if (acceptance == null || "passed".equals(acceptance.status()) || "waived".equals(acceptance.status())) {
            return;
        }
        String evidence = acceptanceEvidence(mapping, checks, steps, goal, policy, profile);
        if (evidence.length() == 0) {
            return;
        }
        acceptanceService.updateAcceptance(connection, change, acceptance, "passed", evidence, now);
    }

    private String acceptanceEvidence(GoalAcceptanceMapping mapping, Map<String, GoalCheck> checks,
                                      List<GoalStep> steps, GoalRun goal, GoalCheckPolicy policy,
                                      GoalProfile profile) {
        if (GoalAcceptanceMapping.SOURCE_MANUAL.equals(mapping.source())) {
            return "";
        }
        if (GoalAcceptanceMapping.SOURCE_EVIDENCE.equals(mapping.source())) {
            return evidenceKeyMatched(mapping, steps);
        }
        String[] required = acceptanceRequiredChecks(mapping, policy, profile);
        if (required.length == 0) {
            return "";
        }
        for (String checkKey : required) {
            GoalCheck check = acceptedCheck(checkKey, checks, goal, policy, profile);
            if (check == null) {
                return "";
            }
        }
        return checksEvidence(required, checks, goal, policy, profile);
    }

    private String evidenceKeyMatched(GoalAcceptanceMapping mapping, List<GoalStep> steps) {
        if (mapping.evidenceKey().length() == 0) {
            return "";
        }
        for (GoalStep step : steps) {
            if (containsEvidenceKey(step.evidence(), mapping.evidenceKey())) {
                return "goal_step=" + step.id() + " evidence_key=" + mapping.evidenceKey();
            }
        }
        return "";
    }

    private boolean containsEvidenceKey(String evidence, String key) {
        if (evidence == null || key == null || key.length() == 0) {
            return false;
        }
        return evidence.contains(key + "=") || evidence.contains(key + ":") || evidence.contains(key);
    }

    private String[] acceptanceRequiredChecks(GoalAcceptanceMapping mapping, GoalCheckPolicy policy,
                                              GoalProfile profile) {
        if (GOAL_ACCEPTANCE.equals(mapping.acceptanceKey())
                && GoalAcceptanceMapping.SOURCE_CHECKS.equals(mapping.source())) {
            return policyRequiredAcceptanceChecks(policy, profile);
        }
        if (GoalAcceptanceMapping.SOURCE_TEST.equals(mapping.source()) && mapping.requiredChecks().length == 0) {
            return new String[]{"test"};
        }
        if (GoalAcceptanceMapping.SOURCE_CHECKS.equals(mapping.source())
                || GoalAcceptanceMapping.SOURCE_TEST.equals(mapping.source())) {
            return mapping.requiredChecks();
        }
        return new String[0];
    }

    private String[] policyRequiredAcceptanceChecks(GoalCheckPolicy policy, GoalProfile profile) {
        Set<String> checks = new LinkedHashSet<String>();
        for (String checkKey : policy.requiredChecks(profile)) {
            addAcceptanceCheck(checks, checkKey);
        }
        return checks.toArray(new String[checks.size()]);
    }

    private void syncLegacyAutoAcceptance(Connection connection, SpecChange change, GoalRun goal,
                                          GoalProfile profile, GoalCheckPolicy policy,
                                          Map<String, GoalCheck> checks, String now) throws Exception {
        String[] required = acceptanceChecks(profile, policy);
        List<String> missing = new ArrayList<String>();
        for (String checkKey : required) {
            if ("spec".equals(checkKey)) {
                continue;
            }
            GoalCheck check = checks.get(checkKey);
            if (!isFreshAccepted(check, goal, policy, profile)) {
                missing.add(checkKey);
            }
        }
        if (!missing.isEmpty()) {
            return;
        }
        SpecAcceptance acceptance = acceptanceRepository.findByKey(connection, change.changeKey(), GOAL_ACCEPTANCE);
        if (acceptance == null || "passed".equals(acceptance.status()) || "waived".equals(acceptance.status())) {
            return;
        }
        acceptanceService.updateAcceptance(connection, change, acceptance, "passed",
                "All non-spec goal checks accepted at step " + goal.stepCount(), now);
    }

    private boolean mappingChecksAccepted(GoalActionMapping mapping, Map<String, GoalCheck> checks,
                                          GoalRun goal, GoalCheckPolicy policy, GoalProfile profile) {
        String[] required = mappingRequiredChecks(mapping);
        if (required.length == 0) {
            return false;
        }
        for (String checkKey : required) {
            GoalCheck check = acceptedCheck(checkKey, checks, goal, policy, profile);
            if (check == null) {
                return false;
            }
        }
        return true;
    }

    private String[] mappingRequiredChecks(GoalActionMapping mapping) {
        if (mapping.requiredChecks().length > 0) {
            return mapping.requiredChecks();
        }
        Set<String> checks = new LinkedHashSet<String>();
        if ("verify_compile".equals(mapping.workflowPhase())) {
            checks.add("compile");
        }
        if ("verify_tests".equals(mapping.workflowPhase()) || "verify_view_flow".equals(mapping.workflowPhase())
                || mapping.actionKey().indexOf("verify") >= 0) {
            checks.add("test");
        }
        for (String gate : mapping.requiredGates()) {
            if ("tests_recorded".equals(gate)) {
                checks.add("test");
            }
        }
        return checks.toArray(new String[checks.size()]);
    }

    private String checksEvidence(GoalActionMapping mapping, Map<String, GoalCheck> checks,
                                  GoalRun goal, GoalCheckPolicy policy, GoalProfile profile) {
        String[] required = mappingRequiredChecks(mapping);
        return checksEvidence(required, checks, goal, policy, profile);
    }

    private String checksEvidence(String[] required, Map<String, GoalCheck> checks,
                                  GoalRun goal, GoalCheckPolicy policy, GoalProfile profile) {
        StringBuilder builder = new StringBuilder();
        for (String checkKey : required) {
            GoalCheck check = acceptedCheck(checkKey, checks, goal, policy, profile);
            if (check == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(checkEvidence(check));
        }
        return builder.length() == 0 ? "goal_checks" : builder.toString();
    }

    private GoalCheck acceptedCheck(String checkKey, Map<String, GoalCheck> checks,
                                    GoalRun goal, GoalCheckPolicy policy, GoalProfile profile) {
        GoalCheck direct = checks.get(checkKey);
        if (isFreshAccepted(direct, goal, policy, profile)) {
            return direct;
        }
        String effective = effectiveCheckKey(checkKey, policy);
        if (!effective.equals(checkKey)) {
            GoalCheck mapped = checks.get(effective);
            if (isFreshAccepted(mapped, goal, policy, profile)) {
                return mapped;
            }
        }
        return null;
    }

    private String effectiveCheckKey(String checkKey, GoalCheckPolicy policy) {
        if ("compile".equals(checkKey)) {
            return verificationModeCheck("compile", policy.compileMode());
        }
        if ("test".equals(checkKey)) {
            return verificationModeCheck("test", policy.testMode());
        }
        return checkKey;
    }

    private String verificationModeCheck(String baseCheck, String mode) {
        if ("manual".equals(mode)) {
            return "manual-" + baseCheck;
        }
        if ("disabled".equals(mode)) {
            return "verification-risk";
        }
        return baseCheck;
    }

    private String[] acceptanceChecks(GoalProfile profile, GoalCheckPolicy policy) {
        Set<String> checks = new LinkedHashSet<String>();
        if (profile != null) {
            for (GoalActionMapping mapping : profile.actionMappings().values()) {
                if (!GoalActionMapping.ACCEPTANCE_CHECKS.equals(mapping.acceptanceSource())
                        && !"auto_pass".equals(mapping.specAcceptanceUpdate())) {
                    continue;
                }
                for (String checkKey : mapping.requiredChecks()) {
                    addAcceptanceCheck(checks, checkKey);
                }
            }
        }
        if (checks.isEmpty()) {
            for (String checkKey : policy.requiredChecks(profile)) {
                addAcceptanceCheck(checks, checkKey);
            }
        }
        return checks.toArray(new String[checks.size()]);
    }

    private void addAcceptanceCheck(Set<String> checks, String checkKey) {
        if (checkKey == null || checkKey.length() == 0 || "spec".equals(checkKey)) {
            return;
        }
        checks.add(checkKey);
    }

    private Map<String, GoalCheck> checksByKey(Connection connection, String goalKey,
                                               List<GoalCheck> currentRunChecks) throws Exception {
        Map<String, GoalCheck> result = new LinkedHashMap<String, GoalCheck>();
        for (GoalCheck check : checkRepository.listByGoal(connection, goalKey)) {
            result.put(check.checkKey(), check);
        }
        if (currentRunChecks != null) {
            for (GoalCheck check : currentRunChecks) {
                result.put(check.checkKey(), check);
            }
        }
        return result;
    }

    private boolean isFreshAccepted(GoalCheck check, GoalRun goal, GoalCheckPolicy policy, GoalProfile profile) {
        return check != null
                && check.stepCountAtCheck() >= goal.stepCount()
                && policy.accepts(check.checkKey(), check.status(), profile);
    }

    private boolean hasImplementationStep(List<GoalStep> steps) {
        for (GoalStep step : steps) {
            if (step.actionKey().indexOf("implement") >= 0 || step.actionKey().indexOf("apply") >= 0) {
                return true;
            }
        }
        return false;
    }

    private boolean isImplementationAction(GoalStep step, GoalActionMapping mapping) {
        return step.actionKey().indexOf("implement") >= 0 || step.actionKey().indexOf("apply") >= 0
                || mapping.workflowPhase().indexOf("implement") >= 0 || mapping.workflowPhase().indexOf("apply") >= 0;
    }

    private String checkEvidence(GoalCheck check) {
        if (check == null) {
            return "";
        }
        if (check.evidencePath().length() > 0) {
            return check.checkKey() + " evidence: " + check.evidencePath();
        }
        return check.checkKey() + " status=" + check.status();
    }

}
