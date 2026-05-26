# CGC Adapter Prototype Evaluation

Status: V0.5.4 optional alpha evaluation.

## Decision

Keep CGC as an optional experimental provider and do not make it the default.

Reason:

- Graph Lite already covers the current legacy and modern testbeds without an
  external dependency.
- A CGC-style provider can be useful when a team has a stronger call graph tool,
  but the adapter contract needs real-world output samples before it should be
  treated as stable.
- The current prototype proves DevHarnessKit can call an external command and
  normalize results into `IMPACT_MAP.md`, while keeping default tests independent
  of CGC availability.

## Prototype Scope

Implemented:

- `provider=cgc` branch for `dhk graph impact`.
- External command invocation:
  `<cgc_command> analyze impact --type <type> --query <query> --depth <n> --format devharness-tsv`.
- Normalization into `GraphImpactResult`.
- Existing `GraphImpactRenderer` writes the final `IMPACT_MAP.md`.
- Integration test uses a fake CGC command so CI does not require CGC.

Not implemented:

- CGC-backed `dhk graph index`.
- Real CodeGraphContext JSON parser.
- Persistence of CGC nodes into SQLite graph tables.
- Statistical accuracy comparison against a real CGC installation.

## Fixture Comparison

| Fixture | Lite result | CGC prototype result | Notes |
| --- | --- | --- | --- |
| `modern-java-api` | Covered by existing Graph Lite baselines. | Fake CGC command emits service, controller, route, and test records. | Confirms normalization and IMPACT_MAP rendering only. |
| `legacy-mybatis-order` | Lite impact covers mapper XML, DTOs, service, controller, and related tests. | Not run with real CGC. | Keep Lite as default for legacy flows. |
| `legacy-jsp-servlet-shop` | Lite impact covers JSP form, servlet route, DAO/service chain, and form fields. | Not run with real CGC. | CGC value is unknown for JSP/servlet until real output is available. |

## Maintenance Cost

Low for the current prototype:

- no bundled CGC dependency;
- no new database schema;
- no new default command path;
- no CI dependency on external binaries.

Moderate if promoted later:

- need stable upstream output contract;
- need sensitive-data filtering for adapter metadata;
- need compatibility tests across CGC versions;
- need graph index persistence semantics for external nodes and edges.

## Recommendation

Continue experiment behind `provider=cgc`. The next useful step is to collect
real CGC output from one modern Java fixture and one legacy Java fixture, then
decide whether to persist CGC graph rows or keep CGC as impact-only enrichment.
