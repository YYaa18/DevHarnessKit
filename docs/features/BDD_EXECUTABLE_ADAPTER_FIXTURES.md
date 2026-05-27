# Executable BDD Adapter Fixtures

DevHarnessKit executable BDD adapters consume report files that already exist.
They do not start Cucumber, Newman, Playwright, Maven, or any browser process.
This keeps the core BDD CLI usable when those tools are not installed.

## Adapter Commands

Manual evidence:

```bash
dhk bdd evidence add --scenario order-query-happy-path \
  --type manual \
  --status passed \
  --summary "人工验证分页查询订单成功"
```

JUnit / MockMvc evidence from Maven Surefire or Failsafe XML:

```bash
dhk bdd bind-test --scenario order-query-happy-path \
  --class com.example.OrderControllerTest \
  --method querySuccess

dhk bdd evidence junit --reports target/surefire-reports \
  --scenario order-query-happy-path
```

External JSON report evidence:

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

## Status Mapping

| Source | Input Status | BDD Evidence Status | Verify Result |
| --- | --- | --- | --- |
| manual | passed | passed | covered |
| manual | failed | failed | failed |
| manual | pending | pending | pending |
| JUnit XML | testcase without failure/error/skipped | passed | covered |
| JUnit XML | failure/error child | failed | failed |
| JUnit XML | skipped child | skipped | covered |
| JUnit XML | missing report or missing binding result | pending | pending |
| Cucumber JSON | status passed | passed | covered |
| Cucumber JSON | status failed/undefined/ambiguous | failed | failed |
| Postman JSON | non-empty failures or failed count > 0 | failed | failed |
| Playwright JSON | status failed/timedOut/interrupted | failed | failed |

## Minimal Fixtures

Surefire / Failsafe XML:

```xml
<testsuite name="com.example.OrderControllerTest" tests="3" failures="1" skipped="1">
  <testcase classname="com.example.OrderControllerTest" name="querySuccess"/>
  <testcase classname="com.example.OrderControllerTest" name="queryFailure">
    <failure message="assertion failed"/>
  </testcase>
  <testcase classname="com.example.OrderControllerTest" name="querySkipped">
    <skipped/>
  </testcase>
</testsuite>
```

Cucumber JSON:

```json
[
  {
    "elements": [
      {
        "name": "order-query-cucumber",
        "steps": [
          { "result": { "status": "passed" } }
        ]
      }
    ]
  }
]
```

Newman / Postman JSON:

```json
{
  "run": {
    "stats": { "assertions": { "failed": 1 } },
    "failures": [{ "source": { "name": "order-query-postman" } }]
  }
}
```

Playwright JSON:

```json
{
  "suites": [
    {
      "specs": [
        {
          "title": "order-query-playwright",
          "tests": [
            { "results": [{ "status": "skipped" }] }
          ]
        }
      ]
    }
  ]
}
```

## Boundary

The JSON report adapters are lightweight status extractors. They attach an
external acceptance result to a BDD scenario, but the original tool report
remains the detailed source for debugging. Keep those reports in CI artifacts
when using them for release evidence.
