/**
 * 库存出库 API + DataConverter
 * 对应后端: /v1/inventory/outbounds
 *
 * DataConverter 处理内容：
 * - 后端返回 Map<String, Object>（已由 Service 层转换为前端友好格式）
 * - 类型/状态为 String（无需数字↔字符串映射，后端已是 'pending'/'approved'/'completed'/'rejected'）
 * - 金额由后端返回分（Long），前端统一做 分→元 转换
 * - BigDecimal 数量 → number（避免大数精度问题）
 * - 字段名对齐（后端 outboundId 实际为 outboundCode 业务编号）
 * - 后端无字段时前端返回默认值（items 列表）
 */
import { get, post, put } from '../request'

import { inventoryOutboundConverter } from './converters'
import type {
  InventoryOutboundInfo,
  OutboundItemInfo,
  OutboundType,
  OutboundStatus,
  OutboundQueryForm,
  OutboundCreateForm,
  OutboundApproveForm,
} from '@/types/warehouse-outbound'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的出库单明细（Map 格式，由 Service convertItemToMap 产生） */
interface OutboundItemBackend {
  outboundItemId: string | null
  outboundId: string | null
  materialId: string | null
  materialName: string | null
  specification: string | null
  unit: string | null
  requestQuantity: number | null       // BigDecimal
  actualQuantity: number | null        // BigDecimal
  batchNo: string | null
  unitCost: number | null               // 单位：分
  totalCost: number | null              // 单位：分
  remark: string | null
}

/** 后端返回的出库单（Map 格式，由 Service convertOutboundToMap 产生） */
interface InventoryOutboundBackend {
  outboundId: string | null              // 实际为 outboundCode 业务编号
  outboundCode: string | null
  outboundType: string | null            // 'requisition'/'sale'/'return'/'other'
  outboundTypeName: string | null
  warehouseId: string | null
  warehouseName: string | null
  targetId: string | null
  targetName: string | null
  referenceNo: string | null
  outboundDate: string | null            // yyyy-MM-dd
  totalQuantity: number | null          // BigDecimal
  totalAmount: number | null             // 单位：分
  status: string | null                  // 'pending'/'approved'/'completed'/'rejected'
  statusName: string | null
  applyUserId: string | null
  applyUserName: string | null
  applyTime: string | null               // yyyy-MM-dd HH:mm:ss
  approveUserId: string | null
  approveUserName: string | null
  approveTime: string | null
  completeTime: string | null
  remark: string | null
  createTime: string | null
  updateTime: string | null
  items?: OutboundItemBackend[] | null
}

/** 后端 IPage 分页响应 */
interface InventoryOutboundPageBackend {
  records: InventoryOutboundBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

// ============================================================
// DataConverter（API 层，处理后端 Map ↔ 前端类型的双向转换）
// ============================================================

/**
 * 出库单数据转换器
 * 注意：与 converters.ts 中的 inventoryOutboundConverter（UI 层，用于 StatusTag 显示）同名不同用途。
 * 本转换器仅在 inventory-outbound.ts 内部使用，不通过 index.ts 导出。
 */
export const inventoryOutboundDataConverter = {
  /** 后端明细 Map → 前端 OutboundItemInfo */
  toItemFrontend(backend: OutboundItemBackend): OutboundItemInfo {
    return {
      outboundItemId: backend.outboundItemId ?? '',
      outboundId: backend.outboundId ?? '',
      materialId: backend.materialId ?? '',
      materialName: backend.materialName ?? '',
      specification: backend.specification ?? '',
      unit: backend.unit ?? '',
      // 后端 BigDecimal → 前端 number
      requestQuantity: backend.requestQuantity != null ? Number(backend.requestQuantity) : 0,
      actualQuantity: backend.actualQuantity != null ? Number(backend.actualQuantity) : 0,
      batchNo: backend.batchNo ?? '',
      // 后端返回分，前端转换为元字符串
      unitCost: inventoryOutboundConverter.toYuan(backend.unitCost),
      totalCost: inventoryOutboundConverter.toYuan(backend.totalCost),
      remark: backend.remark ?? '',
    }
  },

  /** 后端明细列表 → 前端 Info 列表 */
  toItemFrontendList(list: OutboundItemBackend[] | null | undefined): OutboundItemInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(inventoryOutboundDataConverter.toItemFrontend)
  },

  /** 后端 Map → 前端 Info */
  toFrontend(backend: InventoryOutboundBackend): InventoryOutboundInfo {
    const outboundType: OutboundType = inventoryOutboundConverter.toFrontendType(backend.outboundType)
    const status: OutboundStatus = inventoryOutboundConverter.toFrontendStatus(backend.status)
    return {
      // 后端 outboundId 实际是 outboundCode（业务编号），直接采用
      outboundId: backend.outboundId ?? backend.outboundCode ?? '',
      outboundCode: backend.outboundCode ?? '',
      outboundType,
      outboundTypeName: backend.outboundTypeName || inventoryOutboundConverter.toTypeLabel(outboundType),
      warehouseId: backend.warehouseId ?? '',
      warehouseName: backend.warehouseName ?? '',
      targetId: backend.targetId ?? '',
      targetName: backend.targetName ?? '',
      referenceNo: backend.referenceNo ?? '',
      outboundDate: backend.outboundDate ?? '',
      items: inventoryOutboundDataConverter.toItemFrontendList(backend.items),
      // 后端 BigDecimal → 前端 number
      totalQuantity: backend.totalQuantity != null ? Number(backend.totalQuantity) : 0,
      // 后端返回分，前端转换为元字符串
      totalAmount: inventoryOutboundConverter.toYuan(backend.totalAmount),
      status,
      statusName: backend.statusName || inventoryOutboundConverter.toStatusLabel(status),
      applyUserId: backend.applyUserId ?? '',
      applyUserName: backend.applyUserName ?? '',
      applyTime: backend.applyTime ?? '',
      approveUserId: backend.approveUserId ?? '',
      approveUserName: backend.approveUserName ?? '',
      approveTime: backend.approveTime ?? '',
      completeTime: backend.completeTime ?? '',
      remark: backend.remark ?? '',
      createTime: backend.createTime ?? '',
      updateTime: backend.updateTime ?? '',
    }
  },

