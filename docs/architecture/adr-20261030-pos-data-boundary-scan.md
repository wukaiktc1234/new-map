# ADR-20261030：POS 端点「权限门禁 vs 数据边界」全量扫描

> **结论**：10 个 POS controller、共 47 个端点中，**LEAK 5 处（已全部修复）**、**待产品口径 P1 6 处**、
> **权限门禁缺失 3 个 controller**（CallNumber / DiningTable / PosAuth 无 `@PreAuthorize`）。
> 判定口径：LEAK = 按外部传入标识查数据且无「数据属于当前登录人」校验；PASS = 有归属校验；NA = 不涉及他人数据。

## 1. 已修复的 5 处 LEAK（今晚）

| 端点 | 权限码 | 外部可控标识 | 修复 | 实测 |
|---|---|---|---|---|
| `GET /v1/pos/auth/current-shift` | 无（仅登录态） | terminalId | 改 `getMyCurrentShift`（employee 过滤） | B→`data:null` |
| `GET /v1/pos/shift/opening-cash` | `pos:shift:manage` | terminalId | 同上 | B→`data:null` |
| `GET /v1/pos/shift/stats` | `pos:shift:manage` | terminalId | `findActiveShiftByTerminalId`→`getMyCurrentShift` | B→`data:null`（修前 321.00） |
| `GET /v1/pos/shift/exceptions` | `pos:shift:manage` | terminalId | 同上 | B→`[]` |
| `GET /v1/pos/auth/shift/today` | 无（仅登录态） | terminalId | 按 selfKey/userId/username 过滤 | B→**0 条**，A→1 条 |

## 2. 高危但属「业务口径未定」→ P1（今晚不擅自收紧/放宽）

| 端点 | 权限码 | 外部可控标识 | 现状 | 为何不今晚改 |
|---|---|---|---|---|
| `GET /v1/pos/orders` | 类级 `hasAnyAuthority(ROLE_ADMIN/STORE_MANAGER/CASHIER/POS_OPERATOR/*)` | 查询参数 | 无归属过滤 | 「店长是否应看全店订单」是产品口径 |
| `GET /v1/pos/orders/table/{tableNumber}` | 同上 | tableNumber | 无归属过滤 | 按桌号查单跨终端可见，需产品定义 |
| `GET /v1/pos/orders/order/{orderNumber}` | 同上 | orderNumber | 无归属过滤 | 同上（含金额/支付方式） |
| `GET /v1/pos/orders/byOpenid?openid=` | 同上 | openid | 无归属过滤 | 客户维度查询，跨终端可见 |
| `GET /v1/pos/orders/byOrderNumber?orderNumber=` | 同上 | orderNumber | 无归属过滤 | 同上 |
| `POST /v1/pos/orders/order/{orderId}/receipt` | 同上 | orderId | 无归属过滤 | 小票重打，跨终端可打他人小票（信息泄露） |

## 3. 权限门禁缺失（无 `@PreAuthorize`）—— 单独一节，比 LEAK 更基础

| Controller | 端点数 | 现状 | 风险 |
|---|---|---|---|
| `CallNumberController` | 4 GET + 9 写 | **0 个 @PreAuthorize** | 叫号列表/历史/统计对任何登录用户开放 |
| `DiningTableController` | 7 GET + 7 写 | **0 个 @PreAuthorize** | 桌台状态/区域对任何登录用户开放 |
| `PosAuthController` | 6 GET + 3 写 | **0 个 @PreAuthorize**（仅登录态 + 数据边界） | 登录/班次类端点，靠数据边界兜底 |

## 4. 明确 PASS（有归属校验）

| 端点组 | 校验方式 |
|---|---|
| `PosShiftController` /stats /exceptions /opening-cash | `assertTerminalBelongsToMe` + `getMyCurrentShift`（双保险） |
| `PosHoldController` 全部 | `SecurityUtils.getCurrentUserId()` + `terminalId` CAS，查询带 employee_id 谓词 |
| `PosExceptionController` resolve/void | `SecurityUtils.getCurrentUserId()`（P0-D 修复），TxBean 内部自取 |

