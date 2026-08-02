# 仓储/溯源链路状态机梳理

> 范围：仓储管理（仓库、库位、库存、出库、调拨、盘点、报损、调整、预警）与溯源管理（追溯码、原料追溯码、食品追溯码、质量记录、检验记录、临期预警、召回）
> 用途：明确各对象合法状态、状态显示、可执行操作及状态流转 API
> 生成日期：2026-07-30

---

## 1. 仓储状态机

### 1.1 仓库状态机

#### 1.1.1 状态定义

| 前端状态 | 后端编码 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `active` | 1 | 启用 | `success` |
| `inactive` | 0 | 停用 | `info` |

#### 1.1.2 状态流转

```
active ──停用──► inactive
   │              │
   └──启用────────┘
```

#### 1.1.3 状态机问题

- `[待修复]` 当前视图层面未直接维护仓库档案 CRUD，仓库状态切换未在页面中观察到明确入口。

---

### 1.2 库位状态机

#### 1.2.1 状态定义

| 前端状态 | 后端编码 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `active` | 1 | 启用 | `success` |
| `inactive` | 0 | 停用 | `info` |

#### 1.2.2 状态流转

```
active ──停用──► inactive
   │              │
   └──启用────────┘
```

---

### 1.3 库存状态机

#### 1.3.1 状态定义

| 前端状态 | 后端编码 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `normal` | 1 | 正常 | `success` |
| `warning` | 2 | 预警 | `warning` |
| `expired` | 3 | 过期 | `danger` |
| `frozen` | 4 | 冻结 | `info` |

#### 1.3.2 状态机问题

- `[待修复]` 库存状态由后端根据库存数量/过期时间自动计算，前端仅做展示，但未见明确的状态变更 API 文档。
- `[待修复]` `frozen` 状态在业务上对应“盘点冻结”或“调拨锁定”，但类型与视图中未明确其触发条件。

---

### 1.4 库存出库状态机

#### 1.4.1 状态定义

| 前端状态 | 后端状态 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `pending` | `pending`（字符串） | 待审批 | `warning` |
| `approved` | `approved` | 已审批 | `primary` |
| `completed` | `completed` | 已出库 | `success` |
| `rejected` | `rejected` | 已驳回 | `error` |

#### 1.4.2 状态流转

```
pending ──审批通过──► approved ──执行出库──► completed
   │                      │
   └──审批驳回──────────► rejected
```

#### 1.4.3 各状态可操作动作

| 当前状态 | 可执行操作 | 调用 API | 目标状态 | 备注 |
|---------|-----------|---------|---------|------|
| `pending` | 审批 | `inventoryOutboundApi.approve` | `approved` / `rejected` | 审批表单 `{ approved, opinion }` |
| `approved` | 执行出库 | `inventoryOutboundApi.execute` | `completed` | — |
| `rejected` | 查看详情 | `inventoryOutboundApi.getById` | — | 页面无重新提交入口 |
| `completed` | 查看详情 | `inventoryOutboundApi.getById` | — | 只读 |

#### 1.4.4 状态机问题

- `[待修复]` `rejected` 状态无重新编辑/提交入口，状态机存在断点。
- `[待修复]` 出库单无“取消”状态，申请后无法撤回。

---

### 1.5 库存调拨状态机

#### 1.5.1 状态定义

| 前端状态 | 后端编码 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `pending` | 0 | 待调拨 | `default` |
| `shipped` | 1 | 已发出 | `primary` |
| `received` | 2 | 已接收 | `info` |
| `completed` | 3 | 已完成 | `success` |

#### 1.5.2 状态流转

```
pending ──审批通过──► shipped ──确认收货──► completed
```

#### 1.5.3 各状态可操作动作

| 当前状态 | 可执行操作 | 调用 API | 目标状态 | 备注 |
|---------|-----------|---------|---------|------|
| `pending` | 审批 | `inventoryTransferApi.approve` | `shipped`（通过）/ 未定义（驳回） | 审批表单 `{ approved, remark }` |
| `shipped` | 确认收货 | `inventoryTransferApi.execute` | `completed` | InventoryTransfer.vue 中 `handleConfirmReceipt` 调用 execute |
| `completed` | 查看详情 | `inventoryTransferApi.getById` | — | 只读 |

