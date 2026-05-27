# DevHarnessKit BDD-aware Acceptance Harness 详细开发与测试方案

> 建议文件路径：`docs/DEVHARNESS_BDD_AWARE_ACCEPTANCE_HARNESS_DETAILED_PLAN.md`
> 建议版本目标：`V0.6 BDD-aware Acceptance Harness`
> 适用背景：在 DevHarnessKit 已具备 `memory / spec / workflow / goal / policy / graph` 方向后，新增一层可执行行为规格，用于增强需求验收、弱模型约束、老旧项目人工验证和 Graph-aware Goal Harness 的质量闭环。
> V0.6.2 已实现契约与测试矩阵见：`docs/features/BDD_SPEC_GOAL_BINDING_TEST_MATRIX.md`

---

## 1. 背景与定位

DevHarnessKit 当前已经逐步形成以下结构：

```text
Memory     项目事实、历史约束、人工确认知识
Spec       需求变更、任务、验收项
Workflow   阶段、Gate、Artifact、Checkpoint
Goal       主控编排，串联上下文、执行、验证、完成
Policy     文件/命令/数据库/敏感信息安全边界
Graph      代码结构、调用链、影响面分析
Skill      AI 模型使用 DevHarnessKit 的协议入口
```

但当前体系仍然缺一层关键能力：

```text
需求和验收如何变成可执行、可检查、可追踪的行为场景？
```

BDD-aware Acceptance Harness 的目标就是补上这一层：

```text
Spec 描述“要做什么”
BDD 描述“什么行为算做对”
Graph 分析“这个行为影响哪些代码”
Goal 编排“如何完成这个行为”
Check 验证“行为是否成立”
Evidence 记录“验证证据”
Eval 衡量“产出质量”
```

最终目标：

```text
让 AI 不只是知道要改代码，
还必须先明确可验证行为，
再基于代码图谱定位影响面，
最后用自动或人工证据证明行为成立。
```

---

## 2. 核心设计原则

### 2.1 BDD 是验收规格层，不是单纯测试框架

第一版不应直接绑定 Cucumber 或任何具体测试框架。
应先把 BDD 设计成 DevHarnessKit 内部的“行为规格与验收证据层”。

```text
第一阶段：Specification BDD
- 可写
- 可绑定 spec
- 可绑定 goal
- 可导出 feature/context
- 可绑定 evidence
- 可进入 goal verify

后续阶段：Executable BDD
- Cucumber JVM
- JBehave
- JUnit / MockMvc
- Playwright
- Postman / Newman
- Manual Evidence
```

### 2.2 BDD 不替代 Spec

推荐层级：

```text
SpecChange
  └── Acceptance
        └── BDD Scenario
              └── Given / When / Then / And
              └── Evidence
```

Spec 仍然是需求变更容器。
BDD 是 acceptance 的具体行为例子。

### 2.3 BDD 不替代 Graph

BDD 说明行为，Graph 定位代码结构。

```text
BDD: 用户输入客户名称后，订单列表只显示匹配订单
Graph: OrderController -> OrderService -> OrderMapper.xml -> order-list.jsp
```

两者结合后，Goal 才能更可靠地指导模型改代码。

### 2.4 BDD 对弱模型应降低自由度

弱模型在编码任务中常见失败：

```text
直接写代码
不知道验收标准
遗漏边界情况
忽视历史行为
改完就声称完成
没有验证证据
```

BDD 应强制模型先回答：

```text
Given 什么前置条件？
When 用户做什么？
Then 系统应该怎样？
哪些旧行为不能破坏？
这个行为如何验证？
```

---

## 3. 模块目标

新增模块：

```text
dhk bdd
```

核心产物：

```text
BDD_CONTEXT.md
FEATURES/*.feature
BDD_EVIDENCE.md
BDD_COVERAGE.md
SCENARIO_IMPACT_MAP.md
```

核心能力：

```text
1. 创建行为场景
2. 管理 Given/When/Then steps
3. 绑定 spec acceptance
4. 绑定 goal
5. 绑定 graph nodes / impact map
6. 绑定自动或人工证据
7. 导出 BDD context
8. 进入 goal verify
9. 参与 Eval 评分
```

---

## 4. 建议 CLI 设计

### 4.1 顶层命令

