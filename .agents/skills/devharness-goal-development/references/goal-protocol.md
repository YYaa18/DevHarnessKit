# Goal Protocol

1. Use `goal start` for a new task or `goal resume` for an existing open goal.
2. Run `goal next` before each work step.
3. Read `.agents/memory/exports/GOAL_CONTEXT.md`.
4. Perform only the current action.
5. Record every investigation, plan, edit, and verification with `goal step`.
6. Prefer structured evidence fields such as `--read-files`, `--changed-files`, `--compile-result`, `--tests-run`, `--risks`, and `--pending`.
7. Run `goal verify` before final completion.
8. Use `goal check --all` plus `goal evaluate` only when debugging readiness.
9. Run `goal complete` only when verify/evaluate returns `ready_to_complete`.

Use wrapper scripts under `scripts/` whenever possible. They route through `dhk.sh` or `dhk.bat` and pass the repository root explicitly, so they work from subdirectories.

Lower-level `memory`, `workflow`, `spec`, and `db` commands are for debugging or explicitly allowed exceptions only.
