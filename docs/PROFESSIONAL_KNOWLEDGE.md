# Professional Knowledge Layer

DevHarness Kit can inject small professional knowledge snippets into the agent
workflow. The layer is advisory: current project code, confirmed memory, and
fresh evidence remain the source of truth.

## Layers

- Methodology pack: how to inspect, plan, implement, and verify.
- Domain pack: Java enterprise rules for API, database, security, testing,
  logging, exceptions, and local code patterns.
- Knowledge candidate bridge: rules can create draft candidates that users may
  confirm into project memory or personal growth notes.

## Files

```text
.agents/knowledge/packs/<pack-key>/knowledge-pack.json
.agents/knowledge/packs/<pack-key>/**/*.md
```

Each pack declares a manifest with source policy, license notes, reviewed date,
entry files, applicable actions, risk flags, and line budgets.

## Injection Points

- `GOAL_CONTEXT.md` includes `<professional-knowledge>` for agent execution.
- `WORK_BRIEF.md` includes short professional notes for users.
- `AGENT_BRIEF.json` includes `knowledge_context` with rule IDs and `full_ref`
  values for agent-internal lookup.

## Copyright And Source Policy

Knowledge packs must be written as original summaries or paraphrased guidance.
Do not copy commercial books, third-party prompt packs, repository text with an
unknown license, or long passages from public documentation. Keep source notes
short and attribution-oriented.

## Conflict Rule

If a knowledge snippet conflicts with current code or confirmed memory, inspect
the project and prefer current project evidence.
