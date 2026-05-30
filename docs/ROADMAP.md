# Roadmap

DevHarness Kit should stabilize by narrowing the open-source surface before adding major features.

## Beta Patch Releases

Focus: beta release readiness without expanding major feature scope.

- Stabilize `memory init/add/confirm/search/export/checkpoint/recover`.
- Keep file/stdin inputs, JSON output, section-budgeted context export, and `memory backup` compatible across patch releases.
- Keep `doctor` dependable across platforms.
- Keep sensitive-data checks fail-closed where persistence or export is involved.
- Keep automatic pre-migration backups covered by fixture tests.
- Treat [COMPATIBILITY.md](COMPATIBILITY.md) as the source for beta, alpha, and internal contract wording.
- Publish beta developer-preview releases with third-party notices, security notes, checksums, and release archives.

## Stable Candidate Closure

Focus: freeze the smallest dependable public surface before any stable claim.

- BDD acceptance harness is stable in 1.0 for documented commands, evidence,
  generated export anchors, and traceability bindings. Scenarios remain
  acceptance intent, not implementation proof by themselves.
- Graph Lite stable-advisory output is included for `status`, `index`,
  `impact`, and `export`; Graph-aware Goal orchestration and graph internals
  remain outside the stable contract.
- Skill, Policy, Routine, and ECC Control Panel user-facing subsets are
  stable-candidate where their compatibility docs and release gates name a
  schema, command, or local report contract. Full governance enforcement,
  generated adapter layouts, public routine automation, and private helper
  internals remain non-stable.
- Keep DB inspection beta until SQL safety fuzz/property tests and real-version
  compatibility checks are stronger.
- Before optional splits, classify Graph, Skill, Policy/Governance, Routine, and
  ECC as optional-dependency or independent-artifact candidates against
  [STABLE_CONTRACT.md](STABLE_CONTRACT.md); do not let non-stable
  dependencies leak into the stable core promise.
- Use Maven `project.version` as the artifact version source for CI, release,
  and local release-gate scripts.
- Run `scripts/release-gate.sh` before stable-candidate tags.
- Keep `CHANGELOG.md`, README current-version marker, and
  `docs/releases/v<version>.md` synchronized through CI/release-gate checks.
- Keep the conservative JaCoCo line-coverage release gate active, then raise it
  only after targeted coverage and two consecutive beta releases support the new
  floor.
- Reduce onboarding friction by treating [INDEX.md](INDEX.md) as the docs map
  and `scripts/devharness-control-panel.sh` as the recommended setup entrypoint.

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

Focus: Routine / metrics stable-candidate local reports.

- Use [GOAL_METRICS_REPLAY.md](GOAL_METRICS_REPLAY.md) as the per-goal derived model.
- Use [ROUTINE_LOCAL_CI_EXPORT.md](ROUTINE_LOCAL_CI_EXPORT.md) as the local and CI export plan.
- Start with local/offline report generation only; public routine commands are
  still outside the stable surface.
- Keep CI exports free of raw task text, evidence, SQL results, and context Markdown by default.
- Defer dashboards, schedulers, and model scoring until routine export contracts are stable.

## 1.0

Focus: stable CLI and schema compatibility.

- Document supported schema migration paths.
- Provide release checksums and reproducible packaging instructions.
- Promote the beta compatibility contract into a stricter stable command/output/schema contract.
- Promote Workflow/Spec first as a stable audit/traceability subset because
  Goal and BDD already depend on their exports and bindings.
- Maintain the documented semantic versioning and deprecation windows before
  claiming or changing stable behavior.
- Track remaining stable-expansion work in
  [STABLE_EXPANSION_PLAN.md](STABLE_EXPANSION_PLAN.md), backed by Linear roadmap
  `AI-319` and feature issues `AI-320` through `AI-325`.
- Revisit coverage threshold enforcement using [decisions/0002-coverage-threshold-and-sql-safety-fuzz.md](decisions/0002-coverage-threshold-and-sql-safety-fuzz.md) after the CLI surface and DB safety tests stabilize.
- Before any stable tag, produce an artifact-boundary decision for each
  experimental module: keep bundled, mark optional dependency, or split into an
  independent artifact. Use dependency licensing, redistribution, package size,
  installation complexity, and user demand as the decision criteria.
- Keep the current shaded CLI as the default until the optional-artifact decision
  proves that a split reduces user risk more than it increases install friction.
- Add shell completion only after command families and option names are less
  volatile.
