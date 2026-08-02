# 门店管理系统 - RBAC权限架构设计 v1.0

## 📌 设计背景

**现状分析**：
- ✅ 后端已有完善的Spring Security + JWT权限框架
- ✅ SecurityUser支持roles + permissions多维度
- ✅ 支持@PreAuthorize、@DataScope等注解
- ❌ 缺少：针对门店场景的角色定义
- ❌ 缺少：招聘模块的权限矩阵设计
- ❌ 缺少：前端权限控制实现

**设计目标**：
1. 定义符合个体餐饮门店的角色体系
2. 设计招聘模块的完整权限矩阵
3. 为未来扩展（HR、区域经理等）预留接口
4. 提供前端权限控制的实现方案

---

## 一、角色体系设计

### 1.1 角色定义原则

```
设计原则：

1. 最小权限原则（Least Privilege）
   每个角色仅拥有完成其工作所需的最小权限集

2. 职责分离（Separation of Duties）
   关键操作（如录用、薪资）需多人协作或更高权限

3. 角色层次化（Role Hierarchy）
   高级角色继承低级角色的所有权限

4. 可扩展性（Extensibility）
   预留自定义角色空间，支持未来业务变化
```

### 1.2 门店场景角色定义

#### 核心角色（4个）

| 角色代码 | 角色名称 | 英文标识 | 适用对象 | 说明 |
|---------|---------|---------|---------|------|
| **OWNER** | **店主/老板** | `owner` | 业主本人 | 全权管理，等同于超级管理员 |
| **STORE_MANAGER** | **店长** | `store_manager` | 店面负责人 | 日常运营全权，但受财务约束 |
| **HR_STAFF** | **人事专员** | `hr_staff` | 人事人员 | 仅招聘相关权限，无运营权限 |
| **STAFF** | **普通员工** | `staff` | 一般员工 | 仅查看自己的信息 |

#### 扩展角色（预留，2个）

| 角色代码 | 角色名称 | 英文标识 | 适用对象 | 说明 |
|---------|---------|---------|---------|------|
| **AREA_MANAGER** | **区域经理** | `area_manager` | 多店管理者 | 跨店数据查看+审批 |
| **FINANCE** | **财务人员** | `finance` | 财务专员 | 薪资查看+成本审核 |

### 1.3 角色层级关系

```
                    ┌─────────────────┐
                    │    OWNER        │  ← 全权限（*）
                    │  (店主/老板)      │
                    └────────┬────────┘
                             │ 继承所有权限
            ┌────────────────┼────────────────┐
            ▼                ▼                ▼
   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
   │STORE_MANAGER │  │  HR_STAFF   │  │  AREA_MGR    │
   │  (店长)      │  │ (人事专员)   │  │ (区域经理)   │
   └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
          │                  │                 │
          ▼                  ▼                 ▼
   ┌─────────────────────────────────────────────┐
   │                STAFF (普通员工)               │
   │              仅查看个人信息                   │
   └─────────────────────────────────────────────┘
```

**继承规则**：
```
OWNER > STORE_MANAGER > STAFF
OWNER > HR_STAFF > STAFF
OWNER > AREA_MANAGER > STORE_MANAGER > STAFF
OWNER > FINANCE (独立分支)
```

---

## 二、招聘模块权限矩阵

### 2.1 功能权限粒度

#### 维度1：功能模块级（Module Level）

| 功能模块 | OWNER | STORE_MGR | HR_STAFF | STAFF |
|---------|-------|-----------|----------|-------|
| **岗位管理** | | | | |
| ├─ 查看岗位列表 | ✅ | ✅ | ✅ | ⚠️ 仅本店 |
| ├─ 创建岗位 | ✅ | ✅ | ❌ | ❌ |
| ├─ 编辑岗位 | ✅ | ✅ | ❌ | ❌ |
| ├─ 删除岗位 | ✅ | ❌ | ❌ | ❌ |
| ├─ 发布/暂停 | ✅ | ✅ | ❌ | ❌ |
| └─ 复制岗位 | ✅ | ✅ | ❌ | ❌ |
| **应聘者管理** | | | | |
| ├─ 查看应聘列表 | ✅ | ✅ | ✅ | ❌ |
| ├─ 查看详情 | ✅ | ✅ | ✅ | ❌ |
| ├─ 手动录入 | ✅ | ✅ | ✅ | ❌ |
| ├─ 安排面试 | ✅ | ✅ | ✅ | ❌ |
| ├─ 面试评价 | ✅ | ✅ | ✅ | ❌ |
| ├─ 发送OFFER | ✅ | ⚠️ 需审批 | ✅ | ❌ |
| ├─ 确认入职 | ✅ | ⚠️ 需审批 | ✅ | ❌ |
| ├─ 淘汰应聘者 | ✅ | ✅ | ✅ | ❌ |
| └─ 导出数据 | ✅ | ✅ | ⚠️ 仅本部门 | ❌ |
| **系统设置** | | | | |
| ├─ 渠道配置 | ✅ | ❌ | ❌ | ❌ |
| └─ 统计报表 | ✅ | ✅ | ⚠️ 仅招聘统计 | ❌ |

