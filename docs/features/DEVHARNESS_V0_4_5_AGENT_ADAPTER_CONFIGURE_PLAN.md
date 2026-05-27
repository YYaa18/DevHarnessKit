# DevHarnessKit 收敛方案：Agent Adapter Packaging + Verification Policy

版本建议：`V0.4.5 Agent Adapter Packaging & Verification Policy`

## 1. 背景与结论

当前 DevHarnessKit 的核心能力已经从早期 memory-first 发展为：

```text
goal-first + graph-aware when required
```

正式版本中不建议继续保留过时入口。旧技能会增加模型选择成本，尤其会让弱模型在 `memory-first` 与 `goal-first` 之间走错流程。因此正式版本应直接移除旧技能：

```text
删除：
.agents/skills/devharness-java-development/

保留：
.agents/skills/devharness-goal-development/
.agents/skills/devharness-graph-aware-development/
```

对外只暴露一个主入口：

```text
DevHarness Goal Protocol
```

当 `GOAL_CONTEXT.md` 中出现 `graph_required=true` 或 profile 是 `*-with-graph` 时，再启用：

```text
DevHarness Graph-Aware Protocol
```

用户不应该理解或手工选择大量底层命令；模型也不应该自己组合 `memory / workflow / spec / db / graph` 命令。Goal 的目的就是让流程变成：

```text
goal start/resume
goal next
read GOAL_CONTEXT
perform current_action
goal step
goal verify
goal complete
```

---

## 2. 技能收敛策略

### 2.1 正式版技能目录

建议最终保留：

```text
.agents/skills/
  devharness-goal-development/
    SKILL.md
    scripts/
    references/

  devharness-graph-aware-development/
    SKILL.md
    scripts/
    references/
```

删除：

```text
.agents/skills/devharness-java-development/
```

### 2.2 Skill 使用规则

默认使用 `devharness-goal-development`：

```text
1. 不允许先编辑代码。
2. 必须启动或恢复 goal。
3. 每轮必须 goal next。
4. 必须读取 GOAL_CONTEXT.md。
5. 只执行 current_action。
6. 每轮结束必须 goal step。
7. 完成前必须 goal verify。
8. 只有 ready_to_complete 后才能 goal complete。
```

Graph-required 任务才使用 `devharness-graph-aware-development`：

```text
1. GOAL_CONTEXT 要求 graph_index_export 时，先执行 graph index/export。
2. GOAL_CONTEXT 要求 graph_impact 时，生成 IMPACT_MAP.md。
3. 编辑前必须读取 GRAPH_CONTEXT.md 和 IMPACT_MAP.md。
4. 不允许编辑 impact map 之外的文件，除非重新 impact 并记录风险。
5. 改动后必须 graph_reimpact。
6. 不允许自行使用 --allow-stale。
7. allow-stale 必须有 policy 或人工 evidence。
```

---

## 3. Agent 工具适配目标

### 3.1 Claude Code

Claude Code 项目级 skills 期望目录：

```text
.claude/skills/<skill-name>/SKILL.md
```

安装器应生成：

```text
.claude/
  CLAUDE.md
  skills/
    devharness-goal-development/
    devharness-graph-aware-development/
```

`CLAUDE.md` 应保持简短，只负责把 Claude Code 引到 goal protocol：

```md
# DevHarnessKit

This repository uses DevHarnessKit.

For any code change:
1. Use `/devharness-goal-development`.
2. Start or resume a goal.
3. Run goal-next before every work step.
4. Read `.agents/memory/exports/GOAL_CONTEXT.md`.
5. Perform only the current_action.
6. Complete only after goal verify returns ready_to_complete.

If GOAL_CONTEXT shows graph_required=true, use `/devharness-graph-aware-development`.

Do not bypass goal with direct memory/workflow/spec/db commands.
```

### 3.2 OpenCode

OpenCode 主要通过项目根目录 `AGENTS.md` 读取规则。安装器应生成：

```text
AGENTS.md
```

内容应明确：

```text
所有代码任务走 DevHarness goal protocol。
不要先编辑代码。
不要绕过 goal。
Graph-required 时使用 graph-aware 协议。
```

### 3.3 Comate

当前 `.comate/rules` 中旧规则偏 memory-first，应替换为 goal-first：

