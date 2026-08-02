/**
 * 库存预警API
 * 对应后端: /v1/inventory/warnings + /v1/inventory/warning-rules
 */
import { get, post, put, del } from '@/api/request'
import { inventoryWarningConverter } from './converters'
// 金额转换统一委托给 utils/money（fenToYuan 返回 string，保留 2 位小数）
import { fenToYuan } from '@/utils/money'
import type {
  InventoryWarningRuleInfo,
  InventoryWarningRecordInfo,
  WarningRuleQueryForm,
  WarningRuleCreateForm,
  WarningRecordQueryForm,
  WarningHandleForm,
  PurchaseSuggestionItem,
  WarningType,
  NotifyMethod,
} from '@/types/warehouse-warning'

interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

// ============================================================
// 后端类型定义（后端使用数值编码，前端使用语义化字符串）
// ============================================================

/** 预警记录后端类型 */
interface InventoryWarningRecordBackend {
  warningId: string
  warningType: number
  materialId: string
  materialName: string
  warehouseId: string
  warehouseName: string
  currentStock: number
  threshold: number
  unit: string
  status: number
  handler: string
  handlerName: string
  handleTime: string
  handleRemark: string
  createTime: string
  updateTime: string
}

/** 预警规则后端类型 */
interface InventoryWarningRuleBackend {
  ruleId: string
  ruleName: string
  warningType: number
  materialId: string
  materialName: string
  warehouseId: string
  warehouseName: string
  threshold: number
  unit: string
  enabled: number
  notifyMethod: number
  notifyUsers: string
  remark: string
  createTime: string
  updateTime: string
}

/** 采购建议后端类型（estimatedCost 为分） */
interface PurchaseSuggestionBackend {
  materialId: string
  materialName: string
  specification: string
  unit: string
  currentStock: number
  safetyStock: number
  suggestedQuantity: number
  estimatedCost: number
  supplierName: string
  modelResults?: PurchaseSuggestionItem['modelResults']
}

// ============================================================
// 数据转换函数
// ============================================================

/** 后端预警记录 → 前端预警记录 */
function toFrontendWarningRecord(backend: InventoryWarningRecordBackend): InventoryWarningRecordInfo {
  const warningType: WarningType = inventoryWarningConverter.toFrontendType(backend.warningType)
  return {
    warningId: backend.warningId,
    warningType,
    warningTypeName: inventoryWarningConverter.toTypeLabel(warningType),
    materialId: backend.materialId,
    materialName: backend.materialName,
    warehouseId: backend.warehouseId,
    warehouseName: backend.warehouseName,
    currentStock: backend.currentStock,
    threshold: backend.threshold,
    unit: backend.unit,
    status: backend.status,
    statusName: inventoryWarningConverter.toStatusLabel(backend.status),
    handler: backend.handler,
    handlerName: backend.handlerName,
    handleTime: backend.handleTime,
    handleRemark: backend.handleRemark,
    createTime: backend.createTime,
    updateTime: backend.updateTime,
  }
}

/** 后端预警规则 → 前端预警规则 */
function toFrontendWarningRule(backend: InventoryWarningRuleBackend): InventoryWarningRuleInfo {
  const warningType: WarningType = inventoryWarningConverter.toFrontendType(backend.warningType)
  const notifyMethod: NotifyMethod = inventoryWarningConverter.toFrontendNotifyMethod(backend.notifyMethod)
  return {
    ruleId: backend.ruleId,
    ruleName: backend.ruleName,
    warningType,
    warningTypeName: inventoryWarningConverter.toTypeLabel(warningType),
    materialId: backend.materialId,
    materialName: backend.materialName,
    warehouseId: backend.warehouseId,
    warehouseName: backend.warehouseName,
    threshold: backend.threshold,
    unit: backend.unit,
    enabled: backend.enabled,
    notifyMethod,
    notifyMethodName: inventoryWarningConverter.toNotifyMethodLabel(notifyMethod),
    notifyUsers: backend.notifyUsers,
    remark: backend.remark,
    createTime: backend.createTime,
    updateTime: backend.updateTime,
  }
}

