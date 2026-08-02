import type { Router } from 'vue-router'
import { usePermissionStore, UserRole, OrgLevel } from '@/stores/permission'

/**
 * 获取角色在指定域的访问级别
 *
 * 基于角色域权限矩阵的默认实现，待后端权限中心 API 落地后替换为真实接口调用。
 *
 * 访问级别说明：
 * - 'FULL'：完全访问（读+写）
 * - 'READ_ONLY'：只读访问（可查看页面，不可修改）
 * - 'LIMITED'：受限访问
 * - 'HIDDEN'：不可访问
 * - undefined：未知角色，拒绝访问
 *
 * 矩阵设计原则：
 * - owner/admin 对所有域拥有 FULL 权限
 * - 各总监角色对其主管业务域拥有 FULL，其他域 READ_ONLY（可跨域查看）
 * - 门店级角色聚焦门店运营相关域
 * - 基层员工仅可访问工作台
 *
 * @param role 用户角色
 * @param domain 域标识符
 * @returns 访问级别字符串（'FULL' | 'READ_ONLY' | 'LIMITED' | 'HIDDEN'）或 undefined
 */
function getRoleDomainAccessLevel(role: UserRole, domain: string): string | undefined {
  // 超级管理员对所有域拥有完全访问权限
  if (role === UserRole.OWNER || role === UserRole.ADMIN) {
    return 'FULL'
  }

  // 各角色拥有完全访问权限的域清单，未列出的域默认 READ_ONLY
  const roleFullDomains: Partial<Record<UserRole, string[]>> = {
    // 运营总监：门店运营、产品、订单、运营中心、会员管理
    [UserRole.OPS_DIRECTOR]: ['workspace', 'store-ops', 'product', 'order', 'operations', 'member'],
    // 财务总监：财务中心
    [UserRole.FINANCE_DIRECTOR]: ['workspace', 'finance'],
    // HR总监：人事管理
    [UserRole.HR_DIRECTOR]: ['workspace', 'hr'],
    // 店长：门店运营、产品、订单、会员管理
    [UserRole.STORE_MANAGER]: ['workspace', 'store-ops', 'product', 'order', 'member'],
    [UserRole.TEAM_LEADER]: ['workspace'],
    [UserRole.REGION_MANAGER]: ['workspace'],
    [UserRole.AUDITOR]: ['workspace'],
    [UserRole.EMPLOYEE]: ['workspace'],
    [UserRole.SCHEDULER]: ['workspace'],
  }

  const fullDomains = roleFullDomains[role]
  if (!fullDomains) {
    // 未知角色，拒绝访问
    return undefined
  }

  return fullDomains.includes(domain) ? 'FULL' : 'READ_ONLY'
}

/**
 * 路由权限配置
 * 定义每个路由需要的角色/权限要求
 */
interface RoutePermissionConfig {
  /** 需要的角色列表（满足其一即可） */
  roles?: UserRole[]
  /** 需要的权限列表（满足其一即可） */
  permissions?: string[]
  /** 需要的最小组织层级 */
  minOrgLevel?: OrgLevel
  /** 是否需要认证（默认true） */
  requireAuth?: boolean
}

/**
 * 域名称映射（用于提示消息）
 */
const DOMAIN_NAMES: Record<string, string> = {
  'workspace': '工作台',
  'store-ops': '门店管理',
  'product': '产品中心',
  'order': '订单管理',
  'operations': '运营中心',
  'purchase': '采购管理',
  'warehouse': '仓储管理',
  'member': '会员管理',
  'finance': '财务中心',
  'hr': '人事管理',
  'traceability': '食品追溯',
  'device': '设备管理',
  'asset': '资产管理',
  'system': '系统管理',
}

/**
 * 设置路由守卫
 * @param router Vue Router 实例
 */
