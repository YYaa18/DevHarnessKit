# JSON Output

DevHarness Kit supports JSON output for scripts and agents.

Stable commands have conservative minimum fields documented here and in
`STABLE_CONTRACT.md`. Experimental command JSON remains alpha. Supported
commands and existing field names should change conservatively, and consumers
should tolerate additive fields.
See [COMPATIBILITY.md](COMPATIBILITY.md) for the broader command and output stability policy.

Use `--json` for command status output. Use `--format json` only where a command explicitly documents JSON data output, such as `dhk db sql` query results.

## Supported Commands

```bash
dhk doctor --json
dhk memory search --q gateway --json
dhk memory suggest --title "Rule" --content "..." --json
dhk memory candidates list --json
dhk memory dedupe --json
dhk memory conflicts --json
dhk memory stale scan --json
dhk memory pack inspect --path team-memory.zip --json
dhk memory export --task "Order API" --module order --json
dhk db test --jdbc-url <url> --user <user> --password-env <env> --json
dhk db sql --dry-run --sql "SELECT 1" --json
dhk db sql --sql "SELECT 1" --format json --jdbc-url <url> --user <user> --password-env <env>
dhk configure show --json
dhk configure doctor --json
dhk configure explain [key] --json
dhk advise --task "Order API" --json
dhk quickstart --task "Order API" --dry-run --json
dhk goal status --goal <goal-key> --json
dhk goal next --goal <goal-key> --json
dhk goal check --goal <goal-key> --all --json
dhk goal evaluate --goal <goal-key> --json
dhk goal verify --goal <goal-key> [--level fast|standard|release] --json
dhk goal audit --goal <goal-key> --json
dhk goal recheck --goal <goal-key> --json
dhk goal complete --goal <goal-key> --json
dhk workflow start --workflow <workflow> --task <task> --json
dhk workflow status --run <run-key> --json
dhk workflow export --run <run-key> --json
dhk workflow summary --run <run-key> --json
dhk spec create --change <key> --title <title> --json
dhk spec status --change <key> --json
dhk spec export --change <key> --json
dhk spec bind-workflow --change <key> --run <run-key> --json
dhk bdd init --json
dhk bdd add --feature <key> --title <title> --scenario <key> --scenario-title <title> --json
dhk bdd scenario create --scenario <key> --title <title> --json
dhk bdd scenario list --json
dhk bdd scenario show --scenario <key> --json
dhk bdd list --json
dhk bdd show --scenario <key> --json
dhk bdd export --json
dhk bdd lint --json
dhk bdd evidence add --scenario <key> --summary <text> --json
dhk bdd evidence junit --reports <dir[,dir]> --json
dhk bdd evidence report --adapter cucumber|postman|playwright --report <json> --scenario <key> --json
dhk bdd verify --json
dhk bdd coverage --json
dhk bdd bind-spec --scenario <key> --change <key> --acceptance <key> --json
dhk bdd bind-goal --scenario <key> --goal <goal-key> --json
dhk bdd bind-workflow --scenario <key> --run <workflow-run> --json
dhk bdd bind-graph --scenario <key> --file <path>|--symbol <symbol>|--sql-table <table> --json
dhk bdd bind-test --scenario <key> --class <name> [--method <name>] --json
dhk graph status --json
dhk graph impact --file <path>|--symbol <symbol>|--sql-table <table> --json
dhk graph impact --scenario <key> --json
dhk graph export --json
dhk skill lint --skill <key>|--path <skill-dir> --json
dhk skill verify --skill <key>|--path <skill-dir> --json
dhk skill trust --skill <key>|--path <skill-dir> --json
dhk skill audit --skill <key>|--path <skill-dir> --json
dhk skill score --goal <goal-key> --json
dhk skill report --goal <goal-key> --baseline-score <score> --json
```

## Contract Notes

- JSON is written to stdout.
- Diagnostics that are not part of the JSON contract may still be written to stderr.
- Commands may return non-zero exit codes while still writing JSON when the failure is an expected state, such as `doctor` on an uninitialized project or `goal complete` when checks are missing.
- Consumers should tolerate additional fields.
- Consumers should use exit codes plus stable fields such as `command`, `status`, `decision`, `count`, and path fields.

## Required Fields

`doctor`:

