# Goal Protocol

1. Use `goal start` for a new task or `goal resume` for an existing open goal.
2. Run `goal next` before each work step.
3. Read `.agents/memory/exports/GOAL_CONTEXT.md`, especially current action, allowed commands, forbidden actions, evidence contract, freshness status, and completion blockers.
4. Perform only the current action.
5. Record every investigation, plan, edit, and verification with `goal step`.
6. Prefer structured evidence fields such as `--read-files`, `--changed-files`, `--compile-result`, `--tests-run`, `--risks`, and `--pending`.
7. Run `goal verify` before final completion.
8. If `goal verify` is not ready, follow its `next_command`.
9. Use `goal check --all` plus `goal evaluate` only when debugging readiness.
10. Run `goal complete` only when `goal verify` returns `ready_to_complete`.

Use wrapper scripts under `.agents/skills/devharness-goal-development/scripts/` whenever possible. They route through `dhk.sh` and pass the repository root explicitly, so they work from subdirectories.

Lower-level `memory`, `workflow`, `spec`, and `db` commands are for debugging or explicitly allowed exceptions only.
