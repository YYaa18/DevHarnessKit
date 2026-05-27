# DevHarnessKit Skill Contract & Governance 详细开发与测试方案

> 建议文件路径：`docs/DEVHARNESS_SKILL_CONTRACT_AND_GOVERNANCE_DETAILED_PLAN.md`
> 建议版本目标：`V0.7 Skill Contract & Governance`
> 适用背景：在 DevHarnessKit 已具备 `goal / workflow / spec / graph / bdd / eval` 方向后，将 Skills 从“提示词说明”升级为“可治理、可审计、可验证、可测试的执行契约”。

---

## 1. 设计背景

本方案研究并吸收两个外部 Skill 项目的设计思想：

```text
1. andrej-karpathy-skills
2. academic-research-skills
```

它们代表两种互补的 Skill 设计路线：

```text
andrej-karpathy-skills
= 极简行为纪律
= 解决 LLM 编码中的错误假设、过度设计、乱改代码、不验证完成

academic-research-skills
= 大型复杂任务治理流水线
= 解决多阶段任务中的数据权限、人工确认、证据、完整性门禁、审计和质量评分
```

DevHarnessKit 应吸收二者：

```text
Karpathy 风格：
- Think Before Coding
- Simplicity First
- Surgical Changes
- Goal-Driven Execution

Academic Research 风格：
- Stage Matrix
- Data Access Level
- Artifact Passport
- Integrity Gates
- Human Checkpoints
- Multi-agent / Multi-mode Pipeline
- Evidence & Claim Audit
- Score Trajectory / Benchmark Report
```

最终目标：

```text
将 Skill 从“模型看到的一段提示词”
升级为“DevHarnessKit 可验证、可治理、可阻断的执行契约”。
```

---

## 2. 总体定位

新增能力：

```text
Skill Contract & Governance Layer
```

它位于：

```text
AI Agent / Coding Client
        ↓
Skill Contract
        ↓
Goal / Workflow / Spec / Graph / BDD / Policy / Eval
        ↓
Project Code / DB / CI / Docs
```

职责：

```text
1. 定义模型行为纪律
2. 定义 Skill 可访问的数据级别
3. 定义允许和禁止的命令
4. 定义每个任务必须产出的 artifacts
5. 定义 hard gates 和 soft gates
6. 定义 evidence contract
7. 定义 human checkpoint
8. 定义 Skill 安装、信任、审计和安全策略
9. 定义 Skill 质量测试与回归评估
```

---

## 3. 核心设计原则

### 3.1 Skill 是执行契约，不只是 prompt

传统 Skill：

```text
SKILL.md = 一段说明
```

DevHarnessKit Skill Contract：

```text
SKILL.md + contract.json + policy.json + scripts + references + tests
```

每个 Skill 必须说明：

```text
1. 适用任务
2. 数据访问等级
3. 允许命令
4. 禁止命令
5. 必需 artifacts
6. 必需 checks
7. evidence 格式
8. 是否允许模型自动触发
9. 是否需要人工确认
10. 如何验收
```

---

### 3.2 软约束 + 硬门禁

Karpathy 风格规则不能只留在文本里。

例如：

```text
Surgical Changes
```

不应该只是 Skill 里写：

```text
不要乱改文件
```

还应该有 CLI 检查：

```bash
dhk skill gate surgical-change --goal <goal-key>
```

检查：

```text
每个 changed file 是否在 impact map 中
是否触碰 protected file
是否超出 change budget
是否存在大规模格式化
是否修改无关注释
```

---

### 3.3 不同任务不同治理强度

任务分层：

```text
low-risk:
  typo
  simple doc update
  one-line bugfix

medium-risk:
  java-api-change
  java-mvc-change
  test repair
  small refactor

high-risk:
  legacy-small-fix
  sql change
  auth/payment/order status
  config/deploy/security
```

治理策略：

