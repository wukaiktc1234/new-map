# Architecture Principle Baseline

> **Review ID**: PROJECT-MASTER-REVIEW-001
> **Version**: 1.0
> **Date**: 2026-09-09
> **Status**: CONFIRMED
> **Scope**: Food Traceability System 全域架构原则

---

## 一、原则总览

| Category | 原则数量 | 关键关注点 |
|----------|----------|------------|
| TRUTH_SOURCE | 2 | 真相源唯一性、数据模型定义 |
| DATA_OWNERSHIP | 2 | 角色分离、入口与真相区分 |
| WRITE_AUTHORITY | 2 | Service层写入、事件驱动跨域 |
| BUSINESS_ENTRY | 2 | 客户端定位、多端写入所有权 |
| MASTER_DATA | 2 | Canonical Source、Reference追溯 |
| IDENTITY | 2 | Identity传播、下游禁止重新生成 |
| CROSS_DOMAIN | 2 | 数据流所有权、最终一致性 |
| LEGACY | 2 | Legacy隔离、Truth Source切换点 |

---

## 二、Truth Source 原则

### ARCH-TRUTH-001: 每个业务对象必须有且仅有一个 Truth Source

```yaml
principle_id: ARCH-TRUTH-001
title: Truth Source 唯一性
description: 每个业务对象在系统中必须有且仅有一个 Truth Source 作为权威数据源，不得存在多个并行的真相源
category: TRUTH_SOURCE
rationale: |
  多真相源会导致数据不一致、冲突裁决复杂化、报表口径混乱。
  本项目已有14个业务对象明确 Truth Source（见 truth-conflict-map.md），但存在冲突点：
  - food/foods 双写（冲突7）
  - inventory/store_inventory 双写（冲突8）
  - 科目余额双轨（冲突3，已裁决）
implications:
  - 新增业务对象必须在设计阶段明确 Truth Source
  - 不得在代码中绕过 Truth Source 直接读写其他数据源
  - 冲突点必须记录在 truth-conflict-map.md 中并标明状态
evidence:
  - truth-conflict-map.md: 14个 Truth Source 表明
  - master-data-source-map.md: 各主数据 Canonical Source 定义
  - business-object-map.md: 所有核心业务对象的 Truth Source 字段
status: CONFIRMED
```

**验证依据**:
- `truth-conflict-map.md` §一: 14个业务对象均有明确 Truth Source 表
- `master-data-source-map.md` §一: 15个主数据 Canonical Source
- `business-object-map.md`: 所有 VERIFIED 对象均有 Truth Source 字段

---

### ARCH-TRUTH-002: Truth Source 由数据模型定义，不由客户端决定

```yaml
principle_id: ARCH-TRUTH-002
title: Truth Source 数据模型定义
description: Truth Source 由数据模型（数据库表）定义，不由客户端（前端/终端）决定。客户端只是数据的读写入口，不拥有业务事实
category: TRUTH_SOURCE
rationale: |
  客户端是业务入口而非真相源，这是跨终端数据一致性的基础。
  本项目有7+终端（Management Core, POS, Kitchen, Employee, MiniProgram, Receiving Mobile, Inventory, Finance），
  必须由后端数据模型统一管理真相源
implications:
  - 前端代码不得缓存业务事实作为权威数据
  - 前端状态变更必须通过 API 调用后端 Service
  - 多端数据同步以后端 Truth Source 为准
evidence:
  - business-object-map.md: Truth Source 均为数据库表（foods, orders, material_archives等）
  - cross-domain-coordination-matrix.md: 各终端角色定义（OWNER, WRITE, READ）
  - truth-conflict-map.md: 冲突裁决基于数据模型而非客户端
status: CONFIRMED
```

**验证依据**:
- `business-object-map.md`: 所有 VERIFIED 对象的 Truth Source 为数据库表
- `cross-domain-coordination-matrix.md`: Management Core 为多个对象的 OWNER
- `truth-conflict-map.md`: 裁决依据为内部裁决/PADR，非客户端

---

## 三、Data Ownership 原则

### ARCH-OWN-001: Data Owner ≠ Write Actor ≠ UI Owner ≠ API Caller

