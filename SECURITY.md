# Security Policy

DevHarness Kit is a local-first CLI. It does not run a daemon, open an HTTP port, call an LLM API, or sync project memory to a cloud service.

## Supported Versions

| Version | Status |
| --- | --- |
| `0.1.0-alpha` | Developer preview. Security fixes are best-effort while the public API and schema are still stabilizing. |

## Threat Model

DevHarness Kit is designed to reduce accidental context leakage into agents by storing durable project context locally and exporting short Markdown files. It is not a sandbox, a database permission system, a secret scanner with complete coverage, or a DLP product.

Expected safe use:

- Run the CLI in a trusted local project checkout.
- Review generated context before sharing it outside the local environment.
- Store only project facts that are safe for agents to read.
- Use read-only database credentials for `dhk db` commands.
- Keep `.agents/memory/memory.db` and `.agents/memory/exports/*.md` out of public commits unless intentionally publishing examples.

Out of scope:

- Running untrusted SQL safely against a privileged database account.
- Preventing a malicious local user from reading local files.
- Proving that workflow gates or spec acceptance criteria are objectively correct.
- Detecting every possible secret format in arbitrary text.

## Sensitive-Data Guard

DevHarness Kit includes a best-effort sensitive-data guard. It rejects obvious values such as passwords, bearer tokens, JDBC URLs, access keys, cookies, and obvious personal data patterns.

Projects can configure `.agents/devharness/sensitive-policy.json` to set pattern actions to `reject`, `redact`, or `allow`. For financial systems, prefer `redact` for common business PII such as email, phone, and identity number, while keeping credentials and secrets at the default `reject` action.

The guard is heuristic. Users are responsible for reviewing exported context. Do not rely on it as a complete DLP system.

See [docs/SENSITIVE_POLICY.md](docs/SENSITIVE_POLICY.md) for the policy format.

## Readonly Database Safety

`dhk db sql` applies fail-closed SQL text checks and asks JDBC to use a read-only connection where supported.

These checks are guardrails only:

- SQL text validation cannot prove that a database operation has no side effects.
- JDBC `setReadOnly(true)` is not a permission boundary.
- Views, functions, stored procedures, triggers, permissions, and audit configuration can affect real risk.

Always use database credentials that have read-only privileges at the database server. Do not connect DevHarness Kit to production databases with write-capable credentials.

## Workflow and Spec Safety

Workflow and spec commands record manual/audit state for agents and humans. They do not execute tests automatically unless a user explicitly wires external commands around them, and they do not prove implementation correctness.

## Reporting a Vulnerability

Please open a private security advisory on GitHub when available, or contact the maintainers through the repository owner. Include:

- affected version or commit;
- command and inputs needed to reproduce;
- whether any local files, database credentials, or exported context were exposed;
- suggested mitigation if known.

Avoid posting exploit details in public issues until maintainers have had a chance to triage.
