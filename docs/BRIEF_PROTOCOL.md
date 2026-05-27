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
