# Changelog

## 0.1.0-alpha

Initial developer-preview release.

- Memory core: init, add, confirm, search, export, checkpoint, recover.
- DB readonly: test, sql, and dry-run with fail-closed SQL guard.
- Workflow alpha: templates, runs, phases, gates, artifacts, bindings, summary, and export.
- Spec alpha: changes, documents, tasks, acceptance criteria, status, archive, workflow binding, and export.
- Goal orchestration alpha MVP: `goal start`, `goal resume`, `goal next`, `goal step`, `goal status`, `goal export`, `goal check`, `goal evaluate`, `goal complete`, schema v5, `GOAL_CONTEXT.md`, and `GOAL_SUMMARY.md`.
- Goal evidence hardening: `goal step` validates required evidence for the current action and `goal evaluate` refuses completion until configured actions are recorded.
- Goal-first skill hardening: wrapper project-root behavior and protocol reference docs are covered by packaging tests.
- Goal config diagnostics: `dhk doctor` warns about invalid project-level goal profiles and check policies, including unknown fields, invalid actions, empty required checks, and missing workflow templates.
- Goal export contract tests: `GOAL_CONTEXT.md` and `GOAL_SUMMARY.md` required sections, next command, forbidden actions, checkpoint reference, and truncation behavior are covered by tests.
- Workflow/spec export contract docs and tests for required Markdown sections, audit-state boundaries, gates, tasks, acceptance items, and workflow bindings.
- Migration fixture coverage for v2 workflow state and richer v4 spec state, plus archive-state tests for blocked/failed versus skipped/waived spec closure.
- Compatibility contract docs for stable-ish alpha, beta, and alpha CLI/schema/JSON/export surfaces, including downgrade and deprecation policy.
- Module split and installer strategy decision record: keep one shaded alpha jar plus release archives in `0.1.x`, revisit memory-only/DB artifacts before promoting DB readonly or installers.
- Coverage threshold decision and SQL safety fuzz backlog: keep JaCoCo report-only in `0.1.x`, record the current baseline, and require targeted SQL guard fuzz/property tests before stable DB readonly claims.
- Sensitive-data guard for persisted and exported content paths.
- `dhk version`.
- File/stdin input for memory content, checkpoint summaries, SQL, and spec documents.
- `memory search --explain`.
- `--json` output for doctor, memory search/export, and goal status/check/evaluate/complete.
- DB readonly beta hardening: MySQL URL compatibility flags, DB risk notice, `db test --json`, `db sql --dry-run --json`, and `db sql --format json`.
- DB explain plan compatibility: `db sql --explain` works with MySQL 8 and Connector/J 5.1 by avoiding the JDBC read-only hint for validated `EXPLAIN` statements.
- `memory backup`.
- Automatic pre-migration memory backups with old-db fixture coverage.
- Section-budgeted `CURRENT_CONTEXT.md` with a truncation report.
- Project-level goal profile and check policy configuration.
- JaCoCo coverage report generation during `mvn test`.
- Project-level sensitive policy with `reject`, `redact`, and `allow` actions.
- Java/OS CI matrix.
- Release archives with third-party notices and checksums.
- Goal-first agent skill package with start/resume/next/step/check/evaluate/complete wrapper scripts.
- Goal check freshness metadata: schema v6 records `step_count_at_check` for each `goal check` result and exposes it in text/JSON output.
- Goal stale-check enforcement: `goal evaluate` and `goal complete` now reject required checks recorded before the latest goal step.
