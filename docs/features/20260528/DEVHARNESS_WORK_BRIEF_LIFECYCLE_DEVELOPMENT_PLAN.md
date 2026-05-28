# DevHarnessKit Work Brief Lifecycle & Knowledge Candidate 开发方案

建议版本：`V0.8.7 Agent-Mounted Work Brief Harness`

## 1. 背景

DevHarnessKit 当前已经从 memory-first 演进到 goal-first，并进一步形成 graph-aware goal、manual verification、agent adapter packaging、configure/control panel 等能力。当前的问题已经不是底层 CLI 能否完成任务，而是：

```text
用户仍然可能看到过多底层 dhk 命令；
Agent 仍然可能把执行细节暴露给用户；
Work Brief 目前更像任务开始前的说明，而不是贯穿工作全程的交互层；
知识沉淀仍然依赖人工事后整理；
项目事实、个人经验、团队模式之间还没有清晰的用户确认流程。
```

因此下一阶段应把 Work Brief 提升为高优主线，让它成为 DevHarnessKit 面向用户的主交互层。

核心目标：

```text
用户看到 Work Brief / Progress Brief / Verify Brief / Knowledge Brief。
Agent 消费 Agent Execution Brief / GOAL_CONTEXT / harness_commands。
Harness 负责 goal / checks / graph / memory / workflow / spec / artifacts / growth 审计。
```

一句话：

```text
用户不再面对一堆 dhk 命令，而是看到“我将做什么、风险是什么、需要你确认什么、哪些知识值得沉淀”。
```

---

## 2. 总体架构

### 2.1 三层协议

```text
User Layer
  - WORK_BRIEF.md
  - PROGRESS_BRIEF.md
  - VERIFY_BRIEF.md
  - KNOWLEDGE_BRIEF.md
  - 用户选择项 / 风险说明 / 需要确认的问题

Agent Layer
  - AGENT_BRIEF.json
  - GOAL_CONTEXT.md
  - harness_commands
  - allowed_actions / forbidden_actions
  - required_evidence / verification_policy

Harness Layer
  - goal_run / goal_step / goal_check
  - graph snapshot / impact map
  - workflow / spec / artifact
  - memory draft / confirmed
  - growth lessons
  - policy / hooks / audit events
```

原则：

```text
Work Brief 给用户看。
Agent Brief 给模型执行。
Goal/Harness 是事实源和审计源。
```

---

## 3. Work Brief 生命周期

Work Brief 不应只是 quickstart 或 advise 的一次性输出，而应贯穿 goal 全生命周期。

### 3.1 Pre-Work Brief

任务开始前生成。

内容：

```text
task_summary
recommendation
confidence
why
risk_flags
expected_work
will_not_do
user_choices
confirmation_required
```

目标：

```text
让用户知道系统如何理解任务，建议走轻流程还是完整流程，需要确认什么。
```

---

### 3.2 Planning Brief

在 inspect / plan 阶段生成或更新。

内容：

```text
影响面
风险升级点
需要用户确认的问题
建议读取的文件
建议的 profile / verification policy
```

目标：

```text
防止 Agent 在目标不清晰或风险升级时继续修改代码。
```

---

### 3.3 Progress Brief

每个关键 step 后更新。

内容：

```text
已完成什么
当前 current_action
下一步建议
已发现风险
已生成 evidence
是否需要用户介入
```

目标：

```text
让用户不用读 goal/check 内部状态，也能理解当前进展。
```

---

### 3.4 Verify Brief

`goal verify` 阶段生成。

内容：

```text
哪些验证已通过
哪些验证缺 evidence
哪些验证是 manual_required
为什么不能 complete
下一步用户/Agent 应该怎么处理
```

目标：

```text
把 check failed / accepted_statuses 这类内部错误，转成用户能理解的下一步指导。
```

---

### 3.5 Completion Brief

任务完成时生成。

内容：

```text
最终完成内容
变更文件
验证方式
遗留风险
rollback 信息
建议沉淀知识
```

