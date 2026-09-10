# Decision Baseline Recovery — Remediation Gate

> **报告编号**: DBR-GATE-001
> **生成日期**: 2026-09-09
> **版本**: 1.0.0
> **基于**: verified-locked-decisions.md, open-product-decisions.md, open-architecture-decisions.md, invalidated-decisions.md, engineering-only-decisions.md, decision-provenance-matrix.md
> **关键原则**: 绝不自动确认业务决策 | 只保留有充分证据支持的决策 | 废止未经证明的结论

---

## 一、状态重标摘要

### 1.1 全局状态分布

| 状态 | 数量 | 占比 | 说明 |
|------|------|------|------|
| **ENGINEERING_READY** | 12 | 40% | 基于 VERIFIED LOCK / CONFIRMED DEC，可立即执行 |
| **WAITING_PRODUCT_DECISION** | 3 | 10% | 等待 DEC-004 / DEC-006 产品决策 |
| **WAITING_ARCHITECTURE_DECISION** | 4 | 13% | 等待 DEC-010 / DEC-011 / DEC-013 / DEC-014 架构决策 |
| **BLOCKED** | 8 | 27% | 被其他 Decision 或 SR 阻断 |
| **INVALIDATED** | 3 | 10% | 基于废止的 LOCK/DEC，不再执行 |
| **总计** | **30** | 100% | |

### 1.2 状态定义

| 状态 | 定义 | 可执行性 |
|------|------|---------|
| **ENGINEERING_READY** | 已有充分证据支撑，方案明确，无上游未决依赖 | ✅ 立即可执行 |
| **WAITING_PRODUCT_DECISION** | 等待 Product Owner 确认业务决策 | ❌ 需 PO 确认 |
| **WAITING_ARCHITECTURE_DECISION** | 等待 Architecture Owner 确认技术决策 | ❌ 需架构师确认 |
| **BLOCKED** | 被其他未完成的 Decision 或 SR 阻断 | ❌ 需等待前置完成 |
| **INVALIDATED** | 基于废止的 LOCK/DEC，或与代码事实冲突 | ⛔ 不再执行 |

---

## 二、逐项重标结果

### 2.1 ENGINEERING_READY — 可立即执行

---

#### ER-001: foods 菜品真相源执行

```yaml
id: ER-001
name: foods 菜品真相源 — API/Service 层规范化
status: ENGINEERING_READY
priority: P0
decision_source: LOCK-A001 (VERIFIED)

evidence:
  - DB: foods 表为所有菜品操作唯一入口 (V6.0.0)
  - Code: FoodService.java, src/models/food.py
  - API: GET/POST/PUT /api/v1/foods

resolution_basis: >
  LOCK-A001 已 VERIFIED，foods 为菜品唯一真相源。
  禁止在其他表冗余存储菜品基础信息。

engineering_tasks:
  - 排查所有菜品数据冗余存储点
  - 统一通过 foods 表关联菜品信息
  - 确保订单/库存/报表模块引用 food_id
```

---

#### ER-002: material_archives 物料真相源执行

```yaml
id: ER-002
name: material_archives 物料真相源 — API/Service 层规范化
status: ENGINEERING_READY
priority: P0
decision_source: LOCK-A002 (VERIFIED)

evidence:
  - DB: material_archives 表被所有物料业务引用 (V5.0.0)
  - Code: MaterialService.java, src/models/material.py
  - API: GET/POST /api/v1/materials

resolution_basis: >
  LOCK-A002 已 VERIFIED，material_archives 为物料唯一真相源。
  库存/采购/成本核算模块引用物料时必须通过 material_id。

engineering_tasks:
  - 排查所有物料数据冗余存储点
  - 统一通过 material_archives 表关联物料信息
  - 确保库存/采购/生产模块引用 material_id
```

---

#### ER-003: suppliers 供应商真相源执行

```yaml
id: ER-003
name: suppliers 供应商真相源 — API/Service 层规范化
status: ENGINEERING_READY
priority: P0
decision_source: LOCK-A004 (VERIFIED)

evidence:
  - DB: suppliers 表 (V3.0.0)
  - Code: SupplierService.java
  - API: GET/POST /api/v1/suppliers

resolution_basis: >
  LOCK-A004 已 VERIFIED，suppliers 为供应商唯一真相源。
  采购/应付账款模块引用供应商时必须通过 supplier_id。

engineering_tasks:
  - 确保供应商状态变更同步至采购和财务模块
  - 验证供应商合并/拆分数据迁移方案
```

---

#### ER-004: employees 员工真相源执行

```yaml
id: ER-004
name: employees 员工真相源 — API/Service 层规范化
status: ENGINEERING_READY
priority: P0
decision_source: LOCK-A005 (VERIFIED)

evidence:
  - DB: employees 表 (V2.0.0)
  - Code: EmployeeService.java
  - API: GET/POST /api/v1/employees

resolution_basis: >
  LOCK-A005 已 VERIFIED，employees 为员工唯一真相源。
  考勤/薪资/权限模块引用员工时必须通过 employee_id。

engineering_tasks:
  - 验证员工状态变更同步机制
  - 确保历史员工数据可追溯
```

---

#### ER-005: stores_new 门店真相源执行

```yaml
id: ER-005
name: stores_new 门店真相源 — API/Service 层规范化
status: ENGINEERING_READY
priority: P0
decision_source: LOCK-A006 (VERIFIED)

evidence:
  - DB: stores_new 表 (V4.0.0)
  - Code: StoreService.java
  - API: GET/POST /api/v1/stores

resolution_basis: >
  LOCK-A006 已 VERIFIED，stores_new 为门店唯一真相源。
  订单/库存/报表模块引用门店时必须通过 store_id。

engineering_tasks:
  - 确保门店状态变更同步至运营/财务/供应链
  - 验证门店合并/拆分数据迁移方案
```

