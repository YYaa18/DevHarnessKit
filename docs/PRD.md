本版在原有 MVP-A / MVP-B 基础上补充三类设计：

```text
1. V0.2 Workflow 持久化层：把 Superpowers / OpenSpec / OpenHarness / Archon 等工具的流程能力抽象为 SQLite 表结构。
2. CLI 轻量化最优设计：明确依赖、启动、命令生命周期、懒加载、内存、输出大小和可选模块策略。
3. Harness 能力吸纳路线：不直接绑定外部工具，而是吸收其流程、门禁、artifact 和验证思想。
```

---

下面是一套**可交给 Codex 本地开发、测试，然后导入工作环境使用**的完整方案。核心目标是：**不依赖 Comate 的聊天历史，不依赖某个客户端的记忆能力，而是用 DevHarness Kit CLI 同时提供项目记忆和业务库只读查询能力**：项目经验沉淀到 SQLite，再导出短 Markdown 给任意 Agent/Skill 读取；需要验证业务 SQL 时，通过同一个 CLI 动态连接项目数据库并获取查询结果。

---

# 1. 总体结论

建议做成这个结构：

```text
Agent Skill / Rule
        ↓
运行 DevHarness Kit CLI
        ↓
memory 命名空间：SQLite memory.db 长期存储
db 命名空间：业务库只读查询 SQL_RESULT.md
        ↓
导出 CURRENT_CONTEXT.md / RECOVERY_CONTEXT.md / SQL_RESULT.md
        ↓
模型只读取短 Markdown 上下文
```

重点不是让模型直接理解数据库，而是：

```text
SQLite 负责长期沉淀项目记忆
DevHarness Kit CLI 负责检索、筛选、导出、只读查询业务库
Markdown 负责喂给模型
Skill 负责规定开发流程
Rule / AGENTS.md 负责新窗口启动约束
```

这套方案对 Comate、Codex、Claude Code 等客户端都更稳，因为 Skill 本身是文件夹 + `SKILL.md` + 可选脚本 / references 的形式。Comate 文档明确说 Skills 是可移植、可版本控制、可包含脚本和参考资料的包，并且会从 `.agents/skills/` 和 `.comate/skills/` 等位置加载；Codex 也说明 Skill 是包含 `SKILL.md` 的目录，可带 `scripts/`、`references/`、`assets/`，并且会扫描 `.agents/skills`。([Baidu Cloud][1])

---

# 2. 推荐项目目录

建议把 `.agents/` 作为跨客户端主目录。原因是：**Comate 支持 `.agents/skills/`，Codex 也支持 `.agents/skills/`**。Comate 专用 Rules 仍然放 `.comate/rules/`。([Baidu Cloud][1])

```text
your-project/
  AGENTS.md

  .agents/
    skills/
      devharness-java-development/
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

    tools/
      devharness-kit/
        dhk.jar
        README.md

    memory/
      memory.db
      exports/
        CURRENT_CONTEXT.md
        RECOVERY_CONTEXT.md
        PROJECT_INDEX.md
      snapshots/
      backups/

  .comate/
    rules/
      project-memory-bootstrap.mdr
      java-development-guard.mdr

  .gitignore
```

建议 Git 提交：

```text
AGENTS.md
.agents/skills/**
.agents/tools/devharness-kit/dhk.jar
.agents/tools/devharness-kit/README.md
.comate/rules/**
```

不建议 Git 提交：

```text
.agents/memory/memory.db
.agents/memory/exports/CURRENT_CONTEXT.md
.agents/memory/exports/RECOVERY_CONTEXT.md
.agents/memory/exports/SQL_RESULT.md
.agents/memory/snapshots/**
.agents/memory/backups/**
```

`.gitignore` 建议：

```gitignore
.agents/memory/memory.db
.agents/memory/*.db
.agents/memory/*.db-journal
.agents/memory/*.db-wal
.agents/memory/*.db-shm
.agents/memory/exports/CURRENT_CONTEXT.md
.agents/memory/exports/RECOVERY_CONTEXT.md
.agents/memory/exports/SQL_RESULT.md
.agents/memory/snapshots/
.agents/memory/backups/
```

---

# 3. 为什么要用 `.agents/skills`

你的目标是“不同客户端下一致表现”。所以主 Skill 不要写 Comate 专用语法，也不要依赖某个客户端的特殊上下文命令。

通用 Skill 只做四件事：

```text
1. 要求开发前先导出项目记忆
2. 要求读取导出的 Markdown
3. 需要验证业务 SQL 时，通过 dhk db 只读查询项目数据库
4. 要求开发结束后生成 checkpoint / 新记忆建议
```

Comate 侧通过 `.comate/rules/` 做“始终生效”的启动约束。Comate Rules 文档说明 Agent 本身不会保留记忆，Rules 的作用是把规则作为上下文持续提供给模型。([Baidu Cloud][2])

Codex 侧通过 `AGENTS.md` 做启动约束。Codex 官方文档说明它会在开始工作前读取 `AGENTS.md`，并支持按全局、项目、子目录层级组合指令。([OpenAI Developers][3])

---

# 4. Java 工具定位

工具名建议：

```text
dhk
```

全称：

```text
DevHarness Kit CLI
```

最终产物：

```text
dhk.jar
```

定位：

```text
DevHarness Kit 的本地 Java CLI。它包含两个核心能力：memory 命名空间负责 SQLite 项目记忆，db 命名空间负责在 Skill 执行过程中只读查询业务数据库。
```

不做这些事：

```text
不直接调用大模型
不保存业务库连接配置
不执行业务库写操作
不把原始查询结果自动沉淀为长期记忆
不保存完整聊天记录
不保存敏感配置
不替代 Git
不替代 Comate Memory
不让模型直接读 memory.db
```

多项目共存策略：

```text
MVP 支持多个项目在同一台机器上共存
每个项目拥有自己的 .agents/memory/memory.db 和 project.json
project_key 用于区分项目身份，root_path 只是可更新元数据
不提供全局共享 memory.db
不提供跨项目检索
“多项目共享”属于 MVP+，不进入第一版
```

---

# 5. 技术选型

## 5.1 Java

建议源码按 Java 8 语法写，原因是企业环境兼容性更好。
如果你本地 Codex 开发环境支持更高版本，可以仍然把编译目标设置为 Java 8 或 Java 11。

推荐：

```text
Java 8+ 语法
Maven 构建
最终打成 fat jar
工作环境只需要 java -jar
```

## 5.2 SQLite JDBC

使用 `org.xerial:sqlite-jdbc`。我查到 Maven Central 当前展示的版本是 `3.53.1.0`，描述为 SQLite JDBC library。([Maven Central][4])

推荐依赖：

```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.53.1.0</version>
</dependency>
```

Xerial 官方说明它的版本号跟随打包的 SQLite 版本，并额外增加一位项目发布号；如果你们公司依赖审批不允许最新版，就固定到公司允许的版本，CLI 接口保持不变即可。([GitHub][5])

## 5.3 MySQL JDBC

因为业务代码中的数据库查询逻辑是理解项目的重要部分，`dhk` 需要支持动态注入 MySQL 数据库地址，执行只读 SQL，并获取结果。

使用 MySQL Connector/J 5.1，Maven 坐标固定到 `mysql:mysql-connector-java:5.1.49`。([Maven Central][8])

```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>5.1.49</version>
</dependency>
```

MVP 固定兼容 MySQL JDBC URL 形态。([MySQL Documentation][9])

```text
driver_class = com.mysql.jdbc.Driver
jdbc_url = jdbc:mysql://host:3306/database?useUnicode=true&characterEncoding=utf8&useSSL=false
```

安全约束：

```text
不保存数据库密码
不把生产地址默认写入 memory
不把 SQL 原始结果集默认写入 memory
默认只允许 SELECT / SHOW / DESC / DESCRIBE / EXPLAIN
默认拒绝 INSERT / UPDATE / DELETE / DROP / ALTER / TRUNCATE / CREATE / GRANT / REVOKE / CALL / SET
默认限制返回行数和执行超时
```

密码注入方式：

```text
--password-env DHK_DB_PASSWORD
--password-stdin
```

不提供普通 `--password` 参数，避免密码进入 shell history、日志或进程参数。

## 5.4 打包注意

使用 Maven Shade Plugin 打 fat jar。Xerial README 对 shade 打包有专门提示：如果遇到 `No suitable driver found for jdbc:sqlite:`，需要合并 `META-INF/services/java.sql.Driver`。([GitHub][5])

推荐 Shade 配置方向：

```xml
<transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
```

不要依赖工作环境再下载 Maven 依赖。工作环境只运行：

```bat
java -jar .agents\tools\devharness-kit\dhk.jar ...
```

## 5.5 性能、部署与生命周期约束

工作环境机器性能有限，所以 `dhk` 必须把轻量化作为一等约束，而不是实现完成后的优化项。

核心原则：

```text
不做常驻 daemon
不启动本地 HTTP 服务
不监听端口
不引入 Spring / Guice / 连接池 / ORM
不引入日志框架，优先使用 JDK 自带能力
不做后台线程和定时任务
每次命令由 Skill 脚本启动，执行完成后进程自然退出
```

部署目标：

```text
复制 dhk.jar + scripts 即可使用
工作环境不需要 Maven
工作环境不需要安装额外服务
工作环境不需要初始化全局配置
项目内 .agents/ 目录自包含
```