目标：

```text
完成任务的用户层总结。
```

---

### 3.6 Knowledge Brief

完成后或执行中生成。

内容：

```text
项目事实候选
项目约定候选
验证规则候选
个人成长经验候选
团队模式候选
每条候选的证据、置信度、建议去向
```

目标：

```text
让 DevHarnessKit 主动发现并请求用户确认可沉淀知识。
```

---

## 4. 新增核心模型

### 4.1 WorkBrief

```java
final class WorkBrief {
    String briefId;
    String goalKey;
    String taskSummary;
    String recommendation; // patch | standard | strict | analyze_only | ask
    String confidence;     // high | medium | low
    String why;
    String[] riskFlags;
    String[] expectedWork;
    String[] willNotDo;
    UserChoice[] userChoices;
    boolean confirmationRequired;
    String confirmationReason;
    boolean safeToStart;
    String createdAt;
    String updatedAt;
}
```

推荐枚举：

```text
patch
standard
strict
analyze_only
ask
```

不要混用“快速小改 / 完整流程 / full”等不稳定命名。中文只用于展示文案，机器字段固定。

---

### 4.2 AgentExecutionBrief

```java
final class AgentExecutionBrief {
    String schemaVersion;
    String briefId;
    String recommendationId;
    String taskKey;
    String goalKey;
    String mode; // patch | standard | strict | analyze_only | ask
    String profileKey;
    String currentAction;
    String[] allowedActions;
    String[] forbiddenActions;
    String[] requiredEvidence;
    EscalationRule[] escalationRules;
    VerificationPolicyView verificationPolicy;
    String userVisibleSummaryRef;
    HarnessCommand[] harnessCommands;
    boolean showCommandsToUser;
}
```

`harnessCommands` 必须是 agent-internal：

```java
final class HarnessCommand {
    String name;
    String[] argv;
    String when;
    boolean userVisible; // 默认 false
}
```

规则：

```text
Agent Brief 可以包含 dhk 命令。
Work Brief 不应暴露底层命令。
```

---

### 4.3 InteractionRequest

用于主动和用户交互。

```java
final class InteractionRequest {
    String requestId;
    String goalKey;
    String phase; // planning | implementation | verification | completion
    String type;  // clarification | confirmation | risk_escalation | manual_evidence | knowledge_candidate
    String priority; // blocking | important | optional
    String question;
    String why;
    UserChoice[] choices;
    String defaultChoice;
    boolean blocksProgress;
    String status; // open | answered | dismissed
    String createdAt;
    String answeredAt;
}
```

只在 decision-changing 时打断用户：

```text
任务目标不清楚
风险升级
需要人工验证
触碰 protected file / 安全 / 权限 / 配置 / DB
发现值得沉淀的知识点
```

不应在无关小问题上频繁询问。

---

### 4.4 KnowledgeCandidate

用于知识候选提取。

```java
final class KnowledgeCandidate {
    String candidateId;
    String goalKey;
    String type;
    String title;
    String summary;
    String[] evidenceRefs;
    String suggestedDestination; // project_memory | growth | team_pattern
    int confidence;
    boolean requiresConfirmation;
    String sensitiveScanStatus; // passed | failed | skipped
    String status; // draft | confirmed | rejected | deprecated
    String createdAt;
    String confirmedAt;
}
```

候选类型：

```text
project_fact_candidate
project_convention_candidate
business_rule_candidate
verification_rule_candidate
risk_lesson_candidate
growth_lesson_candidate
team_pattern_candidate
```

---

## 5. 新增文件结构

建议按 goal 维度输出 brief：

```text
.agents/devharness/briefs/<goal-key>/
  WORK_BRIEF.md
  AGENT_BRIEF.json
  PROGRESS_BRIEF.md
  VERIFY_BRIEF.md
  KNOWLEDGE_CANDIDATES.md
  interactions.json
  knowledge-candidates.json
```

如果是 `advise` 还没有 goal，可以使用 task key：

