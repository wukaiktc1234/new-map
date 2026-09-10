# PROJECT-DECISION-RECON-001 — Decision-Requirement Mapping Matrix

> **版本**: 1.0  
> **生成日期**: 2026-09-09  
> **评审编号**: PROJECT-MASTER-REVIEW-001  
> **范围**: 食品溯源系统全域决策 → 需求映射  
> **数据来源**: architecture-product-decision-register.yaml + business-requirements-baseline.md + architecture-principle-baseline.md + systemic-problem-register.md + systemic-remediation-candidates.md

---

## 一、映射总览

| 统计项 | 数量 |
|--------|------|
| Decision 总数 | 22 |
| ALREADY_DECIDED | 4 |
| PRODUCT_DECISION | 2 |
| ARCHITECTURE_DECISION | 2 |
| SECURITY_DECISION | 2 |
| ENGINEERING_EXECUTION | 4 |
| IMPLICIT_DECISION | 6 |

| 覆盖维度 | 数量 |
|----------|------|
| Requirements | 17 |
| Principles | 16 |
| Root Causes | 13 |
| Remediation Candidates | 17 |
| Affected Domains | 8 |

---

## 二、映射矩阵

### 2.1 ALREADY_DECIDED (已裁决，4个)

| Decision | Requirements | Principles | Root Causes | Remediation Candidates | Affected Domains |
|----------|-------------|------------|-------------|----------------------|------------------|
| **DEC-006** food/foods 如何统一 | REQ-MASTER-002, REQ-DATA-001 | ARCH-TRUTH-001, ARCH-LEGACY-002 | RC-001 | SR-001 | Product, Order, POS |
| **DEC-007** 金额单位如何统一（分 or 元） | REQ-DATA-004, REQ-CORE-002 | ARCH-TRUTH-001, ARCH-CROSS-002 | RC-004 | SR-003 | Finance, Order, Procurement, Inventory |
| **DEC-008** 订单状态机如何统一 | REQ-DATA-005, REQ-CROSS-002 | ARCH-CROSS-002, ARCH-OWN-001 | RC-005 | SR-002 | Order, POS, Finance |
| **DEC-009** stores/stores_new 如何统一 | REQ-MASTER-005, REQ-DATA-001 | ARCH-TRUTH-001, ARCH-LEGACY-002 | RC-003 | SR-001 | Management Core, POS, Employee, Inventory |

---

### 2.2 PRODUCT_DECISION (产品决策，2个)

| Decision | Requirements | Principles | Root Causes | Remediation Candidates | Affected Domains |
|----------|-------------|------------|-------------|----------------------|------------------|
| **DEC-001** Warehouse 是否需要独立管理 | REQ-MASTER-003, REQ-DATA-001 | ARCH-MASTER-001, ARCH-ID-001 | RC-007 | SR-007 | Inventory, Procurement, Management Core |
| **DEC-004** Customer 是否需要独立于 Member | REQ-MASTER-004, REQ-CORE-001 | ARCH-ENTRY-002, ARCH-OWN-001 | RC-010 | SR-010 | Order, Finance, Marketing |

---

### 2.3 ARCHITECTURE_DECISION (架构决策，2个)

| Decision | Requirements | Principles | Root Causes | Remediation Candidates | Affected Domains |
|----------|-------------|------------|-------------|----------------------|------------------|
| **DEC-010** 跨域数据所有权如何划分 | REQ-CROSS-001, REQ-CORE-001, REQ-CORE-002 | ARCH-OWN-001, ARCH-CROSS-001, ARCH-WRITE-002 | RC-012, RC-013 | SR-012, SR-014 | Order, Inventory, POS, Mini Program |
| **DEC-011** 事件驱动架构是否需要加强 | REQ-CROSS-002, REQ-SEC-001 | ARCH-CROSS-002, ARCH-WRITE-002 | RC-013, RC-014 | SR-013 | Order, Inventory, Finance, Procurement |

---

### 2.4 SECURITY_DECISION (安全决策，2个)

