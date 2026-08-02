/**
 * 域权限矩阵组合式函数
 *
 * 对接后端 /v1/permission-templates (PermissionTemplateController)
 *
 * 数据流：
 * 1. onMounted 调用 loadTemplates() 加载4种系统模板 + 自定义模板
 * 2. selectTemplate(code) 切换模板，解析 roleConfig 为 RoleDomainMatrix
 * 3. cycleRoleDomainAccess() 修改矩阵，递增 matrixVersion 触发响应式更新
 * 4. saveAsCustomTemplate() 保存到 localStorage（含完整级别）+ 后端（仅域列表）
 * 5. syncToMenu() 同步到 permissionStore.setMenuOverrides
 *
 * 自定义模板的完整级别保留策略：
 * - 后端 roleConfig JSON 仅存 { role: [domain, ...] }（域列表）
 * - localStorage 额外存 domainMatrixLevels 扩展字段（含 FULL/READ_ONLY/LIMITED）
 * - 加载自定义模板时优先用 localStorage 的 levels，回退到后端推导（FULL/HIDDEN）
 */
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Grid, Odometer, Goods, Operation, DataBoard, Shop,
  ShoppingCart, Box, Money, UserFilled, Setting,
  Position, Monitor, DataLine,
} from '@element-plus/icons-vue'
import type { Component } from 'vue'
import { permissionTemplateApi, parseRoleConfig, type PermissionTemplateDTO } from '@/api/system/permission-template'
import { usePermissionStore } from '@/stores/permission'
import type { MenuOverride } from '@/types/permission'
import type {
  DomainAccessLevel,
  DomainAccessLevelMetaMap,
  BusinessDomain,
  RoleDomainMatrix,
  RoleRow,
  MenuPreviewItem,
  RoleOverviewStat,
  DomainCompareDiff,
  CustomTemplateStorage,
} from '@/types/domain-permission'

/** 图标名 → 图标组件 映射（用于域图标和菜单图标渲染） */
const iconMap: Record<string, Component> = {
  Grid,
  Odometer,
  Goods,
  Operation,
  DataBoard,
  Shop,
  ShoppingCart,
  Box,
  Money,
  UserFilled,
  Setting,
  Position,
  Monitor,
  DataLine,
}

/**
 * 根据图标名解析为 Element Plus 图标组件
 * @param iconName 图标名（如 'Odometer'）
 * @returns 图标组件（未知名返回 Grid）
 */
export function resolveIcon(iconName: string | undefined): Component {
  if (!iconName) return Grid
  return iconMap[iconName] || Grid
}

/** 自定义模板本地存储 key */
const CUSTOM_TEMPLATE_STORAGE_KEY = 'permission-custom-template'

/** 14 个业务域定义 */
export const businessDomains: BusinessDomain[] = [
  { domainCode: 'workspace', domainName: '工作台', icon: 'Odometer' },
  { domainCode: 'store-ops', domainName: '门店运营', icon: 'Shop' },
  { domainCode: 'product', domainName: '产品中心', icon: 'Goods' },
  { domainCode: 'order', domainName: '订单管理', icon: 'Operation' },
  { domainCode: 'operations', domainName: '运营中心', icon: 'DataBoard' },
  { domainCode: 'purchase', domainName: '采购管理', icon: 'ShoppingCart' },
  { domainCode: 'warehouse', domainName: '仓储管理', icon: 'Box' },
  { domainCode: 'member', domainName: '会员管理', icon: 'UserFilled' },
  { domainCode: 'finance', domainName: '财务中心', icon: 'Money' },
  { domainCode: 'hr', domainName: '人事管理', icon: 'UserFilled' },
  { domainCode: 'traceability', domainName: '溯源管理', icon: 'Position' },
  { domainCode: 'device', domainName: '设备管理', icon: 'Monitor' },
  { domainCode: 'asset', domainName: '资产管理', icon: 'DataLine' },
  { domainCode: 'system', domainName: '系统管理', icon: 'Setting' },
]