---

#### ER-006: departments/positions 组织真相源执行

```yaml
id: ER-006
name: departments/positions 组织真相源 — API/Service 层规范化
status: ENGINEERING_READY
priority: P0
decision_source: LOCK-A007 (VERIFIED), LOCK-A008 (VERIFIED)

evidence:
  - DB: departments 表 (V1.0.0), positions 表 (V1.0.0)
  - Code: DepartmentService.java, PositionService.java
  - API: GET/POST /api/v1/departments, /api/v1/positions

resolution_basis: >
  LOCK-A007 和 LOCK-A008 均已 VERIFIED。
  组织架构信息以 departments 为唯一真相源，职位信息以 positions 为唯一真相源。

engineering_tasks:
  - 验证部门/职位变更同步机制
  - 确保审批流程和报表结构与组织架构一致
```

---

#### ER-007: accounting_subjects 会计科目真相源执行

```yaml
id: ER-007
name: accounting_subjects 会计科目真相源 — API/Service 层规范化
status: ENGINEERING_READY
priority: P0
decision_source: LOCK-A009 (VERIFIED)

evidence:
  - DB: accounting_subjects 表 (V7.0.0)
  - Code: AccountingSubjectService.java
  - API: GET/POST /api/v1/accounting_subjects

resolution_basis: >
  LOCK-A009 已 VERIFIED，accounting_subjects 为会计科目唯一真相源。
  财务记账/报表/成本核算模块引用科目时必须通过 subject_id。

engineering_tasks:
  - 验证科目新增/修改/废止审批流程
  - 确保科目体系变更历史数据可追溯
```

---

#### ER-008: roles/permissions 权限真相源执行

```yaml
id: ER-008
name: roles/permissions 权限真相源 — RBAC 规范化
status: ENGINEERING_READY
priority: P0
decision_source: LOCK-A010 (VERIFIED)

evidence:
  - DB: roles, permissions, role_permissions, user_roles 表 (V1.0.0)
  - Code: RBAC 模型, PermissionVerifyService
  - API: GET/POST /api/v1/roles, /api/v1/permissions

resolution_basis: >
  LOCK-A010 已 VERIFIED，roles/permissions 为权限唯一真相源。
  用户授权/菜单控制/API 访问控制必须基于此定义。

engineering_tasks:
  - 验证权限校验中间件完整性
  - 确保 180+ 权限码覆盖所有 API 端点
  - 完善数据权限范围控制
```

---

#### ER-009: 事件驱动基础执行

```yaml
id: ER-009
name: 事件驱动基础 — WebSocket + Event Outbox
status: ENGINEERING_READY
priority: P0
decision_source: LOCK-B001 (VERIFIED)

evidence:
  - Code: websocket_manager.py, event_dispatcher.py, event_outbox 模型
  - DB: event_outbox 表
  - Config: config/event_bus.yaml

resolution_basis: >
  LOCK-B001 已 VERIFIED，事件驱动架构为系统核心通信模式。
  22 个领域事件 + 10 个监听器已实现。

engineering_tasks:
  - 完善 Event Outbox 可靠投递机制
  - 补充事件丢失重试逻辑
  - 接入 AutoVoucherService 到事件监听器
```

---

#### ER-010: PostgreSQL 数据库规范执行

```yaml
id: ER-010
name: PostgreSQL 数据库规范 — 主存储规范化
status: ENGINEERING_READY
priority: P0
decision_source: LOCK-B002 (VERIFIED)

evidence:
  - DB: 340+ 张业务表
  - Config: backend/pom.xml: postgresql 依赖
  - Migration: 160 个 Flyway 迁移文件

resolution_basis: >
  LOCK-B002 已 VERIFIED，PostgreSQL 为系统主数据库。
  利用 JSONB、全文搜索、事务等特性支持复杂业务需求。

engineering_tasks:
  - 规范化数据库迁移流程
  - 制定大表索引和分区策略
  - 建立读写分离评估机制
```

---

#### ER-011: OAuth2+JWT 认证执行

```yaml
id: ER-011
name: OAuth2+JWT 认证 — 安全基线执行
status: ENGINEERING_READY
priority: P0
decision_source: LOCK-B004 (VERIFIED)

evidence:
  - Code: jjwt-api:0.12.3, SecurityConfig, JwtUtil
  - API: /v1/auth/login, /v1/auth/refresh
  - Migration: V8.0.0 (JWT 配置)

resolution_basis: >
  LOCK-B004 已 VERIFIED，OAuth2+JWT 为系统认证授权方案。
  所有需要认证的 API 均需携带 JWT Bearer Token。

engineering_tasks:
  - 完善 JWT 密钥轮换机制
  - 实现多设备登录管理
  - 建立令牌黑名单（可选）
```

---

#### ER-012: RESTful API 规范执行

```yaml
id: ER-012
name: RESTful API 规范 — 接口标准化
status: ENGINEERING_READY
priority: P0
decision_source: LOCK-B006 (VERIFIED)

evidence:
  - Code: 154 个 Controller, RESTful 风格
  - API: /v1/** REST 端点
  - Doc: docs/api/openapi.yaml (OpenAPI 3.0)

resolution_basis: >
  LOCK-B006 已 VERIFIED，RESTful 为系统对外接口标准。
  遵循资源导向设计、统一状态码、分页、过滤、排序等约定。

engineering_tasks:
  - 验证 API 版本管理策略
  - 确保所有端点符合 RESTful 规范
  - 完善 OpenAPI 文档
```

---

### 2.2 WAITING_PRODUCT_DECISION — 等待产品决策

---

#### WP-001: Customer/Member 关系

