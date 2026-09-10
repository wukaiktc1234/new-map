# PROJECT-DECISION-RECON-001 — 决策协调总结

> **文档类型**: Decision Coordination Summary  
> **文档版本**: v1.0  
> **生成日期**: 2026-09-09  
> **状态**: ACTIVE  
> **基于**: decision-recon-registry.yaml, decision-dependency-graph.md, INDEX.md, product-decision-pack.md, systemic-remediation-candidates.md

---

## 一、决策全景概览

| 分类 | 数量 | 状态 |
|------|------|------|
| **LOCKED (已锁定)** | 5 | LOCK-001, LOCK-003, LOCK-006, LOCK-007, LOCK-010 |
| **ALREADY_DECIDED (已裁决)** | 4 | DEC-006, DEC-007, DEC-008, DEC-009 |
| **SOLUTION_CLEAR (方案明确)** | 4 | DEC-002, DEC-003, DEC-005, DEC-012 |
| **PENDING_PRODUCT (待产品决策)** | 3 | DEC-001, DEC-004, IMPLICIT-001 |
| **PENDING_ARCHITECTURE (待架构决策)** | 4 | DEC-010, DEC-011, DEC-013, DEC-014 |
| **IMPLICIT (隐含决策)** | 6 | IMPLICIT-001 ~ IMPLICIT-006 |

---

## 二、哪些事情已经确定，不需要再讨论？

### 2.1 LOCKED 决策 (已锁定)

| Lock ID | 决策内容 | 锁定原因 | 解锁能力 |
|---------|---------|---------|---------|
| **LOCK-001** | foods 为菜品唯一真相源 | food/foods 双写已验证，foods 表为新真相源 | food 表废弃路径 |
| **LOCK-003** | 金额以分为准 (整数存储) | 新表统一用「分」，统一金额单位标准 | 金额计算一致性、跨域金额合约 |
| **LOCK-006** | stores_new 为门店唯一真相源 | stores 为废弃旧表，新代码必须查询 stores_new | 门店管理能力 |
| **LOCK-007** | departments 为组织真相源 | DepartmentService.create() + DepartmentCreatedEvent 完整支撑 | 组织管理能力 |
| **LOCK-010** | roles/permissions 为权限真相源 | 16 基线角色 + 180+ 权限码已定义 | 权限管理能力 |

**行动**: 以上决策为不可变锚点，所有下游决策必须基于此构建。

### 2.2 ALREADY_DECIDED 裁决决策

| Decision | 裁决结论 | 推荐方案 | 影响范围 |
|----------|---------|---------|---------|
| **DEC-006** | Product-Food-Material 关系 | **B_继承关系** — Material 是基础，Food 继承 Material，Product 继承 Food | Product, Inventory, Procurement |
| **DEC-007** | Order State Machine | **B_完整状态机** — 待确认→已确认→制作中→待配送→配送中→已完成→已取消→已退款 | Order, Payment, Delivery |
| **DEC-008** | Amount/Money Contract | **A_固定精度** — Decimal(10,2)，餐饮业务金额计算相对简单 | Order, Payment, Finance |
| **DEC-009** | Data Ownership | **B_业务域所有** — 数据归业务域所有，符合领域驱动设计 | All 域 |

**行动**: 以上裁决已确认，无需重新讨论，直接纳入工程执行。

---

## 三、哪些事情是当前真实问题，但解决方案已经明确？

| Decision | 问题描述 | 推荐方案 | 方案代码 | 行动 |
|----------|---------|---------|---------|------|
| **DEC-002** | Unit 管理模式 — 无独立表，存储在枚举/配置中 | 保持枚举，未来按需升级 | **A_enum** | 暂不建表，文档化单位规范 |
| **DEC-003** | PaymentMethod 管理 — 支付方式不统一 | 保持枚举，未来按需升级 | **A_enum** | 暂不建表，文档化支付方式 |
| **DEC-005** | Price 管理 — 价格模型不统一 | 保持内联，验证快照机制 | **A_inline** | 暂不建表，验证价格快照 |
| **DEC-012** | Legacy 清理 — 遗留表/服务/代码未清理 | 渐进式清理，分阶段执行 | **渐进式** | 按依赖链分阶段清理 |

