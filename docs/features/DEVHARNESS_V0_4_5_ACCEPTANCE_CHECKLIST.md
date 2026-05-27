# DevHarnessKit V0.4.5 验收清单

状态：已按 `V0.4.5 Agent Adapter Packaging & Verification Policy` 收口。
结论：可以作为 beta developer preview 进入打包前验证；不要描述为 stable。

## A. Skill 收敛

- [x] `.agents/skills/devharness-java-development/` 已删除。
- [x] `.agents/skills/devharness-goal-development/` 存在。
- [x] `.agents/skills/devharness-graph-aware-development/` 存在。
- [x] README 快速开始只推荐 configure + adapter installer + goal-first。
- [x] 默认 agent 入口不再要求 memory-first；memory/workflow/spec/db 仅作为低层调试或专家命令保留。
- [x] graph-aware skill 和 wrapper 禁止模型自行使用 `--allow-stale`；必须有 policy 或人工 approval evidence。

证据：

```text
GoalSkillPackagingTest.legacyMemoryFirstSkillPackageIsRemoved
GoalSkillPackagingTest.installerInstallsAdaptersAndRemovesLegacyEntrypoints
.agents/skills/devharness-graph-aware-development/scripts/graph-impact.sh
README.md Quick Start
```

## B. Agent Adapter 安装

### Claude Code

运行：

```bash
scripts/install-agent-adapters.sh --target claude --force
```

验收：

- [x] `.claude/skills/devharness-goal-development/SKILL.md` 存在。
- [x] `.claude/skills/devharness-graph-aware-development/SKILL.md` 存在。
- [x] `CLAUDE.md` 存在。
- [x] `CLAUDE.md` 明确 goal-first。
- [x] `.claude/skills` 中不存在旧 `devharness-java-development`。

### OpenCode

运行：

```bash
scripts/install-agent-adapters.sh --target opencode --force
```

验收：

- [x] `AGENTS.md` 存在。
- [x] `AGENTS.md` 要求先 goal start/resume。
- [x] `AGENTS.md` 要求每轮 goal next。
- [x] `AGENTS.md` 禁止绕过 goal 调 memory/workflow/spec/db。
- [x] `AGENTS.md` 说明 `graph_required=true` 时使用 graph-aware 协议。

### Comate

运行：

```bash
scripts/install-agent-adapters.sh --target comate --force
```

验收：

- [x] `.comate/rules/devharness-goal-protocol.mdr` 存在。
- [x] `.comate/rules/devharness-graph-aware-protocol.mdr` 存在。
- [x] 旧 `project-memory-bootstrap.mdr` 已删除或不再 memory-first。
- [x] 旧 `java-development-guard.mdr` 已删除或不再 memory-first。
- [x] 新规则明确禁止绕过 goal。

证据：

```text
scripts/install-agent-adapters.sh
GoalSkillPackagingTest.installerInstallsAdaptersAndRemovesLegacyEntrypoints
GoalSkillPackagingTest.installerDryRunDoesNotModifyTarget
GoalSkillPackagingTest.installerFailsWhenGraphAwareSkillIsMissing
```

## C. 安装脚本

- [x] `--dry-run` 不修改文件。
- [x] `--force` 可以覆盖旧适配文件。
- [x] `--jar <path>` 可以安装 `.agents/tools/devharness-kit/dhk.jar`。
- [x] `.sh` wrapper 有执行权限。
- [x] 从子目录调用 wrapper 仍能定位 project-root。
- [x] 缺少 goal/graph-aware skill 时脚本失败并输出明确错误。

证据：

```text
scripts/install-agent-adapters.sh
GoalSkillPackagingTest
SkillGateIntegrationTest.goalDrivenGateAllowsAdditionalVerifyEvidenceStep
```

## D. Verification Policy

