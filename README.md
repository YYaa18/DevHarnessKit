# DevHarness Kit

DevHarness Kit is a local-first development harness for coding agents.

It stores durable project memory, specs, workflow state, and read-only database inspection results locally, then exports short Markdown context for agents. The goal is not to make a model remember an entire project. The goal is to let the project keep auditable context in local SQLite and files, then give the agent only the context it needs for the current task.

## Status

This repository is `0.1.0-alpha` and should be treated as a developer preview.

| Area | Status | Notes |
| --- | --- | --- |
| Memory core | Stable-ish alpha | Usable for local project memory, draft confirmation, search, export, checkpoint, and recovery. |
| Doctor | Stable-ish alpha | Validates project memory storage and export paths. |
| Sensitive guard | Alpha | Best-effort heuristic guard with project-level reject/redact/allow policy. Not a complete DLP system. |
| DB readonly | Beta | Useful for inspection, but SQL guard is not a permission boundary. Use read-only database credentials. |
| Goal orchestration | Experimental alpha | High-level `dhk goal` protocol for start/resume/next/step/check/evaluate/verify/complete and short context export. It is the preferred harness entry for agent work, but not stable. |
| Graph Lite | Experimental alpha | `dhk graph init/status/index/impact/export` can scan files, parse Lite nodes/edges, persist snapshot-bound graph rows, and export local index/impact/context reports. |
| Workflow | Alpha | Records process state for audit and context export. It is not a workflow engine. |
| Spec | Alpha | Records change documents, tasks, acceptance, and status. Markdown is export only. |
| Agent packaging | Alpha | Ships `.agents/skills` and `.comate/rules` helpers for agent workflows. |
| SQLite schema | Alpha | Current schema version is v9. Compatibility policy is documented, but not yet guaranteed as stable. |

Do not publish or describe the current build as stable or 1.0-ready.

## Core Principles

1. SQLite is the source of truth.
2. Markdown files are exports, not the source of truth.
3. Agents consume short context exports instead of reading `memory.db` directly.
4. Draft memory never enters default context export.
5. Database query results are not automatically stored as long-term memory.
6. Workflow and spec commands record audit state; they do not prove correctness.
7. SQL safety checks are guardrails, not a database permission model.
8. Persistent writes pass through sensitive-data checks.
9. Schema changes must be migratable, recoverable, and tested.
10. Open-source releases prioritize recoverability, explainability, and maintainability over feature count.

## Architecture

```text
DevHarnessKit
|-- memory core      project facts, short context export, checkpoint/recovery
|-- db readonly      optional business database inspection
|-- goal             high-level task orchestration over memory/workflow/spec
|-- graph lite       snapshot-bound file graph context for impact analysis
|-- workflow         process state recording, not a workflow engine
|-- spec             requirements/design/tasks/acceptance state recording
`-- agent packaging  skills, rules, and wrapper scripts
```

Flow:

```text
Agent skill or rule
        |
        v
DevHarness Kit CLI
        |
        v
SQLite memory.db + controlled readonly DB query
        |
        v
CURRENT_CONTEXT.md / GOAL_CONTEXT.md / GOAL_SUMMARY.md
RECOVERY_CONTEXT.md / SQL_RESULT.md / SPEC_CONTEXT.md / WORKFLOW_CONTEXT.md
        |
        v
Agent reads short Markdown context
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for module boundaries and
[docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) for the current CLI, schema,
JSON, and export compatibility contract.

## Requirements

- Java 8+ runtime.
- Maven for development and packaging.

## Build

```bash
mvn clean test
mvn -DskipTests package
```

`mvn test` also generates a JaCoCo coverage report at:

```text
target/site/jacoco/index.html
```

The shaded CLI jar is generated as:

```text
target/dhk-cli-0.1.0-alpha-all.jar
```

Release archives are generated during `mvn package`:

```text
target/devharnesskit-0.1.0-alpha.zip
target/devharnesskit-0.1.0-alpha.tar.gz
```

The archives include the CLI jar, scripts, agent skill/rule packaging, `LICENSE`, and `THIRD_PARTY_NOTICES.md`.

Run the local performance smoke after packaging:

```bash
scripts/perf-smoke.sh
```

`scripts/perf-smoke.sh` is a developer validation helper and requires the `sqlite3` CLI to bulk-load sample rows. The packaged CLI does not require `sqlite3` at runtime.

## Quick Start

Check the version:

```bash
java -jar target/dhk-cli-0.1.0-alpha-all.jar version
```

Initialize project memory:

```bash
java -jar target/dhk-cli-0.1.0-alpha-all.jar memory init --project-root .
```

