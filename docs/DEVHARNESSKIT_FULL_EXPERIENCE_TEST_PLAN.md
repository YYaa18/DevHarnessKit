# MiniShop 配送地址中心业务验收规格

本文是一份业务开发验收规格。请从一个空文件夹开始，实现一个最小可运行的 `MiniShop` 项目，并按下列业务要求完成“配送地址中心”。

## 一、给开发助手的实施任务书

请按以下顺序完成开发，不要跳步：

交互要求：

- 每一步完成后都要先暂停，向用户说明本步产出和下一步计划，不要直接进入下一步。
- 每一步都要给出需要用户确认的清单，并等待用户明确回复“确认”或提出修改意见。
- 如果用户提出修改意见，先修正当前步骤，再重新请求确认。
- 用户确认后，才能继续执行下一步。

### 第 1 步：理解业务背景

MiniShop 是一个轻量电商系统。当前要从空文件夹开始创建一个最小可运行项目，并实现“配送地址中心”。

用户需要维护配送地址，并在创建订单时选择一个合法地址。系统需要保证：

- 地址字段校验严格可靠。
- 每个用户最多只有一个默认地址。
- 订单保存下单时的地址快照，历史订单不受地址后续修改或删除影响。

完成本步骤后，请向用户确认：

- 是否认可上述业务背景和目标。
- 是否同意从空文件夹创建最小纯 Java 项目。
- 是否同意先做项目骨架，再分 L1、L2、L3 逐步实现业务规则。

### 第 2 步：创建最小项目骨架

在空目录中创建最小业务项目。默认使用纯 Java + 内存存储。

必须交付：

- 项目说明文件。
- 可运行的测试入口。
- 地址模型、地址存储、地址服务。
- 订单模型、订单地址快照、订单服务。
- 地址服务测试和订单服务测试。

本步骤只建立骨架和最小可运行测试，不要提前完成全部业务规则。

完成本步骤后，请向用户确认：

- 项目是否确实从空文件夹创建。
- README、业务模型、服务、内存存储和测试入口是否已经生成。
- 当前测试入口是否可以运行并覆盖最小骨架。
- 是否没有提前实现 L1、L2、L3 的完整业务规则。
- 用户确认后再进入 L1 配送地址基础校验。

### 第 3 步：实现 L1 配送地址基础校验

实现地址新增和更新的字段校验。

必须交付：

- 地址保存能力。
- 地址更新能力。
- 字段校验错误。
- 正向测试：合法地址可以保存。
- 反向测试：收件人、手机号、省市区、详细地址、邮编等非法输入会失败。
- 验证保存失败时不会产生脏数据。

### 第 4 步：实现 L2 默认地址规则

实现一个用户只能有一个默认地址。

必须交付：

- 第一条地址自动成为默认地址。
- 设置新默认地址时，旧默认地址自动取消。
- 删除默认地址后，自动补一个新的默认地址。
- 不同用户默认地址互不影响。
- 设置不存在、已删除或其他用户地址为默认时失败。
- L1 全部测试继续通过。

### 第 5 步：实现 L3 下单地址快照

实现创建订单时保存地址快照。

必须交付：

- 创建订单时校验地址存在、未删除且属于当前用户。
- 订单保存地址快照，而不是只依赖地址 id。
- 修改地址后，历史订单快照不变。
- 删除地址后，历史订单仍能展示原地址快照。
- 使用不存在、已删除或其他用户地址下单时失败。
- L1、L2 全部测试继续通过。

### 第 6 步：最终验收

完成后逐项确认：

- 项目确实从空文件夹开始。
- 所有业务代码、测试和说明文件都在本次开发中创建。
- 可以一键运行全部测试。
- L1、L2、L3 的正向和反向场景全部通过。
- README 清楚说明如何运行测试。
- 如果过程中发现问题，需要先修复，再重新验证对应阶段。

## 二、起始状态

项目目录建议：

```bash
export ROOT="$HOME/Desktop/MiniShop-Address-Acceptance"
rm -rf "$ROOT"
mkdir -p "$ROOT"
find "$ROOT" -mindepth 1 -maxdepth 1 -print
```

最后一行应没有输出，表示目录为空。

起始约束：

- 不复制已有业务项目。
- 不预置 `src/`、`pom.xml`、测试文件或业务代码。
- 后续所有业务结构、模型、服务、测试和说明文件都应在此空目录中逐步创建。
- 默认使用纯 Java + 内存存储，避免真实数据库、外部服务和复杂基础设施。
- 如果本地没有 Maven/JUnit，可以提供 `scripts/test.sh` 或等价脚本，用 `javac` 和简单断言完成可重复验证。

## 三、最终交付物

最终项目至少包含：