默认 Java 启动参数由脚本统一管理：

```bat
set DHK_JAVA_OPTS=-Xms16m -Xmx128m -Dfile.encoding=UTF-8
java %DHK_JAVA_OPTS% -jar "%JAR%" %*
```

用户可以覆盖 `DHK_JAVA_OPTS`，但默认参数必须能覆盖常规 memory/export/db 查询场景。

性能预算：

| 指标 | MVP 目标 | 说明 |
| --- | ---: | --- |
| fat jar 大小 | 目标 <= 25MB，超过 35MB 必须解释 | 主要由 SQLite JDBC native 包决定 |
| `dhk help` 冷启动 | 目标 <= 1.5s | 不连接 SQLite / MySQL |
| `dhk doctor` | 目标 <= 2s | 不主动连接业务库，只检查 driver |
| `dhk memory search` | 1000 条 memory 内 <= 1s | FTS 不可用时 LIKE + tags 仍要可用 |
| `dhk memory export` | 1000 条 memory 内 <= 2s | 默认输出 12KB~20KB |
| `dhk db test` | 默认连接超时 5s | 超时要快速失败 |
| `dhk db sql` | 默认查询超时 30s | 默认 limit 100，硬上限 1000 |
| JVM 堆 | 默认 `-Xmx128m` | 基础命令应能在 64MB 下运行 |
| SQL_RESULT.md | 默认 <= 64KB | 防止把大结果集喂给 Agent |

生命周期由 Skill 管理：

```text
memory-export.bat/sh  -> 启动 dhk memory export -> 等待退出 -> Agent 读取 CURRENT_CONTEXT.md
db-test.bat/sh        -> 启动 dhk db test       -> 等待退出 -> 返回连接诊断
db-sql.bat/sh         -> 启动 dhk db sql        -> 等待退出 -> Agent 读取 SQL_RESULT.md 或 stdout
memory-checkpoint.*   -> 启动 dhk memory checkpoint -> 等待退出
```

MVP 不需要 `start` / `stop` 命令，因为没有常驻进程。后续如果引入长连接或本地服务模式，必须单独设计 `dhk service start/status/stop`，并默认关闭。

---


## 5.6 CLI 轻量化最优设计

`dhk` 的轻量化不能只靠 JVM 参数，而要从代码结构、依赖边界、命令生命周期和数据访问方式上控制。

### 5.6.1 命令路径分层

所有命令分为三类路径：

```text
Fast path:
- help
- version（如后续增加）
- doctor 的基础路径

Memory path:
- memory init/add/confirm/search/export/checkpoint/recover
- 只允许打开 SQLite
- 不加载 MySQL 连接逻辑

DB path:
- db test/sql
- 只在用户显式调用 db 命令时加载 MySQL driver
- 不进入 Skill 默认流程
```

要求：

```text
Main / CommandRouter 只做参数解析和命令分发。
help 不创建 .agents 目录，不打开 SQLite，不加载 MySQL driver。
memory 命令不引用 MySQL 专用类的静态初始化逻辑。
db 命令内部再 Class.forName("com.mysql.jdbc.Driver")。
```

### 5.6.2 懒加载与无框架约束

MVP 禁止引入以下能力：

```text
Spring / Spring Boot
Guice / Dagger / CDI
ORM / MyBatis / Hibernate
连接池
本地 HTTP server
后台线程池
定时任务
日志框架
JSON 第三方库
CLI 第三方库
YAML parser
模板引擎
```

实现方式：

```text
依赖注入：手工 new 对象。
命令解析：手写 Args。
JSON：仅支持 project.json 的简单字符串字段。
日志：stdout/stderr + exit code。
Markdown：StringBuilder 渲染。
SQL：JDBC 原生 API。
```

### 5.6.3 SQLite 连接策略

SQLite 是本地单文件存储，不需要连接池。每个命令按需打开连接，执行完成后立即关闭。

MVP 默认 PRAGMA：

```sql
PRAGMA foreign_keys = ON;
PRAGMA busy_timeout = 3000;
```

默认不强制开启 WAL，原因：

```text
MVP 大多数场景是单命令单进程。
WAL 会额外生成 .db-wal / .db-shm 文件。
弱网络盘、受控 Windows 目录和杀毒软件环境下，额外文件可能增加排障成本。
```

如果后续确实需要并发读写，再增加可选配置：

```text
DHK_SQLITE_WAL=1
```

此时由 `DbConnectionFactory` 执行：

```sql
PRAGMA journal_mode = WAL;
PRAGMA synchronous = NORMAL;
```

### 5.6.4 打包策略

MVP 默认交付一个 fat jar：

```text
.agents/tools/devharness-kit/dhk.jar
```

原因：

```text
工作环境不需要 Maven。
工作环境不需要联网下载依赖。
Skill 脚本只需要定位一个 jar。
```

如果 fat jar 超过 35MB，必须评估 V0.2 的双分发模式：

```text
dhk-core.jar    memory-only，包含 SQLite JDBC
dhk-full.jar    memory + db，包含 SQLite JDBC + MySQL Connector/J
```

或 lib 目录模式：

```text
.agents/tools/devharness-kit/
  dhk.jar
  lib/
    sqlite-jdbc-*.jar
    mysql-connector-java-5.1.49.jar
```

但 MVP 优先保持单 jar，减少 Skill 脚本复杂度。

### 5.6.5 内存与输出控制

`dhk` 不应把大量数据加载进内存。实现要求：

```text
memory search：SQL 侧 limit，Java 侧只渲染 limit 内结果。
memory export：先按候选集 limit 查询，再打分，不全表加载。
checkpoint recover：只读 latest checkpoint 和少量相关 memory。
db sql：最大 1000 行，默认 100 行，SQL_RESULT.md 最大 64KB。
```

输出文件限制：

```text
CURRENT_CONTEXT.md 默认 12KB~20KB。
RECOVERY_CONTEXT.md 默认 12KB~20KB。
SQL_RESULT.md 默认 <= 64KB。
WORKFLOW_CONTEXT.md（V0.2）默认 <= 12KB。
```

### 5.6.6 退出码与可脚本化

所有命令必须稳定返回退出码：

```text
0 成功
1 运行时错误
2 参数/用法错误
3 安全拒绝或校验失败
4 目标不存在
```

Skill 脚本只透传退出码，不做业务判断。业务判断只在 Java CLI 内完成。

### 5.6.7 轻量化验收

MVP-A 必须记录以下指标：

```text
fat jar 大小
help 冷启动耗时
doctor 耗时
memory search 1000 条样本耗时
memory export 1000 条样本耗时
默认 -Xmx128m 下完整流程是否通过
脚本执行后是否遗留 Java 进程
```

V0.2 workflow 能力也必须遵守同样约束：只做 SQLite 状态持久化和 Markdown 渲染，不做常驻 workflow engine。

---

# 6. CLI 命令设计

最终工具命令统一如下。

命令分层：

```text
dhk memory ...   操作 SQLite 项目记忆和上下文导出
dhk db ...       只读查询业务数据库
dhk doctor       检查 CLI、SQLite、MySQL driver 和导出目录
dhk help         查看帮助
```

## 6.1 初始化

```bat
java -jar dhk.jar memory init --project-root .
```

作用：

```text
创建 .agents/memory/memory.db
创建表结构
创建 exports 目录
写入 schema_version
生成极短 PROJECT_INDEX.md
```

---

## 6.2 MVP+：扫描项目（暂缓）

`memory scan` 不进入 MVP。第一版缺少上下文时，不自动扫描和判断 API/MVC，而是要求用户手工写入关键事实、确认记忆，或提供相关文件。

```bat
java -jar dhk.jar memory scan --project-root .
```

MVP+ 作用：生成一批 `draft` 状态的项目记忆。

扫描内容：

```text
pom.xml / build.gradle
Spring Boot 启动类
@RestController
@Controller
@RequestMapping
MyBatis Mapper XML
@Mapper 接口
application.yml / application.properties
templates / jsp / freemarker / thymeleaf
常见 Result / Response / Page 类型
异常处理类
拦截器 / Filter / Gateway header 处理类
```

输出示例：

```text
[SCAN] possible project_type: springboot-api
[SCAN] found RestController: 18
[SCAN] found MVC Controller: 0
[SCAN] found MyBatis XML mapper: 32
[SCAN] generated draft memories: 14
[NOTICE] Draft memories are not exported until confirmed.
```

原则：

```text
scan 只产生 draft
scan 不自动进入 confirmed
scan 不自动进入 CURRENT_CONTEXT.md
scan 不参与 MVP 验收
```

---

## 6.3 新增记忆

```bat
java -jar dhk.jar memory add ^
  --type gateway_convention ^
  --module global ^
  --title "用户身份由网关注入" ^
  --content "用户 ID 从请求头 X-User-Id 获取，业务服务不解析 token。" ^
  --tags "api,gateway,user-id,header,X-User-Id" ^
  --source "src/main/java/.../UserContextFilter.java"
```

新增后默认是 `draft`。
只有确认过的内容才能被导出。

MVP 约束：

```text
memory add 永远只写 draft
memory add 不接受 --status confirmed
如果传 --status，MVP 只允许 draft
confirmed 只能通过 memory confirm 产生
```

---

## 6.4 确认记忆

```bat
java -jar dhk.jar memory confirm --id 12
```

作用：