/** 后端采购建议 → 前端采购建议（estimatedCost: 分→元字符串） */
function toFrontendPurchaseSuggestion(backend: PurchaseSuggestionBackend): PurchaseSuggestionItem {
  return {
    materialId: backend.materialId,
    materialName: backend.materialName,
    specification: backend.specification,
    unit: backend.unit,
    currentStock: backend.currentStock,
    safetyStock: backend.safetyStock,
    suggestedQuantity: backend.suggestedQuantity,
    estimatedCost: fenToYuan(backend.estimatedCost),
    supplierName: backend.supplierName,
    modelResults: backend.modelResults,
  }
}

/** 前端预警记录查询参数 → 后端参数（warningType 字符串→数值） */
function toBackendRecordQuery(params?: WarningRecordQueryForm): Record<string, unknown> {
  const query: Record<string, unknown> = {}
  if (!params) return query
  if (params.warningType) query.warningType = inventoryWarningConverter.toBackendType(params.warningType)
  if (params.status !== undefined) query.status = params.status
  if (params.warehouseId) query.warehouseId = params.warehouseId
  if (params.materialName) query.materialName = params.materialName
  if (params.startDate) query.startDate = params.startDate
  if (params.endDate) query.endDate = params.endDate
  if (params.page) query.page = params.page
  if (params.size !== undefined) query.pageSize = params.size
  return query
}

/** 前端预警规则查询参数 → 后端参数（warningType 字符串→数值） */
function toBackendRuleQuery(params?: WarningRuleQueryForm): Record<string, unknown> {
  const query: Record<string, unknown> = {}
  if (!params) return query
  if (params.ruleName) query.ruleName = params.ruleName
  if (params.warningType) query.warningType = inventoryWarningConverter.toBackendType(params.warningType)
  if (params.enabled !== undefined) query.enabled = params.enabled
  if (params.page) query.page = params.page
  if (params.size !== undefined) query.pageSize = params.size
  return query
}

/** 前端预警规则创建表单 → 后端参数（warningType/notifyMethod 字符串→数值） */
function toBackendRuleCreate(data: WarningRuleCreateForm): Record<string, unknown> {
  return {
    ruleName: data.ruleName,
    warningType: inventoryWarningConverter.toBackendType(data.warningType),
    materialId: data.materialId,
    warehouseId: data.warehouseId,
    threshold: data.threshold,
    notifyMethod: inventoryWarningConverter.toBackendNotifyMethod(data.notifyMethod),
    notifyUsers: data.notifyUsers,
    remark: data.remark,
  }
}

/** 前端预警规则更新表单 → 后端参数 */
function toBackendRuleUpdate(data: Partial<WarningRuleCreateForm>): Record<string, unknown> {
  const query: Record<string, unknown> = {}
  if (data.ruleName !== undefined) query.ruleName = data.ruleName
  if (data.warningType) query.warningType = inventoryWarningConverter.toBackendType(data.warningType)
  if (data.materialId !== undefined) query.materialId = data.materialId
  if (data.warehouseId !== undefined) query.warehouseId = data.warehouseId
  if (data.threshold !== undefined) query.threshold = data.threshold
  if (data.notifyMethod) query.notifyMethod = inventoryWarningConverter.toBackendNotifyMethod(data.notifyMethod)
  if (data.notifyUsers !== undefined) query.notifyUsers = data.notifyUsers
  if (data.remark !== undefined) query.remark = data.remark
  return query
}

// ============================================================
// API 定义
// ============================================================

