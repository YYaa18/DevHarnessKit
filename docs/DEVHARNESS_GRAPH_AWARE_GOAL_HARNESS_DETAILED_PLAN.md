# DevHarnessKit Graph-aware Goal Harness 详细开发计划

> 建议落地版本：`V0.5 Graph-aware Goal Harness`
>
> 文档目标：把 DevHarnessKit 从 **Goal-driven Harness** 升级为 **Graph-aware Goal Harness**，让 AI 在老旧项目和新项目中都能基于代码结构事实进行影响面分析、上下文选择、验证和审计。
>
> 建议文件位置：`docs/DEVHARNESS_GRAPH_AWARE_GOAL_HARNESS_DETAILED_PLAN.md`

---

## 0. 背景与结论

DevHarnessKit 当前已经具备较强的 Harness 基础：

```text
memory      长期项目事实
spec        需求、任务、验收
workflow    流程、phase、gate、artifact
policy      安全约束
skills      Agent 使用协议
goal        主控编排层
```

但它仍缺少一个关键层：

```text
Graph Context Layer：机器可查询的代码结构事实层。
```

目前 Goal 能回答：

```text
这个任务现在应该走到哪一步？
哪些检查必须通过？
哪些证据必须留下？
什么时候可以 complete？
```

但它还不能稳定回答：

```text
这个方法谁在调用？
这个 XML SQL 被哪些 Service 使用？
这个 Controller 入口影响哪些页面？
这个字段在哪些 JSP / DTO / Mapper / SQL 中出现？
这个改动是否跨越了模块边界？
这个改动是否扩大了影响面？
```

因此，下一阶段应升级为：

```text
Graph-aware Goal Harness
```

核心定义：

```text
DevHarnessKit 用 graph 提供代码结构上下文，
用 goal 编排 AI 开发流程，
用 workflow/spec/policy/checks 约束执行，
用 memory 沉淀人工确认事实。
```

一句话目标：

```text
让 AI 不是“读一堆文件后猜测影响面”，而是“基于可查询的代码图谱进行开发”。
```

---

## 1. 外部参考与可吸收设计

### 1.1 CodeGraphContext 的启发

CodeGraphContext 的核心定位是：把本地代码仓库索引成 graph database，并通过 CLI 或 MCP 给 AI 助手提供代码上下文。其公开 README 中提到的关键能力包括：

```text
- Code indexing
- Relationship analysis
- callers / callees / class hierarchies / call chains
- dead code / complexity
- live file watching
- CLI + MCP server 双模式
- 多语言支持
- 多种图数据库后端
- 可选 SCIP indexing
- 交互式可视化
```

DevHarnessKit 不应该直接复制 CodeGraphContext，而应吸收其设计精华：

```text
代码图谱是 AI 编码上下文的重要基础设施。
图谱能力应作为 Harness 的上下文层，而不是替代 Goal/Workflow/Spec。
```

### 1.2 CodexGraph 的启发

CodexGraph 的研究方向是把 LLM agents 与从代码仓库抽取出的 graph database interface 结合，使模型可以通过结构化图查询做代码导航与上下文获取，而不是只依赖相似度检索或手工工具。

DevHarnessKit 可以吸收的核心思想：

```text
将代码仓库变成可查询图谱。
让 Agent 通过图查询获取结构上下文。
让图谱成为 repo-scale coding 的基础接口。
```

### 1.3 Codebase-Memory 的启发

Codebase-Memory 提出用 Tree-sitter 构建持久知识图谱，并通过 MCP 提供给 LLM 进行代码探索。其重点是减少模型反复 grep / read file 的 token 和工具调用成本。

DevHarnessKit 可以吸收的核心思想：

```text
把“反复读文件找上下文”变成“先查询图谱，再读取少量关键文件”。
```

### 1.4 Repository Intelligence Graph 的启发

Repository Intelligence Graph 强调构建确定性的、可追溯的架构图，特别是构建、测试、依赖、组件和 runner 之间的关系。

DevHarnessKit 可以吸收的核心思想：

