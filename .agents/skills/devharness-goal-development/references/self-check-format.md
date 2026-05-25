# Self-Check Format

End each response with:

```text
DevHarness self-check:
- goal: <goal-key or not-started>
- current_action: <action>
- changed_files: <files or none>
- checks: <passed/pending/failed>
- completion: <not-ready/ready/completed>
```

Do not write `completed` unless `goal complete` succeeded.
