# Context Governor 实现蓝图（1.3 上下文治理层）

> 目标读者：实现者（Codex / 工程师）。本文是**实现就绪的设计蓝图**，不是泛泛的方向。
> 一句话：把 DevHarnessKit 的“短上下文导出”从**静态拼接 + 笨截断**，升级为**预算化、类型化、可压缩、可回查、可观测**的本地上下文治理层。**省 token，但不牺牲证据、恢复能力与 Agent 正确性。**

## 0. 现状基线（必须在其之上做增量，不要从零重写）

这些已经存在，新设计是它们的**升级**，不是替代：

- `export/CurrentContextRenderer.java`：已有 **char 级 section 预算**（`TOTAL_BUDGET=20K, MEMORY=8K, WORKFLOW=3K, SPEC=3K`）、行边界截断、`<truncation-report>` 输出。**这是地基。**
- `export/GoalContextRenderer.java`：渲染 `GOAL_CONTEXT.md`（goal/current-action/evidence-contract/graph/bdd 等 section）。
- `service/ExportSelectionService.java`：按任务 token 给已确认 memory 打分选 top-N（注意：当前对中文分词弱，见 §9 已知限制）。
- `service/goal/GoalContextService.java`、`command/memory/ExportCommand.java`：导出的调用方。
- `command/goal/GoalVerifyCommand.java`：用 `ProcessBuilder` 跑 `mvn package`，把构建输出读进内存缓冲——**这正是日志爆上下文的源头之一**。
- 数据库迁移最新为 **V17**；新表用 **V18**（参照 `db/migration/V17MemoryQualityMigration.java` 的写法）。
- 配置在 `.agents/devharness/config.json`，键值式，访问器在 `model/config/DevHarnessConfig.java`（如 `verification.compile.mode`）。新键加在 `context.*` 命名空间。

## 1. 设计原则（硬约束，违反即拒收）

1. **本地优先**：不调模型、不起服务、不拦截 API。治理发生在**上下文文件导出之前**。
2. **压缩 ≠ 摘要**：任何被压缩的内容都必须**保留原文 + span 引用**，可按 artifact id + 行号回查。SQLite 仍是唯一真相源。
3. **类型化处理**：不同来源用不同策略（§4）。**绝不压缩代码正文**（只做“该读哪些文件”的选择，见 §4 表）。
4. **证据不可逆丢失 = 严重缺陷**：BDD required evidence、manual evidence、失败根因、敏感检查结论**绝不能被压没**；只能压缩“噪音”（成功堆栈、重复行、中间进度）。
5. **可观测**：压缩/裁剪/省略必须可被 `dhk context stats` 看到，不做黑盒。
6. **稳定契约兼容**：现有 `CURRENT_CONTEXT.md` / `GOAL_CONTEXT.md` 的稳定 section 锚点（见 `docs/STABLE_CONTRACT.md`、`docs/EXPORT_CONTRACTS.md`）**只能增字段、不能改既有锚点名/顺序**。新 section 追加在尾部或现有 section 内部。
7. **token 估算先用粗估**：`estimated_tokens = ceil(chars / 4)`（中文按字符计已足够保守）。第一版**不接真实 tokenizer**，留 `TokenEstimator` 接口供日后替换。

## 2. 模块与类布局

```
src/main/java/com/devharnesskit/dhk/context/
  ContextGovernor.java          // 编排：输入 ContextItem[] + 预算策略 -> ContextRenderResult
  ContextItem.java              // 一条上下文项：type, priority, rawText, sourceRef, compressible
  ContextItemType.java          // 枚举，见 §4
  ContextPriority.java          // REQUIRED / HIGH / NORMAL / LOW
  ContextBudget.java            // 总预算 + 各 section 预算（token）
  ContextBudgetPolicy.java      // 从 config.json 读取预算；提供默认值
  ContextRenderResult.java      // 渲染文本 + 每 section 统计（before/after token, truncated, omitted, compressedArtifactIds）
  token/
    TokenEstimator.java         // 接口：int estimate(String)
    CharsOverFourTokenEstimator.java   // 默认实现 ceil(len/4)
  compress/
    ContextCompressor.java      // 接口：CompressResult compress(ContextItem, int tokenBudget)
    PassthroughCompressor.java  // 不压，只按预算行截断（默认）
    BuildLogCompressor.java     // Maven/Gradle/npm 构建日志 -> 结构化 digest（§4.BUILD_LOG）
    TestLogCompressor.java      // pytest/JUnit 文本日志 -> 失败用例 + 根因
    ShellOutputCompressor.java  // 通用 shell 输出 -> command/exitCode/error block/last N lines
    JsonOutputCompressor.java   // 大 JSON -> 关键字段 + 省略计数（P2，可后置）
  artifact/
    ContextArtifact.java        // 数据模型，见 §3
    ContextArtifactRepository.java   // SQLite CRUD（V18 表）
    ContextArtifactService.java      // 压缩落库 + 回查
src/main/java/com/devharnesskit/dhk/command/context/
  ContextCommand.java           // 子命令路由：stats / artifacts / retrieve / render / doctor
  ContextStatsCommand.java
  ContextArtifactsCommand.java
  ContextRetrieveCommand.java
  ContextRenderCommand.java     // P1
  ContextDoctorCommand.java     // P2
```

