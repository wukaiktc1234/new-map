/**
 * BOM 库存联动检查 API
 * 对应后端: /v1/product-center/bom-check
 *
 * 端点说明（与 BomCheckController 保持一致）：
 * - GET  /v1/product-center/bom-check/{dishId}          检查单个菜品
 * - POST /v1/product-center/bom-check/batch             批量检查
 * - POST /v1/product-center/bom-check/pre-order         下单前预检查
 * - GET  /v1/product-center/bom-check/warning-config    获取预警配置
 * - POST /v1/product-center/bom-check/warning-config    保存预警配置
 *
 * 使用项目封装的 get/post 函数（基于 request 实例），直接传 params/data 对象。
 */
import { get, post } from '@/api/request'

/** BOM 检查缺料项明细 */
export interface BomInsufficientItem {
  /** 物料ID（库存产品ID） */
  materialId: string | null
  /** 物料名称 */
  materialName: string | null
  /** 需求数量（已乘以制作份数） */
  requiredQty: number | null
  /** 可用库存数量 */
  availableQty: number
  /** 缺口数量 */
  shortageQty: number
  /** 缺口百分比（0-100） */
  shortagePercent: number
  /** 单位 */
  unit: string | null
  /** 紧急程度：low / medium / high */
  urgency: 'low' | 'medium' | 'high' | string
}

/** 单个菜品 BOM 检查结果 */
export interface BomCheckResult {
  dishId: string
  dishName: string
  canMake: boolean
  totalRequired: number
  insufficientItems: BomInsufficientItem[]
  alternativeDishes: unknown[]
  checkedAt: string
}

/** 批量检查请求项 */
export interface BatchCheckItem {
  dishId: string
  quantity: number
}

/** 批量检查结果 */
export interface BatchCheckResult {
  results: BomCheckResult[]
  totalChecked: number
  canMakeCount: number
  insufficientCount: number
  healthScore: number
}

/** 下单前预检查问题项 */
export interface PreOrderIssue {
  dishId: string
  dishName: string
  issue: string
  /** 严重级别：error / warn */
  severity: 'error' | 'warn' | string
  insufficientItems: BomInsufficientItem[]
}

/** 下单前预检查结果 */
export interface PreOrderCheckResult {
  ready: string[]
  issues: PreOrderIssue[]
}

/** BOM 预警配置 */
export interface BomWarningConfig {
  id: string
  enabled: boolean
  checkPoint: string
  autoSuggestAlternative: boolean
  lowStockThreshold: number
  warnBeforeOrder: boolean
}

/** 菜品可做份数预测-原料明细 */
export interface StockForecastItem {
  materialId: number | null
  materialName: string | null
  unit: string | null
  availableStock: number
  safetyStock: number
  recipeQty: number
  /** 实际每份用量（含损耗，自校准） */
  actualQtyPerServing: number
  /** 隐含损耗率（0.2=20%） */
  impliedLossRate: number
  maxServingsByThis: number
  isBottleneck: boolean
}

/** 菜品可做份数预测（方案D+） */
export interface StockForecastResult {
  dishId: number
  dishName: string
  canMake: boolean
  /** 可做份数（按实际每份用量，瓶颈原料） */
  maxServings: number
  /** 理论可做份数（按BOM用量，无损耗） */
  theoreticalServings: number
  /** 是否有足够销量数据校准 */
  hasSufficientData: boolean
  dataWindowDays: number
  avgDailySales: number
  daysToSellout: number
  suggestedRestock: number
  bottleneckMaterialName: string | null
  items: StockForecastItem[]
}

/** 原料消耗差异分析项（方案E） */
export interface VarianceItem {
  materialId: number
  materialName: string
  unit: string
  theoreticalConsumed: number
  actualConsumed: number
  varianceQty: number
  varianceRate: number
  soldServings: number
}

export const bomCheckApi = {
  /**
   * 检查单个菜品的 BOM 库存
   * @param dishId - 菜品ID
   * @param quantity - 制作数量（默认1）
   */
  checkDish(dishId: string | number, quantity = 1): Promise<BomCheckResult> {
    return get<BomCheckResult>(
      `/v1/product-center/bom-check/${dishId}`,
      { quantity },
    )
  },

  /**
   * 菜品可做份数预测（方案D+：实际每份用量自校准，含损耗/售罄时间/补货量）
   * @param dishId - 菜品ID
   * @param days - 数据窗口（天，默认7）
   */
  forecast(dishId: string | number, days = 7): Promise<StockForecastResult> {
    return get<StockForecastResult>(
      `/v1/product-center/bom-check/${dishId}/forecast`,
      { days },
    )
  },

  /**
   * 原料消耗差异分析（方案E：理论 vs 实际消耗，揪出超耗/浪费）
   * @param days - 数据窗口（天，默认7）
   * @param limit - 返回条数
   */
  varianceAnalysis(days = 7, limit = 20): Promise<VarianceItem[]> {
    return get<VarianceItem[]>(
      '/v1/product-center/bom-check/variance-analysis',
      { days, limit },
    )
  },

  /**
   * 批量检查多个菜品的 BOM 库存
   * @param items - 菜品ID与数量列表
   */
  batchCheck(items: BatchCheckItem[]): Promise<BatchCheckResult> {
    return post<BatchCheckResult>('/v1/product-center/bom-check/batch', { items })
  },

  /**
   * 下单前预检查（识别阻塞和警告级问题）
   * @param dishIds - 菜品ID列表
   */
  preOrderCheck(dishIds: string[]): Promise<PreOrderCheckResult> {
    return post<PreOrderCheckResult>('/v1/product-center/bom-check/pre-order', { dishIds })
  },

  /**
   * 获取 BOM 预警配置
   */
  getWarningConfig(): Promise<BomWarningConfig> {
    return get<BomWarningConfig>('/v1/product-center/bom-check/warning-config')
  },

  /**
   * 保存 BOM 预警配置
   * @param config - 预警配置
   */
  saveWarningConfig(config: BomWarningConfig): Promise<BomWarningConfig> {
    return post<BomWarningConfig>('/v1/product-center/bom-check/warning-config', config)
  },
}
