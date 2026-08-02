/**
 * 物资需求提报 API + DataConverter
 * 对应后端: /v1/purchase/material-requests
 *
 * DataConverter 处理内容：
 * - 状态数字↔字符串（后端 status 0~4 ↔ 前端 'draft'/'pending'/'approved'/'rejected'/'converted'）
 * - ID number↔string（避免大数精度丢失）
 * - 金额 分↔元（后端 Long 分 ↔ 前端 number 元）
 * - 日期 LocalDate/LocalDateTime ↔ string
 */
import { get, post, put, del } from '../request'
// 金额转换统一委托给 utils/money（fenToYuanNumber 别名 fenToYuan 保留 2 位小数精度）
import { yuanToFen, fenToYuanNumber as fenToYuan } from '@/utils/money'

import type {
  MaterialRequestInfo,
  MaterialRequestItem,
  MaterialRequestQueryForm,
  MaterialRequestStatus,
} from '@/types/material-request'

// ============================================================
// 后端原始类型定义
// ============================================================

/** 后端返回的物资需求明细实体（MaterialRequestDTO.MaterialRequestItemVO） */
interface MaterialRequestItemBackend {
  itemId: number
  requestId: number
  materialId: number | null
  materialName: string
  specification: string | null
  quantity: number
  unit: string
  estimatedPrice: number      // 单位：分
  subtotalAmount: number      // 单位：分
  remark: string | null
}

/** 后端返回的物资需求提报实体（MaterialRequestDTO） */
interface MaterialRequestBackend {
  requestId: number
  requestNo: string
  title: string
  storeName: string
  applicantId: number | null
  applicantName: string | null
  expectedDate: string | null       // yyyy-MM-dd
  status: number                    // 0~4
  totalAmount: number               // 单位：分
  convertedRequestNo: string | null
  remark: string | null
  createTime: string | null         // yyyy-MM-dd HH:mm:ss
  updateTime: string | null         // yyyy-MM-dd HH:mm:ss
  items?: MaterialRequestItemBackend[] | null
}