```text
把 memory_item.status 从 draft 改成 confirmed
写入 confirmed_at
写入 confirmed_by，默认 manual
```

---

## 6.5 搜索记忆

```bat
java -jar dhk.jar memory search --q "order api gateway mysql"
```

作用：

```text
按关键词、tag、module、FTS 结果搜索记忆
```

SQLite 的 FTS5 是官方全文检索虚拟表模块，可以用于在大量文本中搜索词项。([SQLite][6])

但注意：中文检索不要完全依赖 FTS。每条记忆都必须有英文、拼音或技术关键词 tags，例如：

```text
api,gateway,user-id,header,X-User-Id,order,module,mybatis,mysql
```

### 6.5.1 MVP+：列出记忆（暂缓）

`memory list` 不进入 MVP，但作为 MVP+ 的低成本维护命令预留：

```bat
java -jar dhk.jar memory list --status draft --limit 20
java -jar dhk.jar memory list --status confirmed --module order
```

用途：

```text
辅助人工确认 draft
按 status/module 查看记忆
不替代 search，不参与 MVP 验收
```

---

## 6.6 测试 MySQL 连接

```bat
java -jar dhk.jar db test ^
  --jdbc-url "jdbc:mysql://127.0.0.1:3306/demo?useUnicode=true&characterEncoding=utf8&useSSL=false" ^
  --user readonly ^
  --password-env DHK_DB_PASSWORD
```

也可以动态拆分注入数据库地址：

```bat
java -jar dhk.jar db test ^
  --host 127.0.0.1 ^
  --port 3306 ^
  --database demo ^
  --user readonly ^
  --password-env DHK_DB_PASSWORD
```

作用：

```text
加载 MySQL 5.1 JDBC 驱动
验证数据库连接
执行 SELECT 1
输出数据库产品名、版本
默认不输出真实 schema 名
不打印密码
不把连接信息写入 memory.db
```

如确实需要排查 schema，可后续增加 `--show-schema`；MVP 默认不提供，避免泄露项目环境信息。

---

## 6.7 执行业务库只读 SQL

```bat
java -jar dhk.jar db sql ^
  --jdbc-url "jdbc:mysql://127.0.0.1:3306/demo?useUnicode=true&characterEncoding=utf8&useSSL=false" ^
  --user readonly ^
  --password-env DHK_DB_PASSWORD ^
  --sql "SELECT id, name FROM t_order LIMIT 10" ^
  --format md ^
  --out .agents\memory\exports\SQL_RESULT.md
```

作用：

```text
执行只读 SQL
获取结果集
输出 table / md
可写入 .agents/memory/exports/SQL_RESULT.md
辅助理解业务代码中的查询逻辑
```

默认安全策略：

```text
只允许 SELECT / SHOW / DESC / DESCRIBE / EXPLAIN
SqlSafetyGuard 无法确认 SQL 安全时默认拒绝执行
拒绝多语句
拒绝写入、DDL、权限变更、文件导出类 SQL
拒绝 WITH、SELECT INTO OUTFILE、SELECT INTO DUMPFILE、SLEEP、LOAD_FILE
默认 limit = 100
硬上限 max-limit = 1000
默认 timeout = 30 秒
max-cell-length = 200
max-output-bytes = 64KB
connection.setReadOnly(true)
Statement.setMaxRows(limit)
Statement.setQueryTimeout(timeout)
```

`--explain` 规则：

```text
如果传入 --explain，原 SQL 必须是 SELECT
工具执行 EXPLAIN <SELECT ...>
如果原 SQL 已经是 EXPLAIN，则不再二次包裹
不允许 EXPLAIN UPDATE / EXPLAIN DELETE / EXPLAIN INSERT
```

多语句检测不能简单判断是否包含分号。MVP 必须用小状态机处理：

```text
normal
single_quote
double_quote
backtick
line_comment
block_comment
```

只在 `normal` 状态下识别首关键字和多语句分号。

`db sql --dry-run`：

```text
只运行 SqlSafetyGuard
不连接数据库
输出首关键字、是否允许、拒绝原因
用于测试 SQL 安全策略
```

`SQL_RESULT.md` 写出前必须再次调用 `SensitiveDataGuard`。命中明显凭据、token、cookie、Authorization、Bearer、AKIA、手机号、身份证、邮箱等敏感结果时，默认拒绝写出。MVP 不提供 `--allow-sensitive-output`。

查询结果默认不进入 `memory_item`。如果查询后得到可复用业务事实，应该由用户或 Agent 总结成脱敏文本，再通过 `dhk memory add` 写入 draft。

---

## 6.8 导出当前任务上下文

```bat
java -jar dhk.jar memory export ^
  --task "新增订单查询接口" ^
  --module order ^
  --mode api ^
  --keywords "gateway,mysql,mybatis,page" ^
  --out .agents\memory\exports\CURRENT_CONTEXT.md
```

作用：

```text
根据任务类型、模块、关键词，筛选 confirmed 记忆
导出短 Markdown
给 Skill / Agent 读取
```

导出时默认只包含：

```text
confirmed
高置信度
未 deprecated
未 archived
与当前 task/module/mode/keywords 相关
```

---

## 6.9 恢复上次任务

```bat
java -jar dhk.jar memory recover ^
  --latest ^
  --out .agents\memory\exports\RECOVERY_CONTEXT.md
```

作用：

```text
读取最近 checkpoint
读取相关模块记忆
导出恢复上下文
```

---

## 6.10 记录 checkpoint

```bat
java -jar dhk.jar memory checkpoint ^
  --task "新增订单查询接口" ^
  --module order ^
  --summary "已完成 Controller 和 Service，Mapper SQL 未验证" ^
  --changed "OrderController.java,OrderService.java,OrderMapper.xml" ^
  --pending "补充 SQL EXPLAIN；补充单元测试" ^
  --verify "mvn -DskipTests compile passed"
```

作用：

```text
保存本次任务状态
支持新窗口恢复
```

---

## 6.11 MVP+：归档 / 压缩（暂缓）

```bat
java -jar dhk.jar memory compact --older-than-days 180
```

MVP 不实现 `memory compact`。MVP+ 作用：

```text
把长期未使用、低置信度、过期的记忆转为 archived
不删除数据
不导出 archived 内容
```

---

## 6.12 健康检查

```bat
java -jar dhk.jar doctor --project-root .
```

检查：

```text
Java 版本
DB 是否存在
schema 版本
FTS 是否可用
MySQL 5.1 driver 是否可加载
exports 目录是否可写
最近 checkpoint
confirmed 记忆数量
draft 记忆数量
是否存在敏感字段风险
```

---

# 7. 数据库设计

SQLite 是轻量、自包含的本地数据库引擎，适合这种“单文件长期记忆库”的场景。([SQLite][7])

MVP 不要设计太复杂，只实现命令真实使用的表：

```text
schema_version
project
memory_item
checkpoint
memory_fts（FTS5 可用时）
```

`module_profile` 和 `decision_log` 保留为 V2 设计，不进入 MVP migration v1。

---

## 7.1 schema_version

```sql
CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER PRIMARY KEY,
    description TEXT NOT NULL,
    applied_at TEXT NOT NULL
);
```

---

## 7.2 project

```sql
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
```

`project_type` 可选：

```text
unknown
springboot-api
springboot-mvc
mixed-api-mvc
legacy-java
```

---

## 7.3 memory_item

这是核心表。

```sql
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
    source_files TEXT NOT NULL DEFAULT '',
    evidence TEXT NOT NULL DEFAULT '',
    confirmed_at TEXT NOT NULL DEFAULT '',
    confirmed_by TEXT NOT NULL DEFAULT '',

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
```

`memory_type` 建议枚举：

```text
project_fact
api_convention
mvc_convention
gateway_convention
database_convention
code_pattern
module_pattern
exception_convention
logging_convention
security_convention
testing_convention
decision
risk
todo
```

`status` 建议枚举：

```text
draft
confirmed
deprecated
archived
```

`confidence` 规则：

```text
0-39    不可信，不导出
40-69   需要人工确认
70-89   可用于上下文
90-100  高可信关键约束
```

默认导出：

```text
status = confirmed
confidence >= 70
```

---

## 7.4 V2：module_profile（MVP 暂缓）

```sql
CREATE TABLE IF NOT EXISTS module_profile (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    project_key TEXT NOT NULL,
    module_name TEXT NOT NULL,

    summary TEXT NOT NULL DEFAULT '',
    entry_files TEXT NOT NULL DEFAULT '',
    service_files TEXT NOT NULL DEFAULT '',
    mapper_files TEXT NOT NULL DEFAULT '',
    related_tables TEXT NOT NULL DEFAULT '',
    data_flow TEXT NOT NULL DEFAULT '',
    conventions TEXT NOT NULL DEFAULT '',

    status TEXT NOT NULL DEFAULT 'draft',
    confidence INTEGER NOT NULL DEFAULT 50,

    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,

    UNIQUE(project_key, module_name),
    FOREIGN KEY (project_key) REFERENCES project(project_key)
);
```

---

## 7.5 V2：decision_log（MVP 暂缓）

