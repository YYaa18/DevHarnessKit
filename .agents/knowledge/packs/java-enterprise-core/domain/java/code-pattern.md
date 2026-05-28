# Code Pattern

<metadata>
- pack: java-enterprise-core
- domain: code-pattern
- status: alpha
- reviewed_at: 2026-05-28
- source_policy: paraphrased-and-attributed
</metadata>

<inject-summary>
- Follow the closest local naming, helper, layering, and dependency direction.
- Prefer existing utilities before adding new abstractions.
- Keep DTO/entity/domain boundaries consistent with neighboring code.
- Avoid mixing unrelated cleanup with the requested behavior change.
</inject-summary>

<iron-law>
Local project pattern beats generic Java preference.
</iron-law>

<anti-patterns>
| anti_pattern | risk | preferred |
|---|---|---|
| New helper for one use | complexity | inline or reuse local helper |
| DTO/entity mixing | data leakage | follow existing boundaries |
| Broad cleanup | review noise | isolate requested behavior |
</anti-patterns>

<evidence-checklist>
- Record existing pattern summary.
- Record changed files and reason.
- Record any boundary exception explicitly.
</evidence-checklist>

<knowledge-candidate-triggers>
- Stable local helper or layering rule discovered.
</knowledge-candidate-triggers>

<source-notes>
- Common maintainability and local-pattern engineering practice.
</source-notes>
