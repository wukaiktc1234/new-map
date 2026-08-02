/**
 * 库存调拨 API + DataConverter
 * 对应后端: /v1/inventory/transfers
 *
 * DataConverter 处理内容：
 * - 状态数字↔字符串（后端 transferStatus 0~3 ↔ 前端 'pending'/'shipped'/'received'/'completed'）
 * - ID number↔string（避免大数精度丢失）
 * - 字段名映射（后端 productName ↔ 前端 materialName、后端 productId ↔ 前端 materialId、后端 transferQuantity ↔ 前端 quantity、后端 createUserId ↔ 前端 applyUserId、后端 createTime ↔ 前端 applyTime、后端 receiveDate ↔ 前端 executeTime、后端 remark ↔ 前端 transferReason）
 * - 后端无字段时前端返回默认值（specification/unit/applyUserName/approveUserId/approveUserName/approveTime/items/approvalHistory）
 * - 状态查询参数直接传字符串（后端 status 为 String）
 */
import { get, post, put, del } from '../request'

import { inventoryTransferConverter } from './converters'
import type {
  InventoryTransferInfo,
  InventoryTransferQueryForm,
  InventoryTransferCreateForm,
  TransferApproveForm,
  TransferStatus,
} from '@/types/warehouse-transfer'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的调拨单实体（Controller 直接返回实体） */
interface InventoryTransferBackend {
  transferId: number
  transferCode: string | null
  fromWarehouseId: number | null
  toWarehouseId: number | null
  transferStatus: number | null          // 0/1/2/3
  transferDate: string | null              // yyyy-MM-dd
  receiveDate: string | null               // yyyy-MM-dd
  totalQuantity: number | null             // BigDecimal
  remark: string | null
  createUserId: number | null
  createTime: string | null                // yyyy-MM-dd HH:mm:ss
  updateTime: string | null                // yyyy-MM-dd HH:mm:ss
  deleted: number
  // 兼容性字段
  productId: number | null
  productName: string | null
  fromWarehouseName: string | null
  toWarehouseName: string | null
  transferQuantity: number | null
}

/** 后端 IPage 分页响应 */
interface InventoryTransferPageBackend {
  records: InventoryTransferBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

// ============================================================
// DataConverter（API 层，处理后端实体 ↔ 前端类型的双向转换）
// ============================================================

/**
 * 调拨单数据转换器
 * 注意：与 converters.ts 中的 inventoryTransferConverter（UI 层，用于 StatusTag 显示）同名不同用途。
 * 本转换器仅在 inventory-transfer.ts 内部使用，不通过 index.ts 导出。
 */
export const inventoryTransferDataConverter = {
  /** 后端实体 → 前端 Info */
  toFrontend(backend: InventoryTransferBackend): InventoryTransferInfo {
    const status: TransferStatus = inventoryTransferConverter.toFrontendStatus(backend.transferStatus ?? 0)
    return {
      transferId: String(backend.transferId),
      transferCode: backend.transferCode ?? '',
      fromWarehouseId: backend.fromWarehouseId != null ? String(backend.fromWarehouseId) : '',
      fromWarehouseName: backend.fromWarehouseName ?? '',
      toWarehouseId: backend.toWarehouseId != null ? String(backend.toWarehouseId) : '',
      toWarehouseName: backend.toWarehouseName ?? '',
      // 后端用 productId/productName 表示物料（兼容性字段）
      materialId: backend.productId != null ? String(backend.productId) : '',
      materialName: backend.productName ?? '',
      // 后端实体无 specification/unit，使用默认值
      specification: '',
      unit: '',
      // 后端 transferQuantity（兼容）优先，回退到 totalQuantity
      quantity: backend.transferQuantity != null
        ? Number(backend.transferQuantity)
        : (backend.totalQuantity != null ? Number(backend.totalQuantity) : 0),
      status,
      statusName: inventoryTransferConverter.toStatusLabel(status),
      // 后端实体无独立 transferReason 字段，使用 remark
      transferReason: backend.remark ?? '',
      // 后端 createUserId 即申请人
      applyUserId: backend.createUserId != null ? String(backend.createUserId) : '',
      // 后端实体无 applyUserName
      applyUserName: '',
      // 后端 createTime 即申请时间
      applyTime: backend.createTime ?? '',
      // 后端实体无审批人信息
      approveUserId: '',
      approveUserName: '',
      approveTime: '',
      // 后端 receiveDate 作为执行完成时间
      executeTime: backend.receiveDate ?? '',
      // 后端实体无审批历史
      approvalHistory: [],
      remark: backend.remark ?? '',
      // 后端实体无明细列表（仅 productId/productName/transferQuantity 单条）
      items: [],
      createTime: backend.createTime ?? '',
      updateTime: backend.updateTime ?? '',
    }
  },

  /** 后端实体列表 → 前端 Info 列表 */
  toFrontendList(list: InventoryTransferBackend[] | null | undefined): InventoryTransferInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(inventoryTransferDataConverter.toFrontend)
  },

  /** 前端创建表单 → 后端 CreateDTO 格式
   *  注意：后端 CreateDTO 仅支持单物料调拨（productId/productName/transferQuantity），
   *  前端表单的 items[] 仅取第一项。如有多项调拨需求，后端 DTO 需扩展。
   */
  toCreateDTO(form: InventoryTransferCreateForm): Record<string, unknown> {
    const firstItem = form.items?.[0]
    return {
      fromWarehouseId: form.fromWarehouseId ? Number(form.fromWarehouseId) : null,
      toWarehouseId: form.toWarehouseId ? Number(form.toWarehouseId) : null,
      productId: firstItem?.materialId ? Number(firstItem.materialId) : null,
      productName: firstItem?.materialId ? firstItem.materialId : null,
      transferQuantity: firstItem?.quantity ?? 0,
      // 后端 remark 同时承载备注，前端 transferReason 优先
      remark: form.transferReason || form.remark || null,
    }
  },