**行动**: 以上问题解决方案已明确，可直接进入 Engineering Card 执行，无需进一步决策。

---

## 四、哪些事情必须由 Product Owner 决定？

### 4.1 DEC-001: Warehouse 管理模式 (P0)

**业务问题**: Warehouse 无独立表，仅通过 inventory.warehouse_id 引用，无法支撑多仓管理。

| 选项 | 方案 | 优点 | 缺点 |
|------|------|------|------|
| A | config (配置项) | 改动最小 | 无法支撑复杂仓库场景 |
| B | independent_table (独立表) | 完全独立，可扩展性强 | 数据模型变更大，迁移成本高 |
| **C** | **E_hybrid (混合)** | **兼顾简单与复杂场景，迁移成本可控** | **需维护两层配置逻辑** |

**推荐**: C — E_hybrid (混合方案)  
**依赖**: 无上游依赖  
**阻断**: 库存管理、仓库间调拨、库存盘点  
**影响域**: Inventory, Procurement, HR  
**截止日期**: 2026-09-16

### 4.2 DEC-004: Customer/Member 关系 (P1)

**业务问题**: Customer 与 Member 概念混用，散客消费无法被追踪。

| 选项 | 方案 | 优点 | 缺点 |
|------|------|------|------|
| A | customer_member合一 | 零改动 | 散客无法被追踪 |
| B | customer独立 | 概念独立 | 改动范围大 |
| **C** | **E_guest_member (散客类型)** | **保持 Member 为统一入口，改动最小** | **概念仍混用** |

**推荐**: C — E_guest_member  
**依赖**: DEC-013 (裸接口权限) 解锁  
**阻断**: 会员管理、客户管理、散客营销  
**影响域**: Order, Finance, Marketing, Mini Program  
**截止日期**: 2026-09-23

### 4.3 IMPLICIT-001: Product/Food/Material 边界

**业务问题**: Product、Food、Material 三者边界不清晰，数据冗余严重。

| 选项 | 方案 | 优点 | 缺点 |
|------|------|------|------|
| A | keep双Master | 保持现有逻辑 | 数据冗余严重 |
| B | merge | 统一数据模型 | Product 模型臃肿 |
| **C** | **C_deprecate_product + KEEP双Master** | **概念清晰，业务逻辑解耦** | **需数据迁移** |

**推荐**: C — 废弃通用 Product，保留 Food 和 Material 作为独立主数据  
**影响域**: Product, Inventory, Procurement  
**影响表**: products, foods, material_archives

---

## 五、哪些事情必须由 Architecture Owner 决定？

### 5.1 DEC-010: 跨域数据所有权 (P0)

**架构问题**: 跨域访问缺乏统一规范，存在直接数据库访问。

| 选项 | 方案 | 优点 | 缺点 |
|------|------|------|------|
| A | 直接数据库访问 | 性能高，实现简单 | 耦合度高，维护困难 |
| **B** | **API 访问** | **松耦合，可维护性好** | **性能开销** |
| C | 事件驱动 | 异步解耦 | 最终一致性，调试困难 |

**推荐**: B — API 访问  
**依赖**: LOCK-003 (金额以分为准), DEC-001 (Warehouse)  
**阻断**: 数据一致性、性能优化  
**解锁**: DEC-011 (事件驱动架构), 服务独立部署, 技术异构  
**影响域**: All 域  
**截止日期**: 2026-09-16

### 5.2 DEC-011: 事件驱动架构 (P1)

**架构问题**: 事件架构不清晰，缺乏统一的事件定义和处理机制。

| 选项 | 方案 | 优点 | 缺点 |
|------|------|------|------|
| A | 同步事件 | 实现简单 | 耦合度高，可靠性差 |
| **B** | **异步事件** | **解耦性好，可靠性高** | **实现复杂** |
| C | 事件溯源 | 可审计，可回溯 | 存储开销大 |

