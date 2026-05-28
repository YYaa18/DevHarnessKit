# DevHarnessKit Human-Facing CLI Recovery & Discoverability 改造方案

建议版本：`V0.8.8 Human-Facing CLI Recovery`

## 1. 背景

本方案来自真实使用 DevHarnessKit 的 dogfooding 反馈。当前 DevHarnessKit 的核心能力已经比较完整：

```text
goal-first
graph-aware
manual verification
Work Brief / Agent Brief
adapter packaging
policy / hook / check / audit
```

但实际使用中的主要不适并不是“功能缺失”，而是：

```text
失败时不知道下一步怎么做。
命令失败信息不自解释。
一些字段只能靠试错发现。
quickstart / goal / brief 等命令在异常路径下没有给出足够明确的恢复路径。
```

这类问题对 CLI 工具尤其致命。因为用户没有 GUI 可以探索，也不应该去读源码才能知道参数名、状态值或下一条命令。

本方案目标：

```text
让 DevHarnessKit 的每个失败路径都能自解释。
任何失败都必须告诉用户：为什么失败、缺什么、有效值是什么、下一步运行什么命令。
```

---

## 2. 总体原则

### 2.1 Actionable Error

所有 validation error、not_ready、blocking、missing evidence、invalid enum 都必须输出：

```text
error_code
message
reason
missing
valid_values
next_command
docs
```

如果没有可执行命令，也必须输出：

```text
next_action
```

示例：

```text
ERROR: blocking interaction requires user answer.

reason:
  当前任务需要用户确认后才能继续。

request:
  interaction-brief-xxx

next_command:
  dhk brief answer --request interaction-brief-xxx --choice "<your choice>"

valid_choices:
  - continue_patch
  - switch_to_strict
  - analyze_only
  - stop
```

JSON 输出：

```json
{
  "ok": false,
  "error_code": "BLOCKING_INTERACTION_REQUIRES_ANSWER",
  "message": "Current goal is blocked by a user interaction.",
  "request_id": "interaction-brief-xxx",
  "next_command": "dhk brief answer --request interaction-brief-xxx --choice \"<choice>\"",
  "valid_choices": ["continue_patch", "switch_to_strict", "analyze_only", "stop"],
  "docs": "docs/WORK_BRIEF.md#interaction-request"
}
```

---

### 2.2 Goal Next 与 Goal Step 的证据契约必须一致

`goal next` 展示的 required evidence 必须是 `goal step` 校验的完整集合。

不允许出现：

```text
goal next 没提 sensitive_result
goal step 却拒绝缺 sensitive_result
```

`goal step` 失败时必须输出：

```text
current_action
missing_items
required_evidence
example_evidence
example_command
```

---

### 2.3 Not Ready 必须输出 blockers

任何 `not_ready` 都不能只给出一句 decision。

必须输出：

```text
decision: not_ready
blockers:
  - key
  - user_message
  - technical_reason
  - next_command
```

`goal complete` 如果未完成，必须：

```text
1. exit code 非 0
2. 不静默保持 verifying
3. 输出与 goal verify 一致的 blocker report
```

---

### 2.4 枚举错误必须显示有效值

所有 enum 类错误必须输出：

```text
invalid value
valid_values
aliases
example
```

覆盖：

```text
spec acceptance status
spec task status
workflow gate status
goal status
check status
verification mode
recommendation mode
graph mode
policy mode
```

---

### 2.5 默认安全，不自动复用旧 goal

`quickstart` 和 `goal start` 默认不应激进复用旧 goal。

自动复用必须满足：

```text
same project_key
same task_hash
same module
same profile
resumable status
not completed
not abandoned
within TTL
```

否则默认创建新 goal。需要复用时必须显式：

```bash
--resume-existing
```

---

## 3. 建议新增模块：Guidance / Actionable Error

### 3.1 新增模型

```java
public final class ActionableError {
    private final String errorCode;
    private final String message;
    private final String reason;
    private final String[] missing;
    private final String[] validValues;
    private final String[] aliases;
    private final String nextCommand;
    private final String docs;
}
```

### 3.2 新增 Renderer

```java
public final class ActionableErrorRenderer {
    public String renderText(ActionableError error);
    public String renderJson(ActionableError error);
}
```

### 3.3 统一出口

建议所有命令层捕获以下异常并渲染为 Actionable Error：

```text
ValidationException
GoalNotReadyException
BlockingInteractionException
InvalidEnumValueException
EvidenceMissingException
QuickstartResumeConflictException
```

---

## 4. 具体改造项

---

# A. 构建摩擦：Jacoco 版本 fallback

## 问题

