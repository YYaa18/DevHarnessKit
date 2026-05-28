---
name: devharness-graph-aware-development
description: Compatibility helper for DevHarnessKit graph evidence. The main goal skill owns graph-required flows.
---

# DevHarness Graph Evidence Helper

Do not treat this as a separate development workflow. For graph-required work,
use `devharness-goal-development` as the primary skill and treat graph as
evidence inside the current main goal action.

When a Work Brief exists, show the user the brief-level intent, risk, and confirmation needs. Keep Graph and Harness commands agent-internal through `AGENT_BRIEF.json` unless the user asks for debugging details.

## Integrated Protocol

1. Start or resume the goal with the goal skill wrappers.
2. Run `goal-next.sh` and read `.agents/memory/exports/GOAL_CONTEXT.md`.
3. If GOAL_CONTEXT has `<graph-assist>`, run the listed graph helper internally as part of the current action.
4. If impact evidence is missing, run `scripts/graph-impact.sh` with the file, symbol, or SQL table named by the task.
5. Before editing, read `.agents/graph/exports/GRAPH_CONTEXT.md` and `.agents/graph/exports/IMPACT_MAP.md`.
6. Edit only files covered by `IMPACT_MAP.md`. If a needed file is outside the map, record the risk and regenerate impact for that file or symbol before editing it.
7. After implementation, run graph impact again for changed files or the main changed symbol, then record the post-change graph evidence in the current `verify` goal step.
8. Run `goal-verify.sh`; graph-required goals must pass `graph` and `impact` checks before completion.
9. Run `goal-complete.sh` only after `goal verify` returns `ready_to_complete`.

For legacy graph profiles such as `legacy-java-small-fix-with-graph` and
`legacy-jsp-servlet-change-with-graph`, also create a rollback plan artifact
and record manual evidence before verification. If GOAL_CONTEXT shows
`protected-impact-risk`, do not edit or complete until manual confirmation is
recorded.

Graph facts are snapshot-bound generated facts. They are not confirmed long-term memory and must be regenerated after code changes.

## Forbidden By Default

- Do not skip `GRAPH_CONTEXT.md` or `IMPACT_MAP.md` in a graph-required goal.
- Do not use `graph impact --allow-stale` yourself. It requires explicit policy approval or human approval evidence; the wrapper blocks it unless `DHK_ALLOW_STALE_APPROVED=true` is set outside the agent flow.
- Do not edit files outside the impact map without re-running impact and recording risk.
- Do not treat graph output as confirmed memory.
- Do not complete when graph or impact checks are failed, stale, or missing.
- Do not use lower-level workflow/spec/db commands unless GOAL_CONTEXT explicitly allows them.
- Do not expose Agent Brief `harness_commands` as the normal user-facing experience.
- Do not perform large refactors, whole-file formatting, or broad cleanup in legacy graph profiles.
- Do not complete a legacy graph profile without `rollback_plan`, `manual_evidence_status=passed`,
  and `manual_evidence_path` evidence.

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