**推荐**: B — 异步事件  
**依赖**: DEC-010 (跨域数据所有权)  
**阻断**: 数据一致性、系统监控  
**解锁**: 事件驱动架构, 实时数据处理  
**影响域**: All 域

### 5.3 DEC-013: 裸接口权限 (P1)

**安全问题**: 认证策略不统一，存在多种认证方式。

| 选项 | 方案 | 优点 | 缺点 |
|------|------|------|------|
| A | Session 认证 | 实现简单，安全性高 | 不支持分布式 |
| **B** | **JWT 认证** | **无状态，可扩展，支持分布式** | **令牌管理复杂** |
| C | OAuth2 认证 | 标准化，支持第三方 | 实现复杂 |

**推荐**: B — JWT 认证  
**依赖**: 无上游依赖 (INDEPENDENT)  
**阻断**: 用户管理、权限控制  
**解锁**: 单点登录, 第三方登录, DEC-004 (Customer/Member)  
**影响域**: Security, User  
**截止日期**: 2026-09-16

### 5.4 DEC-014: 供应商 H5 认证

**安全问题**: 供应商 H5 端无认证机制，安全风险高。

| 选项 | 方案 | 优点 | 缺点 |
|------|------|------|------|
| A | OAuth2 | 标准化，安全性高 | 实现复杂 |
| **B** | **JWT + 白名单** | **轻量级，实现简单** | **需维护白名单** |
| C | 独立认证 | 完全独立控制 | 实现复杂 |

**推荐**: B — JWT + 白名单  
**影响域**: Procurement, Security  
**影响表**: supplier_portal_users

---

## 六、哪些 Decision 存在前后依赖？

### 6.1 Decision Dependency Graph

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                          决策依赖关系图                                         │
├───────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  Phase 1: 锚点决策 (LOCKED)                                                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                                    │
│  │ LOCK-001 │  │ LOCK-003 │  │ LOCK-006 │                                    │
│  │foods真相源│  │金额以分准 │  │stores真相│                                    │
│  └────┬─────┘  └────┬─────┘  └──────────┘                                    │
│       │              │                                                         │
│       │unlocks       │blocks                                                  │
│       ▼              ▼                                                         │
│  Phase 2: 架构决策                                                             │
│  ┌──────────────────────┐  ┌──────────────────────┐                           │
│  │      DEC-010         │  │      DEC-013         │                           │
│  │   跨域数据所有权      │  │    裸接口权限         │← INDEPENDENT             │
│  └──┬───────────┬───────┘  └──────────┬───────────┘                           │
│     │           │                      │                                       │
│  depends_on   unlocks                unlocks                                   │
│     │           │                      │                                       │
│     ▼           ▼                      ▼                                       │
│  Phase 3: 产品决策                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                                    │
│  │ DEC-001  │  │ DEC-011  │  │ DEC-004  │                                    │
│  │Warehouse │  │事件驱动   │  │Customer/ │                                    │
│  └──────────┘  └──────────┘  │ Member   │                                    │
│                               └──────────┘                                    │
│                                                                               │
└───────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 依赖链详细说明

| 依赖类型 | 上游 → 下游 | 业务逻辑 |
|----------|------------|---------|
| **UNLOCKS** | LOCK-001 → LOCK-006 | foods 真相源确定后，stores_new 角色才能精确锁定 |
| **BLOCKS** | LOCK-003 → DEC-010 | 金额单位统一后，跨域数据所有权才能被决策 |
| **DEPENDS_ON** | DEC-001 → DEC-010 | Warehouse 功能范围未定之前，无法确定库存域和订单域的数据所有权 |
| **UNLOCKS** | DEC-010 → DEC-011 | 跨域数据所有权确定后，事件驱动的触发点和数据契约才能精确设计 |
| **UNLOCKS** | DEC-013 → DEC-004 | 裸接口权限模型确定后，Customer/Member 的接口设计才能被最终决策 |

### 6.3 正确的决策锁定顺序

