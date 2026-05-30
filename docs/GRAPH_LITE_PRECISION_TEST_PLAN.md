# Graph Lite Precision Test Plan

Status: implemented local precision suite with structured result JSON output.

This plan focuses on proving and improving Graph Lite precision without turning
Graph Lite into a correctness proof. Graph output remains advisory context. The
goal of this plan is narrower: make impact maps less noisy, make false positives
visible, and keep recall from regressing while precision improves.

## Goals

- Measure Graph Lite impact precision and recall against fixture ground truth.
- Separate parser correctness, impact traversal, test-gap detection, ranking,
  stale-snapshot safety, and performance into independent test layers.
- Turn current manual baseline samples into repeatable regression checks.
- Keep the test plan useful for both local development and future CI/release
  gates.

## Non-Goals

- Do not claim full Java type resolution, runtime dispatch coverage, or SQL
  semantic equivalence.
- Do not require CGC or any external graph provider for the default Lite path.
- Do not make fixture metrics a statistical benchmark for arbitrary production
  projects.
- Do not treat `IMPACT_MAP.md` as proof that implementation changes are correct.

## Precision Terms

Use these definitions consistently across tests:

| Term | Definition |
| --- | --- |
| True positive | A reported file, test, SQL node, route, or risk node that matches the task ground truth. |
| False positive | A reported item that is not in ground truth and is not explicitly allowed as task context. |
| False negative | A ground-truth item that Graph Lite did not report. |
| Impact recall | `true_positive_count / expected_impact_count`. |
| Impact precision | `true_positive_count / reported_relevant_count`. |
| Top-K precision | Precision within the first `K` `recommended-read-files`, normally `K=5` and `K=10`. |
| Forbidden-file precision | Protected, sensitive, annotation infrastructure, shared base classes, and unrelated config must not appear as recommended changes. |

Precision should be scored at two levels:

- **strict precision**: only files or nodes listed in `expected_impact_points`
  and `expected_related_tests` count as true positives.
- **advisory precision**: explicitly allowed contextual files may be neutral
  instead of false positives, but they must not hide strict precision regressions.

## Fixture Set

Current curated fixtures:

| Fixture | Strength | Main Precision Risk |
| --- | --- | --- |
| `modern-java-api` | Controller/service/repository/domain/DTO API flow, route annotations, test gaps. | DTO and repository neighbors can over-expand impact maps. |
| `legacy-mybatis-order` | Java service/mapper/MyBatis XML/SQL parameter flow. | Utility and SQL-parameter queries can fan out through broad service/mapper paths. |
| `legacy-jsp-servlet-shop` | JSP form, `web.xml`, servlet, service, DAO, SQL resource flow. | Route/JSP searches can pull shared header/base servlet or miss SQL resources. |

Every fixture task should keep:

```text
task.md
ground-truth.json
scorecard.md
```

`ground-truth.json` is the scoring source. It must list expected impact points,
related tests, forbidden files, hard-failure conditions, and precision notes.

## L0-L9 Test Ladder

| Level | Purpose | Required Coverage | Pass Bar |
| --- | --- | --- | --- |
| L0 | Graph command smoke. | `graph init`, `status`, `index`, `export`, `impact` on each fixture. | Commands succeed; exports exist; no sensitive values are exported. |
| L1 | Parser extraction precision. | Java classes/methods/routes/tests, MyBatis XML statements, SQL tables/parameters, JSP forms, `web.xml` mappings, properties keys. | Expected nodes/edges exist; protected/sensitive content is skipped; property values are not exported. |
| L2 | Query target exactness. | `--file`, `--symbol`, `--sql-table`, missing-symbol candidates, BDD scenario inputs. | Start nodes match the intended target; missing target suggestions are useful and bounded. |
| L3 | Ground-truth impact scoring. | Run every fixture task query from the graph-aware baseline and compare `IMPACT_MAP.md` with `ground-truth.json`. | Recall does not regress below current baseline; strict precision is reported for every task. |
| L4 | False-positive hardening. | DTO sibling suppression, utility-symbol narrowing, SQL-parameter narrowing, shared JSP/header/base servlet exclusion, support annotation exclusion. | Known false positives stay absent; forbidden files never appear in `recommended-read-files`. |
| L5 | Related-test and missing-test precision. | Existing tests, expected missing tests, repository test gaps, generated/test-double exclusions. | Required tests are found; expected gaps are reported; irrelevant repository tests are not falsely reported. |
| L6 | Recommended-read ranking. | Start files, direct callers/callees, expected tests, top-5/top-10 ordering, 20-file cap. | Start file is top 1; direct production files precede broad neighbors; expected tests appear in top 10 when indexed. |
| L7 | Scenario and traceability impact. | `bdd bind-graph`, `graph impact --scenario`, multi-input dedupe, stale-snapshot rejection. | Scenario impact aggregates inputs without duplicates and preserves Graph Lite advisory boundary text. |
| L8 | Safety, freshness, limits, and performance. | Stale snapshot rejection, allow-stale policy, max depth clamp, max indexed files, skipped metadata, protected files. | Freshness and truncation are explicit; small fixtures index in seconds; skipped sensitive/protected content stays redacted. |
| L9 | Regression gate and release baseline. | Automated score runner writes result JSON and scorecard for all curated tasks. | Average recall and precision meet thresholds; per-task regressions require an explicit baseline update review. |