```text
图谱不只包含代码符号，还应包含 build/test/runtime 结构。
对老旧项目尤其重要：构建方式和运行方式往往比代码本身更难理解。
```

---

## 2. DevHarnessKit 的目标定位

### 2.1 不要把 DevHarnessKit 做成另一个 CodeGraphContext

CodeGraphContext 更像：

```text
Code Graph Indexer + Query CLI + MCP Server
```

DevHarnessKit 应该是：

```text
AI Development Harness Kernel
```

因此新增 graph 能力后，DevHarnessKit 的定位应为：

```text
Graph-aware AI Development Harness
```

### 2.2 新的模块边界

目标架构：

```text
DevHarnessKit
├── memory        人工确认的长期事实
├── spec          需求、任务、验收
├── workflow      流程、phase、gate、artifact
├── goal          开发目标主控编排
├── policy        安全、权限、边界
├── graph         代码结构事实与影响面分析
└── skills        Agent 使用协议
```

各层职责：

| 模块 | 职责 | 是否长期事实 | 是否可自动生成 |
|---|---|---:|---:|
| memory | 人工确认的项目事实 | 是 | 否，最多 suggestion |
| graph | 代码结构、调用关系、依赖关系 | 否，绑定 snapshot | 是 |
| spec | 需求、任务、验收 | 是 | 可 scaffold，需确认 |
| workflow | 流程状态、gate、artifact | 是 | 可由 goal 驱动 |
| goal | 当前任务编排 | 是 | 用户启动，工具推进 |
| policy | 安全边界 | 是 | 可生成模板，需确认 |

核心边界原则：

```text
Graph 是机器分析出的结构事实，必须绑定 snapshot。
Memory 是人工确认的长期事实，可以跨 snapshot 延续。
Graph 结果可以生成 memory suggestion，但不能自动写 confirmed memory。
```

---

## 3. 总体架构设计

### 3.1 Graph Context Layer

新增模块：

```text
src/main/java/com/devharnesskit/dhk/
  command/graph/
  model/graph/
  repository/graph/
  service/graph/
  export/GraphContextRenderer.java
  export/ImpactMapRenderer.java
```

建议服务分层：

```text
GraphCommand
  -> GraphService
     -> GraphIndexService
     -> GraphParserRegistry
     -> GraphQueryService
     -> GraphImpactService
     -> GraphExportService
     -> GraphProviderAdapter
        -> LiteGraphProvider
        -> CgcGraphProvider
        -> ExternalGraphProvider
```

### 3.2 Provider 策略

不要一开始强依赖外部图数据库。建议支持：

```text
provider = lite | cgc | external
```

#### lite provider

DevHarnessKit 自己解析基础结构，写入本地 SQLite。

优点：

```text
零外部服务
符合 local-first
容易集成 goal/workflow/spec
适合 Java 企业项目第一阶段
```

缺点：

```text
语言覆盖有限
复杂调用关系准确度有限
```

#### cgc provider

通过 CodeGraphContext CLI 或 MCP adapter 获取图谱信息，再转换成 DevHarnessKit 标准输出。

优点：

```text
快速获得多语言图谱能力
已有 CLI/MCP/visualization/graph backend 生态
```

缺点：

```text
引入 Python 和图数据库依赖
部署复杂度上升
企业内网可能需要额外审批
```

#### external provider

面向未来：Neo4j、Kuzu、Sourcegraph-like 平台、内部代码图谱服务。

---

## 4. SQLite Graph Lite 数据模型

建议新增 schema v6。

### 4.1 code_graph_snapshot

```sql
CREATE TABLE code_graph_snapshot (
  snapshot_id TEXT PRIMARY KEY,
  project_key TEXT NOT NULL,
  provider TEXT NOT NULL DEFAULT 'lite',
  git_commit TEXT NOT NULL DEFAULT '',
  git_dirty INTEGER NOT NULL DEFAULT 0,
  root_path TEXT NOT NULL DEFAULT '',
  language_summary TEXT NOT NULL DEFAULT '',
  file_count INTEGER NOT NULL DEFAULT 0,
  node_count INTEGER NOT NULL DEFAULT 0,
  edge_count INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'ready',
  created_at TEXT NOT NULL
);
```

