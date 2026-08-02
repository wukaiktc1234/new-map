# 门店运营 / 订单 / 销售 / 产品中心状态机梳理

> 范围：门店档案、门店证件、门店库存、门店要货、日结对账、餐桌/排队、待办任务、订单、退款、预约、菜品/套餐/分类  
> 用途：明确各业务对象合法状态、状态流转路径、触发条件及问题点  
> 生成日期：2026-07-30

---

## 1. 门店档案状态机

### 1.1 状态定义

| 前端状态 | 后端编码 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `running` | 1 | 营业中 | `success` |
| `renovating` | 2 | 装修中 | `warning` |
| `paused` | 3 | 休息中 | `info` |
| `closed` | 4 | 已关闭 | `danger` |

### 1.2 状态流转

```
                    ┌─ 装修完成 ─► running
                    │
running ──停用──► paused ──启用──► running
   │                              │
   │                              │
   └──关闭────► closed ◄──关闭────┘
   │
   └──装修──► renovating
```

### 1.3 各状态可执行动作

| 当前状态 | 可执行操作 | 调用 API | 目标状态 | 备注 |
|---------|-----------|---------|---------|------|
| `running` | 停用 | `PUT /v1/store-archives/{id}/status` | `paused` | 弹出确认框 |
| `running` | 关闭 | `PUT /v1/store-archives/{id}/status` | `closed` | 页面未显式提供“关闭”入口 `[待修复]` |
| `running` | 编辑 | `PUT /v1/store-archives/{id}` | `running` | — |
| `paused` | 启用 | `PUT /v1/store-archives/{id}/status` | `running` | — |
| `paused` | 编辑 | `PUT /v1/store-archives/{id}` | `paused` | — |
| `renovating` | 装修完成 | `PUT /v1/store-archives/{id}/status` | `running` | 页面未显式提供入口 `[待修复]` |
| `closed` | 重新开业 | `PUT /v1/store-archives/{id}/status` | `running` | 页面未显式提供入口 `[待修复]` |

### 1.4 状态机问题

- `[待修复]` 前端状态选项包含 `renovating`/`closed`，但列表操作列只有“启用/停用”切换，缺少“装修”“关闭”“重新开业”入口。
- `[待修复]` 后端编码在字段矩阵中记录为 `1/2/3/4`，但 `storeDataConverter` 中仅映射 `running=1/paused=0/closed=2`，与门店档案实际编码不一致。

---

## 2. 门店证件状态机

### 2.1 证件状态

| 前端状态 | 后端编码 | 中文标签 | 触发条件 |
|---------|---------|---------|---------|
| `active` | `valid` 语义 | 有效 | `daysLeft > 30` |
| `expiring` | `expiring` 语义 | 即将到期 | `0 < daysLeft <= 30` |
| `expired` | `expired` 语义 | 已过期 | `daysLeft <= 0` |
| `revoked` | 映射为 `expired` | 已吊销 | 后端无独立状态 `[待修复]` |

### 2.2 状态流转

```
active ──临近到期（30天内）──► expiring ──到期──► expired
   │                                              ▲
   └─────────────── 吊销/撤销 ────────────────────┘ (revoked 归并到 expired)
```

### 2.3 续期费用报销状态机

| 前端状态 | 中文标签 | 触发角色 |
|---------|---------|---------|
| `unreimbursed` | 待报销 | 员工/店长 |
| `submitted` | 已提交 | 员工 |
| `manager_approved` | 店长已批 | 店长 |
| `finance_approved` | 财务已审 | 财务 |
| `reimbursed` | 已核销 | 财务 |

```
unreimbursed ──员工提交──► submitted ──店长审批──► manager_approved ──财务审核──► finance_approved ──核销──► reimbursed
      │                         │                          │
      │                         └────店长驳回───────────────┘
      │                                                    │
      └────────────────────── 财务驳回 ────────────────────┘
```

### 2.4 状态机问题

