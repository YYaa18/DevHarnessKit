# Compatibility Contract

Current release channel: `0.1.0-alpha` developer preview.

This document defines what users and scripts may cautiously depend on before
1.0, and what remains experimental. It is a contract for release notes and
documentation, not a promise that every current behavior is stable.

## Stability Levels

| Level | Meaning |
| --- | --- |
| Stable-ish alpha | Intended to change conservatively across `0.1.x`; breaking changes require a changelog note and migration guidance. |
| Beta | Useful for real workflows, but still has safety, environment, or dependency constraints that users must review. |
| Alpha | Usable for developer preview feedback. Shape may change before 1.0 with documented release notes. |
| Internal | Historical plans, implementation notes, tests, and helper internals. Do not build automation against these. |

## Command Surface

Stable-ish alpha:

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

Beta:

- `dhk db test`
- `dhk db sql`
- `dhk db sql --dry-run`
- `dhk db sql --explain`
- `dhk db sql --format json`

Alpha:

- `dhk goal start`
- `dhk goal resume`
- `dhk goal next`
- `dhk goal step`
- `dhk goal status`
- `dhk goal export`
- `dhk goal check`
- `dhk goal evaluate`
- `dhk goal complete`
- `dhk workflow ...`
- `dhk spec ...`
- agent skill/rule packaging under `.agents/` and `.comate/`

Human-readable stdout is intended for people and examples. Do not parse it as a
stable API unless a document explicitly names the fields and command as a
machine-readable contract.

## CLI Input Contract

Scripts may rely on these parser behaviors in `0.1.x`:

- long options in `--key value` and `--key=value` form;
- boolean flags such as `--json`, `--dry-run`, `--explain`, and `--all`;
- file/stdin inputs documented in [CLI_INPUTS.md](CLI_INPUTS.md);
- explicit `--project-root` for project-scoped commands.

Unsupported parser behavior should not be treated as a bug-compatible contract,
including short option aliases, shell-specific quoting behavior, and parsing
human-readable output.

## JSON Output Contract

JSON output is alpha, but scripts may depend on:

- valid JSON on stdout for commands listed in [JSON_OUTPUT.md](JSON_OUTPUT.md);
- existing required field names changing conservatively in `0.1.x`;
- additive fields appearing without a breaking-change label;
- non-zero exit codes still being meaningful alongside JSON status fields.

Consumers should ignore unknown fields and use exit codes plus stable status
fields rather than exact formatting or field order.

## Markdown Export Contract

SQLite is the source of truth. Markdown files under `.agents/memory/exports/`
are generated context artifacts.

Stable-ish alpha exports:

- `CURRENT_CONTEXT.md`
- `RECOVERY_CONTEXT.md`

Alpha exports:

- `GOAL_CONTEXT.md`
- `GOAL_SUMMARY.md`
- `WORKFLOW_CONTEXT.md`
- `SPEC_CONTEXT.md`
- `SQL_RESULT.md`

Export section names and order documented in [EXPORT_CONTRACTS.md](EXPORT_CONTRACTS.md)
should change only with a changelog note. New fields or sections may be added
before 1.0 when they do not invalidate existing readers that ignore unknown
content.

Do not hand-edit generated Markdown to change durable state. Regenerate exports
with the matching CLI version.

## SQLite Schema Contract

Current schema version: `6`.

Before 1.0, the schema is not a stable public API. Users may rely on these
operational guarantees:

- DevHarness Kit treats `.agents/memory/memory.db` as source of truth.
- Older non-empty databases are backed up before an in-place migration.
- Fixture-tested upgrade paths are documented in [MIGRATIONS.md](MIGRATIONS.md).
- Commands should fail closed if a required backup cannot be created.
- Markdown exports can be regenerated from SQLite with the matching CLI.

Unsupported:

- direct third-party writes to `memory.db`;
- downgrade migrations;
- modifying generated exports and expecting SQLite state to change;
- relying on table or column details as public API before 1.0.

If downgrade support is added later, it must be explicit in release notes and
migration docs. Until then, recover by restoring a pre-migration backup.

## DB Readonly Contract

DB readonly remains beta because it depends on real database versions, driver
behavior, credentials, permissions, schemas, views, and server configuration.

The compatibility floor is documented in [DB_COMPATIBILITY.md](DB_COMPATIBILITY.md):

- production floor: MySQL Server 5.1;
- local target: MySQL Server 8.x with Connector/J 5.1-compatible account and URL;
- default driver: `mysql:mysql-connector-java:5.1.49`.

The SQL guard and JDBC read-only hint are guardrails, not a database permission
boundary. Always use database-level read-only credentials.

## Deprecation Policy

Before 1.0:

- stable-ish alpha command changes require a changelog note;
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
- command families that are stable-ish alpha, beta, or alpha;
- JSON/export contract changes;
- DB driver and MySQL compatibility constraints;
- any breaking changes, deprecations, or compatibility aliases.