```text
low-risk:
  lightweight skill + basic check

medium-risk:
  goal + graph + bdd optional + check

high-risk:
  goal + graph + bdd + policy + artifact passport + human checkpoint
```

---

### 3.4 不复制外部项目文本

尤其 `academic-research-skills` 使用 CC BY-NC 4.0。DevHarnessKit 可以学习设计思想，但不要直接复制其 prompt、agent 文件、schema 或文案。

本方案吸收的是架构模式：

```text
data access level
stage matrix
integrity gate
artifact passport
human checkpoint
score trajectory
```

而不是复制其内容。

---

## 4. Skill Contract 标准结构

建议每个 DevHarnessKit Skill 目录如下：

```text
.agents/skills/devharness-legacy-small-fix/
  SKILL.md
  contract.json
  policy.json
  references/
    protocol.md
    evidence-format.md
    examples.md
    risk-rules.md
  scripts/
    goal-start.sh
    goal-next.sh
    goal-step.sh
    goal-verify.sh
    skill-self-check.sh
  tests/
    fixtures/
    expected/
    skill-contract-test.json
```

---

## 5. Skill Contract Schema

新增文件：

```text
src/main/resources/schema/skill_contract.schema.json
```

建议结构：

```json
{
  "schema_version": "1",
  "skill_key": "devharness-legacy-small-fix",
  "version": "0.7.0",
  "task_type": "outcome-gradable",
  "risk_level": "high",
  "mode": "strict",
  "data_access_level": "raw",
  "model_invocation": {
    "auto_allowed": false,
    "user_invocable": true
  },
  "requires": {
    "goal": true,
    "graph": true,
    "bdd": true,
    "policy": true,
    "eval": true
  },
  "allowed_commands": [
    "dhk goal next",
    "dhk goal step",
    "dhk goal verify",
    "dhk graph impact",
    "dhk bdd verify"
  ],
  "forbidden_commands": [
    "dhk workflow gate waive",
    "dhk spec archive",
    "dhk memory confirm",
    "dhk db sql"
  ],
  "required_artifacts": [
    "GOAL_CONTEXT.md",
    "IMPACT_MAP.md",
    "BDD_CONTEXT.md",
    "ROLLBACK_PLAN.md",
    "GOAL_SUMMARY.md"
  ],
  "hard_gates": [
    "sensitive",
    "graph_freshness",
    "bdd_evidence",
    "change_budget",
    "surgical_change"
  ],
  "human_checkpoints": [
    "before_implementation",
    "before_completion"
  ],
  "evidence_contract": {
    "required_fields": [
      "summary",
      "changed_files",
      "impact_map",
      "verification_result",
      "rollback_plan"
    ]
  }
}
```

---

## 6. Data Access Level 设计

借鉴 Academic Research Skills 的 `raw / redacted / verified_only` 思想，为 DevHarnessKit 统一定义数据访问等级。

| 等级 | 含义 | 可访问内容 |
|---|---|---|
| raw | 原始数据 | 源码、diff、日志、SQL、用户输入 |
| redacted | 脱敏数据 | 脱敏后的 context、summary、evidence |
| verified_only | 已验证数据 | passed checks、confirmed memory、verified artifacts |

### 6.1 建议规则

```text
memory add:
  raw -> draft only

memory export:
  confirmed + redacted

goal start:
  raw task input, but output redacted context

goal verify:
  verified_only evidence + check logs

bdd complete:
  verified_only scenario evidence

eval report:
  redacted/verified only
```

### 6.2 Skill 使用

Skill Contract 中必须声明：

```json
{
  "data_access_level": "raw"
}
```

CI lint 检查：

```bash
dhk skill lint --check data-access-level
```

---

## 7. Task Type 设计

Skill 必须声明任务类型：

| task_type | 含义 |
|---|---|
| outcome-gradable | 有明确成功/失败标准 |
| open-ended | 开放研究/设计任务 |
| advisory | 只给建议，不执行修改 |
| risky-action | 可能触发外部副作用 |