**图例说明**：
- ✅ = 有权限
- ❌ = 无权限
- ⚠️ = 受限权限（需满足额外条件）

#### 维度2：操作类型级（Action Level）

对于每个功能，进一步细分操作权限：

```java
// 示例：岗位管理的细粒度权限
recruitment:job:view       // 查看岗位
recruitment:job:create     // 创建岗位
recruitment:job:update     // 编辑岗位
recruitment:job:delete     // 删除岗位
recruitment:job:publish    // 发布/暂停
recruitment:job:copy       // 复制岗位

// 应聘者管理的细粒度权限
recruitment:applicant:view         // 查看应聘者
recruitment:applicant:view_detail  // 查看详情（敏感信息）
recruitment:applicant:create        // 手动录入
recruitment:applicant:interview     // 安排面试
recruitment:applicant:evaluate      // 面试评价
recruitment:applicant:offer         // 发送录用通知
recruitment:applicant:onboard        // 确认入职
recruitment:applicant:reject        // 淘汰
recruitment:applicant:export        // 导出数据
```

#### 维度3：数据范围级（Data Scope Level）

```java
// 数据权限注解使用示例
@DataScope(
  deptId = "#deptId",           // 部门过滤
  storeId = "#storeId",         // 门店过滤
  userId = "#currentUserId"     // 个人数据过滤
)

// 数据范围规则：
// OWNER: ALL_DATA              → 可见所有数据
// STORE_MANAGER: DEPT_DATA      → 可见本店所有数据
// HR_STAFF: CUSTOM_DATA        → 可见自己创建的数据
// STAFF: SELF_DATA             → 仅可见自己的记录
```

### 2.2 敏感操作的特殊规则

#### 规则1：发送OFFER（录用通知）

```
权限要求：
├─ 角色：OWNER / HR_STAFF（直接权限）
├─ STORE_MANAGER：⚠️ 需要OWNER审批或达到阈值
│   · 月薪 ≤ 8000元：可自主决定
│   · 月薪 8000-15000元：需通知OWNER
│   · 月薪 > 15000元：必须OWNER审批
└─ 审批流程（可选）：
    · 创建待审批记录
    · OWNER收到通知
    · OWNER确认/拒绝
    · 通过后正式发送OFFER
```

#### 规则2：确认入职

```
权限要求：
├─ 角色：OWNER / HR_STAFF（直接权限）
├─ STORE_MANAGER：⚠️ 需配合HR或OWNER
│   · 可"预确认"（标记为待入职）
│   · 最终确认需HR或OWNER操作
└─ 数据影响：
    · 修改应聘者状态为 "onboarded"
    · 触发后续流程（创建员工档案、开通账号等）
    · 此操作不可逆（需二次确认）
```

#### 规则3：淘汰应聘者

```
权限要求：
├─ 所有有"应聘者管理"权限的角色都可执行
├─ 但需遵守规则：
│   · 必须选择淘汰原因（强制）
│   · 高匹配度（>80%）淘汰时需填写详细理由
│   · 淘汰后状态变更为 "rejected"
│   · 操作记录到日志（谁、何时、为什么）
└─ 数据保护：
    · 淘汰后数据保留90天（合规要求）
    · 超过期限后物理删除或匿名化
```

---

## 三、数据库设计

### 3.1 角色表（roles）

