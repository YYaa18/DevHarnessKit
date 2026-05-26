# CGC Adapter Contract

Status: optional alpha contract.

DevHarnessKit ships with the built-in `lite` graph provider. The `cgc` provider
is an optional adapter direction for teams that install an external Code Graph
Context style command. CGC is not bundled, not required for default usage, and
must not block `dhk graph index`, `dhk graph impact`, or goal workflows that use
`provider=lite`.

## Configuration

`.agents/graph/config.json` can opt into CGC explicitly:

```json
{
  "schema_version": "devharness-graph-config/v1-alpha",
  "provider": "cgc",
  "cgc_command": "cgc"
}
```

To disable CGC, set:

```json
{
  "provider": "lite"
}
```

## Doctor Check

Run:

```bash
dhk graph doctor --project-root .
```

`graph doctor` reports:

```text
provider
cgc_required
cgc_command
cgc_available
cgc_status
cgc_detail
default_provider_unaffected
```

For `provider=lite`, `cgc_status` is `not_required`. For `provider=cgc`, the CLI
probes `cgc_command --version` with a short timeout and reports whether the
command is available. An unavailable CGC command is diagnostic information, not a
process-wide failure.

## Normalization Contract

The current prototype calls:

```text
<cgc_command> analyze impact --type <file|symbol|sql-table> --query <query> --depth <n> --format devharness-tsv
```

It expects tab-separated records:

```text
start_node<TAB>node_key<TAB>node_kind<TAB>name<TAB>qualified_name<TAB>relative_path<TAB>start_line<TAB>end_line<TAB>language<TAB>confidence<TAB>evidence
node<TAB>node_key<TAB>node_kind<TAB>name<TAB>qualified_name<TAB>relative_path<TAB>start_line<TAB>end_line<TAB>language<TAB>confidence<TAB>evidence
risk_node<TAB>node_key<TAB>node_kind<TAB>name<TAB>qualified_name<TAB>relative_path<TAB>start_line<TAB>end_line<TAB>language<TAB>confidence<TAB>evidence
sql_node<TAB>node_key<TAB>node_kind<TAB>name<TAB>qualified_name<TAB>relative_path<TAB>start_line<TAB>end_line<TAB>language<TAB>confidence<TAB>evidence
caller<TAB>edge_kind<TAB>source_node_key<TAB>target_node_key<TAB>relative_path<TAB>confidence<TAB>evidence
callee<TAB>edge_kind<TAB>source_node_key<TAB>target_node_key<TAB>relative_path<TAB>confidence<TAB>evidence
related_file<TAB>relative_path
related_test<TAB>relative_path
missing_related_test<TAB>relative_path
```

The adapter normalizes these records into the same DevHarness graph model used
by Lite snapshots:

- `code_graph_file`: relative path, language, kind, hash, indexed/skipped state.
- `code_graph_node`: node kind, node key, name, qualified name, relative path,
  line range, source, confidence, metadata.
- `code_graph_edge`: edge kind, source node key, target node key, file path,
  confidence, source, metadata.
- `code_graph_snapshot`: provider, config hash, workspace fingerprint, counts,
  limits, status, timestamps.

Adapter output must be snapshot-bound. It is not confirmed memory and must not be
promoted into long-term memory without human review.

## Safety

CGC output must not copy raw secrets, credentials, SQL results, or sensitive file
contents into graph nodes, edges, metadata, or exports. The adapter should keep
short evidence strings and rely on the same sensitive/protected-file boundaries
as Lite graph exports.

## Alpha Limitations

- DevHarnessKit executes the prototype adapter only for `dhk graph impact` when
  `provider=cgc` is explicitly configured.
- `dhk graph index` still uses the built-in Lite indexer.
- The `devharness-tsv` format is an adapter contract, not a CGC upstream API
  guarantee.
- Teams should keep `provider=lite` unless they are actively testing the adapter.