Add a draft memory:

```bash
java -jar target/dhk-cli-0.1.0-alpha-all.jar memory add \
  --project-root . \
  --type gateway_convention \
  --module global \
  --title "User identity comes from gateway" \
  --content "User ID is read from X-User-Id. Business services do not parse tokens." \
  --tags "api,gateway,user-id,header"
```

For longer content, use `--content-file` or `--content-stdin`.

Confirm it:

```bash
java -jar target/dhk-cli-0.1.0-alpha-all.jar memory confirm --project-root . --id 1
```

Export current context:

```bash
java -jar target/dhk-cli-0.1.0-alpha-all.jar memory export \
  --project-root . \
  --task "Implement order query endpoint" \
  --module order \
  --mode api \
  --keywords "gateway,mybatis,mysql"
```

Use `--json` for machine-readable output on supported commands such as `doctor`, `memory search`, `memory export`, `db test`, `db sql --dry-run`, `goal status`, `goal check`, `goal evaluate`, `goal verify`, and `goal complete`. Use `dhk db sql --format json` for JSON query results.

Seed workflow templates and start a run:

```bash
java -jar target/dhk-cli-0.1.0-alpha-all.jar workflow template seed --project-root .

java -jar target/dhk-cli-0.1.0-alpha-all.jar workflow start \
  --project-root . \
  --workflow api-change \
  --task "Implement order query endpoint" \
  --module order \
  --mode api
```

Create a spec change:

```bash
java -jar target/dhk-cli-0.1.0-alpha-all.jar spec create \
  --project-root . \
  --change order-query-api \
  --title "Implement order query endpoint" \
  --summary "Provide paginated order search for the frontend" \
  --module order \
  --mode api
```

Include workflow and spec summaries in `CURRENT_CONTEXT.md`:

```bash
java -jar target/dhk-cli-0.1.0-alpha-all.jar memory export \
  --project-root . \
  --task "Implement order query endpoint" \
  --module order \
  --include-workflow <run-key> \
  --include-spec order-query-api
```

Start a goal-oriented run:

```bash
java -jar target/dhk-cli-0.1.0-alpha-all.jar goal start \
  --project-root . \
  --profile java-api-change \
  --task "Implement order query endpoint" \
  --module order \
  --mode api
```

Then continue through the goal protocol:

```bash
java -jar target/dhk-cli-0.1.0-alpha-all.jar goal next --project-root . --goal <goal-key>
java -jar target/dhk-cli-0.1.0-alpha-all.jar goal step --project-root . --goal <goal-key> \
  --summary "Inspected existing controller/service/mapper/tests" \
  --evidence "existing_controller,existing_service,existing_mapper,existing_tests"
java -jar target/dhk-cli-0.1.0-alpha-all.jar goal status --project-root . --goal <goal-key>
```

Before claiming completion, run goal verification and complete the goal:

```bash
java -jar target/dhk-cli-0.1.0-alpha-all.jar goal verify --project-root . --goal <goal-key>
java -jar target/dhk-cli-0.1.0-alpha-all.jar goal complete --project-root . --goal <goal-key>
```

`goal verify` runs the required checks and evaluates readiness in one command. `goal check` and `goal evaluate` remain available for lower-level debugging.

`goal complete` writes `.agents/memory/exports/GOAL_SUMMARY.md`, records completion artifacts, and creates a checkpoint.

Goal orchestration is still experimental alpha. Built-in Java profiles are intentionally strict: skipped compile/test checks are not accepted, required specs must contain at least one closed task and one closed acceptance item, and pending hard workflow gates block completion unless mapped goal actions or accepted checks close them. `goal complete` closes the checkpoint gate while creating the completion checkpoint.

Goal Sync Strictness makes `goal` the main controller for agent work while workflow and spec remain auditable state layers. Profile mappings can auto-pass deterministic workflow phases, gates, spec tasks, and business acceptance items from accepted evidence or fresh checks. Manual acceptance, gate waivers, spec archive, memory confirmation, and DB SQL remain explicit human or user-approved actions.

For agent-facing usage, `.agents/skills/devharness-goal-development/` provides goal-first wrapper scripts such as `goal-start.sh`, `goal-next.sh`, `goal-step.sh`, `goal-check.sh`, `goal-evaluate.sh`, and `goal-complete.sh`.

Projects can customize goal profiles, required checks, and accepted check statuses with `.agents/devharness/goal-profiles/*.json` and `.agents/devharness/goal-check-policy.json`.

## Safety Model

