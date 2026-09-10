# 食品溯源系统 - 权限/数据范围地图

> 最后更新：2026-09-09

---

## 一、权限基础设施概览

| 指标 | 数值/状态 | 说明 |
|------|-----------|------|
| 角色数 | 16 | 部署基线角色 |
| 权限码 | 180+ | 细粒度权限控制 |
| 数据范围级别 | 8 | ALL → NONE 递减 |
| 权限注解 | 4 类 | @RequiresPermission / @RequiresPermissions / @RequiresRole / @RequiresDataScope |
| AOP 切面 | 3 | PermissionAspect / DataScopeAspect / DataPermissionAspect |
| 权限模板 | permission_templates | @Table 持久化 |
| 用户覆盖 | user_permission_overrides | 角色级覆盖 |

---

## 二、角色体系 (16 角色部署基线)

### 2.1 核心角色矩阵

| 角色代码 | 角色名称 | 数据范围 | 说明 |
|----------|----------|----------|------|
| SUPER_ADMIN / ROLE_ADMIN / Z | 超级管理员 | ALL (100) | 系统最高权限，跳过所有过滤 |
| ROLE_OPERATIONS_DIRECTOR | 运营总监 | COMPANY (80) | 全公司数据访问 |
| ROLE_REGIONAL_MANAGER | 区域经理 | STORE (60) | 区域内门店数据 |
| ROLE_STORE_MANAGER | 店长 | STORE (60) | 单门店数据管理 |
| ROLE_MANAGER | 管理员 | STORE (60) | 门店管理操作 |
| ROLE_CHEF | 厨师长 | DEPARTMENT (40) | 后厨部门数据 |
| ROLE_KITCHEN | 厨师 | DEPARTMENT (40) | 后厨操作数据 |
| ROLE_CASHIER | 收银员 | STORE (60) | 收银/财务数据 |
| ROLE_POS_OPERATOR | POS 操作员 | STORE (60) | POS 终端数据 |
| ROLE_HOST | 迎宾/领班 | STORE (60) | 前厅接待数据 |
| ROLE_PURCHASE | 采购员 | DEPARTMENT (40) | 采购数据 |
| ROLE_WAREHOUSE | 仓管员 | DEPARTMENT (40) | 仓储数据 |
| ROLE_STAFF / STORE_CLERK | 普通员工 | SELF (20) | 仅个人数据 |
| ROLE_HR | 人事专员 | DEPARTMENT (40) | HR 部门数据 |
| ROLE_FINANCE | 财务专员 | DEPARTMENT (40) | 财务部门数据 |
| ROLE_DELIVERY | 配送员 | SELF (20) | 仅配送任务数据 |

### 2.2 角色 → 数据范围级别映射

```
ALL          (100) ──── SUPER_ADMIN
COMPANY      (80)  ──── ROLE_OPERATIONS_DIRECTOR
STORE        (60)  ──── ROLE_REGIONAL_MANAGER / ROLE_STORE_MANAGER / ROLE_MANAGER / ROLE_CASHIER / ROLE_POS_OPERATOR / ROLE_HOST
DEPARTMENT   (40)  ──── ROLE_CHEF / ROLE_KITCHEN / ROLE_PURCHASE / ROLE_WAREHOUSE / ROLE_HR / ROLE_FINANCE
SELF_AND_SUB (40)  ──── (预留，当前无角色使用)
CUSTOM       (30)  ──── (预留，通过 user_permission_overrides 实现)
SELF         (20)  ──── ROLE_STAFF / ROLE_DELIVERY
NONE         (10)  ──── (无数据权限，禁止访问)
```

---

## 三、权限模型

### 3.1 `@PreAuthorize` 域:动作模型

权限码格式：`domain:action`

| 域 (Domain) | 动作 (Action) 示例 | 说明 |
|--------------|-------------------|------|
| system:user | create / read / update / delete / reset-password / update-status / assign-store / assign-roles | 用户管理 |
| system:role | create / update / delete / read / update-status / assign-permissions / set-data-scope | 角色管理 |
| system:permission | create / read / update / delete / update-status / update-sort | 权限管理 |
| system:database | delete | 数据库维护 |
| store:pending | manage | 门店待办 |
| store:status | view | 门店状态 |
| store:log | view | 门店日志 |
| store:settlement | manage | 日结对账 |
| store:announcement | manage | 门店公告 |
| store:certificate | manage | 证件管理 |
| store:recruitment | manage | 门店招聘 |

### 3.2 权限注解类型

