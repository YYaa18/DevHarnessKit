# Graph Forbidden Actions

Forbidden unless GOAL_CONTEXT explicitly allows it:

- editing before graph snapshot and impact map are ready;
- ignoring `required_graph_action`;
- using a stale `GRAPH_SNAPSHOT.json`;
- using an `IMPACT_MAP.md` generated before the latest graph snapshot;
- changing files outside impact `related-files` without re-running impact and recording risk;
- completing when `goal verify` reports failed or stale `graph` / `impact` checks;
- copying graph facts into confirmed memory without human review.
