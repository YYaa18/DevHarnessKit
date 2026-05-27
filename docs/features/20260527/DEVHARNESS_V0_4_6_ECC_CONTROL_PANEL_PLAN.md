# DevHarnessKit V0.4.6 收敛方案：ECC 精华吸收 + Agent 控制面板 + 可治理安装

## 1. 总结

本阶段目标不是继续扩展 Graph parser 或新增更多技能，而是把 DevHarnessKit 的使用入口收敛为：

```text
configure -> agent install -> goal start -> goal next -> goal step -> goal verify -> goal complete
```

并从 ECC 项目中吸收对 DevHarnessKit 有价值的工程设计：

```text
1. Cross-harness source/adapters 架构
2. Manifest-driven install
3. Plan / apply 双阶段安装
4. Install state / doctor / repair / uninstall
5. Status / readiness 快照
6. 多模型 advisory-only 协作
7. Stop-loss phase
8. Project-scoped learning
9. Verification report
10. Security as infrastructure
```

不吸收 ECC 的重型多智能体平台方向，不引入 daemon/dashboard/operator，也不引入大量通用技能包。DevHarnessKit 的边界仍然是：

```text
公司内部 AI 编码 Harness
local-first
goal-first
graph-aware when required
policy/hook governed
verification-driven
```

---

## 2. 从 ECC 吸收的精华设计

### 2.1 Cross-harness source/adapters

ECC 的关键思想是：workflow 的 source of truth 应只有一份，不同 Agent 工具只是 execution surface。

DevHarnessKit 应采用：

```text
Source of truth:
.agents/skills/devharness-goal-development/
.agents/skills/devharness-graph-aware-development/
.agents/devharness/config.json
.agents/devharness/policy.json
.agents/graph/config.json

Generated adapters:
.claude/skills/
AGENTS.md
.comate/rules/
CLAUDE.md
```

原则：

```text
不要维护三套手写规则。
不要让 Claude/OpenCode/Comate 各自拥有不同协议。
所有适配内容都由 dhk agent install 或安装脚本生成。
```

---

### 2.2 Manifest-driven install

新增：

```text
.agents/devharness/agent-manifest.json
```

示例：

```json
{
  "schema_version": "devharness-agent-manifest/v1-alpha",
  "default_skill": "devharness-goal-development",
  "conditional_skills": [
    {
      "name": "devharness-graph-aware-development",
      "when": "GOAL_CONTEXT contains graph_required=true"
    }
  ],
  "targets": {
    "claude": {
      "enabled": true,
      "install_skills": true,
      "write_claude_md": true
    },
    "opencode": {
      "enabled": true,
      "write_agents_md": true
    },
    "comate": {
      "enabled": true,
      "write_rules": true
    }
  },
  "legacy": {
    "remove_memory_first_skill": true,
    "remove_memory_first_comate_rules": true
  }
}
```

---

### 2.3 Plan / Apply 双阶段

安装不应只有“直接写文件”，应拆成：

```bash
dhk agent plan --target all --json
dhk agent install --target all
```

脚本也应支持：

```bash
./scripts/devharness-control-panel.sh plan --target all
./scripts/devharness-control-panel.sh install --target all
```

Plan 输出：

```text
will_write
will_copy
will_link
will_remove
will_chmod
will_install_jar
warnings
```

Apply 执行：

```text
写入 adapters
删除旧入口
安装 jar
设置 chmod
记录 install-state
```

---

### 2.4 Install State

新增：

```text
.agents/devharness/install-state.json
```

用途：

```text
dhk agent doctor
dhk agent repair
dhk agent uninstall --dry-run
dhk status
```

示例：

```json
{
  "schema_version": "devharness-install-state/v1-alpha",
  "installed_at": "2026-05-27T00:00:00Z",
  "installed_by": "devharness-control-panel.sh",
  "targets": ["claude", "opencode", "comate"],
  "mode": "copy",
  "files": [
    {
      "destination": ".claude/skills/devharness-goal-development/SKILL.md",
      "source": ".agents/skills/devharness-goal-development/SKILL.md",
      "managed": true,
      "sha256": "<sha256>"
    }
  ],
  "removed_legacy": [
    ".agents/skills/devharness-java-development",
    ".comate/rules/project-memory-bootstrap.mdr",
    ".comate/rules/java-development-guard.mdr"
  ]
}
```

---

### 2.5 Status / Readiness 快照

吸收 ECC `status` 的思路，新增：

