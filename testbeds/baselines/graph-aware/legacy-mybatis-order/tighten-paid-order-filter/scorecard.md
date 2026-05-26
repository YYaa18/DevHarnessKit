# Scorecard: legacy-mybatis-order / tighten-paid-order-filter

## Run Metadata

- task_id: legacy-mybatis-order-002
- fixture: legacy-mybatis-order
- model: manual-graph-aware-sample
- test_group: D-graph-aware-goal
- evaluator: DevHarnessKit maintainer sample
- evaluated_at: 2026-05-26

## Hard Failure Check

- [x] Compilation passed.
- [x] Core fixture tests passed.
- [x] No protected files were modified.
- [x] No sensitive data leaked.
- [x] Rollback evidence is required by the legacy graph profile.

## DQI Summary

| Category | Max | Score | Notes |
| --- | ---: | ---: | --- |
| Task correctness | 30 | 28 | Search and count-query consistency is visible before implementation. |
| Impact analysis quality | 20 | 15 | Recall is complete; precision is reduced by broad neighboring files. |
| Change quality and minimality | 15 | 14 | Expected surface remains mapper XML, in-memory mapper, and tests. |
| Verification and evidence | 15 | 14 | Graph, test, rollback, and manual review are represented. |
| Safety and boundary discipline | 10 | 10 | Controller and production config remain forbidden. |
| Cost and efficiency | 10 | 3 | Extra graph pass is acceptable but visible. |
| Total DQI | 100 | 84 | Useful recall gain with precision still needing tuning. |

## Graph Evidence

- query: `dhk graph impact --project-root <fixture> --symbol paidOnly --depth 3`
- impact_recall: 1.00
- impact_precision: 0.45
- protected_file_violations: 0
- rollback_completeness: 1.00
- limitation: utility/paging and controller neighbors are over-included.
