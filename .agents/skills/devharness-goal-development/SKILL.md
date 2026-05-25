---
name: devharness-goal-development
description: Use DevHarnessKit goal protocol for controlled coding tasks. Strict flow for weaker models and auditable AI development.
---

# DevHarness Goal Development

Use this skill for code changes in repositories that include DevHarnessKit.

## Required Protocol

1. Do not start by editing code.
2. Start or resume a goal.
3. Run `scripts/goal-next.sh` before each work step.
4. Read `.agents/memory/exports/GOAL_CONTEXT.md`.
5. Perform only the `current_action` from GOAL_CONTEXT.
6. Do not use lower-level `memory`, `workflow`, `spec`, or `db` commands unless GOAL_CONTEXT explicitly allows it.
7. After investigation or code edits, run `scripts/goal-step.sh` with summary, changed files, and the required evidence keys from GOAL_CONTEXT.
8. Before claiming completion, run `scripts/goal-check.sh --all`.
9. Then run `scripts/goal-evaluate.sh`.
10. Only run `scripts/goal-complete.sh` when evaluation returns `ready_to_complete`.

Use the wrapper scripts instead of composing raw `dhk goal ...` commands; the wrappers pass the repository root even when invoked from a subdirectory.

## Forbidden By Default

- Do not manually pass or waive workflow gates.
- Do not archive specs.
- Do not confirm memory.
- Do not run DB SQL.
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
