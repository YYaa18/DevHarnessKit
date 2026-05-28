# Exception Convention

<metadata>
- pack: java-enterprise-core
- domain: exception
- status: alpha
- reviewed_at: 2026-05-28
- source_policy: paraphrased-and-attributed
</metadata>

<inject-summary>
- Match the project exception type and error response mapping.
- Do not swallow exceptions unless the local code already treats them as expected no-op behavior.
- Preserve useful debugging context without leaking sensitive data.
- Record behavior changes for NPE, fallback, retry, or default-value fixes.
</inject-summary>

<iron-law>
Do not hide failure by catching a broad exception without evidence.
</iron-law>

<anti-patterns>
| anti_pattern | risk | preferred |
|---|---|---|
| catch Exception and ignore | hidden failure | narrow handling or rethrow |
| expose internal message | leakage | project-safe error response |
| convert all errors to success | data corruption | explicit fallback policy |
</anti-patterns>

<evidence-checklist>
- Record local exception pattern.
- Record changed failure behavior.
- Verify expected and error paths when possible.
</evidence-checklist>

<knowledge-candidate-triggers>
- Project has a standard business exception or error code convention.
</knowledge-candidate-triggers>

<source-notes>
- Common Java exception handling practice, paraphrased.
</source-notes>