export function setupRouterGuards(router: Router): void {
  
  /**
   * 前置守卫：路由跳转前的权限检查
   */
  router.beforeEach(async (to) => {
    const permissionStore = usePermissionStore()

    // 公开路由白名单：始终可访问，不受登录状态影响
    // 必须在 initFromToken() 之前判断，避免已登录用户访问 /login 时被重定向
    const publicRoutes = ['/login', '/forgot-password', '/403', '/404', '/demo/component-gallery', '/portal/sign']
    if (publicRoutes.some(p => to.path.startsWith(p))) {
      // 已登录访问登录页 → 跳转首页
      if (permissionStore.isLoggedIn && to.path === '/login') {
        return { path: '/home' }
      }
      return true
    }

    // 非公开路由：需要先确保用户信息已加载
    if (!permissionStore.loaded && !permissionStore.loading) {
      await permissionStore.initFromToken()
    }

    // 全局兜底：确保权限模板已加载（菜单按模板 admin 域列表派生）。
    // 幂等：已加载则直接返回；异步不阻塞导航。任何进入路径（刷新/直达 URL/SPA 导航）都保证模板可用。
    if (permissionStore.isLoggedIn) {
      permissionStore.fetchPermissionTemplates().catch(() => {})
    }

    // 加载完用户信息后仍未登录 → 跳转登录页
    if (!permissionStore.isLoggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    // ========== admin/owner 超级管理员豁免域检查 ==========
    // 设计原则：超级管理员拥有全部权限，豁免后续的域检查与"未配置 meta.domain"拦截。
    // 注意：此处仅豁免域/路由权限检查，前置的登录检查与公开路由白名单仍保留，
    // 不会让未登录用户绕过认证；其他角色的权限隔离不受影响。
    // 主要防御场景：
    //   1) 路由未配置 meta.domain（如 URL 拼写错误 /purchase/order 单数形式）
    //   2) 权限模板 centralized-single 模式下 admin 域矩阵不含 purchase
    if (permissionStore.isAdmin) {
      return true
    }

    // ========== 域权限检查（基于路由meta.domain） ==========
    const domain = to.meta.domain as string | undefined

    // 无需域检查的白名单路由（全局页面、错误页、个人中心等）
    // 注：个人中心是登录用户的基础功能（修改密码、查看个人信息、界面偏好），
    // 不属于任何业务域，所有登录用户均可访问。
    const noDomainRequiredRoutes = ['/home', '/404', '/403', '/demo/component-gallery', '/personal-center']
    if (!domain && !noDomainRequiredRoutes.includes(to.path)) {
      // 安全策略：未配置 meta.domain 的业务路由默认拒绝访问
      // 防止开发者忘记配置域权限导致越权
      console.warn(`[路由守卫] ⚠️ 路由 ${to.path} 未配置 meta.domain，已拒绝访问`)
      handleAccessDenied(to.path, to.meta.title as string || '该页面')
      return false
    }

    if (domain) {
      if (!checkDomainAccess(permissionStore, domain)) {
        const domainName = DOMAIN_NAMES[domain] || domain
        handleAccessDenied(to.path, domainName)
        return false
      }
    }

    // 检查路由meta中自定义的权限要求
    if (to.meta.roles || to.meta.permissions) {
      const routeConfig: RoutePermissionConfig = {
        roles: to.meta.roles as UserRole[],
        permissions: to.meta.permissions as string[],
      }
      
      if (!checkRoutePermission(permissionStore, routeConfig)) {
        handleAccessDenied(to.path, to.meta.title as string || '该页面')
        return false
      }
    }
    
    return true
  })

  /**
   * 后置守卫：可用于日志记录等
   */
  router.afterEach((to) => {
    document.title = `${to.meta.title || '页面'} - 食品溯源系统`
  })
}

/**
 * 检查域权限访问
 * 通过域矩阵判断当前用户是否有权访问指定域
 * admin角色自动通过
 */
function checkDomainAccess(
  permissionStore: ReturnType<typeof usePermissionStore>,
  domain: string
): boolean {
  // 超级管理员始终通过
  if (permissionStore.isAdmin) return true

  // 从域矩阵获取当前角色的域访问级别
  const userRoles = permissionStore.userInfo?.roles || []
  if (userRoles.length === 0) return false

  // 检查用户任一角色是否有该域的访问权限（非HIDDEN即可）
  for (const role of userRoles) {
    const accessLevel = getRoleDomainAccessLevel(role, domain)
    if (accessLevel && accessLevel !== 'HIDDEN') {
      return true
    }
  }

  return false
}

/**
 * 检查自定义路由权限配置
 * @param permissionStore 权限Store
 * @param config 权限配置
 */
function checkRoutePermission(
  permissionStore: ReturnType<typeof usePermissionStore>,
  config: RoutePermissionConfig
): boolean {
  // 超级管理员始终通过
  if (permissionStore.isAdmin) return true
  
  // 检查角色要求
  if (config.roles && config.roles.length > 0) {
    if (!permissionStore.hasAnyRole(config.roles)) {
      return false
    }
  }
  
  // 检查权限要求
  if (config.permissions && config.permissions.length > 0) {
    if (!permissionStore.hasAnyPermission(config.permissions)) {
      return false
    }
  }
  
  // 检查组织层级要求
  if (config.minOrgLevel) {
    const levelOrder = [OrgLevel.OWNER, OrgLevel.COMPANY, OrgLevel.DEPARTMENT, OrgLevel.STORE_MGR, OrgLevel.STORE_STAFF]
    const userLevelIndex = levelOrder.indexOf(permissionStore.userOrgLevel)
    const requiredLevelIndex = levelOrder.indexOf(config.minOrgLevel)
    
    if (userLevelIndex < requiredLevelIndex) {
      return false
    }
  }
  
  return true
}

/**
 * 处理无权访问的情况
 * @param targetPath 目标路由路径
 * @param moduleName 模块名称（用于提示）
 */
function handleAccessDenied(
  targetPath: string,
  moduleName: string
): void {
  console.warn(`[路由守卫] ⛔ 用户无权访问 ${moduleName} 模块: ${targetPath}`)
  
  showAccessDeniedMessage(moduleName)
}

/**
 * 显示无权访问提示
 * @param moduleName 模块名称
 */
function showAccessDeniedMessage(moduleName: string): void {
  import('element-plus').then(({ ElMessage }) => {
    ElMessage.warning({
      message: `您没有权限访问「${moduleName}」模块，如需访问请联系管理员分配相应角色`,
      duration: 5000,
      grouping: true,
    })
  })
}
