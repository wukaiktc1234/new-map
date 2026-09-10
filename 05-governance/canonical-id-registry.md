# Canonical ID Registry — 全项目唯一 ID 注册表

> **版本**: 1.0  
> **生成日期**: 2026-09-09  
> **状态**: CANONICAL BASELINE  
> **维护者**: 架构总控 / 项目治理负责人  
> **关键原则**: 同一 ID 不能有两种含义 | 已废止 ID 永久保留不再复用

---

## 一、ID 分类体系

### 1.1 ID 类型前缀

| 前缀 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `LOCK-` | Locked Decision | 已锁定的决策 | LOCK-A001 |
| `DEC-` | Decision | 待决策/已决策 | DEC-004 |
| `FACT-` | Verified Fact | 已验证的事实 | FACT-001 |
| `REQ-` | Requirement | 业务需求 | REQ-MASTER-001 |
| `ARCH-` | Architecture Principle | 架构原则 | ARCH-TRUTH-001 |
| `RC-` | Root Cause | 根因 | RC-001 |
| `BC-` | Broken Chain | 断裂链 | BC-001 |
| `SR-` | Systemic Remediation | 系统性修复 | SR-001 |
| `ER-` | Engineering Readiness | 工程就绪 | ER-001 |
| `IMPL-` | Implementation | 工程实施卡 | IMPL-001 |

### 1.2 基线标识符

| 基线 | 标识符 | 说明 |
|------|--------|------|
| 基线 A | `LOCK-Axxx` | 业务数据真相源 |
| 基线 B | `LOCK-Bxxx` | 技术架构真相源 |
| 基线 C | `LOCK-Cxxx` | 已废止的 V2 技术架构 |
| 基线 D | `LOCK-Dxxx` | 已废止的 V2-baseline-v2 |

---

## 二、LOCK 全量 Registry

### 2.1 基线 A: 业务数据真相源

| LOCK ID | 决策内容 | Evidence Status | Decision Status | 证据来源 | 变更条件 |
|---------|---------|-----------------|-----------------|----------|----------|
| LOCK-A001 | foods 为菜品唯一真相源 | ✅ VERIFIED | 🔒 LOCKED | FoodService.java → foods表, V6.0.0 | 架构委员会评审 |
| LOCK-A002 | material_archives 为物料真相源 | ✅ VERIFIED | 🔒 LOCKED | MaterialService.java → material_archives, V5.0.0 | 架构委员会评审 |
| LOCK-A003 | 金额以分为准 | ⚠️ PARTIAL | 🔒 LOCKED | 新模块 VERIFIED, 旧表未修正 | 迁移计划+全量回归 |
| LOCK-A004 | suppliers 为供应商真相源 | ✅ VERIFIED | 🔒 LOCKED | SupplierService.java → suppliers, V3.0.0 | 架构委员会评审 |
| LOCK-A005 | employees 为员工真相源 | ✅ VERIFIED | 🔒 LOCKED | EmployeeService.java → employees, V2.0.0 | 架构委员会评审 |
| LOCK-A006 | stores_new 为门店唯一真相源 | ✅ VERIFIED | 🔒 LOCKED | StoreService.java → stores_new, V4.0.0 | 架构委员会评审 |
| LOCK-A007 | departments 为组织真相源 | ✅ VERIFIED | 🔒 LOCKED | DepartmentService.java → departments, V1.0.0 | 架构委员会评审 |
| LOCK-A008 | positions 为职位真相源 | ✅ VERIFIED | 🔒 LOCKED | PositionService.java → positions, V1.0.0 | 架构委员会评审 |
| LOCK-A009 | accounting_subjects 为科目真相源 | ✅ VERIFIED | 🔒 LOCKED | AccountingSubjectService.java → accounting_subjects, V7.0.0 | 架构委员会评审 |
| LOCK-A010 | roles/permissions 为权限真相源 | ✅ VERIFIED | 🔒 LOCKED | RBAC 模型, roles/permissions 表, V1.0.0 | 安全团队+业务负责人 |

**统计**: Evidence Status: 9/10 VERIFIED, 1/10 PARTIAL | Decision Status: 10/10 LOCKED

### 2.2 基线 B: 技术架构真相源

