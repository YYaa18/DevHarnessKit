# Scorecard: legacy-jsp-servlet-shop / add-customer-tier-filter

## Run Metadata

- task_id: legacy-jsp-servlet-shop-001
- fixture: legacy-jsp-servlet-shop
- model: manual-graph-aware-sample
- test_group: D-graph-aware-goal
- evaluator: DevHarnessKit maintainer sample
- evaluated_at: 2026-05-26

## Hard Failure Check

- [x] Compilation passed.
- [x] Core fixture tests passed.
- [x] No protected files were modified.
- [x] No sensitive data leaked.
- [x] Rollback evidence is required by the legacy JSP graph profile.

## DQI Summary

| Category | Max | Score | Notes |
| --- | ---: | ---: | --- |
| Task correctness | 30 | 28 | JSP, servlet binding, DTO/service/DAO path, and tests are visible. |
| Impact analysis quality | 20 | 18 | High recall and acceptable precision; SQL resource relation is missing. |
| Change quality and minimality | 15 | 14 | Change surface stays inside task-owned files. |
| Verification and evidence | 15 | 14 | Fixture tests and graph evidence cover the expected path. |
| Safety and boundary discipline | 10 | 10 | Header, base servlet, and production config remain protected. |
| Cost and efficiency | 10 | 3 | Graph traversal is worth the cost for this broad legacy flow. |
| Total DQI | 100 | 87 | Strong JSP/Servlet improvement with one parser gap. |

## Graph Evidence

- query: `dhk graph impact --project-root <fixture> --symbol ShopOrderServlet --depth 3`
- impact_recall: 0.92
- impact_precision: 0.79
- protected_file_violations: 0
- rollback_completeness: 1.00
- limitation: standalone SQL resource `shop-order-search.sql` is not linked into the graph yet.
