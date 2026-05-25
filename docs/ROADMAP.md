# Roadmap

DevHarness Kit should stabilize by narrowing the open-source surface before adding major features.

## 0.1.x

Focus: Memory core readiness.

- Stabilize `memory init/add/confirm/search/export/checkpoint/recover`.
- Keep file/stdin inputs, JSON output, section-budgeted context export, and `memory backup` compatible across patch releases.
- Keep `doctor` dependable across platforms.
- Keep sensitive-data checks fail-closed where persistence or export is involved.
- Keep automatic pre-migration backups covered by fixture tests.
- Publish alpha releases with third-party notices, security notes, checksums, and release archives.

## 0.2.x

Focus: DB readonly as an optional beta module.

- Keep Connector/J 5.1 as the default while the production floor is MySQL 5.1; add compatibility flags for local MySQL 8.
- Make DB risk warnings and docs more visible.
- Add `--sql-file` and `--sql-stdin`.
- Add machine-readable DB probe and dry-run output.
- Consider separating memory-only and DB-enabled artifacts.

## 0.3.x

Focus: Workflow alpha hardening.

- Stabilize workflow output shape.
- Improve artifact and binding audit views.
- Keep workflow clearly framed as state recording, not an execution engine.

## 0.4.x

Focus: goal-oriented orchestration.

- Harden `dhk goal start/resume/next/step/status/export/check/evaluate/complete`.
- Improve configured check policy validation and evidence quality.
- Keep `GOAL_SUMMARY.md` and completion checkpoint output stable enough for alpha users.
- Keep `GOAL_CONTEXT.md` section order and command fields covered by contract tests.
- Migrate the default agent workflow from memory-first prompts to the goal-first protocol.
- Harden project-level goal profile configuration after the built-in profiles stabilize.

## 0.5.x

Focus: Spec alpha hardening.

- Stabilize spec export shape.
- Improve task and acceptance reporting.
- Add fixture-based migration tests for spec schema.

## 1.0

Focus: stable CLI and schema compatibility.

- Document supported schema migration paths.
- Provide release checksums and reproducible packaging instructions.
- Define stable command/output contracts.
- Add coverage thresholds after the JaCoCo report baseline is reviewed.
- Split optional modules if dependency and licensing boundaries require it.
