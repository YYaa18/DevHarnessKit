# Linear 拆分建议：Human-Facing CLI Recovery & Discoverability

## Umbrella

```text
AI-215 Human-Facing CLI Recovery & Discoverability
```

目标：

```text
让所有失败路径自解释，提供下一步可执行动作，消除用户读源码和反复试错。
```

---

## AI-216 Actionable Error Contract

### 范围

- 定义 ActionableError model。
- 定义 ActionableErrorRenderer。
- CLI validation error 统一输出 error_code/reason/missing/valid_values/next_command/docs。
- JSON 输出保持 machine-readable。

### 验收

- blocking interaction / missing evidence / invalid enum / not_ready 都使用统一结构。
- Text/JSON 都覆盖。

---

## AI-217 Goal Evidence Contract Alignment

### 范围

- 引入 GoalEvidenceContract。
- goal next / GOAL_CONTEXT / goal step 共用同一 evidence contract。
- 新增 `goal evidence-template`。
- goal step 失败时输出 example evidence。

### 验收

- goal next 展示的 required_evidence 与 goal step 校验一致。
- verify action 不再靠试错发现字段。

---

## AI-218 Blocking Interaction Next Command

### 范围

- BlockingInteractionException 增加 request id / choices / next_command。
- `brief answer` 参数错误输出 example。
- Work Brief / CLI error 共用交互 guidance。

### 验收

- blocking interaction 错误可以直接复制命令处理。
- 用户无需读源码。

---

## AI-219 Enum Valid Values and Aliases

### 范围

- 统一 enum validation。
- 所有 invalid enum 输出 valid_values 和 aliases。
- acceptance status 支持常见 aliases。

### 验收

- spec acceptance done/accepted/approved/closed alias 到 passed。
- 所有 enum 错误可自解释。

---

## AI-220 Goal Complete Blocker Report

### 范围

- goal complete not_ready 输出完整 blocker report。
- exit code 非 0。
- 与 goal verify blocker renderer 复用。

### 验收

- goal complete 不静默失败。
- 用户无需 goal audit 定位问题。

---

## AI-221 Goal-local Step Display Index

### 范围

- 文本输出显示 goal-local step index。
- JSON 保留全局 step_id 并增加 goal_step_index。

### 验收

- 第二个 goal 的第一步用户看到 step 1。
- 兼容旧 API。

---

## AI-222 Memory List Command

### 范围

- 新增 `memory list`。
- 支持 status/module/tag/limit/json。
- 保持 search 语义。

### 验收

- 可列出所有 confirmed memory。
- 不需要空 query hack。

---

## AI-223 Quickstart Resume Safety

### 范围

- quickstart 默认创建新 goal。
- 只有 --resume-existing 才允许复用。
- 复用必须 same project/task/module/profile/mode/status/TTL。
- mismatch 输出 reason。

### 验收

- 不跨模块复用。
- 不跨任务复用。
- 不复用 completed/abandoned goal。

---

## AI-224 Demo No-build Preset

### 范围

- 新增 demo-no-build preset。
- compile/test disabled。
- graph/spec optional。
- GOAL_CONTEXT 输出 demo warning。

### 验收

- mock 项目无 pom.xml 可走完整 goal 协议。
- 明确不代表真实验证。

---

## AI-225 Build Wrapper / Maven Property Fallback

### 范围

- 抽取 jacoco.version property。
- 新增 scripts/dev-build.sh。
- 支持 DHK_JACOCO_VERSION。
- 不修改 pom.xml。

### 验收

- 构建 fallback 不污染 git diff。
- 不再手动改 pom。