```yaml
principle_id: ARCH-OWN-001
title: 角色分离原则
description: 数据所有者（Data Owner）、写入执行者（Write Actor）、UI归属（UI Owner）、API调用者（API Caller）必须分离，不得合一
category: DATA_OWNERSHIP
rationale: |
  角色合一导致职责不清、权限失控、审计困难。
  本项目已定义7种角色：OWNER, WRITE, READ, REFERENCE, SNAPSHOT, EVENT CONSUMER, DEPENDENCY
  （见 cross-domain-coordination-matrix.md §1.2）
implications:
  - 每个业务对象必须明确 OWNER，OWNER 只能有一个
  - WRITE 角色只能在 OWNER 授权下写入
  - READ 角色不得直接修改数据
  - UI Owner ≠ Data Owner 时必须通过 API 交互
evidence:
  - cross-domain-coordination-matrix.md §1.2: 7种角色定义
  - cross-domain-coordination-matrix.md §2.1: 核心业务对象矩阵
  - data-lineage-map.md §3.1: 数据所有权矩阵
status: CONFIRMED
```

**验证依据**:
- `cross-domain-coordination-matrix.md` §2.1: Food 对象中 Management Core 为 OWNER，其他为 READ
- `cross-domain-coordination-matrix.md` §2.1: Order 对象中 POS 为 OWNER，MiniProgram 为 WRITE
- `data-lineage-map.md` §3.1: 数据所有权矩阵显示 OWNER/WRITE/READ 分离

---

### ARCH-OWN-002: Business Entry Point ≠ Truth Source

```yaml
principle_id: ARCH-OWN-002
title: 业务入口与真相源分离
description: Business Entry Point（业务入口）不等于 Truth Source（真相源）。业务入口只是数据的采集点，真相源是数据的最终归属
category: DATA_OWNERSHIP
rationale: |
  入口与真相分离是跨终端数据一致性的基础。
  本项目有多个业务入口：POS（订单）、MiniProgram（订单）、Receiving Mobile（收货），
  但真相源统一在后端（orders表、inventory表等）
implications:
  - 业务入口不得直接写入 Truth Source，必须通过 Service 层
  - 业务入口采集的数据必须经过校验后才能写入 Truth Source
  - 多个业务入口可以采集同一业务数据，但 Truth Source 只能有一个
evidence:
  - cross-domain-coordination-matrix.md §2.1: Order 的 OWNER 是 POS，但 Truth Source 是 orders 表
  - business-identity-propagation-map.md: Identity 从 Truth Source 传播到各终端
  - master-data-source-map.md §5: 写入路径均通过 Service 层
status: CONFIRMED
```

**验证依据**:
- `cross-domain-coordination-matrix.md` §3.1: 下游系统反向定义上游事实，但 Truth Source 不变
- `business-identity-propagation-map.md`: Identity 从 Truth Source 传播
- `master-data-source-map.md` §5: 写入路径示例

---

## 四、Write Authority 原则

### ARCH-WRITE-001: 写入操作必须通过 Service 层

```yaml
principle_id: ARCH-WRITE-001
title: Service层写入原则
description: 所有写入操作必须通过 Service 层，不得绕过 Service 直接操作数据库或 Repository
category: WRITE_AUTHORITY
rationale: |
  绕过 Service 层直接写入会导致：业务逻辑分散、事务控制失效、审计追踪困难。
  本项目所有业务对象的写入路径均通过 Service（见 business-object-map.md）
implications:
  - Controller 不得直接调用 Repository
  - 前端 API 调用必须经过 Service 层
  - Service 层负责业务校验、事务控制、事件发布
evidence:
  - business-object-map.md: 所有 Write Path 均为 Service 方法
  - master-data-source-map.md §5: 写入路径示例
  - legacy-map.md §7: 直接数据库写入被标记为 Legacy 问题
status: CONFIRMED
```

**验证依据**:
- `business-object-map.md`: 所有业务对象的 Write Path 均为 Service 方法（如 FoodService.create）
- `master-data-source-map.md` §5: 写入路径示例（SupplierService.create/update）
- `legacy-map.md` §7: 直接数据库写入（DatabaseFixConfig, TableInitConfig）被标记为遗留问题

---

### ARCH-WRITE-002: 跨域写入必须通过事件驱动