示例：

```json
{
  "task_type": "outcome-gradable"
}
```

编码任务默认应为：

```text
outcome-gradable
```

原因：

```text
编码任务应有明确的 goal、checks、evidence、completion condition。
```

---

## 8. Karpathy Discipline Gates

吸收 `andrej-karpathy-skills` 的四条原则，将其转成 DevHarnessKit 的可检查门禁。

### 8.1 Think Before Coding Gate

目标：

```text
防止模型擅自解释需求、隐藏困惑、不做权衡。
```

检查内容：

```text
1. goal step 是否包含 assumptions
2. 不确定点是否记录为 pending/questions
3. 是否存在多个解释时的 tradeoff
4. 是否在 implementation 前完成 plan
```

CLI：

```bash
dhk skill gate think-before-coding --goal <goal-key>
```

### 8.2 Simplicity First Gate

目标：

```text
防止过度抽象、未请求功能、 speculative flexibility。
```

检查内容：

```text
1. 新增类/接口数量
2. 新增抽象层数量
3. 是否引入未要求配置项
4. 是否新增不必要 error handling
5. 是否超出 change budget
```

CLI：

```bash
dhk skill gate simplicity --goal <goal-key>
```

### 8.3 Surgical Changes Gate

目标：

```text
只改必要代码，不做 drive-by refactor。
```

检查内容：

```text
1. changed files 是否在 IMPACT_MAP 中
2. 是否触碰 protected files
3. 是否出现 formatting-only diff
4. 是否修改无关注释
5. 是否删除 pre-existing dead code
```

CLI：

```bash
dhk skill gate surgical-change --goal <goal-key>
```

### 8.4 Goal-Driven Execution Gate

目标：

```text
将任务转成可验证目标，并验证后完成。
```

检查内容：

```text
1. goal exists
2. current_action followed
3. fresh checks passed
4. bdd evidence passed
5. graph impact fresh
6. goal complete succeeded
```

CLI：

```bash
dhk skill gate goal-driven --goal <goal-key>
```

---

## 9. Academic Research Style Governance

吸收 `academic-research-skills` 的治理型设计。

### 9.1 Stage Matrix

每个 Goal Profile 应导出 stage matrix：

```text
Action
Skill / Mode
Data Access Level
Input Artifacts
Output Artifacts
Core Checks
Gate / Checkpoint
```

示例：

| Action | Data Level | Input | Output | Gate |
|---|---|---|---|---|
| inspect_existing_code | raw | GOAL_CONTEXT | inspection evidence | Think Before Coding |
| graph_impact_analysis | raw | graph snapshot | IMPACT_MAP | Graph Freshness |
| define_bdd_scenarios | redacted | spec acceptance | BDD_CONTEXT | BDD Coverage |
| implement_minimal_change | raw | plan + impact | diff | Surgical Change |
| verify | verified_only | check logs | verification report | Final Integrity |
| complete | verified_only | all artifacts | passport + summary | Human Checkpoint |

### 9.2 Integrity Gates

新增两个统一门禁：

```text
Pre-Implementation Integrity Gate
Final Integrity Gate
```

#### Pre-Implementation Integrity Gate

必须在写代码前通过：

```text
goal context ready
graph impact ready
bdd scenarios defined if required
spec acceptance exists
protected files known
change budget declared
assumptions recorded
```

#### Final Integrity Gate

完成前必须通过：

```text
fresh checks passed
graph re-impact passed
bdd evidence passed
no sensitive leaks
no protected file violation
rollback plan exists
workflow/spec/goal consistent
artifact passport generated
```

### 9.3 Human Checkpoints

对于高风险任务：

```text
before_implementation
before_completion
before_db_access
before_protected_file_change
```

CLI：

```bash
dhk skill checkpoint request --goal <goal-key> --type before_implementation
dhk skill checkpoint approve --goal <goal-key> --checkpoint <id>
```

