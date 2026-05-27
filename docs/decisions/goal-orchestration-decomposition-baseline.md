# Goal Orchestration Decomposition Baseline

Generated for V0.8.5 Goal Orchestration Decomposition on 2026-05-27.

## Pre-Split Baseline

```text
GoalOrchestrator.java         868 lines
GoalActionSyncService.java    717 lines
GraphImpactService.java       699 lines
```

## Preserved Behavior

The split preserves:

- `dhk goal start`, `next`, `step`, `check`, `verify`, `complete`, `export`, and `audit` command behavior.
- Goal context export states: `context_exporting`, `context_export_failed`, and target ready/running statuses.
- Goal completion artifacts: checkpoint, `GOAL_SUMMARY.md`, `ARTIFACT_PASSPORT.json`, workflow artifact binding, graph artifact binding.
- Goal step required evidence validation and failure wording.
- Workflow/spec synchronization semantics for phase pass, gate pass, spec task completion, acceptance completion, and spec verification.

## New Boundaries

`GoalOrchestrator` is now primarily a command-level coordinator. It delegates:

- Context export and failed-export recovery to `GoalContextExportCoordinator`.
- Completion artifact/checkpoint/passport binding to `GoalCompletionArtifactService`.
- Step evidence validation and changed-file aggregation to `GoalStepEvidenceValidator`.

`GoalActionSyncService` is now primarily a sync facade. It delegates:

- Workflow phase/gate/progress writes to `GoalWorkflowSyncSupport`.
- Spec scaffold/task/verification support to `GoalSpecSyncSupport`.

## Size Targets

The release gate now enforces:

```text
GoalOrchestrator.java      <= 650 lines
GoalActionSyncService.java <= 550 lines
```

Post-split observed size:

```text
GoalOrchestrator.java          601 lines
GoalActionSyncService.java     451 lines
GoalCompletionArtifactService  235 lines
GoalWorkflowSyncSupport        170 lines
GoalSpecSyncSupport            176 lines
GoalStepEvidenceValidator       87 lines
```

## Verification

Required verification:

- `mvn -q -DskipTests compile`
- `mvn -q -Dtest=GoalIntegrationTest,ArtifactPassportIntegrationTest test`
- `scripts/check-class-size.sh`
- `./scripts/release-gate.sh`
