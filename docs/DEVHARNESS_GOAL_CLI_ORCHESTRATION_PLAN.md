# DevHarnessKit CLI Goal 编排改造方案

> 建议落盘路径：`docs/DEVHARNESS_GOAL_CLI_ORCHESTRATION_PLAN.md`  
> 目标版本：V0.4 Goal Orchestration  
> 目标：让用户和 Agent 不再手动组合 `memory` / `workflow` / `spec` / `db` 命令，而是通过 `dhk goal ...` 完成完整 AI 开发闭环。

---

## 1. 背景判断

DevHarnessKit 当前已经具备多个底层能力：

- `memory`：项目事实、确认机制、上下文导出、checkpoint、recover。
- `db`：只读 SQL 检查、dry-run、安全限制、SQL_RESULT 输出。
- `workflow`：template、run、phase、gate、artifact、summary。
- `spec`：change、document、task、acceptance、status、export、archive。
- `skills/rules`：Agent 启动规则和流程提示。

这些能力目前仍然更像“零件”。用户或模型必须知道：什么时候 `memory export`，什么时候 `workflow start`，什么时候 `spec create`，什么时候 `spec bind-workflow`，什么时候 `workflow gate pass`，什么时候 `memory checkpoint`。

这会导致三个问题：

```text
1. 普通用户不会主动构建复杂命令链。
2. 弱模型容易跳步、漏步、误用底层命令。
3. 当前工具无法自然形成“目标 -> 执行 -> 检查 -> 完成 -> 复盘”的闭环。
```

因此，后续核心不是继续增加零散 CLI 子命令，而是新增一个高层编排入口：

```text
dhk goal ...
```

`goal` 是主控层；`memory`、`workflow`、`spec`、`db` 是能力层。

---

## 2. 总体目标

### 2.1 一句话目标

```text
用户只启动 goal；DevHarnessKit 自动串起 memory、workflow、spec、check、checkpoint 和 artifact。
```

### 2.2 面向用户的体验

用户输入：

```bash
dhk goal start \
  --project-root . \
  --profile java-api-change \
  --task "实现订单分页查询接口" \
  --module order \
  --mode api
```

之后用户或 Agent 只需要使用：

```bash
dhk goal next --goal <goal-key>
dhk goal step --goal <goal-key> --summary "..." --changed-files "..."
dhk goal check --goal <goal-key> --all
dhk goal evaluate --goal <goal-key>
dhk goal complete --goal <goal-key>
```

不再要求用户理解底层命令组合。

---

## 3. 设计原则

### 3.1 Goal 是编排层，不是替代层

保留现有底层命令：

```text
dhk memory ...
dhk workflow ...
dhk spec ...
dhk db ...
```

但在 Skills 和日常开发中，默认只使用：

```text
dhk goal ...
```

底层命令用于：

```text
1. 调试。
2. 高级用户手动操作。
3. CI 或内部工具特定集成。
4. goal 内部服务复用。
```

### 3.2 不通过 shell 拼命令

`goal` 不应该通过 shell 调用：

```bash
dhk workflow start
```

也不应该解析 stdout。

正确方式：

```text
GoalCommand
  -> GoalOrchestrator
    -> ProjectService
    -> MigrationRunner
    -> WorkflowSeedService
    -> WorkflowRunService / Repository
    -> SpecService / Repository
    -> ExportSelectionService
    -> WorkflowExportService
    -> SpecExportService
    -> CheckpointRepository
    -> ArtifactService
    -> SensitiveDataGuard
```

也就是说，`goal` 复用 service/repository，而不是复用 CLI 文本输出。

### 3.3 完成条件必须证据化

不能由模型说“完成了”就完成。

`goal evaluate` 应综合：

```text
1. workflow 是否存在 blocking hard gates。
2. spec task 是否全部 done/skipped。
3. spec acceptance 是否全部 passed/waived。
4. required checks 是否全部 passed。
5. sensitive guard 是否通过。
6. checkpoint 是否已创建。
7. 是否存在 waiting_user / approval_required 状态。
```

