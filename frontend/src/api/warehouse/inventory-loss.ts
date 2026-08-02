/**
 * 库存报损 API + DataConverter
 * 对应后端: /v1/inventory/losses
 *
 * DataConverter 处理内容：
 * - 报损类型数字↔字符串（后端 lossType 1~4 ↔ 前端 'expired'/'damaged'/'lost'/'other'）
 * - 状态数字↔字符串（后端 lossStatus 0~2 ↔ 前端 'pending'/'approved'/'processed'）
 * - ID number↔string（避免大数精度丢失）
 * - 金额 分↔元字符串（后端 Long 分 ↔ 前端 string 元）
 * - 字段名映射（后端 createUserId ↔ 前端 applyUserId、后端 createTime ↔ 前端 applyTime、后端 reason ↔ 前端 remark）
 * - 后端无字段时前端返回默认值（warehouseName/applyUserName/approveUserName/processTime/items）
 */
import { get, post, put, del } from '../request'

import { inventoryLossConverter } from './converters'
import type {
  InventoryLossInfo,
  LossType,
  LossStatus,
  InventoryLossQueryForm,
  InventoryLossCreateForm,
  LossApproveForm,
} from '@/types/warehouse-loss'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的报损单实体（Controller 直接返回实体） */
interface InventoryLossBackend {
  lossId: number
  lossCode: string | null
  warehouseId: number | null
  lossType: number | null              // 1/2/3/4
  lossStatus: number | null            // 0/1/2
  totalQuantity: number | null         // BigDecimal
  totalAmount: number | null           // 分
  reason: string | null
  createUserId: number | null
  approveUserId: number | null
  approveTime: string | null            // yyyy-MM-dd HH:mm:ss
  createTime: string | null             // yyyy-MM-dd HH:mm:ss
  updateTime: string | null            // yyyy-MM-dd HH:mm:ss
  deleted: number
}

/** 后端 IPage 分页响应 */
interface InventoryLossPageBackend {
  records: InventoryLossBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

// ============================================================
// DataConverter（API 层，处理后端实体 ↔ 前端类型的双向转换）
// ============================================================

/**
 * 报损单数据转换器
 * 注意：与 converters.ts 中的 inventoryLossConverter（UI 层，用于 StatusTag 显示）同名不同用途。
 * 本转换器仅在 inventory-loss.ts 内部使用，不通过 index.ts 导出。
 */
export const inventoryLossDataConverter = {
  /** 后端实体 → 前端 Info */
  toFrontend(backend: InventoryLossBackend): InventoryLossInfo {
    const lossType: LossType = inventoryLossConverter.toFrontendType(backend.lossType ?? 4)
    const status: LossStatus = inventoryLossConverter.toFrontendStatus(backend.lossStatus ?? 0)
    return {
      lossId: String(backend.lossId),
      lossCode: backend.lossCode ?? '',
      warehouseId: backend.warehouseId != null ? String(backend.warehouseId) : '',
      // 后端实体无 warehouseName
      warehouseName: '',
      lossType,
      lossTypeName: inventoryLossConverter.toTypeLabel(lossType),
      status,
      statusName: inventoryLossConverter.toStatusLabel(status),
      // 后端 Long 分 → 前端 string 元
      totalAmount: inventoryLossConverter.toYuan(backend.totalAmount),
      // 后端 createUserId 即申请人
      applyUserId: backend.createUserId != null ? String(backend.createUserId) : '',
      // 后端实体无 applyUserName
      applyUserName: '',
      // 后端 createTime 即申请时间
      applyTime: backend.createTime ?? '',
      // 后端 approveUserId 即审批人
      approveUserId: backend.approveUserId != null ? String(backend.approveUserId) : '',
      // 后端实体无 approveUserName
      approveUserName: '',
      approveTime: backend.approveTime ?? '',
      // 后端实体无 processTime，使用 updateTime 兜底
      processTime: '',
      // 后端实体无独立 remark 字段，使用 reason
      remark: backend.reason ?? '',
      // 后端实体无明细列表（仅 totalQuantity/totalAmount 汇总）
      items: [],
      createTime: backend.createTime ?? '',
      updateTime: backend.updateTime ?? '',
    }
  },

  /** 后端实体列表 → 前端 Info 列表 */
  toFrontendList(list: InventoryLossBackend[] | null | undefined): InventoryLossInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(inventoryLossDataConverter.toFrontend)
  },

  /** 前端创建表单 → 后端 CreateDTO 格式
   *  注意：后端 CreateDTO 仅支持单条报损（totalQuantity/totalAmount），
   *  前端表单的 items[] 仅取第一项。如有多项报损需求，后端 DTO 需扩展。
   */
  toCreateDTO(form: InventoryLossCreateForm): Record<string, unknown> {
    const firstItem = form.items?.[0]
    const quantity = firstItem?.quantity ?? 0
    // 后端无明细级金额，前端表单也无金额输入，传 0 占位
    const totalAmountFen = 0
    return {
      warehouseId: form.warehouseId ? Number(form.warehouseId) : null,
      lossType: inventoryLossConverter.toBackendType(form.lossType),
      totalQuantity: quantity,
      totalAmount: totalAmountFen,
      // 后端 reason 字段同时承载备注和原因，前端 reason 优先
      reason: firstItem?.reason || form.remark || null,
    }
  },

