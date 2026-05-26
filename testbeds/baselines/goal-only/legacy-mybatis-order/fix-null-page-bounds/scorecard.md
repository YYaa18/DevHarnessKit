# Scorecard: legacy-mybatis-order / fix-null-page-bounds

## Run Metadata

- task_id: legacy-mybatis-order-003
- fixture: legacy-mybatis-order
- model: manual-goal-only-sample
- test_group: C-goal-harness
- evaluator: DevHarnessKit maintainer sample
- evaluated_at: 2026-05-26

## Hard Failure Check

- [x] Compilation passed.
- [x] Core tests passed.
- [x] No protected files were modified.
- [x] No sensitive data leaked.
- [x] Goal completion used required evidence.

## DQI Summary

| Category | Max | Score | Notes |
| --- | ---: | ---: | --- |
| Task correctness | 30 | 27 | Utility behavior and regression test are straightforward. |
| Impact analysis quality | 20 | 15 | Changed files are precise, but service caller is easy to miss without graph. |
| Change quality and minimality | 15 | 14 | Narrow diff with no mapper/config churn. |
| Verification and evidence | 15 | 12 | Command evidence is good; rollback note is basic. |
| Safety and boundary discipline | 10 | 9 | Protected config avoided. |
| Cost and efficiency | 10 | 5 | Manual inspect overhead is still high for a tiny task. |
| Total DQI | 100 | 82 | Good Goal-only case, modest graph upside. |

## Graph Lite Opportunity

Graph Lite should improve impact recall by showing
`OrderService.searchOrders -> LegacyPageBounds.normalizePageSize` before the
edit. The minimum useful improvement here is not correctness, but confidence
that the affected caller and test are both named in the impact map.
