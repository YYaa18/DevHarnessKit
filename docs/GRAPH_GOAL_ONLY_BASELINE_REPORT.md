# Goal-only Baseline Report

Status: V0.4.4 baseline sample, not a statistical benchmark.

## Purpose

Before building Graph Lite, DevHarnessKit needs a small, auditable baseline for
what the current Goal-only harness can and cannot do. These samples answer a
narrow question:

```text
What should Graph Lite improve first, compared with goal-first workflow alone?
```

The current sample set intentionally uses manual scoring exercises instead of a
multi-model benchmark. That keeps the alpha work reproducible on a local
machine and avoids pretending that two or three tasks are statistically enough.

## Sample Set

| Fixture | Task | DQI | Impact Recall | Impact Precision | Main Weakness |
| --- | --- | ---: | ---: | ---: | --- |
| legacy-mybatis-order | fix-null-page-bounds | 82 | 0.67 | 1.00 | Caller impact is easy to miss. |
| modern-java-api | add-status-search-test-gap | 74 | 0.71 | 0.83 | Repository test gap is easy to miss. |

Source files:

```text
testbeds/baselines/goal-only/legacy-mybatis-order/fix-null-page-bounds/result.json
testbeds/baselines/goal-only/modern-java-api/add-status-search-test-gap/result.json
```

## Findings

Goal-only is already adequate for narrow implementation tasks when the user or
agent reads the fixture carefully and follows `goal verify`.

The weak spots are not basic compile/test execution. They are:

- incomplete impact recall for callers and downstream tests;
- weak detection of missing related tests;
- manual effort required to build an impact map;
- limited confidence around architecture boundary checks;
- inconsistent recognition of JSP/XML/SQL/config relationships in legacy code.

## Graph Lite Minimum Acceptance

Graph Lite Core should not claim success merely by producing a graph file. The
minimum useful alpha bar is:

| Requirement | Minimum |
| --- | --- |
| Fixture coverage | Can index `legacy-mybatis-order`, `legacy-jsp-servlet-shop`, and `modern-java-api`. |
| Impact recall | At least 0.75 average on the tracked baseline tasks. |
| Impact precision | At least 0.70 average without flooding unrelated files. |
| Test gap signal | Flags the missing repository test in `modern-java-api-003`. |
| Legacy relation signal | Links route/JSP/XML/SQL/DAO/service where fixture data exists. |
| Safety | Does not export sensitive values or protected file contents. |
| Reproducibility | Exports snapshot metadata and truncation/skip reports. |
| Performance | Small fixtures should index in seconds, not minutes. |

## First Metrics To Improve

Priority order for V0.5.0:

1. Impact recall for callers and tests.
2. Test gap detection.
3. Architecture boundary signal.
4. Legacy route/config/XML relationship signal.
5. Precision and truncation reporting.

## Non-goals

This report does not evaluate raw agents, multiple models, team workflows, or
production repositories. Those belong after Graph Lite can produce useful local
snapshots on these fixtures.
