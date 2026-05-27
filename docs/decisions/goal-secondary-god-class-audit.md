# Secondary Goal Service Decomposition Audit

Generated for AI-182 on 2026-05-27.

## Current Large Classes

After the primary split, the remaining large classes are:

```text
GoalOrchestrator.java         868 lines
GoalActionSyncService.java    717 lines
GraphImpactService.java       699 lines
GoalConfigDiagnosticsService  639 lines
GoalProfileService.java       547 lines
```

## Recommendation

Do not split these in the same batch as `GoalCheckService` and `MigrationRunner`.

The primary split changed hot infrastructure and schema code. Combining it with orchestration-state surgery would increase regression risk without adding immediate release-readiness value.

## Proposed Future Boundaries

### GoalOrchestrator

Keep as facade/coordinator and extract:

- `GoalStartOrchestrationService`: start workflow/spec/goal and initial context export.
- `GoalStepOrchestrationService`: evidence validation, action progression, and context export retry states.
- `GoalCompletionOrchestrationService`: evaluate, checkpoint, summary, artifact binding, and final sync.
- `GoalResumeService`: resume/export recovery paths.

### GoalActionSyncService

Keep as sync facade and extract:

- `WorkflowPhaseSyncService`
- `WorkflowGateSyncService`
- `SpecTaskSyncService`
- `SpecAcceptanceSyncService`

## Guardrail

`scripts/check-class-size.sh` deliberately treats `GoalOrchestrator` and `GoalActionSyncService` as known secondary debt instead of failing the gate today. They should become hard targets only after contract tests for goal start/step/complete and workflow/spec sync are expanded.

## Next Feature Candidate

Suggested future feature:

```text
V0.8.5 Goal Orchestration Decomposition
```

Acceptance:

- No public CLI output changes.
- Goal start/step/resume/complete integration tests remain green.
- Workflow/spec sync tests cover both success and failed export recovery.
- New hard size thresholds are introduced only after extraction.
