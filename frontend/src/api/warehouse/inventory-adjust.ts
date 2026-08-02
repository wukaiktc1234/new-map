/**
 * 库存调整 API + DataConverter
 * 对应后端: /v1/inventory/adjustments
 *
 * DataConverter 处理内容：
 * - 后端返回 Map<String, Object>（已由 Service 层转换为前端友好格式）
 * - 类型/状态为 String（无需数字↔字符串映射，后端已是 'gain'/'loss'/'temp_loss'/'weight_diff'/'other'/'pending'/'approved'/'completed'/'rejected'）
 * - 金额由后端返回分（Long），前端统一做 分→元 转换
 * - BigDecimal 数量 → number（避免大数精度问题）
 * - participatingDepts 后端返回 List<String> → 前端 string[]
 * - 字段名对齐（后端 adjustId 实际为 adjustCode 业务编号）
 * - getAdjustByCode 返回 items + approvalHistory；page 查询不返回（前端兼容处理）
 */
import { get, post, put } from '../request'

import { inventoryAdjustConverter } from './converters'
import type {
  InventoryAdjustInfo,
  AdjustItemInfo,
  ApprovalHistoryItem,
  ApprovalStep,
  AdjustType,
  AdjustStatus,
  AdjustQueryForm,
  AdjustCreateForm,
  AdjustApproveForm,
} from '@/types/warehouse-adjust'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的调整单明细（Map 格式，由 Service convertItemToMap 产生） */
interface AdjustItemBackend {
  adjustItemId: string | null
  adjustId: string | null
  materialId: string | null
  materialName: string | null
  specification: string | null
  unit: string | null
  beforeQuantity: number | null        // BigDecimal
  adjustQuantity: number | null         // BigDecimal
  afterQuantity: number | null          // BigDecimal
  unitCost: number | null               // 单位：分
  adjustAmount: number | null           // 单位：分
  batchNo: string | null
  reason: string | null
}

/** 后端返回的审批历史项 */
interface ApprovalHistoryBackend {
  step: string | null
  userName: string | null
  time: string | null
  comment: string | null
}

/** 后端返回的调整单（Map 格式，由 Service convertAdjustToMap 产生） */
interface InventoryAdjustBackend {
  adjustId: string | null                // 实际为 adjustCode 业务编号
  adjustCode: string | null
  adjustType: string | null              // 'gain'/'loss'/'temp_loss'/'weight_diff'/'other'
  adjustTypeName: string | null
  warehouseId: string | null
  warehouseName: string | null
  referenceCheckCode: string | null
  adjustReason: string | null
  referenceNo: string | null
  totalAdjustQuantity: number | null     // BigDecimal
  totalAdjustAmount: number | null      // 单位：分
  status: string | null                  // 'pending'/'approved'/'completed'/'rejected'
  statusName: string | null
  participatingDepts: string[] | null   // 后端已转为 List<String>
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
  // 仅 getAdjustByCode 返回；page 查询不返回
  items?: AdjustItemBackend[] | null
  approvalHistory?: ApprovalHistoryBackend[] | null
}

/** 后端 IPage 分页响应 */
interface InventoryAdjustPageBackend {
  records: InventoryAdjustBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

// ============================================================
// DataConverter（API 层，处理后端 Map ↔ 前端类型的双向转换）
// ============================================================

/**
 * 调整单数据转换器
 * 注意：与 converters.ts 中的 inventoryAdjustConverter（UI 层，用于 StatusTag 显示）同名不同用途。
 * 本转换器仅在 inventory-adjust.ts 内部使用，不通过 index.ts 导出。
 */
export const inventoryAdjustDataConverter = {
  /** 后端明细 Map → 前端 AdjustItemInfo */
  toItemFrontend(backend: AdjustItemBackend): AdjustItemInfo {
    return {
      adjustItemId: backend.adjustItemId ?? '',
      adjustId: backend.adjustId ?? '',
      materialId: backend.materialId ?? '',
      materialName: backend.materialName ?? '',
      specification: backend.specification ?? '',
      unit: backend.unit ?? '',
      // 后端 BigDecimal → 前端 number
      beforeQuantity: backend.beforeQuantity != null ? Number(backend.beforeQuantity) : 0,
      adjustQuantity: backend.adjustQuantity != null ? Number(backend.adjustQuantity) : 0,
      afterQuantity: backend.afterQuantity != null ? Number(backend.afterQuantity) : 0,
      // 后端返回分，前端转换为元字符串
      unitCost: inventoryAdjustConverter.toYuan(backend.unitCost),
      adjustAmount: inventoryAdjustConverter.toYuan(backend.adjustAmount),
      batchNo: backend.batchNo ?? '',
      reason: backend.reason ?? '',
    }
  },

  /** 后端明细列表 → 前端 Info 列表 */
  toItemFrontendList(list: AdjustItemBackend[] | null | undefined): AdjustItemInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(inventoryAdjustDataConverter.toItemFrontend)
  },

