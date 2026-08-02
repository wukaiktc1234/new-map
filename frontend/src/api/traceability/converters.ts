/**
 * 食品追溯模块数据转换器
 *
 * 职责：
 * - 原料追溯码状态映射（后端字符串 → StatusTag status）
 * - 食品追溯码制作状态/状态映射
 * - 追溯码风险等级映射
 * - 金额转换（分 ↔ 元）
 * - 日期格式转换（ISO 8601 → YYYY-MM-DD / YYYY-MM-DD HH:mm:ss）
 * - 剩余天数计算（前端）
 *
 * 所有转换在 API 边界完成，组件中不直接做映射。
 */
import type {
  MaterialTraceCodeBackend,
  MaterialTraceCodeDisplay,
  MaterialTraceCodeStatus,
  FoodTraceCodeBackend,
  FoodTraceCodeDisplay,
  FoodMakeStatus,
  FoodTraceCodeStatus,
  AlertLevel,
  ExpiryHandlingStatus,
  AbnormalLevel,
  QualityHandlingStatus,
} from '@/types/traceability'
import { fenToYuan as utilsFenToYuan, yuanToFen } from '@/utils/money'

// ============================================================
// 状态映射常量
// ============================================================

/** 原料追溯码状态 → StatusTag status 映射 */
const materialStatusToTagStatus: Record<MaterialTraceCodeStatus, string> = {
  pending: 'info',
  in_stock: 'success',
  picked: 'warning',
  used: 'info',
  expired: 'error',
  returned: 'default',
}

/** 食品制作状态 → StatusTag status 映射 */
const foodMakeStatusToTagStatus: Record<FoodMakeStatus, string> = {
  pending: 'info',
  making: 'warning',
  completed: 'success',
  served: 'primary',
}

/** 食品追溯码状态 → StatusTag status 映射 */
const foodStatusToTagStatus: Record<FoodTraceCodeStatus, string> = {
  created: 'info',
  printed: 'warning',
  served: 'success',
  expired: 'error',
}

/** 风险等级 → StatusTag status 映射 */
const riskLevelToTagStatus: Record<number, string> = {
  1: 'success',
  2: 'warning',
  3: 'error',
}

/** 临期预警等级 → StatusTag status 映射 */
const alertLevelToTagStatus: Record<AlertLevel, string> = {
  RED: 'error',
  YELLOW: 'warning',
  GREEN: 'success',
}

/** 临期处理状态 → StatusTag status 映射 */
const expiryHandlingStatusToTagStatus: Record<ExpiryHandlingStatus, string> = {
  PENDING: 'warning',
  SCRAPPED: 'error',
  RETURNED: 'info',
  RESOLVED: 'success',
}

/** 质量异常等级 → StatusTag status 映射 */
const abnormalLevelToTagStatus: Record<AbnormalLevel, string> = {
  NORMAL: 'success',
  WARNING: 'warning',
  CRITICAL: 'error',
}

/** 质量处理状态 → StatusTag status 映射 */
const qualityHandlingStatusToTagStatus: Record<QualityHandlingStatus, string> = {
  PENDING: 'warning',
  PROCESSING: 'primary',
  RESOLVED: 'success',
  CLOSED: 'info',
}

// ============================================================
// 金额转换工具（统一委托给 utils/money）
// ============================================================

/**
 * 分转元（保留2位小数）
 * 特殊语义：fen 为 undefined/null 时返回 undefined（区分"无值"与"0 元"）
 * 内部委托给 utils/money.fenToYuan 处理实际转换
 */
function fenToYuan(fen?: number | null): string | undefined {
  if (fen === undefined || fen === null) return undefined
  return utilsFenToYuan(fen)
}

// yuanToFen 直接使用 utils/money 导出的实现

// ============================================================
// 日期格式转换工具
// ============================================================

/** 日期格式化为 YYYY-MM-DD */
function formatDate(dateStr?: string): string | undefined {
  if (!dateStr) return undefined
  return dateStr.split('T')[0].split(' ')[0]
}

/** 日期时间格式化为 YYYY-MM-DD HH:mm:ss */
function formatDateTime(dateStr?: string): string | undefined {
  if (!dateStr) return undefined
  // 处理 ISO 8601 格式（含T）
  const normalized = dateStr.replace('T', ' ')
  // 截取到秒
  return normalized.split('.')[0].slice(0, 19)
}

// ============================================================
// 剩余天数计算
// ============================================================

