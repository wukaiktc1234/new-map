# 采购链路状态机梳理

> 范围：采购申请、采购计划、采购订单、到货登记、采购结算  
> 用途：明确各单据的合法状态、状态显示、可执行操作及状态流转 API  
> 生成日期：2026-07-30

---

## 1. 采购申请状态机

### 1.1 状态定义

| 前端状态 | 后端状态 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `draft` | `draft`（语义字符串） | 草稿 | `default` |
| `pending` | `pending` | 待审批 | `warning` |
| `approved` | `approved` | 已审批 | `success` |
| `rejected` | `rejected` | 已拒绝 | `danger` |
| `completed` | `completed` | 已完成 | `success` |
| `cancelled` | `cancelled` | 已取消 | `info` |

### 1.2 状态流转

```
draft ──提交审批──► pending ──审批通过──► approved ──生成订单──► completed
   │                  │
   │                  └──审批拒绝──► rejected
   │                                    │
   └──删除（草稿）                       └──编辑后重新提交──► pending
```

### 1.3 各状态可操作动作

| 当前状态 | 可执行操作 | 调用 API | 目标状态 | 备注 |
|---------|-----------|---------|---------|------|
| `draft` | 编辑 | `PUT /v1/purchase/requests/{id}` | `draft` | 可更新明细与主档 |
| `draft` | 提交审批 | `POST /v1/purchase/requests/{id}/submit` | `pending` | — |
| `draft` | 删除 | `DELETE /v1/purchase/requests/{id}` | 已删除 | 逻辑删除 |
| `pending` | 审批（通过/拒绝） | `POST /v1/purchase/requests/{id}/approve` | `approved` / `rejected` | RequestBody: `{ status, remark }` |
| `rejected` | 编辑 | `PUT /v1/purchase/requests/{id}` | `draft` / `rejected` | 页面重新提交后变为 pending |
| `approved` | 生成采购订单 | `POST /v1/purchase/requests/{id}/generate-order` | `completed` | 自动将申请置为 completed |
| `completed` | 查看 | `GET /v1/purchase/requests/{id}` | — | 只读 |
| `cancelled` | 查看 | `GET /v1/purchase/requests/{id}` | — | 页面未提供主动取消入口 |

### 1.4 状态机问题

- `[待修复]` `cancelled` 状态在类型中存在，但页面未提供任何进入该状态的操作入口。
- `[待修复]` 审批拒绝后，理论上应回到 `draft` 再编辑，但当前页面允许 `rejected` 状态直接编辑并再次提交。
- `[已修复]` `rejected` 状态原仅支持编辑，缺少删除入口；现已在操作列增加删除按钮（`draft` / `rejected` 均可删除）。

---

## 2. 采购计划状态机

### 2.1 状态定义

| 前端状态 | 后端编码 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `draft` | 0 | 草稿 | `default` |
| `pending` | 1 | 待审批 | `warning` |
| `approved` | 2 | 已审批 | `success` |
| `executing` | 3 | 执行中 | `info` |
| `completed` | 4 | 已完成 | `success` |
| `rejected` | 5 | 已拒绝 | `danger` |

### 2.2 状态流转

```
draft ──提交审批──► pending ──审批通过──► approved ──开始执行──► executing ──完成──► completed
   │                  │
   │                  └──审批拒绝──► rejected
   │                                    │
   └──删除                            └──删除（已拒绝可删）
```

### 2.3 各状态可操作动作

| 当前状态 | 可执行操作 | 调用 API | 目标状态 | 备注 |
|---------|-----------|---------|---------|------|
| `draft` | 编辑 | `PUT /v1/purchase/plans/{id}` | `draft` | — |
| `draft` | 提交审批 | `PUT /v1/purchase/plans/{id}/submit` | `pending` | — |
| `draft` | 删除 | `DELETE /v1/purchase/plans/{id}` | 已删除 | — |
| `pending` | 审批通过 | `PUT /v1/purchase/plans/{id}/approve` | `approved` | — |
| `pending` | 审批拒绝 | `PUT /v1/purchase/plans/{id}/reject?reason=...` | `rejected` | — |
| `rejected` | 删除 | `DELETE /v1/purchase/plans/{id}` | 已删除 | 页面显示删除按钮 |
| `approved` | — | — | `executing` | 当前无显式“开始执行”按钮，状态如何进入 executing 未明确 `[待修复]` |
| `executing` | — | — | `completed` | 当前无显式“完成”按钮 `[待修复]` |
| `completed` | 查看 | `GET /v1/purchase/plans/{id}` | — | 只读 |