```sql
CREATE TABLE roles (
  role_id          VARCHAR(32) PRIMARY KEY,  -- 角色ID（雪花算法）
  role_code        VARCHAR(50) NOT NULL UNIQUE,  -- 角色编码（owner/store_manager/hr_staff/staff）
  role_name        VARCHAR(100) NOT NULL,       -- 角色显示名称
  role_description TEXT,                     -- 角色描述
  parent_role_id   VARCHAR(32),                 -- 父角色ID（用于层级继承）
  is_system        BOOLEAN NOT NULL DEFAULT FALSE, -- 是否系统内置角色（不可删除）
  permission_ids   JSONB,                        -- 直接关联的权限ID列表
  data_scope       VARCHAR(20) DEFAULT 'dept',   -- 数据范围（all/dept/custom/self）
  status           SMALLINT NOT NULL DEFAULT 1,   -- 状态（1=启用 0=禁用）
  sort_order       INT DEFAULT 0,                -- 排序权重
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted          INTEGER NOT NULL DEFAULT 0      -- 逻辑删除
);

-- 初始角色数据
INSERT INTO roles (role_id, role_code, role_name, role_description, is_system, data_scope) VALUES
('1000000000000001', 'owner', '店主/老板', '拥有系统全部权限', TRUE, 'all'),
('1000000000000002', 'store_manager', '店长', '负责门店日常运营管理', TRUE, 'dept'),
('1000000000000003', 'hr_staff', '人事专员', '负责招聘相关事务', TRUE, 'custom'),
('1000000000000004', 'staff', '普通员工', '仅查看个人信息', TRUE, 'self');

-- 创建索引
CREATE INDEX idx_roles_code ON roles(role_code);
CREATE INDEX idx_roles_parent ON roles(parent_role_id);
```

### 3.2 权限表（permissions）

```sql
CREATE TABLE permissions (
  permission_id    VARCHAR(32) PRIMARY KEY,
  permission_code  VARCHAR(100) NOT NULL UNIQUE,  -- 权限编码（module:entity:action）
  permission_name VARCHAR(100) NOT NULL,       -- 权限显示名称
  module_name     VARCHAR(50),                  -- 所属模块（recruitment/inventory/finance）
  description     TEXT,                         -- 权限描述
  is_system       BOOLEAN DEFAULT FALSE,         -- 系统内置权限
  status          SMALLINT DEFAULT 1,
  create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted         INTEGER DEFAULT 0
);

-- 招聘模块初始权限数据
INSERT INTO permissions (permission_id, permission_code, permission_name, module_name, description) VALUES
-- 岗位权限
('200000000000001', 'recruitment:job:view', '查看岗位', 'recruitment', '查看岗位列表和详情'),
('200000000000002', 'recruitment:job:create', '创建岗位', 'recruitment', '新增招聘岗位'),
('200000000000003', 'recruitment:job:update', '编辑岗位', 'recruitment', '修改岗位信息'),
('200000000000004', 'recruitment:job:delete', '删除岗位', 'recruitment', '删除岗位（逻辑删除）'),
('200000000000005', 'recruitment:job:publish', '发布岗位', 'recruitment', '发布或暂停招聘'),
('200000000000006', 'recruitment:job:copy', '复制岗位', 'recruitment', '基于模板快速创建'),
-- 应聘者权限
('200000000000007', 'recruitment:applicant:view', '查看应聘者', 'recruitment', '查看应聘者列表'),
('200000000000008', 'recruitment:applicant:view_detail', '查看应聘详情', 'recruitment', '查看详细信息（含手机号）'),
('200000000000009', 'recruitment:applicant:create', '手动录入', 'recruitment', '手动添加应聘记录'),
('200000000000010', 'recruitment:applicant:interview', '安排面试', 'recruitment', '安排面试时间和地点'),
('200000000000011', 'recruitment:applicant:evaluate', '面试评价', 'recruitment', '填写面试评价结果'),
('200000000000012', 'recruitment:applicant:offer', '发送录用', 'recruitment', '发送录用通知/OFFER'),
('200000000000013', 'recruitment:applicant:onboard', '确认入职', 'recruitment', '确认求职者正式入职'),
('200000000000014', 'recruitment:applicant:reject', '淘汰应聘者', 'recruitment', '淘汰不合适的应聘者'),
('200000000000015', 'recruitment:applicant:export', '导出数据', 'recruitment', '导出应聘数据');
```

