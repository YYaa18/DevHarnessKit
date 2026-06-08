# Context Governor 任务清单（给实现者 / Codex）

配合 `docs/CONTEXT_GOVERNOR.md` 使用。按顺序做，每个任务有明确产出与验收。**P0 必须最先完成**（团队验证阶段急需，直接解决日志爆上下文）。

> 全局约束（每个任务都适用）：① 不破坏现有稳定 section 锚点；② 压缩必带原文回查；③ required evidence / 失败根因 / exitCode 绝不丢；④ 不压代码正文；⑤ 落库/导出前过 `SensitiveDataGuard`；⑥ 改完跑全量测试保持绿。

---

## P0 — MVP（证据/日志压缩 + token 预算 + stats）

### T1. token 估算骨架
- 新增 `context/token/TokenEstimator.java`（接口 `int estimate(String)`）+ `CharsOverFourTokenEstimator`（`ceil(len/4)`）。
- 验收：单测覆盖空串/中英文混合；估算稳定可解释。

### T2. context_artifact 表 + 仓库（V18 迁移）
- 新增 `db/migration/V18ContextArtifactMigration.java`（照 V17 写法），更新 `MigrationRunner.V18`、`DefaultMigrationStepCatalog`、`VersionInfo.CURRENT_SCHEMA_VERSION`。
- 同步 schema 版本号：README / README.zh-CN / `docs/COMPATIBILITY.md` / `docs/MIGRATIONS.md`。
- 新增 `context/artifact/{ContextArtifact,ContextArtifactRepository,ContextArtifactService}.java`：CRUD + `store(...)` + `retrieve(artifactKey, lineRange)`。原文 >64KB 落盘 `.agents/context/artifacts/<key>.txt`。
- 验收：`MigrationRunnerTest` 加 V18 断言；`ContextArtifactRepository` CRUD 单测；migration 幂等。

### T3. 日志/输出压缩器
- 新增 `context/compress/{ContextCompressor(接口),PassthroughCompressor,BuildLogCompressor,TestLogCompressor,ShellOutputCompressor}.java`。
- 抽取规则见蓝图 §4；JUnit XML 解析复用 `service/bdd/JunitBddEvidenceService` 思路，勿重复造。
- fallback：抽不出结构时保留末尾 N 行 + `degraded:true`，绝不静默丢全部。
- 验收：用真实 Maven 失败日志 + pytest 失败日志各一段做单测，断言保留：失败用例名、root_cause、exit_code、retained_spans；token 下降 >50%；degraded 路径有测试。

### T4. 接入 goal verify 的构建日志
- `GoalVerifyCommand` 跑 `mvn` 的输出：先过 `BuildLogCompressor` 落 `context_artifact`，evidence/上下文写 digest 而非原始日志。
- 验收：集成测试——verify 后存在 build_log artifact，digest 含失败信息，原始日志可 `context retrieve` 回查。

### T5. `dhk context` 命令族（stats / artifacts / retrieve）
- 新增 `command/context/{ContextCommand,ContextStatsCommand,ContextArtifactsCommand,ContextRetrieveCommand}.java`；`CommandRouter` 注册 `context`；`HelpCommand` 加用法。
- `stats` 输出见蓝图 §10 样例（section token 占用、before/after、压缩率、省略计数、风险）。
- 验收：三命令集成测试；`retrieve` 找不到返回退出码 4；`stats --json` 字段稳定。

### T6. 预算单位 char→token（不突变行为）
- `ContextBudget` / `ContextBudgetPolicy` 从 `config.json` 的 `context.budget.*` 读取（缺省值见蓝图 §6）；`DevHarnessConfig` 加访问器。
- `CurrentContextRenderer` 的 char 常量改为经 policy + TokenEstimator 得出，保证默认行为与现状基本一致（不要让现有上下文突然变短/变长）。
- 验收：默认配置下 `CURRENT_CONTEXT.md` 内容与改造前差异在阈值内（回归对比）；truncation-report 旧字段名保留。

**P0 完成判据**：真实 Maven/pytest 失败日志 token 下降 >50% 且可回查；`dhk context stats` 可观测；全量测试绿；现有 goal/memory/bdd/graph 行为与稳定锚点不变。

---

## P1 — artifact 完善 + render
### T7. `dhk context render --goal <key>`：从 SQLite 重新生成 `CURRENT_CONTEXT.md`/`GOAL_CONTEXT.md`，经 Governor 预算编排。
### T8. `context artifacts` 完整列表 + 分页/过滤；artifact 与 goal 关联展示。

## P2 — 类型化预算 + doctor
### T9. `ContextGovernor` 编排：输入 `ContextItem[]`（GOAL/CURRENT_STEP/MEMORY_FACT/BDD/GRAPH_IMPACT/BUILD_LOG/...）+ 预算策略 → `ContextRenderResult`；各类型按蓝图 §4 策略 + §6 预算。
### T10. `GRAPH_CONTEXT_DIGEST`（蓝图 §5）：Graph 结果以 digest 进上下文，stale 显式标注。
### T11. memory 进上下文遵守预算（复用 ExportSelectionService 排序 + freshness）。
### T12. `dhk context doctor`：覆盖蓝图 §7 全部检查项，发现问题非 0 退出。

## P3 —（可选，不进 stable core）
### T13. `dhk context render --compressor=headroom` 实验 adapter，默认关闭。

---

## 测试清单（最低要求）
- 单测：TokenEstimator、各 Compressor（含 fallback）、ContextArtifactRepository、ContextBudgetPolicy。
- 集成：context stats/artifacts/retrieve、goal verify 日志压缩落库+回查、context render(P1)、context doctor(P2)。
- 迁移：MigrationRunnerTest V18。
- 回归：全量 `mvn test` 绿；`docs/STABLE_CONTRACT.md` / `EXPORT_CONTRACTS.md` 既有锚点不破坏（GoalSkillPackagingTest 等若断言上下文锚点需保持通过）。

## 文档同步
- 更新 `docs/EXPORT_CONTRACTS.md`：新增 `<context-budget-report>` 锚点说明、digest 块格式。
- 更新 `docs/COMPATIBILITY.md` / `MIGRATIONS.md`：schema v18、context_artifact 表。
- 更新 README：1.3 Context Governor 能力简述。
- `dhk version` 的 surface 列表加 `context-governor`（stable-candidate）。