### 2.4 状态机问题

- `[已修复]` `pending` 状态原缺少审批通过/拒绝入口，现已在操作列增加「审批通过」与「驳回」按钮，调用 `purchasePlanApi.approve` / `reject`。
- `[待修复]` `approved` → `executing` 与 `executing` → `completed` 的触发操作在页面中未找到对应按钮，状态流转存在断点。
- `[待修复]` 进度条逻辑基于状态硬编码百分比，但状态转换触发点不明确。

---

## 3. 采购订单状态机

### 3.1 状态定义

| 前端状态 | 后端编码 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `draft` | 0 | 草稿 | `default` |
| `pending` | 1 | 待审批 | `warning` |
| `approved` | 2 | 已审批 | `info` |
| `ordered` | 6 | 已下单 | `primary` |
| `shipped` | 2 `[待修复]` | 已发货 | `primary` |
| `received` | 4 `[待修复]` | 已收货 | `info` |
| `partial_received` | 3 | 部分到货 | `warning` |
| `completed` | 4 | 已完成 | `success` |
| `rejected` | 5 `[待修复]` | 已拒绝 | `danger` |
| `terminated` | 5 `[待修复]` | 已终止 | `danger` |
| `cancelled` | 5 | 已取消 | `info` |

### 3.2 状态流转（前端语义）

```
draft ──提交审批──► pending ──审批通过──► approved ──确认下单──► ordered ──供应商发货──► shipped
   │                  │                                          │
   │                  └──审批拒绝──► rejected                    ├──部分到货──► partial_received
   │                                                            │
   └──删除（草稿）                                               ├──全部到货──► received ──入库/结算──► completed
                                                                │
                                                                └──终止──► terminated
```

### 3.3 各状态可操作动作

| 当前状态 | 可执行操作 | 调用 API | 目标状态 | 备注 |
|---------|-----------|---------|---------|------|
| `draft` | 编辑 | `PUT /v1/purchase/orders/{id}` | `draft` | — |
| `draft` | 提交审批 | `PUT /v1/purchase/orders/{id}/submit` | `pending` | 同时调用审批工作流 |
| `draft` | 删除 | `DELETE /v1/purchase/orders/{id}` | 已删除 | — |
| `pending` | 审批通过 | `PUT /v1/purchase/orders/{id}/approve` | `approved` | 通过 approvalWorkflowApi 弹窗审批 |
| `pending` | 审批拒绝 | `PUT /v1/purchase/orders/{id}/reject?reason=...` | `cancelled` | 后端映射到 cancelled |
| `approved` | 确认下单 | `PUT /v1/purchase/orders/{id}/confirm` | `ordered` | — |
| `approved` | 终止 | `PUT /v1/purchase/orders/{id}/cancel?reason=...` | `terminated` | 弹窗输入终止原因 |
| `ordered` | 收货 | 跳转 `/purchase/stockin?orderId=...` | — | 不直接改订单状态 |
| `ordered` | 终止 | `PUT /v1/purchase/orders/{id}/cancel?reason=...` | `terminated` | — |
| `shipped` | 收货/终止 | 跳转收货 / `cancel` | `received` / `terminated` | `shipped` 后端无独立状态 `[待修复]` |
| `partial_received` | 继续收货/终止 | 跳转收货 / `cancel` | `received` / `terminated` | — |
| `received` | 查看/结算 | `GET` / 跳转结算页 | — | 只读 |
| `completed` | 查看 | `GET` | — | 只读 |
| `terminated` | 查看 | `GET` | — | 只读 |
| `cancelled` | 查看 | `GET` | — | 只读 |

### 3.4 状态机问题

- `[待修复]` 后端仅支持 7 个状态（0-6），前端 11 个状态中的 `shipped`/`received`/`rejected`/`terminated` 被近似映射，易造成状态歧义。
- `[待修复]` `ordered` 状态在列表操作列显示“收货”按钮，但实际是跳转到到货登记页，订单状态由到货登记间接驱动，流转关系需显式文档化。
- `[待修复]` 审批拒绝与终止均调用 `/cancel` 接口并映射到 `cancelled`，但前端分别显示为 `rejected` 和 `terminated`，语义与后端不一致。

