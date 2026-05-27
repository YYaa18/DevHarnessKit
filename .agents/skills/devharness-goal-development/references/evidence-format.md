# Evidence Format

Good evidence is concrete and structured when possible:

```text
--read-files OrderController.java,OrderService.java,OrderMapper.xml,OrderServiceTest.java
--changed-files OrderController.java,OrderService.java
--compile-result passed
--tests-run OrderServiceTest
--risks pagination boundary
--pending none
```

Mirror required evidence keys exactly when GOAL_CONTEXT lists them:

```text
evidence: goal_understanding=User wants a focused API fix; assumptions=no schema change; pending=none
evidence: impacted_files=OrderController.java; risk_points=pagination; verification_plan=mvn test
evidence: compile_result=passed; test_result=passed; sensitive_result=passed
```

When using the CLI directly, prefer repeated `--field key=value` arguments for
required evidence keys:

```bash
dhk goal step --goal <goal-key> \
  --summary "Inspected existing flow" \
  --field existing_controller=OrderController \
  --field existing_service=OrderService \
  --field existing_mapper=OrderMapper \
  --field existing_tests=OrderServiceTest
```

Use the template and dry-run helpers before recording uncertain evidence:

```bash
dhk goal step --goal <goal-key> --template
dhk goal step --goal <goal-key> --summary "Planned change" \
  --field impacted_files=OrderController.java \
  --field risk_points=pagination-boundary \
  --field verification_plan="mvn test" \
  --dry-run
```

Before implementation, include `goal_understanding=<summary>` and either
`assumptions=<summary>` or `pending=<questions-or-none>`. The strict
`think-before-coding` gate uses these fields to verify that the task was
understood before code changed.

When the change spans many files or touches protected paths, include:

```text
evidence: scope_justification=API change needs controller/service/test updates
evidence: impact_evidence=IMPACT_MAP.md reviewed
evidence: protected_file_confirmation=approved
```

The `simplicity` gate flags broad changes without scope evidence and abstraction
risk files without `simplicity_justification=<why-needed>`. The
`surgical-change` gate flags broad changes without impact/scope evidence and
protected files without manual confirmation.

Prefer structured fields for common evidence:

```text
--read-files      files inspected before editing
--changed-files   files modified
--tests-run       tests or checks run
--compile-result  compile/build result
--risks           risk points considered
--pending         remaining work or none
```

Keep `--evidence` for required keys that do not map cleanly to a structured field, such as `verification_plan` or `sensitive_result`.

Avoid vague evidence:

```text
evidence: checked things
```

Every `goal step` should include what was inspected, changed, or verified.
