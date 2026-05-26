# Scorecard: legacy-jsp-servlet-shop / preserve-legacy-search-action

## Run Metadata

- task_id: legacy-jsp-servlet-shop-002
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
- [x] Manual smoke evidence is required for route compatibility.

## DQI Summary

| Category | Max | Score | Notes |
| --- | ---: | ---: | --- |
| Task correctness | 30 | 27 | Current route/JSP/servlet path and route test are visible. |
| Impact analysis quality | 20 | 13 | Recall improves, but the graph is broad and cannot infer the future URL. |
| Change quality and minimality | 15 | 13 | Expected change stays in web.xml, servlet, and servlet test. |
| Verification and evidence | 15 | 15 | Manual route smoke evidence and rollback evidence are mandatory. |
| Safety and boundary discipline | 10 | 10 | JSP action, base servlet, header, and production config remain protected. |
| Cost and efficiency | 10 | 2 | Manual smoke plus broad graph make this the costliest legacy sample. |
| Total DQI | 100 | 80 | Useful route safety gain, but precision/future-route signal need work. |

## Graph Evidence

- query: `dhk graph impact --project-root <fixture> --file src/main/webapp/WEB-INF/web.xml --depth 3`
- impact_recall: 0.86
- impact_precision: 0.43
- protected_file_violations: 0
- rollback_completeness: 1.00
- limitation: the graph cannot discover the not-yet-added `/shop/orderList.do` route.