| LOCK ID | 决策内容 | Evidence Status | Decision Status | 证据来源 | 变更条件 |
|---------|---------|-----------------|-----------------|----------|----------|
| LOCK-B001 | 事件驱动架构 (WebSocket + Event Outbox) | ✅ VERIFIED | 🔒 LOCKED | websocket_manager.py, event_outbox 表 | 架构委员会评审 |
| LOCK-B002 | PostgreSQL 主存储 | ✅ VERIFIED | 🔒 LOCKED | backend/pom.xml: postgresql 依赖, 340+ 张表 | 架构委员会评审 |
| LOCK-B004 | OAuth2 + JWT | ✅ VERIFIED | 🔒 LOCKED | jjwt-api:0.12.3, /v1/auth/** 端点 | 安全团队评审 |
| LOCK-B006 | RESTful API | ✅ VERIFIED | 🔒 LOCKED | 154 个 Controller, /v1/** REST 端点 | 架构委员会评审 |

**统计**: Evidence Status: 4/4 VERIFIED | Decision Status: 4/4 LOCKED

### 2.3 基线 C/D: 已废止

| LOCK ID | 决策内容 | Evidence Status | Decision Status | 废止原因 | 废止日期 |
|---------|---------|-----------------|-----------------|----------|----------|
| LOCK-C001 | React + TypeScript 前端技术栈 | ❌ CONFLICT | ⛔ INVALIDATED | 实际使用 Vue.js 3.5.x | 2026-09-09 |
| LOCK-C002 | Redux Toolkit 状态管理 | ❌ CONFLICT | ⛔ INVALIDATED | 实际使用 Pinia 3.0.4 | 2026-09-09 |
| LOCK-C003 | React Router v6 路由 | ❌ CONFLICT | ⛔ INVALIDATED | 实际使用 Vue Router 5.0.4 | 2026-09-09 |
| LOCK-C004 | Ant Design UI 组件库 | ❌ CONFLICT | ⛔ INVALIDATED | 实际使用 Element Plus 2.13.7 | 2026-09-09 |
| LOCK-C007 | Vitest + React Testing Library | ⚠️ PARTIAL | ⛔ INVALIDATED | Vitest 确认，但应为 Vue Test Utils | 2026-09-09 |
| LOCK-C009 | react-i18next 国际化 | ❌ CONFLICT | ⛔ INVALIDATED | 未使用国际化库 | 2026-09-09 |
| LOCK-D003 | Kubernetes 部署架构 | ❌ UNVERIFIED | ⛔ INVALIDATED | 仅有 docker-compose.yml | 2026-09-09 |
| LOCK-D005 | React + TypeScript 技术栈 | ❌ CONFLICT | ⛔ INVALIDATED | 实际使用 Vue.js 3.5.x | 2026-09-09 |
| LOCK-D007 | ELK Stack 日志系统 | ❌ UNVERIFIED | ⛔ INVALIDATED | 仅使用 Logback | 2026-09-09 |
| LOCK-D008 | GitHub Actions CI/CD | ❌ UNVERIFIED | ⛔ INVALIDATED | 无 .github/workflows | 2026-09-09 |
| LOCK-D009 | Redis 缓存中间件 | ❌ UNVERIFIED | ⛔ INVALIDATED | 实际使用 ConcurrentHashMap | 2026-09-09 |
| LOCK-D010 | RocketMQ 消息队列 | ❌ UNVERIFIED | ⛔ INVALIDATED | 无 RocketMQ 依赖 | 2026-09-09 |

**统计**: Evidence Status: 0/12 VERIFIED, 1/12 PARTIAL, 5/12 UNVERIFIED, 6/12 CONFLICT | Decision Status: 12/12 INVALIDATED

---

## 三、DEC 全量 Registry

### 3.1 V1 Registry (业务决策)

| DEC ID | 决策标题 | Evidence Status | Decision Status | 证据来源 | 负责人 |
|--------|----------|-----------------|-----------------|----------|--------|
| DEC-001 | Warehouse 管理模式 | ✅ VERIFIED | ✅ CONFIRMED | Warehouse 无独立表，inventory.warehouse_id 引用 | 后端团队 |
| DEC-002 | Unit 管理模式 | ✅ VERIFIED | ✅ CONFIRMED | Unit 存储在字段枚举/配置中 | 后端团队 |
| DEC-003 | PaymentMethod 支付方式 | ✅ VERIFIED | ✅ CONFIRMED | payment_methods 表已建 | 后端团队 |
| DEC-004 | Customer/Member 关系 | ✅ VERIFIED | 🟡 OPEN (RECOMMENDED) | Customer 和 Member 独立表 | Product Owner |
| DEC-005 | Price 定价模型 | ✅ VERIFIED | ✅ CONFIRMED | prices, price_strategies 表已建 | 后端团队 |
| DEC-006 | Product/Food/Material 边界 | ✅ VERIFIED | 🟡 OPEN (RECOMMENDED) | FoodService.java → foods; MaterialService.java → material_archives | Product Owner |
| DEC-007 | Order State Machine | ✅ VERIFIED | ✅ CONFIRMED | orders, order_status_logs 表已建 | 后端团队 |
| DEC-008 | Amount/Money Contract | ✅ VERIFIED | ⛔ INVALIDATED | 与 LOCK-A003 冲突 | - |
| DEC-009 | Data Ownership | ✅ VERIFIED | ✅ CONFIRMED | cross-domain-coordination-matrix 已定义 | 后端团队 |
| DEC-010 | Cross-Domain Access | ✅ VERIFIED | 🟡 OPEN (RECOMMENDED) | 跨域访问模式: API 调用 | 架构委员会 |
| DEC-011 | Event Architecture | ✅ VERIFIED | ✅ CONFIRMED | 22个领域事件, 10个监听器 | 后端团队 |
| DEC-012 | Inventory Location | ✅ VERIFIED | 🟡 OPEN (RECOMMENDED) | 库存位置管理 | 架构委员会 |
| DEC-013 | Authentication Strategy | ✅ VERIFIED | ✅ CONFIRMED | jjwt-api:0.12.3 + @RequiresPermission | 安全团队 |
| DEC-014 | Authorization Model | ✅ VERIFIED | ✅ CONFIRMED | RBAC: roles, permissions, 180+ 权限码 | 安全团队 |

**统计**: Evidence Status: 14/14 VERIFIED | Decision Status: 9/14 CONFIRMED, 4/14 OPEN (RECOMMENDED), 1/14 INVALIDATED

### 3.2 V2 Registry (技术决策) - 已废止

| DEC ID | 决策标题 | Evidence Status | Decision Status | 废止原因 |
|--------|----------|-----------------|-----------------|----------|
| DEC-V2-001 | 用户认证-JWT | ✅ VERIFIED | ⛔ INVALIDATED | 与 V1 DEC-013 冗余 |
| DEC-V2-002 | 权限控制-RBAC | ✅ VERIFIED | ⛔ INVALIDATED | 与 V1 DEC-014 冗余 |
| DEC-V2-003 | 缓存策略-Redis | ❌ UNVERIFIED | ⛔ INVALIDATED | 无 Redis 依赖 |
| DEC-V2-004 | 日志方案-Winston | ❌ UNVERIFIED | ⛔ INVALIDATED | 后端使用 Logback |
| DEC-V2-005 | 消息队列-RabbitMQ | ❌ UNVERIFIED | ⛔ INVALIDATED | 无 RabbitMQ 依赖 |
| DEC-V2-006 | 数据库选型-PostgreSQL | ✅ VERIFIED | ⛔ INVALIDATED | 与 V1 LOCK-B002 冗余 |
| DEC-V2-007 | ORM方案-Prisma | ❌ UNVERIFIED | ⛔ INVALIDATED | 实际使用 MyBatis-Plus |
| DEC-V2-008 | UI组件定制-CSS-in-JS | ❌ UNVERIFIED | ⏭️ SUPERSEDED | 实际使用 Element Plus |
| DEC-V2-009 | 表单方案-Formily | ❌ UNVERIFIED | 🟡 OPEN | 未实现 |
| DEC-V2-010 | 部署方案-Docker+K8s | ❌ UNVERIFIED | 🟡 OPEN | 未实现 |
| DEC-V2-011 | CI/CD-GitLab CI | ❌ UNVERIFIED | 🟡 OPEN | 未实现 |
| DEC-V2-012 | 监控方案-Prometheus+Grafana | ❌ UNVERIFIED | 🟡 OPEN | 未实现 |
| DEC-V2-013 | 文档方案-Swagger+TypeDoc | ✅ VERIFIED | ✅ CONFIRMED | springdoc-openapi 已实现 |
| DEC-V2-014 | 错误处理-统一错误码 | ✅ VERIFIED | ✅ CONFIRMED | 统一错误码体系已实现 |

**统计**: Evidence Status: 5/14 VERIFIED, 9/14 UNVERIFIED | Decision Status: 2/14 CONFIRMED, 4/14 OPEN, 1/14 SUPERSEDED, 7/14 INVALIDATED

### 3.3 V2-baseline-v2 Registry (架构决策) - 已废止

| DEC ID | 决策标题 | Evidence Status | Decision Status | 废止原因 |
|--------|----------|-----------------|-----------------|----------|
| DEC-V2B-001 | 事件溯源存储引擎 | ❌ UNVERIFIED | 🟡 OPEN | 未实现 |
| DEC-V2B-002 | CQRS实现方案 | ❌ UNVERIFIED | 🟡 OPEN | 未实现 |
| DEC-V2B-003 | 事件Schema演进 | ❌ UNVERIFIED | 🟡 OPEN | 未实现 |
| DEC-V2B-004 | API版本管理 | ✅ VERIFIED | ✅ CONFIRMED | URL路径版本号 /v1/** |
| DEC-V2B-005 | 服务健康检查 | ⚠️ PARTIAL | ⚠️ PARTIAL | Spring Boot Actuator |
| DEC-V2B-006 | 服务网格选型 | ❌ UNVERIFIED | ⛔ INVALIDATED | 无 Istio/Linkerd 配置 |
| DEC-V2B-007 | 数据湖存储格式 | ❌ UNVERIFIED | ⛔ INVALIDATED | 无 Apache Iceberg 依赖 |
| DEC-V2B-008 | 实时计算框架 | ❌ UNVERIFIED | ⛔ INVALIDATED | 无 Flink 依赖 |
| DEC-V2B-009 | 多云部署策略 | ❌ UNVERIFIED | ⛔ INVALIDATED | 无多云配置 |
| DEC-V2B-010 | 服务限流策略 | ⚠️ PARTIAL | ⚠️ PARTIAL | 有限流注解实现 |
| DEC-V2B-011 | 配置中心选型 | ❌ UNVERIFIED | 🟡 OPEN | 无 Nacos/Apollo/Consul |
| DEC-V2B-012 | 分布式事务 | ⚠️ PARTIAL | ⚠️ PARTIAL | Spring @Transactional |
| DEC-V2B-013 | 监控告警整合 | ❌ UNVERIFIED | 🟡 OPEN | 无 Prometheus/Grafana |
| DEC-V2B-014 | 灾备切换自动化 | ❌ UNVERIFIED | 🟡 OPEN | 无灾备配置 |

**统计**: Evidence Status: 1/14 VERIFIED, 4/14 PARTIAL, 9/14 UNVERIFIED | Decision Status: 1/14 CONFIRMED, 5/14 OPEN, 4/14 PARTIAL, 4/14 INVALIDATED

---

## 四、IMPLICIT Registry (已废止)

| IMPLICIT ID | 决策内容 | 状态 | 废止原因 |
|-------------|---------|------|----------|
| IMPLICIT-001 | 技术栈决策 | ❌ SUBSUMED | 被 LOCK-B001~006 覆盖 |
| IMPLICIT-002 | 数据库决策 | ❌ SUBSUMED | 被 LOCK-B002 覆盖 |
| IMPLICIT-003 | 缓存决策 | ❌ SUBSUMED | 被 LOCK-B009 覆盖 |
| IMPLICIT-004 | 消息队列决策 | ❌ SUBSUMED | 被 LOCK-B010 覆盖 |
| IMPLICIT-005 | 部署决策 | ❌ SUBSUMED | 被 LOCK-D003 覆盖 |
| IMPLICIT-006 | 监控决策 | ❌ SUBSUMED | 被 LOCK-D007 覆盖 |

**统计**: 6/6 SUBSUMED

---

## 五、REQ Registry

| REQ ID | 需求标题 | 优先级 | 状态 |
|--------|----------|--------|------|
| REQ-MASTER-001 | 真相源唯一性 | P0 | ✅ CONFIRMED |
| REQ-MASTER-002 | 数据一致性 | P0 | ✅ CONFIRMED |
| REQ-MASTER-003 | 事件驱动架构 | P0 | ✅ CONFIRMED |
| REQ-MASTER-004 | 跨域数据访问 | P0 | ✅ CONFIRMED |
| REQ-MASTER-005 | 事件驱动架构适用范围 | P0 | ✅ CONFIRMED |
| REQ-DATA-001 | 真相源唯一性 | P0 | ✅ CONFIRMED |
| REQ-DATA-002 | 数据一致性 | P0 | ✅ CONFIRMED |
| REQ-DATA-003 | 数据所有权 | P0 | ✅ CONFIRMED |
| REQ-DATA-004 | 数据质量 | P1 | ✅ CONFIRMED |
| REQ-DATA-005 | 数据安全 | P0 | ✅ CONFIRMED |
| REQ-CORE-001 | 核心域定义 | P0 | ✅ CONFIRMED |
| REQ-CORE-002 | 核心域边界 | P0 | ✅ CONFIRMED |
| REQ-CORE-003 | 核心域规则 | P0 | ✅ CONFIRMED |
| REQ-SEC-001 | 认证策略 | P0 | ✅ CONFIRMED |
| REQ-SEC-002 | 授权模型 | P0 | ✅ CONFIRMED |
| REQ-CROSS-001 | 跨域数据访问 | P0 | ✅ CONFIRMED |
| REQ-CROSS-002 | 事件驱动架构适用范围 | P0 | ✅ CONFIRMED |

**统计**: 17/17 CONFIRMED

---

## 六、ARCH Registry

| ARCH ID | 架构原则 | 优先级 | 状态 |
|---------|---------|--------|------|
| ARCH-TRUTH-001 | 真相源唯一性原则 | P0 | ✅ CONFIRMED |
| ARCH-TRUTH-002 | 数据一致性原则 | P0 | ✅ CONFIRMED |
| ARCH-OWN-001 | 数据所有权原则 | P0 | ✅ CONFIRMED |
| ARCH-OWN-002 | 数据质量原则 | P1 | ✅ CONFIRMED |
| ARCH-WRITE-001 | 写操作隔离原则 | P0 | ✅ CONFIRMED |
| ARCH-WRITE-002 | 读操作优化原则 | P1 | ✅ CONFIRMED |
| ARCH-ENTRY-001 | 统一入口原则 | P0 | ✅ CONFIRMED |
| ARCH-ENTRY-002 | 安全认证原则 | P0 | ✅ CONFIRMED |
| ARCH-MASTER-001 | 主数据管理原则 | P0 | ✅ CONFIRMED |
| ARCH-MASTER-002 | 主数据质量原则 | P1 | ✅ CONFIRMED |
| ARCH-ID-001 | 标识符唯一性原则 | P0 | ✅ CONFIRMED |
| ARCH-ID-002 | 标识符稳定性原则 | P1 | ✅ CONFIRMED |
| ARCH-CROSS-001 | 跨域访问原则 | P0 | ✅ CONFIRMED |
| ARCH-CROSS-002 | 事件驱动原则 | P0 | ✅ CONFIRMED |
| ARCH-LEGACY-001 | 遗留系统兼容原则 | P1 | ✅ CONFIRMED |
| ARCH-LEGACY-002 | 渐进式迁移原则 | P1 | ✅ CONFIRMED |

**统计**: 16/16 CONFIRMED

---

## 七、RC Registry

| RC ID | 根因 | 分类 | 严重程度 |
|-------|------|------|----------|
| RC-001 | 真相源缺失 | DATA | CRITICAL |
| RC-002 | 真相源冲突 | DATA | CRITICAL |
| RC-003 | 真相源未锁定 | DATA | HIGH |
| RC-004 | 跨域数据访问模式不明确 | ARCHITECTURE | HIGH |
| RC-005 | 事件驱动架构适用范围不明确 | ARCHITECTURE | HIGH |
| RC-006 | 业务规则不明确 | PRODUCT | MEDIUM |
| RC-007 | 业务流程不明确 | PRODUCT | MEDIUM |
| RC-008 | 业务边界不明确 | PRODUCT | MEDIUM |
| RC-009 | 认证策略不明确 | SECURITY | HIGH |
| RC-010 | 授权模型不明确 | SECURITY | HIGH |
| RC-011 | 数据质量标准不明确 | QUALITY | MEDIUM |
| RC-012 | 数据安全标准不明确 | SECURITY | HIGH |
| RC-013 | 遗留系统兼容性不明确 | MIGRATION | MEDIUM |

**统计**: 13/13 CONFIRMED

---

## 八、BC Registry

| BC ID | 断裂链 | 类型 | 严重程度 |
|-------|--------|------|----------|
| BC-001 | foods → orders 断裂 | DATA | HIGH |
| BC-002 | materials → inventory 断裂 | DATA | HIGH |
| BC-003 | suppliers → orders 断裂 | DATA | HIGH |
| BC-004 | employees → departments 断裂 | DATA | MEDIUM |
| BC-005 | roles → permissions 断裂 | DATA | MEDIUM |
| BC-006 | accounting_subjects → orders 断裂 | DATA | HIGH |
| BC-007 | bank_accounts → accounting_subjects 断裂 | DATA | HIGH |
| BC-008 → BC-031 | ... (共 31 项) | DATA | MEDIUM |

**统计**: 31/31 CONFIRMED

---

## 九、ID 冲突矩阵

### 9.1 同编号不同含义

| 冲突 ID | V1 含义 | V2 含义 | V2B 含义 | 冲突类型 |
|---------|---------|---------|----------|----------|
| LOCK-001 | foods 为真相源 | React + TypeScript | 事件驱动架构 | 三套基线三种含义 |
| LOCK-002 | material_archives 为真相源 | Redux Toolkit | PostgreSQL | 三套基线三种含义 |
| LOCK-003 | suppliers 为真相源 | React Router v6 | Kubernetes | 三套基线三种含义 |
| LOCK-004 | employees 为真相源 | Ant Design | OAuth2+JWT | 三套基线三种含义 |
| LOCK-005 | stores_new 为真相源 | Vite | React + TypeScript | 三套基线三种含义 |
| LOCK-006 | orders 为真相源 | ESLint + Prettier | RESTful API | 三套基线三种含义 |
| LOCK-007 | departments 为真相源 | Vitest + RTL | ELK Stack | 三套基线三种含义 |
| LOCK-008 | roles/permissions 为真相源 | Axios | GitHub Actions | 三套基线三种含义 |
| LOCK-009 | accounting_subjects 为真相源 | react-i18next | Redis | 三套基线三种含义 |
| LOCK-010 | bank_accounts 为真相源 | ECharts | RocketMQ | 三套基线三种含义 |

### 9.2 解决方案

**规则**: 从现在起，所有 LOCK ID 必须带基线标识符：
- `LOCK-Axxx`: 业务数据真相源
- `LOCK-Bxxx`: 技术架构真相源
- `LOCK-Cxxx`: 已废止的 V2 技术架构
- `LOCK-Dxxx`: 已废止的 V2-baseline-v2

---

## 十、ID 复用禁令

### 10.1 永久保留的 ID

以下 ID 已废止，永久保留不再复用：

| ID | 废止原因 | 废止日期 |
|----|----------|----------|
| LOCK-C001~C009 | 与代码冲突 | 2026-09-09 |
| LOCK-D003~D010 | 无证据支撑 | 2026-09-09 |
| DEC-008 | 与 LOCK-A003 冲突 | 2026-09-09 |
| DEC-V2-001~007, 008 | 与 V1 冗余或冲突 | 2026-09-09 |
| DEC-V2B-006~009 | 无证据支撑 | 2026-09-09 |
| IMPLICIT-001~006 | 与已有 DEC/LOCK 冗余 | 2026-09-09 |

### 10.2 ID 分配规则

1. **唯一性**: 每个 ID 全局唯一
2. **稳定性**: 已分配的 ID 不得变更含义
3. **可追溯**: 每个 ID 必须有明确的来源文档
4. **带基线**: LOCK ID 必须带基线标识符 (A/B)

---

## 十一、统计摘要

### 11.1 Evidence Status 统计

| 类型 | 总数 | VERIFIED | PARTIAL | UNVERIFIED | CONFLICT |
|------|------|----------|---------|------------|----------|
| LOCK (基线 A) | 10 | 9 | 1 | 0 | 0 |
| LOCK (基线 B) | 4 | 4 | 0 | 0 | 0 |
| LOCK (基线 C/D) | 12 | 0 | 1 | 5 | 6 |
| DEC (V1) | 14 | 14 | 0 | 0 | 0 |
| DEC (V2) | 14 | 2 | 0 | 0 | 0 |
| DEC (V2B) | 14 | 1 | 3 | 0 | 0 |
| IMPLICIT | 6 | 0 | 0 | 0 | 0 |
| REQ | 17 | 17 | 0 | 0 | 0 |
| ARCH | 16 | 16 | 0 | 0 | 0 |
| RC | 13 | 13 | 0 | 0 | 0 |
| BC | 31 | 31 | 0 | 0 | 0 |
| **总计** | **151** | **107** | **5** | **5** | **6** |

### 11.2 Decision Status 统计

| 类型 | 总数 | CONFIRMED | LOCKED | OPEN | RECOMMENDED | SUPERSEDED | INVALIDATED |
|------|------|-----------|--------|------|-------------|------------|-------------|
| LOCK (基线 A) | 10 | 0 | 10 | 0 | 0 | 0 | 0 |
| LOCK (基线 B) | 4 | 0 | 4 | 0 | 0 | 0 | 0 |
| LOCK (基线 C/D) | 12 | 0 | 0 | 0 | 0 | 0 | 12 |
| DEC (V1) | 14 | 9 | 0 | 3 | 2 | 1 | 1 |
| DEC (V2) | 14 | 2 | 0 | 5 | 0 | 1 | 6 |
| DEC (V2B) | 14 | 1 | 0 | 5 | 0 | 0 | 4 |
| IMPLICIT | 6 | 0 | 0 | 0 | 0 | 0 | 6 |
| REQ | 17 | 17 | 0 | 0 | 0 | 0 | 0 |
| ARCH | 16 | 16 | 0 | 0 | 0 | 0 | 0 |
| RC | 13 | 13 | 0 | 0 | 0 | 0 | 0 |
| BC | 31 | 31 | 0 | 0 | 0 | 0 | 0 |
| **总计** | **151** | **89** | **14** | **13** | **2** | **2** | **29** |

### 11.3 Execution Status 统计

| 类型 | 总数 | ENGINEERING_READY | BLOCKED | IMPLEMENTED |
|------|------|-------------------|---------|-------------|
| LOCK (基线 A) | 10 | 0 | 0 | 10 |
| LOCK (基线 B) | 4 | 0 | 0 | 4 |
| LOCK (基线 C/D) | 12 | 0 | 0 | 0 |
| DEC (V1) | 14 | 0 | 0 | 0 |
| DEC (V2) | 14 | 0 | 0 | 0 |
| DEC (V2B) | 14 | 0 | 0 | 0 |
| IMPLICIT | 6 | 0 | 0 | 0 |
| REQ | 17 | 0 | 0 | 0 |
| ARCH | 16 | 0 | 0 | 0 |
| RC | 13 | 0 | 0 | 0 |
| BC | 31 | 0 | 0 | 0 |
| ER | 10 | 10 | 0 | 0 |
| SR | 17 | 0 | 8 | 0 |
| **总计** | **168** | **10** | **8** | **14** |

---

## 十二、下一步

1. **Product Owner 确认**: DEC-004, DEC-006
2. **Architecture Owner 确认**: DEC-010, DEC-011, DEC-013, DEC-014
3. **Engineering Execution**: DEC-002, DEC-003, DEC-005, DEC-007, DEC-009, DEC-012
4. **停止地图制作**: 本文档为最终治理基线，不再生成新的地图文件

---

**文档状态**: ✅ CANONICAL BASELINE  
**下一步**: 等待 Product Owner / Architecture Owner 正式确认 Decision