```text
README.md
pom.xml 或 scripts/test.sh
src/main/java/minishop/address/Address.java
src/main/java/minishop/address/AddressRepository.java
src/main/java/minishop/address/AddressService.java
src/main/java/minishop/order/Order.java
src/main/java/minishop/order/OrderAddressSnapshot.java
src/main/java/minishop/order/OrderService.java
src/test/java/minishop/address/AddressServiceTest.java
src/test/java/minishop/order/OrderServiceTest.java
```

如果采用其他语言或目录结构，也必须能清楚表达同样的业务分层：

- 地址模型。
- 地址存储。
- 地址业务服务。
- 订单模型。
- 订单地址快照。
- 订单业务服务。
- 覆盖正向和反向场景的测试。

## 四、业务背景

MiniShop 是一个轻量电商系统。用户需要维护配送地址，并在创建订单时选择一个合法地址。

本次开发分为三轮：

- L1：配送地址基础校验。
- L2：默认地址规则。
- L3：下单地址快照。

每一轮都必须先实现业务，再补充测试；如果测试或人工检查发现瑕疵，需要修复后重新验证本轮。

## 五、领域模型

### 1. 用户

用户不需要完整账户系统，用 `userId` 字符串即可。

示例：

```text
user-1001
user-1002
```

### 2. 配送地址

地址字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| id | String | 是 | 系统生成，唯一 |
| userId | String | 是 | 地址所属用户 |
| receiverName | String | 是 | 1-30 个字符 |
| phone | String | 是 | 中国大陆手机号格式 |
| province | String | 是 | 非空 |
| city | String | 是 | 非空 |
| district | String | 是 | 非空 |
| detail | String | 是 | 1-120 个字符 |
| postalCode | String | 否 | 为空或 6 位数字 |
| defaultAddress | boolean | 是 | 是否默认地址 |
| deleted | boolean | 是 | 是否删除 |
| createdAt | 时间 | 是 | 创建时间 |
| updatedAt | 时间 | 是 | 更新时间 |

### 3. 订单

订单字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| id | String | 是 | 系统生成，唯一 |
| userId | String | 是 | 下单用户 |
| addressId | String | 是 | 下单时选择的地址 id |
| addressSnapshot | OrderAddressSnapshot | 是 | 下单时的地址快照 |
| createdAt | 时间 | 是 | 创建时间 |

### 4. 订单地址快照

快照字段：

| 字段 | 类型 | 必填 | 规则 |
| --- | --- | --- | --- |
| receiverName | String | 是 | 来自下单时地址 |
| phone | String | 是 | 来自下单时地址 |
| province | String | 是 | 来自下单时地址 |
| city | String | 是 | 来自下单时地址 |
| district | String | 是 | 来自下单时地址 |
| detail | String | 是 | 来自下单时地址 |
| postalCode | String | 否 | 来自下单时地址 |

订单创建后，即使用户修改或删除地址，历史订单中的 `addressSnapshot` 也不能变化。

## 六、错误返回要求

业务校验失败时，需要返回可理解的错误。可以使用异常、结果对象或错误码，但测试中必须能断言错误原因。

建议错误码：

| 错误码 | 场景 |
| --- | --- |
| `ADDRESS_RECEIVER_REQUIRED` | 收件人为空 |
| `ADDRESS_RECEIVER_TOO_LONG` | 收件人超过 30 个字符 |
| `ADDRESS_PHONE_REQUIRED` | 手机号为空 |
| `ADDRESS_PHONE_INVALID` | 手机号格式错误 |
| `ADDRESS_REGION_REQUIRED` | 省、市、区任一为空 |
| `ADDRESS_DETAIL_REQUIRED` | 详细地址为空 |
| `ADDRESS_DETAIL_TOO_LONG` | 详细地址超过 120 个字符 |
| `ADDRESS_POSTAL_CODE_INVALID` | 邮编不是 6 位数字 |
| `ADDRESS_NOT_FOUND` | 地址不存在或已删除 |
| `ADDRESS_NOT_OWNED_BY_USER` | 地址不属于当前用户 |

## 七、L0：创建最小业务骨架

目标：在空目录中创建一个可运行、可测试的最小业务项目。

需要实现：

- 地址模型和内存仓储。
- 订单模型和内存仓储。
- 地址服务和订单服务的空实现或最小实现。
- 一个测试入口，能证明项目可以编译和运行。
- `README.md`，说明如何运行测试。

验收标准：

- 项目不是从已有业务代码复制而来。
- 可以运行测试入口。
- 测试入口至少包含一个基础断言，例如“新建服务对象不为空”。
- L0 不应提前实现 L1/L2/L3 的完整业务规则。

## 八、L1：配送地址基础校验

目标：用户新增或更新配送地址时，字段校验正确。

### 业务规则

