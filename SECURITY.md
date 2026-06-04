# Security Policy

DevHarness Kit is a local-first CLI. It does not run a daemon, open an HTTP port, call an LLM API, or sync project memory to a cloud service.

## Supported Versions

| Version | Status |
| --- | --- |
| `1.2.4` | Current stable release. Security fixes are prioritized for the documented stable surface. |
| `1.2.3` | Superseded stable release. Upgrade to `1.2.4` when possible. |
| `1.2.2` | Superseded stable release. Upgrade to `1.2.4` when possible. |
| `1.2.1` | Superseded stable release. Upgrade to `1.2.4` when possible. |
| `1.2.0` | Superseded stable release. Upgrade to `1.2.4` when possible. |
| `1.1.1` | Superseded stable release. Upgrade to `1.2.4` when possible. |
| `1.1.0` | Superseded stable release. Upgrade to `1.2.4` when possible. |
| `1.0.0` | Superseded stable release. Upgrade to `1.2.4` when possible. |
| `0.4.6-beta.1` | Superseded beta developer preview. Upgrade to `1.2.4` when possible. |

## Threat Model

DevHarness Kit is designed to reduce accidental context leakage into agents by storing durable project context locally and exporting short Markdown files. It is not a sandbox, a database permission system, a secret scanner with complete coverage, or a DLP product.

Expected safe use:

- Run the CLI in a trusted local project checkout.
- Review generated context before sharing it outside the local environment.
- Store only project facts that are safe for agents to read.
- Use database credentials that match your trusted development environment;
  lower privilege accounts are recommended when practical.
- Keep `.agents/memory/memory.db` and `.agents/memory/exports/*.md` out of public commits unless intentionally publishing examples.

Out of scope:

- Running untrusted SQL safely against a privileged database account.
- Preventing a malicious local user from reading local files.
- Proving that workflow gates or spec acceptance criteria are objectively correct.
- Detecting every possible secret format in arbitrary text.

## Sensitive-Data Guard

DevHarness Kit includes a best-effort sensitive-data guard. It rejects obvious values such as passwords, bearer tokens, JDBC URLs, access keys, cookies, and obvious personal data patterns.

Projects can configure `.agents/devharness/sensitive-policy.json` to set pattern actions to `reject`, `redact`, or `allow`. For financial systems, prefer `redact` for common business PII such as email, phone, and identity number, while keeping credentials and secrets at the default `reject` action.

Context exports use a reject-oriented posture when sensitive findings remain. Goal completion summaries are generated in redaction mode first and then rejected if sensitive patterns still remain after redaction. This keeps completion records usable while still avoiding raw secret output.

The guard is heuristic. Users are responsible for reviewing exported context. Do not rely on it as a complete DLP system.

See [docs/SENSITIVE_POLICY.md](docs/SENSITIVE_POLICY.md) for the policy format.

## Database Inspection Safety

`dhk db sql` applies fail-closed SQL text checks and asks JDBC to use a read-only connection where supported.

`dhk db test` and non-dry-run `dhk db sql` print a risk notice unless `--json` is used or the caller explicitly passes `--i-understand-db-readonly-risk`.

These checks are guardrails only:

- SQL text validation cannot prove that a database operation has no side effects.
- JDBC `setReadOnly(true)` is not a permission boundary.
- Views, functions, stored procedures, triggers, permissions, and audit configuration can affect real risk.

DevHarness Kit is a developer toolkit, and its DB commands are intended for
trusted personal development environments. The CLI does not force read-only
credentials. Use credentials appropriate to the environment and prefer lower
privilege accounts when practical.

`dhk db sql --explain` is treated as an inspection operation after SQL
validation, but it may run without the JDBC read-only hint because MySQL 8 with
Connector/J 5.1 can reject `EXPLAIN` when that hint is enabled. This makes
credential choice and local development environment discipline especially
important for execution-plan checks.

## Workflow and Spec Safety

Workflow and spec commands record manual/audit state for agents and humans. They do not execute tests automatically unless a user explicitly wires external commands around them, and they do not prove implementation correctness.

## Graph Stale Snapshot Overrides

Graph impact maps are snapshot-bound generated facts. `dhk graph impact` rejects
stale snapshots by default. The `--allow-stale` escape hatch is not intended for
weak-model self-service: by default it requires `--allow-stale-evidence`, and
the graph wrappers in `devharness-goal-development` also require an external
`DHK_ALLOW_STALE_APPROVED=true` approval. Projects may explicitly relax this in
`.agents/devharness/policy.json` with
`graph_allow_stale_requires_approval=false`, but stale graph output remains
advisory and should not replace a fresh index, tests, or human review.

## Reporting a Vulnerability

Please open a private security advisory on GitHub when available, or contact the maintainers through the repository owner. Include:

- affected version or commit;
- command and inputs needed to reproduce;
- whether any local files, database credentials, or exported context were exposed;
- suggested mitigation if known.

Avoid posting exploit details in public issues until maintainers have had a chance to triage.