#### 1.5.4 状态机问题

- `[待修复]` 调拨单类型中定义了 `received` 状态，但页面 UI 从 `shipped` 直接调用 `execute` 进入 `completed`，`received` 状态实际上未被使用。
- `[待修复]` 调拨审批驳回后无明确状态，类型中未定义 `rejected` 状态，但审批表单支持 `approved=false`。
- `[待修复]` 调拨单无“取消”状态，申请后无法撤回。

---

### 1.6 库存盘点状态机

#### 1.6.1 状态定义

| 前端状态 | 后端编码 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `pending` | 0 | 待盘点 | `default` |
| `checking` | 1 | 盘点中 | `warning` |
| `approved` | 2 | 已审核 | `info` |
| `completed` | 3 | 已完成 | `success` |

#### 1.6.2 状态流转

```
pending ──开始盘点──► checking ──审核通过──► approved ──生成调整单/完成──► completed
```

#### 1.6.3 各状态可操作动作

| 当前状态 | 可执行操作 | 调用 API | 目标状态 | 备注 |
|---------|-----------|---------|---------|------|
| `pending` | 查看详情 | `inventoryCheckApi.getById` | — | 页面未提供“开始盘点”入口 |
| `checking` | 审核 | `inventoryCheckApi.approve(row.checkId, true/false)` | `approved`（通过）/ 未定义（驳回） | 弹窗选择通过/驳回 |
| `approved` | 查看详情/生成调整单 | `inventoryCheckApi.getById` + `inventoryAdjustApi.create` | `completed`（隐含） | 详情页提供“生成调整单” |
| `completed` | 查看详情 | `inventoryCheckApi.getById` | — | 只读 |

#### 1.6.4 状态机问题

- `[待修复]` `pending` → `checking` 的流转入口在页面中未明确提供，类型中虽有 `checking` 状态，但创建盘点单后似乎直接进入 `pending`。
- `[待修复]` 盘点详情中额外维护 `warehouseApprovalStatus`（`pending/approved/rejected`）和 `financeConfirmStatus`（`pending/confirmed/rejected`），与主状态 `checkStatus` 存在并行状态，页面仅使用 `checkStatus` 驱动操作，审批流状态未完全闭环。
- `[待修复]` 驳回后状态未明确，类型中无 `rejected` 状态。

---

### 1.7 库存报损状态机

#### 1.7.1 状态定义

| 前端状态 | 后端编码 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `pending` | 0 | 待审核 | `default` |
| `approved` | 1 | 已审核 | `info` |
| `processed` | 2 | 已处理 | `success` |

#### 1.7.2 状态流转

```
pending ──审核通过──► approved ──处理──► processed
```

#### 1.7.3 各状态可操作动作

| 当前状态 | 可执行操作 | 调用 API | 目标状态 | 备注 |
|---------|-----------|---------|---------|------|
| `pending` | 审批 | `inventoryLossApi.approve` | `approved`（通过）/ 未定义（驳回） | 审批表单 `{ approved, remark }` |
| `approved` | 处理 | `inventoryLossApi.process` | `processed` | — |
| `processed` | 查看详情 | `inventoryLossApi.getById` | — | 只读 |

#### 1.7.4 状态机问题

- `[待修复]` 报损单无“驳回”目标状态，类型中未定义 `rejected`。
- `[待修复]` 报损单无“取消”状态。

---

### 1.8 库存调整状态机

#### 1.8.1 状态定义

| 前端状态 | 后端状态 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `pending` | `pending`（字符串） | 待审批 | `warning` |
| `approved` | `approved` | 已审批 | `primary` |
| `completed` | `completed` | 已完成 | `success` |
| `rejected` | `rejected` | 已驳回 | `error` |

#### 1.8.2 状态流转

```
pending ──审批通过──► approved ──执行调整──► completed
   │                      │
   └──审批驳回──────────► rejected
```

#### 1.8.3 各状态可操作动作