| 注解 | 用途 | 示例 |
|------|------|------|
| `@RequiresPermission(value, action)` | 单权限码校验 | `@RequiresPermission(value = "system:user:create", action = "create")` |
| `@RequiresPermissions(values, logical)` | 多权限码校验 (AND/OR) | `@RequiresPermissions(values = {"user:read", "user:write"}, logical = Logical.AND)` |
| `@RequiresRole` | 角色校验 | 角色白名单检查 |
| `@RequiresDataScope(resourceType)` | 数据范围校验 | `@RequiresDataScope(resourceType = "store")` |
| `@DataScope(type, tableAlias)` | SQL 注入级数据过滤 | `@DataScope(type = DataScopeType.STORE, storeIdColumn = "store_id")` |

---

## 四、数据范围 (Data Scope) 系统

### 4.1 八级数据范围

| 级别 | 范围标识 | Rank | 说明 | SQL 行为 |
|------|----------|------|------|----------|
| 1 | ALL | 100 | 全部数据 | 无过滤 |
| 2 | COMPANY | 80 | 公司数据 | `company_id = ?` |
| 3 | REGION | 70 | 区域数据 | (预留) |
| 4 | STORES / DEPARTMENTS | 60 | 多门店/多部门 | (预留多值) |
| 5 | STORE | 40 | 单门店 | `store_id = ?` |
| 6 | DEPARTMENT | 40 | 单部门 | `department_id = ?` |
| 7 | SELF_AND_SUBORDINATE | 40 | 本人及下属 | `user_id IN (self, subordinates)` |
| 8 | CUSTOM | 30 | 自定义 | 动态 SQL |
| 9 | SELF | 20 | 仅自己 | `create_by = ?` 或 `user_id = ?` |
| 10 | NONE | 10 | 无权限 | `1 = 0` |

### 4.2 数据范围绑定机制

```
┌─────────────────────────────────────────────────────────────────┐
│                    数据范围数据源                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  users.store_id  ──────────┐                                    │
│                             ├──→ DataPermissionAspect           │
│  role_stores (无 migration) ┤    (自动 QueryWrapper 过滤)        │
│                             │                                    │
│  role_departments           ├──→ DataScopeAspect                │
│    (无 migration)           │    (SQL 注入级过滤)                │
│                             │                                    │
│  role.dataScope             │    (角色级数据范围定义)            │
│    ↓                        │                                    │
│  PermissionAspect           │    (级别校验: userLevel >= required)│
│                             │                                    │
│  @DataScope(type=AUTO)      │    (自动从角色推断最宽松范围)      │
│                             │                                    │
└─────────────────────────────────────────────────────────────────┘
```

### 4.3 存储表说明

| 表名 | 说明 | Migration 状态 |
|------|------|---------------|
| `users.store_id` | 用户绑定门店 | 有列 |
| `role_stores` | 角色-门店关联 | **无 migration** |
| `role_departments` | 角色-部门关联 | **无 migration** |
| `permission_templates` | 权限模板 | @Table 持久化 |
| `user_permission_overrides` | 用户权限覆盖 | @Table 持久化 |
| `employee_data_scope` | 员工数据范围 | 动态建表 (DatabaseFixConfig) |

---

## 五、域可见性与前端路由守卫

### 5.1 DOMAIN_VISIBLE_ROLES

前端路由守卫使用 `DOMAIN_VISIBLE_ROLES` 控制菜单可见性：

```
前端路由守卫机制：
  ┌──────────────────────────────────────────────────────┐
  │  用户登录 → 获取角色列表                              │
  │       ↓                                              │
  │  DOMAIN_VISIBLE_ROLES 映射表                         │
  │    "store-ops" → [ROLE_STORE_MANAGER, ROLE_STAFF]    │
  │    "finance"   → [ROLE_FINANCE, ROLE_ADMIN]          │
  │    "hr"        → [ROLE_HR, ROLE_ADMIN]               │
  │    ...                                                │
  │       ↓                                              │
  │  过滤用户角色 → 仅显示匹配域的菜单                     │
  └──────────────────────────────────────────────────────┘
```

### 5.2 员工可见性规则

| 终端 | 放行角色 | 说明 |
|------|----------|------|
| Employee 终端 | employee 仅 `workspace/store-ops` 放行 | 员工自助仅限工作台和门店运营 |
| POS 终端 | `ROLE_ADMIN, ROLE_MANAGER, ROLE_CASHIER, ROLE_POS_OPERATOR` | `@PreAuthorize` 强制校验 |
| Kitchen 终端 | `ROLE_ADMIN, ROLE_MANAGER, ROLE_CHEF, ROLE_KITCHEN` | `@PreAuthorize` 强制校验 |
| MiniProgram | 公开 (GET), 写操作需认证 | 小程序端简化流程 |