```yaml
id: WP-001
name: Customer/Member 关系定义
status: WAITING_PRODUCT_DECISION
priority: P0
waiting_for: DEC-004
deadline: 2026-09-30
responsible: Product Owner

evidence:
  - 当前状态: Customer 与 Member 概念混用，散客消费无法追踪
  - 推荐方案: Option C — Guest Member（散客类型）
  - 推荐理由: 改动最小 (15-25 人天)，快速见效，风险可控

blocked_items:
  - SR-010 (Customer Foundation)
  - 用户域积分/等级功能
  - 客户画像完整性

resolution_path: >
  1. 等待 Product Owner 确认 Guest Member 方案
  2. 确认后制定数据迁移计划
  3. 实施 member_type 字段扩展
```

---

#### WP-002: Product/Food/Material 边界

```yaml
id: WP-002
name: Product/Food/Material 边界
status: WAITING_PRODUCT_DECISION
priority: P1
waiting_for: DEC-006
deadline: 2026-10-15
responsible: Product Owner

evidence:
  - 当前状态: Product/Food/Material 三者边界模糊，数据冗余严重
  - 推荐方案: Option C — 废弃 Product，保留 Food + Material
  - 推荐理由: 概念清晰，事件驱动友好，维护成本降低

blocked_items:
  - SR-001 (商品 Identity 统一)
  - 多态数据建模
  - 数据迁移

resolution_path: >
  1. 等待 PO 与架构联合确认 Food + Material 方案
  2. 确认后制定数据迁移计划
  3. 废弃 Product 表，保留历史数据
```

---

#### WP-003: 废止的 V2 结论清理

```yaml
id: WP-003
name: V2 废止结论清理
status: WAITING_PRODUCT_DECISION
priority: P2
waiting_for: PO 确认清理范围
deadline: 2026-10-15
responsible: Product Owner

evidence:
  - DEC-008 (Amount/Money) 被驳回
  - IMPLICIT-001~006 被 SUBSUMED
  - 多套 V2 基线冲突结论

blocked_items:
  - 文档一致性维护

resolution_path: >
  1. 确认废止结论范围
  2. 清理相关文档引用
  3. 更新决策注册表
```

---

### 2.3 WAITING_ARCHITECTURE_DECISION — 等待架构决策

---

#### WA-001: 跨域数据访问模式

```yaml
id: WA-001
name: 跨域数据访问模式
status: WAITING_ARCHITECTURE_DECISION
priority: P0
waiting_for: DEC-010
deadline: 2026-09-30
responsible: Architecture Owner

evidence:
  - 当前状态: 部分服务直接读其他域数据库，数据所有权边界模糊
  - 推荐方案: Option A — API 契约（禁止直接数据库访问）
  - 推荐理由: 数据所有权清晰，访问可控，易于监控

blocked_items:
  - SR-012 (跨域数据所有权)
  - SR-013 (事件驱动可靠性)
  - 所有微服务跨域访问重构

resolution_path: >
  1. 等待 Architecture Owner 确认 API 契约方案
  2. 确认后制定迁移计划 (120-180 人天)
  3. 重构所有跨域直接数据库访问为 API 调用
```

---

#### WA-002: 事件驱动架构适用范围

```yaml
id: WA-002
name: 事件驱动架构适用范围
status: WAITING_ARCHITECTURE_DECISION
priority: P0
waiting_for: DEC-011
deadline: 2026-09-30
responsible: Architecture Owner

evidence:
  - 当前状态: 部分模块已引入 Event Store，部分仍用传统 CRUD
  - 推荐方案: Option B — 核心域事件驱动（订单/库存/支付）
  - 推荐理由: 符合 LOCK-001，平衡成本和解耦

blocked_items:
  - SR-013 (事件驱动可靠性)
  - 核心域服务重构
  - 架构风格统一

resolution_path: >
  1. 等待 Architecture Owner 确认核心域事件驱动方案
  2. 确认后制定迁移计划 (80-120 人天)
  3. 重构核心域服务为事件驱动架构
```

---

#### WA-003: 裸接口权限归属

```yaml
id: WA-003
name: 裸接口权限归属
status: WAITING_ARCHITECTURE_DECISION
priority: P1
waiting_for: DEC-013
deadline: 2026-10-15
responsible: Security Team

evidence:
  - 当前状态: 部分服务有 mTLS，部分无任何认证
  - 推荐方案: Option A — 全面认证（mTLS + JWT）
  - 推荐理由: 安全性高，与 LOCK-004 兼容

blocked_items:
  - SR-010 (权限中间件重构)
  - 内部接口安全基线

resolution_path: >
  1. 等待安全组确认全面认证方案
  2. 确认后制定迁移计划 (40-60 人天)
  3. 为所有内部接口添加 mTLS + JWT 认证
```

---

#### WA-004: 供应商 H5 认证

```yaml
id: WA-004
name: 供应商 H5 认证
status: WAITING_ARCHITECTURE_DECISION
priority: P1
waiting_for: DEC-014
deadline: 2026-10-15
responsible: Security Team

evidence:
  - 当前状态: 两种原型方案并存，尚未最终确定
  - 推荐方案: Option B — iframe 嵌入（共享主站认证）
  - 推荐理由: 用户体验好，认证简单，成本最低

blocked_items:
  - SR-012 (H5 集成适配层)
  - 供应商 H5 安全集成

resolution_path: >
  1. 等待安全组确认 iframe 嵌入方案
  2. 确认后实施安全措施 (CSP, X-Frame-Options 等)
  3. 适配 postMessage 通信协议
```

---

### 2.4 BLOCKED — 被阻断

---

#### BL-001: 统一 Product/Material/Food Identity

```yaml
id: BL-001
name: 统一 Product/Material/Food Identity
status: BLOCKED
priority: P0

blocked_by:
  - DEC-006 (需修订): Product-Food-Material 关系边界需细化
  - DEC-004 (PENDING): Customer/Member 关系待确认

resolution_path: >
  1. 等待 DEC-006 修订版确认 Food + Material 边界
  2. 等待 DEC-004 确认 Customer/Member 关系
  3. 两者完成后可进入 ENGINEERING_READY
```

