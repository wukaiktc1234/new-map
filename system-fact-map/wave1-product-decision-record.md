# Wave 1 Product Decision Record（订单真相源 + 状态机裁决）

> - 文档编号：W1-PDR-V1.0
> - 日期：2026-09-09
> - 基于：Fact Map（§1/§5/§8）+ ORD 审查 + CDDR + PD-015 已有事实
> - 角色：架构总控（会话1）
> - **阶段声明**：本阶段为 **Product Decision Gate**——只裁决，不修改代码/表/UI。裁决后由 Developer 按 Engineering Card 执行。
> - **禁止**：立即修改 orders 表 / 立即修改 OrderQuery.vue / 删除 legacy / 自行设计新订单状态 / UI workaround 绕过真相冲突。

---

## 0. 前置事实确认（Fact Map §5.1 + ORD §1.3/§2.2）

### 0.1 三套订单状态机（CONFLICT，当前事实）

| 模型 | 状态值 | 表 | 字段 | 写入方 |
|---|---|---|---|---|
| **新表 order_status** | 0待确认→1已确认→2已完成→3已取消；4部分退款 5全额退款 6待评价 | orders | order_status (Integer) | OrderNewServiceImpl / OrderService |
| **新表 payment_status** | 0未支付 1部分 2已支付 3已退款 | orders | payment_status (Integer) | OrderNewServiceImpl / OrderService |
| **legacy status** | -1支付中 0待支付 1已支付 2待配送 3配送中 4已完成 5已取消 6退款中 7已退款 | orders_legacy | status (Integer) | PosOrderCreateServiceImpl / PosOrderPaymentServiceImpl |
| **sales_order** | pending/preparing/completed/delivered（String） | 无表（虚实体） | status (String) | SalesOrderServiceImpl（映射 orders 表，但 8 列表无） |

### 0.2 双表核心差异

| 维度 | orders（新） | orders_legacy（旧） |
|---|---|---|
| 主键 | BIGINT order_id | VARCHAR order_id |
| 金额 | **分**（Long） | **元**（DECIMAL 12,2） |
| 状态 | order_status(0-6) + payment_status(0-3) **双字段** | status(0-7) **单字段** |
| 门店 | 有 store_id | 无 store_id |
| 明细表 | order_items | order_items_legacy |
| 支付记录 | order_payment_records | finance_record（POS 路径） |
| 退款记录 | order_refund_records | 无独立表（改 status+流水） |
| 事件 | OrderCreatedEvent / OrderCompletedEvent / OrderRefundEvent | 无事件（POS 直接操作） |

### 0.3 当前谁写哪张表

| 来源 | 写入表 | API |
|---|---|---|
| POS 收银（Home.vue） | **orders_legacy** | /v1/pos/orders/order |
| POS 扫码（Order.vue） | **orders_legacy** | /v1/pos/orders/order/table |
| POS 快速下单 | **orders**（新表） | /v1/pos/orders/create |
| 管理端 | **orders**（新表） | /v1/orders |
| 小程序 | **orders_legacy** | /v1/pos/order |

---

## 1. PD-015 阶段二执行裁决

### 1.1 Business Truth

| 项 | 裁决 |
|---|---|
| **唯一生产真相** | `orders` 表 = 订单唯一真相源（PADR §九-A 已确认） |
| **legacy 定位** | `orders_legacy` = **POS 历史遗留**（只冻结不删除；POS 链路待 G1 后统一） |
| **sales_order 定位** | **虚实体废弃**（PD-015 阶段一已禁新增引用；阶段二=停止读取） |

### 1.2 Canonical Table

| 表 | 角色 | 处置 |
|---|---|---|
| `orders` | **Canonical**（唯一真相源） | 保留；不改 schema（产品负责人禁止） |
| `orders_legacy` | **Legacy**（冻结，不删除） | 冻结新写入（POS 链路迁移后）；历史数据保留只读；迁移后停止双写 |
| `order_items` / `order_items_legacy` | 同上 | 随主表 |
| `order_payment_records` | Canonical 支付记录 | 保留 |
| `finance_record`（POS 路径） | Legacy 支付流水 | 保留只读（POS 财务落点） |

### 1.3 Canonical API

