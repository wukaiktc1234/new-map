# Decision Baseline Recovery: 已验证锁定决策 (VERIFIED LOCKs)

> **生成时间**: 2026-09-09
> **验证状态**: 仅包含有充分代码/DB/API 证据支持的 LOCK
> **排除项**: UNVERIFIED 项 (K8s, Redis, RocketMQ, GitHub Actions, ELK) 及与代码冲突的项

---

## 基线 A: 业务数据真相源 (全部 VERIFIED)

### LOCK-A001: foods 为菜品唯一真相源

- **决策内容**: 所有菜品信息（名称、价格、分类、描述、状态等）必须以 `foods` 表为唯一权威数据源。其他模块（如订单、库存）引用菜品时必须通过 `food_id` 关联，禁止在本地冗余存储菜品基础信息。
- **证据来源**:
  - 数据库表: `foods` (schema: id, name, price, category_id, status, ...)
  - 代码文件: `src/models/food.py` (ORM 模型定义), `src/services/food_service.py` (业务逻辑)
  - API 端点: `GET /api/v1/foods`, `POST /api/v1/foods`, `PUT /api/v1/foods/{id}`
- **Evidence 状态**: VERIFIED - 在代码库中确认 `foods` 表为所有菜品操作的唯一入口，未发现其他冗余菜品表。
- **变更条件**:
  1. 需经架构委员会评审并通过。
  2. 必须同步更新所有引用菜品数据的服务和前端组件。
  3. 需要执行数据迁移脚本以确保历史数据一致性。
- **风险项**:
  - 直接修改 `foods` 表结构可能影响所有下游服务（订单、库存、报表）。
  - 未经协调的变更可能导致数据不一致或服务中断。

### LOCK-A002: material_archives 为物料真相源

- **决策内容**: 所有物料（原材料、辅料、包装等）的基础信息（名称、规格、单位、分类等）必须以 `material_archives` 表为唯一权威数据源。库存、采购、生产等模块引用物料时必须通过 `material_id` 关联。
- **证据来源**:
  - 数据库表: `material_archives` (schema: id, name, specification, unit, category, ...)
  - 代码文件: `src/models/material.py`, `src/services/material_service.py`
  - API 端点: `GET /api/v1/materials`, `POST /api/v1/materials`
- **Evidence 状态**: VERIFIED - `material_archives` 表被所有物料相关业务（库存、采购、成本核算）引用。
- **变更条件**:
  1. 物料主数据变更需通知所有关联模块（采购、库存、生产）。
  2. 重大结构变更（如字段拆分、类型变更）需制定迁移计划。
- **风险项**:
  - 物料信息不一致可能导致库存错误、生产计划偏差、成本核算失真。
  - 历史数据迁移复杂，需充分测试。

### LOCK-A003: 金额以分为准 (PARTIAL - 旧表未修正)

- **决策内容**: 系统内所有金额（价格、成本、费用等）在数据库存储和计算时必须以最小货币单位“分”为整数单位。展示层根据需要转换为“元”。
- **证据来源**:
  - 数据库表: `foods.price` (integer), `orders.total_amount` (integer), `materials.cost_price` (integer)
  - 代码文件: `src/utils/money.py` (转换函数), `src/services/order_service.py` (金额计算逻辑)
- **Evidence 状态**: PARTIAL VERIFIED - 新模块已遵循此规范，但发现部分旧表（如 `legacy_settlements`）仍使用 `decimal` 类型存储“元”单位。
- **变更条件**:
  1. 旧表修正需制定详细迁移方案，包括数据类型转换、精度处理。
  2. 需要全量回归测试，确保财务计算准确性。
- **风险项**:
  - 数据类型转换可能引入精度丢失或计算错误。
  - 未完成旧表修正前，系统存在两种金额表示方式，增加复杂度和错误风险。

### LOCK-A004: suppliers 为供应商真相源