---

#### BL-002: 统一订单状态机

```yaml
id: BL-002
name: 统一订单状态机
status: BLOCKED
priority: P1

blocked_by:
  - BL-001 (商品 Identity 统一): 订单依赖商品身份
  - WA-001 (跨域数据所有权): 订单多端写入需先明确所有权

resolution_path: >
  1. 等待 BL-001 完成
  2. 等待 WA-001 完成
  3. 两者完成后可进入 ENGINEERING_READY
```

---

#### BL-003: 解决 inventory/store_inventory 双写

```yaml
id: BL-003
name: 解决 inventory/store_inventory 双写
status: BLOCKED
priority: P1

blocked_by:
  - BL-001 (商品 Identity 统一): 库存依赖商品身份
  - WP-002 (Product/Food/Material 边界): 库存依赖商品边界明确

resolution_path: >
  1. 等待 BL-001 完成
  2. 等待 WP-002 完成
  3. 两者完成后可进入 ENGINEERING_READY
```

---

#### BL-004: 解决成本双写

```yaml
id: BL-004
name: 解决成本双写
status: BLOCKED
priority: P1

blocked_by:
  - BL-001 (商品 Identity 统一): 成本依赖商品身份
  - ER-003 (金额单位统一 — 待执行): 成本依赖金额单位

resolution_path: >
  1. 等待 ER-003 执行完成
  2. 等待 BL-001 完成
  3. 两者完成后可进入 ENGINEERING_READY
```

---

#### BL-005: 加强事件驱动可靠性

```yaml
id: BL-005
name: 加强事件驱动可靠性
status: BLOCKED
priority: P1

blocked_by:
  - WA-001 (跨域数据访问): 事件依赖所有权明确
  - WA-002 (事件驱动范围): 事件架构待决策
  - BL-002 (订单状态机): 事件监听依赖订单状态
  - ER-009 (事件驱动基础 — 待执行): 凭证生成依赖金额单位

resolution_path: >
  1. 等待 WA-001 完成
  2. 等待 WA-002 完成
  3. 等待 BL-002 完成
  4. 等待 ER-009 执行
  5. 全部完成后可进入 ENGINEERING_READY
```

---

#### BL-006: 解决多端写入冲突

```yaml
id: BL-006
name: 解决多端写入冲突
status: BLOCKED
priority: P0

blocked_by:
  - WA-001 (跨域数据访问): 多端写入依赖所有权明确
  - BL-002 (订单状态机): 多端写入依赖状态机统一

resolution_path: >
  1. 等待 WA-001 完成
  2. 等待 BL-002 完成
  3. 两者完成后可进入 ENGINEERING_READY
```

---

#### BL-007: 清理 Legacy 数据库表

```yaml
id: BL-007
name: 清理 Legacy 数据库表
status: BLOCKED
priority: P2

blocked_by:
  - BL-001 (商品 Identity 统一): 遗留表清理依赖商品身份统一
  - BL-002 (订单状态机): 遗留表清理依赖订单状态统一
  - ER-003 (金额单位统一 — 待执行): 遗留表清理依赖金额单位统一

resolution_path: >
  1. 等待 BL-001 完成
  2. 等待 BL-002 完成
  3. 等待 ER-003 执行完成
  4. 全部完成后可进入 ENGINEERING_READY
```

---

#### BL-008: 清理 Legacy 服务 + Dead Code

```yaml
id: BL-008
name: 清理 Legacy 服务 + Dead Code
status: BLOCKED
priority: P2

blocked_by:
  - BL-007 (遗留表清理): 服务清理依赖表清理

resolution_path: >
  1. 等待 BL-007 完成
  2. 完成后可进入 ENGINEERING_READY
```

---

### 2.5 INVALIDATED — 已废止

---

#### IV-001: V2 技术栈决策 (React/Redux/Router/AntDesign)

```yaml
id: IV-001
name: V2 技术栈决策 (React/Redux/Router/AntDesign)
status: INVALIDATED
priority: N/A

invalidation_reason: >
  代码仓库实际使用 Vue.js 3.5.x + Pinia + Vue Router + Element Plus，
  与 V2 基线假设的 React + TypeScript + Redux + Ant Design 完全冲突。

evidence:
  - frontend/package.json: vue ^3.5.32, pinia ^3.0.4, vue-router ^5.0.4, element-plus ^2.13.7
  - 无 React, Redux, React Router, Ant Design 依赖

impact: 无 — 已由实际代码事实替代
```

---

#### IV-002: V2 基础设施决策 (K8s/ELK/GitHub Actions/Redis/RocketMQ)

```yaml
id: IV-002
name: V2 基础设施决策 (K8s/ELK/GitHub Actions/Redis/RocketMQ)
status: INVALIDATED
priority: N/A

invalidation_reason: >
  代码仓库中无 K8s 配置、无 ELK 配置、无 GitHub Actions 工作流、
  无 Redis 依赖、无 RocketMQ 依赖。实际使用 Docker Compose、Logback、
  ConcurrentHashMap。

evidence:
  - 存在: docker-compose.yml (非 K8s)
  - 存在: Logback 配置 (非 ELK)
  - 缺失: .github/workflows/
  - 缺失: Redis 依赖
  - 缺失: RocketMQ 依赖

impact: 无 — 已由实际代码事实替代
```

---

#### IV-003: V2 DEC 废止决策 (Redis/Winston/RabbitMQ/Prisma)

```yaml
id: IV-003
name: V2 DEC 废止决策 (Redis/Winston/RabbitMQ/Prisma)
status: INVALIDATED
priority: N/A

invalidation_reason: >
  DEC-003 (缓存策略-Redis): 无 Redis 依赖
  DEC-004 (日志方案-Winston): 实际为 Logback
  DEC-005 (消息队列-RabbitMQ): 无 RabbitMQ 依赖
  DEC-007 (ORM方案-Prisma): 实际为 MyBatis-Plus

evidence:
  - 代码库中无对应技术依赖
  - 实际使用替代方案

impact: 无 — 已由实际代码事实替代
```

