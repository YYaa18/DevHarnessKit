# DevHarnessKit Human-Facing CLI Recovery 验收清单

## 1. Actionable Error 通用验收

- [ ] 所有 validation error 有 error_code。
- [ ] 所有 validation error 有 reason。
- [ ] 缺字段错误有 missing。
- [ ] enum 错误有 valid_values。
- [ ] 可恢复错误有 next_command。
- [ ] 不可恢复错误有 next_action。
- [ ] JSON 输出包含 machine-readable error object。
- [ ] Text 输出用户可直接理解。

## 2. Goal Evidence Contract

- [ ] goal next 展示完整 required_evidence。
- [ ] goal step 使用同一 evidence contract 校验。
- [ ] verify action 的 sensitive_result 等字段在 goal next 中可见。
- [ ] goal step 缺字段时输出 missing。
- [ ] goal step 缺字段时输出 example evidence。
- [ ] goal step 缺字段时输出 example command。
- [ ] `dhk goal evidence-template --goal <goal>` 可用。

## 3. Blocking Interaction

- [ ] blocking interaction 错误输出 request id。
- [ ] 输出 valid choices。
- [ ] 输出 next_command。
- [ ] 输出 details command。
- [ ] `brief answer` 参数错误时输出正确示例。
- [ ] 用户无需读源码即可回答 interaction。

## 4. Enum Valid Values

- [ ] acceptance status 错误显示 valid_values。
- [ ] task status 错误显示 valid_values。
- [ ] workflow gate status 错误显示 valid_values。
- [ ] verification mode 错误显示 valid_values。
- [ ] recommendation mode 错误显示 valid_values。
- [ ] graph mode 错误显示 valid_values。
- [ ] policy mode 错误显示 valid_values。
- [ ] aliases 显示在错误信息中。
- [ ] done/accepted/approved/closed 可 alias 到 passed。

## 5. Demo No-Build Preset

- [ ] `demo-no-build` preset 存在。
- [ ] compile.mode=disabled。
- [ ] test.mode=disabled。
- [ ] graph 可选或关闭。
- [ ] 无 pom.xml 的 mock 项目可走完 goal 协议。
- [ ] goal complete 不被 compile/test skipped 卡住。
- [ ] GOAL_CONTEXT 输出 demo warning。
- [ ] README 明确 demo mode 不代表真实验证。

## 6. Goal Complete Blocker Report

- [ ] goal complete not_ready 返回非 0 exit code。
- [ ] 输出 decision=not_ready。
- [ ] 输出 blockers。
- [ ] 每个 blocker 有 user_message。
- [ ] 每个 blocker 有 technical_reason。
- [ ] 每个 blocker 有 next_command。
- [ ] 用户无需 goal audit 即可定位阻塞原因。

## 7. Step Display

- [ ] 文本输出显示 goal-local step index。
- [ ] 第二个 goal 的第一步显示 step 1。
- [ ] JSON 保留全局 step_id。
- [ ] JSON 增加 goal_step_index。
- [ ] 不破坏旧数据。

## 8. Memory List

- [ ] `dhk memory list` 可用。
- [ ] `dhk memory list --status confirmed` 可列出 confirmed memory。
- [ ] 支持 `--limit`。
- [ ] 支持 `--module`。
- [ ] 支持 `--tag`。
- [ ] 支持 `--json`。
- [ ] `memory search --q ""` 仍可以拒绝空查询。

## 9. Quickstart Resume Safety

- [ ] quickstart 默认创建新 goal。
- [ ] 不同 task 不复用。
- [ ] 不同 module 不复用。
- [ ] 不同 profile 不复用。
- [ ] completed goal 不复用。
- [ ] abandoned goal 不复用。
- [ ] `--resume-existing` 才允许复用。
- [ ] 相似 goal 提示包含 mismatch reason。
- [ ] 跨模块复用测试失败。

## 10. Build Wrapper

- [ ] `scripts/dev-build.sh` 存在。
- [ ] 支持 `DHK_JACOCO_VERSION=0.8.10`。
- [ ] 支持 Maven property `-Djacoco.version`。
- [ ] 不修改 pom.xml。
- [ ] 构建失败时给出 fallback 提示。
- [ ] git diff 不出现 pom 临时改动。

## 11. 回归测试

```bash
git diff --check
mvn -q test
mvn -q -DskipTests package
```

专项测试建议：

```bash
mvn -q -Dtest=ActionableErrorRendererTest test
mvn -q -Dtest=GoalEvidenceContractTest test
mvn -q -Dtest=BriefInteractionTest test
mvn -q -Dtest=SpecStatusValidationTest test
mvn -q -Dtest=QuickstartReuseSafetyTest test
mvn -q -Dtest=MemoryListCommandTest test
mvn -q -Dtest=DemoNoBuildPresetTest test
```