### 4.2 code_graph_file

```sql
CREATE TABLE code_graph_file (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  snapshot_id TEXT NOT NULL,
  project_key TEXT NOT NULL,
  file_path TEXT NOT NULL,
  language TEXT NOT NULL DEFAULT '',
  file_hash TEXT NOT NULL DEFAULT '',
  encoding TEXT NOT NULL DEFAULT '',
  line_count INTEGER NOT NULL DEFAULT 0,
  indexed_at TEXT NOT NULL,
  UNIQUE(snapshot_id, file_path)
);
```

### 4.3 code_graph_node

```sql
CREATE TABLE code_graph_node (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  snapshot_id TEXT NOT NULL,
  project_key TEXT NOT NULL,
  node_type TEXT NOT NULL,
  symbol_name TEXT NOT NULL DEFAULT '',
  qualified_name TEXT NOT NULL DEFAULT '',
  file_path TEXT NOT NULL DEFAULT '',
  start_line INTEGER NOT NULL DEFAULT 0,
  end_line INTEGER NOT NULL DEFAULT 0,
  language TEXT NOT NULL DEFAULT '',
  visibility TEXT NOT NULL DEFAULT '',
  signature TEXT NOT NULL DEFAULT '',
  confidence INTEGER NOT NULL DEFAULT 70,
  source TEXT NOT NULL DEFAULT 'lite',
  created_at TEXT NOT NULL
);
```

Node type 建议：

```text
file
package
class
interface
enum
method
function
field
annotation
xml_mapper
sql_statement
jsp_page
route
config_key
test_case
build_target
```

### 4.4 code_graph_edge

```sql
CREATE TABLE code_graph_edge (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  snapshot_id TEXT NOT NULL,
  project_key TEXT NOT NULL,
  edge_type TEXT NOT NULL,
  from_node_id INTEGER NOT NULL,
  to_node_id INTEGER NOT NULL,
  evidence TEXT NOT NULL DEFAULT '',
  confidence INTEGER NOT NULL DEFAULT 70,
  source TEXT NOT NULL DEFAULT 'lite',
  created_at TEXT NOT NULL
);
```

Edge type 建议：

```text
contains
imports
extends
implements
calls
references
reads
writes
maps_to
handles_route
renders_view
uses_config
uses_table
uses_column
tested_by
builds
runs
```

### 4.5 code_graph_query_cache

```sql
CREATE TABLE code_graph_query_cache (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  snapshot_id TEXT NOT NULL,
  project_key TEXT NOT NULL,
  query_type TEXT NOT NULL,
  query_text TEXT NOT NULL,
  result_path TEXT NOT NULL DEFAULT '',
  result_summary TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL
);
```

### 4.6 goal_graph_binding

```sql
CREATE TABLE goal_graph_binding (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  goal_key TEXT NOT NULL,
  snapshot_id TEXT NOT NULL,
  binding_type TEXT NOT NULL,
  artifact_path TEXT NOT NULL DEFAULT '',
  summary TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL
);
```

用途：把某个 goal 与 graph snapshot / impact map / graph context 绑定。

---

## 5. CLI 设计

### 5.1 顶层命令

新增：

```bash
dhk graph <subcommand>
```

子命令：

```text
init
index
status
symbol
impact
callers
callees
path
query
export
watch
doctor
```

### 5.2 dhk graph init

```bash
dhk graph init --project-root .
```

行为：

```text
创建 .agents/graph/
创建默认 graph 配置
初始化 schema v6
输出 provider 当前配置
```

生成：

```text
.agents/graph/config.json
.agents/graph/exports/
```

示例配置：

```json
{
  "provider": "lite",
  "include": ["src/**", "app/**", "web/**"],
  "exclude": ["target/**", "build/**", "node_modules/**", "*.class", "*.jar"],
  "languages": ["java", "xml", "jsp", "properties", "sql"],
  "maxFileBytes": 1048576,
  "indexGeneratedFiles": false
}
```