  /** 前端更新表单 → 后端 UpdateDTO 格式 */
  toUpdateDTO(form: Partial<InventoryTransferCreateForm>): Record<string, unknown> {
    const firstItem = form.items?.[0]
    const payload: Record<string, unknown> = {}
    if (form.fromWarehouseId !== undefined) payload.fromWarehouseId = form.fromWarehouseId ? Number(form.fromWarehouseId) : null
    if (form.toWarehouseId !== undefined) payload.toWarehouseId = form.toWarehouseId ? Number(form.toWarehouseId) : null
    if (firstItem?.materialId !== undefined) {
      payload.productId = firstItem.materialId ? Number(firstItem.materialId) : null
      payload.productName = firstItem.materialId || null
    }
    if (firstItem?.quantity !== undefined) payload.transferQuantity = firstItem.quantity
    if (form.transferReason !== undefined) payload.remark = form.transferReason || null
    return payload
  },

  /** 前端审批表单 → 后端 ApproveDTO 格式
   *  前端 approved(boolean) → 后端 status('approved'/'rejected')
   */
  toApproveDTO(form: TransferApproveForm): Record<string, unknown> {
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
 * 调拨单查询参数（传给后端）
 * 后端 status 参数为 String（'approved'/'rejected' 等），但实际由 Service 自行解析
 */
interface InventoryTransferQueryParams {
  page: number
  pageSize: number
  transferNo?: string
  fromWarehouseId?: number
  toWarehouseId?: number
  status?: string
  applyTimeStart?: string
  applyTimeEnd?: string
  [key: string]: unknown
}

export const inventoryTransferApi = {
  /**
   * 创建调拨单
   * @param data 创建表单数据
   */
  async create(data: InventoryTransferCreateForm): Promise<InventoryTransferInfo> {
    const dto = inventoryTransferDataConverter.toCreateDTO(data)
    const res = await post<InventoryTransferBackend>('/v1/inventory/transfers', dto)
    return inventoryTransferDataConverter.toFrontend(res)
  },

  /**
   * 更新调拨单
   * @param id 调拨单ID
   * @param data 更新数据
   */
  async update(id: string, data: Partial<InventoryTransferCreateForm>): Promise<InventoryTransferInfo> {
    const dto = inventoryTransferDataConverter.toUpdateDTO(data)
    const res = await put<InventoryTransferBackend>(`/v1/inventory/transfers/${id}`, dto)
    return inventoryTransferDataConverter.toFrontend(res)
  },

  /**
   * 根据 ID 获取调拨单详情
   * @param id 调拨单ID
   */
  async getById(id: string): Promise<InventoryTransferInfo> {
    const res = await get<InventoryTransferBackend>(`/v1/inventory/transfers/${id}`)
    return inventoryTransferDataConverter.toFrontend(res)
  },

  /**
   * 删除调拨单
   * @param id 调拨单ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/inventory/transfers/${id}`)
  },

  /**
   * 分页查询调拨单列表
   * @param params 查询参数（含分页）
   */
  async getPage(params?: InventoryTransferQueryForm): Promise<{
    records: InventoryTransferInfo[]
    total: number
    current: number
    size: number
    pages: number
  }> {
    const query: InventoryTransferQueryParams = {
      page: params?.page ?? 1,
      pageSize: params?.size ?? 10,
    }
    // 后端 transferNo 支持模糊查询（前端 transferCode 精确匹配 → 后端 transferNo）
    if (params?.transferCode) query.transferNo = params.transferCode
    if (params?.fromWarehouseId) query.fromWarehouseId = Number(params.fromWarehouseId)
    if (params?.toWarehouseId) query.toWarehouseId = Number(params.toWarehouseId)
    // 后端 status 为 String，直接传前端字符串（pending/shipped/received/completed）
    if (params?.status) query.status = params.status
    if (params?.startDate) query.applyTimeStart = params.startDate
    if (params?.endDate) query.applyTimeEnd = params.endDate

    const res = await get<InventoryTransferPageBackend | null>('/v1/inventory/transfers/page', query)
    const records = inventoryTransferDataConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    const current = res?.current ?? params?.page ?? 1
    const size = res?.size ?? params?.size ?? 10
    const pages = res?.pages ?? Math.ceil(total / size)
    return { records, total, current, size, pages }
  },

  /**
   * 审批调拨单
   * @param id 调拨单ID
   * @param data 审批表单（approved + remark）
   */
  async approve(id: string, data: TransferApproveForm): Promise<InventoryTransferInfo> {
    const dto = inventoryTransferDataConverter.toApproveDTO(data)
    const res = await post<InventoryTransferBackend>(`/v1/inventory/transfers/${id}/approve`, dto)
    return inventoryTransferDataConverter.toFrontend(res)
  },

  /**
   * 执行调拨（确认收货）
   * @param id 调拨单ID
   */
  async execute(id: string): Promise<InventoryTransferInfo> {
    const res = await post<InventoryTransferBackend>(`/v1/inventory/transfers/${id}/execute`)
    return inventoryTransferDataConverter.toFrontend(res)
  },
}

export default inventoryTransferApi