| Decision | Requirements | Principles | Root Causes | Remediation Candidates | Affected Domains |
|----------|-------------|------------|-------------|----------------------|------------------|
| **DEC-013** 裸接口权限如何归属 | REQ-SEC-001 | ARCH-ENTRY-001, ARCH-WRITE-001 | RC-012 | SR-012 | System, Procurement, Inventory |
| **DEC-014** 供应商 H5 外部认证如何实现 | REQ-SEC-001, REQ-SEC-002 | ARCH-ENTRY-001, ARCH-MASTER-001 | RC-012 | SR-012 | Procurement, System |

---

### 2.5 ENGINEERING_EXECUTION (工程执行，4个)

| Decision | Requirements | Principles | Root Causes | Remediation Candidates | Affected Domains |
|----------|-------------|------------|-------------|----------------------|------------------|
| **DEC-002** Unit 是否需要统一管理 | REQ-MASTER-003, REQ-DATA-001 | ARCH-MASTER-001, ARCH-ID-001 | RC-008 | SR-008 | Product, Inventory, Procurement |
| **DEC-003** PaymentMethod 是否需要统一管理 | REQ-MASTER-004, REQ-DATA-004 | ARCH-MASTER-001, ARCH-CROSS-002 | RC-009 | SR-009 | Finance, Order, Marketing |
| **DEC-005** Price 是否需要统一管理 | REQ-MASTER-002, REQ-DATA-001 | ARCH-MASTER-001, ARCH-ID-001 | RC-011 | SR-011 | Product, Order, Procurement |
| **DEC-012** Legacy 系统如何隔离 | REQ-CORE-003, REQ-SEC-001 | ARCH-LEGACY-001, ARCH-LEGACY-002, ARCH-WRITE-001 | RC-015, RC-016, RC-017 | SR-015, SR-016, SR-017 | Finance, System, Inventory |

---

### 2.6 IMPLICIT_DECISION (隐式决策，6个)

| Decision | Requirements | Principles | Root Causes | Remediation Candidates | Affected Domains |
|----------|-------------|------------|-------------|----------------------|------------------|
| **Product/Food/Material Boundary** | REQ-MASTER-002, REQ-MASTER-003, REQ-DATA-001 | ARCH-TRUTH-001, ARCH-MASTER-001, ARCH-ID-002 | RC-001, RC-002 | SR-001 | Management Core, POS, Inventory, Finance |
| **Order State Machine** | REQ-DATA-005, REQ-CROSS-002, REQ-CORE-002 | ARCH-CROSS-002, ARCH-OWN-001, ARCH-ENTRY-002 | RC-005, RC-006 | SR-002, SR-004 | Order, POS, Kitchen, Finance |
| **Amount/Money Contract** | REQ-DATA-004, REQ-CORE-002, REQ-CROSS-001 | ARCH-TRUTH-001, ARCH-CROSS-002, ARCH-OWN-001 | RC-004 | SR-003, SR-006 | Finance, Order, Procurement, Inventory |
| **Data Ownership** | REQ-CROSS-001, REQ-CORE-001, REQ-CORE-002 | ARCH-OWN-001, ARCH-OWN-002, ARCH-CROSS-001, ARCH-ENTRY-002 | RC-012, RC-013 | SR-012, SR-014 | Order, Inventory, POS, Mini Program |
| **Cross-Domain Access** | REQ-CROSS-001, REQ-CROSS-002, REQ-SEC-001 | ARCH-CROSS-001, ARCH-CROSS-002, ARCH-WRITE-002, ARCH-ENTRY-001 | RC-012, RC-013, RC-014 | SR-013, SR-014 | Order, Inventory, Finance, Management Core |
| **Event Architecture** | REQ-CROSS-002, REQ-CORE-003, REQ-SEC-001 | ARCH-WRITE-002, ARCH-CROSS-002, ARCH-WRITE-001 | RC-013, RC-014 | SR-013 | Order, Inventory, Finance, Procurement |

---

## 三、反向映射矩阵

### 3.1 Requirements → Decisions 覆盖度