---

## 六、权限验证流程 (8 层安全链)

```
┌─────────────────────────────────────────────────────────────────┐
│                     HTTP 请求进入                                │
└───────────────────────────┬─────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  ① DeviceWhitelistFilter                                        │
│     - 设备白名单校验                                             │
│     - 非白名单设备直接拒绝                                       │
└───────────────────────────┬─────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  ② RateLimitFilter                                              │
│     - 速率限制 (IP/USER/ALL 维度)                               │
│     - 基于内存计数器实现                                         │
└───────────────────────────┬─────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  ③ JwtAuthenticationFilter                                      │
│     - JWT Token 解析                                            │
│     - 过期/签名验证                                             │
│     - 构建 SecurityContext                                      │
└───────────────────────────┬─────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  ④ MfaAuthenticationFilter                                      │
│     - 多因素认证检查 (需 MFA 的操作)                             │
│     - 二次验证弹窗                                              │
└───────────────────────────┬─────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  ⑤ Spring Security 委托鉴权                                     │
│     - @PreAuthorize("hasAnyRole(...)")                          │
│     - @PreAuthorize("hasAnyAuthority(...)")                     │
│     - URL 级别 .authenticated() / .permitAll()                  │
└───────────────────────────┬─────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  ⑥ PermissionAspect (AOP)                                       │
│     - @RequiresPermission → PermissionVerifyService             │
│     - @RequiresPermissions → 多码校验                           │
│     - @RequiresRole → 角色白名单                                │
│     - @RequiresDataScope → 数据范围级别校验                      │
│       (userLevel >= requiredLevel)                              │
└───────────────────────────┬─────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  ⑦ DataScopeAspect (AOP)                                       │
│     - @DataScope → SQL 注入级数据过滤                           │
│     - 生成 WHERE 存入 DataScopeContext (ThreadLocal)             │
│     - 自动从角色推断最宽松范围 (type=AUTO)                       │
└───────────────────────────┬─────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  ⑧ DataPermissionAspect (AOP)                                   │
│     - 匹配 *ServiceImpl.select*/list*/page* 方法                │
│     - 自动向 QueryWrapper 添加 store_id / department_id 过滤    │
│     - 非管理员强制过滤，管理员跳过                               │
└───────────────────────────┬─────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    业务逻辑执行                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 七、权限缺口 (BLOCKED_PRODUCT_DECISION)

以下权限缺口需产品决策，当前标记为 BLOCKED：

| 编号 | 缺口类型 | 方法数 | 现状 | 待产品决策 |
|------|----------|--------|------|-----------|
| PD-006 | 裸接口权限 | 25 | 接口无 @RequiresPermission 注解 | 需确定域归属和权限码 |
| PD-007 | 申诉接口域归属 | 5 | `/v1/appeals/*` 无前端消费，域归属不明 | 需确定是否保留或归属哪个域 |
| PD-008 | 供应商 H5 外部认证 | 6 | SupplierPortalController H5 端点无认证 | 需确定外部供应商的认证方式 |
| PD-009 | 通用审批权限模型 | 13 | 审批流程无统一权限模型 | 需设计通用审批权限框架 |
| PD-010 | 退款审批身份归因 | — | 退款审批人身份归因不明确 | 需确定审批人身份归属逻辑 |
| — | 发布动作审计 | — | 发布操作无 @AuditLog 审计 | 需补充审计日志 |

### 7.1 裸接口权限 (PD-006) - 25 方法

需逐个确认以下接口的权限码归属：

| 接口域 | 待确认方法数 | 示例接口 |
|--------|-------------|----------|
| self-purchase | 6 | `/v1/self-purchase/*` |
| plan-items | 7 | `/v1/plan-items/*` |
| tasks | 12 | `/v1/tasks/*` |

### 7.2 供应商 H5 外部认证 (PD-008) - 6 端点

```
SupplierPortalController H5 端点：
  /h5/contract    ← 供应商查看合同
  /h5/view        ← 供应商查看详情
  /h5/verify      ← 供应商身份验证
  /h5/sign        ← 供应商签署
  /h5/reject      ← 供应商拒绝
  /h5/...
  
问题：外部供应商无系统账号，H5 端点无 JWT 认证
待定：Token-based 认证？短信验证码？邀请链接？
```

---

## 八、权限基础设施架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        权限基础设施全景                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────┐    ┌──────────────┐    ┌──────────────────┐       │
│  │ role_stores  │    │permission_    │    │ user_permission_ │       │
│  │ (角色-门店)  │    │templates      │    │ overrides        │       │
│  │ 无migration  │    │(权限模板)     │    │ (用户覆盖)       │       │
│  └──────┬──────┘    └──────┬───────┘    └────────┬─────────┘       │
│         │                  │                      │                  │
│         ▼                  ▼                      ▼                  │
│  ┌─────────────────────────────────────────────────────────┐       │
│  │              PermissionVerifyService                      │       │
│  │  - getUserPermissions(userId)                            │       │
│  │  - getUserDataScope(userId)                              │       │
│  │  - AdminPermissions.WILDCARD (*) → 管理员跳过所有校验    │       │
│  └────────────────────────┬────────────────────────────────┘       │
│                           │                                         │
│         ┌─────────────────┼─────────────────┐                      │
│         ▼                 ▼                 ▼                      │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐             │
│  │ Permission   │  │ DataScope    │  │ DataPermission│             │
│  │ Aspect       │  │ Aspect       │  │ Aspect        │             │
│  │ (级别校验)   │  │ (SQL注入)    │  │ (QueryWrapper)│             │
│  └─────────────┘  └──────────────┘  └──────────────┘             │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 九、安全过滤器链配置

| 过滤器 | 位置 | 职责 |
|--------|------|------|
| DeviceWhitelistFilter | addFilterBefore(UsernamePassword) | 设备白名单校验 |
| RateLimitFilter | addFilterBefore(UsernamePassword) | 速率限制 |
| JwtAuthenticationFilter | addFilterBefore(UsernamePassword) | JWT 解析与认证 |
| MfaAuthenticationFilter | addFilterAfter(JwtAuthentication) | 多因素认证 |

### 公开端点 (SecurityConfig 白名单)

| 端点 | 说明 |
|------|------|
| `/v1/auth/**` | 登录/注册 |
| `/v1/public/**` | 公开 API |
| `/v1/wecom/**` | 企业微信集成 |
| `/v1/test/**` | 测试接口 |
| `/v1/foods/**` | 菜品查询 (GET) |
| `/v1/suppliers/list` | 供应商列表 |
| `/v1/departments/tree` | 部门树 |
| `/v1/kitchen/**` (GET) | 厨房查询 |
| `/v1/kitchen-order/**` (GET) | 厨房订单查询 |
| `/v1/orders/**` (GET) | 订单查询 |
| `/v1/mp/**` | 小程序端 |
| `/actuator/health` | 健康检查 |

---

## 十、权限码分布 (按域)

| 域 | 权限码数 | 说明 |
|-----|----------|------|
| system:user | 8 | 用户 CRUD + 分配 |
| system:role | 7 | 角色 CRUD + 分配 |
| system:permission | 6 | 权限 CRUD |
| system:database | 1 | 数据库维护 |
| store:* | 7 | 门店运营 |
| finance:* | ~20 | 财务模块 |
| hr:* | ~15 | 人力资源 |
| purchase:* | ~10 | 采购管理 |
| inventory:* | ~8 | 库存管理 |
| order:* | ~12 | 订单管理 |
| marketing:* | ~10 | 营销模块 |
| pos:* | ~8 | POS 收银 |
| kitchen:* | ~6 | 厨房管理 |
| traceability:* | ~5 | 溯源模块 |
| 设备/通知/资产等 | ~20+ | 其他模块 |
| **总计** | **180+** | |

---

## 十一、风险与关注点

| 风险点 | 说明 | 优先级 |
|--------|------|--------|
| role_stores 无 migration | 表存在但无 Flyway 迁移脚本 | P1 |
| role_departments 无 migration | 表存在但无 Flyway 迁移脚本 | P1 |
| 25 裸接口方法 (PD-006) | 无权限注解保护 | P0 |
| 6 供应商 H5 端点无认证 (PD-008) | 外部接口安全缺口 | P0 |
| 退款审批身份归因 (PD-010) | 审计追踪不完整 | P1 |
| 发布动作无 @AuditLog | 操作审计缺失 | P2 |
| DataPermissionAspect 匹配范围过广 | *ServiceImpl.select*/list*/page* 可能误匹配 | P2 |
| employee_data_scope 动态建表 | 非 Flyway 管理，可能在不同环境不一致 | P2 |

---

*生成时间：2026-09-09*
*数据来源：代码库静态分析 + 实际代码结构验证*