### 3.4 弱模型优先

所有 `goal` 输出都应当适合弱模型读取：

```text
短句
明确动作
固定字段
明确 allowed / forbidden
明确下一条命令
明确缺失项
```

---

## 4. 新增命令设计

## 4.1 `dhk goal start`

### 功能

启动目标，并自动准备 memory、workflow、spec 和上下文。

### 命令

```bash
dhk goal start \
  --project-root . \
  --profile java-api-change \
  --task "实现订单分页查询接口" \
  --module order \
  --mode api \
  --condition "接口可编译、测试通过、验收项完成、无敏感信息导出"
```

### 内部流程

```text
1. 解析 project-root。
2. 创建 `.agents/memory` 目录。
3. 如果未初始化 project.json，则执行 ProjectService.ensureProject。
4. 执行 MigrationRunner，升级到 schema v5。
5. 根据 profile 选择 workflow template。
6. 如果 workflow template 未 seed，自动 seed。
7. 创建 workflow run。
8. 如果 profile 要求 spec，则创建 spec change scaffold。
9. 绑定 spec change 和 workflow run。
10. 创建 goal_run。
11. 初始化 goal_check。
12. 导出 CURRENT_CONTEXT.md。
13. 导出 WORKFLOW_CONTEXT.md。
14. 导出 SPEC_CONTEXT.md。
15. 生成 GOAL_CONTEXT.md。
16. 记录 goal_event 和 goal_artifact。
17. 输出 goal_key、run_key、change_key、next_action。
```

### 输出示例

```text
goal_key: 20260525-order-api-goal
profile: java-api-change
workflow_run: 20260525-api-change-order
spec_change: order-query-api
status: context_ready
current_action: inspect_existing_code
next_command: dhk goal next --goal 20260525-order-api-goal
context_path: .agents/memory/exports/GOAL_CONTEXT.md
```

---

## 4.2 `dhk goal resume`

### 功能

恢复最近一个未完成 goal。

### 命令

```bash
dhk goal resume --project-root .
```

或指定：

```bash
dhk goal resume --project-root . --goal <goal-key>
```

### 输出

```text
goal_key: 20260525-order-api-goal
status: implementing
current_action: implement_minimal_change
next_command: dhk goal next --goal 20260525-order-api-goal
```

---

## 4.3 `dhk goal next`

### 功能

计算当前目标的下一步动作。

### 命令

```bash
dhk goal next --project-root . --goal <goal-key>
```

### 输出示例

```text
goal_key: 20260525-order-api-goal
status: running
current_phase: inspect_existing_code
current_action: inspect_existing_code
next_action_type: agent_action
instruction: Read existing Controller, Service, Mapper, DTO, and tests before editing.
required_evidence:
  - existing_controller
  - existing_service
  - existing_mapper
  - existing_tests
forbidden_actions:
  - edit_code_before_inspection
  - archive_spec
  - waive_gate
context_path: .agents/memory/exports/GOAL_CONTEXT.md
```

### 关键要求

`goal next` 不是简单 status，而是 planner 输出。

---

## 4.4 `dhk goal step`

### 功能

记录一轮 Agent 或人工工作。

### 命令

```bash
dhk goal step \
  --project-root . \
  --goal <goal-key> \
  --summary "已阅读 OrderController、OrderService、OrderMapper 和分页测试" \
  --changed-files "" \
  --evidence "现有分页使用 PageRequest + PageResult"
```

### 内部流程

```text
1. 检查 goal 是否 running。
2. 检查 summary/evidence 是否含敏感信息。
3. 写入 goal_step。
4. 根据当前 action 评估 required_evidence 是否满足。
5. 必要时推进 workflow phase 或提示缺失项。
6. 重新生成 GOAL_CONTEXT.md。
```

### 输出示例