本地 Maven 缓存没有 `jacoco 0.8.12` 时，每次 `mvn package` 都要手动改 pom 到 `0.8.10`，测试后再恢复。

## 改造

### A1. pom.xml 抽取 property

```xml
<properties>
  <jacoco.version>0.8.12</jacoco.version>
</properties>
```

插件引用：

```xml
<version>${jacoco.version}</version>
```

### A2. 新增脚本

```text
scripts/dev-build.sh
```

支持：

```bash
./scripts/dev-build.sh
DHK_JACOCO_VERSION=0.8.10 ./scripts/dev-build.sh
```

脚本行为：

```text
1. 默认执行 mvn -q package。
2. 如设置 DHK_JACOCO_VERSION，则使用 -Djacoco.version=<value>。
3. 不修改 pom.xml。
4. 构建失败时给出 fallback 提示。
```

### A3. 不建议自动编辑 pom.xml

禁止脚本自动写回 pom，避免污染 git diff。

## 验收

```text
DHK_JACOCO_VERSION=0.8.10 ./scripts/dev-build.sh 可运行。
pom.xml 不产生临时 diff。
git diff --check 通过。
```

---

# B. Goal Evidence Contract 对齐

## 问题

`goal next` 显示：

```text
required_evidence:
- existing_controller
- existing_service
- existing_mapper
- existing_tests
```

但 `goal step` 拒绝：

```text
missing sensitive_result
```

`sensitive_result` 没在 `goal next` 中出现。

## 改造

### B1. 统一 evidence source

引入：

```java
GoalEvidenceContract
```

字段：

```java
String goalKey;
String currentAction;
String[] requiredEvidence;
String[] structuredEvidenceFields;
String exampleEvidence;
String exampleCommand;
```

`GoalPlanner`、`GOAL_CONTEXT`、`goal next`、`goal step` 必须都从同一个 `GoalEvidenceContract` 读取。

### B2. 新增命令

```bash
dhk goal evidence-template --goal <goal-key>
```

输出：

```text
current_action: verify

required_evidence:
- compile_result
- test_result
- sensitive_result

example:
dhk goal step --goal <goal-key> \
  --summary "Verification completed" \
  --evidence "compile_result=passed; test_result=manual_passed; sensitive_result=passed"
```

### B3. goal step 缺 evidence 时输出模板

错误示例：

```text
ERROR: goal step evidence missing required items.

current_action:
  verify

missing:
  - sensitive_result

required_evidence:
  - compile_result
  - test_result
  - sensitive_result

next_command:
  dhk goal evidence-template --goal <goal-key>
```

## 验收

```text
goal next 展示的 required_evidence 与 goal step 校验完全一致。
goal step 缺字段时输出 missing、required_evidence、example。
verify action 不再靠试错补字段。
```

---

# C. Blocking Interaction 自解释

## 问题

当前错误：

```text
blocking interaction requires user answer: interaction-brief-xxx - 当前任务需要用户确认后再继续执行
```

没有告诉用户怎么回答。

## 改造

### C1. 新增异常

```java
BlockingInteractionException {
    String requestId;
    String question;
    String[] validChoices;
}
```

### C2. 错误输出

```text
ERROR: blocking interaction requires user answer.

request:
  interaction-brief-xxx

question:
  当前任务需要用户确认后再继续执行。

valid_choices:
  - continue
  - switch_to_strict
  - analyze_only
  - stop

next_command:
  dhk brief answer --request interaction-brief-xxx --choice "<choice>"

details:
  dhk brief show --request interaction-brief-xxx
```

### C3. brief answer 参数错误也要自解释

```text
ERROR: Missing required parameters: --request and --choice

example:
  dhk brief answer --request interaction-brief-xxx --choice continue

show request:
  dhk brief show --request interaction-brief-xxx
```

## 验收

```text
blocking interaction 错误包含 request id。
包含 valid choices。
包含 next_command。
用户无需读 BriefAnswerCommand.java 即可继续。
```

---

# D. 枚举值错误显示 valid values / aliases

## 问题

用户多次尝试：

```text
done
closed
accepted
resolved
open
approved
```

最终才找到 acceptance status 正确值：

```text
passed
```

## 改造

### D1. 引入 EnumValueSpec

```java
final class EnumValueSpec {
    String name;
    String[] validValues;
    Map<String, String> aliases;
}
```

### D2. 所有 enum 参数使用统一校验

覆盖：

```text
spec acceptance status
spec task status
workflow gate status
goal status
check status
verification mode
recommendation mode
graph mode
policy mode
```

### D3. 错误输出

