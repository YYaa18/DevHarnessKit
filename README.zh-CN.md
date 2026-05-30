# DevHarness Kit

[English README](README.md)

DevHarness Kit 是一个本地优先的 AI 编程协作工具。它不是让模型记住整个项目，
而是把任务目标、项目上下文、执行步骤和验证证据放回代码仓库本地，让 Agent 每次
只读取当前任务真正需要的信息。

它适合已经在使用代码 Agent 的团队：你希望 Agent 能写代码，但也希望它别跑偏、
别忘记用户真实需求、别在没有证据时声称完成。

## 当前状态

DevHarness Kit `1.0.0` 是第一个正式稳定版本。稳定承诺范围刻意收窄：
核心 CLI、memory、goal、status、readiness、configure、brief、发布包，以及文档中
列出的导出文件锚点。

当前稳定版本：`1.0.0`。

当前 SQLite schema 版本：v15。

准确边界见
[docs/STABLE_CONTRACT.md](docs/STABLE_CONTRACT.md) 和
[docs/STABLE_CANDIDATE.md](docs/STABLE_CANDIDATE.md)。

## 它解决什么问题

AI 编程 Agent 很容易遇到几类问题：

- 聊着聊着忘记用户最初到底要什么；
- 没看完相关文件就开始改；
- 改完后只说“测试通过”，但没有清楚证据；
- 上下文被压缩后，下一轮不知道做到哪一步；
- 多个工具、规则、检查结果散在聊天和终端里，难以审计。

DevHarness Kit 的思路是：把这些状态变成项目本地的可审计记录。Agent 通过
DevHarness 提供的目标流程工作，每一步都读取短上下文、执行一个明确动作、再把证据
写回本地。

## 基本原理

DevHarness Kit 是一个 Java CLI。它在本地运行，不启动后台服务，不调用大模型接口，
也不会把你的项目上传到云端。

核心流程可以理解成：

```text
用户任务
  -> DevHarness 目标
  -> 本地 SQLite 状态
  -> 短 Markdown/JSON 上下文
  -> Agent 执行当前动作
  -> 记录证据
  -> 检查是否可以完成
```

SQLite 是真实数据源。`GOAL_CONTEXT.md`、`CURRENT_CONTEXT.md`、
`GOAL_SUMMARY.md` 这类 Markdown 文件只是导出给人和 Agent 阅读的上下文，不是手工
维护的源文件。

## 可以达到什么效果

### 让 Agent 按目标工作

DevHarness Kit 可以把一个用户需求变成 Work Brief 和 Goal。Goal 会告诉 Agent：
当前应该做哪一步、需要哪些证据、哪些动作不能做。

效果是：

- Agent 先理解和检查，不是直接乱改；
- 每次只处理当前动作；
- 每一步都有记录；
- 缺少验证或证据时不能轻易说完成。

### 让项目上下文可恢复

项目记忆保存在本地。Agent 看到的是一份短上下文，而不是依赖很长的历史聊天记录。

效果是：

- 项目约定可以跨会话复用；
- 上下文压缩后也能恢复；
- 未确认的草稿记忆不会进入默认上下文；
- 中断后可以通过恢复文件继续工作。

### 让验证证据更清楚

Goal 验证会记录编译、测试、敏感信息检查、流程状态和需求状态。对于不能在 CLI 中
自动跑测试的项目，也可以记录人工证据，比如 IDE 编译通过、手动测试结果、证据文件
路径。

效果是：

- “测试通过”会变成有范围、有路径的证据；
- 手动验证不会被当成 skipped；
- 后续又改了文件时，旧检查会被识别为可能过期；
- 完成摘要能告诉你验证了什么，还剩什么风险。

### 辅助影响面分析

Graph Lite 可以在本地扫描代码关系，生成影响面建议。它不是正确性证明，而是帮助
Agent 更快找到应该读哪些文件。

效果是：

- Agent 可以得到推荐阅读文件；
- 涉及图分析的目标会要求影响面证据；
- 图快照过期时会明确提示，而不是继续误用旧结果。

### 给不同 Agent 安装统一规则

发布包中包含 Agent skill、规则文件和包装脚本。安装器可以把这些资源复制到目标项目，
并生成适合项目的配置。

效果是：

- 项目里有一致的 `.agents/` 结构；
- Agent 通过固定脚本执行流程，不需要临时拼命令；
- 用户看到的是清楚的 Work Brief，Agent 读取的是结构化执行信息。

## 它不是什么

DevHarness Kit 不证明代码一定正确。它做的是记录过程、约束步骤、保存证据、暴露风险，
让人和 Agent 更容易协作。

它也不是：

- 云服务；
- 测试或代码审查的替代品；
- 数据库权限系统；
- 完整的数据泄露防护系统；
- 所有随包附带实验命令的稳定承诺。

数据库检查功能只适合可信的本地开发环境。真实数据库请使用只读账号。SQL guard 是
安全辅助，不是数据库权限边界。

## 安装使用

从 [GitHub Releases](https://github.com/YYaa18/DevHarnessKit/releases) 下载最新
正式发布包。

大多数用户选择 zip 或 tar.gz，解压后运行：

```bash
./install.sh
```

安装向导会询问：

- 你的目标项目路径；
- 要接入哪个 Agent；
- 项目适合自动验证还是人工验证；
- 是否现在创建第一份 Work Brief 或 Goal。

如果只想直接运行 CLI，发布包里也包含：

```text
lib/dhk.jar
```

查看版本：

```bash
java -jar lib/dhk.jar version
```

## 典型使用流程

1. 把 DevHarness Kit 安装到目标项目。
2. 让 Agent 从 Work Brief 或 quickstart 开始。
3. 先看建议的目标、风险和验证要求。
4. Agent 只执行当前 Goal 动作。
5. 每一步完成后记录证据。
6. 完成前运行 Goal 验证。
7. 用总结和 artifact passport 做复查或发布证据。

目前稳定候选的主要入口是：

- `dhk doctor`
- `dhk configure`
- `dhk status`
- `dhk readiness`
- `dhk advise`
- `dhk quickstart`
- `dhk memory`
- `dhk goal`

Graph Lite、BDD、skill governance、policy hook 等能力仍属于实验性范围，不属于
1.0 稳定合同，除非后续 release note 明确提升它们的稳定级别。

## 从源码构建

需要：

- Java 8 或更高版本；
- Maven。

运行测试：

```bash
mvn clean test
```

构建 CLI jar：

```bash
mvn -DskipTests package
```

构建发布包：

```bash
mvn -DskipTests package -P release-archive
```

发布包会包含运行时 jar、安装器、Agent 规则、许可证、安全说明、更新记录和面向用户的
README 文件。仓库中的 `docs/` 目录不会打进用户下载包，开发和设计细节请在 GitHub
源码树中查看。

## 更多文档

需要深入了解时，从这些文档开始：

- [docs/INDEX.md](docs/INDEX.md)：文档导航。
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)：架构和模块边界。
- [docs/STABLE_CONTRACT.md](docs/STABLE_CONTRACT.md)：计划稳定承诺范围。
- [docs/GOAL_CONFIGURATION.md](docs/GOAL_CONFIGURATION.md)：Goal 配置和验证策略。
- [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md)：CLI、schema、JSON 和导出兼容性。
- [docs/MIGRATIONS.md](docs/MIGRATIONS.md)：SQLite 迁移和恢复策略。
- [SECURITY.md](SECURITY.md)：安全模型和问题报告。

## License

DevHarness Kit 源码使用 MIT License。

打包后的 shaded jar 包含第三方依赖，它们有各自的许可证。见
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
