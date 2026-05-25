# Security Notes

The canonical security policy is [../SECURITY.md](../SECURITY.md).

Key points:

- The sensitive-data guard is best-effort and heuristic.
- Projects can configure `.agents/devharness/sensitive-policy.json` for `reject`, `redact`, or `allow` actions.
- SQL readonly checks are guardrails, not a database permission boundary.
- Always use read-only database credentials.
- DB commands print a risk notice for real connections; `--json` returns the same warning as a JSON field where supported.
- `db sql --explain` may run without the JDBC read-only hint for MySQL 8 + Connector/J 5.1 compatibility, so database-level read-only credentials matter.
- Workflow and spec state are audit records, not correctness proofs.