```sql
CREATE TABLE IF NOT EXISTS decision_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    project_key TEXT NOT NULL,
    decision_key TEXT NOT NULL,
    title TEXT NOT NULL,
    decision_content TEXT NOT NULL,
    reason TEXT NOT NULL DEFAULT '',
    evidence TEXT NOT NULL DEFAULT '',

    status TEXT NOT NULL DEFAULT 'confirmed',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,

    UNIQUE(project_key, decision_key),
    FOREIGN KEY (project_key) REFERENCES project(project_key)
);
```

适合存：

```text
本项目不在业务服务内解析 token
API 必须返回 CommonResult<T>
删除默认逻辑删除
新增接口必须经过公司网关
列表接口必须使用 PageResult<T>
```

---

## 7.6 checkpoint

```sql
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

---

## 7.7 memory_fts

```sql
CREATE VIRTUAL TABLE IF NOT EXISTS memory_fts USING fts5(
    item_id UNINDEXED,
    title,
    content,
    tags
);
```

说明：

```text
memory_item 是主表
memory_fts 是搜索索引
Java 服务在新增/更新/删除 memory_item 时同步维护 memory_fts
如果 FTS 不可用，降级为 LIKE + tags 搜索
```

---

# 8. Java 包结构

建议 Codex 按这个结构实现：

```text
src/main/java/com/devharnesskit/dhk/
  Main.java

  cli/
    CommandRouter.java
    CommandContext.java
    Args.java
    ExitCodes.java

  command/
    DoctorCommand.java
    HelpCommand.java

  command/memory/
    InitCommand.java
    AddCommand.java
    ConfirmCommand.java
    SearchCommand.java
    ExportCommand.java
    RecoverCommand.java
    CheckpointCommand.java

  command/projectdb/
    TestCommand.java
    SqlCommand.java

  db/
    DbConfig.java
    DbConnectionFactory.java
    MigrationRunner.java
    TransactionTemplate.java

  model/
    Project.java
    MemoryItem.java
    Checkpoint.java
    SearchResult.java
    ExportRequest.java

  repository/
    ProjectRepository.java
    MemoryRepository.java
    CheckpointRepository.java
    FtsRepository.java

  service/
    MemoryService.java
    SearchService.java
    MysqlConnectionService.java
    SqlExecutionService.java
    ExportService.java
    RecoveryService.java
    CheckpointService.java
    SensitiveDataGuard.java
    SqlSafetyGuard.java

  export/
    MarkdownExporter.java
    CurrentContextTemplate.java
    RecoveryContextTemplate.java
    SqlResultTemplate.java

  sql/
    DbConnectionRequest.java
    SqlExecutionRequest.java
    SqlExecutionResult.java
    SqlColumn.java
    MysqlJdbcUrlBuilder.java
    SqlResultRenderer.java

  util/
    Clock.java
    FileUtil.java
    PathUtil.java
    TextUtil.java
    TagUtil.java
```

不要引入 Spring Boot。
这是命令行小工具，不需要 Spring 容器。

---

# 9. Maven 方案

`pom.xml` 设计目标：

```text
低依赖
fat jar
可离线运行
兼容 Windows
```

依赖建议：

```xml
<dependencies>
    <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
        <version>3.53.1.0</version>
    </dependency>

    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>5.1.49</version>
    </dependency>

    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.12.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

可以不引入 `picocli`。
CLI 参数用手写解析，减少依赖风险。

构建命令：

```bat
mvn clean test
mvn -DskipTests package
```

产物：

```text
target/dhk-cli-0.1.0-all.jar
```

然后复制：

```bat
copy target\dhk-cli-0.1.0-all.jar .agents\tools\devharness-kit\dhk.jar
```

---

# 10. 导出 Markdown 格式

模型不读数据库，只读导出的短文件。

建议 `CURRENT_CONTEXT.md` 格式如下：

```md
# CURRENT_CONTEXT

<generated-at>2026-05-21T10:00:00+08:00</generated-at>

<task>
新增订单查询接口
</task>

<project-summary>
- project_type: springboot-api
- framework: Spring Boot
- database: MySQL
- data_access: MyBatis XML
</project-summary>

<must-follow>
- 不要把 API 项目按 MVC 方式修改。
- 不要新增 token 解析逻辑，用户身份由网关注入。
- 涉及 UPDATE / DELETE 前必须先 SELECT 验证影响范围。
</must-follow>

<relevant-conventions>
## gateway_convention: 用户身份由网关注入
用户 ID 从请求头 X-User-Id 获取，业务服务不解析 token。

Evidence:
- src/main/java/.../UserContextFilter.java

Tags:
api,gateway,user-id,header,X-User-Id

## api_convention: 统一返回结构
所有 API 返回 CommonResult<T>。
</relevant-conventions>

<module-context>
Generated from confirmed memory_item only.
No module_profile table is available in MVP.
</module-context>

<recent-checkpoint>
上次任务：
- 已完成 Controller / Service 初稿
- Mapper SQL 未验证
- 下一步需要检查分页和索引
</recent-checkpoint>

<agent-instructions>
1. 先基于以上上下文制定最小变更方案。
2. 不要猜测不存在的类、字段、表结构。
3. 如上下文不足，列出需要查看的文件。
4. 开发完成后输出是否需要新增 memory_item 或 checkpoint。
</agent-instructions>
```

这里继续使用 Markdown 主体 + XML-like 标签。
原因是：Markdown 人可维护，标签能给弱模型提供更清晰的边界。

`PROJECT_INDEX.md` 在 MVP 中保留，但必须是极短索引，控制在 50 行以内：

```md
# PROJECT_INDEX

<project>
project_key: ...
project_name: ...
project_type: unknown
framework: unknown
database_type: unknown
</project>

<context-files>
- CURRENT_CONTEXT.md: 当前任务上下文
- RECOVERY_CONTEXT.md: 最近任务恢复上下文
- SQL_RESULT.md: 最近一次业务库只读查询结果
</context-files>

<usage>
Before development, run dhk memory export.
For continuation, run dhk memory recover.
For database verification, run dhk db sql with readonly credentials.
</usage>
```

---

# 11. 导出排序算法

`dhk memory export` 不要简单全文搜索。建议评分：

| 条件                    |   加分 |
| --------------------- | ---: |
| status = confirmed    |   必须 |
| confidence >= 90      |   +5 |
| module_name 精确匹配      |  +10 |
| memory_type 匹配 mode   |   +8 |
| tags 命中关键词            |   +6 |
| title 命中关键词           |   +4 |
| content 命中关键词         |   +2 |
| 最近 30 天使用过            |   +2 |
| use_count 高           | +1~3 |
| deprecated / archived | 直接排除 |

导出限制：

```text
默认最多 30 条 memory_item
默认最多 3 条 checkpoint
MVP 不导出 module_profile 表数据
默认输出 12KB ~ 20KB
超过限制时优先保留 must-follow / decision / gateway / database / 当前模块
```

---

# 12. MVP+：API / MVC 项目识别规则（暂缓）

API / MVC 自动识别不进入 MVP。后续实现 `scan` 时，只能输出 `draft` 判断，不直接确认为事实，也不能写入默认导出的 `CURRENT_CONTEXT.md`。

## API 项目倾向

```text
@RestController 数量 > @Controller
Controller 返回 DTO / Result / ResponseEntity
存在 /api、/openapi、/admin 等路径
没有 templates / jsp
存在统一 Result / CommonResult / ApiResult
存在 Gateway / Header / Token / UserContext 相关类
```

## MVC 项目倾向

```text
@Controller 数量较多
Controller 返回 String / ModelAndView
存在 templates / jsp / freemarker / thymeleaf
存在 Model / ModelMap / HttpSession
存在页面跳转 redirect / forward
```

## mixed

```text
@RestController 和 @Controller 都明显存在
既有 JSON API 又有页面模板
```

如果判断不稳定：

```text
project_type = unknown
生成 draft 记忆：
“项目类型待确认，不要直接套用 API 或 MVC 模式。”
```

---

# 13. Skill 集成方案

## 13.1 通用 Skill 路径

```text
.agents/skills/devharness-java-development/SKILL.md
```

`SKILL.md` 只保留跨客户端通用逻辑：

```text
1. 开发前必须通过 dhk memory export 导出 CURRENT_CONTEXT.md。
2. 必须读取 CURRENT_CONTEXT.md 后再分析需求。
3. 如果 CURRENT_CONTEXT.md 不存在或为空，先提示执行 memory init，然后让用户手工 memory add / memory confirm 关键项目事实，或提供相关文件；MVP 不自动 scan。
4. 不允许直接假设项目是 API / MVC。
5. 开发结束后必须生成 checkpoint。
6. 发现新的项目事实时，只能建议写入 draft memory，不能直接当 confirmed 使用。
7. 需要验证业务 SQL 时，通过 dhk db test / dhk db sql 只读查询数据库；不要把数据库密码、生产地址、原始结果集写入 memory。
```

## 13.2 Skill 脚本

```bat
.agents\skills\devharness-java-development\scripts\dhk.bat
```

内容方向：

```bat
@echo off
if "%DHK_JAVA_OPTS%"=="" set DHK_JAVA_OPTS=-Xms16m -Xmx128m -Dfile.encoding=UTF-8
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..\..\..\..
set JAR=%PROJECT_ROOT%\.agents\tools\devharness-kit\dhk.jar

java %DHK_JAVA_OPTS% -jar "%JAR%" %*
```

包装脚本：

```bat
memory-init.bat
memory-export.bat
db-test.bat
db-sql.bat
memory-recover.bat
memory-checkpoint.bat
```

