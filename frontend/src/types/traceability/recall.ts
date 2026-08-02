/**
 * 召回管理类型定义
 * 对应后端 RecallController（/v1/recalls）
 */

/** 召回分析请求 */
export interface RecallAnalyzeRequest {
  /** 批次号 */
  batchNo?: string
  /** 供应商ID */
  supplierId?: number
  /** 物料/菜品名称 */
  targetName?: string
}

/** 批量召回请求 */
export interface BatchRecallRequest {
  /** 追溯码ID列表 */
  traceCodeIds: number[]
  /** 召回原因 */
  reason: string
  /** 操作人ID */
  operatorId: number
  /** 操作人姓名 */
  operatorName: string
}

/** 批量召回结果 */
export interface BatchRecallResult {
  /** 总数 */
  totalCount: number
  /** 成功数 */
  successCount: number
  /** 失败数 */
  failCount: number
}

/** 召回统计 VO */
export interface RecallStatisticsVO {
  /** 今日召回数 */
  totalRecalledToday: number
  /** 待处理召回数 */
  pendingRecall: number
  /** 高风险项数 */
  highRiskItems: number
}

/** 受影响追溯码（前端聚合视图，对齐后端 AffectedTraceCodeVO） */
export interface AffectedTraceCode {
  /** 追溯码ID */
  traceCodeId?: number
  /** 追溯码 */
  traceCode?: string
  /** 物料/菜品名称 */
  targetName?: string
  /** 批次号（来自 targetInfo.batchNo，需视图填充） */
  batchNumber?: string
  /** 供应商名称（来自 targetInfo.supplierName，需视图填充） */
  supplierName?: string
  /** 状态：后端返回 Integer（1正常/2即将过期/3已过期/4已召回/5已消费） */
  status?: number
  /** 状态名称 */
  statusName?: string
  /** 风险等级（1低/2中/3高） */
  riskLevel?: number
  /** 生成时间 */
  createTime?: string
}

/** 受影响订单（前端聚合视图，对齐后端 AffectedOrderVO） */
export interface AffectedOrder {
  /** 订单ID */
  orderId?: number
  /** 订单号 */
  orderNumber?: string
  /** 下单时间 */
  orderTime?: string
  /** 客户信息（脱敏） */
  customerInfo?: string
  /** 涉及份数 */
  quantity?: number
}

/** 受影响菜品（对齐后端 AffectedDishVO） */
export interface AffectedDish {
  /** 菜品ID */
  dishId?: number
  /** 菜品名称 */
  dishName?: string
  /** 涉及数量 */
  quantity?: number
}

/** 问题目标信息（对齐后端 RecallQueryResultVO.TargetInfo） */
export interface RecallTargetInfo {
  /** 目标ID */
  targetId?: number
  /** 目标名称 */
  targetName?: string
  /** 批次号 */
  batchNo?: string
  /** 供应商名称 */
  supplierName?: string
  /** 问题类型描述 */
  problemDescription?: string
}

/** 召回统计汇总（对齐后端 RecallQueryResultVO.RecallSummary） */
export interface RecallSummary {
  /** 受影响追溯码总数 */
  totalTraceCodes?: number
  /** 受影响菜品种类数 */
  totalDishTypes?: number
  /** 受影响订单总数 */
  totalOrders?: number
  /** 已售出数量 */
  soldQuantity?: number
  /** 库存中数量 */
  stockQuantity?: number
}

/** 召回查询结果 VO（对齐后端 RecallQueryResultVO 实际返回字段） */
export interface RecallQueryResultVO {
  /** 问题目标信息 */
  targetInfo?: RecallTargetInfo
  /** 受影响追溯码列表 */
  affectedTraceCodes?: AffectedTraceCode[]
  /** 受影响菜品列表 */
  affectedDishes?: AffectedDish[]
  /** 受影响订单列表 */
  affectedOrders?: AffectedOrder[]
  /** 统计汇总 */
  summary?: RecallSummary
}

/** 召回记录（前端展示类型） */
export interface RecallRecord {
  /** 记录ID */
  id?: number
  /** 召回批次号 */
  batchNo?: string
  /** 召回原因 */
  reason?: string
  /** 操作人姓名 */
  operatorName?: string
  /** 操作时间 */
  operateTime?: string
  /** 受影响追溯码数量 */
  affectedCount?: number
  /** 受影响订单数量 */
  affectedOrderCount?: number
  /** 风险等级 */
  riskLevel?: number
  /** 召回状态 */
  status?: 'pending' | 'processing' | 'completed' | 'cancelled'
}

/** 风险等级映射 */
export const RiskLevelMap: Record<number, { label: string; type: string }> = {
  1: { label: '低风险', type: 'success' },
  2: { label: '中风险', type: 'warning' },
  3: { label: '高风险', type: 'danger' },
}

/** 召回状态映射 */
export const RecallStatusMap: Record<string, { label: string; type: string }> = {
  pending: { label: '待处理', type: 'info' },
  processing: { label: '处理中', type: 'warning' },
  completed: { label: '已完成', type: 'success' },
  cancelled: { label: '已取消', type: 'default' },
}