---

## 10. Artifact Passport

新增：

```text
ARTIFACT_PASSPORT.json
```

生成位置：

```text
.agents/artifacts/<goal-key>/ARTIFACT_PASSPORT.json
```

示例：

```json
{
  "goal_key": "202605-order-query",
  "task": "实现订单查询按客户名称过滤",
  "profile": "legacy-java-small-fix",
  "git_commit_before": "abc123",
  "git_commit_after": "def456",
  "graph_snapshot": "graph-snapshot-001",
  "bdd_scenarios": [
    "order-query.customer-name"
  ],
  "changed_files": [
    "OrderController.java",
    "OrderMapper.xml"
  ],
  "checks": [
    {
      "key": "compile",
      "status": "passed",
      "fresh": true
    }
  ],
  "policy_results": [
    {
      "gate": "surgical_change",
      "status": "passed"
    }
  ],
  "evidence": [
    "BDD_EVIDENCE.md",
    "CHECK_LOGS/compile.log"
  ],
  "rollback_plan": "ROLLBACK_PLAN.md",
  "sensitive_scan": "passed",
  "created_at": "2026-05-26T00:00:00Z"
}
```

---

## 11. 新增 CLI 设计

### 11.1 skill namespace

```bash
dhk skill list
dhk skill show --skill <key>
dhk skill lint --skill <key>
dhk skill verify --skill <key>
dhk skill install --path <path>
dhk skill trust --skill <key>
dhk skill package --skill <key>
```

### 11.2 skill gate namespace

```bash
dhk skill gate think-before-coding --goal <goal>
dhk skill gate simplicity --goal <goal>
dhk skill gate surgical-change --goal <goal>
dhk skill gate goal-driven --goal <goal>
dhk skill gate integrity-pre --goal <goal>
dhk skill gate integrity-final --goal <goal>
```

### 11.3 artifact namespace

```bash
dhk artifact passport --goal <goal>
dhk artifact list --goal <goal>
dhk artifact verify --goal <goal>
```

### 11.4 checkpoint namespace

```bash
dhk checkpoint request --goal <goal> --type before_implementation
dhk checkpoint approve --goal <goal> --id <id>
dhk checkpoint reject --goal <goal> --id <id>
```

---

## 12. 数据库 Schema 设计

建议新增 schema v8。

### 12.1 skill_contract

```sql
CREATE TABLE skill_contract (
  skill_key TEXT PRIMARY KEY,
  version TEXT NOT NULL,
  task_type TEXT NOT NULL,
  risk_level TEXT NOT NULL,
  mode TEXT NOT NULL,
  data_access_level TEXT NOT NULL,
  contract_json TEXT NOT NULL,
  source_path TEXT NOT NULL DEFAULT '',
  trusted INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);
```

### 12.2 skill_gate_result

```sql
CREATE TABLE skill_gate_result (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  goal_key TEXT NOT NULL,
  skill_key TEXT NOT NULL,
  gate_key TEXT NOT NULL,
  status TEXT NOT NULL,
  severity TEXT NOT NULL DEFAULT 'info',
  summary TEXT NOT NULL,
  evidence_path TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL
);
```

### 12.3 artifact_passport

```sql
CREATE TABLE artifact_passport (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  goal_key TEXT NOT NULL,
  passport_path TEXT NOT NULL,
  passport_hash TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'created',
  created_at TEXT NOT NULL
);
```

### 12.4 human_checkpoint

```sql
CREATE TABLE human_checkpoint (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  goal_key TEXT NOT NULL,
  checkpoint_type TEXT NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending',
  reason TEXT NOT NULL DEFAULT '',
  approved_by TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL,
  resolved_at TEXT NOT NULL DEFAULT ''
);
```

---

## 13. Java 包结构

### 13.1 Model

