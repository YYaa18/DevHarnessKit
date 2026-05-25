# Evidence Format

Good evidence is concrete:

```text
changed_files: OrderController.java, OrderService.java
evidence: existing_controller=OrderController#getList; compile=passed; tests=OrderServiceTest
```

Avoid vague evidence:

```text
evidence: checked things
```

Every `goal step` should include what was inspected, changed, or verified.
