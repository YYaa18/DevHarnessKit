# DevHarness Kit CLI MVP 实施计划

本版补充内容：

```text
1. 明确 MVP 仍然只做 Memory Core + DB Readonly，不把 workflow 混入 MVP。
2. 增加 V0.2 workflow 持久化实施计划，用 SQLite 承载 phase / gate / run / event。
3. 增加 CLI 轻量化实施要求：懒加载、无后台、无服务、无重框架、输出大小控制和性能冒烟测试。
```


生成日期：2026-05-21
来源文档：`docs/PRD.md`

项目名称：`DevHarness Kit`

命名约定：

- 产品/项目展示名使用 `DevHarness Kit`。
- Java package 和目录名使用无空格小写标识：`devharnesskit`。
- 顶层 CLI 命令名使用 `dhk`。
- SQLite 项目记忆能力放在 `dhk memory ...` 命名空间。
- 业务数据库只读查询能力放在 `dhk db ...` 命名空间。

## 1. 目标

实现 DevHarness Kit CLI 的第一个可用 MVP：一个本地 Java CLI 工具，先稳定支持 SQLite 项目记忆管理，再把业务数据库只读查询作为受控的第二交付层接入。

MVP-A 要先证明这条 Memory Core 闭环：

```text
memory init 初始化 memory.db
-> memory add 写入 draft 项目事实
-> memory confirm 确认可信事实
-> memory search 检索记忆
-> memory export 导出 CURRENT_CONTEXT.md
-> memory checkpoint 保存任务断点
-> memory recover 导出 RECOVERY_CONTEXT.md
-> doctor 检查 CLI、SQLite、MySQL driver 和导出目录
```

MVP-B 在 MVP-A 稳定后合入：

```text
db test 动态注入 MySQL 连接并验证
-> db sql 执行业务库只读查询并获取结果
-> SQL_RESULT.md 作为临时查询结果供 Agent 读取
```

第一版不做自动扫描、不做复杂知识图谱、不做 GUI，也不直接调用大模型。重点是把“人工确认过的项目事实”稳定变成“新窗口可读取的短上下文”。

## 2. MVP 范围

### MVP-A 必须实现：Memory Core

- Maven Java CLI 工程。
- 可执行 fat jar：`target/dhk-cli-0.1.0-all.jar`。
- SQLite 本地数据库：`.agents/memory/memory.db`。
- 稳定项目身份文件：`.agents/memory/project.json`。
- 核心命令：
  - `help`
  - `doctor`
  - `memory init`
  - `memory add`
  - `memory confirm`
  - `memory search`
  - `memory export`
  - `memory checkpoint`
  - `memory recover`
- MVP 数据表：
  - `schema_version`
  - `project`
  - `memory_item`
  - `checkpoint`
  - `memory_fts`，如果当前 SQLite 支持 FTS5
- Markdown 导出文件：
  - `.agents/memory/exports/CURRENT_CONTEXT.md`
  - `.agents/memory/exports/RECOVERY_CONTEXT.md`
  - `.agents/memory/exports/PROJECT_INDEX.md`
- 敏感信息拦截：
  - `memory add` 时拦截。
  - `memory checkpoint` 时拦截。
  - `memory export` 时再次过滤。

### MVP-B 受控实现：DB Readonly

- MySQL Connector/J 5.1 驱动，用于查询业务数据库：
  - Maven 坐标：`mysql:mysql-connector-java:5.1.49`
  - Driver class：`com.mysql.jdbc.Driver`
- 核心命令：
  - `db test`
  - `db sql`
- SQL 安全执行：
  - 默认只允许只读 SQL。
  - 默认限制返回行数。
  - 默认不把数据库连接串、密码、结果集写入 memory。
  - 查询结果可输出到 stdout 或 `.agents/memory/exports/SQL_RESULT.md`。
  - Skill 不自动调用 `db` 命令，只有用户明确要求验证业务 SQL 时才使用。

### MVP-A / MVP-B 共用交付物

- Windows 和 macOS/Linux 包装脚本：
  - `.bat`
  - `.sh`
- 跨客户端 Skill/Rule 包：
  - `.agents/skills/devharness-java-development/SKILL.md`
  - `.agents/skills/devharness-java-development/scripts/*`
  - `.agents/skills/devharness-java-development/references/*`
  - `.comate/rules/project-memory-bootstrap.mdr`
  - `.comate/rules/java-development-guard.mdr`
- 轻量化与性能预算：
  - 不做常驻 daemon。
  - 不启动本地 HTTP 服务。
  - 不监听端口。
  - 每次命令由 Skill 脚本启动，执行完成后进程自然退出。
  - 默认 JVM 参数：`-Xms16m -Xmx128m -Dfile.encoding=UTF-8`。
  - fat jar 目标 <= 25MB，超过 35MB 必须解释。
- 单元测试和集成测试。

### 暂缓实现

- `scan`
- `compact`
- `module_profile` 自动分析
- `decision_log` 独立命令
- `memory list`
- `memory redact`
- `json` 输出
- 复杂 FTS 排名
- 多项目共享
- 图形界面
- 直接连接 LLM API
- 自动判断 API/MVC 项目类型
- 本地服务模式 / daemon 模式
- workflow_template / workflow_run / workflow_gate 等工作流持久化表（V0.2）
- workflow CLI：template/start/status/export/gate（V0.2）
- spec_change / spec_task 等规格驱动开发表（V0.3）
- 连接池
- Spring / Guice / ORM

这些能力不要混进 MVP。MVP 只先保证人工写入、人工确认、稳定导出、断点恢复。


### CLI 轻量化实施约束

本项目是 Agent Skill 的本地执行工具，不是独立服务。MVP 实现时必须遵守以下约束：

```text
1. Main / CommandRouter 只解析参数和分发命令，不打开数据库。
2. help 命令不得访问文件系统和数据库。
3. memory 命令只能打开 SQLite，不加载 MySQL driver。
4. db 命令内部再加载 com.mysql.jdbc.Driver。
5. SQLite 每个命令一个短连接，不使用连接池。
6. 默认不启用 WAL；如后续需要并发，再通过 DHK_SQLITE_WAL=1 开启。
7. 不引入 Spring / Guice / ORM / YAML parser / JSON 第三方库 / 日志框架。
8. Markdown 渲染使用 StringBuilder，并控制输出大小。
9. 所有脚本只启动 Java 进程、等待退出、透传退出码。
10. 不允许遗留后台 Java 进程。
```

SQLite 默认 PRAGMA：

```sql
PRAGMA foreign_keys = ON;
PRAGMA busy_timeout = 3000;
```

可选 WAL 配置仅在后续版本启用：

```text
DHK_SQLITE_WAL=1
```

此时执行：

```sql
PRAGMA journal_mode = WAL;
PRAGMA synchronous = NORMAL;
```

如果 fat jar 超过 35MB，需要评估双分发模式：

```text
dhk-core.jar: memory-only
dhk-full.jar: memory + db
```

MVP 默认仍优先单 jar，减少 Skill 脚本复杂度。

---

### 性能预算