- `[待修复]` 证件通用模型（7 种证照类型）后端仅实现 `HealthCertificate`（员工健康证），营业执照/食品经营许可等类型无法持久化。
- `[待修复]` `revoked` 状态后端无编码，被映射为 `expired`，状态语义丢失。
- `[待修复]` 续期费用审批状态仅存前端内存，刷新页面后丢失，未调用真实 API。
- `[待修复]` 续期操作仅更新 `expiryDate`，未生成独立的“续期记录” persisted 数据。

---

## 3. 门店库存状态机

> 说明：门店库存无真实业务状态字段，状态由前端根据 `availableQuantity / quantity` 比例计算。

| 前端状态 | 计算规则 | 中文标签 |
|---------|---------|---------|
| `normal` | `quantity > 0` 且 `0.2 <= ratio <= 0.9` | 正常 |
| `warning` | `quantity > 0` 且 `ratio < 0.2` | 预警 |
| `low` | `quantity === 0` | 不足 |
| `over` | `quantity > 0` 且 `ratio > 0.9` | 过量 |

### 状态机问题

- `[待修复]` 库存状态为前端派生，数据库 `store_inventory` 无 `status` 字段，无法按状态筛选或统计。
- `[待修复]` `availableQuantity` 在数据库表中未确认存在，计算依据缺失。

---

## 4. 门店要货状态机

### 4.1 状态定义

| 前端状态 | 中文标签 | StatusTag 类型 |
|---------|---------|---------------|
| `draft` | 草稿 | `default` |
| `pending` | 待审核 | `warning` |
| `approved` | 已审核 | `success` |
| `rejected` | 已驳回 | `danger` |
| `converted` | 已转采购申请 | `info` |

### 4.2 状态流转

```
draft ──提交审批──► pending ──审核通过──► approved ──转采购申请──► converted
   │                    │
   │                    └──审核驳回──► rejected ──重新编辑──► draft
   │
   └──删除
```

### 4.3 各状态可执行动作

| 当前状态 | 可执行操作 | 调用 API | 目标状态 | 备注 |
|---------|-----------|---------|---------|------|
| `draft` | 编辑 | `PUT /v1/material-requests/{id}` | `draft` | — |
| `draft` | 删除 | `DELETE /v1/material-requests/{id}` | 已删除 | — |
| `draft` | 提交审批 | `POST /v1/material-requests/{id}/submit` | `pending` | 支持批量 |
| `pending` | 审核通过 | `POST /v1/material-requests/{id}/approve` | `approved` | — |
| `pending` | 审核驳回 | `POST /v1/material-requests/{id}/reject` | `rejected` | — |
| `rejected` | 编辑 | `PUT /v1/material-requests/{id}` | `draft` | 编辑后需再次提交 |
| `approved` | 转采购申请 | 未实现 | `converted` | `[待修复]` 无真实 API |
| `converted` | 查看 | `GET /v1/material-requests/{id}` | — | 只读 |

### 4.4 状态机问题

- `[待修复]` `converted` 状态仅存在于类型和选项中，无进入该状态的真实操作。
- `[待修复]` 审核驳回后理论上应回到 `draft`，但页面允许直接编辑并再次提交，未强制回到 `draft`。

---

## 5. 日结对账状态机

### 5.1 状态定义

| 前端状态 | 后端编码 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `pending` | 0 | 待对账 | `warning` |
| `approved` | 1 | 已对账 | `success` |
| `rejected` | 2 | 已驳回 | `danger` |

### 5.2 状态流转

```
pending ──确认对账──► approved
    │
    └──驳回──► rejected ──重新对账──► approved
```

### 5.3 各状态可执行动作

| 当前状态 | 可执行操作 | 调用 API | 目标状态 | 备注 |
|---------|-----------|---------|---------|------|
| `pending` | 创建日结 | `POST /v1/daily-settlements` | `approved` | 创建即确认 |
| `pending` | 查看详情 | `GET /v1/daily-settlements/{id}` | — | — |
| `approved` | 查看详情 | `GET /v1/daily-settlements/{id}` | — | 只读 |
| `rejected` | 重新对账 | `PUT /v1/daily-settlements/{id}` | `approved` | 页面未提供入口 `[待修复]` |