```text
.comate/rules/devharness-goal-protocol.mdr
.comate/rules/devharness-graph-aware-protocol.mdr
```

并移除或覆盖旧规则：

```text
.comate/rules/project-memory-bootstrap.mdr
.comate/rules/java-development-guard.mdr
```

正式版不建议保留旧 rules，避免 Comate 把模型带回旧流程。

---

## 4. 快速部署脚本目标

新增脚本：

```text
scripts/install-agent-adapters.sh
```

目标：

```bash
./scripts/install-agent-adapters.sh \
  --project-root . \
  --target all \
  --force
```

支持参数：

```text
--project-root <path>      目标项目根目录，默认当前目录
--target <tool|all>        claude | opencode | comate | all
--mode <copy|link>         默认 copy；内部开发仓可用 link
--jar <path>               可选，把指定 dhk.jar 安装到 .agents/tools/devharness-kit/dhk.jar
--force                    覆盖已有 AGENTS.md / CLAUDE.md / .comate rules / .claude skills
--dry-run                  只打印将执行动作，不写文件
--remove-legacy            删除旧 devharness-java-development skill 和旧 Comate memory-first rules
--no-remove-legacy         不删除旧入口，默认不建议使用
```

安装器行为：

```text
1. 检查项目根目录。
2. 检查 .agents/skills/devharness-goal-development 是否存在。
3. 检查 .agents/skills/devharness-graph-aware-development 是否存在。
4. 删除旧 skill：.agents/skills/devharness-java-development。
5. 删除旧 Comate memory-first rules。
6. 生成 Claude Code 适配：
   - .claude/skills/devharness-goal-development
   - .claude/skills/devharness-graph-aware-development
   - CLAUDE.md

7. 生成 OpenCode 适配：
   - AGENTS.md

8. 生成 Comate 适配：
   - .comate/rules/devharness-goal-protocol.mdr
   - .comate/rules/devharness-graph-aware-protocol.mdr

9. 可选安装 jar：
   - .agents/tools/devharness-kit/dhk.jar

10. chmod +x 所有 shell wrapper。
```

---

## 5. 项目类型与验证能力解耦

之前容易把项目分成：

```text
新项目：能跑测试
老项目：不能跑测试
```

这个判断不准确。

真实情况应该分成两层：

```text
project_type         项目形态
verification_policy  验证能力
```

例如：

```text
Spring Boot 项目也可能无法 mvn test
老项目也可能有可跑的单测
大项目单个测试可能十几分钟
公司环境可能只能从 IDE 的测试按钮触发
```

因此不能用 `legacy / modern` 直接决定是否跑测试。

正确模型：

```text
profile 决定开发流程。
verification policy 决定怎么验证。
project config 记录当前项目真实验证能力。
```

---

## 6. Verification Policy 设计

新增统一配置：

```text
.agents/devharness/config.json
```

建议 schema：

```json
{
  "schema_version": "devharness-config/v1-alpha",
  "project": {
    "project_type": "springboot-enterprise-large",
    "module_style": "api",
    "runtime": "company-environment"
  },
  "verification": {
    "compile": {
      "mode": "manual",
      "reason": "mvn compile requires company-specific environment",
      "manual_trigger": "IDE build action",
      "required_evidence": [
        "manual_evidence_status=passed",
        "compile_scope",
        "manual_evidence_path"
      ]
    },
    "test": {
      "mode": "manual",
      "reason": "mvn test fails locally; tests are run from IDE with company runtime",
      "manual_trigger": "IDE test button",
      "expected_duration": "10m+ per test",
      "required_evidence": [
        "manual_evidence_status=passed",
        "test_scope",
        "manual_evidence_path"
      ]
    },
    "graph": {
      "required": true,
      "fresh_snapshot_required": true,
      "impact_map_required": true,
      "allow_stale_requires_approval": true
    },
    "architecture": {
      "mode": "warn"
    },
    "rollback": {
      "required_when_auto_tests_unavailable": true
    }
  }
}
```

### 6.1 compile/test mode

```text
auto
  CLI 自动运行命令，例如 mvn compile / mvn test。

manual
  CLI 不运行命令，要求用户通过 IDE、CI、公司环境或其他入口验证，并提供 evidence。

disabled
  不要求该 check，但必须引入替代门禁：manual risk、rollback plan、graph impact、protected file confirmation。
```

### 6.2 不要把测试叫 skipped