### 5.3 dhk graph index

```bash
dhk graph index --project-root .
dhk graph index --project-root . --provider lite
dhk graph index --project-root . --changed-only
```

行为：

```text
扫描文件
识别语言
生成 snapshot
解析 node/edge
写 SQLite
导出 GRAPH_INDEX_REPORT.md
```

输出：

```text
snapshot_id: 20260526-graph-001
provider: lite
files_indexed: 324
nodes: 2104
edges: 5567
report: .agents/graph/exports/GRAPH_INDEX_REPORT.md
```

### 5.4 dhk graph impact

```bash
dhk graph impact --symbol com.demo.OrderService.query
dhk graph impact --file src/main/java/com/demo/OrderService.java
dhk graph impact --route GET:/orders
dhk graph impact --sql-table orders
```

行为：

```text
定位目标 node
查直接 callers/callees
查间接 callers/callees
查 related files/routes/sql/tests
生成 IMPACT_MAP.md
写 goal_graph_binding 可选
```

可选参数：

```text
--depth 2
--goal <goal-key>
--format text|json|md
--include-tests
--include-sql
--include-config
```

### 5.5 dhk graph export

```bash
dhk graph export --goal <goal-key>
dhk graph export --snapshot <snapshot-id>
```

生成：

```text
GRAPH_CONTEXT.md
IMPACT_MAP.md
SYMBOL_CONTEXT.md
```

### 5.6 dhk graph status

```bash
dhk graph status
```

输出：

```text
latest_snapshot: 20260526-graph-001
git_commit: abc123
git_dirty: true
indexed_files: 324
stale_files: 12
provider: lite
last_indexed_at: ...
```

### 5.7 dhk graph doctor

```bash
dhk graph doctor
```

检查：

```text
schema v6
provider config
ignore config
file encoding
large file exclusions
CGC availability if provider=cgc
```

---

## 6. Export 合约设计

### 6.1 GRAPH_CONTEXT.md

用途：给 Agent 的短图谱上下文。

结构：

```md
# GRAPH_CONTEXT

<graph-snapshot>
- snapshot_id: ...
- provider: lite
- git_commit: ...
- git_dirty: true
- created_at: ...
</graph-snapshot>

<graph-summary>
- files: 324
- nodes: 2104
- edges: 5567
- languages: java=78%, xml=15%, jsp=4%, properties=3%
</graph-summary>

<relevant-symbols>
...
</relevant-symbols>

<impact-summary>
...
</impact-summary>

<agent-instructions>
- Treat graph facts as snapshot-bound, not long-term memory.
- If code changes after this snapshot, rerun graph impact before completion.
- Do not infer business rules from graph alone.
</agent-instructions>
```

### 6.2 IMPACT_MAP.md

用途：每次改动前的影响面报告。

结构：

```md
# IMPACT_MAP

<target>
- type: method
- symbol: com.demo.OrderService.query
- file: src/main/java/com/demo/OrderService.java
</target>

<direct-callers>
...
</direct-callers>

<indirect-callers depth="2">
...
</indirect-callers>

<callees>
...
</callees>

<related-routes>
...
</related-routes>

<related-sql>
...
</related-sql>

<related-tests>
...
</related-tests>

<risk-nodes>
...
</risk-nodes>

<recommended-read-files>
...
</recommended-read-files>

<confidence>
- high: relationships from parser/scip
- medium: relationships from text scan
- low: heuristic references
</confidence>
```

### 6.3 GRAPH_SNAPSHOT.json

用于机器读写，供 `goal verify` 判断 freshness。

```json
{
  "snapshot_id": "...",
  "git_commit": "...",
  "git_dirty": true,
  "file_hashes": {
    "src/main/java/...": "sha256"
  },
  "node_count": 2104,
  "edge_count": 5567
}
```

---

## 7. Goal 集成设计

### 7.1 GoalProfile 新增字段