```bash
dhk bdd init
dhk bdd add
dhk bdd list
dhk bdd show
dhk bdd update
dhk bdd bind-spec
dhk bdd bind-goal
dhk bdd bind-graph
dhk bdd evidence
dhk bdd export
dhk bdd verify
dhk bdd coverage
dhk bdd lint
```

### 4.2 `dhk bdd init`

初始化 BDD 数据结构和目录。

```bash
dhk bdd init --project-root .
```

输出：

```text
.agents/bdd/
  features/
  evidence/
  exports/
```

数据库迁移：

```text
schema v7: bdd_feature / bdd_scenario / bdd_step / bdd_binding / bdd_evidence
```

---

### 4.3 `dhk bdd add`

添加场景。

```bash
dhk bdd add   --feature order-query   --scenario "按客户名称查询订单"   --given "系统中存在客户名称为 张三 的订单"   --when "用户在订单查询页输入客户名称 张三"   --then "订单列表只显示客户名称包含 张三 的订单"   --tags "order,query,legacy"
```

也支持从文件导入：

```bash
dhk bdd add --from-file docs/scenarios/order-query.feature
```

### 4.4 `dhk bdd bind-spec`

将 BDD scenario 绑定到 spec acceptance。

```bash
dhk bdd bind-spec   --scenario order-query.customer-name   --change order-query-api   --acceptance A001   --relation verifies
```

### 4.5 `dhk bdd bind-goal`

将 BDD scenario 绑定到 goal。

```bash
dhk bdd bind-goal   --scenario order-query.customer-name   --goal 202605-order-query
```

### 4.6 `dhk bdd bind-graph`

将 BDD scenario 绑定到 graph nodes 或 impact map。

```bash
dhk bdd bind-graph   --scenario order-query.customer-name   --node com.example.order.OrderController#query   --relation entrypoint
```

或：

```bash
dhk bdd bind-graph   --scenario order-query.customer-name   --impact-map .agents/graph/exports/SCENARIO_IMPACT_MAP.md
```

### 4.7 `dhk bdd evidence`

记录验证证据。

```bash
dhk bdd evidence   --scenario order-query.customer-name   --goal 202605-order-query   --type manual   --status passed   --summary "人工验证订单查询页按客户名称过滤成功，分页总数正确"   --file evidence/order-query-customer-name.md
```

自动测试证据：

```bash
dhk bdd evidence   --scenario order-query.customer-name   --goal 202605-order-query   --type test   --status passed   --summary "OrderControllerTest#queryByCustomerName passed"   --file target/surefire-reports/OrderControllerTest.txt
```

### 4.8 `dhk bdd export`

导出 BDD 上下文。

```bash
dhk bdd export --goal 202605-order-query
```

输出：

```text
.agents/bdd/exports/BDD_CONTEXT.md
.agents/bdd/features/order-query.feature
```

### 4.9 `dhk bdd verify`

检查 BDD 场景是否满足验收要求。

```bash
dhk bdd verify --goal 202605-order-query
```

验证内容：

```text
1. goal 是否绑定 scenario
2. spec acceptance 是否有 scenario 覆盖
3. scenario 是否有 Given/When/Then
4. scenario 是否绑定 evidence
5. evidence 是否 passed
6. scenario 是否存在 vague steps
7. scenario 是否存在重复或无效步骤
```

### 4.10 `dhk bdd coverage`

输出 BDD 覆盖率报告。

```bash
dhk bdd coverage --change order-query-api
```

输出：

```text
BDD_COVERAGE.md
```

内容：

```text
acceptance_count
acceptance_with_scenario_count
scenario_count
scenario_with_evidence_count
passed_scenario_count
pending_scenario_count
coverage_percent
```

### 4.11 `dhk bdd lint`

检查 BDD 质量。

```bash
dhk bdd lint --goal 202605-order-query
```

检查：

```text
Then 系统正常       -> vague
When 操作完成       -> vague
Given 准备数据       -> vague
重复 scenario        -> duplicate
没有 Then            -> invalid
没有 evidence        -> pending
```

---

## 5. 数据库 Schema 设计

建议新增 schema v7。

### 5.1 bdd_feature