```
┌───────┬──────────────────────────┬──────────────────────────────────────┐
│ Order │ Decision                 │ Reason                               │
├───────┼──────────────────────────┼──────────────────────────────────────┤
│ 1     │ LOCK-001 (已锁定)        │ Foundation: foods 为真相源           │
│ 2     │ LOCK-006 (已锁定)        │ Derives from LOCK-001               │
│ 3     │ LOCK-003 (已锁定)        │ Foundation: 金额以分为准             │
│ 4     │ DEC-013 (Architecture)   │ INDEPENDENT, no upstream deps        │
│ 5     │ DEC-010 (Architecture)   │ BLOCKED_BY → LOCK-003 ✓, DEC-001 ✓ │
│ 6     │ DEC-001 (Product)        │ BLOCKED_BY → DEC-010 (resolved at 5)│
│ 7     │ DEC-004 (Product)        │ DEPENDS_ON → DEC-013 (resolved at 4)│
│ 8     │ DEC-011 (Architecture)   │ DEPENDS_ON → DEC-010 (resolved at 5)│
└───────┴──────────────────────────┴──────────────────────────────────────┘
```

### 6.4 Critical Path (关键路径)

```
LOCK-003 → DEC-010 → DEC-011
```

这是最长的依赖链，必须优先解决。DEC-010 的产出直接约束 DEC-011 的事件 schema 设计。

---

## 七、哪些 Decision 一旦确定，会影响哪些域？

### 7.1 影响分析矩阵

| Decision | 影响域 | 影响表 | 影响 API | 影响前端 | 严重程度 |
|----------|--------|--------|---------|---------|---------|
| **DEC-001** Warehouse | Inventory, Procurement, HR | warehouses, inventory, employees | /api/v1/warehouses | WarehouseList.vue | HIGH |
| **DEC-004** Customer/Member | Order, Finance, Marketing, Mini Program | customers, members, member_points | /api/v1/customers, /api/v1/members | CustomerList.vue, MemberList.vue | MEDIUM |
| **DEC-010** 跨域数据所有权 | All | all_tables | all_apis | all_pages | HIGH |
| **DEC-011** 事件驱动架构 | All | events, event_logs, message_queues | /api/v1/events | EventMonitor.vue | HIGH |
| **DEC-013** 裸接口权限 | Security, User | users, roles, permissions | /api/v1/auth, /api/v1/users | Login.vue, UserList.vue | HIGH |
| **DEC-014** 供应商 H5 认证 | Procurement, Security | supplier_portal_users | /api/v1/supplier-portal | SupplierPortal.vue | MEDIUM |
| **DEC-006** Product-Food-Material | Product, Inventory, Procurement | material_archives, foods, products | /api/v1/materials, /api/v1/foods | MaterialList.vue | HIGH |
| **DEC-007** Order State Machine | Order, Payment, Delivery | orders, order_items, order_status_logs | /api/v1/orders | OrderList.vue | MEDIUM |
| **DEC-008** Amount/Money Contract | Order, Payment, Finance | orders, order_items, payments | /api/v1/payments | PaymentList.vue | HIGH |
| **DEC-009** Data Ownership | All | all_tables | /api/v1/data-governance | DataOwnership.vue | HIGH |

### 7.2 影响链路图

```
DEC-001 (Warehouse)
  │
  ├──→ 影响 Inventory 域: 库存管理、仓位管理、库存盘点
  ├──→ 影响 Procurement 域: 采购入库、仓库调拨
  └──→ 影响 HR 域: 员工-仓库关联

DEC-010 (跨域数据所有权)
  │
  ├──→ 影响所有域: 数据归属定义、跨域访问规范
  ├──→ 解锁 DEC-011: 事件 schema 设计
  └──→ 阻断: 数据一致性、性能优化

DEC-013 (裸接口权限)
  │
  ├──→ 影响 Security 域: 认证、授权、会话管理
  ├──→ 解锁 DEC-004: Customer/Member 接口设计
  └──→ 影响所有前端: 登录、权限验证
```

---

## 八、哪些 Remediation Candidate 必须等待 Decision？

### 8.1 Engineering Readiness Matrix

