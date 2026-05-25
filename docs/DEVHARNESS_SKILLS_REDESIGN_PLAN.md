# DevHarnessKit Skills 重构方案

> 建议落盘路径：`docs/DEVHARNESS_SKILLS_REDESIGN_PLAN.md`  
> 目标版本：V0.4 Skills for Goal-Oriented Harness  
> 目标：让 Skills 不再只是原则性提示，而是成为弱模型也能稳定执行的 Goal 协议入口。

---

## 1. 背景判断

当前 DevHarnessKit 主要强化了 CLI 能力，但 Skills 仍偏“说明文档”模式。现有 Skill 的核心流程是：开发前导出 memory、读取 CURRENT_CONTEXT、只信 confirmed memory、DB 查询需用户明确要求、开发后 checkpoint、新事实写 draft memory。

这些规则方向正确，但对弱模型不够。弱模型常见失败不是不会写代码，而是：

```text
1. 不知道先做什么。
2. 忘记导出上下文。
3. 绕过 workflow/spec。
4. 直接开始改代码。
5. 漏掉 evidence。
6. 自己判断“完成了”。
7. 忘记 checkpoint。
8. 误用 db/sql/memory confirm/gate waive 等底层命令。
```

因此 Skills 需要从：

```text
规则说明
```

升级为：

```text
执行协议
```

CLI 负责确定性能力，`goal` 负责串流程，Skills 负责约束模型如何使用 `goal`。

---

## 2. 新 Skill 设计目标

### 2.1 一句话目标

```text
让弱模型只需要遵守 goal 协议，就能稳定完成一轮 AI 开发任务。
```

### 2.2 具体目标

```text
1. 降低模型自由度。
2. 避免模型手动组合底层 CLI。
3. 强制每轮读取 GOAL_CONTEXT.md。
4. 强制每轮只执行一个 current_action。
5. 强制记录 goal step。
6. 强制通过 goal evaluate 才能声称完成。
7. 强制使用固定 self-check 输出。
8. 将长规范转移到 references，保持 SKILL.md 短小。
```

---

## 3. 设计原则

## 3.1 少原则，多协议

不要写：

```text
请合理使用 memory、workflow、spec。
```

要写：

```text
1. Run goal next.
2. Read GOAL_CONTEXT.md.
3. Perform only current_action.
4. Run goal step.
5. Run goal evaluate before completion.
```

## 3.2 少开放判断，多固定分支

弱模型不应自己判断是否完成。完成判断必须由：

```bash
dhk goal evaluate
```

输出决定。

## 3.3 Skill 默认只允许使用 goal 命令

默认允许：

```text
dhk goal start
dhk goal resume
dhk goal next
dhk goal step
dhk goal check
dhk goal evaluate
dhk goal complete
dhk goal status
dhk goal export
```

默认禁止：

```text
dhk workflow gate waive
dhk workflow phase pass
dhk spec archive
dhk memory confirm
dhk db sql
dhk memory add confirmed facts
```

只有 `GOAL_CONTEXT.md` 显式允许时，才能使用底层命令。

## 3.4 Skill 主体短，长文档放 references

`SKILL.md` 目标：80 行以内。

长内容放在：

```text
references/goal-protocol.md
references/evidence-format.md
references/forbidden-actions.md
references/java-api-profile.md
references/verification-rules.md
```

## 3.5 区分 strict mode 和 expert mode

弱模型使用 strict Skill。

强模型或人类监督场景可以使用 expert Skill。

---

## 4. 新 Skills 目录结构

建议从一个大 Skill 拆成多个可组合 Skill。

```text
.agents/
  skills/
    devharness-goal-development/
      SKILL.md
      scripts/
        goal-start.sh
        goal-resume.sh
        goal-next.sh
        goal-step.sh
        goal-check.sh
        goal-evaluate.sh
        goal-complete.sh
        goal-status.sh
        goal-export.sh
      references/
        goal-protocol.md
        evidence-format.md
        self-check-format.md
        forbidden-actions.md

    devharness-java-api-change/
      SKILL.md
      references/
        java-api-change-profile.md
        controller-service-mapper-pattern.md
        api-acceptance-format.md
        common-api-risks.md

    devharness-java-mvc-change/
      SKILL.md
      references/
        java-mvc-change-profile.md
        view-controller-service-pattern.md
        mvc-acceptance-format.md

    devharness-bugfix/
      SKILL.md
      references/
        bug-localization-flow.md
        minimal-fix-rules.md
        regression-test-rules.md

    devharness-sql-review/
      SKILL.md
      references/
        readonly-db-policy.md
        sql-safety-rules.md
        sql-result-handling.md

    devharness-code-review/
      SKILL.md
      references/
        review-checklist.md
        risk-categories.md
        evidence-format.md
```

