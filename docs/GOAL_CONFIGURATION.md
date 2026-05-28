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

## Goal UX Hardening

V0.4.5 adds a stricter but easier goal interaction surface for agents and
scripts. The intent is to keep completion gates strict while reducing fragile
manual evidence formatting.

Use machine-readable `goal next` when an agent needs to decide the next action:

```bash
dhk goal next --goal <goal-key> --json
```

The JSON output includes `evidence_contract`, `required_evidence`,
`structured_evidence_fields`, `allowed_actions`, `forbidden_actions`,
`required_checks`, `completion_blockers`, and `next_command`.
`structured_evidence_fields` includes the stable dedicated options plus
`--field <required_evidence>=<value>` entries for every action-specific
required evidence key, so agent adapters do not need to guess fields such as
`sensitive_result`.

Use `goal step --template` before recording evidence when the current action is
unclear:

```bash
dhk goal step --goal <goal-key> --template
```

Prefer repeated `--field key=value` inputs over hand-written long evidence
strings:

```bash
dhk goal step --goal <goal-key> \
  --summary "Inspected existing order flow" \
  --field existing_controller=OrderController \
  --field existing_service=OrderService \
  --field existing_mapper=OrderMapper \
  --field existing_tests=OrderServiceTest
```

`--dry-run` validates the same evidence contract without inserting a
`goal_step` row:

```bash
dhk goal step --goal <goal-key> \
  --summary "Planned change" \
  --field impacted_files=OrderController.java,OrderService.java \
  --field risk_points=pagination-boundary \
  --field verification_plan="mvn test" \
  --dry-run
```

Completed goals can be checked without reopening them:

```bash
dhk goal audit --goal <goal-key>
dhk goal recheck --goal <goal-key>
```

`goal audit` is read-only. `goal recheck` reruns checks and refreshes
`goal_check` rows, but it does not add development steps and does not reopen a
completed goal.

`goal verify` supports three levels:

```bash
dhk goal verify --goal <goal-key> --level fast
dhk goal verify --goal <goal-key> --level standard
dhk goal verify --goal <goal-key> --level release
```

- `fast` is a lightweight preflight for sensitive/discipline/graph-bdd checks.
  It is not a completion proof.
- `standard` is the default and preserves the previous `goal verify` behavior:
  run all required checks and evaluate completion readiness.
- `release` runs standard checks and reports package, artifact passport, and
  export-contract status for release review. The artifact passport is normally
  created by `goal complete`, so pre-complete release verification may report
  `missing_until_goal_complete`.

## Project Configuration

V0.4.5 adds a project-level configuration file at:

```text
.agents/devharness/config.json
```

The schema key is `devharness-config/v1-alpha`. Like profile and policy files,
the current parser uses flat string fields. Use dotted keys instead of nested
objects:

```json
{
  "schema_version": "devharness-config/v1-alpha",
  "project.type": "springboot-enterprise-large",
  "project.module_style": "api",
  "verification.compile.mode": "manual",
  "verification.compile.manual_trigger": "IDE build action",
  "verification.test.mode": "manual",
  "verification.test.manual_trigger": "IDE test button",
  "verification.graph.mode": "required",
  "verification.test.cost": "slow",
  "verification.rollback.required_if_test_not_run": "true"
}
```

Create one with:

```bash
dhk configure init --preset springboot-manual-ide-test --dry-run
dhk configure init --preset springboot-manual-ide-test --force
dhk configure show
dhk configure doctor
dhk configure explain verification.test.mode
```

Supported starter presets:

| Preset | Intent |
| --- | --- |
| `manual-ide-test` | Alias for `springboot-manual-ide-test`, useful when the project type is less important than manual verification capability. |
| `springboot-manual-ide-test` | Spring Boot or company-runtime projects where compile/test evidence comes from IDE, CI, or another manual source. |
| `springboot-auto-test` | Projects where CLI compile/test commands are expected to run locally. |
| `graph-advisory` | CLI compile/test remains automatic, while graph configuration is advisory and does not become a completion gate. |
| `demo-no-build` | Demo/mock projects without a build file. Compile/test are disabled and `GOAL_CONTEXT.md` warns that demo mode does not prove correctness. |
| `legacy-java-small-fix` | High-risk legacy maintenance where auto compile/test may be unavailable and rollback evidence is required. |

`verification.compile.mode` and `verification.test.mode` accept:

| Mode | Runtime behavior |
| --- | --- |
| `auto` | `goal verify` runs configured compile/test commands. |
| `manual` | `goal verify` does not run the command and instead requires manual evidence. |
| `disabled` | The direct check is replaced with `verification-risk`, which requires waiver, approver, risk scope, and rollback evidence. `demo-no-build` is the only built-in exception and is for quick evaluation only. |

`verification.graph.mode` accepts:

| Mode | Runtime behavior |
| --- | --- |
| `off` | Graph is not part of the project verification policy. |
| `advisory` | Graph artifacts may be generated for context, but graph/impact checks are not completion gates. |
| `required` | Graph freshness and impact artifacts are required by graph-aware profiles and checks. |

`configure init --dry-run` prints the config path, selected preset, effective
verification modes, graph mode, and whether writing would require `--force`.
It does not create `.agents/devharness/config.json` and is intended for
quickstart/control-panel planning.

Manual verification is not skipped verification. Missing evidence blocks
completion. A manual compile check requires the latest goal-step evidence to
include:

```text
manual_evidence_status=passed
compile_scope=<module, class, or changed file scope>
manual_evidence_path=<project-relative artifact path>
```

A manual test check requires:

```text
manual_evidence_status=passed
test_scope=<class, method, scenario, or regression scope>
manual_evidence_path=<project-relative artifact path>
```

The evidence path must exist inside the project. Typical artifacts are IDE test
screenshots, CI logs, copied console output, or a checked-in manual verification
note under a project-approved audit directory.

When compile or test is `disabled`, the replacement `verification-risk` check
requires:

```text
waive_reason=<why this verification cannot be run>
approver=<human or role approving the risk>
risk_scope=<affected module or behavior>
rollback_plan=<project-relative rollback artifact>
```

`GOAL_CONTEXT.md` renders the effective `<verification-policy>` and, for manual
flows, a `<manual-verification-contract>`. Strict skills should follow that
contract instead of inventing their own test commands.

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
| `graph_required` | If true, the profile declares that graph snapshot and impact evidence are part of the goal protocol. `goal verify` adds graph-aware checks according to the graph fields below. |
| `graph_provider` | Graph provider key. Supported alpha values are `lite` and `cgc`; built-in graph-aware profiles default to `lite`. |
| `graph_require_fresh_snapshot` | If true, graph-aware verification should require a fresh graph snapshot. Defaults to `graph_required`. |
| `graph_require_impact_map` | If true, graph-aware verification should require `IMPACT_MAP.md`. Defaults to `graph_required`. |
| `graph_max_staleness_minutes` | Positive integer staleness window for graph evidence. Defaults to `60`. |
| `bdd_min_coverage_percent` | Goal check policy key. Minimum BDD bound-scenario coverage percentage. Defaults to `100`. |
| `bdd_min_quality_score` | Goal check policy key. Minimum BDD quality score. Defaults to `0` so lint warnings stay advisory unless configured. |
| `bdd_fail_on_quality_errors` | Goal check policy key. When `true`, any BDD lint error on a goal-bound scenario fails the BDD check. |
| `bdd_fail_on_quality_warnings` | Goal check policy key. When `true`, any BDD lint warning on a goal-bound scenario fails the BDD check. |
| `graph_actions` | Comma-separated action keys that are graph-specific and must also be present in `actions`. |
| `bdd_required` | If true, `goal verify` requires the `bdd` check. The check passes only when the goal has bound BDD scenarios and the latest goal-specific evidence for each scenario is accepted. |

BDD policy fields are governance gates for acceptance evidence quality. They do
not replace compile/test checks, code review, or manual validation. A failed BDD
gate should explain whether the blocker is missing scenario binding, insufficient
scenario coverage, stale/missing evidence, or configured quality thresholds.

Built-in graph-aware Java profiles are available as alpha profiles:

```text
java-api-change-with-graph
java-api-change-with-bdd-graph
java-mvc-change-with-graph
safe-refactor-with-graph
```

Graph-required profiles automatically require `graph` when `graph_require_fresh_snapshot=true`
and `impact` when `graph_require_impact_map=true`, even if a local
`goal-check-policy.json` narrows the base check list. The `graph` check requires
`GRAPH_SNAPSHOT.json` and `GRAPH_CONTEXT.md`, verifies that the snapshot
workspace fingerprint still matches the current workspace, and applies
`graph_max_staleness_minutes`. The `impact` check requires `IMPACT_MAP.md`,
verifies that it was generated from the latest graph snapshot, applies the same
staleness window, and checks that recorded `changed_files` under `src/` are
covered by the impact map. The impact map also reports related tests and
`missing-related-tests` using Java file conventions. This is an alpha heuristic:
it is meant to surface likely test gaps, not to prove test sufficiency.