```sql
CREATE TABLE bdd_feature (
  feature_key TEXT PRIMARY KEY,
  project_key TEXT NOT NULL,
  title TEXT NOT NULL,
  module_name TEXT NOT NULL DEFAULT '',
  description TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'active',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
```

### 5.2 bdd_scenario

```sql
CREATE TABLE bdd_scenario (
  scenario_key TEXT PRIMARY KEY,
  feature_key TEXT NOT NULL,
  title TEXT NOT NULL,
  scenario_type TEXT NOT NULL DEFAULT 'acceptance',
  priority TEXT NOT NULL DEFAULT 'normal',
  status TEXT NOT NULL DEFAULT 'draft',
  tags TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
```

### 5.3 bdd_step

```sql
CREATE TABLE bdd_step (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  scenario_key TEXT NOT NULL,
  step_order INTEGER NOT NULL,
  step_type TEXT NOT NULL, -- given / when / then / and
  step_text TEXT NOT NULL,
  normalized_text TEXT NOT NULL DEFAULT ''
);
```

### 5.4 bdd_binding

```sql
CREATE TABLE bdd_binding (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  scenario_key TEXT NOT NULL,
  binding_type TEXT NOT NULL, -- spec / goal / graph_node / graph_impact / test / manual
  binding_key TEXT NOT NULL,
  relation TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL
);
```

### 5.5 bdd_evidence

```sql
CREATE TABLE bdd_evidence (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  scenario_key TEXT NOT NULL,
  goal_key TEXT NOT NULL DEFAULT '',
  evidence_type TEXT NOT NULL, -- test / manual / http / sql / screenshot / log
  status TEXT NOT NULL, -- passed / failed / skipped / pending
  evidence_path TEXT NOT NULL DEFAULT '',
  summary TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL
);
```

### 5.6 bdd_quality_issue

```sql
CREATE TABLE bdd_quality_issue (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  scenario_key TEXT NOT NULL,
  issue_type TEXT NOT NULL, -- vague_step / duplicate / missing_then / missing_evidence
  severity TEXT NOT NULL DEFAULT 'warning',
  message TEXT NOT NULL,
  created_at TEXT NOT NULL
);
```

---

## 6. Model / Repository / Service 设计

### 6.1 Model

```text
src/main/java/com/devharnesskit/dhk/model/bdd/
  BddFeature.java
  BddScenario.java
  BddStep.java
  BddBinding.java
  BddEvidence.java
  BddQualityIssue.java
  BddCoverage.java
  BddVerificationResult.java
```

### 6.2 Repository

```text
src/main/java/com/devharnesskit/dhk/repository/bdd/
  BddFeatureRepository.java
  BddScenarioRepository.java
  BddStepRepository.java
  BddBindingRepository.java
  BddEvidenceRepository.java
  BddQualityIssueRepository.java
```

### 6.3 Service

```text
src/main/java/com/devharnesskit/dhk/service/bdd/
  BddService.java
  BddKeyGenerator.java
  BddExportService.java
  BddVerificationService.java
  BddCoverageService.java
  BddLintService.java
  BddFeatureRenderer.java
  BddContextRenderer.java
```

### 6.4 Command

```text
src/main/java/com/devharnesskit/dhk/command/bdd/
  BddCommand.java
  BddInitCommand.java
  BddAddCommand.java
  BddListCommand.java
  BddShowCommand.java
  BddBindSpecCommand.java
  BddBindGoalCommand.java
  BddBindGraphCommand.java
  BddEvidenceCommand.java
  BddExportCommand.java
  BddVerifyCommand.java
  BddCoverageCommand.java
  BddLintCommand.java
```

---

## 7. 导出契约

### 7.1 BDD_CONTEXT.md

示例：

```md
# BDD_CONTEXT

<goal>
- goal_key: 202605-order-query
- task: 实现订单查询按客户名称过滤
</goal>

<scenario>
- scenario_key: order-query.customer-name
- feature: order-query
- status: active
- tags: order,query,legacy

Given 系统中存在客户名称为 张三 的订单
When 用户在订单查询页输入客户名称 张三
Then 订单列表只显示客户名称包含 张三 的订单
And 分页总数与查询条件匹配
</scenario>

<bindings>
- spec_acceptance: A001
- graph_impact: .agents/graph/exports/SCENARIO_IMPACT_MAP.md
</bindings>

<evidence>
- status: pending
- required: manual or automated test evidence
</evidence>

<model-instructions>
- Do not claim completion until all required scenarios have passed evidence.
- Do not invent scenario results.
- If a scenario cannot be automated, record manual evidence.
</model-instructions>
```

