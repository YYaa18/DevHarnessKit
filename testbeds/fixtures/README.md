# Testbed Fixtures

Put committed synthetic projects under this directory.

Recommended first fixtures:

```text
legacy-mybatis-order/
legacy-jsp-servlet-shop/
modern-java-api/
```

Each fixture should include:

- a small project tree;
- task directories under `tasks/<task-id>/`;
- `task.md`, `ground-truth.json`, and `scorecard.md` for every task;
- safe fake data only;
- protected-file examples when the fixture needs policy tests.

Fixtures should be intentionally small. Prefer clear relationships over volume.