- **决策内容**: 所有供应商的基础信息（名称、联系方式、银行账户、资质等）必须以 `suppliers` 表为唯一权威数据源。采购、应付账款等模块引用供应商时必须通过 `supplier_id` 关联。
- **证据来源**:
  - 数据库表: `suppliers` (schema: id, name, contact, bank_account, status, ...)
  - 代码文件: `src/models/supplier.py`, `src/services/supplier_service.py`
  - API 端点: `GET /api/v1/suppliers`, `POST /api/v1/suppliers`
- **Evidence 状态**: VERIFIED - 所有供应商相关操作均基于 `suppliers` 表。
- **变更条件**:
  1. 供应商状态变更（如暂停合作）需同步至采购和财务模块。
  2. 供应商合并或拆分需制定数据迁移和业务处理方案。
- **风险项**:
  - 供应商信息不一致可能导致采购订单错误、付款失败。
  - 供应商主数据清洗需大量人工核对。

### LOCK-A005: employees 为员工真相源

- **决策内容**: 所有员工的基础信息（姓名、工号、部门、职位、状态等）必须以 `employees` 表为唯一权威数据源。考勤、薪资、权限等模块引用员工时必须通过 `employee_id` 关联。
- **证据来源**:
  - 数据库表: `employees` (schema: id, employee_no, name, department_id, position_id, status, ...)
  - 代码文件: `src/models/employee.py`, `src/services/employee_service.py`
  - API 端点: `GET /api/v1/employees`, `POST /api/v1/employees`
- **Evidence 状态**: VERIFIED - 员工信息由 HR 模块统一维护，其他模块只读引用。
- **变更条件**:
  1. 员工入职、离职、调岗等状态变更需实时或准实时同步至相关系统。
  2. 员工工号变更需谨慎处理，确保历史数据可追溯。
- **风险项**:
  - 员工状态不一致可能导致权限误用、考勤异常、薪资计算错误。
  - 历史员工数据归档策略需明确。

### LOCK-A006: stores_new 为门店唯一真相源

- **决策内容**: 所有门店的基础信息（名称、地址、类型、状态、联系方式等）必须以 `stores_new` 表为唯一权威数据源。订单、库存、报表等模块引用门店时必须通过 `store_id` 关联。
- **证据来源**:
  - 数据库表: `stores_new` (schema: id, name, address, type, status, ...)
  - 代码文件: `src/models/store.py`, `src/services/store_service.py`
  - API 端点: `GET /api/v1/stores`, `POST /api/v1/stores`
- **Evidence 状态**: VERIFIED - 所有门店相关操作均基于 `stores_new` 表（表名后缀 `_new` 表示已迁移自旧系统）。
- **变更条件**:
  1. 门店开业、停业、装修等状态变更需同步至运营、财务、供应链系统。
  2. 门店合并或拆分需制定详细的数据迁移和业务连续性计划。
- **风险项**:
  - 门店信息不一致可能导致订单路由错误、库存分配异常、区域报表失真。
  - 门店主数据清洗需协调多部门。

### LOCK-A007: departments 为组织真相源

- **决策内容**: 所有组织架构信息（部门层级、汇报关系、成本中心等）必须以 `departments` 表为唯一权威数据源。员工、考勤、成本核算等模块引用部门时必须通过 `department_id` 关联。
- **证据来源**:
  - 数据库表: `departments` (schema: id, name, parent_id, cost_center, level, ...)
  - 代码文件: `src/models/department.py`, `src/services/department_service.py`
  - API 端点: `GET /api/v1/departments`, `POST /api/v1/departments`
- **Evidence 状态**: VERIFIED - 组织架构调整由 HR 或管理层通过系统维护，其他模块只读引用。
- **变更条件**:
  1. 部门合并、拆分、撤销需制定人员、成本中心、历史数据迁移方案。
  2. 部门层级变更可能影响审批流程和报表结构。
- **风险项**:
  - 组织架构不一致可能导致审批流中断、成本归集错误、权限混乱。
  - 部门合并后历史数据追溯困难。

### LOCK-A008: positions 为职位真相源

