/**
 * 经营分析决策看板 API
 * 对应后端: DecisionBoardController (/v1/operations/decision-board)
 *
 * 后端返回 List<Map<String, Object>> / Map<String, Object>，结构灵活，
 * 前端按约定字段读取并做防御性处理。
 *
 * 后端端点：
 * - GET /v1/operations/decision-board/health-scores  门店健康度评分
 * - GET /v1/operations/decision-board/store-ranking   门店排名
 * - GET /v1/operations/decision-board/category-sales  品类销售分析
 * - GET /v1/operations/decision-board/insights        AI 洞察建议
 * - GET /v1/operations/decision-board/revenue-trend   营收-成本-利润趋势
 */
import { get } from '@/api/request'

// ============================================================
// 类型定义（与 DecisionBoard.vue 内部类型保持一致）
// ============================================================

/** 健康评分项 */
export interface HealthScoreItem {
  label: string
  score: number
  icon: string
}

/** 门店排名项 */
export interface StoreRankItem {
  rank: number
  storeName: string
  revenue: number
  cost: number
  profit: number
  profitMargin: number
  growth: number
  healthScore: number
}

/** 品类销售项 */
export interface CategoryItem {
  name: string
  value: number
  growth: number
}

/** 严重程度 */
export type InsightSeverity = 'high' | 'medium' | 'low'

/** 智能洞察项 */
export interface InsightItem {
  id: number
  icon: string
  title: string
  description: string
  severity: InsightSeverity
}

/** 时间维度（趋势） */
export type TrendPeriod = 'week' | 'month' | 'quarter'

/** 营收-成本-利润趋势数据 */
export interface RevenueTrendData {
  dates: string[]
  revenue: number[]
  cost: number[]
  profit: number[]
}

// ============================================================
// 后端响应接口（Map<String, Object> 的约定结构）
// ============================================================

interface HealthScoreBackend {
  label?: string
  score?: number
  icon?: string
}

interface StoreRankBackend {
  rank?: number
  storeName?: string
  name?: string
  revenue?: number
  cost?: number
  profit?: number
  profitMargin?: number
  growth?: number
  healthScore?: number
}

interface CategoryBackend {
  name?: string
  value?: number
  growth?: number
}

interface InsightBackend {
  id?: number | string
  icon?: string
  title?: string
  description?: string
  severity?: string
}

interface RevenueTrendBackend {
  dates?: string[]
  revenue?: number[]
  cost?: number[]
  profit?: number[]
}

// ============================================================
// 字段适配器
// ============================================================

/** 后端健康评分 → 前端 HealthScoreItem */
function adaptHealthScore(raw: HealthScoreBackend): HealthScoreItem {
  return {
    label: raw.label || '',
    score: Number(raw.score ?? 0),
    icon: raw.icon || 'DataAnalysis',
  }
}

/** 后端门店排名 → 前端 StoreRankItem（兼容字段名差异） */
function adaptStoreRank(raw: StoreRankBackend, index: number): StoreRankItem {
  return {
    rank: Number(raw.rank ?? index + 1),
    storeName: raw.storeName || raw.name || '未知门店',
    revenue: Number(raw.revenue ?? 0),
    cost: Number(raw.cost ?? 0),
    profit: Number(raw.profit ?? 0),
    profitMargin: Number(raw.profitMargin ?? 0),
    growth: Number(raw.growth ?? 0),
    healthScore: Number(raw.healthScore ?? 0),
  }
}

/** 后端品类销售 → 前端 CategoryItem */
function adaptCategory(raw: CategoryBackend): CategoryItem {
  return {
    name: raw.name || '',
    value: Number(raw.value ?? 0),
    growth: Number(raw.growth ?? 0),
  }
}

/** 后端洞察 → 前端 InsightItem */
function adaptInsight(raw: InsightBackend): InsightItem {
  const severity = raw.severity as InsightSeverity
  return {
    id: Number(raw.id ?? 0),
    icon: raw.icon || 'DataAnalysis',
    title: raw.title || '',
    description: raw.description || '',
    severity: ['high', 'medium', 'low'].includes(severity) ? severity : 'medium',
  }
}

/** 后端趋势 → 前端 RevenueTrendData */
function adaptRevenueTrend(raw: RevenueTrendBackend | null | undefined): RevenueTrendData {
  return {
    dates: Array.isArray(raw?.dates) ? raw.dates.map(String) : [],
    revenue: Array.isArray(raw?.revenue) ? raw.revenue.map(Number) : [],
    cost: Array.isArray(raw?.cost) ? raw.cost.map(Number) : [],
    profit: Array.isArray(raw?.profit) ? raw.profit.map(Number) : [],
  }
}

// ============================================================
// API 实现
// ============================================================

export const decisionBoardApi = {
  /**
   * 获取门店健康度评分
   * GET /v1/operations/decision-board/health-scores
   */
  async getHealthScores(): Promise<HealthScoreItem[]> {
    const res = await get<HealthScoreBackend[]>('/v1/operations/decision-board/health-scores')
    return Array.isArray(res) ? res.map(adaptHealthScore) : []
  },

  /**
   * 获取门店排名
   * GET /v1/operations/decision-board/store-ranking
   */
  async getStoreRanking(): Promise<StoreRankItem[]> {
    const res = await get<StoreRankBackend[]>('/v1/operations/decision-board/store-ranking')
    return Array.isArray(res) ? res.map(adaptStoreRank) : []
  },

  /**
   * 获取品类销售分析
   * GET /v1/operations/decision-board/category-sales
   */
  async getCategorySales(): Promise<CategoryItem[]> {
    const res = await get<CategoryBackend[]>('/v1/operations/decision-board/category-sales')
    return Array.isArray(res) ? res.map(adaptCategory) : []
  },

  /**
   * 获取 AI 洞察建议
   * GET /v1/operations/decision-board/insights
   */
  async getInsights(): Promise<InsightItem[]> {
    const res = await get<InsightBackend[]>('/v1/operations/decision-board/insights')
    return Array.isArray(res) ? res.map(adaptInsight) : []
  },

  /**
   * 获取营收-成本-利润趋势数据
   * GET /v1/operations/decision-board/revenue-trend?period=week|month|quarter
   * @param period 时间维度：week（近7天）/ month（近4周）/ quarter（近6月）
   */
  async getRevenueTrend(period: TrendPeriod = 'week'): Promise<RevenueTrendData> {
    const res = await get<RevenueTrendBackend>('/v1/operations/decision-board/revenue-trend', { period })
    return adaptRevenueTrend(res)
  },
}

export default decisionBoardApi
