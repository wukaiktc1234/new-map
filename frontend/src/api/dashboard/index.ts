/**
 * 经营看板 API
 * 对应后端: DashboardController (/v1/dashboard)
 *
 * 后端端点：
 * - GET /v1/dashboard/overview/{dashboardType}  看板概览（1总览/2销售/3库存/4财务/5会员）
 * - GET /v1/dashboard/today-sales               今日销售概览
 * - GET /v1/dashboard/inventory-alerts          库存预警信息
 * - GET /v1/dashboard/member-growth?days=30    会员增长趋势
 * - GET /v1/dashboard/finance-summary           财务概况
 * - POST /v1/dashboard/config                  保存看板配置
 * - GET /v1/dashboard/config/user/{userId}/type/{dashboardType}  获取用户看板配置
 *
 * 数据来源说明：
 * - 后端 DashboardServiceImpl 已实现真实数据查询（todayOrderCount/warningCount/totalMembers）
 * - 部分字段后端标注 TODO（todayAmount/yesterdayGrowth/monthlyRevenue 等），前端如实展示 0
 * - 前端不再 Mock 任何看板数据，缺失字段以空状态展示，避免误导经营决策
 */
import { get, post } from '@/api/request'

// ============================================================
// 看板类型枚举（与后端 DashboardController 对齐）
// ============================================================

export enum DashboardType {
  /** 总览（今日销售 + 库存预警） */
  OVERVIEW = 1,
  /** 销售看板 */
  SALES = 2,
  /** 库存看板 */
  INVENTORY = 3,
  /** 财务看板 */
  FINANCE = 4,
  /** 会员看板 */
  MEMBER = 5,
}

// ============================================================
// 后端响应类型（Map<String, Object> 的约定结构）
// ============================================================

/**
 * 今日销售概览
 * 数据源: sales_order 表（status=completed）
 * - todayOrderCount: 今日已完成订单数（真实）
 * - todayAmount: 今日销售额（后端 TODO，暂为 0）
 * - yesterdayGrowth: 环比增长（后端 TODO，暂为 0）
 */
export interface TodaySalesOverview {
  todayOrderCount: number
  todayAmount: number
  yesterdayGrowth: number
}

/**
 * 库存预警信息
 * 数据源: inventory 表（warning_status=1）
 * - warningCount: 预警数（真实）
 * - outOfStockCount: 缺货数（后端 TODO，暂为 0）
 * - overstockCount: 积压数（后端 TODO，暂为 0）
 */
export interface InventoryAlerts {
  warningCount: number
  outOfStockCount: number
  overstockCount: number
}

/**
 * 会员增长趋势
 * 数据源: member 表
 * - totalMembers: 会员总数（真实）
 * - newMembers: 最近 N 天新增会员数（真实）
 * - growthRate: 增长率（真实，百分比）
 */
export interface MemberGrowthTrend {
  totalMembers: number
  newMembers: number
  growthRate: number
}

/**
 * 财务概况
 * 数据源: 后端 TODO（暂全部为 0）
 * - monthlyRevenue: 月度营收
 * - monthlyExpense: 月度支出
 * - monthlyProfit: 月度利润
 * - profitMargin: 利润率（百分比）
 */
export interface FinanceSummary {
  monthlyRevenue: number
  monthlyExpense: number
  monthlyProfit: number
  profitMargin: number
}

/**
 * 看板概览响应（按 dashboardType 返回不同结构）
 * - type=1: TodaySalesOverview + { inventoryAlerts: InventoryAlerts }
 * - type=2: TodaySalesOverview
 * - type=3: InventoryAlerts
 * - type=4: { financeSummary: FinanceSummary }
 * - type=5: { memberGrowth: MemberGrowthTrend }
 */
export type DashboardOverview = Record<string, unknown>

// ============================================================
// 看板配置相关类型
// ============================================================

export interface DashboardConfigVO {
  configId: number
  userId: number
  dashboardType: DashboardType
  dashboardTypeName: string
  widgetLayout?: string
  refreshInterval?: number
  isDefault?: boolean
  createTime?: string
}

export interface DashboardConfigCreateDTO {
  userId: number
  dashboardType: DashboardType
  widgetLayout?: string
  refreshInterval?: number
  isDefault?: boolean
}

// ============================================================
// 防御性适配器：后端 Map<String, Object> → 强类型前端接口
// 后端返回字段可能缺失或类型不一致，统一在此处兜底
// ============================================================

function toNumber(value: unknown): number {
  if (value == null) return 0
  if (typeof value === 'number') return Number.isFinite(value) ? value : 0
  const n = Number(value)
  return Number.isFinite(n) ? n : 0
}