  /** 前端更新表单 → 后端 UpdateDTO 格式 */
  toUpdateDTO(form: Partial<InventoryLossCreateForm>): Record<string, unknown> {
    const firstItem = form.items?.[0]
    const payload: Record<string, unknown> = {}
    if (form.warehouseId !== undefined) payload.warehouseId = form.warehouseId ? Number(form.warehouseId) : null
    if (form.lossType !== undefined) payload.lossType = inventoryLossConverter.toBackendType(form.lossType)
    if (firstItem?.quantity !== undefined) payload.totalQuantity = firstItem.quantity
    if (firstItem?.reason !== undefined) payload.reason = firstItem.reason || null
    else if (form.remark !== undefined) payload.reason = form.remark || null
    return payload
  },

  /** 前端审批表单 → 后端 ApproveDTO 格式
   *  前端 approved(boolean) → 后端 status('approved'/'rejected')
   */
  toApproveDTO(form: LossApproveForm): Record<string, unknown> {
    return {
      status: form.approved ? 'approved' : 'rejected',
      remark: form.remark ?? null,
    }
  },
}

// ============================================================
// API 实现
// ============================================================

/**
 * 报损单查询参数（传给后端）
 */
interface InventoryLossQueryParams {
  page: number
  pageSize: number
  lossNo?: string
  warehouseId?: number
  status?: string
  applyTimeStart?: string
  applyTimeEnd?: string
  [key: string]: unknown
}

export const inventoryLossApi = {
  /**
   * 创建报损单
   * @param data 创建表单数据
   */
  async create(data: InventoryLossCreateForm): Promise<InventoryLossInfo> {
    const dto = inventoryLossDataConverter.toCreateDTO(data)
    const res = await post<InventoryLossBackend>('/v1/inventory/losses', dto)
    return inventoryLossDataConverter.toFrontend(res)
  },

  /**
   * 更新报损单
   * @param id 报损单ID
   * @param data 更新数据
   */
  async update(id: string, data: Partial<InventoryLossCreateForm>): Promise<InventoryLossInfo> {
    const dto = inventoryLossDataConverter.toUpdateDTO(data)
    const res = await put<InventoryLossBackend>(`/v1/inventory/losses/${id}`, dto)
    return inventoryLossDataConverter.toFrontend(res)
  },

  /**
   * 根据 ID 获取报损单详情
   * @param id 报损单ID
   */
  async getById(id: string): Promise<InventoryLossInfo> {
    const res = await get<InventoryLossBackend>(`/v1/inventory/losses/${id}`)
    return inventoryLossDataConverter.toFrontend(res)
  },

  /**
   * 删除报损单
   * @param id 报损单ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/inventory/losses/${id}`)
  },

  /**
   * 分页查询报损单列表
   * @param params 查询参数（含分页）
   */
  async getPage(params?: InventoryLossQueryForm): Promise<{
    records: InventoryLossInfo[]
    total: number
    current: number
    size: number
    pages: number
  }> {
    const query: InventoryLossQueryParams = {
      page: params?.page ?? 1,
      pageSize: params?.size ?? 10,
    }
    // 后端 lossNo 支持模糊查询（前端 lossCode 精确匹配 → 后端 lossNo）
    if (params?.lossCode) query.lossNo = params.lossCode
    if (params?.warehouseId) query.warehouseId = Number(params.warehouseId)
    // 后端 status 为 String，直接传前端字符串（pending/approved/processed）
    if (params?.status) query.status = params.status
    if (params?.startDate) query.applyTimeStart = params.startDate
    if (params?.endDate) query.applyTimeEnd = params.endDate
    // 后端不支持按 lossType 查询（仅按 status/warehouseId/lossNo/时间范围）

    const res = await get<InventoryLossPageBackend | null>('/v1/inventory/losses/page', query)
    const records = inventoryLossDataConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    const current = res?.current ?? params?.page ?? 1
    const size = res?.size ?? params?.size ?? 10
    const pages = res?.pages ?? Math.ceil(total / size)
    return { records, total, current, size, pages }
  },

  /**
   * 审批报损单
   * @param id 报损单ID
   * @param data 审批表单（approved + remark）
   */
  async approve(id: string, data: LossApproveForm): Promise<InventoryLossInfo> {
    const dto = inventoryLossDataConverter.toApproveDTO(data)
    const res = await post<InventoryLossBackend>(`/v1/inventory/losses/${id}/approve`, dto)
    return inventoryLossDataConverter.toFrontend(res)
  },

  /**
   * 处理报损（执行库存扣减）
   * @param id 报损单ID
   */
  async process(id: string): Promise<InventoryLossInfo> {
    const res = await post<InventoryLossBackend>(`/v1/inventory/losses/${id}/process`)
    return inventoryLossDataConverter.toFrontend(res)
  },
}

export default inventoryLossApi