### 3.3 用户-角色关联表（user_roles）扩展现有表

```sql
-- 如果已存在user_roles表，添加字段
ALTER TABLE user_roles 
  ADD COLUMN IF NOT EXISTS granted_by VARCHAR(32),      -- 授权人ID
  ADD COLUMN IF NOT EXISTS grant_time TIMESTAMP,        -- 授权时间
  ADD COLUMN IF NOT EXISTS grant_reason TEXT;          -- 授权原因（可选）

-- 创建索引
CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);
```

---

## 四、后端实现方案

### 4.1 自定义权限注解（扩展现有@OperationLog）

```java
/**
 * 权限检查注解
 * 用于Controller方法上，声明所需权限
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    
    /**
     * 所需权限编码（支持多个，逗号分隔）
     * 示例："recruitment:job:create"
     * 多个："recruitment:job:create,recruitment:job:update"
     */
    String value();
    
    /**
     * 数据权限策略
     * 默认使用角色配置的data_scope
     * 可在此覆盖：如 "self" 强制仅个人数据
     */
    String dataScope() default "";
    
    /**
     * 是否记录操作日志
     */
    boolean logOperation() default true;
}
```

### 4.2 权限拦截器（AOP切面）

```java
/**
 * 权限检查切面
 * 拦截带@RequiresPermission注解的方法调用
 */
@Aspect
@Component
@Slf4j
public class PermissionCheckAspect {

    @Around("@annotation(requiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, RequiresPermission requiresPermission) throws Throwable {
        
        // 1. 获取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthenticationException("未登录");
        }
        
        SecurityUser currentUser = (SecurityUser) auth.getPrincipal();
        
        // 2. 检查是否为超级管理员（跳过权限检查）
        if (currentUser.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("*"))) {
            log.debug("用户[{}]是管理员，跳过权限检查", currentUser.getUsername());
            return joinPoint.proceed();
        }
        
        // 3. 解析所需权限
        String[] requiredPermissions = requiresPermission.value().split(",");
        
        // 4. 逐一检查权限
        for (String perm : requiredPermissions) {
            boolean hasPerm = checkSinglePermission(currentUser, perm.trim());
            if (!hasPerm) {
                log.warn("用户[{}]缺少权限[{}]", currentUser.getUsername(), perm);
                throw new AccessDeniedException("权限不足：" + perm);
            }
        }
        
        // 5. 权限通过，执行原方法
        Object result = joinPoint.proceed();
        
        // 6. 记录操作日志（如果启用）
        if (requiresPermission.logOperation()) {
            recordOperationLog(currentUser, joinPoint, requiredPermissions);
        }
        
        return result;
    }
    
    private boolean checkSinglePermission(SecurityUser user, String permission) {
        // 检查用户权限列表
        return user.getPermissions() != null && 
               user.getPermissions().contains(permission);
    }
}
```

### 4.3 Controller层应用示例

```java
@RestController
@RequestMapping("/api/recruitments")
@Tag(name = "招聘管理")
public class RecruitmentController {

    /**
     * 创建岗位 - 需要 recruitment:job:create 权限
     */
    @PostMapping
    @RequiresPermission("recruitment:job:create")
    @OperationLog(module = "recruitment", type = OperationType.CREATE, desc = "创建招聘岗位")
    public Result<RecruitmentVO> createJob(@Valid @RequestBody RecruitmentCreateDTO dto) {
        // 业务逻辑...
    }

    /**
     * 发送OFFER - 需要 recruitment:applicant:offer 权限
     * 且对STORE_MANAGER角色有薪资阈值限制
     */
    @PostMapping("/{id}/offer")
    @RequiresPermission(value = "recruitment:applicant:offer", 
                       dataScope = "custom")  // 自定义数据范围
    public Result<Void> sendOffer(
        @PathVariable String id,
        @RequestBody OfferDTO dto
    ) {
        // 1. 检查当前用户角色
        // 2. 如果是STORE_MANAGER，检查薪资是否超阈值
        // 3. 超阈值则创建审批记录而非直接发送
        // ...
    }

    /**
     * 确认入职 - 敏感操作，需二次确认
     */
    @Patch("/{applicantId}/onboard")
    @RequiresPermission("recruitment:applicant:onboard")
    public Result<Void> confirmOnboarding(
        @PathVariable String applicantId,
        @RequestBody OnboardDTO dto
    ) {
        // 1. 二次确认（前端传confirm: true）
        // 2. 修改状态为 onboarded
        // 3. 触发后续流程（创建员工档案等）
        // ...
    }
}
```