```text
step_id: 3
goal_key: 20260525-order-api-goal
current_action: inspect_existing_code
action_status: evidence_recorded
missing_evidence: none
next_action: create_change_plan
```

---

## 4.5 `dhk goal check`

### 功能

执行或记录确定性检查。

### 命令

```bash
dhk goal check --project-root . --goal <goal-key> --check compile
```

```bash
dhk goal check --project-root . --goal <goal-key> --check test
```

```bash
dhk goal check --project-root . --goal <goal-key> --check sensitive
```

```bash
dhk goal check --project-root . --goal <goal-key> --all
```

### 支持的 check 类型

```text
compile         编译检查
test            测试检查
sensitive       敏感数据检查
spec            spec task / acceptance 状态检查
workflow        workflow hard gate 检查
sql-dry-run     SQL 安全 dry-run 检查
custom-command  profile 中定义的命令检查
```

### 示例输出

```text
check_key: compile
status: passed
command: mvn -q -DskipTests compile
result_summary: BUILD SUCCESS
evidence_path: .agents/memory/artifacts/goals/<goal-key>/checks/compile.log
```

### 安全限制

默认不能自动执行：

```text
1. DB 实际查询。
2. 写权限命令。
3. git push。
4. deploy。
5. archive spec。
6. waive gate。
```

这些操作必须由 policy 或用户显式允许。

---

## 4.6 `dhk goal evaluate`

### 功能

判断目标是否可以完成。

### 命令

```bash
dhk goal evaluate --project-root . --goal <goal-key>
```

### 输出示例：未完成

```text
decision: not_ready
missing:
  - check compile is pending
  - acceptance A001 is pending
  - checkpoint is missing
next_action: run_goal_check_compile
next_command: dhk goal check --goal <goal-key> --check compile
```

### 输出示例：可完成

```text
decision: ready_to_complete
missing: none
next_action: complete_goal
next_command: dhk goal complete --goal <goal-key>
```

---

## 4.7 `dhk goal complete`

### 功能

完成目标，生成 checkpoint、summary、artifact 和 memory suggestion。

### 命令

```bash
dhk goal complete --project-root . --goal <goal-key>
```

### 内部流程

```text
1. 执行 GoalCompletionEvaluator。
2. 如果 not_ready，拒绝完成。
3. 创建 memory checkpoint。
4. 生成 GOAL_SUMMARY.md。
5. 生成 RECOVERY_CONTEXT.md。
6. 记录 workflow summary artifact。
7. 生成 reusable memory suggestions，默认 draft，不自动 confirmed。
8. 标记 goal_run completed。
9. 输出 checkpoint_id 和 summary_path。
```

### 输出示例

```text
goal_key: 20260525-order-api-goal
status: completed
checkpoint_id: 8
summary_path: .agents/memory/exports/GOAL_SUMMARY.md
recovery_context_path: .agents/memory/exports/RECOVERY_CONTEXT.md
memory_suggestions: 2
```

---

## 4.8 `dhk goal status`

### 功能

面向人类用户查看当前 goal 状态。

```bash
dhk goal status --project-root . --goal <goal-key>
```

输出：

```text
goal_key: ...
profile: java-api-change
status: implementing
workflow_run: ...
spec_change: ...
current_action: implement_minimal_change
step_count: 4
checks:
  compile: passed
  test: pending
  sensitive: passed
missing:
  - tests pending
  - acceptance A001 pending
```

---

## 4.9 `dhk goal export`

### 功能

重新生成 GOAL_CONTEXT.md。

```bash
dhk goal export --project-root . --goal <goal-key>
```

用于 context stale 或 Agent 新窗口恢复。

---

## 4.10 `dhk goal abandon`

### 功能

中止目标。

```bash
dhk goal abandon --project-root . --goal <goal-key> --reason "需求取消"
```

要求 reason，不允许无理由中止。

---

## 5. Goal 状态机

### 5.1 主状态

