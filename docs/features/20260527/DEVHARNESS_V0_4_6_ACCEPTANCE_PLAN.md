# DevHarnessKit V0.4.6 验收方案

状态：本轮实现聚焦 `scripts/devharness-control-panel.sh`、兼容安装器、
manifest/install-state、status/doctor/readiness、脚本回归测试和发布文档。
不扩 Graph parser，不引入 daemon/dashboard/operator。

## 1. ECC 精华吸收验收

### Cross-harness source/adapters

- [x] `.agents/skills` 是唯一 source of truth。
- [x] `.claude/skills` 由控制面板/安装器生成。
- [x] `AGENTS.md` 由控制面板/安装器生成。
- [x] `.comate/rules` 由控制面板/安装器生成。
- [x] 不存在三套手写且语义不一致的规则。

### Manifest-driven install

- [x] `.agents/devharness/agent-manifest.json` 被生成。
- [x] manifest 中包含 default skill。
- [x] manifest 中包含 graph-aware conditional skill。
- [x] manifest 中包含 target 配置。
- [x] manifest 中包含 legacy removal 策略。

### Plan / Apply

- [x] `devharness-control-panel.sh plan` 只输出计划，不修改文件。
- [x] `devharness-control-panel.sh install` 执行安装逻辑。
- [x] `--dry-run` 不修改文件。
- [x] `--force` 可覆盖旧 adapter 文件。

### Install State

- [x] `.agents/devharness/install-state.json` 被生成。
- [x] install-state 记录 target。
- [x] install-state 记录 mode。
- [x] install-state 记录 preset。
- [x] install-state 可被 status/doctor 使用。

### Status / Readiness

- [x] `devharness-control-panel.sh status` 输出 goal skill 状态。
- [x] 输出 graph skill 状态。
- [x] 输出 config 状态。
- [x] 输出 policy 状态。
- [x] 输出 Claude/OpenCode/Comate adapter 状态。
- [x] 支持 `--status-format json`。
- [x] 支持 `--status-format markdown`。

---

## 2. Agent Adapter 验收

### Claude Code

命令：

```bash
./scripts/devharness-control-panel.sh configure --target claude --force
```

验收：

- [x] `.claude/skills/devharness-goal-development/SKILL.md` 存在。
- [x] `.claude/skills/devharness-graph-aware-development/SKILL.md` 存在。
- [x] `CLAUDE.md` 存在。
- [x] `CLAUDE.md` 明确 goal-first。
- [x] 旧 `devharness-java-development` 未安装到 `.claude/skills`。

### OpenCode

命令：

```bash
./scripts/devharness-control-panel.sh configure --target opencode --force
```

验收：

- [x] `AGENTS.md` 存在。
- [x] `AGENTS.md` 要求 goal start/resume。
- [x] `AGENTS.md` 要求 goal next。
- [x] `AGENTS.md` 禁止绕过 goal。
- [x] `AGENTS.md` 说明 graph_required 时使用 graph-aware protocol。

### Comate

命令：

```bash
./scripts/devharness-control-panel.sh configure --target comate --force
```

验收：

- [x] `.comate/rules/devharness-goal-protocol.mdr` 存在。
- [x] `.comate/rules/devharness-graph-aware-protocol.mdr` 存在。
- [x] 旧 `project-memory-bootstrap.mdr` 被删除或不再默认生效。
- [x] 旧 `java-development-guard.mdr` 被删除或不再默认生效。
- [x] 新规则明确禁止绕过 goal。

---

## 3. 控制面板验收

命令：

```bash
./scripts/devharness-control-panel.sh configure \
  --preset springboot-manual-ide-test \
  --target all \
  --compile-mode manual \
  --test-mode manual \
  --graph required \
  --force
```

验收：

- [x] 生成 `.agents/devharness/config.json`。
- [x] 生成 `.agents/devharness/policy.json`。
- [x] 生成 `.agents/devharness/agent-manifest.json`。
- [x] 生成 `.agents/devharness/install-state.json`。
- [x] 生成 `.agents/graph/config.json`。
- [x] 生成 Claude adapter。
- [x] 生成 OpenCode adapter。
- [x] 生成 Comate adapter。
- [x] 删除旧 memory-first skill/rules。
- [x] 所有 `.sh` wrapper 可执行。

---

## 4. Verification Policy 验收

### Spring Boot 手工 IDE 测试

配置：

