# 多端权限架构设计文档

> **文档版本**: v1.0
> **创建日期**: 2026-06-04
> **状态**: 设计稿（待评审）
> **适用范围**: 员工端(employee-frontend)权限体系重构 + 多端协作架构对齐

---

## 目录

1. [背景与问题](#1-背景与问题)
2. [系统全景](#2-系统全景)
3. [核心原则](#3-核心原则)
4. [角色模型定义](#4-角色模型定义)
5. [JWT Token 结构设计](#5-jwt-token-结构设计)
6. [前后端权限边界划分](#6-前后端权限边界划分)
7. [员工端权限架构重构方案](#7-员工端权限架构重构方案)
8. [数据隔离规范](#8-数据隔离规范)
9. [多端协作场景](#9-多端协作场景)
10. [迁移路径图](#10-迁移路径图)
11. [附录：当前代码审计清单](#11-附录当前代码审计清单)

---

## 1. 背景与问题

### 1.1 项目定位

食品溯源系统是一个**多端协作的餐饮企业管理平台**，不是单一的"管理后台"或"员工自助终端"。系统包含 5 个前端应用，共享同一个后端服务：

| 端 | 目录 | 端口 | 用户群体 |
|---|------|:----:|---------|
| **员工端** | `employee-frontend/` | 3006 | 服务员、厨师、收银员、店长 |
| **管理端** | `frontend/` | 3000 | HR、财务、运营、老板 |
| **后厨端** | `frontend-kitchen/` | - | 后厨团队 |
| **POS端** | `frontend-pos/` | - | 收银员 |
| **小程序** | `miniprogram/` | - | 外部客户 |

### 1.2 当前问题诊断

#### 问题 A：两套独立的权限体系平行运行

```
【现状】
员工端 (employee-frontend)          管理端 (frontend/)
├─ usePermissionStore               ├─ stores/auth.ts (不同的实现)
│  ├─ UserLevel(5级枚举)            │  ├─ role-based menu
│  ├─ LEVEL_CONFIG_MAP              │  ├─ dict permissions
│  ├─ canApproveOthers()            │  └─ 完全不同的权限判断逻辑
│  ├─ generateChain()               │
│  └─ setRole()/setUserLevel()      │
└─ useAuthContext (刚创建)           └─ 无 AuthContext

        ↕ 两套体系互不通信，各自硬编码角色和规则
```

**后果**：
- 切换账号角色时，员工端的 mock 数据过滤不跟随变化
- 管理端审批通过后，员工端看不到最新状态（因为数据源不同）
- 同一个用户在两个端看到的数据范围不一致

#### 问题 B：前端承担了本应属于后端的职责

| 职责 | 当前位置 | 正确位置 | 风险等级 |
|------|---------|---------|:-------:|
| 用户层级定义 (`UserLevel`) | 前端 TypeScript 枚举 | 后端数据库 + API 返回 | 中 |
| 审批金额上限配置 (`LEVEL_CONFIG_MAP`) | 前端硬编码 Map | 后端业务规则引擎 | 高 |
| 审批链路生成 (`generateChain()`) | 前端算法 | 后端工作流引擎 | 高 |
| "我能审批谁" (`canApproveOthers()`) | 前端计算 | 后端 RBAC 查询 | 高 |
| 角色切换 (`setRole()`) | 前端可调用 | 仅管理员操作（后端） | 高 |
| 数据范围过滤 ("我发起的") | 前端 `filter()` | 后端 SQL `WHERE` | 中 |

#### 问题 C：Mock 阶段埋下的技术债务

```typescript
// employee-frontend/src/api/mock/db.ts — 第19行
const CURRENT_USER_NAME = '张三'  // ❌ 硬编码

// employee-frontend/src/api/approval.ts — 第27行
const CURRENT_USER = '张三'       // ❌ 又一份硬编码（名字还不同！）

// employee-frontend/src/api/mock/approvals.ts — 第29行
const CURRENT_USER_NAME = '张三'   // ❌ 第三份！
```

三处独立常量，改一处其他两处不跟着变。

### 1.3 业务需求澄清

> **关键认知修正**：员工端不是"纯消费端"，而是**多端协作系统中的一个参与方**。
>
> 员工在员工端提交申请 → 数据写入共享数据库 → 管理端看到待审批 → 管理端审批通过 → 数据库更新 → 员工端刷新看到已通过
>
> 这意味着**员工端和管理端必须使用同一套 RBAC 模型、同一套数据源、同一套权限语义**。

---

## 2. 系统全景

### 2.1 已有的后端安全基础设施

后端已经实现了完整的安全模块（位于 `backend/src/main/java/com/example/demo/security/`）：

```
security/
├── config/
│   ├── SecurityConfig.java        ← HTTP 安全配置（CORS/Csrf/Session）
│   ├── JwtConfig.java             ← JWT 配置（密钥/过期时间）
│   └── MfaConfig.java             ← MFA 双因素认证配置
├── filter/
│   ├── JwtAuthenticationFilter.java ← JWT 认证过滤器（每次请求校验 Token）
│   ├── MfaAuthenticationFilter.java ← MFA 二次验证过滤器
│   ├── RateLimitFilter.java       ← 接口限流过滤器
│   ├── AnomalyAccessLogFilter.java ← 异常访问日志
│   └── SecurityHeaderFilter.java  ← 安全响应头
├── model/
│   ├── SecurityUser.java          ← Spring Security UserDetails 实现
│   ├── JwtToken.java              ← JWT Token DTO
│   └── MfaToken.java              ← MFA Token DTO
├── service/
│   ├── AuthenticationService.java   ← 登录认证接口
│   ├── TokenService.java           ← Token 创建/验证/刷新
│   ├── MfaService.java            ← MFA 验证码服务
│   └── UserPermissionCacheService.java ← 权限缓存（Redis）
├── annotation/
│   ├── OperationLog.java          ← 操作日志注解
│   └── DataScope.java             ← 数据权限注解
├── aspect/
│   ├── OperationLogAspect.java    ← 操作日志切面
│   └── DataScopeAspect.java       ← 数据权限切面（自动追加 WHERE 条件）
└── utils/
    ├── JwtUtils.java              ← JWT 工具类
    ├── PasswordUtils.java         ← 密码工具
    ├── MfaUtils.java              ← MFA 工具
    └── AesEncryptionUtil.java     ← AES 加密工具
```

### 2.2 后端已有的 RBAC 种子

`SecurityUser` 模型已经包含完整的权限字段：

```java
public class SecurityUser implements UserDetails {
    private String userId;        // 用户唯一标识
    private String username;      // 登录名
    private String name;          // 显示姓名
    private List<String> roles;     // 角色列表 ["employee", "manager", ...]
    private List<String> permissions; // 权限列表 ["approval:create", "salary:view", ...]
}
```

Controller 层已有 `@PreAuthorize` 注解：

```java
@GetMapping("/my/page")
@PreAuthorize("hasRole('employee')")  // ✅ 已经有角色级控制
public Result<?> getMyApprovals(...) { }

@PostMapping("/{id}/approve")
@PreAuthorize("hasAuthority('approval:approve')")  // ✅ 权限级控制
public Result<?> approveApproval(...) { }
```

但 `reviewerId` 仍是 TODO 占位符——**后端权限体系尚未完全对接前端**。

### 2.3 目标架构图

```
┌─────────────────────────────────────────────────────────────┐
│                     共享后端 (Spring Boot)                   │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │ JWT 认证  │  │ RBAC 权限 │  │ 数据隔离  │  │ 事件同步  │    │
│  │ Filter   │  │ @PreAuth  │  │ DataScope│  │ RabbitMQ │    │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘    │
│       │              │              │              │         │
│  ┌────┴──────────────┴──────────────┴──────────────┴────┐  │
│  │                  PostgreSQL (单一真相源)                │  │
│  └─────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
         ▲                    ▲                    ▲
         │                    │                    │
  ┌──────┴──────┐    ┌──────┴──────┐    ┌──────┴──────┐
  │  员工端      │    │  管理端      │    │  其他端      │
  │  :3006      │    │  :3000      │    │  POS/厨房/小程序│
  │              │    │              │    │              │
  │ useAuthCtx  │    │ stores/auth │    │ 各自轻量适配   │
  │ (身份消费)  │    │ (权限消费)  │    │              │
  └─────────────┘    └─────────────┘    └──────────────┘
```

---

## 3. 核心原则

### 3.1 三层权限模型

```
┌─────────────────────────────────────────────┐
│ Layer 3: UI 展示权限（前端）                   │
│ • Tab 显示/隐藏                              │
│ • 按钮 可见/不可见                            │
│ • 字段 只读/可编辑                            │
│                                             │
│ ⚠️ 这是体验优化，不是安全屏障                   │
│    绕过前端 ≠ 绕过后端                         │
├─────────────────────────────────────────────┤
│ Layer 2: 业务权限（后端 Service）              │
│ • 能否发起某类审批                            │
│ • 能否审批他人的申请                          │
│ • 金额上限是多少                              │
│ • 审批链路如何生成                            │
│                                             │
│ ✅ 所有业务规则在此层执行                      │
├─────────────────────────────────────────────┤
│ Layer 1: 数据权限（后端 Mapper/DataScope）    │
│ • 能查哪些人的数据                           │
│ • 能看哪些字段（脱敏）                        │
│ • 行级过滤（WHERE applicant_id = ?）          │
│                                             │
│ ✅ 即使绕过 Service，SQL 层也保证数据隔离      │
└─────────────────────────────────────────────┘
```

### 3.2 关键原则清单

| # | 原则 | 说明 |
|---|------|------|
| P1 | **后端是唯一的权限仲裁者** | 前端不做任何安全相关的决策，仅做 UI 适配 |
| P2 | **Token 是身份数据的唯一载体** | 角色信息从 JWT 解码获取，不在前端维护状态 |
| P3 | **数据范围由后端查询决定** | "我发起的"是 `WHERE applicant_id = current_user_id`，不是前端 filter |
| P4 | **共享同一套 RBAC 模型** | 员工端和管理端使用相同的 role 定义和 permission 字符串 |
| P5 | **前端权限仅用于优化体验** | 隐藏无权按钮避免用户点击后报 403，但不作为安全依赖 |
| P6 | **Mock 阶段允许简化** | 开发阶段可在前端模拟权限行为，但架构上预留后端对接点 |

---

## 4. 角色模型定义

### 4.1 共享角色定义（所有端共用）

```typescript
/**
 * 系统角色定义 — 所有端共享此枚举
 *
 * 注意：这是前端展示用的轻量定义。
 * 权威的角色列表由后端 /v1/auth/profile 接口返回。
 */
export enum SystemRole {
  /** 普通员工（服务员/厨师/收银员） */
  EMPLOYEE = 'employee',
  /** 组长/领班 */
  TEAM_LEAD = 'team_lead',
  /** 店长/经理 */
  MANAGER = 'store_manager',
  /** HR 人事 */
  HR = 'hr',
  /** 财务 */
  FINANCE = 'finance',
  /** 运营 */
  OPERATIONS = 'operations',
  /** 老板/超级管理员 */
  ADMIN = 'admin',
}

/**
 * 员工端内部使用的层级概念（仅用于 UI 条件渲染）
 *
 * 与 SystemRole 的映射关系：
 *   L1(STAFF)  → EMPLOYEE, TEAM_LEAD 的部分能力
 *   L2(SUPERVISOR) → TEAM_LEAD, 部分 MANAGER 能力
 *   L3(MANAGER) → MANAGER 及以上
 *
 * ⚠️ 此枚举仅存在于员工端，用于控制 UI 显示逻辑（如是否显示部门汇总 Tab）。
 *    不用于任何业务判断或安全相关逻辑。
 */
export enum EmployeeLevel {
  STAFF = 1,       // 普通员工：只能发起、查看自己的数据
  SUPERVISOR = 2,  // 主管/领班：可审批同级、查看团队数据
  MANAGER = 3,     // 经理：可审批全部、查看部门全量数据
  DIRECTOR = 4,    // 总监：跨门店
  ADMIN = 5,       // 超管：全部权限
}
```

### 4.2 角色与权限矩阵

| 操作 | EMPLOYEE | TEAM_LEAD | MANAGER | HR | FINANCE | ADMIN |
|------|:--------:|:---------:|:-------:|:--:|:-------:|:-----:|
| 发起审批 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 查看"我发起的" | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 查看"已完成"(自己) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 查看"已完成"(团队) | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |
| 审批他人申请 | ❌ | ✅(同级) | ✅(全部) | 部分 | ❌ | ✅ |
| 查看部门汇总 | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |
| 修改审批流程 | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| 切换用户角色 | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |

### 4.3 Permission 字符串规范

后端使用细粒度 permission 字符串（Spring Security `hasAuthority()`），前端仅在需要做 UI 条件渲染时引用：

```typescript
/** 审批模块权限 */
export const ApprovalPermissions = {
  CREATE: 'approval:create',       // 发起审批
  VIEW_OWN: 'approval:view:own',   // 查看自己的
  VIEW_TEAM: 'approval:view:team', // 查看团队的（L3+）
  APPROVE: 'approval:approve',     // 审批操作
  WITHDRAW: 'approval:withdraw',   // 撤回
  URGE: 'approval:urge',           // 催办
} as const

/** 薪资模块权限 */
export const SalaryPermissions = {
  VIEW_OWN: 'salary:view:own',
  VIEW_DEPT: 'salary:view:dept',
  EXPORT: 'salary:export',
} as const
```

---

## 5. JWT Token 结构设计

### 5.1 Token Payload 结构

```json
{
  "sub": "u001",
  "username": "zhangsan",
  "name": "张三",
  "roles": ["employee"],
  "permissions": [
    "approval:create",
    "approval:view:own",
    "approval:withdraw",
    "approval:urge",
    "leave:create",
    "leave:view:own",
    "overtime:create",
    "salary:view:own"
  ],
  "level": 1,
  "departmentId": "d001",
  "positionId": "p003",
  "storeId": "s001",
  "iat": 1712345678,
  "exp": 1712349278
}
```

### 5.2 前端解析后的类型定义

```typescript
/** 从 JWT Token 解析出的用户身份信息 */
export interface UserProfile {
  /** 用户 ID */
  userId: string
  /** 登录名 */
  username: string
  /** 显示姓名 */
  name: string
  /** 角色列表（来自后端，权威） */
  roles: SystemRole[]
  /** 权限列表（来自后端，权威） */
  permissions: string[]
  /** 员工层级（仅员工端使用，用于 UI 渲染） */
  level: EmployeeLevel
  /** 所属部门 ID */
  departmentId: string
  /** 职位 ID */
  positionId: string
  /** 门店 ID */
  storeId: string
}

/** 登录接口返回的完整响应 */
export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserProfile
}
```

### 5.3 Token 刷新机制

```
Access Token (30min) ──过期──→ 使用 Refresh Token (7天) 自动刷新
                                        ↓
                                   新 Access Token
                                   （前端拦截器自动处理，
                                    用户无感知）
```

---

## 6. 前后端权限边界划分

### 6.1 职责分工表

| 功能域 | 后端负责 | 前端负责 | 不应该出现的位置 |
|--------|---------|---------|---------------|
| **身份认证** | JWT 签发/验证/刷新 | 存储 Token 到 localStorage | 前端自行计算 token |
| **角色判定** | `@PreAuthorize("hasRole('xxx')")` | 根据 `userProfile.roles` 控制 UI 显隐 | 前端 `if (role === 'manager')` 做业务分支 |
| **权限检查** | `@PreAuthorize("hasAuthority('xxx')")` | 有/无权限时隐藏按钮 | 前端弹窗提示"你无权限"然后放行 |
| **数据范围** | `DataScopeAspect` 自动追加 SQL WHERE | 接收返回的数据直接渲染 | 前端 `.filter(item => item.applicantName === me)` |
| **审批链路** | 根据企业制度配置动态生成 | 展示后端返回的 flowNodes 数组 | 前端 `generateChain()` 硬编码算法 |
| **金额限制** | 业务规则引擎校验 | 表单提交前做友好提示（非强制） | 前端 `LEVEL_CONFIG_MAP[level].maxAmount` |
| **菜单导航** | 返回该角色可见的路由/菜单配置 | 根据配置渲染侧边栏 | 前端硬编码路由表 + roles 过滤 |
| **操作日志** | `@OperationLog` 切面自动记录 | 无需关心 | 前端手动打点 |

### 6.2 API 接口的权限标注规范

每个 Controller 方法必须有明确的权限注解：

```java
// ========== 员工端专用接口 ==========

/** 获取"我发起的"审批列表 */
@GetMapping("/v1/approvals/my/page")
@PreAuthorize("hasRole('employee')")           // 所有登录员工可用
@Operation(summary = "获取我的审批列表")
public Result<PageResult<ApprovalVO>> getMyApprovals(
    @AuthenticationPrincipal SecurityUser user  // 从 Token 取身份
) {
    // Service 层自动追加: WHERE applicant_id = user.getUserId()
    return Result.success(approvalService.getMyPage(user, query));
}

/** 发起审批 */
@PostMapping("/v1/approvals")
@PreAuthorize("hasAuthority('approval:create')")
public Result<ApprovalVO> createApproval(
    @AuthenticationPrincipal SecurityUser user,
    @Valid @RequestBody ApprovalCreateDTO dto
) {
    dto.setApplicantId(user.getUserId());  // 后端设置申请人，不接受前端传入
    return Result.success(approvalService.create(dto));
}

/** 审批操作（通过/驳回） */
@PutMapping("/v1/approvals/{id}/approve")
@PreAuthorize("hasAuthority('approval:approve')")
public Result<Void> approve(
    @PathVariable String id,
    @AuthenticationPrincipal SecurityUser user,  // 审批人从 Token 取
    @RequestBody ApproveRequest body
    // 注意：body 中不再有 reviewerId 字段！
) {
    approvalService.approve(id, user.getUserId(), body.getComment());
    return Result.success();
}


// ========== 管理端专用接口 ==========

/** 获取待审批列表（管理者视角）*/
@GetMapping("/v1/approvals/pending-review/page")
@PreAuthorize("hasAnyRole('manager', 'hr', 'admin')")
public Result<PageResult<ApprovalVO>> getPendingReview(...) { }

/** 获取部门统计 */
@GetMapping("/v1/approvals/stats/dept")
@PreAuthorize("hasAnyRole('manager', 'hr', 'finance', 'admin')")
public Result<ApprovalStatsVO> getDeptStats(...) { }
```

### 6.3 敏感字段安全规则

| 场景 | 后端处理 | 前端处理 |
|------|---------|---------|
| 申请人 ID | 返回给本人和管理者；其他人看到脱敏值 | 直接展示后端返回值 |
| 金额（分→元） | 存分，VO 转元返回 | 用 DataConverter 展示 |
| 手机号 | 非本人/非管理者脱敏为 `138****1234` | 直接展示 |
| 审批意见 | 全文返回给相关人员 | 直接展示 |

---

## 7. 员工端权限架构重构方案

### 7.1 重构目标

将员工端从"自建权限引擎"模式转变为"消费后端权限"模式：

```
【重构前】                          【重构后】
┌──────────────┐                   ┌──────────────┐
│ permissionStore│                   │ useAuthContext│ (精简版)
│ ├─ UserLevel  │                   │ ├─ userProfile│ (从 JWT/store 获取)
│ ├─ LevelConfig│                   │ ├─ hasPerm()  │ (检查权限字符串)
│ ├─ canApprove │                   │ └─ isManager  │ (UI 渲染用)
│ ├─ generateChain                    │              │
│ └─ setRole()  │                   └──────┬───────┘
└──────────────┘                          │
                                           ▼
                                  ┌──────────────────┐
                                  │   后端 API 返回    │
                                  │   {               │
                                  │     roles: [...],  │
                                  │     permissions:[],│
                                  │     level: 1,     │
                                  │     name: "张三"   │
                                  │   }               │
                                  └──────────────────┘
```

### 7.2 useAuthContext 重构规格

```typescript
/**
 * useAuthContext - 员工端统一身份上下文（重构后）
 *
 * 职责：
 * 1. 存储/提供当前用户的身份信息（从登录接口/JWT 获取）
 * 2. 提供便捷的权限检查方法（用于 UI 条件渲染）
 * 3. Mock 阶段提供 switchIdentity() 用于测试
 *
 * 不负责：
 * - 业务规则判断（如"能否审批超过500元的申请"）→ 后端
 * - 数据范围过滤（如"只能看自己的数据"）→ 后端 SQL
 * - 审批链路生成 → 后端工作流引擎
 */

interface AuthContextState {
  /** 用户资料（权威来源：登录API / JWT解码） */
  userProfile: Ref<UserProfile | null>

  /** 是否已登录 */
  isAuthenticated: ComputedRef<boolean>

  /** 是否有指定权限（UI 渲染用） */
  hasPermission(permission: string): boolean

  /** 是否有任一指定权限 */
  hasAnyPermission(permissions: string[]): boolean

  /** 是否为经理及以上（UI 渲染用：显示部门汇总等） */
  isManager: ComputedRef<boolean>

  /** 是否有审批权限（UI 渲染用：显示"待我审批"Tab） */
  canApprove: ComputedRef<boolean>

  /**
   * 初始化身份信息（从登录响应或 localStorage 恢复）
   * 对接真实后端时，从 POST /v1/auth/login 的响应中提取
   */
  initialize(profile: UserProfile): void

  /**
   * 清除身份信息（登出时调用）
   */
  clear(): void

  /**
   * Mock 阶段：切换身份（测试多角色场景）
   * 生产环境应移除此方法
   */
  switchIdentity(options: SwitchIdentityOptions): void
}
```

### 7.3 文件变更清单

| 文件 | 操作 | 说明 |
|------|:----:|------|
| `composables/useAuthContext.ts` | **修改** | 精简为纯身份消费者，移除硬编码，改为从外部初始化 |
| `stores/permission.ts` | **大幅删减** | 保留 `UserLevel` 和 `EmployeeRole` 类型定义；删除 `LEVEL_CONFIG_MAP`、`canApproveOthers()`、`generateChain()`、`setUserLevel()`、`setRole()` |
| `types/permission.ts` | **保留** | 类型定义文件，无需修改 |
| `api/mock/db.ts` | **修改** | `getApprovalsByTab()` 改为接收 `options.currentUserName` 参数（已在上轮完成） |
| `api/mock/approvals.ts` | **保留不变** | `CURRENT_USER_NAME` 仅用于种子数据生成（合理用法） |
| `api/approval.ts` | **修改** | 从 `auth.userName.value` 获取身份（已在上轮完成） |
| `views/ApprovalPage.vue` | **微调** | 移除手动传递 `userLevel`（已在上轮完成） |
| `views/approval/components/ApprovalList.vue` | **保留不变** | `shouldShowApplicant` 是纯 UI 逻辑（合理用法） |
| `views/approval/ApprovalDetailPage.vue` | **保留不变** | `canWithdraw` 使用 `auth.userName.value`（已在上轮完成） |

### 7.4 permission.ts 保留 vs 删除的内容

```typescript
// ====== 保留 ======

/** 员工层级枚举（UI 渲染用） */
export enum UserLevel { STAFF, SUPERVISOR, MANAGER, DIRECTOR, ADMIN }

/** 员工职位枚举（UI 展示用） */
export enum EmployeeRole {
  STORE_STAFF = 'store_staff',    // 服务员
  KITCHEN_STAFF = 'kitchen_staff', // 厨师
  CASHIER = 'cashier',            // 收银员
  TEAM_LEAD = 'team_lead',        // 领班
  STORE_MANAGER = 'store_manager', // 店长
  HR = 'hr',                       // 人事
  FINANCE = 'finance',             // 财务
  BOSS = 'boss',                   // 老板
}

// ====== 删除 ======

// ❌ LEVEL_CONFIG_MAP — 金额上限/审批范围 → 后端业务规则
// ❌ canApproveOthers(targetLevel) — 审批权限 → 后端 @PreAuthorize
// ❌ generateChain(approvalType) — 审批链路 → 后端工作流引擎
// ❌ setUserLevel(level) — 角色切换 → 仅管理员操作
// ❌ setRole(role) — 角色切换 → 仅管理员操作
// ❌ getLevelInfo(level) — 层级详情 → 后端 API 返回
// ❌ levelInfo computed — 同上
```

---

## 8. 数据隔离规范

### 8.1 各 Tab 的数据范围规则

| Tab | L1 员工 | L2 主管 | L3 经理 | L4 总监 | L5 超管 |
|-----|:-------:|:-------:|:-------:|:-------:|:-------:|
| **我发起的** | 自己 | 自己 | 自己 | 自己 | 自己 |
| **待我审批** | 不可见 | 同级 | 全部 | 全部 | 全部 |
| **已完成** | 自己 | 自己 | 团队全员 | 跨门店 | 全部 |
| **部门汇总** | 不可见 | 不可见 | 本部门 | 跨部门 | 全部 |
| **CC 抄送** | 不可见 | 不可见 | 相关 | 相关 | 全部 |

### 8.2 后端 SQL 过滤示例

```sql
-- "我发起的"：Service 层自动追加
SELECT * FROM employee_approvals
WHERE applicant_id = :currentUserId
  AND deleted = 0
ORDER BY create_time DESC;

-- "已完成" + L1/L2：同上（仅自己的）

-- "已完成" + L3+：团队全员
SELECT * FROM employee_approvals
WHERE store_id = :currentStoreId
  AND status IN ('approved', 'rejected', 'cancelled')
  AND deleted = 0
ORDER BY create_time DESC;
```

### 8.3 前端 UI 适配规则

基于后端返回的数据范围，前端决定**如何在视觉上呈现申请人信息**：

```typescript
// ApprovalList.vue — shouldShowApponent 逻辑（最终版）
const shouldShowApplicant = computed(() => {
  if (props.activeTab === 'initiated') return false           // 我发起的：隐含本人
  if (props.activeTab === 'completed' && !isManager.value) return false  // L1/L2 已完成：仅自己的
  return true                                                    // 其他情况：需要区分
})
```

---

## 9. 多端协作场景

### 9.1 审批流程的多端协作

```
时间线 →

┌─ 员工端 ──────────────────────────────────────────────────┐
│                                                          │
│  张三 打开请假页面                                         │
│    ↓                                                      │
│  填写年假申请表单（选择 06-10 ~ 06-12，共3天）             │
│    ↓                                                      │
│  点击"提交" → POST /v1/approvals                          │
│    ↓                                                      │
│  页面跳转到"我发起的"Tab → 看到"年假申请 [待审批]"          │
│    ↓                                                      │
│  ......等待中......                                       │
│    ↓                                                      │
│  刷新页面 → 状态变为"已通过" ✓                             │
│                                                          │
└──────────────────────────────────────────────────────────┘
                           │
                           ▼ POST (RabbitMQ 事件)
                           │
┌─ 后端 ────────────────────────────────────────────────────┐
│                                                          │
│  1. 写入 employee_approvals 表 (status=pending)           │
│  2. 生成审批链路: 张三 → 王店长 → (HR 备案)               │
│  3. 推送 MQ 消息: { type: 'approval.created', ... }        │
│  4. 设置 current_approver_id = 王店长的 userId             │
│                                                          │
│  ......等待中......                                       │
│                                                          │
│  5. 收到 PUT /v1/approvals/{id}/approve                   │
│     (来自管理端，reviewer = 王店长)                        │
│  6. 更新 status = approved                                │
│  7. 推送 MQ 消息: { type: 'approval.approved', ... }       │
│  8. 扣减张三年假余额                                      │
│                                                          │
└──────────────────────────────────────────────────────────┘
                           │
                           ▼ MQ 事件通知
                           │
┌─ 管理端 ─────────────────────────────────────────────────┐
│                                                          │
│  王店长 打开审批中心                                       │
│    ↓                                                      │
│  "待我审批"Tab → 看到"张三 年假申请 [待审批]"             │
│    ↓                                                      │
│  点击进入详情 → 查看假期余额/年度统计/政策标准             │
│    ↓                                                      │
│  点击"通过" → PUT /v1/approvals/{id}/approve              │
│    ↓                                                      │
│  状态变为"已处理"                                         │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

### 9.2 数据一致性保障

| 机制 | 说明 | 实现方式 |
|------|------|---------|
| **单一数据库** | 所有端读写同一个 PostgreSQL | 无需额外同步 |
| **MQ 事件通知** | 状态变更后推送消息 | RabbitMQ `approval.{action}` |
| **乐观锁** | 并发审批冲突检测 | `version` 字段 + `@Version` |
| **Redis 缓存** | 热数据缓存 + 失效通知 | Cache + Pub/Sub |
| **前端轮询/WebSocket** | 实时状态更新 | 开发阶段用轮询，生产用 WS |

### 9.3 各端看到的同一份数据

以 `ap0001`（张三的出差申请）为例：

| 字段 | 员工端(张三) | 管理端(王店长) | 管理端(HR) |
|------|------------|--------------|-----------|
| title | 出差申请 | 出差申请 | 出差申请 |
| applicantName | *(隐藏)* | 张三 | 张三 |
| status | approved | approved | approved |
| 审批意见 | "同意，注意控制预算" | *(自己填的)* | "同意，注意控制预算" |
| contextData | 完整显示 | 完整显示 | 完整显示 |
| 可执行操作 | 查看详情 / 查看排班 | *(已处理)* | 归档查看 |

---

## 10. 迁移路径图

### 10.1 分阶段实施计划

```
Phase 0: 当前状态（2026-06-04）
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
✅ useAuthContext 已创建（基础版）
✅ db.ts 过滤逻辑已参数化
✅ approval.ts 已引用 auth 单例
✅ Title 格式已统一（纯类别名）
✅ ApprovalList shouldShowApplicant 已按角色区分
⚠️  permission.ts 仍包含大量应删除的业务逻辑


Phase 1: 权限 Store 精化（本次）
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 任务:
  [ ] 删除 permission.ts 中的 LEVEL_CONFIG_MAP
  [ ] 删除 canApproveOthers() / generateChain()
  [ ] 删除 setUserLevel() / setRole() / setRole()
  [ ] 保留 UserLevel / EmployeeRole 枚举（类型定义）
  [ ] useAuthContext 增加 initialize(profile) 方法
  [ ] useAuthContext 增加 hasPermission() 方法
  [ ] 更新所有引用处


Phase 2: 后端对接准备（后续）
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 任务:
  [ ] 完善 EmployeeApprovalController 的 reviewerId 逻辑
  [ ] 补充 @DataScope 注解到数据查询方法
  [ ] 实现 /v1/auth/profile 接口（返回完整 UserProfile）
  [ ] 实现 /v1/auth/login 返回 permissions 列表
  [ ] 补充审批模块的 @PreAuthorize 注解到所有端点


Phase 3: 员工端对接后端（后续）
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📋 任务:
  [ ] 登录流程改造：存储完整 UserProfile 到 useAuthContext
  [ ] request 拦截器：自动附加 Authorization header
  [ ] API 层移除 mock 数据，改为真实后端调用
  [ ] 删除 api/mock/ 目录中的业务逻辑（保留 seed 数据）
  [ ] useAuthContext.switchIdentity() 标记 @deprecated 或条件编译移除
```

### 10.2 代码迁移对照表

| 当前代码 | Phase 1 变为 | Phase 3 变为 |
|---------|-------------|-------------|
| `permission.LEVEL_CONFIG_MAP` | **删除** | N/A |
| `permission.canApproveOthers()` | **删除** | N/A |
| `permission.generateChain()` | **删除** | N/A |
| `permission.setUserLevel()` | **删除** | N/A |
| `permission.setRole()` | **删除** | N/A |
| `permission.levelInfo` (computed) | **删除** | N/A |
| `permission.UserLevel` (enum) | **保留** | **保留** |
| `permission.EmployeeRole` (enum) | **保留** | **保留** |
| `useAuthContext().userName` (hardcoded) | 改为 `userProfile.name` | 从 login API 获取 |
| `useAuthContext().userLevel` (hardcoded) | 改为 `userProfile.level` | 从 login API 获取 |
| `db.ts.CURRENT_USER_NAME` | **已删除**（上轮完成） | N/A |
| `approval.ts.CURRENT_USER` | **已删除**（上轮完成） | N/A |
| `approvals.ts.CURRENT_USER_NAME` | **保留**（仅种子数据） | **保留** |

---

## 11. 附录：当前代码审计清单

### 11.1 需要清理的硬编码身份引用

| 文件 | 行号 | 内容 | 处理方式 |
|------|:----:|------|---------|
| `stores/permission.ts` | ~120-180 | `LEVEL_CONFIG_MAP` | **Phase 1 删除** |
| `stores/permission.ts` | ~200-230 | `canApproveOthers()` | **Phase 1 删除** |
| `stores/permission.ts` | ~240-280 | `generateChain()` | **Phase 1 删除** |
| `stores/permission.ts` | ~290-310 | `setUserLevel()` / `setRole()` | **Phase 1 删除** |
| `views/approval/ApprovalDetailPage.vue` | 61 | `'张三'` | **已修复** → `auth.userName.value` |
| `api/mock/__tests__/test-data.ts` | 多处 | `'张三'` | **已修复**（测试数据合理） |

### 11.2 已完成的修复项（本轮）

| # | 修复内容 | 状态 |
|:-:|---------|:----:|
| 1 | 创建 `useAuthContext.ts` 单例身份模块 | ✅ |
| 2 | 统一所有 title 为纯类别名（19条） | ✅ |
| 3 | `db.ts` 过滤逻辑参数化（不再硬编码用户名） | ✅ |
| 4 | `approval.ts` 引用 auth 单例 | ✅ |
| 5 | `ApprovalList.shouldShowApplicant` 按角色区分 | ✅ |
| 6 | ViewSwitcher 改为纯文字 Tab 风格 | ✅ |
| 7 | PolicyStandardCard 通用组件抽取 | ✅ |

### 11.3 文档关联

| 文档 | 关联性 |
|------|-------|
| [docs/spec/09-认证与安全.md](../../spec/09-认证与安全.md) | 后端安全基础设施详细设计 |
| [docs/spec/06-API接口设计.md](../../spec/06-API接口设计.md) | RESTful API 权限注解规范 |
| [docs/spec/04-系统架构.md](../../spec/04-系统架构.md) | 整体分层架构 |
| [docs/admin-dev/034-configurable-permission-architecture/spec.md](../034-configurable-permission-architecture/spec.md) | 管理端权限架构（已有设计） |
| `backend/.../security/AuthenticationModuleDesign.md` | 后端认证模块设计文档 |

---

> **文档维护说明**：本文档随员工端权限重构进度同步更新。每完成一个 Phase 的任务后，对应章节的状态标记应更新。