```text
src/main/java/com/devharnesskit/dhk/model/skill/
  SkillContract.java
  SkillGateResult.java
  SkillGateEvaluation.java
  ArtifactPassport.java
  HumanCheckpoint.java
```

### 13.2 Repository

```text
src/main/java/com/devharnesskit/dhk/repository/skill/
  SkillContractRepository.java
  SkillGateResultRepository.java
  ArtifactPassportRepository.java
  HumanCheckpointRepository.java
```

### 13.3 Service

```text
src/main/java/com/devharnesskit/dhk/service/skill/
  SkillContractService.java
  SkillContractLoader.java
  SkillContractValidator.java
  SkillTrustService.java
  SkillGateService.java
  ThinkBeforeCodingGate.java
  SimplicityGate.java
  SurgicalChangeGate.java
  GoalDrivenGate.java
  IntegrityGateService.java
  ArtifactPassportService.java
  HumanCheckpointService.java
```

### 13.4 Command

```text
src/main/java/com/devharnesskit/dhk/command/skill/
  SkillCommand.java
  SkillListCommand.java
  SkillShowCommand.java
  SkillLintCommand.java
  SkillVerifyCommand.java
  SkillGateCommand.java
  SkillTrustCommand.java
  ArtifactPassportCommand.java
  HumanCheckpointCommand.java
```

---

## 14. Goal 集成

Goal Profile 增加：

```json
{
  "skill_contract_required": true,
  "skill_key": "devharness-legacy-small-fix",
  "pre_implementation_gate_required": true,
  "final_integrity_gate_required": true,
  "artifact_passport_required": true,
  "human_checkpoint_required": true
}
```

Goal verify 时必须执行：

```text
1. skill contract lint
2. pre/final integrity gate
3. behavior discipline gates
4. required artifacts check
5. human checkpoint check
```

Goal complete 前：

```text
1. Artifact Passport must exist
2. Final Integrity Gate must pass
3. Human checkpoint must be approved for high-risk goals
```

---

## 15. Policy 集成

Policy 增加：

```json
{
  "skillPolicy": {
    "requireTrustedSkills": true,
    "blockUntrustedSkillScripts": true,
    "requireDataAccessLevel": true,
    "requireTaskType": true,
    "maxSkillMainLines": 500,
    "forbidAutoInvocationForRiskyActions": true
  }
}
```

---

## 16. Skill Trust 设计

考虑第三方 Skill 风险，新增信任策略。

### 16.1 信任状态

```text
untrusted
reviewed
trusted
blocked
```

### 16.2 检查项

```text
source path
repo URL
license
hash
allowed tools
scripts
dangerous commands
network access
write access
shell commands
```

### 16.3 命令

```bash
dhk skill trust --skill devharness-legacy-small-fix
dhk skill block --skill external-risky-skill
dhk skill audit --path .agents/skills/*
```

---

## 17. Skills 改造建议

### 17.1 devharness-strict-coding

吸收 Karpathy 风格纪律。

```text
Think Before Coding
Simplicity First
Surgical Changes
Goal-Driven Execution
```

### 17.2 devharness-legacy-small-fix

高风险老旧项目改造 skill。

```text
requires:
- goal
- graph
- bdd
- policy
- artifact passport
- human checkpoint
```

### 17.3 devharness-goal-integrity

治理型 skill。

```text
pre-implementation gate
final integrity gate
artifact passport
state consistency
```

### 17.4 devharness-eval-quality

评估型 skill。

```text
DQI score
impact recall/precision
change quality
evidence quality
safety quality
cost efficiency
```

---

## 18. 测试方案

### 18.1 单元测试

#### SkillContractValidatorTest

验证：

```text
missing data_access_level -> fail
missing task_type -> fail
invalid command allowlist -> fail
risky skill with auto invocation -> fail
required artifacts empty -> warning/fail by mode
```

#### DataAccessLevelTest

验证：