  /** 后端审批历史 → 前端 ApprovalHistoryItem 列表 */
  toApprovalHistoryList(list: ApprovalHistoryBackend[] | null | undefined): ApprovalHistoryItem[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(item => ({
      // 后端 step 字符串 → 前端 ApprovalStep（已对齐，仅做类型断言）
      step: (item.step ?? 'submit') as ApprovalStep,
      userName: item.userName ?? '',
      time: item.time ?? '',
      comment: item.comment ?? '',
    }))
  },

  /** 后端 Map → 前端 Info */
  toFrontend(backend: InventoryAdjustBackend): InventoryAdjustInfo {
    const adjustType: AdjustType = inventoryAdjustConverter.toFrontendType(backend.adjustType)
    const status: AdjustStatus = inventoryAdjustConverter.toFrontendStatus(backend.status)
    // 后端 participatingDepts 已是 List<String>，直接采用；缺失时为空数组
    const depts = Array.isArray(backend.participatingDepts) ? backend.participatingDepts : []
    return {
      // 后端 adjustId 实际是 adjustCode（业务编号），直接采用
      adjustId: backend.adjustId ?? backend.adjustCode ?? '',
      adjustCode: backend.adjustCode ?? '',
      adjustType,
      adjustTypeName: backend.adjustTypeName || inventoryAdjustConverter.toTypeLabel(adjustType),
      warehouseId: backend.warehouseId ?? '',
      warehouseName: backend.warehouseName ?? '',
      referenceCheckCode: backend.referenceCheckCode ?? '',
      adjustReason: backend.adjustReason ?? '',
      referenceNo: backend.referenceNo ?? '',
      items: inventoryAdjustDataConverter.toItemFrontendList(backend.items),
      // 后端 BigDecimal → 前端 number
      totalAdjustQuantity: backend.totalAdjustQuantity != null ? Number(backend.totalAdjustQuantity) : 0,
      // 后端返回分，前端转换为元字符串
      totalAdjustAmount: inventoryAdjustConverter.toYuan(backend.totalAdjustAmount),
      status,
      statusName: backend.statusName || inventoryAdjustConverter.toStatusLabel(status),
      participatingDepts: depts,
      applyUserId: backend.applyUserId ?? '',
      applyUserName: backend.applyUserName ?? '',
      applyTime: backend.applyTime ?? '',
      approveUserId: backend.approveUserId ?? '',
      approveUserName: backend.approveUserName ?? '',
      approveTime: backend.approveTime ?? '',
      completeTime: backend.completeTime ?? '',
      approvalHistory: inventoryAdjustDataConverter.toApprovalHistoryList(backend.approvalHistory),
      remark: backend.remark ?? '',
      createTime: backend.createTime ?? '',
      updateTime: backend.updateTime ?? '',
    }
  },

