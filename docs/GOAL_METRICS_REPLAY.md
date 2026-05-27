# Goal Metrics and Replay

Goal metrics and replay are alpha design contracts for routine reporting. They are derived from existing goal tables; they are not a new source of truth.

## Source of Truth

Metrics and replay are computed from:

```text
goal_run
goal_step
goal_check
goal_event
goal_artifact
```

The generated artifacts that may appear later, such as `GOAL_METRICS.md` or routine reports, must be treated like other Markdown exports. SQLite remains authoritative.

## Metrics Snapshot

The code-level schema version is:

```text
goal-metrics/v1.1-alpha
```

A metrics snapshot summarizes one goal run:

| Field | Meaning |
| --- | --- |
| `goal_key` | Goal identifier. |
| `profile_key` | Profile used by the goal. |
| `status` | Current goal status. |
| `current_action` | Current goal action. |
| `expected_steps` | Profile action count recorded on `goal_run.max_steps`. |
| `recorded_steps` | Current `goal_run.step_count`. |
| `accepted_steps` | Steps with `recorded` or `accepted` status. |
| `total_checks` | Count of check records. |
| `required_checks` | Count of required check records. |
| `passed_checks` | Checks with `passed` status. |
| `failed_checks` | Checks with `failed` status. |
| `skipped_checks` | Checks with `skipped` status. |
| `waived_checks` | Checks with `waived` status. |
| `stale_checks` | Checks whose `step_count_at_check` is older than `goal_run.step_count`. |
| `total_events` | Count of goal events. |
| `total_artifacts` | Count of goal artifacts. |
| `started_at` | `goal_run.created_at`. |
| `completed_at` | `goal_run.completed_at`, when present. |
| `duration_ms` | Duration from start to completion, or `-1` when incomplete or unparsable. |
| `bdd_status` | Latest BDD check status, when a `bdd` check exists. |
| `bdd_scenario_coverage_percent` | Scenario evidence coverage parsed from the latest BDD check summary, or `-1` when unavailable. |
| `bdd_evidence_freshness` | `fresh`, `stale`, `missing`, or `not_required` based on latest BDD check step count and status. |
| `bdd_quality_score` | BDD quality score parsed from the latest BDD check summary, or `-1` when unavailable. |
| `bdd_failure_reasons` | Failed BDD check reasons, suitable for trend reports. |

Snapshots are intentionally descriptive. They do not decide whether a goal is complete; `goal evaluate` remains responsible for completion decisions because it has profile and check policy context.

BDD fields are report metrics, not correctness proof. They let future eval and
routine reports compare scenario coverage, evidence freshness, and quality
failures across tasks or models while keeping `goal_check` and BDD SQLite rows as
the source of truth.

## Replay Entries

The code-level schema version is:

```text
goal-replay/v1-alpha
```

Replay is a deterministic ordered timeline. Each entry has:

| Field | Meaning |
| --- | --- |
| `sequence` | One-based order after sorting. |
| `source` | `goal_run`, `goal_event`, `goal_step`, `goal_check`, or `goal_artifact`. |
| `kind` | Event type, action key, check key, artifact type, or run lifecycle marker. |
| `timestamp` | Source timestamp used for ordering. |
| `status` | Source status or level when available. |
| `summary` | Human-readable short summary. |
| `data` | Supplemental data such as evidence path, artifact path, or event payload. |

Ordering uses timestamp first, then source rank:

```text
goal_run
goal_event
goal_step
goal_check
goal_artifact
```

This makes replay stable even when multiple facts share the same timestamp.

## Non-Goals

This design does not add:

- a new SQLite schema;
- a public `goal metrics` or `goal replay` CLI command;
- cross-project aggregation;
- model scoring or automatic quality judgment.

Those can be added after routine exports define their exact needs.

## Routine Usage

Future routine reporting can consume this model to answer questions such as:

- how many goals completed successfully by profile;
- which checks most often block completion;
- whether stale checks are common;
- how many steps a profile usually requires;
- what happened in a goal run without reading chat history.
- how BDD scenario coverage, evidence freshness, and quality score trend across
  comparable tasks or model runs.

The first routine implementation should export derived reports under `.agents/memory/exports/` and include the schema version in every artifact.

Routine local and CI export planning is documented in [ROUTINE_LOCAL_CI_EXPORT.md](ROUTINE_LOCAL_CI_EXPORT.md).
