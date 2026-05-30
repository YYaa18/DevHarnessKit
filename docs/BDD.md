# BDD Specification Layer

Status: stable contract in DevHarness Kit 1.0.0.

DevHarness Kit BDD is a specification-level acceptance harness. It records
features, scenarios, Given/When/Then steps, and quality issues in local SQLite,
then exports short Markdown and `.feature` artifacts for agents and humans.

It is not a standalone executable BDD framework. It does not require Cucumber,
Playwright, Postman, MockMvc, or any other test runner.

## Paths

```text
.agents/bdd/features/
.agents/bdd/evidence/
.agents/bdd/exports/BDD_CONTEXT.md
.agents/bdd/exports/SCENARIO_IMPACT_MAP.md
```

`.agents/memory/memory.db` remains the source of truth. Files under
`.agents/bdd/` are generated or auxiliary artifacts unless a later command
explicitly documents otherwise.

## Commands

```bash
dhk bdd init --project-root <path>
dhk bdd add --feature <key> --title <title> --scenario <key> --scenario-title <title>
dhk bdd scenario create --scenario <key> --title <title>
dhk bdd scenario list [--feature <key>]
dhk bdd scenario show --scenario <key>
dhk bdd list [--feature <key>]
dhk bdd show --scenario <key>|--feature <key>
dhk bdd export [--feature <key>]
dhk bdd lint [--feature <key>]
dhk bdd evidence add --scenario <key> --summary <text>
dhk bdd evidence junit [--reports <dir[,dir]>] [--scenario <key>]
dhk bdd evidence report --adapter cucumber|postman|playwright --report <json> --scenario <key>
dhk bdd verify [--feature <key>|--scenario <key>]
dhk bdd coverage [--feature <key>|--scenario <key>]
dhk bdd bind-spec --scenario <key> --change <key> --acceptance <key>
dhk bdd bind-goal --scenario <key> --goal <goal-key>
dhk bdd bind-workflow --scenario <key> --run <workflow-run-key>
dhk bdd bind-graph --scenario <key> --file <path>|--symbol <symbol>|--sql-table <table>
dhk bdd bind-test --scenario <key> --class <name> [--method <name>]
dhk graph impact --scenario <key>
```

`bdd add` accepts:

```text
--module
--description
--scenario-description
--type acceptance|edge_case|regression|manual|exploratory
--priority low|normal|high|critical
--status draft|active|implemented|verified|blocked|deprecated|archived
--tags
--given
--when
--then
--and
```

If `--status` is omitted, a scenario with all core `given`, `when`, and `then`
steps is created as `active`. Missing core steps keep the scenario in `draft`
and print a lint warning.

All persistent writes pass through `SensitiveDataGuard`.

## Evidence, Verify, And Coverage

`bdd evidence add` records auditable scenario evidence:

```bash
dhk bdd evidence add --scenario order-query-happy-path \
  --status passed \
  --type test \
  --summary "接口测试通过" \
  --command "mvn test" \
  --evidence-path target/surefire-reports/TEST-example.xml
```

Supported evidence statuses:

```text
pending
passed
failed
skipped
waived
```

Supported evidence types:

```text
manual
test
review
screenshot
command
artifact
```

`bdd verify` evaluates selected scenarios and fails when the latest evidence is
missing, pending, or failed. It writes:

```text
.agents/bdd/exports/BDD_EVIDENCE.md
.agents/bdd/exports/BDD_COVERAGE.md
```

DevHarness Kit 1.0.0 includes an adapter SPI for executable BDD evidence. The
built-in adapter is `manual`: it consumes `--type manual` evidence and normalizes
`passed`, `skipped`, and `waived` to `covered`, while preserving `pending` and
`failed` as blocking states. Other evidence types keep the existing stored
status behavior until a concrete framework adapter is added.

DevHarness Kit 1.0.0 also includes a JUnit/MockMvc evidence adapter for Maven
Surefire/Failsafe XML reports. Bind a scenario to a test class or method, then
import report results:

```bash
dhk bdd bind-test --scenario order-query-happy-path \
  --class com.example.OrderControllerTest \
  --method queryByCustomerName

dhk bdd evidence junit --reports target/surefire-reports \
  --scenario order-query-happy-path
```

The importer writes `test` evidence with `passed`, `failed`, `skipped`, or
`pending` status. Generated JUnit evidence uses the `junit` adapter in
`bdd verify`; failed imported evidence makes `bdd verify` fail for that
scenario.

Cucumber, Newman/Postman, and Playwright are optional report-file adapters. They
do not add runtime dependencies and do not call external tools; DevHarnessKit
only consumes a JSON report file that the user or CI already produced:

```bash
dhk bdd evidence report --adapter cucumber \
  --report target/cucumber.json \
  --scenario order-query-happy-path

dhk bdd evidence report --adapter postman \
  --report target/newman/report.json \
  --scenario order-query-happy-path

dhk bdd evidence report --adapter playwright \
  --report playwright-report/results.json \
  --scenario order-query-happy-path
```

