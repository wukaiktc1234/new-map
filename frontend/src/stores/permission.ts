import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type { MenuGroupConfig, MenuOverride, MenuTemplateConfig, ScaleLevel, SubMenuItem } from '@/types/permission'
import { safeJsonParse } from '@/utils/storage'
import { permissionTemplateApi, parseRoleConfig } from '@/api/system/permission-template'

/**
 * 权限模板后端数据结构（已转换后）
 * - templateCode: 模板编码（与后端 code 对应）
 * - templateName: 模板名称（与后端 name 对应）
 * - description: 模板描述
 * - domainMatrix: 角色→可见域列表 映射（由后端 roleConfig JSON 解析得到）
 *
 * 4 种系统模式：centralized-single / standard-chain / large-chain / custom
 */
interface PermissionTemplateBackend {
  templateCode: string
  templateName: string
  description: string
  domainMatrix: Record<string, string[]>
}

/**
 * 用户角色枚举
 */
export enum UserRole {
  /** 超级管理员/老板 */
  OWNER = 'owner',
  /** 超级管理员 (系统内置) */
  ADMIN = 'admin',
  /** 运营总监 */
  OPS_DIRECTOR = 'ops_director',
  /** 采购部经理 */
  PURCHASE_MANAGER = 'purchase_manager',
  /** 部门经理 */
  DEPARTMENT_MANAGER = 'department_manager',
  /** 仓储部经理 */
  WAREHOUSE_MANAGER = 'warehouse_manager',
  /** 财务总监 */
  FINANCE_DIRECTOR = 'finance_director',
  /** HR总监 */
  HR_DIRECTOR = 'hr_director',
  /** 店长/门店经理 */
  STORE_MANAGER = 'store_manager',
  /** 组长/团队负责人 */
  TEAM_LEADER = 'team_leader',
  /** 排班员 (可选) */
  SCHEDULER = 'scheduler',
  /** 区域经理 */
  REGION_MANAGER = 'region_manager',
  /** 稽查专员 */
  AUDITOR = 'auditor',
  /** 普通员工 */
  EMPLOYEE = 'employee',
}

/**
 * 组织层级枚举
 */
export enum OrgLevel {
  /** 超级管理员 (L5) */
  OWNER = 'OWNER',
  /** 公司级管理者 (L4) */
  COMPANY = 'COMPANY',
  /** 部门级管理者 (L3) */
  DEPARTMENT = 'DEPARTMENT',
  /** 门店级管理者 (L3) */
  STORE_MGR = 'STORE_MGR',
  /** 门店执行层 (L2) */
  STORE_STAFF = 'STORE_STAFF',
}

/**
 * 数据范围枚举
 */
export enum DataScope {
  /** 全部数据 */
  ALL = 'all',
  /** 本公司数据 */
  COMPANY = 'company',
  /** 本部门数据 */
  DEPARTMENT = 'department',
  /** 本门店数据 */
  STORE = 'store',
  /** 指定门店 */
  STORES = 'stores',
  /** 仅本人数据 */
  SELF = 'self',
}

/** 用户信息接口 */
interface UserInfo {
  userId: string | number
  username: string
  fullName?: string
  email?: string
  roles: UserRole[]
  permissions: string[]
  orgLevel?: OrgLevel
  dataScope?: DataScope
  storeId?: string
  storeName?: string
  departmentId?: string
  departmentName?: string
}

