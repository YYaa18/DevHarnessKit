package com.devharnesskit.dhk.service.workflow;

import com.devharnesskit.dhk.model.workflow.WorkflowGateTemplate;
import com.devharnesskit.dhk.model.workflow.WorkflowPhaseTemplate;
import com.devharnesskit.dhk.model.workflow.WorkflowTemplate;
import com.devharnesskit.dhk.repository.workflow.WorkflowGateTemplateRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowPhaseTemplateRepository;
import com.devharnesskit.dhk.repository.workflow.WorkflowTemplateRepository;

import java.sql.Connection;
import java.sql.SQLException;

public final class WorkflowSeedService {
    private final WorkflowTemplateRepository templateRepository;
    private final WorkflowPhaseTemplateRepository phaseRepository;
    private final WorkflowGateTemplateRepository gateRepository;

    public WorkflowSeedService(WorkflowTemplateRepository templateRepository,
                               WorkflowPhaseTemplateRepository phaseRepository,
                               WorkflowGateTemplateRepository gateRepository) {
        this.templateRepository = templateRepository;
        this.phaseRepository = phaseRepository;
        this.gateRepository = gateRepository;
    }

    public SeedResult seed(Connection connection, String now) throws SQLException {
        int insertedTemplates = 0;
        int insertedPhases = 0;
        int insertedGates = 0;
        TemplateSpec[] specs = specs();
        for (TemplateSpec spec : specs) {
            if (templateRepository.upsert(connection, new WorkflowTemplate(spec.key, spec.name, spec.description,
                    spec.category, spec.mode, "active", 1, "seed", "builtin:v0.2", now, now))) {
                insertedTemplates++;
            }
            for (int i = 0; i < spec.phases.length; i++) {
                PhaseSpec phase = spec.phases[i];
                if (phaseRepository.upsert(connection, new WorkflowPhaseTemplate(0L, spec.key, phase.key,
                        title(phase.key), i + 1, phase.type, 1, phase.instruction, phase.expectedOutput,
                        "none", now, now))) {
                    insertedPhases++;
                }
            }
            for (GateSpec gate : spec.gates) {
                if (gateRepository.upsert(connection, new WorkflowGateTemplate(0L, spec.key, gate.phaseKey,
                        gate.key, title(gate.key), gate.type, gate.severity, gate.ruleText, "",
                        gate.expectedResult, now, now))) {
                    insertedGates++;
                }
            }
        }
        return new SeedResult(specs.length, totalPhases(specs), totalGates(specs),
                insertedTemplates, insertedPhases, insertedGates);
    }

    private int totalPhases(TemplateSpec[] specs) {
        int total = 0;
        for (TemplateSpec spec : specs) {
            total += spec.phases.length;
        }
        return total;
    }

    private int totalGates(TemplateSpec[] specs) {
        int total = 0;
        for (TemplateSpec spec : specs) {
            total += spec.gates.length;
        }
        return total;
    }