### 5.4 状态机问题

- `[待修复]` 创建日结接口直接生成 `approved` 状态，缺少独立的“确认/驳回”动作。
- `[待修复]` `rejected` 状态仅存在于类型定义，页面无驳回入口及重新对账入口。

---

## 6. 餐桌状态机

### 6.1 状态定义

| 前端状态 | 后端编码 | 中文标签 |
|---------|---------|---------|
| `idle` | 1 | 空闲 |
| `dining` | 2 | 用餐中 |
| `reserved` | 3 | 已预约 |
| `maintenance` | 4 | 维护中 |
| `disabled` | 5 | 停用 |

### 6.2 状态流转

```
idle ──开台/点餐──► dining ──结账──► idle
  │                     │
  │                     └──预约占用──► reserved ──到店──► dining
  │
  └──维护──► maintenance ──恢复──► idle
  └──停用──► disabled ──启用──► idle
```

### 6.3 状态机问题

- `[待修复]` 餐桌状态由 POS 点餐/预约系统驱动，管理端页面仅展示状态，未提供维护/停用/恢复等操作入口。
- `[待修复]` 餐桌状态与预约状态未形成联动约束（同一桌台可同时被预约和用餐）。

---

## 7. 叫号排队状态机

### 7.1 状态定义

| 前端状态 | 后端编码 | 中文标签 |
|---------|---------|---------|
| `waiting` | 1 | 等待中 |
| `called` | 2 | 已叫号 |
| `expired` | 3 | 已过号 |
| `dined` | 4 | 已用餐 |
| `cancelled` | 5 | 已取消 |

### 7.2 状态流转

```
waiting ──叫号──► called ──到店──► dined
   │         │
   │         └──过号──► expired ──重新排队──► waiting
   │
   └──取消──► cancelled
```

### 7.3 状态机问题

- `[待修复]` 排队历史页面为只读列表，无叫号、取消、重新排队等操作入口。
- `[待修复]` `customerName` / `customerPhone` 在页面中为占位符“-”，需后端补充。

---

## 8. 待办任务状态机

### 8.1 状态定义

| 前端状态 | 后端编码 | 中文标签 |
|---------|---------|---------|
| `pending` | 0 | 待处理 |
| `completed` | 1 | 已完成 |
| `expired` | 2 | 已逾期 |
| `cancelled` | 3 | 已取消 |

### 8.2 状态流转

```
pending ──处理完成──► completed
    │
    └──逾期────► expired
    │
    └──取消────► cancelled
```

### 8.3 状态机问题

- `[待修复]` 待办任务页面无“标记完成”“取消”等操作按钮，仅支持跳转处理。
- `[待修复]` `expired` 状态由前端根据 `deadline` 和 `overdueDays` 判断，无后端状态同步机制。

---

## 9. 订单状态机

### 9.1 状态定义

| 前端状态 | 后端编码 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `pending` | 0 | 待确认 | `warning` |
| `confirmed` | 1 | 已确认 | `primary` |
| `completed` | 2 | 已完成 | `success` |
| `cancelled` | 3 | 已取消 | `info` |
| `partial_refund` | 4 | 部分退款 | `warning` |
| `full_refund` | 5 | 全额退款 | `error` |
| `pending_review` | 6 | 待评价 | `primary` |

### 9.2 状态流转

```
pending ──确认──► confirmed ──完成──► completed ──评价──► pending_review
   │                               │
   ├──取消──► cancelled            ├──部分退款──► partial_refund
   │                               │
   └──全额退款──► full_refund      └──全额退款──► full_refund
```

### 9.3 各状态可执行动作