```text
created
initialized
context_ready
planning
implementing
verifying
ready_to_complete
completed
```

### 5.2 异常状态

```text
blocked
waiting_user
failed
abandoned
```

### 5.3 状态流

```text
created
  -> initialized
  -> context_ready
  -> planning
  -> implementing
  -> verifying
  -> ready_to_complete
  -> completed
```

### 5.4 状态解释

| 状态 | 含义 |
| --- | --- |
| created | goal 已创建，但 workflow/spec/context 未完全准备 |
| initialized | project、migration、profile 已准备 |
| context_ready | CURRENT_CONTEXT / WORKFLOW_CONTEXT / SPEC_CONTEXT / GOAL_CONTEXT 已导出 |
| planning | 正在读取现有代码、形成变更计划 |
| implementing | 正在执行最小代码变更 |
| verifying | 正在执行 compile/test/spec/sensitive 等检查 |
| ready_to_complete | 所有完成条件满足，可 complete |
| completed | checkpoint、summary、artifact 已生成 |
| blocked | hard gate 或 policy 阻塞 |
| waiting_user | 需要人工批准或输入 |
| failed | 目标失败且不可自动恢复 |
| abandoned | 目标被人为中止 |

---

## 6. 新增数据表：schema v5

## 6.1 `goal_run`

```sql
CREATE TABLE IF NOT EXISTS goal_run (
  goal_key TEXT PRIMARY KEY,
  project_key TEXT NOT NULL,
  workflow_run_key TEXT NOT NULL DEFAULT '',
  spec_change_key TEXT NOT NULL DEFAULT '',
  profile_key TEXT NOT NULL,
  task_name TEXT NOT NULL,
  module_name TEXT NOT NULL DEFAULT 'global',
  mode TEXT NOT NULL DEFAULT 'auto',
  condition_text TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'created',
  current_action TEXT NOT NULL DEFAULT '',
  max_steps INTEGER NOT NULL DEFAULT 30,
  step_count INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  completed_at TEXT NOT NULL DEFAULT '',
  CHECK (status IN (
    'created', 'initialized', 'context_ready', 'planning', 'implementing',
    'verifying', 'ready_to_complete', 'completed', 'blocked',
    'waiting_user', 'failed', 'abandoned'
  )),
  FOREIGN KEY (project_key) REFERENCES project(project_key)
);
```

## 6.2 `goal_step`

```sql
CREATE TABLE IF NOT EXISTS goal_step (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  goal_key TEXT NOT NULL,
  step_index INTEGER NOT NULL,
  action_key TEXT NOT NULL,
  summary TEXT NOT NULL,
  changed_files TEXT NOT NULL DEFAULT '',
  evidence TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'recorded',
  created_at TEXT NOT NULL,
  CHECK (status IN ('recorded', 'accepted', 'incomplete', 'rejected')),
  FOREIGN KEY (goal_key) REFERENCES goal_run(goal_key)
);
```

## 6.3 `goal_check`

```sql
CREATE TABLE IF NOT EXISTS goal_check (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  goal_key TEXT NOT NULL,
  check_key TEXT NOT NULL,
  check_type TEXT NOT NULL,
  required INTEGER NOT NULL DEFAULT 1,
  command TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'pending',
  result_summary TEXT NOT NULL DEFAULT '',
  evidence_path TEXT NOT NULL DEFAULT '',
  checked_at TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  UNIQUE(goal_key, check_key),
  CHECK (status IN ('pending', 'running', 'passed', 'failed', 'waived', 'skipped')),
  FOREIGN KEY (goal_key) REFERENCES goal_run(goal_key)
);
```

## 6.4 `goal_event`

```sql
CREATE TABLE IF NOT EXISTS goal_event (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  goal_key TEXT NOT NULL,
  event_type TEXT NOT NULL,
  level TEXT NOT NULL DEFAULT 'info',
  message TEXT NOT NULL,
  data TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL,
  CHECK (level IN ('debug', 'info', 'warn', 'error')),
  FOREIGN KEY (goal_key) REFERENCES goal_run(goal_key)
);
```