```text
command
java_version
project_root
memory_dir_ok
memory_db_ok
project_json_ok
exports_dir_ok
sensitive_policy
mysql_driver_loaded
schema_version
fts
project_record
memory_total
memory_draft
memory_confirmed
checkpoint_total
export_warnings
goal_config_warnings
```

`memory search`:

```text
command
query
module
status
explain
count
results
```

Each result includes:

```text
id
title
status
confidence
module
type
tags
content
match
score
explanation
```

The `match` field lists the weighted fields that contributed to the score, such as `tags`, `title`, `content`, `fts`, `module`, and confidence/status hints. When `--explain` is set, `explanation` includes additive score components and penalties such as `tags:+8`, `verified:+1`, `stale:-8`, or `superseded:-12`.

`memory suggest`:

```text
command
candidate_id
status
fingerprint
canonical_key
```

`memory candidates list`:

```text
command
status
limit
count
items
```

Each item includes:

```text
candidate_id
title
status
module
type
confidence
content
accepted_memory_id
```

`memory dedupe` and `memory conflicts`:

```text
command
module
group_count
groups
```

`memory stale scan`:

```text
command
module
stale_count
items
```

`memory pack inspect`:

```text
command
schema_version
item_count
checksum_ok
```

`memory export`:

```text
command
export_path
memory_exported
task
module
mode
workflow_run
spec_change
```

`db test`:

```text
command
status
probe_select_1_ok
probes
risk_warning
```

`db test --json` error:

```text
command
status
error
compatibility_hint
risk_warning
```

`db sql --dry-run --json` success:

```text
command
status
dry_run
explain
sql
```

`db sql --dry-run --json` rejected:

```text
command
status
dry_run
reason
```

`db sql --dry-run --format json` uses the same success and rejected fields as
`db sql --dry-run --json`.

`db sql --format json` query result:

```text
command
status
rows
truncated
output_truncated
columns
data
risk_warning
```

`db sql --format json` error:

```text
command
status
dry_run
error
compatibility_hint
risk_warning
```

`db sql --format json` rejected:

```text
command
status
dry_run
reason
risk_warning
```

## DB Readonly JSON Contract

Status: beta readonly DB inspection contract. The JSON shape is documented so
scripts can consume diagnostics consistently, but `dhk db test` and
`dhk db sql` remain outside the 1.0 stable command surface.

Machine-readable DB diagnostics are written to stdout when `--json` is used,
and `dhk db sql` also writes machine-readable diagnostics to stdout when
`--format json` is used. Expected validation failures still return non-zero
exit codes.

Exit semantics:

```text
0 success
1 runtime, connection, probe, or query execution failure
2 usage, input, or connection-argument validation failure before DB execution
3 SQL safety, policy, or sensitive-output rejection
```

`risk_warning` is part of DB JSON output. The SQL guard and JDBC read-only hint
are not database permission boundaries. Use a database account with read-only
privileges when the target environment requires that boundary.

`configure show`:

```text
command
project_root
config_path
exists
schema_version
project_type
compile_mode
test_mode
graph_required
rollback_required_if_test_not_run
```

`configure doctor`:

```text
command
project_root
config_path
status
warnings
effective_compile_mode
effective_test_mode
manual_evidence_required
```

`configure explain`:

```text
command
key
explanation
```

`advise`:

```text
brief
recommendation
confidence
safe_to_start
confirmation_required
confirmation_reason
requires_user_confirmation_reason
work_brief_path
agent_brief_path
```

`quickstart` includes the existing setup/goal fields plus:

```text
recommendation
safe_to_start
confirmation_required
confirmation_reason
requires_user_confirmation_reason
work_brief_path
agent_brief_path
```

`goal status`:

```text
command
goal_key
profile
status
workflow_run
spec_change
current_action
step_count
context_path
```

`goal check`:

```text
command
goal_key
count
checks
```

Each check includes:

```text
check_key
status
step_count_at_check
workspace_fingerprint
context_fingerprint
check_fingerprint
result_summary
evidence_path
```

`goal next`:

```text
command
goal_key
status
current_action
instruction
allowed_actions
required_evidence
structured_evidence_fields
forbidden_actions
required_checks
context_files
completion_blockers
evidence_contract
graph
scenario_impact
next_command
context_path
```

`structured_evidence_fields` contains the stable dedicated options and dynamic
`--field <required_evidence>=<value>` entries for each required evidence key in
the current action.

`evidence_contract` repeats the current action, required evidence keys,
structured evidence fields, and the rule for recording the next `goal step`.