```text
raw / redacted / verified_only 合法
非法值拒绝
高风险 skill 不能省略 data_access_level
```

#### SkillGateServiceTest

验证：

```text
think-before-coding gate
simplicity gate
surgical-change gate
goal-driven gate
```

#### ArtifactPassportServiceTest

验证：

```text
passport 包含 goal/checks/graph/bdd/evidence
hash 计算稳定
敏感信息拒绝
```

#### HumanCheckpointServiceTest

验证：

```text
request
approve
reject
pending blocks complete
resolved checkpoint not repeated
```

---

### 18.2 集成测试

#### SkillContractIntegrationTest

流程：

```text
install skill
lint skill
verify skill
trust skill
```

断言：

```text
contract persisted
trusted status updated
lint result correct
```

#### GoalWithSkillContractIntegrationTest

流程：

```text
goal start --profile legacy-small-fix
skill contract required
goal verify
```

断言：

```text
missing skill gate -> not_ready
all gates passed -> ready_to_complete
```

#### SurgicalChangeGateIntegrationTest

准备：

```text
IMPACT_MAP.md 包含 A.java
diff 修改 A.java 和 B.java
```

期望：

```text
B.java unrelated -> gate failed
```

#### SimplicityGateIntegrationTest

准备：

```text
新增 6 个类
新增未请求 interface
```

期望：

```text
simplicity gate warning/fail
```

#### HumanCheckpointBlocksCompleteTest

高风险 goal：

```text
before_completion checkpoint pending
```

期望：

```text
goal complete failed
```

批准后：

```text
goal complete succeeded
```

#### ArtifactPassportIntegrationTest

流程：

```text
goal complete
artifact passport generated
```

断言：

```text
ARTIFACT_PASSPORT.json exists
contains check results
contains bdd evidence
contains impact map
contains rollback plan
```

---

### 18.3 安全测试

#### UntrustedSkillScriptBlockedTest

场景：

```text
untrusted skill contains scripts/deploy.sh
policy blockUntrustedSkillScripts=true
```

期望：

```text
skill verify failed
script execution blocked
```

#### DangerousCommandInSkillTest

Skill 包含：

```text
rm -rf
git push --force
curl | bash
kubectl delete
```

期望：

```text
skill audit warning/fail
```

#### SensitiveSkillContentTest

Skill 或 reference 中包含：

```text
password=
JDBC URL
Bearer token
private key
```

期望：

```text
skill lint failed
```

---

### 18.4 E2E 对照测试

新增 F 组：

| 组别 | 名称 |
|---|---|
| D | Graph-aware Goal Harness |
| E | BDD + Graph-aware Goal Harness |
| F | Skill Contract + BDD + Graph-aware Goal Harness |

比较目标：

```text
F 是否比 E 更少误改、更少过度设计、更少绕过流程。
```

指标：

```text
DQI
unrelated_diff_ratio
protected_file_violation_rate
change_budget_violation_rate
manual_intervention_count
goal_complete_false_positive_rate
```

成功标准：

```text
F 组 DQI 比 E 组提升 >= 5 分
F 组 unrelated diff 降低 >= 30%
F 组 protected file violation = 0
F 组 goal complete false positive = 0
```

---

## 19. 性能指标

| 指标 | 目标 |
|---|---:|
| skill lint p95 | < 2s |
| skill verify p95 | < 3s |
| gate surgical-change p95 | < 5s |
| artifact passport generation p95 | < 3s |
| 100 skills audit p95 | < 30s |

---

## 20. 质量指标

Skill Contract Quality Score，满分 100。

| 指标 | 权重 |
|---|---:|
| contract completeness | 20 |
| data access clarity | 15 |
| command safety | 15 |
| artifact/evidence clarity | 15 |
| gate coverage | 15 |
| skill conciseness | 10 |
| test coverage | 10 |

评级：

```text
90-100: production ready
80-89: internal beta
70-79: experimental
<70: not allowed
```

---

