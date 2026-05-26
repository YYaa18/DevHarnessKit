# Export Contracts

DevHarness Kit Markdown files under `.agents/memory/exports/` are generated context artifacts. SQLite remains the source of truth for memory, workflow, spec, and goal state.

Alpha agents and scripts may depend on the required sections below. New fields may be added inside a section before 1.0, but existing section names and order should change only with a changelog note.
See [COMPATIBILITY.md](COMPATIBILITY.md) for which exports are stable-ish alpha
versus alpha in the broader release contract.

## WORKFLOW_CONTEXT.md

Required section order:

```text
# WORKFLOW_CONTEXT
<generated-at>
<workflow-run>
<current-phase>
<phases>
<pending-hard-gates>
<agent-instructions>
```

Required content:

- `workflow-run` includes `run_key`, `workflow`, `task`, `module`, `mode`, `status`, and `current_phase`.
- `current-phase` includes the current phase key, name, status, and template instruction when available.
- `phases` lists all phase statuses.
- `pending-hard-gates` lists pending hard gates with phase keys, or `none`.
- `agent-instructions` must remind agents to continue from the current phase and not skip hard gates.

Workflow records audit state. A passed gate means someone recorded that state; it does not prove code correctness.

## SPEC_CONTEXT.md

Required section order:

```text
# SPEC_CONTEXT
<generated-at>
<spec-change>
<document-type sections>
<tasks>
<acceptance>
<bound-workflows>
<agent-instructions>
```

Required content:

- `spec-change` includes `change_key`, `title`, `status`, `module`, `mode`, `priority`, and optional `summary`.
- document sections use their document type as the XML-like tag, for example `<proposal>` or `<design>`.
- `tasks` lists task status, key, title, and optional description, phase, and evidence.
- `acceptance` lists acceptance status, key, description, expected result, and evidence when available.
- `bound-workflows` lists workflow run bindings.
- `agent-instructions` must describe the spec as a task contract, not as proof of correctness.

Spec records change intent, tasks, acceptance criteria, and workflow bindings. It does not automatically validate implementation correctness.

## GOAL_CONTEXT.md

Required section order:

```text
# GOAL_CONTEXT
<generated-at>
<goal>
<current-action>
<next-instruction>
<allowed-actions>
<allowed-commands>
<forbidden-actions>
<required-evidence>
<evidence-contract>
<structured-evidence-fields>
<required-checks>
<context-files>
<completion-blockers>
<freshness-status>
<completion-condition>
<next-command>
```

Required content:

- `goal` includes `goal_key`, `profile`, `task`, `module`, `mode`, `status`,
  `workflow_run`, `spec_change`, and completion condition when available.
- `current-action` contains the only action the agent should perform next.
- `allowed-commands` lists the goal commands permitted by the current protocol.
- `forbidden-actions` lists lower-level bypasses and completion shortcuts that
  remain blocked unless explicitly allowed.
- `evidence-contract` and `structured-evidence-fields` define what the next
  `goal step` must report.
- `completion-blockers` and `freshness-status` explain why completion is not
  yet ready.
- `next-command` gives the next goal command to run.

Goal context is the agent protocol export. It guides the next action; it is not
durable state and must be regenerated after goal state changes.

## GOAL_SUMMARY.md

Required section order:

```text
# GOAL_SUMMARY
<generated-at>
<goal>
<steps>
<checks>
<completion>
<artifacts>
```

Goal summary is a completion artifact. Sensitive values are redacted before the
summary is written, and the redacted result is checked again before export.

## Regeneration

Do not hand-edit exported Markdown to change source state. Regenerate exports from SQLite with the matching CLI:

```bash
dhk goal export --goal <goal-key>
dhk workflow export --run <run-key>
dhk spec export --change <change-key>
```
