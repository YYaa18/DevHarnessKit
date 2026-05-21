# Memory Export Format

`CURRENT_CONTEXT.md` contains XML-like sections:

- `generated-at`
- `task`
- `project-summary`
- `must-follow`
- `relevant-conventions`
- `recent-checkpoint`
- `agent-instructions`

Agents should read this file before planning or editing. Absence of a section means the fact is unknown, not that a convention can be guessed.
