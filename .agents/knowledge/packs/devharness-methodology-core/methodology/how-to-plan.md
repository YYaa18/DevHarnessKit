# How To Plan

<metadata>
- pack: devharness-methodology-core
- domain: methodology
- status: alpha
- reviewed_at: 2026-05-28
- source_policy: original-devharness-guidance
</metadata>

<inject-summary>
- Keep the plan proportional to the task risk and expected blast radius.
- Prefer the smallest change that satisfies the requested behavior and preserves local style.
- For medium or high risk, name rollback path and verification evidence before coding.
- Separate soft escalation from hard blockers such as protected files or security-sensitive changes.
</inject-summary>

<iron-law>
A plan is not complete until it names how the change will be verified.
</iron-law>

<anti-patterns>
| anti_pattern | risk | preferred |
|---|---|---|
| One-size strict workflow | slow small fixes | choose patch/standard/strict by risk |
| No rollback thought | hard recovery | record rollback path for risky changes |
| Hidden scope expansion | trust loss | state impacted files/modules |
</anti-patterns>

<evidence-checklist>
- Record impacted files or modules.
- Record risk points and verification plan.
- Record escalation reason when mode changes.
</evidence-checklist>

<knowledge-candidate-triggers>
- Reusable planning checklist for this project.
- Project-specific release or rollback rule.
</knowledge-candidate-triggers>

<source-notes>
- Original DevHarnessKit methodology guidance.
</source-notes>