| Requirement | 覆盖 Decisions | 覆盖数 | 是否覆盖充分 |
|-------------|---------------|--------|-------------|
| REQ-MASTER-001 (供应商业务身份 Canonical) | — | 0 | ⚠️ 无直接 Decision |
| REQ-MASTER-002 (商品业务身份 Canonical) | DEC-006, DEC-005, IMPLICIT:Product/Food/Material | 3 | ✅ |
| REQ-MASTER-003 (物业务身份 Canonical) | DEC-001, DEC-002, IMPLICIT:Product/Food/Material | 3 | ✅ |
| REQ-MASTER-004 (员工业务身份 Canonical) | DEC-004 | 1 | ⚠️ 需更多覆盖 |
| REQ-MASTER-005 (门店业务身份 Canonical) | DEC-009 | 1 | ⚠️ 需更多覆盖 |
| REQ-DATA-001 (跨域不得重新生成 Identity) | DEC-006, DEC-001, DEC-002, DEC-005, IMPLICIT:Product/Food/Material | 5 | ✅ |
| REQ-DATA-002 (Dialog/Form 必须继承 Canonical Identity) | — | 0 | ⚠️ 无直接 Decision |
| REQ-DATA-003 (历史交易 Snapshot 必须有明确语义) | — | 0 | ⚠️ 无直接 Decision |
| REQ-DATA-004 (金额单位必须统一) | DEC-007, DEC-003, IMPLICIT:Amount/Money | 3 | ✅ |
| REQ-DATA-005 (状态机必须统一) | DEC-008, IMPLICIT:Order State Machine | 2 | ✅ |
| REQ-CORE-001 (客户端是业务入口，不等于 Truth Owner) | DEC-004, DEC-010, DEC-013, DEC-014, IMPLICIT:Data Ownership | 5 | ✅ |
| REQ-CORE-002 (Truth Source 必须唯一) | DEC-007, DEC-010, IMPLICIT:Amount/Money, IMPLICIT:Data Ownership | 4 | ✅ |
| REQ-CORE-003 (写入操作必须通过 Service 层) | DEC-012 | 1 | ⚠️ 需更多覆盖 |
| REQ-SEC-001 (所有 API 必须有权限保护) | DEC-011, DEC-013, DEC-014, DEC-012, IMPLICIT:Cross-Domain Access, IMPLICIT:Event Architecture | 6 | ✅ |
| REQ-SEC-002 (敏感操作必须有审计日志) | DEC-014 | 1 | ⚠️ 需更多覆盖 |
| REQ-CROSS-001 (跨域数据流必须明确所有权) | DEC-010, DEC-011, IMPLICIT:Data Ownership, IMPLICIT:Cross-Domain Access | 4 | ✅ |
| REQ-CROSS-002 (事件驱动必须保证最终一致性) | DEC-008, DEC-011, IMPLICIT:Order State Machine, IMPLICIT:Amount/Money, IMPLICIT:Cross-Domain Access, IMPLICIT:Event Architecture | 6 | ✅ |

### 3.2 Principles → Decisions 覆盖度