---

## 五、前端权限控制方案

### 5.1 用户信息存储结构（Pinia Store）

```typescript
// stores/user.ts

interface UserInfo {
  id: string
  username: string
  name: string
  roles: RoleInfo[]           // 角色列表
  permissions: string[]        // 权限编码列表
  dataScope: DataScopeConfig  // 数据范围配置
}

interface RoleInfo {
  roleId: string
  roleCode: string           // owner / store_manager / hr_staff / staff
  roleName: string
  parentId?: string
  dataScope: 'all' | 'dept' | 'custom' | 'self'
}

interface DataScopeConfig {
  type: 'all' | 'dept' | 'custom' | 'self'
  deptIds?: string[]           // 可访问的部门ID列表
  storeIds?: string[]          // 可访问的门店ID列表
}
```

### 5.2 权限判断Composable

```typescript
// composables/usePermission.ts

import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 权限检查Hook
 * @example
 * const { hasPerm, hasAnyPerm, hasAllPerm } = usePermission()
 * if (hasPerm('recruitment:job:create')) { ... }
 */
export function usePermission() {
  const userStore = useUserStore()

  /** 检查是否有指定权限 */
  function hasPerm(permission: string): boolean {
    return userStore.permissions.includes(permission) ||
           userStore.roles.some(r => r.roleCode === 'owner')
  }

  /** 检查是否有任一权限 */
  function hasAnyPerm(permissions: string[]): boolean {
    return permissions.some(p => hasPerm(p))
  }

  /** 检查是否拥有所有权限 */
  function hasAllPerm(permissions: string[]): boolean {
    return permissions.every(p => hasPerm(p))
  }

  /** 检查是否拥有指定角色 */
  function hasRole(roleCode: string): boolean {
    return userStore.roles.some(r => r.roleCode === roleCode)
  }

  /** 检查数据范围 */
  function canAccessDataScope(scopeType: string): boolean {
    return userStore.dataScope?.type === scopeType || 
           userStore.dataScope?.type === 'all'
  }

  return {
    hasPerm,
    hasAnyPerm,
    hasAllPerm,
    hasRole,
    canAccessDataScope
  }
}
```

### 5.3 Vue组件中的权限控制

#### 方式1：指令式控制（v-if/v-show）

```vue
<template>
  <!-- 按钮：根据权限显示/隐藏 -->
  <el-button 
    v-if="hasPerm('recruitment:job:create')"
    type="primary"
    @click="openCreateDialog"
  >
    新增岗位
  </el-button>

  <el-button 
    v-if="hasPerm('recruitment:applicant:offer')"
    :disabled="!canSendOfferDirectly"
    @click="sendOffer(currentApplicant)"
  >
    发送OFFER
  </el-button>
  
  <!-- 无权限时的提示 -->
  <el-tooltip 
    v-else
    content="您没有此操作的权限，请联系管理员"
    placement="top"
  >
    <span class="disabled-btn-wrapper">
      <el-button disabled>发送OFFER</el-button>
    </span>
  </el-tooltip>
</template>

<script setup lang="ts">
import { usePermission } from '@/composables/usePermission'

const { hasPerm } = usePermission()
</script>
```

#### 方式2：路由级别权限（导航守卫）

```typescript
// router/permission.ts

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  // 检查页面是否需要特定权限
  if (to.meta.requiredPermission) {
    const hasPerm = userStore.permissions.includes(to.meta.requiredPermission)
    
    if (!hasPerm) {
      // 无权限：重定向到403页面或显示提示
      ElMessage.error('您没有访问该页面的权限')
      next({ path: '/403', query: { from: to.fullPath } })
      return
    }
  }
  
  // 检查角色
  if (to.meta.requiredRole) {
    const hasRole = userStore.roles.some(r => r.roleCode === to.meta.requiredRole)
    if (!hasRole) {
      next('/403')
      return
    }
  }
  
  next()
})

// 路由配置示例
{
  path: '/settings',
  component: SystemSettings,
  meta: { 
    title: '系统设置',
    requiredPermission: 'system:settings:view',
    requiredRole: 'owner'  // 仅店主可访问
  }
}
```