- 收件人姓名不能为空。
- 收件人姓名最多 30 个字符。
- 手机号不能为空。
- 手机号必须符合中国大陆手机号格式，建议正则：`^1[3-9]\d{9}$`。
- 省、市、区不能为空。
- 详细地址不能为空。
- 详细地址最多 120 个字符。
- 邮编可以为空。
- 如果填写邮编，必须是 6 位数字。
- 校验失败时不保存地址。

### 正向场景

输入：

```text
userId: user-1001
receiverName: 张三
phone: 13800138000
province: 浙江省
city: 杭州市
district: 西湖区
detail: 文三路 100 号
postalCode: 310000
```

期望：

- 地址保存成功。
- 返回地址 id。
- 保存后的地址字段与输入一致。
- 如果这是用户第一条地址，应成为默认地址。

### 反向场景

至少覆盖：

| 场景 | 输入变化 | 期望 |
| --- | --- | --- |
| 收件人为空 | `receiverName=""` | 保存失败，错误为 `ADDRESS_RECEIVER_REQUIRED` |
| 收件人过长 | 31 个字符 | 保存失败，错误为 `ADDRESS_RECEIVER_TOO_LONG` |
| 手机号为空 | `phone=""` | 保存失败，错误为 `ADDRESS_PHONE_REQUIRED` |
| 手机号格式错误 | `phone="12345"` | 保存失败，错误为 `ADDRESS_PHONE_INVALID` |
| 省为空 | `province=""` | 保存失败，错误为 `ADDRESS_REGION_REQUIRED` |
| 市为空 | `city=""` | 保存失败，错误为 `ADDRESS_REGION_REQUIRED` |
| 区为空 | `district=""` | 保存失败，错误为 `ADDRESS_REGION_REQUIRED` |
| 详细地址为空 | `detail=""` | 保存失败，错误为 `ADDRESS_DETAIL_REQUIRED` |
| 详细地址过长 | 121 个字符 | 保存失败，错误为 `ADDRESS_DETAIL_TOO_LONG` |
| 邮编格式错误 | `postalCode="abc123"` | 保存失败，错误为 `ADDRESS_POSTAL_CODE_INVALID` |

### L1 通过标准

- L1 正向场景通过。
- 所有 L1 反向场景通过。
- 保存失败时仓储中不能新增脏数据。
- 更新地址时同样执行字段校验。

## 九、L2：默认地址规则

目标：一个用户只能有一个默认地址。

### 业务规则

- 用户新增第一条地址时，系统自动设为默认地址。
- 用户新增非第一条地址时，如果未指定默认地址，则默认值为 false。
- 用户新增或更新某条地址为默认地址时，同一用户其他地址必须自动变为非默认。
- 删除默认地址后，如果该用户还有其他未删除地址，应按最近更新时间选一个新的默认地址。
- 删除非默认地址时，不影响当前默认地址。
- 不同用户的默认地址互不影响。
- 已删除地址不能被设置为默认地址。

### 正向场景

场景 1：第一条地址自动默认。

```text
用户 user-1001 新增第一条地址 A
```

期望：

- 地址 A 保存成功。
- 地址 A 的 `defaultAddress=true`。

场景 2：第二条地址设为默认。

```text
用户 user-1001 已有默认地址 A
用户 user-1001 新增地址 B，并指定 defaultAddress=true
```

期望：

- 地址 B 的 `defaultAddress=true`。
- 地址 A 自动变为 `defaultAddress=false`。

场景 3：删除默认地址后自动补默认。

```text
用户 user-1001 有地址 A、B、C
当前默认地址为 B
删除地址 B
```

期望：

- 地址 B 标记为删除。
- 地址 A、C 中最近更新时间较晚的一条成为默认地址。

场景 4：不同用户互不影响。

```text
user-1001 有默认地址 A
user-1002 有默认地址 X
user-1001 将地址 B 设为默认
```

期望：

- user-1001 的默认地址变为 B。
- user-1002 的默认地址仍为 X。

### 反向场景

至少覆盖：

| 场景 | 期望 |
| --- | --- |
| 设置不存在的地址为默认 | 失败，错误为 `ADDRESS_NOT_FOUND` |
| 设置其他用户的地址为默认 | 失败，错误为 `ADDRESS_NOT_OWNED_BY_USER` |
| 设置已删除地址为默认 | 失败，错误为 `ADDRESS_NOT_FOUND` |
| 删除其他用户的地址 | 失败，错误为 `ADDRESS_NOT_OWNED_BY_USER` |

### L2 通过标准

- 任意时刻，同一用户最多只有一个未删除默认地址。
- 默认地址切换不会影响其他用户。
- 删除默认地址后的补默认规则可重复验证。
- L1 所有测试仍然通过。

## 十、L3：下单地址快照

目标：创建订单时保存地址快照，历史订单不受地址后续变化影响。

### 业务规则