---

## 三、Engineering Readiness Matrix

### 3.1 可立即工程化 (ENGINEERING_READY)

| 编号 | 名称 | 优先级 | 决策来源 | 方案 | 预计工时 |
|------|------|--------|---------|------|---------|
| **ER-001** | foods 菜品真相源 | P0 | LOCK-A001 | API/Service 规范化 | 5-8 天 |
| **ER-002** | material_archives 物料真相源 | P0 | LOCK-A002 | API/Service 规范化 | 5-8 天 |
| **ER-003** | suppliers 供应商真相源 | P0 | LOCK-A004 | API/Service 规范化 | 3-5 天 |
| **ER-004** | employees 员工真相源 | P0 | LOCK-A005 | API/Service 规范化 | 3-5 天 |
| **ER-005** | stores_new 门店真相源 | P0 | LOCK-A006 | API/Service 规范化 | 3-5 天 |
| **ER-006** | departments/positions 组织真相源 | P0 | LOCK-A007/008 | API/Service 规范化 | 3-5 天 |
| **ER-007** | accounting_subjects 会计科目真相源 | P0 | LOCK-A009 | API/Service 规范化 | 3-5 天 |
| **ER-008** | roles/permissions 权限真相源 | P0 | LOCK-A010 | RBAC 规范化 | 5-8 天 |
| **ER-009** | 事件驱动基础 | P0 | LOCK-B001 | WebSocket + Event Outbox | 5-8 天 |
| **ER-010** | PostgreSQL 数据库规范 | P0 | LOCK-B002 | 主存储规范化 | 3-5 天 |
| **ER-011** | OAuth2+JWT 认证 | P0 | LOCK-B004 | 安全基线执行 | 5-8 天 |
| **ER-012** | RESTful API 规范 | P0 | LOCK-B006 | 接口标准化 | 3-5 天 |

**ENGINEERING_READY 总工时**: 46-78 人天

### 3.2 等待产品决策 (WAITING_PRODUCT_DECISION)

| 编号 | 名称 | 优先级 | 阻塞 Decision | 推荐方案 | Deadline |
|------|------|--------|--------------|---------|----------|
| **WP-001** | Customer/Member 关系 | P0 | DEC-004 | Guest Member（散客类型） | 2026-09-30 |
| **WP-002** | Product/Food/Material 边界 | P1 | DEC-006 | 废弃 Product，保留 Food + Material | 2026-10-15 |
| **WP-003** | V2 废止结论清理 | P2 | PO 确认 | 文档清理 | 2026-10-15 |

**WAITING_PRODUCT_DECISION 总阻塞项**: 3 项

### 3.3 等待架构决策 (WAITING_ARCHITECTURE_DECISION)

| 编号 | 名称 | 优先级 | 阻塞 Decision | 推荐方案 | Deadline |
|------|------|--------|--------------|---------|----------|
| **WA-001** | 跨域数据访问模式 | P0 | DEC-010 | API 契约（禁止直接 DB 访问） | 2026-09-30 |
| **WA-002** | 事件驱动架构适用范围 | P0 | DEC-011 | 核心域事件驱动 | 2026-09-30 |
| **WA-003** | 裸接口权限归属 | P1 | DEC-013 | 全面认证（mTLS + JWT） | 2026-10-15 |
| **WA-004** | 供应商 H5 认证 | P1 | DEC-014 | iframe 嵌入 | 2026-10-15 |

**WAITING_ARCHITECTURE_DECISION 总阻塞项**: 4 项

### 3.4 被阻断 (BLOCKED)

| 编号 | 名称 | 优先级 | 阻塞依赖 |
|------|------|--------|---------|
| **BL-001** | 统一 Product/Material/Food Identity | P0 | DEC-006 + DEC-004 |
| **BL-002** | 统一订单状态机 | P1 | BL-001 + WA-001 |
| **BL-003** | 解决 inventory/store_inventory 双写 | P1 | BL-001 + WP-002 |
| **BL-004** | 解决成本双写 | P1 | BL-001 + ER-003 |
| **BL-005** | 加强事件驱动可靠性 | P1 | WA-001 + WA-002 + BL-002 + ER-009 |
| **BL-006** | 解决多端写入冲突 | P0 | WA-001 + BL-002 |
| **BL-007** | 清理 Legacy 数据库表 | P2 | BL-001 + BL-002 + ER-003 |
| **BL-008** | 清理 Legacy 服务 + Dead Code | P2 | BL-007 |

**BLOCKED 总阻塞项**: 8 项

### 3.5 已废止 (INVALIDATED)

| 编号 | 名称 | 废止原因 | 影响等级 |
|------|------|---------|---------|
| **IV-001** | V2 技术栈决策 | React vs Vue.js 代码冲突 | CRITICAL |
| **IV-002** | V2 基础设施决策 | K8s/ELK/Redis/RocketMQ 无代码证据 | HIGH |
| **IV-003** | V2 DEC 废止决策 | Redis/Winston/RabbitMQ/Prisma 无代码证据 | HIGH |

**INVALIDATED 总废止项**: 3 项

---

