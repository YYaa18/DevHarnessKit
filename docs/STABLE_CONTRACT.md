# Stable Contract

DevHarness Kit is still a beta developer preview until a stable release tag
explicitly promotes it. This document defines the smallest surface intended to
become the 1.0 stable contract. Anything not listed here is beta-only,
experimental, or internal.

## Stable Surface Candidate

The 1.0 stable surface is intentionally narrow:

- `dhk help`
- `dhk version`
- `dhk doctor`
- `dhk configure init/show/doctor/explain`
- `dhk status`
- `dhk readiness`
- `dhk advise`
- `dhk quickstart`
- memory core: `init`, `add`, `confirm`, `search`, `export`, `checkpoint`,
  `recover`, and `backup`
- goal core: `start`, `resume`, `next`, `step`, `status`, `export`, `verify`,
  `complete`, `audit`, and `recheck`

For 1.0 and later, command names, required option names, exit code meanings,
and the minimum machine-readable fields below should change only under semantic
versioning and documented deprecation windows.

The current artifact remains `0.4.6-beta.1`; this contract is the freeze target,
not a current stable claim.

## Exit Codes

The stable-beta exit code contract is:

- `0`: success
- `1`: runtime error
- `2`: usage error
- `3`: validation or safety rejection
- `4`: target not found

## Stable JSON Fields

Stable JSON consumers should ignore unknown fields. Additive fields are not
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
  `blocker_details`, `user_guidance`, `next_action`, `next_command`
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

`WORK_BRIEF.md`:

- task summary
- recommendation
- risk flags
- expected work
- user choices
- confirmation requirement

`AGENT_BRIEF.json` is alpha machine-readable adapter protocol, not stable-beta
public API. Its schema is versioned with `devharness-agent-brief/v1-alpha`.

## Beta But Included

DB inspection remains beta but is included in the developer toolkit. DevHarness
Kit does not force database read-only credentials because the intended target is
a trusted personal development environment. SQL guardrails and JDBC read-only
hints reduce accidental risk, but do not replace database permissions or local
developer responsibility.

## 1.0 Boundary Decisions

The first 1.0 stable tag keeps one shaded CLI jar and one release archive for
install simplicity. Experimental surfaces stay bundled for dogfooding and
adapter compatibility, but they remain outside the stable contract unless a
future release note explicitly promotes them.

The artifact-boundary decision for Graph, BDD, Skill/Governance, Policy, Routine,
and ECC Control Panel is recorded in [MODULE_BOUNDARIES.md](MODULE_BOUNDARIES.md).
Splitting those surfaces into optional artifacts is deferred until the split
reduces user risk more than it increases install and adapter complexity.

## Experimental Outside The Contract

The following surfaces are included for dogfooding and feedback, but are not
part of the stable-beta contract:

- Graph Lite and Graph-aware Goal
- BDD acceptance harness
- Skill Contract, Skill Evaluation, and Skill Trust
- Professional Knowledge Layer and knowledge pack injection
- Policy/Hook governance
- ECC Control Panel scripts
- Routine, metrics, and replay
- direct SQLite table or column details

These surfaces may change before 1.0 with release notes.