/** 计算距过期剩余天数（负数表示已过期） */
function calcRemainingDays(expiryDate?: string): number | undefined {
  if (!expiryDate) return undefined
  const expiry = new Date(expiryDate)
  if (isNaN(expiry.getTime())) return undefined
  const now = new Date()
  // 清除时间部分，只比较日期
  expiry.setHours(0, 0, 0, 0)
  now.setHours(0, 0, 0, 0)
  const diff = expiry.getTime() - now.getTime()
  return Math.ceil(diff / (1000 * 60 * 60 * 24))
}

// ============================================================
// 转换器主对象
// ============================================================

export const traceabilityDataConverter = {
  // ---------- 金额转换 ----------
  /** 分转元 */
  fenToYuan,
  /** 元转分 */
  yuanToFen,

  // ---------- 日期转换 ----------
  /** 格式化为日期（YYYY-MM-DD） */
  formatDate,
  /** 格式化为日期时间（YYYY-MM-DD HH:mm:ss） */
  formatDateTime,

  // ---------- 剩余天数 ----------
  /** 计算剩余天数 */
  calcRemainingDays,

  // ---------- 状态映射 ----------
  /** 原料追溯码状态 → StatusTag status */
  materialStatusToTagStatus,
  /** 食品制作状态 → StatusTag status */
  foodMakeStatusToTagStatus,
  /** 食品追溯码状态 → StatusTag status */
  foodStatusToTagStatus,
  /** 风险等级 → StatusTag status */
  riskLevelToTagStatus,
  /** 临期预警等级 → StatusTag status */
  alertLevelToTagStatus,
  /** 临期处理状态 → StatusTag status */
  expiryHandlingStatusToTagStatus,
  /** 质量异常等级 → StatusTag status */
  abnormalLevelToTagStatus,
  /** 质量处理状态 → StatusTag status */
  qualityHandlingStatusToTagStatus,

  // ---------- 对象转换 ----------

  /** 原料追溯码：后端 → 前端展示 */
  toMaterialDisplay(backend: MaterialTraceCodeBackend): MaterialTraceCodeDisplay {
    const status = backend.status as MaterialTraceCodeStatus
    return {
      ...backend,
      unitPriceYuan: fenToYuan(backend.unitPrice),
      totalPriceYuan: fenToYuan(backend.totalPrice),
      statusLabel: status,
      productionDateDisplay: formatDate(backend.productionDate),
      expiryDateDisplay: formatDate(backend.expiryDate),
      remainingDays: calcRemainingDays(backend.expiryDate),
    }
  },

  /** 原料追溯码列表：后端 → 前端展示 */
  toMaterialDisplayList(backendList: MaterialTraceCodeBackend[]): MaterialTraceCodeDisplay[] {
    return backendList.map((item) => this.toMaterialDisplay(item))
  },

  /** 食品追溯码：后端 → 前端展示 */
  toFoodDisplay(backend: FoodTraceCodeBackend): FoodTraceCodeDisplay {
    const makeStatus = backend.makeStatus as FoodMakeStatus
    const status = backend.status as FoodTraceCodeStatus
    return {
      ...backend,
      materialCostYuan: fenToYuan(backend.materialCost),
      laborCostYuan: fenToYuan(backend.laborCost),
      totalCostYuan: fenToYuan(backend.totalCost),
      makeStatusLabel: makeStatus,
      statusLabel: status,
    }
  },

  /** 食品追溯码列表：后端 → 前端展示 */
  toFoodDisplayList(backendList: FoodTraceCodeBackend[]): FoodTraceCodeDisplay[] {
    return backendList.map((item) => this.toFoodDisplay(item))
  },

  /** 订单成本：分 → 元（委托给 utils/money） */
  toOrderCostYuan(costFen: number): string {
    return utilsFenToYuan(costFen)
  },

  // ---------- 临期预警/质量 状态转换 ----------

  /** 临期预警等级 → StatusTag status */
  alertLevelToStatus(alertLevel: AlertLevel): string {
    return alertLevelToTagStatus[alertLevel] || 'info'
  },

  /** 临期处理状态 → StatusTag status */
  expiryHandlingStatusToStatus(status: ExpiryHandlingStatus): string {
    return expiryHandlingStatusToTagStatus[status] || 'info'
  },

  /** 质量异常等级 → StatusTag status */
  abnormalLevelToStatus(level: AbnormalLevel): string {
    return abnormalLevelToTagStatus[level] || 'info'
  },

  /** 质量处理状态 → StatusTag status */
  qualityHandlingStatusToStatus(status: QualityHandlingStatus): string {
    return qualityHandlingStatusToTagStatus[status] || 'info'
  },
}

export default traceabilityDataConverter
