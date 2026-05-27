# God Class Decomposition Baseline

Generated for V0.8.4 God Class Decomposition on 2026-05-27.

## Baseline

Before this sprint, the largest production classes were:

```text
GoalCheckService.java        1301 lines
MigrationRunner.java         1207 lines
GoalOrchestrator.java         868 lines
GoalActionSyncService.java    717 lines
```

## Behavior Contracts

The refactor must preserve:

- Goal check keys: `compile`, `test`, `manual-compile`, `manual-test`, `verification-risk`, `sensitive`, `workflow`, `spec`, `graph`, `impact`, `legacy`, `architecture`, `bdd`, and the four discipline gates.
- Goal check status and summary semantics used by `goal check`, `goal verify`, `goal complete`, and skill gates.
- Goal check freshness metadata: `step_count_at_check`, workspace fingerprint, context fingerprint, and check fingerprint.
- Migration version history from v1 through v13 and all existing schema compatibility fixtures.
- Release-gate behavior: version metadata, coverage threshold, module boundaries, archive smoke, and packaged CLI smoke.

## New Structure

`GoalCheckService` is now a facade. It loads policy/profile, selects a `GoalCheckRunner`, and returns the persisted result.

Runner responsibilities moved to focused classes:

- `MavenGoalCheckRunner`: compile/test process checks.
- `ManualVerificationGoalCheckRunner`: manual compile/test evidence checks.
- `VerificationRiskGoalCheckRunner`: verification-disabled risk evidence.
- `SensitiveGoalCheckRunner`: exported context sensitive scan.
- `SpecGoalCheckRunner`: spec task and acceptance closure.
- `WorkflowGoalCheckRunner`: workflow run and hard-gate state.
- `GraphGoalCheckRunner`: graph context and snapshot freshness.
- `ImpactGoalCheckRunner`: impact map freshness, changed-file coverage, scenario impact, and safe-refactor reimpact.
- `LegacyGoalCheckRunner`: legacy scope, rollback, manual evidence, and protected impact confirmation.
- `ArchitectureGoalCheckRunner`: graph architecture check bridge.
- `BddGoalCheckRunner`: bound scenario evidence and BDD quality gates.
- `DisciplineGateGoalCheckRunner`: think-before-coding, goal-driven, simplicity, and surgical-change gates.

Shared infrastructure moved to:

- `GoalCheckCommandExecutor`: bounded command execution and timeout handling.
- `GoalCheckRecorder`: log writing, check persistence, and fingerprint metadata.
- `GoalCheckSupport`: shared parsing, evidence, artifact, and impact helpers.

`MigrationRunner` is now transaction/backup/registry focused. Each schema migration is a dedicated `MigrationStep` class under `db.migration`, from `V1InitialMemoryMigration` through `V13SkillTrustHardeningMigration`.

## Size Targets

The release gate now checks:

```text
GoalCheckService.java <= 500 lines
MigrationRunner.java  <= 450 lines
GoalCheckSupport.java <= 450 lines
*GoalCheckRunner.java <= 250 lines
V*Migration.java      <= 250 lines
```

Current post-refactor target classes:

```text
GoalCheckService.java     133 lines
MigrationRunner.java      185 lines
ImpactGoalCheckRunner     195 lines
BddGoalCheckRunner        132 lines
GoalCheckSupport          382 lines
```

## Verification

Required verification for this refactor:

- `mvn -q -DskipTests compile`
- `mvn -q -Dtest=GoalCheckServiceTest,MigrationRunnerTest test`
- `scripts/check-class-size.sh`
- `scripts/check-module-boundaries.sh`
- `scripts/release-gate.sh`

The release gate is the final contract because it also exercises coverage, packaged CLI smoke, archive contents, and metadata checks.
