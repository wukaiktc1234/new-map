/**
 * 库存日志API
 * 对应后端: /v1/inventory/logs
 */
import { get, post } from '@/api/request'
import { inventoryLogConverter } from './converters'
import type { InventoryLogInfo, InventoryLogQueryForm } from '@/types/warehouse-log'

interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

// ============================================================
// 后端类型定义
// ============================================================

/** 库存日志后端类型（不含 operationTypeName，由 converter 生成）
 * 注意：后端 InventoryLog 实体当前使用 productId 字段，未返回 productName/warehouseName，
 * 前端统一转换为 materialId/materialName/warehouseName。
 */
interface InventoryLogBackend {
  id: string
  productId: string
  productName?: string
  warehouseId: string
  warehouseName?: string
  operationType: string
  beforeStock: number
  afterStock: number
  changeAmount: number
  operatorId: string
  operatorName: string
  remark: string
  createdAt: string
}

// ============================================================
// 数据转换函数
// ============================================================

/** 后端日志 → 前端日志（补充 operationTypeName） */
function toFrontendLog(backend: InventoryLogBackend): InventoryLogInfo {
  return {
    id: backend.id,
    materialId: backend.productId,
    materialName: backend.productName ?? '',
    warehouseId: backend.warehouseId,
    warehouseName: backend.warehouseName ?? '',
    operationType: backend.operationType,
    operationTypeName: inventoryLogConverter.toOperationTypeLabel(backend.operationType),
    beforeStock: backend.beforeStock,
    afterStock: backend.afterStock,
    changeAmount: backend.changeAmount,
    operatorId: backend.operatorId,
    operatorName: backend.operatorName,
    remark: backend.remark,
    createdAt: backend.createdAt,
  }
}

/** 前端查询参数 → 后端参数（size→pageSize） */
function toBackendLogQuery(params?: InventoryLogQueryForm): Record<string, unknown> {
  const query: Record<string, unknown> = {}
  if (!params) return query
  if (params.materialId) query.productId = params.materialId
  if (params.warehouseId) query.warehouseId = params.warehouseId
  if (params.operatorId) query.operatorId = params.operatorId
  if (params.operationType) query.operationType = params.operationType
  if (params.startTime) query.startTime = params.startTime
  if (params.endTime) query.endTime = params.endTime
  if (params.page) query.page = params.page
  if (params.size !== undefined) query.pageSize = params.size
  return query
}

// ============================================================
// API 定义
// ============================================================

export const inventoryLogApi = {
  /** 分页查询库存日志 */
  async getPage(params?: InventoryLogQueryForm): Promise<PageResult<InventoryLogInfo>> {
    const res = await get<PageResult<InventoryLogBackend>>('/v1/inventory/logs/page', toBackendLogQuery(params))
    return {
      records: (res?.records || []).map(toFrontendLog),
      total: res?.total || 0,
      current: res?.current || params?.page || 1,
      size: res?.size || params?.size || 10,
      pages: res?.pages || 0,
    }
  },

  /** 获取日志详情 */
  async getById(id: string): Promise<InventoryLogInfo> {
    const res = await get<InventoryLogBackend>(`/v1/inventory/logs/${id}`)
    return toFrontendLog(res)
  },

  /** 导出库存日志 */
  async export(params?: Record<string, unknown>): Promise<void> {
    await post<void>('/v1/inventory/logs/export', params)
  },
}
