# Routine Local and CI Export Plan

Routine is a planned alpha layer for summarizing repeated goal runs. It should help a team compare profile, model, and task outcomes over time without reading chat history or uploading project memory.

Implementation is intentionally deferred. This document defines scope, export shape, and privacy boundaries before a public `routine` CLI exists.

## Goals

- Summarize repeated `goal` runs from local SQLite facts.
- Compare outcomes by profile, module, check key, and completion status.
- Make CI artifacts safe to publish inside normal build logs or workflow artifacts.
- Keep all processing offline and local-first.
- Preserve SQLite as source of truth and treat exports as derived artifacts.

## Non-Goals

Routine alpha must not add:

- a daemon, scheduler, background watcher, or HTTP service;
- cloud upload, telemetry, or hosted dashboards;
- automatic model scoring from chat transcripts;
- execution of arbitrary build/test commands outside explicit goal checks;
- raw evidence, raw SQL results, secrets, or long context exports in CI artifacts;
- a promise that metrics prove code correctness.

## Data Sources

Routine reads the same goal facts documented in [GOAL_METRICS_REPLAY.md](GOAL_METRICS_REPLAY.md):

```text
goal_run
goal_step
goal_check
goal_event
goal_artifact
```

Routine may also join workflow/spec status in later phases, but goal facts are the first source. It should not parse chat history.

## Future Commands

The first implementation should prefer explicit commands:

```bash
dhk routine local --since 30d
dhk routine export --format markdown --out .agents/memory/exports/ROUTINE_SUMMARY.md
dhk routine export --ci --out target/devharness-routine
```

Planned command boundaries:

| Command | Scope |
| --- | --- |
| `routine local` | Human-readable local report for the current project. |
| `routine export --format markdown` | Local Markdown export with more detail, still sensitive-guarded. |
| `routine export --format json` | Local machine-readable export for scripts. |
| `routine export --ci` | CI-safe artifact set with strict field filtering. |

`routine local` should not run goals. It summarizes completed or in-progress goal records that already exist.

## Local Export Shape

Local routine export can include project-specific labels because it stays inside the project workspace.

Default paths:

```text
.agents/memory/exports/ROUTINE_SUMMARY.md
.agents/memory/exports/routine-summary.json
.agents/memory/exports/routine-goals.ndjson
.agents/memory/exports/routine-replay/
```

`ROUTINE_SUMMARY.md` sections:

```text
<routine>
- generated_at
- window
- project_key
- source_schema
- export_schema
</routine>

<outcomes>
- total_goals
- completed_goals
- failed_goals
- abandoned_goals
- in_progress_goals
- completion_rate
</outcomes>

<profiles>
- profile_key
- goals
- completed
- failed
- median_duration_ms
- median_steps
- stale_checks
</profiles>

<checks>
- check_key
- passed
- failed
- skipped
- waived
- stale
</checks>

<interventions>
- waiting_user
- waived_checks
- policy_blocked
- stale_checks_repaired
- spec_pending
- workflow_gate_pending
</interventions>

<next-actions>
- suggested follow-up investigations
</next-actions>
```

`routine-summary.json` should use a versioned schema:

```json
{
  "schema_version": "routine-summary/v1-alpha",
  "generated_at": "2026-05-25T00:00:00Z",
  "window": {
    "since": "2026-04-25T00:00:00Z",
    "until": "2026-05-25T00:00:00Z"
  },
  "outcomes": {
    "total_goals": 12,
    "completed_goals": 9,
    "failed_goals": 1,
    "abandoned_goals": 0,
    "in_progress_goals": 2,
    "completion_rate": 0.75
  },
  "profiles": [],
  "checks": [],
  "interventions": []
}
```

`routine-goals.ndjson` should contain one sanitized metrics snapshot per goal. `routine-replay/<goal_key>.ndjson` is optional and should remain local-only by default.

## CI-Safe Export Shape

CI export is stricter than local export. It should be safe to upload as a CI artifact in a private repository, while still avoiding unnecessary project details.

Default path:

```text
target/devharness-routine/
```

Recommended files:

```text
target/devharness-routine/routine-summary.json
target/devharness-routine/routine-checks.ndjson
target/devharness-routine/routine-profiles.ndjson
target/devharness-routine/README.md
```

CI export may include:

- schema version;
- generated timestamp;
- profile key;
- mode;
- high-level module bucket when configured;
- status counts;
- duration buckets;
- step counts;
- check status counts;
- stale check counts;
- intervention category counts.

CI export must exclude by default:

- task names and condition text;
- step summaries and evidence;
- changed file paths;
- event payloads;
- artifact paths that point outside the CI export directory;
- SQL text and SQL result paths;
- raw context Markdown;
- user names, emails, phone numbers, ID numbers, tokens, cookies, private keys, JDBC URLs, and Authorization headers.

CI export should support a future explicit opt-in for richer internal artifacts, for example:

```bash
dhk routine export --ci --include-local-labels
```

That opt-in must still run the sensitive-data guard and local policy hooks.

## Metrics

Success metrics:

- `completed_goals`
- `completion_rate`
- `ready_to_complete_count`
- `median_duration_ms`
- `median_recorded_steps`
- `completed_by_profile`

Failure metrics:

- `failed_goals`
- `abandoned_goals`
- `blocked_goals`
- `failed_checks_by_key`
- `stale_checks_by_key`
- `context_export_failures`
- `policy_blocked_actions`

Intervention metrics:

- `waiting_user_count`
- `waived_checks`
- `skipped_checks`
- `manual_spec_acceptance`
- `workflow_gate_pending`
- `evidence_incomplete`

Routine should report counts first. Percentages and medians should be added only when the denominator is present and clear.

## Privacy Rules

Routine exports must follow these rules:

1. Local SQLite remains the source of truth.
2. Markdown, JSON, and NDJSON routine files are generated exports.
3. CI export is deny-by-default for free-text fields.
4. Local export may include labels, but must still run sensitive-data checks.
5. CI export should prefer counts, categories, and buckets over raw text.
6. Sensitive-data policy and project policy hooks apply before files are written.
7. Routine must not export chat history.
8. Routine must not export raw SQL results.
9. Routine must not upload anything over the network.
10. Users remain responsible for reviewing artifacts before publishing them outside the project.

## Implementation Sequence

Recommended follow-up issues:

1. Add `RoutineSummary` and `RoutineProfileSummary` model classes.
2. Add a `RoutineReportService` that aggregates `GoalMetricsSnapshot` values.
3. Add local Markdown and JSON renderers.
4. Add `routine export --ci` with strict field filtering.
5. Add sensitive/policy hook checks before routine writes files.
6. Add fixture tests for CI-safe exports.
7. Add documentation examples for a small Java API change profile.

## Acceptance Boundary

For the current alpha milestone, this issue is complete when:

- routine local scope and non-goals are documented;
- CI export files and field rules are documented;
- success, failure, and intervention metrics are named;
- privacy boundaries are explicit;
- implementation remains deferred to follow-up issues.