| Remediation | 阻塞 Decision | 前置条件 | 状态 | 优先级 |
|-------------|--------------|---------|------|--------|
| **SR-001** 统一 Product/Material/Food Identity | LOCK-001 ✓, LOCK-003 ✓ | foods 为真相源已确认 | **READY FOR PLANNING** | P0 |
| **SR-002** 统一订单状态机 | DEC-007 (B方案已裁决) | 完整状态机方案确定 | **READY FOR PLANNING** | P1 |
| **SR-003** 统一金额单位 | LOCK-003 ✓ | 金额以分为准已确认 | **READY FOR PLANNING** | P0 |
| **SR-004** 解决凭证状态映射 | DEC-008 (A方案已裁决) | 固定精度方案确定 | **READY FOR PLANNING** | P2 |
| **SR-005** 解决 inventory/store_inventory 双写 | SR-001, SR-007 | 商品 Identity 统一 + Warehouse 基础 | **BLOCKED** | P1 |
| **SR-006** 解决成本双写 | SR-003, SR-001 | 金额单位统一 + 商品 Identity | **BLOCKED** | P1 |
| **SR-007** 建立 Warehouse Foundation | **DEC-001** (PENDING) | Product Owner 决策 Warehouse 模式 | **BLOCKED** | P2 |
| **SR-008** 建立 Unit Foundation | **DEC-002** (A_enum 已明确) | 保持枚举，无需建表 | **READY FOR DOCUMENTATION** | P2 |
| **SR-009** 建立 PaymentMethod Foundation | **DEC-003** (A_enum 已明确) | 保持枚举，无需建表 | **READY FOR DOCUMENTATION** | P2 |
| **SR-010** 建立 Customer Foundation | **DEC-004** (PENDING) | Product Owner 决策 Customer/Member 关系 | **BLOCKED** | P1 |
| **SR-011** 建立 Price Foundation | **DEC-005** (A_inline 已明确) | 保持内联，无需建表 | **READY FOR DOCUMENTATION** | P3 |
| **SR-012** 明确跨域数据所有权 | **DEC-010** (PENDING) | Architecture Owner 决策跨域访问模式 | **BLOCKED** | P0 |
| **SR-013** 加强事件驱动可靠性 | **DEC-011** (PENDING) | Architecture Owner 决策事件架构 | **BLOCKED** | P1 |
| **SR-014** 解决多端写入冲突 | DEC-010, DEC-007 | 数据所有权 + 订单状态机统一 | **BLOCKED** | P0 |
| **SR-015** 清理 Legacy 数据库表 | SR-001, SR-002, SR-003 | 商品 Identity + 订单状态 + 金额单位 | **BLOCKED** | P2 |
| **SR-016** 清理 Legacy 服务 | SR-015 | 遗留表清理 | **BLOCKED** | P2 |
| **SR-017** 清理 Dead Code | SR-016 | Legacy 服务清理 | **BLOCKED** | P2 |

### 8.2 阻塞依赖链

