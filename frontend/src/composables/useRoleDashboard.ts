/**
 * 角色工作台数据加载 Composable
 *
 * 职责：
 * 1. 从 permission store 读取当前用户角色
 * 2. 解析为对应的 RoleProfile（角色组配置）
 * 3. 根据 RoleProfile 中使用的 dataSource 决定调用哪些后端 API
 * 4. 将后端返回值解析为前端可直接展示的统计卡片数据
 * 5. 提供预警信息（库存预警）和空状态说明
 *
 * 数据流：
 *   permissionStore.userInfo.roles
 *     → resolveRoleProfile(roles)
 *     → 检测 profile.statsCards 中使用的 dataSource
 *     → 并行调用 dashboardApi.getTodaySales/getInventoryAlerts/getMemberGrowth/getFinanceSummary
 *     → 按 dataSource 映射为 StatCardValue[]
 *     → 返回给 dashboard/index.vue 渲染
 */
import { ref, computed, type Ref } from 'vue'
import { usePermissionStore, UserRole } from '@/stores/permission'
import { dashboardApi, type TodaySalesOverview, type InventoryAlerts, type MemberGrowthTrend, type FinanceSummary } from '@/api/dashboard'
import {
  resolveRoleProfile,
  type RoleProfile,
  type StatCardConfig,
  type StatDataSource,
} from '@/views/dashboard/role-profiles'

// ============================================================
// 类型定义
// ============================================================

/** 已解析值的统计卡片（供模板直接渲染） */
export interface ResolvedStatCard extends StatCardConfig {
  /** 格式化后的展示值 */
  displayValue: string
  /** 趋势值（百分比，可为负） */
  trend: number
  /** 是否后端 TODO（展示"待实现"角标） */
  showTodoBadge: boolean
}

/** 预警信息项 */
export interface DashboardAlertItem {
  type: 'warning' | 'error'
  module: string
  content: string
  count: number
  /** 跳转路径（可选，点击查看详情） */
  link?: string
}

/** Composable 返回值 */
export interface UseRoleDashboardReturn {
  /** 当前角色组配置 */
  profile: Ref<RoleProfile>
  /** 当前用户名 */
  userName: Ref<string>
  /** 问候语（已替换用户名） */
  greeting: Ref<string>
  /** 已解析的统计卡片列表 */
  statsCards: Ref<ResolvedStatCard[]>
  /** 预警信息列表 */
  alerts: Ref<DashboardAlertItem[]>
  /** 加载中状态 */
  loading: Ref<boolean>
  /** 错误信息（加载失败时） */
  errorMessage: Ref<string>
  /** 是否为空数据状态（所有 API 返回 0 或为空） */
  isEmpty: Ref<boolean>
  /** 重新加载数据 */
  refresh: () => Promise<void>
}

// ============================================================
// 工具函数
// ============================================================

