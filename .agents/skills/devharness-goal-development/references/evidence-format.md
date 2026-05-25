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
evidence: impacted_files=OrderController.java; risk_points=pagination; verification_plan=mvn test
evidence: compile_result=passed; test_result=passed; sensitive_result=passed
```

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