```yaml
principle_id: ARCH-WRITE-002
title: 事件驱动跨域写入
description: 跨域写入操作必须通过事件驱动机制，不得直接调用其他域的 Service 或直接操作其他域的数据
category: WRITE_AUTHORITY
rationale: |
  直接跨域调用会导致：耦合度高、事务边界模糊、故障传播。
  本项目使用 RabbitMQ 事件进行跨域协调（见 event-job-map.md, cross-domain-coordination-matrix.md）
implications:
  - 跨域数据变更必须发布事件
  - 下游系统通过消费事件更新本地数据
  - 事件必须保证最终一致性（见 ARCH-CROSS-002）
evidence:
  - cross-domain-coordination-matrix.md §4.2: 跨终端数据流通过事件驱动
  - event-job-map.md: 事件定义（OrderCreatedEvent, ReceiptConfirmationCompletedEvent等）
  - data-lineage-map.md §2.3: 订单血缘中的事件消费
status: CONFIRMED
```

**验证依据**:
- `cross-domain-coordination-matrix.md` §2.1: Order 的 Inventory 域角色为 EVENT CONSUMER
- `data-lineage-map.md` §2.3: Order 通过 OrderCreatedEvent 驱动下游
- `event-job-map.md`: 事件定义

---

## 五、Business Entry 原则

### ARCH-ENTRY-001: 客户端是业务入口，不拥有业务事实

```yaml
principle_id: ARCH-ENTRY-001
title: 客户端入口定位
description: 客户端（前端/终端）是业务入口，负责数据采集和展示，不拥有业务事实。业务事实的权威来源是后端 Truth Source
category: BUSINESS_ENTRY
rationale: |
  客户端是临时的、可替换的，业务事实必须持久化在后端。
  本项目有7+终端，客户端可能离线、崩溃、被替换，业务事实必须在后端持久化
implications:
  - 客户端不得将本地状态作为业务事实
  - 客户端必须通过 API 与后端同步
  - 客户端缓存数据必须标明非权威
evidence:
  - cross-domain-coordination-matrix.md: 各终端角色均为 READ/WRITE，非 OWNER
  - business-identity-propagation-map.md: Identity 从 Truth Source 传播到客户端
  - business-object-map.md: Truth Source 为数据库表，非前端状态
status: CONFIRMED
```

**验证依据**:
- `cross-domain-coordination-matrix.md` §2.1: 所有终端角色均为 READ/WRITE/EVENT CONSUMER，非 OWNER
- `business-identity-propagation-map.md`: Identity 从 Truth Source 传播
- `business-object-map.md`: Truth Source 为数据库表

---

### ARCH-ENTRY-002: 多端写入必须有明确的主数据所有权

```yaml
principle_id: ARCH-ENTRY-002
title: 多端写入所有权明确
description: 多个终端对同一业务对象有写入权限时，必须明确主数据所有权（OWNER），避免写入冲突
category: BUSINESS_ENTRY
rationale: |
  多端写入无明确所有权会导致：数据覆盖、状态冲突、审计困难。
  本项目 Order 对象在 POS 和 MiniProgram 都有写入权限（见 cross-domain-coordination-matrix.md）
implications:
  - 每个业务对象必须有唯一 OWNER
  - 非 OWNER 的写入必须通过事件或 API 调用 OWNER 的 Service
  - 写入冲突必须在设计阶段解决
evidence:
  - cross-domain-coordination-matrix.md §2.1: Order 的 OWNER 是 POS，MiniProgram 是 WRITE
  - data-lineage-map.md §3.1: 数据所有权矩阵
  - cross-domain-coordination-matrix.md §5: 数据冲突风险点
status: CONFIRMED
```

**验证依据**:
- `cross-domain-coordination-matrix.md` §2.1: Order 对象中 POS 为 OWNER，MiniProgram 为 WRITE
- `data-lineage-map.md` §3.1: 数据所有权矩阵
- `cross-domain-coordination-matrix.md` §5.2: 数据冲突风险点

---

## 六、Master Data 原则

### ARCH-MASTER-001: Master Data 必须有 Canonical Source