| 当前状态 | 可执行操作 | 调用 API | 目标状态 | 备注 |
|---------|-----------|---------|---------|------|
| `pending` | 审批 | `inventoryAdjustApi.approve` | `approved` / `rejected` | 审批表单 `{ approved, opinion }` |
| `approved` | 执行调整 | `inventoryAdjustApi.execute` | `completed` | — |
| `rejected` | 查看详情 | `inventoryAdjustApi.getById` | — | 无重新提交入口 |
| `completed` | 查看详情 | `inventoryAdjustApi.getById` | — | 只读 |

#### 1.8.4 状态机问题

- `[待修复]` `rejected` 状态无重新编辑/提交入口。
- `[待修复]` 调整单无“取消”状态。

---

### 1.9 库存预警记录状态机

#### 1.9.1 状态定义

| 前端表达 | 后端编码 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| 待处理 | 0 | 待处理 | `warning` |
| 已处理 | 1 | 已处理 | `success` |

#### 1.9.2 状态机问题

- `[待修复]` 预警记录状态使用数字 0/1，与仓储其他模块语义化字符串不一致。
- `[待修复]` 预警记录的处理动作未记录具体处理方式（如“采购补货”、“报损”、“忽略”），仅记录 `handleRemark`。

---

## 2. 溯源状态机

### 2.1 追溯码状态机

#### 2.1.1 状态定义

| 前端状态 | 后端状态 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `active` | `active` | 有效 | `success` |
| `recalled` | `recalled` | 已召回 | `danger` |
| `expired` | `expired` | 已过期 | `info` |

#### 2.1.2 状态机问题

- `[待修复]` `TraceCodeVO.status` 类型声明为 `string`，未使用 `TraceCodeStatus` 类型约束。
- `[待修复]` 状态流转 API 未在类型/API 中明确（如如何从 `active` 进入 `recalled`）。

---

### 2.2 原料追溯码状态机

#### 2.2.1 状态定义

| 前端状态 | 后端状态 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `pending` | `pending` | 待入库 | `info` |
| `in_stock` | `in_stock` | 在库 | `success` |
| `picked` | `picked` | 已领用 | `warning` |
| `used` | `used` | 已使用 | `info` |
| `expired` | `expired` | 已过期 | `danger` |
| `returned` | `returned` | 已退货 | `default` |

#### 2.2.2 状态流转

```
pending ──入库──► in_stock ──领用──► picked ──使用──► used
   │                  │
   │                  ├──过期──► expired
   │                  └──退货──► returned
```

#### 2.2.3 状态机问题

- `[待修复]` 原料追溯码状态为字符串，但 `MaterialTraceCodeStatusMap` 中定义了 UI 标签，未明确后端是否同样使用字符串编码。
- `[待修复]` 从 `in_stock` 到 `picked` / `used` / `expired` / `returned` 的流转 API 未在 API 文件中集中梳理。

---

### 2.3 食品追溯码状态机

#### 2.3.1 状态定义（追溯码状态）

| 前端状态 | 后端状态 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `created` | `created` | 已创建 | `info` |
| `printed` | `printed` | 已打印 | `warning` |
| `served` | `served` | 已出餐 | `success` |
| `expired` | `expired` | 已过期 | `danger` |

#### 2.3.2 状态定义（制作状态）

| 前端状态 | 后端状态 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `pending` | `pending` | 待制作 | `info` |
| `making` | `making` | 制作中 | `warning` |
| `completed` | `completed` | 已完成 | `success` |
| `served` | `served` | 已出餐 | `primary` |

#### 2.3.3 状态机问题

- `[待修复]` 食品追溯码存在并行的 `status`（追溯码生命周期）和 `makeStatus`（制作流程），两者都包含 `served`，语义上存在重叠。
- `[待修复]` `status` 从 `created` → `printed` → `served` 的流转与 `makeStatus` 从 `pending` → `making` → `completed` → `served` 的流转未明确联动规则。

---

### 2.4 质量记录状态机

#### 2.4.1 状态定义（异常等级）

| 前端状态 | 后端状态 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `NORMAL` | `NORMAL` | 正常 | `success` |
| `WARNING` | `WARNING` | 警告 | `warning` |
| `CRITICAL` | `CRITICAL` | 严重 | `error` |