| 指标 | MVP 目标 |
| --- | ---: |
| fat jar 大小 | 目标 <= 25MB，超过 35MB 必须解释 |
| `dhk help` 冷启动 | <= 1.5s |
| `dhk doctor` | <= 2s |
| `dhk memory search` | 1000 条 memory 内 <= 1s |
| `dhk memory export` | 1000 条 memory 内 <= 2s |
| `dhk db test` | 默认连接超时 5s |
| `dhk db sql` | 默认查询超时 30s |
| JVM 堆 | 默认 `-Xmx128m`，基础命令应能在 64MB 下运行 |
| `SQL_RESULT.md` | 默认 <= 64KB |

## 3. 关键设计决策

1. **MVP 从手工记忆开始，不从 scan 开始。**
   `scan` 会引入误判风险，尤其是弱模型会把 draft 当事实。第一版先让用户明确写入事实，再确认，再导出。

2. **`draft` 永远不进入默认导出。**
   `dhk memory export` 默认只包含：
   - `status = confirmed`
   - `confidence >= 70`
   - 未归档
   - 未废弃
   - 未命中敏感信息规则

3. **FTS5 是增强能力，不是必需能力。**
   如果 `CREATE VIRTUAL TABLE ... fts5` 失败，工具必须降级到 `LIKE + tags` 搜索，而不是初始化失败。

4. **业务数据库查询是工具能力，不是记忆内容。**
   `dhk db sql` 用来查询业务库，帮助理解业务代码和验证 SQL 逻辑；但连接信息、密码、生产地址、原始结果集默认不进入 SQLite memory。只有人工总结后的结论才能通过 `dhk memory add` 写成 draft memory。

5. **SQL 默认只读。**
   MVP 不支持写 SQL。默认只允许 `SELECT`、`SHOW`、`DESC`、`DESCRIBE`、`EXPLAIN`。禁止 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`TRUNCATE`、`CREATE`、`GRANT`、`REVOKE`、`CALL`、`SET` 等会改变状态或权限的语句。

6. **数据库密码不能通过普通命令行参数传入。**
   不提供 `--password`。只允许：
   - `--password-env DHK_DB_PASSWORD`
   - `--password-stdin`

7. **项目身份不能只依赖绝对路径。**
   `dhk memory init` 生成 `.agents/memory/project.json`，保存稳定 `project_key`。`root_path` 只是元数据，换机器或换盘符后可以更新。

8. **多项目共存靠目录隔离，不做全局共享库。**
   MVP 支持多个项目在同一台机器上共存：每个项目各自拥有 `.agents/memory/memory.db` 和 `project.json`。跨项目检索、全局共享 memory.db、项目间记忆同步都属于后续版本。

9. **MVP schema 只包含命令真正会使用的表。**
   `module_profile` 和 `decision_log` 在 PRD 中有价值，但 MVP 没有对应写入/读取命令。第一版先不建行为，后续用 migration v2 加。

10. **敏感信息清理路径要尽早考虑，但不扩大 MVP。**
   MVP 至少要在 `memory add`、`memory checkpoint`、`memory export` 和 `SQL_RESULT.md` 写出阶段拦截敏感信息。MVP 不实现 `memory redact`；如果敏感信息误写入 `memory.db`，先通过删除并重新初始化、sqlite 手工清理污染行，或等待 MVP+ 的 `memory redact` 处理。

## 4. 建议仓库结构

```text
pom.xml

src/main/java/com/devharnesskit/dhk/
  Main.java

src/main/java/com/devharnesskit/dhk/cli/
  Args.java
  Command.java
  CommandContext.java
  CommandRouter.java
  ExitCodes.java

src/main/java/com/devharnesskit/dhk/command/
  HelpCommand.java
  DoctorCommand.java

src/main/java/com/devharnesskit/dhk/command/memory/
  InitCommand.java
  AddCommand.java
  ConfirmCommand.java
  SearchCommand.java
  ExportCommand.java
  CheckpointCommand.java
  RecoverCommand.java

src/main/java/com/devharnesskit/dhk/command/projectdb/
  TestCommand.java
  SqlCommand.java

src/main/java/com/devharnesskit/dhk/db/
  DbConnectionFactory.java
  MigrationRunner.java
  TransactionTemplate.java

src/main/java/com/devharnesskit/dhk/model/
  Project.java
  MemoryItem.java
  Checkpoint.java
  SearchResult.java
  ExportRequest.java

src/main/java/com/devharnesskit/dhk/repository/
  ProjectRepository.java
  MemoryRepository.java
  CheckpointRepository.java
  FtsRepository.java

src/main/java/com/devharnesskit/dhk/service/
  ProjectService.java
  MemoryService.java
  SearchService.java
  MysqlConnectionService.java
  SqlExecutionService.java
  ExportService.java
  RecoveryService.java
  CheckpointService.java
  SensitiveDataGuard.java
  SqlSafetyGuard.java

src/main/java/com/devharnesskit/dhk/export/
  MarkdownExporter.java
  CurrentContextRenderer.java
  RecoveryContextRenderer.java
  SqlResultRenderer.java

src/main/java/com/devharnesskit/dhk/sql/
  DbConnectionRequest.java
  SqlExecutionRequest.java
  SqlExecutionResult.java
  SqlColumn.java

src/main/java/com/devharnesskit/dhk/util/
  Clock.java
  FileUtil.java
  JsonUtil.java
  PathUtil.java
  TagUtil.java
  TextUtil.java

src/test/java/com/devharnesskit/dhk/
  cli/
  command/
  db/
  repository/
  service/
  integration/

.agents/skills/devharness-java-development/
  SKILL.md
  scripts/
    dhk.bat
    dhk.sh
    memory-init.bat
    memory-init.sh
    memory-export.bat
    memory-export.sh
    db-test.bat
    db-test.sh
    db-sql.bat
    db-sql.sh
    memory-recover.bat
    memory-recover.sh
    memory-checkpoint.bat
    memory-checkpoint.sh
  references/
    memory-contract.md
    api-development-flow.md
    mvc-development-flow.md
    recovery-flow.md
    memory-export-format.md

.comate/rules/
  project-memory-bootstrap.mdr
  java-development-guard.mdr
```

## 5. 分阶段开发内容

### 阶段 0：创建 Maven CLI 骨架

目标：先得到一个能运行的 Java CLI，不接数据库。

开发内容：

- 创建 `pom.xml`。
- 配置 Java 编译目标，优先兼容 Java 8 语法。
- 加入依赖：
  - `org.xerial:sqlite-jdbc`
  - `mysql:mysql-connector-java:5.1.49`
  - `org.junit.jupiter:junit-jupiter`
- 配置 Maven Shade Plugin。
- 配置 `ServicesResourceTransformer`，避免 SQLite JDBC 或 MySQL JDBC 打包后找不到 driver。
- 显式兼容 MySQL 5.1 driver class：`com.mysql.jdbc.Driver`。
- 不引入 Spring、依赖注入框架、ORM、连接池或日志框架。
- 创建 `Main`。
- 创建 `CommandRouter`。
- 实现 `help` 命令。
- 实现基础退出码。
- 记录 fat jar 大小。

验收命令：

```text
mvn clean test
mvn -DskipTests package
java -jar target/dhk-cli-0.1.0-all.jar help
```

