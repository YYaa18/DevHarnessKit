# Graph Context Layer

Graph Lite exports are a short-context layer for agents. They are generated from
the latest completed graph snapshot and are not long-term confirmed memory.

## Contract

```text
.agents/graph/config.json
.agents/graph/exports/GRAPH_INDEX_REPORT.md
.agents/graph/exports/GRAPH_CONTEXT.md
.agents/graph/exports/GRAPH_SNAPSHOT.json
.agents/graph/exports/IMPACT_MAP.md
```

Rules:

- SQLite graph tables remain the source of truth for graph snapshots.
- Markdown and JSON graph exports are derived artifacts.
- Agents should read `GRAPH_CONTEXT.md` and task-specific `IMPACT_MAP.md`, not
  `memory.db`.
- Graph facts are snapshot-bound machine facts. They must not be promoted to
  confirmed memory without human review.
- Regenerate graph exports after code changes.

## Safety Boundary

Graph Lite is best-effort static indexing. It does not provide full AST parsing,
type resolution, runtime call tracing, or security proof.

Sensitive and protected file content is not copied into graph exports. The
scanner may record skipped metadata such as path, size, language, file kind, and
skip reason so users can see what was omitted.

## Agent Use

For graph-aware work, the expected flow is:

```text
dhk graph index
dhk graph export
dhk graph impact --file|--symbol|--sql-table ...
read GRAPH_CONTEXT.md and IMPACT_MAP.md
make the minimal change
rerun graph index/export/impact when files changed
```

Graph output should guide impact analysis, not replace compile, test, sensitive,
workflow, or spec checks.
