# BDD Spec/Goal Binding Test Matrix

Status: V0.6.3 contract lock.

This document records the implemented test coverage for BDD spec/goal binding,
evidence, coverage, and goal verification. It is a companion to
`DEVHARNESS_BDD_AWARE_ACCEPTANCE_HARNESS_DETAILED_PLAN.md`; the detailed plan is
design context, while this file tracks the behavior now enforced by tests.

## Covered Behaviors

| Contract | Test coverage | Expected result |
| --- | --- | --- |
| Scenario can bind to spec acceptance | `BddIntegrationTest.bddBindSpecAndGoalExposeScenarioTraceability` | `bdd_binding` contains `spec_acceptance` row and `bdd show` / `BDD_CONTEXT.md` expose it. |
| Scenario can bind to goal run | `BddIntegrationTest.bddBindSpecAndGoalExposeScenarioTraceability` | `bdd_binding` contains `goal` row and generated `.feature` files include binding comments. |
| Missing BDD evidence fails verify | `BddIntegrationTest.bddEvidenceVerifyAndCoverageFailWhenEvidenceIsMissingOrPending` | `bdd verify` returns validation error and writes `BDD_EVIDENCE.md` / `BDD_COVERAGE.md`. |
| Pending latest evidence fails verify | `BddIntegrationTest.bddEvidenceVerifyAndCoverageFailWhenEvidenceIsMissingOrPending` | JSON output reports `pending_evidence=1` and scenario status `pending`. |
| Passed latest evidence satisfies verify | `BddIntegrationTest.bddEvidenceVerifyAndCoverageFailWhenEvidenceIsMissingOrPending` | `bdd verify` returns success and scenario status becomes `covered`. |
| Coverage report is generated | `BddIntegrationTest.bddEvidenceVerifyAndCoverageFailWhenEvidenceIsMissingOrPending` | `bdd coverage --json` succeeds and `BDD_COVERAGE.md` reports `coverage_status: passed`. |
| BDD-required goal check blocks pending scenario | `BddIntegrationTest.goalVerifyRunsBddRequiredCheckForGoalBoundScenarios` | `goal verify` includes failed `bdd` check while latest goal-specific evidence is pending. |
| BDD-required goal check passes after evidence | `BddIntegrationTest.goalVerifyRunsBddRequiredCheckForGoalBoundScenarios` | `goal check --check bdd` reports `passed` after goal-specific passed evidence is recorded. |
| GOAL_CONTEXT exposes BDD state | `BddIntegrationTest.goalVerifyRunsBddRequiredCheckForGoalBoundScenarios` | `<bdd-status>` shows bound scenario count, covered count, and BDD report paths. |
| Scenario can bind to Graph input | `GraphCommandIntegrationTest.graphImpactCanUseBddScenarioGraphBindings` | `bdd bind-graph` stores `sql_table` input and `graph impact --scenario` exports `SCENARIO_IMPACT_MAP.md`. |
| Legacy scenario impact is usable | `GraphCommandIntegrationTest.graphImpactCanUseBddScenarioGraphBindings` | Legacy MyBatis fixture scenario impact includes controller, service, mapper, SQL, and related tests. |
| Modern scenario impact is usable | `GraphCommandIntegrationTest.graphImpactByScenarioCoversModernFixture` | Modern Java API fixture scenario impact includes service and related tests without relying on exact exhaustive impact proof. |
| Missing scenario graph binding fails | `GraphCommandIntegrationTest.graphImpactByScenarioFailsWithoutGraphBindings` | `graph impact --scenario` returns runtime error and points to `bdd bind-graph`. |
| Stale graph snapshot blocks scenario impact | `GraphCommandIntegrationTest.graphImpactByScenarioRejectsStaleGraphSnapshot` | Code changes after `graph index` cause `graph impact --scenario` to fail with `STALE_GRAPH_SNAPSHOT`. |
| BDD+Graph goal requires scenario impact | `BddIntegrationTest.bddAndGraphRequiredGoalRequiresScenarioImpactMap` | `goal verify` fails while `SCENARIO_IMPACT_MAP.md` is missing and `impact` passes after it is generated. |

## Manual Smoke Commands

```bash
dhk bdd add --feature order-query --title "订单查询" \
  --scenario order-query-happy-path --scenario-title "分页查询订单成功" \
  --given "已有订单数据" --when "用户按分页条件查询" --then "返回订单分页结果"

dhk bdd bind-spec --scenario order-query-happy-path \
  --change order-query-api --acceptance A001

dhk bdd bind-goal --scenario order-query-happy-path --goal <goal-key>

dhk bdd bind-graph --scenario order-query-happy-path --sql-table legacy_order

dhk graph impact --scenario order-query-happy-path

dhk bdd verify --feature order-query

dhk bdd evidence add --scenario order-query-happy-path \
  --goal <goal-key> --status passed --type test --summary "验收证据通过"

dhk bdd coverage --feature order-query

dhk goal verify --goal <goal-key>
```

## Release Gate

Before cutting a BDD-aware release candidate, run:

```bash
mvn -q -Dtest=BddIntegrationTest,GraphCommandIntegrationTest,GoalCheckServiceTest,CommandRouterTest test
mvn -q test
```

The release should not claim BDD executable automation. V0.6.3 verifies
specification-level scenario evidence, goal binding, and Graph Lite advisory
scenario impact maps. Graph Lite remains heuristic and does not prove
correctness, impact completeness, or test sufficiency.
