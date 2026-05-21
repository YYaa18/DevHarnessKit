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
- `dhk workflow artifact list`
- `dhk workflow bind-memory`
- `dhk workflow bind-checkpoint`
- `dhk workflow summary`
- `dhk spec create`
- `dhk spec document set`
- `dhk spec task add/update`
- `dhk spec acceptance add/update`
- `dhk spec status/export/archive`
- `dhk spec bind-workflow`
- SQLite migration and FTS fallback.
- SQLite workflow persistence schema v2 for templates, runs, phases, gates, and events.
- SQLite workflow audit schema v3 for artifacts, exported memory bindings, and checkpoint bindings.
- SQLite spec persistence schema v4 for changes, documents, tasks, acceptance, events, and workflow bindings.
- Sensitive-data guard for memory, workflow, and spec persisted content.
- SQL safety guard for readonly query checks.
- Skill/Rules packaging for `.agents/skills` and `.comate/rules`.
- Integration tests for memory, workflow, spec, artifact binding, and DB SQL dry-run safety.
- Optional live MySQL smoke tests when `DHK_TEST_MYSQL_URL`, `DHK_TEST_MYSQL_USER`, and `DHK_TEST_MYSQL_PASSWORD` are set.
- GitHub Actions CI for Maven test/package.

Planned but not part of the current implementation yet:

- V0.3 follow-up automation around spec/workflow coordination.

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
SPEC_CONTEXT.md / WORKFLOW_CONTEXT.md
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

- **V0.2-B: Workflow Artifacts and Bindings**
  Audit links between workflow runs, generated context files, exported memory, and checkpoints.

- **V0.3: Spec Persistence**
  SQLite-backed spec changes, proposal/design documents, tasks, acceptance criteria, workflow binding, and `SPEC_CONTEXT.md` export. Markdown is an export format for agents, not the source of truth.

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

java -jar target/dhk-cli-0.1.0-all.jar workflow artifact list --project-root . --run <run-key>

java -jar target/dhk-cli-0.1.0-all.jar workflow summary --project-root . --run <run-key>
```

Create a spec change and bind it to a workflow run:

```bash
java -jar target/dhk-cli-0.1.0-all.jar spec create \
  --project-root . \
  --change order-query-api \
  --title "Implement order query endpoint" \
  --summary "Provide paginated order search for the frontend" \
  --module order \
  --mode api

java -jar target/dhk-cli-0.1.0-all.jar spec document set \
  --project-root . \
  --change order-query-api \
  --type design \
  --content "Use Controller -> Service -> Mapper for paginated order search."

java -jar target/dhk-cli-0.1.0-all.jar spec task add \
  --project-root . \
  --change order-query-api \
  --task T001 \
  --title "Add request DTO"

java -jar target/dhk-cli-0.1.0-all.jar spec acceptance add \
  --project-root . \
  --change order-query-api \
  --acceptance A001 \
  --description "Paginated order query returns the standard result wrapper"

java -jar target/dhk-cli-0.1.0-all.jar spec bind-workflow \
  --project-root . \
  --change order-query-api \
  --run <run-key> \
  --type implements
```

Export full spec context, or include a short spec summary in `CURRENT_CONTEXT.md`:

```bash
java -jar target/dhk-cli-0.1.0-all.jar spec export --project-root . --change order-query-api

java -jar target/dhk-cli-0.1.0-all.jar memory export \
  --project-root . \
  --task "Implement order query endpoint" \
  --module order \
  --include-workflow <run-key> \
  --include-spec order-query-api
```

## Safety Model

- `memory add` writes draft memory only.
- Confirmed facts must pass through `memory confirm`.
- Default exports include confirmed memory only.
- Workflow run, phase, gate, workflow export, and `memory export --include-workflow` paths reject sensitive values before persisting or rendering context.
- Workflow phase and gate updates must target the current phase.
- Workflow phases cannot be marked passed while pending or failed hard gates remain for that phase.
- Failed hard gates block the run; passing or waiving the blocking hard gates can resume the run.
- `memory export --include-workflow` records exported memory bindings and the generated current-context artifact for audit.
- `workflow export` records the generated workflow-context artifact for audit.
- Spec commands reject sensitive values before persisting or exporting spec content.
- `spec archive` requires all tasks to be done/skipped and all acceptance criteria to be passed/waived.
- `memory export --include-spec` embeds only a short spec summary; full proposal/design content belongs in `SPEC_CONTEXT.md`.
- Sensitive values such as passwords, bearer tokens, JDBC URLs, access keys, and obvious user data are rejected.
- Raw SQL results are not stored as long-term memory.

## License

MIT
