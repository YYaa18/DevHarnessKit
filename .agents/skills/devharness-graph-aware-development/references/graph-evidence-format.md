# Graph Evidence Format

Use concrete graph evidence in `goal step`:

```text
graph_snapshot=GRAPH_SNAPSHOT.json
graph_context=GRAPH_CONTEXT.md
impact_map=IMPACT_MAP.md
impacted_files=OrderController.java,OrderService.java,OrderMapper.xml
risk_nodes=sql_statement OrderMapper.findOrders
recommended_read_files=OrderController.java,OrderServiceTest.java
```

After implementation:

```text
post_change_impact_map=IMPACT_MAP.md
impact_delta=changed files remain inside related-files
changed_files_covered=OrderController.java,OrderService.java
```

For legacy graph profiles, record rollback and manual evidence:

```text
rollback_plan=.agents/memory/artifacts/goals/<goal-key>/ROLLBACK_PLAN.md
rollback_scope=single JSP/Servlet search flow
manual_evidence=verified legacy search form in local fixture
manual_evidence_status=passed
manual_evidence_path=.agents/memory/artifacts/goals/<goal-key>/MANUAL_EVIDENCE.md
protected_file_confirmation=approved
```

For `goal step`, also use structured fields:

```text
--changed-files src/main/java/com/acme/OrderController.java
--tests-run OrderServiceTest
--risks impact map includes SQL mapper and pagination path
--pending none
```

Do not paste raw graph output into long-term memory. Reference generated files by path and summarize only the relevant impact.