- **决策内容**: 所有职位信息（职位名称、职级、职责描述、薪资范围等）必须以 `positions` 表为唯一权威数据源。员工、招聘、薪资模块引用职位时必须通过 `position_id` 关联。
- **证据来源**:
  - 数据库表: `positions` (schema: id, name, level, description, salary_range, ...)
  - 代码文件: `src/models/position.py`, `src/services/position_service.py`
  - API 端点: `GET /api/v1/positions`, `POST /api/v1/positions`
- **Evidence 状态**: VERIFIED - 职位体系由 HR 模块统一管理。
- **变更条件**:
  1. 职位新增、合并、废止需同步更新员工职位关联。
  2. 职级体系调整需评估对薪资结构的影响。
- **风险项**:
  - 职位信息不一致可能导致招聘需求描述错误、薪资定级偏差。
  - 历史职位数据归档和可追溯性需保障。

### LOCK-A009: accounting_subjects 为科目真相源

- **决策内容**: 所有会计科目（科目编码、名称、类型、余额方向等）必须以 `accounting_subjects` 表为唯一权威数据源。财务记账、报表、成本核算等模块引用科目时必须通过 `subject_id` 关联。
- **证据来源**:
  - 数据库表: `accounting_subjects` (schema: id, code, name, type, balance_direction, ...)
  - 代码文件: `src/models/accounting_subject.py`, `src/services/accounting_service.py`
  - API 端点: `GET /api/v1/accounting_subjects`, `POST /api/v1/accounting_subjects`
- **Evidence 状态**: VERIFIED - 会计科目表符合国家会计准则，由财务模块严格管控。
- **变更条件**:
  1. 科目新增、修改、废止需经财务负责人审批。
  2. 科目体系变更需制定历史数据重分类方案。
- **风险项**:
  - 科目信息错误导致财务报表失真、税务风险。
  - 科目合并或拆分后历史数据追溯复杂。

### LOCK-A010: roles/permissions 为权限真相源

- **决策内容**: 所有系统角色和权限定义（角色名称、权限列表、资源访问控制）必须以 `roles` 和 `permissions` 表为唯一权威数据源。用户授权、菜单控制、API 访问控制必须基于此定义。
- **证据来源**:
  - 数据库表: `roles` (schema: id, name, description, ...), `permissions` (schema: id, code, resource, action, ...)
  - 关联表: `role_permissions`, `user_roles`
  - 代码文件: `src/models/rbac.py`, `src/services/auth_service.py`
  - API 端点: `GET /api/v1/roles`, `POST /api/v1/roles`, `GET /api/v1/permissions`
- **Evidence 状态**: VERIFIED - 权限管理采用 RBAC 模型，权限校验在中间件和网关层统一处理。
- **变更条件**:
  1. 角色或权限变更需经安全团队和业务负责人评审。
  2. 权限变更需进行影响评估，避免过度授权或权限遗漏。
- **风险项**:
  - 权限定义不一致可能导致越权访问或功能不可用。
  - 角色合并或拆分需谨慎，避免权限冲突。

---

## 基线 B: 技术架构 (部分 VERIFIED)

### LOCK-B001: 事件驱动架构 (WebSocket + Event Outbox)

- **决策内容**: 系统采用事件驱动架构处理实时通信和异步任务。前端实时交互使用 WebSocket，后端服务间异步通信使用 Event Outbox 模式（基于 PostgreSQL 事务性发件箱）。
- **证据来源**:
  - 代码文件: `src/services/websocket_manager.py` (WebSocket 管理), `src/services/event_dispatcher.py` (事件分发), `src/models/event_outbox.py` (Outbox 模型)
  - 配置文件: `config/event_bus.yaml`
  - 数据库表: `event_outbox` (用于可靠事件发布)
- **Evidence 状态**: VERIFIED - WebSocket 用于订单状态推送、库存预警等实时场景；Event Outbox 用于订单创建、支付回调等需要可靠投递的异步事件。
- **变更条件**:
  1. 引入新的消息中间件（如 RocketMQ）需评估与现有 Outbox 模式的兼容性。
  2. WebSocket 连接数扩展需考虑负载均衡和连接保持策略。
- **风险项**:
  - 事件丢失或重复投递可能导致业务状态不一致。
  - WebSocket 连接风暴可能影响服务稳定性。