## 四、阻塞依赖链可视化

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    Decision Baseline Recovery — 阻塞依赖链                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  LAYER 1: LOCKED (不可变锚点 — 14 项 VERIFIED)                                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐              │
│  │LOCK-A001 │ │LOCK-A002 │ │LOCK-A004 │ │LOCK-A005 │ │LOCK-A006 │              │
│  │ foods    │ │material  │ │suppliers │ │employees │ │stores_new│              │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘              │
│       │            │            │            │            │                      │
│  ┌────┴─────┐ ┌────┴─────┐ ┌───┴──────┐ ┌───┴──────┐ ┌───┴──────┐              │
│  │LOCK-A007 │ │LOCK-A008 │ │LOCK-A009 │ │LOCK-A010 │ │LOCK-B001 │              │
│  │departments│ │positions │ │accounting│ │roles/perm│ │  EDA     │              │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └────┬─────┘              │
│                                                             │                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                    │                    │
│  │LOCK-B002 │ │LOCK-B004 │ │LOCK-B006 │                    │                    │
│  │PostgreSQL│ │OAuth2+JWT│ │RESTful   │                    │                    │
│  └──────────┘ └──────────┘ └──────────┘                    │                    │
│                                                             │                    │
│  ═══════════════════════════════════════════════════════════════════════════════│
│  LAYER 2: ENGINEERING_READY (12 项 — 可立即执行)                                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐              │
│  │ ER-001   │ │ ER-002   │ │ ER-003   │ │ ER-004   │ │ ER-005   │              │
│  │ foods    │ │material  │ │suppliers │ │employees │ │stores_new│              │
│  │ ✅ READY │ │ ✅ READY │ │ ✅ READY │ │ ✅ READY │ │ ✅ READY │              │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐              │
│  │ ER-006   │ │ ER-007   │ │ ER-008   │ │ ER-009   │ │ ER-010   │              │
│  │ org      │ │accounting│ │roles/perm│ │  EDA     │ │PostgreSQL│              │
│  │ ✅ READY │ │ ✅ READY │ │ ✅ READY │ │ ✅ READY │ │ ✅ READY │              │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘              │
│  ┌──────────┐ ┌──────────┐                                                     │
│  │ ER-011   │ │ ER-012   │                                                     │
│  │OAuth2+JWT│ │RESTful   │                                                     │
│  │ ✅ READY │ │ ✅ READY │                                                     │
│  └──────────┘ └──────────┘                                                     │
│                                                                                 │
│  ═══════════════════════════════════════════════════════════════════════════════│
│  LAYER 3: WAITING (7 项 — 等待决策)                                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                                       │
│  │ WP-001   │ │ WP-002   │ │ WP-003   │                                       │
│  │Customer  │ │ PFM边界  │ │V2清理    │                                       │
│  │⏳ DEC-004│ │⏳ DEC-006│ │⏳ PO确认 │                                       │
│  └────┬─────┘ └────┬─────┘ └──────────┘                                       │
│       │            │                                                            │
│  ┌────┴─────┐ ┌────┴─────┐ ┌──────────┐ ┌──────────┐                          │
│  │ WA-001   │ │ WA-002   │ │ WA-003   │ │ WA-004   │                          │
│  │跨域访问   │ │事件范围  │ │裸接口权限│ │供应商H5  │                          │
│  │⏳ DEC-010│ │⏳ DEC-011│ │⏳ DEC-013│ │⏳ DEC-014│                          │
│  └────┬─────┘ └────┬─────┘ └──────────┘ └──────────┘                          │
│       │            │                                                            │
│       └────────────┼──────────────────────────────────────┐                    │
│                    │                                      │                    │
│  ═══════════════════════════════════════════════════════════════════════════════│
│  LAYER 4: BLOCKED (8 项 — 被阻断)                                               │
│  ┌────────────────▼────────────────┐                                           │
│  │           BL-001                │                                           │
│  │    商品 Identity 统一           │                                           │
│  │    ⏳ DEC-006 + DEC-004         │                                           │
│  └────────────────┬────────────────┘                                           │
│                   │                                                             │
│       ┌───────────┼───────────┬───────────┐                                    │
│       ▼           ▼           ▼           ▼                                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                          │
│  │ BL-002   │ │ BL-003   │ │ BL-004   │ │ BL-006   │                          │
│  │ 订单状态 │ │inv/store │ │ 成本双写 │ │ 多端写入 │                          │
│  │ ❌ BLOCKED│ │ ❌ BLOCKED│ │ ❌ BLOCKED│ │ ❌ BLOCKED│                          │
│  └────┬─────┘ └──────────┘ └──────────┘ └──────────┘                          │
│       │                                                                        │
│       └────────────────┬──────────────────────────────────┐                    │
│                        ▼                                  ▼                    │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐                         │
│  │ BL-005   │ │ BL-007   │ │ BL-008   │ │          │                         │
│  │ 事件可靠 │ │ 遗留表   │ │ 遗留服务 │ │          │                         │
│  │ ❌ BLOCKED│ │ ❌ BLOCKED│ │ ❌ BLOCKED│ │          │                         │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘                         │
│                                                                                 │
│  ═══════════════════════════════════════════════════════════════════════════════│
│  LAYER 5: INVALIDATED (3 项 — 已废止)                                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                                       │
│  │ IV-001   │ │ IV-002   │ │ IV-003   │                                       │
│  │V2技术栈  │ │V2基础设施│ │V2 DEC    │                                       │
│  │⛔ 废止   │ │⛔ 废止   │ │⛔ 废止   │                                       │
│  └──────────┘ └──────────┘ └──────────┘                                       │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 五、执行路径建议

### Phase 0: 立即执行 (ENGINEERING_READY)

