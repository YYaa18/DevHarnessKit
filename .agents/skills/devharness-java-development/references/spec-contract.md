# Spec Contract

DevHarness spec data is a local SQLite-backed task contract for agent work.

## Scope

- `spec_change` defines what should change and why.
- `spec_document` stores proposal, design, requirements, tasks, acceptance, verification, or notes content.
- `spec_task` tracks executable work items.
- `spec_acceptance` tracks acceptance criteria.
- `workflow_spec_binding` links a spec change to workflow runs.
- `spec_event` records audit events.

Markdown is an export format only:

- `SPEC_CONTEXT.md` is the full spec context.
- `CURRENT_CONTEXT.md` may include a short `<spec-context>` summary through `memory export --include-spec`.

## Safety

- Spec commands must reject sensitive data before writing or exporting.
- Do not store secrets, credentials, JDBC URLs, authorization headers, raw SQL results, or obvious user data in spec content.
- Long proposal/design content belongs in `SPEC_CONTEXT.md`, not inline in `CURRENT_CONTEXT.md`.

## Agent Rules

- Treat `SPEC_CONTEXT.md` as the task contract.
- Do not implement outside the stated spec scope.
- Update `spec task` and `spec acceptance` status after work.
- Archive a spec only when tasks are done/skipped and acceptance is passed/waived.
