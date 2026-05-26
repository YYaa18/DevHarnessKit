# Architecture

DevHarness Kit is a local Java CLI. It does not start a daemon, expose an HTTP API, or call LLM providers.

## Module Boundaries

```text
DevHarnessKit
|-- memory core
|-- db readonly
|-- goal
|-- graph lite
|-- workflow
|-- spec
`-- agent packaging
```

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
