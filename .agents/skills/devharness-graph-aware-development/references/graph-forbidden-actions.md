# Graph Forbidden Actions

Forbidden unless GOAL_CONTEXT explicitly allows it:

- editing before graph snapshot and impact map are ready;
- ignoring `graph-assist` guidance in the current main action;
- using a stale `GRAPH_SNAPSHOT.json`;
- using `graph impact --allow-stale` without explicit policy approval or human approval evidence;
- using an `IMPACT_MAP.md` generated before the latest graph snapshot;
- changing files outside impact `related-files` without re-running impact and recording risk in the current goal step;
- completing when `goal verify` reports failed or stale `graph` / `impact` checks;
- copying graph facts into confirmed memory without human review.
- large refactors, whole-file formatting, or broad cleanup under legacy graph profiles.
- completing a legacy graph profile without a rollback plan artifact and passed manual evidence.
- ignoring `protected-impact-risk` in GOAL_CONTEXT.
