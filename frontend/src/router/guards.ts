import type { Router } from 'vue-router'
import { usePermissionStore, UserRole, OrgLevel } from '@/stores/permission'

/**
/**
 * 域权限矩阵（单源：与各模块 menu.ts 的 visibleRoles 对齐，P0-SEC-004）
 *
 * 域 → 可见角色清单。设计原则（P0-SEC-003 默认拒绝）：
 * - 只放行【显式配置】的域-角色组合；未列入本表的域 → undefined（拒绝访问）
 * - 不再有 READ_ONLY 兜底，未配置即拒绝（fail-closed）
 *
 * 与菜单对齐依据（各 menu.ts visibleRoles）：
 * - workspace  ：工作台（workspace/menu.ts 全员）+ 物资需求提报（request-center/menu.ts
 *                [OWNER,ADMIN,STORE_MANAGER,TEAM_LEADER,DEPARTMENT_MANAGER,EMPLOYEE]）
 * - store-ops  ：store-management/menu.ts [STORE_MANAGER,TEAM_LEADER,OWNER,ADMIN,OPS_DIRECTOR]
 * - product    ：product/menu.ts [OWNER,ADMIN,OPS_DIRECTOR,STORE_MANAGER]
 * - order      ：order/menu.ts [OWNER,ADMIN,OPS_DIRECTOR,STORE_MANAGER]
 * - operations ：operations/menu.ts [OWNER,ADMIN,OPS_DIRECTOR]
 * - purchase   ：purchase/menu.ts [OWNER,ADMIN,OPS_DIRECTOR,PURCHASE_MANAGER,STORE_MANAGER]
 * - warehouse  ：warehouse/menu.ts [OWNER,ADMIN,OPS_DIRECTOR,WAREHOUSE_MANAGER,STORE_MANAGER]
 * - member     ：member/menu.ts [OWNER,ADMIN,OPS_DIRECTOR,STORE_MANAGER]
 * - finance    ：finance/menu.ts [OWNER,ADMIN,FINANCE_DIRECTOR]
 * - hr         ：hr/menu.ts [OWNER,ADMIN,HR_DIRECTOR]
 * - traceability：traceability/menu.ts [OWNER,ADMIN,OPS_DIRECTOR,STORE_MANAGER]
 * - device     ：device/menu.ts [OWNER,ADMIN]
 * - asset      ：asset/menu.ts [OWNER,ADMIN]
 * - system     ：system/menu.ts [OWNER,ADMIN]
 *
 * 注：seal（电子签章）路由 /seal/management 的 meta.domain 为 'system'，
 * 但其菜单 seal/menu.ts 可见角色为 [OWNER,ADMIN,HR_DIRECTOR,FINANCE_DIRECTOR,OPS_DIRECTOR]，
 * 因此该路由已在 router/index.ts 声明精确 meta.roles，守卫对显式声明 roles 的路由
 * 以路由声明为准（见 beforeEach 中 hasExplicitRouteRoles 分支），避免域矩阵误拦。
 */
const DOMAIN_VISIBLE_ROLES: Record<string, UserRole[]> = {
  'workspace': [
    UserRole.OWNER, UserRole.ADMIN, UserRole.OPS_DIRECTOR, UserRole.FINANCE_DIRECTOR,
    UserRole.HR_DIRECTOR, UserRole.PURCHASE_MANAGER, UserRole.WAREHOUSE_MANAGER,
    UserRole.DEPARTMENT_MANAGER, UserRole.STORE_MANAGER, UserRole.TEAM_LEADER,
    UserRole.SCHEDULER, UserRole.REGION_MANAGER, UserRole.AUDITOR, UserRole.EMPLOYEE,
  ],
  'store-ops': [UserRole.OWNER, UserRole.ADMIN, UserRole.OPS_DIRECTOR, UserRole.STORE_MANAGER, UserRole.TEAM_LEADER],
  'product': [UserRole.OWNER, UserRole.ADMIN, UserRole.OPS_DIRECTOR, UserRole.STORE_MANAGER],
  'order': [UserRole.OWNER, UserRole.ADMIN, UserRole.OPS_DIRECTOR, UserRole.STORE_MANAGER],
  'operations': [UserRole.OWNER, UserRole.ADMIN, UserRole.OPS_DIRECTOR],
  'purchase': [UserRole.OWNER, UserRole.ADMIN, UserRole.OPS_DIRECTOR, UserRole.PURCHASE_MANAGER, UserRole.STORE_MANAGER],
  'warehouse': [UserRole.OWNER, UserRole.ADMIN, UserRole.OPS_DIRECTOR, UserRole.WAREHOUSE_MANAGER, UserRole.STORE_MANAGER],
  'member': [UserRole.OWNER, UserRole.ADMIN, UserRole.OPS_DIRECTOR, UserRole.STORE_MANAGER],
  'finance': [UserRole.OWNER, UserRole.ADMIN, UserRole.FINANCE_DIRECTOR],
  'hr': [UserRole.OWNER, UserRole.ADMIN, UserRole.HR_DIRECTOR],
  'traceability': [UserRole.OWNER, UserRole.ADMIN, UserRole.OPS_DIRECTOR, UserRole.STORE_MANAGER],
  'device': [UserRole.OWNER, UserRole.ADMIN],
  'asset': [UserRole.OWNER, UserRole.ADMIN],
  'system': [UserRole.OWNER, UserRole.ADMIN],
}