```json
{
  "graph_required": true,
  "graph_provider": "lite",
  "graph_require_fresh_snapshot": true,
  "graph_require_impact_map": true,
  "graph_max_staleness_minutes": 60,
  "graph_actions": "graph_index,graph_impact_analysis,graph_reimpact",
  "required_evidence.graph_impact_analysis": "impact_map,impacted_files,risk_nodes,recommended_read_files"
}
```

### 7.2 Goal action 流程变化

当前 Java profile：

```text
inspect_existing_code
create_change_plan
implement_minimal_change
verify
```

Graph-aware Java profile：

```text
graph_index_or_refresh
graph_impact_analysis
inspect_existing_code
create_change_plan
implement_minimal_change
graph_reimpact
verify
```

Legacy profile：

```text
legacy_inspect
graph_index
graph_impact_analysis
inspect_existing_behavior
create_minimal_plan
implement_small_change
graph_reimpact
legacy_verify
create_rollback_plan
```

### 7.3 Goal next 输出增强

`dhk goal next` 应新增：

```text
graph_snapshot:
  id
  status
  stale
  reason

graph_context:
  GRAPH_CONTEXT.md
  IMPACT_MAP.md

required_graph_action:
  graph_index | graph_impact | graph_reimpact | none
```

### 7.4 Goal verify 新增 graph checks

新增 check：

```text
graph
impact
architecture
change_budget
```

`goal verify` 应判断：

```text
1. 最新 graph snapshot 是否存在。
2. graph snapshot 是否覆盖 changed files。
3. impact map 是否在当前 step 之后生成。
4. 如果 changed files 超出 impact map，verify failed。
5. 如果 graph_reimpact 后影响面扩大，要求记录风险或人工确认。
```

### 7.5 Goal complete 新增绑定

完成时写：

```text
goal_graph_binding:
- snapshot
- impact_map
- graph_context
- post_change_impact_map
```

并在 `GOAL_SUMMARY.md` 中包含：

```text
Graph snapshot used
Pre-change impact summary
Post-change impact summary
Newly impacted nodes
Graph limitations
```

---

## 8. Legacy 项目专项设计

Graph-aware Harness 对老旧项目尤其重要。

### 8.1 Legacy graph 需要支持的关系

```text
JSP action -> Controller / Servlet
Struts action -> Action class
Spring XML bean -> class
Controller -> Service
Service -> DAO / Mapper
Mapper XML -> SQL statement
SQL statement -> table / column
properties key -> config usage
scheduler config -> job class
web.xml -> servlet/filter/listener
```

第一版不要求完美，但要生成可用的 `IMPACT_MAP.md`。

### 8.2 Legacy 专属命令

```bash
dhk legacy inspect
dhk legacy graph-map
dhk legacy impact
dhk legacy rollback-plan
```

其中 `legacy graph-map` 可以调用 graph 模块，但输出 legacy-specific 报告：

```text
LEGACY_GRAPH_MAP.md
LEGACY_ENTRYPOINTS.md
LEGACY_SQL_MAP.md
LEGACY_CONFIG_MAP.md
```

### 8.3 Legacy Goal Profile

建议新增：

```text
legacy-java-small-fix-with-graph
legacy-jsp-change-with-graph
legacy-sql-change-with-graph
legacy-bugfix-with-graph
```

核心约束：

```text
不做大重构
不格式化大文件
不改公共基础类
不改生产配置
必须有 impact map
必须有 rollback plan
必须有 manual evidence 或 smoke evidence
```

---

## 9. Modern 项目专项设计

新项目中的 graph 用途不同，重点是架构边界和重构安全。

### 9.1 支持能力

```bash
dhk graph boundary-check --module order
dhk graph dependency-rule --from controller --to repository
dhk graph impact --route GET:/orders
dhk graph test-gap --symbol OrderService.query
dhk graph unused --module payment
```

### 9.2 Modern Profiles

```text
modern-java-api-change-with-graph
safe-refactor-with-graph
architecture-boundary-check
test-gap-analysis
module-dependency-review
```

### 9.3 Goal verify 结合 architecture check