## 6.5 `goal_artifact`

```sql
CREATE TABLE IF NOT EXISTS goal_artifact (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  goal_key TEXT NOT NULL,
  artifact_type TEXT NOT NULL,
  title TEXT NOT NULL,
  file_path TEXT NOT NULL DEFAULT '',
  content_hash TEXT NOT NULL DEFAULT '',
  summary TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL,
  FOREIGN KEY (goal_key) REFERENCES goal_run(goal_key)
);
```

## 6.6 索引

```sql
CREATE INDEX IF NOT EXISTS idx_goal_run_project_status
  ON goal_run(project_key, status);

CREATE INDEX IF NOT EXISTS idx_goal_step_goal_index
  ON goal_step(goal_key, step_index);

CREATE INDEX IF NOT EXISTS idx_goal_check_goal_status
  ON goal_check(goal_key, status);

CREATE INDEX IF NOT EXISTS idx_goal_event_goal_created
  ON goal_event(goal_key, created_at);
```

---

## 7. Goal Profile

## 7.1 目标

Profile 定义不同任务类型的流程：

```text
java-api-change
java-mvc-change
bugfix
sql-review
code-review
refactor
```

Profile 不应全部写死在 Java 代码中，应支持默认内置 + 项目覆盖。

### 路径

```text
.agents/devharness/profiles/
  java-api-change.json
  bugfix.json
  sql-review.json
```

## 7.2 示例：`java-api-change.json`

```json
{
  "profile_key": "java-api-change",
  "workflow": "api-change",
  "spec_required": true,
  "default_mode": "api",
  "context": {
    "include_memory": true,
    "include_workflow": true,
    "include_spec": true
  },
  "actions": [
    {
      "action_key": "export_context",
      "type": "auto",
      "description": "Export memory, workflow, spec, and goal context."
    },
    {
      "action_key": "inspect_existing_code",
      "type": "agent",
      "description": "Read existing Controller, Service, Mapper, DTO, and tests before editing.",
      "required_evidence": [
        "existing_controller",
        "existing_service",
        "existing_mapper",
        "existing_tests"
      ]
    },
    {
      "action_key": "create_change_plan",
      "type": "agent",
      "description": "Create a minimal impacted-file plan.",
      "required_evidence": [
        "impacted_files",
        "risk_points"
      ]
    },
    {
      "action_key": "implement_minimal_change",
      "type": "agent",
      "description": "Modify only the files listed in the plan."
    },
    {
      "action_key": "verify",
      "type": "check",
      "checks": ["compile", "test", "sensitive", "spec"]
    },
    {
      "action_key": "checkpoint",
      "type": "auto",
      "description": "Create a memory checkpoint and recovery context."
    }
  ],
  "completion": {
    "require_no_blocking_gates": true,
    "require_spec_tasks_closed": true,
    "require_acceptance_closed": true,
    "require_required_checks_passed": true,
    "require_checkpoint": true
  }
}
```

---

## 8. GOAL_CONTEXT.md 设计

## 8.1 目标

`GOAL_CONTEXT.md` 是弱模型执行主上下文。它必须告诉模型：

```text
1. 当前目标是什么。
2. 当前只允许做什么。
3. 禁止做什么。
4. 需要读哪些文件。
5. 需要提供哪些 evidence。
6. 下一条应该执行什么命令。
7. 完成条件是什么。
```

## 8.2 推荐格式

