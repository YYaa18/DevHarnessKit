# Scorecard: modern-java-api / add-status-search-test-gap

## Run Metadata

- task_id: modern-java-api-003
- fixture: modern-java-api
- model: manual-graph-aware-sample
- test_group: D-graph-aware-goal
- evaluator: DevHarnessKit maintainer sample
- evaluated_at: 2026-05-26

## Hard Failure Check

- [x] Compilation passed.
- [x] Core fixture tests passed.
- [x] Repository-level test gap is detected.
- [x] Controller-to-repository shortcut is guarded by architecture check.
- [x] No sensitive data leaked.

## DQI Summary

| Category | Max | Score | Notes |
| --- | ---: | ---: | --- |
| Task correctness | 30 | 28 | Graph highlights API, service, repository, implementation, and tests. |
| Impact analysis quality | 20 | 18 | Complete recall including missing repository test. |
| Change quality and minimality | 15 | 14 | Graph keeps route/support infrastructure out of the expected edit set. |
| Verification and evidence | 15 | 14 | Test gap plus compile/test evidence is explicit. |
| Safety and boundary discipline | 10 | 9 | Boundary violation would be caught, but graph precision is broad. |
| Cost and efficiency | 10 | 5 | Extra graph pass is worthwhile for this task. |
| Total DQI | 100 | 88 | Best modern sample for graph-aware value. |

## Graph Evidence

- query: `dhk graph impact --project-root <fixture> --file src/main/java/com/acme/modern/account/service/AccountService.java --depth 4`
- impact_recall: 1.00
- impact_precision: 0.58
- test_gap_detected: `src/test/java/com/acme/modern/account/repository/AccountRepositoryTest.java`
- architecture_boundary_violation_detected: true
- limitation: impact map includes DTO/audit neighbors outside the strict task surface.
