# V0.8.8 Real Test Feedback Fix Plan

## 背景

本计划来自 v0.4.6-beta.1 真实端到端测试报告。测试证明完整
`advise -> goal -> verify -> complete` 流程可用，同时暴露出几处
体验和可发现性问题。

Linear 当前因 workspace issue 额度限制无法创建新卡片，本文件保留等价修复卡片内容，便于后续补录。

## 修复卡片

### B1/B5: spec acceptance status 可发现性

优先级：P2

修复：

- `dhk spec acceptance statuses` 输出 canonical values 与 aliases。
- invalid status 错误信息展示允许值和别名。
- 常见别名归一化到 canonical status。

验收：

- `done/closed/accepted/resolved/approved` 归一化为 `passed`。
- `open/in_progress` 归一化为 `pending`。
- `rejected` 归一化为 `failed`。
- `waive` 归一化为 `waived`，且仍要求 evidence/reason。

### B2: goal next structured evidence 完整覆盖 required evidence

优先级：P2

修复：

- `goal next` 文本和 JSON 的 `structured_evidence_fields` 增加
  `--field <required_evidence>=<value>`。
- `GOAL_CONTEXT.md` 同步输出这些字段。

验收：

- verify action 中 `sensitive_result` 等 required evidence 可直接从
  `structured_evidence_fields` 发现。

### B3: quickstart 后手动 goal start 产生重复 goal

优先级：P3

修复：

- `goal start` 默认复用同 project/profile/module/task 的 open goal。
- 显式传 `--force-new` 时才创建另一个 goal。

验收：

- quickstart 创建 goal 后，再执行相同参数的 `goal start` 返回
  真实 `status`、`start_result: existing_goal` 和原 goal key。

### B4: 普通离线构建被 release archive 插件阻断

优先级：P3

修复：

- 普通 `mvn package` 只构建 shaded CLI jar。
- release zip/tar.gz 通过 `-P release-archive` 显式生成。
- release gate 与 release workflow 使用该 profile。

验收：

- `mvn -DskipTests package` 不再执行 assembly plugin。
- `mvn -DskipTests package -P release-archive` 仍生成 release archives。
