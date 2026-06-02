# DevHarness Kit

[中文说明](README.zh-CN.md)

DevHarness Kit is a local-first harness for AI-assisted development. It gives an
agent a clear task protocol, short project context, and auditable evidence
without asking the model to remember the whole codebase.

It is designed for teams that already use coding agents, but want the work to be
less loose: every task should have a goal, a current action, recorded evidence,
verification status, and a local recovery trail.

## Status

DevHarness Kit `1.2.0` is the current stable release. The stable contract is
intentionally narrow: core CLI, memory, goal, status, readiness, configure,
brief, BDD acceptance evidence, release packaging, and documented
generated-export anchors. The 1.2 line adds CLI-first memory quality workflows
for candidates, dedupe/conflict checks, stale governance, explainable ranking,
and team memory packs while preserving the 1.0 stable core.

Current stable release: `1.2.0`.

Current schema version is v17.

See
[docs/STABLE_CONTRACT.md](docs/STABLE_CONTRACT.md) and
[docs/STABLE_CANDIDATE.md](docs/STABLE_CANDIDATE.md) for the exact boundary.

## What Problem It Solves

Coding agents are useful, but they often lose track of:

- what the user actually asked for;
- which files were already inspected;
- what evidence proves the change is safe;
- whether tests or manual verification were really done;
- what should happen next after context is compacted or a session is resumed.

DevHarness Kit puts that state into the project instead of the chat. The agent
reads generated context files, follows a goal-first workflow, and records each
step back into local project storage.

## How It Works

DevHarness Kit is a Java CLI. It runs locally. It does not start a server, call
LLM providers, or upload your project to a cloud service.

The basic idea is simple:

```text
User task
  -> DevHarness goal
  -> local SQLite state
  -> short Markdown/JSON context
  -> agent performs one focused action
  -> evidence is recorded
  -> verification and completion are checked
```

SQLite is the source of truth. Markdown files such as `GOAL_CONTEXT.md`,
`CURRENT_CONTEXT.md`, and `GOAL_SUMMARY.md` are generated exports for humans and
agents to read. They are not meant to be hand-edited.

## What It Can Do

### Keep Agent Work On Track

DevHarness Kit can turn a user request into a Work Brief and a goal. The goal
tells the agent what the current action is, what evidence is required, and what
is forbidden. This makes the workflow easier to resume and easier to audit.

Typical result:

- the agent starts with inspection instead of jumping straight to edits;
- the next step is written into `GOAL_CONTEXT.md`;
- every step records what was inspected, changed, or verified;
- completion is blocked until required evidence is present.

### Preserve Useful Project Context

The memory layer stores confirmed project facts locally. Agents receive a short
context export instead of reading an entire repository or relying on old chat
history.

Typical result:

- repeated conventions can be reused across sessions;
- context survives chat compaction;
- drafts do not enter default context until confirmed;
- memory candidates can be reviewed before becoming draft memory;
- duplicate, conflicting, stale, and superseded facts can be surfaced;
- team memory packs can be imported as drafts for human review;
- recovery exports help restart after an interrupted run.

### Make Verification Visible

Goal verification records compile, test, sensitive-data, workflow, and spec
status. Projects that cannot run checks from the CLI can use manual evidence,
for example an IDE compile result or a manual test note.

Typical result:

- "tests passed" becomes recorded evidence, not a vague sentence;
- manual verification has a scope and evidence path;
- stale checks are detected after later changes;
- completion summaries show what was verified and what remains risky.

### Turn Acceptance Intent Into Evidence

The BDD layer records features, scenarios, Given/When/Then steps, evidence, and
traceability links. It can import manual notes, JUnit XML, Cucumber, Postman, and
Playwright report files without running those tools itself.

Typical result:

- product acceptance intent is visible to the agent;
- scenarios can be bound to specs, goals, workflows, graph inputs, and tests;
- missing or failing BDD evidence blocks BDD-required goals;
- passing BDD evidence supports verification without pretending scenarios alone
  prove the implementation.

### Help With Impact Analysis

Graph Lite can build local snapshots of code relationships and generate impact
maps. This is advisory context, not a correctness proof.

Typical result:

- the agent gets recommended files to inspect;
- graph-aware goals can ask for impact evidence;
- stale graph snapshots are called out instead of silently trusted.

### Package Agent Adapters

