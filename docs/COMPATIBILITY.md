# Compatibility Contract

Current release channel: stable. The artifact version is defined by Maven
`project.version` and printed by `dhk version`.

This document defines what users and scripts may depend on for the 1.0 stable
surface, and what remains beta or experimental. It is a contract for release
notes and documentation, not a promise that every bundled behavior is stable.

See [STABLE_CANDIDATE.md](STABLE_CANDIDATE.md) for the narrower
stable-candidate boundary. The stable-candidate track is intentionally smaller
than the full current command surface.

## Stability Levels

| Level | Meaning |
| --- | --- |
| Stable | Part of the 1.0 public contract. Breaking changes follow semantic versioning and documented deprecation windows. |
| Stable-advisory | Output shape and operational semantics are stable, but the result is advisory and must not be treated as proof of correctness. Breaking output changes follow the Stable rule; precision changes may happen when release notes and regression gates explain them. |
| Beta | Useful for real workflows, but outside the stable contract. Breaking changes require a changelog note and migration guidance. Some beta areas still have safety, environment, or dependency constraints that users must review. |
| Alpha | Usable for developer feedback. Shape may change with documented release notes. |
| Internal | Historical plans, implementation notes, tests, and helper internals. Do not build automation against these. |

## Command Surface

Stable core:

- `dhk help`
- `dhk version`
- `dhk doctor`
- `dhk memory init`
- `dhk memory add`
- `dhk memory confirm`
- `dhk memory search`
- `dhk memory export`
- `dhk memory checkpoint`
- `dhk memory recover`
- `dhk memory backup`

Stable:

- `dhk configure init`
- `dhk configure show`
- `dhk configure doctor`
- `dhk configure explain`
- `dhk status`
- `dhk readiness`
- `dhk advise`
- `dhk quickstart`
- `dhk goal start`
- `dhk goal resume`
- `dhk goal next`
- `dhk goal step`
- `dhk goal status`
- `dhk goal export`
- `dhk goal verify`
- `dhk goal complete`
- `dhk goal audit`
- `dhk goal recheck`
- `dhk workflow start`
- `dhk workflow status`
- `dhk workflow export`
- `dhk workflow summary`
- `dhk spec create`
- `dhk spec status`
- `dhk spec export`
- `dhk spec bind-workflow`
- `dhk bdd init`
- `dhk bdd add`
- `dhk bdd scenario create`
- `dhk bdd scenario list`
- `dhk bdd scenario show`
- `dhk bdd list`
- `dhk bdd show`
- `dhk bdd export`
- `dhk bdd lint`
- `dhk bdd evidence add`
- `dhk bdd evidence junit`
- `dhk bdd evidence report`
- `dhk bdd verify`
- `dhk bdd coverage`
- `dhk bdd bind-spec`
- `dhk bdd bind-goal`
- `dhk bdd bind-workflow`
- `dhk bdd bind-graph`
- `dhk bdd bind-test`

Stable-advisory:

- `dhk graph status`
- `dhk graph index`
- `dhk graph impact`
- `dhk graph export`

Stable-candidate governance command surface:

- `dhk skill lint`
- `dhk skill verify`
- `dhk skill trust`
- `dhk skill audit`
- `dhk skill score`
- `dhk skill report`

Stable-candidate reporting command surface:

- `dhk goal retrospective`
- `dhk goal review-summary`
- `dhk goal mr-summary`

Stable-candidate package script entrypoints:

- `install.sh`
- `scripts/install-agent-adapters.sh`
- `scripts/devharness-control-panel.sh configure`
- `scripts/devharness-control-panel.sh plan`
- `scripts/devharness-control-panel.sh install`
- `scripts/devharness-control-panel.sh status`
- `scripts/devharness-control-panel.sh doctor`
- `scripts/devharness-control-panel.sh repair`
- `scripts/devharness-control-panel.sh uninstall`

The ECC Control Panel stable-candidate surface covers documented command and
option names, `plan` and `--dry-run` non-mutation behavior, status formats
`text`, `json`, and `markdown`, and the local `doctor`/`repair` flow. It does
not make generated adapter layouts, generated rule contents, `.agents/`,
`.claude/`, `.comate/`, `devharness-config/v1-alpha`,
`devharness-agent-manifest/v1-alpha`, or `devharness-install-state/v1-alpha`
stable APIs.

Beta:

- `dhk db test`
- `dhk db sql`
- `dhk db sql --dry-run`
- `dhk db sql --explain`
- `dhk db sql --format json`

Alpha:

- `dhk goal check`
- `dhk goal evaluate`
- `dhk graph init`
- `dhk graph doctor`
- `dhk graph prune`
- `dhk skill gate ...`
- `dhk checkpoint ...`
- `dhk artifact passport verify`
- non-stable `dhk workflow ...` subcommands outside `start/status/export/summary`
- non-stable `dhk spec ...` subcommands outside `create/status/export/bind-workflow`
- agent skill/rule packaging under `.agents/` and `.comate/`
- skill contract metadata under `.agents/skills/<skill>/contract.json`

Internal/experimental:

- Skill governance enforcement beyond the documented stable-candidate
  lint/verify/trust/audit/score/report contracts is experimental and introduced
  in stages.
- Graph-aware Goal, full Policy/Hook governance, and public Routine CLI remain
  outside the first stable-candidate surface even when their commands are useful
  for internal dogfood. Routine local report schemas are stable-candidate only
  through generated Markdown/JSON/NDJSON helpers.
- Internal ECC adapter packaging details beyond the documented package script
  entrypoints, including generated file contents and private helper structure.
- Graph Lite SQLite tables, `.agents/graph/config.json`, snapshot rows,
  provider adapter details, precision thresholds, ranking algorithms, and raw
  node/edge scoring internals. Stable-advisory consumers should use the CLI and
  generated exports documented in [STABLE_CONTRACT.md](STABLE_CONTRACT.md),
  [GRAPH_CONTEXT_LAYER.md](GRAPH_CONTEXT_LAYER.md), and
  [GRAPH_LIMITS.md](GRAPH_LIMITS.md).

Human-readable stdout is intended for people and examples. Do not parse it as a
stable API unless a document explicitly names the fields and command as a
machine-readable contract.

## CLI Input Contract

Scripts may rely on these parser behaviors in beta patch releases:

- long options in `--key value` and `--key=value` form;
- boolean flags such as `--json`, `--dry-run`, `--explain`, and `--all`;
- file/stdin inputs documented in [CLI_INPUTS.md](CLI_INPUTS.md);
- explicit `--project-root` for project-scoped commands.

Unsupported parser behavior should not be treated as a bug-compatible contract,
including short option aliases, shell-specific quoting behavior, and parsing
human-readable output.

## JSON Output Contract

JSON output remains conservative. The minimum stable JSON fields for stable
commands are listed in [STABLE_CONTRACT.md](STABLE_CONTRACT.md). Other JSON
output is alpha, but scripts may depend on:

- valid JSON on stdout for commands listed in [JSON_OUTPUT.md](JSON_OUTPUT.md);
- existing required field names changing conservatively in beta patch releases;
- additive fields appearing without a breaking-change label;
- non-zero exit codes still being meaningful alongside JSON status fields.

Graph Lite JSON output is stable-advisory for `graph status`, `graph impact`,
and `graph export`. Scripts may depend on the documented field names and
freshness flags, but they must not treat `related_files`, `recommended_read_files`,
or `risk_nodes` as a complete or authoritative impact set.

Skill governance JSON output is stable-candidate for `skill lint`,
`skill verify`, `skill trust`, `skill audit`, `skill score`, and
`skill report`. Scripts may depend on the documented field names in
[SKILL_CONTRACT.md](SKILL_CONTRACT.md), [SKILL_EVALUATION.md](SKILL_EVALUATION.md),
and [JSON_OUTPUT.md](JSON_OUTPUT.md), but must not treat trust or score output
as a sandbox, permission boundary, or proof of implementation correctness.

DB inspection JSON output is beta for `db test --json`,
`db sql --dry-run --json`, `db sql --dry-run --format json`, and
`db sql --format json`. Scripts may depend on the documented minimum fields and
exit semantics in [JSON_OUTPUT.md](JSON_OUTPUT.md), but must not treat the SQL
guard or JDBC read-only hint as a database permission boundary.

Consumers should ignore unknown fields and use exit codes plus stable status
fields rather than exact formatting or field order.

## Markdown Export Contract

SQLite is the source of truth. Markdown files under `.agents/memory/exports/`
are generated context artifacts.

Beta exports:

- `CURRENT_CONTEXT.md`
- `RECOVERY_CONTEXT.md`
- `GOAL_CONTEXT.md`
- `GOAL_SUMMARY.md`
- `ARTIFACT_PASSPORT.json`
- `WORKFLOW_CONTEXT.md`
- `SPEC_CONTEXT.md`
- BDD exports under `.agents/bdd/exports/` including `BDD_CONTEXT.md`,
  `BDD_EVIDENCE.md`, `BDD_COVERAGE.md`, and `SCENARIO_IMPACT_MAP.md` when
  generated from stable BDD commands. `SCENARIO_IMPACT_MAP.md` keeps Graph Lite
  impact semantics advisory.