- 创建订单时必须传入 `userId` 和 `addressId`。
- 地址必须存在、未删除，并且属于当前用户。
- 订单保存所选地址的快照字段。
- 订单中的地址快照不保存对地址对象的引用，必须是独立值拷贝。
- 用户修改地址后，历史订单地址快照不变。
- 用户删除地址后，历史订单地址快照仍可展示。

### 正向场景

场景 1：使用合法地址创建订单。

```text
user-1001 有地址 A
user-1001 使用地址 A 创建订单 O1
```

期望：

- 订单 O1 创建成功。
- O1.addressId 等于 A.id。
- O1.addressSnapshot 中的收件人、手机号、省市区、详细地址、邮编与地址 A 当时的值一致。

场景 2：修改地址后历史订单不变。

```text
user-1001 使用地址 A 创建订单 O1
随后将地址 A 的 detail 从“文三路 100 号”改为“文三路 200 号”
```

期望：

- 地址 A 的 detail 更新成功。
- O1.addressSnapshot.detail 仍为“文三路 100 号”。

场景 3：删除地址后历史订单仍可展示。

```text
user-1001 使用地址 A 创建订单 O1
随后删除地址 A
```

期望：

- 地址 A 标记为删除。
- O1 仍存在。
- O1.addressSnapshot 仍包含完整地址信息。

### 反向场景

至少覆盖：

| 场景 | 期望 |
| --- | --- |
| 使用不存在的地址创建订单 | 创建失败，错误为 `ADDRESS_NOT_FOUND` |
| 使用已删除地址创建订单 | 创建失败，错误为 `ADDRESS_NOT_FOUND` |
| 使用其他用户的地址创建订单 | 创建失败，错误为 `ADDRESS_NOT_OWNED_BY_USER` |
| 创建订单时 addressId 为空 | 创建失败，错误信息清晰 |

### L3 通过标准

- L3 所有正向和反向场景通过。
- 订单地址快照是值拷贝，不会随地址对象变化。
- L1、L2 所有测试仍然通过。

## 十一、推荐测试数据

用户：

```text
user-1001
user-1002
```

地址 A：

```text
receiverName: 张三
phone: 13800138000
province: 浙江省
city: 杭州市
district: 西湖区
detail: 文三路 100 号
postalCode: 310000
```

地址 B：

```text
receiverName: 李四
phone: 13900139000
province: 上海市
city: 上海市
district: 浦东新区
detail: 世纪大道 88 号
postalCode: 200120
```

地址 C：

```text
receiverName: 王五
phone: 13700137000
province: 江苏省
city: 南京市
district: 玄武区
detail: 中山东路 1 号
postalCode:
```

## 十二、测试覆盖要求

测试至少覆盖：

- L0 项目骨架可以编译或运行。
- L1 地址字段校验全部正向和反向场景。
- L2 默认地址互斥、自动默认、删除补默认、多用户隔离。
- L3 订单快照、地址修改后快照不变、地址删除后订单仍可展示、非法地址下单失败。
- 每轮新增能力不能破坏前一轮测试。

推荐测试命名：

```text
AddressServiceTest.validAddressCanBeSaved
AddressServiceTest.invalidPhoneIsRejected
AddressServiceTest.firstAddressBecomesDefault
AddressServiceTest.switchDefaultAddressClearsPreviousDefault
AddressServiceTest.deleteDefaultAddressPromotesLatestUpdatedAddress
AddressServiceTest.defaultAddressIsIsolatedByUser
OrderServiceTest.createOrderWithAddressSnapshot
OrderServiceTest.addressSnapshotDoesNotChangeAfterAddressUpdate
OrderServiceTest.deletedAddressDoesNotBreakHistoricalOrder
OrderServiceTest.cannotUseOtherUsersAddress
```

## 十三、最终验收清单

完成后逐项确认：

- 项目确实从空文件夹开始。
- 项目中存在清晰的地址和订单业务分层。
- 可以一键运行全部测试。
- L1、L2、L3 全部正向场景通过。
- L1、L2、L3 全部反向场景通过。
- 地址校验失败不会产生脏数据。
- 同一用户任意时刻最多一个默认地址。
- 默认地址规则不影响其他用户。
- 订单地址快照不会随地址修改而变化。
- 删除地址不会影响历史订单展示。
- 错误信息或错误码可以被测试断言。
- README 说明了如何运行项目和测试。

## 十四、问题记录模板

每发现一个问题，按下面格式记录：

```text
阶段：
业务场景：
输入数据：
预期结果：
实际结果：
失败原因：
修复内容：
重新验证结果：
是否通过：
```

最终结论模板：

```text
MiniShop 配送地址中心验收结论：
- L0 项目骨架：
- L1 地址基础校验：
- L2 默认地址规则：
- L3 下单地址快照：
- 测试覆盖：
- 遗留问题：
```