如果无法自动跑测试，不应输出：

```text
test skipped
```

应输出：

```text
test manual_required
```

如果用户已经从 IDE 测试通过，应输出：

```text
test manual_passed
```

如果确实未验证，只能叫：

```text
test waived_with_risk
```

并要求：

```text
waive_reason
approver
rollback_plan
risk_scope
```

---

## 7. `dhk configure` 设计

新增命令族：

```text
dhk configure init
dhk configure show
dhk configure doctor
dhk configure explain
```

### 7.1 交互式初始化

```bash
dhk configure init
```

应询问：

```text
Project type?
- Spring Boot API
- Spring MVC
- Legacy JSP/Servlet
- MyBatis monolith
- Other

Can compile run from CLI?
- yes: mvn compile
- yes: gradle compile
- no: IDE only
- no: company environment only

Can tests run from CLI?
- yes: mvn test
- yes: gradle test
- no: IDE only
- no: CI only
- no

How expensive are tests?
- fast
- slow
- very slow

Require graph impact before edits?
- yes
- no

Allow stale graph impact?
- no
- only with approval evidence
- expert mode only
```

### 7.2 非交互初始化

```bash
dhk configure init \
  --preset springboot-manual-ide-test \
  --target all \
  --graph required \
  --test manual \
  --compile manual \
  --force
```

### 7.3 推荐 presets

```text
springboot-auto-test
springboot-manual-ide-test
springboot-ci-only-test
springmvc-manual-test
legacy-java-small-fix
legacy-jsp-servlet
mybatis-monolith-manual-test
safe-refactor-graph
```

你的场景推荐：

```text
springboot-manual-ide-test
```

它表示：

```text
项目可能是现代 Spring Boot，但验证入口是 IDE/manual，不是 mvn test。
```

---

## 8. Goal 与 Verification Policy 的关系

当前 profile 中：

```text
java-api-change-with-graph 默认要求 compile/test
legacy graph profile 默认不要求 compile/test
```

正式版应调整为：

```text
Profile 给出默认验证策略。
Project config 可以覆盖验证策略。
最终 required checks = Profile + Project Config + Policy 合成。
```

例如：

```json
{
  "profile": "java-api-change-with-graph",
  "default_verification": {
    "compile": "auto",
    "test": "auto"
  }
}
```

项目配置覆盖：

```json
{
  "verification": {
    "compile": { "mode": "manual" },
    "test": { "mode": "manual" }
  }
}
```

最终 `goal verify` 应要求：

```text
sensitive
graph
impact
architecture
workflow
spec
manual-compile
manual-test
```

而不是直接运行 `mvn test`。

---

## 9. GOAL_CONTEXT 输出要求

`GOAL_CONTEXT.md` 应新增：

```md
<verification-policy>
- compile_mode: manual
- compile_trigger: IDE build action
- test_mode: manual
- test_trigger: IDE test button
- test_cost: slow
- auto_maven_test: disabled
- manual_evidence_required: true
- rollback_required_if_test_not_run: true
</verification-policy>

<manual-verification-contract>
- manual_evidence_status=passed
- test_scope=<class or method>
- compile_scope=<module or changed classes>
- manual_evidence_path=<path>
- tester=<human or role>
- risk_if_not_run=<risk summary>
</manual-verification-contract>
```

这样模型会知道：

```text
不要自己跑 mvn test。
需要用户/IDE/CI 产生人工验证证据。
没有证据不能 complete。
```

---

## 10. 验收要求

### 10.1 Skills 收敛验收

```text
1. .agents/skills/devharness-java-development 已删除。
2. .agents/skills 只保留 goal 和 graph-aware 两个正式技能。
3. README 快速开始只指向 goal-first。
4. 不再有默认 memory-first 文档要求。
5. graph-aware skill 禁止模型自行 --allow-stale。
```

### 10.2 Agent adapter 安装验收

Claude Code：

```text
1. 运行 install-agent-adapters.sh --target claude。
2. 生成 .claude/skills/devharness-goal-development/SKILL.md。
3. 生成 .claude/skills/devharness-graph-aware-development/SKILL.md。
4. 生成 CLAUDE.md。
5. CLAUDE.md 明确 goal-first。
6. 旧技能未被复制到 .claude/skills。
```

OpenCode：

