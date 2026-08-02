/**
 * 资产盘点管理API
 * 对应后端: /v1/asset/inventory (AssetInventoryController)
 *
 * 后端接口：
 * - GET /v1/asset/inventory/page           分页查询盘点单
 * - GET /v1/asset/inventory/{id}           获取盘点单详情（含明细）
 * - POST /v1/asset/inventory               创建盘点单
 * - PUT /v1/asset/inventory/{id}           更新盘点单
 * - POST /v1/asset/inventory/{id}/items/{itemId}  录入盘点结果
 * - POST /v1/asset/inventory/{id}/complete 完成盘点
 *
 * 后端状态编码（InventoryCheckAsset.status）：
 *   0-待盘 1-盘中 2-已审核 3-已完成
 * 前端枚举（InventoryStatus）：
 *   DRAFT-草稿 IN_PROGRESS-进行中 COMPLETED-已完成 CANCELLED-已取消
 * 注意：后端当前未提供独立的「已取消」状态码，CANCELLED 不再错误映射到 2（已审核），
 *       而是映射到保留值 4，避免取消操作被误标记为已审核。
import { get, post, put } from '../request'
import type {
  InventoryCheck,
  InventoryCheckItem,
  InventoryCreateDTO,
  InventoryStatus,
  InventoryCheckType,
  PageResponse,
} from '../../types/asset'
import { InventoryStatus as InvStatus } from '../../types/asset'

// ============================================================
// 后端字段类型定义
// ============================================================

/** 后端盘点单实体（InventoryCheckAsset） */
interface InventoryCheckBackend {
  checkId?: number
  checkCode?: string
  checkType?: number
  checkDate?: string
  status?: number
  totalAssetsCount?: number
  actualCount?: number
  diffCount?: number
  createUserId?: number
  approveUserId?: number
  approveTime?: string
  remark?: string
  createTime?: string
  updateTime?: string
}

/** 后端盘点明细实体（InventoryCheckAssetItem） */
interface InventoryItemBackend {
  itemId?: number
  checkId?: number
  assetId?: number
  expectedLocation?: string
  actualLocation?: string
  expectedStatus?: number
  actualStatus?: number
  conditionRating?: number
  remark?: string
  photoEvidence?: string
  createTime?: string
}

/** 后端分页响应 */
interface InventoryPageBackend {
  records: InventoryCheckBackend[] | null
  total?: number
  current?: number
  size?: number
  pages?: number
}

/** 后端详情响应（Map<String, Object>） */
interface InventoryDetailBackend {
  check?: InventoryCheckBackend
  items?: InventoryItemBackend[] | null
}

// ============================================================
// 状态映射
// ============================================================

/** 后端状态 → 前端 InventoryStatus 映射 */
const BackendStatusMap: Record<number, InventoryStatus> = {
  0: InvStatus.DRAFT,
  1: InvStatus.IN_PROGRESS,
  2: InvStatus.COMPLETED,
  3: InvStatus.COMPLETED,
}

/** 前端 InventoryStatus → 后端状态编码 */
const FrontendStatusMap: Record<InventoryStatus, number> = {
  [InvStatus.DRAFT]: 0,
  [InvStatus.IN_PROGRESS]: 1,
  [InvStatus.COMPLETED]: 3,
  // 已取消不再映射到 2（已审核），使用保留值 4；后端暂不支持时调用方应做兼容提示
  [InvStatus.CANCELLED]: 4,
}

/** 前端 InventoryCheckType → 后端 checkType 编码 */
const CheckTypeMap: Record<InventoryCheckType, number> = {
  full: 1,
  sample: 2,
  cyclic: 3,
}

/** 后端 checkType → 前端 InventoryCheckType */
const BackendCheckTypeMap: Record<number, InventoryCheckType> = {
  1: 'full',
  2: 'sample',
  3: 'cyclic',
}

// ============================================================
// 数据转换
// ============================================================

/**
 * 后端盘点单 → 前端 InventoryCheck 转换
 */
function toInventoryCheck(vo: InventoryCheckBackend): InventoryCheck {
  const status = BackendStatusMap[vo.status ?? 0] || 'draft'
  const checkType = BackendCheckTypeMap[vo.checkType ?? 1] || 'full'
  return {
    id: String(vo.checkId ?? ''),
    checkCode: vo.checkCode || '',
    title: vo.remark || vo.checkCode || '',
    status,
    checkType,
    plannedStartTime: vo.checkDate || '',
    plannedEndTime: vo.checkDate || '',
    actualStartTime: vo.checkDate || undefined,
    actualEndTime: vo.approveTime || undefined,
    creatorId: vo.createUserId != null ? String(vo.createUserId) : '',
    creatorName: '',
    auditorName: '',
    scopeDescription: vo.remark,
    totalAssetCount: vo.totalAssetsCount ?? 0,
    countedCount: vo.actualCount ?? 0,
    overageCount: 0,
    shortageCount: vo.diffCount ?? 0,
    remark: vo.remark,
    createTime: vo.createTime || '',
  }
}

/**
 * 后端盘点明细 → 前端 InventoryCheckItem 转换
 */
function toInventoryItem(vo: InventoryItemBackend): InventoryCheckItem {
  const diff = (vo.actualStatus != null && vo.expectedStatus != null)
    ? (vo.actualStatus === vo.expectedStatus ? 0 : -1)
    : 0
  return {
    id: String(vo.itemId ?? ''),
    checkId: vo.checkId != null ? String(vo.checkId) : '',
    assetId: vo.assetId != null ? String(vo.assetId) : '',
    assetCode: '',
    assetName: '',
    bookQuantity: 1,
    actualQuantity: 1,
    diffQuantity: diff,
    diffReason: vo.remark,
    counterName: '',
    countedAt: vo.createTime,
  }
}