| 序号 | 编号 | 任务 | 基于 LOCK | 预计工时 |
|------|------|------|----------|---------|
| 1 | ER-001 | foods 菜品真相源规范化 | LOCK-A001 | 5-8 天 |
| 2 | ER-002 | material_archives 物料真相源规范化 | LOCK-A002 | 5-8 天 |
| 3 | ER-003 | suppliers 供应商真相源规范化 | LOCK-A004 | 3-5 天 |
| 4 | ER-004 | employees 员工真相源规范化 | LOCK-A005 | 3-5 天 |
| 5 | ER-005 | stores_new 门店真相源规范化 | LOCK-A006 | 3-5 天 |
| 6 | ER-006 | departments/positions 组织真相源规范化 | LOCK-A007/008 | 3-5 天 |
| 7 | ER-007 | accounting_subjects 会计科目真相源规范化 | LOCK-A009 | 3-5 天 |
| 8 | ER-008 | roles/permissions 权限真相源规范化 | LOCK-A010 | 5-8 天 |
| 9 | ER-009 | 事件驱动基础完善 | LOCK-B001 | 5-8 天 |
| 10 | ER-010 | PostgreSQL 数据库规范 | LOCK-B002 | 3-5 天 |
| 11 | ER-011 | OAuth2+JWT 认证完善 | LOCK-B004 | 5-8 天 |
| 12 | ER-012 | RESTful API 规范落地 | LOCK-B006 | 3-5 天 |

**Phase 0 总工时**: 46-78 人天

### Phase 1: 等待决策完成后执行

| 序号 | 编号 | 任务 | 前置条件 | 预计启动时间 |
|------|------|------|---------|-------------|
| 1 | WP-001 | Customer/Member 关系实施 | DEC-004 确认 | 2026-09-30 后 |
| 2 | WP-002 | Product/Food/Material 边界实施 | DEC-006 确认 | 2026-10-15 后 |
| 3 | WA-001 | 跨域数据访问重构 | DEC-010 确认 | 2026-09-30 后 |
| 4 | WA-002 | 事件驱动范围实施 | DEC-011 确认 | 2026-09-30 后 |
| 5 | WA-003 | 裸接口权限实施 | DEC-013 确认 | 2026-10-15 后 |
| 6 | WA-004 | 供应商 H5 集成 | DEC-014 确认 | 2026-10-15 后 |

### Phase 2: 依赖链解锁后执行

| 序号 | 编号 | 任务 | 前置条件 | 预计启动时间 |
|------|------|------|---------|-------------|
| 1 | BL-001 | 商品 Identity 统一 | DEC-006 + DEC-004 | Phase 1 完成后 |
| 2 | BL-002 | 订单状态机统一 | BL-001 + WA-001 | Phase 1 完成后 |
| 3 | BL-003 | inventory/store_inventory 双写解决 | BL-001 + WP-002 | Phase 1 完成后 |
| 4 | BL-004 | 成本双写解决 | BL-001 + ER-003 | Phase 1 完成后 |
| 5 | BL-006 | 多端写入冲突解决 | WA-001 + BL-002 | Phase 1 完成后 |

### Phase 3: 全链路解锁后执行

| 序号 | 编号 | 任务 | 前置条件 | 预计启动时间 |
|------|------|------|---------|-------------|
| 1 | BL-005 | 事件驱动可靠性加强 | WA-001 + WA-002 + BL-002 + ER-009 | Phase 2 完成后 |
| 2 | BL-007 | Legacy 数据库表清理 | BL-001 + BL-002 + ER-003 | Phase 2 完成后 |
| 3 | BL-008 | Legacy 服务 + Dead Code 清理 | BL-007 | BL-007 完成后 |

---

## 六、关键路径分析

### 6.1 最长依赖链

```
LOCK-A001~A010 → ER-001~012 (Phase 0)
                        │
DEC-004 ──▶ WP-001 ──▶ BL-001
DEC-006 ──▶ WP-002 ──▶ BL-001
                        │
DEC-010 ──▶ WA-001 ──▶ BL-002 ──▶ BL-005
                        │
                  BL-007 ──▶ BL-008
```

**关键路径长度**: 5 层
**关键路径影响域**: Foundation → Master Data → Identity → Truth Source → Cross-Domain → Legacy Cleanup

### 6.2 辅助关键路径

```
DEC-011 ──▶ WA-002 ──▶ BL-005
DEC-013 ──▶ WA-003 ──▶ (安全基线)
DEC-014 ──▶ WA-004 ──▶ (供应商集成)
```

---

## 七、风险提示

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| DEC-004 延迟 | 用户域全部功能阻塞 | 设置 Deadline 2026-09-30 |
| DEC-006 延迟 | SR-001/SR-002/SR-005 全链路阻塞 | 设置 Deadline 2026-10-15 |
| DEC-010 延迟 | DEC-011/SR-012/SR-014 全链路阻塞 | 设置 Deadline 2026-09-30 |
| DEC-011 延迟 | 事件驱动范围无法确定 | 设置 Deadline 2026-09-30 |
| ER-001~012 执行延迟 | 基础规范化进度受阻 | 优先安排 Phase 0 任务 |
| 金额单位双轨 | 财务报表偏差 100 倍 | ER-009 (金额规范) 优先执行 |

---

## 八、状态变更汇总

| 编号 | 名称 | 原状态 | 新状态 | 变更原因 |
|------|------|--------|--------|---------|
| ER-001~012 | 基线 A/B VERIFIED LOCK 执行项 | CANDIDATE | **ENGINEERING_READY** | 14 项 LOCK 全部 VERIFIED，方案明确 |
| WP-001 | Customer/Member 关系 | CANDIDATE | **WAITING_PRODUCT_DECISION** | DEC-004 PENDING |
| WP-002 | Product/Food/Material 边界 | CANDIDATE | **WAITING_PRODUCT_DECISION** | DEC-006 PENDING |
| WP-003 | V2 废止结论清理 | CANDIDATE | **WAITING_PRODUCT_DECISION** | PO 确认 |
| WA-001 | 跨域数据访问模式 | CANDIDATE | **WAITING_ARCHITECTURE_DECISION** | DEC-010 PENDING |
| WA-002 | 事件驱动架构适用范围 | CANDIDATE | **WAITING_ARCHITECTURE_DECISION** | DEC-011 PENDING |
| WA-003 | 裸接口权限归属 | CANDIDATE | **WAITING_ARCHITECTURE_DECISION** | DEC-013 PENDING |
| WA-004 | 供应商 H5 认证 | CANDIDATE | **WAITING_ARCHITECTURE_DECISION** | DEC-014 PENDING |
| BL-001~008 | 阻断项 | CANDIDATE | **BLOCKED** | 依赖未完成的 Decision |
| IV-001~003 | V2 废止决策 | CANDIDATE | **INVALIDATED** | 与代码事实冲突 |