## 5. NA（不涉及他人数据）

| 端点组 | 理由 |
|---|---|
| `MemberController` 全部 | 会员属门店级公共数据，`member:view` 门禁即为边界（跨门店才需数据边界） |
| `PosApiController` /categories /dishes /combos /menu | 菜单目录公共数据 |
| `PosCouponController` verify/redeem | 券码校验，无终端归属语义 |
| `YoloProductRecognitionController` | 图像识别，无归属语义 |

## 6. 存量不变量违规（今晚新增断言只对新写入生效）

```
pos_shifts.employee_id = 'other'  存在（非纯数字）→ 违反今晚新增的「班次归属键必须 user_id 纯数字」断言
断言只在 startShift 写入时生效，**不追溯存量**
→ 存量违规 1 条（'other'），明日与 33 行夹具一并分批处置
```

## 7. 待办

| 项 | 下一步 |
|---|---|
| P1 六处订单查询 | 等产品口径（店长/跨终端可见范围），不擅自收紧 |
| 3 个无 @PreAuthorize 的 controller | 补权限门禁（需确认各端点语义后逐个加，避免误伤登录类端点） |
| 'other' + 33 行夹具 | 明日分批清理（先 completed，active 单独讨论） |

## 8. 本轮处置结果（Lead 2026-10-30 裁定后）

### 8.1 已补权限门禁：DiningTableController 写端点（7 个）
```
@PreAuthorize("hasAuthority('store:table:manage') or hasAuthority('*')")
POST /v1/pos/tables             PUT /v1/pos/tables/{id}        DELETE /v1/pos/tables/{id}
POST /v1/pos/tables/{id}/occupy POST /{id}/release             POST /transfer
POST /{id}/regenerate-qrcode
```
权限码取证：`store:table:manage` 已授 ROLE_CASHIER / ROLE_STORE_MANAGER / ROLE_SUPER_ADMIN / ROLE_OWNER / ROLE_ADMIN
（**不新造码**，用既有码表语义）。
真实 token 验证（两个角色 + 无 token）：
```
创建/PUT/DELETE/occupy  cashier=200  store_mgr=200        ← 未误伤正常角色
无 token               HTTP 401「未认证，请先登录」        ← 门禁生效
```
⚠ 顺带发现（与本轮门禁无关的**既有缺陷**）：`POST /v1/pos/tables` 进入业务后返回
`{"code":500,"message":"创建桌台失败: JDBC rollback failed"}`（HTTP 仍 200，属旧 catch 包装）。
→ 桌台创建在 schema/事务层面本来就是坏的，记 P1（不是本次门禁导致）。

### 8.2 未补门禁：CallNumberController（按裁定不新造码）
```
实测权限码表：call:* = 0 条 → 无对应语义码
→ 按 Lead 裁定「若既有码表里没有对应码 → 不补门禁，记 P1」（硬造会与产品冲突）
```

### 8.3 第 4 道棘轮：preauthorize_coverage_gate.py
```
模式（与宽 catch 218 / business_error 441 一致）：基线文件 + 只增不减 + self-test 必须能红
  · scripts/qa/preauthorize_coverage_gate.py
  · scripts/qa/preauthorize_baseline.json（首次生成：1966 个历史缺口端点）
  · 判据：基线内允许；**基线外新增无门禁端点 → BLOCK**
self-test 4/4 PASS（含「基线外新增缺口必须 BLOCK」）
实测输出：[GATE] preauthorize_coverage total=2108 有门禁=142 基线内缺=1966 新增缺=0 exit=0
已接 preflight（BLOCK 级）
```

> **一句话**：**历史缺口用基线承载，新增缺口一律阻断**。
> 今晚已有 3 道棘轮（宽 catch 218 / business_error 441 / entity_column），这是第 4 道；**模式已成型**。