```text
.agents/devharness/briefs/<task-key>/
```

---

## 6. 新增命令设计

### 6.1 `dhk advise`

只分析，不创建 goal。

```bash
dhk advise \
  --task "修复订单分页边界" \
  --module order \
  --profile auto \
  --mode recommend
```

输出：

```text
WORK_BRIEF.md
AGENT_BRIEF.json
```

规则：

```text
不创建 goal。
不修改 workflow/spec。
可选记录 recommendation event。
```

---

### 6.2 `dhk brief`

Brief 管理入口。

```bash
dhk brief show --goal <goal-key>
dhk brief export --goal <goal-key>
dhk brief list --goal <goal-key>
dhk brief answer --request <request-id> --choice <choice>
dhk brief knowledge --goal <goal-key>
dhk brief confirm-knowledge --candidate <candidate-id>
dhk brief reject-knowledge --candidate <candidate-id>
```

---

### 6.3 `dhk goal next` 集成

`goal next` 应在返回 current_action 的同时更新：

```text
PROGRESS_BRIEF.md
AGENT_BRIEF.json
```

如果存在 blocking interaction：

```text
current_action = wait_for_user_answer
next_command = dhk brief answer ...
```

Agent 不应继续编辑代码。

---

### 6.4 `dhk goal verify` 集成

`goal verify` 应生成：

```text
VERIFY_BRIEF.md
```

输出用户友好指导：

```text
哪些通过
哪些缺证据
为什么不能 complete
下一步怎么做
```

JSON 仍保留内部结构：

```text
blocker_details
check_key
accepted_statuses
stale_checks
next_command
```

---

### 6.5 `dhk goal complete` 集成

`goal complete` 后生成：

```text
COMPLETION_BRIEF.md
KNOWLEDGE_CANDIDATES.md
```

并提示用户：

```text
发现 3 条可沉淀知识，是否保存为项目记忆草稿或个人成长经验？
```

---

## 7. Mode Advisor

### 7.1 输入

```text
task text
module
profile
configure preset
verification policy
protected files
policy
risk keywords
graph state
impact scope
```

风险关键词：

```text
新增
重构
迁移
权限
登录
支付
风控
配置
schema
SQL
跨模块
灰度
回滚
```

---

### 7.2 输出

```java
final class ModeRecommendation {
    String recommendation; // patch | standard | strict | analyze_only | ask
    String confidence;
    int riskScore;
    String[] riskFlags;
    String[] why;
    boolean confirmationRequired;
    String[] questions;
}
```

---

### 7.3 规则建议

Patch：

```text
局部逻辑
1-3 个文件
无安全/权限/配置/schema/DB风险
无跨模块
验证可控
```

Standard：

```text
新增 API
常规跨层变更
需要测试或 manual evidence
影响面中等
```

Strict：

```text
权限/安全/支付/风控
数据库 schema/迁移
配置/部署
跨模块影响
protected files
graph impact 范围大
```

Analyze only：

```text
用户只要求评估
目标不要求修改
系统无法判断改动范围
```

Ask：

```text
任务目标不清楚
多个实现方向
缺少业务确认
继续会导致错误实现
```

---

## 8. Patch Profiles 与升级规则

### 8.1 新增 profile

```text
java-api-patch
safe-patch
```

Patch required evidence：

```text
changed_files
reason
verification
risk_summary
```

Patch checks：

```text
sensitive
manual-or-auto-verification
discipline
```

---

### 8.2 升级规则

Soft escalation：

```text
changed_files > 3
graph impact 范围中等
测试缺失但风险较低
```

Hard escalation：

```text
protected files
schema/migration
security/permission/auth
payment/risk-control
prod config
DB write behavior
secret/policy boundary
```

Soft escalation 只建议。  
Hard escalation 不允许 patch complete。

---

## 9. Knowledge Candidate 提取策略

### 9.1 Inspect 阶段

提取：

```text
项目结构
命名规则
返回结构
错误码规则
分页规则
权限注解规则
Mapper XML 约定
```

