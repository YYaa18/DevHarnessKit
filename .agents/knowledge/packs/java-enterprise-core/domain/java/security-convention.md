# Security Convention

<metadata>
- pack: java-enterprise-core
- domain: security
- status: alpha
- reviewed_at: 2026-05-28
- source_policy: paraphrased-and-attributed
</metadata>

<inject-summary>
- Treat permission, payment, identity, token, and audit changes as high risk.
- Verify authorization at the boundary where the current project already enforces it.
- Do not log secrets, credentials, tokens, raw identity numbers, or raw sensitive payloads.
- Prefer explicit deny or no-op behavior when risk cannot be verified.
- Record manual approval evidence for security-sensitive scope expansion.
</inject-summary>

<iron-law>
Do not weaken authorization or expose sensitive data to make a flow easier.
</iron-law>

<anti-patterns>
| anti_pattern | risk | preferred |
|---|---|---|
| Add endpoint without auth check | privilege bypass | mirror local authorization pattern |
| Log full request payload | sensitive leak | log safe identifiers only |
| Catch and ignore security errors | hidden failure | explicit failure or approval evidence |
</anti-patterns>

<evidence-checklist>
- Identify the existing authorization pattern.
- Record sensitive-data handling evidence.
- Record approval evidence for protected/security changes.
</evidence-checklist>

<knowledge-candidate-triggers>
- Project has a stable permission annotation or interceptor convention.
- Project has a redaction/audit logging rule worth confirming.
</knowledge-candidate-triggers>

<source-notes>
- OWASP secure coding and access control guidance, paraphrased.
- Common enterprise audit and least-privilege practice.
</source-notes>
