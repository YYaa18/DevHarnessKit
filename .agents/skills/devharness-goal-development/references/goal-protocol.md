# Goal Protocol

1. Use `goal start` for a new task or `goal resume` for an existing open goal.
2. Run `goal next` before each work step.
3. Read `.agents/memory/exports/GOAL_CONTEXT.md`.
4. Perform only the current action.
5. Record work with `goal step`, including every required evidence key listed in GOAL_CONTEXT.
6. Run `goal check --all` before final completion.
7. Run `goal evaluate`.
8. Run `goal complete` only when evaluation returns `ready_to_complete`.

Use wrapper scripts under `scripts/` whenever possible. They route through `dhk.sh` or `dhk.bat` and pass the repository root explicitly, so they work from subdirectories.

Lower-level `memory`, `workflow`, `spec`, and `db` commands are for debugging or explicitly allowed exceptions only.