验收结果：

- `help` 输出命令列表。
- 未知命令返回 usage error。
- jar 可以独立运行。
- `help` 不打开 SQLite 或 MySQL 连接。
- fat jar 大小被记录；超过 35MB 必须说明原因。

### 阶段 1：实现 CLI 基础设施

目标：所有命令共用稳定的参数解析、路径处理和上下文对象。

开发内容：

- 实现 `Args`：
  - 支持 `command`
  - 支持 `--key value`
  - 支持 `--flag`
  - 重复参数 MVP 中采用 last-write-wins
  - 不支持复杂短参数，避免第一版 parser 失控
- 实现 `ExitCodes`：
  - `0` 成功
  - `1` 运行时错误
  - `2` 参数/用法错误
  - `3` 校验或安全拒绝
  - `4` 目标不存在
- 实现 `CommandContext`：
  - 当前工作目录
  - 解析后的 `projectRoot`
  - stdout/stderr 输出器
  - 时钟对象
- 实现 `PathUtil`：
  - 解析 `--project-root`
  - 处理相对路径和绝对路径
  - 创建 `.agents/memory`
  - 创建 `.agents/memory/exports`
  - 定位 `.agents/memory/project.json`
  - 定位 `.agents/memory/memory.db`
- 实现轻量 `JsonUtil`：
  - 只读写简单字符串字段
  - 用于 `project.json`
  - 不额外引入 JSON 依赖

验收标准：

- 缺少必填参数时输出清晰错误。
- 路径包含空格时测试通过。
- 从子目录运行时仍能通过 `--project-root` 定位项目。

### 阶段 2：实现 SQLite 初始化和迁移

目标：建立可重复初始化的本地记忆库。

开发内容：

- 实现 `DbConnectionFactory`：
  - 连接 `.agents/memory/memory.db`
  - 执行 `PRAGMA foreign_keys = ON`
  - 设置合理 busy timeout
- 实现 `MigrationRunner`：
  - migration v1 创建 MVP 表
  - migration 幂等
  - 写入 `schema_version`
- 实现 FTS 初始化：
  - 尝试创建 `memory_fts`
  - 如果失败，记录当前进程 FTS 不可用
  - 不阻断 `dhk memory init`
- 实现 repository：
  - `ProjectRepository`
  - `MemoryRepository`
  - `CheckpointRepository`
  - `FtsRepository`
- 添加索引：
  - `memory_item(project_key, status, confidence)`
  - `memory_item(project_key, module_name)`
  - `checkpoint(project_key, created_at)`

MVP schema：

```sql
CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER PRIMARY KEY,
    description TEXT NOT NULL,
    applied_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS project (
    project_key TEXT PRIMARY KEY,
    project_name TEXT NOT NULL,
    root_path TEXT NOT NULL,
    project_type TEXT NOT NULL DEFAULT 'unknown',
    language TEXT NOT NULL DEFAULT 'java',
    framework TEXT NOT NULL DEFAULT 'unknown',
    database_type TEXT NOT NULL DEFAULT 'unknown',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS memory_item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_key TEXT NOT NULL,
    module_name TEXT NOT NULL DEFAULT 'global',
    memory_type TEXT NOT NULL,
    scope TEXT NOT NULL DEFAULT 'project',
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    tags TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'draft',
    confidence INTEGER NOT NULL DEFAULT 50,
    source_kind TEXT NOT NULL DEFAULT 'manual',
    confirmed_at TEXT NOT NULL DEFAULT '',
    confirmed_by TEXT NOT NULL DEFAULT '',
    source_files TEXT NOT NULL DEFAULT '',
    evidence TEXT NOT NULL DEFAULT '',
    effective_from TEXT NOT NULL DEFAULT '',
    effective_to TEXT NOT NULL DEFAULT '',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    last_used_at TEXT,
    use_count INTEGER NOT NULL DEFAULT 0,
    CHECK (status IN ('draft', 'confirmed', 'deprecated', 'archived')),
    CHECK (confidence >= 0 AND confidence <= 100),
    CHECK (memory_type IN (
      'project_fact',
      'api_convention',
      'mvc_convention',
      'gateway_convention',
      'database_convention',
      'code_pattern',
      'module_pattern',
      'exception_convention',
      'logging_convention',
      'security_convention',
      'testing_convention',
      'decision',
      'risk',
      'todo'
    )),
    FOREIGN KEY (project_key) REFERENCES project(project_key)
);

CREATE TABLE IF NOT EXISTS checkpoint (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_key TEXT NOT NULL,
    task_name TEXT NOT NULL,
    module_name TEXT NOT NULL DEFAULT 'global',
    summary TEXT NOT NULL,
    changed_files TEXT NOT NULL DEFAULT '',
    pending_items TEXT NOT NULL DEFAULT '',
    verify_status TEXT NOT NULL DEFAULT '',
    next_read_files TEXT NOT NULL DEFAULT '',
    created_at TEXT NOT NULL,
    CHECK (length(task_name) > 0),
    CHECK (length(summary) > 0),
    FOREIGN KEY (project_key) REFERENCES project(project_key)
);
```

验收命令：

```text
java -jar target/dhk-cli-0.1.0-all.jar memory init --project-root .
java -jar target/dhk-cli-0.1.0-all.jar memory init --project-root .
```

验收结果：

- `.agents/memory/memory.db` 存在。
- `.agents/memory/project.json` 存在。
- `.agents/memory/exports/PROJECT_INDEX.md` 存在。
- 重复执行 `dhk memory init` 不报错、不重复破坏数据。

### 阶段 3：实现 `memory init` 和 `doctor`

目标：让初始化和环境问题可观测。

`memory init` 开发内容：

- 解析 `--project-root`。
- 创建 `.agents/memory` 和 `.agents/memory/exports`。
- 如果缺少 `project.json`，生成稳定 `project_key`。
- 写入或更新 `project` 表。
- 执行 migration。
- 生成极短 `PROJECT_INDEX.md`，控制在 50 行以内。
- `PROJECT_INDEX.md` 包含：
  - project_key
  - project_name
  - project_type/framework/database_type，未知时写 `unknown`
  - CURRENT_CONTEXT.md / RECOVERY_CONTEXT.md / SQL_RESULT.md 的用途说明
  - 简短 usage：开发前 export，继续任务 recover，验证数据库时显式调用 db sql
- 输出初始化摘要。

`doctor` 开发内容：

- 输出 Java 版本。
- 输出 project root。
- 检查 `.agents/memory` 是否存在。
- 检查 DB 是否存在。
- 检查 schema version。
- 检查 project 记录。
- 检查 FTS 是否可用。
- 检查 MySQL 5.1 driver `com.mysql.jdbc.Driver` 是否可加载。
- 检查 exports 目录是否可写。
- 统计：
  - memory 总数
  - draft 数
  - confirmed 数
  - checkpoint 数
- 扫描导出文件中明显敏感字段并 warning。

验收命令：

```text
java -jar target/dhk-cli-0.1.0-all.jar doctor --project-root .
```

验收结果：

- 初始化完成时退出码为 `0`。
- DB 缺失、schema 缺失、exports 不可写时给出明确错误。

