# DevHarness Kit

DevHarness Kit is a local Java CLI for project memory, short context export, and controlled development handoffs for coding agents.

The goal is not to make a model remember an entire project. The goal is to let the project keep durable, auditable context in local files and SQLite, then give the agent only the short context it needs for the current task.

## Current Status

This repository is in early MVP development.

Implemented in the current codebase:

- Maven-based Java CLI.
- `dhk help`
- `dhk doctor`
- `dhk memory init`
- `dhk memory add`
- `dhk memory confirm`
- `dhk memory search`
- `dhk memory export`
- `dhk memory checkpoint`
- `dhk memory recover`
- `dhk db test`
- `dhk db sql`
- `dhk db sql --dry-run`
- `dhk workflow template seed/list/show`
- `dhk workflow start/status/export`
- `dhk workflow phase pass/fail`
- `dhk workflow gate pass/fail/waive`
- SQLite migration and FTS fallback.
- SQLite workflow persistence schema v2 for templates, runs, phases, gates, and events.
- Sensitive-data guard for memory content.
- SQL safety guard for readonly query checks.
- Skill/Rules packaging for `.agents/skills` and `.comate/rules`.
- Integration tests for the memory workflow and DB SQL dry-run safety.
- Optional live MySQL smoke tests when `DHK_TEST_MYSQL_URL`, `DHK_TEST_MYSQL_USER`, and `DHK_TEST_MYSQL_PASSWORD` are set.
- GitHub Actions CI for Maven test/package.

Planned but not part of the current implementation yet:

- Workflow artifact/binding tables.
- V0.3 spec persistence.

See [docs/PRD.md](docs/PRD.md) and [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md) for the full product and implementation design.

## Design Summary

DevHarness Kit uses a small local CLI and project-scoped storage:

```text
Agent Skill / Rule
        ↓
DevHarness Kit CLI
        ↓
SQLite memory.db + controlled readonly DB query
        ↓
CURRENT_CONTEXT.md / RECOVERY_CONTEXT.md / SQL_RESULT.md
        ↓
Agent reads short Markdown context
```

The first MVP is split into two layers:

- **MVP-A: Memory Core**
  Project memory in SQLite, manual draft-to-confirmed flow, short Markdown export, checkpoint and recovery.

- **MVP-B: DB Readonly**
  Readonly MySQL query support for understanding business SQL, with fail-closed safety checks and result-size limits.

- **V0.2: Workflow Persistence**
  SQLite-backed workflow templates, runs, phases, gates, events, and workflow context export. This is not a workflow engine; commands persist and render process state for agents.

## Requirements

- Java 8+ runtime.
- Maven for development and packaging.

## Build

```bash
mvn clean test
mvn -DskipTests package
```

The shaded CLI jar is generated as:

```text
target/dhk-cli-0.1.0-all.jar
```

Run the local performance smoke after packaging:

```bash
scripts/perf-smoke.sh
```

`scripts/perf-smoke.sh` is a developer validation helper and requires the `sqlite3` CLI to bulk-load sample rows. The packaged DevHarness Kit CLI does not require `sqlite3` at runtime.

## Quick Start

Initialize project memory:

```bash
java -jar target/dhk-cli-0.1.0-all.jar memory init --project-root .
```

Add a draft memory:

```bash
java -jar target/dhk-cli-0.1.0-all.jar memory add \
  --project-root . \
  --type gateway_convention \
  --module global \
  --title "User identity comes from gateway" \
  --content "User ID is read from X-User-Id. Business services do not parse tokens." \
  --tags "api,gateway,user-id,header"
```

Confirm it:

```bash
java -jar target/dhk-cli-0.1.0-all.jar memory confirm --project-root . --id 1
```

Export current context:

```bash
java -jar target/dhk-cli-0.1.0-all.jar memory export \
  --project-root . \
  --task "Implement order query endpoint" \
  --module order \
  --mode api \
  --keywords "gateway,mybatis,mysql"
```

Seed workflow templates and start a workflow run:

```bash
java -jar target/dhk-cli-0.1.0-all.jar workflow template seed --project-root .

java -jar target/dhk-cli-0.1.0-all.jar workflow start \
  --project-root . \
  --workflow api-change \
  --task "Implement order query endpoint" \
  --module order \
  --mode api
```

Export workflow context or include a workflow run in `CURRENT_CONTEXT.md`:

```bash
java -jar target/dhk-cli-0.1.0-all.jar workflow export --project-root . --run <run-key>

java -jar target/dhk-cli-0.1.0-all.jar memory export \
  --project-root . \
  --task "Implement order query endpoint" \
  --module order \
  --include-workflow <run-key>
```

## Safety Model

- `memory add` writes draft memory only.
- Confirmed facts must pass through `memory confirm`.
- Default exports include confirmed memory only.
- Sensitive values such as passwords, bearer tokens, JDBC URLs, access keys, and obvious user data are rejected.
- Raw SQL results are not stored as long-term memory.

## License

MIT
