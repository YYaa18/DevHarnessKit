# GraphImpactService Decomposition Audit

Generated for AI-189 on 2026-05-27.

## Current State

`GraphImpactService.java` remains the largest non-goal service after V0.8.5:

```text
GraphImpactService.java  699 lines
```

It owns several different responsibilities:

- Request normalization and source node selection.
- Incoming/outgoing graph traversal.
- Related file/test/SQL/risk node aggregation.
- Missing related-test inference.
- Markdown impact-map writing.
- Staleness and provider metadata reporting.

## Recommendation

Do not change Graph parser or impact heuristics in the goal-orchestration split. The graph layer is still alpha/advisory, and parser or traversal changes would make this structural refactor harder to verify.

## Proposed Future Split

Candidate V0.8.6 boundary:

- `GraphImpactQueryResolver`: file/symbol/sql-table/scenario input normalization.
- `GraphImpactTraversalService`: BFS depth, incoming/outgoing edge traversal, and depth-limit metadata.
- `GraphImpactRiskService`: risk node and protected file classification.
- `GraphImpactTestInferenceService`: related tests and missing tests.
- `GraphImpactMapWriter`: `IMPACT_MAP.md` rendering and confidence/staleness block.

## Gate Position

`scripts/check-class-size.sh` keeps `GraphImpactService` as known large-class debt for now. It should become a hard threshold only after graph impact golden tests cover representative legacy MyBatis, JSP/Servlet, DTO suppression, protected-file, and stale snapshot cases.