  /** 后端 Map 列表 → 前端 Info 列表 */
  toFrontendList(list: InventoryOutboundBackend[] | null | undefined): InventoryOutboundInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(inventoryOutboundDataConverter.toFrontend)
  },

  /** 前端创建表单 → 后端 CreateDTO 格式
   *  后端 CreateDTO 已支持多明细 items[]，无需降级
   */
  toCreateDTO(form: OutboundCreateForm): Record<string, unknown> {
    return {
      outboundType: form.outboundType,
      warehouseId: form.warehouseId,
      targetId: form.targetId ?? null,
      targetName: form.targetName ?? null,
      referenceNo: form.referenceNo ?? null,
      outboundDate: form.outboundDate,
      items: (form.items ?? []).map(item => ({
        materialId: item.materialId,
        requestQuantity: item.requestQuantity,
        remark: item.remark ?? null,
      })),
      remark: form.remark ?? null,
    }
  },

  /** 前端审批表单 → 后端 ApproveDTO 格式
   *  后端 ApproveDTO 使用 approved(Boolean) + opinion(String)
   */
  toApproveDTO(form: OutboundApproveForm): Record<string, unknown> {
    return {
      approved: form.approved,
      opinion: form.opinion ?? null,
    }
  },
}

// ============================================================
// API 实现
// ============================================================

/**
 * 出库单查询参数（传给后端）
 * 后端已支持 keyword 模糊查询（出库单号/物料名称）
 */
interface InventoryOutboundQueryParams {
  page: number
  size: number
  outboundType?: string
  status?: string
  warehouseId?: string
  startDate?: string
  endDate?: string
  keyword?: string
  [key: string]: unknown
}

export const inventoryOutboundApi = {
  /**
   * 分页查询出库单列表
   * @param params 查询参数（含分页）
   */
  async getPage(params?: OutboundQueryForm): Promise<{
    records: InventoryOutboundInfo[]
    total: number
    current: number
    size: number
    pages: number
  }> {
    const query: InventoryOutboundQueryParams = {
      page: params?.page ?? 1,
      size: params?.size ?? 10,
    }
    // 后端 outboundType/status 均为 String，直接传前端字符串
    if (params?.outboundType) query.outboundType = params.outboundType
    if (params?.status) query.status = params.status
    if (params?.warehouseId) query.warehouseId = params.warehouseId
    if (params?.startDate) query.startDate = params.startDate
    if (params?.endDate) query.endDate = params.endDate
    if (params?.keyword) query.keyword = params.keyword

    const res = await get<InventoryOutboundPageBackend | null>('/v1/inventory/outbounds/page', query)
    const records = inventoryOutboundDataConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    const current = res?.current ?? params?.page ?? 1
    const size = res?.size ?? params?.size ?? 10
    const pages = res?.pages ?? Math.ceil(total / size)
    return { records, total, current, size, pages }
  },

  /**
   * 根据出库单号获取出库单详情
   * @param outboundCode 出库单号（业务编号）
   */
  async getById(outboundCode: string): Promise<InventoryOutboundInfo> {
    const res = await get<InventoryOutboundBackend | null>(`/v1/inventory/outbounds/${outboundCode}`)
    // 后端在不存在时返回 null（404 由 Result.error 体现），此处兜底构造空对象
    return inventoryOutboundDataConverter.toFrontend(res ?? {} as InventoryOutboundBackend)
  },

  /**
   * 创建出库单
   * @param data 创建表单数据
   */
  async create(data: OutboundCreateForm): Promise<InventoryOutboundInfo> {
    const dto = inventoryOutboundDataConverter.toCreateDTO(data)
    const res = await post<InventoryOutboundBackend>('/v1/inventory/outbounds', dto)
    return inventoryOutboundDataConverter.toFrontend(res)
  },

  /**
   * 审批出库单（后端返回 Result<Void>）
   * @param outboundCode 出库单号
   * @param data 审批表单
   */
  async approve(outboundCode: string, data: OutboundApproveForm): Promise<void> {
    const dto = inventoryOutboundDataConverter.toApproveDTO(data)
    await put<void>(`/v1/inventory/outbounds/${outboundCode}/approve`, dto)
  },

  /**
   * 执行出库（确认出库，后端返回 Result<Void>）
   * @param outboundCode 出库单号
   */
  async execute(outboundCode: string): Promise<void> {
    await put<void>(`/v1/inventory/outbounds/${outboundCode}/execute`)
  },
}

export default inventoryOutboundApi