#### 方式3：API请求层面拦截

```typescript
// api/request.ts 的响应拦截器中

// 当后端返回403时
if (response.status === 403) {
  const errorCode = response.data?.code
  
  if (errorCode === 40301) {  // 权限不足
    ElMessage.warning('您没有执行此操作的权限')
    // 可选：隐藏对应按钮或禁用功能
    permissionDeniedEvent.emit(response.data?.permission)
  }
}
```

### 5.4 动态菜单/路由生成

```typescript
// 根据权限动态生成侧边栏菜单
const menuItems = computed(() => [
  {
    title: '岗位管理',
    icon: Briefcase,
    path: '/recruitment/jobs',
    permission: 'recruitment:job:view',
    visible: hasPerm('recruitment:job:view') || hasRole('owner')
  },
  {
    title: '应聘者管理',
    icon: UserFilled,
    path: '/recruitment/applicants',
    permission: 'recruitment:applicant:view',
    visible: hasPerm('recruitment:applicant:view') || hasRole('owner')
  },
  {
    title: '渠道配置',
    icon: Connection,
    path: '/recruitment/channels',
    permission: 'recruitment:channel:manage',
    visible: hasPerm('recruitment:channel:manage') || hasRole('owner')
  },
  {
    title: '系统设置',  // 仅OWNER可见
    icon: Setting,
    path: '/admin/settings',
    permission: 'system:settings:view',
    visible: hasRole('owner')  // 仅店主角色
  }
].filter(item => item.visible !== false)
```

---

## 六、实施路线图

### Phase 1：基础框架（本周）

```
□ 后端：
  ├─ 创建 roles 和 permissions 表（Flyway迁移脚本）
  ├─ 实现 PermissionCheckAspect 切面
  ├─ 创建 @RequiresPermission 注解
  └─ 在现有Controller上添加注解（先从招聘模块开始）

□ 前端：
  ├─ 扩展 user store（增加 roles/permissions 字段）
  ├─ 实现 usePermission composable
  ├─ 在 StoreRecruitment.vue 中应用权限控制
  └─ 测试不同角色的可见性

预计工作量：3-4天
```

### Phase 2：完整集成（下周）

```
□ 后端：
  ├─ 实现角色管理CRUD API（管理员用）
  ├─ 实现用户-角色分配API
  ├─ 添加数据权限过滤（MyBatis-Plus DataPermission）
  └─ 完善操作日志记录

□ 前端：
  ├─ 角色管理页面（仅OWNER可用）
  ├─ 动态路由/菜单生成
  ├─ 403错误页面优化
  └─ 权限变更实时刷新（无需重新登录）

预计工作量：5-7天
```

### Phase 3：高级特性（可选，按需）

```
□ 审批工作流引擎（OFFER/入职的多级审批）
□ 权限申请流程（普通员工临时申请特殊权限）
□ 操作审计面板（谁在什么时候做了什么）
□ 数据脱敏展示（STAFF看不到手机号完整显示）
□ IP/设备绑定（增强安全性）

预计工作量：10-15天
```

---

## 七、当前阶段的简化实施建议

考虑到您目前是**单人管理（OWNER角色）**，建议采用**渐进式实施**：

### MVP阶段（立即实施）

```
✅ 已经具备：
  ├─ Spring Security + JWT认证框架
  ├─ SecurityUser 支持 roles + permissions
  ├─ CustomPermissionEvaluator 权限评估器
  └─ @OperationLog 操作日志

📝 本周只需做：
  1. 定义角色常量（枚举或配置文件）
  2. 在招聘Controller上添加 @PreAuthorize 占位注解
  3. 前端 usePermission composable（轻量版）
  4. 在按钮上添加 v-if="hasPerm(...)" 控制

⏸️ 暂不做的：
  ├─ 不建 roles/permissions 表（先用硬编码）
  ├─ 不做角色管理UI（后台直接改库）
  ├─ 不做审批流（OWNER直接操作）
  └─ 不做数据权限过滤（全量数据）
```

### 何时升级？