| Principle | 覆盖 Decisions | 覆盖数 | 是否覆盖充分 |
|-----------|---------------|--------|-------------|
| ARCH-TRUTH-001 (Truth Source 唯一性) | DEC-006, DEC-007, DEC-009, DEC-001, DEC-002, DEC-005, IMPLICIT:* | 7+ | ✅ |
| ARCH-TRUTH-002 (Truth Source 由数据模型定义) | — | 0 | ⚠️ 无直接 Decision |
| ARCH-OWN-001 (角色分离) | DEC-004, DEC-008, DEC-010, IMPLICIT:Data Ownership, IMPLICIT:Order State Machine | 5 | ✅ |
| ARCH-OWN-002 (业务入口与真相源分离) | DEC-010, IMPLICIT:Data Ownership | 2 | ✅ |
| ARCH-WRITE-001 (Service 层写入) | DEC-012, DEC-013, DEC-014, IMPLICIT:Event Architecture | 4 | ✅ |
| ARCH-WRITE-002 (事件驱动跨域写入) | DEC-010, DEC-011, IMPLICIT:Cross-Domain Access, IMPLICIT:Event Architecture | 4 | ✅ |
| ARCH-ENTRY-001 (客户端入口定位) | DEC-013, DEC-014, IMPLICIT:Cross-Domain Access | 3 | ✅ |
| ARCH-ENTRY-002 (多端写入所有权) | DEC-004, DEC-010, IMPLICIT:Data Ownership, IMPLICIT:Order State Machine | 4 | ✅ |
| ARCH-MASTER-001 (Master Data Canonical) | DEC-001, DEC-002, DEC-003, DEC-005, DEC-014, IMPLICIT:Product/Food/Material | 6 | ✅ |
| ARCH-MASTER-002 (Reference 可追溯) | DEC-001, DEC-002, DEC-005, IMPLICIT:Product/Food/Material | 4 | ✅ |
| ARCH-ID-001 (Identity 传播) | DEC-001, DEC-002, DEC-005, IMPLICIT:Product/Food/Material | 4 | ✅ |
| ARCH-ID-002 (禁止下游重新生成) | IMPLICIT:Product/Food/Material | 1 | ⚠️ 需更多覆盖 |
| ARCH-CROSS-001 (跨域数据所有权) | DEC-010, IMPLICIT:Data Ownership, IMPLICIT:Cross-Domain Access | 3 | ✅ |
| ARCH-CROSS-002 (事件驱动最终一致性) | DEC-007, DEC-008, DEC-011, DEC-003, IMPLICIT:Amount/Money, IMPLICIT:Order State Machine, IMPLICIT:Event Architecture | 7 | ✅ |
| ARCH-LEGACY-001 (Legacy 系统隔离) | DEC-012 | 1 | ⚠️ 需更多覆盖 |
| ARCH-LEGACY-002 (Legacy 迁移切换点) | DEC-006, DEC-009, DEC-012 | 3 | ✅ |

### 3.3 Root Causes → Decisions 覆盖度

| Root Cause | 覆盖 Decisions | 覆盖数 | 是否覆盖充分 |
|------------|---------------|--------|-------------|
| RC-001 food/foods 双写 | DEC-006, IMPLICIT:Product/Food/Material | 2 | ✅ |
| RC-002 product/material 边界不清 | DEC-001, DEC-002, IMPLICIT:Product/Food/Material | 3 | ✅ |
| RC-003 stores/stores_new 双表 | DEC-009 | 1 | ✅ |
| RC-004 金额单位双轨 | DEC-007, IMPLICIT:Amount/Money | 2 | ✅ |
| RC-005 订单状态三套并存 | DEC-008, IMPLICIT:Order State Machine | 2 | ✅ |
| RC-006 凭证状态前后端映射错位 | IMPLICIT:Order State Machine | 1 | ✅ |
| RC-007 Warehouse Foundation 缺失 | DEC-001 | 1 | ✅ |
| RC-008 Unit Foundation 缺失 | DEC-002 | 1 | ✅ |
| RC-009 PaymentMethod Foundation 缺失 | DEC-003 | 1 | ✅ |
| RC-010 Customer Foundation 缺失 | DEC-004 | 1 | ✅ |
| RC-011 Price Foundation 缺失 | DEC-005 | 1 | ✅ |
| RC-012 多端写入冲突 | DEC-010, DEC-013, DEC-014, IMPLICIT:Data Ownership, IMPLICIT:Cross-Domain Access | 5 | ✅ |
| RC-013 数据所有权不明确 | DEC-010, DEC-011, IMPLICIT:Data Ownership, IMPLICIT:Cross-Domain Access, IMPLICIT:Event Architecture | 5 | ✅ |
| RC-014 事件链断裂 | DEC-011, IMPLICIT:Cross-Domain Access, IMPLICIT:Event Architecture | 3 | ✅ |
| RC-015 遗留表残留 | DEC-012 | 1 | ✅ |
| RC-016 遗留服务残留 | DEC-012 | 1 | ✅ |
| RC-017 Dead Code | DEC-012 | 1 | ✅ |

### 3.4 Remediation Candidates → Decisions 覆盖度