---

## 4. 到货登记状态机

### 4.1 状态定义

| 前端状态 | 后端编码 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `pending` | 0 | 待收货 | `warning` |
| `receiving` | 1 | 收货中 | `pending` |
| `partial_received` | 2 | 部分收货 | `warning` |
| `received` | 3 | 已收货 | `success` |
| `closed` | 4 | 已关闭 | `inactive` |

### 4.2 状态流转

```
pending ──编辑物流/开始收货──► receiving ──部分确认──► partial_received ──全部确认──► received
   │
   └──关闭（手动）──► closed
```

### 4.3 各状态可操作动作

| 当前状态 | 可执行操作 | 调用 API | 目标状态 | 备注 |
|---------|-----------|---------|---------|------|
| `pending` | 编辑物流信息 | `PUT /v1/purchase/arrivals/{id}` | `pending` / `receiving` | 可更新物流公司、单号、预计到货日期等 |
| `pending` | 关闭到货单 | `PUT /v1/purchase/arrivals/{id}/close` | `closed` | 需填写关闭原因 |
| `pending` | 查看详情 | `GET /v1/purchase/arrivals/{id}` | — | — |
| `receiving` | 编辑物流信息 | `PUT /v1/purchase/arrivals/{id}` | `receiving` | — |
| `partial_received` | 编辑物流信息 | `PUT /v1/purchase/arrivals/{id}` | `partial_received` | — |
| `received` | 查看 | `GET /v1/purchase/arrivals/{id}` | — | 只读 |
| `closed` | 查看 | `GET /v1/purchase/arrivals/{id}` | — | 只读 |

### 4.4 状态机问题

- `[待修复]` 到货登记页面操作列未在代码片段中完整展示，但代码中 `canEditLogistics` 判断为 `pending/receiving/partial_received`，`canClose` 仅 `pending`。
- `[待修复]` 到货状态 `received` 如何触发（是收货确认单驱动还是手动确认）在代码中未明确，需补充收货确认流程。
- `[待修复]` 到货单与采购订单状态联动关系未文档化：订单的 `partial_received` / `received` / `completed` 状态应由到货登记反写。

---

## 5. 采购结算状态机

### 5.1 状态定义

| 前端状态 | 后端编码 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `pending` | 0 | 待结算 | `warning` |
| `partial` | 1 | 部分结算 | `info` |
| `finance_reviewing` | 2 | 财务审核中 | `warning` |
| `completed` | 3 | 已结算 | `success` |
| `overdue` | 4 | 已逾期 | `danger` |

### 5.2 状态流转

```
pending ──部分付款──► partial ──继续付款──► completed
   │                       │
   │                       └──标记完成──► completed（当 unpaidAmount=0 时）
   │
   └──逾期──► overdue ──付款──► partial / completed
```

### 5.3 各状态可操作动作

| 当前状态 | 可执行操作 | 调用 API | 目标状态 | 备注 |
|---------|-----------|---------|---------|------|
| `pending` | 编辑 | `PUT /v1/purchase/settlements/{id}` | `pending` | 可调整金额、到期日、付款方式 |
| `pending` | 付款 | `POST /v1/purchase/settlements/{id}/settle?action=pay&voucherNo=...` | `partial` / `completed` | 弹窗输入付款凭证号 |
| `partial` | 编辑 | `PUT /v1/purchase/settlements/{id}` | `partial` | — |
| `partial` | 付款 | `POST .../settle?action=pay` | `partial` / `completed` | 取决于剩余未付金额 |
| `partial` | 标记完成 | `POST .../settle?action=complete` | `completed` | 仅当 `unpaidAmount === 0` |
| `partial` / `completed` | 付款记录 | 本地 `paymentRecords = []` | — | 当前为 TODO，未调用真实 API `[待修复]` |
| `finance_reviewing` | 付款 | `POST .../settle?action=pay` | `partial` / `completed` | 页面允许付款 |
| `overdue` | 付款 | `POST .../settle?action=pay` | `partial` / `completed` | 逾期后仍可付款 |

