# SQLite Migration Policy

DevHarness Kit stores project state in:

```text
.agents/memory/memory.db
```

Current schema version: `7`.

## Current Schema Versions

| Version | Scope |
| --- | --- |
| v1 | Memory core project, memory, checkpoint, and indexes. |
| v2 | Workflow templates, runs, phases, gates, and events. |
| v3 | Workflow artifacts, memory bindings, and checkpoint bindings. |
| v4 | Spec changes, documents, tasks, acceptance criteria, events, and workflow bindings. |
| v5 | Goal orchestration runs, steps, events, checks, and artifacts. |
| v6 | Goal check freshness metadata with `goal_check.step_count_at_check`. |
| v7 | Goal context export recovery statuses for resumable failed exports. |

## Alpha Compatibility Policy

`0.1.0-alpha` initializes and migrates project databases in place. The schema is not yet a stable public contract.
See [COMPATIBILITY.md](COMPATIBILITY.md) for the overall CLI, schema, JSON, and export contract.

When an existing non-empty database is below the current schema version, DevHarness Kit creates an automatic pre-migration backup under:

```text
.agents/memory/backups/
```

The backup filename includes the old and new schema versions, for example `pre-migration-v1-to-v7`.

Current fixture coverage includes:

```text
v1-project-minimal.sql   old project table without later metadata columns
v2-workflow-before-artifacts.sql
                         workflow templates/runs/phases/gates/events before artifact and spec tables
v4-before-goal.sql       pre-goal schema with existing spec state
v5-goal-before-step-count.sql
                         goal schema before check freshness metadata
```

These fixtures verify that old databases are backed up before upgrade, important rows are preserved, and the current v7 schema is created.

The workflow fixture verifies that v2 `workflow_template`, `workflow_run`, `workflow_phase_run`, `workflow_gate_run`, and `workflow_event` rows survive upgrade while v3 artifact binding tables, v4 spec tables, v5 goal tables, and v6 check metadata are added.

The spec fixture verifies that v4 `spec_change`, `spec_document`, `spec_task`, `spec_acceptance`, `workflow_spec_binding`, and `spec_event` rows survive upgrade while v5 goal tables and v6 check metadata are added.

The goal fixture verifies that v5 `goal_check` rows survive upgrade and receive a default `step_count_at_check = 0` value.

For older v1 project tables, migration repairs missing project metadata columns such as `root_path`, `language`, `framework`, and `database_type` with conservative defaults.

Before using DevHarness Kit on important project data:

1. Commit or back up `.agents/memory/`.
2. Run `dhk doctor --project-root <path>`.
3. Run the intended command.
4. Re-run `dhk doctor --project-root <path>`.

## Recovery Guidance

If migration or initialization fails:

1. Stop running further write commands.
2. Check whether `.agents/memory/backups/` contains a `pre-migration-*` zip.
3. Copy `.agents/memory/memory.db`, `.agents/memory/project.json`, and any `pre-migration-*` zip to a safe location.
4. Do not delete the original database until the backup zip has been inspected.
5. If workflow or spec commands fail after migration, run `dhk doctor`, then inspect whether workflow/spec rows are present before retrying write commands.
6. Re-run with the latest patched CLI.
7. If the database is still not usable, open an issue with the CLI version, schema version, command, and error output.

If DevHarness Kit cannot create the pre-migration backup, migration fails before applying schema changes. This is intentional: an older database should not be upgraded without a recoverable copy.

## Built-In Backup

Use `memory backup` before risky upgrades:

```bash
dhk memory backup --project-root .
dhk memory backup --project-root . --out /tmp/dhk-memory-backup.zip
```

The backup zip includes `project.json`, `memory.db`, and files directly under `exports/`.

## Rules for Future Migrations

Future schema changes should follow these rules before a stable release:

- each migration has a clear version and description;
- migrations run inside a transaction where SQLite supports it;
- important user databases are backed up before destructive or risky changes;
- old database fixtures are tested in CI;
- migrations are forward-compatible within supported minor versions;
- downgrades are documented as unsupported unless explicitly implemented.

## Downgrade Stance

Downgrade migrations are not supported in `0.1.x`. If a newer CLI migrates a
database and you need to return to an older CLI, restore the automatic
`pre-migration-*` backup or a manual `memory backup` archive created before the
upgrade.

Do not rely on direct table edits to downgrade a database. Table and column
details are not public API before 1.0.

## Export Compatibility

Markdown exports are generated context, not source of truth. If an export format changes, regenerate it from SQLite with the matching CLI version.