| Remediation Candidate | 覆盖 Decisions | 覆盖数 | 是否覆盖充分 |
|----------------------|---------------|--------|-------------|
| SR-001 统一 Product/Material/Food Identity | DEC-006, DEC-009, IMPLICIT:Product/Food/Material | 3 | ✅ |
| SR-002 统一订单状态机 | DEC-008, IMPLICIT:Order State Machine | 2 | ✅ |
| SR-003 统一金额单位 | DEC-007, IMPLICIT:Amount/Money | 2 | ✅ |
| SR-004 解决凭证状态映射 | IMPLICIT:Order State Machine | 1 | ✅ |
| SR-005 解决 inventory/store_inventory 双写 | — | 0 | ⚠️ 需决策覆盖 |
| SR-006 解决成本双写 | IMPLICIT:Amount/Money | 1 | ✅ |
| SR-007 建立 Warehouse Foundation | DEC-001 | 1 | ✅ |
| SR-008 建立 Unit Foundation | DEC-002 | 1 | ✅ |
| SR-009 建立 PaymentMethod Foundation | DEC-003 | 1 | ✅ |
| SR-010 建立 Customer Foundation | DEC-004 | 1 | ✅ |
| SR-011 建立 Price Foundation | DEC-005 | 1 | ✅ |
| SR-012 明确跨域数据所有权 | DEC-010, DEC-013, DEC-014, IMPLICIT:Data Ownership | 4 | ✅ |
| SR-013 加强事件驱动可靠性 | DEC-011, IMPLICIT:Event Architecture | 2 | ✅ |
| SR-014 解决多端写入冲突 | DEC-010, IMPLICIT:Data Ownership, IMPLICIT:Cross-Domain Access | 3 | ✅ |
| SR-015 清理 Legacy 数据库表 | DEC-012 | 1 | ✅ |
| SR-016 清理 Legacy 服务 | DEC-012 | 1 | ✅ |
| SR-017 清理 Dead Code | DEC-012 | 1 | ✅ |

---

## 四、决策依赖链

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           Decision Dependency Chain                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  Foundation Layer (DEC-001~005)                                                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐              │
│  │ DEC-001  │ │ DEC-002  │ │ DEC-003  │ │ DEC-004  │ │ DEC-005  │              │
│  │Warehouse │ │  Unit    │ │Payment   │ │ Customer │ │  Price   │              │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘              │
│       │            │            │            │            │                      │
│       └────────────┼────────────┼────────────┼────────────┘                      │
│                    │            │            │                                    │
│  Data Model Layer  │            │            │                                    │
│  ┌─────────────────▼────────────▼────────────▼────────────┐                      │
│  │              DEC-006 (food/foods)                       │                      │
│  │              DEC-009 (stores/stores_new)                │                      │
│  └───────────────────────┬────────────────────────────────┘                      │
│                          │                                                        │
│  Cross-Domain Layer      │                                                        │
│  ┌───────────────────────▼────────────────────────────────┐                      │
│  │  DEC-010 (跨域数据所有权)                               │                      │
│  │  DEC-011 (事件驱动加强)                                 │                      │
│  └───────────────────────┬────────────────────────────────┘                      │
│                          │                                                        │
│  Security Layer          │                                                        │
│  ┌───────────────────────▼────────────────────────────────┐                      │
│  │  DEC-013 (裸接口权限)                                   │                      │
│  │  DEC-014 (供应商 H5 认证)                               │                      │
│  └───────────────────────┬────────────────────────────────┘                      │
│                          │                                                        │
│  Engineering Layer       │                                                        │
│  ┌───────────────────────▼────────────────────────────────┐                      │
│  │  DEC-012 (Legacy 隔离)                                  │                      │
│  └────────────────────────────────────────────────────────┘                      │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 五、跨维度交叉分析

### 5.1 按 Affected Domains 聚合