### 7.2 `.feature` 导出

```gherkin
Feature: Order Query

  Scenario: 按客户名称查询订单
    Given 系统中存在客户名称为 张三 的订单
    When 用户在订单查询页输入客户名称 张三
    Then 订单列表只显示客户名称包含 张三 的订单
    And 分页总数与查询条件匹配
```

### 7.3 BDD_COVERAGE.md

```md
# BDD Coverage

- spec_change: order-query-api
- acceptance_total: 3
- acceptance_with_scenario: 3
- scenario_total: 5
- scenario_with_evidence: 4
- scenario_passed: 4
- scenario_pending: 1
- coverage: 80%

## Missing Evidence
- order-query.empty-customer-name
```

---

## 8. Goal 集成设计

### 8.1 Goal Profile 新字段

建议在 `GoalProfile` 增加：

```text
bddRequired
bddScenarioRequired
bddVerifyRequired
bddMinScenarioCount
bddRequireEvidence
```

配置示例：

```json
{
  "profile_key": "legacy-java-small-fix-with-bdd",
  "workflow_key": "legacy-small-change",
  "requires_spec": true,
  "bdd_required": true,
  "bdd_min_scenario_count": 1,
  "bdd_require_evidence": true,
  "actions": "inspect_existing_behavior,define_bdd_scenarios,graph_impact_analysis,create_minimal_plan,implement_small_change,verify_bdd_scenarios,create_rollback_plan"
}
```

### 8.2 Goal 流程

推荐流程：

```text
1. inspect_existing_behavior
2. define_bdd_scenarios
3. graph_impact_analysis
4. create_minimal_plan
5. implement_small_change
6. verify_bdd_scenarios
7. create_rollback_plan
```

### 8.3 Goal verify 集成

`goal verify` 应调用：

```text
BddVerificationService.verifyGoal(goalKey)
```

校验：

```text
1. goal 是否要求 BDD
2. 是否绑定足够 scenario
3. scenario 是否覆盖 spec acceptance
4. scenario 是否有 Given/When/Then
5. scenario 是否有 evidence
6. required scenario 是否 passed
7. 是否有 blocking BDD lint issue
```

### 8.4 Goal complete 前强制要求

如果 profile `bddRequired=true`：

```text
goal complete 必须要求：
- bdd verify passed
- bdd coverage 达标
- 所有 required scenario 有 evidence
```

---

## 9. Spec 集成设计

### 9.1 Acceptance 与 Scenario 绑定

每个 spec acceptance 可以绑定多个 scenario。

```text
Acceptance A001: 支持按客户名称查询
  - Scenario S001: 客户名称精确匹配
  - Scenario S002: 客户名称模糊匹配
  - Scenario S003: 客户名称为空时返回全部
```

### 9.2 Spec export 增强

`SPEC_CONTEXT.md` 中增加：

```text
acceptance -> bdd_scenarios -> evidence_status
```

### 9.3 Spec archive 前检查

如果某个 acceptance 绑定了 BDD scenario，则 archive 前必须满足：

```text
required scenario passed 或 waived
scenario evidence exists
blocking BDD quality issues = 0
```

---

## 10. Graph 集成设计

### 10.1 Scenario-driven Impact

新增：

```bash
dhk graph impact --scenario order-query.customer-name
```

输出：

```text
SCENARIO_IMPACT_MAP.md
```

Graph 查询来源：

```text
scenario title
Given/When/Then text
feature key
tags
bound spec acceptance
bound goal task
```

### 10.2 BDD 与 Graph Binding

BDD scenario 可绑定：

```text
entrypoint
controller
service
mapper
sql_table
jsp
test
config
```

### 10.3 Goal 中强制引用 Impact Map

如果 profile 同时要求：

```text
bddRequired=true
graphRequired=true
```

则 `define_bdd_scenarios` 后，必须执行：

```text
graph impact --scenario
```

并在 goal step evidence 中包含：

```text
scenario_impact_map=<path>
```

---

## 11. Policy / Security 设计