    private TemplateSpec[] specs() {
        return new TemplateSpec[]{
                template("api-change", "API Change", "development", "api",
                        "Plan, implement, and verify a backend API change.",
                        phases(new String[][]{
                                {"export_context", "context", "Export current memory context.", "CURRENT_CONTEXT.md is available."},
                                {"inspect_existing_code", "plan", "Read existing endpoint/service/data-access code before editing.", "Relevant files inspected."},
                                {"create_change_plan", "plan", "Create a minimal change plan with impacted files and verification.", "Plan names files, risks, and checks."},
                                {"user_approval", "approval", "Wait for explicit approval when the change is materially branching.", "Approval or non-branching rationale recorded."},
                                {"implement_minimal_change", "implementation", "Apply the smallest coherent implementation.", "Patch is complete."},
                                {"verify_compile", "verification", "Run compile or equivalent build check.", "Compile evidence recorded."},
                                {"verify_tests", "verification", "Run focused tests or record why none apply.", "Test evidence recorded."},
                                {"create_checkpoint", "checkpoint", "Persist a task checkpoint.", "Checkpoint id recorded."},
                                {"suggest_memory_updates", "memory", "Suggest durable memory updates as draft only.", "Memory suggestions recorded."}
                        }),
                        gates(new String[][]{
                                {"export_context", "current_context_exists", "hard", "file_exists", "CURRENT_CONTEXT.md must be available.", "Context exported."},
                                {"export_context", "confirmed_memory_only", "hard", "export_check", "Only confirmed memory may be treated as fact.", "Draft memory excluded."},
                                {"create_change_plan", "impacted_files_listed", "hard", "manual", "Plan must list impacted files.", "Impacted files listed."},
                                {"create_change_plan", "verification_plan_ready", "hard", "manual", "Plan must include verification.", "Verification plan ready."},
                                {"user_approval", "user_approval_before_implementation", "hard", "manual", "Branching implementation requires approval.", "Approval recorded."},
                                {"verify_tests", "tests_recorded", "soft", "test_result", "Test result should be recorded.", "Tests recorded."},
                                {"create_checkpoint", "checkpoint_created", "hard", "manual", "A checkpoint must be created.", "Checkpoint recorded."},
                                {"suggest_memory_updates", "memory_suggestions_recorded", "soft", "memory_query", "Reusable facts should be suggested as draft memory.", "Suggestions recorded."}
                        })),
                template("mvc-change", "MVC Change", "development", "mvc",
                        "Plan, implement, and verify a server-rendered MVC change.",
                        phases(new String[][]{
                                {"export_context", "context", "Export current memory context.", "CURRENT_CONTEXT.md is available."},
                                {"inspect_existing_code", "plan", "Inspect controllers, views, model attributes, and validation.", "MVC flow inspected."},
                                {"create_change_plan", "plan", "Create a minimal MVC change plan.", "Plan recorded."},
                                {"user_approval", "approval", "Wait for approval when needed.", "Approval or rationale recorded."},
                                {"implement_minimal_change", "implementation", "Apply the smallest coherent MVC implementation.", "Patch is complete."},
                                {"verify_compile", "verification", "Run compile.", "Compile evidence recorded."},
                                {"verify_view_flow", "verification", "Verify view names, model fields, and validation.", "View flow evidence recorded."},
                                {"create_checkpoint", "checkpoint", "Persist a checkpoint.", "Checkpoint recorded."},
                                {"suggest_memory_updates", "memory", "Suggest durable memory updates.", "Suggestions recorded."}
                        }),
                        gates(new String[][]{
                                {"inspect_existing_code", "mvc_confirmed", "hard", "manual", "Confirm this is MVC flow.", "MVC flow confirmed."},
                                {"verify_view_flow", "view_name_checked", "hard", "manual", "View name must be checked.", "View checked."},
                                {"verify_view_flow", "model_fields_checked", "hard", "manual", "Model fields must be checked.", "Model fields checked."},
                                {"verify_view_flow", "form_validation_checked", "hard", "manual", "Form validation must be checked.", "Validation checked."},
                                {"create_checkpoint", "checkpoint_created", "hard", "manual", "A checkpoint must be created.", "Checkpoint recorded."}
                        })),
                template("systematic-debugging", "Systematic Debugging", "debug", "debug",
                        "Debug by evidence, hypothesis, minimal fix, and regression verification.",
                        phases(new String[][]{
                                {"export_context", "context", "Export current memory context.", "CURRENT_CONTEXT.md is available."},
                                {"collect_error", "plan", "Collect concrete error evidence.", "Error evidence recorded."},
                                {"identify_first_business_stack", "plan", "Find the first business stack frame.", "Business stack frame identified."},
                                {"list_hypotheses", "plan", "List hypotheses with evidence.", "Hypotheses listed."},
                                {"verify_hypothesis", "verification", "Verify one hypothesis before changing code.", "Hypothesis verified."},
                                {"minimal_fix_plan", "plan", "Prepare minimal fix plan.", "Fix plan recorded."},
                                {"implement_fix", "implementation", "Implement the minimal fix.", "Patch is complete."},
                                {"verify_regression", "verification", "Run regression verification.", "Regression result recorded."},
                                {"create_checkpoint", "checkpoint", "Persist a checkpoint.", "Checkpoint recorded."}
                        }),
                        gates(new String[][]{
                                {"collect_error", "error_evidence_collected", "hard", "manual", "Error evidence must be collected.", "Evidence collected."},
                                {"identify_first_business_stack", "first_business_stack_identified", "hard", "manual", "First business stack frame must be identified.", "Frame identified."},
                                {"list_hypotheses", "hypothesis_has_evidence", "hard", "manual", "Hypothesis must cite evidence.", "Evidence linked."},
                                {"verify_regression", "fix_has_verification", "hard", "test_result", "Fix must have verification.", "Verification recorded."}
                        })),
                template("safe-refactor", "Safe Refactor", "refactor", "auto",
                        "Refactor while preserving behavior with explicit rollback and verification.",
                        phases(new String[][]{
                                {"export_context", "context", "Export current memory context.", "CURRENT_CONTEXT.md is available."},
                                {"identify_behavior_boundary", "plan", "Identify behavior boundary.", "Boundary recorded."},
                                {"create_refactor_plan", "plan", "Create a small refactor plan.", "Plan recorded."},
                                {"user_approval", "approval", "Wait for approval if scope branches.", "Approval or rationale recorded."},
                                {"apply_small_refactor", "implementation", "Apply one bounded refactor.", "Patch is complete."},
                                {"verify_compile", "verification", "Run compile.", "Compile evidence recorded."},
                                {"verify_tests", "verification", "Run tests.", "Test evidence recorded."},
                                {"create_checkpoint", "checkpoint", "Persist a checkpoint.", "Checkpoint recorded."}
                        }),
                        gates(new String[][]{
                                {"identify_behavior_boundary", "behavior_preservation_stated", "hard", "manual", "State preserved behavior.", "Behavior boundary stated."},
                                {"create_refactor_plan", "rollback_plan_ready", "hard", "manual", "Rollback plan must exist.", "Rollback plan ready."},
                                {"apply_small_refactor", "single_boundary_change", "hard", "manual", "Only one boundary should change.", "Boundary respected."},
                                {"verify_tests", "verification_recorded", "hard", "test_result", "Verification must be recorded.", "Verification recorded."}
                        })),
                template("sql-review", "SQL Review", "sql", "sql",
                        "Review SQL for safety, indexes, pagination, and optional readonly checks.",
                        phases(new String[][]{
                                {"export_context", "context", "Export current memory context.", "CURRENT_CONTEXT.md is available."},
                                {"collect_sql", "db", "Collect SQL under review.", "SQL collected."},
                                {"review_safety", "review", "Review readonly and safety constraints.", "Safety findings recorded."},
                                {"review_index_and_pagination", "review", "Review indexes, ordering, and pagination.", "Performance findings recorded."},
                                {"optional_db_readonly_check", "db", "Run readonly DB check only when explicitly requested.", "DB check result recorded or skipped."},
                                {"summarize_findings", "review", "Summarize findings.", "Findings summarized."},
                                {"create_checkpoint", "checkpoint", "Persist a checkpoint.", "Checkpoint recorded."}
                        }),
                        gates(new String[][]{
                                {"review_safety", "sql_is_readonly_or_select_validated", "hard", "sql_safety", "SQL must be readonly or SELECT-valid.", "Readonly validated."},
                                {"review_safety", "no_raw_sql_result_in_memory", "hard", "sensitive_guard", "Raw SQL results must not enter memory.", "Memory boundary respected."},
                                {"review_index_and_pagination", "index_considered", "hard", "manual", "Index impact must be considered.", "Index considered."},
                                {"review_index_and_pagination", "pagination_order_considered", "hard", "manual", "Pagination ordering must be considered.", "Ordering considered."}
                        })),
                template("code-review", "Code Review", "review", "review",
                        "Review code changes for correctness, safety, tests, and maintainability.",
                        phases(new String[][]{
                                {"export_context", "context", "Export current memory context.", "CURRENT_CONTEXT.md is available."},
                                {"collect_diff", "review", "Collect the diff or files under review.", "Diff collected."},
                                {"review_correctness", "review", "Review correctness risks.", "Correctness findings recorded."},
                                {"review_safety", "review", "Review safety and data handling.", "Safety findings recorded."},
                                {"review_tests", "review", "Review test impact.", "Test impact recorded."},
                                {"summarize_findings", "review", "Summarize findings by severity.", "Findings summarized."},
                                {"create_checkpoint", "checkpoint", "Persist a checkpoint.", "Checkpoint recorded."}
                        }),
                        gates(new String[][]{
                                {"collect_diff", "diff_collected", "hard", "manual", "Diff must be collected.", "Diff collected."},
                                {"review_safety", "high_risk_issues_checked", "hard", "manual", "High-risk issues must be checked.", "High-risk review complete."},
                                {"review_tests", "test_impact_checked", "hard", "test_result", "Test impact must be checked.", "Test impact checked."}
                        }))
        };
    }

