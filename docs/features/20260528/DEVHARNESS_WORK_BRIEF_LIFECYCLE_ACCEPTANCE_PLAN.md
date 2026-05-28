# DevHarnessKit Work Brief Lifecycle & Knowledge Candidate 验收方案

## 1. Work Brief 契约验收

### 字段验收

生成的 Work Brief 必须包含：

- [ ] task_summary
- [ ] recommendation
- [ ] confidence
- [ ] why
- [ ] risk_flags
- [ ] expected_work
- [ ] will_not_do
- [ ] user_choices
- [ ] confirmation_required
- [ ] confirmation_reason
- [ ] safe_to_start

### 推荐枚举验收

recommendation 只能是：

- [ ] patch
- [ ] standard
- [ ] strict
- [ ] analyze_only
- [ ] ask

### 用户层隐藏命令

- [ ] Work Brief 不包含要求用户复制执行的 `dhk goal next`。
- [ ] Work Brief 不包含底层 harness_commands。
- [ ] Work Brief 允许用自然语言描述“系统将继续执行下一步”。
- [ ] Debug 模式除外，但必须显式标注。

### Golden tests

至少覆盖：

- [ ] 低风险小改 -> patch。
- [ ] 新增 API -> standard。
- [ ] 权限/支付/安全 -> strict。
- [ ] 任务不清晰 -> ask。
- [ ] 用户只想分析 -> analyze_only。

---

## 2. Agent Execution Brief 验收

### JSON 字段

`AGENT_BRIEF.json` 必须包含：

- [ ] schema_version
- [ ] brief_id
- [ ] recommendation_id
- [ ] task_key 或 goal_key
- [ ] mode
- [ ] profile_key
- [ ] current_action
- [ ] allowed_actions
- [ ] forbidden_actions
- [ ] required_evidence
- [ ] escalation_rules
- [ ] verification_policy
- [ ] user_visible_summary_ref
- [ ] harness_commands
- [ ] show_commands_to_user=false

### 命令隔离

- [ ] `harness_commands` 只出现在 Agent Brief。
- [ ] Work Brief 不泄漏 harness_commands。
- [ ] Adapter skill 明确 Agent internal protocol。
- [ ] 包装测试检查普通用户输出不堆叠底层命令。

### 兼容性

- [ ] schema_version 存在。
- [ ] 字段文档标注 alpha/beta。
- [ ] 新字段向后兼容，不破坏现有 GOAL_CONTEXT。

---

## 3. Interaction Request 验收

### 模型字段

InteractionRequest 必须包含：

- [ ] request_id
- [ ] goal_key
- [ ] phase
- [ ] type
- [ ] priority
- [ ] question
- [ ] why
- [ ] choices
- [ ] default_choice
- [ ] blocks_progress
- [ ] status

### 触发场景

必须覆盖：

- [ ] 任务目标不清楚 -> clarification。
- [ ] 风险升级 -> risk_escalation。
- [ ] 缺 manual test evidence -> manual_evidence。
- [ ] 触碰 protected file -> confirmation。
- [ ] 发现可沉淀知识 -> knowledge_candidate。

### 阻塞行为

- [ ] blocking interaction 存在时，Agent Brief current_action=wait_for_user_answer。
- [ ] blocking interaction 存在时，不允许继续 implement_minimal_change。
- [ ] 用户回答后，InteractionRequest status=answered。
- [ ] 用户回答后，Agent Brief 更新 allowed_next_actions。

---

## 4. Knowledge Candidate 验收

### 模型字段

KnowledgeCandidate 必须包含：

- [ ] candidate_id
- [ ] goal_key
- [ ] type
- [ ] title
- [ ] summary
- [ ] evidence_refs
- [ ] suggested_destination
- [ ] confidence
- [ ] requires_confirmation
- [ ] sensitive_scan_status
- [ ] status

### 类型验收

至少支持：

- [ ] project_fact_candidate
- [ ] project_convention_candidate
- [ ] business_rule_candidate
- [ ] verification_rule_candidate
- [ ] risk_lesson_candidate
- [ ] growth_lesson_candidate
- [ ] team_pattern_candidate

### 生命周期验收

- [ ] 默认 status=draft。
- [ ] sensitive scan failed 时不能 confirm。
- [ ] confirm 到 project memory 时只能进入 memory draft。
- [ ] confirm 到 growth 时只能进入 growth draft。
- [ ] 不允许自动 confirmed memory。
- [ ] reject 后不再出现在默认 Knowledge Brief。

---

## 5. Work Brief 生命周期验收

### Pre-Work Brief