#### 2.4.2 状态定义（处理状态）

| 前端状态 | 后端状态 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `PENDING` | `PENDING` | 待处理 | `warning` |
| `PROCESSING` | `PROCESSING` | 处理中 | `primary` |
| `RESOLVED` | `RESOLVED` | 已解决 | `success` |
| `CLOSED` | `CLOSED` | 已关闭 | `info` |

#### 2.4.3 状态机问题

- `[待修复]` 质量记录使用大写枚举字符串，与仓储模块风格不一致。
- `[待修复]` 处理状态流转 API 未在类型/API 中明确。

---

### 2.5 检验记录状态机

#### 2.5.1 状态定义（检验结果）

| 前端状态 | 后端状态 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `QUALIFIED` | `QUALIFIED` | 合格 | `success` |
| `UNQUALIFIED` | `UNQUALIFIED` | 不合格 | `danger` |
| `CONDITIONAL` | `CONDITIONAL` | 有条件合格 | `warning` |

#### 2.5.2 状态定义（检验类型）

| 前端状态 | 后端状态 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `INCOMING` | `INCOMING` | 进货检验 | `primary` |
| `PROCESS` | `PROCESS` | 过程检验 | `warning` |
| `FINAL` | `FINAL` | 成品检验 | `success` |

#### 2.5.3 状态机问题

- `[待修复]` 检验记录无明确的“复核/重检”状态，检验结果一旦录入不可变更。
- `[待修复]` `CONDITIONAL`（有条件合格）后续处理流程未在状态机中体现。

---

### 2.6 临期预警状态机

#### 2.6.1 状态定义（预警等级）

| 前端状态 | 后端状态 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `RED` | `RED` | 红色预警 | `error` |
| `YELLOW` | `YELLOW` | 黄色预警 | `warning` |
| `GREEN` | `GREEN` | 绿色预警 | `success` |

#### 2.6.2 状态定义（处理状态）

| 前端状态 | 后端状态 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `PENDING` | `PENDING` | 待处理 | `warning` |
| `SCRAPPED` | `SCRAPPED` | 已报损 | `error` |
| `RETURNED` | `RETURNED` | 已退货 | `info` |
| `RESOLVED` | `RESOLVED` | 已处理 | `success` |

#### 2.6.3 状态流转

```
RED/YELLOW/GREEN ──报损──► SCRAPPED
              │
              ├──退货──► RETURNED
              │
              └──其他处理──► RESOLVED
```

#### 2.6.4 状态机问题

- `[待修复]` 预警等级 `GREEN` 在业务上通常无需处理，但页面仍可能展示为“待处理”。
- `[待修复]` 临期预警的处理状态与库存预警记录的状态（0/1）风格不一致。

---

### 2.7 召回记录状态机

#### 2.7.1 状态定义

| 前端状态 | 中文标签 | StatusTag 类型 |
|---------|---------|---------------|
| `pending` | 待处理 | `info` |
| `processing` | 处理中 | `warning` |
| `completed` | 已完成 | `success` |
| `cancelled` | 已取消 | `default` |

#### 2.7.2 状态机问题

- `[待修复]` `RecallRecord` 状态使用小写语义化字符串，但 `AffectedTraceCode.status` 使用后端数字（1/2/3/4/5），同一模块状态表达不一致。
- `[待修复]` 召回状态流转 API 未在类型/API 中明确。

---

## 3. 状态机共性问题

1. **审批型单据缺少“驳回/取消”闭环**：出库、调拨、盘点、报损、调整均缺少明确的 `rejected` / `cancelled` 状态及对应操作入口。
2. **状态编码风格分裂**：仓储使用小写语义化字符串 ↔ 后端数字；溯源质量/检验/临期使用大写字符串；库存预警使用数字 0/1。
3. **并行状态增加理解成本**：盘点单同时存在 `checkStatus`、`warehouseApprovalStatus`、`financeConfirmStatus`；食品追溯码同时存在 `status` 和 `makeStatus`。
4. **状态与操作映射未文档化**：多个模块的状态流转 API 散落在视图代码中，未在类型/API 层形成统一规范。