```text
如果新增 controller -> mapper 直接依赖，verify failed。
如果 service 调用跨 bounded context repository，verify warning 或 failed。
如果影响公共 API，要求新增 acceptance。
```

---

## 10. Parser / Indexer 实现策略

### 10.1 不建议第一版上复杂 AST 引擎

为了保持 DevHarnessKit 当前 Java + SQLite + local-first 的轻量性，第一版建议做 Graph Lite：

```text
Java: 轻量 class/method/import/call 解析
XML: Spring/MyBatis/Struts/web.xml 结构提取
JSP: form/action/link/scriptlet 简单提取
properties/yml: key/value + sensitive guard
SQL: table/column heuristic
```

### 10.2 Java 解析第一版

可先用轻量规则：

```text
package
import
class/interface/enum
method signature
extends/implements
method call pattern
annotation route mapping
```

再逐步升级到：

```text
JavaParser adapter
Tree-sitter adapter
SCIP adapter
```

### 10.3 XML 解析第一版

重点解析：

```text
MyBatis mapper namespace / select / insert / update / delete id
Spring bean id/class
Struts action path/type/forward
web.xml servlet/filter/listener/url-pattern
```

### 10.4 JSP 解析第一版

重点解析：

```text
form action
href/action urls
JSTL include
scriptlet class references
input field names
```

---

## 11. CGC Adapter 设计

### 11.1 配置

```json
{
  "graph": {
    "provider": "cgc",
    "command": "codegraphcontext",
    "repoPath": ".",
    "database": "kuzu",
    "autoIndex": true
  }
}
```

### 11.2 Adapter 命令

DevHarnessKit 调用：

```bash
codegraphcontext index .
codegraphcontext analyze callers <symbol>
codegraphcontext analyze tree <class>
codegraphcontext analyze dead-code
```

然后转换成标准输出：

```text
GRAPH_CONTEXT.md
IMPACT_MAP.md
```

### 11.3 Adapter 原则

```text
CGC 是可选 provider。
DevHarnessKit 不依赖 CGC 才能运行。
所有外部 provider 结果都必须转换为统一 GraphContext 模型。
Graph 结果必须绑定 snapshot/provider/source/confidence。
```

---

## 12. Policy 与安全

### 12.1 不索引敏感文件

默认排除：

```text
.env
*.key
*.pem
*secret*
*password*
application-prod.*
jdbc.properties
credentials.*
```

### 12.2 Index 前 sensitive guard

如果文件疑似敏感：

```text
不写入 graph node/edge content。
只记录 file skipped。
```

### 12.3 Graph export 脱敏

`GRAPH_CONTEXT.md` 和 `IMPACT_MAP.md` 不能输出：

```text
password
token
secret
JDBC URL with credentials
private key
cookie
authorization header
```

### 12.4 Protected file policy

如果 impact map 涉及 protected file：

```text
goal next 标记 high risk
goal step 需要 human approval evidence
goal verify 不允许自动通过
```

---

## 13. 性能与增量索引

### 13.1 文件 hash

每个文件记录：

```text
file_path
sha256
last_modified
size
encoding
```

### 13.2 changed-only index

```bash
dhk graph index --changed-only
```

只重建变化文件相关 node/edge。

### 13.3 大仓库限制

默认限制：

```text
maxFileBytes = 1MB
maxIndexedFiles = 5000
maxImpactDepth = 3
maxExportNodes = 200
```

超出时：

```text
写 truncation report
不静默丢失
```

### 13.4 watch 可选

`dhk graph watch` 不应成为默认 daemon。DevHarnessKit 原则仍然是短生命周期 CLI。

建议：

```text
watch 作为可选开发模式。
goal verify 不依赖 watch，只检查 snapshot freshness。
```

---

## 14. Skills 改造

新增 skill：

```text
devharness-graph-aware-development
```

核心规则：

```text
1. 不允许直接改代码。
2. 先 goal start 或 goal resume。
3. 如果 GOAL_CONTEXT 要求 graph action，必须先执行 graph 命令。
4. 修改前必须读取 IMPACT_MAP.md。
5. 修改后必须执行 graph reimpact。
6. 如果 post-change impact 扩大，必须记录风险。
7. 完成前必须 goal verify。
```

