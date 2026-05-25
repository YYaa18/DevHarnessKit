# Architecture

DevHarness Kit is a local Java CLI. It does not start a daemon, expose an HTTP API, or call LLM providers.

## Module Boundaries

```text
DevHarnessKit
|-- memory core
|-- db readonly
|-- goal
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

Goal is not an autonomous workflow engine. It records state, renders the next deterministic action for an agent to perform, and requires recorded checks before completion. Completion creates a memory checkpoint and a summary export.

`GOAL_CONTEXT.md` and `GOAL_SUMMARY.md` are generated Markdown exports. They are stable enough for alpha agents and scripts to read, but SQLite goal/workflow/spec rows and the completion checkpoint remain the source of truth. Regenerate exports with the matching CLI instead of hand-editing them.

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

## Spec

Spec records change state:

- proposal/design/requirements documents;
- tasks;
- acceptance criteria;
- status transitions;
- workflow bindings.

Spec Markdown exports are context artifacts. SQLite remains source of truth.

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