```bash
dhk status --markdown --write .agents/devharness/status.md
dhk status --json
dhk status --exit-code
```

输出：

```text
agent_adapters: ok / missing / drifted
config: ok / missing / invalid
goal: none / active / blocked / ready_to_complete
graph: fresh / stale / missing
verification: auto / manual_required / manual_passed / failed
policy: strict / guided / expert
install_state: managed / missing / drifted
```

控制面板脚本也应有：

```bash
./scripts/devharness-control-panel.sh status
```

---

### 2.6 Multi-model advisory-only

不吸收多 Agent 并行改代码，只吸收“咨询型多模型”：

```bash
dhk goal consult --role architect
dhk goal consult --role reviewer
dhk goal consult --role tester
dhk goal consult --role sql-reviewer
```

原则：

```text
外部模型只给建议。
外部模型不写文件。
外部模型不推进 goal。
外部模型不 pass gate。
外部模型不 complete。
主 Agent / DevHarness goal flow 才能推进状态。
```

---

### 2.7 Stop-loss phase

每个 goal action 应有 stop-loss 条件：

```json
{
  "action": "graph_impact_analysis",
  "stop_loss": {
    "required_artifacts": ["IMPACT_MAP.md"],
    "required_checks": ["graph", "impact"],
    "cannot_continue_to": ["implement_minimal_change"]
  }
}
```

效果：

```text
缺 graph impact 不能进入代码编辑。
缺 required evidence 不能进入 verify。
缺 manual verification 不能 complete。
```

---

### 2.8 Project-scoped learning

吸收 ECC continuous learning 的 project-scoped 思路，但不自动确认 memory。

新增：

```bash
dhk learn suggest --from-goal <goal-key>
dhk learn review
dhk learn promote
```

规则：

```text
建议可以自动生成。
长期事实不能自动确认。
project-scoped suggestions 不自动污染全局。
只有人工确认后才能进入 confirmed memory 或 profile。
```

---

### 2.9 Verification report

`goal verify` 应输出结构化报告，而不是只输出 ready/not_ready：

```text
VERIFICATION REPORT
Graph: PASS / FAIL / STALE
Impact: PASS / FAIL / MISSING
Compile: AUTO_PASS / MANUAL_REQUIRED / MANUAL_PASS / DISABLED
Test: AUTO_PASS / MANUAL_REQUIRED / MANUAL_PASS / WAIVED_WITH_RISK
Sensitive: PASS / FAIL
Architecture: PASS / WARN / FAIL
Workflow: PASS / FAIL
Spec: PASS / FAIL
Overall: READY / NOT_READY

Missing Evidence:
- manual_evidence_status=passed
- test_scope
- manual_evidence_path

Next Command:
dhk goal step ...
```

---

### 2.10 Security as infrastructure

安全边界不靠 prompt，而靠 policy/hook：

```text
BeforeGoalStep
BeforeGoalVerify
BeforeGraphImpact
BeforeDbSql
BeforeContextExport
BeforeAgentInstall
```

新增：

```bash
dhk policy doctor
dhk policy explain
dhk policy audit
```

---

## 3. 控制面板脚本设计

新增脚本：

```text
scripts/devharness-control-panel.sh
```

定位：

```text
一站式本地控制面板
用于配置、安装、诊断、状态、卸载和修复
```

不是 daemon，不启动服务，不提供 Web UI。

### 3.1 支持命令

```bash
./scripts/devharness-control-panel.sh configure
./scripts/devharness-control-panel.sh plan
./scripts/devharness-control-panel.sh install
./scripts/devharness-control-panel.sh status
./scripts/devharness-control-panel.sh doctor
./scripts/devharness-control-panel.sh repair
./scripts/devharness-control-panel.sh uninstall
```

### 3.2 参数

```text
--project-root <path>
--target claude|opencode|comate|all
--preset <preset>
--mode copy|link
--jar <path>
--force
--dry-run
--remove-legacy
--no-remove-legacy
--compile-mode auto|manual|disabled
--test-mode auto|manual|disabled
--graph required|optional|off
--allow-stale-policy approval|required|expert|off
--status-format text|json|markdown
```

### 3.3 控制面板目标

用户不需要手动修改：

```text
.agents/devharness/config.json
.agents/devharness/policy.json
.agents/graph/config.json
AGENTS.md
CLAUDE.md
.comate/rules/*.mdr
.claude/skills/*
```

而是运行：

