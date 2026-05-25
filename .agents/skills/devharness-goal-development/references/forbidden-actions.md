# Forbidden Actions

Forbidden unless GOAL_CONTEXT or the user explicitly allows it:

- `workflow gate waive`
- `workflow phase pass`
- `spec archive`
- `memory confirm`
- `db sql`
- writing secrets or raw SQL results to memory
- claiming completion before `goal evaluate` and `goal complete`