## Curated Query Matrix

Use these as the first repeatable precision suite:

| Fixture | Task | Query | Precision Focus |
| --- | --- | --- | --- |
| `modern-java-api` | `add-risk-rating-field` | `graph impact --file src/main/java/com/acme/modern/account/dto/AccountResponse.java --depth 4` | Public response DTO should include controller/service/domain/repository seed/tests without sibling request DTO noise. |
| `modern-java-api` | `add-freeze-endpoint-boundary` | `graph impact --file src/main/java/com/acme/modern/account/controller/AccountController.java --depth 4` | Public API route and architecture risk should be visible without support annotation edits. |
| `modern-java-api` | `add-status-search-test-gap` | `graph impact --file src/main/java/com/acme/modern/account/service/AccountService.java --depth 4` | Repository test gap should be detected and not confused with test-double gaps. |
| `legacy-mybatis-order` | `add-customer-level-field` | `graph impact --sql-table legacy_order --depth 6` | Mapper XML, DTO, service, test, and route recall; avoid production config. |
| `legacy-mybatis-order` | `tighten-paid-order-filter` | `graph impact --symbol paidOnly --depth 3` | SQL parameter should include both SQL statements and test double, not controller/utility neighbors. |
| `legacy-mybatis-order` | `fix-null-page-bounds` | `graph impact --symbol LegacyPageBounds --depth 3` | Narrow utility query should stay utility/caller/test only. |
| `legacy-jsp-servlet-shop` | `add-customer-tier-filter` | `graph impact --symbol ShopOrderServlet --depth 3` | JSP/servlet/service/DAO/test recall and SQL resource visibility. |
| `legacy-jsp-servlet-shop` | `preserve-legacy-search-action` | `graph impact --file src/main/webapp/WEB-INF/web.xml --depth 3` | Route compatibility should include current route path without shared header/base servlet noise. |

## Thresholds

Use current baselines as the starting floor, then tighten in phases.

Initial L9 gate:

- average impact recall: `>= 0.90`;
- no curated task recall below `0.85`, except documented future-route cases;
- average strict impact precision: `>= 0.60`;
- no curated task precision below its current committed baseline by more than
  `0.05`;
- forbidden/protected file recommendations: `0`;
- sensitive value leaks: `0`;
- stale snapshot accepted without explicit evidence: `0`.

Precision hardening target:

- average strict impact precision: `>= 0.70`;
- narrow symbol queries, such as utility and SQL parameter tasks: `>= 0.75`;
- top-10 recommended-read precision: `>= 0.70`;
- start node top-1 accuracy: `100%`;
- expected related test in top 10 when indexed: `100%`.

## False-Positive Taxonomy

When precision fails, classify the cause before changing traversal:

| Code | Meaning | Example |
| --- | --- | --- |
| `sibling-dto` | Neighbor DTO/request/response is pulled only by package proximity. | `AccountUpdateRequest` appears for `AccountResponse`. |
| `test-double-gap` | Test gap inferred for `InMemory`, `Noop`, config, publisher, or generated-style class. | `InMemoryAccountRepositoryTest` appears as missing. |
| `layer-fanout` | Traversal expands from service into unrelated controller/repository route graph. | Utility query pulls controller and mapper XML. |
| `shared-ui` | Shared JSP include/base servlet is treated as task-specific impact. | `header.jsp` or `LegacyBaseServlet` appears as recommended. |
| `infra-annotation` | Route annotation/support infrastructure is treated as application code impact. | `HttpPost.java` appears for new endpoint work. |
| `config-noise` | Runtime config or production config appears as related change target. | `application-prod.properties` appears in recommendations. |
| `sql-overreach` | SQL table or parameter query pulls unrelated statements or columns. | `paidOnly` pulls unrelated mapper statements. |