/** 预警记录API */
export const inventoryWarningApi = {
  /** 分页查询预警记录 */
  async getPage(params?: WarningRecordQueryForm): Promise<PageResult<InventoryWarningRecordInfo>> {
    const res = await get<PageResult<InventoryWarningRecordBackend>>('/v1/inventory/warnings/page', toBackendRecordQuery(params))
    return {
      records: (res?.records || []).map(toFrontendWarningRecord),
      total: res?.total || 0,
      current: res?.current || params?.page || 1,
      size: res?.size || params?.size || 10,
      pages: res?.pages || 0,
    }
  },

  /** 获取预警记录详情 */
  async getById(id: string): Promise<InventoryWarningRecordInfo> {
    const res = await get<InventoryWarningRecordBackend>(`/v1/inventory/warnings/${id}`)
    return toFrontendWarningRecord(res)
  },

  /** 处理预警 */
  async handle(id: string, data: WarningHandleForm): Promise<void> {
    await put<void>(`/v1/inventory/warnings/${id}/handle`, undefined, {
      params: { status: 1, handler: 'current', handleRemark: data.handleRemark },
    })
  },

  /** 手动生成预警 */
  async generate(): Promise<void> {
    await post<void>('/v1/inventory/warnings/generate')
  },

  /** 获取采购建议 */
  async getPurchaseSuggestions(): Promise<PurchaseSuggestionItem[]> {
    const res = await get<PurchaseSuggestionBackend[]>('/v1/inventory/warnings/purchase-suggestions')
    return (res || []).map(toFrontendPurchaseSuggestion)
  },

  /** 根据建议创建采购订单 */
  async createPurchaseOrder(data: {
    items: PurchaseSuggestionItem[]
    supplierId: string
    supplierName: string
    expectedDate: string
    priority: string
    remark: string
  }): Promise<{ orderNo: string }> {
    const backendItems = data.items.map(item => ({
      ...item,
      estimatedCost: Math.round(parseFloat(item.estimatedCost) * 100),
    }))
    return await post<{ orderNo: string }>('/v1/inventory/warnings/create-purchase-order', {
      items: backendItems,
      supplierId: data.supplierId,
      supplierName: data.supplierName,
      expectedDate: data.expectedDate,
      priority: data.priority,
      remark: data.remark,
    })
  },
}

/** 预警规则API */
export const inventoryWarningRuleApi = {
  /** 创建预警规则 */
  async create(data: WarningRuleCreateForm): Promise<InventoryWarningRuleInfo> {
    const res = await post<InventoryWarningRuleBackend>('/v1/inventory/warning-rules', toBackendRuleCreate(data))
    return toFrontendWarningRule(res)
  },

  /** 获取预警规则详情 */
  async getById(id: string): Promise<InventoryWarningRuleInfo> {
    const res = await get<InventoryWarningRuleBackend>(`/v1/inventory/warning-rules/${id}`)
    return toFrontendWarningRule(res)
  },

  /** 更新预警规则 */
  async update(id: string, data: Partial<WarningRuleCreateForm>): Promise<InventoryWarningRuleInfo> {
    const res = await put<InventoryWarningRuleBackend>(`/v1/inventory/warning-rules/${id}`, toBackendRuleUpdate(data))
    return toFrontendWarningRule(res)
  },

  /** 删除预警规则 */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/inventory/warning-rules/${id}`)
  },

  /** 分页查询预警规则 */
  async getPage(params?: WarningRuleQueryForm): Promise<PageResult<InventoryWarningRuleInfo>> {
    const res = await get<PageResult<InventoryWarningRuleBackend>>('/v1/inventory/warning-rules/page', toBackendRuleQuery(params))
    return {
      records: (res?.records || []).map(toFrontendWarningRule),
      total: res?.total || 0,
      current: res?.current || params?.page || 1,
      size: res?.size || params?.size || 10,
      pages: res?.pages || 0,
    }
  },

  /** 切换规则启用状态 */
  async toggleStatus(id: string, enabled: number): Promise<InventoryWarningRuleInfo> {
    const res = await put<InventoryWarningRuleBackend>(`/v1/inventory/warning-rules/${id}/toggle-status`, undefined, {
      params: { enabled },
    })
    return toFrontendWarningRule(res)
  },
}