脚本职责：

```text
启动 CLI
传入参数
等待 CLI 退出
透传退出码
不保留后台进程
不启动服务
```

例如：

```bat
@echo off
call "%~dp0dhk.bat" memory export %*
```

`db-sql.bat` 示例：

```bat
@echo off
call "%~dp0dhk.bat" db sql %*
```

这样 Skill 里只需要调用：

```bat
scripts\memory-export.bat --task "..." --module order --mode api --keywords "mysql,gateway"
```

---

# 14. AGENTS.md 设计

Codex 本地开发时，`AGENTS.md` 很关键。它负责告诉 Codex：这个仓库有一个记忆工具，开发前要使用它。Codex 官方文档说明它会在工作前读取 `AGENTS.md`，并按层级合并项目指令。([OpenAI Developers][3])

建议 `AGENTS.md` 内容方向：

```md
# AGENTS.md

## DevHarness Kit CLI

This repository uses `.agents/tools/devharness-kit/dhk.jar` as the local project context CLI.

Before Java development tasks:
1. Run or ask the user to run:
   `.agents\skills\devharness-java-development\scripts\memory-export.bat --task "<task>" --module "<module>" --mode "<api|mvc|auto>" --keywords "<keywords>"`
2. Read `.agents/memory/exports/CURRENT_CONTEXT.md`.
3. Do not assume API/MVC style before reading the exported context.
4. If no context exists, ask the user to run `dhk memory init`, add/confirm key project facts manually, or provide relevant files. Do not run automatic scan in MVP.
5. After development, create `dhk memory checkpoint` and suggest draft memory updates.
6. When database verification is needed, use `dhk db test` and `dhk db sql` with readonly credentials. Do not store JDBC passwords, production hostnames, or raw SQL result sets in memory.

## Restrictions

- Do not store credentials, tokens, database passwords, or production hostnames in memory.
- Do not export archived or deprecated memory.
- Do not treat draft memory as confirmed fact.
- Do not execute write SQL through dhk.
```

---

# 15. Comate Rules 设计

Comate 专用 Rule：

```text
.comate/rules/project-memory-bootstrap.mdr
```

内容方向：

```md
---
description: 项目记忆启动规则
alwaysApply: true
---

# Project Memory Bootstrap

每次进入开发、Debug、代码审查或需求实现前：

1. 优先读取 `.agents/memory/exports/CURRENT_CONTEXT.md`。
2. 如果用户说“继续上次任务”，读取 `.agents/memory/exports/RECOVERY_CONTEXT.md`。
3. 如果导出文件不存在，提示用户运行：
   `.agents\skills\devharness-java-development\scripts\memory-export.bat`
4. 不要依赖聊天历史判断项目结构。
5. 不要把 API 项目按 MVC 方式处理，也不要把 MVC 项目按 API 方式处理。
6. 需要验证业务 SQL 时，只能使用只读数据库账号和 dhk db sql。
7. 不要把 JDBC 密码、生产地址、原始 SQL 结果写入 memory。
8. 开发完成后，输出是否需要新增 draft memory 和 checkpoint。
```

这条 Rule 的意义是：**即使新开 Comate 对话窗口，也能强制模型先看导出文件。**

---

# 16. 本地 Codex 开发计划

你可以把下面任务逐条交给 Codex。

## 里程碑 1：创建 Maven CLI 项目

目标：

```text
创建 dhk Java CLI 项目
支持 java -jar dhk.jar help
支持 memory init / doctor 基础命令
```

验收：

```bat
mvn clean test
java -jar target\dhk-cli-0.1.0-all.jar help
java -jar target\dhk-cli-0.1.0-all.jar doctor --project-root .
```

---

## 里程碑 2：实现 SQLite 初始化和迁移

目标：

```text
实现 schema_version
实现 project
实现 memory_item
实现 checkpoint
实现 migration runner
```

验收：

```bat
java -jar dhk.jar memory init --project-root .
java -jar dhk.jar doctor --project-root .
```

期望：

```text
.agents/memory/memory.db created
schema version = 1
exports dir exists
```

---

## 里程碑 3：实现 memory add / memory confirm / memory search

目标：

```text
支持新增 draft memory
支持确认 memory
支持搜索 memory
支持 tags 搜索
支持 FTS 搜索
FTS 不可用时降级 LIKE
```

验收：

```bat
java -jar dhk.jar memory add --type gateway_convention --module global --title "用户身份由网关注入" --content "用户 ID 从 X-User-Id 获取" --tags "api,gateway,user-id,X-User-Id"
java -jar dhk.jar memory search --q "gateway user-id"
java -jar dhk.jar memory confirm --id 1
```

---

## 里程碑 4：实现 memory export

目标：

```text
根据 task / module / mode / keywords 导出 CURRENT_CONTEXT.md
只导出 confirmed
按评分排序
控制输出大小
输出 Markdown + XML-like 标签
```

验收：

```bat
java -jar dhk.jar memory export --task "新增订单查询接口" --module order --mode api --keywords "gateway,mysql" --out .agents\memory\exports\CURRENT_CONTEXT.md
type .agents\memory\exports\CURRENT_CONTEXT.md
```

---

## 里程碑 5：实现 MVP-B MySQL 只读查询（受控功能，可后置）

目标：

```text
引入 mysql-connector-java 5.1.49
支持动态注入 jdbc-url 或 host/port/database
支持 db test 验证连接
支持 db sql 执行只读查询
支持 table/md 输出
支持 SQL_RESULT.md 导出
默认拒绝写 SQL、DDL、多语句和危险 SQL
Skill 不自动调用 db 命令，只有用户明确要求验证业务 SQL 时才使用
```

验收：

```bat
set DHK_DB_PASSWORD=***
java -jar dhk.jar db test --jdbc-url "jdbc:mysql://127.0.0.1:3306/demo?useUnicode=true&characterEncoding=utf8&useSSL=false" --user readonly --password-env DHK_DB_PASSWORD
java -jar dhk.jar db sql --jdbc-url "jdbc:mysql://127.0.0.1:3306/demo?useUnicode=true&characterEncoding=utf8&useSSL=false" --user readonly --password-env DHK_DB_PASSWORD --sql "SELECT 1" --format md --out .agents\memory\exports\SQL_RESULT.md
java -jar dhk.jar db sql --jdbc-url "jdbc:mysql://127.0.0.1:3306/demo?useUnicode=true&characterEncoding=utf8&useSSL=false" --user readonly --password-env DHK_DB_PASSWORD --sql "DELETE FROM t_order"
```

期望：

```text
前两条成功
DELETE 被拒绝
密码不出现在输出和导出文件中
SQL_RESULT.md 不自动写入 memory_item
```

---

## 里程碑 6：实现 memory checkpoint / memory recover

目标：

```text
支持任务断点
支持恢复最近任务
支持导出 RECOVERY_CONTEXT.md
```

验收：

```bat
java -jar dhk.jar memory checkpoint --task "新增订单查询接口" --module order --summary "已完成 Controller 初稿" --changed "OrderController.java" --pending "补 SQL"
java -jar dhk.jar memory recover --latest --out .agents\memory\exports\RECOVERY_CONTEXT.md
```

---

## MVP+：实现 memory scan（暂缓，不进入 MVP）

目标：

```text
扫描 Java/Spring Boot/MyBatis 项目
识别 API / MVC / mixed 倾向
生成 draft memory
```

验收：

```bat
java -jar dhk.jar memory scan --project-root .
java -jar dhk.jar memory search --q "project_type"
```

---

## 里程碑 7：实现 Windows 脚本和 Skill 目录

目标：

```text
生成 .agents/skills/devharness-java-development/
生成 scripts/*.bat
生成 references/*.md
生成 AGENTS.md 示例
生成 .comate/rules/*.mdr 示例
```

验收：

```bat
.agents\skills\devharness-java-development\scripts\memory-init.bat --project-root .
.agents\skills\devharness-java-development\scripts\memory-export.bat --task "测试" --module global --mode auto --keywords "java"
```

---

# 17. 测试方案

## 17.1 单元测试

覆盖：

```text
Args 解析
Path 处理
DB 初始化幂等性
Migration 幂等性
memory add / memory confirm
Status 过滤
Tags 匹配
Export 排序
Export 大小限制
SensitiveDataGuard
SqlSafetyGuard
MysqlJdbcUrlBuilder
SQL result rendering
```

---

## 17.2 集成测试

创建临时目录模拟项目：

```text
temp-project/
  pom.xml
  src/main/java/com/demo/DemoApplication.java
  src/main/java/com/demo/controller/OrderController.java
  src/main/java/com/demo/service/OrderService.java
  src/main/resources/mapper/OrderMapper.xml
```

验证：

```text
memory export 能生成 CURRENT_CONTEXT.md
memory checkpoint / memory recover 能恢复
db test 能在提供测试 MySQL 环境变量时验证连接
`db sql` 能在提供测试 MySQL 环境变量时执行 SELECT 1
`db sql` 会拒绝 DELETE / UPDATE / DROP 等危险 SQL
```

`memory scan`、API/MVC 自动识别和 MyBatis 扫描测试放到 MVP+，不作为 MVP 集成测试通过条件。

性能冒烟测试：

```text
help 冷启动耗时
doctor 耗时
memory search 1000 条样本耗时
memory export 1000 条样本耗时
fat jar 大小
默认 -Xmx128m 下完整手工流程可运行
db sql 大结果集会被 limit 和 SQL_RESULT.md 大小限制截断
```