- [ ] `dhk advise` 能生成初始 WORK_BRIEF.md。
- [ ] `dhk advise` 不创建 goal。
- [ ] `dhk advise` 能生成 AGENT_BRIEF.json。
- [ ] 输出推荐模式与风险。

### Planning Brief

- [ ] inspect/plan 阶段可生成需要确认的问题。
- [ ] Graph impact 范围大时提示升级。
- [ ] 风险项进入 risk_flags。

### Progress Brief

- [ ] goal step 后更新 PROGRESS_BRIEF.md。
- [ ] 显示已完成内容。
- [ ] 显示下一步。
- [ ] 显示是否有 blocking interaction。

### Verify Brief

- [ ] goal verify 生成 VERIFY_BRIEF.md。
- [ ] 缺 manual evidence 时输出用户友好指导。
- [ ] stale graph 时输出用户友好指导。
- [ ] JSON 保留 machine-readable blocker_details。

### Completion Brief

- [ ] goal complete 生成 COMPLETION_BRIEF.md。
- [ ] 包含最终变更摘要。
- [ ] 包含验证结果。
- [ ] 包含遗留风险。
- [ ] 包含知识候选入口。

### Knowledge Brief

- [ ] completion 后生成 KNOWLEDGE_CANDIDATES.md。
- [ ] 用户可以逐条 confirm/reject。
- [ ] 用户可以选择保存到 project memory 或 growth。
- [ ] 用户不确认则不进入 confirmed memory。

---

## 6. Mode Advisor 验收

### 输入覆盖

Mode Advisor 至少考虑：

- [ ] task text
- [ ] module
- [ ] configure preset
- [ ] verification policy
- [ ] protected files / policy
- [ ] security/payment/permission/config/schema/SQL keywords
- [ ] graph freshness
- [ ] impact scope

### 输出覆盖

- [ ] recommendation
- [ ] confidence
- [ ] risk_score
- [ ] risk_flags
- [ ] why
- [ ] confirmation_required
- [ ] questions

### 场景覆盖

- [ ] 小范围 DTO 映射改动 -> patch。
- [ ] 新增订单查询接口 -> standard。
- [ ] 修改支付回调幂等逻辑 -> strict。
- [ ] 任务“优化订单查询” -> ask。
- [ ] 只要求分析影响面 -> analyze_only。

---

## 7. Patch Profile 验收

### Profile

- [ ] java-api-patch 存在。
- [ ] safe-patch 存在。
- [ ] 默认不要求 spec。
- [ ] 默认不要求 full workflow hard gates。
- [ ] 可配置 graph_mode=advisory。

### Evidence

patch required evidence：

- [ ] changed_files
- [ ] reason
- [ ] verification
- [ ] risk_summary

### Escalation

Soft escalation：

- [ ] changed_files > 3 输出升级建议。
- [ ] graph impact 范围中等输出升级建议。

Hard escalation：

- [ ] protected files 阻止 patch complete。
- [ ] schema/migration 阻止 patch complete。
- [ ] security/permission/auth 阻止 patch complete。
- [ ] payment/risk-control 阻止 patch complete。
- [ ] prod config 阻止 patch complete。

---

## 8. goal step --auto 验收

### Git 工作区

- [ ] 自动收集 changed_files。
- [ ] 自动收集 diff_stat。
- [ ] 自动收集 touched_modules。
- [ ] 自动收集 risk_flags。
- [ ] 自动收集 protected_file_hits。

### 无 Git

- [ ] 降级为手动 evidence 提示。
- [ ] 不抛出不可理解错误。

### 边界

- [ ] 不自动生成 manual_evidence_status=passed。
- [ ] 不自动生成 risk_acceptance。
- [ ] 不自动确认 verification。
- [ ] 触碰 protected file 时要求 escalation 或 confirmation。

---

## 9. Adapter 技能验收

### Skill 文档

- [ ] 出现 user-visible / agent-internal 两层说明。
- [ ] 用户层要求展示 Work Brief。
- [ ] Agent 层要求读取 Agent Brief。
- [ ] 禁止普通用户回复中堆叠 dhk 命令。
- [ ] Debug 模式例外必须显式说明。

### Claude Code

- [ ] Claude skill 先生成/读取 Work Brief。
- [ ] Agent 内部执行 Agent Brief harness_commands。
- [ ] 用户回复中展示自然语言意图、风险、选择项。

### OpenCode / Comate

- [ ] AGENTS.md / Comate rules 指向 Work Brief 主体验。
- [ ] 不要求用户理解 profile/check/evidence。
- [ ] Graph-required 时进入 graph-aware Work Brief 流程。

---

