# Scorecard: legacy-mybatis-order / fix-null-page-bounds

## Run Metadata

- task_id: legacy-mybatis-order-003
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
- [x] Rollback evidence is available.

## DQI Summary

| Category | Max | Score | Notes |
| --- | ---: | ---: | --- |
| Task correctness | 30 | 28 | Utility behavior, caller, and regression test are all visible. |
| Impact analysis quality | 20 | 13 | Recall improves over Goal-only; precision is the main regression. |
| Change quality and minimality | 15 | 14 | Expected implementation remains a narrow utility/test change. |
| Verification and evidence | 15 | 14 | Impact map and fixture test evidence are clear. |
| Safety and boundary discipline | 10 | 10 | Mapper XML and protected config remain outside the change. |
| Cost and efficiency | 10 | 7 | Graph overhead is modest for this small task. |
| Total DQI | 100 | 86 | Good recall improvement, limited by broad traversal. |

## Graph Evidence

- query: `dhk graph impact --project-root <fixture> --symbol LegacyPageBounds --depth 3`
- impact_recall: 1.00
- impact_precision: 0.27
- protected_file_violations: 0
- rollback_completeness: 1.00
- limitation: narrow utility symbols currently over-expand through service and mapper neighbors.