- Graph Lite stable-advisory exports under `.agents/graph/exports/`:
  `GRAPH_INDEX_REPORT.md`, `GRAPH_CONTEXT.md`, `GRAPH_SNAPSHOT.json`, and
  `IMPACT_MAP.md`.

Alpha exports:

- `SQL_RESULT.md`

Export section names and order documented in [EXPORT_CONTRACTS.md](EXPORT_CONTRACTS.md)
should change only with a changelog note. New fields or sections may be added
when they do not invalidate existing readers that ignore unknown content.

Do not hand-edit generated Markdown to change durable state. Regenerate exports
with the matching CLI version.

## SQLite Schema Contract

Current schema version: `16`.

The SQLite database is an internal storage format, not a public write API.
Users may rely on these operational guarantees:

- DevHarness Kit treats `.agents/memory/memory.db` as source of truth.
- Older non-empty databases are backed up before an in-place migration.
- Fixture-tested upgrade paths are documented in [MIGRATIONS.md](MIGRATIONS.md).
- Commands should fail closed if a required backup cannot be created.
- Markdown exports can be regenerated from SQLite with the matching CLI.
- Graph Lite rows are snapshot-bound machine facts, not long-term memory facts.
- BDD rows are acceptance-specification facts for stable BDD commands. Direct
  table access remains unsupported. They are not executable test results unless
  evidence rows explicitly bind them to adapter output.
- Skill contract rows are governance metadata. They do not execute skill
  scripts. `dhk skill verify` stores source hashes; only `dhk skill trust`
  pins a trusted source hash, and later source changes are marked
  `review_required`. Direct reads and writes remain unsupported; use CLI output
  contracts instead.

Unsupported:

- direct third-party writes to `memory.db`;
- downgrade migrations;
- modifying generated exports and expecting SQLite state to change;
- relying on table or column details as public API.
- treating graph snapshots as confirmed project memory.
- treating Graph Lite output as complete static analysis, test proof, or a
  safety/security boundary.
- treating BDD scenario rows as proof that behavior is implemented without
  passing evidence, coverage, or goal checks.
- treating Skill Contract trust status or Skill Evaluation scores as proof that
  a skill is safe, sandboxed, or behaviorally correct.

If downgrade support is added later, it must be explicit in release notes and
migration docs. Until then, recover by restoring a pre-migration backup.

## DB Inspection Contract

DB inspection remains beta because it depends on real database versions, driver
behavior, credentials, permissions, schemas, views, and server configuration.

The compatibility floor is documented in [DB_COMPATIBILITY.md](DB_COMPATIBILITY.md):

- production floor: MySQL Server 5.1;
- local target: MySQL Server 8.x with Connector/J 5.1-compatible account and URL;
- default driver: `mysql:mysql-connector-java:5.1.49`.

[DB_COMPATIBILITY.md](DB_COMPATIBILITY.md) also defines the MySQL 5.1/8
compatibility matrix and evidence classification. Fixture/unit tests, SQLite
probe tests, and dry-run SQL tests are not real MySQL 5.1 or MySQL 8 evidence.
Live compatibility evidence must come from `dhk db test` or the optional
`DHK_TEST_MYSQL_*` smoke path against a named server/account/URL combination.

The SQL guard and JDBC read-only hint are guardrails, not a database permission
boundary. DevHarness Kit is a developer toolkit for trusted local/personal
development environments, so it does not force read-only database credentials.
Use credentials that match your environment risk; lower privileges are
recommended when practical.

## Deprecation Policy

Before 1.0:

- beta command changes require a changelog note;
- beta/alpha command changes require a changelog note when they affect scripts,
  export shapes, or persisted data;
- compatibility aliases should be kept for at least one minor release when
  practical;
- removals should include replacement guidance;
- docs must be updated in the same change as the behavior change.

For 1.0 and later stable releases:

- public stable command/output/schema changes follow semantic versioning;
- removals require at least one minor release of deprecation notice when
  practical;
- stable JSON fields and Markdown anchors should remain additive within a major
  version;
- schema migrations must document supported upgrade paths before release.

## Release Notes Checklist

Each public release should state:

- release channel and stability level;
- current schema version;
- supported migration paths and downgrade stance;
- command families that are beta, alpha, or internal;
- JSON/export contract changes;
- DB driver and MySQL compatibility constraints;
- any breaking changes, deprecations, or compatibility aliases.