### 阶段 4：实现记忆写入：`memory add` 和 `memory confirm`

目标：支持人工写入项目事实，并通过确认机制控制是否进入上下文。

`memory add` 开发内容：

- 必填参数：
  - `--type`
  - `--title`
  - `--content`
- 可选参数：
  - `--module`，默认 `global`
  - `--tags`，默认空
  - `--source`，写入 `source_files`
  - `--evidence`，默认空
  - `--confidence`，默认 `50`
- 校验：
  - `memory_type` 必须在允许枚举内
  - `confidence` 必须是 `0..100`
  - title/content 不能为空
- `memory add` 永远写入 `draft`。
- MVP 不接受 `--status confirmed`；如果传 `--status`，只允许 `draft`。
- 调用 `SensitiveDataGuard`。
- 默认拒绝写入敏感信息，退出码 `3`。
- 写入 `memory_item`。
- FTS 可用时同步写入 `memory_fts`。
- 输出新建 memory ID。

`memory confirm` 开发内容：

- 必填参数：
  - `--id`
- 查找 memory。
- 不存在时返回 `4`。
- 设置 `status = confirmed`。
- 写入 `confirmed_at`。
- 写入 `confirmed_by`，默认 `manual`。
- 如果 confidence 小于 70，默认提升到 70。
- 如果用户传入 `--confidence`，按用户值校验并写入。
- 更新 `updated_at`。
- 同步 FTS。

敏感信息 MVP 规则：

```text
password=
passwd=
secret=
token=
accessKey
secretKey
jdbc:mysql://
Authorization:
Bearer
AKIA
```

验收命令：

```text
java -jar target/dhk-cli-0.1.0-all.jar memory add --type gateway_convention --module global --title "User identity from gateway" --content "User ID is read from X-User-Id." --tags "api,gateway,user-id,X-User-Id"
java -jar target/dhk-cli-0.1.0-all.jar memory confirm --id 1
```

验收结果：

- 新增记忆默认是 `draft`。
- `memory add --status confirmed` 被拒绝。
- `memory confirm` 后变为 `confirmed`。
- 敏感内容样例会被拒绝。

### 阶段 5：实现检索：`memory search`

目标：让用户能查找已有记忆，也为 `memory export` 复用检索能力。

开发内容：

- 必填参数：
  - `--q`
- 可选参数：
  - `--module`
  - `--status`
  - `--limit`，默认 `20`
- query 拆 token。
- 搜索策略：
  - FTS 可用时查 `memory_fts`
  - 始终补充 `LIKE title/content/tags`
  - 对 tags 做优先匹配，适配中文内容配英文标签的场景
- 输出字段：
  - id
  - status
  - confidence
  - module
  - memory_type
  - title
  - tags
  - content 摘要

MVP 排序：

```text
+10 module 精确匹配
+8 tags 命中
+5 title 命中
+3 content 命中
+3 confirmed
+2 confidence >= 90
+1 confidence >= 70
```

验收命令：

```text
java -jar target/dhk-cli-0.1.0-all.jar memory search --q "gateway user-id"
```

验收结果：

- 能搜到刚确认的记忆。
- 不指定 `--status` 时可以显示 draft，便于人工确认。
- 指定 `--status confirmed` 时只显示 confirmed。

### 阶段 6（MVP-B，可后置）：实现 MySQL 连接和只读 SQL 执行：`db test` 和 `db sql`

目标：让 `dhk` 能动态注入业务数据库地址，执行只读 SQL，并把结果输出给开发者或导出成短 Markdown，辅助理解业务代码中的数据库查询逻辑。

如果严格按 MVP-A / MVP-B 交付，先跳过本阶段，完成 `memory export`、`memory checkpoint`、`memory recover` 和 Skill/Rules 后，再回来实现本阶段。

依赖和兼容要求：

- 引入 MySQL Connector/J 5.1：
  - `mysql:mysql-connector-java:5.1.49`
  - driver class：`com.mysql.jdbc.Driver`
- JDBC URL 支持：
  - 直接传 `--jdbc-url`
  - 或通过 `--host --port --database` 组装
- 推荐 URL 参数：
  - `useUnicode=true`
  - `characterEncoding=utf8`
  - `useSSL=false`
  - `serverTimezone=Asia/Shanghai` 仅在目标驱动/数据库组合需要时使用

`db test` 开发内容：

- 必填连接参数二选一：
  - `--jdbc-url`
  - `--host --database`，`--port` 默认 `3306`
- 必填认证参数：
  - `--user`
  - `--password-env` 或 `--password-stdin`
- 可选参数：
  - `--connect-timeout-ms`，默认 `5000`
  - `--socket-timeout-ms`，默认 `30000`
- 行为：
  - 加载 `com.mysql.jdbc.Driver`。
  - 建立连接。
  - 设置 `connection.setReadOnly(true)`。
  - 执行轻量探测：`SELECT 1`。
  - 输出数据库产品名、版本。
  - 默认不输出真实 schema 名。
- 禁止行为：
  - 不打印密码。
  - 不把连接串写入 memory。
  - 不把 host/database 写入 checkpoint，除非用户手工脱敏后通过 `dhk memory add` 写入。

`db sql` 开发内容：

- 必填参数：
  - `--sql`
  - `--user`
  - `--password-env` 或 `--password-stdin`
  - 以及 `--jdbc-url` 或 `--host --database`
- 可选参数：
  - `--port`，默认 `3306`
  - `--limit`，默认 `100`
  - `--max-limit`，硬上限 `1000`
  - `--timeout-seconds`，默认 `30`
  - `--format table|md`，默认 `table`
  - `--out`，可写到 `.agents/memory/exports/SQL_RESULT.md`
  - `--explain`，先执行 `EXPLAIN <sql>`
  - `--dry-run`，只执行安全校验，不连接数据库
- SQL 安全校验：
  - fail-closed：无法确认 SQL 安全时默认拒绝执行。
  - MVP 不支持复杂 SQL；宁可误杀，不可误放。
  - 去掉开头注释后判断首个关键字。
  - 默认只允许 `SELECT`、`SHOW`、`DESC`、`DESCRIBE`、`EXPLAIN`。
  - 拒绝 `WITH`。
  - 拒绝多语句执行。
  - 多语句检测必须使用小状态机，至少支持 `normal`、`single_quote`、`double_quote`、`backtick`、`line_comment`、`block_comment`。
  - 只在 `normal` 状态下识别首关键字和语句分号。
  - 拒绝 `INSERT/UPDATE/DELETE/DROP/ALTER/TRUNCATE/CREATE/GRANT/REVOKE/CALL/SET/REPLACE/LOAD`。
  - 拒绝 `SELECT ... INTO OUTFILE` 和 `SELECT ... INTO DUMPFILE`。
  - 拒绝 `SLEEP`、`LOAD_FILE` 等高风险函数。
  - `--explain` 只允许包裹 `SELECT`；如果原 SQL 已经是 `EXPLAIN`，不再二次包裹；拒绝 `EXPLAIN UPDATE/DELETE/INSERT`。
