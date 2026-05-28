# API Convention

<metadata>
- pack: java-enterprise-core
- domain: api
- status: alpha
- reviewed_at: 2026-05-28
- source_policy: paraphrased-and-attributed
</metadata>

<inject-summary>
- Match the existing controller route style, response wrapper, validation style, and error mapping.
- Keep request and response changes backward compatible unless the task explicitly asks otherwise.
- Validate inputs near the boundary according to local project patterns.
- Record impacted clients or endpoints when changing API shape.
</inject-summary>

<iron-law>
Do not invent a new API style when the module already has a local contract.
</iron-law>

<anti-patterns>
| anti_pattern | risk | preferred |
|---|---|---|
| New response shape by preference | client breakage | mirror local response wrapper |
| Skip validation | bad data | use project validation pattern |
| Change endpoint semantics silently | regressions | record compatibility impact |
</anti-patterns>

<evidence-checklist>
- Record existing endpoint pattern.
- Record changed request/response fields.
- Record compatibility and verification evidence.
</evidence-checklist>

<knowledge-candidate-triggers>
- Project has a stable response wrapper or validation convention.
</knowledge-candidate-triggers>

<source-notes>
- Common Spring MVC / Java API design practice, paraphrased.
</source-notes>
