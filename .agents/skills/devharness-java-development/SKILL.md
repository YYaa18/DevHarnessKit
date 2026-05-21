---
name: devharness-java-development
description: Use DevHarness Kit CLI to load confirmed project memory, run readonly DB checks only when explicitly requested, and save checkpoints after Java development work.
---

# DevHarness Java Development

Use this skill for Java project development in repositories that include DevHarness Kit.

## Required flow

1. Before development, run `scripts/memory-export.sh --task "<task>"` and read `.agents/memory/exports/CURRENT_CONTEXT.md`.
2. If `CURRENT_CONTEXT.md` does not exist, run `scripts/memory-init.sh`, then ask the user or inspect code for facts; do not invent project structure.
3. Treat only confirmed memory as project fact. Never treat draft memory as confirmed.
4. Do not rely on chat history to decide API, MVC, database, gateway, security, or logging conventions.
5. Use `scripts/db-test.sh` or `scripts/db-sql.sh` only when the user explicitly requests business SQL verification.
6. Do not write secrets, tokens, passwords, JDBC URLs, Authorization headers, or raw SQL result sets to memory.
7. After development, run `scripts/memory-checkpoint.sh --task "<task>" --summary "<summary>"` with changed files, pending items, and verification status.
8. New reusable facts should be suggested or written as draft memory with `dhk memory add`; they become exportable only after `dhk memory confirm`.

## References

- `references/memory-contract.md`
- `references/memory-export-format.md`
- `references/recovery-flow.md`
- `references/api-development-flow.md`
- `references/mvc-development-flow.md`
- `references/workflow-contract.md`
- `references/spec-contract.md`