- 执行控制：
  - 使用 `Statement.setQueryTimeout(timeoutSeconds)`。
  - 使用 `Statement.setMaxRows(limit)`。
  - 对结果集做二次截断，避免 driver 不尊重 max rows。
  - 只读取前 `max-limit` 行。
  - 单元格最大长度 `max-cell-length = 200`。
  - 输出最大字节数 `max-output-bytes = 64KB`。
  - `SQL_RESULT.md` 文件最大 64KB。
  - 输出列名、JDBC 类型、行数、是否截断。
- 输出格式：
  - `table`：适合终端阅读，宽度过长时截断。
  - `md`：适合导出给 Agent 阅读。
- 结果保存策略：
  - stdout 默认显示结果。
  - `--out` 只写 exports 文件。
  - 写出 `SQL_RESULT.md` 前再次调用 `SensitiveDataGuard`。
  - 命中明显凭据、token、cookie、Authorization、Bearer、AKIA、手机号、身份证、邮箱等敏感结果时，默认拒绝写出。
  - MVP 不提供 `--allow-sensitive-output`。
  - 不写入 `memory_item`。
  - 如果用户想长期保存结论，必须人工总结后用 `dhk memory add` 写入 draft。

验收命令：

```text
export DHK_DB_PASSWORD='***'
java -jar target/dhk-cli-0.1.0-all.jar db test --jdbc-url "jdbc:mysql://127.0.0.1:3306/demo?useUnicode=true&characterEncoding=utf8&useSSL=false" --user readonly --password-env DHK_DB_PASSWORD
java -jar target/dhk-cli-0.1.0-all.jar db sql --dry-run --sql "SELECT 1"
java -jar target/dhk-cli-0.1.0-all.jar db sql --jdbc-url "jdbc:mysql://127.0.0.1:3306/demo?useUnicode=true&characterEncoding=utf8&useSSL=false" --user readonly --password-env DHK_DB_PASSWORD --sql "SELECT id, name FROM t_order LIMIT 10" --format md --out .agents/memory/exports/SQL_RESULT.md
```

验收结果：

- `db test` 能验证连接并隐藏密码。
- `db sql --dry-run` 不连接数据库，只输出安全校验结论。
- `db sql` 能执行只读查询并输出结果。
- `UPDATE/DELETE/DROP` 等 SQL 被拒绝。
- 不确定安全性的 SQL 被拒绝。
- `SQL_RESULT.md` 不进入 `memory.db`。
- `SQL_RESULT.md` 写出前经过敏感信息扫描和大小限制。
- `doctor` 能报告 MySQL driver 是否可加载。

### 阶段 7：实现当前上下文导出：`memory export`

目标：生成 Agent 真正要读取的 `CURRENT_CONTEXT.md`。

开发内容：

- 必填参数：
  - `--task`
- 可选参数：
  - `--module`，默认 `global`
  - `--mode`，默认 `auto`
  - `--keywords`，逗号分隔
  - `--out`，默认 `.agents/memory/exports/CURRENT_CONTEXT.md`
  - `--limit`，默认 `30`
- 只选择：
  - `status = confirmed`
  - `confidence >= 70`
  - 未命中敏感信息
  - 非 deprecated
  - 非 archived
- MVP 只从 confirmed `memory_item` 和最近 checkpoint 生成上下文。
- MVP 不读取、不渲染 `module_profile` 表数据。
- 排序依据：
  - module
  - mode
  - tags
  - title/content 命中
  - confidence
  - 最近使用
- 每条导出记忆带上 `Match`，说明为什么被选中。
- 导出后更新：
  - `last_used_at`
  - `use_count + 1`
- 控制输出大小：
  - 默认软上限 20KB
  - 超出时保留高分条目，低分条目只保留摘要或剔除

`CURRENT_CONTEXT.md` 结构：

```md
# CURRENT_CONTEXT

<generated-at>...</generated-at>

<task>
...
</task>

<project-summary>
- project_type: ...
- framework: ...
- database: ...
</project-summary>

<must-follow>
- ...
</must-follow>

<relevant-conventions>
## ...
Content...
Evidence:
- ...
Tags:
...
Match:
...
</relevant-conventions>

<module-context>
Generated from confirmed memory_item only.
No module_profile table is available in MVP.
</module-context>

<recent-checkpoint>
...
</recent-checkpoint>

<agent-instructions>
1. 先基于以上上下文制定最小变更方案。
2. 不要把 draft 当 confirmed。
3. 不要猜测不存在的类、字段、表结构。
4. 开发完成后创建 checkpoint，并建议新增 draft memory。
</agent-instructions>
```

验收命令：

```text
java -jar target/dhk-cli-0.1.0-all.jar memory export --task "新增订单查询接口" --module order --mode api --keywords "gateway,mybatis,page"
```

验收结果：

- `CURRENT_CONTEXT.md` 存在。
- 只包含 confirmed memory。
- 不包含 draft memory。
- 不包含敏感字段样例。
- 被导出的 memory `use_count` 增加。

### 阶段 8：实现任务断点：`memory checkpoint` 和 `memory recover`

目标：支持新窗口继续任务。

`memory checkpoint` 开发内容：

- 必填参数：
  - `--task`
  - `--summary`
- 可选参数：
  - `--module`，默认 `global`
  - `--changed`
  - `--pending`
  - `--verify`
  - `--next-read`
- 调用敏感信息检查。
- 写入 `checkpoint`。
- 输出 checkpoint ID。

`memory recover` 开发内容：

- MVP 必填：
  - `--latest`
- 可选参数：
  - `--module`
  - `--out`，默认 `.agents/memory/exports/RECOVERY_CONTEXT.md`
- 找到最新 checkpoint。
- 如果指定 `--module` 且该模块没有 checkpoint，返回明确提示，不自动回退到其他模块。
- 如果未指定 `--module`，返回全项目最新 checkpoint。
- 如果存在多个同时间 checkpoint，按 `id DESC` 打破平局。
- 根据 task/module/pending token 查找相关 confirmed memory。
- 渲染 `RECOVERY_CONTEXT.md`。

`RECOVERY_CONTEXT.md` 结构：

```md
# RECOVERY_CONTEXT

<generated-at>...</generated-at>

<latest-checkpoint>
- task: ...
- module: ...
- summary: ...
- changed_files: ...
- pending_items: ...
- verify_status: ...
- next_read_files: ...
</latest-checkpoint>

<relevant-memory>
...
</relevant-memory>

<agent-instructions>
1. 从 pending_items 继续。
2. 如果有 next_read_files，优先读取这些文件。
3. 编辑前先验证上次 checkpoint 中的假设。
</agent-instructions>
```

验收命令：

```text
java -jar target/dhk-cli-0.1.0-all.jar memory checkpoint --task "新增订单查询接口" --module order --summary "Controller 初稿已完成，Mapper 未验证" --changed "OrderController.java" --pending "验证 SQL" --verify "compile passed"
java -jar target/dhk-cli-0.1.0-all.jar memory recover --latest
```

验收结果：

- `RECOVERY_CONTEXT.md` 存在。
- 包含最新 checkpoint。
- 包含相关 confirmed memory。

### 阶段 9：包装脚本、Skill 和 Rules

目标：让工具能被不同客户端稳定调用。

开发内容：