    private TemplateSpec template(String key, String name, String category, String mode, String description,
                                  PhaseSpec[] phases, GateSpec[] gates) {
        return new TemplateSpec(key, name, description, category, mode, phases, gates);
    }

    private PhaseSpec[] phases(String[][] values) {
        PhaseSpec[] phases = new PhaseSpec[values.length];
        for (int i = 0; i < values.length; i++) {
            phases[i] = new PhaseSpec(values[i][0], values[i][1], values[i][2], values[i][3]);
        }
        return phases;
    }

    private GateSpec[] gates(String[][] values) {
        GateSpec[] gates = new GateSpec[values.length];
        for (int i = 0; i < values.length; i++) {
            gates[i] = new GateSpec(values[i][0], values[i][1], values[i][2], values[i][3], values[i][4], values[i][5]);
        }
        return gates;
    }

    private String title(String key) {
        String[] parts = key.split("_|-");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.length() == 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    public static final class SeedResult {
        private final int templatesSeeded;
        private final int phasesSeeded;
        private final int gatesSeeded;
        private final int templatesInserted;
        private final int phasesInserted;
        private final int gatesInserted;

        public SeedResult(int templatesSeeded, int phasesSeeded, int gatesSeeded,
                          int templatesInserted, int phasesInserted, int gatesInserted) {
            this.templatesSeeded = templatesSeeded;
            this.phasesSeeded = phasesSeeded;
            this.gatesSeeded = gatesSeeded;
            this.templatesInserted = templatesInserted;
            this.phasesInserted = phasesInserted;
            this.gatesInserted = gatesInserted;
        }

        public int templatesSeeded() { return templatesSeeded; }
        public int phasesSeeded() { return phasesSeeded; }
        public int gatesSeeded() { return gatesSeeded; }
        public int templatesInserted() { return templatesInserted; }
        public int phasesInserted() { return phasesInserted; }
        public int gatesInserted() { return gatesInserted; }
    }

    private static final class TemplateSpec {
        private final String key;
        private final String name;
        private final String description;
        private final String category;
        private final String mode;
        private final PhaseSpec[] phases;
        private final GateSpec[] gates;

        private TemplateSpec(String key, String name, String description, String category, String mode,
                             PhaseSpec[] phases, GateSpec[] gates) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.category = category;
            this.mode = mode;
            this.phases = phases;
            this.gates = gates;
        }
    }

    private static final class PhaseSpec {
        private final String key;
        private final String type;
        private final String instruction;
        private final String expectedOutput;

        private PhaseSpec(String key, String type, String instruction, String expectedOutput) {
            this.key = key;
            this.type = type;
            this.instruction = instruction;
            this.expectedOutput = expectedOutput;
        }
    }

    private static final class GateSpec {
        private final String phaseKey;
        private final String key;
        private final String severity;
        private final String type;
        private final String ruleText;
        private final String expectedResult;

        private GateSpec(String phaseKey, String key, String severity, String type, String ruleText,
                         String expectedResult) {
            this.phaseKey = phaseKey;
            this.key = key;
            this.severity = severity;
            this.type = type;
            this.ruleText = ruleText;
            this.expectedResult = expectedResult;
        }
    }
}
