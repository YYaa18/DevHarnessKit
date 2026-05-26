# Legacy Graph-aware Evaluation Report

Status: V0.5.2 curated alpha evaluation, not a statistical benchmark.

## Purpose

AI-67 checks whether the legacy Graph-aware Goal work actually improves
DevHarnessKit on old-style Java projects. The evaluation uses the synthetic
fixtures under `testbeds/fixtures/` and compares Goal-only reference scores
against Graph-aware Goal samples.

The goal is narrow:

```text
Does Graph-aware Goal improve impact recall and completion evidence on legacy
tasks without increasing protected-file or sensitive-data risk?
```

## Method

The fixtures were copied to a temporary directory and evaluated with the current
alpha jar:

```text
dhk graph index --project-root <fixture>
dhk graph export --project-root <fixture>
dhk graph impact --project-root <fixture> --file|--symbol|--sql-table <query>
mvn -q -f testbeds/fixtures/legacy-mybatis-order/pom.xml test
mvn -q -f testbeds/fixtures/legacy-jsp-servlet-shop/pom.xml test
```

Scores are curated maintainer samples. They are useful for regression tracking,
but they are not multi-model statistics.

## Results

| Fixture | Task | Goal-only DQI | Graph-aware DQI | Delta | Goal-only Recall | Graph Recall | Goal-only Precision | Graph Precision | Protected Violations | Rollback Completeness |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| legacy-mybatis-order | fix-null-page-bounds | 82 | 86 | +4 | 0.67 | 1.00 | 1.00 | 0.27 | 0 | 1.00 |
| legacy-mybatis-order | add-customer-level-field | 68 | 88 | +20 | 0.56 | 0.89 | 0.82 | 1.00 | 0 | 1.00 |
| legacy-mybatis-order | tighten-paid-order-filter | 72 | 84 | +12 | 0.60 | 1.00 | 0.83 | 0.45 | 0 | 1.00 |
| legacy-jsp-servlet-shop | add-customer-tier-filter | 64 | 87 | +23 | 0.50 | 0.92 | 0.75 | 0.79 | 0 | 1.00 |
| legacy-jsp-servlet-shop | preserve-legacy-search-action | 70 | 80 | +10 | 0.57 | 0.86 | 0.80 | 0.43 | 0 | 1.00 |

Average:

| Metric | Goal-only | Graph-aware | Delta |
| --- | ---: | ---: | ---: |
| DQI | 71.2 | 85.0 | +13.8 |
| Impact recall | 0.58 | 0.93 | +0.35 |
| Impact precision | 0.84 | 0.59 | -0.25 |
| Protected file violations | 0 | 0 | 0 |
| Rollback completeness | 0.40 | 1.00 | +0.60 |

## Findings

Graph-aware Goal is clearly useful for legacy impact recall. It finds callers,
tests, mapper XML, JSP forms, servlet mappings, and DAO/service neighbors that a
Goal-only flow can easily miss.

The biggest win is not raw correctness on tiny tasks. The win is that the agent
gets an auditable `IMPACT_MAP.md`, explicit rollback/manual evidence
requirements, and protected-file discipline before it edits legacy code.

The tradeoff is precision. Some graph queries are intentionally broad in alpha,
especially utility symbols and `web.xml` route queries. That is safer for old
systems than missing a caller, but it increases reading cost and can confuse weak
models if the profile does not keep the allowed change set strict.

## Threshold Check

| Requirement | Result | Status |
| --- | --- | --- |
| At least 4 legacy tasks scored | 5 tasks | pass |
| Graph-aware DQI improves Goal-only by at least 10 points on legacy average | +13.8 | pass |
| Impact recall is at least 0.80 average | 0.93 | pass |
| Protected file violation is zero | 0 | pass |
| Average impact precision is at least 0.70 | 0.59 | needs work |

## Limitations And Next Fixes

Precision is below the desired alpha threshold. The next graph fixes should
focus on:

1. Narrowing utility-symbol traversal so `LegacyPageBounds` does not pull the
   whole service/mapper route surface into the impact map.
2. Adding stronger SQL parsing for standalone `.sql` resources such as
   `shop-order-search.sql`.
3. Adding task-aware route compatibility hints for future routes such as
   `/shop/orderList.do`, which cannot be discovered from the initial graph.
4. Marking protected impact risk more visibly when broad graph maps include
   files such as shared base servlets or JSP headers.
5. Keeping legacy graph profiles strict: rollback evidence, manual evidence
   where required, and protected-file confirmation must remain completion gates.

## Baseline Files

Graph-aware result files are committed under:

```text
testbeds/baselines/graph-aware/
```

The existing Goal-only sample remains under:

```text
testbeds/baselines/goal-only/
```

Future evaluations can add raw local runs under `testbeds/runs/` first, then
promote selected runs into curated baselines only after review.