/** 金额分→元展示（后端金额以分为单位，工作台卡片统一以元展示） */
function formatCurrency(fen: number): string {
  const yuan = fen / 100
  return `¥${yuan.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

/** 数字千分位格式化 */
function formatNumber(n: number): string {
  return n.toLocaleString('zh-CN')
}

/** 百分比格式化（保留 1 位小数） */
function formatPercent(n: number): string {
  return `${n.toFixed(1)}%`
}

/**
 * 根据 dataSource 标识从已加载的数据集中读取值
 * @returns 数值（number），未知字段返回 0
 */
function resolveStatValue(
  dataSource: StatDataSource,
  todaySales: TodaySalesOverview | null,
  inventory: InventoryAlerts | null,
  member: MemberGrowthTrend | null,
  finance: FinanceSummary | null,
): number {
  switch (dataSource) {
    case 'todaySales.todayOrderCount':
      return todaySales?.todayOrderCount ?? 0
    case 'todaySales.todayAmount':
      return todaySales?.todayAmount ?? 0
    case 'todaySales.yesterdayGrowth':
      return todaySales?.yesterdayGrowth ?? 0
    case 'inventory.warningCount':
      return inventory?.warningCount ?? 0
    case 'inventory.outOfStockCount':
      return inventory?.outOfStockCount ?? 0
    case 'inventory.overstockCount':
      return inventory?.overstockCount ?? 0
    case 'member.totalMembers':
      return member?.totalMembers ?? 0
    case 'member.newMembers':
      return member?.newMembers ?? 0
    case 'member.growthRate':
      return member?.growthRate ?? 0
    case 'finance.monthlyRevenue':
      return finance?.monthlyRevenue ?? 0
    case 'finance.monthlyExpense':
      return finance?.monthlyExpense ?? 0
    case 'finance.monthlyProfit':
      return finance?.monthlyProfit ?? 0
    case 'finance.profitMargin':
      return finance?.profitMargin ?? 0
    default:
      return 0
  }
}

/**
 * 根据 format 类型格式化展示值
 */
function formatStatDisplay(value: number, format?: 'number' | 'currency' | 'percent'): string {
  switch (format) {
    case 'currency':
      return formatCurrency(value)
    case 'percent':
      return formatPercent(value)
    case 'number':
    default:
      return formatNumber(value)
  }
}

/**
 * 检测角色组需要加载哪些数据源
 * @returns 需要加载的 API 集合
 */
function detectRequiredApis(profile: RoleProfile): {
  needTodaySales: boolean
  needInventory: boolean
  needMember: boolean
  needFinance: boolean
} {
  const sources = new Set(profile.statsCards.map(c => c.dataSource))
  return {
    needTodaySales: [...sources].some(s => s.startsWith('todaySales.')),
    needInventory: [...sources].some(s => s.startsWith('inventory.')),
    needMember: [...sources].some(s => s.startsWith('member.')),
    needFinance: [...sources].some(s => s.startsWith('finance.')),
  }
}

/**
 * 根据角色组和已加载的库存数据，构建预警信息列表
 */
function buildAlerts(
  profile: RoleProfile,
  inventory: InventoryAlerts | null,
): DashboardAlertItem[] {
  const alerts: DashboardAlertItem[] = []
  if (!inventory) return alerts

  // 库存预警（warningCount > 0 时才展示，避免零值预警误导）
  if (inventory.warningCount > 0) {
    alerts.push({
      type: 'warning',
      module: '库存预警',
      content: `当前有 ${inventory.warningCount} 项库存预警，请及时关注`,
      count: inventory.warningCount,
      link: '/warehouse/inventory-warning',
    })
  }
  // 缺货预警
  if (inventory.outOfStockCount > 0) {
    alerts.push({
      type: 'error',
      module: '缺货预警',
      content: `当前有 ${inventory.outOfStockCount} 项缺货，请尽快补货`,
      count: inventory.outOfStockCount,
      link: '/warehouse/inventory',
    })
  }
  // 积压预警
  if (inventory.overstockCount > 0) {
    alerts.push({
      type: 'warning',
      module: '积压预警',
      content: `当前有 ${inventory.overstockCount} 项库存积压，请关注周转`,
      count: inventory.overstockCount,
      link: '/warehouse/inventory-analysis',
    })
  }

  return alerts
}

// ============================================================
// Composable 实现
// ============================================================

/**
 * 角色工作台数据加载
 *
 * 使用方式：
 * ```ts
 * const { profile, statsCards, alerts, loading, refresh } = useRoleDashboard()
 * ```
 */
export function useRoleDashboard(): UseRoleDashboardReturn {
  const permissionStore = usePermissionStore()

  // ========== 角色配置 ==========
  const roles = computed<UserRole[]>(() => permissionStore.userInfo?.roles ?? [])
  const profile = ref<RoleProfile>(resolveRoleProfile(roles.value))
  const userName = ref<string>(permissionStore.userInfo?.username || '用户')
  const greeting = ref<string>(
    profile.value.greetingTemplate.replace('{name}', userName.value),
  )

  // ========== 数据状态 ==========
  const statsCards = ref<ResolvedStatCard[]>([])
  const alerts = ref<DashboardAlertItem[]>([])
  const loading = ref(false)
  const errorMessage = ref('')
  const isEmpty = ref(false)

  /**
   * 加载工作台数据
   * 1. 重新解析角色配置（用户角色可能在运行时变更）
   * 2. 根据 profile 需要的 dataSource 并行调用后端 API
   * 3. 解析返回值为统计卡片数据
   * 4. 构建预警信息
   */
  async function refresh(): Promise<void> {
    if (loading.value) return

    // 重新解析角色配置（处理登录后角色变更）
    const currentRoles = permissionStore.userInfo?.roles ?? []
    profile.value = resolveRoleProfile(currentRoles)
    userName.value = permissionStore.userInfo?.username || '用户'
    greeting.value = profile.value.greetingTemplate.replace('{name}', userName.value)

    loading.value = true
    errorMessage.value = ''

    try {
      const required = detectRequiredApis(profile.value)

      // 并行加载所有需要的数据源
      // 失败的 API 返回 null，不影响其他卡片展示
      const [todaySales, inventory, member, finance] = await Promise.all([
        required.needTodaySales
          ? dashboardApi.getTodaySales().catch(err => {
              console.error('[useRoleDashboard] 加载今日销售失败:', err)
              return null
            })
          : Promise.resolve(null),
        required.needInventory
          ? dashboardApi.getInventoryAlerts().catch(err => {
              console.error('[useRoleDashboard] 加载库存预警失败:', err)
              return null
            })
          : Promise.resolve(null),
        required.needMember
          ? dashboardApi.getMemberGrowth(30).catch(err => {
              console.error('[useRoleDashboard] 加载会员增长失败:', err)
              return null
            })
          : Promise.resolve(null),
        required.needFinance
          ? dashboardApi.getFinanceSummary().catch(err => {
              console.error('[useRoleDashboard] 加载财务概况失败:', err)
              return null
            })
          : Promise.resolve(null),
      ])

      // 解析统计卡片值
      statsCards.value = profile.value.statsCards.map(cardConfig => {
        const rawValue = resolveStatValue(
          cardConfig.dataSource,
          todaySales,
          inventory,
          member,
          finance,
        )
        return {
          ...cardConfig,
          displayValue: formatStatDisplay(rawValue, cardConfig.format),
          // 趋势：仅 todaySales.yesterdayGrowth 真实有值，其他卡片无趋势
          trend: cardConfig.dataSource === 'todaySales.yesterdayGrowth' ? rawValue : 0,
          showTodoBadge: !!cardConfig.isTodo,
        }
      })

      // 构建预警信息（库存类角色组才展示）
      const shouldShowAlerts = ['admin_owner', 'ops', 'store'].includes(profile.value.key)
      alerts.value = shouldShowAlerts ? buildAlerts(profile.value, inventory) : []

      // 判断空数据状态（所有卡片值都为 0 且无预警）
      const allZero = statsCards.value.every(c => {
        const num = Number(c.displayValue.replace(/[^0-9.-]/g, ''))
        return num === 0
      })
      isEmpty.value = allZero && alerts.value.length === 0
    } catch (error) {
      console.error('[useRoleDashboard] 加载工作台数据失败:', error)
      errorMessage.value = error instanceof Error ? error.message : '加载工作台数据失败'
      // 失败时保留空卡片，避免显示旧数据
      statsCards.value = profile.value.statsCards.map(cardConfig => ({
        ...cardConfig,
        displayValue: '--',
        trend: 0,
        showTodoBadge: !!cardConfig.isTodo,
      }))
      alerts.value = []
      isEmpty.value = true
    } finally {
      loading.value = false
    }
  }

  return {
    profile,
    userName,
    greeting,
    statsCards,
    alerts,
    loading,
    errorMessage,
    isEmpty,
    refresh,
  }
}
