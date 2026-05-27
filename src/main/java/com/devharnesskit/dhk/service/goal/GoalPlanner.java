package com.devharnesskit.dhk.service.goal;

import com.devharnesskit.dhk.model.goal.GoalPlan;
import com.devharnesskit.dhk.model.goal.GoalProfile;
import com.devharnesskit.dhk.model.goal.GoalRun;

public final class GoalPlanner {
    private static final String[] FORBIDDEN = new String[]{
            "do_not_archive_spec",
            "do_not_waive_gates",
            "do_not_confirm_memory",
            "do_not_run_db_sql_without_user_request",
            "do_not_claim_completion_before_goal_evaluate"
    };

    public GoalPlan plan(GoalRun goal, GoalProfile profile) {
        if ("context_export_failed".equals(goal.status()) || "context_exporting".equals(goal.status())) {
            return new GoalPlan("recover_context_export",
                    "Goal context export is incomplete. Run goal resume before continuing.",
                    new String[0], FORBIDDEN,
                    "dhk goal resume --goal " + goal.goalKey());
        }
        String action = goal.currentAction();
        if (action.length() == 0 && profile.actions().length > 0) {
            action = profile.actions()[0];
        }
        return new GoalPlan(action, instruction(action), requiredEvidence(profile, action), FORBIDDEN,
                "dhk goal step --goal " + goal.goalKey()
                        + " --summary \"<summary>\" --evidence \"<evidence>\"");
    }

    public String nextAction(GoalProfile profile, String currentAction) {
        String[] actions = profile.actions();
        for (int i = 0; i < actions.length; i++) {
            if (actions[i].equals(currentAction)) {
                if (i + 1 < actions.length) {
                    return actions[i + 1];
                }
                return currentAction;
            }
        }
        return actions.length == 0 ? "" : actions[0];
    }

    public String statusForAction(String action) {
        if (action == null || action.length() == 0) {
            return "context_ready";
        }
        if (action.indexOf("inspect") >= 0 || action.indexOf("plan") >= 0
                || action.indexOf("collect") >= 0 || action.indexOf("review") >= 0
                || action.indexOf("hypotheses") >= 0 || action.indexOf("summarize") >= 0
                || action.indexOf("rollback") >= 0) {
            return "planning";
        }
        if (action.indexOf("implement") >= 0 || action.indexOf("apply") >= 0) {
            return "implementing";
        }
        if (action.indexOf("verify") >= 0 || action.indexOf("manual_evidence") >= 0) {
            return "verifying";
        }
        return "context_ready";
    }

    private String instruction(String action) {
        if ("inspect_existing_code".equals(action)) {
            return "Read existing Controller, Service, Mapper, DTO, and tests before editing. Do not edit code yet.";
        }
        if ("create_change_plan".equals(action)) {
            return "Create a minimal impacted-file plan with risks and verification commands.";
        }
        if ("implement_minimal_change".equals(action)) {
            return "Modify only the minimal files needed for the approved plan.";
        }
        if ("verify".equals(action)) {
            return "Run compile/test/sensitive checks or record why a check is not available.";
        }
        if ("understand_patch".equals(action)) {
            return "Understand the small change boundary and read only the minimum relevant files before editing.";
        }
        if ("apply_patch".equals(action)) {
            return "Apply the small scoped change and record objective diff evidence.";
        }
        if ("graph_index_or_refresh".equals(action)) {
            return "Generate a fresh graph snapshot and graph context before inspecting code.";
        }
        if ("graph_impact_analysis".equals(action)) {
            return "Generate an impact map for the task target and inspect related files before planning.";
        }
        if ("graph_reimpact".equals(action)) {
            return "Regenerate the impact map after implementation and confirm changed files remain covered.";
        }
        if ("create_rollback_plan".equals(action)) {
            return "Create a rollback plan artifact with exact revert steps and scope.";
        }
        if ("record_manual_evidence".equals(action)) {
            return "Record manual verification evidence for legacy behavior, including path and pass/fail status.";
        }
        if ("collect_error".equals(action)) {
            return "Collect concrete failing symptoms, logs, and reproduction notes.";
        }
        if ("list_hypotheses".equals(action)) {
            return "List root-cause hypotheses and cite evidence for each one.";
        }
        if ("implement_fix".equals(action)) {
            return "Apply the smallest fix that addresses the supported root cause.";
        }
        if ("verify_regression".equals(action)) {
            return "Run regression checks and record the result.";
        }
        if ("collect_sql".equals(action)) {
            return "Collect the SQL under review. Do not run DB SQL unless the user explicitly asked.";
        }
        if ("review_safety".equals(action)) {
            return "Review readonly safety, sensitive output risk, and forbidden SQL patterns.";
        }
        if ("review_index_and_pagination".equals(action)) {
            return "Review indexes, ordering, pagination, and result-size risk.";
        }
        if ("summarize_findings".equals(action)) {
            return "Summarize findings with clear evidence and remaining risks.";
        }
        if ("collect_diff".equals(action)) {
            return "Collect the diff or files under review before writing findings.";
        }
        if ("review_correctness".equals(action)) {
            return "Review behavioral correctness and regression risk.";
        }
        if ("review_tests".equals(action)) {
            return "Review test coverage and verification gaps.";
        }
        if ("identify_behavior_boundary".equals(action)) {
            return "Identify the behavior boundary that must remain unchanged.";
        }
        if ("create_refactor_plan".equals(action)) {
            return "Create a small refactor plan with rollback and verification.";
        }
        if ("apply_small_refactor".equals(action)) {
            return "Apply one bounded refactor without changing behavior.";
        }
        return "Perform the current goal action and record concrete evidence.";
    }

    private String[] requiredEvidence(GoalProfile profile, String action) {
        if (profile != null) {
            String[] configured = profile.requiredEvidence(action);
            if (configured.length > 0) {
                return configured;
            }
        }
        if ("inspect_existing_code".equals(action)) {
            return new String[]{"existing_controller", "existing_service", "existing_mapper", "existing_tests"};
        }
        if ("create_change_plan".equals(action)) {
            return new String[]{"impacted_files", "risk_points", "verification_plan"};
        }
        if ("implement_minimal_change".equals(action)) {
            return new String[]{"changed_files", "implementation_summary"};
        }
        if ("verify".equals(action)) {
            return new String[]{"compile_result", "test_result", "sensitive_result"};
        }
        if ("understand_patch".equals(action)) {
            return new String[]{"goal_understanding", "assumptions", "read_files"};
        }
        if ("apply_patch".equals(action)) {
            return new String[]{"changed_files", "diff_stat", "implementation_summary", "risk_flags"};
        }
        if ("collect_error".equals(action)) {
            return new String[]{"failing_symptom", "reproduction_or_log"};
        }
        if ("list_hypotheses".equals(action)) {
            return new String[]{"root_cause_hypothesis", "supporting_evidence"};
        }
        if ("implement_fix".equals(action)) {
            return new String[]{"changed_files", "fix_summary"};
        }
        if ("verify_regression".equals(action)) {
            return new String[]{"regression_check_result"};
        }
        if ("collect_diff".equals(action)) {
            return new String[]{"diff_or_file_list"};
        }
        return new String[]{"summary", "evidence"};
    }
}
