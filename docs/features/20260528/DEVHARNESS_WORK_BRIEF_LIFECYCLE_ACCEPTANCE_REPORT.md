# DevHarnessKit Work Brief Lifecycle 验收报告

日期：2026-05-28

## 结论

本轮已完成三份输入文档要求的计划、实施与验收：

- `DEVHARNESS_WORK_BRIEF_LIFECYCLE_DEVELOPMENT_PLAN.md`
- `DEVHARNESS_WORK_BRIEF_LIFECYCLE_ACCEPTANCE_PLAN.md`
- `DEVHARNESS_WORK_BRIEF_LINEAR_BACKLOG_EXTENSION.md`

DevHarnessKit 现在具备 Work Brief 生命周期基础闭环：

```text
WORK_BRIEF.md
AGENT_BRIEF.json
PROGRESS_BRIEF.md
VERIFY_BRIEF.md
COMPLETION_BRIEF.md
KNOWLEDGE_CANDIDATES.md
GROWTH_CONTEXT.md
```

用户层继续看到友好 brief；Agent 层消费 Agent Brief 和 harness commands；
Harness 层保留 goal、check、memory、artifact 审计源。

## Linear 计划

已生成并完成：

- AI-293 Work Brief Lifecycle & Knowledge Candidate 总体落地
- AI-294 Work Brief Lifecycle Interaction
- AI-295 Knowledge Candidate Extraction
- AI-296 Growth Layer 集成
- AI-297 Work Brief Lifecycle 验收测试与报告

## 实现范围

### Work Brief Lifecycle

- `dhk advise` / `dhk quickstart` 在 ask、confirmation、risk escalation 场景生成 interaction request。
- `dhk goal step` 生成 `PROGRESS_BRIEF.md`。
- `dhk goal verify` 生成 `VERIFY_BRIEF.md`。
- `dhk goal complete` 生成 `COMPLETION_BRIEF.md` 和知识候选入口。

### Interaction Request

- 新增 `InteractionRequest` model。
- 新增 `dhk brief answer --request <id> --choice <choice>`。
- blocking interaction 可阻止不合适的 goal step。
- manual evidence interaction 不阻止 verify 阶段补充验证证据。
- Agent Brief 支持 `current_action=wait_for_user_answer`。

### Knowledge Candidate

- 新增 `KnowledgeCandidate` model。
- `goal complete` 默认生成 verification rule 与 growth lesson 候选。
- 新增 `dhk brief knowledge review/confirm/reject`。
- confirm 到 project memory 只创建 draft memory，不自动 confirmed。
- confirm 到 growth 只创建 draft growth lesson。
- sensitive scan 未通过时阻止 confirm。

### Growth Layer

- 新增 `GrowthLesson` model。
- 新增 `dhk growth review/confirm/export`。
- `dhk growth export` 生成 `GROWTH_CONTEXT.md`。
- Agent Brief 输出 `growth_context.advisory_only=true`。

## 验收覆盖

新增和扩展测试集中在 `BriefCommandIntegrationTest`：

- Work Brief 不泄漏 `dhk goal ...` / harness commands。
- Agent Brief 包含 `harness_commands`、`user_visible_summary_ref`、`growth_context`。
- ask 任务生成 blocking clarification interaction。
- `brief answer` 可把 interaction 标记为 answered 并刷新 Agent Brief。
- blocking interaction 在回答前阻止 goal step。
- `goal step` 生成 `PROGRESS_BRIEF.md`。
- `goal verify` 生成 `VERIFY_BRIEF.md`。
- `goal complete` 相关 service 生成 `COMPLETION_BRIEF.md` 和 `KNOWLEDGE_CANDIDATES.md`。
- Knowledge Candidate confirm 到 project memory 只进入 draft memory。
- Knowledge Candidate confirm 到 growth 后，`growth export` 生成 `GROWTH_CONTEXT.md`。

## 验证命令

```bash
mvn -q test
./scripts/check-class-size.sh
./scripts/check-module-boundaries.sh
./scripts/check-version-metadata.sh
git diff --check
mvn -q -DskipTests package
```

结果：全部通过。

## 当前边界

- 本轮实现的是本地文件与 CLI 命令闭环，不引入 daemon、聊天 UI 或外部服务。
- Knowledge Candidate 是规则化基础生成，不是复杂 LLM 总结系统。
- Growth lesson 是 advisory-only，不作为项目事实，也不自动跨项目注入。
- Brief 文件是用户/Agent 交互导出层，不替代 SQLite 和 goal/check 审计事实源。

## 验收判断

通过。

本轮已经把 Work Brief 从“任务开始前的一次性说明”推进为贯穿
planning/progress/verify/completion/knowledge 的用户交互层第一版。
