# JSON Output

DevHarness Kit supports alpha JSON output for scripts and agents.

JSON output is an alpha contract in beta patch releases: supported commands and existing field names should change conservatively, but fields may be added before 1.0.
See [COMPATIBILITY.md](COMPATIBILITY.md) for the broader command and output stability policy.

Use `--json` for command status output. Use `--format json` only where a command explicitly documents JSON data output, such as `dhk db sql` query results.

## Supported Commands

```bash
dhk doctor --json
dhk memory search --q gateway --json
dhk memory export --task "Order API" --module order --json
dhk db test --jdbc-url <url> --user <user> --password-env <env> --json
dhk db sql --dry-run --sql "SELECT 1" --json
dhk db sql --sql "SELECT 1" --format json --jdbc-url <url> --user <user> --password-env <env>
dhk configure show --json
dhk configure doctor --json
dhk configure explain [key] --json
dhk goal status --goal <goal-key> --json
dhk goal next --goal <goal-key> --json
dhk goal check --goal <goal-key> --all --json
dhk goal evaluate --goal <goal-key> --json
dhk goal verify --goal <goal-key> [--level fast|standard|release] --json
dhk goal audit --goal <goal-key> --json
dhk goal recheck --goal <goal-key> --json
dhk goal complete --goal <goal-key> --json
dhk bdd init --json
dhk bdd add --feature <key> --title <title> --scenario <key> --scenario-title <title> --json
dhk bdd list --json
dhk bdd show --scenario <key> --json
dhk bdd export --json
dhk bdd lint --json
dhk bdd evidence add --scenario <key> --summary <text> --json
dhk bdd verify --json
dhk bdd coverage --json
dhk bdd bind-spec --scenario <key> --change <key> --acceptance <key> --json
dhk bdd bind-goal --scenario <key> --goal <goal-key> --json
dhk bdd bind-graph --scenario <key> --file <path>|--symbol <symbol>|--sql-table <table> --json
dhk graph impact --scenario <key> --json
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
```

The `match` field lists the weighted fields that contributed to the score, such as `tags`, `title`, `content`, `fts`, `module`, and confidence/status hints.

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

`bdd init`:

```text
command
bdd_dir
features_dir
evidence_dir
exports_dir
```

`bdd add`:

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

`bdd show`:

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

`bdd bind-graph`:

```text
command
scenario_key
binding_type
binding_key
relation
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