| 当前状态 | 可执行操作 | 调用 API | 目标状态 | 备注 |
|---------|-----------|---------|---------|------|
| `pending` | 查看详情 | `GET /v1/orders/{id}` | — | — |
| `pending` | 取消 | `POST /v1/orders/{id}/cancel` | `cancelled` | 需填写取消原因 |
| `pending` | 退款 | 未实现 | — | `[待修复]` 仅提示去退款管理页 |
| `confirmed` | 完成 | 由 POS 结账触发 | `completed` | 管理端无手动完成入口 |
| `confirmed` | 退款 | 未实现 | `partial_refund` / `full_refund` | `[待修复]` |
| `completed` | 评价 | 未实现 | `pending_review` | 管理端不涉及 |
| `cancelled` | 查看 | `GET /v1/orders/{id}` | — | 只读 |
| `partial_refund` / `full_refund` | 查看 | `GET /v1/orders/{id}` | — | 只读 |

### 9.4 状态机问题

- `[待修复]` 订单查询页无“确认订单”按钮，`pending` 到 `confirmed` 的流转未在管理端暴露。
- `[待修复]` 退款操作仅提示跳转，未提供创建退款申请的真实入口。
- `[待修复]` `pending_review` 状态进入/离开机制未在管理端体现。

---

## 10. 退款单状态机

### 10.1 状态定义

| 前端状态 | 后端编码 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `pending` | 1 | 待审核 | `warning` |
| `approved` | 2 | 已通过 | `primary` |
| `rejected` | 3 | 已拒绝 | `error` |
| `executed` | 4 | 已完成 | `success` |

### 10.2 状态流转

```
pending ──审核通过──► approved ──执行退款──► executed
    │
    └──审核拒绝──► rejected ──重新申请──► pending
```

### 10.3 各状态可执行动作

| 当前状态 | 可执行操作 | 调用 API | 目标状态 | 备注 |
|---------|-----------|---------|---------|------|
| `pending` | 审核 | `PUT /v1/order-refunds/{id}/approve?approved=true|false` | `approved` / `rejected` | 可调整退款金额 |
| `approved` | 执行退款 | 未实现 `[待修复]` | `executed` | 页面统计“处理中”状态，但无执行按钮 |
| `rejected` | 重新申请 | 未实现 `[待修复]` | `pending` | 无入口 |
| `executed` | 查看 | `GET /v1/order-refunds/{id}` | — | 只读 |

### 10.4 状态机问题

- `[待修复]` `approved` → `executed` 的执行退款动作未在页面实现。
- `[待修复]` `rejected` → `pending` 的重新申请路径未实现。
- `[待修复]` 退款类型（全额/部分）依赖 `refundReason` 关键字判断，缺少 `refundType` 字段。

---

## 11. 预约状态机

### 11.1 状态定义

| 前端状态 | 后端编码 | 中文标签 | StatusTag 类型 |
|---------|---------|---------|---------------|
| `pending` | 1 | 待确认 | `warning` |
| `confirmed` | 2 | 已确认 | `primary` |
| `arrived` | 3 | 已到店 | `success` |
| `cancelled` | 4 | 已取消 | `info` |
| `no_show` | 5 | 未到 | `error` |

### 11.2 状态流转

```
pending ──确认──► confirmed ──到店──► arrived
   │                              │
   ├──取消──► cancelled           └──未到──► no_show
   │
   └──直接到店？──► arrived（业务上通常需先确认）
```

### 11.3 各状态可执行动作

| 当前状态 | 可执行操作 | 调用 API | 目标状态 | 备注 |
|---------|-----------|---------|---------|------|
| `pending` | 编辑 | `PUT /v1/reservations/{id}` | `pending` | — |
| `pending` | 确认 | `POST /v1/reservations/{id}/confirm` | `confirmed` | — |
| `pending` | 取消 | `POST /v1/reservations/{id}/cancel` | `cancelled` | 支持填写原因 |
| `confirmed` | 标记到店 | `POST /v1/reservations/{id}/arrive` | `arrived` | — |
| `confirmed` | 取消 | `POST /v1/reservations/{id}/cancel` | `cancelled` | — |
| `arrived` | 查看 | `GET /v1/reservations/{id}` | — | 只读 |
| `cancelled` / `no_show` | 查看 | `GET /v1/reservations/{id}` | — | 只读 |

