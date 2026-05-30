# DevHarness Policy

Status: stable-candidate local schema and hook behavior for 1.0.

`.agents/devharness/policy.json` is a local-first, string-only JSON policy
used by DevHarnessKit diagnostics and command hooks. The stable-candidate
schema identifier is `devharness-policy/v1`; legacy files without
`schema_version` are still loaded, but `doctor` warns so projects can migrate
without breaking existing workflows.

It does not replace:

- `sensitive-policy.json`, which controls sensitive-data reject/redact/allow behavior.
- `goal-profiles/*.json`, which defines goal process actions and evidence.
- `goal-check-policy.json`, which configures goal check commands and accepted check statuses.

`policy.json` is enforced only at the hook points listed below. It is not a
general sandbox, operating-system permission boundary, database permission
boundary, or substitute for review.

## Example

```json
{
  "schema_version": "devharness-policy/v1",
  "mode": "strict",
  "allowed_dhk_commands": "goal start,goal resume,goal next,goal step,goal verify,goal complete",
  "forbidden_dhk_commands": "workflow gate waive,spec archive,memory confirm,db sql",
  "protected_files": ".env,application-prod.yml,deploy/**",
  "allowed_write_paths": "src/**,docs/**,README.md",
  "db_sql_requires_explicit_request": "true",
  "db_require_readonly_credentials": "true",
  "db_allowed_environments": "dev,test,readonly",
  "context_export_require_sensitive_scan": "true",
  "context_export_block_on_sensitive": "true",
  "context_export_allowed_files": ".agents/memory/exports/*.md",
  "context_export_forbidden_files": ".env,application-prod.yml",
  "graph_allow_stale_requires_approval": "true",
  "skill_contract_required": "true",
  "skill_key": "devharness-goal-development",
  "skill_trust_required_for_high_risk": "false",
  "skill_trust_override_checkpoint_type": "skill_trust_override",
  "human_checkpoint_required": "false",
  "human_checkpoint_type": "before_complete"
}
```

## Fields

| Field | Values | Purpose |
| --- | --- | --- |
| `schema_version` | `devharness-policy/v1` | Stable-candidate schema identifier. Missing legacy values load but produce a doctor warning. |
| `mode` | `strict`, `guided`, `expert` | Human-readable policy posture for skills and hooks. |
| `allowed_dhk_commands` | comma-separated command prefixes | Commands that strict-mode agents are expected to prefer. |
| `forbidden_dhk_commands` | comma-separated command prefixes | Commands hooks may block unless explicitly allowed. |
| `protected_files` | project-relative globs | Files that should not be modified without explicit policy approval. |
| `allowed_write_paths` | project-relative globs | File areas a task may modify without extra approval. |
| `db_sql_requires_explicit_request` | boolean string | DB SQL should require explicit user intent. |
| `db_require_readonly_credentials` | boolean string | DB connections should use read-only accounts. |
| `db_allowed_environments` | comma-separated keys | Labels such as `dev`, `test`, or `readonly`; not connection credentials. |
| `context_export_require_sensitive_scan` | boolean string | Context exports should run sensitive-data checks. |
| `context_export_block_on_sensitive` | boolean string | Sensitive findings should block export when hooks enforce policy. |
| `context_export_allowed_files` | project-relative globs | Export files permitted by policy. |
| `context_export_forbidden_files` | project-relative globs | Files that should never be exported as context. |
| `graph_allow_stale_requires_approval` | boolean string | Defaults to `true`; `graph impact --allow-stale` requires explicit evidence unless this is set to `false`. |
| `skill_contract_required` | boolean string | When true, doctor expects `skill_key` to point at a local skill contract. |
| `skill_key` | skill key | Local skill under `.agents/skills/<skill_key>/contract.json` used by weak-model governance. |
| `skill_trust_required_for_high_risk` | boolean string | When true, high-risk skill contracts must be trusted before `goal verify`/`goal complete`, unless an override checkpoint is approved. |
| `skill_trust_override_checkpoint_type` | lowercase key | Checkpoint type used for manual skill-trust override, defaulting to `skill_trust_override`. |
| `human_checkpoint_required` | boolean string | When true, `goal verify` and `goal complete` require an approved human checkpoint. Defaults to `false`. |
| `human_checkpoint_type` | lowercase key | Checkpoint type required for completion, defaulting to `before_complete`. |

Boolean values accept `true/false`, `yes/no`, or `1/0`.

## Diagnostics

Run:

```bash
dhk doctor --project-root .
dhk doctor --project-root . --json
```

Doctor warns about:

- missing or unsupported `schema_version`;
- invalid JSON;
- unknown fields;
- unsupported `mode`;
- invalid command patterns;
- command prefixes that appear in both `allowed_dhk_commands` and `forbidden_dhk_commands`;
- duplicate or empty list items;
- absolute paths, `~`, or `..` in file globs;
- identical paths in `context_export_allowed_files` and `context_export_forbidden_files`;
- invalid boolean values;
- missing skill contract when `skill_contract_required` and `skill_key` are configured;
- `skill_trust_required_for_high_risk` without a `skill_key`;
- invalid `skill_trust_override_checkpoint_type` keys;
- invalid `human_checkpoint_type` keys;
- invalid DB environment keys.

## Human Checkpoints

When a high-risk task requires explicit approval, set:

```json
{
  "human_checkpoint_required": "true",
  "human_checkpoint_type": "before_complete"
}
```

Then request and approve the checkpoint:

```bash
dhk checkpoint request --goal <goal-key> --type before_complete --reason "<why approval is needed>"
dhk checkpoint approve --goal <goal-key> --id <id> --approver <name> --reason "<approval rationale>"
dhk checkpoint list --goal <goal-key>
```

The approved checkpoint is an audit record. It does not replace tests, code
review, security review, or generated artifact verification.

## Skill Trust Gate

For high-risk third-party or local skills, configure:

```json
{
  "skill_contract_required": "true",
  "skill_key": "devharness-risky",
  "skill_trust_required_for_high_risk": "true",
  "skill_trust_override_checkpoint_type": "skill_trust_override"
}
```

When the persisted `skill_contract` row for `skill_key` has
`risk_level=high`, `goal verify` and `goal complete` require:

```text
trusted = true
trust_status = trusted
source_hash = trusted_source_hash
```

If the skill is unknown, untrusted, or `review_required`, completion is blocked
until the skill is trusted with `dhk skill trust` or an approved
`skill_trust_override` checkpoint is recorded.

`doctor --json` returns policy warnings under `policy_warnings`.

## Hook Semantics

Hook decisions use three stable-candidate outcomes:

| Outcome | Meaning |
| --- | --- |
| allow | The hook found no configured block and the command continues. |
| warn | `doctor` reports a damaged, missing, legacy, or conflicting policy setting. Warnings do not mutate project state. |
| block | The command exits with validation error before the protected mutation or export, prints `Policy blocked ...`, and includes `next_command:` guidance. |

Missing `policy.json` means most hooks are permissive, but stale Graph Lite
overrides still use the default `graph_allow_stale_requires_approval=true`.
Damaged JSON is diagnosed by `doctor` and loaded as the default policy. When a
command is both allowed and forbidden, the forbidden rule wins; `doctor` reports
the conflict.

## Hook Points

The stable-candidate hook points are:

| Hook | Command path | What can block |
| --- | --- | --- |
| `before-goal-step` | `dhk goal step` | `forbidden_dhk_commands`, missing `allowed_dhk_commands`, `protected_files`, or changed files outside `allowed_write_paths`. |
| `before-goal-check` | `dhk goal check` | `forbidden_dhk_commands` or missing `allowed_dhk_commands`. `dhk goal verify` may still run its internal checks under the `goal verify` command. |
| `before-goal-complete` | `dhk goal complete` | `forbidden_dhk_commands`, missing `allowed_dhk_commands`, or goal steps that changed `protected_files`. |
| `before-db-sql` | `dhk db sql` | `forbidden_dhk_commands`, missing `allowed_dhk_commands`, or non-dry-run SQL without `--i-understand-db-readonly-risk` when `db_sql_requires_explicit_request` is true. |
| `before-graph-impact` | `dhk graph impact` | `forbidden_dhk_commands`, missing `allowed_dhk_commands`, or `--allow-stale` without `--allow-stale-evidence` when `graph_allow_stale_requires_approval` is true. |
| `before-context-export` | `dhk memory export` and goal context exports | `context_export_allowed_files`, `context_export_forbidden_files`, and sensitive matches when `context_export_block_on_sensitive` is true. |

Blocked hooks print actionable guidance. Examples:

```text
Policy blocked db sql: db_sql_requires_explicit_request requires --i-understand-db-readonly-risk
next_command: dhk db sql --project-root "<project>" --i-understand-db-readonly-risk --sql "select 1"
```

```text
Policy blocked context export: output path is forbidden: .agents/memory/exports/CURRENT_CONTEXT.md
next_command: dhk doctor --project-root "<project>"
```

Hooks never call a network service or daemon.

## Security Notes

`policy.json` is not a permission boundary by itself. It is a local policy declaration for DevHarnessKit commands and future hook points. Keep OS permissions, Git review, DB account privileges, and production access controls in place.