### 11.1 BDD 写入敏感信息检查

以下命令必须经过 `SensitiveDataGuard`：

```text
bdd add
bdd update
bdd evidence
bdd export
```

尤其是：

```text
Given 中不要写真实账号密码
Evidence 中不要写真实手机号、身份证、token、JDBC URL
```

### 11.2 Policy 配置

新增：

```json
{
  "bddPolicy": {
    "requireScenarioForAcceptance": true,
    "requireEvidenceForScenario": true,
    "allowManualEvidence": true,
    "blockVagueThen": true,
    "blockDuplicateScenario": false,
    "minScenarioPerAcceptance": 1
  }
}
```

### 11.3 Forbidden Examples

```text
Then 系统正常
Then 查询成功
When 用户操作
Given 准备好数据
```

这些应被 `bdd lint` 标记为 vague。

---

## 12. Skills 改造

新增 Skill：

```text
.agents/skills/devharness-bdd-acceptance/
  SKILL.md
  references/
    bdd-protocol.md
    scenario-quality-rules.md
    evidence-format.md
    legacy-bdd-examples.md
  scripts/
    bdd-add.sh
    bdd-export.sh
    bdd-verify.sh
    bdd-evidence.sh
```

### 12.1 Skill 核心规则

```md
# DevHarness BDD Acceptance

Use this skill when a goal or spec requires behavior-driven acceptance.

Rules:
1. Do not implement before defining required behavior scenarios.
2. Every required scenario must have Given, When, Then.
3. Do not write vague Then steps such as "system works normally".
4. Bind scenarios to spec acceptance when available.
5. Bind scenarios to goal when working inside a goal.
6. Use graph impact for scenario if graph is enabled.
7. Do not claim completion until bdd verify passes.
8. If automation is unavailable, record manual evidence.
```

### 12.2 弱模型输出模板

```text
BDD self-check:
- scenarios_defined:
- acceptance_bound:
- graph_impact_done:
- evidence_status:
- bdd_verify:
- completion_allowed:
```

---

## 13. 测试方案

BDD 模块测试分为：

```text
1. 单元测试
2. 集成测试
3. Goal/Spec/Graph 组合测试
4. 老旧项目场景测试
5. 质量评估测试
```

---

## 14. 单元测试

### 14.1 BddKeyGeneratorTest

验证：

```text
中文标题生成稳定 key
重复场景生成唯一 key
非法字符清理
空标题拒绝
```

### 14.2 BddLintServiceTest

测试 vague steps：

```text
Then 系统正常 -> warning/error
Then 查询成功 -> warning
When 用户操作 -> warning
Given 准备数据 -> warning
```

测试有效 steps：

```text
Then 订单列表只显示客户名称包含 张三 的订单 -> ok
When 用户请求 GET /orders/1001 -> ok
```

### 14.3 BddCoverageServiceTest

验证：

```text
acceptance_total
acceptance_with_scenario
scenario_with_evidence
passed_scenario
coverage_percent
```

### 14.4 BddContextRendererTest

验证：

```text
BDD_CONTEXT.md 包含 goal
包含 scenario
包含 bindings
包含 evidence
包含 model instructions
敏感信息被拒绝或脱敏
```

---

## 15. 集成测试

### 15.1 BddAddExportIntegrationTest

流程：

```text
dhk bdd init
dhk bdd add
dhk bdd list
dhk bdd export
```

断言：

```text
BDD_CONTEXT.md 存在
.feature 文件存在
scenario step 顺序正确
```

### 15.2 BddBindSpecIntegrationTest

流程：

```text
spec create
spec acceptance add
bdd add
bdd bind-spec
bdd coverage
```

断言：

```text
acceptance_with_scenario = 1
coverage > 0
SPEC_CONTEXT 包含 scenario
```

### 15.3 BddBindGoalIntegrationTest

流程：

```text
goal start
bdd add
bdd bind-goal
bdd export --goal
goal verify
```

断言：

```text
goal verify 在无 evidence 时 not_ready
bdd evidence passed 后 goal verify 可通过 BDD 部分
```

### 15.4 BddEvidenceIntegrationTest

流程：

```text
bdd evidence --status passed
bdd verify
```

断言：

```text
scenario passed
evidence_path 记录
BDD_CONTEXT 显示 passed
```