| API | 角色 | 处置 |
|---|---|---|
| /v1/orders（管理端） | **Canonical** | 保留 |
| /v1/pos/orders/create（快速下单） | **Canonical**（已写 orders） | 保留 |
| /v1/pos/orders/order（收银下单） | **Legacy**（写 orders_legacy） | 阶段二=**冻结新调用**（迁移后不再写 legacy） |
| /v1/pos/orders/order/table（扫码） | **Legacy**（写 orders_legacy） | 同上 |
| /v1/pos/order（小程序） | **Legacy**（写 orders_legacy） | 同上 |
| /v1/sales/order/*（8 端点） | **Legacy**（读 orders 但带虚实体） | 停止消费（PD-015 阶段一已禁新增引用） |

### 1.4 Migration Strategy（代码迁移，不改表结构）

| 步骤 | 内容 | 前置 | 风险 |
|---|---|---|---|
| **阶段二-A：POS 收银写入迁移** | PosOrderCreateServiceImpl.createOrder 从写 orders_legacy 改为写 orders（含金额元→分转换） | PD-017~021 状态语义裁决 | **高**（POS 核心链路） |
| **阶段二-B：POS 扫码写入迁移** | createTableOrder 从写 orders_legacy 改为写 orders | 同上 | **高** |
| **阶段二-C：小程序写入迁移** | /v1/pos/order 从写 orders_legacy 改为写 orders | 同上 | **中** |
| **阶段二-D：POS 读取迁移** | POS 菜单/订单查询从读 orders_legacy 改为读 orders | 阶段二-A/B 完成 | **中** |
| **阶段二-E：财务链路对齐** | POS finance_record 支付流水从 legacy 订单号改为 orders 订单号 | 阶段二-A/B 完成 | **中** |
| **阶段二-F：legacy 冻结** | orders_legacy 停止新写入（保留历史只读） | 阶段二-A/B/C 完成 | **低**（冻结后无新写） |

### 1.5 Legacy Handling

- orders_legacy **不删除**（历史数据保留；POS 退款回补等旧链路仍读）
- 迁移期间双写（新表为主，legacy 写入逐步停用）
- 迁移后 legacy 表标记 `@Deprecated`（代码层）+ 数据保留（DB 层）

### 1.6 不允许工程自行决定的边界

| 边界 | 说明 |
|---|---|
| ❌ 不改 orders 表 schema | 产品负责人明确禁止 |
| ❌ 不新增订单状态值 | 三套状态机的语义统一在 PD-017~021 中裁决 |
| ❌ 不删除 orders_legacy | 冻结不删除 |
| ❌ 不通过 UI workaround 绕过 | 不做"前端显示 orders 数据但后端仍写 legacy"的假迁移 |

---

## 2. PD-017~021 订单状态语义裁决

### 2.1 核心问题

orders 表有 order_status(0-6) + payment_status(0-3) **双字段**。当前语义：

| order_status | 语义 | payment_status | 语义 |
|---|---|---|---|
| 0 | 待确认/待支付 | 0 | 未支付 |
| 1 | 已确认/已支付 | 1 | 部分支付 |
| 2 | 已完成 | 2 | 已支付 |
| 3 | 已取消 | 3 | 已退款 |
| 4 | 部分退款 | — | — |
| 5 | 全额退款 | — | — |
| 6 | 待评价 | — | — |

### 2.2 裁决方案：保持双字段分离，不做合并

| 决策项 | 裁决 | 理由 |
|---|---|---|
| **是否合并 order_status + payment_status** | **不合并**（保持双字段） | PADR-004 已裁决「订单成立/支付/库存/财务严格分离」；双字段=两个独立生命周期维度（订单生命周期 + 资金生命周期），合并违反原则 4（状态单一业务语义） |
| **order_status 0-6 语义** | **沿用现有**（0=待确认→1=已确认→2=已完成→3=已取消；4=部分退款 5=全额退款 6=待评价） | 已有 7 值覆盖完整生命周期；不新增（产品负责人禁止） |
| **payment_status 0-3 语义** | **沿用现有**（0=未支付→1=部分→2=已支付→3=已退款） | 已有 4 值覆盖资金维度；不新增 |
| **legacy status 0-7 映射** | **不映射到新表**（legacy 冻结后废弃） | legacy 是遗留；迁移后以 orders 为准；不做 legacy→orders 的 status 映射表 |
| **sales_order String 状态** | **废弃**（PD-015 阶段一已禁新增引用；阶段二=停止消费） | 虚实体无表，映射无意义 |

### 2.3 状态机图（Canonical，orders 表双字段）

```
订单生命周期（order_status）：
  0(待确认) → 1(已确认) → 2(已完成) → 4/5(退款)
  0(待确认) → 3(已取消)
  1(已确认) → 3(已取消)（未支付可取消）
  2(已完成) → 4(部分退款) / 5(全额退款) → 6(待评价)

资金生命周期（payment_status）：
  0(未支付) → 1(部分) → 2(已支付) → 3(已退款)

交叉约束：
  order_status=0 时 payment_status 必须=0
  order_status=3(取消) 时 payment_status 必须=0 或 3
  order_status=2(完成) 时 payment_status 必须=2
```

### 2.4 POS legacy status 到 orders 的事实差异（不映射，冻结处理）

| legacy status | orders 对应事实 | 说明 |
|---|---|---|
| -1（支付中） | payment_status=0 + 无对应 | legacy 独有（CAS 中间态），orders 无此值 |
| 0（待支付） | order_status=0 + payment_status=0 | ✅ 对应 |
| 1（已支付） | order_status=0/1 + payment_status=2 | **不精确对应**（legacy 单字段无法区分订单状态和支付状态） |
| 2-3（配送） | 无对应 | 餐饮无配送概念（legacy 遗留） |
| 4（已完成） | order_status=2 + payment_status=2 | ✅ 对应 |
| 5（已取消） | order_status=3 | ✅ 对应 |
| 6-7（退款中/已退款） | order_status=4/5 + payment_status=3 | 不精确对应 |

> **关键差异**：legacy 单字段无法区分「已确认待支付」和「已支付待确认」——迁移时必须用双字段重新表达。

### 2.5 POS / Kitchen / Mobile 影响

| 端 | 影响 | 处置 |
|---|---|---|
| POS 收银 | 写入从 legacy 改为 orders（阶段二-A）；支付 CAS 适配 orders 双字段 | 高风险，需灰度 |
| POS 扫码 | 同上（阶段二-B） | 高风险 |
| 小程序 | 写入从 legacy 改为 orders（阶段二-C） | 中风险 |
| 后厨（KDS） | 当前读 legacy 订单——迁移后改读 orders | 中风险 |
| 手持收货 | 读订单状态——迁移后读 orders | 低风险 |

### 2.6 Management Frontend 影响

| 页面 | 影响 | 处置 |
|---|---|---|
| OrderQuery（订单查询） | 从读 orders 改为读 orders（已经是 canonical）；**禁止 UI workaround** | 验证现有读 orders 链路正确 |
| OrderStatistics | 统计数据源从 orders 取（已 canonical） | 验证 |
| OrderRefund（退款） | 退款操作从 orders 取数据 | 验证 |
| Dashboard（今日销售概览） | 从 orders 取（已 canonical）；KL-054 修复依赖 PD-015 | 解锁 |

### 2.7 Event / Side Effect

| 事件 | 影响 | 处置 |
|---|---|---|
| OrderCreatedEvent | POS 迁移后从 orders 发出（已 canonical） | 验证事件载荷兼容 |
| OrderCompletedEvent | 同上 | 验证 |
| OrderRefundEvent | 同上 | 验证 |
| POS finance_record 支付流水 | 从 legacy 订单号改为 orders 订单号（阶段二-E） | 需兼容期（旧流水保留，新流水用 orders 订单号） |

---

## 3. Migration / Legacy Principles

### 3.1 迁移原则

| # | 原则 | 说明 |
|---|---|---|
| M-1 | **不改表结构** | orders 表 schema 不变（产品负责人禁止）；迁移=代码层写入目标切换 |
| M-2 | **先语义再代码** | PD-017~021 状态语义裁决先行；代码迁移按裁决执行 |
| M-3 | **灰度+可回退** | 每阶段（A~F）独立灰度；回退=恢复 legacy 写入（配置开关） |
| M-4 | **双写过渡** | 迁移期间新表+旧表同时写入（以新表为准）；冻结后停止旧表写入 |
| M-5 | **每批 QA+基线** | 每阶段独立 QA 验收+回归基线扩展（协议 V1.0 批次节奏） |
| M-6 | **不删除 legacy** | orders_legacy 数据保留（历史查询/退款回补/审计追溯）；代码标注 @Deprecated |

### 3.2 Legacy 冻结条件

orders_legacy 可冻结的前提：
1. 阶段二-A/B/C 完成（POS/扫码/小程序全部迁移到 orders 写入）
2. 阶段二-D 完成（POS 读取迁移到 orders）
3. 阶段二-E 完成（财务链路对齐）
4. 灰度期观察无异常（≥7 天）
5. 产品负责人确认冻结

---

## 4. Engineering Unlock 条件

### 4.1 Wave 1 可启动的前提

| 条件 | 状态 |
|---|---|
| PD-015 阶段二执行裁决（§1） | ✅ 本记录裁决 |
| PD-017~021 状态语义裁决（§2） | ✅ 本记录裁决（保持双字段，不合并，沿用现有语义） |
| Wave 0 完成（W0-EC-01~04 PASS） | ✅ 已完成 |
| Fact Map 冻结 | ✅ 已冻结 |

### 4.2 Wave 1 Engineering Card 规划

| 编号 | WP | 内容 | 依赖 | 优先级 |
|---|---|---|---|---|
| W1-EC-01 | POS 收银写入迁移（阶段二-A） | PosOrderCreateServiceImpl.createOrder 从 legacy→orders | PD-017~021 裁决 | P0 |
| W1-EC-02 | POS 扫码写入迁移（阶段二-B） | createTableOrder 从 legacy→orders | 同上 | P0 |
| W1-EC-03 | 小程序写入迁移（阶段二-C） | /v1/pos/order 从 legacy→orders | 同上 | P1 |
| W1-EC-04 | POS 读取迁移（阶段二-D） | POS 菜单/订单查询改读 orders | W1-EC-01/02 完成 | P1 |
| W1-EC-05 | 财务链路对齐（阶段二-E） | POS finance_record 订单号迁移 | W1-EC-01/02 完成 | P1 |
| W1-EC-06 | legacy 冻结（阶段二-F） | orders_legacy 停止新写入 | W1-EC-01~05 完成 | P2 |

### 4.3 Wave 1 解锁关系

```
PD-015/017~021 裁决完成（本记录）
  → W1-EC-01/02/03（写入迁移，可并行）
    → W1-EC-04/05（读取/财务迁移，依赖写入完成）
      → W1-EC-06（冻结，依赖全部迁移完成）
```

### 4.4 Wave 1 不做的事

| 不做 | 说明 |
|---|---|
| ❌ 不改 orders 表 schema | 产品负责人禁止 |
| ❌ 不新增订单状态值 | 沿用 0-6 + payment 0-3 |
| ❌ 不删除 orders_legacy | 冻结不删除 |
| ❌ 不设计新的订单状态机 | 双字段分离已裁决 |
| ❌ 不处理 POS 可售口径（G1） | 属 Wave 3（API 整改） |
| ❌ 不处理门店经营范围（PADR-002 演进） | 属演进，不进入 Wave 1 |

---

## 5. 待产品负责人确认

本裁决记录已基于 Fact Map + 四域审查 + PADR 已有决策构建，等待产品负责人确认：

1. **PD-015 阶段二执行策略**（§1.4 六阶段 A~F）是否认可？
2. **订单状态双字段分离**（不合并）是否认可？
3. **order_status 0-6 / payment_status 0-3 沿用现有语义**是否认可？
4. **legacy status 不做映射**（冻结后废弃）是否认可？
5. **Wave 1 Engineering Card 规划**（6 卡）是否认可？

---

*本 W1-PDR 为 Wave 1 Product Decision Record，等待产品负责人确认。不修改代码/表/UI。*
*文档生成：架构总控（会话1）· 2026-09-09 · 基于 Fact Map + ORD 审查 + CDDR + PD-015*