---

## 5. 入口 Skill：`devharness-goal-development`

这是最重要的 Skill。所有开发任务默认先走它。

### 5.1 新版 `SKILL.md` 示例

```md
---
name: devharness-goal-development
description: Use DevHarnessKit goal protocol for any coding task. Strict flow for weaker models and controlled AI development.
---

# DevHarness Goal Development

Use this skill for any code change in a repository that contains DevHarnessKit.

## Required protocol

1. Do not start by editing code.
2. Start or resume a goal.
3. Run `scripts/goal-next.sh` before each work step.
4. Read `.agents/memory/exports/GOAL_CONTEXT.md`.
5. Perform only the `current_action` from GOAL_CONTEXT.
6. Do not use lower-level `memory`, `workflow`, `spec`, or `db` commands unless GOAL_CONTEXT explicitly allows it.
7. After any investigation or code edit, run `scripts/goal-step.sh` with summary, changed files, and evidence.
8. Before claiming completion, run `scripts/goal-check.sh --all`.
9. Then run `scripts/goal-evaluate.sh`.
10. Only run `scripts/goal-complete.sh` when evaluation returns `ready_to_complete`.

## Forbidden by default

- Do not manually pass workflow phases or gates.
- Do not waive gates.
- Do not archive specs.
- Do not confirm memory.
- Do not run DB SQL.
- Do not write secrets, tokens, passwords, JDBC URLs, Authorization headers, cookies, or raw SQL results to memory.
- Do not claim completion while goal evaluation is not ready.

## Required final self-check

End each response with:

DevHarness self-check:
- goal:
- current_action:
- changed_files:
- checks:
- completion:

## References

- `references/goal-protocol.md`
- `references/evidence-format.md`
- `references/self-check-format.md`
- `references/forbidden-actions.md`
```

### 5.2 变化点

旧 Skill：

```text
run memory export -> read CURRENT_CONTEXT -> checkpoint
```

新 Skill：

```text
goal start/resume -> goal next -> read GOAL_CONTEXT -> do current_action -> goal step -> goal check/evaluate/complete
```

这能让弱模型稳定使用完整 Harness。

---

## 6. 任务型 Skill 示例：`devharness-java-api-change`

### 6.1 目的

当任务是 Java API 变更时，绑定 `java-api-change` profile。

### 6.2 `SKILL.md` 示例

```md
---
name: devharness-java-api-change
description: Strict goal-driven flow for Java API changes using DevHarnessKit.
---

# DevHarness Java API Change

Use this skill when implementing or modifying a Java API endpoint.

## Required profile

Use profile: `java-api-change`.

Start with:

`scripts/goal-start.sh --profile java-api-change --task "<task>" --module "<module>" --mode api`

## Before editing

1. Run `scripts/goal-next.sh`.
2. Read `.agents/memory/exports/GOAL_CONTEXT.md`.
3. If current_action is `inspect_existing_code`, read existing Controller, Service, Mapper, DTO, and tests.
4. Do not edit code before required evidence is recorded.

## API change rules

- Do not invent endpoint conventions.
- Do not change authentication or gateway behavior unless GOAL_CONTEXT allows it.
- Do not change global response wrappers unless GOAL_CONTEXT allows it.
- Do not change database schema unless GOAL_CONTEXT allows it.
- Prefer minimal changes.
- Preserve existing naming and error handling conventions.

## Required evidence

- existing endpoint pattern
- impacted files
- changed files
- compile result
- test result
- spec acceptance status

## Completion rule

Never claim completion until:

1. `scripts/goal-check.sh --all` passed.
2. `scripts/goal-evaluate.sh` returns `ready_to_complete`.
3. `scripts/goal-complete.sh` succeeds.

## References

- `references/java-api-change-profile.md`
- `references/controller-service-mapper-pattern.md`
- `references/api-acceptance-format.md`
- `references/common-api-risks.md`
```

