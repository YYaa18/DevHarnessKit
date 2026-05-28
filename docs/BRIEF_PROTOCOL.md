# Work Brief and Agent Brief Protocol

DevHarnessKit now supports an agent-mounted entry layer above the low-level goal
CLI.

## Three Layers

| Layer | Audience | Purpose | Stability |
| --- | --- | --- | --- |
| Work Brief | Human user | Explain task intent, recommended mode, risk, expected work, and confirmation needs. | Beta fields; wording may change. |
| Agent Execution Brief | Mounted coding agent | Provide machine-readable execution policy and internal Harness commands. | Alpha JSON schema with `schema_version`. |
| Goal / Harness Audit | DevHarnessKit | Keep SQLite facts, goal steps, checks, artifacts, workflow/spec/graph evidence. | Internal audit source unless documented elsewhere. |

## Work Brief

Generated at:

```text
.agents/devharness/briefs/WORK_BRIEF.md
```

Required fields:

```text
brief_id
recommendation_id
mode_id
task_summary
recommendation: patch | standard | strict | analyze_only | ask
confidence: high | medium | low
why
risk_flags
expected_work
will_not_do
user_choices
confirmation_required
requires_user_confirmation_reason
safe_to_start
```

The Work Brief must not ask users to copy low-level `dhk goal ...` commands. It
is the user-facing explanation layer.

## Lifecycle Briefs

Work Brief is now the entry point for a small lifecycle of user-facing files
under:

```text
.agents/devharness/briefs/
```

| File | Trigger | Audience | Purpose |
| --- | --- | --- | --- |
| `WORK_BRIEF.md` | `dhk advise` / `dhk quickstart` | User | Explain task intent, recommended mode, risk, and confirmation needs. |
| `PROGRESS_BRIEF.md` | `dhk goal step` | User | Summarize the latest recorded step, current goal state, and next user-visible action. |
| `VERIFY_BRIEF.md` | `dhk goal verify` | User | Translate checks, missing evidence, stale state, and manual verification needs into plain next steps. |
| `COMPLETION_BRIEF.md` | `dhk goal complete` | User | Summarize completion, checkpoint, summary artifact, residual risk, and knowledge-candidate entry. |
| `KNOWLEDGE_CANDIDATES.md` | completion or knowledge review | User | Show draft knowledge candidates that require user confirmation before persistence. |
| `GROWTH_CONTEXT.md` | `dhk growth export` | Agent/User | Export advisory personal growth lessons. It is not project fact. |

These files are export artifacts. SQLite and explicit command state remain the
audit source. Brief files can be regenerated and must not be treated as the only
source of truth.

`WORK_BRIEF.md` may include short professional notes for the user.
`AGENT_BRIEF.json` may include a machine-readable `knowledge_context` with
advisory rule references and `full_ref` paths. Knowledge refs guide the Agent;
they are not project facts.

## Interaction Requests

When task boundaries, risk escalation, protected files, or manual evidence need
user input, DevHarnessKit records structured interaction requests. The user can
answer them through:

```bash
dhk brief answer --request <request-id> --choice "<choice>"
```

Interaction request fields:

```text
request_id
goal_key
phase
type: clarification | confirmation | risk_escalation | manual_evidence | manual_verification | knowledge_candidate
priority: blocking | important | optional
question
why
choices
default_choice
blocks_progress
status: open | answered | rejected
```

Blocking requests prevent goal step execution until answered. This keeps the
user in control without exposing low-level Harness commands.

### Manual Verification Choices

When `goal verify` reaches a manual compile/test check without evidence, it
creates a `manual_verification` interaction instead of leaving the user with a
dead-end failure. The choices are:

```text
manual_passed       user has verified in IDE/local tooling and provides evidence_path
try_auto            Harness attempts the configured compile/test command once
waive_verification  user records an explicit verification-risk waiver
```

`try_auto` is scoped to the current check; it does not rewrite project
configuration. `waive_verification` requires reason, approver, risk scope, and
rollback plan evidence before the manual check can be accepted as waived.

## Agent Execution Brief

Generated at:

```text
.agents/devharness/briefs/AGENT_BRIEF.json
```

Schema:

```text
devharness-agent-brief/v1-alpha
```

Important fields:

```text
brief_id
recommendation_id
task_key
goal_key
mode
profile_key
current_action
allowed_actions
forbidden_actions
required_evidence
escalation_rules
verification_policy
execution_policy.show_commands_to_user = false
harness_commands[].agent_internal_only = true
```

`harness_commands` are structured command argv records for adapters. They are
not the normal user-facing experience.

Agent Brief also includes:

```text
user_visible_summary_ref
growth_context.path
growth_context.advisory_only = true
```

When a blocking interaction is open, `current_action` is
`wait_for_user_answer`. After `dhk brief answer`, adapters may continue by
reading the refreshed Agent Brief and GOAL_CONTEXT.

## Knowledge Candidates And Growth

`dhk goal complete` can generate draft knowledge candidates. A candidate has:

```text
candidate_id
goal_key
type
title
summary
evidence_refs
suggested_destination: project_memory | growth
confidence
requires_confirmation
sensitive_scan_status
status: draft | confirmed | rejected
```

Rules:

- Candidates start as `draft`.
- Rejected candidates disappear from the default Knowledge Brief.
- Confirming to project memory creates only a draft memory item; it never creates confirmed memory.
- Confirming to growth creates a draft growth lesson.
- Failed sensitive scans block confirmation.

Commands:

```bash
dhk brief knowledge review --project-root .
dhk brief knowledge confirm --candidate <candidate-id> --destination project_memory
dhk brief knowledge confirm --candidate <candidate-id> --destination growth
dhk brief knowledge reject --candidate <candidate-id>
dhk growth review --project-root .
dhk growth confirm --lesson <lesson-id>
dhk growth export --project-root .
```

Growth lessons are advisory-only. They can help future Work Brief
recommendations, but they are not project facts and are not injected across
projects without explicit export/use.

## Mode Recommendations

Mode Advisor is deterministic and rule-based in this version.

```text
patch        small scoped logic change
standard     normal feature/API change
strict       high-risk or audit-sensitive work
analyze_only produce briefs without starting work
ask          task boundary is unclear
```

`full` is accepted only as a compatibility alias for `strict`.

## Relation To GOAL_CONTEXT

Agent Brief does not replace `GOAL_CONTEXT.md`.

```text
Agent Brief     task entry protocol for mounted agents
GOAL_CONTEXT    current goal state and next action
SQLite          source of truth and audit state
```

Adapters should read Agent Brief first, then follow `GOAL_CONTEXT.md` for each
goal step.