---

## 九、决策依赖关系 (完整 DAG)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                     决策与 Remediation 依赖关系 DAG                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  LAYER 1: LOCKED (不可变锚点 — 14 项 VERIFIED)                                  │
│  ┌──────────────────────────────────────────────────────────────────────┐       │
│  │ 基线 A: LOCK-A001~A010 (业务数据真相源)                              │       │
│  │ 基线 B: LOCK-B001, B002, B004, B006 (技术架构)                       │       │
│  └──────────────────────────────────────────────────────────────────────┘       │
│                              │                                                   │
│                              ▼                                                   │
│  LAYER 2: ENGINEERING_READY (12 项 — 可立即执行)                                 │
│  ┌──────────────────────────────────────────────────────────────────────┐       │
│  │ ER-001~008: 业务数据真相源规范化                                      │       │
│  │ ER-009~012: 技术架构规范化                                            │       │
│  └──────────────────────────────────────────────────────────────────────┘       │
│                              │                                                   │
│                              ▼                                                   │
│  LAYER 3: ARCHITECTURE OWNER (4 项 — 需 Arch Owner 确认)                        │
│  ┌──────────────────────────────────────────────────────────────────────┐       │
│  │ DEC-010 (跨域数据访问) ──unlocks──▶ DEC-011 (事件驱动范围)           │       │
│  │ DEC-013 (裸接口权限) ←── depends on ── LOCK-B004 (OAuth2+JWT)      │       │
│  │ DEC-014 (供应商 H5)   ←── depends on ── LOCK-B004 (OAuth2+JWT)      │       │
│  └──────────────────────────────────────────────────────────────────────┘       │
│                              │                                                   │
│                              ▼                                                   │
│  LAYER 4: PRODUCT OWNER (2 项 — 需 PO 确认)                                     │
│  ┌──────────────────────────────────────────────────────────────────────┐       │
│  │ DEC-004 (Customer/Member) ←── depends on ── DEC-006 (Product边界)   │       │
│  │ DEC-006 (Product/Food/Material) ←── depends on ── LOCK-B001 (EDA)  │       │
│  └──────────────────────────────────────────────────────────────────────┘       │
│                              │                                                   │
│                              ▼                                                   │
│  LAYER 5: BLOCKED (8 项 — 依赖链解锁后执行)                                     │
│  ┌──────────────────────────────────────────────────────────────────────┐       │
│  │ BL-001 (商品 Identity) → BL-002 (订单状态) → BL-005 (事件可靠)       │       │
│  │                      → BL-007 (遗留表) → BL-008 (遗留服务)           │       │
│  └──────────────────────────────────────────────────────────────────────┘       │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## 十、行动建议

### 10.1 立即行动 (本周)

| 行动 | 负责人 | 截止时间 | 优先级 |
|------|--------|---------|--------|
| 启动 ER-001~012 执行 (Phase 0) | 工程团队 | 立即 | P0 |
| 推进 DEC-010 跨域数据访问决策 | Arch Owner | 2026-09-30 | P0 |
| 推进 DEC-011 事件驱动范围决策 | Arch Owner | 2026-09-30 | P0 |
| 推进 DEC-004 Customer/Member 决策 | PO | 2026-09-30 | P0 |

### 10.2 中期行动 (下周)

| 行动 | 负责人 | 截止时间 | 优先级 |
|------|--------|---------|--------|
| 推进 DEC-006 Product/Food/Material 决策 | PO + Arch | 2026-10-15 | P1 |
| 推进 DEC-013 裸接口权限决策 | 安全组 | 2026-10-15 | P1 |
| 推进 DEC-014 供应商 H5 决策 | 安全组 | 2026-10-15 | P1 |
| 执行 ER-001~012 完成验证 | 工程团队 | Phase 0 完成后 | P1 |

### 10.3 长期行动 (Sprint 2+)

| 行动 | 负责人 | 截止时间 | 优先级 |
|------|--------|---------|--------|
| 执行 WP-001, WP-002 (决策完成后) | 工程团队 | Phase 1 完成后 | P1 |
| 执行 WA-001~004 (决策完成后) | 工程团队 | Phase 1 完成后 | P1 |
| 执行 BL-001~008 (依赖链解锁后) | 工程团队 | Phase 2+ 完成后 | P2 |

---

## 附录: 相关文档

| 文档 | 路径 | 用途 |
|------|------|------|
| 已验证锁定决策 | ./verified-locked-decisions.md | VERIFIED LOCK 详细清单 |
| Open Product Decisions | ./open-product-decisions.md | PO 待决策项 |
| Open Architecture Decisions | ./open-architecture-decisions.md | Arch Owner 待决策项 |
| Decision Provenance Matrix | ./decision-provenance-matrix.md | 三套基线证据溯源 |
| Engineering Only Decisions | ./engineering-only-decisions.md | 纯工程执行项 |
| Decision Baseline Recovery Summary | ./decision-baseline-recovery-summary.md | 最终总结 |
| 系统治理候选清单 | ../review/systemic-remediation-candidates.md | SR-001~017 候选 |
| Remediation Gate Report (Review) | ../review/remediation-gate-report.md | Review 阶段报告 |

---

**文档结束**

*生成时间: 2026-09-09*
*报告编号: DBR-GATE-001*
*关键原则: 绝不自动确认业务决策 | 只保留有充分证据支持的决策 | 废止未经证明的结论*
