# Remediation Gate Report — SR-* 状态重标

> **报告编号**: REMEDIATION-GATE-001
> **生成日期**: 2026-09-09
> **版本**: 1.0.0
> **基于**: decision-recon/*, systemic-remediation-candidates.md
> **范围**: 17 个 Systemic Remediation Candidates 重新标记

---

## 一、决策冲突检查结果

### 1.1 冲突检查摘要

| 冲突项 | 检查结论 | 影响的 SR |
|--------|---------|-----------|
| **DEC-008 被驳回** | LOCK-003 已锁定「金额以分为准」，DEC-008 的 A_固定精度方案已被 LOCK-003 覆盖 | SR-003 |
| **DEC-006 需修订** | Product-Food-Material 关系需细化边界，等待修订后的 DEC-006 | SR-001 |
| **DEC-009 需细化** | Data Ownership 已确认 B_业务域所有，但跨域访问模式需 DEC-010 定义 | SR-012 |

### 1.2 关键决策状态映射

| Decision | 状态 | 锁定/裁决项 | 影响范围 |
|----------|------|------------|---------|
| **LOCK-001** | 🔒 LOCKED | foods 为菜品唯一真相源 | SR-001, SR-002 |
| **LOCK-003** | 🔒 LOCKED | 金额以分为准 (整数存储) | SR-003, SR-004, SR-006 |
| **LOCK-005** | 🔒 LOCKED | material_archives 为物料真相源 | SR-001 |
| **DEC-006** | ⚠️ CONFIRMED (需修订) | B_继承关系 (Material→Food→Product) | SR-001 |
| **DEC-007** | ✅ CONFIRMED | B_完整状态机 | SR-002 |
| **DEC-008** | ⚠️ REJECTED | A_固定精度 (已被 LOCK-003 覆盖) | SR-003 |
| **DEC-009** | ✅ CONFIRMED | B_业务域所有 | SR-012 |
| **DEC-002** | ✅ SOLUTION_CLEAR | A_enum (保持枚举) | SR-008 |
| **DEC-003** | ✅ SOLUTION_CLEAR | A_enum (保持枚举) | SR-009 |
| **DEC-005** | ✅ SOLUTION_CLEAR | A_inline (保持内联) | SR-011 |
| **DEC-012** | ✅ SOLUTION_CLEAR | 渐进式清理 | SR-015~017 |
| **DEC-001** | ⏳ PENDING_PRODUCT | E_hybrid (推荐) | SR-007 |
| **DEC-004** | ⏳ PENDING_PRODUCT | E_guest_member (推荐) | SR-010 |
| **DEC-010** | ⏳ PENDING_ARCHITECTURE | B_API访问 (推荐) | SR-012, SR-013 |
| **DEC-011** | ⏳ PENDING_ARCHITECTURE | B_异步事件 (推荐) | SR-013 |

---

## 二、SR-* 状态重标 (逐项检查)

### 2.1 状态定义

| 状态 | 定义 | 可执行性 |
|------|------|---------|
| **ENGINEERING_READY** | 已有明确方案，无上游依赖，可直接工程化 | ✅ 立即可执行 |
| **WAITING_PRODUCT_DECISION** | 等待产品决策确认 | ❌ 需 PO 确认 |
| **WAITING_ARCHITECTURE_DECISION** | 等待架构决策确认 | ❌ 需架构师确认 |
| **WAITING_DATA_MODEL_DECISION** | 等待数据模型决策确认 | ❌ 需数据模型确认 |
| **BLOCKED** | 被其他 SR 或 Decision 阻断 | ❌ 需等待前置完成 |
| **MERGED** | 已合并到其他候选 | ⏹️ 不单独执行 |
| **LEGACY** | 已废弃，无需处理 | ⏹️ 不处理 |

---

### 2.2 逐项重标结果

---

#### SR-001: 统一 Product/Material/Food Identity

```yaml
candidate_id: SR-001
current_status: CANDIDATE
new_status: WAITING_DATA_MODEL_DECISION
priority: P0

blocked_by:
  - DEC-006 (需修订): Product-Food-Material 关系边界需细化
  - IMPLICIT-001 (PENDING): Product/Food/Material 边界决策

evidence:
  - DEC-006 已裁决 B_继承关系，但需细化 Food 与 Product 的具体边界
  - LOCK-001 已锁定 foods 为菜品唯一真相源
  - LOCK-005 已锁定 material_archives 为物料真相源
  - IMPLICIT-001 待产品决策 Product/Food/Material 边界

resolution_path: >
  1. 等待 DEC-006 修订版确认 Food→Product 继承边界
  2. 等待 IMPLICIT-001 产品决策明确三者边界
  3. 两者完成后可进入 ENGINEERING_READY

dependencies_resolved:
  - LOCK-001: ✅ foods 为菜品真相源
  - LOCK-005: ✅ material_archives 为物料真相源
  - DEC-006: ⚠️ 需修订
  - IMPLICIT-001: ⏳ PENDING
```

---

#### SR-002: 统一订单状态机

```yaml
candidate_id: SR-002
current_status: CANDIDATE
new_status: BLOCKED
priority: P1

blocked_by:
  - SR-001 (商品 Identity 统一): 订单依赖商品身份
  - SR-012 (跨域数据所有权): 订单多端写入需先明确所有权

evidence:
  - DEC-007 已裁决 B_完整状态机方案
  - 但 SR-001 未完成 (WAITING_DATA_MODEL_DECISION)
  - 但 SR-012 未完成 (WAITING_ARCHITECTURE_DECISION)

resolution_path: >
  1. 等待 SR-001 完成 (商品 Identity 统一)
  2. 等待 SR-012 完成 (跨域数据所有权明确)
  3. 两者完成后可进入 ENGINEERING_READY

dependencies_resolved:
  - DEC-007: ✅ 完整状态机方案已确认
  - SR-001: ❌ 未完成
  - SR-012: ❌ 未完成
```

---

#### SR-003: 统一金额单位

```yaml
candidate_id: SR-003
current_status: CANDIDATE
new_status: ENGINEERING_READY
priority: P0

resolution_basis: >
  DEC-008 被驳回，但 LOCK-003 已锁定「金额以分为准」。
  金额单位标准已确定，无需等待 DEC-008。
  新表统一用「分」存储金额，修正遗留表注释和转换逻辑。

evidence:
  - LOCK-003: ✅ 金额以分为准 (整数存储)
  - DEC-008: ⚠️ 被驳回 (A_固定精度被 LOCK-003 覆盖)

engineering_tasks:
  - ER-003: 金额单位统一为「分」
    - 新表统一用分存储
    - 修正 finance_records 注释
    - tax_record/account_balance 迁移或转换

unlocks:
  - SR-004 (凭证状态映射)
  - SR-006 (成本双写)
  - SR-014 (事件链断裂)
```

---

#### SR-004: 解决凭证状态映射

```yaml
candidate_id: SR-004
current_status: CANDIDATE
new_status: ENGINEERING_READY
priority: P2

resolution_basis: >
  SR-003 已标记为 ENGINEERING_READY (LOCK-003 已锁定)。
  凭证状态映射方案已明确 (adapter 层处理)，可直接工程化。

evidence:
  - SR-003: ✅ ENGINEERING_READY
  - DEC-008: 凭证状态映射逻辑已有 adapter 层处理

engineering_tasks:
  - 统一前后端状态码为 0-3
  - 确认 adapter 层映射逻辑封装位置
  - 更新开发者文档
```

---

#### SR-005: 解决 inventory/store_inventory 双写

```yaml
candidate_id: SR-005
current_status: CANDIDATE
new_status: BLOCKED
priority: P1

blocked_by:
  - SR-001 (商品 Identity 统一): 库存依赖商品身份
  - SR-007 (Warehouse Foundation): 库存依赖仓库基础

evidence:
  - SR-001: ❌ 未完成 (WAITING_DATA_MODEL_DECISION)
  - SR-007: ❌ 未完成 (BLOCKED by DEC-001)

resolution_path: >
  1. 等待 SR-001 完成 (商品 Identity 统一)
  2. 等待 SR-007 完成 (Warehouse Foundation 建立)
  3. 两者完成后可进入 ENGINEERING_READY
```

---

#### SR-006: 解决成本双写

```yaml
candidate_id: SR-006
current_status: CANDIDATE
new_status: BLOCKED
priority: P1

blocked_by:
  - SR-003 (金额单位统一): 成本依赖金额单位
  - SR-001 (商品 Identity 统一): 成本依赖商品身份

evidence:
  - SR-003: ✅ ENGINEERING_READY (但未执行)
  - SR-001: ❌ 未完成

resolution_path: >
  1. 等待 SR-003 执行完成 (金额单位统一)
  2. 等待 SR-001 完成 (商品 Identity 统一)
  3. 两者完成后可进入 ENGINEERING_READY
```

---

#### SR-007: 建立 Warehouse Foundation

```yaml
candidate_id: SR-007
current_status: CANDIDATE
new_status: WAITING_PRODUCT_DECISION
priority: P2

blocked_by:
  - DEC-001 (PENDING_PRODUCT): Warehouse 管理模式待产品决策

evidence:
  - DEC-001 推荐方案: E_hybrid (混合模式)
  - DEC-001 Deadline: 2026-09-16

resolution_path: >
  1. 等待 Product Owner 确认 DEC-001 (Warehouse 模式)
  2. 确认后可进入 ENGINEERING_READY

unlocks:
  - SR-001 (商品 Identity)
  - SR-005 (inventory/store_inventory 双写)
```

---

#### SR-008: 建立 Unit Foundation

```yaml
candidate_id: SR-008
current_status: CANDIDATE
new_status: ENGINEERING_READY
priority: P2

resolution_basis: >
  DEC-002 已确认 A_enum 方案：保持枚举，无需建表。
  可直接工程化：文档化单位规范。

evidence:
  - DEC-002: ✅ SOLUTION_CLEAR (A_enum)

engineering_tasks:
  - ER-006: Unit 枚举文档化
    - 文档化单位规范
    - 无需建表
```

---

#### SR-009: 建立 PaymentMethod Foundation

```yaml
candidate_id: SR-009
current_status: CANDIDATE
new_status: ENGINEERING_READY
priority: P2

resolution_basis: >
  DEC-003 已确认 A_enum 方案：保持枚举，无需建表。
  可直接工程化：文档化支付方式。

evidence:
  - DEC-003: ✅ SOLUTION_CLEAR (A_enum)

engineering_tasks:
  - ER-007: PaymentMethod 枚举文档化
    - 文档化支付方式
    - 无需建表
```

---

#### SR-010: 建立 Customer Foundation

```yaml
candidate_id: SR-010
current_status: CANDIDATE
new_status: WAITING_PRODUCT_DECISION
priority: P1

blocked_by:
  - DEC-004 (PENDING_PRODUCT): Customer/Member 关系待产品决策

evidence:
  - DEC-004 推荐方案: E_guest_member (散客作为 GUEST 类型)
  - DEC-004 Deadline: 2026-09-23
  - 依赖 DEC-013 (JWT 认证) 解锁

resolution_path: >
  1. 等待 DEC-013 (JWT 认证) 完成
  2. 等待 Product Owner 确认 DEC-004 (Customer/Member 关系)
  3. 确认后可进入 ENGINEERING_READY

unlocks:
  - SR-003 (金额单位 — 财务依赖客户基础)
  - SR-002 (订单状态 — 订单依赖客户基础)
```

---

#### SR-011: 建立 Price Foundation

```yaml
candidate_id: SR-011
current_status: CANDIDATE
new_status: ENGINEERING_READY
priority: P3

resolution_basis: >
  DEC-005 已确认 A_inline 方案：保持内联，无需建表。
  可直接工程化：验证价格快照机制。

evidence:
  - DEC-005: ✅ SOLUTION_CLEAR (A_inline)

engineering_tasks:
  - ER-008: Price 内联模式验证
    - 验证价格快照机制
    - 无需建表
```

---

#### SR-012: 明确跨域数据所有权

```yaml
candidate_id: SR-012
current_status: CANDIDATE
new_status: WAITING_ARCHITECTURE_DECISION
priority: P0

blocked_by:
  - DEC-010 (PENDING_ARCHITECTURE): 跨域访问模式待架构决策
  - DEC-009 需细化: 虽已确认 B_业务域所有，但跨域访问规范需 DEC-010 定义

evidence:
  - DEC-009: ✅ CONFIRMED (B_业务域所有)
  - DEC-010: ⏳ PENDING_ARCHITECTURE (推荐 B_API访问)
  - DEC-010 Deadline: 2026-09-16

resolution_path: >
  1. 等待 Architecture Owner 完成 DEC-010 (跨域访问模式)
  2. 确认后可进入 ENGINEERING_READY

unlocks:
  - SR-002 (订单状态机)
  - SR-005 (inventory/store_inventory 双写)
  - SR-014 (多端写入冲突)
```

---

#### SR-013: 加强事件驱动可靠性

```yaml
candidate_id: SR-013
current_status: CANDIDATE
new_status: BLOCKED
priority: P1

blocked_by:
  - SR-012 (跨域数据所有权): 事件依赖所有权明确
  - SR-002 (订单状态机): 事件监听依赖订单状态
  - SR-003 (金额单位): 凭证生成依赖金额单位
  - DEC-011 (PENDING_ARCHITECTURE): 事件架构待决策

evidence:
  - DEC-011: ⏳ PENDING_ARCHITECTURE (推荐 B_异步事件)
  - SR-012: ❌ 未完成
  - SR-002: ❌ 未完成
  - SR-003: ✅ ENGINEERING_READY (但未执行)

resolution_path: >
  1. 等待 DEC-010 完成 (解锁 DEC-011)
  2. 等待 DEC-011 完成 (事件架构确认)
  3. 等待 SR-012 完成 (跨域数据所有权)
  4. 等待 SR-002 完成 (订单状态机)
  5. 等待 SR-003 执行 (金额单位统一)
  6. 全部完成后可进入 ENGINEERING_READY
```

---

#### SR-014: 解决多端写入冲突

```yaml
candidate_id: SR-014
current_status: CANDIDATE
new_status: BLOCKED
priority: P0

blocked_by:
  - SR-012 (跨域数据所有权): 多端写入依赖所有权明确
  - SR-002 (订单状态机): 多端写入依赖状态机统一

evidence:
  - SR-012: ❌ 未完成 (WAITING_ARCHITECTURE_DECISION)
  - SR-002: ❌ 未完成 (BLOCKED)

resolution_path: >
  1. 等待 SR-012 完成 (跨域数据所有权)
  2. 等待 SR-002 完成 (订单状态机)
  3. 两者完成后可进入 ENGINEERING_READY
```

---

#### SR-015: 清理 Legacy 数据库表

```yaml
candidate_id: SR-015
current_status: CANDIDATE
new_status: BLOCKED
priority: P2

blocked_by:
  - SR-001 (商品 Identity 统一): 遗留表清理依赖商品身份统一
  - SR-002 (订单状态机): 遗留表清理依赖订单状态统一
  - SR-003 (金额单位): 遗留表清理依赖金额单位统一

evidence:
  - DEC-012: ✅ SOLUTION_CLEAR (渐进式清理)
  - SR-001: ❌ 未完成
  - SR-002: ❌ 未完成
  - SR-003: ✅ ENGINEERING_READY (但未执行)

resolution_path: >
  1. 等待 SR-001 完成 (商品 Identity 统一)
  2. 等待 SR-002 完成 (订单状态机统一)
  3. 等待 SR-003 执行 (金额单位统一)
  4. 全部完成后可进入 ENGINEERING_READY
```

---

#### SR-016: 清理 Legacy 服务

```yaml
candidate_id: SR-016
current_status: CANDIDATE
new_status: BLOCKED
priority: P2

blocked_by:
  - SR-015 (遗留表清理): 服务清理依赖表清理

evidence:
  - DEC-012: ✅ SOLUTION_CLEAR (渐进式清理)
  - SR-015: ❌ 未完成 (BLOCKED)

resolution_path: >
  1. 等待 SR-015 完成 (遗留表清理)
  2. 完成后可进入 ENGINEERING_READY
```

---

#### SR-017: 清理 Dead Code

```yaml
candidate_id: SR-017
current_status: CANDIDATE
new_status: BLOCKED
priority: P2

blocked_by:
  - SR-016 (Legacy 服务清理): Dead Code 清理依赖服务清理

evidence:
  - DEC-012: ✅ SOLUTION_CLEAR (渐进式清理)
  - SR-016: ❌ 未完成 (BLOCKED)

resolution_path: >
  1. 等待 SR-016 完成 (Legacy 服务清理)
  2. 完成后可进入 ENGINEERING_READY
```

---

## 三、Engineering Readiness Matrix

### 3.1 可立即工程化 (ENGINEERING_READY)

| 编号 | 名称 | 优先级 | 决策来源 | 方案 | 工程任务 |
|------|------|--------|---------|------|---------|
| **SR-003** | 统一金额单位 | P0 | LOCK-003 | 金额以分为准 | ER-003 |
| **SR-004** | 解决凭证状态映射 | P2 | DEC-008 | adapter 层映射 | 统一状态码 + 文档 |
| **SR-008** | 建立 Unit Foundation | P2 | DEC-002 (A_enum) | 保持枚举 | ER-006 |
| **SR-009** | 建立 PaymentMethod Foundation | P2 | DEC-003 (A_enum) | 保持枚举 | ER-007 |
| **SR-011** | 建立 Price Foundation | P3 | DEC-005 (A_inline) | 保持内联 | ER-008 |

**总计**: 5 项可立即工程化

### 3.2 等待产品决策 (WAITING_PRODUCT_DECISION)

| 编号 | 名称 | 优先级 | 阻塞 Decision | 推荐方案 | Deadline |
|------|------|--------|--------------|---------|----------|
| **SR-007** | 建立 Warehouse Foundation | P2 | DEC-001 | E_hybrid | 2026-09-16 |
| **SR-010** | 建立 Customer Foundation | P1 | DEC-004 | E_guest_member | 2026-09-23 |

**总计**: 2 项等待产品决策

### 3.3 等待架构决策 (WAITING_ARCHITECTURE_DECISION)

| 编号 | 名称 | 优先级 | 阻塞 Decision | 推荐方案 | Deadline |
|------|------|--------|--------------|---------|----------|
| **SR-012** | 明确跨域数据所有权 | P0 | DEC-010 | B_API访问 | 2026-09-16 |

**总计**: 1 项等待架构决策

### 3.4 等待数据模型决策 (WAITING_DATA_MODEL_DECISION)

| 编号 | 名称 | 优先级 | 阻塞 Decision | 说明 |
|------|------|--------|--------------|------|
| **SR-001** | 统一 Product/Material/Food Identity | P0 | DEC-006 (需修订), IMPLICIT-001 | 边界需细化 |

**总计**: 1 项等待数据模型决策

### 3.5 被阻断 (BLOCKED)

| 编号 | 名称 | 优先级 | 阻塞依赖 |
|------|------|--------|---------|
| **SR-002** | 统一订单状态机 | P1 | SR-001, SR-012 |
| **SR-005** | 解决 inventory/store_inventory 双写 | P1 | SR-001, SR-007 |
| **SR-006** | 解决成本双写 | P1 | SR-003 (待执行), SR-001 |
| **SR-013** | 加强事件驱动可靠性 | P1 | SR-012, SR-002, SR-003, DEC-011 |
| **SR-014** | 解决多端写入冲突 | P0 | SR-012, SR-002 |
| **SR-015** | 清理 Legacy 数据库表 | P2 | SR-001, SR-002, SR-003 |
| **SR-016** | 清理 Legacy 服务 | P2 | SR-015 |
| **SR-017** | 清理 Dead Code | P2 | SR-016 |

**总计**: 8 项被阻断

---

## 四、阻塞依赖链可视化

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          SR-001~017 阻塞依赖链                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ENGINEERING_READY (可立即执行):                                                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐              │
│  │ SR-003   │ │ SR-004   │ │ SR-008   │ │ SR-009   │ │ SR-011   │              │
│  │ 金额单位 │ │ 凭证状态 │ │  Unit    │ │Payment   │ │  Price   │              │
│  │ ✅ READY │ │ ✅ READY │ │ ✅ READY │ │ ✅ READY │ │ ✅ READY │              │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘              │
│                                                                                 │
│  WAITING (等待决策):                                                            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                                       │
│  │ SR-007   │ │ SR-010   │ │ SR-012   │                                       │
│  │Warehouse │ │ Customer │ │ 数据所有 │                                       │
│  │ ⏳ DEC-001│ │ ⏳ DEC-004│ │ ⏳ DEC-010│                                       │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘                                       │
│       │            │            │                                               │
│       └────────────┼────────────┘                                               │
│                    │                                                            │
│  BLOCKED (被阻断):                                                              │
│  ┌────────────────▼────────────────┐                                           │
│  │           SR-001                │                                           │
│  │    商品 Identity 统一           │                                           │
│  │    ⏳ DEC-006 + IMPLICIT-001    │                                           │
│  └────────────────┬────────────────┘                                           │
│                   │                                                             │
│       ┌───────────┼───────────┬───────────┐                                    │
│       ▼           ▼           ▼           ▼                                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                          │
│  │ SR-002   │ │ SR-005   │ │ SR-006   │ │ SR-014   │                          │
│  │ 订单状态 │ │inv/store │ │ 成本双写 │ │ 多端写入 │                          │
│  │ ❌ BLOCKED│ │ ❌ BLOCKED│ │ ❌ BLOCKED│ │ ❌ BLOCKED│                          │
│  └────┬─────┘ └──────────┘ └──────────┘ └──────────┘                          │
│       │                                                                        │
│       └────────────────┬──────────────────────────────────┐                    │
│                        ▼                                  ▼                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                         │
│  │ SR-013   │ │ SR-015   │ │ SR-016   │ │ SR-017   │                         │
│  │ 事件可靠 │ │ 遗留表   │ │ 遗留服务 │ │ Dead Code│                         │
│  │ ❌ BLOCKED│ │ ❌ BLOCKED│ │ ❌ BLOCKED│ │ ❌ BLOCKED│                         │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘                         │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 五、执行路径建议

### Phase 0: 立即执行 (本周)

| 序号 | SR | 任务 | 预计工时 |
|------|-----|------|---------|
| 1 | SR-003 | 金额单位统一为「分」 | 3-5 天 |
| 2 | SR-008 | Unit 枚举文档化 | 1 天 |
| 3 | SR-009 | PaymentMethod 枚举文档化 | 1 天 |
| 4 | SR-011 | Price 内联模式验证 | 2 天 |
| 5 | SR-004 | 凭证状态映射统一 | 2-3 天 |

**Phase 0 总工时**: 9-12 人天

### Phase 1: 等待决策完成后执行

| 序号 | SR | 前置条件 | 预计启动时间 |
|------|-----|---------|-------------|
| 1 | SR-007 | DEC-001 确认 (Warehouse) | 2026-09-16 后 |
| 2 | SR-010 | DEC-004 确认 (Customer/Member) | 2026-09-23 后 |
| 3 | SR-012 | DEC-010 确认 (跨域访问) | 2026-09-16 后 |

### Phase 2: 依赖链解锁后执行

| 序号 | SR | 前置条件 | 预计启动时间 |
|------|-----|---------|-------------|
| 1 | SR-001 | DEC-006 修订 + IMPLICIT-001 | DEC-006 修订后 |
| 2 | SR-002 | SR-001 + SR-012 | Phase 1 完成后 |
| 3 | SR-005 | SR-001 + SR-007 | Phase 1 完成后 |
| 4 | SR-006 | SR-003 + SR-001 | Phase 0 + SR-001 |
| 5 | SR-014 | SR-012 + SR-002 | Phase 1 完成后 |

### Phase 3: 全链路解锁后执行

| 序号 | SR | 前置条件 | 预计启动时间 |
|------|-----|---------|-------------|
| 1 | SR-013 | DEC-011 + SR-012 + SR-002 + SR-003 | Phase 2 完成后 |
| 2 | SR-015 | SR-001 + SR-002 + SR-003 | Phase 2 完成后 |
| 3 | SR-016 | SR-015 | SR-015 完成后 |
| 4 | SR-017 | SR-016 | SR-016 完成后 |

---

## 六、关键路径分析

### 6.1 最长依赖链

```
LOCK-003 → SR-003 → SR-006
    │          │          │
    │ 金额锁定 │ 金额统一 │ 成本双写
    │          │          │
    ▼          ▼          ▼
  DEC-010 → SR-012 → SR-014 → SR-013
    │          │          │          │
  跨域决策  数据所有权  多端写入   事件可靠
```

**关键路径长度**: 4 层
**关键路径影响域**: Finance → All → All → All

### 6.2 辅助关键路径

```
DEC-006 (修订) → SR-001 → SR-002 → SR-015 → SR-016 → SR-017
    │              │          │          │          │          │
  PFM边界     商品统一   订单状态   遗留表    遗留服务   Dead Code
```

**辅助路径长度**: 5 层

---

## 七、风险提示

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| DEC-001 延迟 | SR-007/SR-005/SR-015 全链路阻塞 | 设置 Deadline 2026-09-16 |
| DEC-010 延迟 | DEC-011/SR-012/SR-014 全链路阻塞 | 设置 Deadline 2026-09-16 |
| DEC-006 修订延迟 | SR-001/SR-002/SR-005 全链路阻塞 | 设置 Deadline 2026-09-23 |
| 金额单位双轨 | 财务报表偏差 100 倍 | SR-003 优先执行 |
| food/foods 双写 | 数据不一致 | SR-001 优先推进 |

---

## 八、状态变更汇总

| SR | 原状态 | 新状态 | 变更原因 |
|----|--------|--------|---------|
| SR-001 | CANDIDATE | WAITING_DATA_MODEL_DECISION | DEC-006 需修订 + IMPLICIT-001 PENDING |
| SR-002 | CANDIDATE | BLOCKED | 依赖 SR-001, SR-012 |
| SR-003 | CANDIDATE | **ENGINEERING_READY** | LOCK-003 已锁定，DEC-008 被驳回 |
| SR-004 | CANDIDATE | **ENGINEERING_READY** | SR-003 已 READY，方案已明确 |
| SR-005 | CANDIDATE | BLOCKED | 依赖 SR-001, SR-007 |
| SR-006 | CANDIDATE | BLOCKED | 依赖 SR-003 (待执行), SR-001 |
| SR-007 | CANDIDATE | WAITING_PRODUCT_DECISION | DEC-001 PENDING_PRODUCT |
| SR-008 | CANDIDATE | **ENGINEERING_READY** | DEC-002 A_enum 已明确 |
| SR-009 | CANDIDATE | **ENGINEERING_READY** | DEC-003 A_enum 已明确 |
| SR-010 | CANDIDATE | WAITING_PRODUCT_DECISION | DEC-004 PENDING_PRODUCT |
| SR-011 | CANDIDATE | **ENGINEERING_READY** | DEC-005 A_inline 已明确 |
| SR-012 | CANDIDATE | WAITING_ARCHITECTURE_DECISION | DEC-010 PENDING_ARCHITECTURE |
| SR-013 | CANDIDATE | BLOCKED | 依赖 SR-012, SR-002, SR-003, DEC-011 |
| SR-014 | CANDIDATE | BLOCKED | 依赖 SR-012, SR-002 |
| SR-015 | CANDIDATE | BLOCKED | 依赖 SR-001, SR-002, SR-003 |
| SR-016 | CANDIDATE | BLOCKED | 依赖 SR-015 |
| SR-017 | CANDIDATE | BLOCKED | 依赖 SR-016 |

---

## 九、决策依赖关系 (完整 DAG)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          决策与 SR 依赖关系 DAG                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  LAYER 1: LOCKED (不可变锚点)                                                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐              │
│  │ LOCK-001 │ │ LOCK-003 │ │ LOCK-005 │ │ LOCK-006 │ │ LOCK-010 │              │
│  │foods真相 │ │金额以分准 │ │material  │ │stores真相 │ │权限真相   │              │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘              │
│       │            │            │            │            │                      │
│       └────────────┼────────────┼────────────┼────────────┘                      │
│                    │            │            │                                    │
│  LAYER 2: CONFIRMED (已裁决)                                                    │
│  ┌────────────────▼────────────▼────────────▼────────────┐                      │
│  │ DEC-006 (PFM)  DEC-007 (Order)  DEC-008 (Amount)     │                      │
│  │ DEC-009 (DataOwn)                                    │                      │
│  └───────────────────────┬────────────────────────────────┘                      │
│                          │                                                        │
│  LAYER 3: SOLUTION_CLEAR (方案明确)                                              │
│  ┌───────────┬───────────┼───────────┬───────────┐                                │
│  │ DEC-002   │ │ DEC-003   │ │ DEC-005   │ │ DEC-012   │                        │
│  │  Unit     │ │ Payment   │ │  Price    │ │  Legacy   │                        │
│  └───────────┘ └───────────┘ └───────────┘ └───────────┘                        │
│                                                                                 │
│  LAYER 4: PENDING (待决策)                                                      │
│  ┌───────────┬───────────┬───────────┬───────────┐                                │
│  │ DEC-001   │ │ DEC-004   │ │ DEC-010   │ │ DEC-011   │                        │
│  │Warehouse  │ │Customer   │ │跨域访问   │ │事件架构   │                        │
│  │ ⏳ PRODUCT│ │ ⏳ PRODUCT│ │ ⏳ ARCH   │ │ ⏳ ARCH   │                        │
│  └───────────┘ └───────────┘ └───────────┘ └───────────┘                        │
│                                                                                 │
│  LAYER 5: SR (治理候选)                                                         │
│  ┌──────────────────────────────────────────────────────────────────────┐        │
│  │ ENGINEERING_READY: SR-003, SR-004, SR-008, SR-009, SR-011           │        │
│  │ WAITING: SR-007 (DEC-001), SR-010 (DEC-004), SR-012 (DEC-010)     │        │
│  │ WAITING: SR-001 (DEC-006+IMPLICIT-001)                              │        │
│  │ BLOCKED: SR-002, SR-005, SR-006, SR-013~017                        │        │
│  └──────────────────────────────────────────────────────────────────────┘        │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 十、行动建议

### 10.1 立即行动 (本周)

1. **工程团队**: 执行 SR-003, SR-004, SR-008, SR-009, SR-011
2. **Product Owner**: 启动 DEC-001 (Warehouse) 确认，Deadline 2026-09-16
3. **Architecture Owner**: 启动 DEC-010 (跨域访问) 确认，Deadline 2026-09-16

### 10.2 中期行动 (下周)

1. **Product Owner**: 确认 DEC-004 (Customer/Member)，Deadline 2026-09-23
2. **数据架构师**: 修订 DEC-006 (PFM 边界)，Deadline 2026-09-23
3. **工程团队**: 执行 SR-007, SR-010, SR-012 (决策完成后)

### 10.3 长期行动 (Sprint 3+)

1. **Architecture Owner**: 完成 DEC-011 (事件架构)
2. **工程团队**: 执行 SR-001, SR-002, SR-005, SR-006 (依赖链解锁后)
3. **工程团队**: 执行 SR-013~017 (全链路解锁后)

---

*报告生成时间: 2026-09-09*
*报告编号: REMEDIATION-GATE-001*
*维护方: opencode*
