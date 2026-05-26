# Graph-Aware Goal Protocol

Use this protocol only inside a goal.

1. `goal start` or `goal resume`.
2. `goal next`.
3. If required, run `graph-index-export`.
4. `goal next`.
5. If required, run `graph-impact` for the task target.
6. Read `GRAPH_CONTEXT.md` and `IMPACT_MAP.md`.
7. Record the graph step with `graph_snapshot`, `graph_context`, `impact_map`, `impacted_files`, `risk_nodes`, and `recommended_read_files` evidence as applicable.
8. Inspect and plan using only current action evidence.
9. Implement the smallest change covered by the impact map.
10. Re-run `graph-impact` after implementation for the changed file or symbol.
11. Record `graph_reimpact` evidence with `post_change_impact_map`, `impact_delta`, and `changed_files_covered`.
12. For legacy graph profiles, create the rollback plan artifact and record passed manual evidence.
13. Run `goal verify`.
14. Complete only when verification is ready.

Generated graph files:

```text
.agents/graph/exports/GRAPH_CONTEXT.md
.agents/graph/exports/GRAPH_SNAPSHOT.json
.agents/graph/exports/IMPACT_MAP.md
```

The graph is a snapshot of code facts at a point in time. Regenerate it after code changes.

Legacy graph profiles are intentionally narrow. They should keep changes inside
the impact map, avoid broad formatting, include rollback steps, and treat
protected impact files as requiring manual confirmation.