```bash
./scripts/devharness-control-panel.sh configure \
  --preset springboot-manual-ide-test \
  --compile-mode manual \
  --test-mode manual \
  --graph required \
  --force
```

验收：

- [x] `config.json` 中 `verification.compile.mode=manual`。
- [x] `config.json` 中 `verification.test.mode=manual`。
- [x] `GOAL_CONTEXT.md` 输出 compile_mode manual。
- [x] `GOAL_CONTEXT.md` 输出 test_mode manual。
- [x] `goal verify` 不运行 `mvn test`。
- [x] 缺少 manual evidence 时 `goal verify` not_ready。
- [x] 记录 `manual_evidence_status=passed`、`test_scope`、`manual_evidence_path` 后，manual-test check 通过。
- [x] 未提供 manual evidence 时不能 `goal complete`。

### Spring Boot 自动测试

配置：

```bash
./scripts/devharness-control-panel.sh configure \
  --preset springboot-auto-test \
  --compile-mode auto \
  --test-mode auto \
  --graph required \
  --force
```

验收：

- [x] `verification.compile.mode=auto`。
- [x] `verification.test.mode=auto`。
- [x] `goal verify` 会执行 compile/test 命令。
- [x] compile/test 失败时不能 complete。
- [x] compile/test 通过时 check passed。

### Legacy / 高风险项目

配置：

```bash
./scripts/devharness-control-panel.sh configure \
  --preset legacy-jsp-servlet \
  --compile-mode manual \
  --test-mode manual \
  --graph required \
  --force
```

验收：

- [x] 不默认强制 mvn test。
- [x] 要求 graph/impact/legacy/workflow。
- [x] 要求 rollback_plan。
- [x] 要求 manual_evidence_status=passed。
- [x] protected impact files 要求人为确认。
- [x] large_refactor/mass_refactor/format_only 属于 legacy graph skill 禁止动作；broad change 缺 scope/impact evidence 的 verify failed 已有 gate 覆盖。

---

## 5. Policy / Security 验收

- [x] 默认 `graph_allow_stale_requires_approval=true`。
- [x] `graph impact --allow-stale` 无 evidence 被拒绝。
- [x] `graph impact --allow-stale --allow-stale-evidence "<reason>"` 可通过。
- [x] graph-aware skill wrapper 阻止模型自行使用 `--allow-stale`。
- [x] doctor/policy diagnostics 可报告 graph stale approval 配置。
- [x] context export 仍执行 sensitive scan。
- [x] DB SQL 仍需要明确风险确认。

---

## 6. Regression

最低回归：

```bash
git diff --check
mvn -q test
mvn -q -DskipTests package
```

专项回归：

```bash
mvn -q -Dtest=GraphCommandIntegrationTest test
mvn -q -Dtest=GoalIntegrationTest test
mvn -q -Dtest=GoalSkillPackagingTest test
```

脚本回归：

```bash
./scripts/devharness-control-panel.sh plan --target all --dry-run
./scripts/devharness-control-panel.sh configure --target all --preset springboot-manual-ide-test --force
./scripts/devharness-control-panel.sh status
./scripts/devharness-control-panel.sh doctor
./scripts/devharness-control-panel.sh uninstall --target all --dry-run
```

本轮新增自动化证据：

```text
GoalSkillPackagingTest.controlPanelConfigureWritesManifestInstallStateAndAdapters
GoalSkillPackagingTest.controlPanelPlanAndUninstallDryRunDoNotModifyTargetProject
GoalSkillPackagingTest.controlPanelDoctorFailsWithRepairSuggestionWhenAdaptersAreMissing
GoalSkillPackagingTest.agentAdapterInstallerInstallsGoalFirstAdaptersAndRemovesLegacyEntries
```

---

## 7. Release Gate

在标记 beta 前必须满足：

- [x] 所有 P0 验收通过。
- [x] 全量 Maven 测试通过。
- [x] 安装脚本 dry-run / force / target 单独安装通过。
- [x] Claude/OpenCode/Comate 三种 adapter 文件生成正确。
- [x] 旧 memory-first 技能不会作为默认入口出现。
- [x] Spring Boot manual IDE test preset 可以阻止自动 mvn test。
- [x] 控制面板 status 能解释当前配置状态。

不满足以下条件时不得标 stable：

- [ ] 真实项目 dogfooding 未完成。
- [ ] manual verification 证据格式未稳定。
- [ ] install-state drift repair 未成熟。
- [ ] status/readiness 尚未成为标准排错入口。