The release package includes skills, rules, and wrapper scripts for agent
workflows. The installer can copy the right files into a target project and
generate a configuration for manual or automatic verification.

Typical result:

- a project gets consistent `.agents/` setup;
- the agent uses wrapper scripts instead of fragile ad hoc commands;
- users see a human Work Brief while agents consume structured execution data.

## What It Is Not

DevHarness Kit does not prove that generated code is correct. It records process,
context, evidence, and checks so humans and agents have a safer workflow.

It is also not:

- a hosted service;
- a replacement for tests or review;
- a database permission system;
- a full DLP system;
- a guarantee that every bundled experimental command is part of the stable
  contract.

Database inspection is a local developer tool. Use read-only credentials for
real databases. The SQL guard is a safety aid, not a database permission
boundary.

## Install From A Release

Download the latest release package from
[GitHub Releases](https://github.com/YYaa18/DevHarnessKit/releases).

For most users, choose the zip or tar.gz package, unpack it, and run:

```bash
./install.sh
```

The installer asks for:

- the target project path;
- which agent adapter to install;
- the project verification style;
- whether to create the first Work Brief or goal.

For direct CLI use, the package also contains:

```text
lib/dhk.jar
```

Check the version:

```bash
java -jar lib/dhk.jar version
```

For local install automation, these package entrypoints are stable-candidate in
1.0:

- `install.sh`
- `scripts/install-agent-adapters.sh`
- `scripts/devharness-control-panel.sh` commands: `configure`, `plan`,
  `install`, `status`, `doctor`, `repair`, and `uninstall`

The stable-candidate promise covers the documented command and option names,
non-mutating `plan` and `--dry-run` behavior, `status --status-format
text|json|markdown`, and the `doctor`/`repair` flow. It does not make generated
`.agents/`, `.claude/`, or `.comate/` layouts, rule file contents, or alpha
install-state/manifest schemas stable APIs.

## Typical Workflow

1. Install DevHarness Kit into a project.
2. Ask the agent to start from a Work Brief or quickstart.
3. Review the suggested goal, risk, and verification needs.
4. Let the agent perform only the current goal action.
5. Record evidence after each step.
6. Run goal verification before claiming completion.
7. Use the summary and artifact passport for review or release evidence.

The stable entrypoints are:

- `dhk doctor`
- `dhk configure`
- `dhk status`
- `dhk readiness`
- `dhk advise`
- `dhk quickstart`
- `dhk memory`
- `dhk goal`
- `dhk graph` stable-advisory subset: status, index, impact, export
- `dhk bdd`

Graph Lite is stable-advisory: its documented output shape is stable, but its
impact analysis remains heuristic and must be verified with tests, review, and
goal checks. Skill governance has a stable-candidate contract/report subset, but
full governance enforcement remains outside the stable contract. Policy hooks
have a stable-candidate local schema and hook-behavior subset, but they are not
a sandbox or permission boundary. Routine metrics/replay have stable-candidate
local report schemas, while public routine automation remains experimental.

## Build From Source

Requirements:

- Java 8 or newer;
- Maven.

Build and test:

```bash
mvn clean test
```

Build the shaded CLI jar:

```bash
mvn -DskipTests package
```

Build release archives:

```bash
mvn -DskipTests package -P release-archive
```

Release archives include the runtime jar, installer, agent packaging, license,
security notes, changelog, and user-facing README files. The repository `docs/`
directory is intentionally not bundled into the user package.
`scripts/release-gate.sh` writes `target/ARTIFACT_MANIFEST.json` and
`target/SHA256SUMS`; publish both beside the jar and archives.

## Documentation

Start here when you need more detail:

- [docs/INDEX.md](docs/INDEX.md): documentation map.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): architecture and module boundaries.
- [docs/STABLE_CONTRACT.md](docs/STABLE_CONTRACT.md): intended stable contract.
- [docs/GOAL_CONFIGURATION.md](docs/GOAL_CONFIGURATION.md): goal profiles and verification policy.
- [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md): CLI, schema, JSON, and export compatibility.
- [docs/MIGRATIONS.md](docs/MIGRATIONS.md): SQLite migration and recovery policy.
- [SECURITY.md](SECURITY.md): security model and reporting.

## License

DevHarness Kit source code is licensed under the MIT License.

The shaded binary jar includes third-party dependencies with their own licenses.
See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
