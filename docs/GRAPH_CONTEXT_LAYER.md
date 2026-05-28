# Graph Context Layer

Graph Lite exports are a short-context layer for agents. They are generated from
the latest completed graph snapshot and are not long-term confirmed memory.

## Contract

```text
.agents/graph/config.json
.agents/graph/architecture.json
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
- Graph Lite output is heuristic advisory context. It can recommend files,
  tests, and risk nodes, but it is not a correctness proof and must not replace
  compile, tests, or manual review.
- `IMPACT_MAP.md` can include `related-tests` and `missing-related-tests`.
  Missing tests are inferred from Java path conventions and should be treated as
  review prompts.

## Safety Boundary

Graph Lite is best-effort static indexing. It does not provide full AST parsing,
type resolution, runtime call tracing, or security proof.

`provider=cgc` is an optional alpha adapter contract. DevHarnessKit does not
ship CGC and the default `provider=lite` path must continue to work when CGC is
not installed. Use `dhk graph doctor` to check whether the configured CGC command
is available before enabling the adapter.

Sensitive and protected file content is not copied into graph exports. The
scanner may record skipped metadata such as path, size, language, file kind, and
skip reason so users can see what was omitted.

## Agent Use

For graph-required work, Graph Lite is integrated into the main goal flow. The
agent still follows `goal next -> current action -> goal step`; graph commands
are internal evidence helpers, not separate user-facing phases.

```text
goal next
if GOAL_CONTEXT has graph-assist, run graph index/export or impact internally
read GRAPH_CONTEXT.md and IMPACT_MAP.md
record graph evidence in the current goal step
make the minimal change under the main action
rerun graph impact after changes and record the evidence in verify
```

Graph output should guide impact analysis, not replace compile, test, sensitive,
workflow, or spec checks.

`GRAPH_CONTEXT.md`, `IMPACT_MAP.md`, and graph-integrated `GOAL_CONTEXT.md` include
`graph-confidence` metadata with provider, heuristic precision, freshness, and
test/manual-review reminders. Stale graph snapshots must be refreshed before a
graph-required goal continues impact analysis.

For `safe-refactor-with-graph`, rerunning impact after the change is mandatory.
If the post-change impact is wider than the pre-change impact, record explicit
risk evidence before completion.

## Architecture Check Alpha

Graph-aware Java goal profiles include an alpha `architecture` check. The check
uses `.agents/graph/architecture.json` when present and otherwise defaults to
controller/service/repository conventions. The default mode records a passing
check with an `architecture warning` summary; projects can set `"mode": "fail"`
to make violations block `goal verify`.

Example:

```json
{
  "schema_version": "devharness-graph-architecture/v1-alpha",
  "mode": "fail",
  "controller_patterns": ["src/main/java/**/controller/**"],
  "service_patterns": ["src/main/java/**/service/**"],
  "repository_patterns": ["src/main/java/**/repository/**"],
  "public_api_patterns": ["src/main/java/**/controller/**"],
  "forbidden_dependencies": [
    "controller->repository",
    "repository->controller",
    "repository->service",
    "service->controller"
  ]
}
```

Limitations:

- The check is based on indexed imports and path/package naming.
- Fully-qualified references without imports may be missed.
- Public API impact is inferred from `IMPACT_MAP.md` route entries and files
  matching `public_api_patterns`.