- `memory add` writes draft memory only.
- Confirmed facts must pass through `memory confirm`.
- Default exports include confirmed memory only.
- Sensitive-data checks run before memory, workflow, and spec persistence, and before context rendering paths that include persisted content.
- Projects can configure `.agents/devharness/sensitive-policy.json` to reject, redact, or allow specific patterns.
- Sensitive-data detection is best-effort and heuristic. It is not a complete DLP system.
- Raw SQL results are not stored as long-term memory.
- SQL statements are checked fail-closed and executed through a read-only JDBC connection where supported.
- SQL guard and JDBC read-only mode are not database permission boundaries.
- Always use a database account with read-only privileges.
- Do not connect DevHarness Kit to production databases with write-capable credentials.
- Workflow gates are manual/audit state unless explicitly wired to commands.
- Goal orchestration is a deterministic CLI protocol over existing modules. `goal evaluate` checks recorded evidence before `goal complete`, but the project still does not prove code correctness.
- Goal context exports reject sensitive findings. Goal completion summaries use redaction before writing, then reject if sensitive patterns still remain after redaction.
- Goal check freshness includes goal step count and workspace/context fingerprints so completion cannot silently reuse stale verification after later changes.
- DevHarness Kit records development process state; it does not prove code correctness.

Read [SECURITY.md](SECURITY.md) before using DB readonly features.

## Documentation

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): current architecture and module boundaries.
- [docs/CLI_INPUTS.md](docs/CLI_INPUTS.md): file/stdin input, search explain, and backup command usage.
- [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md): stable-ish alpha, beta, and alpha CLI/schema/output contract.
- [docs/DB_COMPATIBILITY.md](docs/DB_COMPATIBILITY.md): MySQL 5.1 production and MySQL 8 local compatibility guidance.
- [docs/decisions/0001-module-split-and-installer-strategy.md](docs/decisions/0001-module-split-and-installer-strategy.md): alpha distribution and module split decision.
- [docs/decisions/0002-coverage-threshold-and-sql-safety-fuzz.md](docs/decisions/0002-coverage-threshold-and-sql-safety-fuzz.md): coverage gate decision and SQL safety fuzz backlog.
- [docs/DEVHARNESS_GOAL_CLI_ORCHESTRATION_PLAN.md](docs/DEVHARNESS_GOAL_CLI_ORCHESTRATION_PLAN.md): V0.4 goal orchestration plan.
- [docs/DEVHARNESS_SKILLS_REDESIGN_PLAN.md](docs/DEVHARNESS_SKILLS_REDESIGN_PLAN.md): goal-first skill redesign plan.
- [docs/GOAL_CONFIGURATION.md](docs/GOAL_CONFIGURATION.md): project-level goal profile and check policy configuration.
- [docs/GOAL_SYNC_STRICTNESS.md](docs/GOAL_SYNC_STRICTNESS.md): goal-controlled workflow/spec synchronization model and alpha boundaries.
- [docs/GRAPH_CONTEXT_LAYER.md](docs/GRAPH_CONTEXT_LAYER.md): Graph Lite context layer, export boundary, and agent usage.
- [docs/GRAPH_LIMITS.md](docs/GRAPH_LIMITS.md): Graph Lite safety limits, skipped metadata, and truncation reports.
- [docs/GRAPH_SCHEMA.md](docs/GRAPH_SCHEMA.md): Graph Lite alpha schema, config, and SQLite table contract.
- [docs/JSON_OUTPUT.md](docs/JSON_OUTPUT.md): alpha JSON output commands and required fields.
- [docs/MIGRATIONS.md](docs/MIGRATIONS.md): SQLite schema compatibility and recovery policy.
- [docs/POLICY.md](docs/POLICY.md): project policy hooks for protected paths, command guards, DB SQL, export, and goal lifecycle checks.
- [docs/ROADMAP.md](docs/ROADMAP.md): release maturity plan.
- [docs/SENSITIVE_POLICY.md](docs/SENSITIVE_POLICY.md): project-level sensitive-data reject/redact/allow policy.
- [SECURITY.md](SECURITY.md): threat model, limitations, and reporting.
- [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md): dependency license notices for source and shaded binaries.
- [CONTRIBUTING.md](CONTRIBUTING.md): development and PR workflow.
- [RELEASE.md](RELEASE.md): release packaging checklist.

`docs/PRD.md` and `docs/IMPLEMENTATION_PLAN.md` are historical design notes. They are useful background, but README and the docs above describe the current open-source surface.

## License

The DevHarness Kit source code is licensed under the MIT License.

The shaded binary jar includes third-party dependencies with their own licenses. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