---

## 16. Goal 集成测试

### 16.1 GoalRequiresBddScenarioTest

Profile：

```text
bddRequired = true
bddMinScenarioCount = 1
```

流程：

```text
goal start
goal verify
```

期望：

```text
not_ready: missing bdd scenario
```

添加 scenario 后：

```text
bdd add
bdd bind-goal
goal verify
```

期望：

```text
not_ready: missing bdd evidence
```

添加 evidence 后：

```text
bdd evidence --status passed
goal verify
```

期望：

```text
BDD check passed
```

### 16.2 GoalCompleteBlockedByBddTest

流程：

```text
goal complete
```

在 BDD scenario 未 passed 时必须失败。

### 16.3 GoalCompleteWithBddPassedTest

流程：

```text
scenario passed
checks fresh
workflow/spec passed
goal complete
```

断言：

```text
goal completed
BDD evidence included in GOAL_SUMMARY
spec acceptance verified
```

---

## 17. Graph 集成测试

### 17.1 GraphImpactByScenarioTest

流程：

```text
bdd add scenario
graph index
graph impact --scenario
```

断言：

```text
SCENARIO_IMPACT_MAP.md 存在
包含相关 Controller/Service/Mapper
scenario binding graph_impact 存在
```

### 17.2 GoalRequiresScenarioImpactTest

当 profile：

```text
bddRequired = true
graphRequired = true
```

如果没有 scenario impact：

```text
goal verify -> not_ready
```

---

## 18. 老旧项目场景测试

### 18.1 LegacyManualEvidenceTest

老旧项目无自动测试：

```text
bdd scenario
manual evidence passed
compile skipped 不允许
manual evidence passed
goal verify
```

验证：

```text
manual evidence 可作为 scenario evidence
但 compile 策略仍按 profile 执行
```

### 18.2 LegacyHistoricalBehaviorTest

场景：

```gherkin
Scenario: 状态 9 历史订单不出现在普通查询
```

验证：

```text
AI 修改后必须保留历史行为 scenario
goal verify 必须检查该 scenario evidence
```

### 18.3 LegacyRollbackPlanWithBddTest

要求：

```text
legacy profile 下 BDD passed + rollback plan 才允许 complete
```

---

## 19. 性能测试

BDD 性能指标：

| 指标 | 目标 |
|---|---:|
| bdd add p95 | < 1s |
| bdd export p95 | < 2s |
| bdd verify p95 | < 3s |
| bdd coverage p95 | < 3s |
| 1000 scenarios export | < 10s |
| 1000 scenarios lint | < 15s |

数据规模：

```text
小型项目：10 scenarios
中型项目：100 scenarios
大型项目：1000 scenarios
```

---

## 20. 质量指标

BDD-aware 后，DQI 增加 BDD 子指标。

### 20.1 BDD Quality Score

满分 20：

| 指标 | 分值 |
|---|---:|
| scenario coverage | 5 |
| Given/When/Then clarity | 4 |
| evidence binding | 4 |
| historical behavior coverage | 3 |
| graph impact binding | 2 |
| duplicate/vague issue control | 2 |

### 20.2 验收门槛

```text
scenario coverage >= 80%
required scenario evidence passed = 100%
vague Then blocking issue = 0
BDD_CONTEXT export success = 100%
legacy historical behavior scenario coverage >= 70%
```

---

## 21. 对照评估设计

新增 E 组：

| 组别 | 名称 |
|---|---|
| C | Goal Harness |
| D | Graph-aware Goal Harness |
| E | BDD + Graph-aware Goal Harness |

比较：

```text
E vs D：
- 验收清晰度是否提升
- 回归遗漏是否下降
- 老旧历史行为破坏率是否下降
- manual evidence 是否更完整
- 任务完成正确性是否提升
```

成功标准：

```text
E 组 DQI 比 D 组提升 >= 5 分
E 组 historical behavior preservation 提升 >= 15%
E 组 acceptance ambiguity 降低 >= 30%
E 组 manual evidence completeness >= 90%
```

---

## 22. 分阶段路线图

### V0.6.1 BDD Specification Layer

交付：

```text
schema v7
dhk bdd init/add/list/show/export
BDD_CONTEXT.md
.feature export
基础 lint
```

### V0.6.2 BDD + Spec/Goal Binding