```text
1. 运行 install-agent-adapters.sh --target opencode。
2. 生成 AGENTS.md。
3. AGENTS.md 明确所有代码任务走 goal。
4. AGENTS.md 明确 graph_required 时使用 graph-aware。
5. AGENTS.md 禁止绕过 goal 调 workflow/spec/db。
```

Comate：

```text
1. 运行 install-agent-adapters.sh --target comate。
2. 生成 .comate/rules/devharness-goal-protocol.mdr。
3. 生成 .comate/rules/devharness-graph-aware-protocol.mdr。
4. 删除或覆盖旧 memory-first rules。
5. 新 rules 不再要求默认 memory export。
```

安装脚本：

```text
1. --dry-run 不修改文件。
2. --force 可覆盖旧文件。
3. --jar 可安装 jar 到 .agents/tools/devharness-kit/dhk.jar。
4. 所有 .sh wrapper 有可执行权限。
5. 从子目录调用 wrapper 仍能定位 project-root。
```

### 10.3 Verification policy 验收

```text
1. dhk configure init --preset springboot-manual-ide-test 生成 .agents/devharness/config.json。
2. config.json 中 compile.mode=manual。
3. config.json 中 test.mode=manual。
4. GOAL_CONTEXT 输出 verification-policy。
5. goal verify 不运行 mvn test。
6. goal verify 在缺 manual evidence 时 not_ready。
7. goal step 记录 manual_evidence_status=passed + test_scope + manual_evidence_path 后，manual-test check passed。
8. 没有人工证据时不能 complete。
```

### 10.4 新项目 auto-test 验收

```text
1. preset springboot-auto-test 下 compile/test mode=auto。
2. goal verify 会运行 compile/test 命令。
3. compile/test 失败时不能 complete。
4. compile/test 通过时 check passed。
```

### 10.5 大型 Spring Boot 手工测试项目验收

模拟：

```text
Spring Boot 项目
mvn test 不可用
IDE 测试可用
单测耗时 10m+
```

验收：

```text
1. profile 仍可以是 java-api-change-with-graph。
2. verification policy 覆盖 compile/test 为 manual。
3. GOAL_CONTEXT 告诉模型不要运行 mvn test。
4. goal verify 要求人工作证据。
5. 用户提供 IDE 测试 evidence 后，goal verify ready。
```

### 10.6 老旧项目验收

```text
1. legacy profile 不要求 compile/test auto。
2. legacy profile 要求 graph/impact/legacy/workflow。
3. legacy check 要求 rollback_plan。
4. legacy check 要求 manual_evidence_status=passed。
5. protected impact files 要求人为确认。
6. large_refactor/mass_refactor/format_only evidence 会导致 verify failed。
```

---

## 11. 建议开发任务拆分

### P0

```text
1. 删除 devharness-java-development skill。
2. 新增 scripts/install-agent-adapters.sh。
3. 新增 dhk agent install --target claude|opencode|comate|all。
4. 新增 .agents/devharness/config.json schema。
5. 新增 dhk configure init/show/doctor/explain。
6. 新增 verification.compile.mode 和 verification.test.mode。
7. GoalCheckService 支持 manual compile/test。
8. GOAL_CONTEXT 输出 verification-policy。
9. installer 生成 CLAUDE.md、AGENTS.md、Comate rules。
```

### P1

```text
1. configure init 支持 presets。
2. configure init 支持 --target all。
3. installer 支持 --mode copy|link。
4. installer 支持 --dry-run。
5. manual evidence 支持 artifact path 校验。
6. README 收敛为 goal-first 快速开始。
```

### P2

```text
1. 企业 preset registry。
2. configure migrate。
3. profile default verification 与 project config 合成解释。
4. agent install 支持 Windows bat 产物。
```

---

## 12. 最终目标

用户最终体验应简化为：

```bash
dhk configure init --preset springboot-manual-ide-test --target all
dhk goal start --profile auto --task "修复订单分页边界" --module order
```

模型看到：

```text
current_action
allowed_commands
verification_policy
manual_verification_contract
graph_required
next_command
```

用户不需要理解分散配置文件。
模型不需要理解底层命令组合。
不可自动跑测试的项目不会被错误要求 `mvn test`，也不会被简单 `skipped` 放行。
验证责任通过 manual evidence、graph impact、rollback plan、workflow/spec gate 进入审计闭环。
