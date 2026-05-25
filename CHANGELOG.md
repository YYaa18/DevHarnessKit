# Changelog

## 0.1.0-alpha

Initial developer-preview release.

- Memory core: init, add, confirm, search, export, checkpoint, recover.
- DB readonly: test, sql, and dry-run with fail-closed SQL guard.
- Workflow alpha: templates, runs, phases, gates, artifacts, bindings, summary, and export.
- Spec alpha: changes, documents, tasks, acceptance criteria, status, archive, workflow binding, and export.
- Goal orchestration alpha MVP: `goal start`, `goal resume`, `goal next`, `goal step`, `goal status`, `goal export`, `goal check`, `goal evaluate`, `goal complete`, schema v5, `GOAL_CONTEXT.md`, and `GOAL_SUMMARY.md`.
- Sensitive-data guard for persisted and exported content paths.
- `dhk version`.
- File/stdin input for memory content, checkpoint summaries, SQL, and spec documents.
- `memory search --explain`.
- `memory backup`.
- Project-level sensitive policy with `reject`, `redact`, and `allow` actions.
- Java/OS CI matrix.
- Release archives with third-party notices and checksums.
- Goal-first agent skill package with start/resume/next/step/check/evaluate/complete wrapper scripts.