MySQL 集成测试默认跳过。只有提供以下环境变量时才运行：

```text
DHK_TEST_MYSQL_URL
DHK_TEST_MYSQL_USER
DHK_TEST_MYSQL_PASSWORD
```

---

## 17.3 Windows 路径测试

必须覆盖：

```text
路径包含空格
中文路径
相对路径
绝对路径
从项目根目录运行
从子目录运行
bat 脚本调用 jar
```

---

## 17.4 敏感信息测试

`SensitiveDataGuard` 分为强拦截和弱提醒，避免把正常技术概念误判为敏感数据。

强拦截：

```text
password=
passwd=
secret=
accessKey=
secretKey=
jdbc:mysql://
Authorization:
Bearer <非空内容>
AKIA[A-Z0-9]+
手机号
身份证号
邮箱地址
明显 cookie/session 值
```

弱提醒：

```text
token
cookie
session
prod
生产
```

规则：

```text
发现敏感信息时：
1. memory add 默认拒绝写入
2. 如用户强制写入，必须打 warning
3. memory export 时再次过滤
4. SQL_RESULT.md 写出前也必须过滤
5. 弱提醒只 warning，不默认拒绝
```

示例：

```text
允许：业务服务不解析 token，用户身份由网关注入。
拒绝：Authorization: Bearer eyJ...
```

---

# 18. 工作环境导入流程

在 Codex 本地开发完成后，复制以下内容到工作项目：

```text
AGENTS.md
.agents/skills/devharness-java-development/**
.agents/tools/devharness-kit/dhk.jar
.comate/rules/project-memory-bootstrap.mdr
.comate/rules/java-development-guard.mdr
```

然后在工作项目根目录执行：

```bat
.agents\skills\devharness-java-development\scripts\memory-init.bat --project-root .
.agents\skills\devharness-java-development\scripts\dhk.bat doctor --project-root .
```

第一次使用：

```bat
.agents\skills\devharness-java-development\scripts\dhk.bat memory add --type project_fact --module global --title "项目类型待确认" --content "MVP 阶段不自动 scan；请根据实际代码手工补充项目类型、框架、数据访问方式等关键事实。" --tags "bootstrap,manual"
.agents\skills\devharness-java-development\scripts\dhk.bat memory add --type database_convention --module global --title "数据库查询只读验证" --content "需要验证业务 SQL 时使用 dhk db sql 和只读账号；原始结果不写入 memory。" --tags "db,readonly,sql"
.agents\skills\devharness-java-development\scripts\dhk.bat memory search --q "bootstrap"
```

确认关键记忆：

```bat
.agents\skills\devharness-java-development\scripts\dhk.bat memory confirm --id 1
.agents\skills\devharness-java-development\scripts\dhk.bat memory confirm --id 2
```

开发前导出：

```bat
.agents\skills\devharness-java-development\scripts\memory-export.bat ^
  --task "新增订单查询接口" ^
  --module order ^
  --mode api ^
  --keywords "gateway,mysql,mybatis,page" ^
  --out .agents\memory\exports\CURRENT_CONTEXT.md
```

需要验证业务 SQL 时：

```bat
set DHK_DB_PASSWORD=***
.agents\skills\devharness-java-development\scripts\db-test.bat --jdbc-url "jdbc:mysql://127.0.0.1:3306/demo?useUnicode=true&characterEncoding=utf8&useSSL=false" --user readonly --password-env DHK_DB_PASSWORD
.agents\skills\devharness-java-development\scripts\db-sql.bat --jdbc-url "jdbc:mysql://127.0.0.1:3306/demo?useUnicode=true&characterEncoding=utf8&useSSL=false" --user readonly --password-env DHK_DB_PASSWORD --sql "SELECT 1" --format md --out .agents\memory\exports\SQL_RESULT.md
```

然后在 Comate 新窗口中说：

```text
使用 devharness-java-development。先读取 .agents/memory/exports/CURRENT_CONTEXT.md，再分析本次需求，不要依赖聊天历史。
```

---

# 19. 关键安全原则

## 19.1 不保存敏感信息

禁止写入：

```text
数据库密码
生产 IP
token
cookie
session
AK/SK
内部网关密钥
真实用户数据
生产 SQL 结果
```

## 19.2 不让 draft 自动生效

弱模型可能会把猜测当事实。
所以：

```text
scan 产生 draft
AI 建议产生 draft
人工确认后才变 confirmed
memory export 默认只导出 confirmed
```

## 19.3 不删除历史，只归档

```text
deprecated = 过时但仍有参考价值
archived = 长期不用，不导出
delete = 原则上不用，除非涉及敏感信息
```

MVP 不实现 `memory redact`。如果敏感信息误写入 `memory.db`，处理方式是：

```text
1. 手工删除 memory.db 并重新 memory init；或
2. 使用 sqlite 工具人工清理污染行；或
3. 等 MVP+ 提供 memory redact 后再清理。
```

仅靠 `memory export` 过滤不等于已经清理泄漏，因为数据库文件本身可能已污染。

## 19.4 数据库查询默认只读

`dhk` 可以查询业务数据库，但默认不能修改业务数据库。

规则：

```text
使用只读数据库账号
默认只允许 SELECT / SHOW / DESC / DESCRIBE / EXPLAIN
SqlSafetyGuard 无法确认安全时默认拒绝执行
拒绝多语句
拒绝 DML / DDL / 权限变更 / 文件导出类 SQL
限制 max rows
限制 query timeout
限制 max-cell-length = 200
限制 max-output-bytes = 64KB
不打印密码
不把 jdbc-url、密码、生产 host 写入 memory
不把原始 SQL 结果自动写入 memory
SQL_RESULT.md 写出前也必须做敏感信息扫描
```

---

# 20. 最小可行版本

第一版不要做太大。MVP 拆成两个交付层，先跑通 Memory Core，再把 DB Readonly 作为受控能力接入。

## 20.1 MVP-A：Memory Core

必须完成：

```text
memory init
memory add
memory confirm
memory search
memory export
memory checkpoint
memory recover
doctor
Skill / Rules / scripts
SensitiveDataGuard
FTS fallback
```

## 20.2 MVP-B：DB Readonly

在 MVP-A 稳定后合入。可以随第一版交付，但 Skill 不自动调用，只有用户明确要求验证业务 SQL 时才使用：

```text
db test
db sql
SQL_RESULT.md
SqlSafetyGuard
MysqlConnectionService
SQL result renderer
```

可以暂缓：

```text
scan
compact
module_profile 自动分析
decision_log 独立命令
memory list
memory redact
复杂 FTS 排名
json 输出
workflow_template / workflow_run / workflow_gate 等工作流持久化表（V0.2）
workflow CLI：template/start/status/export/gate（V0.2）
spec_change / spec_task 等规格驱动开发表（V0.3）
多项目共享
图形界面
```

MVP-A 验收标准：

```text
1. 能通过 memory init 初始化 memory.db
2. 能通过 memory add 写入 draft 项目事实，且不能直接写入 confirmed
3. 能通过 memory confirm 确认项目事实并记录确认来源
4. 能通过 memory export 按任务导出 CURRENT_CONTEXT.md
5. 新窗口能读取 CURRENT_CONTEXT.md 继续开发
6. 能通过 memory checkpoint 生成 checkpoint
7. 能通过 memory recover 恢复上次任务
8. 整套流程不依赖 Comate 原生 Memory
9. 默认通过 Skill 脚本启动并自然退出，不保留后台进程
10. 默认 -Xmx128m 下可完成核心流程
11. fat jar 大小、启动耗时、memory export 耗时有明确测试记录
```

MVP-B 合入验收标准：

```text
1. 能通过 db test 动态注入 MySQL 数据库地址并验证连接
2. 能通过 db sql 执行只读 SQL 并获取结果
3. 能通过 db sql --dry-run 不连接数据库验证 SQL 安全策略
4. `db sql` 默认 fail-closed，并限制输出大小和敏感结果写出
5. Skill 不自动调用 db 命令，只有用户明确要求验证业务 SQL 时才使用
```

---


# 21. V0.2 Workflow 持久化设计

当前 MVP 的 `memory_item` 保存长期项目事实，`checkpoint` 保存任务断点，`CURRENT_CONTEXT.md` 保存本次 Agent 可读上下文。V0.2 需要新增 workflow 层，用 SQLite 持久化“开发过程本身”。

这层设计用于吸收 Superpowers、OpenSpec、Spec Kit、OpenHarness、Archon 等工具的核心能力，但不直接依赖这些工具的运行时。

```text
Superpowers          -> 开发纪律、验证门禁、TDD、系统化 Debug
OpenSpec / Spec Kit  -> proposal / design / tasks / acceptance
OpenHarness          -> readiness / dry-run / harness 分层
Archon               -> workflow phase / gate / artifact
DevHarness Kit       -> 用 SQLite 持久化这些流程状态，再导出短 Markdown 给 Agent 读取
```

## 21.1 设计原则

```text
1. Workflow 是“怎么做”，memory 是“项目事实”，checkpoint 是“做到哪里”。
2. Workflow 不替代 memory_item，也不替代 checkpoint。
3. Workflow 表不进入 MVP migration v1，作为 V0.2 migration v2。
4. V0.2 只做状态记录、门禁记录、短上下文导出，不做自动执行引擎。
5. Workflow 不启动 daemon，不监听端口，不保存后台任务。
6. Agent 仍然只读取 Markdown，不直接读取 SQLite。
```

