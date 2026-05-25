# Evidence Format

Good evidence is concrete:

```text
changed_files: OrderController.java, OrderService.java
evidence: existing_controller=OrderController#getList; compile=passed; tests=OrderServiceTest
```

Mirror required evidence keys exactly when GOAL_CONTEXT lists them:

```text
evidence: impacted_files=OrderController.java; risk_points=pagination; verification_plan=mvn test
evidence: compile_result=passed; test_result=passed; sensitive_result=passed
```

For `changed_files`, prefer the `--changed-files` option and keep `--evidence` for why the change satisfies the current action.

Avoid vague evidence:

```text
evidence: checked things
```

Every `goal step` should include what was inspected, changed, or verified.
