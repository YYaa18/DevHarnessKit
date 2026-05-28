# Goal Protocol

1. Use `quickstart --task "<latest user request verbatim>"` for a new user task so the Work Brief, Agent Brief, and goal are created together. Use `goal start` or `goal resume` only for explicit lower-level debugging.
2. If an existing Agent Brief has `current_action` = `completed`, treat it as historical and create a new quickstart goal for the new request before editing.
3. Never copy the task from an old Work Brief or GOAL_CONTEXT when creating a new goal.
4. Run `goal next` before each work step.
5. Read `.agents/memory/exports/GOAL_CONTEXT.md`, especially current action, task, allowed commands, forbidden actions, evidence contract, freshness status, and completion blockers.
6. Before editing, compare GOAL_CONTEXT task with the latest user request. If they do not match, stop and create a new quickstart goal with the latest request.
7. Perform only the current action.
8. Record every investigation, plan, edit, and verification with `goal step`.
9. Prefer structured evidence fields such as `--read-files`, `--changed-files`, `--compile-result`, `--tests-run`, `--risks`, and `--pending`.
10. Run `goal verify` before final completion.
11. If `goal verify` is not ready, follow its `next_command`.
12. Use `goal check --all` plus `goal evaluate` only when debugging readiness.
13. Run `goal complete` only when `goal verify` returns `ready_to_complete`.

Use wrapper scripts under `.agents/skills/devharness-goal-development/scripts/` whenever possible. They route through `dhk.sh` and pass the repository root explicitly, so they work from subdirectories.

Typo and documentation-only edits still require the lightweight patch flow. If an edit happened before a current goal existed, or under a goal whose task does not match the latest request, self-repair by creating a quickstart patch goal for the request and recording the already-made edit as a goal step before claiming completion.

Lower-level `memory`, `workflow`, `spec`, and `db` commands are for debugging or explicitly allowed exceptions only.