`CommandRouter` 注册：`this.commands.put("context", () -> new ContextCommand());`（参照现有 `goal`/`memory` 注册写法）。

## 3. 数据模型与迁移（V18）

新表 `context_artifact`（保存“压缩视图 + 原文引用”，实现 §1 原则 2）：

```sql
CREATE TABLE IF NOT EXISTS context_artifact (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  project_key TEXT NOT NULL,
  goal_key TEXT NOT NULL DEFAULT '',
  artifact_key TEXT NOT NULL,            -- 稳定可引用 id，如 ctx-20260608-001
  source_type TEXT NOT NULL,             -- ContextItemType 名
  source_path TEXT NOT NULL DEFAULT '',  -- 原始日志/文件路径（若有）
  original_sha256 TEXT NOT NULL DEFAULT '',
  original_text TEXT NOT NULL DEFAULT '',-- 原文（或落盘路径，见下）
  compressed_text TEXT NOT NULL DEFAULT '',
  retained_spans_json TEXT NOT NULL DEFAULT '',  -- [{source,startLine,endLine}]
  omitted_lines INTEGER NOT NULL DEFAULT 0,
  token_before INTEGER NOT NULL DEFAULT 0,
  token_after INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL,
  CHECK (token_after >= 0),
  FOREIGN KEY (project_key) REFERENCES project(project_key)
);
CREATE INDEX IF NOT EXISTS idx_context_artifact_project_goal
  ON context_artifact(project_key, goal_key, created_at);
CREATE UNIQUE INDEX IF NOT EXISTS idx_context_artifact_key
  ON context_artifact(project_key, artifact_key);
```

迁移类 `db/migration/V18ContextArtifactMigration.java`：完全照 `V17MemoryQualityMigration` 模式（`addColumnIfMissing`/`CREATE TABLE IF NOT EXISTS` + 写 `schema_version` 行）。同步更新：`MigrationRunner.V18`、`DefaultMigrationStepCatalog`、`VersionInfo.CURRENT_SCHEMA_VERSION`、README/COMPATIBILITY/MIGRATIONS 的 schema 版本号、`MigrationRunnerTest`（加 V18 断言）。

原文存储策略：原文 ≤ 64KB 直接存 `original_text`；超过则落盘到 `.agents/context/artifacts/<artifact_key>.txt`，`original_text` 留空、`source_path` 指向落盘文件。回查统一走 `ContextArtifactService.retrieve`。

## 4. ContextItemType 与压缩策略（核心）

| 类型 | 优先级 | 策略 | 绝不允许 |
|------|--------|------|----------|
| `GOAL` / `CURRENT_STEP` | REQUIRED | 不压缩，只裁历史；超预算时**报错而非静默丢** | 丢当前动作/证据契约 |
| `AGENT_RULE` | HIGH | 稳定规则去重，不重复塞入 | — |
| `MEMORY_FACT` | NORMAL | 复用 `ExportSelectionService` 排序（confirmed/freshness/冲突）；按预算取 top-N | 把未确认 draft 当默认上下文 |
| `MEMORY_CANDIDATE` | LOW | 默认不进上下文，仅 `--include-candidates` 时进 | — |
| `BDD_SCENARIO` | HIGH | 保留 Given/When/Then 全文，不摘要 | 摘掉 G/W/T 任一 |
| `BDD_EVIDENCE` | REQUIRED(若 profile 要求) | required evidence 全保留 | 压掉 required evidence |
| `GRAPH_IMPACT` | NORMAL | 输出 digest：required/optional read files + 原因 + stale 状态（§5） | 塞全量扫描结果 |
| `BUILD_LOG` | NORMAL | 结构化 digest（见下），原文落 artifact | 丢失败点/根因/exit code |
| `TEST_LOG` | NORMAL | 失败用例 + 根因 + 失败块行号 | 丢失败用例名 |
| `SHELL_OUTPUT` | NORMAL | command + exitCode + error block + 末尾 N 行 | 丢 exitCode/error |
| `CODE_BODY` | — | **只做选择（该读哪些文件），不压正文** | **压缩代码正文（高风险，禁止）** |
| `RISK` / `RECOVERY_STATE` | HIGH | 不压缩 | — |