```md
# GOAL_CONTEXT

<goal>
- goal_key: 20260525-order-api-goal
- profile: java-api-change
- task: 实现订单分页查询接口
- module: order
- mode: api
- status: implementing
</goal>

<current-action>
implement_minimal_change
</current-action>

<next-instruction>
Modify only the minimal files needed for the order query API.
Do not change authentication, deployment, or global response wrapper.
</next-instruction>

<allowed-actions>
- edit_files_listed_in_plan
- run_compile_check
- record_goal_step
</allowed-actions>

<forbidden-actions>
- do_not_archive_spec
- do_not_waive_gates
- do_not_confirm_memory
- do_not_run_db_sql_without_user_request
- do_not_modify_protected_files
</forbidden-actions>

<required-evidence>
- changed_files
- compile_result
- test_result
- acceptance_status
</required-evidence>

<context-files>
- .agents/memory/exports/CURRENT_CONTEXT.md
- .agents/memory/exports/WORKFLOW_CONTEXT.md
- .agents/memory/exports/SPEC_CONTEXT.md
</context-files>

<completion-condition>
- workflow has no blocking hard gates
- all required checks passed
- all spec tasks done or skipped
- all acceptance passed or waived
- checkpoint created
</completion-condition>

<next-command>
dhk goal step --goal 20260525-order-api-goal --summary "<summary>" --changed-files "<files>" --evidence "<evidence>"
</next-command>
```

## 8.3 输出预算

`GOAL_CONTEXT.md` 应保持短小：

```text
目标大小：8KB 以下
最大大小：16KB
```

长内容保留在：

```text
CURRENT_CONTEXT.md
WORKFLOW_CONTEXT.md
SPEC_CONTEXT.md
RECOVERY_CONTEXT.md
```

---

## 9. 服务结构建议

新增 package：

```text
src/main/java/com/devharnesskit/dhk/command/goal/
src/main/java/com/devharnesskit/dhk/model/goal/
src/main/java/com/devharnesskit/dhk/repository/goal/
src/main/java/com/devharnesskit/dhk/service/goal/
src/main/java/com/devharnesskit/dhk/export/GoalContextRenderer.java
```

### 9.1 Command

```text
GoalCommand.java
GoalStartCommand.java
GoalResumeCommand.java
GoalNextCommand.java
GoalStepCommand.java
GoalCheckCommand.java
GoalEvaluateCommand.java
GoalCompleteCommand.java
GoalStatusCommand.java
GoalExportCommand.java
GoalAbandonCommand.java
```

### 9.2 Model

```text
GoalRun.java
GoalStep.java
GoalCheck.java
GoalEvent.java
GoalArtifact.java
GoalPlan.java
GoalEvaluation.java
GoalProfile.java
```

### 9.3 Repository

```text
GoalRunRepository.java
GoalStepRepository.java
GoalCheckRepository.java
GoalEventRepository.java
GoalArtifactRepository.java
```

### 9.4 Service

```text
GoalOrchestrator.java
GoalPlanner.java
GoalProfileService.java
GoalContextService.java
GoalCheckService.java
GoalCompletionEvaluator.java
GoalArtifactService.java
GoalKeyGenerator.java
```

---

## 10. GoalPlanner 设计

输入：

```text
goal_run
workflow_run
workflow_phase_run
workflow_gate_run
spec_change
spec_task
spec_acceptance
goal_check
goal_step
profile
```

输出：

```text
GoalPlan
  - status
  - currentAction
  - nextActionType
  - instruction
  - requiredEvidence
  - missingEvidence
  - blockingReasons
  - allowedActions
  - forbiddenActions
  - contextFiles
  - nextCommand
```

伪逻辑：

```java
if project not initialized:
    return auto("memory_init");

if workflow template missing:
    return auto("workflow_template_seed");

if workflow run missing:
    return auto("workflow_start");

if profile.specRequired() && spec missing:
    return auto("spec_create");

if spec exists && not bound to workflow:
    return auto("spec_bind_workflow");

if context stale:
    return auto("export_context");

if blocking hard gates exist:
    return blocked("resolve_blocking_gates");

if current profile action requires evidence and evidence missing:
    return agentAction(currentAction);

if required checks pending:
    return checkAction(nextPendingCheck);

if completion conditions satisfied:
    return readyToComplete();

return agentAction(nextAction);
```