生成：

```text
project_convention_candidate
```

---

### 9.2 Planning 阶段

提取：

```text
兼容性风险
跨模块风险
验证约束
升级规则
```

生成：

```text
risk_lesson_candidate
verification_rule_candidate
```

---

### 9.3 Verification 阶段

提取：

```text
项目无法 mvn test
IDE 手工测试规则
CI-only 验证规则
慢测试约束
```

生成：

```text
verification_rule_candidate
growth_lesson_candidate
```

---

### 9.4 Completion 阶段

展示给用户：

```text
以下知识点是否保存？
1. 保存为项目 memory draft
2. 保存为个人 growth draft
3. 保存为团队 pattern draft
4. 忽略
5. 编辑后保存
```

---

## 10. 与 Memory / Growth 的边界

### 10.1 Project Memory

当前项目事实：

```text
必须有代码证据
必须 sensitive scan passed
默认 draft
需要 confirm
```

### 10.2 Growth Lessons

个人经验：

```text
advisory
不能作为当前项目事实
需要人工确认
可跨项目复用
```

### 10.3 Team Patterns

团队模式：

```text
需要更高确认级别
建议后续阶段做
```

---

## 11. 需要新增的 Linear issue

建议在 AI-200 到 AI-211 基础上新增：

```text
AI-212 Work Brief Lifecycle Interaction
AI-213 Knowledge Candidate Extraction
AI-214 Knowledge Candidate 与 Growth Layer 集成
```

### AI-212：Work Brief Lifecycle Interaction

范围：

```text
InteractionRequest model
blocking / optional questions
goal next / verify 输出 interaction
brief answer 命令
Agent Brief wait_for_user_answer
```

### AI-213：Knowledge Candidate Extraction

范围：

```text
KnowledgeCandidate model
Knowledge Brief renderer
completion 阶段候选展示
confirm/reject/edit 流程
memory draft / growth draft 输出
```

### AI-214：Growth 集成

范围：

```text
growth lesson draft
growth export
Work Brief related lessons
Agent Brief growth_context
```

---

## 12. 推荐执行顺序

```text
P0:
1. AI-200 Work Brief 契约
2. AI-201 Agent Brief 契约
3. AI-212 Work Brief Lifecycle Interaction
4. AI-213 Knowledge Candidate Extraction
5. AI-202 Mode Advisor
6. AI-208 dhk advise

P1:
7. AI-204 Patch Profiles
8. AI-207 goal step --auto
9. AI-203 quickstart mode
10. AI-205 adapter 改造
11. AI-206 verify/progress 友好输出

P2:
12. AI-209 文档
13. AI-210 E2E
14. AI-211 发布兼容
15. AI-214 Growth 集成
```

---

## 13. 风险控制

### 13.1 Work Brief 不能替代 Goal

```text
Work Brief 只是用户层交互。
Goal/SQLite/Artifacts 仍是事实源。
```

### 13.2 Agent Brief 不能替代 GOAL_CONTEXT

```text
Agent Brief 是入口协议。
GOAL_CONTEXT 是当前状态约束。
```

### 13.3 Knowledge Candidate 不能自动 confirmed

```text
所有候选知识默认 draft。
确认必须由用户或明确命令完成。
```

### 13.4 主动交互不能过度打断

只在 decision-changing 的点交互。

---

## 14. 最终目标

```text
用户看到：
  我理解的任务是什么
  我建议走什么模式
  风险在哪里
  需要你确认什么
  哪些知识值得保存

Agent 看到：
  执行协议
  allowed/forbidden actions
  required evidence
  harness commands
  wait_for_user_answer

Harness 保存：
  goal state
  checks
  artifacts
  memory draft
  knowledge candidates
  audit trail
```

最终一句话：

```text
Work Brief 成为 DevHarnessKit 的用户主交互层；Agent Brief 成为 Agent 的机器协议；Goal/Harness 继续作为事实源、验证源和审计源。
```