Legacy skill：

```text
devharness-legacy-graph-development
```

补充规则：

```text
不格式化大文件
不改公共基础类
不改生产配置
必须提供 rollback plan
必须提供 manual evidence 或 smoke evidence
```

---

## 15. 测试计划

### 15.1 Unit Tests

```text
GraphFileScannerTest
GraphJavaLiteParserTest
GraphXmlParserTest
GraphJspParserTest
GraphIndexRepositoryTest
GraphImpactServiceTest
GraphContextRendererTest
ImpactMapRendererTest
```

### 15.2 Integration Tests

```text
GraphIndexIntegrationTest
GraphImpactIntegrationTest
GoalGraphIntegrationTest
LegacyGraphIntegrationTest
GraphFreshnessGoalVerifyTest
```

### 15.3 Fixtures

```text
src/test/resources/fixtures/graph/java-api-project
src/test/resources/fixtures/graph/legacy-jsp-struts-project
src/test/resources/fixtures/graph/mybatis-project
src/test/resources/fixtures/graph/spring-xml-project
```

### 15.4 必测场景

```text
1. graph index 生成 snapshot/node/edge。
2. graph impact 找到 caller/callee。
3. goal start 自动要求 graph impact。
4. 没有 IMPACT_MAP 时 goal verify failed。
5. 修改文件后 graph snapshot stale，goal verify failed。
6. graph reimpact 后新增影响面，goal verify 要求 risk evidence。
7. protected file 出现在 impact map，goal 标记 high risk。
8. sensitive 文件不会被索引内容。
```

---

## 16. 迭代路线图

## V0.5.0 Graph Lite Core

目标：DevHarnessKit 自己具备最小代码图谱能力。

交付：

```text
schema v6
model/graph
repository/graph
GraphIndexService
GraphImpactService
GraphContextRenderer
ImpactMapRenderer
dhk graph index/status/impact/export
Java/XML/properties 基础解析
```

验收：

```text
可以对 Java + MyBatis 项目生成 IMPACT_MAP.md。
```

## V0.5.1 Graph-aware Goal

目标：Goal 强制使用 graph context。

交付：

```text
GoalProfile graph fields
GraphGoalBinding
goal next graph action
goal verify graph freshness
goal complete graph artifact binding
Graph-aware skills
```

验收：

```text
graph_required profile 下，没有新鲜 IMPACT_MAP.md 不能 complete。
```

## V0.5.2 Legacy Graph Harness

目标：服务老旧项目。

交付：

```text
legacy inspect
legacy graph-map
JSP/Struts/Spring XML/MyBatis relationship extraction
legacy-java-small-fix-with-graph profile
rollback plan integration
manual evidence check
```

验收：

```text
能对一个 JSP + Controller + Service + Mapper 老项目生成影响面和回滚计划。
```

## V0.5.3 Modern Graph Harness

目标：服务新项目架构治理。

交付：

```text
boundary check
dependency direction check
test gap analysis
safe-refactor-with-graph profile
```

验收：

```text
跨层依赖违规时 goal verify failed。
```

## V0.5.4 CGC Adapter

目标：可选接入 CodeGraphContext。

交付：

```text
graph provider cgc
CGC availability doctor
CGC command adapter
CGC result normalization
```

验收：

```text
安装 codegraphcontext 后，DevHarnessKit 可以使用 CGC 结果生成 IMPACT_MAP.md。
```

---

## 17. 文件改造清单

### 17.1 新增代码目录

```text
src/main/java/com/devharnesskit/dhk/command/graph/
src/main/java/com/devharnesskit/dhk/model/graph/
src/main/java/com/devharnesskit/dhk/repository/graph/
src/main/java/com/devharnesskit/dhk/service/graph/
```

### 17.2 新增导出类

```text
src/main/java/com/devharnesskit/dhk/export/GraphContextRenderer.java
src/main/java/com/devharnesskit/dhk/export/ImpactMapRenderer.java
src/main/java/com/devharnesskit/dhk/export/SymbolContextRenderer.java
```