- 创建 `.agents/skills/devharness-java-development/SKILL.md`。
- Skill 必须规定：
  - 开发前导出 `CURRENT_CONTEXT.md`
  - 读取导出文件后再分析需求
  - 不允许把 draft 当 confirmed
  - 不允许依赖聊天历史判断项目结构
  - 开发结束后写 checkpoint
  - 新发现的事实只能建议写入 draft memory
- 创建脚本：
  - `dhk.bat`
  - `dhk.sh`
  - `memory-init.*`
  - `memory-export.*`
  - `db-test.*`
  - `db-sql.*`
  - `memory-recover.*`
  - `memory-checkpoint.*`
- 包装脚本要求：
  - 如果未设置 `DHK_JAVA_OPTS`，默认使用 `-Xms16m -Xmx128m -Dfile.encoding=UTF-8`。
  - 每次命令只启动一个 `java -jar` 进程。
  - 等待 CLI 执行完成。
  - 透传 CLI 退出码。
  - 不保留后台进程。
  - 不启动本地服务或监听端口。
- 创建 references：
  - `memory-contract.md`
  - `memory-export-format.md`
  - `recovery-flow.md`
  - `api-development-flow.md`
  - `mvc-development-flow.md`
- 创建 Comate rules：
  - 启动时优先读取导出上下文
  - 没有导出文件时提示运行导出脚本
  - 不把 API 项目按 MVC 方式处理
  - 不把 MVC 项目按 API 方式处理
  - 禁止把敏感信息写入 memory

验收命令：

```text
.agents/skills/devharness-java-development/scripts/dhk.sh help
.agents/skills/devharness-java-development/scripts/memory-export.sh --task "测试"
```

Windows 目标环境验收：

```bat
.agents\skills\devharness-java-development\scripts\dhk.bat help
.agents\skills\devharness-java-development\scripts\memory-export.bat --task "测试"
```

### 阶段 10：测试和质量门禁

目标：锁住 MVP 的行为边界，避免后续加 scan 时破坏核心闭环。

单元测试：

- `Args`：
  - 必填参数缺失
  - `--key value`
  - `--flag`
  - unknown command
- `PathUtil`：
  - 相对路径
  - 绝对路径
  - 路径包含空格
  - 从子目录运行
- `SensitiveDataGuard`：
  - 拦截 password/token/Bearer/AKIA
  - 放行普通技术标签
- `SqlSafetyGuard`：
  - 允许 `SELECT/SHOW/DESC/DESCRIBE/EXPLAIN`
  - 拒绝写入/DDL/权限类 SQL
  - 拒绝多语句
  - 拒绝 `SELECT ... INTO OUTFILE`
  - 拒绝 `WITH`、`SLEEP`、`LOAD_FILE`
  - 正确处理字符串、反引号、行注释、块注释中的分号
  - `--explain` 只允许 SELECT
  - 不确定安全性时 fail-closed
- `TagUtil`：
  - 逗号拆分
  - 空格 trim
  - 大小写归一
- `MysqlConnectionService`：
  - 组装 JDBC URL
  - 从环境变量读取密码
  - 不在日志中输出密码
- `MarkdownExporter`：
  - XML-like section
  - confirmed-only
  - 输出大小限制

集成测试：

- `memory init` 创建目录和 DB。
- `memory init` 幂等。
- `memory add` 插入 draft。
- `memory confirm` 使 memory 可导出。
- `memory search` 能按 tag/title/content 搜到。
- `db test` 在提供测试 MySQL 环境变量时能验证连接。
- `db sql --dry-run` 不需要 MySQL 环境即可验证 SQL 安全策略。
- `db sql` 在提供测试 MySQL 环境变量时能执行只读查询。
- `db sql` 对写 SQL 返回安全拒绝。
- `db sql` 对敏感 SQL_RESULT.md 返回安全拒绝。
- `memory export` 排除 draft 和敏感内容。
- `memory checkpoint` 写入断点。
- `memory recover` 导出最新断点。
- FTS 不可用时 fallback 仍可检索。

MySQL 集成测试默认不强制运行，只有同时提供以下环境变量时启用：

```text
DHK_TEST_MYSQL_URL
DHK_TEST_MYSQL_USER
DHK_TEST_MYSQL_PASSWORD
```

性能冒烟测试：

- 记录 fat jar 大小。
- 记录 `dhk help` 冷启动耗时。
- 记录 `dhk doctor` 耗时。
- 构造 1000 条 memory 样本，记录 `dhk memory search` 耗时。
- 构造 1000 条 memory 样本，记录 `dhk memory export` 耗时。
- 使用默认 `-Xmx128m` 跑完整手工验收流程。
- 验证 `db sql` 大结果集被 `limit` 和 `SQL_RESULT.md` 大小上限截断。
- 验证脚本执行结束后没有遗留后台 `dhk`/Java 进程。

完整手工验收流程：

```text
mvn clean test
mvn -DskipTests package
java -jar target/dhk-cli-0.1.0-all.jar help
java -jar target/dhk-cli-0.1.0-all.jar memory init --project-root /tmp/dhk-demo
java -jar target/dhk-cli-0.1.0-all.jar doctor --project-root /tmp/dhk-demo
java -jar target/dhk-cli-0.1.0-all.jar memory add --project-root /tmp/dhk-demo --type gateway_convention --module global --title "User identity from gateway" --content "User ID is read from X-User-Id." --tags "api,gateway,user-id,X-User-Id"
java -jar target/dhk-cli-0.1.0-all.jar memory confirm --project-root /tmp/dhk-demo --id 1
java -jar target/dhk-cli-0.1.0-all.jar memory search --project-root /tmp/dhk-demo --q "gateway user-id"
java -jar target/dhk-cli-0.1.0-all.jar db test --project-root /tmp/dhk-demo --jdbc-url "$DHK_TEST_MYSQL_URL" --user "$DHK_TEST_MYSQL_USER" --password-env DHK_TEST_MYSQL_PASSWORD
java -jar target/dhk-cli-0.1.0-all.jar db sql --project-root /tmp/dhk-demo --jdbc-url "$DHK_TEST_MYSQL_URL" --user "$DHK_TEST_MYSQL_USER" --password-env DHK_TEST_MYSQL_PASSWORD --sql "SELECT 1" --format md --out /tmp/dhk-demo/.agents/memory/exports/SQL_RESULT.md
java -jar target/dhk-cli-0.1.0-all.jar memory export --project-root /tmp/dhk-demo --task "新增订单查询接口" --module order --mode api --keywords "gateway,mybatis"
java -jar target/dhk-cli-0.1.0-all.jar memory checkpoint --project-root /tmp/dhk-demo --task "新增订单查询接口" --module order --summary "Controller 初稿完成，Mapper 待验证" --pending "验证 SQL"
java -jar target/dhk-cli-0.1.0-all.jar memory recover --project-root /tmp/dhk-demo --latest
```

## 6. MVP 验收标准

MVP-A 完成必须满足：

