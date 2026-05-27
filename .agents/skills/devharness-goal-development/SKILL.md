---
name: devharness-goal-development
description: Use DevHarnessKit goal protocol for controlled coding tasks. Strict flow for weaker models and auditable AI development.
---

# DevHarness Goal Development

Use this skill for code changes in repositories that include DevHarnessKit. Development work is goal-first by default.

## Contract Summary

This skill is governed by `contract.json`.

- skill_key: `devharness-goal-development`
- data_access_level: `context`
- allowed_commands: `dhk goal start`, `dhk goal resume`, `dhk goal next`, `dhk goal step`, `dhk goal verify`, `dhk goal complete`, `dhk goal audit`, `dhk goal recheck`
- forbidden_commands: `dhk workflow gate waive`, `dhk spec archive`, `dhk memory confirm`, `dhk db sql`

Do not use commands outside this contract unless `GOAL_CONTEXT.md` explicitly authorizes them or the user directly requests them.

## Core Path

For ordinary code tasks, keep the flow simple:

1. `goal-start.sh` or `goal-resume.sh` creates or restores the task.
2. `goal-next.sh` tells you the current action and required evidence.
3. Do only that action, then record the work with `goal-step.sh`.
4. `goal-verify.sh` runs the readiness checks.
5. `goal-complete.sh` is allowed only after verification reports
   `ready_to_complete`.

The full protocol below keeps weak-model and release-sensitive work auditable.

## Full Protocol

1. Do not start by editing code.
2. Start a new goal with the skill wrapper `scripts/goal-start.sh`, or resume the current goal with `scripts/goal-resume.sh`. From the repository root, call them as `.agents/skills/devharness-goal-development/scripts/goal-start.sh` and `.agents/skills/devharness-goal-development/scripts/goal-resume.sh`.
3. Run the skill wrapper `scripts/goal-next.sh` before each work step. When you
   need machine-readable action/evidence data, run the underlying
   `dhk goal next --json` through the wrapper only if the wrapper supports it;
   otherwise read `GOAL_CONTEXT.md`.
4. Read `.agents/memory/exports/GOAL_CONTEXT.md`.
5. Perform only the `current_action` from GOAL_CONTEXT, and stay within its `<allowed-commands>`, `<forbidden-actions>`, `<evidence-contract>`, and `<freshness-status>` sections.
   - If `<required-graph-action>` is present, run the listed graph command first and do not skip to code edits.
   - `graph_index_export` means run graph index/export, then run `goal next` again.
   - `graph_impact` means generate `IMPACT_MAP.md`, then run `goal next` again.
6. Do not use lower-level `memory`, `workflow`, `spec`, or `db` commands unless GOAL_CONTEXT explicitly allows it.
7. After every investigation, plan, edit, or verification step, run `scripts/goal-step.sh` with summary, structured fields, changed files, and the required evidence keys from GOAL_CONTEXT.
   - Prefer structured evidence fields and `dhk goal step --field key=value`
     over long hand-written evidence strings when using the CLI directly.
   - Use `dhk goal step --template --goal <goal>` to inspect the current
     action's required evidence before recording a step.
   - Use `dhk goal step --dry-run ...` to validate evidence without writing a
     `goal_step` row when evidence shape is uncertain.
   - Before implementation, include `goal_understanding=<summary>` and either `assumptions=<summary>` or `pending=<questions-or-none>` in step evidence. The `think-before-coding` gate fails when this goal-understanding evidence is missing.
   - Keep each `goal step` aligned to the current GOAL_CONTEXT action. The `goal-driven` gate checks recorded action order and evidence.
   - For broad or protected-file changes, include `scope_justification=<why-needed>`, `impact_evidence=<artifact-or-summary>`, and `protected_file_confirmation=approved|confirmed|passed` when applicable. The `simplicity` and `surgical-change` gates flag unsupported scope drift.
8. During development, `dhk goal verify --level fast` may be used as a quick
   preflight, but it is not completion proof.
9. Before claiming completion, run `scripts/goal-verify.sh`; if it is not ready, follow its `next_command`.
   - If verification reports `think-before-coding`, `goal-driven`, `simplicity`, or `surgical-change` as failed or pending, review the failure details and address them before retrying. Add missing evidence or reduce the change scope, then rerun `goal verify`.
10. Only run `scripts/goal-complete.sh` when standard `goal verify` returns
    `ready_to_complete`. For release review, prefer
    `dhk goal verify --level release` before completing or tagging. Use
    `scripts/goal-check.sh --all` plus `scripts/goal-evaluate.sh` only when
    debugging readiness.
11. After a goal is completed, use `dhk goal audit --goal <goal>` for read-only
    review and `dhk goal recheck --goal <goal>` to refresh checks without
    reopening the goal.

Use the wrapper scripts instead of composing raw `dhk goal ...` commands; the wrappers pass the repository root even when invoked from a subdirectory. In this repo they live under `.agents/skills/devharness-goal-development/scripts/`.

## Forbidden By Default

- Do not manually pass or waive workflow gates.
- Do not archive specs.
- Do not confirm memory.
- Do not run DB SQL.
- Do not bypass `goal` by calling lower-level workflow/spec/db commands unless GOAL_CONTEXT explicitly permits that action.
- Do not write secrets, tokens, passwords, JDBC URLs, Authorization headers, cookies, or raw SQL results to memory.
- Do not claim completion while goal evaluation is not ready.

## Required Final Self-Check

End each response with:

```text
DevHarness self-check:
- goal:
- current_action:
- changed_files:
- checks:
- completion:
```

## References

- `references/goal-protocol.md`
- `references/evidence-format.md`
- `references/self-check-format.md`
- `references/forbidden-actions.md`