## 10. Knowledge Extraction 场景验收

### Inspect 阶段

示例：发现项目分页接口都返回 PageResult。

- [ ] 生成 project_convention_candidate。
- [ ] evidence_refs 包含相关 Controller / PageResult 文件。
- [ ] suggested_destination=project_memory。
- [ ] status=draft。

### Verify 阶段

示例：项目只能通过 IDE test button 验证。

- [ ] 生成 verification_rule_candidate。
- [ ] evidence_refs 包含 manual evidence。
- [ ] suggested_destination=project_memory 或 growth。
- [ ] status=draft。

### Completion 阶段

- [ ] Completion Brief 展示知识候选。
- [ ] 用户选择保存后进入 draft memory/growth。
- [ ] 用户忽略后不再默认提示。
- [ ] 未经确认不进入 confirmed memory。

---

## 11. E2E 场景验收

### 场景 1：小 API 逻辑修改

预期：

- [ ] Work Brief 推荐 patch。
- [ ] Agent Brief mode=patch。
- [ ] 不要求 full graph/spec。
- [ ] goal step --auto 收集 changed_files。
- [ ] goal verify 通过轻量验证。

### 场景 2：新增接口

预期：

- [ ] Work Brief 推荐 standard。
- [ ] 生成 expected_work。
- [ ] 需要 standard evidence。
- [ ] 生成 Agent Brief。
- [ ] quickstart 可创建 goal。

### 场景 3：支付/权限/风控

预期：

- [ ] Work Brief 推荐 strict。
- [ ] risk_flags 包含 security/payment/permission。
- [ ] confirmation_required=true。
- [ ] 不允许 patch complete。

### 场景 4：任务不清楚

预期：

- [ ] Work Brief recommendation=ask。
- [ ] 生成 blocking InteractionRequest。
- [ ] Agent Brief current_action=wait_for_user_answer。
- [ ] 用户回答后继续。

### 场景 5：Spring Boot 手工 IDE 测试

预期：

- [ ] Work Brief 显示 test_mode=manual。
- [ ] Agent 不运行 mvn test。
- [ ] Verify Brief 要求 manual_evidence_status/test_scope/manual_evidence_path。
- [ ] 人工证据缺失时 not_ready。
- [ ] 人工证据满足后 ready。

### 场景 6：轻流程扩大范围

预期：

- [ ] 初始推荐 patch。
- [ ] changed_files > 3 或触碰风险文件后输出 escalation suggestion。
- [ ] protected file 触发 hard escalation。
- [ ] Work Brief 更新风险说明。

---

## 12. 文档验收

- [ ] 新增 docs/WORK_BRIEF.md。
- [ ] 新增 docs/AGENT_BRIEF.md 或合并到 WORK_BRIEF.md。
- [ ] 更新 STABLE_CONTRACT.md。
- [ ] README 说明用户不需要理解底层 dhk 命令。
- [ ] 文档明确三层：
  - 用户层 Work Brief
  - Agent 层 Execution Brief
  - Harness 审计层
- [ ] 文档明确 Work Brief text 与 Agent Brief JSON 的稳定级别。

---

## 13. 发布兼容验收

- [ ] 现有 `dhk goal ...` 仍可用。
- [ ] 旧 wrappers 仍可运行。
- [ ] CLI help 仍能查看底层命令。
- [ ] README 首屏强调 agent-mounted harness。
- [ ] CHANGELOG 说明 brief 是主体验，CLI 是底层兼容接口。
- [ ] release archive 包含 brief docs 和 adapter 更新。
- [ ] release gate 通过。

---

## 14. 回归测试

最低命令：

```bash
git diff --check
mvn -q test
mvn -q -DskipTests package
```

专项测试建议：

```bash
mvn -q -Dtest=WorkBriefRendererTest test
mvn -q -Dtest=AgentBriefExportTest test
mvn -q -Dtest=ModeAdvisorTest test
mvn -q -Dtest=InteractionRequestTest test
mvn -q -Dtest=KnowledgeCandidateTest test
mvn -q -Dtest=GoalIntegrationTest test
mvn -q -Dtest=SkillPackagingTest test
```

---

## 15. Release Gate

进入 beta 前必须满足：

- [ ] AI-200 / AI-201 契约已实现。
- [ ] AI-212 / AI-213 生命周期与知识候选已实现或拆入本批次。
- [ ] Work Brief 不泄漏底层命令。
- [ ] Agent Brief 可被 adapter 消费。
- [ ] E2E 6 个场景通过。
- [ ] 文档存在。
- [ ] 现有 CLI 兼容。
- [ ] release gate 通过。
