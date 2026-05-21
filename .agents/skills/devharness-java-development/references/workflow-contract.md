# Workflow Contract

DevHarness workflow support persists process state for agents. It is not an automatic workflow engine.

## Boundaries

- Workflow templates, runs, phases, gates, and events live in project-local SQLite.
- Agents should normally read workflow state through `CURRENT_CONTEXT.md` via `memory export --include-workflow`.
- `WORKFLOW_CONTEXT.md` is a diagnostic export for the full run state.
- Workflow data must not contain secrets, credentials, JDBC URLs, authorization headers, or obvious user data.

## Hard Gates

- Hard gates are blocking controls, not hints.
- A phase cannot pass while any hard gate for that phase is `pending` or `failed`.
- A hard gate failure sets the run to `blocked`.
- A blocked run can resume when the blocking hard gates for the current phase are passed or waived.
- Waiving a gate requires a reason.

## Command Discipline

- Record real evidence in `workflow gate` and `workflow phase` commands.
- Keep summaries short and non-sensitive.
- Use `--phase` with `workflow gate` when a gate key is ambiguous across phases.
- Create a memory checkpoint after meaningful task progress.