关系：

```text
workflow_run
  -> 读取 confirmed memory
  -> 导出 CURRENT_CONTEXT.md
  -> 产生 WORKFLOW_CONTEXT.md
  -> 产生 checkpoint
  -> 建议新增 draft memory
  -> 可绑定 spec proposal/design/tasks
```

---

## 21.2 第一批表：Workflow 模板与运行状态

V0.2-A 只实现 7 张表：

```text
workflow_template
workflow_phase_template
workflow_gate_template
workflow_run
workflow_phase_run
workflow_gate_run
workflow_event
```

### 21.2.1 workflow_template

```sql
CREATE TABLE IF NOT EXISTS workflow_template (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    workflow_key TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    category TEXT NOT NULL DEFAULT 'development',
    target_mode TEXT NOT NULL DEFAULT 'auto',
    language TEXT NOT NULL DEFAULT 'java',
    status TEXT NOT NULL DEFAULT 'active',
    version INTEGER NOT NULL DEFAULT 1,
    source_kind TEXT NOT NULL DEFAULT 'manual',
    source_ref TEXT NOT NULL DEFAULT '',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    CHECK (status IN ('active', 'deprecated', 'archived')),
    CHECK (target_mode IN ('auto', 'api', 'mvc', 'mixed', 'sql', 'debug', 'review'))
);
```

内置模板建议：

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

### 21.2.2 workflow_phase_template

```sql
CREATE TABLE IF NOT EXISTS workflow_phase_template (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    workflow_key TEXT NOT NULL,
    phase_key TEXT NOT NULL,
    phase_name TEXT NOT NULL,
    phase_order INTEGER NOT NULL,
    phase_type TEXT NOT NULL,
    required INTEGER NOT NULL DEFAULT 1,
    instruction TEXT NOT NULL DEFAULT '',
    expected_output TEXT NOT NULL DEFAULT '',
    default_gate_policy TEXT NOT NULL DEFAULT 'none',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    UNIQUE (workflow_key, phase_key),
    CHECK (phase_type IN (
        'context',
        'spec',
        'plan',
        'approval',
        'implementation',
        'verification',
        'review',
        'memory',
        'checkpoint',
        'recovery',
        'db',
        'custom'
    )),
    CHECK (default_gate_policy IN ('none', 'soft', 'hard'))
);
```

### 21.2.3 workflow_gate_template

```sql
CREATE TABLE IF NOT EXISTS workflow_gate_template (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    workflow_key TEXT NOT NULL,
    phase_key TEXT NOT NULL,
    gate_key TEXT NOT NULL,
    gate_name TEXT NOT NULL,
    gate_type TEXT NOT NULL,
    severity TEXT NOT NULL DEFAULT 'hard',
    rule_text TEXT NOT NULL,
    check_command TEXT NOT NULL DEFAULT '',
    expected_result TEXT NOT NULL DEFAULT '',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    UNIQUE (workflow_key, phase_key, gate_key),
    CHECK (gate_type IN (
        'manual',
        'command',
        'file_exists',
        'memory_query',
        'sql_safety',
        'sensitive_guard',
        'test_result',
        'compile_result',
        'export_check',
        'custom'
    )),
    CHECK (severity IN ('info', 'soft', 'hard'))
);
```

### 21.2.4 workflow_run

```sql
CREATE TABLE IF NOT EXISTS workflow_run (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_key TEXT NOT NULL,
    workflow_key TEXT NOT NULL,
    run_key TEXT NOT NULL UNIQUE,
    task_name TEXT NOT NULL,
    task_summary TEXT NOT NULL DEFAULT '',
    module_name TEXT NOT NULL DEFAULT 'global',
    mode TEXT NOT NULL DEFAULT 'auto',
    status TEXT NOT NULL DEFAULT 'created',
    current_phase_key TEXT NOT NULL DEFAULT '',
    started_at TEXT,
    completed_at TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    context_export_path TEXT NOT NULL DEFAULT '',
    recovery_export_path TEXT NOT NULL DEFAULT '',
    workflow_export_path TEXT NOT NULL DEFAULT '',
    checkpoint_id INTEGER,
    CHECK (status IN (
        'created',
        'running',
        'waiting_user',
        'blocked',
        'completed',
        'abandoned',
        'failed'
    )),
    CHECK (mode IN ('auto', 'api', 'mvc', 'mixed', 'sql', 'debug', 'review'))
);
```

### 21.2.5 workflow_phase_run

```sql
CREATE TABLE IF NOT EXISTS workflow_phase_run (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_key TEXT NOT NULL,
    phase_key TEXT NOT NULL,
    phase_name TEXT NOT NULL,
    phase_order INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',
    started_at TEXT,
    completed_at TEXT,
    input_summary TEXT NOT NULL DEFAULT '',
    output_summary TEXT NOT NULL DEFAULT '',
    evidence TEXT NOT NULL DEFAULT '',
    notes TEXT NOT NULL DEFAULT '',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    UNIQUE (run_key, phase_key),
    CHECK (status IN (
        'pending',
        'running',
        'waiting_user',
        'passed',
        'failed',
        'skipped',
        'blocked'
    ))
);
```

### 21.2.6 workflow_gate_run

```sql
CREATE TABLE IF NOT EXISTS workflow_gate_run (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_key TEXT NOT NULL,
    phase_key TEXT NOT NULL,
    gate_key TEXT NOT NULL,
    gate_name TEXT NOT NULL,
    gate_type TEXT NOT NULL,
    severity TEXT NOT NULL DEFAULT 'hard',
    status TEXT NOT NULL DEFAULT 'pending',
    checked_at TEXT,
    result_summary TEXT NOT NULL DEFAULT '',
    failure_reason TEXT NOT NULL DEFAULT '',
    evidence TEXT NOT NULL DEFAULT '',
    command_output_path TEXT NOT NULL DEFAULT '',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    UNIQUE (run_key, phase_key, gate_key),
    CHECK (severity IN ('info', 'soft', 'hard')),
    CHECK (status IN ('pending', 'passed', 'failed', 'waived', 'skipped'))
);
```

门禁规则：

```text
hard gate failed -> workflow_run.status = blocked
soft gate failed -> 允许继续，但必须记录 warning
waived -> 必须记录人工豁免原因
```

### 21.2.7 workflow_event

```sql
CREATE TABLE IF NOT EXISTS workflow_event (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_key TEXT NOT NULL,
    run_key TEXT NOT NULL,
    event_type TEXT NOT NULL,
    phase_key TEXT NOT NULL DEFAULT '',
    gate_key TEXT NOT NULL DEFAULT '',
    level TEXT NOT NULL DEFAULT 'info',
    message TEXT NOT NULL,
    data TEXT NOT NULL DEFAULT '',
    created_at TEXT NOT NULL,
    CHECK (level IN ('debug', 'info', 'warn', 'error')),
    CHECK (event_type IN (
        'run_created',
        'run_started',
        'phase_started',
        'phase_completed',
        'gate_checked',
        'gate_failed',
        'gate_waived',
        'artifact_created',
        'memory_exported',
        'memory_suggested',
        'checkpoint_created',
        'db_query_executed',
        'run_blocked',
        'run_completed',
        'run_failed',
        'custom'
    ))
);
```

---

## 21.3 第二批表：Artifact 与 Binding

V0.2-B 实现以下表，用于把 workflow 与 memory、checkpoint、文件产物关联起来。

### 21.3.1 workflow_artifact

```sql
CREATE TABLE IF NOT EXISTS workflow_artifact (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_key TEXT NOT NULL,
    run_key TEXT NOT NULL,
    artifact_type TEXT NOT NULL,
    title TEXT NOT NULL,
    file_path TEXT NOT NULL DEFAULT '',
    content_hash TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL DEFAULT 'draft',
    produced_by_phase TEXT NOT NULL DEFAULT '',
    summary TEXT NOT NULL DEFAULT '',
    tags TEXT NOT NULL DEFAULT '',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    CHECK (artifact_type IN (
        'current_context', 'workflow_context', 'recovery_context',
        'sql_result', 'checkpoint', 'command_output',
        'memory_suggestion', 'review', 'verification',
        'custom'
    )),
    CHECK (status IN ('draft', 'confirmed', 'deprecated', 'archived')),
    FOREIGN KEY (project_key) REFERENCES project(project_key),
    FOREIGN KEY (run_key) REFERENCES workflow_run(run_key)
);
```

### 21.3.2 workflow_memory_binding

```sql
CREATE TABLE IF NOT EXISTS workflow_memory_binding (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_key TEXT NOT NULL,
    memory_id INTEGER NOT NULL,
    binding_type TEXT NOT NULL,
    phase_key TEXT NOT NULL DEFAULT '',
    reason TEXT NOT NULL DEFAULT '',
    created_at TEXT NOT NULL,
    CHECK (binding_type IN (
        'read',
        'exported',
        'suggested',
        'created_draft',
        'confirmed_after_run',
        'deprecated_after_run'
    ))
);
```

### 21.3.3 workflow_checkpoint_binding

