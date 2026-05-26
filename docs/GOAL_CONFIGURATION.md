# Goal Configuration

Goal orchestration has built-in alpha profiles, but projects can override or add local profiles without changing the jar.

## Profile Overrides

Put profile files under:

```text
.agents/devharness/goal-profiles/
```

Example:

```json
{
  "profile_key": "java-api-change",
  "workflow_key": "api-change",
  "requires_spec": "true",
  "default_mode": "api",
  "actions": "inspect_existing_code,create_change_plan,implement_minimal_change,verify",
  "required_checks": "compile,test,sensitive,spec,workflow",
  "completion_require_fresh_checks": "true",
  "completion_allow_skipped_checks": "false",
  "completion_require_checkpoint": "true",
  "strict_workflow_phase_order": "true",
  "spec_require_non_empty_tasks": "true",
  "spec_require_non_empty_acceptance": "true",
  "required_evidence.inspect_existing_code": "existing_controller,existing_service,existing_mapper,existing_tests",
  "required_evidence.create_change_plan": "impacted_files,risk_points,verification_plan",
  "required_evidence.implement_minimal_change": "changed_files,implementation_summary",
  "required_evidence.verify": "compile_result,test_result,sensitive_result",
  "mapping.inspect_existing_code.workflow_phase": "inspect_existing_code",
  "mapping.inspect_existing_code.phase_pass_mode": "step",
  "mapping.inspect_existing_code.spec_task": "inspect_existing_code",
  "mapping.create_change_plan.workflow_phase": "create_change_plan",
  "mapping.create_change_plan.phase_pass_mode": "step",
  "mapping.create_change_plan.required_gates": "impacted_files_listed,verification_plan_ready",
  "mapping.create_change_plan.gate_pass_mode": "step",
  "mapping.create_change_plan.spec_task": "create_change_plan",
  "mapping.implement_minimal_change.workflow_phase": "implement_minimal_change",
  "mapping.implement_minimal_change.phase_pass_mode": "step",
  "mapping.implement_minimal_change.spec_task": "implement_minimal_change",
  "mapping.verify.workflow_phase": "verify_tests",
  "mapping.verify.phase_pass_mode": "check",
  "mapping.verify.required_gates": "tests_recorded",
  "mapping.verify.gate_pass_mode": "check",
  "mapping.verify.spec_task": "verify",
  "mapping.verify.spec_acceptance_update": "auto_pass",
  "mapping.verify.acceptance_source": "checks",
  "mapping.verify.required_checks": "compile,test,sensitive",
  "acceptance.goal_checks_pass.description": "Required goal checks are accepted",
  "acceptance.goal_checks_pass.expected": "compile/test/sensitive checks are accepted by policy",
  "acceptance.goal_checks_pass.source": "checks",
  "acceptance.goal_checks_pass.required_checks": "compile,test,sensitive"
}
```

Notes:

- Values are strings because DevHarness Kit intentionally uses a minimal JSON parser.
- `actions` is a comma-separated list.
- `workflow_key` must match a seeded workflow template.
- `profile_key` is optional, but when present it must match the profile filename.
- If the profile file is invalid, the built-in profile with the same key is used when available.
- If a custom-only profile is invalid, `goal start --profile <key>` will fail because there is no built-in fallback.
- A goal is not ready to complete until every configured action has a recorded `goal step`.
- `goal step` validates the current action's required evidence. Built-in and configured action evidence keys should appear in `--evidence`, while `changed_files` may be satisfied by `--changed-files`.

## Profile Schema Alpha

The alpha schema is deliberately flat so it can be parsed by the current minimal JSON parser. Nested objects and arrays are not supported yet.

Supported profile fields:

| Field | Meaning |
| --- | --- |
| `profile_key` | Optional self-check. If present, it must match `<profile>.json`. |
| `workflow_key` | Workflow template key used when `goal start` creates the workflow run. |
| `requires_spec` | Whether `goal start` creates a spec change. |
| `default_mode` | Mode used when `goal start --mode` is omitted or `auto`. |
| `actions` | Comma-separated ordered action keys. Every action requires a `goal step`. |
| `required_checks` | Profile-level checks used when no global `goal-check-policy.json` overrides them. |
| `completion_require_fresh_checks` | If true, checks become stale after later goal steps. |
| `completion_allow_skipped_checks` | If false, skipped checks are not accepted unless policy explicitly allows them. |
| `completion_require_checkpoint` | Declares that completion should create a checkpoint. Current `goal complete` always creates one. |
| `strict_workflow_phase_order` | If true, mapped phases are not marked passed while earlier workflow phases are still incomplete. |
| `spec_require_non_empty_tasks` | If true and `requires_spec` is true, the spec check fails when the spec has no tasks. Defaults to `requires_spec`. |
| `spec_require_non_empty_acceptance` | If true and `requires_spec` is true, the spec check fails when the spec has no acceptance criteria. Defaults to `requires_spec`. |
| `graph_required` | If true, the profile declares that graph snapshot and impact evidence are part of the goal protocol. AI-60 only records the policy; enforcement is handled by graph-aware goal checks. |
| `graph_provider` | Graph provider key. Supported alpha values are `lite` and `cgc`; built-in graph-aware profiles default to `lite`. |
| `graph_require_fresh_snapshot` | If true, graph-aware verification should require a fresh graph snapshot. Defaults to `graph_required`. |
| `graph_require_impact_map` | If true, graph-aware verification should require `IMPACT_MAP.md`. Defaults to `graph_required`. |
| `graph_max_staleness_minutes` | Positive integer staleness window for graph evidence. Defaults to `60`. |
| `graph_actions` | Comma-separated action keys that are graph-specific and must also be present in `actions`. |

Built-in graph-aware Java profiles are available as alpha profiles:

```text
java-api-change-with-graph
java-mvc-change-with-graph
```

Their action sequence is:

```text
graph_index_or_refresh
graph_impact_analysis
inspect_existing_code
create_change_plan
implement_minimal_change
graph_reimpact
verify
```

Per-action evidence fields use:

```text
required_evidence.<action_key>: "evidence_key_1,evidence_key_2"
```

Action mapping fields use:

```text
mapping.<action_key>.workflow_phase: "<workflow_phase_key>"
mapping.<action_key>.phase_pass_mode: "none|step|check"
mapping.<action_key>.required_gates: "gate_1,gate_2"
mapping.<action_key>.gate_pass_mode: "none|step|check"
mapping.<action_key>.spec_task: "<spec_task_key>"
mapping.<action_key>.spec_acceptance_update: "manual|auto_pass|disabled"
mapping.<action_key>.acceptance_source: "none|manual|checks"
mapping.<action_key>.required_checks: "compile,test,sensitive"
```

Mappings are alpha process definitions. `goal step`, `goal check`, `goal verify`, and `goal complete` use them to synchronize deterministic workflow/spec state:

- mapped workflow phases use `phase_pass_mode`: `step` passes them after the corresponding goal step, `check` passes them only after the mapping's required checks are freshly accepted, and `none` leaves them manual;
- mapped required gates use `gate_pass_mode`: `step` passes them after accepted goal evidence, `check` passes them only after required checks are freshly accepted, and `none` leaves them manual;
- mapped spec tasks are created automatically and marked done after the corresponding goal step;
- `spec_acceptance_update=auto_pass` with `acceptance_source=checks` creates a goal acceptance item and passes it only after the mapping's required checks are accepted;
- completion creates the checkpoint, passes the `checkpoint_created` gate, and marks the workflow completed when all phases are closed.

For backward compatibility, omitted mapping modes are inferred from existing fields: non-verify workflow phases default to `step`, verify-style phases default to `check`, gates default to `step` when present, and `spec_acceptance_update=auto_pass` defaults `acceptance_source` to `checks`.

Business acceptance mappings use:

```text
acceptance.<acceptance_key>.description: "Human-readable criterion"
acceptance.<acceptance_key>.expected: "Expected observable result"
acceptance.<acceptance_key>.source: "checks|test|evidence|manual"
acceptance.<acceptance_key>.required_checks: "compile,test,sensitive"
acceptance.<acceptance_key>.evidence_key: "business_verified"
```

Business acceptance mappings create managed spec acceptance items. Automatic completion is deliberately narrow:

- `source=checks` passes the acceptance only when every listed `required_checks` is fresh and accepted by policy;
- `source=test` passes the acceptance from the fresh accepted `test` check, or from listed checks when `required_checks` is provided;
- `source=evidence` passes the acceptance only when a recorded goal step contains the configured `evidence_key`;
- `source=manual` creates the acceptance but never auto-passes it, so `goal verify` and `goal complete` remain blocked until it is explicitly confirmed.

Mappings remain alpha and are validated by `dhk doctor`.

See [GOAL_SYNC_STRICTNESS.md](GOAL_SYNC_STRICTNESS.md) for the controller model,
automatic sync rules, manual boundaries, and completion gates.

Built-in Java profiles (`java-api-change` and `java-mvc-change`) now express their own required evidence, required checks, strict completion policy, and action mappings through this same model.

Built-in Java profiles are strict by default:

- `compile`, `test`, `sensitive`, required `spec`, and `workflow` checks must pass.
- A missing `pom.xml` makes compile/test checks `skipped`, which is not accepted.
- Required specs must contain at least one task and one acceptance item, and those items must be closed.
- Workflow phases are strict-order by default: a mapped phase cannot be passed while an earlier phase is still incomplete.
- Pending hard workflow gates fail the workflow check unless mapped goal actions or accepted checks close them. The `checkpoint_created` gate is a completion gate and is closed by `goal complete`.

Minimal custom profile:

```json
{
  "workflow_key": "api-change",
  "requires_spec": "false",
  "default_mode": "api",
  "actions": "inspect_existing_code,create_change_plan,implement_minimal_change,verify",
  "required_checks": "sensitive,workflow",
  "required_evidence.inspect_existing_code": "existing_controller,existing_service,existing_tests"
}
```

## Check Policy

Put the check policy at:

```text
.agents/devharness/goal-check-policy.json
```

Example:

```json
{
  "required_checks": "compile,test,sensitive,spec,workflow",
  "compile_command": "mvn -q -DskipTests compile",
  "test_command": "mvn -q test",
  "fail_pending_hard_gates": "true",
  "accepted_compile_statuses": "passed",
  "accepted_test_statuses": "passed",
  "accepted_sensitive_statuses": "passed",
  "accepted_spec_statuses": "passed",
  "accepted_workflow_statuses": "passed,skipped,waived"
}
```

For a non-Maven project or a controlled internal flow, narrow the required checks:

```json
{
  "required_checks": "sensitive,workflow",
  "fail_pending_hard_gates": "true"
}
```

If a non-Maven flow still needs a compile placeholder, explicitly allow `skipped`:

```json
{
  "required_checks": "compile,sensitive",
  "accepted_compile_statuses": "passed,skipped",
  "accepted_sensitive_statuses": "passed"
}
```

Built-in Java profiles (`java-api-change` and `java-mvc-change`) default to strict accepted statuses for `compile`, `test`, `sensitive`, and spec-required `spec`: those checks must be `passed`. This prevents a missing `pom.xml` from silently satisfying completion through skipped compile/test checks. Use project policy to deliberately relax non-Maven or transitional flows.

For strict profiles, pending hard workflow gates fail by default after goal/action sync runs. Set `fail_pending_hard_gates` explicitly when a project needs a different alpha policy:

```json
{
  "required_checks": "compile,test,sensitive,spec,workflow",
  "fail_pending_hard_gates": "false"
}
```

The command parser is intentionally simple and splits `compile_command` and `test_command` by whitespace. Use wrapper scripts when commands need quoting or shell features.

## Doctor Diagnostics

`dhk doctor` validates project-level goal configuration and prints `WARNING goal_config` diagnostics for:

- unknown fields in profile or policy JSON files;
- invalid or empty action lists;
- profile `profile_key` values that do not match the filename;
- per-action evidence fields that reference actions not listed in `actions`;
- action mappings that reference actions not listed in `actions`;
- action mappings that reference unknown workflow phases or gates for built-in workflows;
- unsupported `spec_acceptance_update` policies;
- unsupported business acceptance mapping fields, sources, checks, or missing source-specific requirements;
- invalid `spec_require_non_empty_tasks` or `spec_require_non_empty_acceptance` values;
- empty `required_checks`;
- unsupported check names;
- unsupported accepted check statuses;
- invalid boolean values;
- profile `workflow_key` values that do not match known workflow templates.

Warnings do not change fallback behavior. The runtime still uses built-in defaults when a configured profile or policy cannot be loaded and a built-in fallback exists. `dhk doctor --json` includes these warnings in `goal_config_warnings`.

## Financial-System Example

Financial projects often need email, phone, and ID-card values in legitimate docs and test data. Keep goal checks strict, and configure sensitive-data policy separately for redaction or allow rules:

```json
{
  "required_checks": "compile,test,sensitive,spec,workflow",
  "compile_command": "mvn -q -DskipTests compile",
  "test_command": "mvn -q test",
  "fail_pending_hard_gates": "true",
  "accepted_compile_statuses": "passed",
  "accepted_test_statuses": "passed",
  "accepted_sensitive_statuses": "passed",
  "accepted_spec_statuses": "passed"
}
```

Recommended pairing:

- Use `sensitive-policy.json` to redact or allow common financial test identifiers.
- Keep secrets, tokens, JDBC URLs, Authorization headers, cookies, private keys, and raw production SQL results rejected.
- Run `dhk doctor` after editing goal config so invalid profile or policy files are visible before an agent starts a goal.