| Domain | 覆盖 Decisions | 覆盖数 | 关键决策 |
|--------|---------------|--------|----------|
| **Management Core** | DEC-001, DEC-009, DEC-012, IMPLICIT:Product/Food/Material | 4 | Warehouse Foundation, Legacy 隔离 |
| **POS** | DEC-006, DEC-008, DEC-009, DEC-010, IMPLICIT:* | 5+ | food/foods 统一, 跨域数据所有权 |
| **Inventory** | DEC-001, DEC-002, DEC-007, DEC-009, DEC-010, DEC-011, DEC-013 | 7 | Warehouse, 事件驱动 |
| **Finance** | DEC-003, DEC-007, DEC-008, DEC-011, DEC-012, IMPLICIT:Amount/Money | 6 | 金额单位, Legacy |
| **Order** | DEC-006, DEC-008, DEC-010, DEC-011, IMPLICIT:Order State Machine, IMPLICIT:Data Ownership | 6+ | food/foods, 状态机, 跨域所有权 |
| **Mini Program** | DEC-010, IMPLICIT:Data Ownership | 2 | 跨域数据所有权 |
| **Kitchen** | IMPLICIT:Order State Machine | 1 | 订单状态 |
| **Employee** | DEC-009 | 1 | stores_new |
| **Marketing** | DEC-003, DEC-004 | 2 | PaymentMethod, Customer |
| **System** | DEC-013, DEC-014, DEC-012 | 3 | 权限, 认证, Legacy |
| **Procurement** | DEC-001, DEC-002, DEC-005, DEC-007, DEC-011 | 5 | Warehouse, 事件驱动 |

### 5.2 按 Category 聚合

| Category | Decisions | 覆盖 |
|----------|-----------|------|
| DATA_MODEL | DEC-006, DEC-007, DEC-008, DEC-009 | 已裁决 + 迁移计划 |
| PRODUCT | DEC-001, DEC-002, DEC-003, DEC-004, DEC-005 | Foundation 决策 |
| ARCHITECTURE | DEC-010, DEC-011, DEC-012 | 跨域 + 事件 + Legacy |
| SECURITY | DEC-013, DEC-014 | 权限 + 认证 |
| IMPLICIT | Product/Food/Material, Order State Machine, Amount/Money, Data Ownership, Cross-Domain Access, Event Architecture | 隐式架构决策 |

---

## 六、Gap Analysis — 未覆盖的交叉项

| Gap | 说明 | 建议 |
|-----|------|------|
| REQ-MASTER-001 (供应商业务身份) | 无直接 Decision 覆盖 | 供应商身份已确认无冲突，可标记为无需额外 Decision |
| REQ-DATA-002 (Dialog/Form 继承) | 无直接 Decision 覆盖 | 属于实现层规范，可通过 Code Review 约束 |
| REQ-DATA-003 (Snapshot 语义) | 无直接 Decision 覆盖 | 属于数据规范，可通过文档约束 |
| REQ-CORE-003 (Service 层写入) | 仅 DEC-012 覆盖 | 建议增加 DEC-013/DEC-014 的写入路径验证 |
| REQ-SEC-002 (审计日志) | 仅 DEC-014 覆盖 | 建议增加审计日志专项 Decision |
| ARCH-TRUTH-002 (数据模型定义) | 无直接 Decision | 已通过其他 Decision 间接覆盖 |
| ARCH-ID-002 (禁止下游重新生成) | 仅 IMPLICIT 覆盖 | 建议显式化为 DEC-015 |
| ARCH-LEGACY-001 (Legacy 隔离) | 仅 DEC-012 覆盖 | 覆盖充分 |
| SR-005 (inventory/store_inventory 双写) | 无直接 Decision | 建议增加 DEC-016 解决库存双写事务 |

---

## 七、维护说明

1. 本文件随 Decision 状态变更而更新
2. 新增 Decision 需同步更新映射矩阵
3. Decision 状态变更（OPEN → RECOMMENDED → CONFIRMED）需更新
4. 新增 Requirement/Principle/Root Cause/Remediation Candidate 需更新反向映射
5. Gap Analysis 需定期审视并转化为新 Decision

---

*生成时间: 2026-09-09*  
*评审编号: PROJECT-MASTER-REVIEW-001*  
*维护者: opencode*