// ============================================================
// API 实现
// ============================================================

export const inventoryApi = {
  /**
   * 获取盘点任务列表
   * 后端路径：GET /v1/asset/inventory/page
   */
  async getList(params?: Record<string, unknown>): Promise<PageResponse<InventoryCheck>> {
    const query: Record<string, unknown> = {
      page: (params?.page as number) ?? 1,
      size: (params?.pageSize as number) ?? 20,
    }
    if (params?.status) {
      const statusStr = params.status as InventoryStatus
      query.status = FrontendStatusMap[statusStr]
    }
    if (params?.checkType) {
      const typeStr = params.checkType as InventoryCheckType
      query.checkType = CheckTypeMap[typeStr]
    }

    const res = await get<InventoryPageBackend | null>('/v1/asset/inventory/page', query)
    const records = (res?.records || []).map(toInventoryCheck)
    return {
      records,
      total: res?.total ?? 0,
      current: res?.current ?? (params?.page as number) ?? 1,
      size: res?.size ?? (params?.pageSize as number) ?? 20,
      pages: res?.pages ?? 0,
    }
  },

  /**
   * 根据ID获取盘点任务详情
   * 后端路径：GET /v1/asset/inventory/{id}
   * 返回 { check, items } 结构，此处提取 check 部分
   */
  async getById(id: string): Promise<InventoryCheck | null> {
    try {
      const res = await get<InventoryDetailBackend | null>(`/v1/asset/inventory/${id}`)
      if (res?.check) {
        return toInventoryCheck(res.check)
      }
      return null
    } catch {
      return null
    }
  },

  /**
   * 创建盘点任务
   * 后端路径：POST /v1/asset/inventory
   */
  async create(data: InventoryCreateDTO): Promise<InventoryCheck> {
    const dto: InventoryCheckBackend = {
      checkCode: `IC-${Date.now()}`,
      checkType: CheckTypeMap[data.checkType] ?? 1,
      checkDate: data.plannedStartTime,
      status: 0,
      totalAssetsCount: data.assetIds?.length ?? 0,
      actualCount: 0,
      diffCount: 0,
      remark: data.title || data.scopeDescription,
    }
    const res = await post<InventoryCheckBackend>('/v1/asset/inventory', dto)
    return toInventoryCheck(res)
  },

  /**
   * 更新盘点任务
   * 后端路径：PUT /v1/asset/inventory/{id}
   */
  async update(id: string, data: Partial<InventoryCheck>): Promise<InventoryCheck | null> {
    const dto: InventoryCheckBackend = {}
    if (data.status !== undefined) {
      dto.status = FrontendStatusMap[data.status]
    }
    if (data.remark !== undefined) dto.remark = data.remark
    if (data.scopeDescription !== undefined) dto.remark = data.scopeDescription
    if (data.checkType !== undefined) {
      dto.checkType = CheckTypeMap[data.checkType]
    }
    const res = await put<InventoryCheckBackend>(`/v1/asset/inventory/${id}`, dto)
    return res ? toInventoryCheck(res) : null
  },

  /**
   * 删除盘点任务
   * 后端无对应接口，抛出异常提示
   */
  async delete(_id: string): Promise<boolean> {
    throw new Error('后端暂不支持删除盘点任务')
  },

  /**
   * 发起盘点（将草稿变为进行中）
   * 后端路径：PUT /v1/asset/inventory/{id}（更新状态为1-盘中）
   */
  async start(id: string): Promise<InventoryCheck | null> {
    return await inventoryApi.update(id, { status: InvStatus.IN_PROGRESS })
  },

  /**
   * 完成盘点
   * 后端路径：POST /v1/asset/inventory/{id}/complete
   */
  async complete(id: string): Promise<InventoryCheck | null> {
    await post<void>(`/v1/asset/inventory/${id}/complete`)
    return await inventoryApi.getById(id)
  },

  /**
   * 取消盘点
   * 后端当前无独立的「已取消」状态码，CANCELLED 已改为映射到保留值 4，
   * 避免被误写入为 2（已审核）。若后端返回不支持，需由调用方提示用户。
   */
  async cancel(id: string): Promise<InventoryCheck | null> {
    return await inventoryApi.update(id, { status: InvStatus.CANCELLED })
  },

  /**
   * 获取盘点明细项
   * 后端路径：GET /v1/asset/inventory/{id}
   * 后端详情接口返回 { check, items }，此处提取 items 部分
   */
  async getItems(checkId: string): Promise<InventoryCheckItem[]> {
    try {
      const res = await get<InventoryDetailBackend | null>(`/v1/asset/inventory/${checkId}`)
      const items = res?.items || []
      return items.map(toInventoryItem)
    } catch {
      return []
    }
  },

  /**
   * 更新盘点明细项（填写实盘数量）
   * 后端路径：POST /v1/asset/inventory/{id}/items/{itemId}
   */
  async updateItem(itemId: string, data: Partial<InventoryCheckItem>): Promise<InventoryCheckItem | null> {
    const checkId = data.checkId || ''
    const dto: InventoryItemBackend = {
      actualLocation: data.diffReason,
      remark: data.diffReason,
    }
    const res = await post<InventoryItemBackend>(`/v1/asset/inventory/${checkId}/items/${itemId}`, dto)
    return res ? toInventoryItem(res) : null
  },
}