  /** 后端 Map 列表 → 前端 Info 列表 */
  toFrontendList(list: InventoryAdjustBackend[] | null | undefined): InventoryAdjustInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(inventoryAdjustDataConverter.toFrontend)
  },

  /** 前端创建表单 → 后端 CreateDTO 格式
   *  后端 CreateDTO 已支持多明细 items[]，无需降级
   */
  toCreateDTO(form: AdjustCreateForm): Record<string, unknown> {
    return {
      adjustType: form.adjustType,
      warehouseId: form.warehouseId,
      referenceCheckCode: form.referenceCheckCode ?? null,
      adjustReason: form.adjustReason ?? null,
      referenceNo: form.referenceNo ?? null,
      participatingDepts: form.participatingDepts ?? [],
      items: (form.items ?? []).map(item => ({
        materialId: item.materialId,
        materialName: item.materialName ?? null,
        specification: item.specification ?? null,
        unit: item.unit ?? null,
        beforeQuantity: item.beforeQuantity ?? 0,
        adjustQuantity: item.adjustQuantity,
        batchNo: item.batchNo ?? null,
        unitCost: item.unitCost ?? null,
        reason: item.reason,
      })),
      remark: form.remark ?? null,
    }
  },

  /** 前端审批表单 → 后端 ApproveDTO 格式
   *  后端 ApproveDTO 使用 approved(Boolean) + opinion(String)
   */
  toApproveDTO(form: AdjustApproveForm): Record<string, unknown> {
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
 * 调整单查询参数（传给后端）
 * 后端已支持 keyword 模糊查询（调整单号/物料名称）
 */
interface InventoryAdjustQueryParams {
  page: number
  size: number
  adjustType?: string
  status?: string
  warehouseId?: string
  startDate?: string
  endDate?: string
  keyword?: string
  [key: string]: unknown
}

export const inventoryAdjustApi = {
  /**
   * 分页查询调整单列表
   * @param params 查询参数（含分页）
   */
  async getPage(params?: AdjustQueryForm): Promise<{
    records: InventoryAdjustInfo[]
    total: number
    current: number
    size: number
    pages: number
  }> {
    const query: InventoryAdjustQueryParams = {
      page: params?.page ?? 1,
      size: params?.size ?? 10,
    }
    // 后端 adjustType/status 均为 String，直接传前端字符串
    if (params?.adjustType) query.adjustType = params.adjustType
    if (params?.status) query.status = params.status
    if (params?.warehouseId) query.warehouseId = params.warehouseId
    if (params?.startDate) query.startDate = params.startDate
    if (params?.endDate) query.endDate = params.endDate
    if (params?.keyword) query.keyword = params.keyword

    const res = await get<InventoryAdjustPageBackend | null>('/v1/inventory/adjustments/page', query)
    const records = inventoryAdjustDataConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    const current = res?.current ?? params?.page ?? 1
    const size = res?.size ?? params?.size ?? 10
    const pages = res?.pages ?? Math.ceil(total / size)
    return { records, total, current, size, pages }
  },

  /**
   * 根据调整单号获取调整单详情（含明细 items 和 审批历史 approvalHistory）
   * @param adjustCode 调整单号（业务编号）
   */
  async getById(adjustCode: string): Promise<InventoryAdjustInfo> {
    const res = await get<InventoryAdjustBackend | null>(`/v1/inventory/adjustments/${adjustCode}`)
    // 后端在不存在时返回 null（404 由 Result.error 体现），此处兜底构造空对象
    return inventoryAdjustDataConverter.toFrontend(res ?? {} as InventoryAdjustBackend)
  },

  /**
   * 创建调整单
   * @param data 创建表单数据
   */
  async create(data: AdjustCreateForm): Promise<InventoryAdjustInfo> {
    const dto = inventoryAdjustDataConverter.toCreateDTO(data)
    const res = await post<InventoryAdjustBackend>('/v1/inventory/adjustments', dto)
    return inventoryAdjustDataConverter.toFrontend(res)
  },

  /**
   * 审批调整单（后端返回 Result<Void>）
   * @param adjustCode 调整单号
   * @param data 审批表单
   */
  async approve(adjustCode: string, data: AdjustApproveForm): Promise<void> {
    const dto = inventoryAdjustDataConverter.toApproveDTO(data)
    await put<void>(`/v1/inventory/adjustments/${adjustCode}/approve`, dto)
  },

  /**
   * 执行库存调整（后端返回 Result<Void>）
   * @param adjustCode 调整单号
   */
  async execute(adjustCode: string): Promise<void> {
    await put<void>(`/v1/inventory/adjustments/${adjustCode}/execute`)
  },
}

export default inventoryAdjustApi
