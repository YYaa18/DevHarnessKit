# How To Inspect

<metadata>
- pack: devharness-methodology-core
- domain: methodology
- status: alpha
- reviewed_at: 2026-05-28
- source_policy: original-devharness-guidance
</metadata>

<inject-summary>
- Read the smallest current-code slice that can prove the existing behavior.
- Prefer local project evidence over generic assumptions or old chat context.
- Identify entry point, data path, existing tests, and nearest conventions before planning.
- Record what was inspected and what remains unknown.
</inject-summary>

<iron-law>
Do not plan from memory when current project evidence is available.
</iron-law>

<anti-patterns>
| anti_pattern | risk | preferred |
|---|---|---|
| Assume framework convention | wrong change shape | inspect local controller/service/mapper/test |
| Read too broadly | context waste | follow graph/impact or nearest module first |
| Skip existing tests | weak verification plan | locate nearest tests before implementation |
</anti-patterns>

<evidence-checklist>
- List read files or graph impact refs.
- Summarize existing pattern and local naming.
- Note unknowns that require user confirmation.
</evidence-checklist>

<knowledge-candidate-triggers>
- Stable local project convention discovered.
- Repeated verification habit discovered.
</knowledge-candidate-triggers>

<source-notes>
- Original DevHarnessKit methodology guidance.
</source-notes>
