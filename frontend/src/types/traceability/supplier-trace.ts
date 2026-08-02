/**
 * 供应商追溯类型定义
 *
 * 对齐后端 SupplierTraceController (/v1/supplier-trace) 的 SupplierTraceVO。
 * 复用同模块的 InspectionRecord 和 RecallRecord 类型，避免 unknown 弱类型。
 */

import type { InspectionRecord } from './inspection'
import type { RecallRecord } from './recall'

/** 供应商批次（对齐后端 SupplierBatchVO） */
export interface SupplierBatch {
  /** 追溯码ID */
  traceCodeId: number
  /** 追溯码 */
  traceCode: string
  /** 物料ID */
  materialId?: string
  /** 物料名称 */
  materialName?: string
  /** 批次号 */
  batchNo?: string
  /** 入库时间 */
  inboundTime?: string
  /** 过期日期 */
  expiryDate?: string
  /** 状态 */
  status: string
  /** 状态中文名 */
  statusName: string
}

/** 供应商追溯汇总（对齐后端 SupplierTraceVO） */
export interface SupplierTrace {
  /** 供应商ID */
  supplierId: number
  /** 供应商名称 */
  supplierName: string
  /** 总批次数 */
  totalBatches: number
  /** 总检验次数 */
  totalInspections: number
  /** 合格率（百分比） */
  qualificationRate: number
  /** 总召回次数 */
  totalRecalls: number
  /** 批次列表 */
  batches: SupplierBatch[]
  /** 检验记录列表 */
  inspections: InspectionRecord[]
  /** 召回记录列表 */
  recalls: RecallRecord[]
}

/** 供应商追溯查询参数 */
export interface SupplierTraceQuery {
  /** 当前页码 */
  page?: number
  /** 每页条数 */
  size?: number
  /** 供应商ID（必填） */
  supplierId: number
  /** 物料名称 */
  materialName?: string
  /** 批次号 */
  batchNo?: string
  /** 起始日期 */
  startDate?: string
  /** 结束日期 */
  endDate?: string
}