`goal evaluate`:

```text
command
decision
ready_to_complete
missing_count
missing
stale_count
stale_checks
next_action
next_command
```

`goal verify`:

```text
command
goal_key
level
check_scope
selected_checks
decision
ready_to_complete
check_count
checks
failed_count
failed_checks
missing_count
missing
stale_count
stale_checks
freshness_status
completion_blocker_count
completion_blockers
next_action
next_command
context_path
```

`goal verify` runs required checks before evaluating readiness. `failed_checks` lists checks with `failed` status; skipped-disallowed checks are reported through `missing`. `freshness_status` is `fresh` only when required checks are current for the latest goal steps and workspace fingerprint. `completion_blockers` combines failed, missing, stale, and policy blockers for agent-facing next-action decisions.

`level=fast` is a preflight and only runs selected lightweight checks.
`level=standard` is the default and runs all required checks. `level=release`
runs standard checks and adds `release_checks`:

```text
package
artifact_passport
export_contract
```

`goal audit`:

```text
command
goal_key
status
current_action
step_count
check_count
artifact_count
artifact_passport
decision
ready_to_complete
missing
stale
invalid
next_command
context_path
```

`goal audit` is read-only; it does not rerun checks or reopen completed goals.

`goal recheck`:

```text
command
goal_key
status_before
status_after
step_count_before
step_count_after
check_count
checks
decision
ready_to_complete
stale_checks
missing
next_command
```

`goal recheck` reruns checks and refreshes check rows without adding goal steps
or changing a completed goal back to an open state.

Profiles with `bdd_required=true` include a `bdd` check in `checks`; failed BDD
checks appear in `failed_checks` and `completion_blockers` like other required
checks.

`workflow start`, `workflow status`, and `workflow export`:

```text
command
run_key
workflow
task
module
mode
status
current_phase
phases
pending_hard_gates
workflow_context_path
```

`workflow_context_path` is present for `workflow export`.

Each `phases` item includes:

```text
phase_key
name
status
phase_order
```

Each `pending_hard_gates` item includes:

```text
gate_key
phase_key
status
```

`workflow summary`:

```text
command
run_key
workflow
task
module
mode
status
current_phase
exported_memory_count
artifact_count
checkpoint_count
bound_spec_count
pending_hard_gate_count
blocking_hard_gate_count
```

`spec create`, `spec status`, and `spec export`:

```text
command
change_key
title
status
module
mode
priority
tasks
acceptance
bound_workflows
export_path
```

`export_path` is present for `spec export`.

Each `tasks` item includes:

```text
task_key
title
status
phase
```

Each `acceptance` item includes:

```text
acceptance_key
description
expected
status
```

`spec bind-workflow`:

```text
command
workflow_spec_binding_id
change_key
title
status
module
mode
run_key
binding_type
bound_workflows
```

Each `bound_workflows` item includes:

```text
run_key
binding_type
```

`bdd init`:

```text
command
bdd_dir
features_dir
evidence_dir
exports_dir
```

`bdd add` and `bdd scenario create`:

```text
command
feature_key
scenario_key
status
step_count
steps
```

`bdd list`:

```text
command
feature_count
scenario_count
features
scenarios
```

`bdd scenario list` returns the same scenario fields as `bdd list` scoped to
the requested feature when `--feature` is provided.

`bdd show` and `bdd scenario show`:

```text
command
feature_key
scenario_key
title
status
steps
bindings
```

For feature-level show, `bdd show --feature <key> --json` returns
`scenario_count` and `scenarios` instead of `steps`.

`bdd export`:

```text
command
context_path
features
scenarios
feature_files
```

`bdd lint`:

```text
command
issue_count
issues
```

`bdd evidence add`:

```text
command
evidence_id
scenario_key
goal_key
evidence_type
status
evidence_path
summary
```

`bdd evidence junit`:

```text
command
scenario_count
binding_count
report_result_count
evidence_count
passed_count
failed_count
skipped_count
pending_count
```

`bdd evidence report`:

```text
command
adapter_key
scenario_count
evidence_count
status
report_path
```

`bdd verify` and `bdd coverage`:

```text
command
passed
scenario_count
covered_count
missing_evidence
pending_evidence
failed_evidence
scenarios
```

Each `scenarios` item includes adapter fields when an executable BDD adapter
normalized the latest evidence:

```text
adapter_key
adapter_normalized_status
```