```bash
./scripts/devharness-control-panel.sh configure \
  --preset springboot-manual-ide-test \
  --target all \
  --graph required \
  --compile-mode manual \
  --test-mode manual \
  --force
```

---

## 4. Verification Policy 收敛

### 4.1 关键设计

项目类型不决定验证方式。验证方式由 verification policy 决定。

```text
项目可以是 Spring Boot，但 test.mode=manual。
项目可以是 legacy，但 test.mode=auto。
```

不要再用：

```text
legacy = 跳过测试
modern = 必须 mvn test
```

而是使用：

```text
compile.mode = auto | manual | disabled
test.mode = auto | manual | disabled
```

### 4.2 Spring Boot 手工 IDE 测试项目

适用：

```text
Spring Boot 大项目
本地 mvn test 因公司环境失败
只能从 IDE 测试按钮触发
单个测试耗时十几分钟
```

配置：

```json
{
  "verification": {
    "compile": {
      "mode": "manual",
      "manual_trigger": "IDE build action"
    },
    "test": {
      "mode": "manual",
      "manual_trigger": "IDE test button",
      "expected_duration": "10m+ per test"
    }
  }
}
```

Goal verify 行为：

```text
不运行 mvn test。
缺人工证据时 not_ready。
有 manual_evidence_status=passed + test_scope + manual_evidence_path 后通过 manual-test。
```

---

## 5. 正式版文件结构

```text
.agents/
  devharness/
    config.json
    policy.json
    install-state.json
    agent-manifest.json
    status.md
  graph/
    config.json
  skills/
    devharness-goal-development/
    devharness-graph-aware-development/

.claude/
  CLAUDE.md
  skills/
    devharness-goal-development/
    devharness-graph-aware-development/

.comate/
  rules/
    devharness-goal-protocol.mdr
    devharness-graph-aware-protocol.mdr

AGENTS.md
scripts/
  devharness-control-panel.sh
  install-agent-adapters.sh
```

删除：

```text
.agents/skills/devharness-java-development
.comate/rules/project-memory-bootstrap.mdr
.comate/rules/java-development-guard.mdr
```

---

## 6. 分阶段落地

### P0

```text
1. 删除旧 memory-first skill。
2. 新增 devharness-control-panel.sh。
3. 新增 dhk agent plan/install/doctor/repair/uninstall。
4. 新增 install-state.json。
5. 新增 dhk configure init/show/doctor/explain。
6. 新增 verification config。
7. GoalCheckService 支持 compile/test manual mode。
8. GOAL_CONTEXT 输出 verification-policy。
9. agent install 生成 CLAUDE.md / AGENTS.md / .comate rules / .claude skills。
```

### P1

```text
1. dhk status。
2. goal verify verification report。
3. agent repair。
4. install drift detection。
5. configure presets。
6. policy doctor/explain。
```

### P2

```text
1. goal consult advisory-only。
2. learn suggest/review/promote。
3. stop-loss profile schema。
4. enterprise preset registry。
```

---

## 7. 最终用户体验

### 大型 Spring Boot 手工验证项目

```bash
./scripts/devharness-control-panel.sh configure \
  --preset springboot-manual-ide-test \
  --target all \
  --graph required \
  --compile-mode manual \
  --test-mode manual \
  --force

./scripts/devharness-control-panel.sh install --target all --force

.agents/skills/devharness-goal-development/scripts/goal-start.sh \
  --profile auto \
  --task "修复订单分页边界" \
  --module order
```

### Agent 看到

```text
current_action
allowed_commands
verification_policy
manual_verification_contract
graph_required
next_command
```

### 用户验证

用户从 IDE 点击测试按钮后记录：

```bash
goal-step.sh \
  --goal <goal-key> \
  --summary "IDE test passed" \
  --changed-files "src/main/java/..." \
  --evidence "manual_evidence_status=passed; test_scope=OrderServiceTest#queryWithNullPageBounds; manual_evidence_path=.agents/verification/<goal>/ide-test-result.md"
```

然后：

```bash
goal-verify.sh --goal <goal-key>
goal-complete.sh --goal <goal-key>
```

---

## 8. 最终判断

从 ECC 吸收的不是多智能体复杂度，而是：

```text
统一 source
多工具 adapter
manifest-driven install
plan/apply/state
readiness status
stop-loss phase
advisory-only multi-model
project-scoped learning
verification report
policy as infrastructure
```

这会让 DevHarnessKit 从“能力已经闭环”进入“用户能顺利接入并长期维护”的阶段。
