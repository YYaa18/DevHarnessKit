---
name: devharness-graph-aware-development
description: Use DevHarnessKit graph-aware goal protocol for code changes that require dependency impact maps and post-change graph evidence.
---

# DevHarness Graph-Aware Development

Use this skill when a task uses a `*-with-graph` goal profile or GOAL_CONTEXT has `graph_required: true`.
It extends the goal-first protocol with mandatory graph evidence.

## Required Protocol

1. Start or resume the goal with the goal skill wrappers.
2. Run `goal-next.sh` and read `.agents/memory/exports/GOAL_CONTEXT.md`.
3. If GOAL_CONTEXT requires `graph_index_export`, run `scripts/graph-index-export.sh`, then run `goal-next.sh` again.
4. If GOAL_CONTEXT requires `graph_impact`, run `scripts/graph-impact.sh` with the file, symbol, or SQL table named by the task, then run `goal-next.sh` again.
5. Before editing, read `.agents/graph/exports/GRAPH_CONTEXT.md` and `.agents/graph/exports/IMPACT_MAP.md`.
6. Edit only files covered by `IMPACT_MAP.md`. If a needed file is outside the map, record the risk and regenerate impact for that file or symbol before editing it.
7. After implementation, run graph impact again for changed files or the main changed symbol, then record the `graph_reimpact` goal step.
8. Run `goal-verify.sh`; graph-required goals must pass `graph` and `impact` checks before completion.
9. Run `goal-complete.sh` only after `goal verify` returns `ready_to_complete`.

Graph facts are snapshot-bound generated facts. They are not confirmed long-term memory and must be regenerated after code changes.

## Forbidden By Default

- Do not skip `GRAPH_CONTEXT.md` or `IMPACT_MAP.md` in a graph-required goal.
- Do not edit files outside the impact map without re-running impact and recording risk.
- Do not treat graph output as confirmed memory.
- Do not complete when graph or impact checks are failed, stale, or missing.
- Do not use lower-level workflow/spec/db commands unless GOAL_CONTEXT explicitly allows them.

## Required Final Self-Check

End each response with:

```text
DevHarness self-check:
- goal:
- current_action:
- graph_snapshot:
- impact_map:
- changed_files:
- checks:
- completion:
```

## References

- `references/graph-protocol.md`
- `references/graph-evidence-format.md`
- `references/graph-forbidden-actions.md`
