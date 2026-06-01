# Stable Candidate Contract

DevHarness Kit `1.0.0` promoted the stable-candidate boundary into the first
stable contract. DevHarness Kit `1.1.1` keeps this document as the boundary map
so future releases do not expand the public promise by accident.

## Stable Candidate Surface

The first stable-candidate track should be limited to:

- `dhk help` and `dhk version`;
- `dhk doctor`;
- configure/status onboarding commands: `configure init`, `configure show`,
  `configure doctor`, `configure explain`, `status`, `readiness`, `advise`, and
  `quickstart`;
- memory core commands: `init`, `add`, `confirm`, `search`, `export`,
  `checkpoint`, `recover`, and `backup`;
- goal core commands: `start`, `resume`, `next`, `step`, `status`, `export`,
  `verify`, `complete`, `audit`, and `recheck`. Goal retrospective reporting
  (`retrospective`, `review-summary`, and compatibility alias `mr-summary`) is a
  stable-candidate reporting surface; its Markdown headings are intended for
  humans, while `goal-retrospective/v1-alpha` JSON may still grow additively;
- BDD acceptance commands: `init`, `add`, `scenario create/list/show`, `list`,
  `show`, `export`, `lint`, evidence import, `verify`, `coverage`, and
  traceability bindings for spec, goal, workflow, graph input, and tests;
- Skill Contract stable-candidate schema and local report fields documented in
  `docs/SKILL_CONTRACT.md` and `docs/SKILL_EVALUATION.md`, covered by the
  default release gate before any future promotion into the stable surface;
- generated `WORK_BRIEF.md`, `CURRENT_CONTEXT.md`, `RECOVERY_CONTEXT.md`,
  `GOAL_CONTEXT.md`, `GOAL_SUMMARY.md`, and `ARTIFACT_PASSPORT.json` section
  names covered by contract tests. BDD context/evidence/coverage export anchors
  are stable for generated artifacts. `AGENT_BRIEF.json` remains alpha schema;
- release archives, checksums, third-party notices, and recovery-oriented
  migration behavior.
- ECC installer/control-panel user-facing entrypoints: `install.sh`,
  `scripts/install-agent-adapters.sh`, and `scripts/devharness-control-panel.sh`
  commands `configure`, `plan`, `install`, `status`, `doctor`, `repair`, and
  `uninstall`. The stable-candidate promise covers documented command/option
  names, non-mutating `plan` and `--dry-run` behavior, status formats
  `text|json|markdown`, and the local `doctor`/`repair` flow. Generated
  adapter layouts and alpha local state files remain internal.

Stable candidate does not mean the CLI proves code correctness. It means the
command contracts, generated artifacts, migration behavior, and release
packaging are conservative enough for teams to build local automation around
them.

## Beta Surface

DB inspection remains beta:

- SQL safety is a guardrail, not a permission boundary.
- DevHarness Kit is a developer toolkit aimed at trusted local development
  environments. It does not force read-only database credentials.
- Use credentials that match the risk of the development environment; lower
  privilege accounts are recommended when practical.
- MySQL compatibility currently keeps Connector/J 5.1 as the default because the
  production floor includes MySQL Server 5.1.
- DB inspection remains beta even with deterministic SQL guard fuzz tests. It
  needs routinely captured, release-gated real-version compatibility evidence
  before any stable readonly claim.

## Experimental Surface

The following capabilities are useful for internal use, but they are not part
of the stable candidate promise:

- Graph-aware Goal orchestration beyond the documented Graph Lite
  stable-advisory output contract;
- Skill governance enforcement beyond the documented stable-candidate subset;
- Policy/Hook governance beyond the documented `devharness-policy/v1` local
  schema and hook behavior subset in `docs/POLICY.md`;
- ECC Control Panel internals beyond the documented stable-candidate package
  script entrypoints;
- Routine CLI, dashboards, schedulers, and cross-project replay beyond the
  documented local report schemas;
- direct SQLite table or column details.

Experimental surface may change in later releases with release notes. Generated
graph, skill, policy, and routine artifacts are local evidence and guidance, not
proof of correctness. Policy hooks are local DevHarnessKit command guards, not
sandboxing or permissions. BDD artifacts are stable acceptance context, but they
still require evidence and goal checks before they support completion.

## Release Gate Expectations

Before a stable or stable-candidate tag:

1. `scripts/release-gate.sh` must pass.
   The line coverage threshold is sourced from
   `scripts/release-thresholds.env` and is currently 60%. Lowering it requires
   a separate governance issue and explicit review; do not reduce the threshold
   to make a release pass.
2. README, `docs/COMPATIBILITY.md`, `docs/MIGRATIONS.md`, and release notes must
   agree on release channel, schema version, and stable/beta/experimental
   boundaries.
3. Release artifacts must include the shaded jar, scripts, agent packaging,
   `LICENSE`, and `THIRD_PARTY_NOTICES.md`.
4. Checksums must be generated from the packaged artifacts.
5. Migration tests must cover the documented upgrade paths, and automatic
   pre-migration backup must remain fail-closed.
6. Security-sensitive guards must have targeted tests, especially SQL safety
   and sensitive-data policy behavior.
7. `scripts/check-module-boundaries.sh` must pass so stable core packages do
   not accidentally import CLI command implementations. Known goal-to-graph,
   goal-to-BDD, and goal-to-governance debt is reported as boundary debt until
   the next physical runner split.
8. Remote CI on `main` must be green across the configured Linux, macOS,
   Windows, Java 8, and Java 17 matrix. Windows-specific smoke that cannot run
   locally may be recorded as separate CI or manual evidence.
9. README, compatibility docs, migration docs, and `dhk version` must agree on
   the current schema version. `scripts/check-version-metadata.sh` enforces
   this before packaging.

## Non-Goals For 1.0

- Do not make Graph Lite a completeness claim for code impact.
- Do not treat BDD scenarios as executable test proof without evidence.
- Do not make SQLite tables a public write API.
- Do not add network services, daemons, or cloud sync.
- Do not make DB inspection stable until SQL safety tests, real MySQL
  compatibility evidence, and development-environment risk guidance are
  release-gated together.