```yaml
principle_id: ARCH-MASTER-001
title: Master Data Canonical Source
description: 所有 Master Data（主数据）必须有明确的 Canonical Source（标准数据源），不得存在多个并行的权威数据源
category: MASTER_DATA
rationale: |
  Master Data 是业务系统的基础数据，多个权威源会导致：数据不一致、报表口径混乱、集成困难。
  本项目已识别15个主数据的 Canonical Source（见 master-data-source-map.md）
implications:
  - 新增 Master Data 必须在设计阶段定义 Canonical Source
  - 已有 Master Data 必须确保 Canonical Source 唯一
  - Legacy Source 必须标记为废弃并制定迁移计划
evidence:
  - master-data-source-map.md §一: 15个主数据 Canonical Source
  - master-data-source-map.md §四: Canonical Source 定义
  - business-object-map.md: 所有 VERIFIED 对象的 Truth Source
status: CONFIRMED
```

**验证依据**:
- `master-data-source-map.md` §一: 15个主数据均有 Canonical Source
- `master-data-source-map.md` §四: Canonical Source 定义（唯一真相源）
- `business-object-map.md`: 所有 VERIFIED 对象的 Truth Source

---

### ARCH-MASTER-002: Reference 数据必须可追溯到 Canonical Source

```yaml
principle_id: ARCH-MASTER-002
description: Reference 数据（引用数据）必须可追溯到其 Canonical Source，不得存在无法追溯的引用
category: MASTER_DATA
title: Reference 数据可追溯
rationale: |
  无法追溯的引用会导致：数据溯源困难、变更影响不可控、数据质量问题。
  本项目已建立 Reference/Snapshot/Derived 判定矩阵（见 master-data-source-map.md §3）
implications:
  - 引用数据必须保留引用关系（外键或语义关联）
  - Snapshot 数据必须标明创建时间点
  - Derived 数据必须标明计算来源
evidence:
  - master-data-source-map.md §3: Reference/Snapshot/Derived 判定矩阵
  - master-data-source-map.md §2: 各主数据引用关系分析
  - data-lineage-map.md §2: 核心业务对象血缘追踪
status: CONFIRMED
```

**验证依据**:
- `master-data-source-map.md` §3: Reference/Snapshot/Derived 判定矩阵（供应商、商品、物料、员工、门店）
- `master-data-source-map.md` §2: 各主数据引用关系分析
- `data-lineage-map.md` §2: 核心业务对象血缘追踪

---

## 七、Identity 原则

### ARCH-ID-001: Business Identity 必须从 Canonical Source 传播

```yaml
principle_id: ARCH-ID-001
title: Identity 传播原则
description: Business Identity（业务标识）必须从 Canonical Source 传播到下游系统，不得在下游重新生成
category: IDENTITY
rationale: |
  Identity 重新生成会导致：引用断裂、数据溯源困难、一致性问题。
  本项目已建立 Identity 传播地图（见 business-identity-propagation-map.md）
implications:
  - 下游系统必须引用上游 Identity，不得重新生成
  - Identity 传播必须使用 REFERENCE 或 SNAPSHOT 类型
  - 不得使用 OVERRIDDEN 或 DUPLICATE 类型
evidence:
  - business-identity-propagation-map.md: Identity 传播地图
  - business-identity-propagation-map.md §3: Identity 问题分类
  - master-data-source-map.md §3: Reference/Snapshot 判定矩阵
status: CONFIRMED
```

**验证依据**:
- `business-identity-propagation-map.md`: Identity 传播地图（supplierId, materialId, storeId等）
- `business-identity-propagation-map.md` §3: Identity 问题分类（Loss, Re-generation, Wrong等）
- `master-data-source-map.md` §3: Reference/Snapshot 判定矩阵

---

### ARCH-ID-002: 不得在下游重新生成 Business Identity

```yaml
principle_id: ARCH-ID-002
title: 禁止下游重新生成 Identity
description: 下游系统不得重新生成 Business Identity，必须从 Canonical Source 传播，避免 Identity 冲突和引用断裂
category: IDENTITY
rationale: |
  下游重新生成 Identity 会导致：引用断裂、数据溯源困难、一致性问题。
  本项目存在 Identity 问题：product_id 错误语义、warehouseId Identity Loss（见 business-identity-propagation-map.md §3）
implications:
  - 下游系统必须通过 API 或事件获取 Identity
  - 下游系统不得在本地生成新的 Identity
  - 已存在的重新生成问题必须制定修复计划
evidence:
  - business-identity-propagation-map.md §3.2: Identity Re-generation 问题（PaymentDialog Re-query）
  - business-identity-propagation-map.md §3.3: Wrong Identity 问题（product_id 错误语义）
  - business-identity-propagation-map.md §3.1: Identity Loss 问题（warehouseId 无 Canonical Source）
status: CONFIRMED
```