1. `mvn clean test` 通过。
2. `mvn -DskipTests package` 能生成可运行 fat jar。
3. `memory init` 能创建 DB、项目身份、exports 目录和 `PROJECT_INDEX.md`。
4. `doctor` 能报告 schema、FTS/fallback、记忆数量、checkpoint 数量和路径可写性。
5. `memory add` 能写入 draft memory，并拒绝敏感内容。
6. `memory add --status confirmed` 被拒绝，confirmed 只能由 `memory confirm` 产生。
7. `memory confirm` 能让 memory 进入 confirmed 状态并记录确认来源。
8. `memory search` 能通过英文 tags 搜到中文内容记忆。
9. `memory export` 能生成 `CURRENT_CONTEXT.md`，且只包含 confirmed memory。
10. `memory checkpoint` 能保存任务断点。
11. `memory recover` 能根据最新 checkpoint 生成 `RECOVERY_CONTEXT.md`。
12. draft、archived、deprecated、低 confidence、敏感内容都不会被 export。
13. `.bat` 和 `.sh` 包装脚本能正确调用 jar。
14. 包装脚本能通过 `DHK_JAVA_OPTS` 管理默认 JVM 参数。
15. CLI 每次由 Skill 脚本启动，执行完成后自然退出，不遗留后台进程。
16. fat jar 大小、help 冷启动、doctor、memory search、memory export 有性能记录。
17. Skill 和 Comate rules 能表达完整使用流程，不依赖聊天历史。

MVP-B 合入时必须满足：

1. `db test` 能动态注入 MySQL JDBC 地址、用户名和密码来源，验证连接但不泄露密码。
2. `db sql` 能执行只读 SQL，获取结果，并支持 table/md 输出。
3. `db sql --dry-run` 不连接数据库即可输出安全校验结论。
4. `db sql` 默认 fail-closed，拒绝写入、DDL、权限、多语句和导出文件类危险 SQL。
5. `SQL_RESULT.md` 写出前经过敏感信息扫描、单元格长度限制和总大小限制。
6. Skill 不自动调用 `db` 命令，只有用户明确要求验证业务 SQL 时才使用。

## 7. 主要风险和缓解

风险：FTS5 在某些环境不可用。
缓解：FTS 只作为增强能力；LIKE + tags 是必须通过的基础路径。

风险：CLI parser 变复杂后出现隐藏 bug。
缓解：MVP 不做短参数、不做复杂数组参数；所有命令保持显式长参数。

风险：导出过期或不可信事实。
缓解：MVP 只导出 confirmed + confidence >= 70；source hash 和 stale detection 放后续版本。

风险：Windows 脚本和 macOS 本地开发行为不一致。
缓解：包装脚本只做一件事：定位 jar 并透传参数；所有路径都加引号。

风险：工作环境机器性能有限，CLI 过重影响日常使用。
缓解：不做 daemon/server，不引入重框架；默认 `-Xmx128m`；性能冒烟测试记录 jar 大小、启动耗时和核心命令耗时。

风险：业务库 SQL 查询误伤生产数据。
缓解：MVP 默认只允许只读 SQL，不提供写 SQL 开关；连接使用只读账号，工具侧再做 SQLSafetyGuard、readOnly connection、query timeout、max rows。

风险：数据库密码进入 shell 历史、日志或 memory。
缓解：不提供 `--password`；只支持 `--password-env` 和 `--password-stdin`；日志和导出文件都必须脱敏。

风险：范围膨胀到 scan/module_profile。
缓解：scan、compact、module_profile、decision_log 行为明确延期，等手工闭环稳定后再做。

风险：过早实现 workflow engine，导致 CLI 变重。
缓解：V0.2 只做 SQLite 状态持久化和 Markdown export，不做自动执行引擎、daemon、YAML parser 或本地服务。

## 8. 建议提交切片

1. 创建 Maven CLI 骨架和 help 命令。
2. 实现 Args、PathUtil、CommandContext。
3. 实现 SQLite migration、memory init、doctor。
4. 实现 memory repository、memory add、memory confirm。
5. 实现 memory search 和 FTS fallback。
6. 实现 `memory export` 生成 `CURRENT_CONTEXT.md` 和 `PROJECT_INDEX.md`。
7. 实现 memory checkpoint 和 memory recover。
8. 实现敏感信息 guard。
9. 增加 Skill、Rules、bat/sh 脚本。
10. MVP-B：实现 MySQL db test/db sql，只读执行和 SQLSafetyGuard。
11. 增加性能冒烟测试和脚本生命周期验证。
12. 补齐集成测试和最终文档。

## 9. 完成定义

在一个全新的临时目录里，能够从 `memory init` 一路跑到 `memory recover`，生成可被新 Agent 窗口读取的 `CURRENT_CONTEXT.md` 和 `RECOVERY_CONTEXT.md`，全程不依赖 IDE 状态、聊天历史、Comate 原生 Memory 或 Codex 会话记忆，即可认为 MVP 完成。


## 10. V0.2 补充计划：Workflow 持久化层

本节是 V0.2 计划，不进入当前 MVP-A / MVP-B 的完成定义。当前 MVP 完成后，再按本节追加 migration v2 和 workflow 命令。

目标：把外部 harness 工具的开发纪律、阶段、门禁、artifact 和运行状态沉淀进 SQLite，而不是依赖散落的本地目录文件。

```text
MVP       memory / db / export / checkpoint / recover
V0.2-A    workflow 模板、阶段、门禁、运行状态、事件
V0.2-B    artifact、memory binding、checkpoint binding、workflow export 合并进 CURRENT_CONTEXT
V0.3      spec_change / spec_task / spec_acceptance
```

### 10.1 V0.2-A 新增包结构

```text
src/main/java/com/devharnesskit/dhk/command/workflow/
  TemplateCommand.java
  StartCommand.java
  StatusCommand.java
  ExportCommand.java
  PhaseCommand.java
  GateCommand.java

src/main/java/com/devharnesskit/dhk/model/workflow/
  WorkflowTemplate.java
  WorkflowPhaseTemplate.java
  WorkflowGateTemplate.java
  WorkflowRun.java
  WorkflowPhaseRun.java
  WorkflowGateRun.java
  WorkflowEvent.java

src/main/java/com/devharnesskit/dhk/repository/workflow/
  WorkflowTemplateRepository.java
  WorkflowRunRepository.java
  WorkflowEventRepository.java

src/main/java/com/devharnesskit/dhk/service/workflow/
  WorkflowSeedService.java
  WorkflowRunService.java
  WorkflowGateService.java
  WorkflowExportService.java

src/main/java/com/devharnesskit/dhk/export/
  WorkflowContextRenderer.java
```

要求：

```text
不引入 YAML parser。
内置模板用 Java 常量或简单文本数组 seed。
workflow export 只渲染短 Markdown，不启动自动执行引擎。
workflow 命令和 memory/db 命令共用 Args / PathUtil / DbConnectionFactory。
```

---

### 10.2 migration v2：Workflow 基础表

V0.2-A migration v2 新增 7 张表：

```text
workflow_template
workflow_phase_template
workflow_gate_template
workflow_run
workflow_phase_run
workflow_gate_run
workflow_event
```

DDL 以 PRD 的 V0.2 Workflow 持久化设计为准。实现要求：

```text
1. migration 幂等。
2. 不修改 MVP v1 表结构。
3. schema_version 从 1 升到 2。
4. doctor 需要显示 workflow schema 是否存在。
5. workflow 表不存在时，MVP 命令仍能正常工作。
```