export const usePermissionStore = defineStore('permission', () => {
  // ========== 状态定义 ==========
  
  /** 用户信息 */
  const userInfo = ref<UserInfo | null>(null)
  
  /** 是否已加载 */
  const loaded = ref(false)
  
  /** 加载中状态 */
  const loading = ref(false)

  // ========== 菜单配置状态 ==========

  /** 已注册的菜单组列表（由各模块 menu.ts 通过 registerMenuGroup() 注册） */
  const registeredMenus = reactive<MenuGroupConfig[]>([])
  const menuVersion = ref(0)

  /** 已解析的菜单缓存 */
  const resolvedMenus = ref<MenuGroupConfig[]>([])

  /** 用户级菜单覆盖（存储在localStorage） */
  const userMenuOverrides = ref<MenuOverride[]>([])

  /** 当前应用的预设模板ID（空=不使用模板） */
  const currentTemplate = ref<string>('')

  /**
   * 权限模板列表（从后端 /v1/permission-templates/system 获取）
   * 4 种系统模式：centralized-single / standard-chain / large-chain / custom
   * 通过 fetchPermissionTemplates() 异步加载
   */
  const permissionTemplates = ref<PermissionTemplateBackend[]>([])

  /** 模板加载状态 */
  const templatesLoading = ref(false)
  const templatesLoaded = ref(false)

  /**
   * 角色×域权限矩阵（roleConfig 解析后）
   * 结构: { [roleCode]: { [domainCode]: 'FULL' | 'HIDDEN' } }
   * 仅当前应用的模板对应的矩阵写入此处，用于菜单覆盖
   */
  const roleDomainMatrix = ref<Record<string, Record<string, string>>>({})

  /** 审计日志记录（F-006: 操作日志） */
  interface AuditLogEntry {
    id: string
    timestamp: Date
    eventType: string
    eventName: string
    operatorName: string
    targetRole?: string
    targetUser?: string
    targetDomain?: string
    detail: string
    riskLevel: 'HIGH' | 'MEDIUM' | 'LOW'
    ipAddress?: string
    userAgent?: string
  }
  const auditLogs = ref<AuditLogEntry[]>([])

  /** 菜单注册锁定标志（F-008: 初始化完成后禁止动态注册） */
  const registrationLocked = ref(false)

  /** 操作防重放锁（F-009: 防止快速重复点击） */
  const isApplyingTemplate = ref(false)

  /** 子组件建议切换的标签页key（如：应用自定义模板后引导到域权限配置） */
  const suggestedTab = ref('')

  /** 菜单是否已解析完成 */
  const menusResolved = ref(false)

  // ========== 预填充审计日志（Bug-4：页面加载即有数据展示） ==========

  /** 生成N小时前的时间戳 */
  function hoursAgo(h: number): Date {
    const d = new Date()
    d.setTime(d.getTime() - h * 3600 * 1000)
    return d
  }

  // 预填5条模拟审计日志，让操作日志tab打开时即有数据
  // 注意：此处必须使用 userInfo（ref）而非 permissionStore.userInfo，
  // 因为当前处于 Store 定义内部，permissionStore 实例尚不存在
  const initOperatorName = userInfo.value?.username || '系统管理员'
  auditLogs.value = [
    {
      id: 'AL-INIT-001',
      timestamp: hoursAgo(0.5),
      eventType: 'TEMPLATE_APPLIED',
      eventName: '应用权限模板',
      operatorName: initOperatorName,
      targetRole: 'standard-chain',
      detail: '从「无」切换为「标准连锁模式」',
      riskLevel: 'MEDIUM',
    },
    {
      id: 'AL-INIT-002',
      timestamp: hoursAgo(2),
      eventType: 'DOMAIN_PERMISSION_CHANGED',
      eventName: '域权限变更',
      operatorName: initOperatorName,
      detail: '设置 3 条覆盖规则（隐藏1项 / 显示2项）',
      riskLevel: 'MEDIUM',
    },
    {
      id: 'AL-INIT-003',
      timestamp: hoursAgo(6),
      eventType: 'USER_OVERRIDE_CREATED',
      eventName: '用户覆盖创建',
      operatorName: initOperatorName,
      targetUser: '张三 (员工)',
      detail: '为用户「张三」添加财务报表查看权限，用于月度经营分析对比',
      riskLevel: 'LOW',
    },
    {
      id: 'AL-INIT-004',
      timestamp: hoursAgo(24),
      eventType: 'ROLE_UPDATED',
      eventName: '角色更新',
      operatorName: initOperatorName,
      targetRole: 'ops_director',
      detail: '修改运营总监的采购域权限为 READ_ONLY（只读监控）',
      riskLevel: 'HIGH',
    },
    {
      id: 'AL-INIT-005',
      timestamp: hoursAgo(48),
      eventType: 'TEMPLATE_APPLIED',
      eventName: '重置权限配置',
      operatorName: initOperatorName,
      detail: '清除所有覆盖规则和模板（原模板: large-chain）',
      riskLevel: 'MEDIUM',
    },
  ]

  // ========== DEFECT-C修复: 域矩阵→菜单自动同步 ==========
  // 当域权限矩阵版本变化时，自动重新计算菜单可见性
  // 原因: 之前需要手动点击"同步到菜单"，用户修改权限后以为已生效但侧边栏未变
  //
  // 注意：原 mock/permission 模块已清理，matrixVersion 为静态值 0。
  // 待后端权限中心 API 落地后，此处需重新接入响应式版本号。

  // ========== 计算属性 ==========

  /**
   * 是否为超级管理员（owner/admin）
   */
  const isAdmin = computed((): boolean => {
    if (!userInfo.value) return false
    return userInfo.value.roles.includes(UserRole.OWNER) ||
           userInfo.value.roles.includes(UserRole.ADMIN)
  })

  /**
   * 模板Code → 规模档位映射
   * 与权限中心模板（PermissionTemplate.templateCode）一一对应
   * 用于控制子菜单在不同规模企业的可见性（scaleLevel 字段）
   */
  const TEMPLATE_TO_SCALE: Record<string, ScaleLevel> = {
    'mini-single': 'mini',
    'centralized-single': 'standard',
    'standard-chain': 'chain-standard',
    'enterprise-chain': 'chain-enterprise',
    'custom': 'chain-enterprise', // 自定义模板默认全开（admin可见所有）
  }

  /**
   * 当前规模档位（基于 currentTemplate 推导）
   * - 未应用模板时默认 standard（集中式单店）
   * - admin 用户始终返回 chain-enterprise（可见所有子菜单）
   */
  const currentScale = computed<ScaleLevel>(() => {
    if (isAdmin.value) return 'chain-enterprise'
    if (!currentTemplate.value) return 'standard'
    return TEMPLATE_TO_SCALE[currentTemplate.value] ?? 'standard'
  })

  /**
   * 按规模档位过滤子菜单
   * - 子菜单未设置 scaleLevel → 全规模可见
   * - 子菜单设置了 scaleLevel → 仅在列出的档位下可见
   */
  function filterChildrenByScale(children: SubMenuItem[]): SubMenuItem[] {
    const scale = currentScale.value
    return children.filter(child => {
      if (!child.scaleLevel || child.scaleLevel.length === 0) return true
      return child.scaleLevel.includes(scale)
    })
  }

  /**
   * 按权限码过滤子菜单
   * - 子菜单未设置 permission → 所有已见用户可见
   * - 子菜单设置了 permission → 需要拥有对应权限码
   */
  function filterChildrenByPermission(children: SubMenuItem[]): SubMenuItem[] {
    if (isAdmin.value) return children
    return children.filter(child => {
      if (!child.permission) return true
      return hasPermission(child.permission)
    })
  }

  /** 是否已登录（有用户信息即视为已登录） */
  const isLoggedIn = computed((): boolean => !!userInfo.value)

  /**
   * 是否为公司级管理者（运营总监/财务总监/HR总监）
   */
  const isCompanyManager = computed((): boolean => {
    if (!userInfo.value) return false
    if (isAdmin.value) return true
    return userInfo.value.roles.some(role => 
      [UserRole.OPS_DIRECTOR, UserRole.FINANCE_DIRECTOR, UserRole.HR_DIRECTOR].includes(role)
    )
  })

  /**
   * 是否为门店管理者（店长/组长）
   */
  const isStoreManager = computed((): boolean => {
    if (!userInfo.value) return false
    if (isAdmin.value) return true
    return userInfo.value.roles.some(role => 
      [UserRole.STORE_MANAGER, UserRole.TEAM_LEADER].includes(role)
    )
  })

  /**
   * 是否可以访问门店运营模块
   * 规则：
   * 1. owner/admin → 可以访问（用于测试和管理）
   * 2. store_manager/team_leader → 可以访问（目标用户）
   * 3. 其他角色 → 不可以访问
   */
  const canAccessStoreOps = computed((): boolean => {
    if (!userInfo.value) return false
    
    // 超级管理员始终可访问
    if (isAdmin.value) return true
    
    // 门店管理者可访问
    const storeRoles = [UserRole.STORE_MANAGER, UserRole.TEAM_LEADER]
    return userInfo.value.roles.some(role => storeRoles.includes(role))
  })

  /**
   * 当前用户的组织层级
   */
  const userOrgLevel = computed((): OrgLevel => {
    if (!userInfo.value) return OrgLevel.STORE_STAFF
    
    if (userInfo.value.orgLevel) {
      return userInfo.value.orgLevel
    }
    
    // 根据角色推断组织层级
    if (isAdmin.value) return OrgLevel.OWNER
    if (isCompanyManager.value) return OrgLevel.COMPANY
    if (isStoreManager.value) return OrgLevel.STORE_MGR
    
    return OrgLevel.STORE_STAFF
  })

  /**
   * 当前用户的数据范围
   */
  const userDataScope = computed((): DataScope => {
    if (!userInfo.value) return DataScope.SELF
    
    if (userInfo.value.dataScope) {
      return userInfo.value.dataScope
    }
    
    // 根据组织层级推断数据范围
    switch (userOrgLevel.value) {
      case OrgLevel.OWNER:
        return DataScope.ALL
      case OrgLevel.COMPANY:
        return DataScope.COMPANY
      case OrgLevel.STORE_MGR:
        return DataScope.STORE
      default:
        return DataScope.SELF
    }
  })

  /**
   * 可访问的门店ID列表
   */
  const accessibleStores = computed((): string[] => {
    if (!userInfo.value) return []
    
    // 超级管理员返回空（表示所有门店）
    if (isAdmin.value) return []
    
    // 有明确storeId时返回本门店
    if (userInfo.value.storeId) {
      return [userInfo.value.storeId]
    }
    
    return []
  })

  // ========== 方法 ==========

  /**
   * 设置用户信息
   * @param info 用户信息
   */
  function setUserInfo(info: UserInfo): void {
    userInfo.value = info
    loaded.value = true
    
    // 同步到localStorage（供v-permission指令使用）
    if (info.permissions && info.permissions.length > 0) {
      localStorage.setItem('permissions', JSON.stringify(info.permissions))
    }
    
    // 同步角色到localStorage
    localStorage.setItem('roles', JSON.stringify(info.roles))
  }

  /**
   * 从Token解析用户信息
   * 无Token或Token无效时保持未登录状态，由路由守卫引导到登录页
   * @param token 可选的Token字符串（默认从localStorage读取）
   */
  async function initFromToken(token?: string): Promise<void> {
    if (loading.value) return

    loading.value = true

    try {
      const tokenStr = token || localStorage.getItem('token')

      if (!tokenStr) {
        // 无Token：保持未登录状态，由路由守卫跳转登录页
        userInfo.value = null
        loading.value = false
        return
      }

      // 解析JWT Token（简化版，仅用于开发环境）
      const payload = parseJWTPayload(tokenStr)

      if (!payload) {
        // Token解析失败：清除无效Token，保持未登录状态
        console.warn('[Permission] Token解析失败，清除认证状态')
        clearUserInfo()
        localStorage.removeItem('token')
        localStorage.removeItem('refresh_token')
        loading.value = false
        return
      }

      // 检查Token是否过期
      if (payload.exp && typeof payload.exp === 'number') {
        const expiresAt = payload.exp * 1000 // JWT exp 是秒级时间戳
        if (expiresAt < Date.now()) {
          // Token已过期：清除认证状态，由路由守卫跳转登录页
          console.warn('[Permission] Token已过期，清除认证状态')
          clearUserInfo()
          localStorage.removeItem('token')
          localStorage.removeItem('refresh_token')
          loading.value = false
          return
        }
      }

      const jwtPayload = payload as Record<string, unknown>
      const info: UserInfo = {
        userId: (jwtPayload.userId as string | number) || (jwtPayload.sub as string) || '',
        username: (jwtPayload.username as string) || '',
        email: jwtPayload.email as string | undefined,
        roles: mapRoles((jwtPayload.roles as string[]) || []),
        permissions: (jwtPayload.permissions as string[]) || [],
        orgLevel: mapOrgLevel((jwtPayload.roles as string[]) || []),
        dataScope: mapDataScope((jwtPayload.roles as string[]) || []),
        storeId: jwtPayload.storeId?.toString(),
        storeName: jwtPayload.storeName as string | undefined,
      }

      setUserInfo(info)

      // 恢复用户菜单覆盖和模板配置
      restoreMenuOverrides()
      const savedTpl = localStorage.getItem('menu-template')
      if (!savedTpl) {
        // 服务端激活模板兜底（2026-08-02 修复）：上线新环境/新浏览器 localStorage 为空时，
        // 从后端读取持久化的激活模板（默认 centralized-single），保证预设权限模板开箱生效。
        try {
          const activeCode = await permissionTemplateApi.getActiveTemplate()
          if (activeCode) {
            currentTemplate.value = activeCode
            localStorage.setItem('menu-template', activeCode)
          }
        } catch (e) {
          console.warn('[Permission] 读取服务端激活模板失败，本次不应用模板:', e)
        }
      } else {
        currentTemplate.value = savedTpl
      }

      // 预加载权限模板（菜单按模板 admin 域列表派生，必须提前加载；异步不阻塞登录）
      fetchPermissionTemplates().catch(() => {})
    } catch (error) {
      console.error('[Permission] 初始化失败:', error)
      // 异常时清除认证状态，不授予任何权限
      userInfo.value = null
    } finally {
      loading.value = false
    }
  }

  /**
   * 清除用户信息（登出时调用）
   */
  function clearUserInfo(): void {
    userInfo.value = null
    loaded.value = false
    localStorage.removeItem('permissions')
    localStorage.removeItem('roles')

    // 重置菜单状态
    userMenuOverrides.value = []
    currentTemplate.value = ''
    menusResolved.value = false
    resolvedMenus.value = []
    menuVersion.value++
  }

  /**
   * 退出登录：清除用户信息、Token和所有本地缓存（含标签页）
   */
  function logout(): void {
    userInfo.value = null
    loaded.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('refresh_token')
    localStorage.removeItem('user_id')
    localStorage.removeItem('username')
    localStorage.removeItem('token_expiry')
    localStorage.removeItem('permissions')
    localStorage.removeItem('roles')
    localStorage.removeItem('menu-overrides')
    localStorage.removeItem('menu-template')
    // 清除标签页缓存，防止下次登录残留旧标签
    localStorage.removeItem('tab-bar-tabs')
    userMenuOverrides.value = []
    currentTemplate.value = ''
    menusResolved.value = false
    resolvedMenus.value = []
    menuVersion.value++
  }

  /**
   * 检查是否拥有指定角色
   * @param role 角色
   */
  function hasRole(role: UserRole): boolean {
    if (!userInfo.value) return false
    if (isAdmin.value) return true
    return userInfo.value.roles.includes(role)
  }

  /**
   * 检查是否拥有任一角色
   * @param roles 角色数组
   */
  function hasAnyRole(roles: UserRole[]): boolean {
    if (!userInfo.value) return false
    if (isAdmin.value) return true
    return userInfo.value.roles.some(role => roles.includes(role))
  }

  /**
   * 检查是否拥有所有角色
   * @param roles 角色数组
   */
  function hasAllRoles(roles: UserRole[]): boolean {
    if (!userInfo.value) return false
    if (isAdmin.value) return true
    return roles.every(role => userInfo.value!.roles.includes(role))
  }

  /**
   * 检查是否拥有指定权限
   * @param permission 权限标识符
   */
  function hasPermission(permission: string): boolean {
    if (!userInfo.value) return false
    if (isAdmin.value) return true
    if (userInfo.value.permissions.includes('*')) return true
    return userInfo.value.permissions.includes(permission)
  }

  /**
   * 检查是否拥有任一权限
   * @param permissions 权限标识符数组
   */
  function hasAnyPermission(permissions: string[]): boolean {
    if (!userInfo.value) return false
    if (isAdmin.value) return true
    if (userInfo.value.permissions.includes('*')) return true
    return permissions.some(p => userInfo.value!.permissions.includes(p))
  }

  /**
   * 检查是否拥有所有权限
   * @param permissions 权限标识符数组
   */
  function hasAllPermissions(permissions: string[]): boolean {
    if (!userInfo.value) return false
    if (isAdmin.value) return true
    if (userInfo.value.permissions.includes('*')) return true
    return permissions.every(p => userInfo.value!.permissions.includes(p))
  }

  /**
   * 检查是否可以访问指定门店
   * @param storeId 门店ID
   */
  function canAccessStore(storeId: string): boolean {
    if (isAdmin.value) return true
    if (userDataScope.value === DataScope.ALL) return true
    if (userDataScope.value === DataScope.COMPANY) return true
    
    return accessibleStores.value.includes(storeId)
  }

  /**
   * 检查是否可以访问指定部门的数据
   * @param deptId 部门ID
   */
  function canAccessDepartment(deptId: string): boolean {
    if (isAdmin.value) return true
    if (!userInfo.value) return false
    if (userDataScope.value === DataScope.ALL || userDataScope.value === DataScope.COMPANY) return true
    
    return userInfo.value.departmentId === deptId
  }

  // ========== 辅助函数 ==========

  /**
   * 解析JWT Payload（简化版，不验证签名）
   */
  function parseJWTPayload(token: string): Record<string, unknown> | null {
    try {
      const parts = token.split('.')
      if (parts.length !== 3) return null
      
      const payload = parts[1]
      const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
      return JSON.parse(decoded)
    } catch {
      return null
    }
  }

  /**
   * 映射后端角色到前端UserRole枚举
   *
   * 兼容多种后端角色格式：
   * - ROLE_ADMIN（数据库原始格式，大写带前缀）
   * - role_admin（JwtUtils 小写化后的格式，见 JwtUtils.generateToken）
   * - admin（无前缀旧格式）
   * - Admin（首字母大写等其他变体）
   *
   * 实现方式：将 key 统一为大写格式，查找时也将输入转大写，
   * 这样可同时兼容大小写、有无 ROLE_ 前缀的各类变体。
   */
  function mapRoles(backendRoles: string[]): UserRole[] {
    // 角色 → UserRole 映射表（key 统一为大写）
    const roleMap: Record<string, UserRole> = {
      'ROLE_ADMIN': UserRole.ADMIN,
      'ROLE_OWNER': UserRole.OWNER,
      'ROLE_OPS_DIRECTOR': UserRole.OPS_DIRECTOR,
      'ROLE_PURCHASE_MANAGER': UserRole.PURCHASE_MANAGER,
      'ROLE_FINANCE_DIRECTOR': UserRole.FINANCE_DIRECTOR,
      'ROLE_HR_DIRECTOR': UserRole.HR_DIRECTOR,
      'ROLE_DEPARTMENT_MANAGER': UserRole.DEPARTMENT_MANAGER,
      'ROLE_WAREHOUSE_MANAGER': UserRole.WAREHOUSE_MANAGER,
      'ROLE_STORE_MANAGER': UserRole.STORE_MANAGER,
      'ROLE_TEAM_LEADER': UserRole.TEAM_LEADER,
      'ROLE_SCHEDULER': UserRole.SCHEDULER,
      'ROLE_REGION_MANAGER': UserRole.REGION_MANAGER,
      'ROLE_AUDITOR': UserRole.AUDITOR,
      'ROLE_EMPLOYEE': UserRole.EMPLOYEE,
      // 无 ROLE_ 前缀格式（兼容旧格式）
      'ADMIN': UserRole.ADMIN,
      'OWNER': UserRole.OWNER,
      'OPS_DIRECTOR': UserRole.OPS_DIRECTOR,
      'PURCHASE_MANAGER': UserRole.PURCHASE_MANAGER,
      'FINANCE_DIRECTOR': UserRole.FINANCE_DIRECTOR,
      'HR_DIRECTOR': UserRole.HR_DIRECTOR,
      'DEPARTMENT_MANAGER': UserRole.DEPARTMENT_MANAGER,
      'WAREHOUSE_MANAGER': UserRole.WAREHOUSE_MANAGER,
      'STORE_MANAGER': UserRole.STORE_MANAGER,
      'TEAM_LEADER': UserRole.TEAM_LEADER,
      'SCHEDULER': UserRole.SCHEDULER,
      'REGION_MANAGER': UserRole.REGION_MANAGER,
      'AUDITOR': UserRole.AUDITOR,
      'EMPLOYEE': UserRole.EMPLOYEE,
    }

    return backendRoles
      .map(role => {
        if (!role) return UserRole.EMPLOYEE
        // 统一转大写查找，兼容后端 JwtUtils 小写化（role_admin → ROLE_ADMIN）及各类变体
        return roleMap[role.toUpperCase()] || UserRole.EMPLOYEE
      })
      .filter((v, i, a) => a.indexOf(v) === i)
  }

  /**
   * 根据角色推断组织层级
   */
  function mapOrgLevel(roles: string[]): OrgLevel {
    if (roles.includes('ROLE_OWNER') || roles.includes('owner')) return OrgLevel.OWNER
    if (roles.includes('ROLE_ADMIN') || roles.includes('admin')) return OrgLevel.OWNER
    if (roles.some(r => ['ROLE_OPS_DIRECTOR', 'ROLE_PURCHASE_MANAGER', 'ROLE_FINANCE_DIRECTOR', 'ROLE_HR_DIRECTOR', 'ROLE_WAREHOUSE_MANAGER'].includes(r))) {
      return OrgLevel.COMPANY
    }
    if (roles.includes('ROLE_DEPARTMENT_MANAGER') || roles.includes('department_manager')) return OrgLevel.COMPANY
    if (roles.some(r => ['ROLE_STORE_MANAGER', 'store_manager'].includes(r))) return OrgLevel.STORE_MGR
    if (roles.some(r => ['ROLE_TEAM_LEADER', 'team_leader'].includes(r))) return OrgLevel.STORE_MGR

    return OrgLevel.STORE_STAFF
  }

  /**
   * 根据组织层级推断数据范围
   */
  function mapDataScope(roles: string[]): DataScope {
    const orgLevel = mapOrgLevel(roles)
    
    switch (orgLevel) {
      case OrgLevel.OWNER:
        return DataScope.ALL
      case OrgLevel.COMPANY:
        return DataScope.COMPANY
      case OrgLevel.STORE_MGR:
        return DataScope.STORE
      default:
        return DataScope.SELF
    }
  }

  // ========== 菜单配置与动态加载方法 ==========

  /**
   * 注册一个菜单组到全局注册表
   * 由各模块的 menu.ts 在导入时自动调用
   * @param config 菜单组配置
   */
  function registerMenuGroup(config: MenuGroupConfig): void {
    // F-008: 初始化完成后禁止动态注册（防止运行时注入恶意菜单）
    if (registrationLocked.value) {
      console.warn(
        `[Permission] ⚠️ 菜单注册已锁定，忽略「${config.title}(${config.id})」的注册请求。` +
        `所有菜单应在应用启动时通过 registerAllMenus() 统一注册。`
      )
      return
    }

    const existing = registeredMenus.findIndex(m => m.id === config.id)
    if (existing >= 0) {
      registeredMenus[existing] = config
    } else {
      registeredMenus.push(config)
    }
    menusResolved.value = false
    menuVersion.value++
  }

  /**
   * 锁定菜单注册（F-008: 在 registerAllMenus() 完成后调用）
   * 锁定后新的 registerMenuGroup 调用将被忽略并记录警告日志
   */
  function lockRegistration(): void {
    registrationLocked.value = true
  }

  /**
   * 建议切换到指定标签页（子组件→父组件通信）
   * @param tabKey 目标标签页key
   */
  function suggestTab(tabKey: string): void {
    suggestedTab.value = tabKey
  }

  /**
   * 获取当前用户可见的完整菜单树
   *
   * 解析优先级（从高到低）：
   * 1. userMenuOverrides — 个别用户的特殊配置
   * 2. roleMenuOverrides — 所属角色的默认配置
   * 3. templateMenuConfig — 预设模板
   * 4. defaultMenuConfig — 系统默认全量菜单
   *
   * 过滤规则：
   * - 超级管理员(OWNER/ADMIN) → 返回全部菜单
   * - visibleRoles 未设置或空 → 所有人可见
   * - visibleRoles 有值 → 检查是否拥有任一所需角色
   */
  function getVisibleMenus(): MenuGroupConfig[] {
    // DEFECT-A修复: 管理员始终返回全部菜单，跳过所有覆盖逻辑
    // 原因: applyOverrides()使用splice物理删除菜单组，
    // 如果先应用覆盖再检查admin，被删除的菜单组已无法恢复
    //
    // 权限中心4模式（2026-06-30）：admin 也需要应用模板覆盖，
    // 例如 centralized-single 模式下 admin 也只看到 9 个核心域。
    // 仅应用模板覆盖（不影响其他覆盖逻辑），保持 admin 的特殊地位。
    if (isAdmin.value) {
      let all: MenuGroupConfig[] = [...registeredMenus].sort((a, b) => a.order - b.order)

      // 应用当前模板覆盖（4 种模式对 admin 生效）
      if (currentTemplate.value) {
        const tpl = getTemplate(currentTemplate.value)
        if (tpl) {
          all = applyOverrides(all, tpl.overrides)
        }
      }

      menusResolved.value = true
      resolvedMenus.value = all
      return all
    }

    let menus: MenuGroupConfig[] = [...registeredMenus]

    if (currentTemplate.value) {
      const tpl = getTemplate(currentTemplate.value)
      if (tpl) {
        menus = applyOverrides(menus, tpl.overrides)
      }
    }

    const roleOver = getRoleOverrides(userInfo.value?.roles || [])
    if (roleOver.length > 0) {
      menus = applyOverrides(menus, roleOver)
    }

    if (userMenuOverrides.value.length > 0) {
      // 仅应用与当前用户相关的覆盖规则：
      // 1. targetUserId 为空/undefined → 全局覆盖（角色级），所有用户生效
      // 2. targetUserId 匹配当前用户 → 用户级覆盖，仅对目标用户生效
      const currentUserId = String(userInfo.value?.userId || '')
      const relevantOverrides = userMenuOverrides.value.filter(
        o => !o.targetUserId || o.targetUserId === currentUserId
      )
      if (relevantOverrides.length > 0) {
        menus = applyOverrides(menus, relevantOverrides)
      }
    }

    // DEFECT-C修复v2: 域矩阵覆盖层
    // 从当前用户的角色域权限矩阵推导菜单可见性，作为最终覆盖源
    // 优先级: 域矩阵 > 用户覆盖 > 角色覆盖 > 模板覆盖 > 注册菜单
    // 这样域矩阵的运行时修改（cycleRoleDomainAccess等）能自动反映到菜单
    const domainOverrides = getDomainMatrixOverrides()
    if (domainOverrides.length > 0) {
      menus = applyOverrides(menus, domainOverrides)
    }

    const filtered = menus.filter(group => {
      if (isAdmin.value) return true

      // 安全网：显式排除已被标记为隐藏的菜单组（防御性检查）
      if (group.hidden === true) return false

      if (!group.visibleRoles || group.visibleRoles.length === 0) {
        return true
      }

      return hasAnyRole(group.visibleRoles)
    })

    filtered.sort((a, b) => a.order - b.order)

    // 按规模档位过滤子菜单（scaleLevel 联动权限模板）
    // admin 用户在 currentScale 中已返回 chain-enterprise，filterChildrenByScale 会保留全部
    const scaleFiltered = filtered.map(group => ({
      ...group,
      children: filterChildrenByPermission(filterChildrenByScale(group.children)),
    }))

    menusResolved.value = true
    resolvedMenus.value = scaleFiltered
    return scaleFiltered
  }

  /**
   * 获取已解析的菜单缓存（避免重复计算）
   */
  function getCachedMenus(): MenuGroupConfig[] {
    if (!menusResolved.value) {
      return getVisibleMenus()
    }
    return resolvedMenus.value
  }

  /**
   * 设置用户级菜单覆盖
   * @param overrides 覆盖规则列表
   * @param persist 是否持久化到localStorage（默认true）
   */
  function setMenuOverrides(overrides: MenuOverride[], persist = true): void {
    userMenuOverrides.value = overrides
    if (persist) {
      localStorage.setItem('menu-overrides', JSON.stringify(overrides))
    }
    menusResolved.value = false
    menuVersion.value++

    // 审计日志（F-006）
    const hideCount = overrides.filter(o => o.action === 'hide').length
    const showCount = overrides.filter(o => o.action === 'show').length
    addAuditLog({
      eventType: 'DOMAIN_PERMISSION_CHANGED',
      eventName: '域权限变更',
      detail: `设置 ${overrides.length} 条覆盖规则（隐藏${hideCount}项 / 显示${showCount}项）`,
      riskLevel: hideCount > 3 ? 'HIGH' : 'MEDIUM',
    })
  }

  /**
   * 从localStorage恢复用户菜单覆盖
   */
  function restoreMenuOverrides(): void {
    const saved = localStorage.getItem('menu-overrides')
    if (saved) {
      const parsed = safeJsonParse<MenuOverride[]>(saved, [])
      // 二次校验：确保解析结果是数组
      userMenuOverrides.value = Array.isArray(parsed) ? parsed : []
    }
  }

  /**
   * 应用预设菜单模板
   * @param templateId 模板ID
   */
  /**
   * 业务链完整性校验（供应链铁三角：product/purchase/store-ops）
   * 三者任一开放，则三者必须全开——防止「商品档案→采购订单→到货→门店收货→门店库存→菜品明细」断链
   * @returns 断裂的角色列表（空数组 = 完整）
   */
  function validateChainIntegrity(template: PermissionTemplateBackend | undefined): string[] {
    if (!template) return []
    const TRIANGLE = ['product', 'purchase', 'store-ops']
    const broken: string[] = []
    for (const [roleCode, domains] of Object.entries(template.domainMatrix)) {
      const opened = TRIANGLE.filter(d => domains.includes(d))
      if (opened.length > 0 && opened.length < TRIANGLE.length) {
        broken.push(`${roleCode}(${opened.join('/')} 缺 ${TRIANGLE.filter(d => !domains.includes(d)).join('/')})`)
      }
    }
    return broken
  }

  function applyTemplate(templateId: string): void {
    // F-009: 防止重复点击（防重放锁）
    if (isApplyingTemplate.value) {
      console.warn('[Permission] 模板应用正在进行中，忽略重复调用')
      return
    }
    isApplyingTemplate.value = true

    try {
      const oldTemplate = currentTemplate.value
      currentTemplate.value = templateId
      localStorage.setItem('menu-template', templateId)
      menusResolved.value = false
      menuVersion.value++

      // 业务链完整性校验：供应链铁三角断裂时告警并留审计（不拦截，避免卡死自定义场景）
      const tpl = permissionTemplates.value.find(t => t.templateCode === templateId)
      const brokenRoles = validateChainIntegrity(tpl)
      if (brokenRoles.length > 0) {
        console.warn('[Permission] 供应链铁三角断裂（product/purchase/store-ops 未全开）:', brokenRoles.join('; '))
        addAuditLog({
          eventType: 'TEMPLATE_APPLIED',
          eventName: '权限模板供应链链完整性告警',
          targetRole: templateId,
          detail: `角色域矩阵存在断裂：${brokenRoles.join('; ')}（商品档案→采购→到货→收货→库存→菜品明细链可能中断）`,
          riskLevel: 'MEDIUM',
        })
      }

      // 同步更新角色域权限矩阵：从已加载模板列表中找到对应模板，
      // 将其 domainMatrix 转换为 { role: { domain: 'FULL' | 'HIDDEN' } } 格式
      // 仅 admin 角色参与的域视为 FULL，未参与的域视为 HIDDEN
      // 其他角色同理（HIDDEN 域对应菜单将被 getDomainMatrixOverrides 隐藏）
      syncRoleDomainMatrix(templateId)

      // 审计日志（F-006）
      addAuditLog({
        eventType: 'TEMPLATE_APPLIED',
        eventName: '应用权限模板',
        targetRole: templateId,
        detail: `从「${oldTemplate || '无'}」切换为「${templateId}」`,
        riskLevel: 'MEDIUM',
      })

      // 持久化激活模板到服务端（多终端一致；失败仅 warn，不影响本地生效）
      permissionTemplateApi.setActiveTemplate(templateId).catch((err: unknown) => {
        console.warn('[Permission] 激活模板持久化失败（仅本终端生效）:', err)
      })
    } finally {
      // 无论成功还是异常，都释放锁（同步执行场景下立即释放，为未来异步化预留）
      isApplyingTemplate.value = false
    }
  }

  /**
   * 同步角色域权限矩阵
   * 根据当前应用的模板，将 domainMatrix 转换为菜单覆盖用的格式
   * @param templateCode 模板编码
   */
  function syncRoleDomainMatrix(templateCode: string): void {
    const tpl = permissionTemplates.value.find(t => t.templateCode === templateCode)
    if (!tpl) {
      roleDomainMatrix.value = {}
      return
    }

    // 14 个业务域全集
    const ALL_DOMAINS = [
      'workspace', 'store-ops', 'product', 'order', 'operations',
      'purchase', 'warehouse', 'member', 'finance', 'hr',
      'traceability', 'device', 'asset', 'system',
    ]

    // 将 {role: [domain, ...]} 转换为 {role: {domain: 'FULL' | 'HIDDEN'}}
    const matrix: Record<string, Record<string, string>> = {}
    for (const [roleCode, domains] of Object.entries(tpl.domainMatrix)) {
      const roleMatrix: Record<string, string> = {}
      for (const domain of ALL_DOMAINS) {
        roleMatrix[domain] = domains.includes(domain) ? 'FULL' : 'HIDDEN'
      }
      matrix[roleCode] = roleMatrix
    }

    // SR-6/9（2026-07-31 决策）：菜单按「模式 + 角色」双重视图。
    // 移除原先对 admin/owner 的"所有域强制 FULL"约束，域矩阵严格按模板角色列表生成：
    // centralized-single 下 admin/owner 开放 finance+purchase（老板/管理员负责财务采购），
    // 其他角色（店长/员工等）不显示（visibleRoles 亦过滤）。
    // 注：菜单全局显隐由 getMenuTemplates() 基于模板 admin 域列表派生；
    // guards.ts 中 isAdmin 的访问豁免保留（仅放行直连 URL，不影响菜单显示）。
    roleDomainMatrix.value = matrix
  }

  /**
   * 异步加载权限模板列表（4 种系统模式）
   * 从后端 /v1/permission-templates/system 获取并转换为前端格式
   */
  async function fetchPermissionTemplates(force = false): Promise<void> {
    if (templatesLoaded.value && !force) return
    if (templatesLoading.value) return

    templatesLoading.value = true
    try {
      const dtoList = await permissionTemplateApi.getSystemTemplates()
      permissionTemplates.value = dtoList.map(dto => ({
        templateCode: dto.code,
        templateName: dto.name,
        description: dto.description || '',
        domainMatrix: parseRoleConfig(dto.roleConfig),
      }))
      templatesLoaded.value = true

      // 若已有 currentTemplate，重新同步矩阵
      if (currentTemplate.value) {
        syncRoleDomainMatrix(currentTemplate.value)
      }

      // 模板加载成功后触发菜单重算（菜单按模板 admin 域列表派生）
      menusResolved.value = false
      menuVersion.value++
    } catch (error) {
      console.error('[Permission] 加载权限模板失败:', error)
      // 加载失败时使用 fallback 默认模板（避免 UI 空白）
      permissionTemplates.value = getFallbackTemplates()
      templatesLoaded.value = true
      menusResolved.value = false
      menuVersion.value++
    } finally {
      templatesLoading.value = false
    }
  }

  /**
   * Fallback 模板：当后端 API 不可用时使用
   * 与后端 BaseDatabaseInitializer.insertPermissionTemplates 保持一致
   */
  function getFallbackTemplates(): PermissionTemplateBackend[] {
    const ALL_DOMAINS = [
      'workspace', 'store-ops', 'product', 'order', 'operations',
      'purchase', 'warehouse', 'member', 'finance', 'hr',
      'traceability', 'device', 'asset', 'system',
    ]
    // centralized-single 基础核心域（9 个，不含采购/财务——按角色开放）
    const CORE_DOMAINS = [
      'workspace', 'store-ops', 'product', 'order', 'operations',
      'member', 'traceability', 'device', 'system',
    ]
    // admin/owner（老板/管理员）额外开放 财务 + 采购：负责财务与采购业务
    const OWNER_DOMAINS = [...CORE_DOMAINS, 'finance', 'purchase']

    return [
      {
        templateCode: 'centralized-single',
        templateName: '集中式单店模式',
        description: '适用于单店小型餐饮企业（5-50人，年营收50万-500万），聚焦核心营业功能，简化后台管理',
        domainMatrix: {
          admin: OWNER_DOMAINS,
          owner: OWNER_DOMAINS,
          hr_director: ['workspace', 'hr'],
          finance_director: ['workspace', 'finance', 'purchase'],
          ops_director: ['workspace', 'store-ops', 'product', 'order', 'operations', 'purchase', 'member'],
          store_manager: ['workspace', 'store-ops', 'product', 'order', 'operations', 'member', 'purchase', 'traceability'],
          team_leader: ['workspace', 'store-ops', 'product', 'order', 'member'],
          employee: ['workspace'],
        },
      },
      {
        templateCode: 'standard-chain',
        templateName: '标准连锁模式',
        description: '适用于2-5家门店的小型连锁餐饮（5-50人，年营收500万-2000万），全业务域开放，支持跨店管理',
        domainMatrix: {
          admin: [...ALL_DOMAINS],
          owner: [...ALL_DOMAINS],
          hr_director: ['workspace', 'hr'],
          finance_director: ['workspace', 'finance'],
          ops_director: ['workspace', 'store-ops', 'product', 'order', 'operations', 'purchase', 'warehouse', 'member'],
          store_manager: ['workspace', 'store-ops', 'product', 'order', 'member', 'traceability'],
          team_leader: ['workspace', 'store-ops'],
          employee: ['workspace'],
        },
      },
      {
        templateCode: 'large-chain',
        templateName: '大型连锁模式',
        description: '适用于多门店大型连锁餐饮（200人以上，年营收2000万+），全业务域开放，所有角色权限完整',
        domainMatrix: {
          admin: [...ALL_DOMAINS],
          owner: [...ALL_DOMAINS],
          hr_director: ['workspace', 'hr'],
          finance_director: ['workspace', 'finance', 'asset'],
          ops_director: ['workspace', 'store-ops', 'product', 'order', 'operations', 'purchase', 'warehouse', 'member', 'traceability'],
          store_manager: ['workspace', 'store-ops', 'product', 'order', 'member', 'traceability'],
          team_leader: ['workspace', 'store-ops'],
          employee: ['workspace'],
        },
      },
      {
        templateCode: 'custom',
        templateName: '自定义模式',
        description: '从零开始，不预设任何域权限，admin 全开，其他角色由用户自行编辑',
        domainMatrix: {
          admin: [...ALL_DOMAINS],
          owner: [...ALL_DOMAINS],
          hr_director: ['workspace', 'hr'],
          finance_director: ['workspace', 'finance'],
          ops_director: ['workspace', 'store-ops', 'order', 'operations'],
          employee: ['workspace'],
        },
      },
    ]
  }

  /**
   * 重置为默认菜单（清除所有覆盖）
   */
  function resetMenusToDefault(): void {
    userMenuOverrides.value = []
    const oldTemplate = currentTemplate.value
    currentTemplate.value = ''
    localStorage.removeItem('menu-overrides')
    localStorage.removeItem('menu-template')
    menusResolved.value = false
    resolvedMenus.value = []
    menuVersion.value++

    // 审计日志（F-006）
    addAuditLog({
      eventType: 'TEMPLATE_APPLIED',
      eventName: '重置权限配置',
      detail: `清除所有覆盖规则和模板（原模板: ${oldTemplate || '无'}）`,
      riskLevel: 'MEDIUM',
    })
  }

  /** 添加审计日志条目（F-006）
   * @param entry 日志条目（不含id/timestamp/operatorName/ipAddress/userAgent，自动填充）
   */
  function addAuditLog(entry: Omit<AuditLogEntry, 'id' | 'timestamp' | 'operatorName' | 'ipAddress' | 'userAgent'>): void {
    const logEntry: AuditLogEntry = {
      ...entry,
      id: `AL${Date.now()}`,
      timestamp: new Date(),
      operatorName: userInfo.value?.username || 'system',
      ipAddress: '本地终端', // 开发环境占位，生产环境由后端注入真实IP
      userAgent: typeof navigator !== 'undefined' ? navigator.userAgent.substring(0, 80) : '',
    }
    auditLogs.value.unshift(logEntry)

    // 保留最近200条日志，防止内存膨胀
    if (auditLogs.value.length > 200) {
      auditLogs.value = auditLogs.value.slice(0, 200)
    }
  }

  /**
   * DEFECT-C修复v2: 从域权限矩阵推导菜单覆盖规则
   *
   * 读取当前用户主角色在域矩阵中的权限级别，
   * 将HIDDEN域对应的菜单组标记为隐藏。
   *
   * 这是菜单可见性的最终数据源，优先级最高，
   * 确保域矩阵的运行时修改能自动反映到侧边栏菜单。
   */
  function getDomainMatrixOverrides(): MenuOverride[] {
    if (!userInfo.value || userInfo.value.roles.length === 0) return []

    // 域→菜单组映射（与getMenuTemplates保持一致）
    const DOMAIN_TO_MENU_GROUP: Record<string, string> = {
      'workspace': 'workspace',
      'store-ops': 'store-management',
      'product': 'product',
      'order': 'order',
      'operations': 'operations',
      'purchase': 'purchase',
      'warehouse': 'warehouse',
      'member': 'member',
      'finance': 'finance',
      'hr': 'hr',
      'traceability': 'traceability',
      'device': 'device',
      'asset': 'asset',
      'system': 'system',
    }

    const matrix = roleDomainMatrix.value
    const overrides: MenuOverride[] = []

    // 合并所有角色的域权限（取各域的最高访问级别）
    const userRoles = userInfo.value.roles || []
    if (userRoles.length === 0) return []

    // 将UserRole枚举值映射到矩阵中的roleCode
    const roleCodeMap: Record<string, string> = {
      'owner': 'admin',
      'admin': 'admin',
      'ops_director': 'ops_director',
      'finance_director': 'finance_director',
      'hr_director': 'hr_director',
      'store_manager': 'store_manager',
      'team_leader': 'team_leader',
      'employee': 'employee',
      'scheduler': 'scheduler',
      'region_manager': 'region_manager',
      'auditor': 'auditor',
    }

    // 域级别优先级（数值越高权限越大）
    const levelPriority: Record<string, number> = {
      'HIDDEN': 0,
      'LIMITED': 1,
      'READ_ONLY': 2,
      'FULL': 3,
    }

    // 合并所有角色的域矩阵，每个域取最高权限
    const mergedMatrix: Record<string, string> = {}
    for (const role of userRoles) {
      const matrixRoleCode = roleCodeMap[role]
      if (!matrixRoleCode) continue
      const roleMatrix = matrix[matrixRoleCode]
      if (!roleMatrix) continue

      for (const [domainCode, level] of Object.entries(roleMatrix)) {
        const currentPriority = levelPriority[mergedMatrix[domainCode]] ?? -1
        const newPriority = levelPriority[level] ?? -1
        if (newPriority > currentPriority) {
          mergedMatrix[domainCode] = level
        }
      }
    }

    // 遍历合并后的域矩阵，HIDDEN域对应的菜单组需要隐藏
    for (const [domainCode, level] of Object.entries(mergedMatrix)) {
      if (level === 'HIDDEN') {
        const groupId = DOMAIN_TO_MENU_GROUP[domainCode]
        if (groupId) {
          overrides.push({ groupId, action: 'hide' })
        }
      }
    }

    return overrides
  }

  /**
   * 内部方法：对菜单列表应用一组覆盖规则
   */
  function applyOverrides(menus: MenuGroupConfig[], overrides: MenuOverride[]): MenuGroupConfig[] {
    const result = [...menus]

    for (const override of overrides) {
      const idx = result.findIndex(g => g.id === override.groupId)
      if (idx === -1) continue

      switch (override.action) {
        case 'hide':
          result.splice(idx, 1)
          break
        case 'show':
          if (!result.find(g => g.id === override.groupId)) {
            const original = registeredMenus.find(g => g.id === override.groupId)
            if (original) result.splice(override.newOrder ?? idx, 0, { ...original })
          }
          break
        case 'replace':
          if (override.replacement) {
            result[idx] = { ...result[idx], ...override.replacement }
          }
          break
        case 'reorder':
          if (override.newOrder !== undefined) {
            const [item] = result.splice(idx, 1)
            result.splice(override.newOrder, 0, item)
          }
          break
      }
    }

    return result
  }

  /**
   * 内部方法：获取角色的默认菜单覆盖（预留）
   */
  function getRoleOverrides(_roles: UserRole[]): MenuOverride[] {
    return []
  }

  /**
   * 内部方法：获取预设模板定义
   */
  function getTemplate(templateId: string): MenuTemplateConfig | null {
    const templates = getMenuTemplates()
    return templates.find(t => t.templateId === templateId) || null
  }

  /**
   * 获取所有可用的预设菜单模板
   *
   * DEFECT-B修复: 从域权限模板动态派生菜单模板，消除双轨制
   * 原因: 之前域权限模板(templates.ts)和菜单模板(getMenuTemplates())是两套独立系统，
   * ID不对应、逻辑不一致，导致模板应用后域矩阵和侧边栏菜单矛盾
   *
   * 派生逻辑: 从admin角色的域列表推导菜单覆盖规则
   * - admin列出的域 = 该模板认为"核心"的模块 → 菜单可见
   * - admin未列出的域 = 该模板认为"非核心" → 菜单隐藏
   */
  function getMenuTemplates(): MenuTemplateConfig[] {
    // 域→菜单组映射（与DomainPermissionTab保持一致）
    const DOMAIN_TO_MENU_GROUP: Record<string, string> = {
      'workspace': 'workspace',
      'store-ops': 'store-management',
      'product': 'product',
      'order': 'order',
      'operations': 'operations',
      'purchase': 'purchase',
      'warehouse': 'warehouse',
      'member': 'member',
      'finance': 'finance',
      'hr': 'hr',
      'traceability': 'traceability',
      'device': 'device',
      'asset': 'asset',
      'system': 'system',
    }

    const allDomainCodes = Object.keys(DOMAIN_TO_MENU_GROUP)

    return permissionTemplates.value.map(tpl => {
      const adminDomains = tpl.domainMatrix['admin'] || []
      const hiddenDomains = allDomainCodes.filter(d => !adminDomains.includes(d))

      const overrides: MenuOverride[] = hiddenDomains
        .map(domainCode => DOMAIN_TO_MENU_GROUP[domainCode])
        .filter((groupId): groupId is string => !!groupId)
        .map(groupId => ({ groupId, action: 'hide' as const }))

      return {
        templateId: tpl.templateCode, // 统一ID，不再需要映射表
        templateName: tpl.templateName,
        description: tpl.description,
        overrides,
      }
    })
  }

  // ========== 返回值 ==========
  
  return {
    // 状态
    userInfo,
    loaded,
    loading,

    // 计算属性
    isAdmin,
    isLoggedIn,
    isCompanyManager,
    isStoreManager,
    canAccessStoreOps,
    userOrgLevel,
    userDataScope,
    accessibleStores,

    // 方法
    setUserInfo,
    initFromToken,
    clearUserInfo,
    logout,
    hasRole,
    hasAnyRole,
    hasAllRoles,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    canAccessStore,
    canAccessDepartment,

    // 菜单相关
    resolvedMenus,
    menusResolved,
    currentTemplate,
    currentScale,
    registeredMenus,
    menuVersion,
    userMenuOverrides,
    registerMenuGroup,
    lockRegistration,
    getVisibleMenus,
    getCachedMenus,
    setMenuOverrides,
    restoreMenuOverrides,
    applyTemplate,
    resetMenusToDefault,
    getMenuTemplates,

    // 权限模板（4 种模式）
    permissionTemplates,
    templatesLoading,
    templatesLoaded,
    fetchPermissionTemplates,

    // 审计日志（F-006）
    auditLogs,
    addAuditLog,

    // 安全控制（P2）
    registrationLocked,
    isApplyingTemplate,
    suggestedTab,
    suggestTab,
  }
})
