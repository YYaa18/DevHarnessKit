# SQLite Migration Policy

DevHarness Kit stores project state in:

```text
.agents/memory/memory.db
```

Current schema version: `13`.

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
| v8 | Goal check workspace, context, and check fingerprints for freshness enforcement. |
| v9 | Graph Lite snapshot, file, node, edge, query cache, and goal binding tables. |
| v10 | BDD specification features, scenarios, steps, bindings, evidence, and quality issue tables. |
| v11 | Skill contract metadata for local skill governance. |
| v12 | Human checkpoint approvals for policy-controlled goal completion. |
| v13 | Skill trust source hashes and trust review status. |

## Alpha Compatibility Policy

The current beta CLI initializes and migrates project databases in place. The schema is not yet a stable public contract.
See [COMPATIBILITY.md](COMPATIBILITY.md) for the overall CLI, schema, JSON, and export contract.

When an existing non-empty database is below the current schema version, DevHarness Kit creates an automatic pre-migration backup under:

```text
.agents/memory/backups/
```

The backup filename includes the old and new schema versions, for example `pre-migration-v1-to-v13`.

Current fixture coverage includes:

```text
v1-project-minimal.sql   old project table without later metadata columns
v2-workflow-before-artifacts.sql
                         workflow templates/runs/phases/gates/events before artifact and spec tables
v4-before-goal.sql       pre-goal schema with existing spec state
v5-goal-before-step-count.sql
                         goal schema before check freshness metadata
v9-graph-before-bdd.sql  Graph Lite snapshot rows before BDD tables
v10-bdd-before-skill.sql BDD rows before skill contract tables
v11-skill-before-trust.sql
                         skill contract rows before trust source hashes
v12-human-checkpoint-before-skill-trust.sql
                         human checkpoint rows before skill trust source hashes
v13-current-minimal.sql current-version fixture used to verify idempotent
                         v13 migration without pre-migration backup churn
```

These fixtures verify that old databases are backed up before upgrade, important rows are preserved, and the current v13 schema is created.

The workflow fixture verifies that v2 `workflow_template`, `workflow_run`, `workflow_phase_run`, `workflow_gate_run`, and `workflow_event` rows survive upgrade while v3 artifact binding tables, v4 spec tables, v5 goal tables, v6 check metadata, v7 export recovery state, v8 fingerprint columns, v9 graph tables, v10 BDD tables, v11 skill contract tables, v12 human checkpoint tables, and v13 skill trust fields are added.

The spec fixture verifies that v4 `spec_change`, `spec_document`, `spec_task`, `spec_acceptance`, `workflow_spec_binding`, and `spec_event` rows survive upgrade while v5 goal tables, v6 check metadata, v7 export recovery state, v8 fingerprint columns, v9 graph tables, v10 BDD tables, v11 skill contract tables, v12 human checkpoint tables, and v13 skill trust fields are added.

The goal fixture verifies that v5 `goal_check` rows survive upgrade, receive a default `step_count_at_check = 0` value, can be extended with v8 fingerprint metadata, and receives v9 graph tables, v10 BDD tables, v11 skill contract tables, v12 human checkpoint tables, and v13 skill trust fields without losing rows.

The Graph fixture verifies that v9 `code_graph_snapshot`, `code_graph_file`, `code_graph_node`, `code_graph_edge`, `code_graph_query_cache`, and `goal_graph_binding` rows survive upgrade while BDD, skill contract, human checkpoint, and skill trust schema is added.

The BDD fixture verifies that v10 `bdd_feature`, `bdd_scenario`, `bdd_step`, `bdd_binding`, `bdd_evidence`, and `bdd_quality_issue` rows survive upgrade while skill contract, human checkpoint, and skill trust schema is added.

The skill fixture verifies that v11 `skill_contract` rows survive upgrade and receive default v13 trust columns (`source_hash`, `trusted_source_hash`, `trust_status`).