---

### 10.3 workflow template seed

命令：

```text
dhk workflow template seed
dhk workflow template list
dhk workflow template show --key api-change
```

内置模板第一批：

```text
api-change
mvc-change
bugfix
systematic-debugging
safe-refactor
sql-review
code-review
spec-driven-change
```

`api-change` 阶段示例：

```text
1. export_context
2. inspect_existing_code
3. create_change_plan
4. user_approval
5. implement_minimal_change
6. verify_compile
7. verify_tests
8. create_checkpoint
9. suggest_memory_updates
```

`systematic-debugging` 阶段示例：

```text
1. export_context
2. collect_error
3. identify_first_business_stack
4. list_hypotheses
5. verify_hypothesis
6. minimal_fix_plan
7. implement_fix
8. verify_regression
9. checkpoint
```

---

### 10.4 workflow run/status/export

命令：

```text
dhk workflow start --workflow api-change --task "新增订单查询接口" --module order --mode api
dhk workflow status --run 20260521-order-api-change
dhk workflow export --run 20260521-order-api-change --out .agents/memory/exports/WORKFLOW_CONTEXT.md
```

`workflow start` 行为：

```text
1. 创建 workflow_run。
2. 展开 workflow_phase_run。
3. 展开 workflow_gate_run。
4. 写 workflow_event: run_created。
5. 输出 run_key、current_phase 和下一步建议。
```

`workflow export` 输出：

```md
# WORKFLOW_CONTEXT

<workflow-run>
run_key: ...
workflow: api-change
status: running
current_phase: create_change_plan
</workflow-run>

<current-phase>
...
</current-phase>

<required-gates>
...
</required-gates>

<agent-instructions>
...
</agent-instructions>
```

限制：

```text
WORKFLOW_CONTEXT.md 默认 <= 12KB。
workflow export 不读取源码。
workflow export 不执行命令。
workflow export 不调用大模型。
```

---

### 10.5 workflow phase/gate

命令：

```text
dhk workflow phase pass --run xxx --phase export_context --summary "已导出 CURRENT_CONTEXT.md"
dhk workflow phase fail --run xxx --phase verify_tests --reason "2 tests failed"

dhk workflow gate pass --run xxx --phase verify_compile --gate compile_passed
dhk workflow gate fail --run xxx --phase verify_tests --gate tests_passed --reason "UserServiceTest failed"
dhk workflow gate waive --run xxx --gate tests_passed --reason "Legacy project has no tests, compile passed only"
```

规则：

```text
hard gate failed -> workflow_run.status = blocked
soft gate failed -> workflow_event level = warn
waive 必须带 reason
phase pass/fail 只能操作 workflow_run.current_phase_key
gate pass/fail/waive 默认只能操作 workflow_run.current_phase_key 下的 gate
phase pass 前必须确认当前 phase 没有 pending / failed hard gate
blocked / failed / completed / abandoned run 不允许继续 phase pass
hard gate pass / waive 后，如果当前 phase 已无阻塞 hard gate，则 blocked run 可恢复 running
workflow start / phase / gate / export 必须调用 SensitiveDataGuard
workflow gate 可以只传 --gate；如果同一 run 中 gate_key 歧义，必须传 --phase
phase pass/fail 必须写 workflow_event
```

---

## 11. V0.2-B 补充计划：Artifact 与 Binding

V0.2-B 追加表：

```text
workflow_artifact
workflow_memory_binding
workflow_checkpoint_binding
```

用途：

```text
workflow_artifact: 记录 CURRENT_CONTEXT.md、WORKFLOW_CONTEXT.md、SQL_RESULT.md、proposal/design/tasks 等文件产物。
workflow_memory_binding: 记录本次 run 读取、导出、建议、创建了哪些 memory。
workflow_checkpoint_binding: 记录本次 run 创建或恢复了哪个 checkpoint。
```

新增命令方向：

```text
dhk workflow artifact list --run xxx
dhk workflow bind-memory --run xxx --memory-id 12 --type exported --reason "gateway rule matched"
dhk workflow bind-checkpoint --run xxx --checkpoint 3 --type created
dhk workflow summary --run xxx
```

`memory export` 支持：

```text
dhk memory export --task "..." --include-workflow <run_key>
```

执行时自动记录 `current_context` artifact 和 `exported` memory binding。

---

## 12. V0.3 补充计划：Spec 持久化

V0.3 才实现规格驱动开发表和命令。不要在 V0.2-A 抢先做。

预留表：

```text
spec_change
spec_document
spec_task
spec_acceptance
```

命令方向：

```text
dhk spec init
dhk spec propose
dhk spec plan
dhk spec tasks
dhk spec verify
dhk spec archive
```

在 V0.3 之前，spec 可以先作为 `workflow_artifact` 引用的文件存在。

---

## 13. Workflow 测试和质量门禁

V0.2-A 单元测试：

```text
workflow template seed 幂等
workflow template list/show
workflow start 展开 phase/gate
workflow status 正确显示 current_phase
gate hard fail 会 block run
pending / failed hard gate 会阻止 phase pass
hard gate waive 后可恢复 blocked run
workflow 写入和导出路径拒绝敏感信息
重复 gate_key 无 --phase 时返回 usage error
gate waive 必须有 reason
workflow export 输出 XML-like section
WORKFLOW_CONTEXT.md 大小 <= 12KB
MVP memory 命令在 workflow 表不存在时仍能运行
```

V0.2-A 集成测试：

```text
memory init -> migration v1
workflow template seed -> migration v2
workflow start -> status -> export
workflow gate fail -> run blocked
workflow phase pass -> event 记录
memory export --include-workflow 合并 inline workflow context
memory export --include-workflow 记录 exported memory binding
workflow export 记录 workflow_context artifact
workflow artifact list / summary 输出审计结果
```

性能要求：

```text
workflow template list <= 1s
workflow status <= 1s
workflow export <= 1s
workflow export 不遗留后台 Java 进程
```

---

## 14. Workflow 完成定义

V0.2-A 完成标准：

```text
1. migration v2 能创建 workflow 基础表。
2. template seed/list/show 可用。
3. workflow start 能生成 run、phase_run、gate_run。
4. workflow status 能展示当前阶段和门禁状态。
5. workflow export 能生成 WORKFLOW_CONTEXT.md。
6. hard gate failed 能阻塞 workflow，且 phase pass 不能绕过 pending / failed hard gate。
7. workflow 持久化和导出路径必须拒绝敏感信息。
8. 所有 workflow 命令遵守轻量化约束，不启动后台服务。
9. MVP-A / MVP-B 既有命令不受 workflow schema 影响。
```

V0.2-B 完成标准：

```text
1. 能记录 workflow artifact。
2. 能记录 workflow 与 memory/checkpoint 的 binding。
3. memory export 可选合并 workflow 上下文。
4. workflow export 自动记录 workflow_context artifact。
5. workflow summary 能输出 artifact / exported memory / checkpoint / pending hard gate 计数。
6. 能查询一次 run 使用了哪些 memory、产生了哪些 checkpoint 和 artifact。
```
