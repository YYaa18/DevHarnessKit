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
- editing files when Agent Brief is completed and no new quickstart goal exists
- treating typo or documentation-only edits as exempt from goal tracking
- creating a new goal with a task copied from an old Work Brief or GOAL_CONTEXT
- completing when GOAL_CONTEXT task does not match the latest user request
- writing secrets or raw SQL results to memory
- claiming completion before `goal verify` or `goal evaluate`
- claiming completion before `goal complete` succeeds
