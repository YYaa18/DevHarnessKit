# Database Convention

<metadata>
- pack: java-enterprise-core
- domain: database
- status: alpha
- reviewed_at: 2026-05-28
- source_policy: paraphrased-and-attributed
</metadata>

<inject-summary>
- Do not concatenate untrusted input into SQL.
- In MyBatis XML, prefer safe parameter binding and inspect any dynamic SQL carefully.
- Keep transaction boundaries in the service layer unless local project code proves another convention.
- Check mapper calls inside loops for N+1 query risk.
- If SQL changes, record binding and result verification evidence.
</inject-summary>

<iron-law>
Never build SQL by concatenating untrusted input.
</iron-law>

<anti-patterns>
| anti_pattern | risk | preferred |
|---|---|---|
| String-built SQL from request data | SQL injection | safe parameter binding |
| Mapper call inside loop | N+1 query | batch query or join |
| Transaction in controller | unclear boundary | service-layer transaction |
</anti-patterns>

<evidence-checklist>
- Search changed SQL/XML for unsafe interpolation.
- Check whether mapper calls were added inside loops.
- Record transaction boundary when data changes.
- Attach SQL or repository verification evidence.
</evidence-checklist>

<knowledge-candidate-triggers>
- Project has a stable mapper XML parameter pattern.
- SQL changes require a recurring manual verification habit.
</knowledge-candidate-triggers>

<source-notes>
- MyBatis public documentation on parameter binding, paraphrased.
- OWASP SQL injection guidance, paraphrased.
- Common Java enterprise service-layer transaction practice.
</source-notes>
