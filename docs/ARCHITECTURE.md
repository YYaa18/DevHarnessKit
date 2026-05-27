# Architecture

DevHarness Kit is a local Java CLI. It does not start a daemon, expose an HTTP API, or call LLM providers.

## Logical Module Boundaries

```text
DevHarnessKit
|-- dhk-core
|   |-- memory core
|   |-- project/local SQLite migration
|   |-- sensitive policy
|   |-- goal core
|   |-- workflow/spec audit state
|   `-- doctor/status/quickstart readiness primitives
|-- dhk-db
|   `-- readonly business database inspection
|-- dhk-graph
|   `-- Graph Lite snapshots, impact maps, and graph-aware goal preflight
|-- dhk-bdd
|   `-- BDD acceptance harness and scenario evidence
|-- dhk-governance
|   `-- skill contract, policy hooks, artifact passports, checkpoints, routine
`-- dhk-cli
    `-- command router, output contracts, scripts, packaging, release archives
```

These are logical boundaries for the current beta line. The repository still
builds one Maven artifact, `dhk-cli`, and one shaded jar. Do not split Maven
modules until the preview conditions at the end of this document are met.

Expected dependency direction:

```text
dhk-cli
  -> dhk-governance
  -> dhk-bdd
  -> dhk-graph
  -> dhk-db
  -> dhk-core
