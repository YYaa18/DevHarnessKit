# Scorecard: modern-java-api / add-risk-rating-field

## Run Metadata

- task_id: modern-java-api-001
- fixture: modern-java-api
- model: manual-graph-aware-sample
- test_group: D-graph-aware-goal
- evaluator: DevHarnessKit maintainer sample
- evaluated_at: 2026-05-26

## Hard Failure Check

- [x] Compilation passed.
- [x] Core fixture tests passed.
- [x] Route annotation infrastructure was not modified.
- [x] No sensitive data leaked.
- [x] Public API response impact was represented through controller and response DTO files.

## DQI Summary

| Category | Max | Score | Notes |
| --- | ---: | ---: | --- |
| Task correctness | 30 | 27 | Domain, DTO, service, repository seed, and tests are in the expected surface. |
| Impact analysis quality | 20 | 16 | Recall improves over Goal-only, but precision drops from extra neighboring files. |
| Change quality and minimality | 15 | 13 | Graph keeps support annotations out of scope. |
| Verification and evidence | 15 | 13 | Existing service/controller tests are visible before implementation. |
| Safety and boundary discipline | 10 | 9 | Public API impact is visible; route-specific precision remains alpha. |
| Cost and efficiency | 10 | 5 | Extra graph pass is acceptable for this task size. |
| Total DQI | 100 | 83 | Useful recall gain with one test-gap false positive. |

## Graph Evidence

- query: `dhk graph impact --project-root <fixture> --file src/main/java/com/acme/modern/account/dto/AccountResponse.java --depth 4`
- impact_recall: 0.88
- impact_precision: 0.70
- public_api_impact_detected: true
- test_gap_detected: false positive, `AccountRepositoryTest`
- limitation: public API routes are inferred from controller impact, not full route-specific semantic analysis.
