# Logging Convention

<metadata>
- pack: java-enterprise-core
- domain: logging
- status: alpha
- reviewed_at: 2026-05-28
- source_policy: paraphrased-and-attributed
</metadata>

<inject-summary>
- Log stable identifiers and decision points, not raw sensitive payloads.
- Keep audit/security logs consistent with existing project categories.
- Avoid noisy logs in loops or hot paths unless explicitly needed.
- Redact personal, credential, token, and secret values before logging.
</inject-summary>

<iron-law>
Never add a log statement that can expose credentials or sensitive personal data.
</iron-law>

<anti-patterns>
| anti_pattern | risk | preferred |
|---|---|---|
| Log full DTO/request | sensitive leakage | log safe id and status |
| Log inside high-volume loop | noise/perf cost | aggregate or debug-gate |
| New audit format | weak operations | follow project audit convention |
</anti-patterns>

<evidence-checklist>
- Check sensitive-data risk in new logs.
- Record audit/logging convention when touched.
- Run sensitive guard after logging changes.
</evidence-checklist>

<knowledge-candidate-triggers>
- Project has a stable audit logging field set.
</knowledge-candidate-triggers>

<source-notes>
- OWASP logging and sensitive data handling guidance, paraphrased.
- Common enterprise audit logging practice.
</source-notes>
