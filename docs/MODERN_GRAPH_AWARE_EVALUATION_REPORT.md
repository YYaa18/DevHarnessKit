# Modern Graph-aware Evaluation Report

Status: V0.5.3 curated alpha evaluation, not a statistical benchmark.

## Question

Does Graph-aware Goal improve modern Java API work compared with Goal-only
execution?

The short answer is yes, with a clear caveat: Graph-aware Goal improves impact
recall, architecture-boundary evidence, and test-gap detection, but Graph Lite is
still broad. Precision is lower than Goal-only because the current traversal
includes neighboring DTO/domain/audit files.

## Sample Set

| Fixture | Task | Goal-only DQI | Graph-aware DQI | Recall Gain | Precision Change | Main Finding |
| --- | --- | ---: | ---: | ---: | ---: | --- |
| modern-java-api | add-risk-rating-field | 76 | 83 | +0.13 | -0.16 | Better public API/DTO recall, but false positive repository test gap. |
| modern-java-api | add-freeze-endpoint-boundary | 73 | 86 | +0.33 | -0.25 | Architecture check catches controller-to-repository shortcuts. |
| modern-java-api | add-status-search-test-gap | 74 | 88 | +0.29 | -0.25 | Missing repository test is surfaced directly in `IMPACT_MAP.md`. |

Average result:

```text
goal_only_dqi_avg: 74.3
graph_aware_dqi_avg: 85.7
impact_recall_avg_goal_only: 0.71
impact_recall_avg_graph_aware: 0.96
impact_precision_avg_goal_only: 0.83
impact_precision_avg_graph_aware: 0.61
```

## What Improved

1. Impact recall improved on all three tasks.
2. `architecture` check adds a concrete boundary guard for modern controller /
   service / repository flows.
3. `missing-related-tests` makes the status-search repository test gap visible
   before completion.
4. Graph-aware summaries bind graph snapshot, impact map, and test-gap evidence
   into `GOAL_SUMMARY.md`.

## False Positives / Limitations

1. The risk-rating task no longer reports `AccountRepositoryTest` as a missing
   test gap when the query starts from `AccountResponse`.
2. Impact traversal is still broad around account domain/repository neighbors, so
   precision is lower than Goal-only manual reasoning.
3. Public API impact is inferred from controller files and route nodes; it is
   not yet a semantic OpenAPI contract diff.
4. Graph Lite remains import/path heuristic; it is not full Java type
   resolution.

## V0.5.5 Precision Follow-up

The first precision fix keeps CGC as a reference-only optional provider and
tightens Graph Lite:

```text
file/symbol starts:
  ignore package/import/type_reference/reference nodes
traversal:
  skip imports and package fanout
  skip external java.* bridge nodes
  stop test_case fanout
test gaps:
  only report repository test gaps when the query starts from service/repository
```

Representative fixture checks after the change:

| Query | Before related files | After related files | Result |
| --- | ---: | ---: | --- |
| `AccountResponse` DTO | 10 | 7 | Removes `AccountUpdateRequest` and the false `AccountRepositoryTest` gap. |
| `AccountService` service | 8 | 8 | Keeps repository test-gap detection for service-level change. |

The modern precision issue is improved but not fully closed. The next remaining
work is route-level public API impact scoring and more semantic repository
implementation filtering.

## Decision

Modern Graph Harness should remain enabled as an alpha graph-aware profile. It
is valuable for:

```text
architecture-boundary work
test-gap-sensitive API changes
safe refactor evidence
impact-map-backed summaries
```

It is not ready to claim high-precision impact planning. The next improvement
should reduce broad DTO/audit neighbor expansion and make route-level public API
impact more explicit.

## Follow-up Issues

Recommended follow-ups after V0.5.3:

1. Tune modern Graph Lite traversal to reduce DTO/audit false positives.
2. Add route-level public API impact scoring instead of file-only inference.
3. Add a configurable test-gap policy so repository test gaps can be warning or
   blocking depending on task/profile.