```

Higher layers may depend on lower layers. Lower layers must not depend on CLI
commands or packaging scripts. Cross-cutting utilities such as JSON rendering,
path resolution, and clocks stay in core-compatible utility packages until a
future module split creates a dedicated shared package.

## Memory Core

Memory core stores durable project facts in project-local SQLite:

```text
.agents/memory/memory.db
.agents/memory/project.json
.agents/memory/exports/CURRENT_CONTEXT.md
.agents/memory/exports/RECOVERY_CONTEXT.md
.agents/memory/exports/GOAL_CONTEXT.md
.agents/memory/exports/GOAL_SUMMARY.md
.agents/memory/artifacts/
```

Important constraints:

- `memory add` creates draft memory.
- `memory confirm` is required before a fact enters default export.
- `memory export` selects confirmed memory with sufficient confidence.
- `CURRENT_CONTEXT.md` uses section budgets and emits a truncation report.
- Markdown exports are generated artifacts, not source of truth.

## DB Readonly

DB readonly is an optional inspection layer. It exists to help users verify business SQL or inspect schema while working with agents.

The SQL guard is a safety aid, not a permission boundary. Use read-only database credentials. Query results are exported to Markdown and are not automatically stored as memory.

## Goal

Goal is the high-level orchestration layer. It coordinates existing memory, workflow, and spec capabilities so users and agents do not need to manually compose lower-level commands for every task.

Goal MVP persists:

- goal runs;
- goal steps;
- goal events;
- goal checks;
- goal artifacts;
- `GOAL_CONTEXT.md`;
- `GOAL_SUMMARY.md`.

Goal is not an autonomous workflow engine. It records state, renders the next deterministic action for an agent to perform, synchronizes deterministic workflow/spec state from profile action mappings, and requires recorded checks before completion. Completion creates a memory checkpoint, closes the checkpoint workflow gate, marks the workflow completed when all phases are closed, and writes a summary export.

`GOAL_CONTEXT.md` and `GOAL_SUMMARY.md` are generated Markdown exports. They are stable enough for alpha agents and scripts to read, but SQLite goal/workflow/spec rows and the completion checkpoint remain the source of truth. Regenerate exports with the matching CLI instead of hand-editing them.

Goal metrics and replay are alpha derived models documented in [GOAL_METRICS_REPLAY.md](GOAL_METRICS_REPLAY.md). They summarize existing goal rows for future routine reporting without adding a new source of truth or public CLI surface.

## Graph Lite

Graph Lite is an alpha, local-only context layer for code relationship snapshots.
It stores snapshot-bound machine facts in SQLite v9 tables:

- `code_graph_snapshot`;
- `code_graph_file`;
- `code_graph_node`;
- `code_graph_edge`;
- `code_graph_query_cache`;
- `goal_graph_binding`.

Graph Lite is not long-term memory. A graph snapshot describes one observed
workspace state and can become stale after code changes. Agents should consume
generated graph exports such as `IMPACT_MAP.md` and `GRAPH_CONTEXT.md` when
available, not directly edit graph rows.

The default project config path is:

```text
.agents/graph/config.json
```

Generated exports and caches are local artifacts under `.agents/graph/exports/`,
`.agents/graph/snapshots/`, and `.agents/graph/cache/`. These generated
directories are ignored by git.

The schema and config contract are documented in [GRAPH_SCHEMA.md](GRAPH_SCHEMA.md).

## Routine Reporting (Planned Alpha)

Routine reporting is a planned local-only layer for summarizing repeated goal runs and CI-safe outcome artifacts. The scope and privacy boundary are documented in [ROUTINE_LOCAL_CI_EXPORT.md](ROUTINE_LOCAL_CI_EXPORT.md). There is no public `routine` command yet.

## Workflow

Workflow records development process state:

- templates;
- runs;
- phases;
- gates;
- events;
- artifacts;
- memory/checkpoint bindings.

Workflow is not a workflow engine. It persists audit state and renders short context for agents.

The alpha `WORKFLOW_CONTEXT.md` export shape is documented in [EXPORT_CONTRACTS.md](EXPORT_CONTRACTS.md).

## Spec

Spec records change state:

- proposal/design/requirements documents;
- tasks;
- acceptance criteria;
- status transitions;
- workflow bindings.

Spec Markdown exports are context artifacts. SQLite remains source of truth.

The alpha `SPEC_CONTEXT.md` export shape is documented in [EXPORT_CONTRACTS.md](EXPORT_CONTRACTS.md).

## Agent Packaging

The repository includes agent-facing packaging:

```text
.agents/skills/devharness-java-development/
.agents/skills/devharness-goal-development/
.comate/rules/
```

Wrapper scripts resolve the repository root from their own location and pass `--project-root` explicitly unless the caller already provided it. This keeps memory storage project-scoped even when an agent invokes scripts from a subdirectory.

## Current Code Layout

```text
src/main/java/com/devharnesskit/dhk/cli
src/main/java/com/devharnesskit/dhk/command
src/main/java/com/devharnesskit/dhk/db
src/main/java/com/devharnesskit/dhk/export
src/main/java/com/devharnesskit/dhk/repository
src/main/java/com/devharnesskit/dhk/service
src/main/java/com/devharnesskit/dhk/sql
src/main/java/com/devharnesskit/dhk/util
```

Commands should stay thin: parse inputs, call services, and print stable output. Repositories own SQL persistence. Services own validation and workflow rules. Renderers own Markdown output.

## Package Ownership

| Logical module | Current packages and files | Ownership notes |
| --- | --- | --- |
| `dhk-cli` | `cli`, `command`, `Main`, release assembly, shell/bat scripts | Parses arguments, routes commands, renders text/JSON, and packages the single shaded jar. |
| `dhk-core` | `db`, `db.migration`, `model`, root `repository`, `repository.goal`, `repository.spec`, `repository.workflow`, `service.goal`, `service.spec`, `service.workflow`, `service.config`, `service.checkpoint`, root `service`, `export`, `util` | Owns local source-of-truth state, migration, memory/context exports, goal orchestration, workflow/spec audit layers, status/readiness primitives, and sensitive policy. |
| `dhk-db` | `command.projectdb`, `sql`, DB-facing services inside root `service` | Owns readonly business DB connection, SQL safety guard, query rendering, and DB risk notices. It should depend on core policy/sensitive utilities only. |
| `dhk-graph` | `command.graph`, `model.graph`, `repository.graph`, `service.graph` | Owns Graph Lite config, index, snapshot persistence, impact maps, prune, and graph-aware goal preflight/check evidence. |
| `dhk-bdd` | `command.bdd`, `model.bdd`, `repository.bdd`, `service.bdd` | Owns feature/scenario/evidence storage and BDD exports/checks. |
| `dhk-governance` | `command.skill`, `command.artifact`, `command.checkpoint`, `model.policy`, `model.skill`, `repository.skill`, `service.policy`, `service.skill`, artifact passport/checkpoint services | Owns skill contract/trust, policy hooks, human checkpoint, artifact passport, and future routine/reporting rules. |

## Known Boundary Debt

- `GoalCheckService` still coordinates checks from graph, BDD, governance, workflow, spec, and command execution. AI-151 introduced `GoalCheckRunner` and a static registry, but the physical check implementations still live in `service.goal`. Before Maven modules, graph/BDD/governance checks should move behind module-owned runners.
- `MigrationRunner` now has a `MigrationStep` contract and versioned methods, but all schema SQL still lives in one class. Before a module split, schema ownership should be documented per step and future steps should be introduced as small migration classes.
- Root `service` still contains cross-module helpers such as sensitive guard, SQL guard, and backup service. A future split should decide whether these stay in `dhk-core` or move to smaller shared internal packages.
- `command` is a single tree containing all command families. This is acceptable while the jar is single-artifact, but Maven preview should keep command packages in `dhk-cli` and move only implementation services/repositories into module artifacts.
- `model.Project` and several generic repositories are shared by all domains. This should remain core until there is a stable public persistence contract.

## Multi-Module Preview Plan

Phase 0, current beta:

- Keep one `pom.xml`, one shaded jar, one release archive, and current install scripts.
- Maintain logical package ownership in this document.
- Continue reducing large cross-domain services (`GoalCheckService`, `MigrationRunner`) before moving files.

Phase 1, logical boundary hardening:

- Move graph/BDD/governance check implementations behind module-owned `GoalCheckRunner` classes.
- Keep migration steps small and version-addressable.
- Add a package dependency report to CI before changing Maven modules.
- Keep `scripts/devharness-control-panel.sh`, generated adapters, and release archive paths unchanged.

Phase 2, Maven multi-module preview:

- Introduce modules without changing the published CLI entrypoint:
  `dhk-core`, `dhk-db`, `dhk-graph`, `dhk-bdd`, `dhk-governance`, and `dhk-cli`.
- Keep `dhk-cli` as the only packaged shaded jar in preview.
- Run the existing integration suite against the assembled `dhk-cli` artifact.
- Do not introduce core/full distribution until install scripts and adapter paths understand both forms.

Phase 3, optional core/full distribution:

- `dhk-core.jar` may exclude readonly DB, Graph Lite, BDD, and governance extras only after README, release archives, wrapper scripts, and third-party notices describe the split.
- `dhk-full.jar` remains the compatibility artifact for existing users.

## Release And Adapter Risks

- Generated agent adapters expect a single `dhk.jar` or `target/dhk-cli-*-all.jar`.
- Release archives currently include one CLI jar plus scripts, licenses, and notices.
- Splitting artifacts too early would require installer, wrapper, docs, CI, and `THIRD_PARTY_NOTICES.md` updates in the same release.
- MySQL 5.1 compatibility is intentionally retained for the user's mixed local/company environment; any future `dhk-db` module must preserve that compatibility unless the release plan says otherwise.