### 11.4 状态机问题

- `[待修复]` `no_show` 状态无任何进入操作，系统未自动将过期未到店预约标记为 `no_show`。
- `[待修复]` 预约编辑时 `storeId` 从桌台反推，可能覆盖后端真实值。

---

## 12. 产品（菜品 / 套餐 / 分类）状态机

### 12.1 状态定义

| 前端状态 | 后端编码 | 中文标签 |
|---------|---------|---------|
| `active` | 1 | 启用/在售 |
| `inactive` | 0 | 停用/停售 |
| `soldout` | 2 | 售罄（仅菜品） |

### 12.2 状态流转

```
               ┌── 库存归零 ──► soldout ──补货──► active
               │
active ──停用──► inactive ──启用──► active
```

### 12.3 各状态可执行动作

| 业务对象 | 当前状态 | 可执行操作 | 调用 API | 目标状态 |
|---------|---------|-----------|---------|---------|
| 菜品 | `active` | 停用 | `PUT /v1/product-center/foods/{id}/status/{status}` | `inactive` |
| 菜品 | `inactive` | 启用 | `PUT .../status/{status}` | `active` |
| 菜品 | `soldout` | 启用 | `PUT .../status/{status}` | `active` |
| 套餐 | `active` | 停用 | `PUT /v1/product-center/combos/{id}/status/{status}` | `inactive` |
| 套餐 | `inactive` | 启用 | `PUT .../status/{status}` | `active` |
| 分类 | `active` | 停用 | `PUT /v1/product-center/categories/{id}/status/{status}` | `inactive` |
| 分类 | `inactive` | 启用 | `PUT .../status/{status}` | `active` |

### 12.4 状态机问题

- `[待修复]` 套餐/分类状态仅支持 `active/inactive`，但产品中心统一类型 `ProductStatus` 包含 `soldout`，类型上存在歧义。
- `[待修复]` 菜品 `soldout` 状态由库存联动触发还是手动触发，未在代码中明确。

---

## 13. 跨模块状态联动关系

```
门店要货 (approved) ──转采购申请──► 采购申请 (draft/pending)
                                    │
                                    ▼ 审批通过后生成采购订单
                                  采购订单 (ordered) ──到货──► 门店库存增加
                                    │
                                    ▼ 结算完成后
                                  日结对账 (approved)

订单 (confirmed/completed) ──发起退款──► 退款单 (pending → executed)
                              │
                              └──退款完成后反写订单状态 (partial_refund / full_refund)

预约 (confirmed) ──到店──► 餐桌 (dining)
```

### 联动问题

- `[待修复]` 门店要货转采购申请的真实 API 未实现，联动链路断裂。
- `[待修复]` 订单退款完成后是否由退款单反写订单状态为 `partial_refund`/`full_refund`，代码中未明确。
- `[待修复]` 预约到店后是否自动将餐桌状态置为 `dining`，管理端未体现。

---

## 14. [待修复] 汇总

1. `[待修复]` 门店档案缺少“装修”“关闭”“重新开业”操作入口。
2. `[待修复]` 门店证件通用模型与健康证实体错位，7 种证照类型无法全部存储。
3. `[待修复]` 证件 `revoked` 状态后端无编码。
4. `[待修复]` 证件续期费用审批流程仅存内存。
5. `[待修复]` 门店库存状态为前端派生，数据库缺少 `status`/`available_quantity` 字段。
6. `[待修复]` 门店要货 `converted` 状态无进入操作。
7. `[待修复]` 日结对账 `rejected` 状态无进入/离开操作。
8. `[待修复]` 餐桌/排队历史页面缺少状态变更操作入口。
9. `[待修复]` 待办任务页面缺少“完成”“取消”等操作入口。
10. `[待修复]` 订单管理端缺少“确认订单”“创建退款”入口。
11. `[待修复]` 退款单 `approved` → `executed` 执行动作缺失。
12. `[待修复]` 预约 `no_show` 状态无进入机制。
13. `[待修复]` 产品 `soldout` 状态触发机制未明确。
