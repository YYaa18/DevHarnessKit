# Stable Candidate Contract

DevHarness Kit is not stable yet. This document defines the intended
stable-candidate boundary so beta releases can converge without expanding the
public promise by accident.

## Stable Candidate Surface

The first stable-candidate track should be limited to:

- `dhk help` and `dhk version`;
- `dhk doctor`;
- memory core commands: `init`, `add`, `confirm`, `search`, `export`,
  `checkpoint`, `recover`, and `backup`;
- goal core commands: `start`, `resume`, `next`, `step`, `status`, `export`,
  `verify`, `complete`, `audit`, and `recheck`;
- generated `CURRENT_CONTEXT.md`, `RECOVERY_CONTEXT.md`, `GOAL_CONTEXT.md`,
  `GOAL_SUMMARY.md`, and `ARTIFACT_PASSPORT.json` section names covered by
  contract tests;
- release archives, checksums, third-party notices, and recovery-oriented
  migration behavior.

Stable candidate does not mean the CLI proves code correctness. It means the
command contracts, generated artifacts, migration behavior, and release
packaging are conservative enough for teams to build local automation around
them.

## Beta Surface

DB readonly remains beta:

- SQL safety is a guardrail, not a permission boundary.
- Production use must rely on database-level read-only credentials.
- MySQL compatibility currently keeps Connector/J 5.1 as the default because the
  production floor includes MySQL Server 5.1.
- DB readonly cannot move beyond beta until deterministic SQL guard fuzz tests
  and real-version compatibility checks are stronger.

## Experimental Surface

The following capabilities are useful for internal use, but they are not part
of the stable candidate promise:

- Graph Lite and Graph-aware Goal;
- BDD acceptance harness;
- Skill Contract, Skill Evaluation, and Skill Trust;
- Policy/Hook governance;
- ECC Control Panel;
- Routine, metrics, and replay;
- direct SQLite table or column details.

Experimental surface may change before 1.0 with release notes. Generated graph,
BDD, skill, policy, and routine artifacts are local evidence and guidance, not
proof of correctness.

## Release Gate Expectations

Before a stable-candidate tag:

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
6. Security-sensitive guards must have targeted tests, especially SQL readonly
   and sensitive-data policy behavior.
7. `scripts/check-module-boundaries.sh` must pass so stable core packages do
   not accidentally import CLI command implementations. Known goal-to-graph,
   goal-to-BDD, and goal-to-governance debt is reported as boundary debt until
   the next physical runner split.

## Non-Goals Before 1.0

- Do not make Graph Lite a completeness claim for code impact.
- Do not treat BDD scenarios as executable test proof without evidence.
- Do not make SQLite tables a public write API.
- Do not add network services, daemons, or cloud sync.
- Do not make DB readonly stable until database permissions, SQL safety tests,
  and compatibility checks are stronger.