/** 访问级别元数据 */
export const DomainAccessLevelMeta: DomainAccessLevelMetaMap = {
  FULL: { label: '完全访问', description: '可查看、编辑、删除域内所有资源' },
  READ_ONLY: { label: '只读访问', description: '仅可查看域内资源，不可编辑' },
  LIMITED: { label: '受限访问', description: '仅可访问域内部分资源（如本门店数据）' },
  HIDDEN: { label: '隐藏', description: '完全不可见，菜单和路由均不显示' },
}

/** 访问级别循环顺序：FULL → READ_ONLY → LIMITED → HIDDEN → FULL */
const LEVEL_CYCLE: DomainAccessLevel[] = ['FULL', 'READ_ONLY', 'LIMITED', 'HIDDEN']

/** 角色显示名映射（用于矩阵行头和预览） */
const roleDisplayNameMap: Record<string, string> = {
  admin: '超级管理员',
  owner: '老板',
  ops_director: '运营总监',
  finance_director: '财务总监',
  hr_director: 'HR总监',
  store_manager: '店长',
  team_leader: '组长',
  employee: '普通员工',
  scheduler: '排班员',
  region_manager: '区域经理',
  auditor: '稽查专员',
}

/** 域代码 → 菜单组ID 映射（用于同步到 Store 的菜单覆盖规则） */
const DOMAIN_TO_MENU_GROUP: Record<string, string> = {
  workspace: 'workspace',
  'store-ops': 'store-management',
  product: 'product',
  order: 'order',
  operations: 'operations',
  purchase: 'purchase',
  warehouse: 'warehouse',
  member: 'member',
  finance: 'finance',
  hr: 'hr',
  traceability: 'traceability',
  device: 'device',
  asset: 'asset',
  system: 'system',
}

/** 菜单域映射（用于菜单预览面板，按 businessDomains 顺序） */
const menuDomainMapping: { title: string; icon: string; path: string; domainCode: string }[] = [
  { title: '工作台', icon: 'Odometer', path: '/home', domainCode: 'workspace' },
  { title: '门店运营', icon: 'Shop', path: '/store-management', domainCode: 'store-ops' },
  { title: '产品中心', icon: 'Goods', path: '/product', domainCode: 'product' },
  { title: '订单管理', icon: 'Operation', path: '/order', domainCode: 'order' },
  { title: '运营中心', icon: 'DataBoard', path: '/operations', domainCode: 'operations' },
  { title: '采购管理', icon: 'ShoppingCart', path: '/purchase', domainCode: 'purchase' },
  { title: '仓储管理', icon: 'Box', path: '/warehouse', domainCode: 'warehouse' },
  { title: '会员管理', icon: 'UserFilled', path: '/member', domainCode: 'member' },
  { title: '财务中心', icon: 'Money', path: '/finance', domainCode: 'finance' },
  { title: '人事管理', icon: 'UserFilled', path: '/hr', domainCode: 'hr' },
  { title: '溯源管理', icon: 'Position', path: '/traceability', domainCode: 'traceability' },
  { title: '设备管理', icon: 'Monitor', path: '/device', domainCode: 'device' },
  { title: '资产管理', icon: 'DataLine', path: '/asset', domainCode: 'asset' },
  { title: '系统管理', icon: 'Setting', path: '/system', domainCode: 'system' },
]

/**
 * 获取角色显示名
 * @param roleCode 角色代码
 * @returns 角色显示名（未注册的角色直接返回 code）
 */
export function getRoleDisplayName(roleCode: string): string {
  return roleDisplayNameMap[roleCode] || roleCode
}

/**
 * 获取域名称
 * @param domainCode 域代码
 * @returns 域中文名（未注册的域直接返回 code）
 */
export function getDomainName(domainCode: string): string {
  return businessDomains.find(d => d.domainCode === domainCode)?.domainName || domainCode
}

/**
 * 获取域图标名
 * @param domainCode 域代码
 * @returns Element Plus 图标组件名（默认 'Grid'）
 */
export function getDomainIcon(domainCode: string): string {
  return businessDomains.find(d => d.domainCode === domainCode)?.icon || 'Grid'
}

/**
 * 获取访问级别样式（背景色与文字色，使用 CSS 变量）
 * @param level 访问级别
 * @returns CSS 样式对象
 */
