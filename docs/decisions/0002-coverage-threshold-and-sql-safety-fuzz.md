# Decision 0002: Coverage Threshold and SQL Safety Fuzz Backlog

Date: 2026-05-25

Status: Accepted for `0.1.x`; revisit before stable CLI claims.

## Context

DevHarness Kit runs JaCoCo during `mvn test` and writes the report to:

```text
target/site/jacoco/index.html
target/site/jacoco/jacoco.csv
target/site/jacoco/jacoco.xml
```

The current post-alpha-hardening baseline from `target/site/jacoco/jacoco.csv`
is:

| Metric | Baseline |
| --- | ---: |
| Instruction coverage | 84.52% |
| Line coverage | 84.87% |
| Branch coverage | 62.14% |
| Complexity coverage | 62.38% |

The baseline is useful, but not yet a stable quality gate. The CLI surface has
recently grown across memory, DB readonly, goal, workflow, spec, release
packaging, and agent skills. A global threshold added too early would be easy to
game, brittle across Java/OS matrix jobs, and noisy for packaging or CLI-entry
classes that are better covered by smoke tests.

SQL safety is more important than a single aggregate coverage number. The
current `SqlSafetyGuardTest` covers core examples, but does not yet exercise
generated combinations of comments, quotes, semicolons, whitespace, mixed case,
identifier quoting, and mutation keywords.

## Decision

Do not add a JaCoCo `check` threshold to CI in `0.1.x`.

Keep CI stable with:

```bash
mvn -B clean test
mvn -B -DskipTests package
```

Continue generating JaCoCo reports on every test run, document the current
baseline, and revisit threshold enforcement after the 1.0 compatibility contract
is closer to stable.

For SQL safety, create an explicit fuzz/property-test backlog instead of adding
ad hoc random tests immediately.

## Rationale

Coverage gates are most useful when the public surface is stable enough that
failures signal real quality regressions. In `0.1.x`, a hard gate would mostly
penalize expected alpha churn:

- DB command coverage depends on real database behavior that CI intentionally
  avoids for safety and portability.
- `Main`, tiny DTOs, and wrapper-like command branches can distort aggregate
  thresholds.
- Branch and complexity coverage are far lower than line coverage, so choosing
  one global threshold would either be toothless or noisy.
- Security-sensitive code needs targeted tests, not only aggregate coverage.

The right next step is to preserve the baseline and improve high-risk targeted
coverage before enforcing a global threshold.

## Future Coverage Gate Shape

Revisit coverage enforcement when:

- command/output compatibility has stabilized beyond alpha;
- package boundaries are less volatile;
- DB readonly beta behavior has settled;
- SQL safety and sensitive-data guards have stronger targeted tests;
- the CI matrix remains stable across Java 8, Java 17, Windows, macOS, and Linux.

Candidate future policy:

- start with report-only baseline checks in release review;
- add a line or instruction threshold only after two consecutive releases remain
  above the candidate threshold;
- avoid branch/complexity gates until command routing and DB paths have more
  targeted coverage;
- prefer package-specific gates for core services before global gates;
- keep generated exports, package assembly, and wrapper behavior covered by
  integration/smoke tests.

## SQL Safety Fuzz Backlog

Add deterministic fuzz/property tests for `SqlSafetyGuard` before treating DB
readonly as anything stronger than beta.

Suggested test scope:

- generated readonly statements with mixed whitespace, comments, case, and
  optional trailing semicolon are accepted;
- generated mutation statements using `insert`, `update`, `delete`, `drop`,
  `alter`, `truncate`, `create`, `grant`, `revoke`, `call`, `set`, `replace`,
  and `load` are rejected;
- multiple statements are rejected even when the second statement is hidden
  after comments or whitespace;
- semicolons inside string literals, quoted identifiers, and comments do not
  create false multiple-statement failures;
- high-risk functions and patterns such as `sleep(`, `load_file(`,
  `into outfile`, and `into dumpfile` remain rejected;
- `--explain` only wraps `SELECT` statements;
- explicit `EXPLAIN` only applies to readonly statements;
- unbalanced quotes or comments should fail closed if parser behavior becomes
  ambiguous.

Implementation guidance:

- use deterministic seeds so CI failures are reproducible;
- keep generated samples small and easy to print on failure;
- test invariants separately from hand-picked regression examples;
- do not run real DB SQL from fuzz tests;
- keep the SQL guard documented as a heuristic guardrail, not a database
  permission boundary.

## Consequences

For `0.1.x`:

- CI remains stable and does not fail on coverage percentage changes alone.
- Release review should inspect the JaCoCo report and watch large drops.
- SQL safety improvement is tracked as targeted follow-up work.

For 1.0 readiness:

- coverage gates should be introduced only with a documented threshold decision;
- SQL safety fuzz/property tests should land before any stable DB readonly
  claim;
- aggregate coverage should not replace security-specific tests and threat
  model documentation.