### 5.4 发票状态

| 发票状态编码 | 中文标签 | 说明 |
|-------------|---------|------|
| 0 | 未开票 | — |
| 1 | 已开票 | — |
| 2 | 已收票 | — |

发票相关 API：
- 申请发票：`POST /v1/purchase/settlements/{id}/invoice/apply`
- 确认收票：`POST /v1/purchase/settlements/{id}/invoice/receive?invoiceNo=...`

### 5.5 状态机问题

- `[待修复]` `finance_reviewing` 状态如何进入/离开在页面中未明确，无“提交财务审核”或“财务审核通过”按钮。
- `[待后端配合]` 付款记录功能为 TODO，当前直接赋值为空数组，已改为弹窗显示「[待后端配合] 付款记录查询功能开发中」。
- `[待修复]` 发票状态 `invoiceStatus` 在结算表单中由用户手动选择（0/1/2），未与 `applyInvoice` / `receiveInvoice` API 联动。

---

## 6. 跨单据状态联动关系

```
采购申请 (approved) ──生成订单──► 采购订单 (draft)
                                    │
                                    ▼ 确认下单
                                  采购订单 (ordered)
                                    │
                                    ▼ 到货登记
                                  到货登记 (pending → received)
                                    │
                                    ▼ 反写订单状态
                                  采购订单 (received / partial_received / completed)
                                    │
                                    ▼ 创建结算
                                  采购结算 (pending → completed)
```

### 联动问题

- `[待修复]` 采购申请生成订单后，申请状态自动变为 `completed`，但代码中是否由后端原子性保证需确认。
- `[待修复]` 采购订单的 `received`/`partial_received`/`completed` 状态由到货登记/入库/结算哪个环节反写，当前代码中未明确。
- `[待修复]` 采购结算 `completed` 后，是否应反写采购订单 `paymentStatus` 为 `paid`，当前未实现。

---

## 7. 状态显示与标签对照

| 单据 | 状态来源 | 标签来源 | 颜色来源 |
|-----|---------|---------|---------|
| 采购申请 | `converters.ts` `purchaseRequestConverter` | `requestStatusLabelMap` | `requestStatusMap` |
| 采购计划 | `converters.ts` `purchasePlanConverter` | `planStatusLabelMap` | `planStatusMap` |
| 采购订单 | `converters.ts` `purchaseOrderConverter` | `orderStatusLabelMap` | `orderStatusMap` |
| 到货登记 | `PurchaseStockin.vue` 本地 `getStatusConfig` | 本地映射 | 本地映射 |
| 采购结算 | `converters.ts` `purchaseSettlementConverter` | `settlementStatusLabelMap` | `settlementStatusMap` |

### 显示问题

- `[待修复]` 到货登记状态映射分散在组件本地，未集中到 `converters.ts`，与其他单据不一致。
- `[待修复]` 采购订单 `approved` 中文标签在 `PurchaseOrderStatusOptions` 中为“已审核”，在 `converters.ts` 中为“已审批”。
- `[待修复]` 采购结算 `pending` 中文标签在类型注释中为“待付款”，在页面选项中为“待结算”。

---

## 8. [待修复] 汇总

1. `[待修复]` 采购申请 `cancelled` 状态无进入入口。
2. `[待修复]` 采购计划 `approved` → `executing` → `completed` 的触发操作缺失。
3. `[待修复]` 采购订单前端 11 状态与后端 7 状态不匹配。
4. `[待修复]` 采购订单审批拒绝与终止均映射到后端 `cancelled`，但前端显示为不同状态。
5. `[待修复]` 到货登记 `received` 状态触发机制未明确（收货确认单驱动 or 手动）。
6. `[待修复]` 到货登记与采购订单状态联动关系未文档化。
7. `[待修复]` 采购结算 `finance_reviewing` 状态进入/离开机制缺失。
8. `[待后端配合]` 采购结算付款记录为 TODO，未调用真实 API，已改为明确「[待后端配合] 功能开发中」提示。
9. `[待修复]` 采购结算发票状态选择未与发票 API 联动。
10. `[待修复]` 状态标签来源不统一：到货登记在组件本地映射，其余在 `converters.ts`。