/** 后端 IPage 分页响应 */
interface MaterialRequestPageBackend {
  records: MaterialRequestBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

// ============================================================
// 状态映射（后端数字 ↔ 前端字符串）
// 后端 status：0草稿 1待审核 2已审核 3已驳回 4已转采购申请
// ============================================================

const STATUS_TO_FRONTEND: Record<number, MaterialRequestStatus> = {
  0: 'draft',
  1: 'pending',
  2: 'approved',
  3: 'rejected',
  4: 'converted',
}

const STATUS_TO_BACKEND: Record<MaterialRequestStatus, number> = {
  draft: 0,
  pending: 1,
  approved: 2,
  rejected: 3,
  converted: 4,
}

// ============================================================
// 金额转换（分 ↔ 元，统一委托给 utils/money，见文件顶部 import）
// ============================================================

// ============================================================
// DataConverter
// ============================================================

/**
 * 物资需求提报数据转换器（API 层，处理后端实体 ↔ 前端类型的双向转换）
 *
 * 注意：与 converters.ts 中的 materialRequestConverter（UI 层，用于 StatusTag 显示）同名不同用途。
 * 本转换器仅在 material-request.ts 内部使用，不通过 index.ts 导出。
 */
export const materialRequestConverter = {
  /** 后端明细实体 → 前端 MaterialRequestItem */
  toItemFrontend(backend: MaterialRequestItemBackend): MaterialRequestItem {
    return {
      materialId: backend.materialId != null ? String(backend.materialId) : '',
      materialName: backend.materialName,
      specification: backend.specification ?? '',
      quantity: Number(backend.quantity),
      unit: backend.unit,
      estimatedPrice: fenToYuan(backend.estimatedPrice),
      subtotalAmount: fenToYuan(backend.subtotalAmount),
      remark: backend.remark ?? '',
    }
  },

  /** 后端实体 → 前端 MaterialRequestInfo */
  toFrontend(backend: MaterialRequestBackend): MaterialRequestInfo {
    return {
      requestId: String(backend.requestId),
      requestNo: backend.requestNo,
      title: backend.title,
      storeName: backend.storeName,
      createBy: backend.applicantId != null ? String(backend.applicantId) : '',
      createByName: backend.applicantName ?? '',
      expectedDate: backend.expectedDate ?? '',
      status: STATUS_TO_FRONTEND[backend.status] ?? 'draft',
      totalAmount: fenToYuan(backend.totalAmount),
      convertedRequestNo: backend.convertedRequestNo ?? '',
      remark: backend.remark ?? '',
      createTime: backend.createTime ?? '',
      updateTime: backend.updateTime ?? '',
      items: (backend.items ?? []).map(materialRequestConverter.toItemFrontend),
    }
  },

  /** 后端实体列表 → 前端 Info 列表 */
  toFrontendList(list: MaterialRequestBackend[] | null | undefined): MaterialRequestInfo[] {
    if (!list || !Array.isArray(list)) return []
    return list.map(materialRequestConverter.toFrontend)
  },

  /** 前端表单数据 → 后端 CreateDTO/UpdateDTO 格式 */
  toCreateDTO(form: {
    title: string;
    storeName: string;
    expectedDate: string;
    remark?: string;
    items: MaterialRequestItem[];
  }): Record<string, unknown> {
    const items = form.items.map(item => ({
      materialId: item.materialId ? Number(item.materialId) : null,
      materialName: item.materialName,
      specification: item.specification ?? null,
      quantity: item.quantity,
      unit: item.unit,
      estimatedPrice: yuanToFen(item.estimatedPrice ?? 0),
      remark: item.remark ?? null,
    }))
    return {
      title: form.title,
      storeName: form.storeName,
      expectedDate: form.expectedDate,
      remark: form.remark ?? null,
      items,
    }
  },

  /** 状态字符串 → 后端数字 */
  toBackendStatus(status: MaterialRequestStatus): number {
    return STATUS_TO_BACKEND[status] ?? 0
  },
}

// ============================================================
// API 实现
// ============================================================

/**
 * 物资需求提报查询参数（传给后端）
 */
interface MaterialRequestQueryParams {
  current: number
  size: number
  requestNo?: string
  status?: number
  storeName?: string
  applicantId?: number
  startDate?: string
  endDate?: string
  keyword?: string
  [key: string]: unknown
}

/** 转采购申请响应 */
interface ConvertResult {
  materialRequestId: number
  materialRequestNo: string
  purchaseRequestId: string
  purchaseRequestNo: string
}

/** 统计响应 */
interface StatisticsResult {
  draft: number
  pending: number
  approved: number
  rejected: number
  converted: number
  total: number
}

export const materialRequestApi = {
  /**
   * 分页查询物资需求提报列表
   * @param params 查询参数（含分页）
   */
  async getList(params: MaterialRequestQueryForm & { page?: number; size?: number }): Promise<{ records: MaterialRequestInfo[]; total: number }> {
    const query: MaterialRequestQueryParams = {
      current: params.page ?? 1,
      size: params.size ?? 10,
    }
    if (params.requestNo) query.requestNo = params.requestNo
    if (params.status) query.status = materialRequestConverter.toBackendStatus(params.status)
    if (params.storeName) query.storeName = params.storeName
    if (params.createBy) query.applicantId = Number(params.createBy)
    if (params.startDate) query.startDate = params.startDate
    if (params.endDate) query.endDate = params.endDate
    if (params.keyword) query.keyword = params.keyword

    const res = await get<MaterialRequestPageBackend | null>('/v1/purchase/material-requests', query)
    const records = materialRequestConverter.toFrontendList(res?.records)
    const total = res?.total ?? 0
    return { records, total }
  },

  /**
   * 根据 ID 查询物资需求提报详情（含明细）
   * @param id 提报ID
   */
  async getById(id: string): Promise<MaterialRequestInfo | null> {
    const res = await get<MaterialRequestBackend | null>(`/v1/purchase/material-requests/${id}`)
    return res ? materialRequestConverter.toFrontend(res) : null
  },

  /**
   * 创建物资需求提报（草稿状态）
   * @param data 表单数据
   */
  async create(data: {
    title: string;
    storeName: string;
    expectedDate: string;
    remark?: string;
    items: MaterialRequestItem[];
  }): Promise<MaterialRequestInfo> {
    const dto = materialRequestConverter.toCreateDTO(data)
    const res = await post<MaterialRequestBackend>('/v1/purchase/material-requests', dto)
    return materialRequestConverter.toFrontend(res)
  },

  /**
   * 更新物资需求提报（仅草稿状态可更新）
   * @param id 提报ID
   * @param data 表单数据
   */
  async update(id: string, data: {
    title: string;
    storeName: string;
    expectedDate: string;
    remark?: string;
    items: MaterialRequestItem[];
  }): Promise<MaterialRequestInfo> {
    const dto = materialRequestConverter.toCreateDTO(data)
    const res = await put<MaterialRequestBackend>(`/v1/purchase/material-requests/${id}`, dto)
    return materialRequestConverter.toFrontend(res)
  },

  /**
   * 删除物资需求提报（仅草稿状态可删除）
   * @param id 提报ID
   */
  async delete(id: string): Promise<void> {
    await del<void>(`/v1/purchase/material-requests/${id}`)
  },

  /**
   * 提交审核（草稿 → 待审核）
   * @param id 提报ID
   */
  async submit(id: string): Promise<void> {
    await put<void>(`/v1/purchase/material-requests/${id}/submit`)
  },

  /**
   * 审核通过（待审核 → 已审核）
   * @param id 提报ID
   */
  async approve(id: string): Promise<void> {
    await put<void>(`/v1/purchase/material-requests/${id}/approve`)
  },

  /**
   * 审核驳回（待审核 → 已驳回，记录驳回原因）
   * @param id 提报ID
   * @param reason 驳回原因
   */
  async reject(id: string, reason: string): Promise<void> {
    await put<void>(`/v1/purchase/material-requests/${id}/reject?reason=${encodeURIComponent(reason)}`)
  },

  /**
   * 转采购申请（已审核 → 已转采购申请）
   * @param id 提报ID
   * @returns 转换结果，含生成的采购申请单号
   */
  async convert(id: string): Promise<ConvertResult> {
    const res = await post<ConvertResult>(`/v1/purchase/material-requests/${id}/convert`)
    return res
  },

  /**
   * 统计各状态数量
   */
  async getStatistics(): Promise<StatisticsResult> {
    const res = await get<StatisticsResult>('/v1/purchase/material-requests/statistics')
    return res
  },
}

export default materialRequestApi
