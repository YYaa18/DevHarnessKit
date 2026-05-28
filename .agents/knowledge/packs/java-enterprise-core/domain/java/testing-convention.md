# Testing Convention

<metadata>
- pack: java-enterprise-core
- domain: testing
- status: alpha
- reviewed_at: 2026-05-28
- source_policy: paraphrased-and-attributed
</metadata>

<inject-summary>
- Verification evidence must match the configured project mode: auto, manual, or disabled.
- For manual IDE verification, record scope, evidence path, tester, and result.
- For patch work, at least verify the touched behavior and sensitive-data scan.
- When automated tests are unavailable, record explicit risk and rollback evidence.
</inject-summary>

<iron-law>
A skipped test is not proof; it is either accepted policy, manual evidence, or a risk.
</iron-law>

<anti-patterns>
| anti_pattern | risk | preferred |
|---|---|---|
| Claim manual pass without artifact | unverifiable | attach evidence path |
| Ignore skipped compile/test | false readiness | use manual verification interaction |
| Run expensive tests against policy | slow or unsafe | follow configured verification mode |
</anti-patterns>

<evidence-checklist>
- Record compile_result and test_result.
- Record manual_evidence_path when verification is manual.
- Record risk and rollback plan when verification is waived.
</evidence-checklist>

<knowledge-candidate-triggers>
- Project has a repeatable IDE/manual verification workflow.
- Project has module-specific test command conventions.
</knowledge-candidate-triggers>

<source-notes>
- Original DevHarnessKit verification policy guidance.
- Common CI/manual testing evidence practice.
</source-notes>