## 21. 分阶段路线图

### V0.7.1 Skill Contract MVP

交付：

```text
contract.json schema
dhk skill lint
dhk skill verify
data_access_level
task_type
allowed/forbidden commands
```

### V0.7.2 Karpathy Discipline Gates

交付：

```text
ThinkBeforeCodingGate
SimplicityGate
SurgicalChangeGate
GoalDrivenGate
strict coding skill
```

### V0.7.3 Artifact Passport

交付：

```text
ARTIFACT_PASSPORT.json
artifact passport service
goal complete integration
passport verify
```

### V0.7.4 Integrity Gates

交付：

```text
PreImplementationIntegrityGate
FinalIntegrityGate
human checkpoints
goal verify integration
```

### V0.7.5 Skill Trust & Security

交付：

```text
skill trust
skill audit
untrusted script block
dangerous command scanner
license metadata
source hash
```

### V0.7.6 Evaluation Integration

交付：

```text
skill quality score
DQI integration
F group evaluation
benchmark report
score trajectory
```

---

## 22. 验收标准

V0.7 完成标准：

```text
1. 每个 DevHarness Skill 都有 contract.json。
2. skill lint 能拒绝缺少 data_access_level/task_type 的 skill。
3. strict coding skill 能执行 Karpathy discipline gates。
4. high-risk goal complete 受 human checkpoint 阻断。
5. Artifact Passport 在 goal complete 时生成。
6. Final Integrity Gate 能检查 fresh checks、graph、bdd、policy、passport。
7. untrusted skill script 默认不能执行。
8. dangerous command 能被 skill audit 识别。
9. E2E 测试中 F 组相对 E 组 DQI 至少提升 5 分。
10. protected file violation 和 sensitive leak 均为 0。
```

---

## 23. 风险与缓解

| 风险 | 说明 | 缓解 |
|---|---|---|
| 过度复杂 | Skill Contract 过重，影响简单任务 | risk_level 分层 |
| 模型仍绕过 | Skill 是文本，模型可忽略 | goal verify + policy hard gate |
| 第三方 Skill 风险 | 外部 skill 可能恶意或过时 | trust policy + audit + hash |
| 许可证风险 | 外部 skill 不可商业使用 | 只学设计，不复制内容 |
| Gate 误报 | surgical/simplicity 难自动判断 | warning/fail 分级 + human override |
| 性能下降 | 多 gate 增加耗时 | 按 risk level 启用 |
| 维护成本 | 每个 skill 多 contract/test | 提供 generator 和 lint |

---

## 24. 建议新增生成器

为了降低维护成本，新增：

```bash
dhk skill generate   --name devharness-legacy-small-fix   --risk high   --requires goal,graph,bdd,policy
```

生成：

```text
SKILL.md
contract.json
policy.json
references/
scripts/
tests/
```

---

## 25. 总结

本方案的目标不是增加更多提示词，而是让 Skill 进入工程化治理体系。

最终 DevHarnessKit 的 Skill 应具备：

```text
可发现
可验证
可审计
可阻断
可评分
可回归测试
可被弱模型稳定执行
```

一句话目标：

```text
把 Karpathy 风格的编码纪律，
和 Academic Research Skills 风格的阶段、证据、门禁、审计体系，
融合成 DevHarnessKit 的 Skill Contract & Governance Layer。
```

---

## 26. 参考资料

- multica-ai/andrej-karpathy-skills: https://github.com/multica-ai/andrej-karpathy-skills
- Imbad0202/academic-research-skills: https://github.com/Imbad0202/academic-research-skills
- Claude Code Skills documentation: https://code.claude.com/docs/en/skills
- Claude Code Hooks documentation: https://code.claude.com/docs/en/hooks-guide
- Agentic coding manifests empirical study: https://arxiv.org/abs/2509.14744
- Agent skill ecosystem security analysis: https://arxiv.org/abs/2603.16572
