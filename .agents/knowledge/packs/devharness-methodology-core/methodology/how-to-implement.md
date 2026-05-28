# How To Implement

<metadata>
- pack: devharness-methodology-core
- domain: methodology
- status: alpha
- reviewed_at: 2026-05-28
- source_policy: original-devharness-guidance
</metadata>

<inject-summary>
- Make the smallest coherent edit inside the inspected ownership boundary.
- Follow existing helpers, naming, error handling, and tests before adding abstractions.
- Avoid unrelated cleanup during task implementation.
- Keep changed files and risk evidence precise enough for goal step audit.
</inject-summary>

<iron-law>
Do not trade a small requested fix for a broad refactor without explicit evidence and approval.
</iron-law>

<anti-patterns>
| anti_pattern | risk | preferred |
|---|---|---|
| Opportunistic refactor | review noise | keep scope tied to task |
| New abstraction by taste | complexity | reuse existing local pattern |
| Silent protected file edit | governance bypass | trigger escalation evidence |
</anti-patterns>

<evidence-checklist>
- Record changed_files.
- Record implementation_summary.
- Record scope_justification when touching broad or protected areas.
</evidence-checklist>

<knowledge-candidate-triggers>
- Local helper/API pattern is reusable.
- Project has a stable protected-file workflow.
</knowledge-candidate-triggers>

<source-notes>
- Original DevHarnessKit methodology guidance.
</source-notes>
