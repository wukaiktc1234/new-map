# Decision Provenance Matrix — Decision Baseline Recovery

> **版本**: 1.0  
> **生成日期**: 2026-09-09  
> **数据来源**: V1/V2/V2-baseline 三套基线交叉验证 + 代码库静态分析 + 项目地图文件  
> **维护者**: opencode

---

## 一、概述

本文档为 Decision Baseline Recovery 建立完整的决策溯源矩阵，为每个 Decision 提供多维度证据链：

| 证据维度 | 说明 |
|----------|------|
| **代码证据** | package.json / pom.xml / 源代码文件 |
| **DB证据** | 数据库表结构 / Flyway迁移文件 |
| **API证据** | REST API端点定义 / Swagger |
| **迁移证据** | Flyway迁移脚本版本号 |
| **状态** | VERIFIED / PARTIAL / UNVERIFIED / CONFLICT |

---

## 二、基线A — 业务数据真相源 LOCK (V1 registry)

### 2.1 基线A LOCK-001~010 证据溯源

| Decision | 证据来源 | 代码证据 | DB证据 | API证据 | 迁移证据 | 状态 |
|----------|---------|---------|--------|---------|----------|------|
| **LOCK-001** (foods为真相源) | foundation-map.md §8, master-data-source-map.md §2.2 | FoodService.java → foods表 | V6.0.0 (foods表) | /v1/product-center/foods | V6.0.0 | **VERIFIED** |
| **LOCK-002** (material_archives为真相源) | foundation-map.md §7, master-data-source-map.md §2.3 | MaterialService.java → material_archives | V5.0.0 (material_archives) | /v1/product-center/materials | V5.0.0 | **VERIFIED** |
| **LOCK-003** (suppliers为真相源) | foundation-map.md §6, master-data-source-map.md §2.1 | SupplierService.java → suppliers | V3.0.0 (suppliers) | /v1/procurement/suppliers | V3.0.0 | **VERIFIED** |
| **LOCK-004** (employees为真相源) | foundation-map.md §3, master-data-source-map.md §2.4 | EmployeeService.java → employees | V2.0.0 (employees) | /v1/hr/employees | V2.0.0 | **VERIFIED** |
| **LOCK-005** (stores_new为真相源) | foundation-map.md §2, truth-conflict-map.md §冲突4 | StoreService.java → stores_new | V4.0.0 (stores_new) | /v1/store/stores | V4.0.0 | **VERIFIED** |
| **LOCK-006** (orders为真相源) | foundation-map.md, truth-conflict-map.md §冲突5 | OrderService.java → orders | V3.0.0 (orders) | /v1/order/orders | V3.0.0 | **VERIFIED** |
| **LOCK-007** (departments为真相源) | foundation-map.md §1, db-reality-map.md §2 | DepartmentService.java → departments | V1.0.0 (departments) | /v1/hr/departments | V1.0.0 | **VERIFIED** |
| **LOCK-008** (roles/permissions为真相源) | foundation-map.md §4-5, db-reality-map.md §1 | RoleService.java + PermissionService.java | V1.0.0 (roles, permissions) | /v1/system/roles, /v1/system/permissions | V1.0.0 | **VERIFIED** |
| **LOCK-009** (accounting_subjects为真相源) | foundation-map.md §11, truth-conflict-map.md §冲突3 | AccountingSubjectService.java → accounting_subjects | V7.0.0 (accounting_subjects) | /v1/finance/accounting-subjects | V7.0.0 | **VERIFIED** |
| **LOCK-010** (bank_accounts为真相源) | foundation-map.md §12, master-data-source-map.md §2.7 | BankAccountService.java → bank_accounts | V7.0.0 (bank_accounts) | /v1/finance/bank-accounts | V7.0.0 | **VERIFIED** |

### 2.2 基线A 小结

| 指标 | 数值 |
|------|------|
| VERIFIED | 10/10 |
| PARTIAL | 0/10 |
| UNVERIFIED | 0/10 |
| CONFLICT | 0/10 |

**结论**: 基线A (业务数据真相源) 全部通过代码证据验证，可信度 HIGH。

---

## 三、基线B — 技术架构 LOCK (V2 registry)

### 3.1 基线B LOCK-001~010 证据溯源