### 17.3 修改现有类

```text
CommandRouter.java          增加 graph namespace
MigrationRunner.java        增加 schema v6
PathUtil.java               增加 graph paths
GoalProfile.java            增加 graph 字段
GoalProfileService.java     增加 graph-aware profiles
GoalOrchestrator.java       集成 graph required / graph verify
GoalCompletionEvaluator.java 增加 graph freshness blocker
GoalContextRenderer.java    输出 graph context 信息
GoalSummaryRenderer.java    输出 graph snapshot / impact summary
DoctorCommand.java          增加 graph doctor
```

### 17.4 新增 docs

```text
docs/GRAPH_CONTEXT_LAYER.md
docs/GRAPH_SCHEMA.md
docs/GRAPH_GOAL_INTEGRATION.md
docs/LEGACY_GRAPH_HARNESS.md
docs/CGC_ADAPTER.md
```

---

## 18. 风险与缓解

| 风险 | 说明 | 缓解 |
|---|---|---|
| 图谱不准 | 轻量解析会漏掉动态调用 | 每条 edge 带 confidence/source；低置信度明确标注 |
| 误导 Agent | Agent 把 graph 当长期事实 | GRAPH_CONTEXT 强调 snapshot-bound |
| 复杂度扩散 | DevHarnessKit 变成重型图数据库项目 | 默认 Graph Lite，可选 adapter，不做强依赖 |
| 老项目解析困难 | JSP/XML/Struts/iBatis 复杂 | 先做 heuristic + evidence，不追求完美 |
| 性能问题 | 大仓库索引慢 | changed-only、hash、size limit、truncation report |
| 安全泄漏 | graph export 输出敏感配置 | sensitive guard + ignore + redaction |
| 依赖膨胀 | 引入 Tree-sitter/Neo4j/Kuzu 复杂 | 第一版只用 SQLite Lite，外部 provider 可选 |

---

## 19. 验收标准

### V0.5 Beta 必须满足

```text
1. dhk graph index 能生成 snapshot/node/edge。
2. dhk graph impact 能生成 IMPACT_MAP.md。
3. graph_required profile 下，goal verify 要求新鲜 IMPACT_MAP。
4. 修改相关文件后，旧 graph snapshot 被判定 stale。
5. graph result 绑定 goal artifact。
6. sensitive 文件不会输出到 graph context。
7. legacy fixture 能生成 JSP/Controller/Service/Mapper 影响面。
8. Java API fixture 能生成 Controller/Service/Mapper/Test 影响面。
```

### 不要求第一版完成

```text
1. 完整准确的 Java type resolution。
2. 完整跨语言调用分析。
3. 完整可视化 HTML。
4. 完整 MCP server。
5. 完整 Neo4j/Kuzu 内置支持。
```

---

## 20. 最终建议

DevHarnessKit 下一阶段不应只做 Legacy Harness，而应做：

```text
V0.5 Graph-aware Goal Harness
```

原因：

```text
老项目需要 graph 来降低影响面不清的风险。
新项目需要 graph 来做架构边界和重构安全。
弱模型需要 graph 来减少盲目读文件和错误推断。
强模型也需要 graph 来减少上下文浪费和提升确定性。
```

最终目标：

```text
Goal 负责流程。
Graph 负责代码结构。
Policy 负责边界。
Checks 负责验证。
Memory 负责长期事实。
Spec 负责验收。
Workflow 负责审计。
```

一句话：

```text
DevHarnessKit 应从 Goal-driven Harness 升级为 Graph-aware Goal Harness。
```

---

## 21. 参考资料

- CodeGraphContext GitHub: https://github.com/CodeGraphContext/CodeGraphContext
- CodexGraph: https://arxiv.org/abs/2408.03910
- Codebase-Memory: https://arxiv.org/abs/2603.27277
- Repository Intelligence Graph: https://arxiv.org/abs/2601.10112
- Code Property Graph: https://en.wikipedia.org/wiki/Code_property_graph