```
┌─────────────────────────────────────────────────────────────────────┐
│                     BLOCKED 依赖链                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  DEC-001 (Warehouse) ──→ SR-007 ──→ SR-005 ──→ SR-015 ──→ SR-016 ──→ SR-017│
│                                                                     │
│  DEC-004 (Customer)  ──→ SR-010 ──→ SR-003 ──→ SR-006              │
│                                                                     │
│  DEC-010 (跨域)      ──→ SR-012 ──→ SR-014                          │
│           │              │                                           │
│           └──→ DEC-011   └──→ SR-002 ──→ SR-015                    │
│                                                                     │
│  DEC-013 (权限)      ──→ DEC-004 ──→ SR-010                        │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 九、哪些工程事项其实已经可以直接进入 Engineering Card？

### 9.1 ENGINEERING_READY 项

以下事项解决方案已明确，无上游依赖，可直接进入工程执行：

| 编号 | 工程事项 | 决策来源 | 方案 | 优先级 | 影响范围 |
|------|---------|---------|------|--------|---------|
| **ER-001** | foods 表废弃路径确认 | LOCK-001 | foods 为唯一真相源，制定 food 表迁移计划 | P0 | Management Core, POS, Inventory |
| **ER-002** | stores_new 为门店真相源确认 | LOCK-006 | 新代码必须查询 stores_new，标记 stores 为 DEPRECATED | P0 | 全业务流程 |
| **ER-003** | 金额单位统一为「分」 | LOCK-003 | 新表统一用分存储，修正 finance_records 注释 | P0 | Finance, Order, Payment |
| **ER-004** | 完整订单状态机实现 | DEC-007 (B) | 实现待确认→已确认→制作中→待配送→配送中→已完成→已取消→已退款 | P1 | Order, Payment, Delivery |
| **ER-005** | 金额合约 Decimal(10,2) 实现 | DEC-008 (A) | 统一金额字段精度和计算逻辑 | P1 | Order, Payment, Finance |
| **ER-006** | Unit 枚举文档化 | DEC-002 (A_enum) | 文档化单位规范，无需建表 | P2 | Product, Inventory |
| **ER-007** | PaymentMethod 枚举文档化 | DEC-003 (A_enum) | 文档化支付方式，无需建表 | P2 | Payment, Order |
| **ER-008** | Price 内联模式验证 | DEC-005 (A_inline) | 验证价格快照机制 | P2 | Product, Order |
| **ER-009** | Product-Food-Material 继承关系 | DEC-006 (B) | 建立 Material→Food→Product 继承链 | P1 | Product, Inventory, Procurement |
| **ER-010** | 裸接口权限方案实现 | DEC-013 (B) | JWT 认证实现 | P1 | Security, User |
| **ER-011** | 跨域数据所有权 API 规范 | DEC-010 (B) | 建立跨域 API 访问规范 | P0 | All |
| **ER-012** | Legacy 渐进式清理 Phase 1 | DEC-012 | 清理 orders_legacy 表、food 表消费者 | P2 | 全业务 |

### 9.2 入卡标准

以下条件满足即可入 Engineering Card：
- [x] 决策已锁定或方案已明确
- [x] 无上游依赖或依赖已满足
- [x] 影响范围已识别
- [x] 优先级已确定

---

## 十、行动建议

### 10.1 短期 (本周)

1. **Product Owner 确认 DEC-001** — Warehouse 管理模式，解锁 SR-007
2. **Architecture Owner 启动 DEC-013** — 裸接口权限 INDEPENDENT，可立即启动
3. **Architecture Owner 启动 DEC-010** — 跨域数据所有权，解锁 DEC-011
4. **工程团队执行 ER-001~ER-005** — 已锁定/已裁决项直接入卡

### 10.2 中期 (下周)

1. **Product Owner 确认 DEC-004** — Customer/Member 关系，依赖 DEC-013
2. **Architecture Owner 完成 DEC-010** — 跨域数据所有权，解锁 DEC-011
3. **工程团队执行 ER-009~ER-011** — 架构决策完成后入卡

### 10.3 长期 (Sprint 3+)

1. **架构决策完成 DEC-011** — 事件驱动架构
2. **工程团队执行 SR-005~SR-017** — 依赖链逐步解锁
3. **Legacy 渐进式清理** — SR-015~017 分阶段执行

---

## 十一、风险提示

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| DEC-001 延迟 | SR-007/SR-005/SR-015 全链路阻塞 | 设置 Deadline 2026-09-16 |
| DEC-010 延迟 | DEC-011/SR-012/SR-014 全链路阻塞 | 设置 Deadline 2026-09-16 |
| DEC-013 延迟 | DEC-004/SR-010 全链路阻塞 | INDEPENDENT，可立即启动 |
| 金额单位双轨 | 财务报表偏差 100 倍 | ER-003 优先执行 |
| food/foods 双写 | 数据不一致 | ER-001 优先执行 |

---

> **下一步**: 请 Product Owner 确认 DEC-001/DEC-004/IMPLICIT-001，Architecture Owner 启动 DEC-010/DEC-011/DEC-013/DEC-014，工程团队执行 ER-001~ER-012。
