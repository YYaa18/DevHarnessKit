# DevHarness Policy

`.agents/devharness/policy.json` is an alpha project safety policy. It is local-first, string-only JSON, and intended to be read by current doctor diagnostics and future hook points.

It does not replace:

- `sensitive-policy.json`, which controls sensitive-data reject/redact/allow behavior.
- `goal-profiles/*.json`, which defines goal process actions and evidence.
- `goal-check-policy.json`, which configures goal check commands and accepted check statuses.

In the current alpha, `policy.json` is enforced only at the hook points listed below. It is not a general sandbox.

## Example

```json
{
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
  "context_export_forbidden_files": ".env,application-prod.yml"
}
```

## Fields

| Field | Values | Purpose |
| --- | --- | --- |
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

Boolean values accept `true/false`, `yes/no`, or `1/0`.

## Diagnostics

Run:

```bash
dhk doctor --project-root .
dhk doctor --project-root . --json
```

Doctor warns about:

- invalid JSON;
- unknown fields;
- unsupported `mode`;
- invalid command patterns;
- duplicate or empty list items;
- absolute paths, `~`, or `..` in file globs;
- invalid boolean values;
- invalid DB environment keys.

`doctor --json` returns policy warnings under `policy_warnings`.

## Hook Points

The first alpha hook points are:

| Hook | Command path | What can block |
| --- | --- | --- |
| `before-goal-complete` | `dhk goal complete` | `forbidden_dhk_commands`, missing `allowed_dhk_commands`, or goal steps that changed `protected_files`. |
| `before-db-sql` | `dhk db sql` | `forbidden_dhk_commands`, missing `allowed_dhk_commands`, or non-dry-run SQL without `--i-understand-db-readonly-risk` when `db_sql_requires_explicit_request` is true. |
| `before-context-export` | `dhk memory export` and goal context exports | `context_export_allowed_files`, `context_export_forbidden_files`, and sensitive matches when `context_export_block_on_sensitive` is true. |

No policy file means these hooks are permissive. A configured policy file can block commands with validation errors; hooks never call a network service or daemon.

## Security Notes

`policy.json` is not a permission boundary by itself. It is a local policy declaration for DevHarnessKit commands and future hook points. Keep OS permissions, Git review, DB account privileges, and production access controls in place.
