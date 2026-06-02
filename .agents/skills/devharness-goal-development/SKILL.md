---
name: devharness-goal-development
description: Use DevHarnessKit goal protocol for controlled coding tasks, completion review summaries, MR/PR summaries, and retrospective materials.
---

# DevHarness Goal Development

Use this skill for code changes in repositories that include DevHarnessKit. Development work is goal-first by default.

## Contract Summary

This skill is governed by `contract.json`.

- skill_key: `devharness-goal-development`
- data_access_level: `context`
- allowed_commands: `dhk advise`, `dhk quickstart`, `dhk goal start`, `dhk goal resume`, `dhk goal next`, `dhk goal step`, `dhk goal evidence-template`, `dhk goal status`, `dhk goal export`, `dhk goal check`, `dhk goal evaluate`, `dhk goal verify`, `dhk goal complete`, `dhk goal audit`, `dhk goal recheck`, `dhk goal retrospective`, `dhk goal review-summary`, `dhk goal mr-summary`, `dhk memory search`, `dhk memory suggest`
- forbidden_commands: `dhk workflow gate waive`, `dhk spec archive`, `dhk memory confirm`, `dhk db sql`

Do not use commands outside this contract unless `GOAL_CONTEXT.md` explicitly authorizes them or the user directly requests them.

## Core Path

For ordinary code tasks, keep the user-facing flow simple:

1. Show the user the Work Brief summary: task intent, recommended mode, risk, confirmation needs, and expected work.
   - If the Work Brief says `confirmation_required: true`, show the confirmation reason and checklist, then wait for explicit user confirmation before answering the interaction or continuing. Do not silently pick a confirmation option for the user.
   - If the user is running a step-by-step business acceptance plan, pause after each business step, list what was completed and what needs confirmation, and wait before starting the next step.
2. Read `.agents/devharness/briefs/AGENT_BRIEF.json` yourself when it exists.
3. Do not show `harness_commands` to ordinary users unless they explicitly ask for debugging details.
4. Do not assume a global `dhk` command exists. Prefer the Agent Brief `script` + `args` fields or the project-local wrapper scripts under `.agents/skills/devharness-goal-development/scripts/`.
5. If Agent Brief contains a non-empty `goal_key` and `current_action` is not `completed`, do not start a second goal for the same task. Use the Agent Brief commands for that exact goal.
6. If Agent Brief is missing, has no `goal_key`, or has `current_action` = `completed`, treat the current user request as a new task. Run `quickstart.sh --task "<latest user request verbatim>" --mode recommend --resume-existing --module "<best module>"`, then re-read Work Brief, Agent Brief, and GOAL_CONTEXT before editing.
7. Even typo or documentation-only edits use the lightweight patch flow; do not directly edit README, comments, or docs without a current goal.
8. Never copy a task from an old Work Brief or GOAL_CONTEXT when starting a new goal. Before editing, compare GOAL_CONTEXT task with the latest user request; if they do not match, create a new quickstart goal with the latest request.

Agent-internal execution still uses the goal protocol:

1. `quickstart.sh` creates the task brief and first goal for a new user request. Use `goal-start.sh` or `goal-resume.sh` only when GOAL_CONTEXT or debug instructions explicitly require them.
   - If Agent Brief already has an active `goal_key`, skip `quickstart.sh` and run `goal-next.sh --goal <goal_key>` instead.
   - If Agent Brief has `current_action` = `completed`, it is historical; run `quickstart.sh` for the new request instead of reusing the completed goal.
2. `goal-next.sh` tells you the current action and required evidence.
3. Do only that action, then record the work with `goal-step.sh`.
4. If you changed files and `goal audit` or `goal next` still shows `step_count` unchanged or "goal steps incomplete", self-repair by recording the missing step before replying.
5. If files were edited before any current goal existed, or under a goal whose task does not match the latest user request, self-repair by creating a quickstart patch goal for the user request, then record the already-made edit as a goal step before claiming completion.
6. `goal-verify.sh` runs the readiness checks.
7. `goal-complete.sh` is allowed only after verification reports
   `ready_to_complete`.

## Retrospective And Review Summaries

Use this skill proactively when the user asks for review material, an MR/PR
summary, retrospective output, completion recap, "what changed", "what is left",
or Chinese requests such as "复盘材料", "复盘", "评审摘要", or "合并摘要".

After a goal has recorded meaningful steps and checks, prefer the project-local
wrapper:

- `scripts/goal-retrospective.sh --goal <goal-key>` for human-readable Markdown.
- `scripts/goal-retrospective.sh --goal <goal-key> --write <path>` for a shareable file.
- `scripts/goal-retrospective.sh --goal <goal-key> --json` for automation.

This report is a review artifact, not a substitute for `goal verify` or
`goal complete`. If the goal is still in progress, clearly label the report as
in-progress and keep following the active GOAL_CONTEXT action.

## Troubleshooting Memory (Error To Solution)

Turn solved errors into reusable knowledge so the next similar failure is faster.

When you hit a non-trivial error during a goal (compile error, runtime
exception, failing test with a clear signature, environment/config failure):

1. Before debugging from scratch, search prior solutions:
   `dhk memory search --q "<key part of the error message or signature>"`.
   If a recorded solution matches, apply and verify it instead of re-deriving.
2. After you actually solve it, propose a troubleshooting candidate:
   `dhk memory suggest --title "<short error signature>" --content "现象/Symptom: ...; 根因/Root cause: ...; 修复/Fix: ..."`.
   Keep the title close to the real error text so future searches match.
3. Tell the user it is a pending candidate and that confirming it
   (`dhk memory candidates accept` or `dhk memory confirm`) makes it
   auto-surface in future task context. Do not confirm it yourself.

Boundaries: `dhk memory search` (read) and `dhk memory suggest` (propose) are
allowed for this loop. You still must not run `dhk memory confirm`, must not
accept candidates yourself, and must never put secrets, tokens, passwords,
JDBC URLs, Authorization headers, cookies, or raw SQL results into a memory
suggestion. Record the same fix inside the current `goal step` evidence too,
so the goal record stays complete.

The full protocol below keeps weak-model and release-sensitive work auditable.

## Full Protocol

1. Do not start by editing code.
1. If `.agents/devharness/briefs/WORK_BRIEF.md` exists, summarize it for the user instead of listing raw Harness commands.
1. If `.agents/devharness/briefs/AGENT_BRIEF.json` exists, treat its `harness_commands` as agent-internal only.
2. For a new user request, start through `.agents/skills/devharness-goal-development/scripts/quickstart.sh --task "<latest user request verbatim>" --mode recommend --resume-existing --module "<best module>"`. This creates a fresh Work Brief, Agent Brief, and patch/standard/strict goal without exposing raw commands to the user.
   - If `.agents/devharness/briefs/AGENT_BRIEF.json` has a non-empty `goal_key` and `current_action` is not `completed`, do not start another goal. Use that active goal and its `harness_commands`.
   - If `current_action` is `completed`, the brief is historical. Create a new quickstart goal for the new request before editing.
   - Use `scripts/goal-start.sh` or `scripts/goal-resume.sh` only when GOAL_CONTEXT or debug instructions explicitly require lower-level goal control.
3. Run the skill wrapper `scripts/goal-next.sh` before each work step. When you
   need machine-readable action/evidence data, run the underlying
   `dhk goal next --json` through the wrapper only if the wrapper supports it;
   otherwise read `GOAL_CONTEXT.md`.
4. Read `.agents/memory/exports/GOAL_CONTEXT.md`.
5. Perform only the `current_action` from GOAL_CONTEXT, and stay within its `<allowed-commands>`, `<forbidden-actions>`, `<evidence-contract>`, and `<freshness-status>` sections.
   - If `<graph-assist>` is present, treat graph as part of the current action, not as a separate workflow.
   - Run the listed graph helper internally when the current action needs fresh graph context or impact evidence.
   - Record graph evidence such as `graph_snapshot`, `graph_context`, `impact_map`, `recommended_read_files`, or `graph_result` in the current `goal step`.
   - Do not create or expect a separate `graph_*` goal step.
