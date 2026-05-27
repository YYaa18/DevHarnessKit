# Compatibility Contract

Current release channel: beta developer preview. The artifact version is defined
by Maven `project.version` and printed by `dhk version`.

This document defines what users and scripts may cautiously depend on before
1.0, and what remains experimental. It is a contract for release notes and
documentation, not a promise that every current behavior is stable.

See [STABLE_CANDIDATE.md](STABLE_CANDIDATE.md) for the narrower
stable-candidate boundary. The stable-candidate track is intentionally smaller
than the full current command surface.

## Stability Levels

| Level | Meaning |
| --- | --- |
| Beta | Useful for real workflows before 1.0; breaking changes require a changelog note and migration guidance. Some beta areas still have safety, environment, or dependency constraints that users must review. |
| Alpha | Usable for developer preview feedback. Shape may change before 1.0 with documented release notes. |
| Internal | Historical plans, implementation notes, tests, and helper internals. Do not build automation against these. |

## Command Surface

Beta core:

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

Stable-candidate beta:

- `dhk configure init`
- `dhk configure show`
- `dhk configure doctor`
- `dhk configure explain`
- `dhk status`
- `dhk readiness`
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
- `dhk graph status`
- `dhk graph index`
- `dhk graph impact`
- `dhk graph export`
- `dhk graph prune`
- `dhk bdd init`
- `dhk bdd add`
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
- `dhk bdd bind-graph`
- `dhk bdd bind-test`
- `dhk skill lint`
- `dhk skill verify`
- `dhk skill trust`
- `dhk skill audit`
- `dhk skill score`
- `dhk skill report`
- `dhk skill gate ...`
- `dhk checkpoint ...`
- `dhk artifact passport verify`
- `dhk workflow ...`
- `dhk spec ...`
- agent skill/rule packaging under `.agents/` and `.comate/`
- skill contract metadata under `.agents/skills/<skill>/contract.json`

Internal/experimental:

- Skill contract enforcement commands are experimental and introduced in stages.
- Graph-aware Goal, BDD acceptance, Policy/Hook governance, Skill Evaluation,
  Routine, and ECC Control Panel remain outside the first stable-candidate
  surface even when their commands are useful for internal dogfood.
- Graph Lite SQLite tables, `.agents/graph/config.json`, snapshot rows, and
  generated graph exports beyond `GRAPH_INDEX_REPORT.md`, `IMPACT_MAP.md`,
  `GRAPH_CONTEXT.md`, and `GRAPH_SNAPSHOT.json`.
- Graph Lite limits, skipped metadata, and truncation semantics are alpha and
  documented in [GRAPH_LIMITS.md](GRAPH_LIMITS.md).

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

JSON output remains conservative before 1.0. The minimum stable-beta JSON fields
for stable-beta commands are listed in [STABLE_CONTRACT.md](STABLE_CONTRACT.md).
Other JSON output is alpha, but scripts may depend on:

- valid JSON on stdout for commands listed in [JSON_OUTPUT.md](JSON_OUTPUT.md);
- existing required field names changing conservatively in beta patch releases;
- additive fields appearing without a breaking-change label;
- non-zero exit codes still being meaningful alongside JSON status fields.

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

Alpha exports:

- `WORKFLOW_CONTEXT.md`
- `SPEC_CONTEXT.md`
- `SQL_RESULT.md`
- BDD exports under `.agents/bdd/exports/` including `BDD_CONTEXT.md`,
  `BDD_EVIDENCE.md`, `BDD_COVERAGE.md`, and `SCENARIO_IMPACT_MAP.md` once the
  BDD command surface is enabled.

Export section names and order documented in [EXPORT_CONTRACTS.md](EXPORT_CONTRACTS.md)
should change only with a changelog note. New fields or sections may be added
before 1.0 when they do not invalidate existing readers that ignore unknown
content.

Do not hand-edit generated Markdown to change durable state. Regenerate exports
with the matching CLI version.

## SQLite Schema Contract

Current schema version: `13`.

Before 1.0, the schema is not a stable public API. Users may rely on these
operational guarantees:

- DevHarness Kit treats `.agents/memory/memory.db` as source of truth.
- Older non-empty databases are backed up before an in-place migration.
- Fixture-tested upgrade paths are documented in [MIGRATIONS.md](MIGRATIONS.md).
- Commands should fail closed if a required backup cannot be created.
- Markdown exports can be regenerated from SQLite with the matching CLI.
- Graph Lite rows are snapshot-bound machine facts, not long-term memory facts.
- BDD rows are alpha acceptance-specification facts. They are not executable
  test results unless evidence rows explicitly bind them to adapter output.
- Skill contract rows are alpha governance metadata. They do not execute skill
  scripts. `dhk skill verify` stores source hashes; only `dhk skill trust`
  pins a trusted source hash, and later source changes are marked
  `review_required`.

Unsupported:

- direct third-party writes to `memory.db`;
- downgrade migrations;
- modifying generated exports and expecting SQLite state to change;
- relying on table or column details as public API before 1.0.
- treating graph snapshots as confirmed project memory.
- treating BDD scenario rows as proof that behavior is implemented without
  passing evidence, coverage, or goal checks.

If downgrade support is added later, it must be explicit in release notes and
migration docs. Until then, recover by restoring a pre-migration backup.

## DB Inspection Contract

DB inspection remains beta because it depends on real database versions, driver
behavior, credentials, permissions, schemas, views, and server configuration.

The compatibility floor is documented in [DB_COMPATIBILITY.md](DB_COMPATIBILITY.md):

- production floor: MySQL Server 5.1;
- local target: MySQL Server 8.x with Connector/J 5.1-compatible account and URL;
- default driver: `mysql:mysql-connector-java:5.1.49`.

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

After 1.0, deprecation windows and semantic versioning rules should be tightened
before any stable claim is made.

## Release Notes Checklist

Each public release should state:

- release channel and stability level;
- current schema version;
- supported migration paths and downgrade stance;
- command families that are beta, alpha, or internal;
- JSON/export contract changes;
- DB driver and MySQL compatibility constraints;
- any breaking changes, deprecations, or compatibility aliases.
