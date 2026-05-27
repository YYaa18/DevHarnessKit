# Stable-Beta Contract

DevHarness Kit is still a beta developer preview. This document defines the
smallest contract that may be tested as a stable-beta surface before any 1.0
claim. Anything not listed here is either beta-only, experimental, or internal.

## Stable-Beta Surface

The stable-beta surface is intentionally narrow:

- `dhk help`
- `dhk version`
- `dhk doctor`
- `dhk configure init/show/doctor/explain`
- `dhk status`
- `dhk readiness`
- `dhk quickstart`
- memory core: `init`, `add`, `confirm`, `search`, `export`, `checkpoint`,
  `recover`, and `backup`
- goal core: `start`, `resume`, `next`, `step`, `status`, `export`, `verify`,
  `complete`, `audit`, and `recheck`

Stable-beta means command names, required option names, exit code meanings, and
the minimum machine-readable fields below should change only with release notes
and migration guidance.

## Exit Codes

The stable-beta exit code contract is:

- `0`: success
- `1`: runtime error
- `2`: usage error
- `3`: validation or safety rejection
- `4`: target not found

## Stable JSON Fields

Stable-beta JSON consumers should ignore unknown fields. Additive fields are not
breaking changes.

The following fields are the minimum stable JSON fields:

- common command identity: `command`
- goal identity: `goal_key`
- completion decision: `decision`, `ready_to_complete`
- verification counts: `check_count`, `failed_count`, `missing_count`,
  `completion_blocker_count`, `stale_count`
- verification arrays: `checks`, `failed_checks`, `missing`,
  `completion_blockers`, `stale_checks`
- verification guidance: `blocker_summary`, `blocker_categories`,
  `blocker_details`, `next_action`, `next_command`
- context pointer: `context_path`

`goal verify` blocker categories are stable-beta values:

- `incomplete_goal_steps`
- `context_export_not_ready`
- `goal_status_blocked`
- `pending_check`
- `stale_check`
- `pending_workflow_gate`
- `missing_spec`
- `missing_evidence`
- `skipped_required_check`
- `failed_check`
- `goal_blocker`

## Stable Markdown Export Anchors

SQLite remains the source of truth. Markdown files are generated artifacts.
Stable-beta scripts may rely on these files existing after the matching command
succeeds, and may scan the listed anchors.

`CURRENT_CONTEXT.md`:

- project summary section
- confirmed memory section
- recent checkpoint section

`GOAL_CONTEXT.md`:

- goal identity
- current action
- allowed actions
- forbidden actions
- evidence contract
- freshness status
- next command

`GOAL_SUMMARY.md`:

- goal identity
- final status
- steps summary
- checks summary
- artifacts summary

`ARTIFACT_PASSPORT.json`:

- `schema_version`
- `goal_key`
- `generated_at`
- `artifacts`
- `checks`
- `manual_evidence_status`

## Beta But Included

DB inspection remains beta but is included in the developer toolkit. DevHarness
Kit does not force database read-only credentials because the intended target is
a trusted personal development environment. SQL guardrails and JDBC read-only
hints reduce accidental risk, but do not replace database permissions or local
developer responsibility.

## Experimental Outside The Contract

The following surfaces are included for dogfooding and feedback, but are not
part of the stable-beta contract:

- Graph Lite and Graph-aware Goal
- BDD acceptance harness
- Skill Contract, Skill Evaluation, and Skill Trust
- Policy/Hook governance
- ECC Control Panel scripts
- Routine, metrics, and replay
- direct SQLite table or column details

These surfaces may change before 1.0 with release notes.