**结构化 digest 输出格式**（写进上下文文件的就是这个 JSON/Markdown，不是原始日志）：

```json
{
  "type": "build_log_summary",
  "command": "mvn test",
  "exit_code": 1,
  "status": "failed",
  "failed_tests": ["ProjectServiceTest.shouldCreateProject"],
  "root_cause_candidates": ["UNIQUE constraint failed: project.code"],
  "retained_spans": [{ "source": "logs/mvn-test.log", "start_line": 183, "end_line": 226 }],
  "omitted_lines": 1840,
  "artifact_ref": "ctx-20260608-001"
}
```

压缩器判定与抽取规则（BuildLogCompressor / TestLogCompressor 至少覆盖）：
- **Maven**：抓 `\[ERROR\]`、`BUILD FAILURE/SUCCESS`、`Tests run: ... Failures: ... Errors: ...`、`<<< FAILURE!`、首个 stacktrace 块、`Caused by:` 链。`failed_tests` 从 surefire 行或 `<<< FAILURE!` 提取。
- **pytest**：`FAILED `/`ERROR ` 行、`E   ` 断言块、`= short test summary =` 区。
- **JUnit XML**：解析 `<testcase>`/`<failure>`（可复用现有 `service/bdd/JunitBddEvidenceService` 的解析思路，不要重复造）。
- **shell 通用**：保留首行命令、最后 `omitted` 之外的 error 块、`exit_code`（若可得）、末尾 40 行。
- 抽不出失败结构时：fallback 为“保留末尾 N 行 + 标 `degraded: true`”，**绝不静默丢全部**。

## 5. Graph Impact Digest（接 Graph Lite，§4 GRAPH_IMPACT）

`GRAPH_CONTEXT_DIGEST` 块（agent 看到行动指引而非扫描噪音）：

```
## Graph Impact Digest
goal: <task>
required_read:
  1. <path>  (reason: direct symbol match)
optional_read:
  1. <path>  (reason: db constraint relation)
graph_snapshot: status=fresh|stale; indexed_at=...; confidence=advisory_only
```

复用现有 `export/GraphImpactRenderer` 的数据来源（related_files / risk_nodes / snapshot_stale），只改“塞进上下文”的渲染为 digest。**stale 必须显式标注**（不可隐藏）。

## 6. 配置（`.agents/devharness/config.json`，新增 `context.*` 键）

```json
{
  "context.budget.total_tokens": "12000",
  "context.budget.goal": "2500",
  "context.budget.current_step": "1500",
  "context.budget.memory": "2500",
  "context.budget.evidence": "2500",
  "context.budget.graph": "1200",
  "context.budget.bdd": "1500",
  "context.budget.risks": "800",
  "context.compress.evidence": "true",
  "context.compress.min_lines_to_compress": "40"
}
```

`ContextBudgetPolicy` 缺省值即上表；缺键用默认。`DevHarnessConfig` 增加 `contextBudget(section)` 访问器。**注意单位从 char 迁到 token**：现有 `CurrentContextRenderer` 的 char 常量改为从 policy 取 token 预算 × 4（或直接用 TokenEstimator 反推），保证行为不突变。

## 7. 命令规格

```
dhk context stats   [--goal <key>] [--json]
dhk context artifacts [--goal <key>] [--json]
dhk context retrieve <artifact-key> [--lines <a>-<b>] [--json]
dhk context render  --goal <key>            # P1：从 SQLite 重新生成上下文文件
dhk context doctor  [--goal <key>] [--json] # P2
```

