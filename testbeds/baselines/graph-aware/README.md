# Graph-aware Baseline Samples

Status: V0.5.3 curated alpha samples, not a statistical benchmark.

These results capture manual Graph-aware Goal evaluation samples for the
synthetic testbeds. They are meant to keep the Graph Lite impact layer honest:
each sample records the graph query, impact recall/precision, rollback evidence
expectation, protected-file result, and DQI score.

The committed samples intentionally stay small. Local exploratory runs belong
under `testbeds/runs/` and should not be committed unless a future issue
promotes them into a curated baseline.

Current sample set:

| Fixture | Task | DQI | Impact Recall | Impact Precision | Main Finding |
| --- | --- | ---: | ---: | ---: | --- |
| legacy-mybatis-order | add-customer-level-field | 88 | 0.89 | 1.00 | Strong DTO/XML/service recall, but route is still missed. |
| legacy-mybatis-order | tighten-paid-order-filter | 84 | 1.00 | 0.45 | Finds SQL/test double path, but impact map is broad. |
| legacy-mybatis-order | fix-null-page-bounds | 86 | 1.00 | 0.27 | Finds caller/test, but narrow utility queries over-expand. |
| legacy-jsp-servlet-shop | add-customer-tier-filter | 87 | 0.92 | 0.79 | Finds JSP/servlet/service/DAO/tests; SQL resource is missed. |
| legacy-jsp-servlet-shop | preserve-legacy-search-action | 80 | 0.86 | 0.43 | Finds route/JSP/servlet path; precision and future-route signal need work. |
| modern-java-api | add-risk-rating-field | 83 | 0.88 | 0.70 | Improves public API/DTO recall; reports one repository-test false positive. |
| modern-java-api | add-freeze-endpoint-boundary | 86 | 1.00 | 0.55 | Captures boundary-sensitive surface and architecture violation risk. |
| modern-java-api | add-status-search-test-gap | 88 | 1.00 | 0.58 | Surfaces missing `AccountRepositoryTest` before completion. |
