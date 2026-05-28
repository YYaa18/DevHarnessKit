# Graph-Aware Goal Protocol

Use this protocol only inside a goal.

1. `goal start` or `goal resume`.
2. `goal next`.
3. If `<graph-assist>` asks for fresh context, run `graph-index-export` internally as part of the current action.
4. If impact evidence is missing, run `graph-impact` for the task target internally as part of the current action.
5. Read `GRAPH_CONTEXT.md` and `IMPACT_MAP.md`.
6. Record graph evidence such as `graph_snapshot`, `graph_context`, `impact_map`, `impacted_files`, `risk_nodes`, and `recommended_read_files` in the current goal step.
7. Inspect and plan using only current action evidence.
8. Implement the smallest change covered by the impact map.
9. Re-run `graph-impact` after implementation for the changed file or symbol.
10. Record post-change graph evidence with `post_change_impact_map`, `impact_delta`, and `changed_files_covered` in the current verify step.
11. For legacy graph profiles, create the rollback plan artifact and record passed manual evidence.
12. Run `goal verify`.
13. Complete only when verification is ready.

Generated graph files:

```text
.agents/graph/exports/GRAPH_CONTEXT.md
.agents/graph/exports/GRAPH_SNAPSHOT.json
.agents/graph/exports/IMPACT_MAP.md
```

The graph is a snapshot of code facts at a point in time. Regenerate it after code changes.
Do not use `graph impact --allow-stale` from the strict skill flow. That escape
hatch requires explicit project policy or human approval evidence outside the
weak-model protocol.

Legacy graph profiles are intentionally narrow. They should keep changes inside
the impact map, avoid broad formatting, include rollback steps, and treat
protected impact files as requiring manual confirmation.