---

## 11. GoalCheckService 设计

### 11.1 内置检查

| check | 行为 |
| --- | --- |
| compile | 执行 profile 中定义的 compile command，默认 `mvn -q -DskipTests compile` |
| test | 执行 profile 中定义的 test command，默认 `mvn -q test` |
| sensitive | 对 CURRENT_CONTEXT、GOAL_CONTEXT、SPEC_CONTEXT、WORKFLOW_CONTEXT 做 sensitive scan |
| spec | 检查 spec tasks 和 acceptance 状态 |
| workflow | 检查 hard gates |
| sql-dry-run | 只做 SQL safety dry-run，不默认连接数据库 |

### 11.2 Evidence

每次 check 保存：

```text
.agents/memory/artifacts/goals/<goal-key>/checks/<check-key>.log
```

`goal_check.evidence_path` 记录路径。

---

## 12. GoalCompletionEvaluator 设计

输出：

```text
ready_to_complete
not_ready
blocked
waiting_user
```

检查项：

```text
1. goal_run.status 不在 failed/abandoned。
2. workflow run 不存在 blocking hard gate。
3. workflow 不处于 blocked/failed/abandoned。
4. required goal_check 全部 passed 或允许 skipped/waived。
5. spec task 全部 done/skipped。
6. spec acceptance 全部 passed/waived。
7. sensitive check passed。
8. checkpoint created。
9. 未超过 max_steps。
```

如果未完成，必须输出 missing 列表和 next_command。

---

## 13. 与现有模块的整合

### 13.1 memory

`goal start`：

```text
ensure project
migrate
export current context
```

`goal complete`：

```text
create checkpoint
export recovery context
generate memory suggestions as draft
```

### 13.2 workflow

`goal start`：

```text
seed template
start run
```

`goal step`：

```text
根据 evidence 自动推进 phase，或提示缺失 evidence。
```

注意：第一版不建议自动 pass 所有 gate。可以先只自动 pass 可确定 gate，例如：

```text
current_context_exists
confirmed_memory_only
sensitive_guard_passed
compile_passed
tests_recorded
```

其他 gate 仍需人工或 profile 明确允许。

### 13.3 spec

`goal start`：

```text
create spec scaffold
bind workflow
```

`goal evaluate`：

```text
检查 tasks 和 acceptance 是否关闭。
```

`goal complete`：

```text
可选标记 spec verified，但不默认 archive。
```

### 13.4 db

默认不自动执行真实 DB 查询。

仅在以下情况允许：

```text
1. 用户明确要求。
2. policy 允许。
3. 使用 readonly credential。
4. goal next 显示当前 action 允许 DB verification。
```

---

## 14. 实施路线

### Milestone 1：Goal MVP

范围：

```text
dhk goal start
dhk goal next
dhk goal step
dhk goal evaluate
GOAL_CONTEXT.md
goal_run / goal_step / goal_event
```

不做自动 check，不做 complete。

验收：

```text
用户能通过 goal start 自动创建 workflow/spec/context。
Agent 能通过 goal next 获取下一步。
goal step 能记录证据并刷新 GOAL_CONTEXT。
```

### Milestone 2：Check / Complete

范围：

```text
dhk goal check
dhk goal complete
goal_check / goal_artifact
compile/test/sensitive/spec/workflow checks
checkpoint 自动创建
GOAL_SUMMARY.md
```

验收：

```text
goal evaluate 在 checks/spec 未完成时拒绝 complete。
goal complete 自动生成 checkpoint 和 summary。
```

### Milestone 3：Profile 配置化

范围：

```text
GoalProfileService
内置 java-api-change / bugfix
项目本地 profile override
profile-defined checks
```

验收：

```text
不同 profile 能生成不同 next_action 和 required_evidence。
```

### Milestone 4：Policy / Hooks 预留

范围：