| Decision | 证据来源 | 代码证据 | DB证据 | API证据 | 迁移证据 | 状态 |
|----------|---------|---------|--------|---------|----------|------|
| **LOCK-001** (React + TypeScript) | V2 registry | frontend/package.json L39: `"vue": "^3.5.32"` | N/A | N/A | N/A | **CONFLICT** |
| **LOCK-002** (Redux Toolkit) | V2 registry | frontend/package.json L35: `"pinia": "^3.0.4"` | N/A | N/A | N/A | **CONFLICT** |
| **LOCK-003** (React Router v6) | V2 registry | frontend/package.json L40: `"vue-router": "^5.0.4"` | N/A | N/A | N/A | **CONFLICT** |
| **LOCK-004** (Ant Design) | V2 registry | frontend/package.json L32: `"element-plus": "^2.13.7"` | N/A | N/A | N/A | **CONFLICT** |
| **LOCK-005** (Vite) | V2 registry | frontend/package.json L49: `"vite": "^8.0.8"` | N/A | N/A | N/A | **VERIFIED** |
| **LOCK-006** (ESLint + Prettier) | V2 registry | 未在package.json中明确 | N/A | N/A | N/A | **UNVERIFIED** |
| **LOCK-007** (Vitest + React Testing Library) | V2 registry | Vitest确认，但React Testing Library应为Vue Test Utils | N/A | N/A | N/A | **PARTIAL** |
| **LOCK-008** (Axios) | V2 registry | frontend-pos/package.json L14: `"axios": "^1.6.2"` | N/A | N/A | N/A | **VERIFIED** |
| **LOCK-009** (react-i18next) | V2 registry | package.json中无react-i18next依赖 | N/A | N/A | N/A | **CONFLICT** |
| **LOCK-010** (ECharts) | V2 registry | frontend/package.json L30: `"echarts": "^6.0.0"` | N/A | N/A | N/A | **VERIFIED** |

### 3.2 基线B 小结

| 指标 | 数值 |
|------|------|
| VERIFIED | 3/10 (LOCK-005, 008, 010) |
| PARTIAL | 1/10 (LOCK-007) |
| UNVERIFIED | 1/10 (LOCK-006) |
| CONFLICT | 5/10 (LOCK-001, 002, 003, 004, 009) |

**结论**: 基线B (技术架构) 存在严重系统性偏差。V2错误地假设项目使用React技术栈，实际项目使用Vue.js。

### 3.3 基线B 冲突详情

| LOCK | V2声明 | 实际代码 | 严重程度 |
|------|--------|----------|----------|
| LOCK-001 | React + TypeScript | Vue.js 3.5.x + TypeScript | **CRITICAL** |
| LOCK-002 | Redux Toolkit | Pinia 3.0.4 | **CRITICAL** |
| LOCK-003 | React Router v6 | Vue Router 5.0.4 | **CRITICAL** |
| LOCK-004 | Ant Design | Element Plus 2.13.7 | **CRITICAL** |
| LOCK-009 | react-i18next | 未使用 | **HIGH** |

---

## 四、基线C — 技术架构 LOCK (V2-baseline-v2)

### 4.1 基线C LOCK-001~010 证据溯源