```text
ERROR: Invalid acceptance status: done

valid_values:
  - pending
  - passed
  - failed
  - waived

aliases:
  - done -> passed
  - accepted -> passed
  - approved -> passed
  - closed -> passed
  - skipped -> waived

example:
  dhk spec acceptance update --status passed ...
```

## 验收

```text
所有 invalid enum 错误都包含 valid_values。
有 aliases 的显示 aliases。
spec acceptance 支持 done/accepted/approved/closed alias 到 passed。
```

---

# E. Demo No-Build Preset

## 问题

`springboot-auto-test` 对 mock 项目不友好；没有 `pom.xml` 时 compile/test skipped 可能卡 complete。`springboot-manual-ide-test` 又要求 manual evidence 文件。

需要一个只用于快速体验的模式。

## 改造

### E1. 新增 preset

```text
demo-no-build
harness-demo
quickstart-demo
```

建议保留一个正式命名：

```text
demo-no-build
```

### E2. 配置

```json
{
  "verification": {
    "compile": { "mode": "disabled" },
    "test": { "mode": "disabled" },
    "demo": {
      "enabled": true,
      "warning": "demo mode does not prove code correctness"
    }
  },
  "graph": {
    "required": false
  },
  "spec": {
    "required": false
  }
}
```

### E3. GOAL_CONTEXT 必须警告

```text
verification_mode: demo
warning: demo mode does not prove code correctness
allowed_for: quickstart, examples, mock projects
not_allowed_for: production development
```

### E4. quickstart 集成

```bash
dhk quickstart --preset demo-no-build --task "..." --module demo
```

## 验收

```text
无 pom.xml 的 mock 项目可走完整 goal 协议。
goal complete 不被 compile/test skipped 卡住。
输出明确 demo mode warning。
demo mode 不应作为生产默认 preset。
```

---

# F. goal complete not_ready 必须列出 blockers

## 问题

`goal complete` 没有报错，但状态仍是 `verifying`。用户不知道卡在哪里。

## 改造

### F1. complete 前 evaluate

`goal complete` 必须调用 evaluate，并在 not_ready 时输出完整 blocker report。

### F2. 非 0 exit code

not_ready 时返回：

```text
ExitCodes.VALIDATION_ERROR
```

不能返回 success。

### F3. 输出格式

```text
goal complete: not ready

blockers:
1. spec acceptance goal_checks_pass is pending
   reason:
     Required checks acceptance has not been passed.
   next_command:
     dhk goal verify --goal <goal>

2. workflow gate checkpoint_created is pending
   reason:
     completion checkpoint has not been created.
   next_command:
     dhk goal verify --goal <goal> --explain

3. impact check failed
   reason:
     changed files not covered by impact map.
   next_command:
     dhk graph impact --file <changed-file>
```

### F4. JSON

```json
{
  "decision": "not_ready",
  "blockers": [
    {
      "key": "impact",
      "user_message": "Impact map does not cover changed files.",
      "technical_reason": "changed files not covered by impact map",
      "next_command": "dhk graph impact --file <changed-file>"
    }
  ]
}
```

## 验收

```text
goal complete not_ready 时 exit code 非 0。
输出 blockers、reason、next_command。
goal audit 不再是唯一定位方式。
```

---

# G. Step ID 用户层改为 goal-local index

## 问题

全局 step_id 在第二个 goal 中从 5 开始，用户误以为 step 记录异常。

## 改造

### G1. 输出新增 goal_step_index

```text
step: 3/4
goal_step_index: 3
internal_step_id: 8
```

文本默认显示：

```text
step: 3
```

JSON 保留：

```json
{
  "step_id": 8,
  "goal_step_index": 3
}
```

### G2. Repository 层支持计算

实现：

```java
int countStepsBefore(goalKey, stepId)
```

或直接在插入后：

```text
goal_step_index = current step count for goal
```

不一定需要改 DB schema，展示层可计算。

## 验收

```text
同一个 goal 的用户输出从 step 1 开始。
JSON 保留全局 step_id。
不会破坏现有数据。
```

---

# H. memory list 命令

## 问题

用户想列出所有 confirmed memory，但 `memory search --q ""` 报错。

## 改造

新增：

```bash
dhk memory list
```

参数：

```text
--status confirmed|draft|deprecated|rejected
--module <module>
--tag <tag>
--limit <n>
--json
```

行为：

```text
按 created_at 或 updated_at 倒序列出 memory。
不需要 --q。
search 仍然要求非空 query。
```

输出：

```text
memory list
status: confirmed
total: 12

- memory_id: 101
  title: Payment callback idempotency
  module: payment
  tags: idempotency,payment
```

## 验收

```text
memory list --status confirmed 可列出 confirmed memory。
memory list --json 输出机器可读结果。
memory search 仍保持非空 q 语义。
```