function adaptTodaySales(raw: Record<string, unknown> | null | undefined): TodaySalesOverview {
  return {
    todayOrderCount: toNumber(raw?.todayOrderCount ?? raw?.todayOrderCnt),
    todayAmount: toNumber(raw?.todayAmount),
    yesterdayGrowth: toNumber(raw?.yesterdayGrowth),
  }
}

function adaptInventoryAlerts(raw: Record<string, unknown> | null | undefined): InventoryAlerts {
  // overview 端点 type=1 会嵌套返回 inventoryAlerts
  const nested = raw?.inventoryAlerts
  const source = (nested && typeof nested === 'object' ? nested : raw) as Record<string, unknown> | null
  return {
    warningCount: toNumber(source?.warningCount),
    outOfStockCount: toNumber(source?.outOfStockCount),
    overstockCount: toNumber(source?.overstockCount),
  }
}

function adaptMemberGrowth(raw: Record<string, unknown> | null | undefined): MemberGrowthTrend {
  // overview 端点 type=5 会嵌套返回 memberGrowth
  const nested = raw?.memberGrowth
  const source = (nested && typeof nested === 'object' ? nested : raw) as Record<string, unknown> | null
  return {
    totalMembers: toNumber(source?.totalMembers),
    newMembers: toNumber(source?.newMembers),
    growthRate: toNumber(source?.growthRate),
  }
}

function adaptFinanceSummary(raw: Record<string, unknown> | null | undefined): FinanceSummary {
  // overview 端点 type=4 会嵌套返回 financeSummary
  const nested = raw?.financeSummary
  const source = (nested && typeof nested === 'object' ? nested : raw) as Record<string, unknown> | null
  return {
    monthlyRevenue: toNumber(source?.monthlyRevenue),
    monthlyExpense: toNumber(source?.monthlyExpense),
    monthlyProfit: toNumber(source?.monthlyProfit),
    profitMargin: toNumber(source?.profitMargin),
  }
}

// ============================================================
// API 实现
// ============================================================

export const dashboardApi = {
  /**
   * 获取今日销售概览
   * GET /v1/dashboard/today-sales
   * 真实数据源：sales_order 表（status=completed，今日订单）
   */
  async getTodaySales(): Promise<TodaySalesOverview> {
    const res = await get<Record<string, unknown>>('/v1/dashboard/today-sales')
    return adaptTodaySales(res)
  },

  /**
   * 获取库存预警信息
   * GET /v1/dashboard/inventory-alerts
   * 真实数据源：inventory 表（warning_status=1）
   */
  async getInventoryAlerts(): Promise<InventoryAlerts> {
    const res = await get<Record<string, unknown>>('/v1/dashboard/inventory-alerts')
    return adaptInventoryAlerts(res)
  },

  /**
   * 获取会员增长趋势
   * GET /v1/dashboard/member-growth?days=30
   * 真实数据源：member 表
   * @param days 最近天数（默认 30）
   */
  async getMemberGrowth(days = 30): Promise<MemberGrowthTrend> {
    const res = await get<Record<string, unknown>>('/v1/dashboard/member-growth', { days })
    return adaptMemberGrowth(res)
  },

  /**
   * 获取财务概况
   * GET /v1/dashboard/finance-summary
   * 注意：后端目前为 TODO，返回全 0，前端如实展示
   */
  async getFinanceSummary(): Promise<FinanceSummary> {
    const res = await get<Record<string, unknown>>('/v1/dashboard/finance-summary')
    return adaptFinanceSummary(res)
  },

  /**
   * 获取看板概览（按类型聚合多源数据）
   * GET /v1/dashboard/overview/{dashboardType}
   * @param dashboardType 1总览 2销售 3库存 4财务 5会员
   */
  async getOverview(dashboardType: DashboardType): Promise<DashboardOverview> {
    return await get<DashboardOverview>(`/v1/dashboard/overview/${dashboardType}`)
  },

  /**
   * 保存看板配置
   * POST /v1/dashboard/config
   */
  async saveConfig(dto: DashboardConfigCreateDTO): Promise<number> {
    return await post<number>('/v1/dashboard/config', dto)
  },

  /**
   * 获取用户看板配置
   * GET /v1/dashboard/config/user/{userId}/type/{dashboardType}
   */
  async getUserConfig(userId: number, dashboardType: DashboardType): Promise<DashboardConfigVO | null> {
    return await get<DashboardConfigVO | null>(
      `/v1/dashboard/config/user/${userId}/type/${dashboardType}`
    )
  },
}

export default dashboardApi