**验证依据**:
- `business-identity-propagation-map.md` §3.2: Identity Re-generation 问题（PaymentDialog Re-query Pattern）
- `business-identity-propagation-map.md` §3.3: Wrong Identity 问题（product_id 实际指向物料）
- `business-identity-propagation-map.md` §3.1: Identity Loss 问题（warehouseId 无 Canonical Source）

---

## 八、Cross-Domain 原则

### ARCH-CROSS-001: 跨域数据流必须明确所有权

```yaml
principle_id: ARCH-CROSS-001
title: 跨域数据所有权
description: 跨域数据流必须明确所有权，每个数据元素必须有明确的 OWNER，不得存在无主数据
category: CROSS_DOMAIN
rationale: |
  无主数据会导致：责任不清、故障排查困难、数据质量问题。
  本项目已建立数据所有权矩阵（见 cross-domain-coordination-matrix.md §5, data-lineage-map.md §3）
implications:
  - 每个业务对象必须有唯一 OWNER
  - 跨域数据流必须标明数据来源和所有权
  - 无主数据必须指定 OWNER
evidence:
  - cross-domain-coordination-matrix.md §5: 数据所有权矩阵
  - data-lineage-map.md §3: 数据所有权详细分析
  - cross-domain-coordination-matrix.md §3: 下游系统反向定义上游事实
status: CONFIRMED
```

**验证依据**:
- `cross-domain-coordination-matrix.md` §5: 数据所有权矩阵（Food, Material, Order等）
- `data-lineage-map.md` §3: 数据所有权详细分析
- `cross-domain-coordination-matrix.md` §3: 下游系统反向定义上游事实

---

### ARCH-CROSS-002: 事件驱动必须保证最终一致性

```yaml
principle_id: ARCH-CROSS-002
title: 事件驱动最终一致性
description: 事件驱动的跨域数据流必须保证最终一致性，事件丢失或延迟不得导致数据永久不一致
category: CROSS_DOMAIN
rationale: |
  事件丢失或延迟会导致：数据不一致、业务中断、财务对账失败。
  本项目使用 RabbitMQ 事件驱动，存在事件丢失风险（见 cross-domain-coordination-matrix.md §7）
implications:
  - 事件必须持久化（持久化队列）
  - 事件消费失败必须重试
  - 必须有死信队列处理无法消费的事件
  - 必须有补偿机制处理最终一致性失败
evidence:
  - cross-domain-coordination-matrix.md §7.1: 事件丢失风险
  - cross-domain-coordination-matrix.md §6: 跨域协调风险矩阵
  - data-lineage-map.md §7: 数据血缘风险矩阵
status: CONFIRMED
```

**验证依据**:
- `cross-domain-coordination-matrix.md` §7.1: 关键发现中提到事件丢失风险
- `cross-domain-coordination-matrix.md` §6: 跨域协调风险矩阵（高风险协调点）
- `data-lineage-map.md` §7: 数据血缘风险矩阵（高风险血缘点）

---

## 九、Legacy 原则

### ARCH-LEGACY-001: Legacy 系统必须隔离，不得影响新系统

```yaml
principle_id: ARCH-LEGACY-001
title: Legacy 系统隔离
description: Legacy 系统（遗留系统）必须与新系统隔离，不得影响新系统的正常运行
category: LEGACY
rationale: |
  Legacy 系统与新系统耦合会导致：故障传播、维护困难、升级风险。
  本项目存在多个 Legacy 组件（见 legacy-map.md）
implications:
  - Legacy 代码必须标记为 @Deprecated
  - Legacy 数据库表必须标记为废弃
  - Legacy 服务实现必须逐步替换
  - 新功能不得依赖 Legacy 组件
evidence:
  - legacy-map.md: Legacy 数据库表、服务实现、控制器、接口
  - business-object-map.md: Legacy 对象（food, product, orders_legacy, voucher_header, sales_order）
  - truth-conflict-map.md: Legacy 冲突点（金额单位双轨、product_id 错误语义）
status: CONFIRMED
```