- [x] 新增 `.agents/devharness/config.json`。
- [x] 支持 `verification.compile.mode = auto/manual/disabled`。
- [x] 支持 `verification.test.mode = auto/manual/disabled`。
- [x] `dhk configure init --preset springboot-manual-ide-test` 可生成配置。
- [x] `dhk configure show` 可展示有效配置。
- [x] `dhk configure doctor` 可诊断缺失/冲突配置。
- [x] `dhk configure explain` 可解释配置项含义。
- [x] 配置 schema 为扁平 dotted-key JSON：`devharness-config/v1-alpha`。

证据：

```text
src/main/java/com/devharnesskit/dhk/command/configure/
src/main/java/com/devharnesskit/dhk/service/config/DevHarnessConfigService.java
ConfigureCommandIntegrationTest
docs/GOAL_CONFIGURATION.md
```

## E. Spring Boot 手工测试项目

模拟场景：

```text
Spring Boot 大项目
mvn test 不可用
只能从 IDE 测试按钮触发
单测耗时 10m+
```

验收：

- [x] 使用 `springboot-manual-ide-test` preset。
- [x] `GOAL_CONTEXT.md` 输出 `compile_mode: manual`。
- [x] `GOAL_CONTEXT.md` 输出 `test_mode: manual`。
- [x] `GOAL_CONTEXT.md` 明确不要自动运行 `mvn test`。
- [x] `goal verify` 缺少人工验证 evidence 时返回 not_ready。
- [x] `goal step` 提供 `manual_evidence_status=passed`、`test_scope`、`manual_evidence_path` 后，manual-test check 通过。
- [x] 没有人工证据时不能 `goal complete`。

证据：

```text
ConfigureCommandIntegrationTest.manualPresetRendersManualVerificationContractInGoalContext
ConfigureCommandIntegrationTest.manualVerificationFailsClosedAndPassesWithProjectLocalEvidence
```

## F. 新项目自动测试

- [x] 使用 `springboot-auto-test` preset。
- [x] `compile.mode=auto`。
- [x] `test.mode=auto`。
- [x] `goal verify` 会执行 compile/test 命令。
- [x] compile/test 失败时不能 complete。
- [x] compile/test 通过时 check passed。

证据：

```text
ConfigureCommandIntegrationTest.autoPresetKeepsCompileAndTestChecks
ConfigureCommandIntegrationTest.autoPresetSkippedCompileRemainsNotReadyWhenPomIsMissing
GoalIntegrationTest
```

## G. Legacy / 高风险项目

- [x] legacy profile 不默认要求 auto compile/test。
- [x] legacy profile 要求 graph/impact/legacy/workflow。
- [x] legacy check 要求 rollback plan。
- [x] legacy check 要求 manual evidence。
- [x] protected impact files 要求人为确认。
- [x] large_refactor/mass_refactor/format_only 属于 legacy graph skill 禁止动作；实际 gate 覆盖 broad change 缺少 scope/impact evidence 的失败路径。

证据：

```text
.agents/skills/devharness-graph-aware-development/SKILL.md
.agents/skills/devharness-graph-aware-development/references/graph-forbidden-actions.md
GoalIntegrationTest.legacyGraphProfileVerifyFailsWithoutRollbackAndManualEvidence
GoalIntegrationTest.legacyGraphProfileSurfacesProtectedImpactRiskAndCompletesWithEvidence
SkillGateIntegrationTest
```

## H. 回归命令

最低回归：

```bash
git diff --check
mvn -q test
mvn -q -DskipTests package
```

v0.4.5 专项回归：

```bash
mvn -q -Dtest=GoalSkillPackagingTest,ConfigureCommandIntegrationTest,SkillGateIntegrationTest test
mvn -q -Dtest=GoalIntegrationTest test
```

本轮 AI-131 文档收口至少应执行：

```bash
git diff --check
mvn -q -Dtest=ConfigureCommandIntegrationTest,GoalSkillPackagingTest test
mvn -q -DskipTests package
```

执行结果在对应 Linear issue 与最终回执中记录。