---

## 7. Bugfix Skill 示例

```md
---
name: devharness-bugfix
description: Strict goal-driven bugfix flow using DevHarnessKit.
---

# DevHarness Bugfix

Use this skill for fixing a defect or failing test.

## Required profile

Use profile: `bugfix`.

## Required flow

1. Start or resume a goal.
2. Run `goal next`.
3. If current_action is `localize_bug`, inspect only relevant code and failing evidence.
4. Record root-cause hypothesis with `goal step`.
5. Make the smallest fix.
6. Run regression checks.
7. Do not broaden scope without GOAL_CONTEXT permission.

## Required evidence

- failing symptom
- root-cause hypothesis
- changed files
- regression check result

## Forbidden

- Do not rewrite unrelated modules.
- Do not suppress failing tests without explicit approval.
- Do not change public API unless GOAL_CONTEXT allows it.
```

---

## 8. SQL Review Skill 示例

```md
---
name: devharness-sql-review
description: Controlled readonly SQL review flow using DevHarnessKit.
---

# DevHarness SQL Review

Use this skill only when the user explicitly asks to verify business SQL.

## Required flow

1. Run goal next.
2. Confirm GOAL_CONTEXT allows DB verification.
3. Run SQL dry-run first.
4. Never pass DB password as a normal CLI argument.
5. Use readonly credentials only.
6. Do not write raw SQL results to memory.
7. Summarize business conclusions separately from raw results.

## Forbidden

- Do not run DB SQL without explicit user request.
- Do not connect to production unless policy allows it.
- Do not run write SQL.
- Do not store JDBC URLs, passwords, or raw result sets in memory.
```

---

## 9. 脚本封装设计

所有脚本都应该自动识别项目根目录，并显式传 `--project-root`。

### 9.1 `goal-start.sh`

```sh
#!/usr/bin/env sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../../../.." && pwd)
exec "$SCRIPT_DIR/dhk.sh" goal start --project-root "$PROJECT_ROOT" "$@"
```

### 9.2 `goal-next.sh`

```sh
#!/usr/bin/env sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../../../.." && pwd)
exec "$SCRIPT_DIR/dhk.sh" goal next --project-root "$PROJECT_ROOT" "$@"
```

### 9.3 `goal-step.sh`

```sh
#!/usr/bin/env sh
set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../../../.." && pwd)
exec "$SCRIPT_DIR/dhk.sh" goal step --project-root "$PROJECT_ROOT" "$@"
```

### 9.4 同理新增

```text
goal-check.sh
goal-evaluate.sh
goal-complete.sh
goal-status.sh
goal-export.sh
goal-resume.sh
```

Windows `.bat` 也需要对应版本。

---

## 10. 弱模型执行协议

## 10.1 每轮开始

```text
1. Run goal next.
2. Read GOAL_CONTEXT.md.
3. Extract:
   - current_action
   - allowed_actions
   - forbidden_actions
   - required_evidence
   - next_command
4. Perform only current_action.
```

## 10.2 每轮结束

```text
1. Record what was done with goal step.
2. Include changed_files if any.
3. Include evidence, not vague statements.
4. Run goal next again if continuing.
```

## 10.3 完成前

```text
1. Run goal check --all.
2. Run goal evaluate.
3. If not ready, continue from next_action.
4. If ready, run goal complete.
5. Only then claim completion.
```

---

## 11. 固定 self-check 输出

所有使用 DevHarness Skill 的回复都应以固定格式结束。

### 11.1 格式

```text
DevHarness self-check:
- goal: <goal-key or not-started>
- current_action: <action>
- changed_files: <files or none>
- checks: <passed/pending/failed>
- completion: <not-ready/ready/completed>
```

### 11.2 示例

```text
DevHarness self-check:
- goal: 20260525-order-api-goal
- current_action: implement_minimal_change
- changed_files: OrderController.java, OrderService.java, OrderMapper.xml
- checks: compile passed, test pending
- completion: not-ready
```

这能显著降低模型误报完成的概率。

---