The human checkpoint fixture verifies that v12 `human_checkpoint` rows survive upgrade while existing skill contract rows receive v13 trust columns.

The current-version fixture verifies that a database already reporting schema
v13 can be migrated repeatedly without duplicate `schema_version` rows or
automatic pre-migration backup churn.

## MigrationStep Mapping

`MigrationRunner` now executes an ordered `MigrationStep` contract. The runner
keeps transaction control, pre-upgrade backup, rollback, and optional FTS setup;
each version remains isolated behind a versioned step entry.

| Version | MigrationStep description | Implementation method |
| --- | --- | --- |
| v1 | `MVP memory schema` | `MigrationRunner.migrateV1` |
| v2 | `V0.2 workflow persistence schema` | `MigrationRunner.migrateV2` |
| v3 | `V0.2-B workflow artifact binding schema` | `MigrationRunner.migrateV3` |
| v4 | `V0.3 spec persistence schema` | `MigrationRunner.migrateV4` |
| v5 | `V0.4 goal orchestration schema` | `MigrationRunner.migrateV5` |
| v6 | `V0.4 goal check freshness schema` | `MigrationRunner.migrateV6` |
| v7 | `V0.4 goal context export failure schema` | `MigrationRunner.migrateV7` |
| v8 | `V0.4 goal workflow/spec sync schema` | `MigrationRunner.migrateV8` |
| v9 | `V0.4 graph lite schema` | `MigrationRunner.migrateV9` |
| v10 | `V0.4 BDD acceptance schema` | `MigrationRunner.migrateV10` |
| v11 | `V0.4 skill contract schema` | `MigrationRunner.migrateV11` |
| v12 | `V0.4 human checkpoint schema` | `MigrationRunner.migrateV12` |
| v13 | `V0.4 skill trust hardening schema` | `MigrationRunner.migrateV13` |

Adding a future migration should add one `MigrationStep` entry, one fixture or
upgrade test, and one row in this table.

Graph Lite v9 tables are additive. They store snapshot-bound machine facts:

```text
code_graph_snapshot
code_graph_file
code_graph_node
code_graph_edge
code_graph_query_cache
goal_graph_binding
```

They are not confirmed memory, and they should not be treated as durable human
facts. See [GRAPH_SCHEMA.md](GRAPH_SCHEMA.md).

BDD v10 tables are additive alpha storage for specification-level acceptance
scenarios:

```text
bdd_feature
bdd_scenario
bdd_step
bdd_binding
bdd_evidence
bdd_quality_issue
```

They are local SQLite source-of-truth rows for `dhk bdd` commands and generated
BDD exports under `.agents/bdd/`. They do not require or imply Cucumber,
Playwright, Postman, or any other executable BDD framework.

Skill Contract v11 tables are additive alpha storage for local skill governance:

```text
skill_contract
```

They store parsed `contract.json` metadata for future `dhk skill` lint, verify,
trust, and audit commands. They do not execute scripts or mark any skill trusted
by default. See [SKILL_CONTRACT.md](SKILL_CONTRACT.md).

Human checkpoint v12 tables are additive beta storage for explicit approval steps:

```text
human_checkpoint
```

They preserve local approval decisions for policy-controlled goal completion.

Schema v13 adds source-hash trust fields to `skill_contract`:

```text
source_hash
trusted_source_hash
trust_status
```

These fields support `dhk skill trust` and make changed trusted skills visible
as `review_required` on the next `dhk skill verify`.

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

Downgrade migrations are not supported in beta patch releases. If a newer CLI migrates a
database and you need to return to an older CLI, restore the automatic
`pre-migration-*` backup or a manual `memory backup` archive created before the
upgrade.

Do not rely on direct table edits to downgrade a database. Table and column
details are not public API before 1.0.

## Export Compatibility

Markdown exports are generated context, not source of truth. If an export format changes, regenerate it from SQLite with the matching CLI version.