交付：

```text
bdd bind-spec
bdd bind-goal
bdd evidence
bdd verify
bdd coverage
goal verify 集成 BDD
```

### V0.6.3 BDD + Graph Impact

交付：

```text
bdd bind-graph
graph impact --scenario
SCENARIO_IMPACT_MAP.md
goal profile bddRequired + graphRequired 联动
```

### V0.6.4 Executable BDD Adapter

交付：

```text
manual evidence adapter
JUnit/MockMvc adapter
Cucumber JVM adapter
Postman/Newman adapter
Playwright adapter
```

### V0.6.5 BDD Quality Gate

交付：

```text
duplicate scenario detection
vague step detection
scenario coverage gate
unbound scenario gate
BDD Quality Score
eval report integration
```

---

## 23. 文件级改造清单

### 23.1 新增包

```text
src/main/java/com/devharnesskit/dhk/model/bdd/
src/main/java/com/devharnesskit/dhk/repository/bdd/
src/main/java/com/devharnesskit/dhk/service/bdd/
src/main/java/com/devharnesskit/dhk/command/bdd/
```

### 23.2 修改现有类

```text
CommandRouter.java
MigrationRunner.java
GoalProfile.java
GoalProfileService.java
GoalCompletionEvaluator.java
GoalOrchestrator.java
SpecExportCommand.java
CurrentContextRenderer.java
JsonOutput.java
PathUtil.java
```

### 23.3 新增导出路径

```text
.agents/bdd/features/
.agents/bdd/evidence/
.agents/bdd/exports/BDD_CONTEXT.md
.agents/bdd/exports/BDD_COVERAGE.md
```

### 23.4 新增测试

```text
src/test/java/com/devharnesskit/dhk/service/bdd/
src/test/java/com/devharnesskit/dhk/integration/BddIntegrationTest.java
src/test/java/com/devharnesskit/dhk/integration/GoalBddIntegrationTest.java
src/test/java/com/devharnesskit/dhk/integration/GraphBddIntegrationTest.java
```

---

## 24. 风险与缓解

| 风险 | 说明 | 缓解 |
|---|---|---|
| BDD 场景膨胀 | 大量重复 scenario | duplicate check + scenario reuse |
| 场景不可执行 | Then 过于模糊 | bdd lint + vague step block |
| 老项目自动化困难 | 无测试框架 | manual evidence first |
| 与 Spec 重叠 | 两套验收概念混乱 | 明确 Spec=容器，BDD=行为例子 |
| 过早绑定 Cucumber | 增加复杂度 | 先 Specification BDD |
| 模型伪造 evidence | 弱模型可能写假证据 | evidence_path + check log + policy |
| 敏感信息进入 Given/Evidence | 老项目真实数据多 | SensitiveDataGuard + redaction/reject policy |

---

## 25. 最终验收标准

V0.6 BDD-aware Acceptance Harness 达成标准：

```text
1. 能创建、绑定、导出 BDD scenario。
2. Spec acceptance 能绑定至少一个 BDD scenario。
3. Goal verify 能检查 BDD scenario coverage 和 evidence。
4. Goal complete 能被 BDD pending evidence 阻止。
5. BDD_CONTEXT.md 能稳定指导弱模型。
6. 老旧项目 manual evidence 可被结构化记录。
7. Graph 可按 scenario 输出 impact map。
8. BDD lint 能识别 vague/duplicate/missing evidence。
9. 测试覆盖 BDD + Spec + Goal + Graph 组合路径。
10. 对照评估中，BDD + Graph-aware Goal Harness 比 Graph-aware Goal Harness 至少提升 5 分 DQI。
```

---

## 26. 总结

BDD-aware Acceptance Harness 是 DevHarnessKit 从“能执行任务”走向“能验证行为”的关键一层。

最终架构应形成：

```text
Memory：项目事实和历史约束
Spec：需求变更
BDD：行为场景和验收例子
Graph：代码结构和影响面
Goal：执行编排
Workflow：过程门禁
Check：自动/人工验证
Evidence：证据归档
Eval：质量评分
```

一句话目标：

```text
让 AI 每次改代码前先明确行为场景，
每次改代码后用证据证明场景成立，
并把这些行为规格沉淀为项目长期资产。
```
