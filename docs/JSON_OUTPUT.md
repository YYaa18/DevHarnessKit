# JSON Output

DevHarness Kit supports alpha JSON output for scripts and agents.

JSON output is an alpha contract in `0.1.x`: supported commands and existing field names should change conservatively, but fields may be added before 1.0.

Use `--json` for command status output. Use `--format json` only where a command explicitly documents JSON data output, such as `dhk db sql` query results.

## Supported Commands

```bash
dhk doctor --json
dhk memory search --q gateway --json
dhk memory export --task "Order API" --module order --json
dhk db test --jdbc-url <url> --user <user> --password-env <env> --json
dhk db sql --dry-run --sql "SELECT 1" --json
dhk db sql --sql "SELECT 1" --format json --jdbc-url <url> --user <user> --password-env <env>
dhk goal status --goal <goal-key> --json
dhk goal check --goal <goal-key> --all --json
dhk goal evaluate --goal <goal-key> --json
dhk goal complete --goal <goal-key> --json
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
result_summary
evidence_path
```

`goal evaluate`:

```text
command
decision
ready_to_complete
missing_count
missing
next_action
next_command
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
next_action
next_command
```
