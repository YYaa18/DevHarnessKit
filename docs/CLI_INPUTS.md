# CLI Input Options

DevHarness Kit supports inline text for short values and file/stdin input for longer content.

## Memory Content

Use exactly one content source:

```bash
dhk memory add --type project_fact --title "Rule" --content "Short text"
dhk memory add --type project_fact --title "Rule" --content-file notes.md
cat notes.md | dhk memory add --type project_fact --title "Rule" --content-stdin
```

## Checkpoint Summary

Use exactly one summary source:

```bash
dhk memory checkpoint --task "Order API" --summary "Short summary"
dhk memory checkpoint --task "Order API" --summary-file checkpoint.md
cat checkpoint.md | dhk memory checkpoint --task "Order API" --summary-stdin
```

## SQL

Use exactly one SQL source:

```bash
dhk db sql --dry-run --sql "SELECT 1"
dhk db sql --dry-run --sql-file query.sql
cat query.sql | dhk db sql --dry-run --sql-stdin
```

`--sql-file` and `--sql-stdin` still pass through the same SQL safety guard as `--sql`.

## Spec Documents

Use exactly one document source:

```bash
dhk spec document set --change order-api --type design --content "Short design"
dhk spec document set --change order-api --type design --content-file design.md
cat design.md | dhk spec document set --change order-api --type design --content-stdin
```

`--file` remains supported as a compatibility alias for spec documents.

## Search Explain

Use `--explain` to show the score and weighted match fields:

```bash
dhk memory search --q gateway --explain
```

The score is a ranking aid, not a stable public API.

## JSON Output

Use `--json` for machine-readable output:

```bash
dhk doctor --json
dhk memory search --q gateway --json
dhk memory export --task "Order API" --module order --json
dhk goal status --goal <goal-key> --json
dhk goal check --goal <goal-key> --all --json
dhk goal evaluate --goal <goal-key> --json
dhk goal complete --goal <goal-key> --json
```

JSON output is intended for scripts and agents. It is still alpha and may expand before 1.0, but existing field names should be changed conservatively.

## Backup

Before risky upgrades or migrations, create a memory backup:

```bash
dhk memory backup --project-root .
dhk memory backup --project-root . --out /tmp/dhk-memory.zip
```

The backup zip includes `project.json`, `memory.db`, and files directly under `exports/`.

Commands that migrate an existing older `memory.db` also create a pre-migration backup automatically under `.agents/memory/backups/`.

## Goal Configuration

Goal profiles and checks can be configured per project:

```text
.agents/devharness/goal-profiles/<profile>.json
.agents/devharness/goal-check-policy.json
```

See [GOAL_CONFIGURATION.md](GOAL_CONFIGURATION.md) for the supported fields.

## Sensitive Policy

Project-level sensitive-data handling is configured in:

```text
.agents/devharness/sensitive-policy.json
```

Use `redact` for common business PII that should not hard-block development:

```json
{
  "email": "redact",
  "phone": "redact",
  "identity_number": "redact"
}
```

See [SENSITIVE_POLICY.md](SENSITIVE_POLICY.md) for supported keys and guidance.