```text
policy.json
forbidden commands
protected files
BeforeGoalComplete hook
BeforeDbSql hook
```

验收：

```text
protected file 修改可以阻止 goal complete。
```

---

## 15. 集成测试计划

### 15.1 `goalStartCreatesWorkflowSpecAndContext`

```text
1. 临时项目目录。
2. 执行 goal start。
3. 验证 project.json、memory.db 存在。
4. 验证 workflow_run 存在。
5. 验证 spec_change 存在。
6. 验证 workflow_spec_binding 存在。
7. 验证 GOAL_CONTEXT.md 存在。
```

### 15.2 `goalNextReturnsCurrentAction`

```text
1. goal start。
2. goal next。
3. 验证 current_action = inspect_existing_code。
4. 验证 forbidden_actions 存在。
```

### 15.3 `goalStepRecordsEvidenceAndAdvances`

```text
1. goal start。
2. goal step 记录 inspect evidence。
3. goal next。
4. 验证 next_action = create_change_plan。
```

### 15.4 `goalEvaluateRejectsIncompleteGoal`

```text
1. goal start。
2. 不执行 check。
3. goal evaluate。
4. 验证 decision = not_ready。
5. 验证 missing 包含 compile/test/spec/checkpoint。
```

### 15.5 `goalCompleteCreatesCheckpoint`

```text
1. goal start。
2. 填充 required evidence。
3. 标记 spec tasks done。
4. 标记 acceptance passed。
5. 执行 checks。
6. goal complete。
7. 验证 checkpoint 存在。
8. 验证 GOAL_SUMMARY.md 存在。
```

---

## 16. 第一版 help 文案

```text
Usage:
  dhk goal start --profile <profile> --task <task> [--module <module>] [--mode <mode>]
  dhk goal resume [--goal <goal-key>]
  dhk goal next --goal <goal-key>
  dhk goal step --goal <goal-key> --summary <summary> [--changed-files <files>] [--evidence <evidence>]
  dhk goal check --goal <goal-key> --check <check-key>
  dhk goal check --goal <goal-key> --all
  dhk goal evaluate --goal <goal-key>
  dhk goal complete --goal <goal-key>
  dhk goal status --goal <goal-key>
  dhk goal export --goal <goal-key>
  dhk goal abandon --goal <goal-key> --reason <reason>
```

---

## 17. 风险与约束

### 17.1 不要让 goal 变成隐藏黑箱

所有自动动作都必须记录：

```text
goal_event
goal_artifact
stdout summary
```

### 17.2 不要默认自动豁免 gate

`waive` 必须人工明确。

### 17.3 不要默认自动 archive spec

`complete goal` 和 `archive spec` 分开。第一版只允许 goal complete 标记 spec verified，不直接 archive。

### 17.4 不要默认执行 DB 查询

DB 查询必须用户明确要求，且需要 readonly policy。

### 17.5 不要让 profile 过度复杂

第一版只支持 JSON 中有限字段，不引入 YAML parser 或脚本 DSL。

---

## 18. 推荐优先级

最高优先级：

```text
1. goal_run / goal_step / goal_event schema。
2. dhk goal start。
3. dhk goal next。
4. GOAL_CONTEXT.md。
5. dhk goal step。
```

第二优先级：

```text
1. dhk goal check。
2. dhk goal evaluate。
3. dhk goal complete。
4. GOAL_SUMMARY.md。
```

第三优先级：

```text
1. profile 配置化。
2. policy/hook 集成。
3. team/routine 预留。
```

---

## 19. 最终效果

改造完成后，DevHarnessKit 的使用方式从：

```text
用户手动组合一堆 CLI 命令
```

变为：

```text
用户启动 goal
Agent 每轮读取 GOAL_CONTEXT
Agent 按 goal next 执行当前动作
goal check/evaluate 判断完成
goal complete 生成 checkpoint 和 summary
```

这会让 DevHarnessKit 从“上下文工具”升级为“AI 开发流程编排工具”。

