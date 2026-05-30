# Graph Lite Limits

Graph Lite is intentionally bounded. It should stay fast, local-first, and safe
on old enterprise projects.

Status: stable-advisory in DevHarness Kit 1.0.0.

The limit names, skip reasons, freshness flags, and truncation reporting
documented here are stable-advisory output semantics. The exact analysis result
remains heuristic and must be verified with normal compile, tests, manual review,
and goal checks.

## Default Limits

```json
{
  "limits": {
    "max_file_bytes": 1048576,
    "max_indexed_files": 5000,
    "max_impact_depth": 3,
    "max_export_nodes": 500
  }
}
```

These values are stored in `.agents/graph/config.json` after `dhk graph init`.
If the file is missing, the same defaults are used in memory.

## What Gets Skipped

Graph Lite skips file content when:

- the path matches configured `exclude` patterns;
- the path looks sensitive, such as `secret`, `password`, `.pem`, or `.key`;
- project policy marks the path as protected through
  `.agents/devharness/policy.json` `protected_files`;
- file size exceeds `max_file_bytes`;
- the scan already reached `max_indexed_files`;
- the file is a known binary build artifact such as `.jar` or `.class`.

Skipped files can still appear as metadata in reports with a skip reason. Raw
content from sensitive or protected files must not enter graph nodes, edges,
`GRAPH_CONTEXT.md`, `GRAPH_SNAPSHOT.json`, or `IMPACT_MAP.md`.

## Truncation Reports

Graph reports should not silently omit data.

`GRAPH_INDEX_REPORT.md` includes:

- configured limits;
- count of files skipped by safety and limit reasons;
- node/edge export totals and truncation flags.

`GRAPH_CONTEXT.md` includes:

- configured export limits;
- skipped file count from the snapshot;
- file-hash and risk-node truncation flags.

`dhk graph impact` clamps requested depth to `max_impact_depth` and prints:

- `requested_depth`;
- effective `depth`;
- `max_impact_depth`;
- `depth_limited`.

## Non-Goals

Graph Lite is not a complete parser. The stable-advisory contract does not
guarantee:

- full Java type resolution;
- reflection or runtime dispatch resolution;
- SQL generated dynamically at runtime;
- complete framework plugin support;
- precise call graph parity with dedicated static analysis tools.

For critical changes, treat Graph Lite as an impact-analysis assistant and keep
normal compile/test/manual evidence requirements.
