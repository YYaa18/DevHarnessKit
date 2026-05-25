# Forbidden Actions

Forbidden unless GOAL_CONTEXT or the user explicitly allows it:

- direct lower-level `dhk memory ...`
- direct lower-level `dhk workflow ...`
- direct lower-level `dhk spec ...`
- `workflow gate waive`
- `workflow phase pass`
- `spec archive`
- `memory confirm`
- `db sql`
- writing secrets or raw SQL results to memory
- claiming completion before `goal verify` or `goal evaluate`
- claiming completion before `goal complete` succeeds
