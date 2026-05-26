# Roadmap

DevHarness Kit should stabilize by narrowing the open-source surface before adding major features.

## Beta Patch Releases

Focus: beta release readiness without expanding major feature scope.

- Stabilize `memory init/add/confirm/search/export/checkpoint/recover`.
- Keep file/stdin inputs, JSON output, section-budgeted context export, and `memory backup` compatible across patch releases.
- Keep `doctor` dependable across platforms.
- Keep sensitive-data checks fail-closed where persistence or export is involved.
- Keep automatic pre-migration backups covered by fixture tests.
- Treat [COMPATIBILITY.md](COMPATIBILITY.md) as the source for stable-ish alpha, beta, and alpha contract wording.
- Publish beta developer-preview releases with third-party notices, security notes, checksums, and release archives.

## 0.2.x

Focus: DB readonly as an optional beta module.

- Keep Connector/J 5.1 as the default while the production floor is MySQL 5.1; add compatibility flags for local MySQL 8.
- Make DB risk warnings and docs more visible.
- Add `--sql-file` and `--sql-stdin`.
- Add machine-readable DB probe and dry-run output.
- Plan deterministic SQL safety fuzz/property tests before DB readonly can move beyond beta.
- Revisit the module split decision in [decisions/0001-module-split-and-installer-strategy.md](decisions/0001-module-split-and-installer-strategy.md) after DB readonly contracts settle.
- If install ergonomics becomes necessary, prefer a simple archive-based install script before Homebrew, Scoop, SDKMAN, or other package-manager channels.

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

## 0.6.x

Focus: Policy / hook alpha.

- Keep project policy local-only and deny-by-default only where a policy declares restrictions.
- Add hook coverage for goal completion, DB SQL, and context export.
- Keep hook failures clear and recoverable.
- Avoid treating policy as a sandbox or operating-system permission boundary.

## 0.7.x

Focus: Routine / metrics alpha.

- Use [GOAL_METRICS_REPLAY.md](GOAL_METRICS_REPLAY.md) as the per-goal derived model.
- Use [ROUTINE_LOCAL_CI_EXPORT.md](ROUTINE_LOCAL_CI_EXPORT.md) as the local and CI export plan.
- Start with local/offline report generation only.
- Keep CI exports free of raw task text, evidence, SQL results, and context Markdown by default.
- Defer dashboards, schedulers, and model scoring until routine export contracts are stable.

## 1.0

Focus: stable CLI and schema compatibility.

- Document supported schema migration paths.
- Provide release checksums and reproducible packaging instructions.
- Promote the beta compatibility contract into a stricter stable command/output/schema contract.
- Define post-1.0 semantic versioning and deprecation windows before claiming stability.
- Revisit coverage threshold enforcement using [decisions/0002-coverage-threshold-and-sql-safety-fuzz.md](decisions/0002-coverage-threshold-and-sql-safety-fuzz.md) after the CLI surface and DB safety tests stabilize.
- Split optional modules only if dependency licensing, redistribution, package size, or user demand justifies the extra artifact surface.
