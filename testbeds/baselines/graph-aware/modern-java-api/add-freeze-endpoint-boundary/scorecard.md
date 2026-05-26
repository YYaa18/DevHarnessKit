# Scorecard: modern-java-api / add-freeze-endpoint-boundary

## Run Metadata

- task_id: modern-java-api-002
- fixture: modern-java-api
- model: manual-graph-aware-sample
- test_group: D-graph-aware-goal
- evaluator: DevHarnessKit maintainer sample
- evaluated_at: 2026-05-26

## Hard Failure Check

- [x] Compilation passed.
- [x] Core fixture tests passed.
- [x] Controller boundary rule is explicitly checked.
- [x] No protected files were modified.
- [x] No sensitive data leaked.

## DQI Summary

| Category | Max | Score | Notes |
| --- | ---: | ---: | --- |
| Task correctness | 30 | 28 | Route, service, domain/repository boundary, and tests are all visible. |
| Impact analysis quality | 20 | 17 | Recall is complete for expected files; graph is broad around account neighbors. |
| Change quality and minimality | 15 | 14 | Boundary check discourages controller-to-repository shortcuts. |
| Verification and evidence | 15 | 13 | Compile/test plus architecture evidence are available before completion. |
| Safety and boundary discipline | 10 | 10 | Architecture violation is detected by the `architecture` check. |
| Cost and efficiency | 10 | 4 | Extra index/impact pass is visible but useful. |
| Total DQI | 100 | 86 | Strong fit for graph-aware boundary work. |

## Graph Evidence

- query: `dhk graph impact --project-root <fixture> --file src/main/java/com/acme/modern/account/controller/AccountController.java --depth 4`
- impact_recall: 1.00
- impact_precision: 0.55
- architecture_boundary_violation_detected: true
- public_api_impact_detected: true
- limitation: broad account-neighbor traversal lowers precision.