export function getLevelStyle(level: DomainAccessLevel): Record<string, string> {
  const styles: Record<DomainAccessLevel, Record<string, string>> = {
    FULL: {
      background: 'rgba(var(--fts-primary-rgb, 64, 158, 255), 0.12)',
      color: 'var(--fts-primary)',
    },
    READ_ONLY: {
      background: 'rgba(var(--fts-success-rgb, 103, 194, 58), 0.1)',
      color: 'var(--fts-success)',
    },
    LIMITED: {
      background: 'rgba(var(--fts-warning-rgb, 230, 162, 60), 0.1)',
      color: 'var(--fts-warning)',
    },
    HIDDEN: {
      background: 'transparent',
      color: 'var(--fts-text-quaternary)',
    },
  }
  return styles[level] || styles.HIDDEN
}

/**
 * 将权限模板 DTO 解析为完整矩阵
 *
 * 解析规则：
 * 1. 后端 roleConfig JSON 含 { role: [domain, ...] } 域列表
 * 2. 若 levelsOverride 存在（自定义模板的 domainMatrixLevels），使用其完整级别
 * 3. 否则按域列表推导：在列表中=FULL，不在=HIDDEN
 * 4. admin/owner 角色对所有域强制为 FULL
 *
 * @param template 权限模板 DTO
 * @param levelsOverride 完整级别覆盖（可选，来自 localStorage）
 * @returns 角色×域 完整矩阵
 */
function parseTemplateToMatrix(
  template: PermissionTemplateDTO,
  levelsOverride?: Record<string, Record<string, string>>,
): RoleDomainMatrix {
  const domainMatrix = parseRoleConfig(template.roleConfig)
  const matrix: RoleDomainMatrix = {}

  // 收集所有角色（来自域列表和级别覆盖）
  const allRoles = new Set<string>([
    ...Object.keys(domainMatrix),
    ...(levelsOverride ? Object.keys(levelsOverride) : []),
  ])

  for (const roleCode of allRoles) {
    const domains = domainMatrix[roleCode] || []
    const levels = levelsOverride?.[roleCode]
    const roleMatrix: Record<string, DomainAccessLevel> = {}

    // admin/owner 角色特殊处理：对所有域 FULL（不可限制）
    const isAdminRole = roleCode === 'admin' || roleCode === 'owner'

    for (const domain of businessDomains) {
      if (isAdminRole) {
        roleMatrix[domain.domainCode] = 'FULL'
      } else if (levels && levels[domain.domainCode]) {
        roleMatrix[domain.domainCode] = levels[domain.domainCode] as DomainAccessLevel
      } else {
        roleMatrix[domain.domainCode] = domains.includes(domain.domainCode) ? 'FULL' : 'HIDDEN'
      }
    }
    matrix[roleCode] = roleMatrix
  }

  return matrix
}

/**
 * 从 localStorage 读取自定义模板（含完整级别）
 * @returns 自定义模板存储对象，无则返回 null
 */
function loadCustomFromStorage(): CustomTemplateStorage | null {
  try {
    const saved = localStorage.getItem(CUSTOM_TEMPLATE_STORAGE_KEY)
    if (!saved) return null
    return JSON.parse(saved) as CustomTemplateStorage
  } catch (error) {
    console.error('[useDomainPermission] 解析本地自定义模板失败:', error)
    return null
  }
}

/**
 * 域权限矩阵组合式函数
 */