/**
 * 获取角色在指定域的访问级别（P0-SEC-003：默认拒绝）
 *
 * 访问级别说明：
 * - 'FULL'：完全访问（读+写），仅当域-角色组合在 DOMAIN_VISIBLE_ROLES 中显式配置
 * - undefined：未显式配置 → 拒绝访问（fail-closed）
 *
 * @param role 用户角色
 * @param domain 域标识符
 * @returns 访问级别字符串（'FULL'）或 undefined
 */
function getRoleDomainAccessLevel(role: UserRole, domain: string): string | undefined {
  // 超级管理员对所有域拥有完全访问权限
  if (role === UserRole.OWNER || role === UserRole.ADMIN) {
    return 'FULL'
  }

  // 默认拒绝：未显式配置的域返回 undefined，checkDomainAccess 据此拒绝
  const visibleRoles = DOMAIN_VISIBLE_ROLES[domain]
  if (!visibleRoles) {
    return undefined
  }

  return visibleRoles.includes(role) ? 'FULL' : undefined
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

    // 公开路由判定（P1-SEC-002）：读取 meta.requireAuth === false（或旧 meta.public）判定公开页，
    // 不再使用硬编码双轨白名单；/login、/demo/component-gallery、/verify-code、/device-activation
    // 等页面已在 router/index.ts 声明 requireAuth: false，此处统一按 meta 判定。
    // 必须在 initFromToken() 之前判断，避免已登录用户访问 /login 时被重定向
    const isPublicRoute = to.meta.requireAuth === false || to.meta.public === true
    if (isPublicRoute) {
      if (permissionStore.isLoggedIn && to.path === '/login') {
        return { path: '/home' }
      }
      return true
    }

    // 非公开路由：需要先确保用户信息已加载。
    // 竞态修复（2026-10-23）：原来 `!loaded && !loading` 会在「初始化进行中」时直接跳过等待，
    // 于是紧接着的 isLoggedIn 判定拿到 false，把刚登录/刚刷新深链的用户弹回 /login。
    // 现改为：若正在加载则等它结束（最多 5s），仍未 loaded 再兜底初始化一次。
    if (!permissionStore.loaded) {
      if (permissionStore.loading) {
        const deadline = Date.now() + 5000
        while (permissionStore.loading && Date.now() < deadline) {
          await new Promise((resolve) => setTimeout(resolve, 20))
        }
      }
      if (!permissionStore.loaded) {
        await permissionStore.initFromToken()
      }
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
      // P0-SEC-004：路由若显式声明 meta.roles，则以其为单源精确校验（如 /seal/management、
      // /system/device-whitelist、/company-init、/supplier-portal/links 等敏感路由）；
      // 未显式声明的业务域路由 → 走域矩阵 DOMAIN_VISIBLE_ROLES 强制校验（默认拒绝）。
      const routeRoles = to.meta.roles as UserRole[] | undefined
      if (Array.isArray(routeRoles) && routeRoles.length > 0) {
        if (!checkRoutePermission(permissionStore, { roles: routeRoles })) {
          handleAccessDenied(to.path, to.meta.title as string || '该页面')
          return false
        }
      } else if (!checkDomainAccess(permissionStore, domain)) {
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