Each false-positive fix should add a regression assertion before or with the
algorithm change.

## Automation Plan

The first automation slice is implemented as:

```text
scripts/graph-lite-precision-suite.sh
```

It runs the curated JUnit score harness:

```text
GraphLitePrecisionSuiteTest
```

Implemented flow:

1. Copy each fixture to an isolated temporary directory.
2. Run `dhk graph index`.
3. Run the curated query for each fixture task.
4. Parse `IMPACT_MAP.md` sections:
   - `start-nodes`
   - `related-files`
   - `related-tests`
   - `missing-related-tests`
   - `related-sql`
   - `risk-nodes`
   - `recommended-read-files`
5. Compare parsed output with `ground-truth.json`.
6. Enforce recall, strict precision, top-10 ranking, expected-test ranking, and
   known false-positive/forbidden-file assertions.
7. Write structured result JSON with suite averages and case-level diagnostics.
8. Run from the release gate by default:

```text
scripts/release-gate.sh
```

Use `scripts/release-gate.sh --skip-graph-lite-precision` only for targeted
release-gate diagnosis where the precision suite has already been run and
recorded separately. The legacy `--with-graph-lite-precision` flag is kept as a
compatibility no-op.

The standalone suite writes ignored local result output under:

```text
testbeds/runs/graph-lite-precision/<timestamp>/
```

The release gate writes its default precision result under:

```text
target/graph-lite-precision/result.json
```

Reviewed results may later be promoted into:

```text
testbeds/baselines/graph-lite-precision/
```

Current result JSON includes:

```json
{
  "schema_version": "devharness-graph-lite-precision-result/v1",
  "suite": "graph-lite-precision",
  "passed": true,
  "case_count": 8,
  "average_recall": 0.96,
  "average_precision_strict": 0.85,
  "average_top10_precision": 0.87,
  "cases": [
    {
      "fixture": "modern-java-api",
      "task_id": "modern-java-api-001",
      "graph_query": "dhk graph impact ...",
      "impact_recall": 0.88,
      "impact_precision_strict": 0.70,
      "top10_precision": 0.70,
      "missing_expected": [],
      "false_positives": [],
      "forbidden_hits": [],
      "sensitive_leaks": 0,
      "snapshot_stale": false,
      "duration_ms": 0,
      "passed": true
    }
  ]
}
```

## Implemented Slices

1. **Score Parser**
   `GraphLitePrecisionSuiteTest` parses `IMPACT_MAP.md` into structured
   sections and compares file-level output to `ground-truth.json`.

2. **Top-K Ranking Tests**
   The precision suite asserts start file top 1, top-10 precision, and expected
   related tests in top 10.

3. **Precision Negatives**
   The suite asserts known false positives stay absent from recommendations:
   sibling DTOs, annotation infrastructure, shared JSP/header, base servlet,
   protected config, utility overreach, and test-double gaps.

4. **SQL/JSP Recall Gaps**
   Graph Lite now parses standalone SQL resources and the suite asserts the
   legacy JSP/Servlet/DAO path keeps `src/main/resources/sql/*.sql` visible for
   field-flow impact while route-only web.xml checks avoid SQL overreach.

5. **Release Gate**
   `scripts/release-gate.sh` runs the suite by default and writes ignored
   diagnostics under `target/graph-lite-precision/result.json`. Use
   `--skip-graph-lite-precision` only for targeted diagnosis; the historical
   `--with-graph-lite-precision` flag remains accepted for compatibility.

6. **Structured Result JSON**
   `GraphLitePrecisionSuiteTest` writes `result.json` with suite averages,
   thresholds, per-case recall, strict precision, top-10 precision, missing
   expected files, false positives, forbidden hits, and pass/fail diagnostics.
   `scripts/graph-lite-precision-suite.sh` writes to
   `testbeds/runs/graph-lite-precision/latest/result.json` by default. Use
   `DHK_GRAPH_PRECISION_RESULT_DIR` or `DHK_GRAPH_PRECISION_RESULT_JSON` to
   redirect the ignored local output.

## Review Checklist

Before claiming Graph Lite precision improved:

- The changed behavior has at least one ground-truth-backed test.
- Recall did not drop below the task floor.
- Precision improvement is visible in strict precision, not only advisory
  precision.
- `recommended-read-files` is shorter or better ranked for the target query.
- Forbidden/protected/sensitive files remain absent.
- Stale-snapshot and truncation metadata are still present.
- The plan result says "advisory" and does not imply correctness proof.
