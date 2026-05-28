# DevHarnessKit Work Brief Lifecycle 建议 Linear 拆分

## Umbrella

建议新增：

```text
AI-212 Work Brief Lifecycle Interaction
AI-213 Knowledge Candidate Extraction
AI-214 Knowledge Candidate 与 Growth Layer 集成
```

---

## AI-212：Work Brief Lifecycle Interaction

### 背景

当前 AI-200 定义 Work Brief 用户层契约，但主要是任务开始前的说明。为了让 Work Brief 成为 DevHarnessKit 的用户主交互层，需要让它贯穿 goal 生命周期，在 planning、progress、verify、completion 阶段持续更新，并在关键决策点主动与用户交互。

### 目标

让 Work Brief 支持：

```text
clarification
confirmation
risk escalation
manual evidence request
knowledge candidate prompt
```

### 范围

1. 定义 `InteractionRequest` model。
2. 支持 blocking / important / optional 交互优先级。
3. `goal next` 可输出/更新 interaction。
4. `goal verify` 可输出 manual evidence request。
5. `Agent Brief` 支持 `wait_for_user_answer`。
6. 新增 `dhk brief answer` 或等价命令。
7. Work Brief / Progress Brief 显示用户友好问题与选择项。

### 非目标

* 不做聊天 UI。
* 不引入 daemon。
* 不让模型自由生成未结构化问题。
* 不绕过 goal 协议。

### 验收

* 任务不清楚时 recommendation=ask，并生成 blocking question。
* 风险升级时生成 user choice。
* manual test 缺 evidence 时生成 manual evidence request。
* 用户回答后 Agent Brief 更新。
* blocking interaction 存在时不能进入 implement action。

---

## AI-213：Knowledge Candidate Extraction

### 背景

DevHarnessKit 在 goal 执行过程中已经能够观察代码、graph、verification、manual evidence 和风险变化。很多信息可以沉淀为项目记忆、个人成长经验或团队模式，但目前依赖人工事后整理。

### 目标

在 goal 执行过程中主动发现可沉淀知识点，生成候选，并请求用户确认。

### 范围

1. 定义 `KnowledgeCandidate` model。
2. 支持 project_fact / project_convention / business_rule / verification_rule / risk_lesson / growth_lesson / team_pattern 类型。
3. 生成 `KNOWLEDGE_CANDIDATES.md`。
4. Completion Brief 展示知识候选。
5. 支持 confirm/reject/edit。
6. confirm 到 project memory 时只生成 draft memory。
7. confirm 到 growth 时只生成 draft growth lesson。
8. sensitive scan failed 时阻止 confirm。

### 非目标

* 不自动 confirmed memory。
* 不自动进入团队共享。
* 不做复杂 LLM 总结系统。
* 不跨项目隐式注入知识。

### 验收

* Inspect 阶段可生成 project convention candidate。
* Verify 阶段可生成 verification rule candidate。
* Completion 阶段展示知识候选。
* 不经用户确认不能进入 confirmed memory。
* sensitive scan failed 时不生成候选或阻止 confirm。

---

## AI-214：Knowledge Candidate 与 Growth Layer 集成

### 背景

部分知识不是当前项目事实，而是个人开发经验或跨项目模式。它们不应进入 project memory，但可以作为 advisory growth lesson 在未来任务中复用。

### 目标

把 Knowledge Candidate 与 Growth Layer 连接起来，支持个人经验沉淀和未来 Work Brief 推荐。

### 范围

1. `suggested_destination=growth`。
2. Growth lesson draft。
3. `dhk growth review/confirm`。
4. `dhk growth export`。
5. Work Brief 展示 related growth lessons。
6. Agent Brief 包含 growth_context。
7. 明确 growth lesson 是 advisory，不是项目事实。

### 非目标

* 不做团队经验库。
* 不自动跨项目注入。
* 不自动确认 growth lesson。

### 验收

* Knowledge Candidate 可保存为 growth draft。
* growth export 可生成 GROWTH_CONTEXT.md。
* Work Brief 能显示相关 growth lessons。
* Agent Brief 中标记 advisory_only=true。
