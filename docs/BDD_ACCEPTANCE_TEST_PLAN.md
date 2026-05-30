# BDD L0-L9 Acceptance Test Plan

Status: active test plan for the BDD acceptance harness.

This plan verifies BDD capability from the smallest command surface to a full
goal-gated business acceptance flow. Each level must be run independently. When
a defect is found at any level, fix the defect first, then rerun that same level
before moving forward.

## Execution Rule

For every level:

1. Run the listed command or automated test.
2. Check the expected result and generated artifacts.
3. If the result is wrong, patch the behavior or test fixture.
4. Rerun that level until it passes.
5. Only then continue to the next level.

## L0-L9 Matrix

| Level | Acceptance focus | Primary command path | Expected result | Automated coverage |
| --- | --- | --- | --- | --- |
| L0 | Initialize the BDD store and inspect the empty surface. | `dhk bdd init`, `dhk bdd list`, `dhk bdd export` | SQLite schema and `.agents/bdd/` exports are created; empty list/export are readable. | `BddIntegrationTest.bddInitAddListShowAndJsonOutput` |
| L1 | Create, list, and show one scenario through the user-facing alias. | `dhk bdd scenario create/list/show` | Scenario is active when Given/When/Then are complete; duplicate keys return an actionable error. | `BddIntegrationTest.bddScenarioAliasCreatesListsShowsAndRejectsDuplicates` |
| L2 | Validate input quality before a scenario becomes trusted acceptance context. | `dhk bdd add`, `dhk bdd lint` | Missing core steps keep a scenario in draft; vague or duplicate scenarios create quality issues. | `BddIntegrationTest.bddAddMissingCoreStepsCreatesDraftAndSensitiveInputIsRejected`, `BddIntegrationTest.bddLintRecordsMissingVagueAndDuplicateScenarioIssues` |
| L3 | Record evidence and prove verify/coverage behavior. | `dhk bdd evidence add`, `dhk bdd verify`, `dhk bdd coverage` | Missing or pending evidence blocks verify; latest passed evidence covers the scenario and exports reports. | `BddIntegrationTest.bddEvidenceVerifyAndCoverageFailWhenEvidenceIsMissingOrPending` |
| L4 | Bind scenarios to spec and goal gates. | `dhk bdd bind-spec`, `dhk bdd bind-goal`, `dhk goal verify` | Goal-scoped BDD checks read only scenarios bound to that goal and require goal-specific passed evidence. | `BddIntegrationTest.bddBindSpecAndGoalExposeScenarioTraceability`, `BddIntegrationTest.bddVerifyGoalAggregatesOnlyGoalBoundScenarios`, `BddIntegrationTest.goalVerifyRunsBddRequiredCheckForGoalBoundScenarios` |
| L5 | Import executable evidence without depending on external test runners at runtime. | `dhk bdd bind-test`, `dhk bdd evidence junit`, `dhk bdd evidence report` | Surefire/Failsafe, Cucumber, Postman, and Playwright report files map to BDD evidence statuses. | `BddIntegrationTest.bddJunitEvidenceAdapterImportsSurefireResultsAndVerifySeesFailures`, `BddIntegrationTest.bddOptionalJsonReportAdaptersImportExternalAcceptanceResults` |
| L6 | Enforce configured BDD quality and coverage policy. | `dhk goal verify` with `bdd_required=true` and policy thresholds | Low quality score or insufficient coverage blocks the BDD goal check. | `BddIntegrationTest.goalBddCheckCanFailOnConfiguredQualityScoreGate`, `BddIntegrationTest.goalBddCheckCanFailOnConfiguredCoverageThreshold` |
| L7 | Preserve traceability in downstream context exports. | `dhk spec export`, `dhk workflow export`, `dhk graph impact --scenario` | Spec, workflow, and graph exports show scenario bindings and latest evidence context. | `BddIntegrationTest.specAndWorkflowExportsIncludeBddTraceability`, `BddIntegrationTest.bddAndGraphRequiredGoalRequiresScenarioImpactMap` |
| L8 | Defensively reject malformed or stale evidence paths. | `dhk bdd evidence add --evidence-path`, completed-goal evidence attempts | Missing evidence files and completed-goal late evidence are rejected; accepted evidence remains clean. | `BddIntegrationTest.bddDefensiveValidationKeepsEvidenceTraceClean` |
| L9 | Prove one realistic business acceptance end to end. | Address-delivery validation scenario through goal start, BDD bind, evidence, verify | A real scenario can be created, bound, evidenced, and accepted by the BDD-required goal check. | `BddIntegrationTest.addressDeliveryValidationEndToEndBddAcceptancePassesGoalCheck` |

## Suggested Local Run Order

Start with the focused suite while developing:

```bash
mvn -q -Dtest=CommandRouterTest,BddIntegrationTest test
```

Before claiming a BDD capability release, include graph and goal check coverage:

```bash
mvn -q -Dtest=BddIntegrationTest,GraphCommandIntegrationTest,GoalCheckServiceTest,CommandRouterTest test
```

Use the full test suite when touching shared goal, workflow, spec, graph, or
storage behavior:

```bash
mvn -q test
```

## Manual Smoke Flow

```bash
dhk bdd scenario create \
  --scenario address-delivery-validation \
  --title "新增配送地址校验" \
  --given "用户提交新增配送地址请求" \
  --when "地址缺少必填字段或格式非法" \
  --then "系统返回明确的校验错误"

dhk bdd bind-goal --scenario address-delivery-validation --goal <goal-key>

dhk bdd evidence add \
  --scenario address-delivery-validation \
  --goal <goal-key> \
  --status passed \
  --type manual \
  --summary "新增配送地址校验已人工验收" \
  --evidence-path .agents/bdd/evidence/address-delivery-validation.txt

dhk bdd verify --goal <goal-key>
dhk goal verify --goal <goal-key>
```

This smoke flow is intentionally small. It proves that a human-readable BDD
scenario can become goal-gated acceptance evidence without requiring Cucumber,
Postman, Playwright, or a dedicated BDD runtime.
