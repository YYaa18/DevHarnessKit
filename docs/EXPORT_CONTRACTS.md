# Export Contracts

DevHarness Kit Markdown files under `.agents/memory/exports/` are generated context artifacts. SQLite remains the source of truth for memory, workflow, spec, and goal state. Work Brief files under `.agents/devharness/briefs/` are an agent-mounted entry layer, not a replacement source of truth.

Alpha agents and scripts may depend on the required sections below. New fields may be added inside a section before 1.0, but existing section names and order should change only with a changelog note.
See [COMPATIBILITY.md](COMPATIBILITY.md) for which exports are beta
versus alpha in the broader release contract.

## WORK_BRIEF.md

Required user-facing anchors:

```text
# Work Brief
## 本次工作
## 建议模式
## 为什么这样建议
## 风险提示
## 预计会做
## 不会做
## 你可以选择
## 是否需要确认
```

The Work Brief must not instruct users to copy low-level `dhk goal ...`
commands. Agent-internal command argv records belong in `AGENT_BRIEF.json`.

## AGENT_BRIEF.json

`AGENT_BRIEF.json` uses `schema_version=devharness-agent-brief/v1-alpha`.
It is machine-readable adapter protocol and remains alpha. It must keep
`execution_policy.show_commands_to_user=false` and every `harness_commands`
entry must include `agent_internal_only=true`.

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
<graph-profile>          optional for graph-aware profiles
<bdd-status>             optional for BDD-required profiles
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
- `bdd-status`, when present, lists goal-bound scenario count, covered/missing/
  pending/failed evidence counts, BDD report paths, and the next BDD command.
- `next-command` gives the next goal command to run.

Goal context is the agent protocol export. It guides the next action; it is not
durable state and must be regenerated after goal state changes.

## GOAL_SUMMARY.md

Required section order:

```text
# GOAL_SUMMARY
<generated-at>
<goal>
<completion-bindings>
<graph-artifacts> optional
<steps>
<checks>
<agent-instructions>
```

Goal summary is a completion artifact. Sensitive values are redacted before the
summary is written, and the redacted result is checked again before export.
The `<completion-bindings>` section includes `ARTIFACT_PASSPORT.json` and the
`dhk artifact passport verify` command so release and CI reviewers can audit
the completion evidence. The passport is evidence metadata, not a substitute
for human review or correctness proof.

## BDD_CONTEXT.md

Required section order:

```text
# BDD_CONTEXT
<generated-at>
<boundary>
<features>
<scenarios>
<agent-instructions>
```

Required content:

- `boundary` explains that BDD rows are specification-level acceptance context,
  generated exports are not source of truth, and evidence/coverage are separate
  checks.
- `features` lists feature key, status, title, and module.
- `scenarios` lists scenario key, status, title, feature, type, priority, tags,
  exported Given/When/Then/And steps, and any spec/goal traceability bindings.
- `agent-instructions` must remind agents not to treat scenarios as proof of
  implementation correctness.

BDD context is generated from SQLite v10 BDD rows. `.feature` files under
`.agents/bdd/features/` are also generated exports.

Scenario binding lines use this shape:

```text
- spec_acceptance <change-key>:<acceptance-key> (verifies)
- goal <goal-key> (supports)
- sql_table <table> (impacts)
- file <path> (impacts)
- symbol <symbol> (impacts)
```

## SCENARIO_IMPACT_MAP.md

Required section order:

```text
# SCENARIO_IMPACT_MAP
<generated-at>
<summary>
<scenario-impact-boundary>
<impact-inputs>
<impact-results>
<related-files>
<related-tests>
<related-sql>
<risk-nodes>
<recommended-read-files>
```

Required content:

- `summary` lists the scenario key, input binding count, impact result count,
  found result count, related file/test counts, and snapshot stale state.
- `scenario-impact-boundary` states that Graph Lite scenario impact is
  advisory and must not be treated as correctness proof.
- `impact-inputs` lists scenario-bound `file`, `symbol`, and `sql_table`
  inputs.
- `impact-results` summarizes each underlying `graph impact` query.
- `related-sql` and `risk-nodes` list node evidence with file, line,
  confidence, source, and evidence fields.
- The report inherits Graph Lite snapshot freshness enforcement.

## BDD_EVIDENCE.md

Required section order:

```text
# BDD_EVIDENCE
<generated-at>
<summary>
<scenario-evidence>
```

Required content:

- `summary` lists scenario, covered, missing, pending, and failed counts.
- `scenario-evidence` lists each selected scenario, optional adapter
  normalization details, and its evidence rows.
- Sensitive values must be rejected before the report is written.

## BDD_COVERAGE.md

Required section order:

```text
# BDD_COVERAGE
<generated-at>
<coverage-summary>
<scenario-coverage>
```

Required content:

- `coverage-summary` lists selected scenario coverage counts and status.
- `scenario-coverage` lists scenario key, evidence status, evidence count, and
  optional adapter normalization details.

## Regeneration

Do not hand-edit exported Markdown to change source state. Regenerate exports from SQLite with the matching CLI:

```bash
dhk goal export --goal <goal-key>
dhk workflow export --run <run-key>
dhk spec export --change <change-key>
dhk bdd export
```