export function useDomainPermission() {
  const permissionStore = usePermissionStore()

  // ========== 模板状态 ==========
  /** 系统模板列表（4种模式） */
  const templates = ref<PermissionTemplateDTO[]>([])
  /** 自定义模板（来自后端，可能为 null） */
  const customTemplate = ref<PermissionTemplateDTO | null>(null)
  /** 自定义模板本地存储（含完整级别，可能为 null） */
  const customStorage = ref<CustomTemplateStorage | null>(null)
  /** 当前选中的模板代码 */
  const currentTemplateCode = ref<string>('centralized-single')

  // ========== 矩阵状态 ==========
  /** 当前矩阵（编辑状态） */
  const roleMatrix = ref<RoleDomainMatrix>({})
  /** 矩阵版本号（每次变更递增，驱动响应式更新） */
  const matrixVersion = ref(0)

  // ========== UI 状态 ==========
  /** 加载中 */
  const loading = ref(false)
  /** 加载失败 */
  const loadError = ref(false)
  /** 同步菜单中 */
  const syncing = ref(false)
  /** 保存自定义模板中 */
  const savingCustom = ref(false)

  // ========== 交互状态 ==========
  /** 当前选中角色 */
  const selectedRole = ref<string>('ops_director')
  /** 对比模式开关 */
  const compareMode = ref(false)
  /** 对比目标角色 */
  const compareRole = ref<string>('store_manager')

  // ========== 模板选项（用于切换选择器） ==========
  /** 可选模板列表（4个系统模板 + 自定义，如果存在） */
  const templateOptions = computed(() => {
    const opts = templates.value
      .filter(t => t.code !== 'custom')
      .map(t => ({ code: t.code, name: t.name, description: t.description }))
    // 自定义模板选项：后端存在或本地存在均显示
    if (customTemplate.value || customStorage.value) {
      opts.push({
        code: 'custom',
        name: customStorage.value?.templateName || customTemplate.value?.name || '自定义模板',
        description: customStorage.value?.description || customTemplate.value?.description || '用户编辑的自定义权限方案',
      })
    }
    return opts
  })

  // ========== 计算属性 ==========

  /** 总域数量 */
  const totalDomainCount = computed(() => businessDomains.length)

  /**
   * 动态角色列表：从当前矩阵派生
   * admin 始终排在最前，其余按 code 字母序
   */
  const roles = computed(() => {
    // 依赖 matrixVersion 触发响应式更新
    const _ver = matrixVersion.value
    const matrix = roleMatrix.value
    return Object.keys(matrix)
      .map(code => ({
        code,
        name: getRoleDisplayName(code),
      }))
      .sort((a, b) => {
        if (a.code === 'admin') return -1
        if (b.code === 'admin') return 1
        return a.code.localeCompare(b.code)
      })
  })

  /** 非 admin 角色列表（用于卡片对比） */
  const nonAdminRoles = computed(() => roles.value.filter(r => r.code !== 'admin'))

  /**
   * 矩阵数据（含每角色的可见域列表）
   * 用于表格行渲染
   */
  const matrixData = computed<RoleRow[]>(() => {
    const _ver = matrixVersion.value
    const matrix = roleMatrix.value
    return roles.value.map(role => {
      const roleMatrixEntry = matrix[role.code] || {}
      const domains = Object.entries(roleMatrixEntry)
        .filter(([, level]) => level !== 'HIDDEN')
        .map(([domain]) => domain)
      return {
        code: role.code,
        name: role.name,
        domains,
      }
    })
  })

  /**
   * 菜单预览：基于 selectedRole 推导每个菜单项的可见性与访问级别
   */
  const menuPreview = computed<MenuPreviewItem[]>(() => {
    const _ver = matrixVersion.value
    const role = selectedRole.value
    return menuDomainMapping.map(menu => {
      const level = getRoleDomainAccessLevel(role, menu.domainCode)
      return {
        title: menu.title,
        icon: menu.icon,
        path: menu.path,
        domainCode: menu.domainCode,
        accessLevel: level,
        visible: level !== 'HIDDEN',
      }
    })
  })

  /** 可见菜单数 */
  const visibleMenuCount = computed(() => menuPreview.value.filter(m => m.visible).length)
  /** 只读菜单数 */
  const readOnlyMenuCount = computed(() => menuPreview.value.filter(m => m.accessLevel === 'READ_ONLY').length)
  /** 完全访问菜单数 */
  const fullAccessMenuCount = computed(() => menuPreview.value.filter(m => m.accessLevel === 'FULL').length)
  /** 总菜单数 */
  const totalMenuCount = computed(() => menuDomainMapping.length)

  /** 当前角色域分布统计 */
  const currentRoleStats = computed(() => {
    const _ver = matrixVersion.value
    const matrix = roleMatrix.value[selectedRole.value] || {}
    const levels = Object.values(matrix) as DomainAccessLevel[]
    return {
      full: levels.filter(l => l === 'FULL').length,
      readOnly: levels.filter(l => l === 'READ_ONLY').length,
      limited: levels.filter(l => l === 'LIMITED').length,
      hidden: levels.filter(l => l === 'HIDDEN').length,
      total: levels.length,
    }
  })

  /**
   * 对比差异：当前角色 vs 对比角色
   * 仅在 compareMode 开启时计算
   */
  const compareDiff = computed<DomainCompareDiff[] | null>(() => {
    if (!compareMode.value) return null
    const _ver = matrixVersion.value
    const currentMatrix = roleMatrix.value[selectedRole.value] || {}
    const compareMatrix = roleMatrix.value[compareRole.value] || {}
    const diffs: DomainCompareDiff[] = []
    for (const domain of businessDomains) {
      const cur = currentMatrix[domain.domainCode] as DomainAccessLevel
      const cmp = compareMatrix[domain.domainCode] as DomainAccessLevel
      if (cur !== cmp) {
        diffs.push({
          domainCode: domain.domainCode,
          domainName: domain.domainName,
          current: cur,
          compare: cmp,
        })
      }
    }
    return diffs
  })

  /** 全角色权限概览（用于摘要栏） */
  const roleOverviewStats = computed<RoleOverviewStat[]>(() => {
    const _ver = matrixVersion.value
    const matrix = roleMatrix.value
    return nonAdminRoles.value.map(role => {
      const domains = matrix[role.code] || {}
      const levels = Object.values(domains) as DomainAccessLevel[]
      return {
        code: role.code,
        name: role.name,
        full: levels.filter(l => l === 'FULL').length,
        readOnly: levels.filter(l => l === 'READ_ONLY').length,
        limited: levels.filter(l => l === 'LIMITED').length,
        hidden: levels.filter(l => l === 'HIDDEN').length,
      }
    })
  })

  // ========== 矩阵读取方法 ==========

  /**
   * 获取角色在指定域的访问级别
   * @param roleCode 角色代码
   * @param domainCode 域代码
   * @returns 访问级别（无数据返回 HIDDEN）
   */
  function getRoleDomainAccessLevel(roleCode: string, domainCode: string): DomainAccessLevel {
    return roleMatrix.value[roleCode]?.[domainCode] || 'HIDDEN'
  }

  /**
   * 获取角色的可见域列表
   * @param roleCode 角色代码
   * @returns 可见域代码列表（非 HIDDEN）
   */
  function getRoleDomains(roleCode: string): string[] {
    const matrix = roleMatrix.value[roleCode] || {}
    return Object.entries(matrix)
      .filter(([, level]) => level !== 'HIDDEN')
      .map(([domain]) => domain)
  }

  /**
   * 获取完整矩阵（只读视图）
   */
  function getFullRoleDomainMatrix(): RoleDomainMatrix {
    return roleMatrix.value
  }

  // ========== 矩阵编辑方法 ==========

  /**
   * 循环切换角色在指定域的访问级别
   * 顺序：FULL → READ_ONLY → LIMITED → HIDDEN → FULL
   * admin/owner 角色禁止修改
   *
   * @param roleCode 角色代码
   * @param domainCode 域代码
   * @returns 切换后的访问级别
   */
  function cycleRoleDomainAccess(roleCode: string, domainCode: string): DomainAccessLevel {
    if (roleCode === 'admin' || roleCode === 'owner') {
      return 'FULL'
    }
    const current = getRoleDomainAccessLevel(roleCode, domainCode)
    const idx = LEVEL_CYCLE.indexOf(current)
    const next = LEVEL_CYCLE[(idx + 1) % LEVEL_CYCLE.length]

    // 直接修改矩阵 ref
    if (!roleMatrix.value[roleCode]) {
      roleMatrix.value[roleCode] = {}
    }
    roleMatrix.value[roleCode][domainCode] = next
    matrixVersion.value++

    return next
  }

  // ========== 模板加载与切换 ==========

  /**
   * 加载所有模板（4种系统模板 + 自定义模板）
   * 默认选中 centralized-single
   */
  async function loadTemplates(): Promise<void> {
    loading.value = true
    loadError.value = false
    try {
      // 并行加载系统模板和自定义模板（自定义模板失败不阻塞）
      const [systemList, custom] = await Promise.all([
        permissionTemplateApi.getSystemTemplates(),
        permissionTemplateApi.getByCode('custom').catch(() => null),
      ])
      templates.value = systemList
      customTemplate.value = custom
      customStorage.value = loadCustomFromStorage()

      // 默认加载 centralized-single 模板
      await selectTemplate('centralized-single')
    } catch (error) {
      console.error('[useDomainPermission] 加载权限模板失败:', error)
      loadError.value = true
    } finally {
      loading.value = false
    }
  }

  /**
   * 切换到指定模板，解析为矩阵
   * @param code 模板代码
   */
  async function selectTemplate(code: string): Promise<void> {
    currentTemplateCode.value = code

    let template: PermissionTemplateDTO | null = null
    let levelsOverride: Record<string, Record<string, string>> | undefined

    if (code === 'custom') {
      // 自定义模板：优先用本地存储（含完整级别），回退到后端
      if (customStorage.value) {
        // 从本地存储构造一个临时 DTO 用于解析
        template = {
          id: customTemplate.value?.id || 0,
          name: customStorage.value.templateName,
          code: 'custom',
          description: customStorage.value.description,
          enterpriseType: '',
          scaleRange: '',
          roleConfig: JSON.stringify(customStorage.value.domainMatrix),
          isSystem: false,
          status: 1,
          createdAt: customStorage.value.savedAt,
          updatedAt: customStorage.value.savedAt,
        }
        levelsOverride = customStorage.value.domainMatrixLevels
      } else if (customTemplate.value) {
        template = customTemplate.value
      }
    } else {
      template = templates.value.find(t => t.code === code) || null
    }

    if (!template) {
      roleMatrix.value = {}
      matrixVersion.value++
      return
    }

    roleMatrix.value = parseTemplateToMatrix(template, levelsOverride)
    matrixVersion.value++

    // 调整 selectedRole：若当前角色不在新矩阵中，选第一个非 admin 角色
    const roleCodes = Object.keys(roleMatrix.value)
    if (!roleCodes.includes(selectedRole.value)) {
      const firstNonAdmin = roleCodes.find(c => c !== 'admin' && c !== 'owner')
      selectedRole.value = firstNonAdmin || roleCodes[0] || 'ops_director'
    }
  }

  // ========== 同步与保存 ==========

  /**
   * 将当前选中角色的域权限矩阵同步到 Store 的菜单覆盖规则
   *
   * 策略：对当前选中角色，将其所有 HIDDEN 域对应的菜单组设为隐藏
   * 这样管理员可以预览该角色视角下的侧边栏效果
   */
  async function syncToMenu(): Promise<void> {
    if (selectedRole.value === 'admin' || selectedRole.value === 'owner') {
      ElMessage.warning('超级管理员拥有全部权限，无需同步')
      return
    }

    syncing.value = true
    try {
      const matrix = roleMatrix.value[selectedRole.value]
      if (!matrix) {
        ElMessage.warning('未找到该角色的权限矩阵数据')
        return
      }

      const overrides: MenuOverride[] = []
      for (const domainCode of Object.keys(DOMAIN_TO_MENU_GROUP)) {
        const level = matrix[domainCode] as DomainAccessLevel
        const menuGroupId = DOMAIN_TO_MENU_GROUP[domainCode]
        if (level === 'HIDDEN' && menuGroupId) {
          overrides.push({ groupId: menuGroupId, action: 'hide' })
        }
      }

      permissionStore.setMenuOverrides(overrides)
      permissionStore.getVisibleMenus()

      const roleName = roles.value.find(r => r.code === selectedRole.value)?.name || selectedRole.value
      ElMessage.success(`已同步「${roleName}」的权限到菜单，隐藏了 ${overrides.length} 个模块`)
    } finally {
      syncing.value = false
    }
  }

  /**
   * 将当前域权限矩阵保存为自定义模板
   *
   * 保存策略：
   * 1. localStorage：保存完整级别（含 FULL/READ_ONLY/LIMITED）
   * 2. 后端 /v1/permission-templates：保存简化版 roleConfig（仅域列表）
   *    - 若后端已有 custom 模板，调用 update
   *    - 否则调用 create
   *
   * admin/owner 角色不保存（始终全开）
   */
  async function saveAsCustomTemplate(): Promise<void> {
    const matrix = roleMatrix.value
    if (!matrix || Object.keys(matrix).length === 0) {
      ElMessage.warning('当前无权限矩阵数据可保存')
      return
    }

    savingCustom.value = true
    try {
      // 构建自定义模板数据
      const customMatrix: Record<string, string[]> = {}
      const customMatrixLevels: Record<string, Record<string, string>> = {}

      for (const [roleCode, domains] of Object.entries(matrix)) {
        if (roleCode === 'admin' || roleCode === 'owner') continue
        // 域名列表（兼容标准模板格式）
        customMatrix[roleCode] = Object.entries(domains)
          .filter(([, level]) => level !== 'HIDDEN')
          .map(([domain]) => domain)
        // 级别明细（扩展格式，防止 READ_ONLY/LIMITED 丢失）
        customMatrixLevels[roleCode] = {}
        for (const [domain, level] of Object.entries(domains)) {
          if (level !== 'HIDDEN') {
            customMatrixLevels[roleCode][domain] = level
          }
        }
      }

      const now = new Date().toISOString()
      const customStorageData: CustomTemplateStorage = {
        templateCode: 'custom',
        templateName: '自定义模板',
        description: '用户通过域权限矩阵编辑器创建的自定义权限方案',
        domainMatrix: customMatrix,
        domainMatrixLevels: customMatrixLevels,
        savedAt: now,
      }

      // 1. 保存到 localStorage（含完整级别）
      localStorage.setItem(CUSTOM_TEMPLATE_STORAGE_KEY, JSON.stringify(customStorageData))
      customStorage.value = customStorageData

      // 2. 尝试保存到后端（仅域列表，不包含 levels 扩展字段）
      try {
        if (customTemplate.value) {
          await permissionTemplateApi.update(customTemplate.value.id, {
            roleConfig: JSON.stringify(customMatrix),
            name: '自定义模板',
            description: customStorageData.description,
          })
        } else {
          const created = await permissionTemplateApi.create({
            name: '自定义模板',
            code: 'custom',
            description: customStorageData.description,
            enterpriseType: 'restaurant',
            scaleRange: 'all',
            roleConfig: JSON.stringify(customMatrix),
          })
          customTemplate.value = created
        }
      } catch (error) {
        // 后端保存失败不阻塞，仅提示（本地存储已成功）
        console.warn('[useDomainPermission] 后端保存自定义模板失败（本地已保存）:', error)
        ElMessage.warning('自定义模板已保存到本地，但后端同步失败（可不影响使用）')
        return
      }

      ElMessage.success('自定义模板已保存（含完整访问级别），可在模板切换中查看')
    } catch (error: unknown) {
      console.error('[useDomainPermission] 保存自定义模板失败:', error)
      ElMessage.error('保存自定义模板失败')
    } finally {
      savingCustom.value = false
    }
  }

  return {
    // 状态
    templates,
    customTemplate,
    customStorage,
    currentTemplateCode,
    templateOptions,
    roleMatrix,
    matrixVersion,
    loading,
    loadError,
    syncing,
    savingCustom,
    selectedRole,
    compareMode,
    compareRole,

    // 计算属性
    roles,
    nonAdminRoles,
    matrixData,
    menuPreview,
    visibleMenuCount,
    readOnlyMenuCount,
    fullAccessMenuCount,
    totalMenuCount,
    totalDomainCount,
    currentRoleStats,
    compareDiff,
    roleOverviewStats,

    // 矩阵读取
    getRoleDomainAccessLevel,
    getRoleDomains,
    getFullRoleDomainMatrix,

    // 矩阵编辑
    cycleRoleDomainAccess,

    // 模板加载与切换
    loadTemplates,
    selectTemplate,

    // 同步与保存
    syncToMenu,
    saveAsCustomTemplate,
  }
}