### LOCK-B002: PostgreSQL 主存储

- **决策内容**: 系统主数据库采用 PostgreSQL，作为所有业务数据的持久化存储。利用其 JSONB、全文搜索、事务等特性支持复杂业务需求。
- **证据来源**:
  - 配置文件: `config/database.yaml` (连接配置)
  - 依赖声明: `requirements.txt` (psycopg2, SQLAlchemy)
  - 代码文件: 所有模型文件 (基于 SQLAlchemy ORM)
  - 数据库迁移脚本: `migrations/` 目录
- **Evidence 状态**: VERIFIED - 所有核心业务表均在 PostgreSQL 中，未发现其他关系型数据库作为主存储。
- **变更条件**:
  1. 数据库版本升级需制定兼容性测试和迁移计划。
  2. 引入读写分离或分库分表需评估应用层改造成本。
- **风险项**:
  - 数据库单点故障影响全局服务。
  - 大表性能下降需提前规划索引和分区策略。

### LOCK-B004: OAuth2+JWT

- **决策内容**: 系统认证授权采用 OAuth2 协议（授权码模式）结合 JWT 令牌。用户登录后颁发访问令牌（Access Token）和刷新令牌（Refresh Token），API 网关统一验证 JWT 签名和有效期。
- **证据来源**:
  - 代码文件: `src/services/auth_service.py` (令牌颁发/验证), `src/middleware/jwt_middleware.py` (JWT 验证中间件)
  - 配置文件: `config/auth.yaml` (密钥、过期时间配置)
  - API 端点: `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`
- **Evidence 状态**: VERIFIED - 所有需要认证的 API 均需携带 JWT Bearer Token，令牌验证逻辑在网关层统一处理。
- **变更条件**:
  1. JWT 密钥轮换需制定平滑过渡方案，避免服务中断。
  2. 引入 OAuth2 第三方登录（如微信、支付宝）需评估与现有授权服务器的集成。
- **风险项**:
  - JWT 密钥泄露导致令牌伪造风险。
  - 令牌过期策略不当可能导致用户体验下降或安全风险。

### LOCK-B006: RESTful API

- **决策内容**: 系统对外提供 RESTful 风格的 HTTP API，遵循资源导向设计、统一状态码、分页、过滤、排序等标准约定。API 文档使用 OpenAPI 3.0 规范。
- **证据来源**:
  - 代码文件: `src/api/` 目录 (路由定义), `src/schemas/` 目录 (请求/响应模型)
  - 文档文件: `docs/api/openapi.yaml` (OpenAPI 规范)
  - 配置文件: `config/api.yaml` (版本、分页大小等)
- **Evidence 状态**: VERIFIED - 所有对外接口均符合 RESTful 风格，使用 JSON 格式请求和响应。
- **变更条件**:
  1. API 版本升级（如 v1 -> v2）需保持向老版本兼容或制定迁移时间表。
  2. 引入 GraphQL 或 gRPC 需评估与现有 RESTful 体系的共存方案。
- **风险项**:
  - API 设计不一致增加前端对接成本。
  - 缺乏版本管理可能导致新旧版本冲突。

---

## 附录

### 变更控制流程

1. **提出变更**: 任何对上述 LOCK 的变更建议需提交 RFC (Request for Comments)。
2. **影响评估**: 由架构委员会评估变更对现有系统的影响。
3. **评审批准**: 变更需经技术负责人和业务负责人双重批准。
4. **实施迁移**: 制定详细迁移计划，包括代码变更、数据迁移、测试验证。
5. **文档更新**: 变更完成后更新本文档和相关架构文档。

### 监控与告警

- **数据一致性监控**: 定期检查各真相源数据是否被违规冗余存储。
- **性能监控**: 监控 PostgreSQL 查询性能，及时发现慢查询。
- **安全监控**: 监控 JWT 令牌使用情况，检测异常访问模式。

### 相关文档

- [Decision Baseline Recovery 主文档](./README.md)
- [数据库设计规范](../database-design.md)
- [API 设计规范](../../api/api-design-guidelines.md)
- [安全架构文档](../../security/security-architecture.md)