**验证依据**:
- `legacy-map.md`: 8个 Legacy 数据库表、8个 Legacy 服务实现、13个 Legacy 控制器
- `business-object-map.md`: Legacy 对象（food, product, orders_legacy, voucher_header, sales_order）
- `truth-conflict-map.md`: Legacy 冲突点（冲突1-冲突10）

---

### ARCH-LEGACY-002: Legacy 数据迁移必须有明确的 Truth Source 切换点

```yaml
principle_id: ARCH-LEGACY-002
title: Legacy 迁移切换点
description: Legacy 数据迁移必须有明确的 Truth Source 切换点，切换点前后数据必须保持一致
category: LEGACY
rationale: |
  无明确切换点会导致：数据不一致、业务中断、回滚困难。
  本项目存在多个迁移场景（food→foods, stores→stores_new, account_balance→accounting_subjects）
implications:
  - 迁移计划必须明确切换点时间
  - 切换点前后必须有数据校验机制
  - 必须有回滚方案
  - 切换点后必须废弃旧 Truth Source
evidence:
  - truth-conflict-map.md: 冲突点裁决和状态
  - master-data-source-map.md: Legacy Source 标记
  - master-data-source-map.md §6: 数据源一致性风险
status: CONFIRMED
```

**验证依据**:
- `truth-conflict-map.md`: 冲突3（科目余额双轨）已裁决，accounting_subjects 为准
- `truth-conflict-map.md`: 冲突4（stores/stores_new）迁移中
- `master-data-source-map.md` §6: 数据源一致性风险（Dual-Write, Amount Unit Conflict）

---

## 十、原则状态汇总

| Principle ID | 状态 | 验证依据 |
|--------------|------|----------|
| ARCH-TRUTH-001 | CONFIRMED | truth-conflict-map.md, master-data-source-map.md |
| ARCH-TRUTH-002 | CONFIRMED | business-object-map.md, cross-domain-coordination-matrix.md |
| ARCH-OWN-001 | CONFIRMED | cross-domain-coordination-matrix.md, data-lineage-map.md |
| ARCH-OWN-002 | CONFIRMED | cross-domain-coordination-matrix.md, business-identity-propagation-map.md |
| ARCH-WRITE-001 | CONFIRMED | business-object-map.md, legacy-map.md |
| ARCH-WRITE-002 | CONFIRMED | cross-domain-coordination-matrix.md, event-job-map.md |
| ARCH-ENTRY-001 | CONFIRMED | cross-domain-coordination-matrix.md, business-object-map.md |
| ARCH-ENTRY-002 | CONFIRMED | cross-domain-coordination-matrix.md, data-lineage-map.md |
| ARCH-MASTER-001 | CONFIRMED | master-data-source-map.md, business-object-map.md |
| ARCH-MASTER-002 | CONFIRMED | master-data-source-map.md, data-lineage-map.md |
| ARCH-ID-001 | CONFIRMED | business-identity-propagation-map.md |
| ARCH-ID-002 | CONFIRMED | business-identity-propagation-map.md |
| ARCH-CROSS-001 | CONFIRMED | cross-domain-coordination-matrix.md, data-lineage-map.md |
| ARCH-CROSS-002 | CONFIRMED | cross-domain-coordination-matrix.md, data-lineage-map.md |
| ARCH-LEGACY-001 | CONFIRMED | legacy-map.md, business-object-map.md |
| ARCH-LEGACY-002 | CONFIRMED | truth-conflict-map.md, master-data-source-map.md |

---

## 十一、维护说明

1. 本文件随架构评审更新而更新
2. 新增原则必须经过评审并记录验证依据
3. 原则状态变更（如 CONFIRMED→VIOLATED）必须更新本文件
4. 原则违反必须记录在 systemic-problem-register.md 中

---

*生成时间: 2026-09-09*
*验证依据: Project Master Map 系列文档*
*数据来源: 代码库静态分析 + 地图文件交叉验证*