BDD-required profiles automatically require `bdd`, even when a local
`goal-check-policy.json` narrows the base check list. The `bdd` check reads
`bdd_binding` rows where `binding_type=goal` and `binding_key=<goal-key>`, then
requires the latest evidence for each bound scenario and goal to be `passed`,
`waived`, or `skipped`. Missing, pending, or failed scenario evidence blocks
`goal verify` and `goal complete`.

When a profile sets both `bdd_required=true` and `graph_required=true`, the
`impact` check also requires `.agents/bdd/exports/SCENARIO_IMPACT_MAP.md`.
The map must reference a goal-bound scenario, must not be marked stale, and must
be newer than the latest `GRAPH_SNAPSHOT.json`. `goal next` and
`GOAL_CONTEXT.md` surface the next scenario impact command:

```bash
dhk graph impact --project-root <project-root> --scenario <scenario-key>
```

`goal next` and `GOAL_CONTEXT.md` surface graph preflight fields when graph is
required: `snapshot_workspace_fingerprint`, `current_workspace_fingerprint`,
`graph_stale`, `freshness_status`, and the required graph action. If a fresh
snapshot is required and the snapshot is stale, the next command points back to
`dhk graph index` + `dhk graph export` before graph impact analysis can
continue.

Built-in graph-aware Java profiles also require the alpha `architecture` check.
It reads `.agents/graph/architecture.json` when present, otherwise uses default
controller/service/repository path rules:

```text
controller -> service -> repository
controller must not import repository
repository must not import controller or service
service must not import controller
```

The architecture check is a Graph Lite heuristic. It uses indexed Java import
edges and path/package conventions; it is not full Java type resolution. The
default mode is `warn`, which records a passing check with an
`architecture warning` summary. Set `"mode": "fail"` in
`.agents/graph/architecture.json` to make violations block completion.

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

`safe-refactor-with-graph` uses the same graph index/impact flow, but its
`graph_reimpact` evidence is stricter. It requires:

```text
post_change_impact_map
impact_delta
changed_files_covered
```

When `impact_delta` says the post-change impact expanded or widened, the impact
check also requires explicit expansion risk evidence such as
`impact_expansion_risk` or `risk_evidence`. General planning `risk_points` are
not enough for this post-change claim. Missing reimpact evidence blocks
`goal verify`.

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
mapping.<action_key>.required_checks: "compile,test,sensitive,graph,impact"
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
acceptance.<acceptance_key>.required_checks: "compile,test,sensitive,graph,impact"
acceptance.<acceptance_key>.evidence_key: "business_verified"
```

Business acceptance mappings create managed spec acceptance items. Automatic completion is deliberately narrow:

- `source=checks` passes the acceptance only when every listed `required_checks` is fresh and accepted by policy;
- `source=test` passes the acceptance from the fresh accepted `test` check, or from listed checks when `required_checks` is provided;
- `source=evidence` passes the acceptance only when a recorded goal step contains the configured `evidence_key`;
- `source=manual` creates the acceptance but never auto-passes it, so `goal verify` and `goal complete` remain blocked until it is explicitly confirmed.

Mappings remain alpha and are validated by `dhk doctor`.

Manual acceptance status values are intentionally small:

```bash
dhk spec acceptance statuses
```

Canonical values are `pending`, `passed`, `failed`, and `waived`.
Common user-facing aliases are accepted and normalized: `done`, `closed`,
`accepted`, `resolved`, and `approved` become `passed`; `open` and
`in_progress` become `pending`; `rejected` becomes `failed`; `waive` becomes
`waived`. `waived` still requires `--evidence` or `--reason`.

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
  "accepted_workflow_statuses": "passed,skipped,waived",
  "accepted_graph_statuses": "passed",
  "accepted_impact_statuses": "passed",
  "accepted_architecture_statuses": "passed"
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

## Project Safety Policy

Project-level safety policy lives at:

```text
.agents/devharness/policy.json
```

Graph stale overrides are denied-by-default for weak-model flows:

```json
{
  "graph_allow_stale_requires_approval": "true"
}
```

The default is `true`. When a caller uses `dhk graph impact --allow-stale`, the
CLI requires `--allow-stale-evidence <evidence>` unless project policy
explicitly sets `graph_allow_stale_requires_approval` to `false`. Strict
graph-aware skill scripts also block `--allow-stale` unless a human or policy
sets `DHK_ALLOW_STALE_APPROVED=true` outside the model-controlled flow.

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