## 12. Strict Mode 与 Expert Mode

## 12.1 Strict Mode

面向弱模型、小模型、低成本模型。

约束：

```text
1. 每轮只能执行一个 current_action。
2. 必须 goal next。
3. 必须读 GOAL_CONTEXT。
4. 必须 goal step。
5. 不允许直接 pass gate。
6. 不允许直接 archive spec。
7. 不允许直接 confirm memory。
8. 不允许自由执行 DB SQL。
9. 必须用 self-check 结尾。
```

## 12.2 Expert Mode

面向强模型、人类监督、架构设计任务。

允许：

```text
1. 阅读更多 references。
2. 提出 profile 修改建议。
3. 在 GOAL_CONTEXT 允许时使用底层 workflow/spec 命令。
4. 做多方案分析。
```

但仍要求：

```text
goal evaluate -> goal complete
```

---

## 13. Policy / Hook 配合

Skill 是软约束，不能完全依赖模型遵守。应配套 policy/hook。

建议生成：

```text
.agents/devharness/policy.json
.agents/devharness/hooks.json
```

### 13.1 policy 示例

```json
{
  "model_mode": "strict",
  "allowed_dhk_commands": [
    "goal start",
    "goal resume",
    "goal next",
    "goal step",
    "goal check",
    "goal evaluate",
    "goal complete",
    "goal status",
    "goal export"
  ],
  "forbidden_direct_commands": [
    "workflow gate waive",
    "workflow phase pass",
    "spec archive",
    "memory confirm",
    "db sql"
  ],
  "protected_files": [
    ".env",
    "application-prod.yml",
    "deploy/**",
    "pom.xml"
  ]
}
```

### 13.2 hook 示例

```json
{
  "BeforeGoalComplete": [
    {
      "name": "required-evaluate",
      "type": "goal_evaluate",
      "blocking": true
    },
    {
      "name": "sensitive-scan",
      "type": "sensitive_guard",
      "blocking": true
    }
  ],
  "BeforeDbSql": [
    {
      "name": "explicit-user-request-required",
      "type": "policy",
      "blocking": true
    }
  ]
}
```

---

## 14. 项目专属 Skill 生成

建议新增命令：

```bash
dhk skill generate --project-root . --profile java-api-change
```

生成项目专属 Skill：

```text
.agents/skills/devharness-java-api-change/
  SKILL.md
  scripts/*
  references/project-runbook.md
  references/verification.md
  references/module-map.md
```

### 14.1 生成信息来源

```text
pom.xml
README.md
已有 memory
workflow templates
spec templates
项目目录结构
测试命令
模块命名
```

### 14.2 价值

弱模型不需要每次重新推断：

```text
项目怎么编译
项目怎么测试
Controller 在哪里
Mapper 在哪里
DTO 怎么命名
API 响应格式是什么
```

这些都可以在项目专属 references 中固定。

---

## 15. References 设计

### 15.1 `goal-protocol.md`

内容：

```text
1. goal start/resume/next/step/check/evaluate/complete 的使用时机。
2. 每轮必须遵守的步骤。
3. 不允许绕过 goal。
4. 失败时如何恢复。
```

### 15.2 `evidence-format.md`

内容：

```text
changed_files 如何写
evidence 如何写
compile/test 结果如何写
不允许 vague evidence
```

### 15.3 `forbidden-actions.md`

内容：

```text
禁止直接 waive gate
禁止直接 archive spec
禁止直接 confirm memory
禁止写 secrets
禁止连接生产库
禁止修改 protected files
```

### 15.4 `self-check-format.md`

内容：

```text
固定 self-check 模板
示例
错误示例
```

---

## 16. Skills 与 GOAL_CONTEXT 的职责分工