- **`context stats`**：输出每 section 的 `tokens_used / budget`、总 before/after token、compression_ratio、被省略内容计数、可回查 artifact 列表、风险提示。示例输出见 §10 验收。
- **`context artifacts`**：列出本项目/本 goal 的压缩 artifact（key/type/token_before→after/omitted_lines）。
- **`context retrieve`**：按 artifact_key 回查原文，`--lines a-b` 取行区间。退出码：找不到 → `NOT_FOUND(4)`。
- **`context doctor`**（P2）：检查并以非 0 退出码报告：超预算 / 高优先级内容被压 / required evidence 被省 / stale graph 进了上下文 / unconfirmed memory 进了默认上下文 / 压缩 artifact 缺 original_ref。

退出码沿用 `cli/ExitCodes`（0 成功 / 2 用法 / 3 校验 / 4 未找到 / 1 运行错）。

## 8. 与现有流程的集成点（不要破坏）

1. **`GoalVerifyCommand`**：跑 `mvn` 的输出**先过 `BuildLogCompressor` 落 artifact**，再把 digest（而非原始日志）写入 evidence/上下文。原始输出进 `context_artifact.original_text`。
2. **`CurrentContextRenderer` / `GoalContextService`**：渲染前先经 `ContextGovernor` 按预算编排；保留现有稳定 section 锚点，新增 `<context-budget-report>`（在 `<truncation-report>` 旁或替代它，但保留旧字段名以兼容）。
3. **`ExportSelectionService`**：作为 `MEMORY_FACT` 的排序来源被 Governor 调用，不改其签名。
4. **敏感数据**：所有压缩产物在落库/导出前必须过现有 `service/SensitiveDataGuard`（与 memory/brief 一致），命中即 reject 或 redact。

## 9. 已知限制（实现时一并修或显式记录）

- **中文分词弱**：`util/TextUtil.tokens` 把整段中文当一个 token，导致中文 memory 的相关性选择召回差。Governor 的 memory 选择若沿用它，需在文档标注；可选增强：对中文按 bigram 或保留子串匹配（非本期必须，但 `context doctor` 应能提示“memory 召回为 0 但库中有相关条目”）。
- token 粗估与真实 tokenizer 有偏差：第一版可接受，`stats` 注明 `estimated`。

## 10. 验收标准（分阶段，每阶段独立可交付）

### P0（MVP，团队验证急需）：证据/日志压缩 + token 预算 + stats
- `BuildLogCompressor` 能把一段真实失败的 Maven/pytest 日志压成 §4 digest，**保留**失败用例名、根因候选、exit code、失败块行号；原文落 `context_artifact`，可 `context retrieve <key> --lines a-b` 回查。
- token 下降 **> 50%**（用真实日志度量）。
- `dhk context stats` 输出形如：
  ```
  CURRENT_CONTEXT.md
    before: 31,420 est tokens   after: 8,740 est tokens   reduction: 72.1%
  sections:
    goal: 1,420/1,500   memory: 2,200/2,500   evidence: 1,900/2,500   graph: 860/1,200   bdd: 1,100/1,500
  compressed_artifacts: build_log:1  shell_output:1
  risks:
    - 1 evidence log compressed (degraded=false)
    - 2 memory candidates excluded (unconfirmed)
  ```
- **回归**：现有 goal/memory/BDD/graph 行为不变；`CURRENT_CONTEXT.md`/`GOAL_CONTEXT.md` 既有稳定锚点不变；全量测试通过。
- **新测试**：`ContextArtifactRepository` CRUD；`BuildLogCompressor`/`TestLogCompressor`/`ShellOutputCompressor` 各覆盖（含 fallback 路径）；`ContextStatsCommand`、`ContextRetrieveCommand` 集成测试；`MigrationRunnerTest` V18 断言。

### P1：artifact 索引完善 + `context render`
- `context artifacts` / `context retrieve` 完整；`context render --goal` 可从 SQLite 重新生成上下文文件。

### P2：类型化预算 + `context doctor`
- memory/graph/bdd 进入上下文时遵守 §6 预算；`context doctor` 覆盖 §7 全部检查项，发现问题非 0 退出。

### P3（可选，不进 stable core）：headroom adapter
- `dhk context render --compressor=headroom` 作为实验项；默认不启用。

## 11. 明确不做（防止跑偏）
- **不压缩代码正文**（只做 Graph Lite 的“该读哪些文件”选择）。
- **不接 API proxy / 不起后台服务**。
- **不做 RAG chunk 压缩**（后置）。
- **不把 headroom 放进 stable core**。

## 12. 落地顺序（建议）
P0 → P1 → P2 → P3。P0 必须先于一切，因为它直接解决“日志爆上下文导致首个真实 pilot 翻车”，是团队验证阶段的最高优先级。