---

# I. quickstart goal 复用安全

## 问题

`quickstart` 将 user 任务复用了之前 payment graph-aware goal，造成上下文污染。

## 改造

### I1. 默认不复用

默认：

```text
create new goal
```

### I2. 明确复用条件

只有满足全部条件才可自动候选：

```text
same project_key
same normalized task hash
same module
same profile
same mode
status in resumable states
not completed
not abandoned
created within TTL
```

### I3. 需要显式参数

```bash
dhk quickstart --resume-existing
```

否则即使发现相似 goal，也默认创建新 goal。

### I4. 输出冲突提示

```text
similar goal found:
- 20260528011859-payment-goal
  module: payment
  task: ...

current request:
  module: user
  task: ...

decision:
  create_new

reason:
  module mismatch
```

## 验收

```text
不同 task 不复用。
不同 module 不复用。
不同 profile 不复用。
默认 create new。
只有 --resume-existing 才复用。
跨模块复用必须失败。
```

---

# J. Build Wrapper / Maven Property Fallback

## 问题

每轮测试后要手动恢复 pom.xml。

## 改造

与 A 合并实现，但单独作为用户体验项：

```text
scripts/dev-build.sh
DHK_JACOCO_VERSION
-Djacoco.version
不写 pom.xml
```

也可以新增：

```bash
dhk dev build
```

但第一版脚本即可。

## 验收

```text
用户不需要 git checkout pom.xml。
构建 fallback 不污染工作区。
构建失败给出下一步建议。
```

---

## 5. 新增 Linear 建议

建议新增 umbrella：

```text
AI-215 Human-Facing CLI Recovery & Discoverability
```

子任务：

```text
AI-216 Actionable Error Contract
AI-217 Goal Evidence Contract Alignment
AI-218 Blocking Interaction Next Command
AI-219 Enum Valid Values and Aliases
AI-220 Goal Complete Blocker Report
AI-221 Goal-local Step Display Index
AI-222 Memory List Command
AI-223 Quickstart Resume Safety
AI-224 Demo No-build Preset
AI-225 Build Wrapper / Maven Property Fallback
```

优先级：

```text
P0:
AI-216
AI-217
AI-218
AI-220
AI-223

P1:
AI-219
AI-221
AI-222
AI-224

P2:
AI-225
```

---

## 6. 统一输出规范

### 6.1 成功输出

必须包含：

```text
status
what_changed
next_command
important_paths
```

### 6.2 失败输出

必须包含：

```text
error_code
reason
missing
valid_values
next_command
docs
```

### 6.3 not_ready 输出

必须包含：

```text
decision: not_ready
blockers:
  - key
  - user_message
  - technical_reason
  - next_command
```

### 6.4 JSON 输出

```json
{
  "ok": false,
  "error_code": "...",
  "blockers": [],
  "next_command": "...",
  "valid_values": []
}
```

---

## 7. 测试计划

### 7.1 Actionable Error tests

```text
Blocking interaction error includes next_command.
Missing evidence error includes required_evidence and example.
Invalid enum error includes valid_values.
goal complete not_ready includes blockers.
```

### 7.2 Quickstart tests

```text
quickstart creates new goal by default.
quickstart does not reuse goal across module.
quickstart --resume-existing only resumes valid matching goal.
demo-no-build quickstart completes protocol without pom.xml.
```

### 7.3 Memory tests

```text
memory list --status confirmed lists confirmed memory.
memory search still rejects empty q.
```

### 7.4 Step display tests

```text
second goal text output shows step 1.
JSON still contains global step_id.
```

### 7.5 Build tests

```text
dev-build.sh honors DHK_JACOCO_VERSION.
dev-build.sh does not modify pom.xml.
```

---

## 8. Release Gate

进入 beta 前必须满足：

```text
1. P0 全部完成。
2. 所有 not_ready 输出 blockers。
3. 所有 validation error 输出 next_command 或 valid_values。
4. quickstart 不再错误复用跨模块 goal。
5. memory list 可用。
6. demo-no-build 可用。
7. mvn -q test 通过。
8. git diff --check 通过。
```

---

## 9. 最终目标

DevHarnessKit 的设计哲学是：

```text
工具告诉用户下一步该做什么。
```

这个哲学现在已经在 Agent-facing 的 `GOAL_CONTEXT` 里体现出来。
下一步必须同样应用到 Human-facing CLI：

```text
任何失败都不能只说失败。
必须告诉用户：为什么失败、缺什么、有效值是什么、下一步命令是什么。
```

一句话目标：

```text
让用户不再通过读源码和反复试错使用 DevHarnessKit。