The report adapters are lightweight status extractors. They are useful for
attaching external acceptance results to BDD scenarios, but they are not a
replacement for the source tool's own detailed report.

Fixture contracts and status mapping examples are kept in
`docs/features/BDD_EXECUTABLE_ADAPTER_FIXTURES.md`.

`bdd coverage` writes the same reports but returns success so it can be used as
an audit/reporting command in CI or release checks.

When a goal profile sets `bdd_required=true`, `goal verify` runs a `bdd` check.
That check requires scenarios to be bound to the goal with `bdd bind-goal` and
requires the latest goal-specific evidence for each bound scenario to be
accepted. Pending or missing BDD evidence blocks completion.

## Traceability Bindings

`bdd bind-spec` links a scenario to a spec acceptance row:

```bash
dhk bdd bind-spec --scenario order-query-happy-path --change order-query-api --acceptance A001
```

The default relation is `verifies`. Override it with `--relation <key>`.

`bdd bind-goal` links a scenario to a goal run:

```bash
dhk bdd bind-goal --scenario order-query-happy-path --goal 20260526120000-order-query
```

The default relation is `supports`. Bindings are persisted in `bdd_binding` and
are exported through `bdd show` and `BDD_CONTEXT.md` so each scenario can be
traced back to the spec acceptance and goal that introduced or verified it.

`bdd bind-workflow` links a scenario to a workflow run:

```bash
dhk bdd bind-workflow --scenario order-query-happy-path --run workflow-run-key
```

The default relation is `validates`. Workflow exports include a BDD trace for
bound scenarios and their latest evidence.

`bdd bind-graph` links a scenario to graph impact inputs:

```bash
dhk bdd bind-graph --scenario order-query-happy-path --sql-table legacy_order
dhk bdd bind-graph --scenario order-query-happy-path --file src/main/java/com/acme/OrderController.java
dhk bdd bind-graph --scenario order-query-happy-path --symbol com.acme.OrderService.query
```

The default relation is `impacts`. `graph impact --scenario <key>` reads the
scenario's `file`, `symbol`, and `sql_table` bindings, runs the normal Graph
Lite impact logic for each input, and writes:

```text
.agents/bdd/exports/SCENARIO_IMPACT_MAP.md
```

Scenario impact remains advisory. It inherits Graph Lite freshness checks and
will reject stale snapshots unless `--allow-stale` is explicitly approved by
policy or human evidence.

## Schema

BDD v10 tables:

```text
bdd_feature
bdd_scenario
bdd_step
bdd_binding
bdd_evidence
bdd_quality_issue
```

DevHarness Kit 1.0.0 actively uses features, scenarios, steps, spec/goal/
workflow/graph/test bindings, evidence rows, and quality issues.

## Export Contract

`dhk bdd export` writes:

```text
.agents/bdd/exports/BDD_CONTEXT.md
.agents/bdd/features/<feature>.feature
```

`dhk bdd verify` and `dhk bdd coverage` write:

```text
.agents/bdd/exports/BDD_EVIDENCE.md
.agents/bdd/exports/BDD_COVERAGE.md
```

`BDD_CONTEXT.md` required section order:

```text
# BDD_CONTEXT
<generated-at>
<boundary>
<features>
<scenarios>
<agent-instructions>
```

The context export must remind agents that BDD scenarios are acceptance intent,
not proof of implementation. Evidence, coverage, and goal checks are separate
stages.

Scenario bindings are exported inside the scenario block:

```text
bindings:
- spec_acceptance order-query-api:A001 (verifies)
- goal 20260526120000-order-query (supports)
```

## Lint Contract

`dhk bdd lint` records open rows in `bdd_quality_issue`.

Initial rules:

```text
missing_given   error
missing_when    error
missing_then    error
vague_step      warning
vague_scenario  warning
duplicate_scenario_title warning
missing_acceptance_mapping warning
```

The command is advisory by default and returns success when issues are found.
Goal checks can promote quality issues into gates through
`.agents/devharness/goal-check-policy.json`:

```json
{
  "bdd_min_coverage_percent": "100",
  "bdd_min_quality_score": "90",
  "bdd_fail_on_quality_errors": "true",
  "bdd_fail_on_quality_warnings": "false"
}
```

The BDD quality score starts at 100, subtracts 25 for each lint error and 10 for
each lint warning on goal-bound scenarios, and is clamped at 0. By default,
quality warnings remain advisory and evidence coverage still requires all
goal-bound scenarios to be covered.

Quality gates are explicit failure signals, not behavioral proof. A passing BDD
gate means the required scenarios are bound, evidence is accepted, and configured
lint/coverage thresholds are satisfied. It does not prove the implementation is
correct without compile/test checks and human review.

## Boundaries

- SQLite is the source of truth.
- Markdown and `.feature` files are generated exports.
- BDD scenarios do not prove behavior is implemented.
- BDD quality gates are acceptance-support checks. They should be read together
  with code review, compile/test results, graph impact, and any required manual
  approval.
