# Professional Knowledge Injection

Professional knowledge is injected by `GOAL_CONTEXT.md`, `WORK_BRIEF.md`, and
`AGENT_BRIEF.json`.

## Rules

- Treat professional knowledge as advisory guidance, not project fact.
- Prefer current project code and confirmed memory when they conflict with a
  knowledge pack rule.
- Do not load entire knowledge packs unless GOAL_CONTEXT references a
  `full_ref` and the current action requires it.
- Do not paste knowledge pack internals to the user when Work Brief already
  provides a user-facing summary.
- Do not confirm generated knowledge candidates without user review.

## Locations

- Knowledge packs: `.agents/knowledge/packs/*`
- User summary: `.agents/devharness/briefs/WORK_BRIEF.md`
- Agent refs: `.agents/devharness/briefs/AGENT_BRIEF.json`
- Goal snippets: `.agents/memory/exports/GOAL_CONTEXT.md`