| Decision | 证据来源 | 代码证据 | DB证据 | API证据 | 迁移证据 | 状态 |
|----------|---------|---------|--------|---------|----------|------|
| **LOCK-001** (事件驱动架构) | V2-baseline-v2 §1 | event-job-map.md: 22个领域事件, 10个监听器 | N/A | N/A | N/A | **VERIFIED** |
| **LOCK-002** (PostgreSQL) | V2-baseline-v2 §1 | backend/pom.xml L191-196: postgresql依赖 | 340+ 张业务表 | /v1/* 所有API | 160个Flyway迁移文件 | **VERIFIED** |
| **LOCK-003** (Kubernetes) | V2-baseline-v2 §1 | 代码库中无K8s配置文件 | N/A | N/A | N/A | **UNVERIFIED** |
| **LOCK-004** (OAuth 2.0 + JWT) | V2-baseline-v2 §1 | backend/pom.xml L230-258: jjwt-api:0.12.3 | N/A | /v1/auth/** | V8.0.0 (JWT配置) | **VERIFIED** |
| **LOCK-005** (React + TypeScript) | V2-baseline-v2 §1 | frontend/package.json: Vue.js (非React) | N/A | N/A | N/A | **CONFLICT** |
| **LOCK-006** (RESTful API) | V2-baseline-v2 §1 | 154个Controller, RESTful风格 | N/A | /v1/** REST端点 | N/A | **VERIFIED** |
| **LOCK-007** (ELK Stack) | V2-baseline-v2 §1 | 代码库中无ELK配置 | N/A | N/A | N/A | **UNVERIFIED** |
| **LOCK-008** (GitHub Actions) | V2-baseline-v2 §1 | 代码库中无.github/workflows | N/A | N/A | N/A | **UNVERIFIED** |
| **LOCK-009** (Redis) | V2-baseline-v2 §1 | 代码库中无Redis依赖 | N/A | N/A | N/A | **UNVERIFIED** |
| **LOCK-010** (RocketMQ) | V2-baseline-v2 §1 | 代码库中无RocketMQ依赖 | N/A | N/A | N/A | **UNVERIFIED** |

### 4.2 基线C 小结

| 指标 | 数值 |
|------|------|
| VERIFIED | 3/10 (LOCK-001, 002, 004, 006) |
| PARTIAL | 0/10 |
| UNVERIFIED | 5/10 (LOCK-003, 007, 008, 009, 010) |
| CONFLICT | 1/10 (LOCK-005) |

**结论**: 基线C 中基础设施相关决策 (K8s, ELK, GitHub Actions, Redis, RocketMQ) 均无代码证据支撑，属于计划性决策而非已实现决策。

---

## 五、DEC-001~014 决策证据溯源

### 5.1 V1 Registry DEC 决策溯源

| Decision | 证据来源 | 代码证据 | DB证据 | API证据 | 迁移证据 | 状态 |
|----------|---------|---------|--------|---------|----------|------|
| **DEC-001** (Warehouse管理模式) | V1 registry | Warehouse无独立表，inventory.warehouse_id引用 | N/A (无warehouses表) | N/A | N/A | **CONFIRMED** |
| **DEC-002** (Unit管理模式) | V1 registry | Unit存储在字段枚举/配置中 | N/A (无units表) | N/A | N/A | **CONFIRMED** |
| **DEC-003** (Payment Method) | V1 registry | payment_methods配置表 | payment_methods表 | /v1/payment-methods | V6.0.0 | **CONFIRMED** |
| **DEC-004** (Customer-Member关系) | V1 registry | Customer和Member独立表 | customers, members表 | /v1/customers, /v1/members | V5.0.0 | **OPEN** |
| **DEC-005** (Price Decision) | V1 registry | 价格管理模型 | prices, price_strategies表 | /v1/prices | V6.0.0 | **CONFIRMED** |
| **DEC-006** (Product-Food-Material关系) | V1 registry | FoodService.java → foods; MaterialService.java → material_archives | foods, material_archives表 | /v1/foods, /v1/materials | V5.0.0 | **SUPERSEDED** |
| **DEC-007** (Order State Machine) | V1 registry | OrderService.java状态管理 | orders, order_status_logs表 | /v1/orders | V3.0.0 | **CONFIRMED** |
| **DEC-008** (Amount/Money Contract) | V1 registry | 金额字段类型: 新表用分，遗留表用元 | orders.amount(分), tax_record.amount(元) | N/A | N/A | **CONFIRMED** |
| **DEC-009** (Data Ownership) | V1 registry | cross-domain-coordination-matrix.md §5.1 | N/A | N/A | N/A | **OPEN** |
| **DEC-010** (Cross-Domain Access) | V1 registry | 跨域访问模式: API调用 | N/A | N/A | N/A | **OPEN** |
| **DEC-011** (Event Architecture) | V1 registry | 22个领域事件, 10个监听器 | events, event_logs表 | N/A | V6.0.0 | **CONFIRMED** |
| **DEC-012** (Inventory Location) | V1 registry | 库存位置管理 | inventory_locations, inventory表 | /v1/inventory | V5.0.0 | **OPEN** |
| **DEC-013** (Authentication Strategy) | V1 registry | JWT认证: jjwt-api:0.12.3 | users表 | /v1/auth/** | V8.0.0 | **CONFIRMED** |
| **DEC-014** (Authorization Model) | V1 registry | RBAC: roles, permissions, role_permissions | roles, permissions表 | /v1/roles, /v1/permissions | V1.0.0 | **CONFIRMED** |

### 5.2 V2 Registry DEC 决策溯源

| Decision | 证据来源 | 代码证据 | DB证据 | API证据 | 迁移证据 | 状态 |
|----------|---------|---------|--------|---------|----------|------|
| **DEC-001** (用户认证-JWT) | V2 registry | jjwt-api:0.12.3 + SecurityConfig | N/A | /v1/auth/** | V8.0.0 | **CONFIRMED** |
| **DEC-002** (权限控制-RBAC) | V2 registry | 180+ 权限码, @RequiresPermission注解 | roles, permissions表 | /v1/permissions | V1.0.0 | **CONFIRMED** |
| **DEC-003** (缓存策略-Redis) | V2 registry | 代码库中无Redis依赖 | N/A | N/A | N/A | **INVALIDATED** |
| **DEC-004** (日志方案-Winston) | V2 registry | 后端使用Spring Boot日志(Logback) | N/A | N/A | N/A | **INVALIDATED** |
| **DEC-005** (消息队列-RabbitMQ) | V2 registry | 代码库中无RabbitMQ依赖 | N/A | N/A | N/A | **INVALIDATED** |
| **DEC-006** (数据库选型-PostgreSQL) | V2 registry | backend/pom.xml: postgresql依赖 | PostgreSQL | N/A | V1.0.0+ | **CONFIRMED** |
| **DEC-007** (ORM方案-Prisma) | V2 registry | backend/pom.xml: mybatis-plus-spring-boot3-starter | MyBatis-Plus | N/A | N/A | **INVALIDATED** |
| **DEC-008** (UI组件定制-CSS-in-JS) | V2 registry | Element Plus样式系统 | N/A | N/A | N/A | **SUPERSEDED** |
| **DEC-009** (表单方案-Formily) | V2 registry | Element Plus Form组件 | N/A | N/A | N/A | **OPEN** |
| **DEC-010** (部署方案-Docker+K8s) | V2 registry | 无K8s配置文件 | N/A | N/A | N/A | **OPEN** |
| **DEC-011** (CI/CD-GitLab CI) | V2 registry | 无.gitlab-ci.yml | N/A | N/A | N/A | **OPEN** |
| **DEC-012** (监控方案-Prometheus+Grafana) | V2 registry | 无Prometheus依赖 | N/A | N/A | N/A | **OPEN** |
| **DEC-013** (文档方案-Swagger+TypeDoc) | V2 registry | springdoc-openapi-starter-webmvc-ui | N/A | /v3/api-docs | N/A | **CONFIRMED** |
| **DEC-014** (错误处理-统一错误码) | V2 registry | 统一错误码体系 | N/A | N/A | N/A | **CONFIRMED** |

### 5.3 V2-baseline-v2 DEC 决策溯源

| Decision | 证据来源 | 代码证据 | DB证据 | API证据 | 迁移证据 | 状态 |
|----------|---------|---------|--------|---------|----------|------|
| **DEC-001** (事件溯源存储引擎) | V2-baseline-v2 §3 | 无EventStoreDB/PostgreSQL Event Store代码 | N/A | N/A | N/A | **OPEN** |
| **DEC-002** (CQRS实现方案) | V2-baseline-v2 §4 | 无CQRS模式代码 | N/A | N/A | N/A | **OPEN** |
| **DEC-003** (事件Schema演进) | V2-baseline-v2 §4 | 无Schema Registry配置 | N/A | N/A | N/A | **OPEN** |
| **DEC-004** (API版本管理) | V2-baseline-v2 §3 | URL路径版本号: /v1/** | N/A | /v1/** | N/A | **CONFIRMED** |
| **DEC-005** (服务健康检查) | V2-baseline-v2 §4 | Spring Boot Actuator | N/A | /actuator/health | N/A | **PARTIAL** |
| **DEC-006** (服务网格选型) | V2-baseline-v2 §2 | 无Istio/Linkerd配置 | N/A | N/A | N/A | **INVALIDATED** |
| **DEC-007** (数据湖存储格式) | V2-baseline-v2 §2 | 无Apache Iceberg依赖 | N/A | N/A | N/A | **INVALIDATED** |
| **DEC-008** (实时计算框架) | V2-baseline-v2 §2 | 无Flink依赖 | N/A | N/A | N/A | **INVALIDATED** |
| **DEC-009** (多云部署策略) | V2-baseline-v2 §2 | 无多云配置 | N/A | N/A | N/A | **INVALIDATED** |
| **DEC-010** (服务限流策略) | V2-baseline-v2 §3 | 有限流注解实现 | N/A | N/A | N/A | **PARTIAL** |
| **DEC-011** (配置中心选型) | V2-baseline-v2 §3 | 无Nacos/Apollo/Consul配置 | N/A | N/A | N/A | **OPEN** |
| **DEC-012** (分布式事务) | V2-baseline-v2 §4 | Spring @Transactional | N/A | N/A | N/A | **PARTIAL** |
| **DEC-013** (监控告警整合) | V2-baseline-v2 §3 | 无Prometheus/Grafana配置 | N/A | N/A | N/A | **OPEN** |
| **DEC-014** (灾备切换自动化) | V2-baseline-v2 §3 | 无灾备配置 | N/A | N/A | N/A | **OPEN** |

---

## 六、三套基线冲突识别

### 6.1 同编号不同含义的 LOCK

| 编号 | V1基线含义 | V2基线含义 | V2-baseline-v2含义 | 冲突类型 |
|------|-----------|-----------|-------------------|----------|
| **LOCK-001** | foods为真相源 (业务数据) | React + TypeScript (技术栈) | 事件驱动架构 (EDA) | **三套基线三种含义** |
| **LOCK-002** | material_archives为真相源 | Redux Toolkit (状态管理) | PostgreSQL (数据库) | **三套基线三种含义** |
| **LOCK-003** | suppliers为真相源 | React Router v6 (路由) | Kubernetes (容器编排) | **三套基线三种含义** |
| **LOCK-004** | employees为真相源 | Ant Design (UI组件库) | OAuth 2.0 + JWT (认证) | **三套基线三种含义** |
| **LOCK-005** | stores_new为真相源 | Vite (构建工具) | React + TypeScript (技术栈) | **三套基线三种含义** |
| **LOCK-006** | orders为真相源 | ESLint + Prettier (代码规范) | RESTful API (接口标准) | **三套基线三种含义** |
| **LOCK-007** | departments为真相源 | Vitest + React Testing Library | ELK Stack (日志) | **三套基线三种含义** |
| **LOCK-008** | roles/permissions为真相源 | Axios (HTTP客户端) | GitHub Actions (CI/CD) | **三套基线三种含义** |
| **LOCK-009** | accounting_subjects为真相源 | react-i18next (国际化) | Redis (缓存) | **三套基线三种含义** |
| **LOCK-010** | bank_accounts为真相源 | ECharts (数据可视化) | RocketMQ (消息队列) | **三套基线三种含义** |

**结论**: 三套基线的LOCK编号完全重叠但含义完全不同，这是最大的系统性冲突。

### 6.2 与代码冲突的 LOCK

| 冲突LOCK | 基线 | V2声明 | 实际代码 | 冲突严重程度 |
|----------|------|--------|----------|-------------|
| LOCK-001 | V2 | React + TypeScript | Vue.js 3.5.x + TypeScript | **CRITICAL** |
| LOCK-002 | V2 | Redux Toolkit | Pinia 3.0.4 | **CRITICAL** |
| LOCK-003 | V2 | React Router v6 | Vue Router 5.0.4 | **CRITICAL** |
| LOCK-004 | V2 | Ant Design | Element Plus 2.13.7 | **CRITICAL** |
| LOCK-005 | V2-baseline-v2 | React + TypeScript | Vue.js 3.5.x + TypeScript | **CRITICAL** |
| LOCK-007 | V2 | React Testing Library | Vue Test Utils | **HIGH** |
| LOCK-009 | V2 | react-i18next | 未使用 | **HIGH** |
| LOCK-003 | V2-baseline-v2 | Kubernetes | 无K8s配置 | **MEDIUM** |
| LOCK-007 | V2-baseline-v2 | ELK Stack | 无ELK配置 | **MEDIUM** |
| LOCK-008 | V2-baseline-v2 | GitHub Actions | 无.github/workflows | **MEDIUM** |
| LOCK-009 | V2-baseline-v2 | Redis | 无Redis依赖 | **MEDIUM** |
| LOCK-010 | V2-baseline-v2 | RocketMQ | 无RocketMQ依赖 | **MEDIUM** |

### 6.3 冲突统计

| 冲突类型 | 数量 | 说明 |
|----------|------|------|
| 同编号不同含义 | 10 | 三套基线LOCK编号完全重叠 |
| V2与代码冲突 | 7 | V2假设React，实际Vue.js |
| V2-baseline-v2无证据 | 5 | K8s/ELK/GitHub Actions/Redis/RocketMQ |
| 总冲突项 | 22 | |

---

## 七、可信基线判定

### 7.1 基线可信度评级

| 基线 | 判定 | 可信度 | 理由 |
|------|------|--------|------|
| **V1 (业务数据真相源)** | **有条件可信** | HIGH | 10/10 LOCK VERIFIED，业务决策部分有效 |
| **V2 (技术架构-React假设)** | **不可信** | CRITICAL | 7/10 LOCK与代码冲突，技术栈假设完全错误 |
| **V2-baseline-v2 (基础设施)** | **部分可信** | MEDIUM | 4/10 VERIFIED，5/10 UNVERIFIED (计划性决策) |

### 7.2 可信决策清单

| 决策ID | 来源基线 | 标题 | 证据状态 | 可信度 |
|--------|----------|------|----------|--------|
| LOCK-001~010 | V1 | 业务数据真相源 | 全部VERIFIED | HIGH |
| LOCK-005 | V2 | 构建工具Vite | VERIFIED | HIGH |
| LOCK-008 | V2 | API通信Axios | VERIFIED | HIGH |
| LOCK-010 | V2 | 数据可视化ECharts | VERIFIED | HIGH |
| LOCK-001 | V2-baseline-v2 | 事件驱动架构 | VERIFIED | HIGH |
| LOCK-002 | V2-baseline-v2 | PostgreSQL | VERIFIED | HIGH |
| LOCK-004 | V2-baseline-v2 | OAuth 2.0 + JWT | VERIFIED | HIGH |
| LOCK-006 | V2-baseline-v2 | RESTful API | VERIFIED | HIGH |
| DEC-001~014 | V1 | 业务决策 | 大部分CONFIRMED | HIGH |
| DEC-001,002,006,013,014 | V2 | 技术决策 | CONFIRMED | HIGH |

### 7.3 需要重建的决策

| 决策ID | 原声明 | 应重建为 | 证据来源 |
|--------|--------|----------|----------|
| LOCK-001 | React + TypeScript | Vue.js 3.5.x + TypeScript | frontend/package.json |
| LOCK-002 | Redux Toolkit | Pinia 3.0.4 | frontend/package.json |
| LOCK-003 | React Router v6 | Vue Router 5.0.4 | frontend/package.json |
| LOCK-004 | Ant Design | Element Plus 2.13.7 | frontend/package.json |
| LOCK-007 | React Testing Library | Vue Test Utils | frontend/package.json |

---

## 八、附录: 证据文件索引

| 证据类型 | 文件路径 | 用途 |
|----------|----------|------|
| 技术栈证据 | frontend/package.json | 前端依赖验证 |
| 后端依赖 | backend/pom.xml | 后端依赖验证 |
| 业务真相源 | foundation-map.md | 13个Foundation事实 |
| 主数据源 | master-data-source-map.md | 15个主数据Canonical Source |
| 冲突事实 | truth-conflict-map.md | 10个已知冲突 |
| 能力就绪 | capability-readiness-map.md | 5个能力就绪状态 |
| DB现实 | db-reality-map.md | 数据库表结构 |
| 业务对象 | business-object-map.md | 业务对象关系 |
| 权限矩阵 | permission-data-scope-map.md | 180+权限码 |
| 跨域协调 | cross-domain-coordination-matrix.md | 15个业务对象所有权 |
| 事件体系 | event-job-map.md | 22个领域事件 |
| 决策注册表V1 | decision-recon-registry.yaml | V1决策定义 |
| 决策注册表V2 | decision-recon-v2-registry.yaml | V2决策定义 |
| 决策基线V2 | decision-baseline-v2.md | V2-baseline-v2定义 |
| 废止报告 | DECISION-INVALIDATION-REPORT.md | V2废止分析 |

---

**文档结束**

*生成时间: 2026-09-09*  
*数据来源: 三套基线交叉验证 + 代码库静态分析 + 项目地图文件*  
*维护者: opencode*
