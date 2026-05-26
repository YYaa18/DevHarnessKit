# Scorecard: legacy-mybatis-order / add-customer-level-field

## Run Metadata

- task_id: legacy-mybatis-order-001
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
| Task correctness | 30 | 28 | Expected DTO/XML/service/test surface is identified before implementation. |
| Impact analysis quality | 20 | 18 | Recall is high and precision is clean; route/controller remains the main miss. |
| Change quality and minimality | 15 | 14 | Expected change surface stays bounded to DTO, mapper, service, test. |
| Verification and evidence | 15 | 15 | Graph impact, fixture test, rollback, and manual evidence are all required. |
| Safety and boundary discipline | 10 | 10 | Protected production config remains outside the change set. |
| Cost and efficiency | 10 | 3 | Graph setup adds cost for a small fixture. |
| Total DQI | 100 | 88 | Strong Graph-aware legacy field-addition case. |

## Graph Evidence

- query: `dhk graph impact --project-root <fixture> --sql-table legacy_order --depth 3`
- impact_recall: 0.89
- impact_precision: 1.00
- protected_file_violations: 0
- rollback_completeness: 1.00
- limitation: route/controller entrypoint is not reached from the table query.
