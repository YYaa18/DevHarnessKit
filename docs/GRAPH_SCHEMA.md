# Graph Lite Schema And Config

Status: alpha design contract for V0.5.0 Graph Lite Core.

Graph Lite stores snapshot-bound machine facts about the local workspace. It is
not confirmed project memory and is not a stable public database API before 1.0.

## Paths

```text
.agents/graph/config.json
.agents/graph/exports/GRAPH_INDEX_REPORT.md
.agents/graph/exports/GRAPH_CONTEXT.md
.agents/graph/exports/IMPACT_MAP.md
.agents/graph/exports/GRAPH_SNAPSHOT.json
.agents/graph/snapshots/
.agents/graph/cache/
```

Only `config.json` is intended to be committed. Generated exports, snapshots,
and cache files are ignored.

## Current Commands

```bash
dhk graph init --project-root <path>
dhk graph status --project-root <path>
dhk graph index --project-root <path>
dhk graph impact --file <path>|--symbol <name>|--sql-table <table> --project-root <path>
dhk graph export --project-root <path>
```

`graph init` creates `.agents/graph/config.json` and generated artifact
directories. `graph status` scans configured files, runs the built-in Lite
parsers, and writes `GRAPH_INDEX_REPORT.md`. `graph index` runs the same scan
and parser pipeline, then persists a completed snapshot, file rows, node rows,
and edge rows into SQLite. Each index run creates a new snapshot; historical
snapshots are not silently overwritten. `graph impact` reads the latest completed
snapshot and writes `IMPACT_MAP.md` with related files, SQL nodes, tests, risk
nodes, recommended reads, and scoring input counts.

`graph export` writes:

```text
GRAPH_CONTEXT.md
GRAPH_SNAPSHOT.json
```

`GRAPH_CONTEXT.md` section order:

```text
boundary
snapshot
file-hashes
node-kinds
risk-nodes
agent-instructions
```

`GRAPH_SNAPSHOT.json` required fields:

```text
schema_version
generated_at
snapshot_id
snapshot_key
provider
status
workspace_fingerprint
config_hash
git_commit
git_dirty
file_count
node_count
edge_count
file_hashes[]
```

## Default Config

The default tracked config is `.agents/graph/config.json`.

Key defaults:

```text
provider: lite
max_file_bytes: 1048576
max_indexed_files: 5000
max_impact_depth: 3
max_export_nodes: 500
```

Default include roots:

```text
src/main/java/**
src/test/java/**
src/main/resources/**
src/main/webapp/**
pom.xml
```

Default excludes:

```text
target/**
build/**
.git/**
.agents/memory/**
.agents/graph/exports/**
.agents/graph/snapshots/**
.agents/graph/cache/**
```

Protected or sensitive file content should not be copied into graph exports.
Implementations may record skipped metadata such as path, reason, and size.
See [GRAPH_LIMITS.md](GRAPH_LIMITS.md) for default limits, skipped reasons, and
truncation reporting.

## Lite Parser Scope

The built-in Lite parser is heuristic and source-bound. Every extracted node and
edge includes `source=lite`, confidence, file path, line, and short evidence.

Current extraction:

```text
Java:
  package, import, class/interface/enum, method/test_case, annotation route,
  simple method calls, constructor/type references, extends/implements

XML/MyBatis:
  mapper namespace, resultMap, select/insert/update/delete statement,
  Java mapper mapping, table and column heuristics, XML parse errors

properties:
  config_key only; property values are never exported
```

Parse failures are report entries, not fatal index failures. Sensitive filenames
are skipped by the scanner, and parser evidence must not include raw property
values, SQL result data, tokens, passwords, or credentials.

## SQLite Version

Graph Lite schema is v9. Earlier design notes that mention v6 are historical
and should not be used for implementation.

## Tables

`code_graph_snapshot`

- one row per indexed workspace snapshot;
- includes provider, config hash, workspace fingerprint, status, counts, limits,
  summary, created/completed timestamps;
- status values: `created`, `indexing`, `completed`, `failed`, `stale`.

`code_graph_file`

- one row per file considered by a snapshot;
- records relative path, kind, language, content hash, size, indexed/skipped
  state, skip reason, and protected-file flag.

`code_graph_node`

- one row per extracted symbol, route, SQL statement, JSP form, XML mapping, or
  other graph node;
- scoped by snapshot and file;
- stores node kind, name, qualified name, line range, signature, and metadata.

`code_graph_edge`

- one row per relationship between two node keys in a snapshot;
- stores edge kind, optional file, confidence, and metadata;
- confidence is 0 to 100.

`code_graph_query_cache`

- optional cache for impact queries and graph exports;
- scoped to a snapshot and query key.

`goal_graph_binding`

- binds a goal to graph evidence used during development;
- binding types: `used`, `pre_change`, `post_change`, `impact_map`, `summary`;
- used by future goal completion summaries and audit trails.

## Snapshot Boundary

Graph rows describe the code state identified by `workspace_fingerprint`. They
become stale when the workspace changes. Do not confirm graph rows into memory
without human review. Markdown graph exports are generated context artifacts and
can be regenerated from SQLite with the matching CLI.

See [GRAPH_CONTEXT_LAYER.md](GRAPH_CONTEXT_LAYER.md) for the agent-facing graph
context contract.