## Routine Report JSON

Routine local report JSON is stable-candidate and generated by local report
helpers, not by a stable public `dhk routine` CLI command in 1.0.

`routine-summary.json`:

```text
schema_version
generated_at
window.since
window.until
source_schema
metrics_schema
replay_schema
outcomes.total_goals
outcomes.completed_goals
outcomes.failed_goals
outcomes.abandoned_goals
outcomes.in_progress_goals
outcomes.completion_rate
profiles
checks
interventions
```

Stable-candidate schema versions:

```text
routine-summary/v1
goal-metrics/v1
goal-replay/v1
```

`routine-goals.ndjson` contains one `goal-metrics/v1` object per line.
`routine-replay/<goal-key>.ndjson` contains deterministic `goal-replay/v1`
entries sorted by timestamp, source, kind, status, summary, and data.

CI-safe routine exports use the same `routine-summary/v1` aggregate schema and
add two filtered NDJSON files:

```text
routine-checks.ndjson
routine-profiles.ndjson
```

Each line includes `schema_version`, `record_type`, and aggregate count fields.
CI-safe exports intentionally exclude replay entries, task text, step evidence,
SQL text or result paths, context Markdown, chat transcript content, and paths
outside the export directory.

Routine JSON is derived reporting. It is not a completion decision, scheduler,
dashboard, model score, telemetry stream, or direct SQLite API.

`bdd bind-spec`:

```text
command
scenario_key
binding_type
binding_key
relation
change_key
acceptance_key
```

`bdd bind-goal`:

```text
command
scenario_key
binding_type
binding_key
relation
goal_key
```

`bdd bind-workflow`:

```text
command
scenario_key
binding_type
binding_key
relation
run_key
```

`bdd bind-graph`:

```text
command
scenario_key
binding_type
binding_key
relation
```

`bdd bind-test`:

```text
command
scenario_key
binding_type
binding_key
relation
test_class
test_method
```

`graph status`:

```text
command
config_source
report_path
files_considered
indexed_files
skipped_files
max_file_bytes
max_indexed_files
max_impact_depth
max_export_nodes
graph_nodes
graph_edges
parse_errors
latest_snapshot_key
latest_snapshot_status
current_workspace_fingerprint
latest_snapshot_workspace_fingerprint
latest_snapshot_stale
latest_snapshot_nodes
latest_snapshot_edges
```

`graph impact --file|--symbol|--sql-table`:

```text
command
found
query_type
query
depth
requested_depth
max_impact_depth
depth_limited
snapshot_key
snapshot_stale
allow_stale
allow_stale_evidence
current_workspace_fingerprint
snapshot_workspace_fingerprint
related_files
related_sql
related_tests
risk_nodes
recommended_read_files
impact_map
```

`graph impact --scenario`:

```text
command
scenario_key
found
input_bindings
impact_results
found_results
snapshot_stale
allow_stale
related_files
related_tests
related_sql
risk_nodes
recommended_read_files
scenario_impact_map
```

Graph Lite JSON output is stable-advisory: field names and freshness semantics
are stable, but the reported impact is heuristic context, not proof that the
impact set is complete or that an implementation is correct.

`graph export`:

```text
command
snapshot_key
files
nodes
edges
context_path
snapshot_path
```

`skill lint`, `skill verify`, and `skill trust`:

```text
command
skill_key
status
source_path
source_hash
trusted_source_hash
trust_status
trusted
missing
invalid
forbidden
issues
```

Each `issues` item includes:

```text
category
field
message
suggestion
```

`skill audit`:

```text
command
skill_key
decision
source_path
source_hash
issue_count
critical
high
medium
issues
```

Each `issues` item includes:

```text
severity
category
path
message
suggestion
```

`skill score` and `skill report`:

```text
command
goal_key
group
skill_key
skill_contract_present
skill_trusted
trust_status
skill_quality_score
dqi_score
baseline_score
dqi_delta
gate_pass_rate
evidence_completeness
rollback_quality
```

Skill governance JSON output is stable-candidate: field names are intended to
remain stable after the release gate is added, but trust status and scores are
local governance signals, not sandboxing or correctness proof.

`goal complete` success:

```text
command
goal_key
status
checkpoint_id
summary_path
```

`goal complete` not ready:

```text
command
status
decision
ready_to_complete
missing_count
missing
stale_count
stale_checks
next_action
next_command
```