6. Do not use lower-level `workflow`, `spec`, or `db` commands unless GOAL_CONTEXT explicitly allows it. For `memory`, only `dhk memory search` and `dhk memory suggest` are allowed (see Troubleshooting Memory); never run `dhk memory confirm` or accept candidates yourself.
7. After every investigation, plan, edit, or verification step, run `scripts/goal-step.sh` with summary, structured fields, changed files, and the required evidence keys from GOAL_CONTEXT.
   - If Agent Brief provides `script`, `args`, and `cwd`, execute that project-local script from `cwd`; do not depend on a global `dhk` binary.
   - If files changed but `step_count` did not increase, the action is not recorded. Re-read the required evidence and record the missing `goal step` before replying.
   - If files changed before a current goal existed, create a quickstart patch goal for that request, then record the already-made edit with `goal-step.sh --auto` and the required fields.
   - If GOAL_CONTEXT task does not match the latest user request, do not continue on that goal. Create a new quickstart patch goal with the latest request and record the edit there.
   - Prefer structured evidence fields and `dhk goal step --field key=value`
     over long hand-written evidence strings when using the CLI directly.
   - Use `dhk goal step --template --goal <goal>` to inspect the current
     action's required evidence before recording a step.
   - Use `dhk goal step --dry-run ...` to validate evidence without writing a
     `goal_step` row when evidence shape is uncertain.
   - Before implementation, include `goal_understanding=<summary>` and either `assumptions=<summary>` or `pending=<questions-or-none>` in step evidence. The `think-before-coding` gate fails when this goal-understanding evidence is missing.
   - Keep each `goal step` aligned to the current GOAL_CONTEXT action. The `goal-driven` gate checks recorded action order and evidence.
   - For broad or protected-file changes, include `scope_justification=<why-needed>`, `impact_evidence=<artifact-or-summary>`, and `protected_file_confirmation=approved|confirmed|passed` when applicable. The `simplicity` and `surgical-change` gates flag unsupported scope drift.
8. During development, `dhk goal verify --level fast` may be used as a quick
   preflight, but it is not completion proof.
9. Before claiming completion, run `scripts/goal-verify.sh`; if it is not ready, follow its `next_command`.
   - If verification reports `think-before-coding`, `goal-driven`, `simplicity`, or `surgical-change` as failed or pending, review the failure details and address them before retrying. Add missing evidence or reduce the change scope, then rerun `goal verify`.
10. Only run `scripts/goal-complete.sh` when standard `goal verify` returns
    `ready_to_complete`. For release review, prefer
    `dhk goal verify --level release` before completing or tagging. Use
    `scripts/goal-check.sh --all` plus `scripts/goal-evaluate.sh` only when
    debugging readiness.
11. After a goal is completed, use `dhk goal audit --goal <goal>` for read-only
    review and `dhk goal recheck --goal <goal>` to refresh checks without
    reopening the goal.
12. When the user asks for retrospective, review-summary, MR/PR summary, or
   复盘 materials, use `scripts/goal-retrospective.sh --goal <goal>` after
   the goal has enough recorded evidence. Prefer `--write <path>` when the
   user needs a file they can share.

Use the wrapper scripts instead of composing raw `dhk goal ...` commands; the wrappers pass the repository root even when invoked from a subdirectory. In this repo they live under `.agents/skills/devharness-goal-development/scripts/`.

## Build And Verify Loop Guard

Never retry a failing or hanging build/test in a loop. This is the most common
way a bare agent gets stuck on a project that cannot compile locally.

- Run compile/test only as the verification policy allows. When the policy is
  manual (`auto_maven_test: disabled`), do not run `mvn` / build commands at all;
  record manual or risk evidence instead.
- If a build fails or cannot run for environmental reasons (missing
  dependencies, no local toolchain, sandbox limits, legacy project), record it
  once as evidence: `compile_result=blocked: <reason>`, `test_result=blocked: <reason>`,
  `risk_if_not_run=<impact>`, plus a rollback plan when required. Do not run the
  same failing command again expecting a different result.
- Then either complete with valid manual evidence (IDE/CI/human verification with
  scope and an evidence file), or stop and report the blocker to the user.
  `goal verify` returns a finite blocker list to act on — act on it, do not loop.

## Forbidden By Default

- Do not manually pass or waive workflow gates.
- Do not archive specs.
- Do not confirm memory.
- Do not run DB SQL.
- Do not bypass `goal` by calling lower-level workflow/spec/db commands unless GOAL_CONTEXT explicitly permits that action.
- Do not paste Agent Brief `harness_commands` into user-facing replies unless the user asks for debug-level command details.
- Do not write secrets, tokens, passwords, JDBC URLs, Authorization headers, cookies, or raw SQL results to memory.
- Do not claim completion while goal evaluation is not ready.

## Required Final Self-Check

End each response with:

```text
DevHarness self-check:
- goal:
- current_action:
- goal_task_matches_user_request:
- changed_files:
- checks:
- completion:
```

## References

- `references/goal-protocol.md`
- `references/evidence-format.md`
- `references/knowledge-injection.md`
- `references/self-check-format.md`
- `references/forbidden-actions.md`
- `references/graph-protocol.md`
- `references/graph-evidence-format.md`
- `references/graph-forbidden-actions.md`
