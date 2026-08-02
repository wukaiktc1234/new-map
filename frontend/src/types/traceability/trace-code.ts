/**
 * 追溯码核心类型定义
 * 迁移自 types/trace-code.ts，并补充 VO/DTO 类型
 * 对应后端 TraceCodeController（/v1/trace-codes）
 */

/** 追溯码状态 */
export type TraceCodeStatus = 'active' | 'recalled' | 'expired'

/** 追溯码（基础类型，保留兼容） */
export interface TraceCode {
  id?: string
  code: string
  productId: number
  productName: string
  batchNumber?: string
  sourceType?: string
  sourceId?: string
  status: string
  warehouseId?: number
  warehouseName?: string
  operator?: string
  operatorId?: number
  location?: string
  purpose?: string
  reason?: string
  createdAt?: string
  updatedAt?: string
}

/** 追溯码操作日志 */
export interface TraceCodeLog {
  id?: string
  traceCodeId: string
  traceCode: string
  operationType: string
  operator?: string
  operatorId?: number
  location?: string
  purpose?: string
  reason?: string
  remark?: string
  createdAt?: string
}

/** 追溯码生成请求（保留兼容） */
export interface TraceCodeGenerateRequest {
  productId: number
  productName: string
  quantity: number
  sourceType: string
  sourceId: string
  batchNumber?: string
  warehouseId?: number
  warehouseName?: string
}

/** 扫码出库请求（保留兼容） */
export interface TraceCodeScanRequest {
  code: string
  operator: string
  operatorId: number
  location: string
  purpose: string
}

/** 未拆封退回请求（保留兼容） */
export interface TraceCodeReturnRequest {
  code: string
  operator: string
  operatorId: number
  location: string
  reason: string
}

// ============================================================
// 补充类型（对应后端 DTO/VO）
// ============================================================

/** 追溯码创建 DTO（对应后端 TraceCodeCreateDTO） */
export interface TraceCodeCreateDTO {
  productId: number
  productName: string
  batchNumber?: string
  sourceType: string
  sourceId: string
  warehouseId?: number
  warehouseName?: string
  quantity?: number
}

/** 追溯码查询 DTO（对应后端 TraceCodeQueryDTO） */
export interface TraceCodeQueryDTO {
  /** 当前页码 */
  page?: number
  /** 每页条数 */
  size?: number
  /** 追溯码 */
  traceCode?: string
  /** 批次号 */
  batchNumber?: string
  /** 供应商ID */
  supplierId?: number
  /** 状态 */
  status?: string
  /** 起始日期 */
  startDate?: string
  /** 结束日期 */
  endDate?: string
}

/** 追溯链节点 VO */
export interface TraceChainNodeVO {
  /** 节点ID */
  id?: number
  /** 追溯码ID */
  traceCodeId: number
  /** 节点类型（1原料/2入库/3领料/4投料/5成品/6出餐/7销售） */
  nodeType: number
  /** 节点名称 */
  nodeName: string
  /** 位置 */
  location: string
  /** 操作人ID */
  operatorId?: number
  /** 操作人姓名 */
  operatorName?: string
  /** 操作时间 */
  operateTime?: string
  /** 数量 */
  quantity?: number
  /** 详情JSON */
  detailJson?: string
}

/** 追溯码 VO（对应后端 TraceCodeVO） */
export interface TraceCodeVO {
  /** 主键ID */
  id: number
  /** 追溯码 */
  traceCode: string
  /** 产品ID */
  productId: number
  /** 产品名称 */
  productName: string
  /** 批次号 */
  batchNumber?: string
  /** 供应商ID */
  supplierId?: number
  /** 供应商名称 */
  supplierName?: string
  /** 状态 */
  status: string
  /** 风险等级（1低/2中/3高） */
  riskLevel?: number
  /** 追溯链节点列表 */
  chainNodes?: TraceChainNodeVO[]
  /** 扫码次数 */
  scanCount?: number
  /** 创建时间 */
  createdAt?: string
  /** 更新时间 */
  updatedAt?: string
}

/** 追溯码状态映射 */
export const TraceCodeStatusMap: Record<TraceCodeStatus, { label: string; type: string }> = {
  active: { label: '有效', type: 'success' },
  recalled: { label: '已召回', type: 'danger' },
  expired: { label: '已过期', type: 'info' },
}

/** 追溯链节点类型映射 */
export const ChainNodeTypeMap: Record<number, { label: string; type: string }> = {
  1: { label: '原料', type: 'primary' },
  2: { label: '入库', type: 'success' },
  3: { label: '领料', type: 'warning' },
  4: { label: '投料', type: 'warning' },
  5: { label: '成品', type: 'success' },
  6: { label: '出餐', type: 'info' },
  7: { label: '销售', type: 'info' },
}