| 文件 | 职责 |
| --- | --- |
| SKILL.md | 永久协议：必须通过 goal、禁止绕过、输出格式 |
| GOAL_CONTEXT.md | 当前任务状态：当前 action、allowed、forbidden、next command |
| CURRENT_CONTEXT.md | 项目事实：confirmed memory 和近期 checkpoint |
| WORKFLOW_CONTEXT.md | 流程状态：phase/gate/event |
| SPEC_CONTEXT.md | 需求状态：document/task/acceptance |
| references/*.md | 长规则、示例、任务类型说明 |

关键原则：

```text
SKILL.md 不负责描述当前任务状态。
GOAL_CONTEXT.md 不负责解释长期规则。
```

---

## 17. 对弱模型的具体增益机制

### 17.1 降低上下文负载

弱模型不需要读取全部 memory/spec/workflow 细节，只需要优先读取 GOAL_CONTEXT。

### 17.2 降低规划自由度

弱模型不需要自己决定流程，只需要执行 current_action。

### 17.3 降低命令组合难度

弱模型不需要知道底层 CLI 顺序，只用 goal 命令。

### 17.4 降低误报完成

完成必须由 goal evaluate 和 goal complete 控制。

### 17.5 增强反馈闭环

每轮 goal step 都写 evidence，下一轮 goal next 基于 evidence 继续。

---

## 18. 迁移计划

### Step 1：保留旧 Skill，新增 goal Skill

新增：

```text
.agents/skills/devharness-goal-development/
```

旧的：

```text
.agents/skills/devharness-java-development/
```

暂时保留，但 README 标注 legacy。

### Step 2：改写旧 Skill

将旧 Skill 从 memory-first 改成 goal-first。

### Step 3：新增任务型 Skills

优先：

```text
devharness-java-api-change
devharness-bugfix
devharness-sql-review
```

### Step 4：新增 scripts

```text
goal-start.sh / .bat
goal-next.sh / .bat
goal-step.sh / .bat
goal-check.sh / .bat
goal-evaluate.sh / .bat
goal-complete.sh / .bat
```

### Step 5：新增 skill generate

后续再做，不阻塞第一版。

---

## 19. 测试计划

### 19.1 Skill 文件测试

检查：

```text
1. SKILL.md 存在。
2. frontmatter name/description 正确。
3. scripts 存在。
4. references 存在。
5. 没有引用不存在的文件。
```

### 19.2 Script 测试

检查：

```text
1. 从项目根目录执行成功。
2. 从子目录执行仍然传入正确 --project-root。
3. jar 缺失时错误信息明确。
4. exit code 透传。
```

### 19.3 Protocol 测试

通过集成测试模拟：

```text
1. goal start。
2. skill script goal-next。
3. 读取 GOAL_CONTEXT。
4. goal-step。
5. goal-evaluate not_ready。
6. goal-check。
7. goal-complete。
```

### 19.4 弱模型回放测试

使用固定任务集：

```text
1. Java API 新增。
2. Bugfix。
3. SQL review。
4. Test repair。
```

比较：

```text
旧 Skill：memory-first flow
新 Skill：goal-first strict flow
```

指标：

```text
1. 是否先读上下文。
2. 是否跳过流程。
3. 是否误报完成。
4. 是否记录 evidence。
5. 是否创建 checkpoint。
6. 是否触碰 forbidden command。
```

---

## 20. 推荐第一版交付内容

### 必须交付

```text
1. devharness-goal-development/SKILL.md
2. goal-start/next/step/check/evaluate/complete scripts
3. references/goal-protocol.md
4. references/evidence-format.md
5. references/self-check-format.md
6. references/forbidden-actions.md
```

### 可选交付

```text
1. devharness-java-api-change/SKILL.md
2. devharness-bugfix/SKILL.md
3. devharness-sql-review/SKILL.md
```

### 暂缓

```text
1. skill generate
2. team skills
3. routine skills
4. model-specific skill variants
```

---

## 21. 最终效果

改造前：

```text
Skill 告诉模型：你应该使用 memory、不要泄密、最后 checkpoint。
```

改造后：

```text
Skill 告诉模型：你只能通过 goal 协议执行；每轮先 goal next，只做 current_action，做完 goal step，完成前 goal evaluate。
```

这会显著提升弱模型稳定性，因为模型不需要自己理解 DevHarnessKit 的全部能力，只需要遵守一个短协议。

最终目标：

```text
CLI 负责能力。
Goal 负责流程。
Skill 负责让模型按流程工作。
GOAL_CONTEXT 负责告诉模型当前步骤。
Check/Evaluate 负责判断是否完成。
```

这是 DevHarnessKit 从“AI 辅助开发工具”升级为“AI 开发 Harness”的关键调整。

