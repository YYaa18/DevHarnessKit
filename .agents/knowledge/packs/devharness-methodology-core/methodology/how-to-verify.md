# How To Verify

<metadata>
- pack: devharness-methodology-core
- domain: methodology
- status: alpha
- reviewed_at: 2026-05-28
- source_policy: original-devharness-guidance
</metadata>

<inject-summary>
- Verification must be evidence, not confidence language.
- Prefer automated checks when configured; use manual evidence only when project policy requires it.
- If a check cannot run, record explicit risk, approver, and rollback evidence instead of hiding the gap.
- Re-run stale checks after later goal steps or workspace changes.
</inject-summary>

<iron-law>
Do not claim completion while required checks are stale, missing, or unaccepted by policy.
</iron-law>

<anti-patterns>
| anti_pattern | risk | preferred |
|---|---|---|
| "Looks good" | unverifiable | attach command/manual evidence |
| Ignore stale check | false readiness | refresh verify after changes |
| Waive without reason | audit gap | record reason, approver, risk, rollback |
</anti-patterns>

<evidence-checklist>
- Record compile/test/sensitive status.
- Record manual evidence path if verification is manual.
- Record waived risk and rollback plan when checks are waived.
</evidence-checklist>

<knowledge-candidate-triggers>
- Project-specific manual verification convention.
- Reusable check failure or recovery lesson.
</knowledge-candidate-triggers>

<source-notes>
- Original DevHarnessKit methodology guidance.
</source-notes>