```
触发条件（满足任一即考虑升级）：
□ 第二个账号需要登录系统（如店长）
□ 需要区分"可看"和"可操作"的场景
□ 开始招聘多名员工（需要HR角色）
□ 开设第二家分店（需要区域经理）

升级成本预估：
  - 从MVP到Phase 1：2-3天
  - 从Phase 1到Phase 2：5-7天
  - 数据迁移风险：低（向后兼容）
```

---

## 八、测试用例

### 8.1 角色权限测试矩阵

```
测试场景1：OWNER（店主）登录
├─ 可见：所有按钮和菜单 ✅
├─ 可执行：创建/编辑/删除/发送OFFER/确认入职 ✅
└─ 数据范围：所有门店所有数据 ✅

测试场景2：STORE_MANAGER（店长）登录
├─ 可见：岗位管理、应聘者管理 ✅
├─ 不可见：系统设置、渠道配置 ❌
├─ 可执行：创建岗位、安排面试、淘汰 ✅
├─ 不可执行：删除岗位 ❌
├─ 条件执行：发送OFFER（月薪≤8K可直接） ⚠️
└─ 数据范围：仅本店数据 ✅

测试场景3：HR_STAFF（人事）登录
├─ 可见：应聘者管理 ✅
├─ 不可见：岗位管理（只读）、系统设置 ❌
├─ 可执行：查看、手动录入、安排面试、淘汰 ✅
├─ 不可执行：删除应聘者 ❌
├─ 特殊权限：发送OFFER、确认入职 ✅
└─ 数据范围：仅自己创建的应聘记录 ✅

测试场景4：STAFF（普通员工）登录
├─ 可见：仅个人信息页 ❌（或极简版）
├─ 不可见：所有管理功能 ❌
├─ 可执行：修改自己的密码/手机号 ✅
└─ 数据范围：仅自己的记录（如有） ✅
```

### 8.2 边界情况测试

```
□ 未登录用户访问 → 重定向到登录页
□ Token过期用户操作 → 返回401并提示重新登录
□ 并发权限变更 → 下次请求生效（或强制刷新）
□ 角色被禁用 → 下次登录后降为最低权限
□ 尝试越权操作（篡改请求参数） → 后端拒绝并记录日志
□ SQL注入绕过权限 → MyBatis-Plus DataPermission拦截
```

---

## 九、安全注意事项

### 9.1 防御措施

```
✅ 前端：
  ├─ 权限信息存储在内存（Pinia），不持久化到LocalStorage
  ├─ 敏感操作需二次确认（对话框）
  ├─ 权限不足时隐藏按钮（而非仅禁用，防止逆向工程）
  └─ 所有权限判断在后端再次验证（不信任前端）

✅ 后端：
  ├─ 权限检查在AOP切面统一处理（避免遗漏）
  ├─ 使用@PreAuthorize注解（编译期校验）
  ├─ 操作日志记录所有敏感操作
  └─ 定期审计权限分配合理性

❌ 禁止：
  ├─ 在URL中暴露权限码（如 ?permission=admin）
  ├─ 在API响应中返回完整权限列表（仅返回当前需要的）
  └─ 前端权限判断作为唯一防线（必须是双重验证）
```

### 9.2 性能优化

```
权限缓存策略：
├─ 用户登录时加载完整权限列表（一次性）
├─ 存储在Redis/JWT Token中（避免每次查询DB）
├─ 权限变更时清除缓存（强制下次刷新）
└─ 权限检查时间复杂度：O(1)（内存Hash查找）

预估性能影响：
├─ 单次请求额外耗时：< 1ms（内存查找）
├─ 内存占用增量：< 50KB/用户（权限字符串数组）
└─ 数据库查询增量：仅在登录时（一次）
```

---

## 十、文档与维护

### 相关文档清单

| 文档名 | 内容 | 更新频率 |
|--------|------|---------|
| **RBAC-ARCHITECTURE.md** ⭐ | 本文档：完整权限架构设计 | 架构变更时 |
| **ROLE-MATRIX.xlsx** | 各角色权限矩阵Excel（便于非技术人员查阅） | 角色调整时 |
| **PERMISSION-LIST.md** | 全局权限编码清单 | 新增功能时 |
| **API-PERMISSION-MAP.md** | API端点 ↔ 权限对照表 | API变更时 |

---

**最后更新**: 2026-05-16
**版本**: v1.0
**适用范围**: 门店管理系统（招聘模块优先实施）
**下一步**: 实施 Phase 1 基础框架