```sql
CREATE TABLE IF NOT EXISTS workflow_checkpoint_binding (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_key TEXT NOT NULL,
    checkpoint_id INTEGER NOT NULL,
    binding_type TEXT NOT NULL DEFAULT 'created',
    created_at TEXT NOT NULL,
    CHECK (binding_type IN ('created', 'recovered_from', 'updated'))
);
```

Spec binding 留到 V0.3，V0.2-B 不创建 `workflow_spec_binding`。

---

## 21.4 推荐索引

```sql
CREATE INDEX IF NOT EXISTS idx_workflow_template_status
ON workflow_template(status);

CREATE INDEX IF NOT EXISTS idx_workflow_phase_template_workflow
ON workflow_phase_template(workflow_key, phase_order);

CREATE INDEX IF NOT EXISTS idx_workflow_gate_template_phase
ON workflow_gate_template(workflow_key, phase_key);

CREATE INDEX IF NOT EXISTS idx_workflow_run_project_status
ON workflow_run(project_key, status);

CREATE INDEX IF NOT EXISTS idx_workflow_run_module
ON workflow_run(project_key, module_name);

CREATE INDEX IF NOT EXISTS idx_workflow_phase_run_status
ON workflow_phase_run(run_key, status);

CREATE INDEX IF NOT EXISTS idx_workflow_gate_run_status
ON workflow_gate_run(run_key, status);

CREATE INDEX IF NOT EXISTS idx_workflow_event_run
ON workflow_event(run_key, created_at);

CREATE INDEX IF NOT EXISTS idx_workflow_artifact_run
ON workflow_artifact(run_key, artifact_type);

CREATE INDEX IF NOT EXISTS idx_workflow_memory_binding_memory
ON workflow_memory_binding(memory_id);

CREATE INDEX IF NOT EXISTS idx_workflow_memory_binding_run
ON workflow_memory_binding(run_key, binding_type);

CREATE INDEX IF NOT EXISTS idx_workflow_checkpoint_binding_run
ON workflow_checkpoint_binding(run_key);
```

---

# 22. V0.2 Workflow CLI 与导出格式

V0.2 不做完整 workflow engine，只做模板初始化、运行状态、门禁状态和短上下文导出。

## 22.1 模板命令

```text
dhk workflow template seed
dhk workflow template list
dhk workflow template show --key api-change
```

`seed` 从 Java 内置常量写入模板，不读取 YAML，不引入 YAML parser。

## 22.2 运行命令

```text
dhk workflow start --workflow api-change --task "新增订单查询接口" --module order --mode api
dhk workflow status --run 20260521-order-api-change
dhk workflow export --run 20260521-order-api-change --out .agents/memory/exports/WORKFLOW_CONTEXT.md
```

`workflow start` 行为：

```text
1. 创建 workflow_run。
2. 按 workflow_phase_template 展开 workflow_phase_run。
3. 按 workflow_gate_template 展开 workflow_gate_run。
4. 写入 workflow_event。
5. 输出当前阶段和下一步建议。
```

## 22.3 阶段与门禁命令

```text
dhk workflow phase pass --run xxx --phase export_context --summary "已导出 CURRENT_CONTEXT.md"
dhk workflow phase fail --run xxx --phase verify_tests --reason "2 tests failed"

dhk workflow gate pass --run xxx --phase verify_compile --gate compile_passed
dhk workflow gate fail --run xxx --phase verify_tests --gate tests_passed --reason "UserServiceTest failed"
dhk workflow gate waive --run xxx --gate tests_passed --reason "Legacy project has no tests, compile passed only"
```

`waive` 必须记录原因。

Workflow 持久化路径和导出路径必须复用 `SensitiveDataGuard`：

```text
workflow start: task / summary / module / mode
workflow phase: summary / reason / evidence
workflow gate: summary / reason / evidence
workflow export: 最终 WORKFLOW_CONTEXT.md
memory export --include-workflow: 最终 CURRENT_CONTEXT.md
```

Hard gate 是阻塞约束：

```text
phase pass/fail 只能操作 workflow_run.current_phase_key
gate pass/fail/waive 默认只能操作 workflow_run.current_phase_key 下的 gate
phase pass 前必须确认当前 phase 没有 pending / failed hard gate
hard gate fail 后 run 进入 blocked
blocked run 不允许 phase pass
hard gate pass / waive 后，如果当前 phase 已无 pending / failed hard gate，可恢复 running
同一 run 内 gate_key 歧义时必须指定 --phase
```

## 22.4 WORKFLOW_CONTEXT.md

`workflow export` 生成短文件：

```md
# WORKFLOW_CONTEXT

<workflow-run>
run_key: 20260521-order-api-change
workflow: api-change
status: running
current_phase: create_change_plan
</workflow-run>

<current-phase>
phase_key: create_change_plan
instruction: 输出最小变更方案，列出影响文件和验证方式。
</current-phase>

<required-gates>
- impacted_files_listed: pending
- verification_plan_ready: pending
- user_approval: pending
</required-gates>

<agent-instructions>
1. 当前阶段未通过前，不要进入下一阶段。
2. hard gate failed 时必须停止并说明原因。
3. 新发现的项目事实只能写入 draft memory。
</agent-instructions>
```

最终也可以把 workflow 片段合并进 `CURRENT_CONTEXT.md`：

```text
dhk memory export --task "..." --include-workflow <run_key>
```

V0.2-A 已支持 `--include-workflow`，默认仍让 Agent 主要读取 `CURRENT_CONTEXT.md`。

## 22.5 Artifact / Binding 命令

```text
dhk workflow artifact list --run 20260521-order-api-change
dhk workflow bind-memory --run 20260521-order-api-change --memory-id 12 --type read
dhk workflow bind-checkpoint --run 20260521-order-api-change --checkpoint 3
dhk workflow summary --run 20260521-order-api-change
```

`workflow export` 会自动记录 `workflow_context` artifact。
`memory export --include-workflow` 会自动记录 `current_context` artifact 和 `exported` memory binding。

---

# 23. 外部 Harness 能力吸纳路线

DevHarness Kit 不直接依赖外部 harness 工具，而是吸收其能力模型。

## 23.1 立即吸收：Skill / references 层

不改 CLI，直接增加 Skill references：

```text
.agents/skills/devharness-java-development/references/
  verification-before-completion.md
  systematic-debugging.md
  writing-plans.md
  executing-plans.md
  code-review-gate.md
  spec-driven-change.md
```

这些文件约束 Agent 的开发流程，但不增加运行时重量。

## 23.2 V0.2 吸收：Workflow 层

通过 `workflow_*` 表吸收：

```text
phase
gate
run state
event
artifact
memory binding
checkpoint binding
```

适合吸收：

```text
verification-before-completion
systematic-debugging
safe-refactor
api-change
mvc-change
sql-review
code-review
```

## 23.3 V0.3 吸收：Spec 层

新增独立 spec 表或先通过 artifact/binding 承载：

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

V0.3 后，`CURRENT_CONTEXT.md` 可同时包含：

```text
confirmed memory
latest checkpoint
workflow state
active spec tasks
```

---

# 24. 最终架构判断

你的方案应固定为：

```text
.agents/skills/                    跨客户端 Skill
AGENTS.md                          Codex 启动约束
.comate/rules/                     Comate 启动约束
dhk.jar                            Java + SQLite JDBC + MySQL 5.1 JDBC 工具
scripts/*.bat|*.sh                 Skill 管理 CLI 启动、退出和默认 JVM 参数
memory.db                          长期记忆
CURRENT_CONTEXT.md                 当前任务短上下文
RECOVERY_CONTEXT.md                新窗口恢复上下文
SQL_RESULT.md                      临时 SQL 查询结果，不自动进入长期记忆
WORKFLOW_CONTEXT.md                 V0.2 工作流状态短上下文
checkpoint                         任务断点
workflow_*                          V0.2 开发流程、阶段、门禁和事件持久化表
```

一句话概括：

```text
不要让模型记忆项目；让工具存储项目，让模型只读取本次需要的短上下文。
```

这套方案能适配你说的几个现实限制：模型能力一般、100k 上下文会压缩、新窗口丢历史、Comate 偶发不可用、工作环境只能跑 Java/Jar/Windows shell。

[1]: https://cloud.baidu.com/doc/COMATE/s/Nmma28iqe "Skills - 智能代码助手公有云COMATE | 百度智能云文档"
[2]: https://cloud.baidu.com/doc/COMATE/s/Zm9l4agw3 "Rules - 智能代码助手公有云COMATE | 百度智能云文档"
[3]: https://developers.openai.com/codex/guides/agents-md "Custom instructions with AGENTS.md – Codex | OpenAI Developers"
[4]: https://central.sonatype.com/artifact/org.xerial/sqlite-jdbc "Maven Central: org.xerial:sqlite-jdbc"
[5]: https://github.com/xerial/sqlite-jdbc "GitHub - xerial/sqlite-jdbc: SQLite JDBC Driver · GitHub"
[6]: https://sqlite.org/fts5.html?utm_source=chatgpt.com "SQLite FTS5 Extension"
[7]: https://sqlite.org/?utm_source=chatgpt.com "SQLite Home Page"
[8]: https://central.sonatype.com/artifact/mysql/mysql-connector-java/5.1.49 "Maven Central: mysql:mysql-connector-java:5.1.49"
[9]: https://dev.mysql.com/doc/connector-j/en/connector-j-reference-jdbc-url-format.html "MySQL Connector/J JDBC URL Format"
